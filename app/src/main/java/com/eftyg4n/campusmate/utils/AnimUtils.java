package com.eftyg4n.campusmate.utils;

import android.view.View;
import android.view.animation.DecelerateInterpolator;

// Entrance animation shared by the welcome screen and the Home dashboard:
// every view fades in while sliding up a little, one after another.
public final class AnimUtils {

    private static final long DURATION_MS = 380;
    private static final long STEP_MS = 70;

    private AnimUtils() {
    }

    public static void staggerIn(View... views) {
        for (int i = 0; i < views.length; i++) {
            View view = views[i];
            if (view == null || view.getVisibility() != View.VISIBLE) {
                continue;
            }
            float offset = 24 * view.getResources().getDisplayMetrics().density;
            view.setAlpha(0f);
            view.setTranslationY(offset);
            view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(i * STEP_MS)
                    .setDuration(DURATION_MS)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }
}
