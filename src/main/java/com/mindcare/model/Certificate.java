package com.mindcare.model;

/**
 * Certificate model – professional certificates uploaded by workers.
 */
public class Certificate {

    public enum Status {
        PENDING, APPROVED, REJECTED
    }

    private int id;
    private int workerId;
    private String workerName;
    private String name;
    private String issuer;
    private String issuedDate;
    private String expiryDate;
    private String filePath;
    private Status status;
    private String adminComment;
    private String aiAnalysis;
    private String uploadedAt;

    public Certificate() {}

    public Certificate(int id, String workerName, String name, String issuer, String issuedDate, Status status) {
        this.id = id;
        this.workerName = workerName;
        this.name = name;
        this.issuer = issuer;
        this.issuedDate = issuedDate;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getWorkerId() { return workerId; }
    public void setWorkerId(int workerId) { this.workerId = workerId; }

    public String getWorkerName() { return workerName; }
    public void setWorkerName(String workerName) { this.workerName = workerName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public String getIssuedDate() { return issuedDate; }
    public void setIssuedDate(String issuedDate) { this.issuedDate = issuedDate; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getAdminComment() { return adminComment; }
    public void setAdminComment(String adminComment) { this.adminComment = adminComment; }

    public String getAiAnalysis() { return aiAnalysis; }
    public void setAiAnalysis(String aiAnalysis) { this.aiAnalysis = aiAnalysis; }

    public String getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(String uploadedAt) { this.uploadedAt = uploadedAt; }
}
