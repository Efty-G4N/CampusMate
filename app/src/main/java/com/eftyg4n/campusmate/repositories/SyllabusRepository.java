package com.eftyg4n.campusmate.repositories;

import static com.eftyg4n.campusmate.database.DatabaseHelper.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.eftyg4n.campusmate.database.DatabaseHelper;
import com.eftyg4n.campusmate.models.SyllabusTopic;
import com.eftyg4n.campusmate.utils.IdUtils;

import java.util.ArrayList;
import java.util.List;

public class SyllabusRepository {

    private final DatabaseHelper helper;

    public SyllabusRepository(Context context) {
        helper = DatabaseHelper.getInstance(context);
    }

    // Adds all titles at the end of the course's list, in one transaction.
    // Returns how many were saved.
    public int insertMany(String courseId, List<String> titles) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int next = nextPosition(db, courseId);
        long now = System.currentTimeMillis();
        int saved = 0;
        db.beginTransaction();
        try {
            for (String title : titles) {
                ContentValues v = new ContentValues();
                v.put(COL_ID, IdUtils.newId());
                v.put(COL_TOPIC_COURSE_ID, courseId);
                v.put(COL_TOPIC_TITLE, title);
                v.put(COL_TOPIC_POSITION, next++);
                v.put(COL_TOPIC_DONE, 0);
                v.put(COL_CREATED_AT, now);
                v.put(COL_UPDATED_AT, now);
                v.put(COL_SYNC_STATUS, SYNC_PENDING);
                if (db.insert(TABLE_TOPIC, null, v) != -1) {
                    saved++;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return saved;
    }

    public boolean setDone(String id, boolean done) {
        ContentValues v = new ContentValues();
        v.put(COL_TOPIC_DONE, done ? 1 : 0);
        v.put(COL_TOPIC_DONE_AT, done ? System.currentTimeMillis() : 0);
        v.put(COL_UPDATED_AT, System.currentTimeMillis());
        v.put(COL_SYNC_STATUS, SYNC_PENDING);
        return helper.getWritableDatabase().update(TABLE_TOPIC, v,
                COL_ID + "=?", new String[]{id}) > 0;
    }

    public void setDoneMany(List<String> ids) {
        for (String id : ids) {
            setDone(id, true);
        }
    }

    public boolean delete(String id) {
        return helper.getWritableDatabase()
                .delete(TABLE_TOPIC, COL_ID + "=?", new String[]{id}) > 0;
    }

    public List<SyllabusTopic> getByCourse(String courseId) {
        List<SyllabusTopic> list = new ArrayList<>();
        Cursor c = helper.getReadableDatabase().query(TABLE_TOPIC, null,
                COL_TOPIC_COURSE_ID + "=?", new String[]{courseId},
                null, null, COL_TOPIC_POSITION + " ASC");
        try {
            while (c.moveToNext()) {
                list.add(fromCursor(c));
            }
        } finally {
            c.close();
        }
        return list;
    }

    private int nextPosition(SQLiteDatabase db, String courseId) {
        Cursor c = db.rawQuery("SELECT MAX(" + COL_TOPIC_POSITION + ") FROM " + TABLE_TOPIC
                + " WHERE " + COL_TOPIC_COURSE_ID + "=?", new String[]{courseId});
        try {
            // MAX() is NULL for an empty list; getInt() gives 0, so the first position is 0.
            if (c.moveToFirst() && !c.isNull(0)) {
                return c.getInt(0) + 1;
            }
            return 0;
        } finally {
            c.close();
        }
    }

    private SyllabusTopic fromCursor(Cursor c) {
        SyllabusTopic t = new SyllabusTopic();
        t.setId(c.getString(c.getColumnIndexOrThrow(COL_ID)));
        t.setOwnerUid(c.getString(c.getColumnIndexOrThrow(COL_OWNER_UID)));
        t.setCourseId(c.getString(c.getColumnIndexOrThrow(COL_TOPIC_COURSE_ID)));
        t.setTitle(c.getString(c.getColumnIndexOrThrow(COL_TOPIC_TITLE)));
        t.setPosition(c.getInt(c.getColumnIndexOrThrow(COL_TOPIC_POSITION)));
        t.setDone(c.getInt(c.getColumnIndexOrThrow(COL_TOPIC_DONE)) == 1);
        t.setDoneAt(c.getLong(c.getColumnIndexOrThrow(COL_TOPIC_DONE_AT)));
        t.setCreatedAt(c.getLong(c.getColumnIndexOrThrow(COL_CREATED_AT)));
        t.setUpdatedAt(c.getLong(c.getColumnIndexOrThrow(COL_UPDATED_AT)));
        t.setSyncStatus(c.getInt(c.getColumnIndexOrThrow(COL_SYNC_STATUS)));
        return t;
    }
}
