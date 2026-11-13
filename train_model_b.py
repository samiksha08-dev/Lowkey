
from pathlib import Path
import json
import joblib
import numpy as np
import pandas as pd

from sklearn.preprocessing import StandardScaler
from sklearn.ensemble import IsolationForest

BASE = Path(__file__).resolve().parent

DATA_PATH = BASE / "LOWKEY_PROCESSED/features/lowkey_ml_features.csv"
OUTPUT_DIR = BASE / "LOWKEY_PROCESSED/model"
OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

# Orientation-reduced feature set
FEATURES = [
    "tap_count",
    "scroll_count",
    "magnitude_mean",
    "magnitude_std",
    "magnitude_min",
    "magnitude_max",
    "magnitude_rms"
]

print("\n[1] Loading existing phone-collected dataset")

df = pd.read_csv(DATA_PATH)
df["timestamp_ist"] = pd.to_datetime(df["timestamp_ist"])

# Same selection used for Model A
touch = (
    (df["tap_count"] > 0) |
    (df["scroll_count"] > 0)
)

valid = touch & (df["acc_sample_count"] >= 30)

data = df.loc[valid].copy()
data = data.sort_values("timestamp_ist")

print("Selected genuine-owner windows:", len(data))

# Check selected features
X = data[FEATURES].apply(pd.to_numeric, errors="coerce")

if X.isna().any().any():
    raise ValueError("Missing values in Model B features")

if not np.isfinite(X.to_numpy()).all():
    raise ValueError("Non-finite feature values found")

if (X.nunique() <= 1).any():
    raise ValueError("Constant feature detected")

# Same chronological split as Model A
data["date"] = data["timestamp_ist"].dt.date

day28 = data[
    data["date"] == pd.Timestamp("2026-10-28").date()
]

day29 = data[
    data["date"] == pd.Timestamp("2026-10-29").date()
]

day30 = data[
    data["date"] == pd.Timestamp("2026-10-30").date()
]

cut = int(len(day29) * 0.70)

train = pd.concat([
    day28,
    day29.iloc[:cut]
])

validation = day29.iloc[cut:]
test = day30

X_train = train[FEATURES]
X_val = validation[FEATURES]
X_test = test[FEATURES]

print("\n[2] Data split")
print("Training:", len(X_train))
print("Validation:", len(X_val))
print("Holdout:", len(X_test))

# Fit scaler ONLY on training data
scaler = StandardScaler()

X_train_scaled = scaler.fit_transform(X_train)
X_val_scaled = scaler.transform(X_val)
X_test_scaled = scaler.transform(X_test)

# Train Model B
print("\n[3] Training Model B")

model = IsolationForest(
    n_estimators=300,
    contamination="auto",
    random_state=42,
    n_jobs=-1
)

model.fit(X_train_scaled)

# Calibrate using validation data
val_scores = model.score_samples(X_val_scaled)

threshold = float(np.percentile(val_scores, 5))

val_flagged = val_scores < threshold

# Evaluate on October 30
test_scores = model.score_samples(X_test_scaled)

test_flagged = test_scores < threshold

val_frr = float(val_flagged.mean())
test_frr = float(test_flagged.mean())

print("\n===== MODEL B RESULTS =====")

print("Features:", len(FEATURES))
print("Training windows:", len(X_train))
print("Validation windows:", len(X_val))
print("Holdout windows:", len(X_test))

print("Validation rejection rate:",
      round(val_frr * 100, 2), "%")

print("Holdout rejection rate:",
      round(test_frr * 100, 2), "%")

print("Anomaly threshold:", threshold)

# Save separately; do not overwrite Model A
artifact = {
    "model": model,
    "scaler": scaler,
    "feature_names": FEATURES,
    "anomaly_threshold": threshold,
    "score_rule": "score < threshold means anomalous",
    "window_seconds": 10,
    "minimum_accelerometer_samples": 30
}

joblib.dump(
    artifact,
    OUTPUT_DIR / "lowkey_model_b_magnitude.joblib"
)

summary = {
    "model": "IsolationForest_Model_B",
    "features": FEATURES,
    "training_windows": len(X_train),
    "validation_windows": len(X_val),
    "test_windows": len(X_test),
    "validation_genuine_rejection_rate": val_frr,
    "test_genuine_rejection_rate": test_frr,
    "anomaly_threshold": threshold
}

with open(
    OUTPUT_DIR / "model_b_summary.json",
    "w",
    encoding="utf-8"
) as f:
    json.dump(summary, f, indent=4)

pd.DataFrame({
    "timestamp_ist": test["timestamp_ist"].values,
    "anomaly_score": test_scores,
    "predicted_anomaly": test_flagged
}).to_csv(
    OUTPUT_DIR / "model_b_holdout_scores.csv",
    index=False
)

print("\nModel B saved successfully!")
print("Output:", OUTPUT_DIR)
