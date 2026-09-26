package com.example.app.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sessions")
public class SessionEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long timestamp; // epoch ms
    public long durationMs;
    public String dominantActivity;
    public float avgConfidence;
    public int predictionCount;
    public float avgLatency;

    public SessionEntity(long timestamp, long durationMs, String dominantActivity, float avgConfidence, int predictionCount, float avgLatency) {
        this.timestamp = timestamp;
        this.durationMs = durationMs;
        this.dominantActivity = dominantActivity;
        this.avgConfidence = avgConfidence;
        this.predictionCount = predictionCount;
        this.avgLatency = avgLatency;
    }
    public long getId() { return id; }
    public long getTimestamp() { return timestamp; }
    public long getDurationMs() { return durationMs; }
    public String getDominantActivity() { return dominantActivity; }
    public float getAvgConfidence() { return avgConfidence; }
    public int getPredictionCount() { return predictionCount; }
    public float getAvgLatency() { return avgLatency; }
}
