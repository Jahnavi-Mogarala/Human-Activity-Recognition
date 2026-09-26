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
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.app.database.SessionEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {
    private LiveMotionViewModel viewModel;
    private TextView tvHomeSessionCount, tvHomeActiveTime, tvHomeTopActivity, tvNoRecentSessions;
    private RecyclerView rvRecentSessions;
    private SessionAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tvHomeSessionCount = view.findViewById(R.id.tvHomeSessionCount);
        tvHomeActiveTime = view.findViewById(R.id.tvHomeActiveTime);
        tvHomeTopActivity = view.findViewById(R.id.tvHomeTopActivity);
        tvNoRecentSessions = view.findViewById(R.id.tvNoRecentSessions);
        rvRecentSessions = view.findViewById(R.id.rvRecentSessions);

        if (rvRecentSessions != null) {
            rvRecentSessions.setLayoutManager(new LinearLayoutManager(requireContext()));
            adapter = new SessionAdapter();
            rvRecentSessions.setAdapter(adapter);
        }

        Button startBtn = view.findViewById(R.id.startSessionButton);
        if (startBtn != null) {
            startBtn.setOnClickListener(v -> navigateTo(R.id.liveFragment));
        }

        View btnGuide = view.findViewById(R.id.btnQuickGuide);
        if (btnGuide != null) {
            btnGuide.setOnClickListener(v -> navigateTo(R.id.activityGuideFragment));
        }

        View btnInsights = view.findViewById(R.id.btnQuickInsights);
        if (btnInsights != null) {
            btnInsights.setOnClickListener(v -> navigateTo(R.id.insightsFragment));
        }

        View btnLearnFall = view.findViewById(R.id.btnLearnFallDetection);
        if (btnLearnFall != null) {
            btnLearnFall.setOnClickListener(v -> navigateTo(R.id.elderlySafetyFragment));
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity(), new LiveMotionViewModelFactory(requireActivity().getApplication()))
                .get(LiveMotionViewModel.class);

        viewModel.getAllSessions().observe(getViewLifecycleOwner(), sessions -> {
            if (sessions == null || sessions.isEmpty()) {
                if (tvHomeSessionCount != null) tvHomeSessionCount.setText("0");
                if (tvHomeActiveTime != null) tvHomeActiveTime.setText("0 min");
                if (tvHomeTopActivity != null) tvHomeTopActivity.setText("None");
                if (tvNoRecentSessions != null) tvNoRecentSessions.setVisibility(View.VISIBLE);
                if (rvRecentSessions != null) rvRecentSessions.setVisibility(View.GONE);
            } else {
                if (tvNoRecentSessions != null) tvNoRecentSessions.setVisibility(View.GONE);
                if (rvRecentSessions != null) rvRecentSessions.setVisibility(View.VISIBLE);

                int count = sessions.size();
                long totalMs = 0;
                Map<String, Integer> counts = new HashMap<>();

                for (SessionEntity s : sessions) {
                    totalMs += s.getDurationMs();
                    String act = s.getDominantActivity() != null ? s.getDominantActivity() : "Unknown";
                    counts.put(act, counts.getOrDefault(act, 0) + 1);
                }

                long minutes = totalMs / 60000;
                long sec = (totalMs % 60000) / 1000;

                String topAct = "None";
                int maxCount = 0;
                for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                    if (entry.getValue() > maxCount) {
                        maxCount = entry.getValue();
                        topAct = entry.getKey();
                    }
                }

                if (tvHomeSessionCount != null) tvHomeSessionCount.setText(String.valueOf(count));
                if (tvHomeActiveTime != null) {
                    tvHomeActiveTime.setText(minutes > 0 ? minutes + " min" : sec + " s");
                }
                if (tvHomeTopActivity != null) tvHomeTopActivity.setText(topAct);

                // Show top 3 recent sessions
                List<SessionEntity> recentList = new ArrayList<>();
                for (int i = 0; i < Math.min(3, sessions.size()); i++) {
                    recentList.add(sessions.get(i));
                }
                if (adapter != null) {
                    adapter.submitList(recentList);
                }
            }
        });

        TextView tvHomeSensorStatus = view.findViewById(R.id.tvHomeSensorStatus);
        TextView tvHomeFallStatus = view.findViewById(R.id.tvHomeFallStatus);

        viewModel.getIsSessionRunning().observe(getViewLifecycleOwner(), isRunning -> {
            boolean active = isRunning != null && isRunning;
            if (tvHomeSensorStatus != null) {
                tvHomeSensorStatus.setText(active ? "Active" : "Inactive");
                tvHomeSensorStatus.setTextColor(active ? 0xFF4CAF50 : 0xFF757575); // Green or Grey
            }
            if (tvHomeFallStatus != null) {
                tvHomeFallStatus.setText(active ? "Monitoring" : "Disabled");
                tvHomeFallStatus.setTextColor(active ? 0xFF2196F3 : 0xFF757575); // Blue or Grey
            }
        });
    }

    private void navigateTo(int destinationId) {
        try {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            navController.navigate(destinationId);
        } catch (Exception ignored) {
        }
    }
}
