# Quantization Report

**Result:** Quantization not feasible with the current model/runtime.

The attempted dynamic quantization using `torch.quantization.quantize_dynamic` raised deprecation warnings and cannot be applied to the existing TorchScript model (`bilstm_attention.pt`). No quantized model file was generated, and the original full‑precision model remains unchanged.

*Future work:* To enable quantization, the model would need to be exported with quantization‑aware training or converted using PT2‑E APIs that produce a TorchScript model compatible with Android Lite.
