# scripts/export_torchscript.py
"""Export the trained BiLSTM+Attention PyTorch model to TorchScript.

Usage:
    python scripts/export_torchscript.py \
        --checkpoint models/experiments/dev_smoke/best_checkpoint.pth \
        --config configs/experiments/dev_sanity.yaml \
        --output bilstm_attention.pt
"""
import os
import sys
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

import os
import torch
from ml.models.bilstm_attention import BiLSTMAttention
import yaml
import argparse

def load_config(path: str):
    with open(path, 'r') as f:
        return yaml.safe_load(f)

def load_checkpoint(path: str):
    ckpt = torch.load(path, map_location='cpu')
    # The checkpoint saved by train.py stores the model under the key 'model_state_dict'
    if isinstance(ckpt, dict) and 'model_state_dict' in ckpt:
        return ckpt['model_state_dict']
    return ckpt

def main():
    parser = argparse.ArgumentParser(description='Export trained model to TorchScript')
    parser.add_argument('--checkpoint', type=str, required=True, help='Path to .pth checkpoint')
    parser.add_argument('--config', type=str, required=True, help='Path to training yaml config')
    parser.add_argument('--output', type=str, default='bilstm_attention.pt', help='Output TorchScript file')
    args = parser.parse_args()

    cfg = load_config(args.config)
    model = BiLSTMAttention(
        input_dim=6,
        hidden_size=cfg.get('hidden_size', 128),
        num_layers=cfg.get('num_layers', 2),
        num_classes=cfg.get('num_classes', 6)
    )
    model.eval()
    state_dict = load_checkpoint(args.checkpoint)
    model.load_state_dict(state_dict)

    dummy = torch.randn(1, 128, 6)
    traced = torch.jit.trace(model, dummy)
    traced.save(args.output)
    print(f"TorchScript model saved to {args.output}")

if __name__ == '__main__':
    main()
