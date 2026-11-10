package com.eftyg4n.campusmate.models;

// One search result from the Open Library API (not stored in the database).
public class Book {

    private final String title;
    private final String authors;     // "A, B" or empty
    private final int firstYear;      // 0 = unknown
    private final String coverUrl;    // null = no cover
    private final String webUrl;      // page of the book on openlibrary.org

    public Book(String title, String authors, int firstYear, String coverUrl, String webUrl) {
        this.title = title;
        this.authors = authors;
        this.firstYear = firstYear;
        this.coverUrl = coverUrl;
        this.webUrl = webUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthors() {
        return authors;
    }

    public int getFirstYear() {
        return firstYear;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public String getWebUrl() {
        return webUrl;
    }
}
