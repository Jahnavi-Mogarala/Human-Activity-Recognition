package com.example.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.app.database.SessionEntity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InsightsFragment extends Fragment {
    private LiveMotionViewModel viewModel;
    private TextView tvTotalSessions, tvTotalTime, tvAvgConfidence, tvTopActivity, tvDistributionBreakdown, tvEmptyInsights;
    private LinearLayout insightsContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_insights, container, false);
        tvTotalSessions = view.findViewById(R.id.tvTotalSessions);
        tvTotalTime = view.findViewById(R.id.tvTotalTime);
        tvAvgConfidence = view.findViewById(R.id.tvAvgConfidence);
        tvTopActivity = view.findViewById(R.id.tvTopActivity);
        tvDistributionBreakdown = view.findViewById(R.id.tvDistributionBreakdown);
        tvEmptyInsights = view.findViewById(R.id.tvEmptyInsights);
        insightsContainer = view.findViewById(R.id.insightsContainer);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity(), new LiveMotionViewModelFactory(requireActivity().getApplication()))
                .get(LiveMotionViewModel.class);

        viewModel.getAllSessions().observe(getViewLifecycleOwner(), sessions -> {
            if (sessions == null || sessions.isEmpty()) {
                if (tvEmptyInsights != null) tvEmptyInsights.setVisibility(View.VISIBLE);
                if (insightsContainer != null) insightsContainer.setVisibility(View.GONE);
            } else {
                if (tvEmptyInsights != null) tvEmptyInsights.setVisibility(View.GONE);
                if (insightsContainer != null) insightsContainer.setVisibility(View.VISIBLE);

                int totalCount = sessions.size();
                long totalDurationMs = 0;
                float sumConfidence = 0;
                Map<String, Integer> activityCounts = new HashMap<>();

                for (SessionEntity s : sessions) {
                    totalDurationMs += s.getDurationMs();
                    sumConfidence += s.getAvgConfidence();
                    String act = s.getDominantActivity() != null ? s.getDominantActivity() : "Unknown";
                    activityCounts.put(act, activityCounts.getOrDefault(act, 0) + 1);
                }

                float avgConf = sumConfidence / totalCount;
                long totalMinutes = totalDurationMs / 60000;
                long totalSeconds = (totalDurationMs % 60000) / 1000;

                String topAct = "None";
                int maxActCount = 0;
                for (Map.Entry<String, Integer> entry : activityCounts.entrySet()) {
                    if (entry.getValue() > maxActCount) {
                        maxActCount = entry.getValue();
                        topAct = entry.getKey();
                    }
                }

                if (tvTotalSessions != null) tvTotalSessions.setText(String.valueOf(totalCount));
                if (tvTotalTime != null) {
                    tvTotalTime.setText(totalMinutes > 0 ? totalMinutes + " m " + totalSeconds + " s" : totalSeconds + " s");
                }
                if (tvAvgConfidence != null) tvAvgConfidence.setText(String.format("%.1f%%", avgConf));
                if (tvTopActivity != null) tvTopActivity.setText(topAct);

                if (tvDistributionBreakdown != null) {
                    StringBuilder sb = new StringBuilder();
                    for (Map.Entry<String, Integer> entry : activityCounts.entrySet()) {
                        int pct = Math.round((entry.getValue() * 100.0f) / totalCount);
                        sb.append("• ").append(entry.getKey()).append(": ").append(entry.getValue())
                          .append(" session(s) (").append(pct).append("%)\n");
                    }
                    tvDistributionBreakdown.setText(sb.toString().trim());
                }
            }
        });
    }
}
