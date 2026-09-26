package com.example.app;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.app.database.SessionEntity;
import com.example.app.repository.SessionRepository;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

public class LiveMotionViewModel extends AndroidViewModel {
    private static final String TAG = "LiveMotionViewModel";

    private final MutableLiveData<Integer> activityIndex = new MutableLiveData<>(-1);
    private final MutableLiveData<float[]> rawScores = new MutableLiveData<>();
    private final MutableLiveData<float[]> smoothedScores = new MutableLiveData<>();
    private final MutableLiveData<Long> latencyNs = new MutableLiveData<>(0L);

    private final MutableLiveData<Integer> predictionCount = new MutableLiveData<>(0);
    private final MutableLiveData<Long> sessionStartMs = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> sessionDurationMs = new MutableLiveData<>(0L);
    private final MutableLiveData<Float> avgConfidence = new MutableLiveData<>(0f);

    private final MutableLiveData<Double> accelHz = new MutableLiveData<>(-1.0);
    private final MutableLiveData<Double> gyroHz = new MutableLiveData<>(-1.0);
    private final MutableLiveData<Integer> windowSize = new MutableLiveData<>(0);
    private final MutableLiveData<String> sessionStatus = new MutableLiveData<>("READY");
    private final MutableLiveData<String> movementStatus = new MutableLiveData<>("--");

    private final MutableLiveData<Boolean> isSessionRunning = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isSessionPaused = new MutableLiveData<>(false);

    private long startTimeWallMs = 0L;
    private long pausedDurationMs = 0L;
    private long pauseStartTimeMs = 0L;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            Boolean running = isSessionRunning.getValue();
            Boolean paused = isSessionPaused.getValue();
            if (running != null && running && (paused == null || !paused) && startTimeWallMs > 0) {
                long elapsed = System.currentTimeMillis() - startTimeWallMs - pausedDurationMs;
                sessionDurationMs.postValue(Math.max(0L, elapsed));
                timerHandler.postDelayed(this, 1000);
            }
        }
    };

    private final Deque<float[]> accelBuffer = new ArrayDeque<>();
    private final Deque<float[]> gyroBuffer = new ArrayDeque<>();
    private static final long CHART_WINDOW_MS = 5_000L;

    // Temporal prediction smoothing & stabilization
    private final float[] smoothedProbs = new float[6];
    private static final float ALPHA = 0.35f;
    private int lastStablePrediction = -1;
    private int candidatePrediction = -1;
    private int candidateCount = 0;

    private final MutableLiveData<Long> avgLatencyNs = new MutableLiveData<>(0L);
    private final MutableLiveData<List<String>> activityHistory = new MutableLiveData<>(new ArrayList<>());
    private int latencySampleCount = 0;

    // Fall Detection
    public final MutableLiveData<Boolean> isFallDetected = new MutableLiveData<>(false);
    private int fallState = 0; // 0=NORMAL, 1=FREEFALL/LOW-G, 2=IMPACT, 3=INACTIVITY
    private long fallStateChangedMs = 0L;
    private static final double G_FREEFALL_THRESHOLD = 0.4;
    private static final double G_IMPACT_THRESHOLD = 2.5;
    private static final double G_INACTIVITY_THRESHOLD = 0.15;
    private static final long FALL_TIMEOUT_MS = 2000L;
    private static final long INACTIVITY_WAIT_MS = 2000L;

    private final SessionRepository sessionRepository;
    private final LiveData<List<SessionEntity>> allSessions;
    private boolean sessionSaved = false;
    private final MutableLiveData<String> dbError = new MutableLiveData<>(null);

    public LiveMotionViewModel(Application application) {
        super(application);
        sessionRepository = new SessionRepository(application);
        allSessions = sessionRepository.getAllSessions();
    }

    public void startSession() {
        Boolean running = isSessionRunning.getValue();
        Boolean paused = isSessionPaused.getValue();

        if (running != null && running && paused != null && paused) {
            // Resume paused session
            isSessionPaused.setValue(false);
            if (pauseStartTimeMs > 0) {
                pausedDurationMs += (System.currentTimeMillis() - pauseStartTimeMs);
                pauseStartTimeMs = 0L;
            }
            sessionStatus.setValue("COLLECTING");
        } else if (running == null || !running) {
            // Fresh start
            isSessionRunning.setValue(true);
            isSessionPaused.setValue(false);
            startTimeWallMs = System.currentTimeMillis();
            pausedDurationMs = 0L;
            pauseStartTimeMs = 0L;
            sessionSaved = false;
            sessionStatus.setValue("COLLECTING");
        }
        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.post(timerRunnable);
    }

    public void pauseSession() {
        Boolean running = isSessionRunning.getValue();
        Boolean paused = isSessionPaused.getValue();
        if (running == null || !running) return;

        if (paused != null && paused) {
            // Unpause
            startSession();
        } else {
            // Pause
            isSessionPaused.setValue(true);
            pauseStartTimeMs = System.currentTimeMillis();
            sessionStatus.setValue("PAUSED");
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    public void stopAndSaveSession() {
        timerHandler.removeCallbacks(timerRunnable);
        isSessionRunning.setValue(false);
        isSessionPaused.setValue(false);
        sessionStatus.setValue("STOPPED");
        saveCompletedSession();
    }

    public void setActivityPrediction(int rawClassIdx, float[] scores, long latency) {
        if (scores == null || scores.length == 0) return;

        // 1. Unconditional softmax over raw PyTorch logits
        float[] currentProbs = softmax(scores);

        // 2. Exponential Moving Average (EMA) smoothing across consecutive windows
        for (int i = 0; i < 6 && i < currentProbs.length; i++) {
            smoothedProbs[i] = ALPHA * currentProbs[i] + (1.0f - ALPHA) * smoothedProbs[i];
        }

        // 3. Find argmax class of smoothed probabilities
        int smoothedClassIdx = 0;
        for (int i = 1; i < 6; i++) {
            if (smoothedProbs[i] > smoothedProbs[smoothedClassIdx]) {
                smoothedClassIdx = i;
            }
        }
        
        // Log diagnostic to prediction_debug.txt
        try {
            java.io.File debugFile = new java.io.File(getApplication().getExternalFilesDir(null), "prediction_debug.txt");
            if (debugFile.length() > 5 * 1024 * 1024) debugFile.delete(); // Rotate if > 5MB
            try (java.io.FileWriter fw = new java.io.FileWriter(debugFile, true)) {
                float conf = smoothedProbs[smoothedClassIdx] * 100f;
                String logLine = String.format(Locale.US, "%d, %.4f, %.4f, %.4f, %.4f, %.4f, %.4f, %s, %.1f%%\n",
                        System.currentTimeMillis(), smoothedProbs[0], smoothedProbs[1], smoothedProbs[2],
                        smoothedProbs[3], smoothedProbs[4], smoothedProbs[5],
                        ActivityLabels.getLabel(smoothedClassIdx), conf);
                fw.write(logLine);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to write prediction debug log", e);
        }

        // 4. Temporal prediction stabilization
        if (lastStablePrediction == -1) {
            lastStablePrediction = smoothedClassIdx;
        } else if (smoothedClassIdx != lastStablePrediction) {
            if (smoothedClassIdx == candidatePrediction) {
                candidateCount++;
                if (candidateCount >= 2 || smoothedProbs[smoothedClassIdx] > (smoothedProbs[lastStablePrediction] + 0.15f)) {
                    lastStablePrediction = smoothedClassIdx;
                    candidateCount = 0;
                }
            } else {
                candidatePrediction = smoothedClassIdx;
                candidateCount = 1;
            }
        } else {
            candidateCount = 0;
        }

        int finalClassIdx = lastStablePrediction;
        activityIndex.postValue(finalClassIdx);
        rawScores.postValue(scores);
        smoothedScores.postValue(smoothedProbs.clone());
        latencyNs.postValue(latency);
        addLatency(latency);

        int count = predictionCount.getValue() != null ? predictionCount.getValue() : 0;
        count++;
        predictionCount.postValue(count);

        float currentPercent = Math.min(100.0f, Math.max(0.0f, smoothedProbs[finalClassIdx] * 100.0f));
        float avg = avgConfidence.getValue() != null ? avgConfidence.getValue() : 0f;
        if (avg == 0f) {
            avg = currentPercent;
        } else {
            avg = ((avg * (count - 1)) + currentPercent) / count;
        }
        avgConfidence.postValue(avg);

        // Append to timeline log
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String timeStr = sdf.format(new Date());
        String eventStr = ActivityLabels.getLabel(finalClassIdx) + " (" + Math.round(currentPercent) + "%) — " + timeStr;

        List<String> currentHistory = activityHistory.getValue();
        if (currentHistory == null) currentHistory = new ArrayList<>();
        List<String> updated = new ArrayList<>(currentHistory);
        updated.add(0, eventStr);
        if (updated.size() > 10) updated.remove(updated.size() - 1);
        activityHistory.postValue(updated);
    }

    public void setWindowSize(int size) { windowSize.postValue(size); }
    public void setSessionStatus(String status) { sessionStatus.postValue(status); }

    public void addAccelSample(long timestampNs, float x, float y, float z) {
        pruneOld(accelBuffer, timestampNs);
        accelBuffer.addLast(new float[]{timestampNs / 1_000_000f, x, y, z});
        updateMovementStatus();
        processFallDetection(x, y, z);
    }

    private void processFallDetection(float x, float y, float z) {
        Boolean fallAlert = isFallDetected.getValue();
        if (fallAlert != null && fallAlert) return; // Alert already active

        long now = System.currentTimeMillis();
        // Since x, y, z are in 'g', we must add 1.0 back to Z (assuming Z is vertical) for total magnitude
        // Or, since standard Linear Acceleration has gravity removed, 
        // freefall would be near 0, but total magnitude of linear acceleration during fall impact is huge.
        double mag = Math.sqrt(x * x + y * y + z * z);

        if (fallState == 0) {
            // Wait for freefall/low-g or sudden jolt
            if (mag > G_IMPACT_THRESHOLD) { // direct impact detected
                fallState = 2;
                fallStateChangedMs = now;
            }
        } else if (fallState == 2) {
            // Wait for inactivity post-impact
            if (now - fallStateChangedMs > INACTIVITY_WAIT_MS) {
                // Check if recently inactive
                double variance = getRecentAccelVariance();
                if (variance < G_INACTIVITY_THRESHOLD) {
                    fallState = 3;
                    isFallDetected.postValue(true);
                    
                    // Log the fall event
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                    String timeStr = sdf.format(new Date());
                    String eventStr = "⚠ Fall Detected — " + timeStr;
                    List<String> currentHistory = activityHistory.getValue();
                    if (currentHistory == null) currentHistory = new ArrayList<>();
                    List<String> updated = new ArrayList<>(currentHistory);
                    updated.add(0, eventStr);
                    if (updated.size() > 10) updated.remove(updated.size() - 1);
                    activityHistory.postValue(updated);
                } else {
                    fallState = 0; // Not inactive, false alarm
                }
            } else if (mag > G_IMPACT_THRESHOLD) {
                fallStateChangedMs = now; // reset timer on continued impact
            }
        } else if (now - fallStateChangedMs > FALL_TIMEOUT_MS) {
            fallState = 0; // Timeout, reset state
        }
    }

    private double getRecentAccelVariance() {
        if (accelBuffer.size() < 10) return 1.0;
        double sum = 0.0, sumSq = 0.0;
        int n = 0;
        for (float[] sample : accelBuffer) {
            double mag = Math.sqrt(sample[1] * sample[1] + sample[2] * sample[2] + sample[3] * sample[3]);
            sum += mag;
            sumSq += mag * mag;
            n++;
        }
        double mean = sum / n;
        return (sumSq / n) - (mean * mean);
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

    public LiveData<Boolean> getIsSessionRunning() { return isSessionRunning; }
    public LiveData<Boolean> getIsSessionPaused() { return isSessionPaused; }
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
    public LiveData<Long> getAvgLatencyNs() { return avgLatencyNs; }
    public LiveData<List<String>> getActivityHistory() { return activityHistory; }
    public LiveData<List<SessionEntity>> getAllSessions() { return allSessions; }
    public LiveData<String> getDbError() { return dbError; }

    public void updateMovementStatus() {
        if (accelBuffer.isEmpty()) {
            movementStatus.postValue("--");
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
        movementStatus.postValue(variance < threshold ? "Standing still" : "Moving");
    }

    public void addLatency(long latency) {
        latencySampleCount++;
        Long currentAvg = avgLatencyNs.getValue();
        if (currentAvg == null || currentAvg == 0) {
            avgLatencyNs.postValue(latency);
        } else {
            long total = currentAvg * (latencySampleCount - 1) + latency;
            avgLatencyNs.postValue(total / latencySampleCount);
        }
    }

    public void resetAll() {
        timerHandler.removeCallbacks(timerRunnable);
        isSessionRunning.setValue(false);
        isSessionPaused.setValue(false);
        startTimeWallMs = 0L;
        pausedDurationMs = 0L;
        pauseStartTimeMs = 0L;
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
        accelBuffer.clear();
        gyroBuffer.clear();
        activityHistory.setValue(new ArrayList<>());
        latencySampleCount = 0;
        sessionSaved = false;
        lastStablePrediction = -1;
        candidatePrediction = -1;
        candidateCount = 0;
        fallState = 0;
        isFallDetected.setValue(false);
        for (int i = 0; i < 6; i++) smoothedProbs[i] = 0f;
    }

    public void dismissFallAlert() {
        fallState = 0;
        isFallDetected.postValue(false);
    }

    public synchronized void saveCompletedSession() {
        if (sessionSaved) return;
        Long duration = sessionDurationMs.getValue();
        Integer predCount = predictionCount.getValue();
        Float confidence = avgConfidence.getValue();
        Long avgLat = avgLatencyNs.getValue();
        Integer idx = activityIndex.getValue();
        String dominant = (idx != null && idx >= 0) ? ActivityLabels.getLabel(idx) : "Walking";
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
            Log.d(TAG, "ROOM_SAVE: Session saved successfully");
        } catch (Exception e) {
            dbError.postValue("Room save error: " + e.getMessage());
            Log.e(TAG, "ROOM_SAVE: Error saving session", e);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        timerHandler.removeCallbacks(timerRunnable);
    }

    private float[] softmax(float[] logits) {
        if (logits == null || logits.length == 0) return new float[6];
        float max = Float.NEGATIVE_INFINITY;
        for (float v : logits) if (v > max) max = v;
        float sum = 0f;
        float[] exp = new float[logits.length];
        for (int i = 0; i < logits.length; i++) {
            exp[i] = (float) Math.exp(logits[i] - max);
            sum += exp[i];
        }
        if (sum > 0f) {
            for (int i = 0; i < exp.length; i++) {
                exp[i] /= sum;
            }
        }
        return exp;
    }
}
