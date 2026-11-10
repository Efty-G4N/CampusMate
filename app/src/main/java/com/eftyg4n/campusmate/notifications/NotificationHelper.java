package com.eftyg4n.campusmate.notifications;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.format.DateUtils;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.activities.DeadlinesActivity;
import com.eftyg4n.campusmate.activities.ResourceListActivity;
import com.eftyg4n.campusmate.models.Deadline;
import com.eftyg4n.campusmate.services.DownloadService;

public class NotificationHelper {

    // Channel ids must never change after release (users keep their channel settings).
    public static final String CHANNEL_DEADLINES = "campusmate_deadlines";
    public static final String CHANNEL_DOWNLOADS = "campusmate_downloads";

    private NotificationHelper() {
    }

    // minSdk is 26, so notification channels are always required: no version check needed.
    // Safe to call many times (Android ignores a channel that already exists).
    public static void createChannels(Context context) {
        NotificationChannel deadlines = new NotificationChannel(
                CHANNEL_DEADLINES,
                context.getString(R.string.notif_channel_deadlines_name),
                NotificationManager.IMPORTANCE_HIGH);
        deadlines.setDescription(context.getString(R.string.notif_channel_deadlines_desc));

        // LOW importance: a progress bar must not beep on every update.
        NotificationChannel downloads = new NotificationChannel(
                CHANNEL_DOWNLOADS,
                context.getString(R.string.notif_channel_downloads_name),
                NotificationManager.IMPORTANCE_LOW);
        downloads.setDescription(context.getString(R.string.notif_channel_downloads_desc));

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(deadlines);
            manager.createNotificationChannel(downloads);
        }
    }

    // Android 13+ needs the POST_NOTIFICATIONS runtime permission.
    // The user can also switch notifications off for the whole app in Settings.
    public static boolean canPostNotifications(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    @SuppressLint("MissingPermission") // canPostNotifications() checks it just above.
    public static void showDeadlineReminder(Context context, Deadline deadline) {
        if (!canPostNotifications(context)) {
            return;
        }

        // Tap -> DeadlinesActivity. The "parent stack" (from the Manifest's
        // parentActivityName) puts MainActivity under it, so Back goes to the app.
        Intent intent = new Intent(context, DeadlinesActivity.class);
        int requestCode = deadline.getId().hashCode();
        PendingIntent pendingIntent = TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(intent)
                .getPendingIntent(requestCode,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String due = DateUtils.formatDateTime(context, deadline.getDueAt(),
                DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_DATE
                        | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_ALL);
        String text = context.getString(R.string.notif_deadline_text, due);
        if (deadline.getCourseCode() != null) {
            text = deadline.getCourseCode() + " · " + text;
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_DEADLINES)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(deadline.getTitle())
                        .setContentText(text)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_REMINDER)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

        // The same deadline reminded twice would replace the older notification:
        // use a different id per deadline AND per reminder time if you want both visible.
        NotificationManagerCompat.from(context).notify(requestCode, builder.build());
    }

    // ---------- Downloads (used by DownloadService) ----------

    // Tap -> the resource list, with MainActivity under it (Manifest parentActivityName).
    private static PendingIntent resourceListIntent(Context context) {
        return TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(new Intent(context, ResourceListActivity.class))
                .getPendingIntent(2001,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    // percent < 0 = unknown size (indeterminate bar). This is the notification the
    // foreground service must show for as long as it runs.
    public static Notification buildDownloadProgress(Context context, String resourceId,
                                                     String title, int percent) {
        Intent cancel = new Intent(context, DownloadService.class)
                .setAction(DownloadService.ACTION_CANCEL)
                .putExtra(DownloadService.EXTRA_RESOURCE_ID, resourceId);
        PendingIntent cancelIntent = PendingIntent.getService(context, resourceId.hashCode(),
                cancel, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String text = percent < 0
                ? context.getString(R.string.notif_download_waiting)
                : context.getString(R.string.notif_download_percent, percent);

        return new NotificationCompat.Builder(context, CHANNEL_DOWNLOADS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.notif_download_title, title))
                .setContentText(text)
                .setProgress(100, Math.max(percent, 0), percent < 0)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setContentIntent(resourceListIntent(context))
                .addAction(0, context.getString(R.string.notif_download_cancel), cancelIntent)
                .build();
    }

    @SuppressLint("MissingPermission") // canPostNotifications() checks it.
    public static void updateDownloadProgress(Context context, int notificationId,
                                              String resourceId, String title, int percent) {
        if (canPostNotifications(context)) {
            NotificationManagerCompat.from(context).notify(notificationId,
                    buildDownloadProgress(context, resourceId, title, percent));
        }
    }

    // Completion (or failure) notification. It is separate from the progress one,
    // because the progress notification disappears when the service stops.
    @SuppressLint("MissingPermission") // canPostNotifications() checks it.
    public static void showDownloadResult(Context context, String resourceId, String title,
                                          boolean success, int reasonTextRes) {
        if (!canPostNotifications(context)) {
            return;
        }
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_DOWNLOADS)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(context.getString(success
                                ? R.string.notif_download_done : reasonTextRes))
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setContentIntent(resourceListIntent(context))
                        .setAutoCancel(true);
        NotificationManagerCompat.from(context).notify(resourceId.hashCode(), builder.build());
    }
}
