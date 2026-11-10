package com.eftyg4n.campusmate.repositories;

import static com.eftyg4n.campusmate.database.DatabaseHelper.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import com.eftyg4n.campusmate.database.DatabaseHelper;
import com.eftyg4n.campusmate.models.Routine;
import com.eftyg4n.campusmate.utils.IdUtils;

import java.util.ArrayList;
import java.util.List;

public class RoutineRepository {

    private final DatabaseHelper helper;

    public RoutineRepository(Context context) {
        helper = DatabaseHelper.getInstance(context);
    }

    // Returns the new id, or null if it failed.
    public String insert(Routine routine) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long now = System.currentTimeMillis();

        routine.setId(IdUtils.newId());
        routine.setCreatedAt(now);
        routine.setUpdatedAt(now);
        routine.setSyncStatus(SYNC_PENDING);

        long row = db.insert(TABLE_ROUTINE, null, toValues(routine));
        if (row == -1) {
            return null;
        }
        return routine.getId();
    }

    public boolean update(Routine routine) {
        SQLiteDatabase db = helper.getWritableDatabase();
        routine.setUpdatedAt(System.currentTimeMillis());
        routine.setSyncStatus(SYNC_PENDING);
        return db.update(TABLE_ROUTINE, toValues(routine),
                COL_ID + "=?", new String[]{routine.getId()}) > 0;
    }

    public boolean delete(String id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.delete(TABLE_ROUTINE, COL_ID + "=?", new String[]{id}) > 0;
    }

    public long count(String semesterId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        return DatabaseUtils.queryNumEntries(db, TABLE_ROUTINE,
                COL_ROUTINE_SEMESTER_ID + "=?", new String[]{semesterId});
    }

    // The whole week for one student: the section's theory classes
    // plus the labs of the student's lab group. Sorted by day, then time.
    public List<Routine> getForClass(String semesterId, String section, String labGroup) {
        return query(semesterId, section, labGroup, -1);
    }

    // Same, but only one day (0 = Sunday ... 6 = Saturday).
    public List<Routine> getForDay(String semesterId, String section, String labGroup,
                                   int dayOfWeek) {
        return query(semesterId, section, labGroup, dayOfWeek);
    }

    private List<Routine> query(String semesterId, String section, String labGroup,
                                int dayOfWeek) {
        SQLiteDatabase db = helper.getReadableDatabase();

        String sql = "SELECT r.*, c." + COL_COURSE_CODE + " AS course_code, "
                + "c." + COL_COURSE_TITLE + " AS course_title, "
                + "c." + COL_COURSE_COLOR + " AS course_color "
                + "FROM " + TABLE_ROUTINE + " r JOIN " + TABLE_COURSE + " c "
                + "ON c." + COL_ID + " = r." + COL_ROUTINE_COURSE_ID + " "
                + "WHERE r." + COL_ROUTINE_SEMESTER_ID + "=? AND r."
                + COL_ROUTINE_SECTION + "=?";

        List<String> args = new ArrayList<>();
        args.add(semesterId);
        args.add(section);

        if (labGroup == null || labGroup.isEmpty()) {
            sql += " AND r." + COL_ROUTINE_LAB_GROUP + " IS NULL";
        } else {
            sql += " AND (r." + COL_ROUTINE_LAB_GROUP + " IS NULL OR r."
                    + COL_ROUTINE_LAB_GROUP + "=?)";
            args.add(labGroup);
        }

        if (dayOfWeek >= 0) {
            sql += " AND r." + COL_ROUTINE_DAY + "=?";
            args.add(String.valueOf(dayOfWeek));
        }

        sql += " ORDER BY r." + COL_ROUTINE_DAY + " ASC, r." + COL_ROUTINE_START_MINUTE + " ASC";

        List<Routine> list = new ArrayList<>();
        Cursor cursor = db.rawQuery(sql, args.toArray(new String[0]));
        try {
            while (cursor.moveToNext()) {
                list.add(fromCursor(cursor));
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    private ContentValues toValues(Routine r) {
        ContentValues values = new ContentValues();
        values.put(COL_ID, r.getId());
        values.put(COL_OWNER_UID, r.getOwnerUid());
        values.put(COL_ROUTINE_SEMESTER_ID, r.getSemesterId());
        values.put(COL_ROUTINE_COURSE_ID, r.getCourseId());
        values.put(COL_ROUTINE_DAY, r.getDayOfWeek());
        values.put(COL_ROUTINE_START_PERIOD, r.getStartPeriod());
        values.put(COL_ROUTINE_END_PERIOD, r.getEndPeriod());
        values.put(COL_ROUTINE_START_MINUTE, r.getStartMinute());
        values.put(COL_ROUTINE_END_MINUTE, r.getEndMinute());
        values.put(COL_ROUTINE_ROOM, r.getRoom());
        values.put(COL_ROUTINE_CLASS_TYPE, r.getClassType());
        values.put(COL_ROUTINE_SECTION, r.getSection());
        values.put(COL_ROUTINE_LAB_GROUP, r.getLabGroup());
        values.put(COL_ROUTINE_TEACHER_CODES, r.getTeacherCodes());
        values.put(COL_ROUTINE_STATUS, r.getStatus());
        values.put(COL_ROUTINE_NOTE, r.getNote());
        values.put(COL_CREATED_AT, r.getCreatedAt());
        values.put(COL_UPDATED_AT, r.getUpdatedAt());
        values.put(COL_SYNC_STATUS, r.getSyncStatus());
        return values;
    }

    private Routine fromCursor(Cursor c) {
        Routine r = new Routine();
        r.setId(c.getString(c.getColumnIndexOrThrow(COL_ID)));
        r.setOwnerUid(c.getString(c.getColumnIndexOrThrow(COL_OWNER_UID)));
        r.setSemesterId(c.getString(c.getColumnIndexOrThrow(COL_ROUTINE_SEMESTER_ID)));
        r.setCourseId(c.getString(c.getColumnIndexOrThrow(COL_ROUTINE_COURSE_ID)));
        r.setDayOfWeek(c.getInt(c.getColumnIndexOrThrow(COL_ROUTINE_DAY)));
        r.setStartPeriod(c.getInt(c.getColumnIndexOrThrow(COL_ROUTINE_START_PERIOD)));
        r.setEndPeriod(c.getInt(c.getColumnIndexOrThrow(COL_ROUTINE_END_PERIOD)));
        r.setStartMinute(c.getInt(c.getColumnIndexOrThrow(COL_ROUTINE_START_MINUTE)));
        r.setEndMinute(c.getInt(c.getColumnIndexOrThrow(COL_ROUTINE_END_MINUTE)));
        r.setRoom(c.getString(c.getColumnIndexOrThrow(COL_ROUTINE_ROOM)));
        r.setClassType(c.getString(c.getColumnIndexOrThrow(COL_ROUTINE_CLASS_TYPE)));
        r.setSection(c.getString(c.getColumnIndexOrThrow(COL_ROUTINE_SECTION)));
        r.setLabGroup(c.getString(c.getColumnIndexOrThrow(COL_ROUTINE_LAB_GROUP)));
        r.setTeacherCodes(c.getString(c.getColumnIndexOrThrow(COL_ROUTINE_TEACHER_CODES)));
        r.setStatus(c.getString(c.getColumnIndexOrThrow(COL_ROUTINE_STATUS)));
        r.setNote(c.getString(c.getColumnIndexOrThrow(COL_ROUTINE_NOTE)));
        r.setCreatedAt(c.getLong(c.getColumnIndexOrThrow(COL_CREATED_AT)));
        r.setUpdatedAt(c.getLong(c.getColumnIndexOrThrow(COL_UPDATED_AT)));
        r.setSyncStatus(c.getInt(c.getColumnIndexOrThrow(COL_SYNC_STATUS)));
        // Columns that come from the JOIN with course
        r.setCourseCode(c.getString(c.getColumnIndexOrThrow("course_code")));
        r.setCourseTitle(c.getString(c.getColumnIndexOrThrow("course_title")));
        r.setCourseColor(c.getInt(c.getColumnIndexOrThrow("course_color")));
        return r;
    }
}