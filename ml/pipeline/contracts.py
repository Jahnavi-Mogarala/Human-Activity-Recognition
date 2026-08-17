from dataclasses import dataclass
from typing import Optional, List
import pandas as pd

@dataclass
class CanonicalSample:
    timestamp: Optional[float]
    subject_id: int
    session_id: Optional[int]
    dataset: str
    activity: str
    device_type: str
    sensor_location: str
    sampling_rate: int
    acc_x: Optional[float] = None
    acc_y: Optional[float] = None
    acc_z: Optional[float] = None
    gyro_x: Optional[float] = None
    gyro_y: Optional[float] = None
    gyro_z: Optional[float] = None
    mag_x: Optional[float] = None
    mag_y: Optional[float] = None
    mag_z: Optional[float] = None
    heart_rate: Optional[float] = None

def to_dataframe(samples: List[CanonicalSample]) -> pd.DataFrame:
    """Convert a list of CanonicalSample objects into a pandas DataFrame."""
    return pd.DataFrame([s.__dict__ for s in samples])
