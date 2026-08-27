#!/usr/bin/env bash
# Test the FastAPI /predict endpoint with a dummy 128x6 window
# Usage: ./test_predict.sh [host] [port]
# Default host localhost and port 8000

HOST=${1:-localhost}
PORT=${2:-8000}

# Create a dummy JSON payload (128 timesteps, 6 channels) filled with zeros
PAYLOAD=$(python - <<'PY'
import json, numpy as np
window = np.zeros((128,6)).tolist()
print(json.dumps({"window": window}))
PY
)

curl -s -X POST "http://${HOST}:${PORT}/predict" \
     -H "Content-Type: application/json" \
     -d "$PAYLOAD"
