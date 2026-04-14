package org.example.model;

import java.time.LocalDateTime;

public class ForumSubject {
    private int id;
    private String titre;
    private String description;
    private LocalDateTime dateCreation;
    private String imageUrl;
    private boolean pinned;
    private boolean anonymous;
    private String status;
    private String category;
    private String attachmentPath;
    private String attachmentMimeType;
    private Long attachmentSize;
    private Integer idUser;
    private String username;

    public ForumSubject() {
    }

    public ForumSubject(String titre, String description, LocalDateTime dateCreation, String imageUrl, boolean pinned,
                        boolean anonymous, String status, String category, String attachmentPath,
                        String attachmentMimeType, Long attachmentSize, Integer idUser) {
        this.titre = titre;
        this.description = description;
        this.dateCreation = dateCreation;
        this.imageUrl = imageUrl;
        this.pinned = pinned;
        this.anonymous = anonymous;
        this.status = status;
        this.category = category;
        this.attachmentPath = attachmentPath;
        this.attachmentMimeType = attachmentMimeType;
        this.attachmentSize = attachmentSize;
        this.idUser = idUser;
    }

    public ForumSubject(int id, String titre, String description, LocalDateTime dateCreation, String imageUrl,
                        boolean pinned, boolean anonymous, String status, String category, String attachmentPath,
                        String attachmentMimeType, Long attachmentSize, Integer idUser, String username) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.dateCreation = dateCreation;
        this.imageUrl = imageUrl;
        this.pinned = pinned;
        this.anonymous = anonymous;
        this.status = status;
        this.category = category;
        this.attachmentPath = attachmentPath;
        this.attachmentMimeType = attachmentMimeType;
        this.attachmentSize = attachmentSize;
        this.idUser = idUser;
        this.username = username;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAttachmentPath() {
        return attachmentPath;
    }

    public void setAttachmentPath(String attachmentPath) {
        this.attachmentPath = attachmentPath;
    }

    public String getAttachmentMimeType() {
        return attachmentMimeType;
    }

    public void setAttachmentMimeType(String attachmentMimeType) {
        this.attachmentMimeType = attachmentMimeType;
    }

    public Long getAttachmentSize() {
        return attachmentSize;
    }

    public void setAttachmentSize(Long attachmentSize) {
        this.attachmentSize = attachmentSize;
    }

    public Integer getIdUser() {
        return idUser;
    }

    public void setIdUser(Integer idUser) {
        this.idUser = idUser;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
