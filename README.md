# LOWKEY — Zero-Trust Dynamic Behavioural Authentication Framework

LOWKEY is an Android research prototype that explores continuous behavioural authentication using interaction and motion signals. It collects selected behavioural events while monitoring is enabled, converts them into time-windowed features, and evaluates those features with an on-device machine-learning model.

> **Project status:** Under active development. LOWKEY is an academic prototype, not a production-ready security product. Its current model has not been validated across enough users and sessions to support claims of reliable identity recognition.

## Overview

The project combines three main components:

1. **Android Data Collection:** Accelerometer readings and accessibility-based tap and scroll events are collected while monitoring is enabled and the required Android permissions and services are active.
2. **Behavioural Feature Engineering and Model Development:** Collected data is cleaned and converted into 10-second windows for model training and evaluation.
3. **On-Device Inference:** The Android app loads a compact ONNX version of the current owner-baseline model and produces an anomaly score for eligible windows.

## Current Implementation

### Android Application

- Monitoring can be enabled or disabled by the user.
- Accelerometer readings are collected by the Android monitoring service.
- The accessibility service records `TAP` and `SCROLL` events in a local SQLite database.
- Behavioural records are grouped into 10-second windows.
- A window is evaluated only when it contains at least 30 accelerometer samples and at least one tap or scroll event.
- The app calculates seven behavioural features and passes them to the ONNX inference engine.
- Inference results, including anomaly score, threshold and anomaly decision, are currently written to Logcat for testing.

### Current Model B

The current Android-integrated model is an Isolation Forest trained on owner-collected behavioural windows.

It uses the following seven features in this exact order:

1. `tap_count`
2. `scroll_count`
3. `magnitude_mean`
4. `magnitude_std`
5. `magnitude_min`
6. `magnitude_max`
7. `magnitude_rms`

The training script:

- Fits a `StandardScaler` using training data only.
- Trains an `IsolationForest` with 300 estimators.
- Uses a fixed random seed for reproducibility.
- Calibrates the anomaly threshold using the 5th percentile of owner validation scores.

The current decision rule is:

- `score < threshold` means anomalous.
- `score >= threshold` means not anomalous.

The scaler parameters and anomaly threshold are stored in model metadata and loaded by the Android inference engine.

### Current Limitations

- Model B is an **owner-baseline anomaly detector**, not a supervised identity classifier.
- An anomaly score is not proof of who is holding the phone.
- Initial testing with one short, mostly-scroll friend session did not demonstrate reliable impostor detection at the current owner-calibrated threshold.
- Exploratory additional-feature and session-aggregation experiments have not established a reliable detection rule.
- More balanced, labelled sessions from the owner and multiple non-owners are required.
- Model evaluation should split by complete sessions rather than randomly splitting neighbouring windows, to reduce data leakage.
- Model inference is currently logged for evaluation. Automatic score-to-verification and protection integration remains under development.
- The protection overlay is a prototype. Its current button is not a substitute for real PIN or biometric authentication.

## Architecture

The current Android inference pipeline is:

```text
User enables monitoring
        |
        +--> Accelerometer Collector
        |       |
        |       +--> Accelerometer Records
        |
        +--> Accessibility Service
                |
                +--> TAP / SCROLL Events
                         |
                         +--> SQLite Database

Accelerometer Records + TAP/SCROLL Counts
        |
        v
BehaviourWindowProcessor
        |
        +--> 10-second Window
        +--> Minimum Sample and Activity Checks
        +--> Seven Behavioural Features
        |
        v
LowkeyOnnxEngine
        |
        +--> StandardScaler Parameters from Metadata
        +--> ONNX Isolation Forest Inference
        +--> Anomaly Score and Threshold Comparison
        |
        v
Logcat Diagnostics
(Current Integration)
```

## Repository Structure

```text
Lowkey/
|
|-- app/
|   |
|   |-- src/
|       |
|       |-- main/
|       |   |
|       |   |-- java/com/example/lowkey/
|       |   |   |
|       |   |   |-- accelerometer/
|       |   |   |   `-- Accelerometer Collection and Monitoring
|       |   |   |
|       |   |   |-- ml/
|       |   |   |   `-- Window Processing and ONNX Inference
|       |   |   |
|       |   |   |-- security/
|       |   |   |   `-- Security Manager and Protection Overlay
|       |   |   |
|       |   |   `-- tap/
|       |   |       `-- Accessibility TAP/SCROLL Collection
|       |   |
|       |   `-- assets/
|       |       `-- Local ONNX Model Assets (Not Committed)
|       |
|       `-- androidTest/
|           `-- On-Device ONNX Instrumentation Test
|
|-- train_model_b.py
|   `-- Current Model B Training Script
|
|-- convert_model_b.py
|   `-- ONNX Conversion Workflow
|
|-- convert_model_b_compact.py
|   `-- Compact ONNX Export and Verification
|
|-- score_friend_model_b.py
|   `-- Offline Friend-Session Scoring Utility
|
|-- audit_model_b_training_ranges.py
|   `-- Training Range Audit
|
|-- audit_model_b_validation_scores.py
|   `-- Validation Score Audit
|
`-- README.md
```

Local/private data directories such as `LOWKEY_DATASET/` and `LOWKEY_PROCESSED/` are excluded from the public repository.

## Requirements

- Android Studio with a compatible Android Gradle Plugin and JDK setup. The Android Studio embedded JDK is recommended.
- Android SDK installed through Android Studio.
- A physical Android device with an accelerometer for behavioural collection and on-device testing.
- Python 3 with the packages required by the data preparation, training and conversion scripts.

The Android project uses:

- Kotlin
- AndroidX
- SQLite
- ONNX Runtime for Android

The ONNX Runtime Android dependency is configured through the Gradle version catalog.

## Build and Test the Android App

1. Clone the repository.
2. Open the project in Android Studio.
3. Allow Gradle Sync to complete.
4. Provide the required local ONNX model assets.
5. Select the `app` run configuration.
6. Build and run the application on a connected Android device.

### Build on Windows PowerShell

```powershell
.\gradlew.bat assembleDebug
```

### Build on macOS, Linux or Git Bash

```bash
./gradlew assembleDebug
```

### Run Android Instrumentation Tests on Windows

Connect and authorize a physical Android device, then run:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

### Run Android Instrumentation Tests on macOS, Linux or Git Bash

```bash
./gradlew connectedDebugAndroidTest
```

The ONNX instrumentation test verifies that the model loads and that a sample inference returns a finite score with the expected threshold.

This is an on-device inference smoke test, not a complete behavioural authentication accuracy evaluation.

## Local Model Assets

The public repository intentionally does **not** include personalized model binaries or metadata.

The following files must be supplied locally for on-device inference:

```text
app/src/main/assets/
|
|-- lowkey_model_b_compact.onnx
|
`-- lowkey_model_b_onnx_metadata.json
```

These files are excluded through `.gitignore`.

To generate compatible assets:

1. Prepare an authorized behavioural dataset.
2. Generate the required feature CSV.
3. Train Model B.
4. Convert the trained model to ONNX.
5. Generate and verify the matching ONNX metadata.
6. Place both files in `app/src/main/assets/`.

Expected feature file:

```text
LOWKEY_PROCESSED/features/lowkey_ml_features.csv
```

### Training and Conversion Commands

```bash
python train_model_b.py

python convert_model_b.py

python convert_model_b_compact.py
```

Check the output paths printed by the conversion scripts.

The metadata must correspond to the exact ONNX model and contain:

- Expected seven-feature order
- StandardScaler means
- StandardScaler scales
- Anomaly threshold
- Minimum accelerometer sample count

Do not combine an ONNX model with metadata from a different training run.

> Without these local assets, the Android source can be inspected and the project may compile, but `LowkeyOnnxEngine` cannot initialise for inference. The ONNX instrumentation test also requires these assets.

## Training and Evaluation Workflow

The current training script expects a locally prepared feature CSV:

```text
LOWKEY_PROCESSED/features/lowkey_ml_features.csv
```

`train_model_b.py` currently uses a date-specific chronological split:

| Dataset | Usage |
|---|---|
| 28 October 2026 | Training |
| First 70% of 29 October 2026 | Training |
| Remaining 30% of 29 October 2026 | Validation and threshold calibration |
| 30 October 2026 | Holdout evaluation |

This split is specific to the dataset used during development.

Anyone using a different dataset must update the date ranges and verify that training, validation and holdout contain suitable non-empty sessions.

Adjacent windows from the same session should not be randomly distributed between training and evaluation sets.

The friend-scoring and audit scripts are offline analysis utilities. They require locally collected, authorized datasets and cannot be fully executed using the public repository alone.

## Privacy and Data Handling

Behavioural interaction and motion traces can be sensitive.

LOWKEY excludes the following from the public repository:

- Raw behavioural recordings
- Processed personal datasets
- SQLite database exports
- Training and evaluation CSV files
- Personalized ONNX model binaries
- Personalized model metadata

Data handling guidelines:

- Keep raw recordings and processed behavioural datasets in restricted local storage.
- Do not commit another person's behavioural data without explicit informed permission.
- Do not publish logs containing identifiable activity.
- Do not upload personalized model binaries or metadata to the public repository.
- Enable monitoring and accessibility collection only with the device user's knowledge and consent.
- Review `git status` before every commit.

The `.gitignore` file contains exclusions for local datasets, CSV/DB exports, processed data and personalized ONNX assets.

## Development Status

LOWKEY is an ongoing academic research prototype.

The current development focus includes:

- Improving the quality and balance of owner/non-owner data collection.
- Evaluating supervised identity-classification approaches against the existing owner-baseline anomaly detector.
- Investigating additional behavioural features.
- Evaluating session-level detection strategies.
- Validating proposed approaches on separate sessions and multiple participants.
- Connecting a validated detection strategy to the authentication and protection flow.

Any proposed detection strategy must be evaluated for both impostor detection and genuine-owner false rejection before integration.

## Disclaimer

LOWKEY is intended for academic research and experimentation.

Behavioural signals are probabilistic and may vary with device orientation, applications, tasks, accessibility-event availability and collection conditions.

The current prototype has not been demonstrated to reliably distinguish arbitrary users.

Do not rely on LOWKEY as the sole mechanism for protecting a device, account or sensitive information.