package com.eftyg4n.campusmate.utils;

import android.content.Context;
import android.os.PowerManager;
import android.provider.Settings;

// Reads the CURRENT state of the system (no permission needed).
public class SystemStatusUtils {

    private SystemStatusUtils() {
    }

    public static boolean isAirplaneModeOn(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    public static boolean isPowerSaveOn(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return powerManager != null && powerManager.isPowerSaveMode();
    }
}