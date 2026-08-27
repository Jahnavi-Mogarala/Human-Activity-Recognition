#!/usr/bin/env bash
# Run the FastAPI server for the Human Activity Recognition backend
# Requires uvicorn and the backend package to be installed.
# Usage: ./run_fastapi.sh [host] [port]
# Default host 0.0.0.0 and port 8000

HOST=${1:-0.0.0.0}
PORT=${2:-8000}

uvicorn backend.main:app --host $HOST --port $PORT

# Run the FastAPI server for the Human Activity Recognition backend
# Requires uvicorn and the backend package to be installed.
# Usage: ./run_fastapi.sh [host] [port]
# Default host 0.0.0.0 and port 8000

HOST=
PORT=

uvicorn backend.main:app --host System.Management.Automation.Internal.Host.InternalHost --port 
