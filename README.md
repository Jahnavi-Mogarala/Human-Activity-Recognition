# MotionShield

## Privacy‑Preserving Human Activity Recognition Using Smartphone Sensor Fusion

### Overview
MotionShield is an end‑to‑end machine‑learning system for recognizing human activities from smartphone motion sensors. It implements a unified data pipeline, a Bi‑LSTM + Temporal‑Attention model, and utilities for training, evaluation, and inference. The main focus is achieving reliable activity recognition while avoiding camera-based monitoring.

### Project Structure
```
├── backend/            FastAPI inference service
├── frontend/           React + Vite UI (currently placeholder)
├── ml/                 Core ML pipeline and model definitions
├── data/               Raw (downloaded) and processed dataset files
├── scripts/            CLI utilities: download, prepare, train, evaluate, smoke test
├── configs/            YAML configurations for datasets, models, experiments
├── docs/               Documentation, model cards
├── tests/              Unit and integration tests
├── notebooks/          Jupyter notebooks
├── .gitignore
├── requirements.txt
├── environment.yml
└── README.md
```

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

  
