package com.eftyg4n.campusmate.network;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

// ONE RequestQueue for the whole app (each queue starts its own threads and cache).
// getApplicationContext(): never keep an Activity here, it would leak.
public class VolleySingleton {

    private static VolleySingleton instance;
    private final RequestQueue queue;

    private VolleySingleton(Context context) {
        queue = Volley.newRequestQueue(context.getApplicationContext());
    }

    public static synchronized VolleySingleton getInstance(Context context) {
        if (instance == null) {
            instance = new VolleySingleton(context);
        }
        return instance;
    }

    public <T> void add(Request<T> request) {
        queue.add(request);
    }

    // Screens cancel their requests in onDestroy so no callback arrives after they are gone.
    public void cancelAll(Object tag) {
        queue.cancelAll(tag);
    }
}
