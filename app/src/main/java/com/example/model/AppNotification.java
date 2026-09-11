package com.example.model;

/**
 * Model class for Notifications & Announcements in the ERP system.
 */
public class AppNotification {
    private int id;
    private String title;
    private String message;
    private String date;
    private String targetRole; // ALL, TEACHER, STUDENT
    private String category;   // EXAM, ASSIGNMENT, URGENT, GENERAL, CIRCULAR
    private String sender;     // e.g. "Dr. Alan Turing"
    private boolean isRead;

    public AppNotification(int id, String title, String message, String date, String targetRole) {
        this(id, title, message, date, targetRole, "GENERAL", "Faculty Admin", false);
    }

    public AppNotification(int id, String title, String message, String date, String targetRole, String category, String sender, boolean isRead) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.date = date;
        this.targetRole = targetRole != null ? targetRole : "ALL";
        this.category = category != null ? category : "GENERAL";
        this.sender = sender != null ? sender : "Faculty Admin";
        this.isRead = isRead;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getDate() {
        return date;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}

