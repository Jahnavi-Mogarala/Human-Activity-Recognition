import React, { useState } from 'react';

function PredictionForm() {
  const [windowData, setWindowData] = useState('');
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setResult(null);
    let payload;
    try {
      payload = JSON.parse(windowData);
    } catch (e) {
      setError('Invalid JSON');
      return;
    }
    try {
      const response = await fetch('http://localhost:8000/predict', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ window: payload })
      });
      const json = await response.json();
      setResult(json);
    } catch (e) {
      setError('Request failed');
    }
  };

  return (
    <section id="prediction-form">
      <h2>Predict Activity</h2>
      <form onSubmit={handleSubmit}>
        <textarea
          rows={10}
          cols={60}
          placeholder="Enter a JSON array of shape [128,6]"
          value={windowData}
          onChange={(e) => setWindowData(e.target.value)}
        />
        <br />
        <button type="submit">Predict</button>
      </form>
      {error && <p style={{ color: 'red' }}>{error}</p>}
      {result && (
        <pre>{JSON.stringify(result, null, 2)}</pre>
      )}
    </section>
  );
}

export default PredictionForm;
