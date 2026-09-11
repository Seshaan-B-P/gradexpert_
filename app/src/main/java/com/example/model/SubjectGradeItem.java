package com.example.model;

/**
 * Model class for Grade Calculator & Published Results subject entries.
 */
public class SubjectGradeItem {
    private String subjectCode;
    private String subjectName;
    private int credits = 4;
    private double internalMarks = 0.0;
    private double externalMarks = 0.0;
    private double internal1 = 0.0;
    private double assignment = 0.0;
    private double modelExam = 0.0;
    private double universityExam = 0.0;
    private int maxInternal = 30;
    private int maxExternal = 70;
    private double totalMarks = 0.0;
    private double percentage = 0.0;
    private String grade = "F";
    private int gradePoint = 0;
    private int creditPoints = 0;

    public SubjectGradeItem() {
    }

    public SubjectGradeItem(String subjectName, int credits, double internalMarks, double externalMarks) {
        this.subjectName = subjectName;
        this.credits = credits > 0 ? credits : 4;
        this.internalMarks = internalMarks;
        this.externalMarks = externalMarks;
        this.internal1 = internalMarks;
        this.universityExam = externalMarks;
        this.maxInternal = 30;
        this.maxExternal = 70;
        recalculate();
    }

    public SubjectGradeItem(String subjectName, int credits, int marksObtained) {
        this.subjectName = subjectName;
        this.credits = credits > 0 ? credits : 4;
        this.maxInternal = 30;
        this.maxExternal = 70;
        this.internal1 = Math.round(marksObtained * 0.30 * 10.0) / 10.0;
        this.internalMarks = this.internal1;
        this.universityExam = Math.round(marksObtained * 0.70 * 10.0) / 10.0;
        this.externalMarks = this.universityExam;
        recalculate();
    }

    public SubjectGradeItem(String subjectCode, String subjectName, int credits, double internalMarks, double externalMarks) {
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.credits = credits > 0 ? credits : 4;
        this.internalMarks = internalMarks;
        this.externalMarks = externalMarks;
        this.internal1 = internalMarks;
        this.universityExam = externalMarks;
        this.maxInternal = 30;
        this.maxExternal = 70;
        recalculate();
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public int getCredits() {
        return credits > 0 ? credits : 4;
    }

    public void setCredits(int credits) {
        this.credits = credits > 0 ? credits : 4;
        calculatePoints();
    }

    public double getInternalMarks() {
        return internalMarks;
    }

    public void setInternalMarks(double internalMarks) {
        this.internalMarks = internalMarks;
        this.internal1 = internalMarks;
        recalculate();
    }

    public double getExternalMarks() {
        return externalMarks;
    }

    public void setExternalMarks(double externalMarks) {
        this.externalMarks = externalMarks;
        this.universityExam = externalMarks;
        recalculate();
    }

    public double getInternal1() {
        return internal1;
    }

    public void setInternal1(double internal1) {
        this.internal1 = internal1;
        this.internalMarks = internal1;
    }

    public double getAssignment() {
        return assignment;
    }

    public void setAssignment(double assignment) {
        this.assignment = assignment;
    }

    public double getModelExam() {
        return modelExam;
    }

    public void setModelExam(double modelExam) {
        this.modelExam = modelExam;
    }

    public double getUniversityExam() {
        return universityExam;
    }

    public void setUniversityExam(double universityExam) {
        this.universityExam = universityExam;
        this.externalMarks = universityExam;
    }

    public int getMaxInternal() {
        return maxInternal;
    }

    public void setMaxInternal(int maxInternal) {
        this.maxInternal = maxInternal;
        recalculate();
    }

    public int getMaxExternal() {
        return maxExternal;
    }

    public void setMaxExternal(int maxExternal) {
        this.maxExternal = maxExternal;
        recalculate();
    }

    public double getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(double totalMarks) {
        this.totalMarks = totalMarks;
    }

    public int getMarksObtained() {
        return (int) Math.round(totalMarks);
    }

    public void setMarksObtained(int marksObtained) {
        this.internal1 = Math.round(marksObtained * 0.30 * 10.0) / 10.0;
        this.internalMarks = this.internal1;
        this.universityExam = Math.round(marksObtained * 0.70 * 10.0) / 10.0;
        this.externalMarks = this.universityExam;
        recalculate();
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public int getGradePoint() {
        return gradePoint;
    }

    public void setGradePoint(int gradePoint) {
        this.gradePoint = gradePoint;
    }

    public int getCreditPoints() {
        return creditPoints;
    }

    public void recalculate() {
        double compTotal = this.internal1 + this.assignment + this.universityExam;
        if (compTotal > 0) {
            this.totalMarks = compTotal;
        } else if (this.internalMarks > 0 || this.externalMarks > 0) {
            this.totalMarks = this.internalMarks + this.externalMarks;
        }
        
        int maxTotal = this.maxInternal + this.maxExternal;
        if (maxTotal <= 0) maxTotal = 100;
        this.percentage = (this.totalMarks / (double) maxTotal) * 100.0;
        calculateGradeFromPercentage();
    }

    private void calculateGradeFromPercentage() {
        if (percentage >= 90.0) {
            grade = "A+";
            gradePoint = 10;
        } else if (percentage >= 80.0) {
            grade = "A";
            gradePoint = 9;
        } else if (percentage >= 70.0) {
            grade = "B+";
            gradePoint = 8;
        } else if (percentage >= 60.0) {
            grade = "B";
            gradePoint = 7;
        } else if (percentage >= 50.0) {
            grade = "C";
            gradePoint = 6;
        } else if (percentage >= 40.0) {
            grade = "D";
            gradePoint = 5;
        } else {
            grade = "F";
            gradePoint = 0;
        }
        calculatePoints();
    }

    private void calculatePoints() {
        this.creditPoints = this.getCredits() * this.gradePoint;
    }
}
