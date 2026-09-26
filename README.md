# MotionShield — Privacy-Preserving Human Activity Recognition

MotionShield is an end-to-end, privacy-preserving Human Activity Recognition (HAR) and safety monitoring application powered by smartphone motion sensors, a Bi-LSTM with Temporal Attention neural network, and an independent real-time fall detection safety engine.

---

## Architecture Overview

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

## Key Features

- **On-Device Inference**: Runs PyTorch TorchScript model (`bilstm_attention.pt`) directly on the Android device for low-latency, privacy-preserving activity classification without external cloud requirements.
- **Six Activity Classes**:
  1. **Walking**
  2. **Upstairs** (Walking Upstairs)
  3. **Downstairs** (Walking Downstairs)
  4. **Sitting**
  5. **Standing**
  6. **Laying**
- **Independent Fall Detection Engine**: Sensor-driven multi-stage fall detection tracking free-fall acceleration drops, high-impact spikes ($>2.5g$), and post-impact inactivity. Includes a prominent alert card with an `I'M OK` recovery button.
- **Continuous Sliding Window**: Generates predictions every 0.5 seconds using a 128-sample sliding window with a 25-sample stride at a 50 Hz target sampling rate.
- **Elderly Safety & Precautions**: Built-in safety guidelines, fall prevention tips, stair safety rules, and phone placement instructions.
- **Developer ML Diagnostics**: Live diagnostics screen displaying real-time 6-class probability vectors, raw PyTorch logits, inference latency, and window status.

---

## Model Performance (UCI-HAR Benchmark)

| Metric | Value |
|---|---|
| **Test Accuracy** | **94.57%** |
| **Balanced Accuracy** | **94.55%** |
| **Macro Precision** | **94.62%** |
| **Macro Recall** | **94.54%** |
| **Macro F1** | **94.58%** |
| **Weighted F1** | **94.57%** |

---

## Physical-Device Validation

All six UCI-HAR activity classes were exercised on a physical Android device. Static activities such as Sitting and Laying produced stable predictions, while dynamic activities such as Walking, Upstairs, and Downstairs showed more variable probability distributions due to orientation variance and natural phone handling during testing. The application exposes real-time six-class probabilities through the developer diagnostics screen.

---

## Repository Structure

- `frontend/android/` – Native Android app featuring PyTorch Mobile, Material 3 design, Room session persistence, and Navigation Component.
- `ml/` – PyTorch model architectures (`bilstm_attention.py`), loss functions, and data pipelines.
- `backend/` – Optional FastAPI inference service for remote deployment.
- `scripts/` – Data download, preprocessing, model training, quantization, and evaluation utilities.
- `reports/` – Experiment reports, confusion matrices, and metrics.
- `tests/` – Pytest suite for model and data pipelines.

---

## Getting Started

### Android App Setup
1. Open `frontend/android/` in Android Studio or build via Gradle command line:
   ```bash
   cd frontend/android
   ./gradlew clean assembleDebug
   ```
2. Install the debug APK (`app-debug.apk`) onto an Android device running Android 7.0+ (API 24+).

### Python ML Pipeline Setup
```bash
python -m venv .venv
source .venv/bin/activate  # On Windows: .venv\Scripts\activate
pip install -r requirements.txt
python scripts/evaluate.py
```

---

## Disclaimer
MotionShield is an activity-monitoring research prototype. Fall detection is an assistive sensor feature and is not a medical device or a substitute for professional emergency services.

---

## License
This project is licensed under the MIT License.
