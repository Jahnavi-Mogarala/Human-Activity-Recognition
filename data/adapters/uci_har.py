#!/usr/bin/env python
"""UCI‑HAR dataset adapter.

Provides a memory‑efficient conversion of the raw UCI‑HAR files into a
:pandas:`DataFrame` that follows the project's canonical sensor schema.
The adapter implements three public methods required by the data pipeline:

* ``load()`` – returns a DataFrame with the canonical columns.
* ``validate(df)`` – runs basic sanity checks and returns a report dict.
* ``metadata(df)`` – extracts dataset‑level metadata for storage.

Missing sensor modalities (magnetometer, heart‑rate) are represented as
``None``. Windowing and normalization are performed later in the
pipeline, after subject‑level splits.
"""

import pandas as pd
import numpy as np
from pathlib import Path

# Canonical column order – ensures consistent DataFrame layout across adapters
CANONICAL_COLUMNS = [
    "timestamp",
    "subject_id",
    "session_id",
    "dataset",
    "activity",
    "device_type",
    "sensor_location",
    "native_sampling_rate",
    "acc_x",
    "acc_y",
    "acc_z",
    "gyro_x",
    "gyro_y",
    "gyro_z",
    "mag_x",
    "mag_y",
    "mag_z",
    "heart_rate",
]

RAW_ROOT = Path("data/raw/UCI-HAR")
EXTRACTED_ROOT = RAW_ROOT / "UCI HAR Dataset"

class UCI_HARAdapter:
    """Adapter for the UCI‑HAR dataset.

    The original dataset provides pre‑windowed inertial signals (accelerometer
    and gyroscope) sampled at 50 Hz. Each split (train / test) contains six
    ``Inertial Signals`` files with shape ``(n_windows, 128)``. This adapter
    reads those files, constructs a DataFrame where each row represents a
    single window, and fills the canonical columns.
    """

    @staticmethod
    def _load_split(split: str) -> pd.DataFrame:
        """Load a split (``train`` or ``test``) and return a DataFrame.

        Parameters
        ----------
        split: str
            Either ``"train"`` or ``"test"``.
        """
        split_dir = EXTRACTED_ROOT / split
        # Load activity labels and subject IDs (one label per window)
        y_path = split_dir / f"y_{split}.txt"
        subject_path = split_dir / f"subject_{split}.txt"
        activities = np.loadtxt(y_path, dtype=int)
        subjects = np.loadtxt(subject_path, dtype=int)

        # Load six inertial signal files (accelerometer + gyroscope)
        signals_dir = split_dir / "Inertial Signals"
        acc_x = np.loadtxt(signals_dir / f"body_acc_x_{split}.txt")
        acc_y = np.loadtxt(signals_dir / f"body_acc_y_{split}.txt")
        acc_z = np.loadtxt(signals_dir / f"body_acc_z_{split}.txt")
        gyro_x = np.loadtxt(signals_dir / f"body_gyro_x_{split}.txt")
        gyro_y = np.loadtxt(signals_dir / f"body_gyro_y_{split}.txt")
        gyro_z = np.loadtxt(signals_dir / f"body_gyro_z_{split}.txt")

        # Stack into (n_windows, 128, 6)
        windows = np.stack([acc_x, acc_y, acc_z, gyro_x, gyro_y, gyro_z], axis=2)

        records = []
        for i in range(windows.shape[0]):
            rec = {
                "timestamp": None,
                "subject_id": int(subjects[i]),
                "session_id": None,
                "dataset": "UCI-HAR",
                "activity": int(activities[i]),
                "device_type": "smartphone",
                "sensor_location": "waist",
                "native_sampling_rate": 50,
                "acc_x": windows[i, :, 0].tolist(),
                "acc_y": windows[i, :, 1].tolist(),
                "acc_z": windows[i, :, 2].tolist(),
                "gyro_x": windows[i, :, 3].tolist(),
                "gyro_y": windows[i, :, 4].tolist(),
                "gyro_z": windows[i, :, 5].tolist(),
                "mag_x": None,
                "mag_y": None,
                "mag_z": None,
                "heart_rate": None,
            }
            records.append(rec)
        df = pd.DataFrame.from_records(records)
        df = df[CANONICAL_COLUMNS]
        return df

    @classmethod
    def load(cls) -> pd.DataFrame:
        """Public method returning a DataFrame with *all* windows.

        Combines the train and test splits preserving the original subject IDs.
        """
        if not EXTRACTED_ROOT.exists():
            raise FileNotFoundError(
                f"UCI‑HAR dataset not found at {EXTRACTED_ROOT}. Run scripts/download_datasets.py first."
            )
        train_df = cls._load_split("train")
        test_df = cls._load_split("test")
        return pd.concat([train_df, test_df], ignore_index=True)

    @staticmethod
    def validate(df: pd.DataFrame) -> dict:
        """Run basic sanity checks on the DataFrame.

        Returns a dictionary that can be printed as a JSON‑compatible report.
        """
        report = {}
        report["rows"] = len(df)
        report["subjects"] = sorted(df["subject_id"].dropna().unique().tolist())
        report["activities"] = sorted(df["activity"].dropna().unique().tolist())
        sensor_cols = ["acc_x", "acc_y", "acc_z", "gyro_x", "gyro_y", "gyro_z"]
        for col in sensor_cols:
            nan_rows = df[col].apply(lambda lst: any(pd.isna(v) for v in lst) if isinstance(lst, list) else pd.isna(lst)).sum()
            report[f"nan_{col}"] = int(nan_rows)
        return report

    @staticmethod
    def metadata(df: pd.DataFrame) -> dict:
        """Extract dataset‑level metadata for storage alongside the NPZ."""
        return {
            "dataset": "UCI-HAR",
            "dataset_version": "original",
            "native_sampling_rate": 50,
            "channels": ["acc_x", "acc_y", "acc_z", "gyro_x", "gyro_y", "gyro_z"],
            "subjects": df["subject_id"].dropna().unique().tolist(),
            "activities": df["activity"].dropna().unique().tolist(),
        }
