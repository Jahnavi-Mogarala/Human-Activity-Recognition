package com.example.app;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class InferencePhone {
    private static final String TAG = "InferencePhone";
    private Module module;

    public InferencePhone(Context context) {
        try {
            // Load the TorchScript model from assets
            AssetFileDescriptor fileDescriptor = context.getAssets().openFd("bilstm_attention.pt");
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            MappedByteBuffer modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
            module = Module.load(modelBuffer);
        } catch (IOException e) {
            Log.e(TAG, "Error loading model", e);
        }
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
        // Find argmax
        int pred = 0;
        for (int i = 1; i < scores.length; i++) {
            if (scores[i] > scores[pred]) pred = i;
        }
        return pred;
    }
}
