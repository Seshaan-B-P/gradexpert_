package com.example.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Calculated DTO/View Model class representing a student's consolidated academic report.
 */
public class AcademicReport {

    private String studentId;
    private String studentName;
    private String registerNo;
    private String department;
    private String semester;

    // Attendance stats (Hour-Based)
    private int totalAttendanceHours;
    private int presentHours;
    private int absentHours;
    private double attendancePercentage;

    // Assignment stats
    private int assignmentsAssigned;
    private int assignmentsSubmitted;
    private int assignmentsPending;
    private int assignmentsLate;
    private double assignmentCompletionPercentage;
    private double avgAssignmentMarks;

    // Academic Result stats (Official Published ONLY)
    private double totalMarks;
    private double averageMarksPercentage;
    private double sgpa;
    private double cgpa;
    private int totalSubjectsCount;
    private int passedSubjectsCount;
    private int failedSubjectsCount;
    private boolean hasPublishedResult;

    // Performance Status
    private String academicStatus; // EXCELLENT, GOOD, AVERAGE, NEEDS_IMPROVEMENT, CRITICAL
    private boolean isAtRisk;
    private List<String> atRiskReasons = new ArrayList<>();

    // Subject breakdown
    private List<SubjectGradeItem> subjectGradeList = new ArrayList<>();

    public AcademicReport() {
        this.academicStatus = "AVERAGE";
    }

    public AcademicReport(String studentId, String studentName, String registerNo, String department, String semester) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.registerNo = registerNo;
        this.department = department;
        this.semester = semester;
        this.academicStatus = "AVERAGE";
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName != null ? studentName : "";
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getRegisterNo() {
        return registerNo != null ? registerNo : "";
    }

    public void setRegisterNo(String registerNo) {
        this.registerNo = registerNo;
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

    public int getTotalAttendanceHours() {
        return totalAttendanceHours;
    }

    public void setTotalAttendanceHours(int totalAttendanceHours) {
        this.totalAttendanceHours = totalAttendanceHours;
    }

    public int getPresentHours() {
        return presentHours;
    }

    public void setPresentHours(int presentHours) {
        this.presentHours = presentHours;
    }

    public int getAbsentHours() {
        return absentHours;
    }

    public void setAbsentHours(int absentHours) {
        this.absentHours = absentHours;
    }

    public double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }

    public int getAssignmentsAssigned() {
        return assignmentsAssigned;
    }

    public void setAssignmentsAssigned(int assignmentsAssigned) {
        this.assignmentsAssigned = assignmentsAssigned;
    }

    public int getAssignmentsSubmitted() {
        return assignmentsSubmitted;
    }

    public void setAssignmentsSubmitted(int assignmentsSubmitted) {
        this.assignmentsSubmitted = assignmentsSubmitted;
    }

    public int getAssignmentsPending() {
        return assignmentsPending;
    }

    public void setAssignmentsPending(int assignmentsPending) {
        this.assignmentsPending = assignmentsPending;
    }

    public int getAssignmentsLate() {
        return assignmentsLate;
    }

    public void setAssignmentsLate(int assignmentsLate) {
        this.assignmentsLate = assignmentsLate;
    }

    public double getAssignmentCompletionPercentage() {
        return assignmentCompletionPercentage;
    }

    public void setAssignmentCompletionPercentage(double assignmentCompletionPercentage) {
        this.assignmentCompletionPercentage = assignmentCompletionPercentage;
    }

    public double getAvgAssignmentMarks() {
        return avgAssignmentMarks;
    }

    public void setAvgAssignmentMarks(double avgAssignmentMarks) {
        this.avgAssignmentMarks = avgAssignmentMarks;
    }

    public double getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(double totalMarks) {
        this.totalMarks = totalMarks;
    }

    public double getAverageMarksPercentage() {
        return averageMarksPercentage;
    }

    public void setAverageMarksPercentage(double averageMarksPercentage) {
        this.averageMarksPercentage = averageMarksPercentage;
    }

    public double getSgpa() {
        return sgpa;
    }

    public void setSgpa(double sgpa) {
        this.sgpa = sgpa;
    }

    public double getCgpa() {
        return cgpa;
    }

    public void setCgpa(double cgpa) {
        this.cgpa = cgpa;
    }

    public int getTotalSubjectsCount() {
        return totalSubjectsCount;
    }

    public void setTotalSubjectsCount(int totalSubjectsCount) {
        this.totalSubjectsCount = totalSubjectsCount;
    }

    public int getPassedSubjectsCount() {
        return passedSubjectsCount;
    }

    public void setPassedSubjectsCount(int passedSubjectsCount) {
        this.passedSubjectsCount = passedSubjectsCount;
    }

    public int getFailedSubjectsCount() {
        return failedSubjectsCount;
    }

    public void setFailedSubjectsCount(int failedSubjectsCount) {
        this.failedSubjectsCount = failedSubjectsCount;
    }

    public boolean isHasPublishedResult() {
        return hasPublishedResult;
    }

    public void setHasPublishedResult(boolean hasPublishedResult) {
        this.hasPublishedResult = hasPublishedResult;
    }

    public String getAcademicStatus() {
        return academicStatus;
    }

    public void setAcademicStatus(String academicStatus) {
        this.academicStatus = academicStatus;
    }

    public boolean isAtRisk() {
        return isAtRisk;
    }

    public void setAtRisk(boolean atRisk) {
        isAtRisk = atRisk;
    }

    public List<String> getAtRiskReasons() {
        return atRiskReasons;
    }

    public void setAtRiskReasons(List<String> atRiskReasons) {
        this.atRiskReasons = atRiskReasons;
    }

    public List<SubjectGradeItem> getSubjectGradeList() {
        return subjectGradeList;
    }

    public void setSubjectGradeList(List<SubjectGradeItem> subjectGradeList) {
        this.subjectGradeList = subjectGradeList;
    }

    /**
     * Computes performance category and evaluates At-Risk status.
     */
    public void calculateMetricsAndStatus() {
        // Attendance % = Present / Total * 100
        if (totalAttendanceHours > 0) {
            attendancePercentage = ((double) presentHours / (double) totalAttendanceHours) * 100.0;
        } else {
            attendancePercentage = 0.0;
        }

        // Assignment completion % = (Submitted + Late) / Assigned * 100
        if (assignmentsAssigned > 0) {
            assignmentCompletionPercentage = ((double) (assignmentsSubmitted + assignmentsLate) / (double) assignmentsAssigned) * 100.0;
            assignmentsPending = Math.max(0, assignmentsAssigned - (assignmentsSubmitted + assignmentsLate));
        } else {
            assignmentCompletionPercentage = 0.0;
            assignmentsPending = 0;
        }

        // Academic Status logic
        if (averageMarksPercentage >= 90.0) {
            academicStatus = "EXCELLENT";
        } else if (averageMarksPercentage >= 80.0) {
            academicStatus = "GOOD";
        } else if (averageMarksPercentage >= 70.0) {
            academicStatus = "AVERAGE";
        } else if (averageMarksPercentage >= 60.0) {
            academicStatus = "NEEDS_IMPROVEMENT";
        } else {
            academicStatus = "CRITICAL";
        }

        // At-Risk criteria evaluation
        atRiskReasons.clear();
        isAtRisk = false;

        if (totalAttendanceHours > 0 && attendancePercentage < 75.0) {
            isAtRisk = true;
            atRiskReasons.add(String.format("Low Attendance (%.1f%%)", attendancePercentage));
        }
        if (hasPublishedResult && averageMarksPercentage < 60.0) {
            isAtRisk = true;
            atRiskReasons.add(String.format("Low Academic Average (%.1f%%)", averageMarksPercentage));
        }
        if (failedSubjectsCount > 0) {
            isAtRisk = true;
            atRiskReasons.add(String.format("%d Failed Subject(s)", failedSubjectsCount));
        }
        if (assignmentsPending >= 2) {
            isAtRisk = true;
            atRiskReasons.add(String.format("%d Pending Assignment(s)", assignmentsPending));
        }
    }
}
