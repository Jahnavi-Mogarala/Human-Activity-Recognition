package com.example.app;

import android.app.Application;
import android.os.SystemClock;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.app.database.SessionEntity;
import com.example.app.repository.SessionRepository;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * ViewModel that holds all live data required for the Live Motion Monitor dashboard.
 * It is scoped to the Activity so it survives fragment recreation and orientation changes.
 */
public class LiveMotionViewModel extends AndroidViewModel {
    // Prediction data
    private final MutableLiveData<Integer> activityIndex = new MutableLiveData<>();
    private final MutableLiveData<float[]> rawScores = new MutableLiveData<>();
    private final MutableLiveData<float[]> smoothedScores = new MutableLiveData<>();
    private final MutableLiveData<Long> latencyNs = new MutableLiveData<>();

    // Session stats
    private final MutableLiveData<Integer> predictionCount = new MutableLiveData<>();
    private final MutableLiveData<Long> sessionStartMs = new MutableLiveData<>();
    private final MutableLiveData<Long> sessionDurationMs = new MutableLiveData<>();
    private final MutableLiveData<Float> avgConfidence = new MutableLiveData<>();

    // Sensor rates
    private final MutableLiveData<Double> accelHz = new MutableLiveData<>();
    private final MutableLiveData<Double> gyroHz = new MutableLiveData<>();

    // Window progress (0-128)
    private final MutableLiveData<Integer> windowSize = new MutableLiveData<>();

    // Session state strings (READY, COLLECTING, INFERENCE_ACTIVE, PAUSED, STOPPED, ERROR)
    private final MutableLiveData<String> sessionStatus = new MutableLiveData<>();

    // Movement status heuristic (Active / Low movement)
    private final MutableLiveData<String> movementStatus = new MutableLiveData<>();

    // Safety event counters – optional, may remain null if modules are absent
    private final MutableLiveData<Integer> fallCount = new MutableLiveData<>();
    private final MutableLiveData<Integer> sedentaryCount = new MutableLiveData<>();
    private final MutableLiveData<Integer> anomalyCount = new MutableLiveData<>();

    // Telemetry buffers for charts – each entry holds timestamp (ms) and three axis values
    private final Deque<float[]> accelBuffer = new ArrayDeque<>(); // [timestamp, x, y, z]
    private final Deque<float[]> gyroBuffer = new ArrayDeque<>(); // [timestamp, x, y, z]
    private static final long CHART_WINDOW_MS = 5_000L; // 5‑second rolling buffer

    // Buffer for smoothing raw scores (last 5 predictions)
    private final Deque<float[]> rawScoreHistory = new ArrayDeque<>();
    private static final int SMOOTH_WINDOW = 5;

    // Average latency LiveData (computed on demand)
    private final MutableLiveData<Long> avgLatencyNs = new MutableLiveData<>();
    // Activity history placeholder (no persistence)
    private final MutableLiveData<java.util.List<String>> activityHistory = new MutableLiveData<>(new java.util.ArrayList<>());

    // Latency sample count for averaging
    private int latencySampleCount = 0;

    // Repository for persisting completed sessions
    private final SessionRepository sessionRepository;

    // Expose persistent sessions to UI
    private final LiveData<java.util.List<SessionEntity>> allSessions;

    // Guard against duplicate saves per session
    private boolean sessionSaved = false;

    // Database error messages
    private final MutableLiveData<String> dbError = new MutableLiveData<>();

    /**
     * Constructor initializes all LiveData with deterministic default values and creates the repository.
     */
    public LiveMotionViewModel(Application application) {
        super(application);
        // Initialise defaults
        activityIndex.setValue(-1);
        rawScores.setValue(null);
        smoothedScores.setValue(null);
        latencyNs.setValue(0L);
        avgLatencyNs.setValue(0L);
        predictionCount.setValue(0);
        sessionStartMs.setValue(0L);
        sessionDurationMs.setValue(0L);
        avgConfidence.setValue(0f);
        accelHz.setValue(-1.0);
        gyroHz.setValue(-1.0);
        windowSize.setValue(0);
        sessionStatus.setValue("READY");
        movementStatus.setValue("--");
        fallCount.setValue(null);
        sedentaryCount.setValue(null);
        anomalyCount.setValue(null);
        dbError.setValue(null);

        // Initialise repository and LiveData list
        sessionRepository = new SessionRepository(application);
        allSessions = sessionRepository.getAllSessions();
    }

    // --- Existing update methods ---------------------------------------------------
    public void setActivityPrediction(int idx, float[] scores, long latency) {
        activityIndex.setValue(idx);
        rawScores.setValue(scores);
        // Update smoothing buffer
        if (rawScoreHistory.size() >= SMOOTH_WINDOW) {
            rawScoreHistory.removeFirst();
        }
        rawScoreHistory.addLast(scores.clone());
        // Compute smoothed scores as element‑wise average
        float[] smoothed = new float[scores.length];
        for (float[] s : rawScoreHistory) {
            for (int i = 0; i < s.length; i++) {
                smoothed[i] += s[i];
            }
        }
        int n = rawScoreHistory.size();
        for (int i = 0; i < smoothed.length; i++) {
            smoothed[i] = smoothed[i] / n;
        }
        smoothedScores.setValue(smoothed);
        latencyNs.setValue(latency);

        // Update prediction count and average confidence
        int count = predictionCount.getValue() != null ? predictionCount.getValue() : 0;
        count++;
        predictionCount.setValue(count);
        float max = 0f;
        for (float s : scores) if (s > max) max = s;
        float avg = avgConfidence.getValue() != null ? avgConfidence.getValue() : 0f;
        avg = ((avg * (count - 1)) + max) / count;
        avgConfidence.setValue(avg);

        // Record session start time on first prediction
        if (sessionStartMs.getValue() != null && sessionStartMs.getValue() == 0 && isRunningOnDevice()) {
            sessionStartMs.setValue(SystemClock.elapsedRealtime());
        }
        // Compute duration if start time known
        Long startMs = sessionStartMs.getValue();
        if (startMs != null && startMs > 0 && isRunningOnDevice()) {
            long duration = SystemClock.elapsedRealtime() - startMs;
            sessionDurationMs.setValue(duration);
        }
    }

    public void setSmoothedScores(float[] scores) { smoothedScores.setValue(scores); }
    public void setWindowSize(int size) { windowSize.setValue(size); }
    public void setSessionStatus(String status) { sessionStatus.setValue(status); }
    public void setSensorRates(double accel, double gyro) { accelHz.setValue(accel); gyroHz.setValue(gyro); }

    public void addAccelSample(long timestampNs, float x, float y, float z) {
        pruneOld(accelBuffer, timestampNs);
        accelBuffer.addLast(new float[]{timestampNs / 1_000_000f, x, y, z});
        updateMovementStatus();
    }

    public void addGyroSample(long timestampNs, float x, float y, float z) {
        pruneOld(gyroBuffer, timestampNs);
        gyroBuffer.addLast(new float[]{timestampNs / 1_000_000f, x, y, z});
    }

    private void pruneOld(Deque<float[]> buffer, long nowNs) {
        long cutoff = nowNs - CHART_WINDOW_MS * 1_000_000L;
        while (!buffer.isEmpty() && buffer.peekFirst()[0] * 1_000_000L < cutoff) {
            buffer.pollFirst();
        }
    }

    public Deque<float[]> getAccelBuffer() { return new ArrayDeque<>(accelBuffer); }
    public Deque<float[]> getGyroBuffer() { return new ArrayDeque<>(gyroBuffer); }

    // Expose LiveData getters -------------------------------------------------------
    public LiveData<Integer> getActivityIndex() { return activityIndex; }
    public LiveData<float[]> getRawScores() { return rawScores; }
    public LiveData<float[]> getSmoothedScores() { return smoothedScores; }
    public LiveData<Long> getLatencyNs() { return latencyNs; }
    public LiveData<Integer> getPredictionCount() { return predictionCount; }
    public LiveData<Long> getSessionDurationMs() { return sessionDurationMs; }
    public LiveData<Float> getAvgConfidence() { return avgConfidence; }
    public LiveData<Double> getAccelHz() { return accelHz; }
    public LiveData<Double> getGyroHz() { return gyroHz; }
    public LiveData<Integer> getWindowSize() { return windowSize; }
    public LiveData<String> getSessionStatus() { return sessionStatus; }
    public LiveData<String> getMovementStatus() { return movementStatus; }
    public LiveData<Integer> getFallCount() { return fallCount; }
    public LiveData<Integer> getSedentaryCount() { return sedentaryCount; }
    public LiveData<Integer> getAnomalyCount() { return anomalyCount; }
    public LiveData<Long> getAvgLatencyNs() { return avgLatencyNs; }
    public LiveData<java.util.List<String>> getActivityHistory() { return activityHistory; }
    public LiveData<java.util.List<SessionEntity>> getAllSessions() { return allSessions; }
    public LiveData<String> getDbError() { return dbError; }

    // Update movement status based on accelerometer variance
    public void updateMovementStatus() {
        if (accelBuffer.isEmpty()) {
            movementStatus.setValue("--");
            return;
        }
        double sum = 0.0, sumSq = 0.0;
        int n = 0;
        for (float[] sample : accelBuffer) {
            double mag = Math.sqrt(sample[1] * sample[1] + sample[2] * sample[2] + sample[3] * sample[3]);
            sum += mag;
            sumSq += mag * mag;
            n++;
        }
        double mean = sum / n;
        double variance = (sumSq / n) - (mean * mean);
        double threshold = 0.05;
        movementStatus.setValue(variance < threshold ? "Low movement" : "Moving");
    }

    // Record latency for avg calculation
    public void addLatency(long latency) {
        latencySampleCount++;
        if (avgLatencyNs.getValue() == null) {
            avgLatencyNs.setValue(latency);
        } else {
            long total = avgLatencyNs.getValue() * (latencySampleCount - 1) + latency;
            avgLatencyNs.setValue(total / latencySampleCount);
        }
    }

    // Reset all ViewModel state ------------------------------------------------------
    public void resetAll() {
        activityIndex.setValue(-1);
        rawScores.setValue(null);
        smoothedScores.setValue(null);
        latencyNs.setValue(0L);
        avgLatencyNs.setValue(0L);
        predictionCount.setValue(0);
        sessionStartMs.setValue(0L);
        sessionDurationMs.setValue(0L);
        avgConfidence.setValue(0f);
        accelHz.setValue(-1.0);
        gyroHz.setValue(-1.0);
        windowSize.setValue(0);
        sessionStatus.setValue("READY");
        movementStatus.setValue("--");
        fallCount.setValue(null);
        sedentaryCount.setValue(null);
        anomalyCount.setValue(null);
        accelBuffer.clear();
        gyroBuffer.clear();
        activityHistory.setValue(new java.util.ArrayList<>());
        latencySampleCount = 0;
        sessionSaved = false;
    }

    // Save completed session to Room -------------------------------------------------
    public void saveCompletedSession() {
        if (sessionSaved) return; // guard duplicate
        Long duration = sessionDurationMs.getValue();
        Integer predCount = predictionCount.getValue();
        Float confidence = avgConfidence.getValue();
        Long avgLat = avgLatencyNs.getValue();
        Integer idx = activityIndex.getValue();
        String dominant = (idx != null && idx >= 0) ? getHumanActivityName(idx) : "Unknown";
        long timestamp = System.currentTimeMillis();
        if (duration == null) duration = 0L;
        if (predCount == null) predCount = 0;
        if (confidence == null) confidence = 0f;
        if (avgLat == null) avgLat = 0L;
        float latencyMs = avgLat / 1_000_000f;
        SessionEntity session = new SessionEntity(timestamp, duration, dominant, confidence, predCount, latencyMs);
        try {
            sessionRepository.insert(session);
            sessionSaved = true;
        } catch (Exception e) {
            dbError.postValue("Room save error: " + e.getMessage());
        }
    }

    private String getHumanActivityName(int classIdx) {
        switch (classIdx) {
            case 0: return "Walking";
            case 1: return "Walking Upstairs";
            case 2: return "Walking Downstairs";
            case 3: return "Sitting";
            case 4: return "Standing";
            case 5: return "Laying";
            default: return "Unknown";
        }
    }

    protected boolean isRunningOnDevice() {
        return android.os.Build.VERSION.SDK_INT >= 1;
    }
}
