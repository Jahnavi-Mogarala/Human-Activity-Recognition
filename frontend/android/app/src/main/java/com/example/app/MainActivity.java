package com.example.app;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Main Activity responsible for sensor registration (20,000 µs target period),
 * 128-sample window buffering, PyTorch model inference invocation, and synchronizing
 * session control with LiveMotionViewModel.
 */
public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = "MainActivity";
    private static final long SYNC_TOLERANCE_MS = 20L; // 20 ms tolerance

    private SensorManager sensorManager;
    private Sensor accelSensor;
    private Sensor gyroSensor;

    private final float[] latestAccel = new float[3];
    private long accelTimestamp = Long.MIN_VALUE;
    private final float[] latestGyro = new float[3];
    private long gyroTimestamp = Long.MIN_VALUE;

    private final SensorBuffer buffer = new SensorBuffer();
    private final Preprocess preprocess = new Preprocess();
    private InferencePhone inference;
    private LiveMotionViewModel viewModel;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this, new LiveMotionViewModelFactory(getApplication()))
                .get(LiveMotionViewModel.class);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(bottomNav, navController);
        }

        inference = new InferencePhone(this);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }

        for (int i = 0; i < 3; i++) {
            latestAccel[i] = Float.NaN;
            latestGyro[i] = Float.NaN;
        }

        Log.d(TAG, "SESSION_STATE: MainActivity initialized");
    }

    public void startSession() {
        if (viewModel != null) {
            viewModel.startSession();
        }
        Log.d(TAG, "SESSION_STATE: Session started");
    }

    public void pauseSession() {
        if (viewModel != null) {
            viewModel.pauseSession();
        }
        Log.d(TAG, "SESSION_STATE: Session paused / toggled");
    }

    public void resetSession() {
        buffer.clear();
        if (viewModel != null) {
            viewModel.resetAll();
        }
        Log.d(TAG, "SESSION_STATE: Session reset");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null) {
            if (accelSensor != null) {
                sensorManager.registerListener(this, accelSensor, 20000); // 20,000 µs (50 Hz)
            }
            if (gyroSensor != null) {
                sensorManager.registerListener(this, gyroSensor, 20000); // 20,000 µs (50 Hz)
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Keep sensor collection active during background run if session is active
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Android gives m/s^2 (includes gravity). Model expects 'g'. 1g = 9.80665 m/s^2.
            // Keeping gravity allows the model to detect orientation (Flat=Laying, Vertical=Sitting/Standing)
            latestAccel[0] = event.values[0] / 9.80665f;
            latestAccel[1] = event.values[1] / 9.80665f;
            latestAccel[2] = event.values[2] / 9.80665f;
            accelTimestamp = event.timestamp;
            if (viewModel != null) {
                viewModel.addAccelSample(event.timestamp, latestAccel[0], latestAccel[1], latestAccel[2]);
            }
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            System.arraycopy(event.values, 0, latestGyro, 0, 3);
            gyroTimestamp = event.timestamp;
            if (viewModel != null) {
                viewModel.addGyroSample(event.timestamp, event.values[0], event.values[1], event.values[2]);
            }
        }

        if (accelTimestamp != Long.MIN_VALUE && gyroTimestamp != Long.MIN_VALUE) {
            long diffNs = Math.abs(accelTimestamp - gyroTimestamp);
            long diffMs = diffNs / 1_000_000L;
            if (diffMs <= SYNC_TOLERANCE_MS) {
                Boolean isRunning = viewModel != null ? viewModel.getIsSessionRunning().getValue() : Boolean.FALSE;
                Boolean isPaused = viewModel != null ? viewModel.getIsSessionPaused().getValue() : Boolean.FALSE;

                if (isRunning == null || !isRunning || (isPaused != null && isPaused)) {
                    accelTimestamp = Long.MIN_VALUE;
                    gyroTimestamp = Long.MIN_VALUE;
                    return;
                }

                float[] sample = new float[]{
                        latestAccel[0], latestAccel[1], latestAccel[2],
                        latestGyro[0], latestGyro[1], latestGyro[2]
                };

                accelTimestamp = Long.MIN_VALUE;
                gyroTimestamp = Long.MIN_VALUE;
                buffer.addSample(sample);

                if (viewModel != null) {
                    viewModel.setWindowSize(buffer.getCurrentSize());
                }

                if (buffer.isWindowReady()) {
                    Log.d(TAG, "WINDOW_READY: sliding window evaluated");
                    float[][] window = buffer.getWindow();
                    float[] flat = preprocess.apply(window);
                    buffer.markWindowRead();
                    
                    Log.d(TAG, "INFERENCE_START: invoking bilstm_attention model");
                    long startNs = System.nanoTime();
                    int pred = inference.predict(flat);
                    long latencyNs = System.nanoTime() - startNs;
                    float[] scores = inference.getLastScores();
                    Log.d(TAG, "INFERENCE_RESULT: predictedClass=" + pred + ", latencyMs=" + (latencyNs / 1_000_000f));

                    if (viewModel != null) {
                        viewModel.setActivityPrediction(pred, scores, latencyNs);
                    }

                    if (predictionListener != null) {
                        predictionListener.onPrediction(pred, scores, latencyNs);
                    }
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
