# tests/test_evaluation.py
"""Evaluation script tests.
Runs the evaluation script on the existing model/config using a temporary output directory.
Ensures the script finishes without error and generates the expected report files.
"""

import os, sys, subprocess
from pathlib import Path

repo_root = Path(__file__).resolve().parents[1]
sys.path.append(str(repo_root))

def test_evaluation_script(tmp_path):
    # Paths
    config_path = repo_root / "models" / "smartphone_har" / "config.yaml"
    ckpt_path = repo_root / "models" / "smartphone_har" / "best_checkpoint.pth"
    assert config_path.is_file(), f"Config missing: {config_path}"
    assert ckpt_path.is_file(), f"Checkpoint missing: {ckpt_path}"

    output_dir = tmp_path / "eval_reports"
    output_dir.mkdir()

    cmd = [
        str(repo_root / "har-torch-py311" / "Scripts" / "python.exe"),
        str(repo_root / "scripts" / "evaluate.py"),
        "--config",
        str(config_path),
        "--checkpoint",
        str(ckpt_path),
        "--output_dir",
        str(output_dir),
    ]
    result = subprocess.run(cmd, capture_output=True, text=True)
    # Ensure the script completed successfully
    assert result.returncode == 0, f"Evaluation script failed (code {result.returncode}):\n{result.stdout}\n{result.stderr}"

    # Expected files
    expected_files = [
        "classification_report.json",
        "confusion_matrix.png",
    ]
    for fname in expected_files:
        fpath = output_dir / fname
        assert fpath.is_file(), f"Missing expected report file: {fpath}"

    # Verify metrics.json exists and contains required keys
    metrics_path = repo_root / "models" / "smartphone_har" / "metrics.json"
    assert metrics_path.is_file(), f"Metrics file not found: {metrics_path}"
    import json
    metrics = json.load(open(metrics_path))
    for key in ["overall", "per_class", "model_statistics"]:
        assert key in metrics, f"Key '{key}' missing in metrics.json"
