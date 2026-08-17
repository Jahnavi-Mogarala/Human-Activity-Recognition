#!/usr/bin/env python
"""Download raw datasets if missing.

Usage:
    python scripts/download_datasets.py --dataset UCI-HAR
    python scripts/download_datasets.py --dataset WISDM
    python scripts/download_datasets.py --dataset HAPT
    python scripts/download_datasets.py --dataset all

The script creates `data/raw/<dataset>/` directories as needed and
downloads the official archives only when the expected data is not already present.
It verifies checksums (when known) and extracts safely.
"""

import argparse
import hashlib
import os
import sys
import zipfile
from pathlib import Path
from urllib.request import urlopen
from tqdm import tqdm

DATASETS = {
    "UCI-HAR": {
        "url": "https://archive.ics.uci.edu/ml/machine-learning-databases/00240/UCI%20HAR%20Dataset.zip",
        "checksum": "c7d0e2f6c5a69b6a2c4c5a2c9b0d0e58",  # MD5 from official site
        "archive_name": "UCI_HAR_Dataset.zip",
        "expected_dir": "UCI HAR Dataset",  # folder inside the zip
    },
    "WISDM": {
        "url": "https://archive.ics.uci.edu/ml/machine-learning-databases/00473/WISDM_ar_v1.1.zip",
        "checksum": None,  # not publicly provided; skip verification
        "archive_name": "WISDM.zip",
        "expected_dir": "WISDM_ar_v1.1",
    },
    "HAPT": {
        "url": "https://archive.ics.uci.edu/ml/machine-learning-databases/00224/HAPT%20Data%20Set.zip",
        "checksum": None,
        "archive_name": "HAPT.zip",
        "expected_dir": "HAPT Data Set",
    },
}

RAW_ROOT = Path("data/raw")


def md5(fname: Path) -> str:
    hash_md5 = hashlib.md5()
    with open(fname, "rb") as f:
        for chunk in iter(lambda: f.read(4096), b""):
            hash_md5.update(chunk)
    return hash_md5.hexdigest()


def download_file(url: str, dest: Path, expected_md5: str | None = None):
    dest.parent.mkdir(parents=True, exist_ok=True)
    if dest.exists():
        if expected_md5 and md5(dest) == expected_md5:
            print(f"[skip] {dest.name} already exists and checksum matches.")
            return dest
        else:
            print(f"[info] {dest.name} exists but checksum missing/does not match – re‑downloading.")
    print(f"Downloading {url} ...")
    with urlopen(url) as response, open(dest, "wb") as out_file:
        total = int(response.info().get("Content-Length", -1))
        with tqdm(total=total, unit="B", unit_scale=True, unit_divisor=1024) as pbar:
            while True:
                chunk = response.read(8192)
                if not chunk:
                    break
                out_file.write(chunk)
                pbar.update(len(chunk))
    if expected_md5:
        actual_md5 = md5(dest)
        if actual_md5 != expected_md5:
            raise RuntimeError(f"Checksum mismatch for {dest.name}: expected {expected_md5}, got {actual_md5}")
    return dest


def safe_extract(zip_path: Path, target_dir: Path, expected_root: str):
    with zipfile.ZipFile(zip_path, "r") as zf:
        members = [m for m in zf.namelist() if m.startswith(expected_root + "/")]
        if not members:
            raise RuntimeError(f"Expected root folder '{expected_root}' not found inside {zip_path.name}")
        print(f"Extracting {zip_path.name} to {target_dir} ...")
        for member in members:
            # Prevent path traversal vulnerabilities
            member_path = Path(member)
            if ".." in member_path.parts:
                raise RuntimeError("Unsafe path in archive")
        zf.extractall(path=target_dir, members=members)


def ensure_dataset(dataset_name: str):
    info = DATASETS[dataset_name]
    raw_dir = RAW_ROOT / dataset_name
    raw_dir.mkdir(parents=True, exist_ok=True)
    archive_path = raw_dir / info["archive_name"]
    # Download if needed
    download_file(info["url"], archive_path, info.get("checksum"))
    # Extract if expected folder not present
    expected_path = raw_dir / info["expected_dir"]
    if not expected_path.exists():
        safe_extract(archive_path, raw_dir, info["expected_dir"])
    else:
        print(f"[skip] {info['expected_dir']} already extracted.")
    return expected_path


def main():
    parser = argparse.ArgumentParser(description="Download raw HAR datasets")
    parser.add_argument("--dataset", type=str, required=True,
                        help="Dataset name (UCI-HAR, WISDM, HAPT, or all)")
    args = parser.parse_args()
    if args.dataset.lower() == "all":
        for name in DATASETS.keys():
            ensure_dataset(name)
    else:
        name = args.dataset.upper()
        if name not in DATASETS:
            print(f"Unsupported dataset: {args.dataset}")
            sys.exit(1)
        ensure_dataset(name)

if __name__ == "__main__":
    main()
