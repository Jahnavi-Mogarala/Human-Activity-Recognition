# inspect_scaler.py
import pickle, sys, json, pathlib
p = pathlib.Path(sys.argv[1])
with p.open('rb') as f:
    obj = pickle.load(f)
print('type:', type(obj))
try:
    print('keys:', list(obj.keys()))
except Exception:
    pass
print('repr:', repr(obj)[:200])
