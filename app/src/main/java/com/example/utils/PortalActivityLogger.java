package com.example.utils;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized audit logger utility for writing real successful operation events to Firestore portalActivities.
 */
public class PortalActivityLogger {

    private static final String TAG = "PortalActivityLogger";
    private static final String COLLECTION_ACTIVITIES = "portalActivities";

    private static PortalActivityLogger instance;
    private final FirebaseFirestore db;
    private final CollectionReference activitiesRef;

    private PortalActivityLogger(Context context) {
        db = FirebaseFirestore.getInstance();
        activitiesRef = db.collection(COLLECTION_ACTIVITIES);
    }

    public static synchronized PortalActivityLogger getInstance(Context context) {
        if (instance == null) {
            instance = new PortalActivityLogger(context.getApplicationContext());
        }
        return instance;
    }

    private String getCurrentTeacherUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : "TCH1001";
    }

    private Task<DocumentReference> logActivity(String type, String title, String description,
                                                String entityType, String entityId, String entityName,
                                                String studentId, String studentName, String registerNo,
                                                String subjectId, String subjectName, Map<String, Object> extraMeta) {
        String docId = "act_" + System.currentTimeMillis();
        Map<String, Object> data = new HashMap<>();

        data.put("activityId", docId);
        data.put("type", type);
        data.put("title", title);
        data.put("description", description);
        data.put("entityType", entityType);
        data.put("entityId", entityId != null ? entityId : "");
        data.put("entityName", entityName != null ? entityName : "");
        data.put("studentId", studentId != null ? studentId : "");
        data.put("studentName", studentName != null ? studentName : "");
        data.put("registerNo", registerNo != null ? registerNo : "");
        data.put("subjectId", subjectId != null ? subjectId : "");
        data.put("subjectName", subjectName != null ? subjectName : "");
        data.put("teacherId", getCurrentTeacherUid());
        data.put("teacherName", "Faculty Admin");
        data.put("timestamp", FieldValue.serverTimestamp());

        Map<String, Object> metadata = extraMeta != null ? extraMeta : new HashMap<>();
        data.put("metadata", metadata);

        return activitiesRef.add(data).addOnSuccessListener(ref -> {
            Log.d(TAG, "Logged activity [" + type + "]: " + title);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to log activity [" + type + "]", e);
        });
    }

    // --- STUDENT LOGS ---
    public Task<DocumentReference> logStudentAdded(String studentId, String studentName, String registerNo, String dept, String sem) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("department", dept);
        meta.put("semester", sem);
        return logActivity("STUDENT_ADDED", "Student Added",
                studentName + " was added to the student database.",
                "STUDENT", studentId, studentName, studentId, studentName, registerNo, "", "", meta);
    }

    public Task<DocumentReference> logStudentUpdated(String studentId, String studentName, String registerNo) {
        return logActivity("STUDENT_UPDATED", "Student Updated",
                studentName + "'s profile was updated.",
                "STUDENT", studentId, studentName, studentId, studentName, registerNo, "", "", null);
    }

    public Task<DocumentReference> logStudentDeleted(String studentId, String studentName, String registerNo) {
        return logActivity("STUDENT_DELETED", "Student Deleted",
                studentName + " was removed from the student database.",
                "STUDENT", studentId, studentName, studentId, studentName, registerNo, "", "", null);
    }

    // --- SUBJECT LOGS ---
    public Task<DocumentReference> logSubjectCreated(String subjectId, String subjectName, String subjectCode, String dept, String sem) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("subjectCode", subjectCode);
        meta.put("department", dept);
        meta.put("semester", sem);
        return logActivity("SUBJECT_CREATED", "Subject Created",
                subjectName + " (" + subjectCode + ") was created.",
                "SUBJECT", subjectId, subjectName, "", "", "", subjectId, subjectName, meta);
    }

    public Task<DocumentReference> logSubjectUpdated(String subjectId, String subjectName, String subjectCode) {
        return logActivity("SUBJECT_UPDATED", "Subject Updated",
                subjectName + " (" + subjectCode + ") details were updated.",
                "SUBJECT", subjectId, subjectName, "", "", "", subjectId, subjectName, null);
    }

    public Task<DocumentReference> logSubjectDeleted(String subjectId, String subjectName, String subjectCode) {
        return logActivity("SUBJECT_DELETED", "Subject Deleted",
                subjectName + " (" + subjectCode + ") was removed.",
                "SUBJECT", subjectId, subjectName, "", "", "", subjectId, subjectName, null);
    }

    // --- ATTENDANCE LOGS ---
    public Task<DocumentReference> logAttendanceMarked(String dept, String sem, String date, int totalStudents, int presentCount) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("department", dept);
        meta.put("semester", sem);
        meta.put("date", date);
        meta.put("totalStudents", totalStudents);
        meta.put("presentCount", presentCount);
        return logActivity("ATTENDANCE_MARKED", "Attendance Marked",
                "Attendance was marked for " + dept + " (" + sem + ").",
                "ATTENDANCE", dept + "_" + sem, dept, "", "", "", "", "", meta);
    }

    public Task<DocumentReference> logAttendanceUpdated(String dept, String sem, String date) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("department", dept);
        meta.put("semester", sem);
        meta.put("date", date);
        return logActivity("ATTENDANCE_UPDATED", "Attendance Updated",
                "Attendance was updated for " + dept + " (" + sem + ").",
                "ATTENDANCE", dept + "_" + sem, dept, "", "", "", "", "", meta);
    }

    // --- ASSIGNMENT LOGS ---
    public Task<DocumentReference> logAssignmentCreated(String assignmentId, String title, String subjectName) {
        return logActivity("ASSIGNMENT_CREATED", "Assignment Created",
                title + " was created for " + subjectName + ".",
                "ASSIGNMENT", assignmentId, title, "", "", "", "", subjectName, null);
    }

    public Task<DocumentReference> logAssignmentUpdated(String assignmentId, String title) {
        return logActivity("ASSIGNMENT_UPDATED", "Assignment Updated",
                title + " was updated.",
                "ASSIGNMENT", assignmentId, title, "", "", "", "", "", null);
    }

    public Task<DocumentReference> logAssignmentDeleted(String assignmentId, String title) {
        return logActivity("ASSIGNMENT_DELETED", "Assignment Deleted",
                title + " was deleted.",
                "ASSIGNMENT", assignmentId, title, "", "", "", "", "", null);
    }

    // --- RESULT LOGS ---
    public Task<DocumentReference> logResultPublished(String dept, String sem, String academicYear) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("department", dept);
        meta.put("semester", sem);
        meta.put("academicYear", academicYear);
        return logActivity("RESULT_PUBLISHED", "Result Published",
                sem + " results were published for " + dept + ".",
                "RESULT", dept + "_" + sem, sem + " Results", "", "", "", "", "", meta);
    }

    public Task<DocumentReference> logResultUnpublished(String dept, String sem) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("department", dept);
        meta.put("semester", sem);
        return logActivity("RESULT_UNPUBLISHED", "Result Unpublished",
                sem + " results were unpublished for " + dept + ".",
                "RESULT", dept + "_" + sem, sem + " Results", "", "", "", "", "", meta);
    }

    // --- BROADCAST LOGS ---
    public Task<DocumentReference> logBroadcastCreated(String alertId, String title, String target) {
        return logActivity("BROADCAST_CREATED", "Broadcast Created",
                title + " broadcast was created.",
                "BROADCAST", alertId, title, "", "", "", "", "", null);
    }

    public Task<DocumentReference> logBroadcastPublished(String alertId, String title, String target) {
        return logActivity("BROADCAST_PUBLISHED", "Broadcast Published",
                title + " was broadcast to " + target + ".",
                "BROADCAST", alertId, title, "", "", "", "", "", null);
    }

    // --- REPORT LOGS ---
    public Task<DocumentReference> logReportGenerated(String dept, String sem, String reportCategory) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("department", dept);
        meta.put("semester", sem);
        meta.put("category", reportCategory);
        return logActivity("REPORT_GENERATED", "Academic Report Generated",
                reportCategory + " report was generated for " + dept + " (" + sem + ").",
                "REPORT", dept + "_" + sem, reportCategory, "", "", "", "", "", meta);
    }

    // --- AUTHENTICATION & GENERAL ADMIN LOGS ---
    public Task<DocumentReference> logTeacherLogin(String teacherName, String email) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("email", email);
        return logActivity("TEACHER_LOGIN", "Teacher Login",
                teacherName + " logged into GradeXpert.",
                "AUTHENTICATION", getCurrentTeacherUid(), teacherName, "", "", "", "", "", meta);
    }

    public Task<DocumentReference> logTeacherActivity(String actorName, String title, String description, String category) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("category", category != null ? category : "General");
        return logActivity("ADMIN_ACTIVITY", title,
                description,
                "ADMIN", getCurrentTeacherUid(), actorName != null ? actorName : "System Admin", "", "", "", "", "", meta);
    }

    // --- PASSWORD MANAGEMENT AUDIT LOGS ---
    public Task<DocumentReference> logPasswordChanged(String targetUserId, String targetUserName, String targetUserRole, String adminUserId, String adminUserName) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("targetUserId", targetUserId != null ? targetUserId : "");
        meta.put("targetUserName", targetUserName != null ? targetUserName : "");
        meta.put("targetUserRole", targetUserRole != null ? targetUserRole : "");
        meta.put("adminUserId", adminUserId != null ? adminUserId : getCurrentTeacherUid());
        meta.put("adminUserName", adminUserName != null ? adminUserName : "System Admin");

        String roleStr = targetUserRole != null ? targetUserRole : "User";
        String desc = roleStr + " " + targetUserName + "'s password was changed by Admin.";

        return logActivity("PASSWORD_CHANGED", "Password Changed",
                desc,
                "USER_SECURITY", targetUserId, targetUserName,
                "STUDENT".equalsIgnoreCase(targetUserRole) ? targetUserId : "",
                targetUserName, "", "", "", meta);
    }

    public Task<DocumentReference> logStudentAccountCreated(String studentId, String studentName, String regNo, String dept) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("department", dept);
        meta.put("registerNo", regNo);
        meta.put("role", "STUDENT");

        return logActivity("STUDENT_ACCOUNT_CREATED", "Student Account Created",
                "Student " + studentName + "'s account was created by Admin.",
                "STUDENT", studentId, studentName, studentId, studentName, regNo, "", "", meta);
    }

    public Task<DocumentReference> logTeacherAccountCreated(String teacherId, String teacherName, String email, String dept) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("department", dept);
        meta.put("email", email);
        meta.put("role", "TEACHER");

        return logActivity("TEACHER_ACCOUNT_CREATED", "Teacher Account Created",
                "Teacher " + teacherName + "'s account was created by Admin.",
                "TEACHER", teacherId, teacherName, "", "", "", "", "", meta);
    }

    public Task<DocumentReference> logTeacherProfileUpdated(String teacherId, String teacherName, String dept, String designation) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("department", dept);
        meta.put("designation", designation);
        meta.put("role", "TEACHER");

        return logActivity("TEACHER_PROFILE_UPDATED", "Teacher Profile Updated",
                "Teacher " + teacherName + "'s details were updated by Admin.",
                "TEACHER", teacherId, teacherName, "", "", "", "", "", meta);
    }

    public Task<DocumentReference> logTeacherSubjectAssignment(String teacherId, String teacherName, String action, String subjectId, String subjectName) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("action", action);
        meta.put("subjectId", subjectId != null ? subjectId : "");
        meta.put("subjectName", subjectName != null ? subjectName : "");
        meta.put("role", "TEACHER");

        String title = "TEACHER_SUBJECT_ASSIGNED".equals(action) ? "Subject Assigned"
                : ("TEACHER_SUBJECT_REMOVED".equals(action) ? "Subject Removed" : "Subject Assignments Updated");
        String desc = "Teacher " + teacherName + " assignment updated: " + (subjectName != null && !subjectName.isEmpty() ? subjectName : action);

        return logActivity(action, title, desc,
                "TEACHER_ASSIGNMENT", teacherId, teacherName, "", "", "",
                subjectId != null ? subjectId : "", subjectName != null ? subjectName : "", meta);
    }

    public Task<DocumentReference> logTeacherStatusChanged(String teacherId, String teacherName, String newStatus) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("newStatus", newStatus);
        meta.put("role", "TEACHER");

        return logActivity("TEACHER_STATUS_CHANGED", "Teacher Status Updated",
                "Teacher " + teacherName + "'s status was changed to " + newStatus + " by Admin.",
                "TEACHER", teacherId, teacherName, "", "", "", "", "", meta);
    }

    public Task<DocumentReference> logPasswordResetRequested(String requestId, String userId, String userName, String role, String identifier) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("requestId", requestId);
        meta.put("role", role);
        meta.put("identifier", identifier);

        return logActivity("PASSWORD_RESET_REQUESTED", "Password Reset Requested",
                role + " " + userName + " (" + identifier + ") requested a password reset.",
                "USER_SECURITY", userId, userName,
                "STUDENT".equalsIgnoreCase(role) ? userId : "",
                userName, identifier, "", "", meta);
    }

    public Task<DocumentReference> logPasswordResetCompleted(String requestId, String userId, String userName, String role, String adminName) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("requestId", requestId);
        meta.put("role", role);
        meta.put("adminName", adminName != null ? adminName : "System Admin");

        return logActivity("PASSWORD_RESET_COMPLETED", "Password Reset Completed",
                "Admin completed password reset for " + role + " " + userName + ".",
                "USER_SECURITY", userId, userName,
                "STUDENT".equalsIgnoreCase(role) ? userId : "",
                userName, "", "", "", meta);
    }

    public Task<DocumentReference> logPasswordResetRejected(String requestId, String userId, String userName, String role, String reason, String adminName) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("requestId", requestId);
        meta.put("role", role);
        meta.put("reason", reason != null ? reason : "");
        meta.put("adminName", adminName != null ? adminName : "System Admin");

        return logActivity("PASSWORD_RESET_REJECTED", "Password Reset Rejected",
                "Password reset request for " + role + " " + userName + " was rejected by Admin.",
                "USER_SECURITY", userId, userName,
                "STUDENT".equalsIgnoreCase(role) ? userId : "",
                userName, "", "", "", meta);
    }
}


