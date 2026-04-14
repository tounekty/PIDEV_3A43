package com.mindcare.model;

/**
 * Contract model – created when a client accepts a worker's offer.
 */
public class Contract {

    public enum Status {
        ACTIVE, COMPLETED, DISPUTED, CANCELLED
    }

    private int id;
    private int serviceRequestId;
    private String serviceRequestTitle;
    private int clientId;
    private String clientName;
    private int workerId;
    private String workerName;
    private double amount;
    private String startDate;
    private String endDate;
    private Status status;
    private int progress; // 0-100

    public Contract() {}

    public Contract(int id, String serviceRequestTitle, String clientName, String workerName,
                    double amount, String startDate, String endDate, Status status) {
        this.id = id;
        this.serviceRequestTitle = serviceRequestTitle;
        this.clientName = clientName;
        this.workerName = workerName;
        this.amount = amount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getServiceRequestId() { return serviceRequestId; }
    public void setServiceRequestId(int serviceRequestId) { this.serviceRequestId = serviceRequestId; }

    public String getServiceRequestTitle() { return serviceRequestTitle; }
    public void setServiceRequestTitle(String s) { this.serviceRequestTitle = s; }

    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public int getWorkerId() { return workerId; }
    public void setWorkerId(int workerId) { this.workerId = workerId; }

    public String getWorkerName() { return workerName; }
    public void setWorkerName(String workerName) { this.workerName = workerName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }

    public String getAmountFormatted() { return String.format("$%.0f", amount); }
}
