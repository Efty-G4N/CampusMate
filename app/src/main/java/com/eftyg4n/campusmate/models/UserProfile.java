package com.eftyg4n.campusmate.models;

public class UserProfile extends BaseModel {

    private String name;
    private String email;
    private String university;
    private String department;
    private String session;
    private int yearNo;
    private int termNo;
    private String section;
    private String labGroup;
    private String rollNo;
    private int rollSerialDigits = 3;
    private String photoUri;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUniversity() {
        return university;
    }

    public void setUniversity(String university) {
        this.university = university;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public int getYearNo() {
        return yearNo;
    }

    public void setYearNo(int yearNo) {
        this.yearNo = yearNo;
    }

    public int getTermNo() {
        return termNo;
    }

    public void setTermNo(int termNo) {
        this.termNo = termNo;
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

    public String getRollNo() {
        return rollNo;
    }

    public void setRollNo(String rollNo) {
        this.rollNo = rollNo;
    }

    public int getRollSerialDigits() {
        return rollSerialDigits;
    }

    public void setRollSerialDigits(int rollSerialDigits) {
        this.rollSerialDigits = rollSerialDigits;
    }

    public String getPhotoUri() {
        return photoUri;
    }

    public void setPhotoUri(String photoUri) {
        this.photoUri = photoUri;
    }
}