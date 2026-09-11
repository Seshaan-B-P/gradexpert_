package com.example.repository;

import android.content.Context;
import android.util.Log;

import com.example.model.AcademicReport;
import com.example.model.Student;
import com.example.model.SubjectGradeItem;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for compiling real-time dynamic Academic Analytics and Reports from Firestore collections.
 */
public class AcademicReportRepository {

    private static final String TAG = "AcademicReportRepo";

    private static AcademicReportRepository instance;
    private final FirebaseFirestore db;

    public interface OnReportsLoadedListener {
        void onSuccess(List<AcademicReport> reports);
        void onError(String errorMessage);
    }

    private AcademicReportRepository(Context context) {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized AcademicReportRepository getInstance(Context context) {
        if (instance == null) {
            instance = new AcademicReportRepository(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Aggregates students, attendance, assignments, submissions, and official published results.
     */
    public void fetchConsolidatedAcademicReports(String department, String semester, String subjectFilter, String academicYear, OnReportsLoadedListener listener) {
        Log.d(TAG, "Fetching academic reports for Dept: " + department + ", Sem: " + semester);

        // Task 1: Fetch Students
        com.google.firebase.firestore.Query studentQuery = db.collection("students");
        if (department != null && !department.isEmpty() && !"All Departments".equalsIgnoreCase(department)) {
            studentQuery = studentQuery.whereEqualTo("department", department);
        }
        if (semester != null && !semester.isEmpty() && !"All Semesters".equalsIgnoreCase(semester)) {
            studentQuery = studentQuery.whereEqualTo("semester", semester);
        }
        Task<QuerySnapshot> studentTask = studentQuery.get();

        // Task 2: Fetch Attendance
        Task<QuerySnapshot> attendanceTask = db.collection("attendance").get();

        // Task 3: Fetch Assignments
        Task<QuerySnapshot> assignmentTask = db.collection("assignments").get();

        // Task 4: Fetch Submissions
        Task<QuerySnapshot> submissionTask = db.collection("assignment_submissions").get();

        // Task 5: Fetch Published Results
        Task<QuerySnapshot> resultTask = db.collection("results").get();

        Tasks.whenAllComplete(studentTask, attendanceTask, assignmentTask, submissionTask, resultTask)
                .addOnSuccessListener(tasks -> {
                    List<AcademicReport> reportList = new ArrayList<>();
                    Map<String, AcademicReport> reportMap = new HashMap<>();

                    // Parse Students
                    if (studentTask.isSuccessful() && studentTask.getResult() != null) {
                        for (DocumentSnapshot doc : studentTask.getResult().getDocuments()) {
                            String stdId = doc.getString("studentId");
                            if (stdId == null || stdId.isEmpty()) stdId = doc.getId();

                            String name = doc.getString("name");
                            if (name == null) name = doc.getString("studentName");
                            if (name == null) name = "Student";

                            String regNo = doc.getString("registerNo");
                            if (regNo == null) regNo = doc.getString("regNo");
                            if (regNo == null) regNo = doc.getString("reg_no");
                            if (regNo == null) regNo = "MCA00" + doc.getId();

                            String dept = doc.getString("department");
                            String sem = doc.getString("semester");

                            AcademicReport ar = new AcademicReport(stdId, name, regNo, dept != null ? dept : department, sem != null ? sem : semester);
                            reportMap.put(stdId, ar);
                            reportMap.put(regNo, ar); // Secondary mapping by RegNo
                        }
                    }

                    // Process Hour-Based Attendance
                    if (attendanceTask.isSuccessful() && attendanceTask.getResult() != null) {
                        for (DocumentSnapshot doc : attendanceTask.getResult().getDocuments()) {
                            String stdId = doc.getString("studentId");
                            String status = doc.getString("status");

                            AcademicReport ar = reportMap.get(stdId);
                            if (ar != null && status != null) {
                                ar.setTotalAttendanceHours(ar.getTotalAttendanceHours() + 1);
                                if ("PRESENT".equalsIgnoreCase(status)) {
                                    ar.setPresentHours(ar.getPresentHours() + 1);
                                } else if ("ABSENT".equalsIgnoreCase(status)) {
                                    ar.setAbsentHours(ar.getAbsentHours() + 1);
                                }
                            }
                        }
                    }

                    // Process Assignments & Submissions
                    int totalAssigned = 0;
                    if (assignmentTask.isSuccessful() && assignmentTask.getResult() != null) {
                        totalAssigned = assignmentTask.getResult().size();
                    }

                    for (AcademicReport ar : reportMap.values()) {
                        ar.setAssignmentsAssigned(totalAssigned);
                    }

                    if (submissionTask.isSuccessful() && submissionTask.getResult() != null) {
                        for (DocumentSnapshot doc : submissionTask.getResult().getDocuments()) {
                            String stdId = doc.getString("studentId");
                            String status = doc.getString("status");
                            Double marks = doc.getDouble("marksObtained");

                            AcademicReport ar = reportMap.get(stdId);
                            if (ar != null) {
                                if ("SUBMITTED".equalsIgnoreCase(status)) {
                                    ar.setAssignmentsSubmitted(ar.getAssignmentsSubmitted() + 1);
                                } else if ("LATE".equalsIgnoreCase(status)) {
                                    ar.setAssignmentsLate(ar.getAssignmentsLate() + 1);
                                } else if ("GRADED".equalsIgnoreCase(status)) {
                                    ar.setAssignmentsSubmitted(ar.getAssignmentsSubmitted() + 1);
                                    if (marks != null) {
                                        ar.setAvgAssignmentMarks(marks);
                                    }
                                }
                            }
                        }
                    }

                    // Process Official Published Results strictly
                    if (resultTask.isSuccessful() && resultTask.getResult() != null) {
                        for (DocumentSnapshot doc : resultTask.getResult().getDocuments()) {
                            String resStatus = doc.getString("status");
                            // Official reports include PUBLISHED results ONLY
                            if (!"PUBLISHED".equalsIgnoreCase(resStatus)) {
                                continue;
                            }

                            String stdId = doc.getString("studentId");
                            Double pct = doc.getDouble("percentage");
                            Double sgpa = doc.getDouble("sgpa");
                            Double cgpa = doc.getDouble("cgpa");

                            AcademicReport ar = reportMap.get(stdId);
                            if (ar != null) {
                                ar.setHasPublishedResult(true);
                                if (pct != null) ar.setAverageMarksPercentage(pct);
                                if (sgpa != null) ar.setSgpa(sgpa);
                                if (cgpa != null) ar.setCgpa(cgpa);
                            }
                        }
                    }

                    // Build final distinct list
                    List<AcademicReport> distinctReports = new ArrayList<>();
                    for (AcademicReport ar : reportMap.values()) {
                        if (!distinctReports.contains(ar)) {
                            ar.calculateMetricsAndStatus();
                            distinctReports.add(ar);
                        }
                    }

                    if (listener != null) listener.onSuccess(distinctReports);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error aggregating academic reports", e);
                    if (listener != null) listener.onError(e.getMessage());
                });
    }
}
