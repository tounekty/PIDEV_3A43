package com.mindcare.model;

/**
 * Conversation model – a chat thread between two users tied to a contract.
 */
public class Conversation {

    private int id;
    private int contractId;
    private String contractTitle;
    private int participantId;
    private String participantName;
    private String lastMessage;
    private String lastMessageTime;
    private int unreadCount;

    public Conversation() {}

    public Conversation(int id, String contractTitle, String participantName, String lastMessage, String lastMessageTime, int unreadCount) {
        this.id = id;
        this.contractTitle = contractTitle;
        this.participantName = participantName;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.unreadCount = unreadCount;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getContractId() { return contractId; }
    public void setContractId(int contractId) { this.contractId = contractId; }

    public String getContractTitle() { return contractTitle; }
    public void setContractTitle(String contractTitle) { this.contractTitle = contractTitle; }

    public int getParticipantId() { return participantId; }
    public void setParticipantId(int participantId) { this.participantId = participantId; }

    public String getParticipantName() { return participantName; }
    public void setParticipantName(String participantName) { this.participantName = participantName; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public String getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(String lastMessageTime) { this.lastMessageTime = lastMessageTime; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
}
