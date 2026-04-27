package org.example.model;

import java.time.LocalDate;

public class Mood {
    private final int id;
    private final String moodType;
    private final LocalDate moodDate;
    private final String note;
    private final Integer stressLevel;
    private final Integer energyLevel;
    private final String sleepTime;
    private final String wakeTime;
    private final Double sleepHours;
    private final String adminComment;
    private final boolean supportEmailSent;
    private final String studentName;
    private final String studentEmail;
    private final Integer userId;

    public Mood(int id, String moodType, LocalDate moodDate, String note) {
        this(id, moodType, moodDate, note, null, null, null, null, null, null);
    }

    public Mood(int id, String moodType, LocalDate moodDate, String note, Integer stressLevel, Integer energyLevel) {
        this(id, moodType, moodDate, note, stressLevel, energyLevel, null, null, null, null);
    }

    public Mood(int id, String moodType, LocalDate moodDate, String note, Double sleepHours) {
        this(id, moodType, moodDate, note, null, null, null, null, sleepHours, null);
    }

    public Mood(int id, String moodType, LocalDate moodDate, String note, String sleepTime, String wakeTime, Double sleepHours) {
        this(id, moodType, moodDate, note, null, null, sleepTime, wakeTime, sleepHours, null);
    }

    public Mood(int id, String moodType, LocalDate moodDate, String note, String adminComment) {
        this(id, moodType, moodDate, note, null, null, null, null, null, adminComment);
    }

    public Mood(int id, String moodType, LocalDate moodDate, String note, Double sleepHours, String adminComment) {
        this(id, moodType, moodDate, note, null, null, null, null, sleepHours, adminComment);
    }

    public Mood(int id, String moodType, LocalDate moodDate, String note, String sleepTime, String wakeTime, Double sleepHours, String adminComment) {
        this(id, moodType, moodDate, note, null, null, sleepTime, wakeTime, sleepHours, adminComment);
    }

    public Mood(int id, String moodType, LocalDate moodDate, String note, Integer stressLevel, Integer energyLevel,
                String sleepTime, String wakeTime, Double sleepHours, String adminComment) {
        this(id, moodType, moodDate, note, stressLevel, energyLevel, sleepTime, wakeTime, sleepHours, adminComment, false);
    }

    public Mood(int id, String moodType, LocalDate moodDate, String note, Integer stressLevel, Integer energyLevel,
                String sleepTime, String wakeTime, Double sleepHours, String adminComment, boolean supportEmailSent) {
        this(id, moodType, moodDate, note, stressLevel, energyLevel, sleepTime, wakeTime, sleepHours, adminComment,
                supportEmailSent, null, null);
    }

    public Mood(int id, String moodType, LocalDate moodDate, String note, Integer stressLevel, Integer energyLevel,
                String sleepTime, String wakeTime, Double sleepHours, String adminComment, boolean supportEmailSent,
                String studentName, String studentEmail) {
        this(id, moodType, moodDate, note, stressLevel, energyLevel, sleepTime, wakeTime, sleepHours, adminComment,
                supportEmailSent, studentName, studentEmail, null);
    }

    public Mood(int id, String moodType, LocalDate moodDate, String note, Integer stressLevel, Integer energyLevel,
                String sleepTime, String wakeTime, Double sleepHours, String adminComment, boolean supportEmailSent,
                String studentName, String studentEmail, Integer userId) {
        this.id = id;
        this.moodType = moodType;
        this.moodDate = moodDate;
        this.note = note;
        this.stressLevel = stressLevel;
        this.energyLevel = energyLevel;
        this.sleepTime = sleepTime;
        this.wakeTime = wakeTime;
        this.sleepHours = sleepHours;
        this.adminComment = adminComment;
        this.supportEmailSent = supportEmailSent;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.userId = userId;
    }

    public int getId() {
        return id;
    }

    public String getMoodType() {
        return moodType;
    }

    public LocalDate getMoodDate() {
        return moodDate;
    }

    public String getNote() {
        return note;
    }

    public Integer getStressLevel() {
        return stressLevel;
    }

    public Integer getEnergyLevel() {
        return energyLevel;
    }

    public String getSleepTime() {
        return sleepTime;
    }

    public String getWakeTime() {
        return wakeTime;
    }

    public Double getSleepHours() {
        return sleepHours;
    }

    public String getAdminComment() {
        return adminComment;
    }

    public boolean isSupportEmailSent() {
        return supportEmailSent;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public Integer getUserId() {
        return userId;
    }

    @Override
    public String toString() {
        return "Mood{" +
                "id=" + id +
                ", moodType='" + moodType + '\'' +
                ", moodDate=" + moodDate +
                ", note='" + note + '\'' +
                ", stressLevel=" + stressLevel +
                ", energyLevel=" + energyLevel +
                ", adminComment='" + adminComment + '\'' +
                ", supportEmailSent=" + supportEmailSent +
                ", studentName='" + studentName + '\'' +
                ", studentEmail='" + studentEmail + '\'' +
                ", userId=" + userId +
                '}';
    }
}
