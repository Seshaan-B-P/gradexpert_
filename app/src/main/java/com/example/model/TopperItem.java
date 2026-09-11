package com.example.model;

/**
 * Model class for Topper Report entries.
 */
public class TopperItem {
    private int rank;
    private String name;
    private String regNo;
    private String department;
    private double cgpa;
    private double sgpa;
    private String medalBadge; // Gold, Silver, Bronze, Dean's List

    public TopperItem(int rank, String name, String regNo, String department, double cgpa, double sgpa, String medalBadge) {
        this.rank = rank;
        this.name = name;
        this.regNo = regNo;
        this.department = department;
        this.cgpa = cgpa;
        this.sgpa = sgpa;
        this.medalBadge = medalBadge;
    }

    public int getRank() {
        return rank;
    }

    public String getName() {
        return name;
    }

    public String getRegNo() {
        return regNo;
    }

    public String getDepartment() {
        return department;
    }

    public double getCgpa() {
        return cgpa;
    }

    public double getSgpa() {
        return sgpa;
    }

    public String getMedalBadge() {
        return medalBadge;
    }
}
