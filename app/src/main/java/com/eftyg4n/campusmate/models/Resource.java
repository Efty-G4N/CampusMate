package com.eftyg4n.campusmate.models;

import java.io.File;

// One item of the Resource Library: a file, a web link, or an online book.
public class Resource extends BaseModel {

    // ---------- Category (the order must match "resource_category_labels") ----------
    public static final String CATEGORY_NOTES = "NOTES";
    public static final String CATEGORY_TEXTBOOK = "TEXTBOOK";
    public static final String CATEGORY_SLIDES = "SLIDES";
    public static final String CATEGORY_HANDWRITTEN = "HANDWRITTEN";
    public static final String CATEGORY_IMAGE = "IMAGE";
    public static final String CATEGORY_PREVIOUS_QUESTION = "PREVIOUS_QUESTION";
    public static final String CATEGORY_CT_QUESTION = "CT_QUESTION";
    public static final String CATEGORY_LAB_MANUAL = "LAB_MANUAL";
    public static final String CATEGORY_LAB_REPORT = "LAB_REPORT";
    public static final String CATEGORY_ASSIGNMENT = "ASSIGNMENT";
    public static final String CATEGORY_LINK = "LINK";
    public static final String CATEGORY_OTHER = "OTHER";

    public static final String[] CATEGORIES = {
            CATEGORY_NOTES, CATEGORY_TEXTBOOK, CATEGORY_SLIDES, CATEGORY_HANDWRITTEN,
            CATEGORY_IMAGE, CATEGORY_PREVIOUS_QUESTION, CATEGORY_CT_QUESTION,
            CATEGORY_LAB_MANUAL, CATEGORY_LAB_REPORT, CATEGORY_ASSIGNMENT,
            CATEGORY_LINK, CATEGORY_OTHER
    };

    // ---------- Source type ----------
    public static final String SOURCE_FILE = "FILE";               // picked from the phone
    public static final String SOURCE_LINK = "LINK";               // a web address
    public static final String SOURCE_ONLINE_BOOK = "ONLINE_BOOK"; // saved from Open Library

    private String courseId;      // null = general resource
    private String topic;         // free text for now (linked to the syllabus later)
    private String title;
    private String description;
    private String category = CATEGORY_NOTES;
    private String sourceType = SOURCE_FILE;
    private String uri;           // original content URI, or the web link
    private String localPath;     // our own copy inside the app (offline access)
    private String mimeType;
    private long sizeBytes;
    private String author;        // used by online books
    private String coverUrl;      // used by online books
    private String remoteUrl;     // Firebase Storage link (later stage)
    private boolean bookmarked;
    private long lastOpenedAt;    // 0 = never opened

    // Filled only by queries that JOIN the course table.
    private String courseCode;

    public static int categoryIndex(String category) {
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (CATEGORIES[i].equals(category)) {
                return i;
            }
        }
        return CATEGORIES.length - 1;
    }

    public boolean hasLocalFile() {
        return localPath != null && new File(localPath).exists();
    }

    public boolean isImage() {
        return mimeType != null && mimeType.startsWith("image/");
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    public boolean isBookmarked() {
        return bookmarked;
    }

    public void setBookmarked(boolean bookmarked) {
        this.bookmarked = bookmarked;
    }

    public long getLastOpenedAt() {
        return lastOpenedAt;
    }

    public void setLastOpenedAt(long lastOpenedAt) {
        this.lastOpenedAt = lastOpenedAt;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }
}
