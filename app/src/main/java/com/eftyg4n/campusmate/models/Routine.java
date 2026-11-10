package com.eftyg4n.campusmate.models;

public class Routine extends BaseModel {

    public static final String TYPE_THEORY = "THEORY";
    public static final String TYPE_LAB = "LAB";

    public static final String STATUS_NORMAL = "NORMAL";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_RESCHEDULED = "RESCHEDULED";

    private String semesterId;
    private String courseId;
    private int dayOfWeek;      // 0 = Sunday ... 6 = Saturday
    private int startPeriod;
    private int endPeriod;
    private int startMinute;    // minutes after midnight (08:00 = 480)
    private int endMinute;
    private String room;
    private String classType = TYPE_THEORY;
    private String section;
    private String labGroup;    // null = whole section
    private String teacherCodes; // example: "SN+MAN"
    private String status = STATUS_NORMAL;
    private String note;

    // Display only (filled by the JOIN with the course table, not stored in routine)
    private String courseCode;
    private String courseTitle;
    private int courseColor;

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

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public int getStartPeriod() {
        return startPeriod;
    }

    public void setStartPeriod(int startPeriod) {
        this.startPeriod = startPeriod;
    }

    public int getEndPeriod() {
        return endPeriod;
    }

    public void setEndPeriod(int endPeriod) {
        this.endPeriod = endPeriod;
    }

    public int getStartMinute() {
        return startMinute;
    }

    public void setStartMinute(int startMinute) {
        this.startMinute = startMinute;
    }

    public int getEndMinute() {
        return endMinute;
    }

    public void setEndMinute(int endMinute) {
        this.endMinute = endMinute;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getClassType() {
        return classType;
    }

    public void setClassType(String classType) {
        this.classType = classType;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getLabGroup() {
        return labGroup;
    }

    public void setLabGroup(String labGroup) {
        this.labGroup = labGroup;
    }

    public String getTeacherCodes() {
        return teacherCodes;
    }

    public void setTeacherCodes(String teacherCodes) {
        this.teacherCodes = teacherCodes;
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

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public int getCourseColor() {
        return courseColor;
    }

    public void setCourseColor(int courseColor) {
        this.courseColor = courseColor;
    }
}