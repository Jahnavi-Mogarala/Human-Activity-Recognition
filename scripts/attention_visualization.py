import os
import json
import numpy as np
import torch
import matplotlib.pyplot as plt
import seaborn as sns
from pathlib import Path

# Paths
BASE_DIR = Path(__file__).resolve().parents[1]
CONFIG_PATH = BASE_DIR / 'models' / 'smartphone_har' / 'config.yaml'
CHECKPOINT_PATH = BASE_DIR / 'models' / 'smartphone_har' / 'best_checkpoint.pth'
TEST_DATA_PATH = BASE_DIR / 'data' / 'processed' / 'UCI_HAR' / 'UCI-HAR_test.npz'
OUTPUT_DIR = BASE_DIR / 'reports' / 'attention'
OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

# Load config (simple yaml parsing)
import yaml
with open(CONFIG_PATH, 'r') as f:
    cfg = yaml.safe_load(f)

# Model definition import
from ml.models.bilstm_attention import BilstmAttentionModel

# Instantiate model with same hyper‑parameters as training
model = BilstmAttentionModel(
    input_dim=6,
    hidden_size=cfg.get('hidden_size', 128),
    num_layers=cfg.get('num_layers', 2),
    num_classes=cfg.get('num_classes', 6),
    dropout=cfg.get('dropout', 0.3),
    attention_dim=cfg.get('attention_dim', 64),
)
model.load_state_dict(torch.load(CHECKPOINT_PATH, map_location='cpu'))
model.eval()

# Load test data
npz = np.load(TEST_DATA_PATH)
X_test = npz['X']  # shape (N, 128, 6)
y_test = npz['y']  # shape (N,)

# Mapping from internal 0‑based to external 1‑based label
label_map = {i: i+1 for i in range(6)}
activity_names = {
    1: 'Walking',
    2: 'Walking Upstairs',
    3: 'Walking Downstairs',
    4: 'Sitting',
    5: 'Standing',
    6: 'Lying'
}

# Collect up to two examples per activity for visualization
examples_per_class = {c: [] for c in range(6)}
for idx, (sample, lbl) in enumerate(zip(X_test, y_test)):
    cls = int(lbl) - 1  # internal label 0‑5
    if len(examples_per_class[cls]) < 2:
        examples_per_class[cls].append((idx, sample))
    if all(len(v) == 2 for v in examples_per_class.values()):
        break

visualization_count = 0
for cls, examples in examples_per_class.items():
    for ex_idx, (sample_idx, sample) in enumerate(examples):
        # Model expects torch tensor of shape (1, seq_len, input_dim)
        inp = torch.from_numpy(sample).float().unsqueeze(0)
        with torch.no_grad():
            att_weights = model.get_attention(inp)  # (1, 128)
        att_weights = att_weights.squeeze(0).cpu().numpy()
        # Verify sum ≈ 1
        weight_sum = att_weights.sum()
        # Plot
        plt.figure(figsize=(8, 3))
        sns.lineplot(x=np.arange(1, 129), y=att_weights, marker='o')
        plt.title(f"Attention over time – Activity: {activity_names[label_map[cls]]} (sample {sample_idx})")
        plt.xlabel('Time step (128‑window)')
        plt.ylabel('Attention weight')
        plt.ylim(0, att_weights.max()*1.1)
        plt.tight_layout()
        out_path = OUTPUT_DIR / f"attention_{activity_names[label_map[cls]].replace(' ', '_').lower()}_sample{ex_idx+1}.png"
        plt.savefig(out_path)
        plt.close()
        # Write a tiny JSON record for verification (optional)
        json_path = OUTPUT_DIR / f"metadata_{activity_names[label_map[cls]].replace(' ', '_').lower()}_sample{ex_idx+1}.json"
        with open(json_path, 'w') as jf:
            json.dump({
                "sample_index": int(sample_idx),
                "internal_label": int(cls),
                "external_label": int(label_map[cls]),
                "activity_name": activity_names[label_map[cls]],
                "attention_sum": float(weight_sum)
            }, jf, indent=2)
        visualization_count += 1

print(f"Generated {visualization_count} attention visualizations in {OUTPUT_DIR}")
