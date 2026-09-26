package com.example.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

public class ProfileFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        TextView btnGuide = view.findViewById(R.id.btnNavGuide);
        TextView btnSafety = view.findViewById(R.id.btnNavSafety);
        TextView btnInsights = view.findViewById(R.id.btnNavInsights);
        TextView btnAbout = view.findViewById(R.id.btnNavAbout);

        if (btnGuide != null) {
            btnGuide.setOnClickListener(v -> navigateTo(R.id.activityGuideFragment));
        }
        if (btnSafety != null) {
            btnSafety.setOnClickListener(v -> navigateTo(R.id.elderlySafetyFragment));
        }
        if (btnInsights != null) {
            btnInsights.setOnClickListener(v -> navigateTo(R.id.insightsFragment));
        }
        if (btnAbout != null) {
            btnAbout.setOnClickListener(v -> navigateTo(R.id.aboutFragment));
        }

        TextView btnDiag = view.findViewById(R.id.btnNavDiagnostics);
        if (btnDiag != null) {
            btnDiag.setOnClickListener(v -> navigateTo(R.id.mlDiagnosticsFragment));
        }

        return view;
    }

    private void navigateTo(int destinationId) {
        try {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            navController.navigate(destinationId);
        } catch (Exception ignored) {
        }
    }
}
