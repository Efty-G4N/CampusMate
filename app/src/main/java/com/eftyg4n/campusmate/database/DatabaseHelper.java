package com.eftyg4n.campusmate.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "campusmate.db";
    private static final int DB_VERSION = 4;

    // ---------- Sync status values ----------
    public static final int SYNC_PENDING = 0;
    public static final int SYNC_SYNCED = 1;

    // ---------- Common columns (every table) ----------
    public static final String COL_ID = "id";
    public static final String COL_OWNER_UID = "owner_uid";
    public static final String COL_CREATED_AT = "created_at";
    public static final String COL_UPDATED_AT = "updated_at";
    public static final String COL_SYNC_STATUS = "sync_status";

    // ---------- Table: user_profile ----------
    public static final String TABLE_USER_PROFILE = "user_profile";
    public static final String COL_PROFILE_NAME = "name";
    public static final String COL_PROFILE_EMAIL = "email";
    public static final String COL_PROFILE_UNIVERSITY = "university";
    public static final String COL_PROFILE_DEPARTMENT = "department";
    public static final String COL_PROFILE_SESSION = "session";
    public static final String COL_PROFILE_YEAR = "year_no";
    public static final String COL_PROFILE_TERM = "term_no";
    public static final String COL_PROFILE_SECTION = "section";
    public static final String COL_PROFILE_PHOTO_URI = "photo_uri";
    public static final String COL_PROFILE_ROLL_NO = "roll_no";
    public static final String COL_PROFILE_LAB_GROUP = "lab_group";
    public static final String COL_PROFILE_SERIAL_DIGITS = "roll_serial_digits";

    // ---------- Table: semester ----------
    public static final String TABLE_SEMESTER = "semester";
    public static final String COL_SEMESTER_NAME = "name";
    public static final String COL_SEMESTER_SESSION = "session";
    public static final String COL_SEMESTER_YEAR = "year_no";
    public static final String COL_SEMESTER_TERM = "term_no";
    public static final String COL_SEMESTER_SECTION = "section";
    public static final String COL_SEMESTER_START_DATE = "start_date";
    public static final String COL_SEMESTER_END_DATE = "end_date";
    public static final String COL_SEMESTER_IS_CURRENT = "is_current";
    public static final String COL_SEMESTER_IS_ARCHIVED = "is_archived";

    // ---------- Table: course ----------
    public static final String TABLE_COURSE = "course";
    public static final String COL_COURSE_SEMESTER_ID = "semester_id";
    public static final String COL_COURSE_CODE = "code";
    public static final String COL_COURSE_TITLE = "title";
    public static final String COL_COURSE_CREDITS = "credits";
    public static final String COL_COURSE_TYPE = "type";
    public static final String COL_COURSE_DESCRIPTION = "description";
    public static final String COL_COURSE_COLOR = "color";

    // ---------- Table: teacher ----------
    public static final String TABLE_TEACHER = "teacher";
    public static final String COL_TEACHER_NAME = "name";
    public static final String COL_TEACHER_SHORT_CODE = "short_code";
    public static final String COL_TEACHER_DEPARTMENT = "department";
    public static final String COL_TEACHER_EMAIL = "email";
    public static final String COL_TEACHER_PHONE = "phone";
    public static final String COL_TEACHER_DESIGNATION = "designation";
    public static final String COL_TEACHER_ROOM_NO = "room_no";
    public static final String COL_TEACHER_EXTENSION = "extension";


    // ---------- Table: course_teacher ----------
    public static final String TABLE_COURSE_TEACHER = "course_teacher";
    public static final String COL_CT_COURSE_ID = "course_id";
    public static final String COL_CT_TEACHER_ID = "teacher_id";
    // ---------- Table: class_group (sections and lab groups by roll range) ----------
    public static final String TABLE_CLASS_GROUP = "class_group";
    public static final String COL_GROUP_SEMESTER_ID = "semester_id";
    public static final String COL_GROUP_PARENT_ID = "parent_id";
    public static final String COL_GROUP_NAME = "name";
    public static final String COL_GROUP_TYPE = "type";
    public static final String COL_GROUP_ROLL_FROM = "roll_from";
    public static final String COL_GROUP_ROLL_TO = "roll_to";

    // ---------- Table: classmate ----------
    public static final String TABLE_CLASSMATE = "classmate";
    public static final String COL_CLASSMATE_ROLL_NO = "roll_no";
    public static final String COL_CLASSMATE_FULL_NAME = "full_name";
    // ---------- Table: routine (weekly class slots) ----------
    public static final String TABLE_ROUTINE = "routine";
    public static final String COL_ROUTINE_SEMESTER_ID = "semester_id";
    public static final String COL_ROUTINE_COURSE_ID = "course_id";
    public static final String COL_ROUTINE_DAY = "day_of_week";
    public static final String COL_ROUTINE_START_PERIOD = "start_period";
    public static final String COL_ROUTINE_END_PERIOD = "end_period";
    public static final String COL_ROUTINE_START_MINUTE = "start_minute";
    public static final String COL_ROUTINE_END_MINUTE = "end_minute";
    public static final String COL_ROUTINE_ROOM = "room";
    public static final String COL_ROUTINE_CLASS_TYPE = "class_type";
    public static final String COL_ROUTINE_SECTION = "section";
    public static final String COL_ROUTINE_LAB_GROUP = "lab_group";
    public static final String COL_ROUTINE_TEACHER_CODES = "teacher_codes";
    public static final String COL_ROUTINE_STATUS = "status";
    public static final String COL_ROUTINE_NOTE = "note";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USER_PROFILE + " ("
                + COL_ID + " TEXT PRIMARY KEY, "
                + COL_OWNER_UID + " TEXT, "
                + COL_PROFILE_NAME + " TEXT NOT NULL, "
                + COL_PROFILE_EMAIL + " TEXT, "
                + COL_PROFILE_UNIVERSITY + " TEXT, "
                + COL_PROFILE_DEPARTMENT + " TEXT, "
                + COL_PROFILE_SESSION + " TEXT, "
                + COL_PROFILE_YEAR + " INTEGER, "
                + COL_PROFILE_TERM + " INTEGER, "
                + COL_PROFILE_SECTION + " TEXT, "
                + COL_PROFILE_PHOTO_URI + " TEXT, "
                + COL_PROFILE_ROLL_NO + " TEXT, "
                + COL_PROFILE_LAB_GROUP + " TEXT, "
                + COL_PROFILE_SERIAL_DIGITS + " INTEGER NOT NULL DEFAULT 3, "
                + COL_CREATED_AT + " INTEGER NOT NULL, "
                + COL_UPDATED_AT + " INTEGER NOT NULL, "
                + COL_SYNC_STATUS + " INTEGER NOT NULL DEFAULT 0)");

        db.execSQL("CREATE TABLE " + TABLE_SEMESTER + " ("
                + COL_ID + " TEXT PRIMARY KEY, "
                + COL_OWNER_UID + " TEXT, "
                + COL_SEMESTER_NAME + " TEXT NOT NULL, "
                + COL_SEMESTER_SESSION + " TEXT, "
                + COL_SEMESTER_YEAR + " INTEGER, "
                + COL_SEMESTER_TERM + " INTEGER, "
                + COL_SEMESTER_SECTION + " TEXT, "
                + COL_SEMESTER_START_DATE + " INTEGER, "
                + COL_SEMESTER_END_DATE + " INTEGER, "
                + COL_SEMESTER_IS_CURRENT + " INTEGER NOT NULL DEFAULT 0, "
                + COL_SEMESTER_IS_ARCHIVED + " INTEGER NOT NULL DEFAULT 0, "
                + COL_CREATED_AT + " INTEGER NOT NULL, "
                + COL_UPDATED_AT + " INTEGER NOT NULL, "
                + COL_SYNC_STATUS + " INTEGER NOT NULL DEFAULT 0)");

        db.execSQL("CREATE TABLE " + TABLE_COURSE + " ("
                + COL_ID + " TEXT PRIMARY KEY, "
                + COL_OWNER_UID + " TEXT, "
                + COL_COURSE_SEMESTER_ID + " TEXT NOT NULL, "
                + COL_COURSE_CODE + " TEXT NOT NULL, "
                + COL_COURSE_TITLE + " TEXT NOT NULL, "
                + COL_COURSE_CREDITS + " REAL, "
                + COL_COURSE_TYPE + " TEXT NOT NULL DEFAULT 'THEORY', "
                + COL_COURSE_DESCRIPTION + " TEXT, "
                + COL_COURSE_COLOR + " INTEGER, "
                + COL_CREATED_AT + " INTEGER NOT NULL, "
                + COL_UPDATED_AT + " INTEGER NOT NULL, "
                + COL_SYNC_STATUS + " INTEGER NOT NULL DEFAULT 0, "
                + "UNIQUE (" + COL_COURSE_SEMESTER_ID + ", " + COL_COURSE_CODE + "), "
                + "FOREIGN KEY (" + COL_COURSE_SEMESTER_ID + ") REFERENCES "
                + TABLE_SEMESTER + "(" + COL_ID + ") ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE " + TABLE_TEACHER + " ("
                + COL_ID + " TEXT PRIMARY KEY, "
                + COL_OWNER_UID + " TEXT, "
                + COL_TEACHER_NAME + " TEXT NOT NULL, "
                + COL_TEACHER_SHORT_CODE + " TEXT, "
                + COL_TEACHER_DEPARTMENT + " TEXT, "
                + COL_TEACHER_EMAIL + " TEXT, "
                + COL_TEACHER_PHONE + " TEXT, "
                + COL_TEACHER_DESIGNATION + " TEXT, "
                + COL_TEACHER_ROOM_NO + " TEXT, "
                + COL_TEACHER_EXTENSION + " TEXT, "
                + COL_CREATED_AT + " INTEGER NOT NULL, "
                + COL_UPDATED_AT + " INTEGER NOT NULL, "
                + COL_SYNC_STATUS + " INTEGER NOT NULL DEFAULT 0)");

        db.execSQL("CREATE TABLE " + TABLE_COURSE_TEACHER + " ("
                + COL_CT_COURSE_ID + " TEXT NOT NULL, "
                + COL_CT_TEACHER_ID + " TEXT NOT NULL, "
                + COL_CREATED_AT + " INTEGER NOT NULL, "
                + COL_SYNC_STATUS + " INTEGER NOT NULL DEFAULT 0, "
                + "PRIMARY KEY (" + COL_CT_COURSE_ID + ", " + COL_CT_TEACHER_ID + "), "
                + "FOREIGN KEY (" + COL_CT_COURSE_ID + ") REFERENCES "
                + TABLE_COURSE + "(" + COL_ID + ") ON DELETE CASCADE, "
                + "FOREIGN KEY (" + COL_CT_TEACHER_ID + ") REFERENCES "
                + TABLE_TEACHER + "(" + COL_ID + ") ON DELETE CASCADE)");
        createClassGroupTable(db);
        createClassmateTable(db);
        createRoutineTable(db);
    }
    private void createRoutineTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_ROUTINE + " ("
                + COL_ID + " TEXT PRIMARY KEY, "
                + COL_OWNER_UID + " TEXT, "
                + COL_ROUTINE_SEMESTER_ID + " TEXT NOT NULL, "
                + COL_ROUTINE_COURSE_ID + " TEXT NOT NULL, "
                + COL_ROUTINE_DAY + " INTEGER NOT NULL, "
                + COL_ROUTINE_START_PERIOD + " INTEGER NOT NULL, "
                + COL_ROUTINE_END_PERIOD + " INTEGER NOT NULL, "
                + COL_ROUTINE_START_MINUTE + " INTEGER NOT NULL, "
                + COL_ROUTINE_END_MINUTE + " INTEGER NOT NULL, "
                + COL_ROUTINE_ROOM + " TEXT, "
                + COL_ROUTINE_CLASS_TYPE + " TEXT NOT NULL DEFAULT 'THEORY', "
                + COL_ROUTINE_SECTION + " TEXT, "
                + COL_ROUTINE_LAB_GROUP + " TEXT, "
                + COL_ROUTINE_TEACHER_CODES + " TEXT, "
                + COL_ROUTINE_STATUS + " TEXT NOT NULL DEFAULT 'NORMAL', "
                + COL_ROUTINE_NOTE + " TEXT, "
                + COL_CREATED_AT + " INTEGER NOT NULL, "
                + COL_UPDATED_AT + " INTEGER NOT NULL, "
                + COL_SYNC_STATUS + " INTEGER NOT NULL DEFAULT 0, "
                + "FOREIGN KEY (" + COL_ROUTINE_SEMESTER_ID + ") REFERENCES "
                + TABLE_SEMESTER + "(" + COL_ID + ") ON DELETE CASCADE, "
                + "FOREIGN KEY (" + COL_ROUTINE_COURSE_ID + ") REFERENCES "
                + TABLE_COURSE + "(" + COL_ID + ") ON DELETE CASCADE)");
    }
    private void createClassGroupTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_CLASS_GROUP + " ("
                + COL_ID + " TEXT PRIMARY KEY, "
                + COL_OWNER_UID + " TEXT, "
                + COL_GROUP_SEMESTER_ID + " TEXT NOT NULL, "
                + COL_GROUP_PARENT_ID + " TEXT, "
                + COL_GROUP_NAME + " TEXT NOT NULL, "
                + COL_GROUP_TYPE + " TEXT NOT NULL, "
                + COL_GROUP_ROLL_FROM + " INTEGER NOT NULL, "
                + COL_GROUP_ROLL_TO + " INTEGER NOT NULL, "
                + COL_CREATED_AT + " INTEGER NOT NULL, "
                + COL_UPDATED_AT + " INTEGER NOT NULL, "
                + COL_SYNC_STATUS + " INTEGER NOT NULL DEFAULT 0, "
                + "UNIQUE (" + COL_GROUP_SEMESTER_ID + ", " + COL_GROUP_NAME + "), "
                + "FOREIGN KEY (" + COL_GROUP_SEMESTER_ID + ") REFERENCES "
                + TABLE_SEMESTER + "(" + COL_ID + ") ON DELETE CASCADE, "
                + "FOREIGN KEY (" + COL_GROUP_PARENT_ID + ") REFERENCES "
                + TABLE_CLASS_GROUP + "(" + COL_ID + ") ON DELETE CASCADE)");
    }

    private void createClassmateTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_CLASSMATE + " ("
                + COL_ID + " TEXT PRIMARY KEY, "
                + COL_OWNER_UID + " TEXT, "
                + COL_CLASSMATE_ROLL_NO + " TEXT NOT NULL UNIQUE, "
                + COL_CLASSMATE_FULL_NAME + " TEXT NOT NULL, "
                + COL_CREATED_AT + " INTEGER NOT NULL, "
                + COL_UPDATED_AT + " INTEGER NOT NULL, "
                + COL_SYNC_STATUS + " INTEGER NOT NULL DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1 -> 2: teachers get designation, room number and extension.
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_TEACHER + " ADD COLUMN "
                    + COL_TEACHER_DESIGNATION + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_TEACHER + " ADD COLUMN "
                    + COL_TEACHER_ROOM_NO + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_TEACHER + " ADD COLUMN "
                    + COL_TEACHER_EXTENSION + " TEXT");
        }
        // Version 2 -> 3: roll number and lab group in profile, class groups, classmates.
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_USER_PROFILE + " ADD COLUMN "
                    + COL_PROFILE_ROLL_NO + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_USER_PROFILE + " ADD COLUMN "
                    + COL_PROFILE_LAB_GROUP + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_USER_PROFILE + " ADD COLUMN "
                    + COL_PROFILE_SERIAL_DIGITS + " INTEGER NOT NULL DEFAULT 3");
            createClassGroupTable(db);
            createClassmateTable(db);
        }
        // Version 3 -> 4: weekly routine table.
        if (oldVersion < 4) {
            createRoutineTable(db);
        }
    }
}