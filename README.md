LOWKEY — Zero-Trust Dynamic Behavioural Authentication Framework
LOWKEY is an Android research prototype that explores continuous behavioural authentication using interaction and motion signals. It collects selected behavioural events while monitoring is enabled, converts them into time-windowed features, and evaluates those features with an on-device machine-learning model.
Project status: Under active development. LOWKEY is an academic prototype, not a production-ready security product. Its current model has not been validated across enough users and sessions to support claims of reliable identity recognition.

Overview
The project combines three main parts:
1. Android data collection — accelerometer readings and accessibility-based tap/scroll events are collected while the user has enabled monitoring and the required Android permissions/services.
2. Behavioural feature engineering and model development — collected data is cleaned and converted into 10-second windows for model training and evaluation.
3. On-device inference — the Android app loads a compact ONNX version of the current owner-baseline model and produces an anomaly score for eligible windows.
   Current implementation
   Android application
- Monitoring can be enabled or disabled by the user.
- Accelerometer readings are collected by the Android monitoring service.
- The accessibility service records TAP and SCROLL events in a local SQLite database.
- Behavioural records are grouped into 10-second windows.
- A window is evaluated only when it contains at least 30 accelerometer samples and at least one tap or scroll event.
- The app calculates seven features and passes them to the ONNX inference engine.
- Inference results (score, threshold and anomaly decision) are currently written to Logcat for testing.
  Current Model B
  The current Android-integrated model is an Isolation Forest trained on owner-collected behavioural windows. It uses these seven features, in this exact order:
1. tap_count
2. scroll_count
3. magnitude_mean
4. magnitude_std
5. magnitude_min
6. magnitude_max
7. magnitude_rms
   The training script fits a StandardScaler on training data only, trains an IsolationForest with 300 estimators, and calibrates the anomaly threshold from the 5th percentile of owner validation scores. The current decision rule is:
- score < threshold → anomalous
- score >= threshold → not anomalous
  The threshold and scaler parameters are stored in the model metadata and loaded by the Android inference engine. The threshold is model-specific; it should not be copied to a different model or dataset without recalibration.
  Current limitations
- The current Isolation Forest is an owner-baseline anomaly detector, not a supervised identity classifier. An anomaly score is not proof of who is holding the phone.
- Early testing with one short, mostly-scroll friend session did not demonstrate reliable impostor detection at the current owner-calibrated threshold.
- Exploratory additional-feature and session-aggregation experiments have not yet established a reliable detection rule.
- More balanced, labelled sessions from the owner and multiple non-owners are needed. Evaluation should split by complete sessions (not randomly split neighbouring windows) to avoid data leakage.
- Model inference is currently logged for evaluation. Automatic score-to-verification/protection integration is still under development.
- The protection overlay is a prototype; its current button is not a substitute for real PIN or biometric authentication.
  Architecture
  User enables monitoring
  |
  +--> Accelerometer collector
  |       |
  |       +--> accelerometer records
  |
  +--> Accessibility service
  |
  +--> TAP / SCROLL events --> SQLite

Accelerometer records + TAP/SCROLL counts
|
v
BehaviourWindowProcessor
|
+--> 10-second window
+--> minimum sample / activity checks
+--> seven behavioural features
|
v
LowkeyOnnxEngine
|
+--> StandardScaler parameters from metadata
+--> ONNX Isolation Forest inference
+--> anomaly score and threshold comparison
|
v
Logcat diagnostics (current integration)
Repository structure
Lowkey/
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/com/example/lowkey/
│       │   │   ├── accelerometer/    # Accelerometer collection and monitoring
│       │   │   ├── ml/               # Window processing and ONNX inference
│       │   │   ├── security/         # Security manager and protection overlay prototype
│       │   │   └── tap/              # Accessibility tap/scroll collection and storage
│       │   └── assets/               # Local model assets (not committed)
│       └── androidTest/              # On-device ONNX instrumentation test
├── LOWKEY_DATASET/                   # Local/private data; not committed
├── LOWKEY_PROCESSED/                 # Local/private features and model artifacts; not committed
├── train_model_b.py                 # Current Model B training script
├── convert_model_b.py               # ONNX conversion workflow
├── convert_model_b_compact.py       # Compact ONNX export/verification workflow
├── score_friend_model_b.py          # Offline friend-session scoring utility
└── audit_model_b_*.py               # Training/validation audit utilities
Requirements
- Android Studio with a compatible Android Gradle Plugin/JDK setup (the Android Studio embedded JDK is recommended).
- Android SDK installed through Android Studio.
- A physical Android device with an accelerometer for behavioural collection and on-device testing.
- Python 3 with the packages required by the data-preparation, training and conversion scripts.
  The Android project uses Kotlin, AndroidX, SQLite and ONNX Runtime for Android. The ONNX Runtime Android dependency is configured in the Gradle version catalog.
  Build and test the Android app
1. Clone the repository and open the project in Android Studio.
2. Allow Gradle sync to complete.
3. Provide the local ONNX model assets described in Local model assets.
4. Select the app run configuration and build/run the application.
   From a terminal, the debug build can be run with:

Windows PowerShell
.\gradlew.bat assembleDebug
macOS / Linux / Git Bash
./gradlew assembleDebug
To run the connected Android instrumentation tests, connect and authorize a physical device, then run:

Windows PowerShell
.\gradlew.bat connectedDebugAndroidTest
macOS / Linux / Git Bash
./gradlew connectedDebugAndroidTest
The ONNX instrumentation test verifies that the model can be loaded and that a sample inference returns a finite score with the threshold in the metadata. It is a smoke test, not an accuracy evaluation.
Local model assets
The repository intentionally does not include personalized model binaries or metadata. The following files are ignored by Git and must be supplied locally for on-device inference:
app/src/main/assets/lowkey_model_b_compact.onnx
app/src/main/assets/lowkey_model_b_onnx_metadata.json
To create compatible assets, prepare your own authorised dataset, generate the expected feature file, train Model B, and run the ONNX conversion workflow:
LOWKEY_PROCESSED/features/lowkey_ml_features.csv
python train_model_b.py
python convert_model_b.py
python convert_model_b_compact.py
Check the output paths printed by the conversion scripts and place the resulting ONNX file and matching metadata at the asset paths above. The metadata must correspond to the exact ONNX model and include the expected seven-feature order, scaler means/scales, anomaly threshold and minimum accelerometer sample count. Do not mix an ONNX file with metadata from another training run.

Without these local assets, the Android source can be inspected and the project may compile, but LowkeyOnnxEngine cannot initialise for inference. The instrumentation inference test also requires the assets.
Training and evaluation workflow
The current training script expects a locally prepared feature CSV at:
LOWKEY_PROCESSED/features/lowkey_ml_features.csv
train_model_b.py currently uses a date-specific chronological split:
- 28 October 2026 and the first 70% of 29 October 2026 for training;
- the remaining 30% of 29 October 2026 for validation and threshold calibration;
- 30 October 2026 for the holdout evaluation.
  This split is specific to the dataset used during development. Anyone using a different dataset must update the date ranges and verify that training, validation and holdout contain suitable non-empty sessions. Do not randomly split adjacent windows from the same session across training and test sets.

The friend-scoring and audit scripts are analysis utilities. They require locally collected, authorised datasets and are not runnable from the public repository alone.
Privacy and data handling
Behavioural interaction and motion traces can be sensitive. This repository excludes raw and processed personal datasets, SQLite databases, model-training outputs and personalised ONNX assets.
- Keep raw recordings, feature CSVs, SQLite databases and trained model artifacts in restricted local storage.
- Do not commit another person's behavioural data without explicit informed permission.
- Do not include personal datasets, logs containing identifiable activity, or personalised model binaries in public issues or pull requests.
- Monitoring and accessibility collection should be enabled only with the device user's knowledge and consent.
  The .gitignore file contains exclusions for local datasets, CSV/DB exports, processed data and the personalised ONNX assets. Review git status before every commit.
  Development status
  LOWKEY is an ongoing prototype. The current focus is improving the quality and balance of owner/non-owner data collection, evaluating supervised identity-classification approaches against the existing owner-baseline anomaly detector, and validating any proposed decision strategy on separate sessions and participants before connecting it to the protection flow.
  Disclaimer
  LOWKEY is intended for academic research and experimentation. Behavioural signals are probabilistic and can vary with device orientation, applications, tasks, accessibility-event availability and collection conditions. Do not rely on this prototype as the sole mechanism for protecting a device or account.