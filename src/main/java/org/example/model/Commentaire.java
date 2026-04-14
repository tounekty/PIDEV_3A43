package org.example.model;

import java.time.LocalDateTime;

public class Commentaire {
    private int id;
    private int resourceId;
    private int userId;
    private String authorName;
    private String authorEmail;
    private String content;
    private int rating;         // 1-5
    private LocalDateTime createdAt;
    private boolean approved;
    
    // Constructeurs
    public Commentaire() {
    }
    
    public Commentaire(int resourceId, String authorName, String authorEmail, 
                       String content, int rating, int userId) {
        this.resourceId = resourceId;
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.content = content;
        this.rating = rating;
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
        this.approved = false;
    }
    
    public Commentaire(int id, int resourceId, int userId, String authorName, String authorEmail,
                       String content, int rating, LocalDateTime createdAt, boolean approved) {
        this.id = id;
        this.resourceId = resourceId;
        this.userId = userId;
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.content = content;
        this.rating = rating;
        this.createdAt = createdAt;
        this.approved = approved;
    }
    
    // Getters & Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getResourceId() {
        return resourceId;
    }
    
    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }
    
    public int getUserId() {
        return userId;
    }
    
    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    public String getAuthorName() {
        return authorName;
    }
    
    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }
    
    public String getAuthorEmail() {
        return authorEmail;
    }
    
    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public int getRating() {
        return rating;
    }
    
    public void setRating(int rating) {
        this.rating = rating;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public boolean isApproved() {
        return approved;
    }
    
    public void setApproved(boolean approved) {
        this.approved = approved;
    }
    
    @Override
    public String toString() {
        return "Commentaire{" +
                "id=" + id +
                ", authorName='" + authorName + '\'' +
                ", rating=" + rating +
                ", approved=" + approved +
                '}';
    }
}
