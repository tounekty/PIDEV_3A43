package org.example.model;

import java.time.LocalDateTime;

public class ForumMessage {
    private int id;
    private String contenu;
    private LocalDateTime dateMessage;
    private boolean anonymous;
    private String attachmentPath;
    private String attachmentMimeType;
    private Long attachmentSize;
    private int idSujet;
    private int idUser;
    private String username;
    private Integer parentMessageId;
    private int threadLevel = 1;
    private int likeCount;
    private int dislikeCount;
    private Boolean userReactionLike;

    public ForumMessage() {
    }

    public ForumMessage(String contenu, LocalDateTime dateMessage, boolean anonymous, String attachmentPath,
                        String attachmentMimeType, Long attachmentSize, int idSujet, int idUser) {
        this.contenu = contenu;
        this.dateMessage = dateMessage;
        this.anonymous = anonymous;
        this.attachmentPath = attachmentPath;
        this.attachmentMimeType = attachmentMimeType;
        this.attachmentSize = attachmentSize;
        this.idSujet = idSujet;
        this.idUser = idUser;
        this.threadLevel = 1;
    }

    public ForumMessage(int id, String contenu, LocalDateTime dateMessage, boolean anonymous, String attachmentPath,
                        String attachmentMimeType, Long attachmentSize, int idSujet, int idUser, String username) {
        this(id, contenu, dateMessage, anonymous, attachmentPath, attachmentMimeType, attachmentSize, idSujet, idUser, username, null);
    }

    public ForumMessage(int id, String contenu, LocalDateTime dateMessage, boolean anonymous, String attachmentPath,
                        String attachmentMimeType, Long attachmentSize, int idSujet, int idUser, String username,
                        Integer parentMessageId) {
        this(id, contenu, dateMessage, anonymous, attachmentPath, attachmentMimeType, attachmentSize,
            idSujet, idUser, username, parentMessageId, 0, 0, null);
        }

        public ForumMessage(int id, String contenu, LocalDateTime dateMessage, boolean anonymous, String attachmentPath,
                String attachmentMimeType, Long attachmentSize, int idSujet, int idUser, String username,
                Integer parentMessageId, int likeCount, int dislikeCount, Boolean userReactionLike) {
        this.id = id;
        this.contenu = contenu;
        this.dateMessage = dateMessage;
        this.anonymous = anonymous;
        this.attachmentPath = attachmentPath;
        this.attachmentMimeType = attachmentMimeType;
        this.attachmentSize = attachmentSize;
        this.idSujet = idSujet;
        this.idUser = idUser;
        this.username = username;
        this.parentMessageId = parentMessageId;
        this.threadLevel = 1;
        this.likeCount = likeCount;
        this.dislikeCount = dislikeCount;
        this.userReactionLike = userReactionLike;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public LocalDateTime getDateMessage() {
        return dateMessage;
    }

    public void setDateMessage(LocalDateTime dateMessage) {
        this.dateMessage = dateMessage;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
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

    public int getIdSujet() {
        return idSujet;
    }

    public void setIdSujet(int idSujet) {
        this.idSujet = idSujet;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getParentMessageId() {
        return parentMessageId;
    }

    public void setParentMessageId(Integer parentMessageId) {
        this.parentMessageId = parentMessageId;
    }

    public int getThreadLevel() {
        return threadLevel;
    }

    public void setThreadLevel(int threadLevel) {
        this.threadLevel = threadLevel;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getDislikeCount() {
        return dislikeCount;
    }

    public void setDislikeCount(int dislikeCount) {
        this.dislikeCount = dislikeCount;
    }

    public Boolean getUserReactionLike() {
        return userReactionLike;
    }

    public void setUserReactionLike(Boolean userReactionLike) {
        this.userReactionLike = userReactionLike;
    }
}
