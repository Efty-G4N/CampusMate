package com.eftyg4n.campusmate.models;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

// A class-wide announcement stored in Cloud Firestore (collection "notices").
// Shared by every signed-in student, unlike the private SQLite data.
public class Notice {

    private String id;
    private String courseCode;   // "" = general notice
    private String title;
    private String body;
    private String authorName;
    private String ownerUid;     // who wrote it: only this user may edit or delete it
    private long createdAt;

    public static Notice fromDocument(DocumentSnapshot doc) {
        Notice n = new Notice();
        n.id = doc.getId();
        n.courseCode = doc.getString("courseCode");
        n.title = doc.getString("title");
        n.body = doc.getString("body");
        n.authorName = doc.getString("authorName");
        n.ownerUid = doc.getString("ownerUid");
        Long created = doc.getLong("createdAt"); // null if the field is missing
        n.createdAt = created == null ? 0 : created;
        return n;
    }

    // The field names here must match the Firestore security rules (ownerUid!).
    public static Map<String, Object> toMap(String courseCode, String title, String body,
                                            String authorName, String ownerUid) {
        Map<String, Object> map = new HashMap<>();
        map.put("courseCode", courseCode == null ? "" : courseCode);
        map.put("title", title);
        map.put("body", body);
        map.put("authorName", authorName);
        map.put("ownerUid", ownerUid);
        map.put("createdAt", System.currentTimeMillis());
        return map;
    }

    public String getId() {
        return id;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getOwnerUid() {
        return ownerUid;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
