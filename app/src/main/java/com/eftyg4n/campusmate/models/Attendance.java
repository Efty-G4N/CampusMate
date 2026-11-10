package com.eftyg4n.campusmate.models;

// One class day of one course: was the student there or not.
public class Attendance extends BaseModel {

    public static final String STATUS_PRESENT = "PRESENT";
    public static final String STATUS_LATE = "LATE";      // counts as attended
    public static final String STATUS_ABSENT = "ABSENT";

    private String courseId;
    private long classDate;      // local midnight of the class day, in milliseconds
    private String status = STATUS_PRESENT;
    private String note;

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public long getClassDate() {
        return classDate;
    }

    public void setClassDate(long classDate) {
        this.classDate = classDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean isAttended() {
        return STATUS_PRESENT.equals(status) || STATUS_LATE.equals(status);
    }
}
