package com.eftyg4n.campusmate.models;

// One line of a course syllabus, ticked off when it has been covered.
public class SyllabusTopic extends BaseModel {

    private String courseId;
    private String title;
    private int position;
    private boolean done;
    private long doneAt;      // 0 while not done

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }
    public long getDoneAt() { return doneAt; }
    public void setDoneAt(long doneAt) { this.doneAt = doneAt; }
}
