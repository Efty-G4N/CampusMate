package com.eftyg4n.campusmate.repositories;

import static com.eftyg4n.campusmate.database.DatabaseHelper.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import com.eftyg4n.campusmate.database.DatabaseHelper;
import com.eftyg4n.campusmate.models.Course;
import com.eftyg4n.campusmate.utils.IdUtils;

import java.util.ArrayList;
import java.util.List;

public class CourseRepository {

    private final DatabaseHelper helper;

    public CourseRepository(Context context) {
        helper = DatabaseHelper.getInstance(context);
    }

    // Returns the new id, or null if it failed
    // (for example: the same course code already exists in this semester).
    public String insert(Course course) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long now = System.currentTimeMillis();

        course.setId(IdUtils.newId());
        course.setCreatedAt(now);
        course.setUpdatedAt(now);
        course.setSyncStatus(SYNC_PENDING);

        long row = db.insert(TABLE_COURSE, null, toValues(course));
        if (row == -1) {
            return null;
        }
        return course.getId();
    }

    public boolean update(Course course) {
        SQLiteDatabase db = helper.getWritableDatabase();
        course.setUpdatedAt(System.currentTimeMillis());
        course.setSyncStatus(SYNC_PENDING);
        try {
            return db.update(TABLE_COURSE, toValues(course),
                    COL_ID + "=?", new String[]{course.getId()}) > 0;
        } catch (SQLiteConstraintException e) {
            return false;
        }
    }

    public boolean delete(String id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.delete(TABLE_COURSE, COL_ID + "=?", new String[]{id}) > 0;
    }

    public Course getById(String id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(TABLE_COURSE, null, COL_ID + "=?",
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

    public List<Course> getBySemester(String semesterId) {
        return search(semesterId, null, null);
    }

    // query: text to find in code or title (null = no text filter)
    // type : Course.TYPE_THEORY / TYPE_LAB / TYPE_CUSTOM (null = all types)
    public List<Course> search(String semesterId, String query, String type) {
        SQLiteDatabase db = helper.getReadableDatabase();

        StringBuilder where = new StringBuilder(COL_COURSE_SEMESTER_ID + "=?");
        List<String> args = new ArrayList<>();
        args.add(semesterId);

        if (query != null && !query.trim().isEmpty()) {
            where.append(" AND (" + COL_COURSE_CODE + " LIKE ? OR "
                    + COL_COURSE_TITLE + " LIKE ?)");
            String like = "%" + query.trim() + "%";
            args.add(like);
            args.add(like);
        }
        if (type != null) {
            where.append(" AND " + COL_COURSE_TYPE + "=?");
            args.add(type);
        }

        List<Course> list = new ArrayList<>();
        Cursor cursor = db.query(TABLE_COURSE, null, where.toString(),
                args.toArray(new String[0]), null, null, COL_COURSE_CODE + " ASC");
        try {
            while (cursor.moveToNext()) {
                list.add(fromCursor(cursor));
            }
        } finally {
            cursor.close();
        }
        return list;
    }
    // Courses taught by one teacher (JOIN with the course_teacher table).
    public List<Course> getByTeacher(String teacherId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        String sql = "SELECT c.* FROM " + TABLE_COURSE + " c INNER JOIN "
                + TABLE_COURSE_TEACHER + " ct ON ct." + COL_CT_COURSE_ID
                + " = c." + COL_ID
                + " WHERE ct." + COL_CT_TEACHER_ID + " = ?"
                + " ORDER BY c." + COL_COURSE_CODE + " ASC";

        List<Course> list = new ArrayList<>();
        Cursor cursor = db.rawQuery(sql, new String[]{teacherId});
        try {
            while (cursor.moveToNext()) {
                list.add(fromCursor(cursor));
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    private ContentValues toValues(Course c) {
        ContentValues values = new ContentValues();
        values.put(COL_ID, c.getId());
        values.put(COL_OWNER_UID, c.getOwnerUid());
        values.put(COL_COURSE_SEMESTER_ID, c.getSemesterId());
        values.put(COL_COURSE_CODE, c.getCode());
        values.put(COL_COURSE_TITLE, c.getTitle());
        values.put(COL_COURSE_CREDITS, c.getCredits());
        values.put(COL_COURSE_TYPE, c.getType());
        values.put(COL_COURSE_DESCRIPTION, c.getDescription());
        values.put(COL_COURSE_COLOR, c.getColor());
        values.put(COL_CREATED_AT, c.getCreatedAt());
        values.put(COL_UPDATED_AT, c.getUpdatedAt());
        values.put(COL_SYNC_STATUS, c.getSyncStatus());
        return values;
    }

    private Course fromCursor(Cursor c) {
        Course course = new Course();
        course.setId(c.getString(c.getColumnIndexOrThrow(COL_ID)));
        course.setOwnerUid(c.getString(c.getColumnIndexOrThrow(COL_OWNER_UID)));
        course.setSemesterId(c.getString(c.getColumnIndexOrThrow(COL_COURSE_SEMESTER_ID)));
        course.setCode(c.getString(c.getColumnIndexOrThrow(COL_COURSE_CODE)));
        course.setTitle(c.getString(c.getColumnIndexOrThrow(COL_COURSE_TITLE)));
        course.setCredits(c.getDouble(c.getColumnIndexOrThrow(COL_COURSE_CREDITS)));
        course.setType(c.getString(c.getColumnIndexOrThrow(COL_COURSE_TYPE)));
        course.setDescription(c.getString(c.getColumnIndexOrThrow(COL_COURSE_DESCRIPTION)));
        course.setColor(c.getInt(c.getColumnIndexOrThrow(COL_COURSE_COLOR)));
        course.setCreatedAt(c.getLong(c.getColumnIndexOrThrow(COL_CREATED_AT)));
        course.setUpdatedAt(c.getLong(c.getColumnIndexOrThrow(COL_UPDATED_AT)));
        course.setSyncStatus(c.getInt(c.getColumnIndexOrThrow(COL_SYNC_STATUS)));
        return course;
    }
}