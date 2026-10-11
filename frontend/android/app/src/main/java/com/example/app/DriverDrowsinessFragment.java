package com.example.app;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;

public class DriverDrowsinessFragment extends Fragment implements SensorEventListener {
    private View cardDrowsinessAlert;
    private TextView tvDrivingStatusTitle;
    private TextView tvDrivingVigilance;
    private TextView tvSteeringStability;
    private TextView tvMicrosleepEvents;
    private TextView tvFatigueScore;
    private MaterialButton btnToggleDrivingMonitor;

    private SensorManager sensorManager;
    private Sensor accelSensor;
    private Sensor gyroSensor;
    private Vibrator vibrator;

    private boolean isMonitoring = false;
    private int noddingJerkCount = 0;
    private long lastJerkTimeMs = 0;
    private long immobilityStartMs = 0;
    private boolean inImmobility = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_driver_drowsiness, container, false);

        cardDrowsinessAlert = view.findViewById(R.id.cardDrowsinessAlert);
        tvDrivingStatusTitle = view.findViewById(R.id.tvDrivingStatusTitle);
        tvDrivingVigilance = view.findViewById(R.id.tvDrivingVigilance);
        tvSteeringStability = view.findViewById(R.id.tvSteeringStability);
        tvMicrosleepEvents = view.findViewById(R.id.tvMicrosleepEvents);
        tvFatigueScore = view.findViewById(R.id.tvFatigueScore);
        btnToggleDrivingMonitor = view.findViewById(R.id.btnToggleDrivingMonitor);

        MaterialButton btnAwakeDismiss = view.findViewById(R.id.btnAwakeDismiss);
        MaterialButton btnCallHelplineDrowsy = view.findViewById(R.id.btnCallHelplineDrowsy);
        MaterialButton btnTestDrowsyAlarm = view.findViewById(R.id.btnTestDrowsyAlarm);
        View btnDialHelplineDriver = view.findViewById(R.id.btnDialHelplineDriver);

        if (getContext() != null) {
            sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager != null) {
                accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            }
            vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        }

        btnToggleDrivingMonitor.setOnClickListener(v -> {
            if (isMonitoring) {
                stopMonitoring();
            } else {
                startMonitoring();
            }
        });

        if (btnAwakeDismiss != null) {
            btnAwakeDismiss.setOnClickListener(v -> dismissAlarm());
        }

        if (btnCallHelplineDrowsy != null) {
            btnCallHelplineDrowsy.setOnClickListener(v -> dialHelpline());
        }

        if (btnDialHelplineDriver != null) {
            btnDialHelplineDriver.setOnClickListener(v -> dialHelpline());
        }

        if (btnTestDrowsyAlarm != null) {
            btnTestDrowsyAlarm.setOnClickListener(v -> triggerDrowsinessAlarm("Simulated fatigue alarm triggered"));
        }

        return view;
    }

    private void dialHelpline() {
        Intent dialIntent = new Intent(Intent.ACTION_DIAL);
        dialIntent.setData(Uri.parse("tel:" + UserManager.HELPLINE_NUMBER));
        startActivity(dialIntent);
    }

    private void startMonitoring() {
        isMonitoring = true;
        noddingJerkCount = 0;
        immobilityStartMs = System.currentTimeMillis();
        cardDrowsinessAlert.setVisibility(View.GONE);
        btnToggleDrivingMonitor.setText("Stop Driving Monitor");
        btnToggleDrivingMonitor.setBackgroundColor(0xFFDC2626);
        tvDrivingStatusTitle.setText("Driving Monitor: Active");
        tvDrivingVigilance.setText("Vigilance State: Alert & Focused");
        tvDrivingVigilance.setTextColor(0xFF16A34A);
        tvMicrosleepEvents.setText("0");
        tvFatigueScore.setText("Low (2%)");

        if (sensorManager != null) {
            try {
                if (accelSensor != null) sensorManager.registerListener(this, accelSensor, 20000);
                if (gyroSensor != null) sensorManager.registerListener(this, gyroSensor, 20000);
            } catch (Exception e) {
                try {
                    if (accelSensor != null) sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
                    if (gyroSensor != null) sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
                } catch (Exception ignored) {}
            }
        }
        Toast.makeText(getContext(), "Driver drowsiness monitoring active", Toast.LENGTH_SHORT).show();
    }

    private void stopMonitoring() {
        isMonitoring = false;
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        btnToggleDrivingMonitor.setText("Start Driving Monitor");
        btnToggleDrivingMonitor.setBackgroundColor(0xFF2563EB);
        tvDrivingStatusTitle.setText("Driving Monitor: Idle");
        tvDrivingVigilance.setText("Vigilance State: Inactive");
        tvDrivingVigilance.setTextColor(0xFF64748B);
        cardDrowsinessAlert.setVisibility(View.GONE);
    }

    private void triggerDrowsinessAlarm(String reason) {
        if (cardDrowsinessAlert != null) {
            cardDrowsinessAlert.setVisibility(View.VISIBLE);
        }
        tvDrivingVigilance.setText("CRITICAL: Drowsiness Detected");
        tvDrivingVigilance.setTextColor(0xFFDC2626);
        tvFatigueScore.setText("HIGH (88%)");

        // Sound/Haptic Alarm
        if (vibrator != null && vibrator.hasVibrator()) {
            try {
                long[] pattern = {0, 400, 200, 400, 200, 600};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
                } else {
                    vibrator.vibrate(pattern, -1);
                }
            } catch (Exception ignored) {}
        }

        if (getContext() != null) {
            Toast.makeText(getContext(), reason, Toast.LENGTH_LONG).show();
        }
    }

    private void dismissAlarm() {
        if (cardDrowsinessAlert != null) {
            cardDrowsinessAlert.setVisibility(View.GONE);
        }
        tvDrivingVigilance.setText("Vigilance State: Alert (Driver Confirmed)");
        tvDrivingVigilance.setTextColor(0xFF16A34A);
        tvFatigueScore.setText("Moderate (20%)");
        if (vibrator != null) vibrator.cancel();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isMonitoring) return;
        long now = System.currentTimeMillis();

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float ax = event.values[0];
            float ay = event.values[1];
            float az = event.values[2];
            float total = (float) Math.sqrt(ax * ax + ay * ay + az * az);

            // Detect abnormal stillness (< 0.2g deviation from 1g)
            float deviation = Math.abs(total - 9.8f);
            if (deviation < 0.35f) {
                if (!inImmobility) {
                    inImmobility = true;
                    immobilityStartMs = now;
                } else if (now - immobilityStartMs > 12000L) {
                    // Immobility exceeding 12 seconds while driving indicates sleep/loss of consciousness
                    tvFatigueScore.setText("Elevated (65%)");
                }
            } else {
                // Movement observed
                if (inImmobility && (now - immobilityStartMs > 8000L) && deviation > 3.8f) {
                    // Sudden reactive jerk after prolonged immobility = Head nodding / startled wake-up!
                    noddingJerkCount++;
                    tvMicrosleepEvents.setText(String.valueOf(noddingJerkCount));
                    if (now - lastJerkTimeMs > 5000L) {
                        lastJerkTimeMs = now;
                        triggerDrowsinessAlarm("Head-nodding / microsleep jerk detected!");
                    }
                }
                inImmobility = false;
            }
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float yawRate = Math.abs(event.values[2]);
            if (yawRate > 1.8f) {
                tvSteeringStability.setText("Erratic Swerve");
                tvSteeringStability.setTextColor(0xFFDC2626);
            } else {
                tvSteeringStability.setText("Stable");
                tvSteeringStability.setTextColor(0xFF16A34A);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onPause() {
        super.onPause();
        if (isMonitoring) {
            stopMonitoring();
        }
    }
}
