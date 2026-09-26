package com.example.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {
    private TextView activityNameText;
    private ImageView mascotImage;
    private CircularProgressIndicator confidenceRing;
    private TextView confidenceText;
    private TextView movementStatusText;
    private TextView sessionTimerText;
    private TextView sessionStatusText;
    private TextView sensorStatusText;
    private TextView timelineLogText;
    private MaterialButton btnStart;
    private MaterialButton btnPause;
    private MaterialButton btnStop;
    private TextView btnReset;
    private View fallAlertCard;
    private MaterialButton btnFallDismiss;

    private LiveMotionViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        activityNameText = view.findViewById(R.id.activityName);
        mascotImage = view.findViewById(R.id.mascotImage);
        confidenceRing = view.findViewById(R.id.confidenceRing);
        confidenceText = view.findViewById(R.id.confidenceText);
        movementStatusText = view.findViewById(R.id.movementStatus);
        sessionTimerText = view.findViewById(R.id.sessionTimerText);
        sessionStatusText = view.findViewById(R.id.sessionStatus);
        sensorStatusText = view.findViewById(R.id.sensorStatus);
        timelineLogText = view.findViewById(R.id.timelineLogText);
        fallAlertCard = view.findViewById(R.id.fallAlertCard);
        btnFallDismiss = view.findViewById(R.id.btnFallDismiss);

        btnStart = view.findViewById(R.id.btnStart);
        btnPause = view.findViewById(R.id.btnPause);
        btnStop = view.findViewById(R.id.btnStop);
        btnReset = view.findViewById(R.id.btnReset);

        View heroCard = view.findViewById(R.id.currentActivityCard);
        if (heroCard != null) {
            heroCard.setOnClickListener(v -> navigateTo(R.id.activityGuideFragment));
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity(), new LiveMotionViewModelFactory(requireActivity().getApplication()))
                .get(LiveMotionViewModel.class);

        setupObservers();

        if (btnStart != null) {
            btnStart.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).startSession();
                }
            });
        }

        if (btnPause != null) {
            btnPause.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).pauseSession();
                }
            });
        }

        if (btnStop != null) {
            btnStop.setOnClickListener(v -> {
                if (viewModel != null) {
                    viewModel.stopAndSaveSession();
                }
                navigateTo(R.id.sessionSummaryFragment);
            });
        }

        if (btnReset != null) {
            btnReset.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).resetSession();
                }
                if (sessionTimerText != null) sessionTimerText.setText("00:00");
                if (activityNameText != null) activityNameText.setText("Ready to Detect");
                if (confidenceRing != null) confidenceRing.setProgress(0);
                if (confidenceText != null) confidenceText.setText("0% confidence");
                if (mascotImage != null) mascotImage.setImageResource(R.drawable.ic_walking);
                if (timelineLogText != null) {
                    timelineLogText.setText("No predictions recorded yet. Tap Start Session to begin monitoring.");
                }
            });
        }
        if (btnFallDismiss != null) {
            btnFallDismiss.setOnClickListener(v -> {
                if (viewModel != null) {
                    viewModel.dismissFallAlert();
                }
            });
        }
    }

    private void setupObservers() {
        viewModel.isFallDetected.observe(getViewLifecycleOwner(), fall -> {
            if (fallAlertCard != null) {
                fallAlertCard.setVisibility((fall != null && fall) ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getSessionDurationMs().observe(getViewLifecycleOwner(), durationMs -> {
            if (durationMs != null) {
                long elapsed = durationMs;
                long seconds = (elapsed / 1000) % 60;
                long minutes = (elapsed / (1000 * 60));
                if (sessionTimerText != null) {
                    sessionTimerText.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
                }
            }
        });

        viewModel.getSessionStatus().observe(getViewLifecycleOwner(), this::updateButtonStates);

        viewModel.getActivityIndex().observe(getViewLifecycleOwner(), classIdx -> {
            if (classIdx == null || classIdx < 0) return;
            if (activityNameText != null) {
                activityNameText.setText(ActivityLabels.getLabel(classIdx));
            }
            if (mascotImage != null) {
                mascotImage.setImageResource(ActivityLabels.getDrawableRes(classIdx));
            }
            if (movementStatusText != null) {
                movementStatusText.setText(classIdx == 3 || classIdx == 4 || classIdx == 5 ? "Standing still" : "Moving");
            }
        });

        viewModel.getSmoothedScores().observe(getViewLifecycleOwner(), probs -> {
            if (probs == null) return;
            Integer idx = viewModel.getActivityIndex().getValue();
            int classIdx = (idx != null && idx >= 0) ? idx : 0;
            float maxProb = (classIdx >= 0 && classIdx < probs.length) ? probs[classIdx] : 0f;
            int percent = Math.min(100, Math.max(0, Math.round(maxProb * 100.0f)));
            if (confidenceRing != null) {
                confidenceRing.setProgress(percent);
            }
            if (confidenceText != null) {
                confidenceText.setText(percent + "% confidence");
            }
        });

        viewModel.getActivityHistory().observe(getViewLifecycleOwner(), history -> {
            if (history == null || history.isEmpty()) return;
            if (timelineLogText != null) {
                StringBuilder sb = new StringBuilder();
                for (String ev : history) {
                    sb.append("• ").append(ev).append("\n");
                }
                timelineLogText.setText(sb.toString().trim());
            }
        });

        viewModel.getIsSessionRunning().observe(getViewLifecycleOwner(), isRunning -> {
            if (sensorStatusText != null) {
                sensorStatusText.setText((isRunning != null && isRunning) ? "● Sensors Active" : "○ Sensors Inactive");
            }
        });
    }

    private void updateButtonStates(String status) {
        if (status == null) status = "READY";
        if (sessionStatusText != null) {
            switch (status) {
                case "READY":
                    sessionStatusText.setText("Ready to detect");
                    break;
                case "COLLECTING":
                case "ANALYZING":
                    sessionStatusText.setText("Session Active");
                    break;
                case "PAUSED":
                    sessionStatusText.setText("Session Paused");
                    break;
                case "STOPPED":
                    sessionStatusText.setText("Session Ended");
                    break;
                default:
                    sessionStatusText.setText(status);
                    break;
            }
        }

        if ("READY".equals(status) || "STOPPED".equals(status)) {
            if (btnStart != null) btnStart.setVisibility(View.VISIBLE);
            if (btnPause != null) btnPause.setVisibility(View.GONE);
            if (btnStop != null) btnStop.setVisibility(View.GONE);
        } else if ("PAUSED".equals(status)) {
            if (btnStart != null) btnStart.setVisibility(View.GONE);
            if (btnPause != null) {
                btnPause.setVisibility(View.VISIBLE);
                btnPause.setText("Resume");
            }
            if (btnStop != null) btnStop.setVisibility(View.VISIBLE);
        } else { // ACTIVE / COLLECTING / ANALYZING
            if (btnStart != null) btnStart.setVisibility(View.GONE);
            if (btnPause != null) {
                btnPause.setVisibility(View.VISIBLE);
                btnPause.setText("Pause");
            }
            if (btnStop != null) btnStop.setVisibility(View.VISIBLE);
        }
    }

    private void navigateTo(int destinationId) {
        try {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            navController.navigate(destinationId);
        } catch (Exception ignored) {
        }
    }
}
