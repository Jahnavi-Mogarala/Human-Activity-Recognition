package com.example.app;

import java.util.ArrayDeque;

/**
 * Tracks recent sensor timestamps and computes a rolling observed sampling frequency.
 * Keeps timestamps within a configurable time window (nanoseconds).
 */
public class SamplingRateTracker {
    private final long windowNs; // e.g., 2_000_000_000L for 2 seconds
    private final ArrayDeque<Long> timestamps = new ArrayDeque<>();

    public SamplingRateTracker(long windowNs) {
        this.windowNs = windowNs;
    }

    /** Add a new sensor event timestamp (nanoseconds). */
    public void addTimestamp(long ts) {
        timestamps.addLast(ts);
        // Remove timestamps older than the window
        long cutoff = ts - windowNs;
        while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
            timestamps.removeFirst();
        }
    }

    /**
     * Returns the observed sampling frequency in Hz, or -1 if not enough data.
     * Requires at least two timestamps within the window.
     */
    public double getRateHz() {
        if (timestamps.size() < 2) {
            return -1; // insufficient samples
        }
        long first = timestamps.peekFirst();
        long last = timestamps.peekLast();
        long deltaNs = last - first;
        if (deltaNs <= 0) return -1;
        double rate = (timestamps.size() - 1) * 1e9 / (double) deltaNs;
        // Round to one decimal place for UI stability
        return Math.round(rate * 10.0) / 10.0;
    }
}
