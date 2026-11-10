package com.eftyg4n.campusmate;

import android.app.Application;
import android.util.Log;

import com.eftyg4n.campusmate.database.KuetSeedData;
import com.eftyg4n.campusmate.notifications.NotificationHelper;
import com.google.firebase.database.FirebaseDatabase;

public class CampusMateApp extends Application {

    private static final String TAG = "CampusMate_DB";

    @Override
    public void onCreate() {
        super.onCreate();

        // Realtime Database keeps a copy on disk and queues writes (and deletes) made
        // offline. It must be switched on ONCE, before any other Firebase Database call.
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (RuntimeException e) {
            // Usually: the database URL is missing from google-services.json.
            Log.w(TAG, "Realtime Database not ready: " + e.getMessage());
        }

        // Notification channels must exist before the first notification is shown.
        NotificationHelper.createChannels(this);

        // First-run data: runs only when the tables are empty (idempotent)
        boolean seeded = KuetSeedData.seedIfEmpty(this);
        KuetSeedData.seedClassData(this);
        KuetSeedData.seedRoutineData(this);
        Log.d(TAG, "App start: semester data seeded now = " + seeded);
    }
}