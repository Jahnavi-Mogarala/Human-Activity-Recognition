package com.example.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.app.database.SessionEntity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HistoryDetailFragment extends Fragment {
    private LiveMotionViewModel viewModel;
    private TextView tvDetailActivity, tvDetailDate, tvDetailDuration, tvDetailConfidence, tvDetailPredictions, tvDetailLatency;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history_detail, container, false);
        tvDetailActivity = view.findViewById(R.id.tvDetailActivity);
        tvDetailDate = view.findViewById(R.id.tvDetailDate);
        tvDetailDuration = view.findViewById(R.id.tvDetailDuration);
        tvDetailConfidence = view.findViewById(R.id.tvDetailConfidence);
        tvDetailPredictions = view.findViewById(R.id.tvDetailPredictions);
        tvDetailLatency = view.findViewById(R.id.tvDetailLatency);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity(), new LiveMotionViewModelFactory(requireActivity().getApplication()))
                .get(LiveMotionViewModel.class);

        viewModel.getAllSessions().observe(getViewLifecycleOwner(), sessions -> {
            if (sessions != null && !sessions.isEmpty()) {
                SessionEntity session = sessions.get(0); // Display latest session
                if (tvDetailActivity != null) tvDetailActivity.setText(session.getDominantActivity());
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                if (tvDetailDate != null) tvDetailDate.setText("Date: " + sdf.format(new Date(session.getTimestamp())));
                long sec = session.getDurationMs() / 1000;
                if (tvDetailDuration != null) tvDetailDuration.setText("Duration: " + (sec > 0 ? sec + " seconds" : session.getDurationMs() + " ms"));
                if (tvDetailConfidence != null) tvDetailConfidence.setText(String.format(Locale.getDefault(), "Avg Confidence: %.1f%%", session.getAvgConfidence()));
                if (tvDetailPredictions != null) tvDetailPredictions.setText("Prediction Windows: " + session.getPredictionCount());
                if (tvDetailLatency != null) tvDetailLatency.setText(String.format(Locale.getDefault(), "Avg Latency: %.2f ms", session.getAvgLatency()));
            }
        });
    }
}
