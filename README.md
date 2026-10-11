# MotionShield

**Privacy-Preserving Smartphone Human Activity Recognition and Safety Monitoring System**

> A complete end-to-end Android application performing on-device AI inference for human activity recognition, fall detection, driver drowsiness monitoring, and wrist pulse estimation — with zero cloud dependency.

---

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [System Architecture](#system-architecture)
- [Machine Learning Model](#machine-learning-model)
- [Sensor Processing Pipeline](#sensor-processing-pipeline)
- [Activity Classes](#activity-classes)
- [Fall Detection Engine](#fall-detection-engine)
- [Driver Drowsiness Monitor](#driver-drowsiness-monitor)
- [Wrist Pulse & BP Check](#wrist-pulse--bp-check)
- [Multi-User System](#multi-user-system)
- [Elderly Safety](#elderly-safety)
- [Session & History Architecture](#session--history-architecture)
- [Model Evaluation](#model-evaluation)
- [Physical Device Validation](#physical-device-validation)
- [Repository Structure](#repository-structure)
- [Android Setup](#android-setup)
- [ML Pipeline Setup](#ml-pipeline-setup)
- [Privacy Design](#privacy-design)
- [Disclaimer](#disclaimer)

---

## Overview

MotionShield is a production-grade, privacy-first Human Activity Recognition (HAR) and personal safety platform built entirely for Android. It combines a custom-trained **Bidirectional LSTM with Temporal Attention** neural network (exported to PyTorch TorchScript) with a suite of independent sensor-based safety subsystems — all executing locally on the user's smartphone with no cloud calls, no external data transmission, and no internet dependency during operation.

The application was developed to demonstrate that modern deep learning inference, real-time signal processing, and multi-feature safety monitoring can be delivered entirely on consumer mobile hardware.

---

## Key Features

| Feature | Description |
|---|---|
| **On-Device HAR Inference** | PyTorch TorchScript model runs via C++ JNI bindings. No sensor data ever leaves the device. |
| **6-Class Activity Recognition** | Classifies Walking, Walking Upstairs, Walking Downstairs, Sitting, Standing, Laying in real time. |
| **Independent Fall Detection** | Sensor-based 3-stage state machine (free-fall → impact → inactivity). Separate from ML model. |
| **Driver Drowsiness Monitor** | Detects microsleep head-nod jerk patterns and erratic swerve via accelerometer and gyroscope. |
| **Wrist BCG Pulse Estimation** | Ballistocardiographic heart rate estimation using accelerometer micro-vibrations at 50 Hz. |
| **Multi-User Login** | 3-profile switcher with SharedPreferences persistence. No server authentication required. |
| **Emergency Helpline** | One-tap dial (9440085239) accessible from Home, Profile, Live, Elderly Safety, and all safety screens. |
| **Room Database Sessions** | Full session history: duration, dominant activity, confidence, latency, sample count. |
| **ML Diagnostics Screen** | Real-time 6-class probability vector, raw logits, inference latency, window status. |
| **Elderly Safety Guidance** | Evidence-based precautions for walking, stairs, footwear, phone placement, and rest. |
| **Activity Guide & Instructions** | Step-by-step instructions for phone placement and activity testing built into the app. |

---

## System Architecture

```
                     Smartphone Sensors
               ┌──────────────────────────┐
               │  Accelerometer           │
               │  Gyroscope               │
               └────────────┬─────────────┘
                            │
               ┌────────────▼─────────────┐
               │   Sensor Buffer          │
               │   128 samples @ 50 Hz    │
               │   25-sample stride       │
               └──────────┬──────┬────────┘
                          │      │
              ┌───────────▼──┐ ┌─▼──────────────────┐
              │  HAR Model   │ │  Safety Subsystems  │
              │  Bi-LSTM +   │ │  ┌───────────────┐  │
              │  Temporal    │ │  │ Fall Detection│  │
              │  Attention   │ │  ├───────────────┤  │
              └──────┬───────┘ │  │ Driver        │  │
                     │         │  │ Drowsiness    │  │
              ┌──────▼───────┐ │  ├───────────────┤  │
              │ 6 Activities │ │  │ Wrist BCG     │  │
              │ + Confidence │ │  │ Pulse Check   │  │
              └──────┬───────┘ │  └───────────────┘  │
                     │         └──────────┬────────────┘
              ┌──────▼──────────────────┐ │
              │   Room Database         │ │  Emergency
              │   Session History       │ │  Helpline Alert
              └─────────────────────────┘ │  (9440085239)
                                          ▼
                                    User Interface
```

---

## Android Application Flow

```
MainActivity  (Bottom Navigation + Sensor Manager)
├── HomeFragment
│     Multi-user greeting, session summary, quick-action cards
│     (Wrist Vitals, Driver Safety, Helpline, Activity Guide)
│
├── DashboardFragment  [Live]
│     Real-time activity label, confidence bar, probability waveform
│     Fall Alert card with I'M OK + Helpline buttons
│     Session start/stop controls
│
├── HistoryFragment
│     Completed session list (Room DB), swipe-to-delete, filter
│     └── HistoryDetailFragment  (session breakdown)
│
└── ProfileFragment
      User profile (Jahnavi / Multi-user switcher)
      Emergency helpline card
      Navigation hub →
        ├── ActivityGuideFragment   (How to use MotionShield)
        ├── ElderlySafetyFragment   (Safety precautions + helpline)
        ├── InsightsFragment        (Analytics from Room storage)
        ├── MLDiagnosticsFragment   (6-class probabilities, logits, latency)
        ├── WristVitalCheckFragment (BCG pulse + BP estimation)
        ├── DriverDrowsinessFragment(Microsleep & swerve detection)
        └── AboutFragment           (Technical architecture specs)
```

---

## Sensor Processing Pipeline

| Step | Detail |
|---|---|
| **Sensor Registration** | `TYPE_ACCELEROMETER` + `TYPE_GYROSCOPE` at 20,000 µs (50 Hz) |
| **Gravity Scaling** | Divide by 9.80665 m/s² to match UCI-HAR training representation |
| **Channel Order** | `[acc_x, acc_y, acc_z, gyro_x, gyro_y, gyro_z]` — 6 channels per sample |
| **Sliding Window** | 128-sample buffer (2.56 s), 25-sample stride (0.5 s update rate) |
| **Z-Score Normalization** | Per-channel mean/std from UCI-HAR training split: `x_scaled = (x − μ) / σ` |
| **Input Tensor** | Shape `[1, 128, 6]` passed to TorchScript model |
| **Softmax Output** | Raw logits → numerically stable softmax → 6 probabilities summing to 1.0 |

---

## Machine Learning Model

**Architecture:** Bidirectional LSTM with Temporal Attention

```
Input [1, 128, 6]
    ↓
Bi-LSTM  (hidden=128, 2 layers, dropout=0.3)
    ↓  (concatenated bidirectional hidden states)
Temporal Attention  (64-dim projection → softmax weights → weighted sum)
    ↓
Classification Head  (Linear → 6 classes)
    ↓
Softmax  →  Probability vector [6]
```

| Parameter | Value |
|---|---|
| Input shape | `[batch, 128, 6]` |
| LSTM hidden size | 128 per direction (256 concatenated) |
| LSTM layers | 2 |
| Attention dimension | 64 |
| Dropout | 0.3 |
| Output classes | 6 |
| Model format | PyTorch TorchScript (`.pt`) |
| Runtime | PyTorch Mobile (Android JNI) |

---

## Activity Classes

| Index | Label | Display Name |
|:---:|:---|:---|
| 0 | `WALKING` | Walking |
| 1 | `WALKING_UPSTAIRS` | Walking Upstairs |
| 2 | `WALKING_DOWNSTAIRS` | Walking Downstairs |
| 3 | `SITTING` | Sitting |
| 4 | `STANDING` | Standing |
| 5 | `LAYING` | Laying |

> Fall Detection is **not** a seventh ML class. It is a separate sensor-based safety subsystem.

---

## Fall Detection Engine

Operates independently from the 6-class HAR model using its own sensor listener:

```
State Machine:
NORMAL → IMPACT_CANDIDATE → INACTIVITY_CONFIRMATION → FALL_ALERT
```

| Stage | Trigger |
|---|---|
| Impact Detection | Acceleration magnitude > 2.5g |
| Inactivity Confirmation | Variance < 0.15 sustained for 2 seconds post-impact |
| Fall Alert UI | Full-screen warning card: "I'M OK" reset button + one-tap helpline call |

---

## Driver Drowsiness Monitor

A dedicated `DriverDrowsinessFragment` using the device accelerometer and gyroscope to detect in-vehicle microsleep events:

| Signal | Detection |
|---|---|
| **Accelerometer** | Prolonged immobility (>8–12s, deviation <0.35g from 9.8g) followed by reactive jerk (>3.8g) = microsleep head-nod |
| **Gyroscope** | Yaw rate >1.8 rad/s = erratic lane-swerve |

- **CRITICAL ALARM** card with haptic wave pattern (400-200-400-200-600ms)
- **I'M AWAKE** acknowledgment button + helpline dial
- Safe driving guidelines and fatigue risk percentage displayed live
- Sensors stop on `onPause()` — no background battery drain
- Safe simulation test button for validation without actual driving risk

---

## Wrist Pulse & BP Check

`WristVitalCheckFragment` uses Ballistocardiography (BCG) — inertial sensing of cardiac-impulse micro-vibrations through the phone's accelerometer:

**Algorithm:**
```
1. Register accelerometer at 50 Hz (20,000 µs)
2. Compute magnitude: √(x² + y² + z²)
3. High-pass filter: subtract running mean (α=0.97) to isolate BCG from gravity
4. Adaptive peak threshold: 40% of signal RMS, updated per second
5. Peak detection: slope reversal above threshold + 0.5s refractory window
6. BPM = (peak_count / 15s) × 60, clamped to [45, 160]
7. BP proxy: systolic ≈ 70 + 0.65×BPM, diastolic ≈ 40 + 0.50×BPM
```

- 15-second measurement window with countdown progress bar
- Haptic heartbeat feedback every second during measurement
- Dynamic status: Normal (green) / Bradycardia (blue) / Elevated (orange-red)
- Fitness-grade estimation — not a medical device

---

## Multi-User System

`UserManager.java` provides a lightweight 3-profile switcher:

| Profile | Role |
|---|---|
| **Jahnavi** (default) | Primary Account • Fitness & Health |
| **Senior Care** | Caregiver Account • Elderly Monitoring |
| **Guest User** | Guest Account • Temporary Access |

- Active profile persisted via `SharedPreferences`
- Switchable from Home screen avatar tap or Profile → Switch Profile button
- Greeting and subtitle update instantly on profile switch
- No database, no server, no authentication overhead

---

## Elderly Safety

Built-in safety guidance in `ElderlySafetyFragment`:

- Clear walking paths and lighting recommendations
- Stair navigation with handrail guidance
- Footwear and phone placement advice
- Rest and medical warning indicators
- Emergency helpline (9440085239) prominent at top of screen

---

## Session & History Architecture

- **ViewModel**: `LiveMotionViewModel` — centralized state surviving Fragment recreation
- **Room ORM**: `SessionEntity` table — stores timestamp, duration, dominant activity, average confidence, prediction count, average latency
- **Single-write guarantee**: Prevents duplicate session records on termination
- **History UI**: Sorted session list, swipe-to-delete, detail drill-down per session
- **Home stats**: Live session count, total active time, top activity computed from Room data

---

## Model Evaluation

Evaluated on the official UCI-HAR held-out test split (subject-level separation, zero leakage):

| Metric | Score |
|:---|:---:|
| **Accuracy** | **94.57%** |
| Balanced Accuracy | 94.55% |
| Macro Precision | 94.62% |
| Macro Recall | 94.54% |
| Macro F1 | 94.58% |
| Weighted F1 | 94.57% |

---

## Physical Device Validation

Validated on a physical Android device (`ijhamrtgde49ij45`). All six UCI-HAR activity classes were exercised via the Live session screen and ML Diagnostics screen:

| Activity | Confidence Range | Stability |
|---|---|---|
| Sitting | ~90% | Stable |
| Laying | ~61–85% | Stable |
| Standing | ~75–88% | Generally stable, occasional Walking overlap |
| Walking | ~55–80% | Variable — hand-held vs waist-mounted placement |
| Walking Upstairs | ~50–65% | Variable — stride pattern dependent |
| Walking Downstairs | ~50–70% | Variable — stride pattern dependent |

> Static activities (Sitting, Laying) produce consistently high-confidence predictions. Dynamic activities show more variable distributions due to hand-held phone orientation differences from the waist-mounted UCI-HAR benchmark collection protocol. Real-time probability vectors are observable live via the ML Diagnostics screen.

---

## Repository Structure

```
Human-Activity-Recognition/
├── frontend/
│   └── android/
│       └── app/src/main/
│           ├── assets/                   # bilstm_attention.pt, scaler_mean.json, scaler_std.json
│           ├── java/com/example/app/
│           │   ├── LiveMotionViewModel.java     # Central HAR + fall detection state
│           │   ├── SensorBuffer.java            # 128-sample sliding window
│           │   ├── Preprocess.java              # Z-score normalization
│           │   ├── InferencePhone.java          # PyTorch TorchScript JNI bridge
│           │   ├── UserManager.java             # Multi-user profile management
│           │   ├── HomeFragment.java            # Home dashboard
│           │   ├── DashboardFragment.java       # Live activity monitor
│           │   ├── ProfileFragment.java         # User profile + navigation hub
│           │   ├── HistoryFragment.java         # Session history list
│           │   ├── HistoryDetailFragment.java   # Session detail view
│           │   ├── ActivityGuideFragment.java   # Instructions & phone placement
│           │   ├── ElderlySafetyFragment.java   # Safety precautions
│           │   ├── InsightsFragment.java        # Analytics
│           │   ├── MLDiagnosticsFragment.java   # Real-time probability diagnostics
│           │   ├── WristVitalCheckFragment.java # BCG pulse estimation
│           │   ├── DriverDrowsinessFragment.java# Microsleep & swerve detection
│           │   └── AboutFragment.java           # Technical specifications
│           └── res/
│               ├── layout/                      # Fragment XML layouts
│               ├── navigation/nav_graph.xml     # Jetpack Navigation graph
│               └── drawable/                   # Vector icons (walking, stairs, etc.)
├── ml/
│   └── models/                                 # Bi-LSTM + Attention PyTorch module
├── models/
│   └── smartphone_har/                         # Trained checkpoints + metadata
├── scripts/                                    # Training, preprocessing, evaluation
├── reports/                                    # Experiment reports and metrics
├── tests/                                      # pytest suite
├── README.md
├── requirements.txt
└── .gitignore
```

---

## Android Setup

**Requirements:** Android Studio Hedgehog+ or Gradle CLI, Android device/emulator API 24+ (Android 7.0+)

```bash
# Build debug APK
cd frontend/android
./gradlew clean assembleDebug

# Install on connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Required permissions (auto-requested at runtime):**
- `BODY_SENSORS` — accelerometer/gyroscope access
- `HIGH_SAMPLING_RATE_SENSORS` — 50 Hz sensor registration
- `VIBRATE` — haptic feedback for wrist check and drowsiness alarm
- `CALL_PHONE` / `ACTION_DIAL` — emergency helpline dialing

---

## ML Pipeline Setup

```bash
# Create virtual environment
python -m venv .venv
.venv\Scripts\activate          # Windows
# source .venv/bin/activate     # macOS/Linux

# Install dependencies
pip install -r requirements.txt

# Run training pipeline
python scripts/train.py

# Evaluate on UCI-HAR test set
python scripts/evaluate.py

# Run automated test suite
python -m pytest -v
```

---

## Dataset

**UCI Human Activity Recognition Using Smartphones Dataset**

| Property | Value |
|---|---|
| Subjects | 30 volunteers, ages 19–48 |
| Activities | 6 daily activities |
| Sampling rate | 50 Hz |
| Sensors | 3-axis accelerometer + 3-axis gyroscope |
| Split | Subject-level (zero subject overlap train/test) |
| Source | [UCI ML Repository](https://archive.ics.uci.edu/ml/datasets/human+activity+recognition+using+smartphones) |

---

## Privacy Design

MotionShield performs all inference locally:

- Sensor data never transmitted to external servers
- No internet permission in AndroidManifest
- Room database stored in device-private storage
- No analytics, telemetry, or crash reporting SDKs
- User profiles stored in device SharedPreferences only

---

## Disclaimer

MotionShield is a research prototype and academic project. The wrist pulse feature uses inertial BCG sensing and is a fitness-grade estimation — not a certified medical device. Fall detection is an assistive monitoring feature — not a substitute for professional emergency medical services. Driver drowsiness detection is a supplementary safety aid — always prioritise safe driving practices over any on-screen alerts.

---

## License

MIT License — see [LICENSE](LICENSE) for details.
