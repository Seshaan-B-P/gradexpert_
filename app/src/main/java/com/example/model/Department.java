package com.example.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

/**
 * Department model representing an academic department / degree program in GradeXpert.
 * Supports program level classification: "UG" (Undergraduate) or "PG" (Postgraduate).
 */
public class Department {
    private String departmentId;
    private String departmentName;
    private String departmentCode;
    private String name; // Alias for Firestore compatibility
    private String shortName; // Alias for Firestore compatibility
    private String programLevel = "UG"; // "UG" or "PG"
    private String status = "ACTIVE"; // "ACTIVE" or "INACTIVE"

    @ServerTimestamp
    private Timestamp createdAt;

    public Department() {
        this.status = "ACTIVE";
        this.programLevel = "UG";
    }

    public Department(String departmentId, String departmentName, String departmentCode, String status) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.departmentCode = departmentCode;
        this.name = departmentName;
        this.shortName = departmentCode;
        this.status = status != null ? status : "ACTIVE";
        this.programLevel = resolveDefaultProgramLevel(departmentCode, departmentName);
    }

    public Department(String departmentId, String departmentName, String departmentCode, String programLevel, String status) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.departmentCode = departmentCode;
        this.name = departmentName;
        this.shortName = departmentCode;
        this.programLevel = validateProgramLevel(programLevel);
        this.status = status != null ? status : "ACTIVE";
    }

    public static String validateProgramLevel(String level) {
        if (level == null) return "UG";
        String clean = level.trim().toUpperCase();
        if ("PG".equals(clean) || "POSTGRADUATE".equals(clean)) return "PG";
        return "UG";
    }

    public static String resolveDefaultProgramLevel(String code, String name) {
        String combined = ((code != null ? code : "") + " " + (name != null ? name : "")).toUpperCase();
        if (combined.contains("MCA") || combined.contains("M.TECH") || combined.contains("MTECH") ||
                combined.contains("M.SC") || combined.contains("MSC") || combined.contains("MBA") ||
                combined.contains("MASTER")) {
            return "PG";
        }
        return "UG";
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        if (departmentName != null && !departmentName.isEmpty()) return departmentName;
        return name != null ? name : "";
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
        this.name = departmentName;
    }

    public String getName() {
        return getDepartmentName();
    }

    public void setName(String name) {
        this.name = name;
        if (this.departmentName == null || this.departmentName.isEmpty()) {
            this.departmentName = name;
        }
    }

    public String getDepartmentCode() {
        if (departmentCode != null && !departmentCode.isEmpty()) return departmentCode;
        return shortName != null ? shortName : "";
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
        this.shortName = departmentCode;
    }

    public String getShortName() {
        return getDepartmentCode();
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
        if (this.departmentCode == null || this.departmentCode.isEmpty()) {
            this.departmentCode = shortName;
        }
    }

    public String getProgramLevel() {
        return programLevel != null ? programLevel.toUpperCase() : "UG";
    }

    public void setProgramLevel(String programLevel) {
        this.programLevel = validateProgramLevel(programLevel);
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

    public String getDisplayName() {
        String code = getDepartmentCode();
        String name = getDepartmentName();
        String level = getProgramLevel();
        if (!code.isEmpty()) {
            return "[" + level + "] " + code + " - " + name;
        }
        return "[" + level + "] " + name;
    }
}
