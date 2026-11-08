package com.eftyg4n.campusmate.repositories;

import static com.eftyg4n.campusmate.database.DatabaseHelper.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import com.eftyg4n.campusmate.database.DatabaseHelper;
import com.eftyg4n.campusmate.models.ClassGroup;
import com.eftyg4n.campusmate.utils.IdUtils;

import java.util.ArrayList;
import java.util.List;

public class ClassGroupRepository {

    private final DatabaseHelper helper;

    public ClassGroupRepository(Context context) {
        helper = DatabaseHelper.getInstance(context);
    }

    // Returns the new id, or null if it failed (for example: same name in this semester).
    public String insert(ClassGroup group) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long now = System.currentTimeMillis();

        group.setId(IdUtils.newId());
        group.setCreatedAt(now);
        group.setUpdatedAt(now);
        group.setSyncStatus(SYNC_PENDING);

        long row = db.insert(TABLE_CLASS_GROUP, null, toValues(group));
        if (row == -1) {
            return null;
        }
        return group.getId();
    }

    public boolean update(ClassGroup group) {
        SQLiteDatabase db = helper.getWritableDatabase();
        group.setUpdatedAt(System.currentTimeMillis());
        group.setSyncStatus(SYNC_PENDING);
        return db.update(TABLE_CLASS_GROUP, toValues(group),
                COL_ID + "=?", new String[]{group.getId()}) > 0;
    }

    // Deleting a section also deletes its lab groups (ON DELETE CASCADE).
    public boolean delete(String id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.delete(TABLE_CLASS_GROUP, COL_ID + "=?", new String[]{id}) > 0;
    }

    public long count(String semesterId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        return DatabaseUtils.queryNumEntries(db, TABLE_CLASS_GROUP,
                COL_GROUP_SEMESTER_ID + "=?", new String[]{semesterId});
    }

    public List<ClassGroup> getBySemester(String semesterId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<ClassGroup> list = new ArrayList<>();
        Cursor cursor = db.query(TABLE_CLASS_GROUP, null, COL_GROUP_SEMESTER_ID + "=?",
                new String[]{semesterId}, null, null, COL_GROUP_NAME + " ASC");
        try {
            while (cursor.moveToNext()) {
                list.add(fromCursor(cursor));
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    // The section whose roll range contains this serial number (null if none).
    public ClassGroup findSection(String semesterId, int serial) {
        return findByType(semesterId, ClassGroup.TYPE_SECTION, serial);
    }

    // The lab group whose roll range contains this serial number (null if none).
    public ClassGroup findLabGroup(String semesterId, int serial) {
        return findByType(semesterId, ClassGroup.TYPE_LAB_GROUP, serial);
    }

    private ClassGroup findByType(String semesterId, String type, int serial) {
        SQLiteDatabase db = helper.getReadableDatabase();
        String where = COL_GROUP_SEMESTER_ID + "=? AND " + COL_GROUP_TYPE + "=? AND "
                + COL_GROUP_ROLL_FROM + "<=? AND " + COL_GROUP_ROLL_TO + ">=?";
        String[] args = {semesterId, type, String.valueOf(serial), String.valueOf(serial)};
        Cursor cursor = db.query(TABLE_CLASS_GROUP, null, where, args,
                null, null, null, "1");
        try {
            if (cursor.moveToFirst()) {
                return fromCursor(cursor);
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    private ContentValues toValues(ClassGroup g) {
        ContentValues values = new ContentValues();
        values.put(COL_ID, g.getId());
        values.put(COL_OWNER_UID, g.getOwnerUid());
        values.put(COL_GROUP_SEMESTER_ID, g.getSemesterId());
        values.put(COL_GROUP_PARENT_ID, g.getParentId());
        values.put(COL_GROUP_NAME, g.getName());
        values.put(COL_GROUP_TYPE, g.getType());
        values.put(COL_GROUP_ROLL_FROM, g.getRollFrom());
        values.put(COL_GROUP_ROLL_TO, g.getRollTo());
        values.put(COL_CREATED_AT, g.getCreatedAt());
        values.put(COL_UPDATED_AT, g.getUpdatedAt());
        values.put(COL_SYNC_STATUS, g.getSyncStatus());
        return values;
    }

    private ClassGroup fromCursor(Cursor c) {
        ClassGroup g = new ClassGroup();
        g.setId(c.getString(c.getColumnIndexOrThrow(COL_ID)));
        g.setOwnerUid(c.getString(c.getColumnIndexOrThrow(COL_OWNER_UID)));
        g.setSemesterId(c.getString(c.getColumnIndexOrThrow(COL_GROUP_SEMESTER_ID)));
        g.setParentId(c.getString(c.getColumnIndexOrThrow(COL_GROUP_PARENT_ID)));
        g.setName(c.getString(c.getColumnIndexOrThrow(COL_GROUP_NAME)));
        g.setType(c.getString(c.getColumnIndexOrThrow(COL_GROUP_TYPE)));
        g.setRollFrom(c.getInt(c.getColumnIndexOrThrow(COL_GROUP_ROLL_FROM)));
        g.setRollTo(c.getInt(c.getColumnIndexOrThrow(COL_GROUP_ROLL_TO)));
        g.setCreatedAt(c.getLong(c.getColumnIndexOrThrow(COL_CREATED_AT)));
        g.setUpdatedAt(c.getLong(c.getColumnIndexOrThrow(COL_UPDATED_AT)));
        g.setSyncStatus(c.getInt(c.getColumnIndexOrThrow(COL_SYNC_STATUS)));
        return g;
    }
}