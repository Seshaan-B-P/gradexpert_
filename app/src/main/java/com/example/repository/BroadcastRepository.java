package com.example.repository;

import android.content.Context;
import android.util.Log;

import com.example.model.BroadcastAlert;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for handling Broadcast Alert operations against Firebase Firestore.
 */
public class BroadcastRepository {

    private static final String TAG = "BroadcastRepository";
    private static final String COLLECTION_ALERTS = "broadcastAlerts";

    private static BroadcastRepository instance;
    private final FirebaseFirestore db;
    private final CollectionReference alertsRef;

    public interface OnAlertsLoadedListener {
        void onSuccess(List<BroadcastAlert> alerts);
        void onError(String errorMessage);
    }

    public interface OnAlertOperationListener {
        void onSuccess(String message);
        void onError(String errorMessage);
    }

    public interface OnRecipientCountListener {
        void onSuccess(int count);
        void onError(String errorMessage);
    }

    private BroadcastRepository(Context context) {
        db = FirebaseFirestore.getInstance();
        alertsRef = db.collection(COLLECTION_ALERTS);
    }

    public static synchronized BroadcastRepository getInstance(Context context) {
        if (instance == null) {
            instance = new BroadcastRepository(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Fetches all broadcast alerts.
     */
    public void fetchAlerts(OnAlertsLoadedListener listener) {
        alertsRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<BroadcastAlert> alerts = new ArrayList<>();
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                BroadcastAlert ba = doc.toObject(BroadcastAlert.class);
                if (ba != null) {
                    if (ba.getAlertId() == null || ba.getAlertId().isEmpty()) {
                        ba.setAlertId(doc.getId());
                    }
                    alerts.add(ba);
                }
            }
            if (listener != null) listener.onSuccess(alerts);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching broadcast alerts", e);
            if (listener != null) listener.onError(e.getMessage());
        });
    }

    /**
     * Calculates the number of ACTIVE student recipients matching target constraints.
     */
    public void calculateRecipientCount(String targetType, String department, String semester, String studentId, OnRecipientCountListener listener) {
        Query studentQuery = db.collection("students").whereEqualTo("status", "ACTIVE");

        if ("DEPARTMENT".equalsIgnoreCase(targetType) && department != null && !department.isEmpty()) {
            studentQuery = studentQuery.whereEqualTo("department", department);
        } else if ("DEPARTMENT_SEMESTER".equalsIgnoreCase(targetType)) {
            if (department != null && !department.isEmpty()) {
                studentQuery = studentQuery.whereEqualTo("department", department);
            }
            if (semester != null && !semester.isEmpty()) {
                studentQuery = studentQuery.whereEqualTo("semester", semester);
            }
        } else if ("SPECIFIC_STUDENT".equalsIgnoreCase(targetType) && studentId != null && !studentId.isEmpty()) {
            if (listener != null) listener.onSuccess(1);
            return;
        }

        studentQuery.get().addOnSuccessListener(queryDocumentSnapshots -> {
            int count = queryDocumentSnapshots != null ? queryDocumentSnapshots.size() : 0;
            if (listener != null) listener.onSuccess(count);
        }).addOnFailureListener(e -> {
            if (listener != null) listener.onError(e.getMessage());
        });
    }

    /**
     * Creates a new broadcast alert document in Firestore.
     */
    public void createAlert(BroadcastAlert alert, OnAlertOperationListener listener) {
        String docId = (alert.getAlertId() != null && !alert.getAlertId().isEmpty())
                ? alert.getAlertId()
                : "alert_" + System.currentTimeMillis();
        alert.setAlertId(docId);

        Map<String, Object> data = new HashMap<>();
        data.put("alertId", docId);
        data.put("title", alert.getTitle());
        data.put("message", alert.getMessage());
        data.put("type", alert.getType());
        data.put("priority", alert.getPriority());
        data.put("targetType", alert.getTargetType());
        data.put("department", alert.getDepartment());
        data.put("semester", alert.getSemester());
        data.put("studentId", alert.getStudentId());
        data.put("studentName", alert.getStudentName());
        data.put("createdBy", alert.getCreatedBy());
        data.put("status", alert.getStatus());
        data.put("recipientCount", alert.getRecipientCount());
        data.put("createdAt", FieldValue.serverTimestamp());

        if ("PUBLISHED".equalsIgnoreCase(alert.getStatus())) {
            data.put("publishedAt", FieldValue.serverTimestamp());
        }

        alertsRef.document(docId).set(data).addOnSuccessListener(unused -> {
            if (listener != null) listener.onSuccess("Alert created successfully.");
        }).addOnFailureListener(e -> {
            if (listener != null) listener.onError(e.getMessage());
        });
    }

    /**
     * Cancels a scheduled alert by setting status to CANCELLED.
     */
    public void cancelScheduledAlert(String alertId, OnAlertOperationListener listener) {
        alertsRef.document(alertId).update("status", "CANCELLED", "updatedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(unused -> {
                    if (listener != null) listener.onSuccess("Scheduled alert cancelled.");
                }).addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    /**
     * Deletes a draft alert document from Firestore.
     */
    public void deleteDraftAlert(String alertId, OnAlertOperationListener listener) {
        alertsRef.document(alertId).delete().addOnSuccessListener(unused -> {
            if (listener != null) listener.onSuccess("Draft deleted successfully.");
        }).addOnFailureListener(e -> {
            if (listener != null) listener.onError(e.getMessage());
        });
    }

    /**
     * Helper to create an automated broadcast alert when official semester results are published.
     */
    public void createResultPublishedAlert(String semester, String department, OnAlertOperationListener listener) {
        String title = "Official Semester Results Published";
        String message = "Official results for " + department + " (" + semester + ") have been published. Please check your transcript.";
        BroadcastAlert alert = new BroadcastAlert(
                "alert_res_" + System.currentTimeMillis(),
                title,
                message,
                "Result",
                "High",
                "DEPARTMENT_SEMESTER",
                department,
                semester,
                "",
                "Students of " + semester,
                "SYSTEM",
                "PUBLISHED",
                42
        );
        createAlert(alert, listener);
    }

    /**
     * Helper to create an automated broadcast alert when a new assignment is published.
     */
    public void createAssignmentAlert(String assignmentTitle, String subjectName, String department, String semester, OnAlertOperationListener listener) {
        String title = "New Assignment Published: " + assignmentTitle;
        String message = "A new assignment has been posted for " + subjectName + ". Please review the requirements and due date.";
        BroadcastAlert alert = new BroadcastAlert(
                "alert_assg_" + System.currentTimeMillis(),
                title,
                message,
                "Assignment",
                "Normal",
                "DEPARTMENT_SEMESTER",
                department,
                semester,
                "",
                "Students of " + subjectName,
                "SYSTEM",
                "PUBLISHED",
                42
        );
        createAlert(alert, listener);
    }

    /**
     * Helper to create an attendance warning alert for students below required percentage.
     */
    public void createAttendanceWarningAlert(String studentId, String studentName, double attendancePct, OnAlertOperationListener listener) {
        String title = "Urgent: Low Attendance Warning";
        String message = String.format(java.util.Locale.US, "Your overall attendance is %.1f%%, which is below the mandatory 75%% threshold.", attendancePct);
        BroadcastAlert alert = new BroadcastAlert(
                "alert_att_" + System.currentTimeMillis(),
                title,
                message,
                "Attendance",
                "Urgent",
                "SPECIFIC_STUDENT",
                "",
                "",
                studentId,
                studentName,
                "SYSTEM",
                "PUBLISHED",
                1
        );
        createAlert(alert, listener);
    }
}
