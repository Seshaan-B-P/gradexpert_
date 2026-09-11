package com.example.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

/**
 * Model representing a Teacher in GradeXpert.
 * Supports both local SQLite caching and Firebase Firestore sync.
 * Includes programLevel ("UG" / "PG") classification.
 * NEVER stores passwords in Firestore.
 */
public class Teacher {
    private String id;
    private String uid;
    private String teacherId;
    private String name;
    private String email;
    private String password; // Used only for legacy SQLite compatibility; NEVER stored in Firestore
    private String department;
    private String departmentId;
    private String departmentShortName;
    private String programLevel = "UG"; // "UG" or "PG"
    private String phone;
    private String employeeId;
    private String loginId;
    private String designation;
    private String qualification;
    private String dateOfJoining;
    private String status = "ACTIVE"; // "ACTIVE", "INACTIVE", "SUSPENDED"

    private java.util.List<String> assignedSubjectIds = new java.util.ArrayList<>();
    private java.util.List<String> assignedSubjectNames = new java.util.ArrayList<>();

    @ServerTimestamp
    private Timestamp createdAt;

    public Teacher() {
    }

    public Teacher(int id, String name, String email, String password, String department, String phone) {
        this.id = String.valueOf(id);
        this.uid = String.valueOf(id);
        this.teacherId = String.valueOf(id);
        this.name = name;
        this.email = email;
        this.password = password;
        this.department = department;
        this.phone = phone;
        this.status = "ACTIVE";
        this.programLevel = Department.resolveDefaultProgramLevel(department, department);
    }

    public Teacher(String uid, String name, String email, String phone, String employeeId,
                   String department, String departmentId, String designation,
                   String qualification, String dateOfJoining, String status) {
        this.id = uid;
        this.uid = uid;
        this.teacherId = uid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.employeeId = employeeId;
        this.department = department;
        this.departmentId = departmentId;
        this.designation = designation;
        this.qualification = qualification;
        this.dateOfJoining = dateOfJoining;
        this.status = status != null ? status.toUpperCase() : "ACTIVE";
        this.programLevel = Department.resolveDefaultProgramLevel(departmentId, department);
    }

    public Teacher(String uid, String name, String email, String phone, String employeeId,
                   String department, String departmentId, String departmentShortName,
                   String programLevel, String designation, String qualification,
                   String dateOfJoining, String status) {
        this.id = uid;
        this.uid = uid;
        this.teacherId = uid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.employeeId = employeeId;
        this.department = department;
        this.departmentId = departmentId;
        this.departmentShortName = departmentShortName;
        this.programLevel = Department.validateProgramLevel(programLevel);
        this.designation = designation;
        this.qualification = qualification;
        this.dateOfJoining = dateOfJoining;
        this.status = status != null ? status.toUpperCase() : "ACTIVE";
    }

    public int getId() {
        if (id != null) {
            try { return Integer.parseInt(id); } catch (Exception e) { return Math.abs(id.hashCode()); }
        }
        if (uid != null) {
            try { return Integer.parseInt(uid); } catch (Exception e) { return Math.abs(uid.hashCode()); }
        }
        return 0;
    }

    public void setId(int id) {
        this.id = String.valueOf(id);
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTeacherId() {
        return teacherId != null ? teacherId : getUid();
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public String getUid() {
        return uid != null ? uid : (id != null ? id : "");
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getFirebaseUid() {
        return getUid();
    }

    public void setFirebaseUid(String firebaseUid) {
        setUid(firebaseUid);
    }

    public String getLoginId() {
        return loginId != null ? loginId : "";
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email != null ? email : "";
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password != null ? password : "";
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDepartment() {
        return department != null ? department : "";
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDepartmentId() {
        return departmentId != null ? departmentId : "";
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentShortName() {
        return departmentShortName != null ? departmentShortName : "";
    }

    public void setDepartmentShortName(String departmentShortName) {
        this.departmentShortName = departmentShortName;
    }

    public String getProgramLevel() {
        return programLevel != null ? programLevel : "UG";
    }

    public void setProgramLevel(String programLevel) {
        this.programLevel = Department.validateProgramLevel(programLevel);
    }

    public String getPhone() {
        return phone != null ? phone : "";
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmployeeId() {
        return employeeId != null ? employeeId : "";
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getDesignation() {
        return designation != null ? designation : "Faculty";
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getQualification() {
        return qualification != null ? qualification : "";
    }

    public void setQualification(String qualification) {
        this.qualification = qualification;
    }

    public String getDateOfJoining() {
        return dateOfJoining != null ? dateOfJoining : "";
    }

    public void setDateOfJoining(String dateOfJoining) {
        this.dateOfJoining = dateOfJoining;
    }

    public String getStatus() {
        return status != null ? status.toUpperCase() : "ACTIVE";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public java.util.List<String> getAssignedSubjectIds() {
        if (assignedSubjectIds == null) {
            assignedSubjectIds = new java.util.ArrayList<>();
        }
        return assignedSubjectIds;
    }

    public void setAssignedSubjectIds(java.util.List<String> assignedSubjectIds) {
        if (assignedSubjectIds != null) {
            this.assignedSubjectIds = new java.util.ArrayList<>(assignedSubjectIds);
        } else {
            this.assignedSubjectIds = new java.util.ArrayList<>();
        }
    }

    public java.util.List<String> getAssignedSubjectNames() {
        if (assignedSubjectNames == null) {
            assignedSubjectNames = new java.util.ArrayList<>();
        }
        return assignedSubjectNames;
    }

    public void setAssignedSubjectNames(java.util.List<String> assignedSubjectNames) {
        if (assignedSubjectNames != null) {
            this.assignedSubjectNames = new java.util.ArrayList<>(assignedSubjectNames);
        } else {
            this.assignedSubjectNames = new java.util.ArrayList<>();
        }
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
