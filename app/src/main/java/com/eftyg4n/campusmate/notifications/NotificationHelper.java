package com.eftyg4n.campusmate.notifications;

import android.Manifest;
import android.annotation.SuppressLint;
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
import com.eftyg4n.campusmate.models.Deadline;

public class NotificationHelper {

    // Channel ids must never change after release (users keep their channel settings).
    public static final String CHANNEL_DEADLINES = "campusmate_deadlines";

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

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(deadlines);
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
}
