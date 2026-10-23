# LOWKEY 🔐
### Zero-Trust Dynamic Behavioural Authentication Framework

## About the Project

LOWKEY is an Android-based security project that aims to continuously verify a user's identity by analysing behavioural patterns during normal smartphone usage.

Instead of relying only on passwords or traditional authentication methods, LOWKEY aims to identify suspicious changes in user behaviour. When potentially suspicious activity is detected, the system is designed to initiate a protective response and request PIN or biometric re-authentication.

The project combines behavioural data collection, machine learning, and Android security features to explore continuous user authentication.

## Objectives

- Collect behavioural data during smartphone usage.
- Analyse patterns associated with an authorised user.
- Develop a machine learning model for behavioural verification.
- Detect potentially suspicious behavioural changes.
- Integrate on-device behavioural analysis into an Android application.
- Provide additional authentication when suspicious activity is detected.
- Explore a protective screen-blur mechanism to improve privacy.

## Key Features

- User registration and login
- User profile and settings
- Monitoring ON/OFF control
- Tap and touch behaviour monitoring
- Swipe and scroll behaviour monitoring
- Accelerometer-based device movement monitoring
- App usage behaviour monitoring
- Behavioural data collection and storage
- Planned machine-learning-based behavioural verification
- Planned suspicious activity detection
- Planned screen-blur protection
- Planned PIN and biometric re-authentication

## Technology Stack

### Android Development
- **Android Studio** — development environment
- **Kotlin** — Android application development
- **XML** — user interface layouts

### Behavioural Data Collection
- **Android Accessibility APIs** — supported interaction and gesture-related events
- **Android Sensor API (`SensorManager`)** — accelerometer data
- **`UsageStatsManager`** — app usage information, subject to user-granted access
- **CSV / SQLite** — behavioural data storage and dataset preparation

### Machine Learning and Data Processing
- **Python** — model development and analysis
- **Pandas** — data manipulation
- **NumPy** — numerical processing
- **Scikit-learn** — machine learning and model evaluation
- **TensorFlow Lite** — planned on-device model integration

### Authentication and Security
- **Android Biometric API** — biometric authentication
- **PIN verification** — additional authentication
- **Android overlay mechanisms** — planned screen protection, subject to operating-system restrictions

## Project Workflow

The project follows these development stages:

1. Android UI development
2. User registration and login
3. Profile and settings implementation
4. Behavioural monitoring feature implementation
5. Behavioural data collection
6. Dataset organisation and preprocessing
7. Feature extraction and selection
8. Machine learning model training
9. Model evaluation and validation
10. Model conversion for on-device use
11. Machine learning integration into Android
12. Real-time behavioural analysis
13. Suspicious activity detection
14. PIN or biometric re-authentication
15. Protective response and system testing

## Behavioural Data Collection

The current phase focuses on collecting behavioural data from authorised users during normal smartphone usage.

The implemented monitoring features cover the following behavioural signals:

- **Tap and touch:** available touch-related events and measurements
- **Swipe:** supported swipe and gesture-related information
- **Scroll:** supported scrolling events and patterns
- **Accelerometer:** device movement measurements along the X, Y, and Z axes
- **App usage:** application usage patterns, subject to Android permissions and API limitations

The collected data will be organised into a structured dataset for cleaning, preprocessing, and feature extraction. This dataset will be used in subsequent machine learning experiments.

The availability of individual measurements depends on the Android APIs, permissions, device capabilities, and operating-system restrictions.

## Current Status

The initial Android UI, registration and login, profile/settings, and **all planned behavioural monitoring features have been implemented**.

The project is currently in the **data collection phase**. The focus is on collecting behavioural data, checking its quality, and preparing a usable dataset for machine learning.

### Upcoming Work

- Collect representative behavioural data from authorised users.
- Verify that the collected data is complete, consistent, and correctly labelled.
- Organise and combine the collected data into a structured dataset.
- Clean the data and handle missing or invalid values.
- Extract relevant features for behavioural analysis.
- Train and evaluate suitable machine learning models.
- Integrate the selected model into the Android application.
- Test suspicious behaviour detection and authentication responses.
- Evaluate performance, false alarms, and usability.

## Team Responsibilities

The project is being developed by a team of four members. Responsibilities are distributed across behavioural monitoring and data collection, dataset preparation and machine learning, Android integration, and authentication/security.

The team will combine the collected data and implementation components as the project progresses.

## Limitations

- Behavioural monitoring capabilities vary according to Android permissions and API restrictions.
- Cross-application touch and gesture data may not be available in the same detail as interactions within the application's own UI.
- App usage monitoring requires the appropriate user-granted access.
- Behavioural models may produce false positives or false negatives.
- Screen overlays and protective responses are subject to Android security and operating-system restrictions.
- The accuracy and reliability of the authentication framework must be established through testing.

## Future Scope

- Improve behavioural verification accuracy.
- Reduce false positives and false negatives.
- Optimise model inference and battery consumption.
- Expand evaluation across different users and Android devices.
- Improve the responsiveness of suspicious activity detection.
- Strengthen privacy-conscious data storage and processing.
- Evaluate the complete authentication and protection workflow.

## Disclaimer

LOWKEY is an academic project under development. Its behavioural authentication accuracy, security effectiveness, and compatibility with other applications require further testing and validation. It should not be considered a production-ready security solution until these aspects have been thoroughly evaluated.
