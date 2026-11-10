package com.eftyg4n.campusmate.services;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.eftyg4n.campusmate.models.Deadline;
import com.eftyg4n.campusmate.notifications.NotificationHelper;
import com.eftyg4n.campusmate.notifications.ReminderScheduler;
import com.eftyg4n.campusmate.repositories.DeadlineRepository;

// WorkManager runs doWork() on a background thread when the planned time arrives.
public class ReminderWorker extends Worker {

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String deadlineId = getInputData().getString(ReminderScheduler.KEY_DEADLINE_ID);
        if (deadlineId == null) {
            return Result.failure();
        }

        // Read the CURRENT data: the student may have edited, completed or deleted it.
        Deadline deadline = new DeadlineRepository(getApplicationContext()).getById(deadlineId);
        if (deadline == null || deadline.isCompleted()) {
            return Result.success();
        }

        NotificationHelper.showDeadlineReminder(getApplicationContext(), deadline);
        return Result.success();
    }
}
