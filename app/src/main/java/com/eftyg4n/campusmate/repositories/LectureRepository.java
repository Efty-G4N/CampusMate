package com.eftyg4n.campusmate.repositories;

import static com.eftyg4n.campusmate.database.DatabaseHelper.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.eftyg4n.campusmate.database.DatabaseHelper;
import com.eftyg4n.campusmate.models.Lecture;
import com.eftyg4n.campusmate.utils.IdUtils;

import java.util.ArrayList;
import java.util.List;

public class LectureRepository {

    private final DatabaseHelper helper;

    public LectureRepository(Context context) {
        helper = DatabaseHelper.getInstance(context);
    }

    public boolean insert(Lecture lecture) {
        long now = System.currentTimeMillis();
        lecture.setId(IdUtils.newId());
        lecture.setCreatedAt(now);
        lecture.setUpdatedAt(now);
        lecture.setSyncStatus(SYNC_PENDING);
        return helper.getWritableDatabase().insert(TABLE_LECTURE, null, toValues(lecture)) != -1;
    }

    public boolean update(Lecture lecture) {
        lecture.setUpdatedAt(System.currentTimeMillis());
        lecture.setSyncStatus(SYNC_PENDING);
        return helper.getWritableDatabase().update(TABLE_LECTURE, toValues(lecture),
                COL_ID + "=?", new String[]{lecture.getId()}) > 0;
    }

    public boolean delete(String id) {
        return helper.getWritableDatabase()
                .delete(TABLE_LECTURE, COL_ID + "=?", new String[]{id}) > 0;
    }

    // Newest day first.
    public List<Lecture> getByCourse(String courseId) {
        List<Lecture> list = new ArrayList<>();
        Cursor c = helper.getReadableDatabase().query(TABLE_LECTURE, null,
                COL_LECTURE_COURSE_ID + "=?", new String[]{courseId},
                null, null, COL_LECTURE_DATE + " DESC, " + COL_CREATED_AT + " DESC");
        try {
            while (c.moveToNext()) {
                list.add(fromCursor(c));
            }
        } finally {
            c.close();
        }
        return list;
    }

    private ContentValues toValues(Lecture l) {
        ContentValues v = new ContentValues();
        v.put(COL_ID, l.getId());
        v.put(COL_OWNER_UID, l.getOwnerUid());
        v.put(COL_LECTURE_COURSE_ID, l.getCourseId());
        v.put(COL_LECTURE_DATE, l.getLectureDate());
        v.put(COL_LECTURE_TITLE, l.getTitle());
        v.put(COL_LECTURE_NOTE, l.getNote());
        v.put(COL_CREATED_AT, l.getCreatedAt());
        v.put(COL_UPDATED_AT, l.getUpdatedAt());
        v.put(COL_SYNC_STATUS, l.getSyncStatus());
        return v;
    }

    private Lecture fromCursor(Cursor c) {
        Lecture l = new Lecture();
        l.setId(c.getString(c.getColumnIndexOrThrow(COL_ID)));
        l.setOwnerUid(c.getString(c.getColumnIndexOrThrow(COL_OWNER_UID)));
        l.setCourseId(c.getString(c.getColumnIndexOrThrow(COL_LECTURE_COURSE_ID)));
        l.setLectureDate(c.getLong(c.getColumnIndexOrThrow(COL_LECTURE_DATE)));
        l.setTitle(c.getString(c.getColumnIndexOrThrow(COL_LECTURE_TITLE)));
        l.setNote(c.getString(c.getColumnIndexOrThrow(COL_LECTURE_NOTE)));
        l.setCreatedAt(c.getLong(c.getColumnIndexOrThrow(COL_CREATED_AT)));
        l.setUpdatedAt(c.getLong(c.getColumnIndexOrThrow(COL_UPDATED_AT)));
        l.setSyncStatus(c.getInt(c.getColumnIndexOrThrow(COL_SYNC_STATUS)));
        return l;
    }
}
