package com.eftyg4n.campusmate.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "campusmate.db";
    private static final int DB_VERSION = 8;

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

    // ---------- Table: deadline (assignments, quizzes, CTs, projects, exams ...) ----------
    public static final String TABLE_DEADLINE = "deadline";
    public static final String COL_DEADLINE_SEMESTER_ID = "semester_id";
    public static final String COL_DEADLINE_COURSE_ID = "course_id";
    public static final String COL_DEADLINE_TITLE = "title";
    public static final String COL_DEADLINE_DESCRIPTION = "description";
    public static final String COL_DEADLINE_TYPE = "type";
    public static final String COL_DEADLINE_DUE_AT = "due_at";
    public static final String COL_DEADLINE_PRIORITY = "priority";
    public static final String COL_DEADLINE_STATUS = "status";
    public static final String COL_DEADLINE_COMPLETED_AT = "completed_at";

    // ---------- Table: reminder (many reminders for one deadline) ----------
    public static final String TABLE_REMINDER = "reminder";
    public static final String COL_REMINDER_DEADLINE_ID = "deadline_id";
    public static final String COL_REMINDER_MINUTES_BEFORE = "minutes_before";
    // ---------- Table: resource (Resource Library) ----------
    public static final String TABLE_RESOURCE = "resource";
    public static final String COL_RESOURCE_COURSE_ID = "course_id";
    public static final String COL_RESOURCE_TOPIC = "topic";
    public static final String COL_RESOURCE_TITLE = "title";
    public static final String COL_RESOURCE_DESCRIPTION = "description";
    public static final String COL_RESOURCE_CATEGORY = "category";
    public static final String COL_RESOURCE_SOURCE_TYPE = "source_type";
    public static final String COL_RESOURCE_URI = "uri";
    public static final String COL_RESOURCE_LOCAL_PATH = "local_path";
    public static final String COL_RESOURCE_MIME = "mime_type";
    public static final String COL_RESOURCE_SIZE = "size_bytes";
    public static final String COL_RESOURCE_AUTHOR = "author";
    public static final String COL_RESOURCE_COVER_URL = "cover_url";
    public static final String COL_RESOURCE_REMOTE_URL = "remote_url";
    public static final String COL_RESOURCE_BOOKMARKED = "is_bookmarked";
    public static final String COL_RESOURCE_LAST_OPENED = "last_opened_at";

    public static final String TABLE_ATTENDANCE = "attendance";
    public static final String COL_ATT_COURSE_ID = "course_id";
    public static final String COL_ATT_DATE = "class_date";
    public static final String COL_ATT_STATUS = "status";
    public static final String COL_ATT_NOTE = "note";

    public static final String TABLE_MARK = "mark";
    public static final String COL_MARK_COURSE_ID = "course_id";
    public static final String COL_MARK_TITLE = "title";
    public static final String COL_MARK_CATEGORY = "category";
    public static final String COL_MARK_OBTAINED = "obtained";
    public static final String COL_MARK_TOTAL = "total";

    public static final String TABLE_TOPIC = "syllabus_topic";
    public static final String COL_TOPIC_COURSE_ID = "course_id";
    public static final String COL_TOPIC_TITLE = "title";
    public static final String COL_TOPIC_POSITION = "position";
    public static final String COL_TOPIC_DONE = "is_done";
    public static final String COL_TOPIC_DONE_AT = "done_at";

    public static final String TABLE_LECTURE = "lecture";
    public static final String COL_LECTURE_COURSE_ID = "course_id";
    public static final String COL_LECTURE_DATE = "lecture_date";
    public static final String COL_LECTURE_TITLE = "title";
    public static final String COL_LECTURE_NOTE = "note";

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
        createDeadlineTable(db);
        createReminderTable(db);
        createResourceTable(db);
        createAttendanceTable(db);
        createMarkTable(db);
        createTopicTable(db);
        createLectureTable(db);
    }

    // course_id is SET NULL (not CASCADE): deleting a course must not delete
    // the student's files, they simply become "general" resources.
    // CT, quiz, assignment, lab... marks of one course.
    private void createMarkTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_MARK + " ("
                + COL_ID + " TEXT PRIMARY KEY, "
                + COL_OWNER_UID + " TEXT, "
                + COL_MARK_COURSE_ID + " TEXT NOT NULL, "
                + COL_MARK_TITLE + " TEXT NOT NULL, "
                + COL_MARK_CATEGORY + " TEXT NOT NULL, "
                + COL_MARK_OBTAINED + " REAL NOT NULL, "
                + COL_MARK_TOTAL + " REAL NOT NULL, "
                + COL_CREATED_AT + " INTEGER NOT NULL, "
                + COL_UPDATED_AT + " INTEGER NOT NULL, "
                + COL_SYNC_STATUS + " INTEGER NOT NULL DEFAULT 0, "
                + "FOREIGN KEY (" + COL_MARK_COURSE_ID + ") REFERENCES "
                + TABLE_COURSE + "(" + COL_ID + ") ON DELETE CASCADE)");
    }

    // The syllabus of a course as a checklist of topics, kept in order.
    private void createTopicTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_TOPIC + " ("
                + COL_ID + " TEXT PRIMARY KEY, "
                + COL_OWNER_UID + " TEXT, "
                + COL_TOPIC_COURSE_ID + " TEXT NOT NULL, "
                + COL_TOPIC_TITLE + " TEXT NOT NULL, "
                + COL_TOPIC_POSITION + " INTEGER NOT NULL, "
                + COL_TOPIC_DONE + " INTEGER NOT NULL DEFAULT 0, "
                + COL_TOPIC_DONE_AT + " INTEGER, "
                + COL_CREATED_AT + " INTEGER NOT NULL, "
                + COL_UPDATED_AT + " INTEGER NOT NULL, "
                + COL_SYNC_STATUS + " INTEGER NOT NULL DEFAULT 0, "
                + "FOREIGN KEY (" + COL_TOPIC_COURSE_ID + ") REFERENCES "
                + TABLE_COURSE + "(" + COL_ID + ") ON DELETE CASCADE)");
    }

    // What was taught on which day.
    private void createLectureTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_LECTURE + " ("
                + COL_ID + " TEXT PRIMARY KEY, "
                + COL_OWNER_UID + " TEXT, "
                + COL_LECTURE_COURSE_ID + " TEXT NOT NULL, "
                + COL_LECTURE_DATE + " INTEGER NOT NULL, "
                + COL_LECTURE_TITLE + " TEXT NOT NULL, "
                + COL_LECTURE_NOTE + " TEXT, "
                + COL_CREATED_AT + " INTEGER NOT NULL, "
                + COL_UPDATED_AT + " INTEGER NOT NULL, "
                + COL_SYNC_STATUS + " INTEGER NOT NULL DEFAULT 0, "
                + "FOREIGN KEY (" + COL_LECTURE_COURSE_ID + ") REFERENCES "
                + TABLE_COURSE + "(" + COL_ID + ") ON DELETE CASCADE)");
    }

    // One row per course and class day (UNIQUE), removed together with its course.
    private void createAttendanceTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_ATTENDANCE + " ("
                + COL_ID + " TEXT PRIMARY KEY, "
                + COL_OWNER_UID + " TEXT, "
                + COL_ATT_COURSE_ID + " TEXT NOT NULL, "
                + COL_ATT_DATE + " INTEGER NOT NULL, "
                + COL_ATT_STATUS + " TEXT NOT NULL, "
                + COL_ATT_NOTE + " TEXT, "
                + COL_CREATED_AT + " INTEGER NOT NULL, "
                + COL_UPDATED_AT + " INTEGER NOT NULL, "
                + COL_SYNC_STATUS + " INTEGER NOT NULL DEFAULT 0, "
                + "UNIQUE (" + COL_ATT_COURSE_ID + ", " + COL_ATT_DATE + "), "
                + "FOREIGN KEY (" + COL_ATT_COURSE_ID + ") REFERENCES "
                + TABLE_COURSE + "(" + COL_ID + ") ON DELETE CASCADE)");
    }

    private void createResourceTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_RESOURCE + " ("
                + COL_ID + " TEXT PRIMARY KEY, "
                + COL_OWNER_UID + " TEXT, "
                + COL_RESOURCE_COURSE_ID + " TEXT, "
                + COL_RESOURCE_TOPIC + " TEXT, "
                + COL_RESOURCE_TITLE + " TEXT NOT NULL, "
                + COL_RESOURCE_DESCRIPTION + " TEXT, "
                + COL_RESOURCE_CATEGORY + " TEXT NOT NULL DEFAULT 'NOTES', "
                + COL_RESOURCE_SOURCE_TYPE + " TEXT NOT NULL DEFAULT 'FILE', "
                + COL_RESOURCE_URI + " TEXT, "
                + COL_RESOURCE_LOCAL_PATH + " TEXT, "
                + COL_RESOURCE_MIME + " TEXT, "
                + COL_RESOURCE_SIZE + " INTEGER NOT NULL DEFAULT 0, "
                + COL_RESOURCE_AUTHOR + " TEXT, "
                + COL_RESOURCE_COVER_URL + " TEXT, "
                + COL_RESOURCE_REMOTE_URL + " TEXT, "
                + COL_RESOURCE_BOOKMARKED + " INTEGER NOT NULL DEFAULT 0, "
                + COL_RESOURCE_LAST_OPENED + " INTEGER NOT NULL DEFAULT 0, "
                + COL_CREATED_AT + " INTEGER NOT NULL, "
                + COL_UPDATED_AT + " INTEGER NOT NULL, "
                + COL_SYNC_STATUS + " INTEGER NOT NULL DEFAULT 0, "
                + "FOREIGN KEY (" + COL_RESOURCE_COURSE_ID + ") REFERENCES "
                + TABLE_COURSE + "(" + COL_ID + ") ON DELETE SET NULL)");
    }

    private void createDeadlineTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_DEADLINE + " ("
                + COL_ID + " TEXT PRIMARY KEY, "
                + COL_OWNER_UID + " TEXT, "
                + COL_DEADLINE_SEMESTER_ID + " TEXT NOT NULL, "
                + COL_DEADLINE_COURSE_ID + " TEXT, "
                + COL_DEADLINE_TITLE + " TEXT NOT NULL, "
                + COL_DEADLINE_DESCRIPTION + " TEXT, "
                + COL_DEADLINE_TYPE + " TEXT NOT NULL DEFAULT 'ASSIGNMENT', "
                + COL_DEADLINE_DUE_AT + " INTEGER NOT NULL, "
                + COL_DEADLINE_PRIORITY + " INTEGER NOT NULL DEFAULT 1, "
                + COL_DEADLINE_STATUS + " TEXT NOT NULL DEFAULT 'PENDING', "
                + COL_DEADLINE_COMPLETED_AT + " INTEGER, "
                + COL_CREATED_AT + " INTEGER NOT NULL, "
                + COL_UPDATED_AT + " INTEGER NOT NULL, "
                + COL_SYNC_STATUS + " INTEGER NOT NULL DEFAULT 0, "
                + "FOREIGN KEY (" + COL_DEADLINE_SEMESTER_ID + ") REFERENCES "
                + TABLE_SEMESTER + "(" + COL_ID + ") ON DELETE CASCADE, "
                + "FOREIGN KEY (" + COL_DEADLINE_COURSE_ID + ") REFERENCES "
                + TABLE_COURSE + "(" + COL_ID + ") ON DELETE CASCADE)");
    }

    private void createReminderTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_REMINDER + " ("
                + COL_ID + " TEXT PRIMARY KEY, "
                + COL_OWNER_UID + " TEXT, "
                + COL_REMINDER_DEADLINE_ID + " TEXT NOT NULL, "
                + COL_REMINDER_MINUTES_BEFORE + " INTEGER NOT NULL, "
                + COL_CREATED_AT + " INTEGER NOT NULL, "
                + COL_UPDATED_AT + " INTEGER NOT NULL, "
                + COL_SYNC_STATUS + " INTEGER NOT NULL DEFAULT 0, "
                + "UNIQUE (" + COL_REMINDER_DEADLINE_ID + ", "
                + COL_REMINDER_MINUTES_BEFORE + "), "
                + "FOREIGN KEY (" + COL_REMINDER_DEADLINE_ID + ") REFERENCES "
                + TABLE_DEADLINE + "(" + COL_ID + ") ON DELETE CASCADE)");
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
        // Version 4 -> 5: deadlines and their reminders.
        if (oldVersion < 5) {
            createDeadlineTable(db);
            createReminderTable(db);
        }
        // Version 5 -> 6: Resource Library.
        if (oldVersion < 6) {
            createResourceTable(db);
        }
        // Version 6 -> 7: attendance records.
        if (oldVersion < 7) {
            createAttendanceTable(db);
        }
        // Version 7 -> 8: marks, syllabus topics, lecture history.
        if (oldVersion < 8) {
            createMarkTable(db);
            createTopicTable(db);
            createLectureTable(db);
        }
    }
}