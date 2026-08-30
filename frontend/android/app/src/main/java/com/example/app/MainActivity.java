package com.example.app;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity that collects accelerometer and gyroscope data, synchronises them, builds a
 * 128‑sample window, preprocesses the data, and runs inference via InferencePhone.
 *
 * Synchronisation tolerance: 20 ms – samples are paired only when their timestamps
 * differ by at most this amount. This mirrors typical Android sensor rates (≈50‑100 Hz).
 */
public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = "MainActivity";
    private static final long SYNC_TOLERANCE_MS = 20L; // 20 ms

    private boolean isCollecting = false;
    private int predictionCount = 0;
    private String sessionStatus = "READY";
    private SensorManager sensorManager;
    // Session control methods
    public void startSession() {
        if (isCollecting) return;
        isCollecting = true;
        sessionStatus = "COLLECTING";
        if (predictionListener != null) {
            predictionListener.onSessionStatus(sessionStatus);
        }
    }

    public void pauseSession() {
        if (!isCollecting) return;
        isCollecting = false;
        sessionStatus = "PAUSED";
        if (predictionListener != null) {
            predictionListener.onSessionStatus(sessionStatus);
        }
    }

    public void resetSession() {
        // Clear buffer and counters
        buffer.clear();
        predictionCount = 0;
        isCollecting = false;
        sessionStatus = "READY";
        if (predictionListener != null) {
            predictionListener.onSessionStatus(sessionStatus);
            predictionListener.onPredictionCount(predictionCount);
            predictionListener.onWindowUpdate(buffer.getCurrentSize());
        }
    }

    // Helper to report sensor availability status
    public String getSensorStatusString() {
        boolean accelOk = accelSensor != null;
        boolean gyroOk = gyroSensor != null;
        String accelStatus = accelOk ? "🟢 OK" : "⚠️ MISSING";
        String gyroStatus = gyroOk ? "🟢 OK" : "⚠️ MISSING";
        return "Accelerometer: " + accelStatus + " | Gyroscope: " + gyroStatus;
    }

    // Update UI during inference
    private void updateInferenceUI(int pred, float[] scores, long latencyNs) {
        if (predictionListener != null) {
            predictionListener.onPrediction(pred, scores, latencyNs);
            predictionCount++;
            predictionListener.onPredictionCount(predictionCount);
            predictionListener.onLatencyUpdate(latencyNs);
            predictionListener.onSessionStatus("ANALYZING");
        }
    }

    // Existing onSensorChanged will call updateInferenceUI when buffer full

    private Sensor accelSensor;
    private Sensor gyroSensor;

    // latest readings (NaN indicates not yet received)
    private final float[] latestAccel = new float[3];
    private long accelTimestamp = Long.MIN_VALUE;
    private final float[] latestGyro = new float[3];
    private long gyroTimestamp = Long.MIN_VALUE;

    private final SensorBuffer buffer = new SensorBuffer();
    private final Preprocess preprocess = new Preprocess();
    private InferencePhone inference;

    // Listener for UI updates
    public interface PredictionListener {
        void onPrediction(int classIdx, float[] scores, long latencyNs);
        void onWindowUpdate(int size);
        void onSessionStatus(String status);
        void onPredictionCount(int count);
        void onLatencyUpdate(long latencyNs);
    }

    private PredictionListener predictionListener;

    public void setPredictionListener(PredictionListener listener) {
        this.predictionListener = listener;
    }
    public static final String[] LABELS = {
        "WALKING", "WALKING_UPSTAIRS", "WALKING_DOWNSTAIRS",
        "SITTING", "STANDING", "LAYING"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up BottomNavigation with NavController
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(bottomNav, navController);

            try {
            DashboardFragment fragment = (DashboardFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
            if (fragment != null) {
                setPredictionListener(fragment);
            }
        } catch (Exception e) {
            Log.w(TAG, "DashboardFragment not attached yet", e);
        }

        inference = new InferencePhone(this);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        // initialise placeholder NaNs
        for (int i = 0; i < 3; i++) {
            latestAccel[i] = Float.NaN;
            latestGyro[i] = Float.NaN;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Normal motion sensors do NOT require runtime permission on API 34.
        sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, latestAccel, 0, 3);
            accelTimestamp = event.timestamp;
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            System.arraycopy(event.values, 0, latestGyro, 0, 3);
            gyroTimestamp = event.timestamp;
        }

        // Try to pair the two latest readings
        if (accelTimestamp != Long.MIN_VALUE && gyroTimestamp != Long.MIN_VALUE) {
            long diffNs = Math.abs(accelTimestamp - gyroTimestamp);
            long diffMs = diffNs / 1_000_000L;
            if (diffMs <= SYNC_TOLERANCE_MS) {
                // If not collecting, ignore samples
                if (!isCollecting) {
                    // Reset timestamps to avoid reusing stale data
                    accelTimestamp = Long.MIN_VALUE;
                    gyroTimestamp = Long.MIN_VALUE;
                    return;
                }
                // Build six‑feature sample
                float[] sample = new float[]{
                    latestAccel[0], latestAccel[1], latestAccel[2],
                    latestGyro[0], latestGyro[1], latestGyro[2]
                };
                // Reset timestamps so we don't reuse the same pair
                accelTimestamp = Long.MIN_VALUE;
                gyroTimestamp = Long.MIN_VALUE;
                buffer.addSample(sample);
                // Notify UI of window progress
                if (predictionListener != null) {
                    predictionListener.onWindowUpdate(buffer.getCurrentSize());
                }
                if (buffer.isFull()) {
                    // Preprocess and inference
                    float[][] window = buffer.getWindow(); // [128][6]
                    float[] flat = preprocess.apply(window); // 768 floats
                    long startNs = System.nanoTime();
                    int pred = inference.predict(flat);
                    long latencyNs = System.nanoTime() - startNs;
                    Log.i(TAG, "Predicted activity class: " + pred);
                    // Retrieve raw scores from inference
                    float[] scores = inference.getLastScores();
                    // Notify UI listener if present
                    updateInferenceUI(pred, scores, latencyNs);
                    buffer.clear();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No handling required for this demo
    }
}
