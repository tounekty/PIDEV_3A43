package org.example.model;

import java.time.LocalDateTime;

public class Resource {
    public static final String TYPE_ARTICLE = "article";
    public static final String TYPE_VIDEO = "video";
    
    private int id;
    private String title;
    private String description;
    private String type;
    private String filePath;
    private String videoUrl;
    private String imageUrl;
    private LocalDateTime createdAt;
    private int userId;
    
    // Constructeurs
    public Resource() {
    }
    
    public Resource(String title, String description, String type, int userId) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
    }
    
    public Resource(int id, String title, String description, String type, String filePath, 
                    String videoUrl, String imageUrl, LocalDateTime createdAt, int userId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.type = type;
        this.filePath = filePath;
        this.videoUrl = videoUrl;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
        this.userId = userId;
    }
    
    // Getters & Setters
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public String getVideoUrl() {
        return videoUrl;
    }
    
    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public int getUserId() {
        return userId;
    }
    
    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    @Override
    public String toString() {
        return "Resource{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
