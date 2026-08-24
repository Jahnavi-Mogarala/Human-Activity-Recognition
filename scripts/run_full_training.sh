#!/usr/bin/env bash
set -e
python scripts/train.py --config configs/experiments/full_train.yaml --output_dir models/experiments/full_run "$@"
