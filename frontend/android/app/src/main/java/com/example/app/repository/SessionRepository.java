package com.example.app.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.app.database.AppDatabase;
import com.example.app.database.SessionDao;
import com.example.app.database.SessionEntity;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/**
 * Repository that provides an API for accessing session data.
 * It runs DB operations on a background executor.
 */
public class SessionRepository {
    private final SessionDao sessionDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<List<SessionEntity>> allSessions = new MutableLiveData<>();

    public SessionRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        this.sessionDao = db.sessionDao();
        // Load initial data
        loadAllSessions();
    }

    private void loadAllSessions() {
        executor.execute(() -> {
            List<SessionEntity> sessions = sessionDao.getAllSessions();
            allSessions.postValue(sessions);
        });
    }

    public LiveData<List<SessionEntity>> getAllSessions() {
        return allSessions;
    }

    public void insert(SessionEntity session) {
        executor.execute(() -> {
            sessionDao.insert(session);
            // Refresh list after insertion
            loadAllSessions();
        });
    }
}
