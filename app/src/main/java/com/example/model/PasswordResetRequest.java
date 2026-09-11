package com.example.model;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

/**
 * Model representing a password reset request submitted by a Student or Teacher to the System Admin.
 * 
 * NEVER stores passwords or plain credentials.
 */
public class PasswordResetRequest {

    private String requestId;
    private String userId;
    private String userName;
    private String email;
    private String role; // "STUDENT" or "TEACHER"
    private String identifier; // RegNo for student, Employee ID for teacher
    private String department;
    private String semester;
    private String status = "PENDING"; // "PENDING", "APPROVED", "REJECTED", "COMPLETED"
    
    @ServerTimestamp
    private Date requestedAt;
    private Date processedAt;
    private String processedBy;
    private String adminNote;

    public PasswordResetRequest() {
    }

    public PasswordResetRequest(String requestId, String userId, String userName, String email,
                                String role, String identifier, String department, String semester) {
        this.requestId = requestId;
        this.userId = userId;
        this.userName = userName;
        this.email = email;
        this.role = role != null ? role.toUpperCase() : "STUDENT";
        this.identifier = identifier;
        this.department = department != null ? department : "";
        this.semester = semester != null ? semester : "";
        this.status = "PENDING";
        this.requestedAt = new Date();
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Date requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Date getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Date processedAt) {
        this.processedAt = processedAt;
    }

    public String getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public void setAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }

    public String getRegisterNo() {
        return "STUDENT".equalsIgnoreCase(role) ? identifier : "";
    }

    public void setRegisterNo(String registerNo) {
        if (registerNo != null && !registerNo.isEmpty()) {
            this.identifier = registerNo;
        }
    }

    public String getEmployeeId() {
        return "TEACHER".equalsIgnoreCase(role) ? identifier : "";
    }

    public void setEmployeeId(String employeeId) {
        if (employeeId != null && !employeeId.isEmpty()) {
            this.identifier = employeeId;
        }
    }
}
