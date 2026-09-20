package com.example.app;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory;
import android.app.Application;

/**
 * Factory to create LiveMotionViewModel with Application context for Room repository.
 */
public class LiveMotionViewModelFactory implements ViewModelProvider.Factory {
    private final Application application;

    public LiveMotionViewModelFactory(Application application) {
        this.application = application;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(LiveMotionViewModel.class)) {
            return (T) new LiveMotionViewModel(application);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
