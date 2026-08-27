# tests/test_model.py
"""Model loading and inference tests.
Ensures the Bi‑LSTM + Temporal‑Attention checkpoint loads, a dummy forward pass works,
output shape is correct, predictions map to 1‑6, and attention weights have the expected shape and row‑sum ≈ 1.
"""

import os, sys
import torch
import numpy as np

# Add repository root to PYTHONPATH
repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
sys.path.append(repo_root)

from ml.models import get_model

# Helper to build model with same config as evaluation script expects
def build_model_from_config(config_path):
    import yaml
    cfg = yaml.safe_load(open(config_path))
    input_dim = 6  # fixed for UCI‑HAR
    model_cfg = {
        "input_dim": input_dim,
        "num_classes": cfg.get("num_classes", 6),
        "hidden_size": cfg.get("hidden_size", 128),
        "num_layers": cfg.get("num_layers", 2),
        "dropout": cfg.get("dropout", 0.3),
        "attention_dim": cfg.get("attention_dim", 64),
    }
    return get_model(cfg["model"], **model_cfg)


def test_checkpoint_load_and_forward():
    # Paths
    config_path = os.path.join(repo_root, "models", "smartphone_har", "config.yaml")
    ckpt_path = os.path.join(repo_root, "models", "smartphone_har", "best_checkpoint.pth")
    assert os.path.isfile(config_path), f"Config not found: {config_path}"
    assert os.path.isfile(ckpt_path), f"Checkpoint not found: {ckpt_path}"

    model = build_model_from_config(config_path)
    device = torch.device("cpu")
    model.to(device)
    ckpt = torch.load(ckpt_path, map_location=device)
    model.load_state_dict(ckpt["model_state_dict"])
    model.eval()

    # Dummy input (batch=2, seq=128, channels=6)
    dummy = torch.randn(2, 128, 6, dtype=torch.float32, device=device)
    with torch.no_grad():
        output = model(dummy)
        # Check output shape (batch, num_classes)
        assert output.shape == (2, 6), f"Unexpected output shape: {output.shape}"
        # Predictions should be indices 0‑5 internally
        preds = output.argmax(dim=1)
        assert preds.min() >= 0 and preds.max() <= 5
        # Convert to external labels 1‑6
        external = preds + 1
        assert external.min() >= 1 and external.max() <= 6
        # Attention weights (if model provides get_attention)
        if hasattr(model, "get_attention"):
            attn = model.get_attention(dummy)
            # Expected shape (batch, seq_len)
            assert attn.shape == (2, 128), f"Attention shape incorrect: {attn.shape}"
            # Row sums should be approximately 1.0
            row_sums = attn.sum(dim=1)
            diff = torch.abs(row_sums - 1.0)
            assert torch.all(diff < 1e-4), f"Attention rows do not sum to 1: {row_sums}"
