package com.eftyg4n.campusmate.repositories;

import static com.eftyg4n.campusmate.database.DatabaseHelper.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.eftyg4n.campusmate.database.DatabaseHelper;
import com.eftyg4n.campusmate.models.Resource;
import com.eftyg4n.campusmate.utils.IdUtils;

import java.util.ArrayList;
import java.util.List;

public class ResourceRepository {

    public static final int MODE_ALL = 0;
    public static final int MODE_BOOKMARKED = 1;
    public static final int MODE_RECENT = 2;

    private static final String SELECT_WITH_COURSE =
            "SELECT r.*, c." + COL_COURSE_CODE + " AS course_code FROM " + TABLE_RESOURCE
                    + " r LEFT JOIN " + TABLE_COURSE + " c ON c." + COL_ID
                    + " = r." + COL_RESOURCE_COURSE_ID;

    private final DatabaseHelper helper;

    public ResourceRepository(Context context) {
        helper = DatabaseHelper.getInstance(context);
    }

    public String insert(Resource r) {
        long now = System.currentTimeMillis();
        r.setId(IdUtils.newId());
        r.setCreatedAt(now);
        r.setUpdatedAt(now);
        r.setSyncStatus(SYNC_PENDING);
        long row = helper.getWritableDatabase().insert(TABLE_RESOURCE, null, toValues(r));
        return row == -1 ? null : r.getId();
    }

    public boolean update(Resource r) {
        r.setUpdatedAt(System.currentTimeMillis());
        r.setSyncStatus(SYNC_PENDING);
        return helper.getWritableDatabase().update(TABLE_RESOURCE, toValues(r),
                COL_ID + "=?", new String[]{r.getId()}) > 0;
    }

    // Only the database row is deleted here; the caller deletes the local file.
    public boolean delete(String id) {
        return helper.getWritableDatabase().delete(TABLE_RESOURCE,
                COL_ID + "=?", new String[]{id}) > 0;
    }

    public boolean setBookmarked(String id, boolean bookmarked) {
        ContentValues values = new ContentValues();
        values.put(COL_RESOURCE_BOOKMARKED, bookmarked ? 1 : 0);
        values.put(COL_UPDATED_AT, System.currentTimeMillis());
        values.put(COL_SYNC_STATUS, SYNC_PENDING);
        return helper.getWritableDatabase().update(TABLE_RESOURCE, values,
                COL_ID + "=?", new String[]{id}) > 0;
    }

    public void markOpened(String id) {
        ContentValues values = new ContentValues();
        values.put(COL_RESOURCE_LAST_OPENED, System.currentTimeMillis());
        helper.getWritableDatabase().update(TABLE_RESOURCE, values,
                COL_ID + "=?", new String[]{id});
    }

    // Used after a download finishes: attach the downloaded file to the row.
    public boolean setLocalFile(String id, String localPath, String mime, long size) {
        ContentValues values = new ContentValues();
        values.put(COL_RESOURCE_LOCAL_PATH, localPath);
        if (mime != null) {
            values.put(COL_RESOURCE_MIME, mime);
        }
        values.put(COL_RESOURCE_SIZE, size);
        values.put(COL_UPDATED_AT, System.currentTimeMillis());
        values.put(COL_SYNC_STATUS, SYNC_PENDING);
        return helper.getWritableDatabase().update(TABLE_RESOURCE, values,
                COL_ID + "=?", new String[]{id}) > 0;
    }

    public Resource getById(String id) {
        Cursor cursor = helper.getReadableDatabase().rawQuery(
                SELECT_WITH_COURSE + " WHERE r." + COL_ID + "=?", new String[]{id});
        try {
            return cursor.moveToFirst() ? fromCursor(cursor) : null;
        } finally {
            cursor.close();
        }
    }

    // courseId != null  -> only that course.
    // courseId == null  -> this semester's courses + general resources (no course).
    // category == null  -> every category.
    public List<Resource> search(String semesterId, String courseId, String query,
                                 String category, int mode) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<String> args = new ArrayList<>();

        if (courseId != null) {
            where.append(" AND r.").append(COL_RESOURCE_COURSE_ID).append("=?");
            args.add(courseId);
        } else {
            where.append(" AND (c.").append(COL_COURSE_SEMESTER_ID).append("=? OR r.")
                    .append(COL_RESOURCE_COURSE_ID).append(" IS NULL)");
            args.add(semesterId);
        }

        if (category != null) {
            where.append(" AND r.").append(COL_RESOURCE_CATEGORY).append("=?");
            args.add(category);
        }
        if (mode == MODE_BOOKMARKED) {
            where.append(" AND r.").append(COL_RESOURCE_BOOKMARKED).append("=1");
        } else if (mode == MODE_RECENT) {
            where.append(" AND r.").append(COL_RESOURCE_LAST_OPENED).append(">0");
        }

        if (query != null && !query.trim().isEmpty()) {
            String like = "%" + query.trim() + "%";
            where.append(" AND (r.").append(COL_RESOURCE_TITLE).append(" LIKE ? OR r.")
                    .append(COL_RESOURCE_TOPIC).append(" LIKE ? OR r.")
                    .append(COL_RESOURCE_AUTHOR).append(" LIKE ? OR c.")
                    .append(COL_COURSE_CODE).append(" LIKE ?)");
            for (int i = 0; i < 4; i++) {
                args.add(like);
            }
        }

        String order = mode == MODE_RECENT
                ? " ORDER BY r." + COL_RESOURCE_LAST_OPENED + " DESC"
                : " ORDER BY r." + COL_CREATED_AT + " DESC";

        List<Resource> list = new ArrayList<>();
        Cursor cursor = helper.getReadableDatabase().rawQuery(
                SELECT_WITH_COURSE + where + order, args.toArray(new String[0]));
        try {
            while (cursor.moveToNext()) {
                list.add(fromCursor(cursor));
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    private ContentValues toValues(Resource r) {
        ContentValues v = new ContentValues();
        v.put(COL_ID, r.getId());
        v.put(COL_OWNER_UID, r.getOwnerUid());
        v.put(COL_RESOURCE_COURSE_ID, r.getCourseId());
        v.put(COL_RESOURCE_TOPIC, r.getTopic());
        v.put(COL_RESOURCE_TITLE, r.getTitle());
        v.put(COL_RESOURCE_DESCRIPTION, r.getDescription());
        v.put(COL_RESOURCE_CATEGORY, r.getCategory());
        v.put(COL_RESOURCE_SOURCE_TYPE, r.getSourceType());
        v.put(COL_RESOURCE_URI, r.getUri());
        v.put(COL_RESOURCE_LOCAL_PATH, r.getLocalPath());
        v.put(COL_RESOURCE_MIME, r.getMimeType());
        v.put(COL_RESOURCE_SIZE, r.getSizeBytes());
        v.put(COL_RESOURCE_AUTHOR, r.getAuthor());
        v.put(COL_RESOURCE_COVER_URL, r.getCoverUrl());
        v.put(COL_RESOURCE_REMOTE_URL, r.getRemoteUrl());
        v.put(COL_RESOURCE_BOOKMARKED, r.isBookmarked() ? 1 : 0);
        v.put(COL_RESOURCE_LAST_OPENED, r.getLastOpenedAt());
        v.put(COL_CREATED_AT, r.getCreatedAt());
        v.put(COL_UPDATED_AT, r.getUpdatedAt());
        v.put(COL_SYNC_STATUS, r.getSyncStatus());
        return v;
    }

    private Resource fromCursor(Cursor c) {
        Resource r = new Resource();
        r.setId(c.getString(c.getColumnIndexOrThrow(COL_ID)));
        r.setOwnerUid(c.getString(c.getColumnIndexOrThrow(COL_OWNER_UID)));
        r.setCourseId(c.getString(c.getColumnIndexOrThrow(COL_RESOURCE_COURSE_ID)));
        r.setTopic(c.getString(c.getColumnIndexOrThrow(COL_RESOURCE_TOPIC)));
        r.setTitle(c.getString(c.getColumnIndexOrThrow(COL_RESOURCE_TITLE)));
        r.setDescription(c.getString(c.getColumnIndexOrThrow(COL_RESOURCE_DESCRIPTION)));
        r.setCategory(c.getString(c.getColumnIndexOrThrow(COL_RESOURCE_CATEGORY)));
        r.setSourceType(c.getString(c.getColumnIndexOrThrow(COL_RESOURCE_SOURCE_TYPE)));
        r.setUri(c.getString(c.getColumnIndexOrThrow(COL_RESOURCE_URI)));
        r.setLocalPath(c.getString(c.getColumnIndexOrThrow(COL_RESOURCE_LOCAL_PATH)));
        r.setMimeType(c.getString(c.getColumnIndexOrThrow(COL_RESOURCE_MIME)));
        r.setSizeBytes(c.getLong(c.getColumnIndexOrThrow(COL_RESOURCE_SIZE)));
        r.setAuthor(c.getString(c.getColumnIndexOrThrow(COL_RESOURCE_AUTHOR)));
        r.setCoverUrl(c.getString(c.getColumnIndexOrThrow(COL_RESOURCE_COVER_URL)));
        r.setRemoteUrl(c.getString(c.getColumnIndexOrThrow(COL_RESOURCE_REMOTE_URL)));
        r.setBookmarked(c.getInt(c.getColumnIndexOrThrow(COL_RESOURCE_BOOKMARKED)) == 1);
        r.setLastOpenedAt(c.getLong(c.getColumnIndexOrThrow(COL_RESOURCE_LAST_OPENED)));
        r.setCreatedAt(c.getLong(c.getColumnIndexOrThrow(COL_CREATED_AT)));
        r.setUpdatedAt(c.getLong(c.getColumnIndexOrThrow(COL_UPDATED_AT)));
        r.setSyncStatus(c.getInt(c.getColumnIndexOrThrow(COL_SYNC_STATUS)));
        r.setCourseCode(c.getString(c.getColumnIndexOrThrow("course_code")));
        return r;
    }
}
