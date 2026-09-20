package com.example.app;

import android.app.Application;
import androidx.room.Room;
import com.example.app.database.AppDatabase;

public class MotionShieldApp extends Application {
    private static AppDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();
        database = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "motionshield-db")
                .fallbackToDestructiveMigration()
                .build();
    }

    public static AppDatabase getDatabase() {
        return database;
    }
}
