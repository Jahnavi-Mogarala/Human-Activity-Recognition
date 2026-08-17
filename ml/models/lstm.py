import torch
import torch.nn as nn

class LSTMModel(nn.Module):
    """Simple LSTM classifier for windowed sensor data.
    Expects input shape (batch, seq_len, n_features).
    """
    def __init__(self, input_dim: int, hidden_dim: int = 128, num_layers: int = 2, num_classes: int = 6):
        super().__init__()
        self.lstm = nn.LSTM(input_dim, hidden_dim, num_layers, batch_first=True)
        self.fc = nn.Linear(hidden_dim, num_classes)

    def forward(self, x):
        # x: (batch, seq_len, input_dim)
        _, (hn, _) = self.lstm(x)
        # Take last hidden state
        out = self.fc(hn[-1])
        return out

# Export name expected by factory
class LstmModel(LSTMModel):
    pass
