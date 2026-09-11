package com.example.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

/**
 * Model class representing a Broadcast Alert document in Firebase Firestore.
 */
public class BroadcastAlert {

    private String alertId;
    private String title;
    private String message;
    private String type; // General, Exam, Assignment, Attendance, Result, Event, Important Notice, Emergency
    private String priority; // Normal, High, Urgent
    private String targetType; // ALL_STUDENTS, DEPARTMENT, DEPARTMENT_SEMESTER, SPECIFIC_STUDENT
    private String department;
    private String semester;
    private String studentId;
    private String studentName;
    private String createdBy;

    @ServerTimestamp
    private Timestamp createdAt;

    private Timestamp scheduledAt;
    private Timestamp publishedAt;

    private String status; // DRAFT, SCHEDULED, PUBLISHED, CANCELLED
    private int recipientCount;

    public BroadcastAlert() {
        this.type = "General Announcement";
        this.priority = "Normal";
        this.targetType = "ALL_STUDENTS";
        this.status = "PUBLISHED";
    }

    public BroadcastAlert(String alertId, String title, String message, String type, String priority,
                          String targetType, String department, String semester, String studentId,
                          String studentName, String createdBy, String status, int recipientCount) {
        this.alertId = alertId;
        this.title = title;
        this.message = message;
        this.type = type != null ? type : "General Announcement";
        this.priority = priority != null ? priority : "Normal";
        this.targetType = targetType != null ? targetType : "ALL_STUDENTS";
        this.department = department;
        this.semester = semester;
        this.studentId = studentId;
        this.studentName = studentName;
        this.createdBy = createdBy;
        this.status = status != null ? status : "PUBLISHED";
        this.recipientCount = recipientCount;
    }

    public String getAlertId() {
        return alertId;
    }

    public void setAlertId(String alertId) {
        this.alertId = alertId;
    }

    public String getTitle() {
        return title != null ? title : "";
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message != null ? message : "";
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type != null ? type : "General Announcement";
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPriority() {
        return priority != null ? priority : "Normal";
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getTargetType() {
        return targetType != null ? targetType : "ALL_STUDENTS";
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Timestamp scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public Timestamp getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Timestamp publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getStatus() {
        return status != null ? status : "PUBLISHED";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getRecipientCount() {
        return recipientCount;
    }

    public void setRecipientCount(int recipientCount) {
        this.recipientCount = recipientCount;
    }
}
