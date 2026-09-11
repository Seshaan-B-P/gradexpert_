package com.example.model;

public class TeacherAssignment {
    private String assignmentId;
    private String teacherName;
    private String teacherEmail;
    private String subjectCode;
    private String subjectName;
    private String department;
    private String semester;
    private String academicYear;
    private String status;

    public TeacherAssignment() {
        this.status = "ACTIVE";
    }

    public TeacherAssignment(String assignmentId, String teacherName, String teacherEmail, String subjectCode, String subjectName, String department, String semester, String academicYear, String status) {
        this.assignmentId = assignmentId;
        this.teacherName = teacherName;
        this.teacherEmail = teacherEmail;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.department = department;
        this.semester = semester;
        this.academicYear = academicYear;
        this.status = status != null ? status : "ACTIVE";
    }

    public String getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getTeacherName() {
        return teacherName != null ? teacherName : "";
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getTeacherEmail() {
        return teacherEmail != null ? teacherEmail : "";
    }

    public void setTeacherEmail(String teacherEmail) {
        this.teacherEmail = teacherEmail;
    }

    public String getSubjectCode() {
        return subjectCode != null ? subjectCode : "";
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName != null ? subjectName : "";
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getDepartment() {
        return department != null ? department : "";
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getSemester() {
        return semester != null ? semester : "";
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getAcademicYear() {
        return academicYear != null ? academicYear : "2026-27";
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public String getStatus() {
        return status != null ? status : "ACTIVE";
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
