package com.eftyg4n.campusmate.repositories;

import static com.eftyg4n.campusmate.database.DatabaseHelper.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.eftyg4n.campusmate.database.DatabaseHelper;
import com.eftyg4n.campusmate.models.Deadline;
import com.eftyg4n.campusmate.utils.IdUtils;

import java.util.ArrayList;
import java.util.List;

public class DeadlineRepository {

    // Filters for the list screen.
    public static final int FILTER_ALL = 0;
    public static final int FILTER_PENDING = 1;
    public static final int FILTER_OVERDUE = 2;
    public static final int FILTER_COMPLETED = 3;

    // Every read query JOINs the course, so a row can show the course code and color.
    private static final String SELECT_WITH_COURSE =
            "SELECT d.*, c." + COL_COURSE_CODE + " AS course_code, c."
                    + COL_COURSE_COLOR + " AS course_color FROM " + TABLE_DEADLINE
                    + " d LEFT JOIN " + TABLE_COURSE + " c ON c." + COL_ID
                    + " = d." + COL_DEADLINE_COURSE_ID;

    private final DatabaseHelper helper;

    public DeadlineRepository(Context context) {
        helper = DatabaseHelper.getInstance(context);
    }

    // Saves the deadline AND its reminders in one transaction (all or nothing).
    // Returns the new id, or null if it failed.
    public String insert(Deadline deadline, List<Integer> reminderMinutes) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long now = System.currentTimeMillis();

        deadline.setId(IdUtils.newId());
        deadline.setCreatedAt(now);
        deadline.setUpdatedAt(now);
        deadline.setSyncStatus(SYNC_PENDING);

        db.beginTransaction();
        try {
            if (db.insert(TABLE_DEADLINE, null, toValues(deadline)) == -1) {
                return null;
            }
            replaceReminders(db, deadline.getId(), reminderMinutes, now);
            db.setTransactionSuccessful();
            return deadline.getId();
        } finally {
            db.endTransaction();
        }
    }

    public boolean update(Deadline deadline, List<Integer> reminderMinutes) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long now = System.currentTimeMillis();
        deadline.setUpdatedAt(now);
        deadline.setSyncStatus(SYNC_PENDING);

        db.beginTransaction();
        try {
            int rows = db.update(TABLE_DEADLINE, toValues(deadline),
                    COL_ID + "=?", new String[]{deadline.getId()});
            if (rows == 0) {
                return false;
            }
            replaceReminders(db, deadline.getId(), reminderMinutes, now);
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    // The reminders are deleted automatically (ON DELETE CASCADE).
    public boolean delete(String id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.delete(TABLE_DEADLINE, COL_ID + "=?", new String[]{id}) > 0;
    }

    // Marks a deadline completed (or pending again) and keeps the completion time.
    public boolean setCompleted(String id, boolean completed) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long now = System.currentTimeMillis();

        ContentValues values = new ContentValues();
        values.put(COL_DEADLINE_STATUS,
                completed ? Deadline.STATUS_COMPLETED : Deadline.STATUS_PENDING);
        if (completed) {
            values.put(COL_DEADLINE_COMPLETED_AT, now);
        } else {
            values.putNull(COL_DEADLINE_COMPLETED_AT);
        }
        values.put(COL_UPDATED_AT, now);
        values.put(COL_SYNC_STATUS, SYNC_PENDING);
        return db.update(TABLE_DEADLINE, values, COL_ID + "=?", new String[]{id}) > 0;
    }

    public Deadline getById(String id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery(SELECT_WITH_COURSE + " WHERE d." + COL_ID + "=?",
                new String[]{id});
        try {
            if (cursor.moveToFirst()) {
                return fromCursor(cursor);
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    // courseId == null means "all courses".
    public List<Deadline> getList(String semesterId, String courseId, int filter) {
        SQLiteDatabase db = helper.getReadableDatabase();

        StringBuilder where = new StringBuilder(" WHERE d." + COL_DEADLINE_SEMESTER_ID + "=?");
        List<String> args = new ArrayList<>();
        args.add(semesterId);

        if (courseId != null) {
            where.append(" AND d." + COL_DEADLINE_COURSE_ID + "=?");
            args.add(courseId);
        }

        long now = System.currentTimeMillis();
        if (filter == FILTER_PENDING) {
            where.append(" AND d." + COL_DEADLINE_STATUS + "=?");
            args.add(Deadline.STATUS_PENDING);
        } else if (filter == FILTER_OVERDUE) {
            where.append(" AND d." + COL_DEADLINE_STATUS + "=? AND d."
                    + COL_DEADLINE_DUE_AT + "<?");
            args.add(Deadline.STATUS_PENDING);
            args.add(String.valueOf(now));
        } else if (filter == FILTER_COMPLETED) {
            where.append(" AND d." + COL_DEADLINE_STATUS + "=?");
            args.add(Deadline.STATUS_COMPLETED);
        }

        // Pending first (nearest due time first), completed ones at the end.
        String order = " ORDER BY CASE d." + COL_DEADLINE_STATUS
                + " WHEN 'PENDING' THEN 0 ELSE 1 END, d." + COL_DEADLINE_DUE_AT + " ASC";

        List<Deadline> list = new ArrayList<>();
        Cursor cursor = db.rawQuery(SELECT_WITH_COURSE + where + order,
                args.toArray(new String[0]));
        try {
            while (cursor.moveToNext()) {
                list.add(fromCursor(cursor));
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    // Pending deadlines, nearest first (overdue ones come first). Used by the Home card.
    public List<Deadline> getUpcoming(String semesterId, int limit) {
        SQLiteDatabase db = helper.getReadableDatabase();
        String sql = SELECT_WITH_COURSE + " WHERE d." + COL_DEADLINE_SEMESTER_ID
                + "=? AND d." + COL_DEADLINE_STATUS + "=? ORDER BY d."
                + COL_DEADLINE_DUE_AT + " ASC LIMIT " + limit;

        List<Deadline> list = new ArrayList<>();
        Cursor cursor = db.rawQuery(sql,
                new String[]{semesterId, Deadline.STATUS_PENDING});
        try {
            while (cursor.moveToNext()) {
                list.add(fromCursor(cursor));
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    public int countPending(String semesterId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_DEADLINE
                        + " WHERE " + COL_DEADLINE_SEMESTER_ID + "=? AND "
                        + COL_DEADLINE_STATUS + "=?",
                new String[]{semesterId, Deadline.STATUS_PENDING});
        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }

    // ---------- Reminders ----------

    // Example result: [0, 60, 1440] = at due time, 1 hour before, 1 day before.
    public List<Integer> getReminderMinutes(String deadlineId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<Integer> list = new ArrayList<>();
        Cursor cursor = db.query(TABLE_REMINDER, new String[]{COL_REMINDER_MINUTES_BEFORE},
                COL_REMINDER_DEADLINE_ID + "=?", new String[]{deadlineId},
                null, null, COL_REMINDER_MINUTES_BEFORE + " ASC");
        try {
            while (cursor.moveToNext()) {
                list.add(cursor.getInt(0));
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    // Deletes the old reminders of the deadline and inserts the new list.
    // Must be called inside a transaction (the callers above do that).
    private void replaceReminders(SQLiteDatabase db, String deadlineId,
                                  List<Integer> minutesList, long now) {
        db.delete(TABLE_REMINDER, COL_REMINDER_DEADLINE_ID + "=?", new String[]{deadlineId});
        for (int minutes : minutesList) {
            ContentValues values = new ContentValues();
            values.put(COL_ID, IdUtils.newId());
            values.put(COL_REMINDER_DEADLINE_ID, deadlineId);
            values.put(COL_REMINDER_MINUTES_BEFORE, minutes);
            values.put(COL_CREATED_AT, now);
            values.put(COL_UPDATED_AT, now);
            values.put(COL_SYNC_STATUS, SYNC_PENDING);
            db.insert(TABLE_REMINDER, null, values);
        }
    }

    private ContentValues toValues(Deadline d) {
        ContentValues values = new ContentValues();
        values.put(COL_ID, d.getId());
        values.put(COL_OWNER_UID, d.getOwnerUid());
        values.put(COL_DEADLINE_SEMESTER_ID, d.getSemesterId());
        values.put(COL_DEADLINE_COURSE_ID, d.getCourseId());
        values.put(COL_DEADLINE_TITLE, d.getTitle());
        values.put(COL_DEADLINE_DESCRIPTION, d.getDescription());
        values.put(COL_DEADLINE_TYPE, d.getType());
        values.put(COL_DEADLINE_DUE_AT, d.getDueAt());
        values.put(COL_DEADLINE_PRIORITY, d.getPriority());
        values.put(COL_DEADLINE_STATUS, d.getStatus());
        if (d.getCompletedAt() > 0) {
            values.put(COL_DEADLINE_COMPLETED_AT, d.getCompletedAt());
        } else {
            values.putNull(COL_DEADLINE_COMPLETED_AT);
        }
        values.put(COL_CREATED_AT, d.getCreatedAt());
        values.put(COL_UPDATED_AT, d.getUpdatedAt());
        values.put(COL_SYNC_STATUS, d.getSyncStatus());
        return values;
    }

    private Deadline fromCursor(Cursor c) {
        Deadline d = new Deadline();
        d.setId(c.getString(c.getColumnIndexOrThrow(COL_ID)));
        d.setOwnerUid(c.getString(c.getColumnIndexOrThrow(COL_OWNER_UID)));
        d.setSemesterId(c.getString(c.getColumnIndexOrThrow(COL_DEADLINE_SEMESTER_ID)));
        d.setCourseId(c.getString(c.getColumnIndexOrThrow(COL_DEADLINE_COURSE_ID)));
        d.setTitle(c.getString(c.getColumnIndexOrThrow(COL_DEADLINE_TITLE)));
        d.setDescription(c.getString(c.getColumnIndexOrThrow(COL_DEADLINE_DESCRIPTION)));
        d.setType(c.getString(c.getColumnIndexOrThrow(COL_DEADLINE_TYPE)));
        d.setDueAt(c.getLong(c.getColumnIndexOrThrow(COL_DEADLINE_DUE_AT)));
        d.setPriority(c.getInt(c.getColumnIndexOrThrow(COL_DEADLINE_PRIORITY)));
        d.setStatus(c.getString(c.getColumnIndexOrThrow(COL_DEADLINE_STATUS)));
        // getLong() on a NULL column returns 0, which is exactly what we want here.
        d.setCompletedAt(c.getLong(c.getColumnIndexOrThrow(COL_DEADLINE_COMPLETED_AT)));
        d.setCreatedAt(c.getLong(c.getColumnIndexOrThrow(COL_CREATED_AT)));
        d.setUpdatedAt(c.getLong(c.getColumnIndexOrThrow(COL_UPDATED_AT)));
        d.setSyncStatus(c.getInt(c.getColumnIndexOrThrow(COL_SYNC_STATUS)));
        d.setCourseCode(c.getString(c.getColumnIndexOrThrow("course_code")));
        d.setCourseColor(c.getInt(c.getColumnIndexOrThrow("course_color")));
        return d;
    }
}
