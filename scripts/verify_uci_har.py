#!/usr/bin/env python
"""Verification script for UCI-HAR processed data.
Prints a JSON report with all required metrics.
"""
import json
import numpy as np
from pathlib import Path

def load_npz(path):
    data = np.load(path)
    return {
        "windows": data["windows"],
        "labels": data["labels"],
        "subject_ids": data["subject_ids"]
    }

def compute_stats(npz_dict):
    windows = npz_dict["windows"]
    labels = npz_dict["labels"]
    subjects = npz_dict["subject_ids"]
    return {
        "num_windows": int(windows.shape[0]),
        "window_shape": list(windows.shape),
        "unique_subjects": sorted(map(int, set(subjects.tolist()))),
        "num_subjects": len(set(subjects.tolist())),
        "nan_count": int(np.isnan(windows).sum()),
        "inf_count": int(np.isinf(windows).sum()),
        "label_distribution": {int(k): int(v) for k, v in zip(*np.unique(labels, return_counts=True))}
    }

def main():
    base_raw = Path("data/raw/UCI-HAR")
    raw_files = sorted([p.name for p in base_raw.rglob("*") if p.is_file()])

    base_proc = Path("data/processed/UCI_HAR")
    proc_files = sorted([p.name for p in base_proc.iterdir() if p.is_file()])

    train = load_npz(base_proc / "UCI-HAR_train.npz")
    val = load_npz(base_proc / "UCI-HAR_val.npz")
    test = load_npz(base_proc / "UCI-HAR_test.npz")

    train_stats = compute_stats(train)
    val_stats = compute_stats(val)
    test_stats = compute_stats(test)

    # Load metadata
    def load_meta(name):
        with open(base_proc / f"metadata_{name}.json") as f:
            return json.load(f)
    meta_train = load_meta("train")
    meta_val = load_meta("val")
    meta_test = load_meta("test")

    # Subject overlap check
    overlap = {
        "train_val": set(train_stats["unique_subjects"]).intersection(val_stats["unique_subjects"]),
        "train_test": set(train_stats["unique_subjects"]).intersection(test_stats["unique_subjects"]),
        "val_test": set(val_stats["unique_subjects"]).intersection(test_stats["unique_subjects"])
    }

    report = {
        "raw_dataset_files": raw_files,
        "processed_files": proc_files,
        "train": train_stats,
        "val": val_stats,
        "test": test_stats,
        "subject_overlap": {k: sorted(v) for k, v in overlap.items()},
        "channel_order": meta_train.get("channels"),
        "sampling_rate": meta_train.get("native_sampling_rate"),
        "normalization": {
            "train_fitted": meta_train.get("normalization_fitted_on"),
            "val_fitted": meta_val.get("normalization_fitted_on"),
            "test_fitted": meta_test.get("normalization_fitted_on")
        },
        "class_labels": ["WALKING", "WALKING_UPSTAIRS", "WALKING_DOWNSTAIRS", "SITTING", "STANDING", "LAYING"]
    }
    print(json.dumps(report, indent=2))

if __name__ == "__main__":
    main()
