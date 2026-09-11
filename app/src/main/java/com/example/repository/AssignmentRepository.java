package com.example.repository;

import android.content.Context;
import android.util.Log;

import com.example.model.Assignment;
import com.example.model.AssignmentSubmission;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository class managing Firebase Firestore operations for Assignments and Student Submissions.
 */
public class AssignmentRepository {

    private static final String TAG = "AssignmentRepository";
    private static AssignmentRepository instance;
    private final FirebaseFirestore db;

    public interface OnAssignmentOperationListener {
        void onSuccess(String message);
        void onError(String errorMessage);
    }

    public interface OnAssignmentsLoadedListener {
        void onSuccess(List<Assignment> assignments);
        void onError(String errorMessage);
    }

    public interface OnSubmissionsLoadedListener {
        void onSuccess(List<AssignmentSubmission> submissions);
        void onError(String errorMessage);
    }

    public interface OnStudentSubmissionCheckListener {
        void onChecked(AssignmentSubmission existingSubmission);
        void onError(String errorMessage);
    }

    private AssignmentRepository(Context context) {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized AssignmentRepository getInstance(Context context) {
        if (instance == null) {
            instance = new AssignmentRepository(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Create or update an assignment in Firestore.
     */
    public void saveAssignment(Assignment assignment, OnAssignmentOperationListener listener) {
        if (assignment == null) {
            if (listener != null) listener.onError("Invalid assignment object.");
            return;
        }

        String docId = assignment.getAssignmentId();
        if (docId == null || docId.trim().isEmpty()) {
            docId = "assign_" + System.currentTimeMillis();
            assignment.setAssignmentId(docId);
        }
        final String finalDocId = docId;

        Map<String, Object> data = new HashMap<>();
        data.put("assignmentId", finalDocId);
        data.put("title", assignment.getTitle());
        data.put("description", assignment.getDescription());
        data.put("department", assignment.getDepartment());
        data.put("semester", assignment.getSemester());
        data.put("subjectId", assignment.getSubjectId());
        data.put("subjectName", assignment.getSubjectName());
        data.put("assignmentType", assignment.getAssignmentType());
        data.put("dueDate", assignment.getDueDate());
        data.put("dueTime", assignment.getDueTime());
        data.put("maxMarks", assignment.getMaxMarks());
        data.put("attachmentUrl", assignment.getAttachmentUrl() != null ? assignment.getAttachmentUrl() : "");
        data.put("attachmentName", assignment.getAttachmentName() != null ? assignment.getAttachmentName() : "");
        data.put("createdBy", assignment.getCreatedBy());
        data.put("status", assignment.getStatus());
        data.put("updatedAt", FieldValue.serverTimestamp());

        if (assignment.getCreatedAt() == null) {
            data.put("createdAt", FieldValue.serverTimestamp());
        }

        db.collection("assignments")
                .document(finalDocId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Assignment saved successfully: " + finalDocId);
                    if (listener != null) listener.onSuccess(finalDocId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save assignment: " + e.getMessage());
                    if (listener != null) listener.onError("Failed to save assignment: " + e.getMessage());
                });
    }

    /**
     * Fetch teacher's assignments from Firestore with status filtering.
     */
    public void fetchTeacherAssignments(String teacherUid, String statusFilter, OnAssignmentsLoadedListener listener) {
        Query query = db.collection("assignments");
        if (teacherUid != null && !teacherUid.trim().isEmpty()) {
            query = query.whereEqualTo("createdBy", teacherUid);
        }

        query.get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Assignment> assignments = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        try {
                            Assignment a = doc.toObject(Assignment.class);
                            if (a != null) {
                                if (statusFilter == null || "ALL".equalsIgnoreCase(statusFilter) || statusFilter.equalsIgnoreCase(a.getStatus())) {
                                    assignments.add(a);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing assignment doc: " + doc.getId(), e);
                        }
                    }
                    if (listener != null) listener.onSuccess(assignments);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch teacher assignments: " + e.getMessage());
                    if (listener != null) listener.onError("Unable to load assignments from server.");
                });
    }

    /**
     * Fetch ONLY published assignments matching student's department & semester.
     */
    public void fetchStudentAssignments(String department, String semester, OnAssignmentsLoadedListener listener) {
        Query query = db.collection("assignments").whereEqualTo("status", "PUBLISHED");

        if (department != null && !department.isEmpty() && !"All Departments".equalsIgnoreCase(department)) {
            query = query.whereEqualTo("department", department);
        }
        if (semester != null && !semester.isEmpty() && !"All Semesters".equalsIgnoreCase(semester)) {
            query = query.whereEqualTo("semester", semester);
        }

        query.get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Assignment> list = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        try {
                            Assignment a = doc.toObject(Assignment.class);
                            if (a != null) list.add(a);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing published assignment doc: " + doc.getId(), e);
                        }
                    }
                    if (listener != null) listener.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch student assignments: " + e.getMessage());
                    if (listener != null) listener.onError("Unable to connect to server.");
                });
    }

    /**
     * Update status (e.g., DRAFT -> PUBLISHED, or PUBLISHED -> CLOSED).
     */
    public void updateAssignmentStatus(String assignmentId, String newStatus, OnAssignmentOperationListener listener) {
        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("status", newStatus);
        updateMap.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("assignments").document(assignmentId)
                .update(updateMap)
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess("Status updated to " + newStatus);
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError("Failed to update status: " + e.getMessage());
                });
    }

    /**
     * Delete an assignment.
     */
    public void deleteAssignment(String assignmentId, OnAssignmentOperationListener listener) {
        db.collection("assignments").document(assignmentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess("Assignment deleted successfully.");
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError("Failed to delete assignment: " + e.getMessage());
                });
    }

    /**
     * Check if student already submitted an assignment.
     */
    public void checkStudentSubmission(String studentId, String assignmentId, OnStudentSubmissionCheckListener listener) {
        String docId = "sub_" + studentId + "_" + assignmentId;
        db.collection("assignment_submissions").document(docId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        try {
                            AssignmentSubmission sub = doc.toObject(AssignmentSubmission.class);
                            if (listener != null) listener.onChecked(sub);
                            return;
                        } catch (Exception ignored) {}
                    }
                    if (listener != null) listener.onChecked(null);
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    /**
     * Submit student solution.
     */
    public void submitAssignment(AssignmentSubmission submission, OnAssignmentOperationListener listener) {
        String docId = "sub_" + submission.getStudentId() + "_" + submission.getAssignmentId();
        submission.setSubmissionId(docId);

        Map<String, Object> data = new HashMap<>();
        data.put("submissionId", docId);
        data.put("assignmentId", submission.getAssignmentId());
        data.put("studentId", submission.getStudentId());
        data.put("studentName", submission.getStudentName());
        data.put("registerNo", submission.getRegisterNo());
        data.put("answer", submission.getAnswer() != null ? submission.getAnswer() : "");
        data.put("attachmentUrl", submission.getAttachmentUrl() != null ? submission.getAttachmentUrl() : "");
        data.put("submittedAt", FieldValue.serverTimestamp());
        data.put("marks", submission.getMarks());
        data.put("feedback", submission.getFeedback() != null ? submission.getFeedback() : "");
        data.put("status", submission.getStatus());

        db.collection("assignment_submissions").document(docId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess("Assignment submitted successfully.");
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError("Submission failed: " + e.getMessage());
                });
    }

    /**
     * Fetch submissions for a specific assignment (for teacher view).
     */
    public void fetchSubmissionsForAssignment(String assignmentId, OnSubmissionsLoadedListener listener) {
        db.collection("assignment_submissions")
                .whereEqualTo("assignmentId", assignmentId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<AssignmentSubmission> list = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        try {
                            AssignmentSubmission sub = doc.toObject(AssignmentSubmission.class);
                            if (sub != null) list.add(sub);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing submission doc: " + doc.getId(), e);
                        }
                    }
                    if (listener != null) listener.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError("Unable to load submissions.");
                });
    }

    /**
     * Grade a student submission (teacher enters marks and feedback).
     */
    public void gradeSubmission(String submissionId, double marks, String feedback, OnAssignmentOperationListener listener) {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("marks", marks);
        updateData.put("feedback", feedback);
        updateData.put("status", "GRADED");

        db.collection("assignment_submissions").document(submissionId)
                .update(updateData)
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess("Grade and feedback saved successfully.");
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError("Failed to save grade: " + e.getMessage());
                });
    }
}
