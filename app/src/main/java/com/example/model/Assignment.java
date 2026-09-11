package com.example.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Model class representing an Assignment stored in Firebase Firestore.
 */
public class Assignment {

    private String assignmentId;
    private String title;
    private String description;
    private String department;
    private String semester;
    private String subjectId;
    private String subjectName;
    private String assignmentType;
    private Timestamp dueDate;
    private String dueTime;
    private double maxMarks;
    private String attachmentUrl;
    private String attachmentName;
    private String createdBy;

    @ServerTimestamp
    private Timestamp createdAt;

    @ServerTimestamp
    private Timestamp updatedAt;

    private String status; // "DRAFT", "PUBLISHED", "CLOSED"

    // Legacy fields for student submission tracking compatibility
    private String submissionDate;
    private String submittedFilePath;

    public String getSubmissionDate() {
        return submissionDate != null ? submissionDate : "";
    }

    public void setSubmissionDate(String submissionDate) {
        this.submissionDate = submissionDate;
    }

    public String getSubmittedFilePath() {
        return submittedFilePath != null ? submittedFilePath : "";
    }

    public void setSubmittedFilePath(String submittedFilePath) {
        this.submittedFilePath = submittedFilePath;
    }

    // No-arg constructor for Firestore
    public Assignment() {}

    public Assignment(String assignmentId, String title, String description, String department,
                      String semester, String subjectId, String subjectName, String assignmentType,
                      Timestamp dueDate, String dueTime, double maxMarks, String attachmentUrl,
                      String attachmentName, String createdBy, String status) {
        this.assignmentId = assignmentId;
        this.title = title;
        this.description = description;
        this.department = department;
        this.semester = semester;
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.assignmentType = assignmentType;
        this.dueDate = dueDate;
        this.dueTime = dueTime;
        this.maxMarks = maxMarks;
        this.attachmentUrl = attachmentUrl;
        this.attachmentName = attachmentName;
        this.createdBy = createdBy;
        this.status = status;
    }

    // Compatibility constructor for legacy code
    public Assignment(int id, String title, int subjectId, String department, String semester, String dueDateStr, String description, String attachmentUrl) {
        this.assignmentId = String.valueOf(id > 0 ? id : System.currentTimeMillis());
        this.title = title;
        this.subjectId = String.valueOf(subjectId);
        this.department = department;
        this.semester = semester;
        this.description = description;
        this.attachmentUrl = attachmentUrl;
        this.status = "PUBLISHED";
        this.maxMarks = 100.0;
        this.assignmentType = "Theory";
    }

    public Assignment(int id, String title, String subjectId, String department, String semester, String dueDateStr, String description, String attachmentUrl) {
        this.assignmentId = String.valueOf(id > 0 ? id : System.currentTimeMillis());
        this.title = title;
        this.subjectId = subjectId;
        this.department = department;
        this.semester = semester;
        this.description = description;
        this.attachmentUrl = attachmentUrl;
        this.status = "PUBLISHED";
        this.maxMarks = 100.0;
        this.assignmentType = "Theory";
    }

    public String getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }

    // Legacy int ID getter/setter for compatibility
    public int getId() {
        try {
            return Integer.parseInt(assignmentId);
        } catch (Exception e) {
            return (int) (System.currentTimeMillis() % 100000);
        }
    }

    public void setId(int id) {
        this.assignmentId = String.valueOf(id);
    }

    // Legacy deadline and filePath getters/setters for FirestoreHelper compatibility
    public String getDeadline() {
        if (dueTime != null && !dueTime.isEmpty()) return dueTime;
        if (dueDate != null) return dueDate.toDate().toString();
        return submissionDate != null ? submissionDate : "";
    }

    public void setDeadline(String deadline) {
        this.dueTime = deadline;
    }

    public String getFilePath() {
        if (attachmentUrl != null && !attachmentUrl.isEmpty()) return attachmentUrl;
        return submittedFilePath != null ? submittedFilePath : "";
    }

    public void setFilePath(String filePath) {
        this.attachmentUrl = filePath;
        this.submittedFilePath = filePath;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getAssignmentType() {
        return assignmentType;
    }

    public void setAssignmentType(String assignmentType) {
        this.assignmentType = assignmentType;
    }

    public Timestamp getDueDate() {
        return dueDate;
    }

    public void setDueDate(Timestamp dueDate) {
        this.dueDate = dueDate;
    }

    public String getDueTime() {
        return dueTime;
    }

    public void setDueTime(String dueTime) {
        this.dueTime = dueTime;
    }

    public double getMaxMarks() {
        return maxMarks;
    }

    public void setMaxMarks(double maxMarks) {
        this.maxMarks = maxMarks;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public String getAttachmentName() {
        return attachmentName;
    }

    public void setAttachmentName(String attachmentName) {
        this.attachmentName = attachmentName;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isDraft() {
        return "DRAFT".equalsIgnoreCase(status);
    }

    public boolean isPublished() {
        return "PUBLISHED".equalsIgnoreCase(status);
    }

    public boolean isClosed() {
        return "CLOSED".equalsIgnoreCase(status);
    }
}
