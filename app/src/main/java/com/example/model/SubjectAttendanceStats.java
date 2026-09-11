package com.example.model;

/**
 * Model class for Subject-wise attendance breakdown statistics.
 */
public class SubjectAttendanceStats {
    private int subjectId;
    private String subjectCode;
    private String subjectName;
    private int totalClasses;
    private int attendedClasses;

    public SubjectAttendanceStats(int subjectId, String subjectCode, String subjectName, int totalClasses, int attendedClasses) {
        this.subjectId = subjectId;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.totalClasses = totalClasses;
        this.attendedClasses = attendedClasses;
    }

    public SubjectAttendanceStats(int subjectId, String subjectName, int totalClasses, int attendedClasses, double percentage) {
        this.subjectId = subjectId;
        this.subjectCode = "";
        this.subjectName = subjectName;
        this.totalClasses = totalClasses;
        this.attendedClasses = attendedClasses;
    }

    public SubjectAttendanceStats(int subjectId, String subjectName, int totalClasses, int attendedClasses) {
        this.subjectId = subjectId;
        this.subjectCode = "";
        this.subjectName = subjectName;
        this.totalClasses = totalClasses;
        this.attendedClasses = attendedClasses;
    }

    public int getSubjectId() {
        return subjectId;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public int getTotalClasses() {
        return totalClasses;
    }

    public int getAttendedClasses() {
        return attendedClasses;
    }

    public int getAbsentClasses() {
        return totalClasses - attendedClasses;
    }

    public int getPercentage() {
        if (totalClasses == 0) return 0;
        return (int) Math.round(((double) attendedClasses / totalClasses) * 100);
    }

    public String getStatusCategory() {
        int pct = getPercentage();
        if (pct >= 85) return "Excellent";
        if (pct >= 75) return "Good";
        if (pct >= 65) return "Warning";
        return "Critical";
    }
}
