package com.example.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

/**
 * Model class for Student Assignment Submissions stored in Firebase Firestore.
 */
public class AssignmentSubmission {

    private String submissionId;
    private String assignmentId;
    private String studentId;
    private String studentName;
    private String registerNo;
    private String answer;
    private String attachmentUrl;

    @ServerTimestamp
    private Timestamp submittedAt;

    private double marks;
    private String feedback;
    private String status; // "SUBMITTED", "LATE", "GRADED"

    // No-arg constructor for Firestore
    public AssignmentSubmission() {}

    public AssignmentSubmission(String submissionId, String assignmentId, String studentId,
                                String studentName, String registerNo, String answer,
                                String attachmentUrl, String status) {
        this.submissionId = submissionId;
        this.assignmentId = assignmentId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.registerNo = registerNo;
        this.answer = answer;
        this.attachmentUrl = attachmentUrl;
        this.status = status;
        this.marks = 0;
        this.feedback = "";
    }

    // Compatibility constructor for legacy code
    public AssignmentSubmission(int id, int assignmentId, int studentId, String studentName, String registerNo,
                                String submissionDate, String filePath, String status, String remarks) {
        this.submissionId = String.valueOf(id > 0 ? id : System.currentTimeMillis());
        this.assignmentId = String.valueOf(assignmentId);
        this.studentId = String.valueOf(studentId);
        this.studentName = studentName;
        this.registerNo = registerNo;
        this.attachmentUrl = filePath;
        this.status = status != null ? status : "SUBMITTED";
        this.feedback = remarks;
        this.answer = submissionDate;
    }

    public AssignmentSubmission(int id, String assignmentId, String studentId, String studentName, String registerNo,
                                String submissionDate, String filePath, String status, String remarks) {
        this.submissionId = String.valueOf(id > 0 ? id : System.currentTimeMillis());
        this.assignmentId = assignmentId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.registerNo = registerNo;
        this.attachmentUrl = filePath;
        this.status = status != null ? status : "SUBMITTED";
        this.feedback = remarks;
        this.answer = submissionDate;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(String submissionId) {
        this.submissionId = submissionId;
    }

    // Legacy int ID getter/setter for compatibility
    public int getId() {
        try {
            return Integer.parseInt(submissionId);
        } catch (Exception e) {
            return (int) (System.currentTimeMillis() % 100000);
        }
    }

    public void setId(int id) {
        this.submissionId = String.valueOf(id);
    }

    public String getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
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

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public Timestamp getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Timestamp submittedAt) {
        this.submittedAt = submittedAt;
    }

    public double getMarks() {
        return marks;
    }

    public void setMarks(double marks) {
        this.marks = marks;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
