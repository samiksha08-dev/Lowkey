# LOWKEY – Member 2: Data Cleaning, Preprocessing and Feature Engineering

## Overview
This repository contains the source code and documentation for Member 2's contribution to the LOWKEY project.

**Project:** LOWKEY – Zero-Trust Dynamic Behavioural Authentication Framework

**Role:** Data Cleaning, Preprocessing and Feature Engineering

## Responsibilities
- Inspect and audit raw TAP, SCROLL and accelerometer data.
- Clean and prepare behavioural data.
- Preserve missing values where appropriate.
- Remove only exact duplicate accelerometer records.
- Generate 10-second window-based features.
- Merge modality features using overlapping timestamps.
- Add missingness indicators.
- Prepare an ML-ready feature dataset for Member 3.

## Files
| File | Description |
|---|---|
| `01_data_audit.ipynb` | Data audit and preprocessing notebook |
| `data_cleaning.py` | Data cleaning functions |
| `preprocessing.py` | Raw data preprocessing |
| `feature_engineering.py` | Feature extraction and merging |
| `requirements.txt` | Python dependencies |

## Requirements
Install dependencies using:

```bash
pip install -r requirements.txt
```

## Dataset Privacy
Raw and processed behavioural datasets contain personal data.

**Important:** Dataset CSV files, ZIP files and other personal behavioural data must remain in restricted storage such as Google Drive. They must not be uploaded to this GitHub repository.

## Execution
Run the scripts using the appropriate input dataset paths and restricted output directory.

Example:

```bash
python preprocessing.py --data-dir ./LOWKEY_DATA --output-dir ./LOWKEY_PROCESSED
```

Feature engineering:

```bash
python feature_engineering.py --tap-scroll ./LOWKEY_PROCESSED/cleaned_tap_scroll.csv --accelerometer ./LOWKEY_PROCESSED/cleaned_accelerometer.csv --output-dir ./LOWKEY_PROCESSED
```

## Handoff
The ML-ready feature dataset and feature dictionary are prepared for Member 3's model training, validation and evaluation.

**Member 2 contribution:** Data auditing, cleaning, preprocessing and feature engineering.
