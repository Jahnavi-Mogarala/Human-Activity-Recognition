package com.example.app;

import android.content.Context;

import android.util.Log;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.Module;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.File;


import java.io.IOException;


public class InferencePhone {
    private static final String TAG = "InferencePhone";
    private Module module;
    private float[] lastScores;

    public InferencePhone(Context context) {
        try {
            // Load the TorchScript model from assets using LiteModuleLoader
            String modelPath = copyAsset(context, "bilstm_attention.pt");
            module = Module.load(modelPath);
        } catch (IOException e) {
            Log.e(TAG, "Error loading model", e);
        }
    }

    /**
     * Copies a model asset to the app's internal storage and returns the absolute file path.
     * This is required because LiteModuleLoader expects a file system path.
     */
    private String copyAsset(Context context, String assetName) throws IOException {
        File outFile = new File(context.getFilesDir(), assetName);
        if (outFile.exists() && outFile.length() > 0) {
            return outFile.getAbsolutePath();
        }
        try (InputStream is = context.getAssets().open(assetName);
             FileOutputStream fos = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = is.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
            fos.flush();
        }
        return outFile.getAbsolutePath();
    }

    /**
     * Run inference on a 128‑step window of 6‑channel sensor data.
     * @param window float[128][6] flattened to a 1‑D float array of length 768.
     * @return predicted class index (0‑5)
     */
    public int predict(float[] window) {
        if (module == null) {
            Log.e(TAG, "Model not loaded");
            return -1;
        }
        // Input shape expected: (1, 128, 6)
        Tensor inputTensor = Tensor.fromBlob(window, new long[]{1, 128, 6});
        Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();
        float[] scores = outputTensor.getDataAsFloatArray();
        // Store scores for UI
        this.lastScores = scores;
        // Find argmax
        int pred = 0;
        for (int i = 1; i < scores.length; i++) {
            if (scores[i] > scores[pred]) pred = i;
        }
        return pred;
    }
    public float[] getLastScores() {
        return lastScores != null ? lastScores.clone() : null;
    }
}
