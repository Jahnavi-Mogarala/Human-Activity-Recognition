#!/usr/bin/env bash
set -e
python scripts/evaluate.py \
    --config configs/experiments/full_train.yaml \
    --output_dir reports  $@
