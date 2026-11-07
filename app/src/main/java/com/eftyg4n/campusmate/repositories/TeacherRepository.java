package com.eftyg4n.campusmate.repositories;

import static com.eftyg4n.campusmate.database.DatabaseHelper.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.eftyg4n.campusmate.database.DatabaseHelper;
import com.eftyg4n.campusmate.models.Teacher;
import com.eftyg4n.campusmate.utils.IdUtils;

import java.util.ArrayList;
import java.util.List;

public class TeacherRepository {

    private final DatabaseHelper helper;

    public TeacherRepository(Context context) {
        helper = DatabaseHelper.getInstance(context);
    }

    // Returns the new id, or null if the insert failed.
    public String insert(Teacher teacher) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long now = System.currentTimeMillis();

        teacher.setId(IdUtils.newId());
        teacher.setCreatedAt(now);
        teacher.setUpdatedAt(now);
        teacher.setSyncStatus(SYNC_PENDING);

        long row = db.insert(TABLE_TEACHER, null, toValues(teacher));
        if (row == -1) {
            return null;
        }
        return teacher.getId();
    }

    public boolean update(Teacher teacher) {
        SQLiteDatabase db = helper.getWritableDatabase();
        teacher.setUpdatedAt(System.currentTimeMillis());
        teacher.setSyncStatus(SYNC_PENDING);
        return db.update(TABLE_TEACHER, toValues(teacher),
                COL_ID + "=?", new String[]{teacher.getId()}) > 0;
    }

    // Deleting a teacher also removes his/her rows in course_teacher (ON DELETE CASCADE).
    public boolean delete(String id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.delete(TABLE_TEACHER, COL_ID + "=?", new String[]{id}) > 0;
    }

    public Teacher getById(String id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TEACHER, null, COL_ID + "=?",
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

    // Finds the first teacher with this short code (example: "SA").
    public Teacher getByShortCode(String shortCode) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TEACHER, null, COL_TEACHER_SHORT_CODE + "=?",
                new String[]{shortCode}, null, null, null, "1");
        try {
            if (cursor.moveToFirst()) {
                return fromCursor(cursor);
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    public List<Teacher> getAll() {
        return search(null);
    }

    // query: text to find in name or short code (null = everyone)
    public List<Teacher> search(String query) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<Teacher> list = new ArrayList<>();

        String where = null;
        String[] args = null;
        if (query != null && !query.trim().isEmpty()) {
            where = COL_TEACHER_NAME + " LIKE ? OR " + COL_TEACHER_SHORT_CODE + " LIKE ?";
            String like = "%" + query.trim() + "%";
            args = new String[]{like, like};
        }

        Cursor cursor = db.query(TABLE_TEACHER, null, where, args, null, null,
                COL_TEACHER_NAME + " ASC");
        try {
            while (cursor.moveToNext()) {
                list.add(fromCursor(cursor));
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    // All teachers of one course (JOIN with the course_teacher table).
    public List<Teacher> getByCourse(String courseId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        String sql = "SELECT t.* FROM " + TABLE_TEACHER + " t INNER JOIN "
                + TABLE_COURSE_TEACHER + " ct ON ct." + COL_CT_TEACHER_ID
                + " = t." + COL_ID
                + " WHERE ct." + COL_CT_COURSE_ID + " = ?"
                + " ORDER BY t." + COL_TEACHER_SHORT_CODE + " ASC";

        List<Teacher> list = new ArrayList<>();
        Cursor cursor = db.rawQuery(sql, new String[]{courseId});
        try {
            while (cursor.moveToNext()) {
                list.add(fromCursor(cursor));
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    // Returns false if this teacher is already assigned to the course
    // (or if the course/teacher id does not exist).
    public boolean assignToCourse(String courseId, String teacherId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CT_COURSE_ID, courseId);
        values.put(COL_CT_TEACHER_ID, teacherId);
        values.put(COL_CREATED_AT, System.currentTimeMillis());
        values.put(COL_SYNC_STATUS, SYNC_PENDING);
        long row = db.insertWithOnConflict(TABLE_COURSE_TEACHER, null, values,
                SQLiteDatabase.CONFLICT_IGNORE);
        return row != -1;
    }

    public boolean unassignFromCourse(String courseId, String teacherId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.delete(TABLE_COURSE_TEACHER,
                COL_CT_COURSE_ID + "=? AND " + COL_CT_TEACHER_ID + "=?",
                new String[]{courseId, teacherId}) > 0;
    }

    private ContentValues toValues(Teacher t) {
        ContentValues values = new ContentValues();
        values.put(COL_ID, t.getId());
        values.put(COL_OWNER_UID, t.getOwnerUid());
        values.put(COL_TEACHER_NAME, t.getName());
        values.put(COL_TEACHER_SHORT_CODE, t.getShortCode());
        values.put(COL_TEACHER_DEPARTMENT, t.getDepartment());
        values.put(COL_TEACHER_DESIGNATION, t.getDesignation());
        values.put(COL_TEACHER_ROOM_NO, t.getRoomNo());
        values.put(COL_TEACHER_EXTENSION, t.getExtension());
        values.put(COL_TEACHER_EMAIL, t.getEmail());
        values.put(COL_TEACHER_PHONE, t.getPhone());
        values.put(COL_CREATED_AT, t.getCreatedAt());
        values.put(COL_UPDATED_AT, t.getUpdatedAt());
        values.put(COL_SYNC_STATUS, t.getSyncStatus());
        return values;
    }

    private Teacher fromCursor(Cursor c) {
        Teacher t = new Teacher();
        t.setId(c.getString(c.getColumnIndexOrThrow(COL_ID)));
        t.setOwnerUid(c.getString(c.getColumnIndexOrThrow(COL_OWNER_UID)));
        t.setName(c.getString(c.getColumnIndexOrThrow(COL_TEACHER_NAME)));
        t.setShortCode(c.getString(c.getColumnIndexOrThrow(COL_TEACHER_SHORT_CODE)));
        t.setDepartment(c.getString(c.getColumnIndexOrThrow(COL_TEACHER_DEPARTMENT)));
        t.setDesignation(c.getString(c.getColumnIndexOrThrow(COL_TEACHER_DESIGNATION)));
        t.setRoomNo(c.getString(c.getColumnIndexOrThrow(COL_TEACHER_ROOM_NO)));
        t.setExtension(c.getString(c.getColumnIndexOrThrow(COL_TEACHER_EXTENSION)));
        t.setEmail(c.getString(c.getColumnIndexOrThrow(COL_TEACHER_EMAIL)));
        t.setPhone(c.getString(c.getColumnIndexOrThrow(COL_TEACHER_PHONE)));
        t.setCreatedAt(c.getLong(c.getColumnIndexOrThrow(COL_CREATED_AT)));
        t.setUpdatedAt(c.getLong(c.getColumnIndexOrThrow(COL_UPDATED_AT)));
        t.setSyncStatus(c.getInt(c.getColumnIndexOrThrow(COL_SYNC_STATUS)));
        return t;
    }
}