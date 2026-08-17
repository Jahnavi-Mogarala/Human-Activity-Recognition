# MotionShield

## Privacy‑Preserving Human Activity Recognition Using Smartphone Sensor Fusion

### Overview
MotionShield is an end‑to‑end machine‑learning system for recognizing human activities from smartphone motion sensors. It implements a unified data pipeline, a Bi‑LSTM + Temporal‑Attention model, and utilities for training, evaluation, and inference.

### Project Structure
```
.
├── backend/            # FastAPI inference service
├── frontend/           # React + Vite UI (currently placeholder)
├── ml/                 # Core ML pipeline and model definitions
├── data/               # Raw (downloaded) and processed dataset files
├── scripts/            # CLI utilities: download, prepare, train, evaluate, smoke test
├── configs/            # YAML configurations for datasets, models, experiments
├── docs/               # Documentation, model cards
├── tests/              # Unit and integration tests
├── notebooks/          # Exploratory Jupyter notebooks
├── .gitignore
├── requirements.txt
├── environment.yml
└── README.md
```

### Current Implementation Status
| Component                              | Status |
|---------------------------------------|--------|
| Dataset integration (UCI‑HAR)         | Completed |
| UCI‑HAR validation                    | Completed |
| Subject‑level train/val/test split    | Completed |
| Leakage checks (no subject overlap)  | Completed |
| Sensor window preparation (128‑step) | Completed |
| Bi‑LSTM + Temporal‑Attention model   | Completed |
| Model smoke test                      | Passed |
| Sanity training (2‑epoch)             | In progress |
| Final model training                  | Pending |
| Final test evaluation                 | Pending |
| Real‑time smartphone inference         | Planned |
| FastAPI inference service             | Implemented |
| Web application (React)               | Planned |

### Dataset Details (UCI‑HAR)
- **Subjects:** 30
- **Total windows:** 10,299
- **Activities (6):** WALKING, WALKING_UPSTAIRS, WALKING_DOWNSTAIRS, SITTING, STANDING, LAYING
- **Sensor channels (6):** `acc_x`, `acc_y`, `acc_z`, `gyro_x`, `gyro_y`, `gyro_z`
- **Sampling rate:** 50 Hz (native)
- **Window length:** 128 time steps
- **No NaN / Inf values**
- **Zero subject overlap** between splits

### Data Pipeline
1. **Download** raw data (`data/raw/`).
2. **Adapter** discovers dataset files and converts them to a canonical sensor schema.
3. **Validation** checks for missing values and correct channel order.
4. **Cleaning** removes corrupt records.
5. **Sampling‑rate handling** preserves the native rate (no forced resampling).
6. **Windowing** creates fixed‑length windows (128 steps).
7. **Normalization** fitted on training data only.
8. **Subject IDs** are retained for split generation.
9. **NPZ output** stored under `data/processed/<dataset>/`.

### Model Architecture
- **Bi‑LSTM** encoder with two layers per direction.
- **Temporal Attention** mechanism to focus on informative timesteps.
- **Input shape:** `[N, 128, 6]` (samples, time steps, channels).
- **Output:** 6 activity classes (0‑based indices).

### Training & Evaluation
- `scripts/train.py` saves checkpoints, scaler, config, label mapping, and a JSON training history.
- `scripts/evaluate.py` loads the best checkpoint, the train‑fitted scaler, and computes comprehensive metrics (accuracy, balanced accuracy, precision, recall, macro/weighted F1, per‑class scores) and visual reports.

### Setup Instructions
1. **Clone the repository**
   ```bash
   git clone https://github.com/Jahnavi-Mogarala/Human-Activity-Recognition.git
   cd Human-Activity-Recognition
   ```
2. **Create the Python environment**
   ```bash
   python -m venv .venv
   .venv\Scripts\activate
   pip install -r requirements.txt
   ```
3. **Download the UCI‑HAR dataset**
   ```bash
   python scripts/download_datasets.py --dataset UCI-HAR
   ```
4. **Prepare the dataset**
   ```bash
   python scripts/prepare_dataset.py --dataset UCI-HAR
   ```
5. **Run a quick sanity training** (2 epochs)
   ```bash
   python scripts/train.py --config configs/experiments/dev_sanity.yaml --output_dir models/experiments/dev_smoke
   ```
6. **Evaluate the trained model** (once final training is completed)
   ```bash
   python scripts/evaluate.py
   ```

### Notes
- The repository does **not** contain raw dataset files; they are downloaded via the provided script.
- Large binary artifacts (e.g., `.npz` files, model checkpoints) are kept out of version control via `.gitignore`.
- Current results are limited to the sanity run; full model performance metrics will be added after final training.

---
*This README reflects the actual state of the project as of the latest local implementation.*
