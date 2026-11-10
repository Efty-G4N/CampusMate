package com.eftyg4n.campusmate.services;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.webkit.MimeTypeMap;

import androidx.annotation.Nullable;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.notifications.NotificationHelper;
import com.eftyg4n.campusmate.repositories.ResourceRepository;
import com.eftyg4n.campusmate.utils.ResourceFileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// FOREGROUND service: downloads a web file into the app's own storage.
//  - the user sees an ongoing notification with a progress bar and a Cancel button
//  - Android will not kill it quickly, because it is a foreground service
//  - downloads run one after another on a single background thread
//  - when the queue is empty the service stops itself
public class DownloadService extends Service {

    public static final String ACTION_START = "com.eftyg4n.campusmate.action.DOWNLOAD_START";
    public static final String ACTION_CANCEL = "com.eftyg4n.campusmate.action.DOWNLOAD_CANCEL";
    // Broadcast sent when ONE download ends (success or not).
    public static final String ACTION_DOWNLOAD_FINISHED =
            "com.eftyg4n.campusmate.action.DOWNLOAD_FINISHED";

    public static final String EXTRA_RESOURCE_ID = "extra_resource_id";
    public static final String EXTRA_URL = "extra_url";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_SUCCESS = "extra_success";
    public static final String EXTRA_REASON = "extra_reason";

    public static final int REASON_NONE = 0;
    public static final int REASON_NETWORK = 1;
    public static final int REASON_NOT_A_FILE = 2;
    public static final int REASON_CANCELLED = 3;
    public static final int REASON_INSECURE = 4;

    private static final int FOREGROUND_ID = 1001;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Set<String> cancelledIds = ConcurrentHashMap.newKeySet();
    private ExecutorService executor;
    private int pendingJobs; // touched on the main thread only

    // Any screen can start a download with one line.
    public static void start(Context context, String resourceId, String url, String title) {
        Intent intent = new Intent(context, DownloadService.class)
                .setAction(ACTION_START)
                .putExtra(EXTRA_RESOURCE_ID, resourceId)
                .putExtra(EXTRA_URL, url)
                .putExtra(EXTRA_TITLE, title);
        // Not startService(): from API 26 a background-started service must promise
        // to call startForeground() within a few seconds. We do that in onStartCommand.
        ContextCompat.startForegroundService(context, intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();

        if (ACTION_CANCEL.equals(action)) {
            String id = intent.getStringExtra(EXTRA_RESOURCE_ID);
            if (id != null) {
                cancelledIds.add(id);
            }
            return START_NOT_STICKY;
        }

        if (!ACTION_START.equals(action)) {
            if (pendingJobs == 0) {
                stopSelf();
            }
            return START_NOT_STICKY;
        }

        String id = intent.getStringExtra(EXTRA_RESOURCE_ID);
        String url = intent.getStringExtra(EXTRA_URL);
        String title = intent.getStringExtra(EXTRA_TITLE);
        if (title == null) {
            title = getString(R.string.app_name);
        }

        // MUST come first, even if the extras are bad (see start() above).
        Notification first = NotificationHelper.buildDownloadProgress(
                this, id == null ? "none" : id, title, -1);
        ServiceCompat.startForeground(this, FOREGROUND_ID, first,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);

        if (id == null || url == null) {
            if (pendingJobs == 0) {
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
            }
            return START_NOT_STICKY;
        }

        pendingJobs++;
        final String jobTitle = title;
        executor.execute(() -> runJob(id, url, jobTitle));
        return START_NOT_STICKY;
    }

    // ---------- Background thread ----------

    private void runJob(String id, String url, String title) {
        NotificationHelper.updateDownloadProgress(this, FOREGROUND_ID, id, title, -1);

        int reason;
        File file = null;
        String mime = null;
        if (cancelledIds.contains(id)) {
            reason = REASON_CANCELLED;
        } else {
            DownloadResult result = download(id, url, title);
            reason = result.reason;
            file = result.file;
            mime = result.mime;
        }

        boolean success = reason == REASON_NONE;
        if (success) {
            new ResourceRepository(this).setLocalFile(id, file.getAbsolutePath(),
                    mime, file.length());
        }

        final int finalReason = reason;
        mainHandler.post(() -> finishJob(id, title, finalReason));
    }

    private static class DownloadResult {
        int reason = REASON_NETWORK;
        File file;
        String mime;
    }

    private DownloadResult download(String id, String url, String title) {
        DownloadResult result = new DownloadResult();

        // Android blocks plain http by default (cleartext). Say it clearly instead of failing.
        if (!url.toLowerCase(Locale.ROOT).startsWith("https://")) {
            result.reason = REASON_INSECURE;
            return result;
        }

        HttpURLConnection connection = null;
        File target = null;
        boolean completed = false;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(20_000);
            connection.setRequestProperty("User-Agent", "CampusMate/1.0 (student project)");

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return result; // REASON_NETWORK
            }

            String type = connection.getContentType();
            if (type != null && type.contains(";")) {
                type = type.substring(0, type.indexOf(';'));
            }
            if (type != null && type.trim().toLowerCase(Locale.ROOT).startsWith("text/html")) {
                // The link is a web page, not a downloadable file.
                result.reason = REASON_NOT_A_FILE;
                return result;
            }

            long total = connection.getContentLengthLong(); // -1 if the server does not say
            String name = fileNameFor(url, type);
            target = ResourceFileUtils.uniqueFile(ResourceFileUtils.resourcesDir(this), name);

            try (InputStream in = connection.getInputStream();
                 OutputStream out = new FileOutputStream(target)) {
                byte[] buffer = new byte[16 * 1024];
                long done = 0;
                long lastUpdate = 0;
                int read;
                while ((read = in.read(buffer)) != -1) {
                    if (cancelledIds.contains(id)) {
                        result.reason = REASON_CANCELLED;
                        return result;
                    }
                    out.write(buffer, 0, read);
                    done += read;

                    // Do not flood the system: at most ~2 notification updates per second.
                    long now = SystemClock.elapsedRealtime();
                    if (now - lastUpdate > 500) {
                        lastUpdate = now;
                        int percent = total > 0 ? (int) (done * 100 / total) : -1;
                        NotificationHelper.updateDownloadProgress(
                                this, FOREGROUND_ID, id, title, percent);
                    }
                }
            }

            result.file = target;
            result.mime = type != null ? type.trim() : ResourceFileUtils.mimeFromName(name);
            result.reason = REASON_NONE;
            completed = true;
        } catch (IOException e) {
            result.reason = REASON_NETWORK;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (!completed && target != null) {
                //noinspection ResultOfMethodCallIgnored
                target.delete(); // never keep a half-downloaded file
            }
        }
        return result;
    }

    // From the url's last part; adds an extension from the MIME type if it has none.
    private String fileNameFor(String url, String mime) {
        String last = Uri.parse(url).getLastPathSegment();
        String name = ResourceFileUtils.sanitize(last == null || last.isEmpty() ? "download" : last);
        if (!name.contains(".") && mime != null) {
            String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime.trim());
            if (ext != null) {
                name = name + "." + ext;
            }
        }
        return name;
    }

    // ---------- Main thread ----------

    private void finishJob(String id, String title, int reason) {
        cancelledIds.remove(id);
        boolean success = reason == REASON_NONE;

        if (reason != REASON_CANCELLED) {
            NotificationHelper.showDownloadResult(this, id, title, success, reasonText(reason));
        }

        // Only OUR app may receive it (setPackage), and the receiver is not exported.
        Intent finished = new Intent(ACTION_DOWNLOAD_FINISHED)
                .setPackage(getPackageName())
                .putExtra(EXTRA_RESOURCE_ID, id)
                .putExtra(EXTRA_SUCCESS, success)
                .putExtra(EXTRA_REASON, reason);
        sendBroadcast(finished);

        pendingJobs--;
        if (pendingJobs <= 0) {
            stopForeground(STOP_FOREGROUND_REMOVE); // remove the progress notification
            stopSelf();
        }
    }

    public static int reasonText(int reason) {
        switch (reason) {
            case REASON_NOT_A_FILE:
                return R.string.notif_download_failed_not_file;
            case REASON_INSECURE:
                return R.string.notif_download_failed_insecure;
            case REASON_CANCELLED:
                return R.string.notif_download_cancelled;
            default:
                return R.string.notif_download_failed_network;
        }
    }

    @Override
    public void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // started service, not a bound one
    }
}
