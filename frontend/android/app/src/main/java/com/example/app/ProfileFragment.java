package com.example.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class ProfileFragment extends Fragment {
    private TextView tvUserName;
    private TextView tvUserRole;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvUserName = view.findViewById(R.id.tvProfileUserName);
        tvUserRole = view.findViewById(R.id.tvProfileUserRole);
        MaterialButton btnSwitchUser = view.findViewById(R.id.btnSwitchUser);
        MaterialButton btnCallHelpline = view.findViewById(R.id.btnCallHelpline);

        updateUserUI();

        if (btnSwitchUser != null) {
            btnSwitchUser.setOnClickListener(v -> showMultiUserDialog());
        }

        if (btnCallHelpline != null) {
            btnCallHelpline.setOnClickListener(v -> {
                Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                dialIntent.setData(Uri.parse("tel:" + UserManager.HELPLINE_NUMBER));
                startActivity(dialIntent);
            });
        }

        View btnWristVitals = view.findViewById(R.id.btnNavWristVitals);
        if (btnWristVitals != null) {
            btnWristVitals.setOnClickListener(v -> navigateTo(R.id.wristVitalCheckFragment));
        }

        View btnInstructions = view.findViewById(R.id.btnNavInstructions);
        if (btnInstructions != null) {
            btnInstructions.setOnClickListener(v -> navigateTo(R.id.activityGuideFragment));
        }

        View btnGuide = view.findViewById(R.id.btnNavGuide);
        if (btnGuide != null) {
            btnGuide.setOnClickListener(v -> navigateTo(R.id.activityGuideFragment));
        }

        View btnSafety = view.findViewById(R.id.btnNavSafety);
        if (btnSafety != null) {
            btnSafety.setOnClickListener(v -> navigateTo(R.id.elderlySafetyFragment));
        }

        View btnInsights = view.findViewById(R.id.btnNavInsights);
        if (btnInsights != null) {
            btnInsights.setOnClickListener(v -> navigateTo(R.id.insightsFragment));
        }

        View btnAbout = view.findViewById(R.id.btnNavAbout);
        if (btnAbout != null) {
            btnAbout.setOnClickListener(v -> navigateTo(R.id.aboutFragment));
        }

        View btnDiag = view.findViewById(R.id.btnNavDiagnostics);
        if (btnDiag != null) {
            btnDiag.setOnClickListener(v -> navigateTo(R.id.mlDiagnosticsFragment));
        }

        return view;
    }

    private void updateUserUI() {
        if (getContext() == null) return;
        UserManager.UserProfile current = UserManager.getActiveUser(requireContext());
        if (tvUserName != null) tvUserName.setText(current.name);
        if (tvUserRole != null) tvUserRole.setText(current.role);
    }

    private void showMultiUserDialog() {
        if (getContext() == null) return;
        List<UserManager.UserProfile> users = UserManager.getAllUsers();
        String[] userNames = new String[users.size()];
        for (int i = 0; i < users.size(); i++) {
            userNames[i] = users.get(i).name + " (" + users.get(i).role + ")";
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Switch Account / Multi-User Login")
                .setItems(userNames, (dialog, which) -> {
                    UserManager.setActiveUser(requireContext(), users.get(which).id);
                    updateUserUI();
                    Toast.makeText(requireContext(), "Switched active profile to " + users.get(which).name, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void navigateTo(int destinationId) {
        try {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            navController.navigate(destinationId);
        } catch (Exception ignored) {
        }
    }
}
