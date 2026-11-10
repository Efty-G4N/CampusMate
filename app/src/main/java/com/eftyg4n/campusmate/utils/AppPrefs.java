package com.eftyg4n.campusmate.utils;

import android.content.Context;
import android.content.SharedPreferences;

// Small app-wide settings (SharedPreferences): the data itself lives in SQLite.
public final class AppPrefs {

    public static final String FILE = "campusmate_prefs";
    private static final String KEY_ONBOARDED = "onboarded";
    private static final String KEY_REQUIRED_ATTENDANCE = "attendance_required_percent";

    private AppPrefs() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    // True once the student has passed the welcome screen (any of its three choices).
    public static boolean isOnboarded(Context context) {
        return prefs(context).getBoolean(KEY_ONBOARDED, false);
    }

    public static void setOnboarded(Context context) {
        prefs(context).edit().putBoolean(KEY_ONBOARDED, true).apply();
    }

    // Same value the Attendance screen lets the student change.
    public static int requiredAttendance(Context context) {
        return prefs(context).getInt(KEY_REQUIRED_ATTENDANCE,
                AttendanceUtils.DEFAULT_REQUIRED_PERCENT);
    }
}
