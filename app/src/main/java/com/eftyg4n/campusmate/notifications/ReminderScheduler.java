package com.eftyg4n.campusmate.notifications;

import android.content.Context;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.eftyg4n.campusmate.models.Deadline;
import com.eftyg4n.campusmate.services.ReminderWorker;

import java.util.List;
import java.util.concurrent.TimeUnit;

// Plans the reminder notifications with WorkManager.
// WorkManager keeps the plan even after the app is closed or the phone restarts.
public class ReminderScheduler {

    public static final String KEY_DEADLINE_ID = "deadline_id";

    private ReminderScheduler() {
    }

    // One tag per deadline lets us cancel all its reminders with one call.
    private static String tagOf(String deadlineId) {
        return "deadline_" + deadlineId;
    }

    public static void schedule(Context context, Deadline deadline, List<Integer> minutesList) {
        long now = System.currentTimeMillis();

        for (int minutes : minutesList) {
            long triggerAt = deadline.getDueAt() - minutes * 60_000L;
            long delay = triggerAt - now;
            if (delay < 0) {
                continue;   // that moment is already in the past
            }

            Data input = new Data.Builder()
                    .putString(KEY_DEADLINE_ID, deadline.getId())
                    .build();

            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ReminderWorker.class)
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(input)
                    .addTag(tagOf(deadline.getId()))
                    .build();

            // A unique name per reminder: scheduling the same one again replaces it.
            String uniqueName = tagOf(deadline.getId()) + "_" + minutes;
            WorkManager.getInstance(context).enqueueUniqueWork(
                    uniqueName, ExistingWorkPolicy.REPLACE, request);
        }
    }

    // Call before re-scheduling (edit) and when a deadline is deleted or completed.
    public static void cancel(Context context, String deadlineId) {
        WorkManager.getInstance(context).cancelAllWorkByTag(tagOf(deadlineId));
    }
}
