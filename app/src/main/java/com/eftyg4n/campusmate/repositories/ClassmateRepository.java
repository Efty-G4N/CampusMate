package com.eftyg4n.campusmate.repositories;

import static com.eftyg4n.campusmate.database.DatabaseHelper.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import com.eftyg4n.campusmate.database.DatabaseHelper;
import com.eftyg4n.campusmate.models.Classmate;
import com.eftyg4n.campusmate.utils.IdUtils;

import java.util.ArrayList;
import java.util.List;

public class ClassmateRepository {

    private final DatabaseHelper helper;

    public ClassmateRepository(Context context) {
        helper = DatabaseHelper.getInstance(context);
    }

    // Returns the new id, or null if it failed (for example: this roll already exists).
    public String insert(Classmate classmate) {
        SQLiteDatabase db = helper.getWritableDatabase();
        prepareNew(classmate, System.currentTimeMillis());
        long row = db.insert(TABLE_CLASSMATE, null, toValues(classmate));
        if (row == -1) {
            return null;
        }
        return classmate.getId();
    }

    // Inserts many classmates in ONE transaction. Rolls that already exist are skipped.
    // Returns how many were really inserted.
    public int insertAll(List<Classmate> list) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long now = System.currentTimeMillis();
        int inserted = 0;

        db.beginTransaction();
        try {
            for (int i = 0; i < list.size(); i++) {
                Classmate classmate = list.get(i);
                prepareNew(classmate, now);
                long row = db.insertWithOnConflict(TABLE_CLASSMATE, null,
                        toValues(classmate), SQLiteDatabase.CONFLICT_IGNORE);
                if (row != -1) {
                    inserted++;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return inserted;
    }

    public boolean update(Classmate classmate) {
        SQLiteDatabase db = helper.getWritableDatabase();
        classmate.setUpdatedAt(System.currentTimeMillis());
        classmate.setSyncStatus(SYNC_PENDING);
        return db.update(TABLE_CLASSMATE, toValues(classmate),
                COL_ID + "=?", new String[]{classmate.getId()}) > 0;
    }

    public boolean delete(String id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.delete(TABLE_CLASSMATE, COL_ID + "=?", new String[]{id}) > 0;
    }

    public int deleteAll() {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.delete(TABLE_CLASSMATE, null, null);
    }

    public long count() {
        SQLiteDatabase db = helper.getReadableDatabase();
        return DatabaseUtils.queryNumEntries(db, TABLE_CLASSMATE);
    }

    public Classmate getByRoll(String rollNo) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CLASSMATE, null, COL_CLASSMATE_ROLL_NO + "=?",
                new String[]{rollNo.trim()}, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                return fromCursor(cursor);
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    public List<Classmate> getAll() {
        return search(null);
    }

    // query: text to find in roll number or name (null = everyone)
    public List<Classmate> search(String query) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<Classmate> list = new ArrayList<>();

        String where = null;
        String[] args = null;
        if (query != null && !query.trim().isEmpty()) {
            where = COL_CLASSMATE_ROLL_NO + " LIKE ? OR " + COL_CLASSMATE_FULL_NAME + " LIKE ?";
            String like = "%" + query.trim() + "%";
            args = new String[]{like, like};
        }

        Cursor cursor = db.query(TABLE_CLASSMATE, null, where, args, null, null,
                COL_CLASSMATE_ROLL_NO + " ASC");
        try {
            while (cursor.moveToNext()) {
                list.add(fromCursor(cursor));
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    private void prepareNew(Classmate classmate, long now) {
        classmate.setId(IdUtils.newId());
        classmate.setCreatedAt(now);
        classmate.setUpdatedAt(now);
        classmate.setSyncStatus(SYNC_PENDING);
    }

    private ContentValues toValues(Classmate c) {
        ContentValues values = new ContentValues();
        values.put(COL_ID, c.getId());
        values.put(COL_OWNER_UID, c.getOwnerUid());
        values.put(COL_CLASSMATE_ROLL_NO, c.getRollNo());
        values.put(COL_CLASSMATE_FULL_NAME, c.getFullName());
        values.put(COL_CREATED_AT, c.getCreatedAt());
        values.put(COL_UPDATED_AT, c.getUpdatedAt());
        values.put(COL_SYNC_STATUS, c.getSyncStatus());
        return values;
    }

    private Classmate fromCursor(Cursor c) {
        Classmate m = new Classmate();
        m.setId(c.getString(c.getColumnIndexOrThrow(COL_ID)));
        m.setOwnerUid(c.getString(c.getColumnIndexOrThrow(COL_OWNER_UID)));
        m.setRollNo(c.getString(c.getColumnIndexOrThrow(COL_CLASSMATE_ROLL_NO)));
        m.setFullName(c.getString(c.getColumnIndexOrThrow(COL_CLASSMATE_FULL_NAME)));
        m.setCreatedAt(c.getLong(c.getColumnIndexOrThrow(COL_CREATED_AT)));
        m.setUpdatedAt(c.getLong(c.getColumnIndexOrThrow(COL_UPDATED_AT)));
        m.setSyncStatus(c.getInt(c.getColumnIndexOrThrow(COL_SYNC_STATUS)));
        return m;
    }
}