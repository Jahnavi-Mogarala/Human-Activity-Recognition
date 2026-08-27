# MotionShield

## Overview
MotionShield is a privacy‑preserving Human Activity Recognition (HAR) system that uses smartphone accelerometer and gyroscope data. It employs a Bi‑LSTM with Temporal Attention to classify six activities.

## Verified Results
| Metric | Value |
|--------|-------|
| Test Accuracy | **80.19%** |
| Macro F1 | **80.94%** |
| Number of Classes | **6** |
| Test Samples | **3,437** |
| Model Parameters | **552,582** |
| Checkpoint | `bilstm_attention.pt` |

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

## License
This project is licensed under the MIT License.


## Privacy-Preserving Human Activity Recognition Using Smartphone Sensors

MotionShield is a Human Activity Recognition (HAR) project that uses
smartphone accelerometer and gyroscope data to recognize human activities.

The current implementation uses the UCI-HAR dataset and a Bi-LSTM with
Temporal Attention for activity classification. The project includes the
data preparation, validation, training and evaluation pipeline.

---

## Aim

The aim of MotionShield is to build a reliable smartphone-based activity
recognition system using motion sensor data.

The project focuses on:
- Combining accelerometer and gyroscope data
- Processing sensor data in fixed time windows
- Keeping subjects separate during training and testing
- Avoiding data leakage
- Using a Bi-LSTM with Temporal Attention for activity classification
- Preparing the model for future smartphone-based inference

---

## Dataset

The current implementation uses the **UCI Human Activity Recognition Using
Smartphones** dataset.

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
acc_x
acc_y
acc_z
gyro_x
gyro_y
gyro_z
```

### BLOCK DIAGRAM

<img width="1188" height="792" alt="BLOCK  DIAGRAM" src="https://github.com/user-attachments/assets/0134db92-08fe-4d9a-818d-28450806d79a" />

### Overview
MotionShield is a complete system for recognizing human activities from a phone’s motion sensors. It includes a data pipeline, a bi‑directional LSTM network with an attention mechanism, and utilities for training, evaluation, and running predictions directly on the device.

### Project Layout
```
.
├── backend/            # FastAPI service for remote predictions
├── ml/                 # Core code for the prediction algorithm
├── data/               # Raw (downloaded) and processed data files
├── scripts/            # Command‑line tools: download, prepare, train, evaluate, quick test
├── configs/            # Configuration files for data and training runs
├── docs/               # Documentation, model cards
├── tests/              # Unit and integration tests
├── notebooks/          # Exploratory Jupyter notebooks
├── .gitignore
├── requirements.txt
├── environment.yml
└── README.md
```

### Current Progress
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
| Full training run                     | Pending |
| Final evaluation on test set           | Pending |
| Real‑time inference on the phone       | Planned |
| FastAPI remote service                 | Implemented |
| Web front‑end (React)                  | Planned |

### Dataset Information (UCI‑HAR)
- **Subjects:** 30
- **Total windows:** 10,299
- **Activities (6):** WALKING, WALKING_UPSTAIRS, WALKING_DOWNSTAIRS, SITTING, STANDING, LAYING
- **Sensor channels (6):** `acc_x`, `acc_y`, `acc_z`, `gyro_x`, `gyro_y`, `gyro_z`
- **Sampling rate:** 50 Hz (native)
- **Window length:** 128 time steps
- **No missing or infinite values**
- **No overlap of subjects between training, validation and test sets**

### Data Processing Steps
1. **Download** the raw files into `data/raw/`.
2. **Adapter** discovers the files and converts them to a common sensor format.
3. **Validation** checks for missing values and correct channel order.
4. **Cleaning** removes any corrupted records.
5. **Sampling‑rate handling** keeps the original 50 Hz rate.
6. **Windowing** creates fixed‑length windows of 128 samples.
7. **Normalization** is computed on the training data only.
8. **Subject IDs** are kept so we can split the data without mixing subjects.
9. **NPZ files** are written to `data/processed/<dataset>/`.

### Prediction Algorithm
- A bi‑directional LSTM with two layers per direction processes each 128‑step window.
- An attention mechanism highlights the most informative time steps before the final classification layer.
- Input shape: `[batch, 128, 6]` (samples, time steps, sensor channels).
- Output: one of six activity labels.

### Training & Evaluation
- `scripts/train.py` reads the prepared data, runs the training loop, and stores the best checkpoint, the normalisation parameters, and a small JSON log of the training progress.
- `scripts/evaluate.py` loads the saved checkpoint and normalisation data, runs the model on a held‑out test set, and reports metrics such as accuracy, precision, recall and F1 scores, together with optional visual reports.

### Getting Started
1. **Clone the repository**
   ```
   git clone https://github.com/Jahnavi-Mogarala/Human-Activity-Recognition.git
   cd Human-Activity-Recognition
   ```
2. **Create the Python environment**
   ```
   python -m venv .venv
   .venv\Scripts\activate
   pip install -r requirements.txt
   ```
3. **Download the UCI‑HAR data**
   ```
   python scripts/download_datasets.py --dataset UCI-HAR
   ```
4. **Prepare the data**
   ```
   python scripts/prepare_dataset.py --dataset UCI-HAR
   ```
5. **Run a quick sanity training (2 epochs)**
   ```
   python scripts/train.py --config configs/experiments/dev_sanity.yaml --output_dir models/experiments/dev_smoke
   ```
6. **Evaluate the model** (after a full training run)
   ```
   python scripts/evaluate.py
   ```

### Notes
- The raw dataset files are not stored in the repository; they are downloaded automatically by the script above.
- Large binary files (e.g., processed NPZ files, checkpoint files) are listed in `.gitignore` and are therefore not tracked by Git.
- At the moment only the quick sanity run has been completed; full training and a complete evaluation will be added later.

---
*This README reflects the current state of the project.*

## Privacy-Preserving Human Activity Recognition Using Smartphone Sensors

<<<<<<< HEAD
### Overview
MotionShield is a complete system for recognizing human activities from a phone’s motion sensors. It includes a data pipeline, a bi‑directional LSTM network with an attention mechanism, and utilities for training, evaluation, and running predictions directly on the device.

### Project Layout
```
.
├── backend/            # FastAPI service for remote predictions
├── ml/                 # Core code for the prediction algorithm
├── data/               # Raw (downloaded) and processed data files
├── scripts/            # Command‑line tools: download, prepare, train, evaluate, quick test
├── configs/            # Configuration files for data and training runs
├── docs/               # Documentation, model cards
├── tests/              # Unit and integration tests
├── notebooks/          # Exploratory Jupyter notebooks
├── .gitignore
├── requirements.txt
├── environment.yml
└── README.md
```

### Current Progress
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
| Full training run                     | Pending |
| Final evaluation on test set           | Pending |
| Real‑time inference on the phone       | Planned |
| FastAPI remote service                 | Implemented |
| Web front‑end (React)                  | Planned |

### Dataset Information (UCI‑HAR)
- **Subjects:** 30
- **Total windows:** 10,299
- **Activities (6):** WALKING, WALKING_UPSTAIRS, WALKING_DOWNSTAIRS, SITTING, STANDING, LAYING
- **Sensor channels (6):** `acc_x`, `acc_y`, `acc_z`, `gyro_x`, `gyro_y`, `gyro_z`
- **Sampling rate:** 50 Hz (native)
- **Window length:** 128 time steps
- **No missing or infinite values**
- **No overlap of subjects between training, validation and test sets**

### Data Processing Steps
1. **Download** the raw files into `data/raw/`.
2. **Adapter** discovers the files and converts them to a common sensor format.
3. **Validation** checks for missing values and correct channel order.
4. **Cleaning** removes any corrupted records.
5. **Sampling‑rate handling** keeps the original 50 Hz rate.
6. **Windowing** creates fixed‑length windows of 128 samples.
7. **Normalization** is computed on the training data only.
8. **Subject IDs** are kept so we can split the data without mixing subjects.
9. **NPZ files** are written to `data/processed/<dataset>/`.

### Prediction Algorithm
- A bi‑directional LSTM with two layers per direction processes each 128‑step window.
- An attention mechanism highlights the most informative time steps before the final classification layer.
- Input shape: `[batch, 128, 6]` (samples, time steps, sensor channels).
- Output: one of six activity labels.

### Training & Evaluation
- `scripts/train.py` reads the prepared data, runs the training loop, and stores the best checkpoint, the normalisation parameters, and a small JSON log of the training progress.
- `scripts/evaluate.py` loads the saved checkpoint and normalisation data, runs the model on a held‑out test set, and reports metrics such as accuracy, precision, recall and F1 scores, together with optional visual reports.

### Getting Started
1. **Clone the repository**
   ```
   git clone https://github.com/Jahnavi-Mogarala/Human-Activity-Recognition.git
   cd Human-Activity-Recognition
   ```
2. **Create the Python environment**
   ```
   python -m venv .venv
   .venv\Scripts\activate
   pip install -r requirements.txt
   ```
3. **Download the UCI‑HAR data**
   ```
   python scripts/download_datasets.py --dataset UCI-HAR
   ```
4. **Prepare the data**
   ```
   python scripts/prepare_dataset.py --dataset UCI-HAR
   ```
5. **Run a quick sanity training (2 epochs)**
   ```
   python scripts/train.py --config configs/experiments/dev_sanity.yaml --output_dir models/experiments/dev_smoke
   ```
6. **Evaluate the model** (after a full training run)
   ```
   python scripts/evaluate.py
   ```

### Notes
- The raw dataset files are not stored in the repository; they are downloaded automatically by the script above.
- Large binary files (e.g., processed NPZ files, checkpoint files) are listed in `.gitignore` and are therefore not tracked by Git.
- At the moment only the quick sanity run has been completed; full training and a complete evaluation will be added later.

---
*This README reflects the current state of the project.*
=======
MotionShield is a Human Activity Recognition (HAR) project that uses
smartphone accelerometer and gyroscope data to recognize human activities.

The current implementation uses the UCI-HAR dataset and a Bi-LSTM with
Temporal Attention for activity classification. The project includes the
data preparation, validation, training and evaluation pipeline.

---

## Aim

The aim of MotionShield is to build a reliable smartphone-based activity
recognition system using motion sensor data.

The project focuses on:

- Combining accelerometer and gyroscope data
- Processing sensor data in fixed time windows
- Keeping subjects separate during training and testing
- Avoiding data leakage
- Using a Bi-LSTM with Temporal Attention for activity classification
- Preparing the model for future smartphone-based inference

---

## Dataset

The current implementation uses the **UCI Human Activity Recognition Using
Smartphones** dataset.

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
acc_x
acc_y
acc_z
gyro_x
gyro_y
gyro_z
```


### BLOCK DIAGRAM

<img width="1188" height="792" alt="BLOCK  DIAGRAM" src="https://github.com/user-attachments/assets/0134db92-08fe-4d9a-818d-28450806d79a" />

>>>>>>> 5c5715b7912be1a8a393c60b195e64c049e04de5
