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
import java.util.Locale;

public class MLDiagnosticsFragment extends Fragment {
    private LiveMotionViewModel viewModel;
    private TextView tvOutput;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ml_diagnostics, container, false);
        tvOutput = view.findViewById(R.id.tvDiagnosticsOutput);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity(), new LiveMotionViewModelFactory(requireActivity().getApplication()))
                .get(LiveMotionViewModel.class);

        viewModel.getSmoothedScores().observe(getViewLifecycleOwner(), probs -> {
            if (probs == null || probs.length < 6) return;
            StringBuilder sb = new StringBuilder();
            sb.append("REAL PROBABILITY VECTOR:\n");
            sb.append(String.format(Locale.US, "Walking:    %5.1f%%\n", probs[0] * 100f));
            sb.append(String.format(Locale.US, "Upstairs:   %5.1f%%\n", probs[1] * 100f));
            sb.append(String.format(Locale.US, "Downstairs: %5.1f%%\n", probs[2] * 100f));
            sb.append(String.format(Locale.US, "Sitting:    %5.1f%%\n", probs[3] * 100f));
            sb.append(String.format(Locale.US, "Standing:   %5.1f%%\n", probs[4] * 100f));
            sb.append(String.format(Locale.US, "Laying:     %5.1f%%\n", probs[5] * 100f));

            int maxIdx = 0;
            for (int i = 1; i < 6; i++) {
                if (probs[i] > probs[maxIdx]) maxIdx = i;
            }
            sb.append("\nPREDICTED: ").append(ActivityLabels.getLabel(maxIdx));

            float[] raw = viewModel.getRawScores().getValue();
            if (raw != null && raw.length >= 6) {
                sb.append("\n\nRAW LOGITS:\n");
                for (int i=0; i<6; i++) {
                    sb.append(String.format(Locale.US, "C%d: %6.2f\n", i, raw[i]));
                }
            }
            Long lat = viewModel.getLatencyNs().getValue();
            if (lat != null) {
                sb.append(String.format(Locale.US, "\nLatency: %.1f ms", lat / 1_000_000f));
            }

            if (tvOutput != null) {
                tvOutput.setText(sb.toString());
            }
        });
    }
}
