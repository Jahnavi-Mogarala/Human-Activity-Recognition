import importlib
from typing import Any

def get_model(name: str, **kwargs: Any):
    """Factory to instantiate a model by name.
    Supported names: 'random_forest', 'lstm', 'bilstm', 'bilstm_attention'.
    Additional keyword arguments are passed to the model constructor.
    """
    module_name = f"ml.models.{name}"
    try:
        module = importlib.import_module(module_name)
    except ImportError as e:
        raise ValueError(f"Model '{name}' is not available.") from e
    # Expect each module to expose a class with PascalCase name
    class_name = ''.join(part.title() for part in name.split('_')) + 'Model'
    if hasattr(module, class_name):
        return getattr(module, class_name)(**kwargs)
    else:
        raise AttributeError(f"Module '{module_name}' does not define '{class_name}'.")
