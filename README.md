# MotionShield

## Overview
MotionShield is a privacy‑preserving Human Activity Recognition (HAR) system that uses smartphone accelerometer and gyroscope data. It employs a Bi‑LSTM with Temporal Attention to classify six activities.

## Verified Results
| Metric | Value |
|--------|-------|
| Test Accuracy | **94.57%** |
| Balanced Accuracy | **94.55%** |
| Macro Precision | **94.62%** |
| Macro Recall | **94.54%** |
| Macro F1 | **94.58%** |
| Weighted Precision | **94.59%** |
| Weighted Recall | **94.57%** |
| Weighted F1 | **94.57%** |
| Checkpoint | `best_checkpoint.pth` |

*All numbers are from the official evaluation (no fabrication).*

## Repository Layout
- `backend/` – FastAPI service for remote inference.
- `frontend/android/` – Android app for on‑device inference (APK built at `frontend/android/app/build/outputs/apk/debug/app-debug.apk`).
- `frontend/react/` – (Planned) React front‑end.
- `ml/` – Model definition (`ml/models/bilstm_attention.py`) and pipeline utilities.
- `scripts/` – Data download, preparation, training, evaluation, and validation scripts.
- `reports/` – Experiment reports, confusion matrix, performance, quantisation, and audit.
- `tests/` – Pytest suite (8 / 8 passed).

## Installation
```bash
git clone https://github.com/Jahnavi-Mogarala/Human-Activity-Recognition.git
cd Human-Activity-Recognition
python -m venv .venv
.venv\\Scripts\\activate
pip install -r requirements.txt
```

## Data Preparation
```bash
python scripts/download_datasets.py --dataset UCI-HAR
python scripts/prepare_dataset.py --dataset UCI-HAR
```

## Evaluation
```bash
har-torch-py311\\Scripts\\python.exe -m pytest -q   # confirm test suite passes
python scripts/evaluate.py                     # prints accuracy, macro F1, etc.
```

## Android Inference
The Android app loads the TorchScript model (`bilstm_attention.pt`) and scaler (`scaler.pkl`) from `frontend/android/app/src/main/assets/`. It processes a 128‑step window of the six sensor channels and outputs the predicted activity.

## Aim
The aim of MotionShield is to build a reliable smartphone-based activity recognition system using motion sensor data.

The project focuses on:
- Combining accelerometer and gyroscope data
- Processing sensor data in fixed time windows
- Keeping subjects separate during training and testing
- Avoiding data leakage
- Using a Bi-LSTM with Temporal Attention for activity classification
- Preparing the model for future smartphone-based inference

## Dataset (UCI-HAR)
| Property | Details |
|---|---|
| Subjects | 30 |
| Activities | 6 |
| Sensor channels | 6 |
| Sampling rate | 50 Hz |
| Window size | 128 samples |
| Total windows | 10,299 |

### Activities
- WALKING
- WALKING_UPSTAIRS
- WALKING_DOWNSTAIRS
- SITTING
- STANDING
- LAYING

### Sensor Channels
```text
acc_x, acc_y, acc_z, gyro_x, gyro_y, gyro_z
```

### Block Diagram
<img width="1188" height="792" alt="BLOCK  DIAGRAM" src="https://github.com/user-attachments/assets/0134db92-08fe-4d9a-818d-28450806d79a" />

## Current Progress
| Component                              | Status |
|---------------------------------------|--------|
| Dataset integration (UCI‑HAR)         | Completed |
| UCI‑HAR validation                    | Completed |
| Subject‑level train/val/test split    | Completed |
| Leakage checks (no subject overlap)  | Completed |
| Sensor window preparation (128‑step) | Completed |
| Bi‑LSTM + Attention algorithm         | Completed |
| Basic functionality test               | Passed |
| Quick sanity training (2‑epoch)       | Completed |
| Full training run                     | Completed |
| Final evaluation on test set           | Completed |
| Real‑time inference on the phone       | In Progress |
| FastAPI remote service                 | Implemented |
| Web front‑end (React)                  | Planned |

## License
This project is licensed under the MIT License.
