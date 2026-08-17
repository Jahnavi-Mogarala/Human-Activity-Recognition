#!/usr/bin/env python
"""Prepare raw HAR datasets into canonical NPZ format.

This script follows the strict pipeline required by the project:

1. Load the raw dataset via a memory‑efficient adapter that returns a
   :class:`pandas.DataFrame` with the canonical sensor schema.
2. Run raw‑data validation and emit a JSON report.
3. Perform **subject‑level splitting** using ``GroupKFold`` with three
   folds – each fold becomes one of *train*, *validation*, or *test*.
   The splits are deterministic and contain **zero subject overlap**.
4. For each split, build the ``windows`` array ``[N, T, C]`` where ``C``
   contains only the sensor modalities actually present in the dataset.
5. Save the split‑specific NPZ files and a companion ``metadata_*.json``
   containing both native and target sampling rates, window configuration,
   channels, and split information.
6. Write a human‑readable validation summary to the console and a
   ``validation_report.json`` file under ``data/processed/<dataset>/``.

The script does **not** perform any scaling or model training – those are
handled later in the training pipeline.
"""

import argparse
import importlib
import json
from pathlib import Path
import numpy as np
import pandas as pd
from tqdm import tqdm
from sklearn.model_selection import GroupKFold
import sys
sys.path.append(str(Path(__file__).resolve().parents[1]))

# ---------------------------------------------------------------------------
# Helper to dynamically load the appropriate adapter class
# ---------------------------------------------------------------------------

def load_adapter(dataset_name: str):
    mod_name = dataset_name.lower().replace("-", "_").replace(" ", "_")
    module_path = f"data.adapters.{mod_name}"
    try:
        module = importlib.import_module(module_path)
    except ImportError as e:
        raise ImportError(f"Adapter module for {dataset_name} not found: {e}")
    class_name = f"{dataset_name.upper().replace('-', '_')}Adapter"
    try:
        adapter_cls = getattr(module, class_name)
    except AttributeError as e:
        raise AttributeError(f"Adapter class {class_name} missing in {module_path}: {e}")
    return adapter_cls()

# ---------------------------------------------------------------------------
# Window construction – expects each sensor column to hold a list of samples
# ---------------------------------------------------------------------------

def build_windows(df_slice: pd.DataFrame, sensor_cols: list) -> np.ndarray:
    windows = []
    for _, row in tqdm(df_slice.iterrows(), total=len(df_slice), desc="Building windows"):
        channel_data = []
        for col in sensor_cols:
            values = row[col]
            if isinstance(values, list):
                channel_data.append(values)
            else:
                # Missing modality – create a list of NaNs to keep shape consistent
                length = len(row[sensor_cols[0]]) if isinstance(row[sensor_cols[0]], list) else 0
                channel_data.append([float('nan')] * length)
        # (T, C)
        windows.append(np.stack(channel_data, axis=1))
    # (N, T, C)
    return np.stack(windows, axis=0)

# ---------------------------------------------------------------------------
def main():
    parser = argparse.ArgumentParser(description="Prepare a raw HAR dataset into NPZ files.")
    parser.add_argument("--dataset", type=str, required=True,
                        help="Dataset to process (UCI-HAR, WISDM, HAPT, all)")
    args = parser.parse_args()

    dataset_names = [args.dataset.upper()] if args.dataset.lower() != "all" else ["UCI-HAR", "WISDM", "HAPT"]

    for ds in dataset_names:
        print(f"\n=== Processing {ds} ===")
        adapter = load_adapter(ds)
        df = adapter.load()
        # -------------------------------------------------------------------
        # Validation
        # -------------------------------------------------------------------
        val_report = adapter.validate(df)
        report_path = Path("data/processed") / ds.replace('-', '_') / "validation_report.json"
        report_path.parent.mkdir(parents=True, exist_ok=True)
        with open(report_path, "w", encoding="utf-8") as f:
            json.dump(val_report, f, indent=2)
        print("Validation report written to", report_path)
        print("Summary:")
        for k, v in val_report.items():
            print(f"  {k}: {v}")

        # -------------------------------------------------------------------
        # Subject‑level split (deterministic GroupKFold with 3 folds)
        # -------------------------------------------------------------------
        groups = df["subject_id"].astype(str).values
        gkf = GroupKFold(n_splits=3)
        # Each fold's test indices become one of the splits
        fold_test_indices = [test_idx for _, test_idx in gkf.split(df, groups=groups)]
        split_names = ["train", "val", "test"]
        # Verify we have exactly three disjoint folds
        assert len(fold_test_indices) == 3, "Expected three folds for train/val/test"
        # Determine which subjects belong to each split for reporting
        split_subjects = {}
        for name, idx in zip(split_names, fold_test_indices):
            split_subjects[name] = sorted(df.loc[idx, "subject_id"].unique().tolist())
        print("Subject splits:")
        for name in split_names:
            print(f"  {name}: {split_subjects[name]}")
        # Overlap check
        print("Overlap check:")
        print("  train & val =", set(split_subjects["train"]).intersection(split_subjects["val"]))
        print("  train & test =", set(split_subjects["train"]).intersection(split_subjects["test"]))
        print("  val & test =", set(split_subjects["val"]).intersection(split_subjects["test"]))

        # Determine actual sensor columns present (exclude completely missing modalities)
        sensor_candidates = ["acc_x", "acc_y", "acc_z", "gyro_x", "gyro_y", "gyro_z",
                             "mag_x", "mag_y", "mag_z", "heart_rate"]
        sensor_cols = [c for c in sensor_candidates if c in df.columns]
        sensor_cols = [c for c in sensor_cols if not df[c].apply(lambda x: x is None).all()]

        # -------------------------------------------------------------------
        # Process each split
        # -------------------------------------------------------------------
        for split_name, idx in zip(split_names, fold_test_indices):
            split_df = df.iloc[idx]
            print(f"Building windows for {split_name} split (rows: {len(split_df)})")
            windows = build_windows(split_df, sensor_cols)
            labels = split_df["activity"].to_numpy()
            subject_ids = split_df["subject_id"].to_numpy()

            # -------------------------------------------------------------------
            # Metadata for this split
            # -------------------------------------------------------------------
            meta = adapter.metadata(df)
            meta.update({
                "split": split_name,
                "native_sampling_rate": meta.get("native_sampling_rate"),
                "target_sampling_rate": None,  # to be set by experiment config later
                "window_seconds": None,        # filled later by experiment config
                "window_samples": windows.shape[1],
                "overlap": None,
                "channels": sensor_cols,
                "subjects": split_subjects[split_name],
                "num_windows": windows.shape[0],
            })
            out_dir = Path("data/processed") / ds.replace('-', '_')
            out_dir.mkdir(parents=True, exist_ok=True)
            npz_path = out_dir / f"{ds}_{split_name}.npz"
            np.savez_compressed(npz_path, windows=windows, labels=labels, subject_ids=subject_ids)
            meta_path = out_dir / f"metadata_{split_name}.json"
            with open(meta_path, "w", encoding="utf-8") as f:
                json.dump(meta, f, indent=2)
            print(f"Saved {split_name} NPZ to {npz_path}")
            print(f"Saved {split_name} metadata to {meta_path}")

if __name__ == "__main__":
    main()
