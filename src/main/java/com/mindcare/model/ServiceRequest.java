package com.mindcare.model;

/**
 * ServiceRequest model – posted by clients to find workers.
 */
public class ServiceRequest {

    public enum Status {
        OPEN, IN_PROGRESS, COMPLETED, CANCELLED
    }

    private int id;
    private String title;
    private String description;
    private String category;
    private double budget;
    private String deadline;
    private Status status;
    private int clientId;
    private String clientName;
    private String createdAt;
    private int offersCount;

    public ServiceRequest() {}

    public ServiceRequest(int id, String title, String category, double budget, String deadline, Status status, String clientName) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.budget = budget;
        this.deadline = deadline;
        this.status = status;
        this.clientName = clientName;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getBudget() { return budget; }
    public void setBudget(double budget) { this.budget = budget; }

    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public int getOffersCount() { return offersCount; }
    public void setOffersCount(int offersCount) { this.offersCount = offersCount; }

    public String getBudgetFormatted() { return String.format("$%.0f", budget); }
}
