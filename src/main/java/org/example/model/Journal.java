package org.example.model;

import java.time.LocalDate;

public class Journal {
    private final int id;
    private final String title;
    private final String content;
    private final LocalDate entryDate;
    private final Integer moodId;
    private final String adminComment;

    public Journal(int id, String title, String content, LocalDate entryDate, Integer moodId) {
        this(id, title, content, entryDate, moodId, null);
    }

    public Journal(int id, String title, String content, LocalDate entryDate, Integer moodId, String adminComment) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.entryDate = entryDate;
        this.moodId = moodId;
        this.adminComment = adminComment;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public Integer getMoodId() {
        return moodId;
    }

    public String getAdminComment() {
        return adminComment;
    }

    @Override
    public String toString() {
        return "Journal{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", entryDate=" + entryDate +
                ", moodId=" + moodId +
                ", adminComment='" + adminComment + '\'' +
                '}';
    }
}
