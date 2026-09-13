package com.example.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

/**
 * Student model representing a student profile in GradeXpert ERP & Firebase Firestore.
 * Supports program level classification: "UG" (Undergraduate) or "PG" (Postgraduate).
 */
public class Student {

    private int numericId = 0;
    private String studentId;
    private String name;
    private String registerNo;
    private String email;
    private String phone;
    private String department;
    private String departmentId;
    private String departmentShortName;
    private String firebaseUid;
    private String loginId;
    private String programLevel = "UG"; // "UG" or "PG"
    private String semester;
    private String section;
    private String gender;
    private String dateOfBirth;
    private String profileImageUrl;
    private String status; // "ACTIVE" or "INACTIVE"
    private int presentHours = 0;
    private int totalHours = 0;

    @ServerTimestamp
    private Timestamp createdAt;

    @ServerTimestamp
    private Timestamp updatedAt;

    public Student() {
        this.status = "ACTIVE";
        this.programLevel = "UG";
    }

    public Student(String studentId, String name, String registerNo, String email, String phone,
                   String department, String semester, String section, String gender,
                   String dateOfBirth, String profileImageUrl, String status) {
        this.studentId = studentId;
        this.name = name;
        this.registerNo = registerNo;
        this.email = email;
        this.phone = phone;
        this.department = department;
        this.semester = semester;
        this.section = section;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.profileImageUrl = profileImageUrl;
        this.status = status != null ? status : "ACTIVE";
        this.programLevel = Department.resolveDefaultProgramLevel(department, department);
    }

    public Student(String studentId, String name, String registerNo, String email, String phone,
                   String department, String departmentId, String departmentShortName,
                   String programLevel, String semester, String section, String gender,
                   String dateOfBirth, String profileImageUrl, String status) {
        this.studentId = studentId;
        this.name = name;
        this.registerNo = registerNo;
        this.email = email;
        this.phone = phone;
        this.department = department;
        this.departmentId = departmentId;
        this.departmentShortName = departmentShortName;
        this.programLevel = Department.validateProgramLevel(programLevel);
        this.semester = semester;
        this.section = section;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.profileImageUrl = profileImageUrl;
        this.status = status != null ? status : "ACTIVE";
    }

    // Compatibility constructor for legacy code using (int id, String name, String regNo, ...)
    public Student(int id, String name, String regNo, String department, int semester, String email, String phone, String photoUri) {
        this.numericId = id;
        this.studentId = id > 0 ? String.valueOf(id) : "";
        this.name = name;
        this.registerNo = regNo;
        this.department = department;
        this.semester = "Semester " + (semester > 0 ? semester : 1);
        this.email = email;
        this.phone = phone;
        this.profileImageUrl = photoUri;
        this.status = "ACTIVE";
        this.programLevel = Department.resolveDefaultProgramLevel(department, department);
    }

    public Student(int id, String name, String regNo, String department, String semester, String email, String phone, String photoUri) {
        this.numericId = id;
        this.studentId = id > 0 ? String.valueOf(id) : "";
        this.name = name;
        this.registerNo = regNo;
        this.department = department;
        this.semester = semester;
        this.email = email;
        this.phone = phone;
        this.profileImageUrl = photoUri;
        this.status = "ACTIVE";
        this.programLevel = Department.resolveDefaultProgramLevel(department, department);
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
        if (studentId != null && studentId.matches("\\d+")) {
            try {
                this.numericId = Integer.parseInt(studentId);
            } catch (Exception ignored) {}
        }
    }

    // Legacy int ID getter/setter for compatibility
    public int getId() {
        if (this.numericId > 0) {
            return this.numericId;
        }
        if (studentId != null && !studentId.isEmpty()) {
            try {
                return Integer.parseInt(studentId);
            } catch (Exception ignored) {}
        }
        return this.numericId;
    }

    public void setId(int id) {
        this.numericId = id;
        if (this.studentId == null || this.studentId.isEmpty() || this.studentId.matches("\\d+")) {
            this.studentId = String.valueOf(id);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegisterNo() {
        return registerNo;
    }

    public void setRegisterNo(String registerNo) {
        this.registerNo = registerNo;
    }

    // Legacy regNo getter/setter
    public String getRegNo() {
        return registerNo != null ? registerNo : "";
    }

    public void setRegNo(String regNo) {
        this.registerNo = regNo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getUid() {
        return getFirebaseUid();
    }

    public void setUid(String uid) {
        setFirebaseUid(uid);
    }

    public String getFirebaseUid() {
        if (firebaseUid != null && !firebaseUid.trim().isEmpty()) {
            return firebaseUid.trim();
        }
        if (studentId != null && !studentId.matches("\\d+")) {
            return studentId.trim();
        }
        return firebaseUid != null ? firebaseUid : "";
    }

    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
        if (firebaseUid != null && !firebaseUid.trim().isEmpty() && this.numericId <= 0 && (this.studentId == null || this.studentId.isEmpty() || this.studentId.matches("\\d+"))) {
            this.studentId = firebaseUid.trim();
        }
    }

    public String getLoginId() {
        return loginId != null ? loginId : "";
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDepartmentName() {
        return department != null ? department : "";
    }

    public void setDepartmentName(String departmentName) {
        if (departmentName != null && !departmentName.isEmpty()) {
            this.department = departmentName;
        }
    }

    public String getDepartmentId() {
        return departmentId;
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
        if (programLevel == null || programLevel.isEmpty()) {
            return Department.resolveDefaultProgramLevel(departmentShortName, department);
        }
        return Department.validateProgramLevel(programLevel);
    }

    public void setProgramLevel(String programLevel) {
        this.programLevel = Department.validateProgramLevel(programLevel);
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getPhotoUri() {
        return profileImageUrl;
    }

    public void setPhotoUri(String photoUri) {
        this.profileImageUrl = photoUri;
    }

    public String getStatus() {
        return status != null ? status : "ACTIVE";
    }

    public void setStatus(String status) {
        this.status = status;
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

    public int getPresentHours() {
        return presentHours;
    }

    public void setPresentHours(int presentHours) {
        this.presentHours = presentHours;
    }

    public int getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(int totalHours) {
        this.totalHours = totalHours;
    }

    public double getAttendancePercentage() {
        if (totalHours <= 0) return 85.0;
        return (presentHours * 100.0) / totalHours;
    }
}
