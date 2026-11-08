package com.eftyg4n.campusmate.models;

public class ClassGroup extends BaseModel {

    public static final String TYPE_SECTION = "SECTION";
    public static final String TYPE_LAB_GROUP = "LAB_GROUP";

    private String semesterId;
    private String parentId;
    private String name;
    private String type = TYPE_SECTION;
    private int rollFrom;
    private int rollTo;

    public String getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(String semesterId) {
        this.semesterId = semesterId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getRollFrom() {
        return rollFrom;
    }

    public void setRollFrom(int rollFrom) {
        this.rollFrom = rollFrom;
    }

    public int getRollTo() {
        return rollTo;
    }

    public void setRollTo(int rollTo) {
        this.rollTo = rollTo;
    }
}