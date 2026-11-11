package com.eftyg4n.campusmate.utils;

import android.content.Context;
import android.widget.Toast;

import com.eftyg4n.campusmate.firebase.SyncManager;

// Personal records (attendance, marks) are opened only by a signed-in student.
// Everything else in the app stays open to guests.
public final class AccessGate {

    private AccessGate() {
    }

    public static boolean isSignedIn() {
        return SyncManager.currentUser() != null;
    }

    // True when the student may continue; otherwise shows the message and returns false.
    public static boolean require(Context context, int messageRes) {
        if (isSignedIn()) {
            return true;
        }
        Toast.makeText(context, messageRes, Toast.LENGTH_LONG).show();
        return false;
    }
}
