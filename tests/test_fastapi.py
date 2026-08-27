# tests/test_fastapi.py
"""FastAPI endpoint tests.
The project defines a minimal FastAPI app in `backend/main.py` with a health check.
We verify that the app loads and the health endpoint responds with status OK.
"""

import sys, os
from pathlib import Path
import pytest
from fastapi.testclient import TestClient

# Add repository root to PYTHONPATH
repo_root = Path(__file__).resolve().parents[1]
sys.path.append(str(repo_root))

# Import the FastAPI app – the actual location is backend/main.py
from backend.main import app

def test_health_endpoint():
    client = TestClient(app)
    response = client.get("/api/health")
    assert response.status_code == 200
    json_data = response.json()
    assert json_data.get("status") == "ok"
