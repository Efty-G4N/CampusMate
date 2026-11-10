package com.eftyg4n.campusmate.models;

public class Deadline extends BaseModel {

    // ---------- Type ----------
    public static final String TYPE_ASSIGNMENT = "ASSIGNMENT";
    public static final String TYPE_QUIZ = "QUIZ";
    public static final String TYPE_CT = "CT";
    public static final String TYPE_PROJECT = "PROJECT";
    public static final String TYPE_LAB_SUBMISSION = "LAB_SUBMISSION";
    public static final String TYPE_PRESENTATION = "PRESENTATION";
    public static final String TYPE_FINAL_EXAM = "FINAL_EXAM";
    public static final String TYPE_PRACTICAL_EXAM = "PRACTICAL_EXAM";
    public static final String TYPE_CUSTOM = "CUSTOM";

    // The order here must match the string-array "deadline_type_labels" in strings.xml.
    public static final String[] TYPES = {
            TYPE_ASSIGNMENT, TYPE_QUIZ, TYPE_CT, TYPE_PROJECT, TYPE_LAB_SUBMISSION,
            TYPE_PRESENTATION, TYPE_FINAL_EXAM, TYPE_PRACTICAL_EXAM, TYPE_CUSTOM
    };

    // ---------- Priority (also the index in "deadline_priority_labels") ----------
    public static final int PRIORITY_LOW = 0;
    public static final int PRIORITY_MEDIUM = 1;
    public static final int PRIORITY_HIGH = 2;

    // ---------- Status ----------
    // "Overdue" is NOT stored: it is calculated (pending + due time already passed).
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";

    private String semesterId;
    private String courseId;      // null = a general task that belongs to no course
    private String title;
    private String description;
    private String type = TYPE_ASSIGNMENT;
    private long dueAt;           // milliseconds since 1970
    private int priority = PRIORITY_MEDIUM;
    private String status = STATUS_PENDING;
    private long completedAt;     // 0 = not completed

    // Filled only by queries that JOIN the course table (not stored in this table).
    private String courseCode;
    private int courseColor;

    public static int typeIndex(String type) {
        for (int i = 0; i < TYPES.length; i++) {
            if (TYPES[i].equals(type)) {
                return i;
            }
        }
        return 0;
    }

    public boolean isCompleted() {
        return STATUS_COMPLETED.equals(status);
    }

    public boolean isOverdue(long now) {
        return !isCompleted() && dueAt < now;
    }

    public String getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(String semesterId) {
        this.semesterId = semesterId;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getDueAt() {
        return dueAt;
    }

    public void setDueAt(long dueAt) {
        this.dueAt = dueAt;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public int getCourseColor() {
        return courseColor;
    }

    public void setCourseColor(int courseColor) {
        this.courseColor = courseColor;
    }
}
