import os
import joblib
import numpy as np
from sklearn.ensemble import RandomForestClassifier

class RandomForestModel:
    def __init__(self, n_estimators: int = 200, random_state: int = 42):
        self.clf = RandomForestClassifier(n_estimators=n_estimators, random_state=random_state)
        self.scaler = None

    def fit(self, X: np.ndarray, y: np.ndarray):
        # X shape: (n_samples, n_features)
        self.clf.fit(X, y)
        return self

    def predict(self, X: np.ndarray):
        return self.clf.predict(X)

    def predict_proba(self, X: np.ndarray):
        return self.clf.predict_proba(X)

    def save(self, model_path: str, scaler_path: str = None):
        joblib.dump(self.clf, model_path)
        if scaler_path and self.scaler:
            joblib.dump(self.scaler, scaler_path)
