import torch
import torch.nn as nn

class BiLSTMModel(nn.Module):
    """Bidirectional LSTM classifier.
    Input shape: (batch, seq_len, input_dim).
    """
    def __init__(self, input_dim: int, hidden_dim: int = 128, num_layers: int = 2, num_classes: int = 6):
        super().__init__()
        self.lstm = nn.LSTM(input_dim, hidden_dim, num_layers, batch_first=True, bidirectional=True)
        # Hidden dim is doubled because of bidirectionality
        self.fc = nn.Linear(hidden_dim * 2, num_classes)

    def forward(self, x):
        _, (hn, _) = self.lstm(x)  # hn shape: (num_layers*2, batch, hidden_dim)
        # Concatenate last hidden states from both directions
        # Take the last layer's forward and backward hidden states
        forward_last = hn[-2]
        backward_last = hn[-1]
        concat = torch.cat([forward_last, backward_last], dim=1)
        out = self.fc(concat)
        return out

# Export name expected by factory
class BilstmModel(BiLSTMModel):
    pass
