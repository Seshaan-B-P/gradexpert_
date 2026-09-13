package com.example.model;

import java.io.Serializable;

/**
 * Model representing an attendance log entry recorded by a student for class monitoring.
 */
public class StudentAttendanceLog implements Serializable {
    private int id;
    private int studentId;
    private String subjectName;
    private int totalClasses;
    private int attendedClasses;
    private double percentage;
    private String loggedDate;
    private String notes;
    private double threshold;
    private String status; // "NORMAL" or "BELOW_THRESHOLD"

    public StudentAttendanceLog() {}

    public StudentAttendanceLog(int id, int studentId, String subjectName, int totalClasses, int attendedClasses,
                                double percentage, String loggedDate, String notes, double threshold, String status) {
        this.id = id;
        this.studentId = studentId;
        this.subjectName = subjectName;
        this.totalClasses = totalClasses;
        this.attendedClasses = attendedClasses;
        this.percentage = percentage;
        this.loggedDate = loggedDate;
        this.notes = notes;
        this.threshold = threshold;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public int getTotalClasses() {
        return totalClasses;
    }

    public void setTotalClasses(int totalClasses) {
        this.totalClasses = totalClasses;
    }

    public int getAttendedClasses() {
        return attendedClasses;
    }

    public void setAttendedClasses(int attendedClasses) {
        this.attendedClasses = attendedClasses;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public String getLoggedDate() {
        return loggedDate;
    }

    public void setLoggedDate(String loggedDate) {
        this.loggedDate = loggedDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isBelowThreshold() {
        return "BELOW_THRESHOLD".equalsIgnoreCase(status) || percentage < threshold;
    }

    /**
     * Calculates the number of consecutive classes the student must attend to reach the threshold.
     */
    public int getClassesNeededForThreshold() {
        if (percentage >= threshold || threshold >= 100.0) {
            return 0;
        }
        double target = threshold / 100.0;
        double needed = (target * totalClasses - attendedClasses) / (1.0 - target);
        return (int) Math.ceil(Math.max(0.0, needed));
    }

    /**
     * Calculates how many classes the student can afford to miss while staying at or above threshold.
     */
    public int getClassesCanAffordToMiss() {
        if (percentage < threshold || threshold <= 0.0) {
            return 0;
        }
        double target = threshold / 100.0;
        double canMiss = (attendedClasses / target) - totalClasses;
        return (int) Math.floor(Math.max(0.0, canMiss));
    }
}
