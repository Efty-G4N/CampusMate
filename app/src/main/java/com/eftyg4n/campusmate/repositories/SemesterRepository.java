package com.eftyg4n.campusmate.repositories;

import static com.eftyg4n.campusmate.database.DatabaseHelper.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.eftyg4n.campusmate.database.DatabaseHelper;
import com.eftyg4n.campusmate.models.Semester;
import com.eftyg4n.campusmate.utils.IdUtils;

import java.util.ArrayList;
import java.util.List;

public class SemesterRepository {

    private final DatabaseHelper helper;

    public SemesterRepository(Context context) {
        helper = DatabaseHelper.getInstance(context);
    }

    // Returns the new id, or null if the insert failed.
    public String insert(Semester semester) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long now = System.currentTimeMillis();

        semester.setId(IdUtils.newId());
        semester.setCreatedAt(now);
        semester.setUpdatedAt(now);
        semester.setSyncStatus(SYNC_PENDING);

        db.beginTransaction();
        try {
            if (semester.isCurrent()) {
                clearCurrent(db);
            }
            long row = db.insert(TABLE_SEMESTER, null, toValues(semester));
            if (row == -1) {
                return null;
            }
            db.setTransactionSuccessful();
            return semester.getId();
        } finally {
            db.endTransaction();
        }
    }

    public boolean update(Semester semester) {
        SQLiteDatabase db = helper.getWritableDatabase();
        semester.setUpdatedAt(System.currentTimeMillis());
        semester.setSyncStatus(SYNC_PENDING);

        db.beginTransaction();
        try {
            if (semester.isCurrent()) {
                clearCurrent(db);
            }
            int rows = db.update(TABLE_SEMESTER, toValues(semester),
                    COL_ID + "=?", new String[]{semester.getId()});
            if (rows == 0) {
                return false;
            }
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    public boolean delete(String id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.delete(TABLE_SEMESTER, COL_ID + "=?", new String[]{id}) > 0;
    }

    public Semester getById(String id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SEMESTER, null, COL_ID + "=?",
                new String[]{id}, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                return fromCursor(cursor);
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    public List<Semester> getAll() {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<Semester> list = new ArrayList<>();
        Cursor cursor = db.query(TABLE_SEMESTER, null, null, null, null, null,
                COL_SEMESTER_YEAR + " DESC, " + COL_SEMESTER_TERM + " DESC, "
                        + COL_CREATED_AT + " DESC");
        try {
            while (cursor.moveToNext()) {
                list.add(fromCursor(cursor));
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    // Returns the current semester, or null if none is selected.
    public Semester getCurrent() {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SEMESTER, null, COL_SEMESTER_IS_CURRENT + "=1",
                null, null, null, null, "1");
        try {
            if (cursor.moveToFirst()) {
                return fromCursor(cursor);
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    public boolean setCurrent(String id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            clearCurrent(db);
            ContentValues values = new ContentValues();
            values.put(COL_SEMESTER_IS_CURRENT, 1);
            values.put(COL_UPDATED_AT, System.currentTimeMillis());
            values.put(COL_SYNC_STATUS, SYNC_PENDING);
            int rows = db.update(TABLE_SEMESTER, values, COL_ID + "=?", new String[]{id});
            if (rows == 0) {
                return false;
            }
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    // Archiving a semester also removes its "current" mark.
    public boolean setArchived(String id, boolean archived) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_SEMESTER_IS_ARCHIVED, archived ? 1 : 0);
        if (archived) {
            values.put(COL_SEMESTER_IS_CURRENT, 0);
        }
        values.put(COL_UPDATED_AT, System.currentTimeMillis());
        values.put(COL_SYNC_STATUS, SYNC_PENDING);
        return db.update(TABLE_SEMESTER, values, COL_ID + "=?", new String[]{id}) > 0;
    }

    private void clearCurrent(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(COL_SEMESTER_IS_CURRENT, 0);
        db.update(TABLE_SEMESTER, values, null, null);
    }

    private ContentValues toValues(Semester s) {
        ContentValues values = new ContentValues();
        values.put(COL_ID, s.getId());
        values.put(COL_OWNER_UID, s.getOwnerUid());
        values.put(COL_SEMESTER_NAME, s.getName());
        values.put(COL_SEMESTER_SESSION, s.getSession());
        values.put(COL_SEMESTER_YEAR, s.getYearNo());
        values.put(COL_SEMESTER_TERM, s.getTermNo());
        values.put(COL_SEMESTER_SECTION, s.getSection());
        values.put(COL_SEMESTER_START_DATE, s.getStartDate());
        values.put(COL_SEMESTER_END_DATE, s.getEndDate());
        values.put(COL_SEMESTER_IS_CURRENT, s.isCurrent() ? 1 : 0);
        values.put(COL_SEMESTER_IS_ARCHIVED, s.isArchived() ? 1 : 0);
        values.put(COL_CREATED_AT, s.getCreatedAt());
        values.put(COL_UPDATED_AT, s.getUpdatedAt());
        values.put(COL_SYNC_STATUS, s.getSyncStatus());
        return values;
    }

    private Semester fromCursor(Cursor c) {
        Semester s = new Semester();
        s.setId(c.getString(c.getColumnIndexOrThrow(COL_ID)));
        s.setOwnerUid(c.getString(c.getColumnIndexOrThrow(COL_OWNER_UID)));
        s.setName(c.getString(c.getColumnIndexOrThrow(COL_SEMESTER_NAME)));
        s.setSession(c.getString(c.getColumnIndexOrThrow(COL_SEMESTER_SESSION)));
        s.setYearNo(c.getInt(c.getColumnIndexOrThrow(COL_SEMESTER_YEAR)));
        s.setTermNo(c.getInt(c.getColumnIndexOrThrow(COL_SEMESTER_TERM)));
        s.setSection(c.getString(c.getColumnIndexOrThrow(COL_SEMESTER_SECTION)));
        s.setStartDate(c.getLong(c.getColumnIndexOrThrow(COL_SEMESTER_START_DATE)));
        s.setEndDate(c.getLong(c.getColumnIndexOrThrow(COL_SEMESTER_END_DATE)));
        s.setCurrent(c.getInt(c.getColumnIndexOrThrow(COL_SEMESTER_IS_CURRENT)) == 1);
        s.setArchived(c.getInt(c.getColumnIndexOrThrow(COL_SEMESTER_IS_ARCHIVED)) == 1);
        s.setCreatedAt(c.getLong(c.getColumnIndexOrThrow(COL_CREATED_AT)));
        s.setUpdatedAt(c.getLong(c.getColumnIndexOrThrow(COL_UPDATED_AT)));
        s.setSyncStatus(c.getInt(c.getColumnIndexOrThrow(COL_SYNC_STATUS)));
        return s;
    }
}