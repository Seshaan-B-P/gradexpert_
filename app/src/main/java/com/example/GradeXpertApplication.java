package com.example;

import android.app.Application;
import android.util.Log;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.database.DatabaseHelper;
import com.example.database.FirestoreHelper;
import com.example.utils.SessionManager;
import com.google.firebase.FirebaseApp;

/**
 * Application class initializing global configuration such as Saved Night Mode Theme and Automatic Cloud Sync.
 */
public class GradeXpertApplication extends Application {

    private static final String TAG = "GradeXpertApp";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            FirebaseApp.initializeApp(this);
        } catch (Exception e) {
            Log.e(TAG, "FirebaseApp init error: " + e.getMessage());
        }

        try {
            SessionManager sessionManager = new SessionManager(this);
            if (sessionManager.isDarkMode()) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        } catch (Exception e) {
            Log.e(TAG, "Theme init error: " + e.getMessage());
        }

        // Automatic background cloud synchronization from Firebase Firestore to local cache
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                DatabaseHelper dbHelper = new DatabaseHelper(getApplicationContext());
                FirestoreHelper.getInstance().syncFirestoreDataToLocal(dbHelper, null);
                com.example.repository.ResultRepository.getInstance(getApplicationContext()).syncPendingLocalResults();
            } catch (Exception e) {
                Log.e(TAG, "Background sync error: " + e.getMessage());
            }
        }).start();
    }
}


