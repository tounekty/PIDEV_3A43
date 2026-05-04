package com.mindcare.model;

/**
 * Message model – private messages between contract parties.
 */
public class Message {

    private int id;
    private int senderId;
    private String senderName;
    private int receiverId;
    private int contractId;
    private String content;
    private String sentAt;
    private boolean read;
    private boolean sentByMe; // computed, not stored

    public Message() {}

    public Message(int id, int senderId, String senderName, String content, String sentAt, boolean sentByMe) {
        this.id = id;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.sentAt = sentAt;
        this.sentByMe = sentByMe;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public int getReceiverId() { return receiverId; }
    public void setReceiverId(int receiverId) { this.receiverId = receiverId; }

    public int getContractId() { return contractId; }
    public void setContractId(int contractId) { this.contractId = contractId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSentAt() { return sentAt; }
    public void setSentAt(String sentAt) { this.sentAt = sentAt; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public boolean isSentByMe() { return sentByMe; }
    public void setSentByMe(boolean sentByMe) { this.sentByMe = sentByMe; }
}
