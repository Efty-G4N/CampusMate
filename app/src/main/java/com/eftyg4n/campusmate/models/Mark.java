package com.eftyg4n.campusmate.models;

// One scored item of a course: a CT, quiz, assignment, lab test...
public class Mark extends BaseModel {

    // Stored in the database; the screen shows the matching label from mark_category_labels.
    public static final String[] CATEGORIES =
            {"CT", "QUIZ", "ASSIGNMENT", "LAB", "MID", "FINAL", "OTHER"};
    public static final String CATEGORY_CT = "CT";

    private String courseId;
    private String title;
    private String category = CATEGORY_CT;
    private double obtained;
    private double total;

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getObtained() { return obtained; }
    public void setObtained(double obtained) { this.obtained = obtained; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
}
