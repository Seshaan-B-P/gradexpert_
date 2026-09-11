package com.example.model;

/**
 * Model class for student attendance entry on a given subject and date.
 */
public class AttendanceRecord {
    private int id;
    private int studentId;
    private String studentName;
    private String studentRegNo;
    private int subjectId;
    private String date; // YYYY-MM-DD
    private String status; // "PRESENT", "ABSENT"

    public AttendanceRecord(int id, int studentId, String studentName, String studentRegNo, int subjectId, String date, String status) {
        this.id = id;
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentRegNo = studentRegNo;
        this.subjectId = subjectId;
        this.date = date;
        this.status = status;
    }

    public AttendanceRecord(int studentId, String studentName, String studentRegNo, int subjectId, String date, String status) {
        this(0, studentId, studentName, studentRegNo, subjectId, date, status);
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

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentRegNo() {
        return studentRegNo;
    }

    public void setStudentRegNo(String studentRegNo) {
        this.studentRegNo = studentRegNo;
    }

    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isPresent() {
        return "PRESENT".equalsIgnoreCase(status);
    }
}
