package com.example.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

/**
 * Subject model representing an academic course module in GradeXpert ERP & Firebase Firestore.
 * Supports program level classification: "UG" (Undergraduate) or "PG" (Postgraduate).
 */
public class Subject {

    private String subjectId;
    private String subjectCode;
    private String subjectName;
    private String description;
    private String department;
    private String departmentId;
    private String departmentShortName;
    private String programLevel = "UG"; // "UG" or "PG"
    private String semester;
    private int credits;
    private String subjectType; // Theory, Practical, Lab, Project, Elective
    private int weeklyHours;
    private int totalHours;
    private String status; // "ACTIVE" or "INACTIVE"
    private String createdBy;

    @ServerTimestamp
    private Timestamp createdAt;

    @ServerTimestamp
    private Timestamp updatedAt;

    public Subject() {
        this.status = "ACTIVE";
        this.programLevel = "UG";
    }

    public static int parseSemesterNumber(String rawSem) {
        if (rawSem == null || rawSem.trim().isEmpty()) return 1;
        String clean = rawSem.trim().toUpperCase();
        if (clean.contains("VIII") || clean.endsWith("8")) return 8;
        if (clean.contains("VII") || clean.endsWith("7")) return 7;
        if (clean.contains("VI") || clean.endsWith("6")) return 6;
        if (clean.contains("IV") || clean.endsWith("4")) return 4;
        if (clean.contains("V") || clean.endsWith("5")) return 5;
        if (clean.contains("III") || clean.endsWith("3")) return 3;
        if (clean.contains("II") || clean.endsWith("2")) return 2;
        if (clean.contains("I") || clean.endsWith("1")) return 1;

        String digits = clean.replaceAll("[^0-9]", "");
        if (!digits.isEmpty()) {
            try { return Integer.parseInt(digits); } catch (Exception ignored) {}
        }
        return 1;
    }

    public static boolean isSemesterMatching(String semFilter, String subjectSem) {
        if (semFilter == null || "All Semesters".equalsIgnoreCase(semFilter) || "All".equalsIgnoreCase(semFilter)) {
            return true;
        }
        if (subjectSem == null) return false;
        if (semFilter.equalsIgnoreCase(subjectSem)) return true;
        return parseSemesterNumber(semFilter) == parseSemesterNumber(subjectSem);
    }

    public static boolean isDepartmentMatching(String deptFilter, String subjectDept) {
        if (deptFilter == null || "All Departments".equalsIgnoreCase(deptFilter) || "All".equalsIgnoreCase(deptFilter)) {
            return true;
        }
        if (subjectDept == null) return false;
        if (deptFilter.equalsIgnoreCase(subjectDept)) return true;

        String f = deptFilter.toLowerCase();
        String s = subjectDept.toLowerCase();

        if (f.contains("computer application") || f.contains("mca")) {
            return s.contains("computer application") || s.contains("mca");
        }
        if (f.contains("computer science") || f.contains("cse")) {
            return s.contains("computer science") || s.contains("cse");
        }
        if (f.contains("information technology") || f.contains("it")) {
            return s.contains("information technology") || s.contains(" it") || s.equals("it");
        }
        if (f.contains("electronics") || f.contains("ece")) {
            return s.contains("electronics") || s.contains("ece");
        }
        return s.contains(f) || f.contains(s);
    }

    public Subject(String subjectId, String subjectCode, String subjectName, String description,
                   String department, String semester, int credits, String subjectType,
                   int weeklyHours, int totalHours, String status, String createdBy) {
        this.subjectId = subjectId;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.description = description;
        this.department = department;
        this.semester = semester;
        this.credits = credits;
        this.subjectType = subjectType != null ? subjectType : "Theory";
        this.weeklyHours = weeklyHours;
        this.totalHours = totalHours;
        this.status = status != null ? status : "ACTIVE";
        this.createdBy = createdBy;
        this.programLevel = Department.resolveDefaultProgramLevel(department, department);
    }

    public Subject(String subjectId, String subjectCode, String subjectName, String description,
                   String department, String departmentId, String departmentShortName,
                   String programLevel, String semester, int credits, String subjectType,
                   int weeklyHours, int totalHours, String status, String createdBy) {
        this.subjectId = subjectId;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.description = description;
        this.department = department;
        this.departmentId = departmentId;
        this.departmentShortName = departmentShortName;
        this.programLevel = Department.validateProgramLevel(programLevel);
        this.semester = semester;
        this.credits = credits;
        this.subjectType = subjectType != null ? subjectType : "Theory";
        this.weeklyHours = weeklyHours;
        this.totalHours = totalHours;
        this.status = status != null ? status : "ACTIVE";
        this.createdBy = createdBy;
    }

    // Compatibility constructor for legacy code using (int id, String subjectCode, String subjectName, int credits, int semester, String department)
    public Subject(int id, String subjectCode, String subjectName, int credits, int semester, String department) {
        this.subjectId = String.valueOf(id > 0 ? id : System.currentTimeMillis());
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.credits = credits;
        this.semester = "Semester " + (semester > 0 ? semester : 1);
        this.department = department;
        this.subjectType = "Theory";
        this.weeklyHours = 4;
        this.totalHours = 45;
        this.status = "ACTIVE";
        this.programLevel = Department.resolveDefaultProgramLevel(department, department);
    }

    public Subject(int id, String subjectCode, String subjectName, int credits, String semester, String department) {
        this.subjectId = String.valueOf(id > 0 ? id : System.currentTimeMillis());
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.credits = credits;
        this.semester = semester;
        this.department = department;
        this.subjectType = "Theory";
        this.weeklyHours = 4;
        this.totalHours = 45;
        this.status = "ACTIVE";
        this.programLevel = Department.resolveDefaultProgramLevel(department, department);
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    // Legacy int ID getter/setter for compatibility
    public int getId() {
        try {
            return Integer.parseInt(subjectId);
        } catch (Exception e) {
            return (int) (System.currentTimeMillis() % 100000);
        }
    }

    public void setId(int id) {
        this.subjectId = String.valueOf(id);
    }

    public String getSubjectCode() {
        return subjectCode != null ? subjectCode : "";
    }

    public String getCode() {
        return getSubjectCode();
    }

    public void setSubjectCode(String subjectCode) {
        if (subjectCode != null && !subjectCode.trim().isEmpty()) {
            this.subjectCode = subjectCode;
        }
    }

    public void setSubject_code(String subject_code) {
        if (subject_code != null && !subject_code.trim().isEmpty()) {
            this.subjectCode = subject_code;
        }
    }

    public void setCode(String code) {
        if (code != null && !code.trim().isEmpty()) {
            this.subjectCode = code;
        }
    }

    public String getSubjectName() {
        return subjectName != null ? subjectName : "";
    }

    public String getName() {
        return getSubjectName();
    }

    public void setSubjectName(String subjectName) {
        if (subjectName != null && !subjectName.trim().isEmpty()) {
            this.subjectName = subjectName;
        }
    }

    public void setSubject_name(String subject_name) {
        if (subject_name != null && !subject_name.trim().isEmpty()) {
            this.subjectName = subject_name;
        }
    }

    public void setName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            this.subjectName = name;
        }
    }

    public void setTitle(String title) {
        if (title != null && !title.trim().isEmpty()) {
            this.subjectName = title;
        }
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

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public String getSubjectType() {
        return subjectType != null ? subjectType : "Theory";
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    public int getWeeklyHours() {
        return weeklyHours;
    }

    public void setWeeklyHours(int weeklyHours) {
        this.weeklyHours = weeklyHours;
    }

    public int getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(int totalHours) {
        this.totalHours = totalHours;
    }

    public String getStatus() {
        return status != null ? status : "ACTIVE";
    }

    public void setStatus(String status) {
        this.status = status;
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
}
