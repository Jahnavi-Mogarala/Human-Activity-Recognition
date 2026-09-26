package com.example.app;

import android.app.Application;
import com.example.app.database.AppDatabase;

public class MotionShieldApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppDatabase.getInstance(this);
    }

    public static AppDatabase getDatabase() {
        return AppDatabase.getInstance(null);
    }
}
