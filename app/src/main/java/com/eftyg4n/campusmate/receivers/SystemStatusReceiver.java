package com.eftyg4n.campusmate.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.util.Log;

import com.eftyg4n.campusmate.utils.SystemStatusUtils;

// Listens for Airplane mode and Battery Saver changes.
// Registered in code (not in the Manifest), see HomeFragment.
public class SystemStatusReceiver extends BroadcastReceiver {

    private static final String TAG = "CampusMate_Receiver";

    // The screen that owns the receiver implements this to update itself.
    public interface Listener {
        void onAirplaneModeChanged(boolean enabled);

        void onPowerSaveChanged(boolean enabled);
    }

    private final Listener listener;

    public SystemStatusReceiver(Listener listener) {
        this.listener = listener;
    }

    // Which broadcasts this receiver wants to hear.
    public static IntentFilter createFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
        return filter;
    }

    // Runs on the main thread, so it must stay short.
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }

        if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
            boolean enabled = SystemStatusUtils.isAirplaneModeOn(context);
            Log.d(TAG, "Airplane mode changed: " + enabled);
            listener.onAirplaneModeChanged(enabled);
        } else if (PowerManager.ACTION_POWER_SAVE_MODE_CHANGED.equals(action)) {
            boolean enabled = SystemStatusUtils.isPowerSaveOn(context);
            Log.d(TAG, "Battery Saver changed: " + enabled);
            listener.onPowerSaveChanged(enabled);
        }
    }
}