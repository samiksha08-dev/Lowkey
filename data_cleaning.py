"""LOWKEY Member 2: data cleaning utilities.

Raw behavioural data is read from a restricted local/Google Drive folder.
Do not commit raw or processed behavioural datasets to GitHub.
"""
from __future__ import annotations

import pandas as pd


def add_ist_timestamps(df: pd.DataFrame) -> pd.DataFrame:
    """Add UTC and Asia/Kolkata timestamps from millisecond Unix timestamps."""
    result = df.copy()
    result["timestamp"] = pd.to_numeric(result["timestamp"], errors="raise").astype("int64")
    result["timestamp_utc"] = pd.to_datetime(
        result["timestamp"], unit="ms", utc=True
    )
    result["timestamp_ist"] = result["timestamp_utc"].dt.tz_convert(
        "Asia/Kolkata"
    )
    return result


def clean_tap_scroll(events: pd.DataFrame) -> pd.DataFrame:
    """Sort TAP/SCROLL events and add a TAP coordinate-availability flag.

    Missing coordinates are intentionally preserved; they are not replaced by 0.
    """
    result = add_ist_timestamps(events)
    result = result.sort_values("timestamp").reset_index(drop=True)

    result["coordinate_available"] = (
        result["event_type"].eq("TAP")
        & result["x_position"].notna()
        & result["y_position"].notna()
    ).astype("int8")
    return result


def clean_accelerometer(acc: pd.DataFrame) -> pd.DataFrame:
    """Remove exact duplicate sensor records and sort chronologically.

    Repeated timestamps alone are not treated as duplicates.
    """
    result = add_ist_timestamps(acc)
    result = result.drop_duplicates(
        subset=["timestamp", "x", "y", "z", "accuracy"]
    )
    result = result.sort_values("timestamp").reset_index(drop=True)
    return result
