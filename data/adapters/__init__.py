import importlib
from pathlib import Path
import yaml

def get_adapter(dataset_key: str):
    """Return the adapter class for the given dataset key.
    The dataset key must match an entry in `configs/dataset_registry.yaml`.
    """
    registry_path = Path(__file__).parents[2] / "configs" / "dataset_registry.yaml"
    with open(registry_path) as f:
        registry = yaml.safe_load(f)
    if dataset_key not in registry:
        raise ValueError(f"Dataset '{dataset_key}' not found in registry.")
    # Convention: adapter module name is the dataset key (e.g., 'uci_har')
    module_name = f"data.adapters.{dataset_key}"
    module = importlib.import_module(module_name)
    # All adapters expose a class named <PascalCase>Adapter, e.g., UCIHARAdapter
    class_name = ''.join(part.title() for part in dataset_key.split('_')) + 'Adapter'
    return getattr(module, class_name)()
