package com.example.model;

import java.io.Serializable;

/**
 * Model representing student academic performance metrics for a specific semester in a trend analysis.
 */
public class SemesterPerformanceTrend implements Serializable {

    private int semester;
    private String semesterLabel;
    private double sgpa;
    private double cgpa;
    private double percentage;
    private double totalMarks;
    private int totalCredits;
    private String status;
    private String publishedDate;
    private boolean isEstimated;

    public SemesterPerformanceTrend() {}

    public SemesterPerformanceTrend(int semester, String semesterLabel, double sgpa, double cgpa, double percentage, double totalMarks, int totalCredits, String status) {
        this.semester = semester;
        this.semesterLabel = semesterLabel;
        this.sgpa = sgpa;
        this.cgpa = cgpa;
        this.percentage = percentage;
        this.totalMarks = totalMarks;
        this.totalCredits = totalCredits;
        this.status = status;
        this.isEstimated = false;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }

    public String getSemesterLabel() {
        return (semesterLabel != null && !semesterLabel.isEmpty()) ? semesterLabel : ("Sem " + semester);
    }

    public void setSemesterLabel(String semesterLabel) {
        this.semesterLabel = semesterLabel;
    }

    public double getSgpa() {
        return sgpa;
    }

    public void setSgpa(double sgpa) {
        this.sgpa = sgpa;
    }

    public double getCgpa() {
        return cgpa;
    }

    public void setCgpa(double cgpa) {
        this.cgpa = cgpa;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public double getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(double totalMarks) {
        this.totalMarks = totalMarks;
    }

    public int getTotalCredits() {
        return totalCredits;
    }

    public void setTotalCredits(int totalCredits) {
        this.totalCredits = totalCredits;
    }

    public String getStatus() {
        return status != null ? status : (sgpa >= 8.5 ? "Distinction" : (sgpa >= 7.0 ? "First Class" : "Pass"));
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(String publishedDate) {
        this.publishedDate = publishedDate;
    }

    public boolean isEstimated() {
        return isEstimated;
    }

    public void setEstimated(boolean estimated) {
        isEstimated = estimated;
    }
}
