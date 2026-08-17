import torch
import torch.nn as nn

class BilstmAttentionModel(nn.Module):
    """Bi‑LSTM with temporal attention.

    Args:
        input_dim (int): Number of input channels (sensor axes).
        hidden_size (int): Hidden size for each LSTM direction.
        num_layers (int): Number of stacked LSTM layers.
        num_classes (int): Number of activity classes.
        dropout (float): Dropout probability applied after LSTM.
        attention_dim (int): Dimension of the attention linear projection.
    """
    def __init__(self, input_dim: int, hidden_size: int = 128, num_layers: int = 2,
                 num_classes: int = 6, dropout: float = 0.3, attention_dim: int = 64):
        super().__init__()
        self.lstm = nn.LSTM(input_dim, hidden_size, num_layers=num_layers,
                            batch_first=True, dropout=dropout, bidirectional=True)
        # Attention: compute weights over the time dimension
        self.attn_proj = nn.Linear(hidden_size * 2, attention_dim)
        self.attn_score = nn.Linear(attention_dim, 1, bias=False)
        self.classifier = nn.Linear(hidden_size * 2, num_classes)
        self.dropout = nn.Dropout(dropout)

    def forward(self, x):
        # x: (batch, seq_len, input_dim)
        lstm_out, _ = self.lstm(x)  # (batch, seq_len, hidden*2)
        # Compute attention weights
        proj = torch.tanh(self.attn_proj(lstm_out))  # (batch, seq_len, attention_dim)
        scores = self.attn_score(proj).squeeze(-1)   # (batch, seq_len)
        attn_weights = torch.softmax(scores, dim=1).unsqueeze(-1)  # (batch, seq_len, 1)
        # Weighted sum of LSTM outputs
        context = torch.sum(lstm_out * attn_weights, dim=1)  # (batch, hidden*2)
        context = self.dropout(context)
        logits = self.classifier(context)
        return logits
