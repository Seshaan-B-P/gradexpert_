package com.example.model;

/**
 * Summary item model representing an aggregated attendance session record for history views.
 */
public class AttendanceHistoryItem {

    private String date;
    private String subjectName;
    private int hour;
    private int presentCount;
    private int absentCount;
    private int totalStudents;
    private double percentage;

    public AttendanceHistoryItem(String date, String subjectName, int hour, int presentCount, int absentCount, int totalStudents) {
        this.date = date;
        this.subjectName = subjectName;
        this.hour = hour;
        this.presentCount = presentCount;
        this.absentCount = absentCount;
        this.totalStudents = totalStudents;
        this.percentage = totalStudents > 0 ? ((double) presentCount / totalStudents) * 100.0 : 0.0;
    }

    public String getDate() {
        return date;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public int getHour() {
        return hour;
    }

    public int getPresentCount() {
        return presentCount;
    }

    public int getAbsentCount() {
        return absentCount;
    }

    public int getTotalStudents() {
        return totalStudents;
    }

    public double getPercentage() {
        return percentage;
    }
}
