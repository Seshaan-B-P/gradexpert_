package com.example.model;

public class AcademicYear {
    private String yearId;
    private String yearTitle; // e.g. "2026-27"
    private boolean isCurrent;
    private String status; // "ACTIVE", "INACTIVE"

    public AcademicYear() {
        this.status = "ACTIVE";
    }

    public AcademicYear(String yearId, String yearTitle, boolean isCurrent, String status) {
        this.yearId = yearId;
        this.yearTitle = yearTitle;
        this.isCurrent = isCurrent;
        this.status = status != null ? status : "ACTIVE";
    }

    public String getYearId() {
        return yearId;
    }

    public void setYearId(String yearId) {
        this.yearId = yearId;
    }

    public String getYearTitle() {
        return yearTitle != null ? yearTitle : "";
    }

    public void setYearTitle(String yearTitle) {
        this.yearTitle = yearTitle;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public void setCurrent(boolean current) {
        isCurrent = current;
    }

    public String getStatus() {
        return status != null ? status : "ACTIVE";
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
