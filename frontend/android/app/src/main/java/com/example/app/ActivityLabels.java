package com.example.app;

public class ActivityLabels {
    public static final String[] LABELS = {
        "Walking",
        "Upstairs",
        "Downstairs",
        "Sitting",
        "Standing",
        "Laying"
    };

    public static String getLabel(int classIdx) {
        if (classIdx >= 0 && classIdx < LABELS.length) {
            return LABELS[classIdx];
        }
        return "Detecting...";
    }

    public static int getDrawableRes(int classIdx) {
        switch (classIdx) {
            case 0: return R.drawable.ic_walking;
            case 1: return R.drawable.ic_walking_upstairs;
            case 2: return R.drawable.ic_walking_downstairs;
            case 3: return R.drawable.ic_sitting;
            case 4: return R.drawable.ic_standing;
            case 5: return R.drawable.ic_laying;
            default: return R.drawable.ic_walking;
        }
    }

    public static int getColorRes(int classIdx) {
        switch (classIdx) {
            case 0: return R.color.color_walking;
            case 1: return R.color.color_walking_upstairs;
            case 2: return R.color.color_walking_downstairs;
            case 3: return R.color.color_sitting;
            case 4: return R.color.color_standing;
            case 5: return R.color.color_laying;
            default: return R.color.colorPrimary;
        }
    }

    public static String getDescription(int classIdx) {
        switch (classIdx) {
            case 0: return "Rhythmic, continuous forward motion detected via accelerometer and gyroscope cadence.";
            case 1: return "Ascending stair steps. Characterized by vertical acceleration spikes and pitch angle changes.";
            case 2: return "Descending stair steps. Characterized by downward impact deceleration spikes.";
            case 3: return "Stationary seated posture. Low total acceleration variance with steady gravity orientation.";
            case 4: return "Stationary upright posture. Minimal linear motion with subtle postural balance sway.";
            case 5: return "Horizontal reclining posture. Device gravity vector aligned with horizontal body plane.";
            default: return "Activity detected using smartphone motion sensors and Bi-LSTM model.";
        }
    }

    public static final String PLACEMENT_TIP = "For best recognition accuracy, keep the smartphone positioned consistently in your hand or front trouser pocket during movement.";
}
