"""10-second feature engineering for LOWKEY Member 2.

This script creates modality-level window features, joins them on overlapping
10-second windows, and adds missingness indicators. Run only on restricted
local/Drive data; never upload resulting behavioural datasets to GitHub.
"""
import argparse
from pathlib import Path
import numpy as np
import pandas as pd


WINDOW = "10s"


def _ensure_time(df):
    out = df.copy()
    if "timestamp_ist" not in out.columns:
        if "timestamp" not in out.columns:
            raise ValueError("Input must contain timestamp_ist or millisecond timestamp")
        out["timestamp_ist"] = pd.to_datetime(out["timestamp"], unit="ms", utc=True).dt.tz_convert("Asia/Kolkata")
    else:
        out["timestamp_ist"] = pd.to_datetime(out["timestamp_ist"], errors="coerce")
    return out.dropna(subset=["timestamp_ist"]).sort_values("timestamp_ist")


def tap_scroll_features(events):
    events = _ensure_time(events)
    events = events.set_index("timestamp_ist")
    parts = []
    if "event_type" in events.columns:
        taps = events[events["event_type"].astype(str).str.upper().eq("TAP")]
        scrolls = events[events["event_type"].astype(str).str.upper().str.contains("SCROLL", na=False)]
    else:
        taps, scrolls = events.iloc[0:0], events

    tap = pd.DataFrame(index=taps.resample(WINDOW).size().index)
    tap["tap_count"] = taps.resample(WINDOW).size()
    for col, outcol in [("x", "avg_tap_x"), ("y", "avg_tap_y")]:
        if col in taps.columns:
            tap[outcol] = taps[col].resample(WINDOW).mean()
    parts.append(tap)

    scroll = pd.DataFrame(index=scrolls.resample(WINDOW).size().index)
    scroll["scroll_count"] = scrolls.resample(WINDOW).size()
    for col, outcol in [("x", "avg_scroll_x"), ("y", "avg_scroll_y"),
                        ("scroll_delta_y", "avg_scroll_delta_y")]:
        if col in scrolls.columns:
            scroll[outcol] = scrolls[col].resample(WINDOW).mean()
    parts.append(scroll)
    result = pd.concat(parts, axis=1).sort_index()
    result.index.name = "timestamp_ist"
    return result.reset_index()


def accelerometer_features(acc):
    acc = _ensure_time(acc)
    for col in ["x", "y", "z"]:
        if col not in acc.columns:
            raise ValueError(f"Accelerometer input missing {col}")
        acc[col] = pd.to_numeric(acc[col], errors="coerce")
    acc["magnitude"] = np.sqrt(acc["x"]**2 + acc["y"]**2 + acc["z"]**2)
    grouped = acc.set_index("timestamp_ist").resample(WINDOW)
    result = grouped.agg({
        "x": ["mean", "std", "min", "max"],
        "y": ["mean", "std", "min", "max"],
        "z": ["mean", "std", "min", "max"],
        "magnitude": ["mean", "std", "min", "max"],
    })
    result.columns = ["_".join(c) for c in result.columns]
    result["acc_sample_count"] = grouped.size()
    result["magnitude_rms"] = grouped["magnitude"].apply(lambda s: np.sqrt(np.nanmean(np.square(s))) if s.notna().any() else np.nan)
    result.index.name = "timestamp_ist"
    return result.reset_index()


def build_feature_dictionary(columns):
    descriptions = {
        "tap_count": ("TAP/SCROLL", "Number of TAP events in a 10-second window", "count"),
        "avg_tap_x": ("TAP/SCROLL", "Mean horizontal coordinate of TAP events", "coordinate"),
        "avg_tap_y": ("TAP/SCROLL", "Mean vertical coordinate of TAP events", "coordinate"),
        "scroll_count": ("TAP/SCROLL", "Number of SCROLL events in a 10-second window", "count"),
        "avg_scroll_x": ("TAP/SCROLL", "Mean horizontal scroll position", "coordinate"),
        "avg_scroll_y": ("TAP/SCROLL", "Mean vertical scroll position", "coordinate"),
        "avg_scroll_delta_y": ("TAP/SCROLL", "Mean vertical scroll displacement", "coordinate/window"),
        "acc_sample_count": ("Accelerometer", "Number of sensor samples in a 10-second window", "count"),
    }
    rows = []
    for name in columns:
        source, description, unit = descriptions.get(name, ("Derived", "See feature name and preprocessing code", "varies"))
        rows.append({"feature_name": name, "source": source, "description": description, "unit": unit})
    return pd.DataFrame(rows)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--tap-scroll", required=True)
    parser.add_argument("--accelerometer", required=True)
    parser.add_argument("--output-dir", required=True)
    args = parser.parse_args()

    outdir = Path(args.output_dir)
    outdir.mkdir(parents=True, exist_ok=True)
    tap_raw = pd.read_csv(args.tap_scroll)
    acc_raw = pd.read_csv(args.accelerometer)

    tap_features = tap_scroll_features(tap_raw)
    acc_features = accelerometer_features(acc_raw)

    tap_features["timestamp_ist"] = pd.to_datetime(tap_features["timestamp_ist"], errors="coerce")
    acc_features["timestamp_ist"] = pd.to_datetime(acc_features["timestamp_ist"], errors="coerce")

    # Keep only the common 10-second time coverage.
    start = max(tap_features["timestamp_ist"].min(), acc_features["timestamp_ist"].min())
    end = min(tap_features["timestamp_ist"].max(), acc_features["timestamp_ist"].max())
    tap_features = tap_features[tap_features["timestamp_ist"].between(start, end)]
    acc_features = acc_features[acc_features["timestamp_ist"].between(start, end)]

    merged = tap_features.merge(acc_features, on="timestamp_ist", how="inner")
    for col in merged.columns:
        if col != "timestamp_ist" and merged[col].isna().any():
            merged[f"{col}_missing"] = merged[col].isna().astype("int8")

    merged.to_csv(outdir / "lowkey_ml_features.csv", index=False)
    build_feature_dictionary(merged.columns).to_csv(outdir / "lowkey_feature_dictionary.csv", index=False)
    tap_features.to_csv(outdir / "tap_scroll_10sec_features.csv", index=False)
    acc_features.to_csv(outdir / "accelerometer_10sec_features.csv", index=False)
    print("Feature engineering complete:", merged.shape)
    print("Outputs saved in:", outdir)


if __name__ == "__main__":
    main()
