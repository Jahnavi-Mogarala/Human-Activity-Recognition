package com.example.app.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {SessionEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public abstract SessionDao sessionDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    Context appContext = (context != null) ? context.getApplicationContext() : null;
                    if (appContext == null) {
                        throw new IllegalStateException("Context passed to AppDatabase.getInstance() is null and database is not initialized.");
                    }
                    INSTANCE = Room.databaseBuilder(
                            appContext,
                            AppDatabase.class,
                            "motionshield-db"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}
