package com.utshob.mybikes;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.database.FirebaseDatabase;
import com.utshob.mybikes.util.Prefs;
import com.utshob.mybikes.work.NotificationHelper;

public class MyBikesApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            // Keeps data available offline and syncs when the phone is online again.
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Exception ignored) {
            // Already enabled.
        }
        Prefs.init(this);
        AppCompatDelegate.setDefaultNightMode(Prefs.themeMode());
        NotificationHelper.createChannel(this);
    }
}
