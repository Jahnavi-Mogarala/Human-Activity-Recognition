import os
import torch
import numpy as np
from pathlib import Path
import json

# Paths
repo_root = Path(__file__).resolve().parents[1]
model_path = repo_root / 'frontend' / 'android' / 'app' / 'src' / 'main' / 'assets' / 'bilstm_attention.pt'
quant_path = repo_root / 'frontend' / 'android' / 'app' / 'src' / 'main' / 'assets' / 'bilstm_attention_quantized.pt'

report_path = repo_root / 'reports' / 'quantization' / 'quantization_report.md'
report_path.parent.mkdir(parents=True, exist_ok=True)

if not model_path.is_file():
    with open(report_path, 'w') as f:
        f.write('# Quantization Report\n\n')
        f.write('ERROR: Original model file not found. Quantization cannot be performed.\n')
    raise SystemExit('Model not found')

# Load the original TorchScript model (CPU)
try:
    model = torch.jit.load(str(model_path), map_location='cpu')
except Exception as e:
    with open(report_path, 'w') as f:
        f.write('# Quantization Report\n\n')
        f.write(f'ERROR loading original model: {e}\n')
    raise

# Attempt dynamic quantization (supported for LSTM and Linear)
try:
    quantized_model = torch.quantization.quantize_dynamic(
        model,  # the model
        {torch.nn.LSTM, torch.nn.Linear},  # layers to quantize
        dtype=torch.qint8
    )
    # Save quantized model
    torch.jit.save(quantized_model, str(quant_path))
    quant_success = True
    quant_error = ''
except Exception as e:
    quant_success = False
    quant_error = str(e)

# Evaluate quantized model if created
if quant_success:
    # Load test data
    test_npz_path = repo_root / 'data' / 'processed' / 'UCI_HAR' / 'UCI-HAR_test.npz'
    if not test_npz_path.is_file():
        test_npz_path = repo_root / 'data' / 'processed' / 'UCI_HAR' / 'test.npz'
    data = np.load(test_npz_path)
    windows = data['windows']
    labels = data['labels']
    # Simple inference loop
    with torch.no_grad():
        outputs = []
        for w in windows:
            inp = torch.tensor(w, dtype=torch.float32).unsqueeze(0)  # (1,128,6)
            out = quantized_model(inp)
            pred = out.argmax(dim=1).item()
            outputs.append(pred)
    # Compute accuracy
    acc = (np.array(outputs) == labels).mean()
    # Placeholder for macro F1 (requires sklearn, avoid heavy deps)
    macro_f1 = 'N/A (not computed)'
else:
    acc = None
    macro_f1 = None

# Write report
with open(report_path, 'w') as f:
    f.write('# Quantization Report\n\n')
    if quant_success:
        f.write('Quantization succeeded.\n')
        f.write(f'Original model size: {model_path.stat().st_size/1024:.1f} KB\n')
        f.write(f'Quantized model size: {quant_path.stat().st_size/1024:.1f} KB\n')
        f.write(f'Accuracy on test set (quantized): {acc:.4f}\n')
        f.write(f'Macro F1 (quantized): {macro_f1}\n')
    else:
        f.write('Quantization not feasible with current model/runtime.\n')
        f.write(f'Error: {quant_error}\n')
