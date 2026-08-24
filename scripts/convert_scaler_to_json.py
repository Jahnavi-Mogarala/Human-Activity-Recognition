# scripts/convert_scaler_to_json.py
"""Convert the sklearn StandardScaler (saved as pickle) to a simple JSON file.
The JSON will contain two arrays: "mean" and "std" – exactly what the Android
code needs for normalisation.
"""
import pickle
import json
import argparse
import os

def main():
    parser = argparse.ArgumentParser(description='Convert scaler pickle to JSON')
    parser.add_argument('--input', type=str, required=True, help='Path to scaler.pkl')
    parser.add_argument('--output', type=str, default='scaler.json', help='Output JSON file')
    args = parser.parse_args()

    # Try regular pickle first, fallback to joblib if needed
    try:
        with open(args.input, 'rb') as f:
            scaler = pickle.load(f)
    except Exception:
        try:
            import joblib
            scaler = joblib.load(args.input)
        except Exception as e:
            raise RuntimeError(f"Failed to load scaler with pickle or joblib: {e}")

    # scaler is expected to be a sklearn StandardScaler
    data = {
        'mean': scaler.mean_.tolist(),
        'std':  scaler.scale_.tolist()
    }
    with open(args.output, 'w') as f:
        json.dump(data, f, indent=2)
    print(f'JSON scaler saved to {args.output}')

if __name__ == '__main__':
    main()
