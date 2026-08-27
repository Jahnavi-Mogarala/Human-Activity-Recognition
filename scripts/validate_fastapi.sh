#!/usr/bin/env bash
# Simple curl test for the /predict endpoint
curl -X POST http://127.0.0.1:8000/predict \
  -H "Content-Type: application/json" \
  -d '{"window": [[0,0,0,0,0,0]]}'
