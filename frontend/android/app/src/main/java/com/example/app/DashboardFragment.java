package com.example.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class DashboardFragment extends Fragment implements MainActivity.PredictionListener {
    private TextView activityNameText;
    private ImageView mascotImage;
    private ProgressBar[] confidenceBars = new ProgressBar[6];
    private TextView[] confidenceLabels = new TextView[6];
    private TextView windowProgressText;
    private TextView sensorStatusText;
    private TextView sessionStatusText;
    private TextView predictionCountText;
    private TextView latencyText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        activityNameText = view.findViewById(R.id.activityName);
        mascotImage = view.findViewById(R.id.mascotImage);
        confidenceBars[0] = view.findViewById(R.id.progress0);
        confidenceBars[1] = view.findViewById(R.id.progress1);
        confidenceBars[2] = view.findViewById(R.id.progress2);
        confidenceBars[3] = view.findViewById(R.id.progress3);
        confidenceBars[4] = view.findViewById(R.id.progress4);
        confidenceBars[5] = view.findViewById(R.id.progress5);
        confidenceLabels[0] = view.findViewById(R.id.label0);
        confidenceLabels[1] = view.findViewById(R.id.label1);
        confidenceLabels[2] = view.findViewById(R.id.label2);
        confidenceLabels[3] = view.findViewById(R.id.label3);
        confidenceLabels[4] = view.findViewById(R.id.label4);
        confidenceLabels[5] = view.findViewById(R.id.label5);
        // Bind additional UI elements
        windowProgressText = view.findViewById(R.id.windowProgress);
        sensorStatusText = view.findViewById(R.id.sensorStatus);
        sessionStatusText = view.findViewById(R.id.sessionStatus);
        predictionCountText = view.findViewById(R.id.predictionCount);
        latencyText = view.findViewById(R.id.latencyText);
        return view;
    }

    /**
     * Called by MainActivity after each inference.
     */
    @Override
    public void onPrediction(int classIdx, float[] scores, long latencyNs) {
        // Update activity name (large text)
        if (activityNameText != null && classIdx >= 0 && classIdx < MainActivity.LABELS.length) {
            activityNameText.setText(MainActivity.LABELS[classIdx]);
        }
        // Determine if scores are logits; if sum > 1.0 assume logits and apply softmax
        float[] probs = scores != null ? scores.clone() : new float[6];
        if (scores != null) {
            float sum = 0f;
            for (float s : scores) sum += s;
            if (sum > 1.0f) { // likely logits
                probs = softmax(scores);
            }
        }
        // Update confidence bars (0-100 %)
        for (int i = 0; i < confidenceBars.length && i < probs.length; i++) {
            int percent = Math.round(probs[i] * 100);
            confidenceBars[i].setProgress(percent);
            confidenceLabels[i].setText(String.format("%s: %d%%", MainActivity.LABELS[i], percent));
        }
        // Update mascot color based on activity
        if (mascotImage != null) {
            int colorResId = getActivityColorRes(classIdx);
            mascotImage.setColorFilter(getResources().getColor(colorResId));
        }
    }

    @Override
    public void onWindowUpdate(int size) {
        if (windowProgressText != null) {
            windowProgressText.setText(String.format("Window: %d/128", size));
        }
    }

    @Override
    public void onSessionStatus(String status) {
        if (sessionStatusText != null) {
            sessionStatusText.setText(status);
        }
    }

    @Override
    public void onPredictionCount(int count) {
        if (predictionCountText != null) {
            predictionCountText.setText(String.format("Predictions: %d", count));
        }
    }

    @Override
    public void onLatencyUpdate(long latencyNs) {
        if (latencyText != null) {
            long ms = latencyNs / 1_000_000L;
            latencyText.setText(String.format("Latency: %d ms", ms));
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Hook up buttons
        view.findViewById(R.id.btnStart).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).startSession();
                // Update session status UI immediately
                if (sessionStatusText != null) sessionStatusText.setText("COLLECTING");
            }
        });
        view.findViewById(R.id.btnPause).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).pauseSession();
                if (sessionStatusText != null) sessionStatusText.setText("PAUSED");
            }
        });
        view.findViewById(R.id.btnReset).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).resetSession();
                // Reset UI fields
                if (sessionStatusText != null) sessionStatusText.setText("READY");
                if (predictionCountText != null) predictionCountText.setText("Predictions: 0");
                if (windowProgressText != null) windowProgressText.setText("Window: 0/128");
                if (latencyText != null) latencyText.setText("Latency: 0 ms");
                // Also reset bars and labels
                for (int i = 0; i < confidenceBars.length; i++) {
                    confidenceBars[i].setProgress(0);
                    confidenceLabels[i].setText(String.format("%s: 0%%", MainActivity.LABELS[i]));
                }
                activityNameText.setText("---");
                mascotImage.clearColorFilter();
            }
        });
        // Initial sensor status display
        if (sensorStatusText != null && getActivity() instanceof MainActivity) {
            sensorStatusText.setText(((MainActivity) getActivity()).getSensorStatusString());
        }
    }

    private int getActivityColorRes(int classIdx) {
        // Map each activity to the palette defined in colors.xml
        switch (classIdx) {
            case 0: return R.color.color_walking; // WALKING
            case 1: return R.color.color_walking_upstairs; // WALKING_UPSTAIRS
            case 2: return R.color.color_walking_downstairs; // WALKING_DOWNSTAIRS
            case 3: return R.color.color_sitting; // SITTING
            case 4: return R.color.color_standing; // STANDING
            case 5: return R.color.color_laying; // LAYING
            default: return R.color.background_color;
        }
    }





    private float[] softmax(float[] logits) {
        float max = Float.NEGATIVE_INFINITY;
        for (float v : logits) {
            if (v > max) max = v;
        }
        float sum = 0f;
        float[] exp = new float[logits.length];
        for (int i = 0; i < logits.length; i++) {
            exp[i] = (float) Math.exp(logits[i] - max);
            sum += exp[i];
        }
        for (int i = 0; i < exp.length; i++) {
            exp[i] /= sum;
        }
        return exp;
    }
}
