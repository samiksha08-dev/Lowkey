
from pathlib import Path
import numpy as np
import pandas as pd

BASE = Path(__file__).resolve().parent

DATA_PATH = (
    BASE / "LOWKEY_PROCESSED/features/lowkey_ml_features.csv"
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

AUDIT_COLUMNS = FEATURES + ["acc_sample_count"]


def main():

    print("\n===== LOWKEY MODEL B TRAINING DATA AUDIT =====")

    if not DATA_PATH.exists():
        raise FileNotFoundError(
            f"Training dataset not found: {DATA_PATH}"
        )

    df = pd.read_csv(DATA_PATH)

    required = AUDIT_COLUMNS + ["timestamp_ist"]

    missing = [
        col for col in required if col not in df.columns
    ]

    if missing:
        raise ValueError(f"Missing columns: {missing}")

    df["timestamp_ist"] = pd.to_datetime(
        df["timestamp_ist"],
        errors="coerce"
    )

    for col in AUDIT_COLUMNS:
        df[col] = pd.to_numeric(
            df[col],
            errors="coerce"
        )

    df = df.dropna(subset=["timestamp_ist"])

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

    X_train = train[AUDIT_COLUMNS]

    if X_train.empty:
        raise ValueError("Training dataset is empty.")

    if X_train.isna().any().any():
        raise ValueError("Missing training values found.")

    if not np.isfinite(X_train.to_numpy()).all():
        raise ValueError("Non-finite training values found.")

    print("\n===== DATASET SUMMARY =====")
    print("Total raw windows:", len(df))
    print("Eligible windows:", len(data))
    print("Training windows:", len(train))
    print("Validation windows:", len(validation))
    print("Holdout windows:", len(test))

    print("\n===== TRAINING FEATURE DISTRIBUTION =====")

    summary = pd.DataFrame({
        "min": X_train.min(),
        "p01": X_train.quantile(0.01),
        "p05": X_train.quantile(0.05),
        "median": X_train.median(),
        "mean": X_train.mean(),
        "p95": X_train.quantile(0.95),
        "p99": X_train.quantile(0.99),
        "max": X_train.max(),
        "std": X_train.std()
    })

    print(summary.round(4).to_string())

    print("\n===== ACCELEROMETER SAMPLE COUNT AUDIT =====")

    print(
        "Training sample count min:",
        int(X_train["acc_sample_count"].min())
    )

    print(
        "Training sample count median:",
        int(X_train["acc_sample_count"].median())
    )

    print(
        "Training sample count mean:",
        round(X_train["acc_sample_count"].mean(), 2)
    )

    print(
        "Training sample count P95:",
        round(X_train["acc_sample_count"].quantile(0.95), 2)
    )

    print(
        "Training sample count P99:",
        round(X_train["acc_sample_count"].quantile(0.99), 2)
    )

    print(
        "Training sample count max:",
        int(X_train["acc_sample_count"].max())
    )

    print("\n===== AUDIT COMPLETE =====")


if __name__ == "__main__":
    main()
