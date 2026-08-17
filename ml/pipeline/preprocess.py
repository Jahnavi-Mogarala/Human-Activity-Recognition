import os
from pathlib import Path
import numpy as np
import pandas as pd
from sklearn.preprocessing import StandardScaler
from typing import Tuple

def clean(df: pd.DataFrame) -> pd.DataFrame:
    """Drop rows with NaNs in required sensor columns and ensure timestamps are monotonic."""
    required_cols = [c for c in df.columns if c.startswith("acc_") or c.startswith("gyro_")]
    df_clean = df.dropna(subset=required_cols)
    if "timestamp" in df_clean.columns:
        df_clean = df_clean.sort_values("timestamp")
    return df_clean.reset_index(drop=True)

def resample(df: pd.DataFrame, target_rate: int = 50) -> pd.DataFrame:
    """Resample to target_rate (Hz) using linear interpolation.
    Assumes a uniform original sampling frequency.
    """
    if "timestamp" not in df.columns:
        return df
    # Create a new timestamp index
    start, end = df["timestamp"].iloc[0], df["timestamp"].iloc[-1]
    n_samples = int((end - start) * target_rate) + 1
    new_ts = np.linspace(start, end, n_samples)
    df_new = pd.DataFrame({"timestamp": new_ts})
    # Interpolate each numeric column
    for col in df.columns:
        if col == "timestamp":
            continue
        df_new[col] = np.interp(new_ts, df["timestamp"], df[col])
    return df_new

def window(df: pd.DataFrame, window_seconds: float = 2.0, overlap: float = 0.5) -> np.ndarray:
    """Create overlapping windows. Returns a 3‑D array (n_windows, window_len, n_channels)."""
    if "timestamp" not in df.columns:
        raise ValueError("Dataframe must contain a 'timestamp' column for windowing.")
    # Determine sampling interval from median diff
    dt = np.median(np.diff(df["timestamp"]))
    window_len = int(window_seconds / dt)
    step = int(window_len * (1 - overlap))
    sensor_cols = [c for c in df.columns if c not in {"timestamp", "subject_id", "session_id", "dataset", "activity", "device_type", "sensor_location", "sampling_rate"}]
    data = df[sensor_cols].values
    windows = []
    for start in range(0, len(data) - window_len + 1, step):
        windows.append(data[start:start + window_len])
    return np.stack(windows)

def normalize(windows: np.ndarray, scaler_path: Path | None = None) -> Tuple[np.ndarray, StandardScaler]:
    """Standard‑scale each channel across all windows.
    Returns scaled windows and the fitted scaler.
    """
    n_windows, win_len, n_channels = windows.shape
    reshaped = windows.reshape(-1, n_channels)
    scaler = StandardScaler()
    scaled = scaler.fit_transform(reshaped)
    windows_scaled = scaled.reshape(n_windows, win_len, n_channels)
    if scaler_path:
        scaler_path.parent.mkdir(parents=True, exist_ok=True)
        import joblib
        joblib.dump(scaler, scaler_path)
    return windows_scaled, scaler

def process_dataset(df: pd.DataFrame, out_path: Path) -> None:
    """Run the full preprocessing pipeline and save ``.npz`` with windows and labels.
    The label column is taken from the ``activity`` field.
    """
    df_clean = clean(df)
    df_resampled = resample(df_clean)
    windows = window(df_resampled)
    windows_scaled, scaler = normalize(windows, out_path.parent / "scaler.pkl")
    # Encode labels
    labels = df_resampled["activity"].iloc[: len(windows_scaled) * windows_scaled.shape[1]].values
    # Simple majority label per window
    win_labels = []
    win_len = windows_scaled.shape[1]
    for i in range(len(windows_scaled)):
        start = i * win_len
        end = start + win_len
        win_labels.append(pd.Series(labels[start:end]).mode()[0])
    np.savez_compressed(out_path, windows=windows_scaled, labels=win_labels)
    print(f"Saved processed data to {out_path}")
