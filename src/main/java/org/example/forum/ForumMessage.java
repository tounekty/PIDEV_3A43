package org.example.forum;

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
    }

    public ForumMessage(int id, String contenu, LocalDateTime dateMessage, boolean anonymous, String attachmentPath,
                        String attachmentMimeType, Long attachmentSize, int idSujet, int idUser, String username) {
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
}
