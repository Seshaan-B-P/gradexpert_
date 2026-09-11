package com.example.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Model class representing a teacher's result update / revision request for an already approved semester result.
 * Maintained in Firestore collection: resultUpdateRequests/{requestId}
 */
public class ResultUpdateRequest {

    private String requestId;
    private String resultId;
    private String studentId;
    private String studentName;
    private String registerNo;
    private String programLevel = "UG"; // "UG" or "PG"
    private String departmentId;
    private String departmentName;
    private int semester;
    private String academicYear = "2025-2026";
    private int studentNumericId = 0;
    private String studentUid;

    private int oldVersion = 1;
    private int newVersion = 2;

    private double oldMarks = 0.0;
    private double newMarks = 0.0;
    private double oldPercentage = 0.0;
    private double newPercentage = 0.0;
    private double oldSgpa = 0.0;
    private double newSgpa = 0.0;
    private double oldCgpa = 0.0;
    private double newCgpa = 0.0;

    private String oldGrades = "";
    private String newGrades = "";

    private List<SubjectGradeItem> oldSubjects = new ArrayList<>();
    private List<SubjectGradeItem> newSubjects = new ArrayList<>();

    private String reasonForUpdate;
    private String teacherId;
    private String teacherName;
    private String updatedBy;

    private String status = "UPDATE_PENDING_APPROVAL"; // "UPDATE_PENDING_APPROVAL", "APPROVED", "UPDATE_REJECTED"

    private Object submittedAt;
    private Object updatedAt;
    private Object approvedAt;
    private String approvedBy;
    private Object rejectedAt;
    private String rejectedBy;
    private String rejectionReason;

    public ResultUpdateRequest() {
        this.status = "UPDATE_PENDING_APPROVAL";
        this.programLevel = "UG";
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getResultId() {
        return resultId;
    }

    public void setResultId(String resultId) {
        this.resultId = resultId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public int getStudentNumericId() {
        return studentNumericId;
    }

    public void setStudentNumericId(int studentNumericId) {
        this.studentNumericId = studentNumericId;
    }

    public String getStudentUid() {
        if (studentUid != null && !studentUid.trim().isEmpty()) {
            return studentUid.trim();
        }
        if (studentId != null && !studentId.matches("\\d+")) {
            return studentId.trim();
        }
        return studentUid != null ? studentUid : "";
    }

    public void setStudentUid(String studentUid) {
        this.studentUid = studentUid;
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

    public String getProgramLevel() {
        if (programLevel == null || programLevel.trim().isEmpty()) {
            return "UG";
        }
        return "PG".equalsIgnoreCase(programLevel) ? "PG" : "UG";
    }

    public void setProgramLevel(String programLevel) {
        this.programLevel = (programLevel != null && "PG".equalsIgnoreCase(programLevel)) ? "PG" : "UG";
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }

    public String getAcademicYear() {
        return academicYear != null ? academicYear : "2025-2026";
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public int getOldVersion() {
        return oldVersion > 0 ? oldVersion : 1;
    }

    public void setOldVersion(int oldVersion) {
        this.oldVersion = oldVersion > 0 ? oldVersion : 1;
    }

    public int getNewVersion() {
        return newVersion > oldVersion ? newVersion : (oldVersion + 1);
    }

    public void setNewVersion(int newVersion) {
        this.newVersion = newVersion;
    }

    public double getOldMarks() {
        return oldMarks;
    }

    public void setOldMarks(double oldMarks) {
        this.oldMarks = oldMarks;
    }

    public double getNewMarks() {
        return newMarks;
    }

    public void setNewMarks(double newMarks) {
        this.newMarks = newMarks;
    }

    public double getOldPercentage() {
        return oldPercentage;
    }

    public void setOldPercentage(double oldPercentage) {
        this.oldPercentage = oldPercentage;
    }

    public double getNewPercentage() {
        return newPercentage;
    }

    public void setNewPercentage(double newPercentage) {
        this.newPercentage = newPercentage;
    }

    public double getOldSgpa() {
        return oldSgpa;
    }

    public void setOldSgpa(double oldSgpa) {
        this.oldSgpa = oldSgpa;
    }

    public double getNewSgpa() {
        return newSgpa;
    }

    public void setNewSgpa(double newSgpa) {
        this.newSgpa = newSgpa;
    }

    public double getOldCgpa() {
        return oldCgpa;
    }

    public void setOldCgpa(double oldCgpa) {
        this.oldCgpa = oldCgpa;
    }

    public double getNewCgpa() {
        return newCgpa;
    }

    public void setNewCgpa(double newCgpa) {
        this.newCgpa = newCgpa;
    }

    public String getOldGrades() {
        return oldGrades;
    }

    public void setOldGrades(String oldGrades) {
        this.oldGrades = oldGrades;
    }

    public String getNewGrades() {
        return newGrades;
    }

    public void setNewGrades(String newGrades) {
        this.newGrades = newGrades;
    }

    public List<SubjectGradeItem> getOldSubjects() {
        return oldSubjects != null ? oldSubjects : new ArrayList<>();
    }

    public void setOldSubjects(List<SubjectGradeItem> oldSubjects) {
        this.oldSubjects = oldSubjects != null ? oldSubjects : new ArrayList<>();
    }

    public List<SubjectGradeItem> getNewSubjects() {
        return newSubjects != null ? newSubjects : new ArrayList<>();
    }

    public void setNewSubjects(List<SubjectGradeItem> newSubjects) {
        this.newSubjects = newSubjects != null ? newSubjects : new ArrayList<>();
    }

    public String getReasonForUpdate() {
        return reasonForUpdate != null ? reasonForUpdate : "";
    }

    public void setReasonForUpdate(String reasonForUpdate) {
        this.reasonForUpdate = reasonForUpdate;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getStatus() {
        return status != null ? status : "UPDATE_PENDING_APPROVAL";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Object getSubmittedAt() {
        return submittedAt;
    }

    public String getSubmittedAtString() {
        if (submittedAt == null) return null;
        if (submittedAt instanceof com.google.firebase.Timestamp) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US);
            return sdf.format(((com.google.firebase.Timestamp) submittedAt).toDate());
        }
        return submittedAt.toString();
    }

    public void setSubmittedAt(Object submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Object getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Object updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Object getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Object approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Object getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(Object rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public String getRejectedBy() {
        return rejectedBy;
    }

    public void setRejectedBy(String rejectedBy) {
        this.rejectedBy = rejectedBy;
    }

    public String getRejectionReason() {
        return rejectionReason != null ? rejectionReason : "";
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
