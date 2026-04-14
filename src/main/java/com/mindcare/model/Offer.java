package com.mindcare.model;

/**
 * Offer model – submitted by workers in response to service requests.
 */
public class Offer {

    public enum Status {
        PENDING, ACCEPTED, REJECTED, WITHDRAWN
    }

    private int id;
    private int serviceRequestId;
    private String serviceRequestTitle;
    private int workerId;
    private String workerName;
    private double price;
    private String coverLetter;
    private String deliveryTime;
    private Status status;
    private String createdAt;

    public Offer() {}

    public Offer(int id, String serviceRequestTitle, String workerName, double price, String deliveryTime, Status status) {
        this.id = id;
        this.serviceRequestTitle = serviceRequestTitle;
        this.workerName = workerName;
        this.price = price;
        this.deliveryTime = deliveryTime;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getServiceRequestId() { return serviceRequestId; }
    public void setServiceRequestId(int serviceRequestId) { this.serviceRequestId = serviceRequestId; }

    public String getServiceRequestTitle() { return serviceRequestTitle; }
    public void setServiceRequestTitle(String serviceRequestTitle) { this.serviceRequestTitle = serviceRequestTitle; }

    public int getWorkerId() { return workerId; }
    public void setWorkerId(int workerId) { this.workerId = workerId; }

    public String getWorkerName() { return workerName; }
    public void setWorkerName(String workerName) { this.workerName = workerName; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getCoverLetter() { return coverLetter; }
    public void setCoverLetter(String coverLetter) { this.coverLetter = coverLetter; }

    public String getDeliveryTime() { return deliveryTime; }
    public void setDeliveryTime(String deliveryTime) { this.deliveryTime = deliveryTime; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getPriceFormatted() { return String.format("$%.0f", price); }
}
