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

    if "event_type" not in events.columns:
        raise ValueError("TAP/SCROLL input must contain event_type")

    events["_event_type"] = (
        events["event_type"]
        .astype("string")
        .str.strip()
        .str.upper()
    )

    events = events.set_index("timestamp_ist")

    taps = events[events["_event_type"] == "TAP"]
    scrolls = events[events["_event_type"] == "SCROLL"]

    # Create a continuous timeline covering all observed events.
    window_index = events.resample(WINDOW).size().index

    # ---------------- TAP FEATURES ----------------
    tap_features = pd.DataFrame(index=window_index)

    tap_count = (
        taps.resample(WINDOW)
        .size()
        .reindex(window_index, fill_value=0)
    )

    tap_features["tap_count"] = tap_count.astype("int64")

    for source, target in [
        ("x_position", "avg_tap_x"),
        ("y_position", "avg_tap_y")
    ]:
        if source in taps.columns:
            values = pd.to_numeric(
                taps[source], errors="coerce"
            )
            tap_features[target] = (
                values.resample(WINDOW)
                .mean()
                .reindex(window_index)
            )

    # TAP coordinate availability
    if {"x_position", "y_position"}.issubset(taps.columns):
        valid = (
            taps["x_position"].notna()
            & taps["y_position"].notna()
        )

        available = (
            valid.astype("int8")
            .resample(WINDOW)
            .sum()
            .reindex(window_index, fill_value=0)
        )
    else:
        available = pd.Series(
            0, index=window_index, dtype="int64"
        )

    tap_features["tap_coordinate_available_count"] = available

    tap_features["tap_coordinate_coverage"] = (
        available / tap_count.replace(0, np.nan)
    )

    # ---------------- SCROLL FEATURES ----------------
    scroll_features = pd.DataFrame(index=window_index)

    scroll_count = (
        scrolls.resample(WINDOW)
        .size()
        .reindex(window_index, fill_value=0)
    )

    scroll_features["scroll_count"] = (
        scroll_count.astype("int64")
    )

    for source, target in [
        ("scroll_x", "avg_scroll_x"),
        ("scroll_y", "avg_scroll_y"),
        ("scroll_delta_x", "avg_scroll_delta_x"),
        ("scroll_delta_y", "avg_scroll_delta_y")
    ]:
        if source in scrolls.columns:
            values = pd.to_numeric(
                scrolls[source], errors="coerce"
            )

            scroll_features[target] = (
                values.resample(WINDOW)
                .mean()
                .reindex(window_index)
            )

    # ---------------- COMBINE FEATURES ----------------
    result = pd.concat(
        [tap_features, scroll_features],
        axis=1
    )

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
        "timestamp_ist": (
            "Time",
            "Start time of the 10-second window",
            "IST datetime"
        ),

        # TAP features
        "tap_count": (
            "TAP",
            "Total number of taps in the window",
            "count"
        ),
        "avg_tap_x": (
            "TAP",
            "Average horizontal tap coordinate",
            "pixels"
        ),
        "avg_tap_y": (
            "TAP",
            "Average vertical tap coordinate",
            "pixels"
        ),
        "tap_coordinate_available_count": (
            "TAP",
            "Number of taps with valid X and Y coordinates",
            "count"
        ),
        "tap_coordinate_coverage": (
            "TAP",
            "Proportion of taps with valid coordinates",
            "ratio"
        ),

        # SCROLL features
        "scroll_count": (
            "SCROLL",
            "Number of scroll events in the window",
            "count"
        ),
        "avg_scroll_x": (
            "SCROLL",
            "Average horizontal scroll position",
            "pixels"
        ),
        "avg_scroll_y": (
            "SCROLL",
            "Average vertical scroll position",
            "pixels"
        ),
        "avg_scroll_delta_x": (
            "SCROLL",
            "Average horizontal scroll displacement per event",
            "pixels/event"
        ),
        "avg_scroll_delta_y": (
            "SCROLL",
            "Average vertical scroll displacement per event",
            "pixels/event"
        ),

        # Accelerometer features
        "acc_sample_count": (
            "Accelerometer",
            "Number of sensor readings in the window",
            "count"
        ),
        "magnitude_rms": (
            "Accelerometer",
            "Root mean square of acceleration magnitude",
            "m/s^2"
        )
    }

    # Add descriptions for accelerometer statistics
    for axis in ["x", "y", "z", "magnitude"]:
        for statistic in ["mean", "std", "min", "max"]:
            name = f"{axis}_{statistic}"

            descriptions[name] = (
                "Accelerometer",
                f"{statistic.title()} of {axis} acceleration",
                "m/s^2"
            )

    rows = []

    for name in columns:

        if name in descriptions:
            source, description, unit = descriptions[name]

        elif name.endswith("_missing"):
            base_name = name[:-8]

            source = "Data Quality"
            description = (
                f"Missingness indicator for {base_name}; "
                "1 means missing and 0 means available"
            )
            unit = "binary (0/1)"

        else:
            source = "Derived"
            description = "Derived feature; inspect preprocessing code"
            unit = "varies"

        rows.append({
            "feature_name": name,
            "source": source,
            "description": description,
            "unit": unit
        })

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
