package com.example.model;

public class Semester {
    private String semesterId;
    private String semesterTitle; // e.g. "Semester I", "Semester II", "Semester III", "Semester IV"
    private int semesterNumber; // 1, 2, 3, 4
    private String status; // "ACTIVE", "INACTIVE"

    public Semester() {
        this.status = "ACTIVE";
    }

    public Semester(String semesterId, String semesterTitle, int semesterNumber, String status) {
        this.semesterId = semesterId;
        this.semesterTitle = semesterTitle;
        this.semesterNumber = semesterNumber;
        this.status = status != null ? status : "ACTIVE";
    }

    public String getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(String semesterId) {
        this.semesterId = semesterId;
    }

    public String getSemesterTitle() {
        return semesterTitle != null ? semesterTitle : "";
    }

    public void setSemesterTitle(String semesterTitle) {
        this.semesterTitle = semesterTitle;
    }

    public int getSemesterNumber() {
        return semesterNumber;
    }

    public void setSemesterNumber(int semesterNumber) {
        this.semesterNumber = semesterNumber;
    }

    public String getStatus() {
        return status != null ? status : "ACTIVE";
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
