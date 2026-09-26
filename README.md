# MotionShield

Privacy-Preserving Smartphone Human Activity Recognition and Safety Monitoring System.

---

## Overview
MotionShield is an end-to-end, privacy-preserving Human Activity Recognition (HAR) and safety monitoring application. It processes real-time smartphone accelerometer and gyroscope data using an on-device PyTorch model combining a Bidirectional LSTM (Bi-LSTM) with Temporal Attention, paired with an independent multi-stage fall detection safety subsystem.

---
## ANDROID APP DEMO 


[![Demo Video] (<img width="387" height="812" alt="image" src="https://github.com/user-attachments/assets/4fe66d62-01a4-4940-b9c7-baeeea0e2dcb" />
)] (https://drive.google.com/file/d/1JZvviVcmxFAGQ7o4-roClT8AoN5CRq6Q/view?usp=sharing)

> **Note:** Click on the image video will play. The video above demonstrates MotionShield App workflow in real-time.

## Problem Statement
Traditional human activity recognition systems often rely on cloud-based processing, raising privacy concerns regarding continuous sensor streaming. Furthermore, static and dynamic activity classification requires low-latency, temporal context modeling that operates efficiently within mobile resource constraints.

---

## Solution
MotionShield executes local, on-device inference using PyTorch Mobile (TorchScript). Sensor streams are buffered locally, preprocessed with training-matched feature scaling, and evaluated in continuous sliding windows. All inference, session tracking, and safety monitoring occur directly on the device.

---

## Key Features
- **On-Device Local Inference**: Executes PyTorch TorchScript model (`bilstm_attention.pt`) directly on Android via C++ JNI bindings without sending sensor data to external servers.
- **Six-Class Activity Recognition**: Classifies Walking, Walking Upstairs, Walking Downstairs, Sitting, Standing, and Laying.
- **Independent Fall Detection Engine**: Multi-stage safety subsystem monitoring free-fall acceleration drops, impact acceleration spikes ($>2.5g$), and post-impact low variance ($<0.15$). Displays an interactive `I'M OK` recovery alert.
- **Continuous Sliding Window**: Evaluates 128-sample temporal windows with a 25-sample stride (0.5s update rate) at a 50 Hz target sampling frequency.
- **Room Database Session Persistence**: Tracks duration, dominant activity, average confidence, sample counts, and inference latency using Android Room ORM.
- **Elderly Safety & Guidance**: Integrated safety precautions covering safe walking, stair navigation, footwear recommendations, and phone placement.
- **Developer ML Diagnostics**: Dedicated screen displaying real-time 6-class probability distributions, raw model logits, inference latency, and window status.

---

## System Architecture

```text
                 Smartphone Sensors
              ┌─────────────────────┐
              │ Accelerometer       │
              │ Gyroscope           │
              └──────────┬──────────┘
                         │
              ┌──────────▼──────────┐
              │ Sensor Buffer       │
              │ 128 samples         │
              │ 25-sample stride    │
              └───────┬───────┬─────┘
                      │       │
             ┌────────▼───┐ ┌─▼──────────────┐
             │ HAR Model  │ │ Fall Detection │
             │ Bi-LSTM +  │ │ Safety Logic   │
             │ Attention  │ │                │
             └──────┬─────┘ └───────┬────────┘
                    │               │
             ┌──────▼──────┐   ┌────▼─────────┐
             │ 6 Activities│   │ Fall Alert   │
             │             │   │ + I'm OK     │
             └─────────────┘   └──────────────┘
```

---

## Android Application Flow

```text
SplashActivity
   │
   ▼
MainActivity (Sensor Manager & Bottom Navigation)
   ├── HomeFragment (Dashboard Overview & Safety Status)
   ├── DashboardFragment (Live Activity Monitor, Controls, Waveform & Fall Alerts)
   ├── HistoryFragment (Saved Room Sessions List & Filters)
   └── ProfileFragment (User Profile & Navigation Hub)
         ├── ActivityGuideFragment (Testing Instructions & Phone Placement)
         ├── ElderlySafetyFragment (Safety Precautions & Guidelines)
         ├── InsightsFragment (Analytics Derived from Room Storage)
         ├── MLDiagnosticsFragment (Developer Probability Vector Diagnostic)
         └── AboutFragment (Architecture & Technical Specs)
```

---

## Sensor Processing Pipeline

1. **Sensor Registration**: Registers Android `Sensor.TYPE_ACCELEROMETER` and `Sensor.TYPE_GYROSCOPE` at a target rate of 20,000 $\mu$s (50 Hz).
2. **Standard Gravity Conversion**: Linear and gravitational acceleration components from `TYPE_ACCELEROMETER` are divided by $9.80665\,\text{m/s}^2$ to match the standard gravity ($g$) representation of the training dataset.
3. **Channel Vector**: Each temporal sample consists of 6 ordered channels:
   $$\text{sample} = [\text{acc}_x, \text{acc}_y, \text{acc}_z, \text{gyro}_x, \text{gyro}_y, \text{gyro}_z]$$
4. **Sliding Window Buffer**: Accumulates 128 samples ($2.56\,\text{seconds}$). Once filled, a stride of 25 samples ($0.5\,\text{seconds}$) slides the window forward continuously.
5. **Feature Scaling**: Normalizes each channel using mean and standard deviation vectors computed from the UCI-HAR training split:
   $$x_{\text{scaled}} = \frac{x - \mu}{\sigma}$$
6. **Input Tensor**: Flattens scaled window to shape `[1, 128, 6]` for model input.

---

## Machine Learning Model

- **Architecture**: Bidirectional Long Short-Term Memory with Temporal Attention (Bi-LSTM + Temporal Attention).
- **Sequence Length**: 128 temporal steps.
- **Feature Channels**: 6 input features.
- **Hidden Size**: 128 units per directional LSTM (256 concatenated bi-directional state).
- **Layers**: 2 recurrent layers.
- **Attention Dimension**: 64-dimensional linear projection computing normalized temporal attention weights.
- **Dropout**: 0.3 rate applied between layers.
- **Output Layer**: 6-class linear classification head.
- **Inference Runtime**: PyTorch Mobile TorchScript (`bilstm_attention.pt`).

---

## Activity Classes

The ML classifier outputs predictions for six activities:

| Index | Label | User-Facing Name |
|:---:|:---|:---|
| 0 | `WALKING` | Walking |
| 1 | `WALKING_UPSTAIRS` | Walking Upstairs |
| 2 | `WALKING_DOWNSTAIRS` | Walking Downstairs |
| 3 | `SITTING` | Sitting |
| 4 | `STANDING` | Standing |
| 5 | `LAYING` | Laying |

---

## Fall Detection Safety Engine

The fall detection subsystem operates independently from the 6-class ML classifier:

- **Impact Detection**: Monitors magnitude of linear acceleration vectors ($\sqrt{x^2 + y^2 + z^2} > 2.5g$).
- **Post-Impact Inactivity**: Evaluates low acceleration variance ($<0.15$) over a 2-second temporal window following impact.
- **State Machine**: Transitions through `NORMAL` $\rightarrow$ `IMPACT_CANDIDATE` $\rightarrow$ `INACTIVITY_CONFIRMATION` $\rightarrow$ `FALL_ALERT`.
- **UI Interface**: Renders a warning card on the Live screen with an interactive `I'M OK` button that resets the state machine upon user input.

---

## Elderly Safety & Precautions

The application includes safety guidance:

- **Safe Walking**: Maintain clear, well-lit walking paths; wear stable footwear.
- **Stair Safety**: Use handrails; move at a steady, comfortable pace.
- **Phone Placement**: Keep the smartphone securely positioned in a pocket or waistband to minimize erratic displacement.
- **Rest & Health**: Discontinue activity monitoring if experiencing dizziness, weakness, or discomfort.

---

## Session & History Architecture

- **State Management**: Centralized in `LiveMotionViewModel`, surviving Fragment view recreations during navigation.
- **Persistence**: Android Room ORM storing completed sessions (`SessionEntity` table) with timestamp, duration, dominant activity, average confidence, prediction count, and average latency.
- **Data Integrity**: Enforces a single database insertion per completed session to prevent duplicate records upon session termination.

---

## Physical Device Validation

Physical-device validation was performed on an Android smartphone (`ijhamrtgde49ij45`). All six UCI-HAR activity classes were exercised. Static activities such as Sitting (~90.0% confidence) and Laying (~61.4% confidence) produced stable predictions, while dynamic activities such as Walking, Upstairs (~56.6%), and Downstairs showed more variable probability distributions due to hand-held phone placement, orientation variance, and physical motion patterns differing from waist-mounted benchmark collection. Real-time probability vectors were verified via the developer diagnostics screen.

---

## Dataset (UCI-HAR Benchmark)

- **Source**: UCI Human Activity Recognition Using Smartphones Dataset.
- **Subjects**: 30 volunteers aged 19–48 years.
- **Activities**: 6 standard daily activities.
- **Sensor Data**: 3-axial accelerometer and 3-axial gyroscope at 50 Hz.
- **Splits**: Subject-level separation ensuring zero subject overlap between training, validation, and test sets.

---

## Model Evaluation (Test-Set Metrics)

Evaluation on the official UCI-HAR test dataset:

| Metric | Score |
|:---|:---:|
| **Accuracy** | **94.57%** |
| **Balanced Accuracy** | **94.55%** |
| **Macro Precision** | **94.62%** |
| **Macro Recall** | **94.54%** |
| **Macro F1** | **94.58%** |
| **Weighted F1** | **94.57%** |

*Note: Benchmark test metrics reflect fixed dataset evaluation and are distinct from real-world physical device validation.*

---

## Repository Structure

```text
Human-Activity-Recognition/
├── frontend/
│   └── android/
│       ├── app/
│       │   ├── src/main/assets/       # TorchScript model & scaler
│       │   ├── src/main/java/         # Android application source
│       │   └── src/main/res/          # Layouts, graphics, navigation
│       └── build.gradle
├── ml/
│   └── models/                        # Bi-LSTM + Attention PyTorch module definitions
├── models/
│   └── smartphone_har/                # Trained checkpoints and model metadata
├── backend/                           # Optional FastAPI remote inference service
├── scripts/                           # Training, data prep, and evaluation scripts
├── reports/                           # Experiment reports and metrics
├── tests/                             # Automated pytest suite
├── README.md
├── requirements.txt
└── .gitignore
```

---

## Android Setup

1. Open `frontend/android` in Android Studio or build via Gradle command line:
   ```bash
   cd frontend/android
   ./gradlew clean assembleDebug
   ```
2. The compiled debug APK will be generated at:
   `frontend/android/app/build/outputs/apk/debug/app-debug.apk`
3. Install onto a physical Android device or emulator running Android 7.0+ (API 24+):
   ```bash
   adb install -r frontend/android/app/build/outputs/apk/debug/app-debug.apk
   ```

---

## ML Pipeline Setup

```bash
# Create and activate virtual environment
python -m venv .venv
source .venv/bin/activate  # On Windows: .venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Run Python automated test suite
python -m pytest
```

---

## Testing

Run the automated test suite covering model forward passes, preprocessing, evaluation scripts, and FastAPI endpoints:

```bash
python -m pytest -v
```

---

## Privacy Design

MotionShield performs all sensor sampling, preprocessing, TorchScript neural network inference, and database storage locally on the user's Android device. Sensor data is not transmitted to external network endpoints during normal operation.

---

## Disclaimer

MotionShield is an activity-monitoring research prototype. Fall detection is an assistive sensor feature and is not a medical device or a substitute for professional emergency medical services.

---

## License

This project is licensed under the MIT License.
