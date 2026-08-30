# tests/test_full_training.py
"""Verification test for the full training output.

Ensures that the final checkpoint file and metrics JSON exist and contain the
required top‑level keys.
"""

import json
from pathlib import Path

def test_checkpoint_and_metrics_exist():
    # Resolve repository root correctly regardless of current working directory
    repo_root = Path(__file__).resolve().parents[1]
    model_dir = repo_root / "models" / "smartphone_har"
    checkpoint = model_dir / "best_checkpoint.pth"
    metrics_file = model_dir / "metrics.json"

    assert checkpoint.is_file(), f"Checkpoint not found: {checkpoint}"
    assert metrics_file.is_file(), f"Metrics file not found: {metrics_file}"

    with metrics_file.open() as f:
        metrics = json.load(f)

    for key in ["overall", "per_class", "model_statistics"]:
        assert key in metrics, f"Key '{key}' missing in metrics.json"
