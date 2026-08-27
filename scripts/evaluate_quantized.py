#!/usr/bin/env python
"""Evaluate the dynamically quantized BiLSTM+TemporalAttention model.
This script mirrors `scripts/evaluate.py` but loads the quantized checkpoint
(using `torch.load(..., weights_only=False)`) and applies dynamic quantization
to the original architecture before loading the saved state dict.
"""

import argparse
import json
import pathlib
import torch
from torch.quantization import quantize_dynamic
import sys

# Add project root to path for imports
sys.path.append(str(pathlib.Path(__file__).resolve().parents[1]))
from scripts.evaluate import load_config, build_model, load_scaler, scale_data, load_test_data, compute_latency

def main() -> None:
    parser = argparse.ArgumentParser(description="Evaluate quantized model")
    parser.add_argument("--config", type=str, default="models/smartphone_har/config.yaml",
                        help="Path to training config.yaml")
    parser.add_argument("--checkpoint", type=str,
                        default="models/smartphone_har/bilstm_attention_quantized.pth",
                        help="Path to quantized checkpoint (state_dict)")
    parser.add_argument("--output_dir", type=str, default="reports/quantization",
                        help="Directory to write evaluation outputs")
    args = parser.parse_args()

    config_path = pathlib.Path(args.config)
    cfg = load_config(config_path)
    model_dir = config_path.parent

    # Load scaler
    scaler_path = model_dir / "scaler.pkl"
    scaler = load_scaler(scaler_path)
    means, stds = scaler["means"], scaler["stds"]

    # Load test data
    test_npz_path = pathlib.Path(cfg.get('test_path'))
    test_windows, test_labels = load_test_data(test_npz_path)
    # Scale
    test_windows = scale_data(test_windows, means, stds)

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    # Build original model architecture
    model = build_model(cfg, input_dim=6).to(device)
    # Apply dynamic quantization
    quantized_model = quantize_dynamic(model, {torch.nn.Linear, torch.nn.LSTM}, dtype=torch.qint8)

    # Load quantized state dict (allow unsafe globals)
    ckpt = torch.load(args.checkpoint, map_location=device, weights_only=False)
    quantized_model.load_state_dict(ckpt["model_state_dict"])
    quantized_model.eval()

    # Inference
    X_test = torch.tensor(test_windows, dtype=torch.float32).to(device)
    with torch.no_grad():
        outputs = quantized_model(X_test)
        preds = outputs.argmax(dim=1).cpu().numpy()

    # Compute metrics (reuse code from evaluate script)
    from sklearn.metrics import (accuracy_score, balanced_accuracy_score,
                                 precision_score, recall_score, f1_score,
                                 classification_report, confusion_matrix)
    overall = {
        "accuracy": accuracy_score(test_labels, preds),
        "balanced_accuracy": balanced_accuracy_score(test_labels, preds),
        "macro_precision": precision_score(test_labels, preds, average="macro", zero_division=0),
        "macro_recall": recall_score(test_labels, preds, average="macro", zero_division=0),
        "macro_f1": f1_score(test_labels, preds, average="macro", zero_division=0),
        "weighted_precision": precision_score(test_labels, preds, average="weighted", zero_division=0),
        "weighted_recall": recall_score(test_labels, preds, average="weighted", zero_division=0),
        "weighted_f1": f1_score(test_labels, preds, average="weighted", zero_division=0),
    }
    class_report = classification_report(test_labels, preds, output_dict=True, zero_division=0)

    # Latency
    latency_info = compute_latency(quantized_model, device, X_test, batch_size=64, repetitions=30)

    # Save metrics JSON
    output_dir = pathlib.Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    metrics_path = output_dir / "quantized_metrics.json"
    with open(metrics_path, "w") as f:
        json.dump({"overall": overall, "latency": latency_info, "class_report": class_report}, f, indent=2)

    # Print summary
    print("=== Quantized Model Evaluation Summary ===")
    for k, v in overall.items():
        print(f"{k}: {v:.4f}")
    print(f"Mean inference latency (per batch): {latency_info['mean_latency']:.6f} s")
    print(f"Total inference time (full test set): {latency_info['total_inference_time']:.2f} s")
    print("=== End Summary ===")

if __name__ == "__main__":
    main()
