package com.example.app;

/**
 * Preprocess applies the exact scaling parameters that were saved during model training.
 * The scaling formula is (raw - mean) / std for each of the six sensor channels.
 *
 * Input: float[128][6] – 128 chronological samples, each sample already ordered as
 *        [acc_x, acc_y, acc_z, gyro_x, gyro_y, gyro_z].
 * Output: float[768] – flattened array suitable for InferencePhone.predict().
 */
public class Preprocess {
    // Values taken directly from models/smartphone_har/scaler.pkl
    private static final float[] MEAN = {
        -0.00098865f, -0.00042587f, -0.00083114f,
         0.00263046f, -0.00176406f,  0.00078994f
    };
    private static final float[] STD = {
        0.19412018f, 0.12827216f, 0.10815313f,
        0.42995446f, 0.40469554f, 0.2493495f
    };

    /**
     * Apply scaling and flatten the window.
     * @param window float[128][6] raw sensor values
     * @return flattened float[768] ready for inference
     */
    public float[] apply(float[][] window) {
        if (window == null || window.length != 128) {
            throw new IllegalArgumentException("Window must have 128 samples");
        }
        float[] flat = new float[128 * 6];
        int idx = 0;
        for (int i = 0; i < 128; i++) {
            float[] sample = window[i];
            if (sample == null || sample.length != 6) {
                throw new IllegalArgumentException("Each sample must contain 6 values");
            }
            for (int f = 0; f < 6; f++) {
                flat[idx++] = (sample[f] - MEAN[f]) / STD[f];
            }
        }
        return flat;
    }
}
