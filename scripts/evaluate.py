#!/usr/bin/env python
"""Evaluation script for the Bi‑LSTM + Temporal Attention model.

Usage:
    python scripts/evaluate.py \
        --config models/smartphone_har/config.yaml \
        [--checkpoint models/smartphone_har/best_checkpoint.pth] \
        [--output_dir reports]

The script loads the trained checkpoint, the training‑fitted scaler, the test split,
re‑creates the exact model architecture, runs inference, computes a full set of
metrics, generates plots and JSON reports, and prints a concise summary.
"""

import argparse
import json
import yaml
import joblib
import os
import time
import numpy as np
import torch
from pathlib import Path
from sklearn.metrics import (
    accuracy_score,
    balanced_accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    classification_report,
    confusion_matrix,
)
import matplotlib.pyplot as plt
import seaborn as sns

# Model factory
from ml.models import get_model


def load_config(config_path: Path) -> dict:
    return yaml.safe_load(config_path.read_text())


def load_scaler(scaler_path: Path):
    return joblib.load(scaler_path)


def scale_data(arr: np.ndarray, means: np.ndarray, stds: np.ndarray) -> np.ndarray:
    flat = arr.reshape(-1, arr.shape[2])
    scaled = (flat - means) / stds
    return scaled.reshape(arr.shape)


def load_test_data(test_path: Path):
    data = np.load(test_path)
    windows = data["windows"]
    labels = data["labels"]
    return windows, labels


def verify_metadata(meta: dict, cfg: dict, windows_shape: tuple):
    # Channel order
    expected_channels = ["acc_x", "acc_y", "acc_z", "gyro_x", "gyro_y", "gyro_z"]
    if meta.get("channels") != expected_channels:
        raise ValueError(f"Channel order mismatch: {meta.get('channels')} vs {expected_channels}")
    # Sequence length and channels
    if windows_shape[1:] != (128, 6):
        raise ValueError(f"Unexpected window shape {windows_shape}, expected (N,128,6)")
    # Number of classes
    if cfg.get("num_classes", 6) != meta.get("num_classes", 6):
        raise ValueError("Number of classes in config does not match metadata")
    # Architecture name
    if cfg.get("model") != meta.get("model", cfg.get("model")):
        raise ValueError("Model architecture name mismatch between config and metadata")


def build_model(cfg: dict, input_dim: int):
    model_cfg = {
        "input_dim": input_dim,
        "num_classes": cfg.get("num_classes", 6),
        "hidden_size": cfg.get("hidden_size", 128),
        "num_layers": cfg.get("num_layers", 2),
        "dropout": cfg.get("dropout", 0.3),
        "attention_dim": cfg.get("attention_dim", 64),
    }
    model = get_model(cfg["model"], **model_cfg)
    return model


def plot_confusion(cm: np.ndarray, class_names: list, out_path: Path):
    plt.figure(figsize=(8, 6))
    sns.heatmap(cm, annot=True, fmt="d", cmap="Blues", xticklabels=class_names, yticklabels=class_names)
    plt.xlabel("Predicted")
    plt.ylabel("True")
    plt.title("Confusion Matrix")
    plt.tight_layout()
    plt.savefig(out_path)
    plt.close()


def plot_training_curves(history_path: Path, out_path: Path):
    with open(history_path) as f:
        hist = json.load(f)
    epochs = hist["epochs"]
    plt.figure(figsize=(10, 4))
    # Loss
    plt.subplot(1, 2, 1)
    plt.plot(epochs, hist["train_loss"], label="Train Loss")
    plt.plot(epochs, hist["val_loss"], label="Val Loss")
    plt.xlabel("Epoch")
    plt.ylabel("Loss")
    plt.title("Training / Validation Loss")
    plt.legend()
    # Accuracy
    plt.subplot(1, 2, 2)
    plt.plot(epochs, hist["train_accuracy"], label="Train Acc")
    plt.plot(epochs, hist["val_accuracy"], label="Val Acc")
    plt.xlabel("Epoch")
    plt.ylabel("Accuracy")
    plt.title("Training / Validation Accuracy")
    plt.legend()
    plt.tight_layout()
    plt.savefig(out_path)
    plt.close()


def plot_per_class_metrics(report: dict, out_path: Path):
    classes = list(report["by_class"].keys())
    precision = [report["by_class"][c]["precision"] for c in classes]
    recall = [report["by_class"][c]["recall"] for c in classes]
    f1 = [report["by_class"][c]["f1-score"] for c in classes]
    x = np.arange(len(classes))
    width = 0.25
    plt.figure(figsize=(10, 6))
    plt.bar(x - width, precision, width, label="Precision")
    plt.bar(x, recall, width, label="Recall")
    plt.bar(x + width, f1, width, label="F1")
    plt.xticks(x, classes, rotation=45)
    plt.ylabel("Score")
    plt.title("Per‑Class Metrics")
    plt.legend()
    plt.tight_layout()
    plt.savefig(out_path)
    plt.close()


def compute_latency(model: torch.nn.Module, device: torch.device, data: torch.Tensor, batch_size: int = 64, repetitions: int = 30):
    model.eval()
    loader = torch.utils.data.DataLoader(data, batch_size=batch_size, shuffle=False)
    # Warm‑up
    with torch.no_grad():
        for xb in loader:
            xb = xb.to(device)
            _ = model(xb)
            break
    latencies = []
    total_samples = 0
    total_time = 0.0
    with torch.no_grad():
        for _ in range(repetitions):
            start = time.time()
            for xb in loader:
                xb = xb.to(device)
                _ = model(xb)
                total_samples += xb.size(0)
            torch.cuda.synchronize() if device.type == "cuda" else None
            end = time.time()
            elapsed = end - start
            latencies.append(elapsed / len(loader))
            total_time += elapsed
    latencies = np.array(latencies)
    avg_latency = latencies.mean()
    median = np.median(latencies)
    std = latencies.std()
    avg_batch_latency = avg_latency
    total_inference_time = total_time
    return {
        "mean_latency": avg_latency,
        "median_latency": median,
        "std_latency": std,
        "average_batch_latency": avg_batch_latency,
        "total_inference_time": total_inference_time,
        "batch_size": batch_size,
        "device": str(device),
    }


def main():
    parser = argparse.ArgumentParser(description="Evaluate a trained MotionShield model")
    parser.add_argument("--config", type=str, required=True, help="Path to training config.yaml (produced by train.py)")
    parser.add_argument("--checkpoint", type=str, default=None, help="Path to checkpoint .pth (defaults to best_checkpoint.pth in same dir as config)")
    parser.add_argument("--output_dir", type=str, default="reports", help="Directory to store evaluation reports and plots")
    args = parser.parse_args()

    config_path = Path(args.config)
    cfg = load_config(config_path)
    model_dir = config_path.parent

    # Resolve checkpoint path
    ckpt_path = Path(args.checkpoint) if args.checkpoint else model_dir / "best_checkpoint.pth"
    if not ckpt_path.is_file():
        raise FileNotFoundError(f"Checkpoint not found: {ckpt_path}")

    scaler_path = model_dir / "scaler.pkl"
    if not scaler_path.is_file():
        raise FileNotFoundError(f"Scaler file not found: {scaler_path}")
    scaler = load_scaler(scaler_path)
    means = scaler["means"]
    stds = scaler["stds"]

    # Load test data
    test_npz_path = Path(cfg.get('test_path'))
    if not test_npz_path.is_file():
        raise FileNotFoundError(f"Test NPZ not found: {test_npz_path}")
    test_windows, test_labels = load_test_data(test_npz_path)

    # Load metadata for verification
    metadata_path = model_dir / "model_metadata.json"
    if not metadata_path.is_file():
        raise FileNotFoundError(f"Model metadata not found: {metadata_path}")
    meta = json.load(open(metadata_path))

    # Verify metadata against config and data
    verify_metadata(meta, cfg, test_windows.shape)

    # Scale test data using training‑fitted scaler
    test_windows = scale_data(test_windows, means, stds)

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    model = build_model(cfg, input_dim=test_windows.shape[2]).to(device)
    # Load checkpoint state dict
    ckpt = torch.load(ckpt_path, map_location=device)
    model.load_state_dict(ckpt["model_state_dict"])
    model.eval()

    # Inference
    X_test = torch.tensor(test_windows, dtype=torch.float32).to(device)
    with torch.no_grad():
        outputs = model(X_test)
        preds = outputs.argmax(dim=1).cpu().numpy()
    true = test_labels

    # Metrics
    overall = {
        "accuracy": accuracy_score(true, preds),
        "balanced_accuracy": balanced_accuracy_score(true, preds),
        "macro_precision": precision_score(true, preds, average="macro", zero_division=0),
        "macro_recall": recall_score(true, preds, average="macro", zero_division=0),
        "macro_f1": f1_score(true, preds, average="macro", zero_division=0),
        "weighted_precision": precision_score(true, preds, average="weighted", zero_division=0),
        "weighted_recall": recall_score(true, preds, average="weighted", zero_division=0),
        "weighted_f1": f1_score(true, preds, average="weighted", zero_division=0),
    }
    # Detailed per‑class report
    class_report = classification_report(true, preds, output_dict=True, zero_division=0)
    # Save classification report JSON (only)
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    report_json_path = output_dir / "classification_report.json"
    with open(report_json_path, "w") as f:
        json.dump(class_report, f, indent=2)

    # Confusion matrix plot
    cm = confusion_matrix(true, preds)
    class_names = [meta.get("label_mapping", {}).get(str(i), str(i)) for i in range(cfg.get("num_classes", 6))]
    # Ensure names correspond to actual activity labels
    if not all(name for name in class_names):
        # fallback to generic names
        class_names = [str(i) for i in range(cfg.get("num_classes", 6))]
    plot_confusion(cm, class_names, output_dir / "confusion_matrix.png")

    # Training curves plot
    training_history_path = model_dir / "training_history.json"
    if training_history_path.is_file():
        plot_training_curves(training_history_path, output_dir / "training_curves.png")

    # Per‑class metrics bar chart
    plot_per_class_metrics(class_report, output_dir / "per_class_metrics.png")

    # Model statistics
    param_count = sum(p.numel() for p in model.parameters())
    trainable_params = sum(p.numel() for p in model.parameters() if p.requires_grad)
    checkpoint_size = ckpt_path.stat().st_size
    latency_info = compute_latency(model, device, X_test, batch_size=64, repetitions=30)

    metrics = {
        "overall": overall,
        "per_class": class_report["by_class"] if "by_class" in class_report else {},
        "model_statistics": {
            "total_parameters": param_count,
            "trainable_parameters": trainable_params,
            "checkpoint_size_bytes": checkpoint_size,
            "device": latency_info["device"],
            "average_batch_latency_sec": latency_info["average_batch_latency"],
            "mean_latency_sec": latency_info["mean_latency"],
            "median_latency_sec": latency_info["median_latency"],
            "std_latency_sec": latency_info["std_latency"],
            "total_inference_time_sec": latency_info["total_inference_time"],
            "batch_size": latency_info["batch_size"],
        },
    }
    # Save metrics JSON in model directory
    metrics_path = model_dir / "metrics.json"
    with open(metrics_path, "w") as f:
        json.dump(metrics, f, indent=2)

    # Human‑readable summary
    print("=== Evaluation Summary ===")
    for k, v in overall.items():
        print(f"{k}: {v:.4f}")
    print(f"Checkpoint size: {checkpoint_size / (1024 ** 2):.2f} MB")
    print(f"Total parameters: {param_count}")
    print(f"Trainable parameters: {trainable_params}")
    print(f"Mean inference latency (per batch): {latency_info['mean_latency']:.6f} s")
    print(f"Total inference time (full test set): {latency_info['total_inference_time']:.2f} s")
    print(f"Reports written to: {output_dir.resolve()}")
    print("=== End Evaluation ===")

if __name__ == "__main__":
    main()
