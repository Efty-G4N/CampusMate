package com.eftyg4n.campusmate.models;

// One reminder of one deadline: "notify me X minutes before the due time".
// A deadline can have many reminders (for example 1 day before AND 1 hour before).
public class Reminder extends BaseModel {

    private String deadlineId;
    private int minutesBefore;   // 0 = at the due time

    public String getDeadlineId() {
        return deadlineId;
    }

    public void setDeadlineId(String deadlineId) {
        this.deadlineId = deadlineId;
    }

    public int getMinutesBefore() {
        return minutesBefore;
    }

    public void setMinutesBefore(int minutesBefore) {
        this.minutesBefore = minutesBefore;
    }
}
