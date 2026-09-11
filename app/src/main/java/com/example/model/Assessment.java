package com.example.model;

/**
 * Model class representing an academic evaluation/assessment.
 */
public class Assessment {
    private int id;
    private String title;
    private String type; // "Internal Test", "Model Exam", "Practical", "Assignment", "Quiz"
    private int subjectId;
    private int semester;
    private String date;
    private double maxMarks;
    private String status; // "PENDING", "COMPLETED"

    private String subjectName;
    private String subjectCode;

    public Assessment() {
    }

    public Assessment(int id, String title, String type, int subjectId, int semester, String date, double maxMarks, String status) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.subjectId = subjectId;
        this.semester = semester;
        this.date = date;
        this.maxMarks = maxMarks;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getMaxMarks() {
        return maxMarks;
    }

    public void setMaxMarks(double maxMarks) {
        this.maxMarks = maxMarks;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }
}
