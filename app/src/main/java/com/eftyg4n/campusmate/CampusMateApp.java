package com.eftyg4n.campusmate;

import android.app.Application;
import android.util.Log;

import com.eftyg4n.campusmate.database.KuetSeedData;

public class CampusMateApp extends Application {

    private static final String TAG = "CampusMate_DB";

    @Override
    public void onCreate() {
        super.onCreate();

        // First-run data: runs only when the tables are empty (idempotent)
        boolean seeded = KuetSeedData.seedIfEmpty(this);
        KuetSeedData.seedClassData(this);
        Log.d(TAG, "App start: semester data seeded now = " + seeded);
    }
}