package com.eftyg4n.campusmate.repositories;

import static com.eftyg4n.campusmate.database.DatabaseHelper.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.eftyg4n.campusmate.database.DatabaseHelper;
import com.eftyg4n.campusmate.models.UserProfile;
import com.eftyg4n.campusmate.utils.IdUtils;

public class UserProfileRepository {

    private final DatabaseHelper helper;

    public UserProfileRepository(Context context) {
        helper = DatabaseHelper.getInstance(context);
    }

    // Returns the student's profile, or null if it was not created yet.
    public UserProfile getProfile() {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USER_PROFILE, null, null, null, null, null,
                COL_CREATED_AT + " ASC", "1");
        try {
            if (cursor.moveToFirst()) {
                return fromCursor(cursor);
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    // Creates the profile if it has no id yet, otherwise updates it.
    public boolean save(UserProfile profile) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long now = System.currentTimeMillis();
        profile.setUpdatedAt(now);
        profile.setSyncStatus(SYNC_PENDING);

        if (profile.getId() == null) {
            profile.setId(IdUtils.newId());
            profile.setCreatedAt(now);
            return db.insert(TABLE_USER_PROFILE, null, toValues(profile)) != -1;
        }
        return db.update(TABLE_USER_PROFILE, toValues(profile),
                COL_ID + "=?", new String[]{profile.getId()}) > 0;
    }

    public boolean delete(String id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.delete(TABLE_USER_PROFILE, COL_ID + "=?", new String[]{id}) > 0;
    }

    private ContentValues toValues(UserProfile p) {
        ContentValues values = new ContentValues();
        values.put(COL_ID, p.getId());
        values.put(COL_OWNER_UID, p.getOwnerUid());
        values.put(COL_PROFILE_NAME, p.getName());
        values.put(COL_PROFILE_EMAIL, p.getEmail());
        values.put(COL_PROFILE_UNIVERSITY, p.getUniversity());
        values.put(COL_PROFILE_DEPARTMENT, p.getDepartment());
        values.put(COL_PROFILE_SESSION, p.getSession());
        values.put(COL_PROFILE_YEAR, p.getYearNo());
        values.put(COL_PROFILE_TERM, p.getTermNo());
        values.put(COL_PROFILE_SECTION, p.getSection());
        values.put(COL_PROFILE_LAB_GROUP, p.getLabGroup());
        values.put(COL_PROFILE_ROLL_NO, p.getRollNo());
        values.put(COL_PROFILE_SERIAL_DIGITS, p.getRollSerialDigits());
        values.put(COL_PROFILE_PHOTO_URI, p.getPhotoUri());
        values.put(COL_CREATED_AT, p.getCreatedAt());
        values.put(COL_UPDATED_AT, p.getUpdatedAt());
        values.put(COL_SYNC_STATUS, p.getSyncStatus());
        return values;
    }

    private UserProfile fromCursor(Cursor c) {
        UserProfile p = new UserProfile();
        p.setId(c.getString(c.getColumnIndexOrThrow(COL_ID)));
        p.setOwnerUid(c.getString(c.getColumnIndexOrThrow(COL_OWNER_UID)));
        p.setName(c.getString(c.getColumnIndexOrThrow(COL_PROFILE_NAME)));
        p.setEmail(c.getString(c.getColumnIndexOrThrow(COL_PROFILE_EMAIL)));
        p.setUniversity(c.getString(c.getColumnIndexOrThrow(COL_PROFILE_UNIVERSITY)));
        p.setDepartment(c.getString(c.getColumnIndexOrThrow(COL_PROFILE_DEPARTMENT)));
        p.setSession(c.getString(c.getColumnIndexOrThrow(COL_PROFILE_SESSION)));
        p.setYearNo(c.getInt(c.getColumnIndexOrThrow(COL_PROFILE_YEAR)));
        p.setTermNo(c.getInt(c.getColumnIndexOrThrow(COL_PROFILE_TERM)));
        p.setSection(c.getString(c.getColumnIndexOrThrow(COL_PROFILE_SECTION)));
        p.setLabGroup(c.getString(c.getColumnIndexOrThrow(COL_PROFILE_LAB_GROUP)));
        p.setRollNo(c.getString(c.getColumnIndexOrThrow(COL_PROFILE_ROLL_NO)));
        p.setRollSerialDigits(c.getInt(c.getColumnIndexOrThrow(COL_PROFILE_SERIAL_DIGITS)));
        p.setPhotoUri(c.getString(c.getColumnIndexOrThrow(COL_PROFILE_PHOTO_URI)));
        p.setCreatedAt(c.getLong(c.getColumnIndexOrThrow(COL_CREATED_AT)));
        p.setUpdatedAt(c.getLong(c.getColumnIndexOrThrow(COL_UPDATED_AT)));
        p.setSyncStatus(c.getInt(c.getColumnIndexOrThrow(COL_SYNC_STATUS)));
        return p;
    }
}