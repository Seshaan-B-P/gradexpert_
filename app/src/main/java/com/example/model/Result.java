package com.example.model;

/**
 * Model class representing a student's published/draft semester result.
 */
public class Result {
    private int id;
    private String studentId; // Legacy / SQLite numeric ID as String
    private int studentNumericId; // Local SQLite integer student ID
    private String studentUid; // Firebase Auth UID for Firestore authorization
    private String studentLoginId; // Human-readable Login ID (e.g. MCA001)
    private int semester;
    private double totalMarks;
    private double percentage;
    private double sgpa;
    private double cgpa;
    private String status; // "PENDING_APPROVAL", "APPROVED", "REJECTED", "DRAFT", "PUBLISHED"
    private String publishedDate;

    // Optional student metadata fields for display
    private String studentName;
    private String regNo;
    private String registerNo;
    private String department;
    private String departmentName;
    private String departmentId;
    private String programLevel = "UG"; // "UG" or "PG"
    private String academicYear = "2025-2026";
    private String resultId;

    // Workflow & Versioning Fields
    private int version = 1;
    private String publishedBy;
    private String teacherId;
    private String teacherName;
    private String approvalNote;
    private String approvedBy;
    private String rejectedBy;
    private String rejectionReason;

    // Timestamps
    private Object createdAt;
    private Object updatedAt;
    private Object submittedAt;
    private Object approvedAt;
    private Object rejectedAt;

    // Subject breakdown and marks mappings
    private java.util.List<SubjectGradeItem> subjects = new java.util.ArrayList<>();
    private java.util.Map<String, Object> marks = new java.util.HashMap<>();
    private java.util.Map<String, String> grades = new java.util.HashMap<>();

    public Result() {
        this.status = "DRAFT";
        this.version = 1;
        this.programLevel = "UG";
    }

    public Result(int id, int studentNumericId, int semester, double totalMarks, double percentage, double sgpa, double cgpa, String status, String publishedDate) {
        this.id = id;
        this.studentNumericId = studentNumericId;
        this.studentId = String.valueOf(studentNumericId);
        this.semester = semester;
        this.totalMarks = totalMarks;
        this.percentage = percentage;
        this.sgpa = sgpa;
        this.cgpa = cgpa;
        this.status = status;
        this.publishedDate = publishedDate;
        this.version = 1;
        this.programLevel = "UG";
        this.resultId = this.studentId + "_" + semester;
    }

    public Result(int id, String studentId, int semester, double totalMarks, double percentage, double sgpa, double cgpa, String status, String publishedDate) {
        this.id = id;
        this.studentId = studentId;
        try {
            this.studentNumericId = Integer.parseInt(studentId);
        } catch (Exception e) {
            this.studentNumericId = 0;
        }
        this.semester = semester;
        this.totalMarks = totalMarks;
        this.percentage = percentage;
        this.sgpa = sgpa;
        this.cgpa = cgpa;
        this.status = status;
        this.publishedDate = publishedDate;
        this.version = 1;
        this.programLevel = "UG";
        this.resultId = (studentId != null ? studentId : "0") + "_" + semester;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStudentId() {
        if (studentId != null && !studentId.trim().isEmpty()) {
            return studentId;
        }
        return studentNumericId > 0 ? String.valueOf(studentNumericId) : "";
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
        if (this.studentNumericId <= 0 && studentId != null) {
            try {
                this.studentNumericId = Integer.parseInt(studentId);
            } catch (Exception ignored) {}
        }
        if (this.resultId == null || this.resultId.isEmpty()) {
            this.resultId = (studentId != null ? studentId : "0") + "_" + this.semester;
        }
    }

    public void setStudentId(int studentNumericId) {
        this.studentNumericId = studentNumericId;
        this.studentId = String.valueOf(studentNumericId);
        if (this.resultId == null || this.resultId.isEmpty()) {
            this.resultId = this.studentId + "_" + this.semester;
        }
    }

    public int getStudentNumericId() {
        if (studentNumericId > 0) return studentNumericId;
        if (studentId != null) {
            try {
                return Integer.parseInt(studentId);
            } catch (Exception ignored) {}
        }
        return 0;
    }

    public void setStudentNumericId(int studentNumericId) {
        this.studentNumericId = studentNumericId;
        if (this.studentId == null || this.studentId.isEmpty()) {
            this.studentId = String.valueOf(studentNumericId);
        }
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

    public String getStudentLoginId() {
        return studentLoginId != null ? studentLoginId : "";
    }

    public void setStudentLoginId(String studentLoginId) {
        this.studentLoginId = studentLoginId;
    }

    public int getStudentIdAsInt() {
        return getStudentNumericId();
    }

    public String getStudentRegisterNo() {
        return getRegisterNo();
    }

    public void setStudentRegisterNo(String studentRegisterNo) {
        setRegisterNo(studentRegisterNo);
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
        String sId = getStudentId();
        if (!sId.isEmpty() && (this.resultId == null || this.resultId.isEmpty())) {
            this.resultId = sId + "_" + semester;
        }
    }

    public double getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(double totalMarks) {
        this.totalMarks = totalMarks;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
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

    public String getStatus() {
        return status;
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

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getRegNo() {
        return (regNo != null && !regNo.isEmpty()) ? regNo : registerNo;
    }

    public void setRegNo(String regNo) {
        this.regNo = regNo;
        this.registerNo = regNo;
    }

    public String getRegisterNo() {
        return (registerNo != null && !registerNo.isEmpty()) ? registerNo : regNo;
    }

    public void setRegisterNo(String registerNo) {
        this.registerNo = registerNo;
        this.regNo = registerNo;
    }

    public String getDepartment() {
        return (department != null && !department.isEmpty()) ? department : departmentName;
    }

    public void setDepartment(String department) {
        this.department = department;
        this.departmentName = department;
    }

    public String getDepartmentName() {
        return (departmentName != null && !departmentName.isEmpty()) ? departmentName : department;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
        this.department = departmentName;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
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

    public String getAcademicYear() {
        return (academicYear != null && !academicYear.isEmpty()) ? academicYear : "2025-2026";
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public String getResultId() {
        if (resultId != null && !resultId.trim().isEmpty()) {
            return resultId;
        }
        String sId = getStudentId();
        return (!sId.isEmpty() ? sId : "0") + "_" + semester;
    }

    public void setResultId(String resultId) {
        this.resultId = resultId;
    }

    public int getVersion() {
        return version > 0 ? version : 1;
    }

    public void setVersion(int version) {
        this.version = version > 0 ? version : 1;
    }

    public String getPublishedBy() {
        return publishedBy;
    }

    public void setPublishedBy(String publishedBy) {
        this.publishedBy = publishedBy;
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

    public String getApprovalNote() {
        return approvalNote;
    }

    public void setApprovalNote(String approvalNote) {
        this.approvalNote = approvalNote;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getRejectedBy() {
        return rejectedBy;
    }

    public void setRejectedBy(String rejectedBy) {
        this.rejectedBy = rejectedBy;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Object getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Object createdAt) {
        this.createdAt = createdAt;
    }

    public Object getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Object updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Object getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Object submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Object getApprovedAt() {
        return approvedAt;
    }

    public String getApprovedAtString() {
        if (approvedAt == null) return null;
        if (approvedAt instanceof com.google.firebase.Timestamp) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US);
            return sdf.format(((com.google.firebase.Timestamp) approvedAt).toDate());
        }
        return approvedAt.toString();
    }

    public void setApprovedAt(Object approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getSubmittedAtString() {
        if (submittedAt == null) return null;
        if (submittedAt instanceof com.google.firebase.Timestamp) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US);
            return sdf.format(((com.google.firebase.Timestamp) submittedAt).toDate());
        }
        return submittedAt.toString();
    }

    public Object getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(Object rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public java.util.List<SubjectGradeItem> getSubjects() {
        return subjects != null ? subjects : new java.util.ArrayList<>();
    }

    public void setSubjects(java.util.List<SubjectGradeItem> subjects) {
        this.subjects = subjects != null ? subjects : new java.util.ArrayList<>();
    }

    public java.util.Map<String, Object> getMarks() {
        return marks != null ? marks : new java.util.HashMap<>();
    }

    public void setMarks(java.util.Map<String, Object> marks) {
        this.marks = marks != null ? marks : new java.util.HashMap<>();
    }

    public java.util.Map<String, String> getGrades() {
        return grades != null ? grades : new java.util.HashMap<>();
    }

    public void setGrades(java.util.Map<String, String> grades) {
        this.grades = grades != null ? grades : new java.util.HashMap<>();
    }
}
