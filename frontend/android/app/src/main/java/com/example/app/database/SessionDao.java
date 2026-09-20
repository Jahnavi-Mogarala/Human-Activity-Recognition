package com.example.app.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.OnConflictStrategy;
import java.util.List;

@Dao
public interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SessionEntity session);

    @Query("SELECT * FROM sessions ORDER BY timestamp DESC")
    List<SessionEntity> getAllSessions();
}
