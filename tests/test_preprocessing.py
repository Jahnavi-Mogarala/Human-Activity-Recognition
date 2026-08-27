# tests/test_preprocessing.py
"""Preprocessing tests.
Verify that the training scaler loads correctly, scales test windows without NaNs/Infs,
and preserves the expected shape (N, 128, 6).
"""

import sys, os, numpy as np

repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
sys.path.append(repo_root)

from scripts.evaluate import load_scaler, scale_data

def test_scaler_load_and_apply():
    scaler_path = os.path.join(repo_root, "models", "smartphone_har", "scaler.pkl")
    if not os.path.isfile(scaler_path):
        raise FileNotFoundError(f"Scaler not found at {scaler_path}")
    scaler = load_scaler(scaler_path)
    means = scaler["means"]
    stds = scaler["stds"]
    # The test NPZ may be named 'test.npz' or 'UCI-HAR_test.npz'.
    possible_names = ["test.npz", "UCI-HAR_test.npz"]
    for name in possible_names:
        test_npz = os.path.join(repo_root, "data", "processed", "UCI_HAR", name)
        if os.path.isfile(test_npz):
            break
    else:
        raise FileNotFoundError("Test NPZ file not found in expected locations")
    data = np.load(test_npz)
    windows = data["windows"]
    scaled = scale_data(windows, means, stds)
    assert scaled.shape == windows.shape
    assert not np.isnan(scaled).any()
    assert np.isfinite(scaled).all()
