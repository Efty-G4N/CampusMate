package com.eftyg4n.campusmate.utils;

// The attendance arithmetic, kept free of Android classes so it can be unit tested.
// "attended" = present + late, "total" = every recorded class day.
public final class AttendanceUtils {

    public static final int DEFAULT_REQUIRED_PERCENT = 75;

    private AttendanceUtils() {
    }

    public static double percent(int attended, int total) {
        return total == 0 ? 0 : attended * 100.0 / total;
    }

    // True while attended / total is at or above the required percentage.
    public static boolean isOnTrack(int attended, int total, int requiredPercent) {
        return total == 0 || attended * 100 >= requiredPercent * total;
    }

    // How many classes in a row can still be missed without dropping below the requirement.
    // Largest m with attended / (total + m) >= required / 100.
    public static int canMiss(int attended, int total, int requiredPercent) {
        int m = attended * 100 / requiredPercent - total;
        return Math.max(m, 0);
    }

    // How many classes in a row must be attended to get back to the requirement.
    // Smallest n with (attended + n) / (total + n) >= required / 100.
    // Returns -1 when it can never happen (required is 100% and a class was already missed).
    public static int needToAttend(int attended, int total, int requiredPercent) {
        int need = requiredPercent * total - attended * 100;
        if (need <= 0) {
            return 0;
        }
        if (requiredPercent >= 100) {
            return -1;
        }
        int gain = 100 - requiredPercent;       // each attended class closes this much
        return (need + gain - 1) / gain;        // ceiling division
    }
}
