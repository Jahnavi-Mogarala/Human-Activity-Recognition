# Repository Audit Summary

## Overview
This audit enumerates all top‑level directories and files in the **Human‑Activity‑Recognition** project, classifies each item, and identifies files that should not be tracked.

### Directory Listing
- `backend/` – FastAPI service code.
- `configs/` – YAML configuration files.
- `data/` – Raw and processed dataset files.
- `frontend/` – Android and React front‑ends.
- `ml/` – Model definitions and pipeline utilities.
- `reports/` – Generated experiment reports, plots, and validation documents.
- `scripts/` – Command‑line utilities for data download, preparation, training, evaluation, and validation.
- `tests/` – Pytest suite.

### File Classification
| Path | Type | Tracked? |
|------|------|----------|
| `bilstm_attention.pt` | Model checkpoint (binary) | **Tracked** (via Git LFS) |
| `reports/confusion_matrix.png` | Plot image | **Tracked** |
| `frontend/android/app/build/…` | Android build artefacts | **Ignored** |
| `__pycache__/` | Python bytecode caches | **Ignored** |
| `logs/` | Log files | **Ignored** |
| `har-torch-py311/` | Python runtime / environment | **Ignored** |
| `venv/` | Virtual environment | **Ignored** |

### Unwanted Files Detected
The following untracked paths should remain untracked (added to `.gitignore`):
- `frontend/android/app/build/`
- `frontend/android/.gradle/`
- `frontend/android/.cxx/`
- `frontend/android/local.properties`
- `logs/`
- `har-torch-py311/`
- `venv/`
- `__pycache__/`
- `*.pyc`

The audit is complete.
