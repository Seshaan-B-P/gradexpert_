package com.example.model;

/**
 * User model representing authentication identity and session properties.
 */
public class User {
    private String id;
    private String uid;
    private String name;
    private String email;
    private String password;
    private String role; // "ADMIN", "TEACHER", or "STUDENT"
    private String identifier; // RegNo for student, Employee ID for teacher
    private String loginId;
    private String status = "ACTIVE"; // "ACTIVE", "INACTIVE", "SUSPENDED"
    private String department = "";
    private String semester = "";

    public User() {
    }

    public User(int id, String name, String email, String password, String role, String identifier) {
        this.id = String.valueOf(id);
        this.uid = String.valueOf(id);
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.identifier = identifier;
        this.status = "ACTIVE";
    }

    public User(int id, String name, String email, String password, String role, String identifier, String status) {
        this.id = String.valueOf(id);
        this.uid = String.valueOf(id);
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.identifier = identifier;
        this.status = status != null ? status : "ACTIVE";
    }

    public User(String uid, String name, String email, String password, String role, String identifier, String status) {
        this.id = uid;
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.identifier = identifier;
        this.status = status != null ? status : "ACTIVE";
    }

    public int getId() {
        if (id != null) {
            try {
                return Integer.parseInt(id);
            } catch (Exception e) {
                return Math.abs(id.hashCode());
            }
        }
        if (uid != null) {
            try {
                return Integer.parseInt(uid);
            } catch (Exception e) {
                return Math.abs(uid.hashCode());
            }
        }
        return 0;
    }

    public void setId(int id) {
        this.id = String.valueOf(id);
    }

    public void setId(String id) {
        this.id = id;
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

    public String getRole() {
        return role != null ? role : "STUDENT";
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getIdentifier() {
        return identifier != null ? identifier : "";
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getStatus() {
        return status != null ? status : "ACTIVE";
    }

    public void setStatus(String status) {
        this.status = status;
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
}

