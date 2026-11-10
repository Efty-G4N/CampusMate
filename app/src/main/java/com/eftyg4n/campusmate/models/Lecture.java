package com.eftyg4n.campusmate.models;

// What was taught on one day of one course.
public class Lecture extends BaseModel {

    private String courseId;
    private long lectureDate;    // local midnight of that day
    private String title;
    private String note;

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public long getLectureDate() { return lectureDate; }
    public void setLectureDate(long lectureDate) { this.lectureDate = lectureDate; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
