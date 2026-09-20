package com.example.app;

import static org.junit.Assert.*;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class LiveMotionViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private LiveMotionViewModel viewModel;

    @Before
    public void setUp() {
        viewModel = new LiveMotionViewModel();
    }

    @Test
    public void movementLowVariance_resultsInLowMovement() {
        // Add several identical accel samples (magnitude constant)
        long base = System.nanoTime();
        for (int i = 0; i < 20; i++) {
            viewModel.addAccelSample(base + i * 10_000_000L, 0.1f, 0.1f, 0.1f);
        }
        // movement status is updated inside addAccelSample
        assertEquals("Low movement", viewModel.getMovementStatus().getValue());
    }

    @Test
    public void movementHighVariance_resultsInMoving() {
        long base = System.nanoTime();
        // Vary magnitude deliberately
        viewModel.addAccelSample(base, 0.1f, 0.1f, 0.1f);
        viewModel.addAccelSample(base + 10_000_000L, 5f, 5f, 5f);
        viewModel.addAccelSample(base + 20_000_000L, 0.2f, 0.2f, 0.2f);
        assertEquals("Moving", viewModel.getMovementStatus().getValue());
    }

    @Test
    public void latencyAccumulation_andAverageLatency() {
        viewModel.addLatency(10_000_000L); // 10 ms
        viewModel.addLatency(20_000_000L); // 20 ms
        // avgLatencyNs should be (10+20)/2 = 15_000_000 ns
        assertEquals(15_000_000L, viewModel.getAvgLatencyNs().getValue().longValue());
    }

    @Test
    public void resetAll_clearsState() {
        // Populate some state first
        viewModel.setActivityPrediction(1, new float[]{0.1f,0.2f,0.3f,0.1f,0.2f,0.1f}, 5_000_000L);
        viewModel.setSensorRates(50.0, 50.0);
        viewModel.resetAll();
        assertEquals(Integer.valueOf(-1), viewModel.getActivityIndex().getValue());
        assertNull(viewModel.getRawScores().getValue());
        assertEquals(Double.valueOf(-1.0), viewModel.getAccelHz().getValue());
        assertEquals(Double.valueOf(-1.0), viewModel.getGyroHz().getValue());
        assertEquals(Integer.valueOf(0), viewModel.getPredictionCount().getValue());
        assertEquals("READY", viewModel.getSessionStatus().getValue());
    }

    @Test
    public void predictionCount_increments() {
        viewModel.setActivityPrediction(0, new float[]{0.5f,0.1f,0.1f,0.1f,0.1f,0.1f}, 1_000_000L);
        viewModel.setActivityPrediction(1, new float[]{0.2f,0.6f,0.1f,0.05f,0.03f,0.02f}, 1_200_000L);
        assertEquals(Integer.valueOf(2), viewModel.getPredictionCount().getValue());
    }

    @Test
    public void confidenceScores_areExposed() {
        float[] scores = new float[]{0.1f,0.2f,0.3f,0.15f,0.15f,0.1f};
        viewModel.setActivityPrediction(2, scores, 2_000_000L);
        assertArrayEquals(scores, viewModel.getRawScores().getValue(), 0.0001f);
    }

    @Test
    public void windowProgress_updatesLiveData() {
        viewModel.setWindowSize(64);
        assertEquals(Integer.valueOf(64), viewModel.getWindowSize().getValue());
    }

    @Test
    public void sessionState_updatesLiveData() {
        viewModel.setSessionStatus("COLLECTING");
        assertEquals("COLLECTING", viewModel.getSessionStatus().getValue());
    }

    @Test
    public void sensorRate_updatesLiveData() {
        viewModel.setSensorRates(55.5, 48.3);
        assertEquals(Double.valueOf(55.5), viewModel.getAccelHz().getValue());
        assertEquals(Double.valueOf(48.3), viewModel.getGyroHz().getValue());
    }
}
