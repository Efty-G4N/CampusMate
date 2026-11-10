package com.eftyg4n.campusmate.utils;

import android.content.Context;
import android.os.SystemClock;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;

import com.eftyg4n.campusmate.R;

// Optional app lock: fingerprint / face, with the phone's screen lock as the fallback.
// It protects the data ON the phone, which a sign-in could not do.
public final class AppLock {

    // Leaving the app for less than this does not ask again (a quick look at a message).
    private static final long RELOCK_AFTER_MS = 30_000;

    private static final int AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_WEAK
            | BiometricManager.Authenticators.DEVICE_CREDENTIAL;

    // Lives only as long as the app process: a fresh start always asks once.
    private static boolean unlockedThisRun;
    private static long stoppedAt;

    private AppLock() {
    }

    // False when the phone has no screen lock or fingerprint set up (we must never lock a
    // student out of their own data because of that).
    public static boolean canUseLock(Context context) {
        return BiometricManager.from(context).canAuthenticate(AUTHENTICATORS)
                == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public static boolean shouldLockNow(Context context) {
        if (!AppPrefs.isLockEnabled(context) || !canUseLock(context)) {
            return false;
        }
        if (!unlockedThisRun) {
            return true;
        }
        return stoppedAt != 0 && SystemClock.elapsedRealtime() - stoppedAt > RELOCK_AFTER_MS;
    }

    public static void markUnlocked() {
        unlockedThisRun = true;
        stoppedAt = 0;
    }

    public static void markStopped() {
        stoppedAt = SystemClock.elapsedRealtime();
    }

    public static BiometricPrompt.PromptInfo promptInfo(Context context) {
        return new BiometricPrompt.PromptInfo.Builder()
                .setTitle(context.getString(R.string.lock_prompt_title))
                .setSubtitle(context.getString(R.string.lock_prompt_subtitle))
                .setAllowedAuthenticators(AUTHENTICATORS)
                .build();
    }
}
