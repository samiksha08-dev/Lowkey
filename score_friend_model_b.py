
from pathlib import Path
import json

import numpy as np
import pandas as pd
import onnxruntime as ort


ROOT = Path(__file__).resolve().parent

FEATURES_FILE = (
    ROOT / "friend_test_2026-11-12"
    / "features" / "lowkey_ml_features.csv"
)

MODEL_FILE = (
    ROOT / "LOWKEY_PROCESSED"
    / "model" / "lowkey_model_b_compact.onnx"
)

METADATA_FILE = (
    ROOT / "LOWKEY_PROCESSED"
    / "model" / "lowkey_model_b_onnx_metadata.json"
)

OUTPUT_FILE = (
    ROOT / "friend_test_2026-11-12"
    / "friend_model_b_scores.csv"
)


def main():
    for path in [FEATURES_FILE, MODEL_FILE, METADATA_FILE]:
        if not path.exists():
            raise FileNotFoundError(f"File not found: {path}")

    with open(METADATA_FILE, "r", encoding="utf-8") as f:
        metadata = json.load(f)

    features = metadata["features"]
    threshold = float(metadata["anomaly_threshold"])
    minimum_samples = int(
        metadata["minimum_accelerometer_samples"]
    )

    data = pd.read_csv(FEATURES_FILE)

    required = features + [
        "acc_sample_count",
        "tap_count",
        "scroll_count",
        "timestamp_ist"
    ]

    missing = [col for col in required if col not in data.columns]

    if missing:
        raise ValueError(f"Missing columns: {missing}")

    acc_count = pd.to_numeric(
        data["acc_sample_count"], errors="coerce"
    )

    touch_count = (
        pd.to_numeric(data["tap_count"], errors="coerce").fillna(0)
        + pd.to_numeric(data["scroll_count"], errors="coerce").fillna(0)
    )

    valid = (
        (acc_count >= minimum_samples)
        & (touch_count > 0)
    )

    eligible = data.loc[valid].copy()

    if eligible.empty:
        raise ValueError("No eligible windows found.")

    X_raw = eligible[features].apply(
        pd.to_numeric, errors="coerce"
    ).to_numpy(dtype=np.float64)

    if not np.isfinite(X_raw).all():
        raise ValueError("Invalid or missing model feature values.")

    scaler_mean = np.asarray(
        metadata["scaler_mean"], dtype=np.float64
    )
    scaler_scale = np.asarray(
        metadata["scaler_scale"], dtype=np.float64
    )

    if len(scaler_mean) != len(features):
        raise ValueError("Scaler mean does not match features.")

    if len(scaler_scale) != len(features):
        raise ValueError("Scaler scale does not match features.")

    if np.any(scaler_scale == 0):
        raise ValueError("Invalid scaler scale.")

    # Apply the original StandardScaler parameters.
    X_scaled = (X_raw - scaler_mean) / scaler_scale
    X_scaled = np.ascontiguousarray(X_scaled, dtype=np.float32)

    # Run the existing Model B ONNX model.
    session = ort.InferenceSession(
        str(MODEL_FILE),
        providers=["CPUExecutionProvider"]
    )

    input_name = session.get_inputs()[0].name
    output_name = session.get_outputs()[0].name

    scores = session.run(
        [output_name],
        {input_name: X_scaled}
    )[0].reshape(-1)

    if len(scores) != len(eligible):
        raise RuntimeError("Model output count mismatch.")

    results = eligible[
        ["timestamp_ist"] + features + ["acc_sample_count"]
    ].copy()

    results["model_b_score"] = scores
    results["anomalous"] = scores < threshold
    results["anomaly_threshold"] = threshold

    results.to_csv(OUTPUT_FILE, index=False)

    anomalous_count = int(results["anomalous"].sum())
    normal_count = len(results) - anomalous_count

    print("\n========== FRIEND MODEL B RESULTS ==========")
    print("Model:", metadata["model_name"])
    print("Algorithm:", metadata["algorithm"])
    print("Eligible windows:", len(results))
    print("Anomalous windows:", anomalous_count)
    print("Normal windows:", normal_count)

    print(
        "Anomalous percentage:",
        round(anomalous_count / len(results) * 100, 2), "%"
    )

    print("Threshold:", threshold)

    print("\nScore statistics:")
    print("Minimum:", round(float(scores.min()), 6))
    print("Median:", round(float(np.median(scores)), 6))
    print("Maximum:", round(float(scores.max()), 6))

    print("\nFirst 10 window results:")
    print(
        results[
            ["timestamp_ist", "tap_count", "scroll_count",
             "acc_sample_count", "model_b_score", "anomalous"]
        ].head(10).to_string(index=False)
    )

    print("\nResults saved to:", OUTPUT_FILE)


if __name__ == "__main__":
    main()
