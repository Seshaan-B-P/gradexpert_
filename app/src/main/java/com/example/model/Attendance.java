package com.example.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Model representing a single student attendance record per hour/period for Firebase Firestore.
 */
public class Attendance {

    private String attendanceId;
    private String studentId;
    private String studentName;
    private String registerNo;
    private String subjectId;
    private String subjectName;
    private String department;
    private String semester;
    private String date; // YYYY-MM-DD
    private int hour; // 1 to 8
    private String status; // "PRESENT", "ABSENT", "UNMARKED"
    private String markedBy;

    @ServerTimestamp
    private Timestamp createdAt;

    @ServerTimestamp
    private Timestamp updatedAt;

    // No-arg constructor required for Firestore serialization
    public Attendance() {
    }

    public Attendance(String attendanceId, String studentId, String studentName, String registerNo,
                      String subjectId, String subjectName, String department, String semester,
                      String date, int hour, String status, String markedBy) {
        this.attendanceId = attendanceId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.registerNo = registerNo;
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.department = department;
        this.semester = semester;
        this.date = date;
        this.hour = hour;
        this.status = status;
        this.markedBy = markedBy;
    }

    public String getAttendanceId() {
        return attendanceId;
    }

    public void setAttendanceId(String attendanceId) {
        this.attendanceId = attendanceId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getRegisterNo() {
        return registerNo;
    }

    public void setRegisterNo(String registerNo) {
        this.registerNo = registerNo;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMarkedBy() {
        return markedBy;
    }

    public void setMarkedBy(String markedBy) {
        this.markedBy = markedBy;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isPresent() {
        return "PRESENT".equalsIgnoreCase(status);
    }

    public boolean isAbsent() {
        return "ABSENT".equalsIgnoreCase(status);
    }

    public boolean isUnmarked() {
        return status == null || "UNMARKED".equalsIgnoreCase(status) || status.trim().isEmpty();
    }

    /**
     * Generate deterministic document ID: {studentId}_{subjectId}_{date}_{hour}
     */
    public static String generateDocumentId(String studentId, String subjectId, String date, int hour) {
        String cleanStudent = studentId != null ? studentId.trim().replaceAll("[^a-zA-Z0-9_-]", "") : "0";
        String cleanSubject = subjectId != null ? subjectId.trim().replaceAll("[^a-zA-Z0-9_-]", "") : "0";
        String cleanDate = date != null ? date.trim() : "nodate";
        return cleanStudent + "_" + cleanSubject + "_" + cleanDate + "_" + hour;
    }
}
