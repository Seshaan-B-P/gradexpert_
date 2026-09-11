package com.example.model;

/**
 * Model class for Monthly attendance chart statistics.
 */
public class MonthlyAttendanceStats {
    private String monthKey; // e.g., "2026-03" or "March"
    private String monthLabel; // e.g. "Mar", "Apr"
    private int totalClasses;
    private int attendedClasses;

    public MonthlyAttendanceStats(String monthKey, String monthLabel, int totalClasses, int attendedClasses) {
        this.monthKey = monthKey;
        this.monthLabel = monthLabel;
        this.totalClasses = totalClasses;
        this.attendedClasses = attendedClasses;
    }

    public String getMonthKey() {
        return monthKey;
    }

    public String getMonthLabel() {
        return monthLabel;
    }

    public int getTotalClasses() {
        return totalClasses;
    }

    public int getAttendedClasses() {
        return attendedClasses;
    }

    public float getPercentage() {
        if (totalClasses == 0) return 0f;
        return (float) ((double) attendedClasses / totalClasses * 100);
    }
}
