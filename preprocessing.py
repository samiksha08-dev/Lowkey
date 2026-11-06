"""Run LOWKEY Member 2 raw-data cleaning.

Expected inputs in --data-dir:
  tap_scroll_events.csv
  accelerometer_data.csv

Outputs are written to --output-dir. Keep all personal behavioural data in
restricted storage; do not commit CSV outputs to GitHub.
"""
import argparse
from pathlib import Path
import pandas as pd
from data_cleaning import clean_tap_scroll, clean_accelerometer


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data-dir", required=True, help="Folder containing the two raw CSV files")
    parser.add_argument("--output-dir", required=True, help="Restricted folder for cleaned CSV outputs")
    args = parser.parse_args()

    data_dir, output_dir = Path(args.data_dir), Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    tap_path = data_dir / "tap_scroll_events.csv"
    acc_path = data_dir / "accelerometer_data.csv"
    if not tap_path.exists() or not acc_path.exists():
        raise FileNotFoundError("Expected tap_scroll_events.csv and accelerometer_data.csv in --data-dir")

    tap = clean_tap_scroll(pd.read_csv(tap_path))
    acc = clean_accelerometer(pd.read_csv(acc_path))

    # Flag extreme scroll values for review; do not silently delete them.
    if "scroll_x" in tap.columns:
        tap["scroll_x_anomaly"] = tap["scroll_x"].abs().gt(10000)
    if "scroll_delta_x" in tap.columns:
        tap["scroll_delta_x_anomaly"] = tap["scroll_delta_x"].abs().gt(10000)
    if "scroll_delta_y" in tap.columns:
        tap["scroll_delta_y_anomaly"] = tap["scroll_delta_y"].le(-5000)

    tap.to_csv(output_dir / "cleaned_tap_scroll.csv", index=False)
    acc.to_csv(output_dir / "cleaned_accelerometer.csv", index=False)
    print("Cleaning complete. Outputs saved in:", output_dir)


if __name__ == "__main__":
    main()
