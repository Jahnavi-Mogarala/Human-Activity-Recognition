#!/usr/bin/env python
"""Quantize the BiLSTM+TemporalAttention model using dynamic quantization.
The script:
1. Loads the training config (models/smartphone_har/config.yaml).
2. Builds the original model architecture.
3. Loads the original checkpoint (best_checkpoint.pth).
4. Applies torch.quantization.quantize_dynamic on supported layers (nn.Linear, nn.LSTM).
5. Saves the quantized model state_dict to models/smartphone_har/bilstm_attention_quantized.pth.
"""

import argparse
import json
import pathlib
import torch
from torch.quantization import quantize_dynamic

# Import helpers from the existing evaluation script
import sys
sys.path.append(str(pathlib.Path(__file__).resolve().parents[1]))  # add project root to path
from scripts.evaluate import load_config, build_model

def main() -> None:
    parser = argparse.ArgumentParser(description="Dynamic quantization of the BiLSTM+TemporalAttention model")
    parser.add_argument("--config", type=str, default="models/smartphone_har/config.yaml",
                        help="Path to the config.yaml used for training")
    parser.add_argument("--checkpoint", type=str,
                        default="models/smartphone_har/best_checkpoint.pth",
                        help="Path to the original full‑precision checkpoint")
    parser.add_argument("--output", type=str,
                        default="models/smartphone_har/bilstm_attention_quantized.pth",
                        help="Path where the quantized checkpoint will be saved")
    args = parser.parse_args()

    config_path = pathlib.Path(args.config)
    cfg = load_config(config_path)

    # Build the original model architecture (un‑quantized)
    model = build_model(cfg, input_dim=6)  # input_dim is always 6 for UCI‑HAR
    # Load the original state dict
    ckpt = torch.load(args.checkpoint, map_location="cpu")
    model.load_state_dict(ckpt["model_state_dict"])
    model.eval()

    # Apply dynamic quantization – supports Linear and LSTM layers
    quantized_model = quantize_dynamic(
        model,
        {torch.nn.Linear, torch.nn.LSTM},
        dtype=torch.qint8,
    )

    # Save the quantized state dict in the same format as the original checkpoint
    quantized_ckpt = {"model_state_dict": quantized_model.state_dict()}
    torch.save(quantized_ckpt, args.output)
    print(f"Quantized checkpoint saved to {args.output}")

if __name__ == "__main__":
    main()
