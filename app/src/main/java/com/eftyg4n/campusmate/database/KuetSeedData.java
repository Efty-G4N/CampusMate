package com.eftyg4n.campusmate.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.eftyg4n.campusmate.models.Course;
import com.eftyg4n.campusmate.models.Semester;
import com.eftyg4n.campusmate.models.Teacher;
import com.eftyg4n.campusmate.repositories.CourseRepository;
import com.eftyg4n.campusmate.repositories.SemesterRepository;
import com.eftyg4n.campusmate.repositories.TeacherRepository;
import android.util.Log;

import com.eftyg4n.campusmate.models.ClassGroup;
import com.eftyg4n.campusmate.models.Classmate;
import com.eftyg4n.campusmate.repositories.ClassGroupRepository;
import com.eftyg4n.campusmate.repositories.ClassmateRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import com.eftyg4n.campusmate.models.Routine;
import com.eftyg4n.campusmate.repositories.CourseRepository;
import com.eftyg4n.campusmate.repositories.RoutineRepository;
import java.util.HashMap;
import java.util.Map;

// Sample data: KUET CSE, 2nd Year 2nd Term, Session 2024-2025.
// Inserted only once, when the database has no semester yet.
public class KuetSeedData {

    // short code, name, department, designation, room, extension, email, phone
    // (null = not available in the faculty list)
    private static final String[][] TEACHERS = {
            {"MI", "Dr. Md. Milon Islam", "CSE", null, null, null, null, null},
            {"DD", "Mrs. Dola Das", "CSE", "Assistant Professor", "CSE 205", "8377",
                    "dola.das@kuet.ac.bd", "01679-502532"},
            {"SH", "Mr. Md. Sakhawat Hossain", "CSE", "Lecturer", "CSE 222", "377",
                    "sakhawat@kuet.ac.bd", "01863-651897"},
            {"NS", "Mr. Md. Nazirulhasan Shawon", "CSE", "Lecturer", "CSE 301(D)", "8381",
                    "shawon@kuet.ac.bd", "01609-810194"},
            {"SA", "Mr. Safin Ahmmed", "CSE", "Lecturer", "CSE 301(D)", "8387",
                    "safinahmmed@kuet.ac.bd", "01533-635763"},
            {"SN", "Ms. Subah Nawar", "CSE", null, "CSE 208", "8355",
                    "nawar@kuet.ac.bd", "01825-556567"},
            {"MAN", "Mr. Md. Mubtashim Abrar Nihal", "CSE", null, null, null, null, null},
            {"LBM", "Mrs. Lamisa Bintee Mizan Deya", "CSE", null, null, null, null, null},
            {"EK", "Mr. Ehsanul Karim", "CSE", null, null, null, null, null},
            {"FRH", "Dr. Md. Faruque Hossain", "ECE", null, null, null, null, null},
            {"FH", "Dr. Md. Faisal Hossain", "ECE", null, null, null, null, null},
            {"MRM", "Mr. Mushfiqur Rahman Masuk", "ECE", null, null, null, null, null},
            {"MIA", "Mr. Md. Minhajul Islam Arnab", "ECE", null, null, null, null, null},
            {"ARJ", "Dr. A. R. M. Jalal Uddin Jamali", "MATH", null, null, null, null, null},
            {"HZ", "Dr. Md. Hasanuzzaman", "MATH", null, null, null, null, null},
            {"RA", "Dr. S.M Rabiul Alam", "HUM", null, null, null, null, null},
            {"FA", "Ms. Farhana Afroj", "HUM", null, null, null, null, null}
    };

    // The following arrays are in the same order (index 0 = CSE 2200, ...)
    private static final String[] COURSE_CODES = {
            "CSE 2200", "CSE 2201", "CSE 2202", "CSE 2208", "CSE 2209",
            "ECE 2213", "ECE 2214", "HUM 2207", "MATH 2207"
    };

    private static final String[] COURSE_TITLES = {
            "Advanced Programming Laboratory",
            "Algorithm Analysis and Design",
            "Algorithm Analysis and Design Laboratory",
            "Numerical Methods Laboratory",
            "Theory of Computation",
            "Digital Electronics",
            "Digital Electronics Laboratory",
            "Economics and Accounting",
            "Complex Variable, Vector Analysis and Statistics"
    };

    private static final double[] COURSE_CREDITS = {
            1.5, 3.0, 1.5, 1.5, 2.0, 3.0, 1.5, 3.0, 3.0
    };

    private static final String[] COURSE_TYPES = {
            Course.TYPE_LAB, Course.TYPE_THEORY, Course.TYPE_LAB, Course.TYPE_LAB,
            Course.TYPE_THEORY, Course.TYPE_THEORY, Course.TYPE_LAB, Course.TYPE_THEORY,
            Course.TYPE_THEORY
    };

    private static final int[] COURSE_COLORS = {
            0xFF1E88E5, 0xFF8E24AA, 0xFF5E35B1, 0xFF00897B, 0xFFF4511E,
            0xFFFB8C00, 0xFF6D4C41, 0xFF43A047, 0xFFE53935
    };

    // Teacher short codes of each course (from the class routine).
    private static final String[][] COURSE_TEACHERS = {
            {"SA", "EK"},
            {"SH", "MAN"},
            {"SN", "MAN"},
            {"MI", "LBM"},
            {"DD", "NS"},
            {"FRH", "FH"},
            {"MRM", "MIA"},
            {"RA", "FA"},
            {"ARJ", "HZ"}
    };
    private static final String STUDENTS_CSV = "kuet_cse_2023_students.csv";

    // name, type, parent section name (null for a section), roll serial from, to
    // Lab groups come after their section, because the section must exist first.
    private static final String[][] CLASS_GROUPS = {
            {"A", ClassGroup.TYPE_SECTION, null, "1", "60"},
            {"B", ClassGroup.TYPE_SECTION, null, "61", "999"},
            {"A1", ClassGroup.TYPE_LAB_GROUP, "A", "1", "30"},
            {"A2", ClassGroup.TYPE_LAB_GROUP, "A", "31", "60"},
            {"B1", ClassGroup.TYPE_LAB_GROUP, "B", "61", "90"},
            {"B2", ClassGroup.TYPE_LAB_GROUP, "B", "91", "999"}
    };
    // Period times in minutes after midnight. Index = period number (1..9).
    // 08:00, 08:50, 09:40, (break) 10:40, 11:30, 12:20, (break) 14:30, 15:20, 16:10 ... 17:00
    private static final int[] PERIOD_START = {0, 480, 530, 580, 640, 690, 740, 870, 920, 970};
    private static final int[] PERIOD_END = {0, 530, 580, 630, 690, 740, 790, 920, 970, 1020};

    private static final String NOTE_C = "Marked with 'c' in the routine (meaning not confirmed)";

    // Section B (from 2-2_Routine.jpg).
    // day (0 = Sunday), first period, last period, course code, class type,
    // lab group (null = whole section), teacher codes, room, note
    private static final String[][] ROUTINE_SECTION_B = {
            // Sunday
            {"0", "2", "2", "CSE 2209", Routine.TYPE_THEORY, null, "DD", "B-105", null},
            {"0", "3", "3", "MATH 2207", Routine.TYPE_THEORY, null, "ARJ", "B-105", null},
            {"0", "4", "4", "HUM 2207", Routine.TYPE_THEORY, null, "RA/FA", "B-105", NOTE_C},
            // Monday
            {"1", "1", "1", "CSE 2201", Routine.TYPE_THEORY, null, "SH/MAN", "B-105", NOTE_C},
            {"1", "2", "2", "ECE 2213", Routine.TYPE_THEORY, null, "FRH/FH", "B-105", null},
            {"1", "3", "3", "MATH 2207", Routine.TYPE_THEORY, null, "HZ", "B-105", null},
            {"1", "4", "6", "CSE 2202", Routine.TYPE_LAB, "B1", "SN+MAN", "CSE-305", null},
            {"1", "7", "9", "ECE 2214", Routine.TYPE_LAB, "B2", "MRM+MIA", "ECE-104", null},
            // Tuesday
            {"2", "1", "1", "CSE 2209", Routine.TYPE_THEORY, null, "NS", "B-105", null},
            {"2", "2", "2", "HUM 2207", Routine.TYPE_THEORY, null, "RA", "B-105", null},
            {"2", "3", "3", "ECE 2213", Routine.TYPE_THEORY, null, "FRH", "B-105", null},
            {"2", "4", "6", "CSE 2208", Routine.TYPE_LAB, "B1", "MI+LBM", "CSE-103", null},
            {"2", "4", "6", "CSE 2202", Routine.TYPE_LAB, "B2", "SN+MAN", "CSE-305", null},
            // Wednesday
            {"3", "1", "3", "ECE 2214", Routine.TYPE_LAB, "B1", "MRM+MIA", "ECE-104", null},
            {"3", "4", "4", "MATH 2207", Routine.TYPE_THEORY, null, "ARJ/HZ", "B-105", null},
            {"3", "5", "5", "CSE 2201", Routine.TYPE_THEORY, null, "SH", "B-105", null},
            {"3", "7", "9", "CSE 2200", Routine.TYPE_LAB, "B2", "SA+EK", "CSE-202", null},
            // Thursday
            {"4", "1", "3", "CSE 2208", Routine.TYPE_LAB, "B2", "MI+LBM", "CSE-103", null},
            {"4", "1", "3", "CSE 2200", Routine.TYPE_LAB, "B1", "SA+EK", "CSE-202", null},
            {"4", "4", "4", "HUM 2207", Routine.TYPE_THEORY, null, "FA", "B-105", null},
            {"4", "5", "5", "ECE 2213", Routine.TYPE_THEORY, null, "FRH", "B-105", null},
            {"4", "6", "6", "CSE 2201", Routine.TYPE_THEORY, null, "MAN", "B-105", null}
    };

    private KuetSeedData() {
    }

    // Returns true if data was inserted, false if it was skipped or failed.
    public static boolean seedIfEmpty(Context context) {
        SemesterRepository semesterRepo = new SemesterRepository(context);
        if (!semesterRepo.getAll().isEmpty()) {
            return false;
        }

        CourseRepository courseRepo = new CourseRepository(context);
        TeacherRepository teacherRepo = new TeacherRepository(context);
        SQLiteDatabase db = DatabaseHelper.getInstance(context).getWritableDatabase();

        // One transaction: either everything is inserted or nothing.
        db.beginTransaction();
        try {
            Semester semester = new Semester();
            semester.setName("2nd Year 2nd Term");
            semester.setSession("2024-2025");
            semester.setYearNo(2);
            semester.setTermNo(2);
            semester.setCurrent(true);
            String semesterId = semesterRepo.insert(semester);
            if (semesterId == null) {
                return false;
            }

            for (int i = 0; i < TEACHERS.length; i++) {
                Teacher teacher = new Teacher();
                teacher.setShortCode(TEACHERS[i][0]);
                teacher.setName(TEACHERS[i][1]);
                teacher.setDepartment(TEACHERS[i][2]);
                teacher.setDesignation(TEACHERS[i][3]);
                teacher.setRoomNo(TEACHERS[i][4]);
                teacher.setExtension(TEACHERS[i][5]);
                teacher.setEmail(TEACHERS[i][6]);
                teacher.setPhone(TEACHERS[i][7]);
                if (teacherRepo.insert(teacher) == null) {
                    return false;
                }
            }

            for (int i = 0; i < COURSE_CODES.length; i++) {
                Course course = new Course();
                course.setSemesterId(semesterId);
                course.setCode(COURSE_CODES[i]);
                course.setTitle(COURSE_TITLES[i]);
                course.setCredits(COURSE_CREDITS[i]);
                course.setType(COURSE_TYPES[i]);
                course.setColor(COURSE_COLORS[i]);
                String courseId = courseRepo.insert(course);
                if (courseId == null) {
                    return false;
                }

                for (int j = 0; j < COURSE_TEACHERS[i].length; j++) {
                    Teacher teacher = teacherRepo.getByShortCode(COURSE_TEACHERS[i][j]);
                    if (teacher != null) {
                        teacherRepo.assignToCourse(courseId, teacher.getId());
                    }
                }
            }

            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }
    // Adds sections, lab groups and the classmate list to the current semester.
    // Each part is inserted only if it is still empty, so this is safe to call every time.
    public static void seedClassData(Context context) {
        Semester current = new SemesterRepository(context).getCurrent();
        if (current == null) {
            return;
        }

        ClassGroupRepository groupRepo = new ClassGroupRepository(context);
        ClassmateRepository classmateRepo = new ClassmateRepository(context);

        if (groupRepo.count(current.getId()) == 0) {
            SQLiteDatabase db = DatabaseHelper.getInstance(context).getWritableDatabase();
            db.beginTransaction();
            try {
                String[] ids = new String[CLASS_GROUPS.length];
                for (int i = 0; i < CLASS_GROUPS.length; i++) {
                    String parentId = null;
                    if (CLASS_GROUPS[i][2] != null) {
                        for (int j = 0; j < i; j++) {
                            if (CLASS_GROUPS[j][0].equals(CLASS_GROUPS[i][2])) {
                                parentId = ids[j];
                            }
                        }
                    }

                    ClassGroup group = new ClassGroup();
                    group.setSemesterId(current.getId());
                    group.setName(CLASS_GROUPS[i][0]);
                    group.setType(CLASS_GROUPS[i][1]);
                    group.setParentId(parentId);
                    group.setRollFrom(Integer.parseInt(CLASS_GROUPS[i][3]));
                    group.setRollTo(Integer.parseInt(CLASS_GROUPS[i][4]));
                    ids[i] = groupRepo.insert(group);
                    if (ids[i] == null) {
                        return;
                    }
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (classmateRepo.count() == 0) {
            classmateRepo.insertAll(readStudentsFromAssets(context));
        }
    }
    // Adds the Section B weekly routine to the current semester.
    // Inserted only if the routine table is still empty, so it is safe to call every time.
    public static void seedRoutineData(Context context) {
        Semester current = new SemesterRepository(context).getCurrent();
        if (current == null) {
            return;
        }

        RoutineRepository routineRepo = new RoutineRepository(context);
        if (routineRepo.count(current.getId()) > 0) {
            return;
        }

        // course code -> course id, so each routine row can point to its course
        Map<String, String> courseIds = new HashMap<>();
        List<Course> courses = new CourseRepository(context).getBySemester(current.getId());
        for (int i = 0; i < courses.size(); i++) {
            courseIds.put(courses.get(i).getCode(), courses.get(i).getId());
        }

        SQLiteDatabase db = DatabaseHelper.getInstance(context).getWritableDatabase();
        db.beginTransaction();
        try {
            for (int i = 0; i < ROUTINE_SECTION_B.length; i++) {
                String[] row = ROUTINE_SECTION_B[i];
                String courseId = courseIds.get(row[3]);
                if (courseId == null) {
                    Log.e("CampusMate_DB", "Routine: course not found " + row[3]);
                    return;
                }

                int firstPeriod = Integer.parseInt(row[1]);
                int lastPeriod = Integer.parseInt(row[2]);

                Routine routine = new Routine();
                routine.setSemesterId(current.getId());
                routine.setCourseId(courseId);
                routine.setDayOfWeek(Integer.parseInt(row[0]));
                routine.setStartPeriod(firstPeriod);
                routine.setEndPeriod(lastPeriod);
                routine.setStartMinute(PERIOD_START[firstPeriod]);
                routine.setEndMinute(PERIOD_END[lastPeriod]);
                routine.setClassType(row[4]);
                routine.setSection("B");
                routine.setLabGroup(row[5]);
                routine.setTeacherCodes(row[6]);
                routine.setRoom(row[7]);
                routine.setNote(row[8]);
                if (routineRepo.insert(routine) == null) {
                    return;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        // Check: classes per day (Sun..Thu) for Section B / lab group B2
        StringBuilder perDay = new StringBuilder();
        for (int day = 0; day <= 4; day++) {
            perDay.append(routineRepo.getForDay(current.getId(), "B", "B2", day).size());
            if (day < 4) {
                perDay.append(",");
            }
        }
        Log.d("CampusMate_DB", "Routine seeded: " + routineRepo.count(current.getId())
                + " rows; B/B2 per day (Sun..Thu) = " + perDay);
    }

    // Reads assets/kuet_cse_2023_students.csv (header: roll_no,full_name).
    private static List<Classmate> readStudentsFromAssets(Context context) {
        List<Classmate> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getAssets().open(STUDENTS_CSV), StandardCharsets.UTF_8))) {
            String line = reader.readLine(); // header line, skipped
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                int comma = line.indexOf(',');
                if (comma <= 0) {
                    continue;
                }
                Classmate classmate = new Classmate();
                classmate.setRollNo(line.substring(0, comma).trim());
                classmate.setFullName(line.substring(comma + 1).trim());
                list.add(classmate);
            }
        } catch (IOException e) {
            Log.e("CampusMate_DB", "Could not read " + STUDENTS_CSV, e);
        }
        return list;
    }
}