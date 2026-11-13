
"""
LOWKEY - Convert Model B to ONNX

Converts:
StandardScaler + IsolationForest

The exported ONNX model accepts the original
7 numerical features and returns score_samples.

Original JOBLIB model remains unchanged.
"""

from pathlib import Path
import json

import joblib
import numpy as np
import pandas as pd
import onnx
import onnxruntime as ort

from sklearn.pipeline import Pipeline
from skl2onnx import convert_sklearn
from skl2onnx.common.data_types import FloatTensorType


# ==========================================
# 1. PATHS
# ==========================================

BASE = Path(__file__).resolve().parent

MODEL_DIR = BASE / "LOWKEY_PROCESSED" / "model"

MODEL_PATH = (
    MODEL_DIR / "lowkey_model_b_magnitude.joblib"
)

ONNX_PATH = MODEL_DIR / "lowkey_model_b.onnx"

TEMP_PATH = MODEL_DIR / "lowkey_model_b_candidate.onnx"

METADATA_PATH = (
    MODEL_DIR / "model_b_onnx_metadata.json"
)


# ==========================================
# 2. LOAD TRAINED MODEL B
# ==========================================

print("\n[1/6] Loading trained Model B...")

if not MODEL_PATH.exists():
    raise FileNotFoundError(MODEL_PATH)

artifact = joblib.load(MODEL_PATH)

scaler = artifact["scaler"]
model = artifact["model"]
features = artifact["feature_names"]

print("Model loaded successfully.")
print("Features:", len(features))


# ==========================================
# 3. BUILD COMPLETE PIPELINE
# ==========================================

print("\n[2/6] Combining scaler and model...")

pipeline = Pipeline([
    ("scaler", scaler),
    ("isolation_forest", model)
])


# ==========================================
# 4. CONVERT TO ONNX
# ==========================================

print("\n[3/6] Converting Model B to ONNX...")

initial_types = [
    (
        "float_input",
        FloatTensorType([None, len(features)])
    )
]

options = {
    id(model): {
        "score_samples": True
    }
}

onnx_model = convert_sklearn(
    pipeline,
    initial_types=initial_types,
    options=options
)

onnx.checker.check_model(onnx_model)

TEMP_PATH.write_bytes(
    onnx_model.SerializeToString()
)

print("ONNX conversion completed.")
print("Temporary model:", TEMP_PATH)


# ==========================================
# 5. VERIFY ONNX AGAINST PYTHON
# ==========================================

print("\n[4/6] Checking inference compatibility...")

session = ort.InferenceSession(
    str(TEMP_PATH),
    providers=["CPUExecutionProvider"]
)

input_name = session.get_inputs()[0].name

# Check actual output names and order.
outputs = session.get_outputs()

print("\nONNX OUTPUTS:")

for i, output in enumerate(outputs):
    print(i, output.name, output.shape, output.type)

# Use the same genuine-owner observations used for training.
data_path = (
    BASE
    / "LOWKEY_PROCESSED"
    / "features"
    / "lowkey_ml_features.csv"
)

df = pd.read_csv(data_path)

touch = (
    (df["tap_count"] > 0)
    | (df["scroll_count"] > 0)
)

valid = touch & (
    df["acc_sample_count"] >= 30
)

data = df.loc[valid].copy()

X = data[features].astype("float32")

# Python reference scores.
X_scaled = scaler.transform(X)

python_scores = model.score_samples(
    X_scaled
)

python_predictions = model.predict(
    X_scaled
)

# ONNX inference from the original unscaled features.
onnx_outputs = session.run(
    None,
    {
        input_name: X.to_numpy(dtype=np.float32)
    }
)

if len(onnx_outputs) < 3:
    raise RuntimeError(
        "Expected predict, decision_function and score_samples outputs."
    )

# skl2onnx IsolationForest output order:
# 0 = predict
# 1 = decision_function
# 2 = score_samples

onnx_predictions = np.asarray(
    onnx_outputs[0]
).reshape(-1)

onnx_scores = np.asarray(
    onnx_outputs[2]
).reshape(-1)

if len(onnx_scores) != len(python_scores):
    raise RuntimeError("ONNX output row count mismatch.")

score_error = np.max(
    np.abs(python_scores - onnx_scores)
)

prediction_match = np.array_equal(
    python_predictions,
    onnx_predictions
)

print("\n===== PYTHON vs ONNX =====")

print("Test observations:", len(X))

print(
    "Maximum score difference:",
    float(score_error)
)

print(
    "Predictions identical:",
    prediction_match
)

# Strict parity check.
if score_error > 1e-5:
    raise RuntimeError(
        "ONNX score parity check failed."
    )

if not prediction_match:
    raise RuntimeError(
        "ONNX prediction parity check failed."
    )

print("\nONNX inference matches Python.")


# ==========================================
# 6. SAVE VERIFIED MODEL AND METADATA
# ==========================================

print("\n[5/6] Saving verified ONNX model...")

if ONNX_PATH.exists():
    raise FileExistsError(
        f"Refusing to overwrite existing model: {ONNX_PATH}"
    )

TEMP_PATH.replace(ONNX_PATH)

metadata = {
    "model_name": "LOWKEY Isolation Forest Model B",
    "algorithm": "IsolationForest",
    "input_features": features,
    "input_feature_count": len(features),
    "minimum_accelerometer_samples": (
        artifact["minimum_accelerometer_samples"]
    ),
    "anomaly_threshold": (
        artifact["anomaly_threshold"]
    ),
    "score_rule": "score < threshold means anomalous",
    "scaler_included": True,
    "python_onnx_max_score_difference": float(score_error),
    "python_onnx_predictions_identical": bool(
        prediction_match
    )
}

with open(
    METADATA_PATH,
    "w",
    encoding="utf-8"
) as file:
    json.dump(metadata, file, indent=4)

print("Verified ONNX model saved.")


# ==========================================
# 7. FINAL SUMMARY
# ==========================================

print("\n[6/6] CONVERSION COMPLETE")

print("ONNX model:", ONNX_PATH)
print("Metadata:", METADATA_PATH)

print("\nOriginal JOBLIB model preserved.")
print("No original dataset modified.")
