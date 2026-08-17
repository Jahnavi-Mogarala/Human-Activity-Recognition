import os
import joblib
import torch
from pathlib import Path
from typing import Any

# Simple model router that returns a loaded model based on the requested dataset.
# In a full implementation this would select among multiple trained models.

def load_model(model_dir: str = "models/smartphone_har") -> Any:
    """Load the latest model checkpoint from ``model_dir``.
    Returns the PyTorch model object (or scikit‑learn model) and any associated metadata.
    """
    model_path = Path(model_dir) / "model.pth"
    if not model_path.exists():
        raise FileNotFoundError(f"Model checkpoint not found at {model_path}")
    # Determine type by file extension or metadata – here we assume a PyTorch model.
    model = torch.load(model_path, map_location=torch.device('cpu'))
    return model
