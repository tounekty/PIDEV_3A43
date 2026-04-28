package com.mindcare.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Appointment {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private int id;
    private LocalDateTime dateTime;
    private String location;
    private String description;
    private String status;
    private Integer studentId;
    private String studentName;
    private Integer psyId;
    private String psyName;
    private Integer patientFileId;
    private String reportName;
    private LocalDateTime reportUpdatedAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public Integer getPsyId() {
        return psyId;
    }

    public void setPsyId(Integer psyId) {
        this.psyId = psyId;
    }

    public String getPsyName() {
        return psyName;
    }

    public void setPsyName(String psyName) {
        this.psyName = psyName;
    }

    public Integer getPatientFileId() {
        return patientFileId;
    }

    public void setPatientFileId(Integer patientFileId) {
        this.patientFileId = patientFileId;
    }

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public LocalDateTime getReportUpdatedAt() {
        return reportUpdatedAt;
    }

    public void setReportUpdatedAt(LocalDateTime reportUpdatedAt) {
        this.reportUpdatedAt = reportUpdatedAt;
    }

    public String getDateTimeDisplay() {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DISPLAY_FORMAT);
    }

    public String getDossierReference() {
        if (patientFileId != null && patientFileId > 0) {
            return "DOSSIER-" + patientFileId;
        }
        if (studentId != null && studentId > 0) {
            return "DOSSIER-ETUDIANT-" + studentId;
        }
        return "N/A";
    }
}
