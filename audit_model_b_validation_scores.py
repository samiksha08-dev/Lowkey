
from pathlib import Path
import joblib
import numpy as np
import pandas as pd

BASE = Path(__file__).resolve().parent

DATA_PATH = (
    BASE / "LOWKEY_PROCESSED/features/lowkey_ml_features.csv"
)

MODEL_PATH = (
    BASE / "LOWKEY_PROCESSED/model/lowkey_model_b_magnitude.joblib"
)

FEATURES = [
    "tap_count",
    "scroll_count",
    "magnitude_mean",
    "magnitude_std",
    "magnitude_min",
    "magnitude_max",
    "magnitude_rms"
]


def describe_scores(name, scores):

    scores = np.asarray(scores, dtype=float)

    print(f"\n===== {name} =====")

    print("Count:", len(scores))
    print("Minimum:", round(float(np.min(scores)), 6))
    print("P01:", round(float(np.percentile(scores, 1)), 6))
    print("P05:", round(float(np.percentile(scores, 5)), 6))
    print("P25:", round(float(np.percentile(scores, 25)), 6))
    print("Median:", round(float(np.median(scores)), 6))
    print("P75:", round(float(np.percentile(scores, 75)), 6))
    print("P95:", round(float(np.percentile(scores, 95)), 6))
    print("P99:", round(float(np.percentile(scores, 99)), 6))
    print("Maximum:", round(float(np.max(scores)), 6))


def main():

    print("\n===== LOWKEY MODEL B VALIDATION SCORE AUDIT =====")

    if not DATA_PATH.exists():
        raise FileNotFoundError(DATA_PATH)

    if not MODEL_PATH.exists():
        raise FileNotFoundError(MODEL_PATH)

    df = pd.read_csv(DATA_PATH)

    df["timestamp_ist"] = pd.to_datetime(
        df["timestamp_ist"]
    )

    touch = (
        (df["tap_count"] > 0) |
        (df["scroll_count"] > 0)
    )

    valid = touch & (df["acc_sample_count"] >= 30)

    data = df.loc[valid].copy()
    data = data.sort_values("timestamp_ist")

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

    artifact = joblib.load(MODEL_PATH)

    if artifact["feature_names"] != FEATURES:
        raise ValueError("Model feature order mismatch.")

    model = artifact["model"]
    scaler = artifact["scaler"]

    threshold = float(artifact["anomaly_threshold"])

    # Prepare exact validation features.
    X_val = validation[FEATURES].apply(
        pd.to_numeric,
        errors="coerce"
    )

    if X_val.isna().any().any():
        raise ValueError("Missing validation feature values.")

    if not np.isfinite(X_val.to_numpy()).all():
        raise ValueError("Non-finite validation values found.")

    X_val_scaled = scaler.transform(X_val)

    validation_scores = model.score_samples(
        X_val_scaled
    )

    validation_flags = validation_scores < threshold

    # Recalculate original holdout results.
    X_test = test[FEATURES].apply(
        pd.to_numeric,
        errors="coerce"
    )

    if X_test.isna().any().any():
        raise ValueError("Missing holdout feature values.")

    X_test_scaled = scaler.transform(X_test)

    holdout_scores = model.score_samples(
        X_test_scaled
    )

    holdout_flags = holdout_scores < threshold

    print("\n===== DATA SPLIT =====")
    print("Training:", len(train))
    print("Validation:", len(validation))
    print("Holdout:", len(test))

    print("\nSaved threshold:", threshold)

    recalculated_threshold = float(
        np.percentile(validation_scores, 5)
    )

    print(
        "Recalculated 5th percentile:",
        recalculated_threshold
    )

    print("\n===== GENUINE VALIDATION REJECTION =====")

    print(
        "Validation rejection rate:",
        round(float(validation_flags.mean() * 100), 2),
        "%"
    )

    print("\n===== GENUINE HOLDOUT REJECTION =====")

    print(
        "Holdout rejection rate:",
        round(float(holdout_flags.mean() * 100), 2),
        "%"
    )

    describe_scores(
        "VALIDATION SCORE DISTRIBUTION",
        validation_scores
    )

    describe_scores(
        "HOLDOUT SCORE DISTRIBUTION",
        holdout_scores
    )

    print("\n===== AUDIT COMPLETE =====")


if __name__ == "__main__":
    main()
