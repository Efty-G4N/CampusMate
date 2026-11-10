package com.eftyg4n.campusmate.repositories;

import static com.eftyg4n.campusmate.database.DatabaseHelper.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.eftyg4n.campusmate.database.DatabaseHelper;
import com.eftyg4n.campusmate.models.Attendance;
import com.eftyg4n.campusmate.utils.IdUtils;

import java.util.ArrayList;
import java.util.List;

public class AttendanceRepository {

    // Totals for one course, counted by SQLite (no need to load every row).
    public static class Summary {
        public final int total;
        public final int attended;

        Summary(int total, int attended) {
            this.total = total;
            this.attended = attended;
        }

        public int absent() {
            return total - attended;
        }
    }

    private final DatabaseHelper helper;

    public AttendanceRepository(Context context) {
        helper = DatabaseHelper.getInstance(context);
    }

    // One record per course and day: marking the same day again updates it.
    // Returns false if the database refused the write.
    public boolean upsert(String courseId, long classDate, String status, String note) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long now = System.currentTimeMillis();

        String existingId = null;
        Cursor cursor = db.query(TABLE_ATTENDANCE, new String[]{COL_ID},
                COL_ATT_COURSE_ID + "=? AND " + COL_ATT_DATE + "=?",
                new String[]{courseId, String.valueOf(classDate)}, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                existingId = cursor.getString(0);
            }
        } finally {
            cursor.close();
        }

        ContentValues values = new ContentValues();
        values.put(COL_ATT_STATUS, status);
        values.put(COL_ATT_NOTE, note);
        values.put(COL_UPDATED_AT, now);
        values.put(COL_SYNC_STATUS, SYNC_PENDING);

        if (existingId != null) {
            return db.update(TABLE_ATTENDANCE, values, COL_ID + "=?",
                    new String[]{existingId}) > 0;
        }
        values.put(COL_ID, IdUtils.newId());
        values.put(COL_ATT_COURSE_ID, courseId);
        values.put(COL_ATT_DATE, classDate);
        values.put(COL_CREATED_AT, now);
        return db.insert(TABLE_ATTENDANCE, null, values) != -1;
    }

    public boolean delete(String id) {
        return helper.getWritableDatabase()
                .delete(TABLE_ATTENDANCE, COL_ID + "=?", new String[]{id}) > 0;
    }

    // Newest class day first.
    public List<Attendance> getByCourse(String courseId) {
        List<Attendance> list = new ArrayList<>();
        Cursor cursor = helper.getReadableDatabase().query(TABLE_ATTENDANCE, null,
                COL_ATT_COURSE_ID + "=?", new String[]{courseId},
                null, null, COL_ATT_DATE + " DESC");
        try {
            while (cursor.moveToNext()) {
                list.add(fromCursor(cursor));
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    public Summary getSummary(String courseId) {
        Cursor cursor = helper.getReadableDatabase().rawQuery(
                "SELECT COUNT(*), SUM(CASE WHEN " + COL_ATT_STATUS + " IN ('"
                        + Attendance.STATUS_PRESENT + "','" + Attendance.STATUS_LATE
                        + "') THEN 1 ELSE 0 END) FROM " + TABLE_ATTENDANCE
                        + " WHERE " + COL_ATT_COURSE_ID + "=?",
                new String[]{courseId});
        try {
            cursor.moveToFirst();
            // SUM() is NULL when there are no rows; getInt() turns that into 0.
            return new Summary(cursor.getInt(0), cursor.getInt(1));
        } finally {
            cursor.close();
        }
    }

    private Attendance fromCursor(Cursor c) {
        Attendance a = new Attendance();
        a.setId(c.getString(c.getColumnIndexOrThrow(COL_ID)));
        a.setOwnerUid(c.getString(c.getColumnIndexOrThrow(COL_OWNER_UID)));
        a.setCourseId(c.getString(c.getColumnIndexOrThrow(COL_ATT_COURSE_ID)));
        a.setClassDate(c.getLong(c.getColumnIndexOrThrow(COL_ATT_DATE)));
        a.setStatus(c.getString(c.getColumnIndexOrThrow(COL_ATT_STATUS)));
        a.setNote(c.getString(c.getColumnIndexOrThrow(COL_ATT_NOTE)));
        a.setCreatedAt(c.getLong(c.getColumnIndexOrThrow(COL_CREATED_AT)));
        a.setUpdatedAt(c.getLong(c.getColumnIndexOrThrow(COL_UPDATED_AT)));
        a.setSyncStatus(c.getInt(c.getColumnIndexOrThrow(COL_SYNC_STATUS)));
        return a;
    }
}
