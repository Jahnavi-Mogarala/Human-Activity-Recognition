package com.example.app;

/**
 * Circular buffer that stores exactly 128 samples, each sample containing
 * six sensor values in the order [acc_x, acc_y, acc_z, gyro_x, gyro_y, gyro_z].
 * The buffer does not overwrite old data; it simply stops accepting new
 * samples once it is full. After inference the buffer can be cleared.
 */
public class SensorBuffer {
    private static final int WINDOW_SIZE = 128;
    private static final int STRIDE = 25; // predict every 0.5 sec
    private static final int FEATURE_COUNT = 6;
    private final float[][] buffer = new float[WINDOW_SIZE][FEATURE_COUNT];
    private int index = 0;
    private int samplesSinceLastWindow = 0;

    /**
     * Adds a new six‑feature sample to the buffer.
     * @param sample float[6] containing accelerometer then gyroscope values.
     * @throws IllegalArgumentException if the sample length != 6.
     */
    public void addSample(float[] sample) {
        if (sample == null || sample.length != FEATURE_COUNT) {
            throw new IllegalArgumentException("Sample must have exactly 6 float values");
        }
        
        if (index < WINDOW_SIZE) {
            System.arraycopy(sample, 0, buffer[index], 0, FEATURE_COUNT);
            index++;
            if (index == WINDOW_SIZE) {
                samplesSinceLastWindow = STRIDE; // force isWindowReady true immediately on first fill
            }
        } else {
            // Shift left by 1
            for (int i = 0; i < WINDOW_SIZE - 1; i++) {
                System.arraycopy(buffer[i+1], 0, buffer[i], 0, FEATURE_COUNT);
            }
            System.arraycopy(sample, 0, buffer[WINDOW_SIZE - 1], 0, FEATURE_COUNT);
            samplesSinceLastWindow++;
        }
    }

    public boolean isWindowReady() {
        return index == WINDOW_SIZE && samplesSinceLastWindow >= STRIDE;
    }

    public void markWindowRead() {
        samplesSinceLastWindow = 0;
    }

    public boolean isFull() {
        return index == WINDOW_SIZE;
    }

    /** Returns the window as a 2‑D array [128][6] in chronological order. */
    public float[][] getWindow() {
        if (!isFull()) {
            throw new IllegalStateException("Buffer not full yet");
        }
        // Return a copy to avoid external mutation
        float[][] copy = new float[WINDOW_SIZE][FEATURE_COUNT];
        for (int i = 0; i < WINDOW_SIZE; i++) {
            System.arraycopy(buffer[i], 0, copy[i], 0, FEATURE_COUNT);
        }
        return copy;
    }

    // Reset buffer for a new session.
    public void clear() {
        index = 0;
        samplesSinceLastWindow = 0;
    }

    // Returns the number of samples currently stored (0‑128).
    public int getCurrentSize() {
        return index;
    }
}
