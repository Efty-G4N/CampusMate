package com.eftyg4n.campusmate.utils;

import java.util.Arrays;

// The arithmetic of the Marks screen, kept free of Android so it can be unit tested.
public final class MarksUtils {

    // KUET counts the best few class tests; the Marks screen uses this many.
    public static final int BEST_CT_COUNT = 3;

    private MarksUtils() {
    }

    public static double percent(double obtained, double total) {
        return total <= 0 ? 0 : obtained * 100.0 / total;
    }

    // Average percentage of the best n scores (all of them when there are fewer than n).
    public static double bestAveragePercent(double[] percents, int n) {
        if (percents.length == 0 || n <= 0) {
            return 0;
        }
        double[] sorted = Arrays.copyOf(percents, percents.length);
        Arrays.sort(sorted);                      // ascending
        int count = Math.min(n, sorted.length);
        double sum = 0;
        for (int i = 0; i < count; i++) {
            sum += sorted[sorted.length - 1 - i];
        }
        return sum / count;
    }

    // Accepts "7", "7.5" and "7,5"; returns -1 for anything else.
    public static double parseNumber(String text) {
        if (text == null) {
            return -1;
        }
        try {
            double v = Double.parseDouble(text.trim().replace(',', '.'));
            return Double.isNaN(v) || Double.isInfinite(v) ? -1 : v;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // "7.5" for 7.5 but "8" for 8.0
    public static String plain(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value)
                : String.format(java.util.Locale.US, "%.2f", value);
    }
}
