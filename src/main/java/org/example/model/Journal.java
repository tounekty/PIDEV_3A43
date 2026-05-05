package org.example.model;

import java.time.LocalDate;

public class Journal {
    private int id;
    private String title;
    private String content;
    private LocalDate entryDate;
    private Integer moodId;
    private String adminComment;
    private Integer userId;

    public Journal() {}

    public Journal(String title, String content, LocalDate entryDate, Integer moodId, Integer userId) {
        this.title = title;
        this.content = content;
        this.entryDate = entryDate;
        this.moodId = moodId;
        this.userId = userId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public Integer getMoodId() {
        return moodId;
    }

    public void setMoodId(Integer moodId) {
        this.moodId = moodId;
    }

    public String getAdminComment() {
        return adminComment;
    }

    public void setAdminComment(String adminComment) {
        this.adminComment = adminComment;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
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
                ", userId=" + userId +
                '}';
    }
}
