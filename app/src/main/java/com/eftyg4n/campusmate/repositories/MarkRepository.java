package com.eftyg4n.campusmate.repositories;

import static com.eftyg4n.campusmate.database.DatabaseHelper.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.eftyg4n.campusmate.database.DatabaseHelper;
import com.eftyg4n.campusmate.models.Mark;
import com.eftyg4n.campusmate.utils.IdUtils;

import java.util.ArrayList;
import java.util.List;

public class MarkRepository {

    private final DatabaseHelper helper;

    public MarkRepository(Context context) {
        helper = DatabaseHelper.getInstance(context);
    }

    public boolean insert(Mark mark) {
        long now = System.currentTimeMillis();
        mark.setId(IdUtils.newId());
        mark.setCreatedAt(now);
        mark.setUpdatedAt(now);
        mark.setSyncStatus(SYNC_PENDING);
        return helper.getWritableDatabase().insert(TABLE_MARK, null, toValues(mark)) != -1;
    }

    public boolean update(Mark mark) {
        mark.setUpdatedAt(System.currentTimeMillis());
        mark.setSyncStatus(SYNC_PENDING);
        return helper.getWritableDatabase().update(TABLE_MARK, toValues(mark),
                COL_ID + "=?", new String[]{mark.getId()}) > 0;
    }

    public boolean delete(String id) {
        return helper.getWritableDatabase()
                .delete(TABLE_MARK, COL_ID + "=?", new String[]{id}) > 0;
    }

    // Newest first.
    public List<Mark> getByCourse(String courseId) {
        List<Mark> list = new ArrayList<>();
        Cursor c = helper.getReadableDatabase().query(TABLE_MARK, null,
                COL_MARK_COURSE_ID + "=?", new String[]{courseId},
                null, null, COL_CREATED_AT + " DESC");
        try {
            while (c.moveToNext()) {
                list.add(fromCursor(c));
            }
        } finally {
            c.close();
        }
        return list;
    }

    private ContentValues toValues(Mark m) {
        ContentValues v = new ContentValues();
        v.put(COL_ID, m.getId());
        v.put(COL_OWNER_UID, m.getOwnerUid());
        v.put(COL_MARK_COURSE_ID, m.getCourseId());
        v.put(COL_MARK_TITLE, m.getTitle());
        v.put(COL_MARK_CATEGORY, m.getCategory());
        v.put(COL_MARK_OBTAINED, m.getObtained());
        v.put(COL_MARK_TOTAL, m.getTotal());
        v.put(COL_CREATED_AT, m.getCreatedAt());
        v.put(COL_UPDATED_AT, m.getUpdatedAt());
        v.put(COL_SYNC_STATUS, m.getSyncStatus());
        return v;
    }

    private Mark fromCursor(Cursor c) {
        Mark m = new Mark();
        m.setId(c.getString(c.getColumnIndexOrThrow(COL_ID)));
        m.setOwnerUid(c.getString(c.getColumnIndexOrThrow(COL_OWNER_UID)));
        m.setCourseId(c.getString(c.getColumnIndexOrThrow(COL_MARK_COURSE_ID)));
        m.setTitle(c.getString(c.getColumnIndexOrThrow(COL_MARK_TITLE)));
        m.setCategory(c.getString(c.getColumnIndexOrThrow(COL_MARK_CATEGORY)));
        m.setObtained(c.getDouble(c.getColumnIndexOrThrow(COL_MARK_OBTAINED)));
        m.setTotal(c.getDouble(c.getColumnIndexOrThrow(COL_MARK_TOTAL)));
        m.setCreatedAt(c.getLong(c.getColumnIndexOrThrow(COL_CREATED_AT)));
        m.setUpdatedAt(c.getLong(c.getColumnIndexOrThrow(COL_UPDATED_AT)));
        m.setSyncStatus(c.getInt(c.getColumnIndexOrThrow(COL_SYNC_STATUS)));
        return m;
    }
}
