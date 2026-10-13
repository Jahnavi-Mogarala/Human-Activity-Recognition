package com.example.app;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MainActivity — sensor registration, rolling-window buffer, background inference.
 *
 * Key design decisions:
 * 1. Sensors stay registered across onPause/onResume so inference continues even
 *    when the screen dims or the user navigates to another fragment.
 * 2. Accel and gyro events are combined via a small latched approach: each sensor
 *    updates its own latest-value array, and every accel event (which arrives at
 *    ~50 Hz) flushes a combined sample into the buffer. Gyro is interpolated at the
 *    same instant. This is exactly how UCI-HAR combined-channel windows are built.
 *    There is NO strict timestamp sync window — that was the root cause of sample
 *    starvation in the previous version.
 * 3. Inference runs on a dedicated single-thread ExecutorService (not the sensor
 *    callback thread) so the sensor pipeline is never blocked.
 * 4. An AtomicBoolean inferenceInFlight prevents overlapping inference jobs from
 *    posting stale predictions over newer ones.
 */
public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = "MainActivity";

    private SensorManager sensorManager;
    private Sensor accelSensor;
    private Sensor gyroSensor;

    // Latest sensor readings (written on sensor thread, read on inference thread)
    private final float[] latestAccel = new float[3];   // in g units (divided by 9.80665)
    private final float[] latestGyro  = new float[3];   // rad/s (raw, unchanged)
    private volatile boolean gyroReady = false;

    private final SensorBuffer buffer = new SensorBuffer();
    private final Preprocess     preprocess = new Preprocess();
    private InferencePhone inference;
    private LiveMotionViewModel viewModel;

    // Single-threaded executor keeps inference jobs sequential
    private final ExecutorService inferenceExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean   inferenceInFlight = new AtomicBoolean(false);

    // ─── Interface for optional external prediction listeners ─────────────────
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

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this, new LiveMotionViewModelFactory(getApplication()))
                .get(LiveMotionViewModel.class);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(bottomNav, navController);
        }

        inference = new InferencePhone(this);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroSensor  = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }

        // Initialise arrays to zero (gyro may not arrive before first accel event)
        for (int i = 0; i < 3; i++) {
            latestAccel[i] = 0f;
            latestGyro[i]  = 0f;
        }

        registerSensors();
        Log.d(TAG, "SESSION_STATE: MainActivity initialized");
    }

    private void registerSensors() {
        if (sensorManager == null) return;
        if (accelSensor != null) {
            sensorManager.registerListener(this, accelSensor, 20000); // 20,000 µs = 50 Hz
        }
        if (gyroSensor != null) {
            sensorManager.registerListener(this, gyroSensor, 20000);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-register if they were unregistered (e.g. after onStop → onStart)
        registerSensors();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Intentionally do NOT unregister sensors here.
        // Keeping sensors active lets inference continue while the user is on
        // a different fragment (History, Profile, etc.) as long as a session is running.
        // Sensors are only unregistered in onDestroy.
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        inferenceExecutor.shutdown();
    }

    // ─── Session control (called from DashboardFragment buttons) ─────────────

    public void startSession() {
        if (viewModel != null) viewModel.startSession();
        Log.d(TAG, "SESSION_STATE: Session started");
    }

    public void pauseSession() {
        if (viewModel != null) viewModel.pauseSession();
        Log.d(TAG, "SESSION_STATE: Session paused / toggled");
    }

    public void resetSession() {
        buffer.clear();
        if (viewModel != null) viewModel.resetAll();
        Log.d(TAG, "SESSION_STATE: Session reset");
    }

    // ─── Sensor events ────────────────────────────────────────────────────────

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {

            case Sensor.TYPE_ACCELEROMETER:
                // UCI-HAR training used TYPE_ACCELEROMETER (total acceleration including gravity)
                // divided by 9.80665 to express in g units.
                latestAccel[0] = event.values[0] / 9.80665f;
                latestAccel[1] = event.values[1] / 9.80665f;
                latestAccel[2] = event.values[2] / 9.80665f;

                // Update ViewModel chart buffer & fall detection
                if (viewModel != null) {
                    viewModel.addAccelSample(event.timestamp, latestAccel[0], latestAccel[1], latestAccel[2]);
                }

                // Every accelerometer event drives one sample into the HAR buffer.
                // Gyro values are snapshotted at this instant (latest available reading).
                // This mirrors how UCI-HAR interleaved samples are stored: both sensors
                // were recorded at the same 50 Hz clock; we use the most recent gyro value
                // available, which at 50 Hz is at most one period (~20 ms) stale.
                pushSampleToBuffer();
                break;

            case Sensor.TYPE_GYROSCOPE:
                latestGyro[0] = event.values[0]; // rad/s, no unit conversion needed
                latestGyro[1] = event.values[1];
                latestGyro[2] = event.values[2];
                gyroReady = true;

                if (viewModel != null) {
                    viewModel.addGyroSample(event.timestamp, latestGyro[0], latestGyro[1], latestGyro[2]);
                }
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // ─── Sample buffering and inference dispatch ──────────────────────────────

    private void pushSampleToBuffer() {
        // Guard: session must be running and not paused
        Boolean isRunning = viewModel != null ? viewModel.getIsSessionRunning().getValue() : null;
        Boolean isPaused  = viewModel != null ? viewModel.getIsSessionPaused().getValue()  : null;
        if (isRunning == null || !isRunning || Boolean.TRUE.equals(isPaused)) return;

        // If gyro has never fired (e.g. sensor unavailable), default to zeros.
        // The model still works, just with flat gyro channels until gyro starts.
        float[] sample = new float[]{
                latestAccel[0], latestAccel[1], latestAccel[2],
                latestGyro[0],  latestGyro[1],  latestGyro[2]
        };

        buffer.addSample(sample);

        if (viewModel != null) {
            viewModel.setWindowSize(buffer.getCurrentSize());
        }

        if (buffer.isWindowReady() && !inferenceInFlight.get()) {
            // Snapshot the window on the sensor thread (fast array copy), then
            // hand off to the inference thread.
            final float[][] window = buffer.getWindow();
            buffer.markWindowRead();
            dispatchInference(window);
        }
    }

    private void dispatchInference(float[][] window) {
        if (!inferenceInFlight.compareAndSet(false, true)) {
            // A job is already queued; skip this window to avoid stale predictions
            return;
        }

        inferenceExecutor.execute(() -> {
            try {
                float[] flat = preprocess.apply(window);
                long startNs = System.nanoTime();
                int pred = inference.predict(flat);
                long latencyNs = System.nanoTime() - startNs;
                float[] scores = inference.getLastScores();

                Log.d(TAG, "INFERENCE_RESULT: class=" + pred
                        + " latencyMs=" + String.format("%.1f", latencyNs / 1_000_000f));

                if (viewModel != null && scores != null) {
                    viewModel.setActivityPrediction(pred, scores, latencyNs);
                }
                if (predictionListener != null) {
                    predictionListener.onPrediction(pred, scores, latencyNs);
                }
            } catch (Exception e) {
                Log.e(TAG, "INFERENCE_ERROR: " + e.getMessage(), e);
            } finally {
                inferenceInFlight.set(false);
            }
        });
    }
}
