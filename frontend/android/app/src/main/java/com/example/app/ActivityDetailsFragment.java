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

public class ActivityDetailsFragment extends Fragment {
    private ImageView ivDetailIllustration;
    private TextView tvDetailTitle, tvDetailDesc, tvDetailPlacement;
    private LiveMotionViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity_details, container, false);
        ivDetailIllustration = view.findViewById(R.id.ivDetailIllustration);
        tvDetailTitle = view.findViewById(R.id.tvDetailTitle);
        tvDetailDesc = view.findViewById(R.id.tvDetailDesc);
        tvDetailPlacement = view.findViewById(R.id.tvDetailPlacement);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity(), new LiveMotionViewModelFactory(requireActivity().getApplication()))
                .get(LiveMotionViewModel.class);

        viewModel.getActivityIndex().observe(getViewLifecycleOwner(), idx -> {
            int classIdx = (idx != null && idx >= 0) ? idx : 0;
            if (tvDetailTitle != null) tvDetailTitle.setText(ActivityLabels.getLabel(classIdx));
            if (ivDetailIllustration != null) ivDetailIllustration.setImageResource(ActivityLabels.getDrawableRes(classIdx));
            if (tvDetailDesc != null) tvDetailDesc.setText(ActivityLabels.getDescription(classIdx));
            if (tvDetailPlacement != null) tvDetailPlacement.setText(ActivityLabels.PLACEMENT_TIP);
        });
    }
}
