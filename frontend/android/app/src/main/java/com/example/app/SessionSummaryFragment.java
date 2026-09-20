package com.example.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.example.app.database.SessionEntity;
import java.util.List;

public class SessionSummaryFragment extends Fragment {
    private LiveMotionViewModel viewModel;
    private TextView tvDuration, tvDominantActivity, tvAvgConfidence, tvPredictionCount, tvAvgLatency;
    private Button btnViewHistory, btnDone;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_session_summary, container, false);
        tvDuration = view.findViewById(R.id.tvDuration);
        tvDominantActivity = view.findViewById(R.id.tvDominantActivity);
        tvAvgConfidence = view.findViewById(R.id.tvAvgConfidence);
        tvPredictionCount = view.findViewById(R.id.tvPredictionCount);
        tvAvgLatency = view.findViewById(R.id.tvAvgLatency);
        btnViewHistory = view.findViewById(R.id.btnViewHistory);
        btnDone = view.findViewById(R.id.btnDone);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity(), new LiveMotionViewModelFactory(requireActivity().getApplication())).get(LiveMotionViewModel.class);
        viewModel.getAllSessions().observe(getViewLifecycleOwner(), new Observer<List<SessionEntity>>() {
            @Override
            public void onChanged(List<SessionEntity> sessions) {
                if (sessions != null && !sessions.isEmpty()) {
                    SessionEntity latest = sessions.get(0);
                    tvDuration.setText("Duration: " + latest.getDurationMs() + " ms");
                    tvDominantActivity.setText("Activity: " + latest.getDominantActivity());
                    tvAvgConfidence.setText("Avg Confidence: " + String.format("%.2f", latest.getAvgConfidence()));
                    tvPredictionCount.setText("Predictions: " + latest.getPredictionCount());
                    tvAvgLatency.setText("Avg Latency: " + latest.getAvgLatency() + " ns");
                }
            }
        });
        btnViewHistory.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            navController.navigate(R.id.historyFragment);
        });
        btnDone.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            navController.navigate(R.id.homeFragment);
        });
    }
}
