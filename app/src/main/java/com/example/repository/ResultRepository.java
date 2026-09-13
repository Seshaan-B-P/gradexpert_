package com.example.repository;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.database.DatabaseHelper;
import com.example.model.AppNotification;
import com.example.model.Result;
import com.example.model.ResultUpdateRequest;
import com.example.model.Student;
import com.example.model.SubjectGradeItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Central repository managing the complete Result Publishing and Result Update Approval workflow.
 * Encapsulates Firestore collections: results, resultUpdateRequests, notifications, and auditLogs.
 */
public class ResultRepository {

    private static final String TAG = "ResultRepository";

    public static final String COLLECTION_RESULTS = "results";
    public static final String COLLECTION_UPDATE_REQUESTS = "resultUpdateRequests";
    public static final String COLLECTION_NOTIFICATIONS = "notifications";
    public static final String COLLECTION_AUDIT_LOGS = "auditLogs";

    public static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_UPDATE_PENDING_APPROVAL = "UPDATE_PENDING_APPROVAL";
    public static final String STATUS_UPDATE_REJECTED = "UPDATE_REJECTED";

    public interface OnResultOperationListener {
        void onSuccess(String message);
        void onError(String errorMessage);
    }

    public interface OnResultsLoadedListener {
        void onLoaded(List<Result> results);
        void onError(String errorMessage);
    }

    public interface OnUpdateRequestsLoadedListener {
        void onLoaded(List<ResultUpdateRequest> requests);
        void onError(String errorMessage);
    }

    public interface OnSingleResultLoadedListener {
        void onLoaded(Result result);
        void onError(String errorMessage);
    }

    public interface OnCountListener {
        void onCount(int count);
        void onError(String errorMessage);
    }

    private static ResultRepository instance;
    private final Context context;
    private final FirebaseFirestore db;
    private final DatabaseHelper dbHelper;

    private ResultRepository(Context context) {
        this.context = context.getApplicationContext();
        this.db = FirebaseFirestore.getInstance();
        this.dbHelper = new DatabaseHelper(this.context);
    }

    public static synchronized ResultRepository getInstance(Context context) {
        if (instance == null) {
            instance = new ResultRepository(context);
        }
        return instance;
    }

    public boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo netInfo = cm.getActiveNetworkInfo();
                return netInfo != null && netInfo.isConnected();
            }
        } catch (Exception e) {
            Log.w(TAG, "Network check error: " + e.getMessage());
        }
        return false;
    }

    // =========================================================================
    // 1. TEACHER: SUBMIT RESULT FOR APPROVAL (NEW OR RE-SUBMISSION)
    // =========================================================================

    public void submitResultForApproval(@NonNull Result result, @NonNull OnResultOperationListener listener) {
        final String docId = result.getResultId();
        if (docId == null || docId.trim().isEmpty()) {
            listener.onError("Invalid result identifier.");
            return;
        }

        result.setStatus(STATUS_PENDING_APPROVAL);
        if (result.getVersion() <= 0) result.setVersion(1);

        String currentPubDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());
        result.setPublishedDate(currentPubDate);

        // Map subjects to Map list for Firestore storage
        List<Map<String, Object>> subjectMaps = new ArrayList<>();
        for (SubjectGradeItem sub : result.getSubjects()) {
            Map<String, Object> sm = new HashMap<>();
            sm.put("subjectCode", sub.getSubjectCode());
            sm.put("subjectName", sub.getSubjectName());
            sm.put("credits", sub.getCredits());
            sm.put("internalMarks", sub.getInternalMarks());
            sm.put("internal1", sub.getInternal1());
            sm.put("assignment", sub.getAssignment());
            sm.put("modelExam", sub.getModelExam());
            sm.put("universityExam", sub.getUniversityExam());
            sm.put("totalMarks", sub.getTotalMarks());
            sm.put("percentage", sub.getPercentage());
            sm.put("grade", sub.getGrade());
            sm.put("gradePoint", sub.getGradePoint());
            sm.put("creditPoints", sub.getCreditPoints());
            subjectMaps.add(sm);
        }

        String candidateUid = result.getStudentUid();
        if (candidateUid == null || candidateUid.trim().isEmpty()) {
            candidateUid = dbHelper.getStudentFirebaseUid(result.getStudentNumericId());
        }
        if (candidateUid == null || candidateUid.trim().isEmpty()) {
            candidateUid = dbHelper.getStudentFirebaseUidByRegNo(result.getRegisterNo());
        }
        if (candidateUid == null || candidateUid.trim().isEmpty()) {
            candidateUid = result.getStudentId();
        }
        final String sUid = candidateUid;
        result.setStudentUid(sUid);

        Map<String, Object> data = new HashMap<>();
        data.put("resultId", docId);
        data.put("id", result.getId());
        data.put("student_id", result.getStudentNumericId());
        data.put("studentId", result.getStudentId() != null ? result.getStudentId() : String.valueOf(result.getStudentNumericId()));
        data.put("studentUid", sUid);
        data.put("studentRegisterNo", result.getRegisterNo() != null ? result.getRegisterNo() : "");
        data.put("studentName", result.getStudentName() != null ? result.getStudentName() : "");
        data.put("registerNo", result.getRegisterNo() != null ? result.getRegisterNo() : "");
        data.put("reg_no", result.getRegisterNo() != null ? result.getRegisterNo() : "");
        data.put("programLevel", result.getProgramLevel() != null ? result.getProgramLevel() : "UG");
        data.put("departmentId", result.getDepartmentId() != null ? result.getDepartmentId() : "");
        data.put("departmentName", result.getDepartmentName() != null ? result.getDepartmentName() : "");
        data.put("department", result.getDepartmentName() != null ? result.getDepartmentName() : "");
        data.put("semester", result.getSemester());
        data.put("academicYear", result.getAcademicYear() != null ? result.getAcademicYear() : "2025-2026");
        data.put("subjects", subjectMaps);
        data.put("totalMarks", result.getTotalMarks());
        data.put("total_marks", result.getTotalMarks());
        data.put("percentage", result.getPercentage());
        data.put("sgpa", result.getSgpa());
        data.put("cgpa", result.getCgpa());
        data.put("status", STATUS_PENDING_APPROVAL);
        data.put("version", result.getVersion());
        data.put("publishedBy", result.getTeacherName() != null ? result.getTeacherName() : "Faculty");
        data.put("teacherId", result.getTeacherId() != null ? result.getTeacherId() : "");
        data.put("teacherName", result.getTeacherName() != null ? result.getTeacherName() : "Faculty");
        data.put("published_date", currentPubDate);
        data.put("submittedAt", FieldValue.serverTimestamp());
        data.put("updatedAt", FieldValue.serverTimestamp());
        data.put("rejectionReason", null);
        data.put("rejectedAt", null);
        data.put("rejectedBy", null);
        data.put("approvedAt", null);
        data.put("approvedBy", null);
        data.put("approvalNote", null);

        // Offline check
        if (!isNetworkAvailable()) {
            // Save locally in SQLite cache with PENDING_APPROVAL status and LOCAL_PENDING_SYNC
            dbHelper.publishOrUpdateResult(result.getStudentNumericId(), result.getSemester(),
                    result.getTotalMarks(), result.getPercentage(), result.getSgpa(), result.getCgpa(),
                    STATUS_PENDING_APPROVAL, currentPubDate, "LOCAL_PENDING_SYNC", sUid);
            listener.onSuccess("Result saved locally. It will be submitted when internet connection is restored.");
            return;
        }

        db.collection(COLLECTION_RESULTS).document(docId).set(data)
                .addOnSuccessListener(aVoid -> {
                    // Cache locally as PENDING_APPROVAL with SYNCED status
                    dbHelper.publishOrUpdateResult(result.getStudentNumericId(), result.getSemester(),
                            result.getTotalMarks(), result.getPercentage(), result.getSgpa(), result.getCgpa(),
                            STATUS_PENDING_APPROVAL, currentPubDate, "SYNCED", sUid);

                    // Notify Admin
                    dispatchAdminResultApprovalNotification(result, docId);

                    // Log Audit Trail
                    logAuditTrail("RESULT_SUBMITTED", docId, sUid != null ? sUid : result.getStudentId(),
                            result.getStudentName(), result.getSemester(),
                            result.getTeacherName(), "TEACHER",
                            "DRAFT", STATUS_PENDING_APPROVAL, result.getVersion(), result.getVersion(),
                            "Submitted result for semester " + result.getSemester() + " for Admin approval.");

                    listener.onSuccess("Result submitted for Admin approval");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed submitting result to Firestore: " + e.getMessage());
                    listener.onError("Failed to submit result: " + e.getMessage());
                });
    }

    /**
     * Synchronizes locally pending results (saved offline) to Firestore once internet is restored.
     */
    public void syncPendingLocalResults() {
        if (!isNetworkAvailable()) return;
        List<Result> pending = dbHelper.getPendingSyncResults();
        if (pending == null || pending.isEmpty()) return;

        for (Result result : pending) {
            String docId = result.getResultId();
            if (docId == null || docId.isEmpty()) {
                docId = result.getStudentId() + "_" + result.getSemester();
            }

            String candidateUid = result.getStudentUid();
            if (candidateUid == null || candidateUid.trim().isEmpty()) {
                candidateUid = dbHelper.getStudentFirebaseUid(result.getStudentNumericId());
            }
            if (candidateUid == null || candidateUid.trim().isEmpty()) {
                candidateUid = result.getStudentId();
            }
            final String sUid = candidateUid;

            String sLoginId = result.getStudentLoginId();
            if (sLoginId == null || sLoginId.trim().isEmpty()) {
                try {
                    Student sObj = dbHelper.getStudentById(result.getStudentNumericId());
                    if (sObj != null && sObj.getLoginId() != null && !sObj.getLoginId().trim().isEmpty()) {
                        sLoginId = sObj.getLoginId().trim();
                    }
                } catch (Exception ignored) {}
            }
            if (sLoginId == null || sLoginId.trim().isEmpty()) {
                sLoginId = result.getRegisterNo() != null ? result.getRegisterNo() : "";
            }

            Map<String, Object> data = new HashMap<>();
            data.put("resultId", docId);
            data.put("id", result.getId());
            data.put("student_id", result.getStudentNumericId());
            data.put("studentId", result.getStudentId());
            data.put("studentUid", sUid);
            data.put("studentLoginId", sLoginId);
            data.put("studentRegisterNo", result.getRegisterNo() != null ? result.getRegisterNo() : "");
            data.put("studentName", result.getStudentName() != null ? result.getStudentName() : "");
            data.put("registerNo", result.getRegisterNo() != null ? result.getRegisterNo() : "");
            data.put("reg_no", result.getRegisterNo() != null ? result.getRegisterNo() : "");
            data.put("programLevel", result.getProgramLevel() != null ? result.getProgramLevel() : "UG");
            data.put("departmentId", result.getDepartmentId() != null ? result.getDepartmentId() : "");
            data.put("departmentName", result.getDepartmentName() != null ? result.getDepartmentName() : "");
            data.put("semester", result.getSemester());
            data.put("academicYear", result.getAcademicYear() != null ? result.getAcademicYear() : "2025-2026");
            data.put("totalMarks", result.getTotalMarks());
            data.put("total_marks", result.getTotalMarks());
            data.put("percentage", result.getPercentage());
            data.put("sgpa", result.getSgpa());
            data.put("cgpa", result.getCgpa());
            data.put("status", STATUS_PENDING_APPROVAL);
            data.put("version", result.getVersion() > 0 ? result.getVersion() : 1);
            data.put("published_date", result.getPublishedDate() != null ? result.getPublishedDate() : "");
            data.put("submittedAt", FieldValue.serverTimestamp());
            data.put("updatedAt", FieldValue.serverTimestamp());

            final String finalDocId = docId;
            final Result finalResult = result;
            db.collection(COLLECTION_RESULTS).document(finalDocId).set(data, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        dbHelper.markResultSynced(finalResult.getStudentNumericId(), finalResult.getSemester());
                        dispatchAdminResultApprovalNotification(finalResult, finalDocId);
                        Log.d(TAG, "Successfully synced local pending result: " + finalDocId);
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed syncing pending result: " + e.getMessage()));
        }
    }

    // =========================================================================
    // 2. TEACHER: REQUEST RESULT UPDATE FOR ALREADY APPROVED RESULT
    // =========================================================================

    public void submitResultUpdateRequest(@NonNull ResultUpdateRequest request, @NonNull OnResultOperationListener listener) {
        if (!isNetworkAvailable()) {
            listener.onError("Internet connection required to request a result update.");
            return;
        }

        if (request.getReasonForUpdate() == null || request.getReasonForUpdate().trim().isEmpty()) {
            listener.onError("Please provide a valid reason for updating this approved result.");
            return;
        }

        String reqId = "upd_" + request.getResultId() + "_" + System.currentTimeMillis();
        request.setRequestId(reqId);
        request.setStatus(STATUS_UPDATE_PENDING_APPROVAL);

        List<Map<String, Object>> oldSubMaps = serializeSubjects(request.getOldSubjects());
        List<Map<String, Object>> newSubMaps = serializeSubjects(request.getNewSubjects());

        String candidateUid = request.getStudentUid();
        if (candidateUid == null || candidateUid.trim().isEmpty()) {
            candidateUid = dbHelper.getStudentFirebaseUid(request.getStudentNumericId());
        }
        if (candidateUid == null || candidateUid.trim().isEmpty()) {
            candidateUid = request.getStudentId();
        }
        final String sUid = candidateUid;
        request.setStudentUid(sUid);

        Map<String, Object> data = new HashMap<>();
        data.put("requestId", reqId);
        data.put("resultId", request.getResultId());
        data.put("student_id", request.getStudentNumericId());
        data.put("studentId", request.getStudentId());
        data.put("studentUid", sUid);
        data.put("studentName", request.getStudentName() != null ? request.getStudentName() : "");
        data.put("registerNo", request.getRegisterNo() != null ? request.getRegisterNo() : "");
        data.put("programLevel", request.getProgramLevel() != null ? request.getProgramLevel() : "UG");
        data.put("departmentId", request.getDepartmentId() != null ? request.getDepartmentId() : "");
        data.put("departmentName", request.getDepartmentName() != null ? request.getDepartmentName() : "");
        data.put("semester", request.getSemester());
        data.put("academicYear", request.getAcademicYear() != null ? request.getAcademicYear() : "2025-2026");

        data.put("oldVersion", request.getOldVersion());
        data.put("newVersion", request.getNewVersion());

        data.put("oldMarks", request.getOldMarks());
        data.put("newMarks", request.getNewMarks());
        data.put("oldPercentage", request.getOldPercentage());
        data.put("newPercentage", request.getNewPercentage());
        data.put("oldSgpa", request.getOldSgpa());
        data.put("newSgpa", request.getNewSgpa());
        data.put("oldCgpa", request.getOldCgpa());
        data.put("newCgpa", request.getNewCgpa());

        data.put("oldGrades", request.getOldGrades());
        data.put("newGrades", request.getNewGrades());
        data.put("oldSubjects", oldSubMaps);
        data.put("newSubjects", newSubMaps);

        data.put("reasonForUpdate", request.getReasonForUpdate());
        data.put("teacherId", request.getTeacherId() != null ? request.getTeacherId() : "");
        data.put("teacherName", request.getTeacherName() != null ? request.getTeacherName() : "Faculty");
        data.put("updatedBy", request.getTeacherName() != null ? request.getTeacherName() : "Faculty");

        data.put("status", STATUS_UPDATE_PENDING_APPROVAL);
        data.put("submittedAt", FieldValue.serverTimestamp());
        data.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_UPDATE_REQUESTS).document(reqId).set(data)
                .addOnSuccessListener(aVoid -> {
                    // Send notification to Admin
                    dispatchAdminResultUpdateNotification(request, reqId);

                    // Log audit trail
                    logAuditTrail("RESULT_UPDATE_SUBMITTED", request.getResultId(), request.getStudentId(),
                            request.getStudentName(), request.getSemester(),
                            request.getTeacherName(), "TEACHER",
                            STATUS_APPROVED, STATUS_UPDATE_PENDING_APPROVAL,
                            request.getOldVersion(), request.getNewVersion(),
                            request.getReasonForUpdate());

                    listener.onSuccess("Update request submitted for Admin approval. The currently approved result will remain active until Admin approves.");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed creating result update request: " + e.getMessage());
                    listener.onError("Failed to submit update request: " + e.getMessage());
                });
    }

    // =========================================================================
    // 3. ADMIN: APPROVE NEW RESULT
    // =========================================================================

    public void approveResult(@NonNull Result result, String adminUid, String adminName, String approvalNote, @NonNull OnResultOperationListener listener) {
        if (!isNetworkAvailable()) {
            listener.onError("Internet connection required to approve results.");
            return;
        }

        String docId = result.getResultId();
        String approvedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", STATUS_APPROVED);
        updates.put("approvedAt", FieldValue.serverTimestamp());
        updates.put("approvedBy", adminUid != null ? adminUid : "ADMIN");
        updates.put("approvalNote", approvalNote != null ? approvalNote.trim() : "");
        updates.put("published_date", approvedDate);
        updates.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_RESULTS).document(docId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    String effectiveUid = (result.getStudentUid() != null && !result.getStudentUid().isEmpty()) ? result.getStudentUid() : result.getStudentId();

                    // Update local cache
                    dbHelper.publishOrUpdateResult(result.getStudentNumericId(), result.getSemester(),
                            result.getTotalMarks(), result.getPercentage(), result.getSgpa(), result.getCgpa(),
                            STATUS_APPROVED, approvedDate, "SYNCED", effectiveUid);

                    // Notify Teacher
                    dispatchNotificationToUser(result.getTeacherId(), "TEACHER",
                            "RESULT_APPROVED", "Result Approved",
                            "Semester " + result.getSemester() + " result for " + result.getStudentName() + " (" + result.getRegisterNo() + ") was approved by Admin.");

                    // Notify Student
                    dispatchNotificationToUser(effectiveUid, "STUDENT",
                            "RESULT_PUBLISHED", "Semester Result Published",
                            "Your Semester Result has been published.");

                    try {
                        String studentGrade = result.getSgpa() >= 9.0 ? "O" : (result.getSgpa() >= 8.0 ? "A+" : (result.getSgpa() >= 7.0 ? "A" : "B+"));
                        com.example.utils.GradeNotificationHelper.dispatchGradeUploadAlert(
                                context,
                                result.getStudentName(),
                                result.getRegisterNo(),
                                result.getStudentNumericId(),
                                "Semester " + result.getSemester() + " Official Result",
                                "SEM" + result.getSemester(),
                                studentGrade,
                                result.getTotalMarks(),
                                result.getPercentage(),
                                result.getSgpa(),
                                adminName != null ? adminName : "Examination Cell",
                                result.getSemester()
                        );
                    } catch (Exception ignored) {}

                    // Log Audit Trail
                    logAuditTrail("RESULT_APPROVED", docId, result.getStudentId(),
                            result.getStudentName(), result.getSemester(),
                            adminName != null ? adminName : "System Admin", "ADMIN",
                            STATUS_PENDING_APPROVAL, STATUS_APPROVED, result.getVersion(), result.getVersion(),
                            approvalNote != null && !approvalNote.isEmpty() ? approvalNote : "Result approved by Admin.");

                    listener.onSuccess("Result approved successfully. The result is now visible to the student.");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed approving result in Firestore: " + e.getMessage());
                    listener.onError("Failed to approve result: " + e.getMessage());
                });
    }

    // =========================================================================
    // 4. ADMIN: REJECT NEW RESULT
    // =========================================================================

    public void rejectResult(@NonNull Result result, String adminUid, String adminName, @NonNull String rejectionReason, @NonNull OnResultOperationListener listener) {
        if (!isNetworkAvailable()) {
            listener.onError("Internet connection required to reject results.");
            return;
        }

        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            listener.onError("Please provide a rejection reason.");
            return;
        }

        String docId = result.getResultId();

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", STATUS_REJECTED);
        updates.put("rejectedAt", FieldValue.serverTimestamp());
        updates.put("rejectedBy", adminUid != null ? adminUid : "ADMIN");
        updates.put("rejectionReason", rejectionReason.trim());
        updates.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_RESULTS).document(docId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Update local cache
                    dbHelper.publishOrUpdateResult(result.getStudentNumericId(), result.getSemester(),
                            result.getTotalMarks(), result.getPercentage(), result.getSgpa(), result.getCgpa(),
                            STATUS_REJECTED, result.getPublishedDate(), "SYNCED", result.getStudentId());

                    // Notify Teacher with rejection reason
                    dispatchNotificationToUser(result.getTeacherId(), "TEACHER",
                            "RESULT_REJECTED", "Result Submission Rejected",
                            "Semester " + result.getSemester() + " result for " + result.getStudentName() + " was rejected by Admin. Reason: " + rejectionReason);

                    // Log Audit Trail
                    logAuditTrail("RESULT_REJECTED", docId, result.getStudentId(),
                            result.getStudentName(), result.getSemester(),
                            adminName != null ? adminName : "System Admin", "ADMIN",
                            STATUS_PENDING_APPROVAL, STATUS_REJECTED, result.getVersion(), result.getVersion(),
                            rejectionReason.trim());

                    listener.onSuccess("Result rejected. Teacher has been notified with the reason.");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed rejecting result: " + e.getMessage());
                    listener.onError("Failed to reject result: " + e.getMessage());
                });
    }

    // =========================================================================
    // 5. ADMIN: APPROVE RESULT UPDATE
    // =========================================================================

    public void approveResultUpdate(@NonNull ResultUpdateRequest request, String adminUid, String adminName, @NonNull OnResultOperationListener listener) {
        if (!isNetworkAvailable()) {
            listener.onError("Internet connection required to approve result updates.");
            return;
        }

        String reqId = request.getRequestId();
        String parentResultId = request.getResultId();
        int newVersion = request.getNewVersion();
        String approvedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());

        List<Map<String, Object>> newSubMaps = serializeSubjects(request.getNewSubjects());

        WriteBatch batch = db.batch();

        // 1. Update Request document
        Map<String, Object> reqUpdates = new HashMap<>();
        reqUpdates.put("status", STATUS_APPROVED);
        reqUpdates.put("approvedAt", FieldValue.serverTimestamp());
        reqUpdates.put("approvedBy", adminUid != null ? adminUid : "ADMIN");
        reqUpdates.put("updatedAt", FieldValue.serverTimestamp());
        batch.update(db.collection(COLLECTION_UPDATE_REQUESTS).document(reqId), reqUpdates);

        // 2. Atomically update parent Result document
        Map<String, Object> parentUpdates = new HashMap<>();
        parentUpdates.put("version", newVersion);
        parentUpdates.put("totalMarks", request.getNewMarks());
        parentUpdates.put("total_marks", request.getNewMarks());
        parentUpdates.put("percentage", request.getNewPercentage());
        parentUpdates.put("sgpa", request.getNewSgpa());
        parentUpdates.put("cgpa", request.getNewCgpa());
        parentUpdates.put("subjects", newSubMaps);
        parentUpdates.put("status", STATUS_APPROVED);
        parentUpdates.put("approvedBy", adminUid != null ? adminUid : "ADMIN");
        parentUpdates.put("approvedAt", FieldValue.serverTimestamp());
        parentUpdates.put("updatedAt", FieldValue.serverTimestamp());
        parentUpdates.put("published_date", approvedDate);
        if (request.getStudentUid() != null && !request.getStudentUid().isEmpty()) {
            parentUpdates.put("studentUid", request.getStudentUid());
        }
        batch.update(db.collection(COLLECTION_RESULTS).document(parentResultId), parentUpdates);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    String effectiveUid = (request.getStudentUid() != null && !request.getStudentUid().isEmpty()) ? request.getStudentUid() : request.getStudentId();

                    // Update local SQLite marks & results
                    int stId = request.getStudentNumericId();
                    if (stId <= 0) {
                        try { stId = Integer.parseInt(request.getStudentId()); } catch (Exception ignored) {}
                    }
                    if (stId <= 0 && request.getRegisterNo() != null) {
                        com.example.model.Student st = dbHelper.getStudentDetails(request.getRegisterNo());
                        if (st != null) stId = st.getId();
                    }
                    if (stId > 0) {
                        dbHelper.publishOrUpdateResult(stId, request.getSemester(),
                                request.getNewMarks(), request.getNewPercentage(), request.getNewSgpa(), request.getNewCgpa(),
                                STATUS_APPROVED, approvedDate, "SYNCED", effectiveUid);
                    }

                    // Notify Teacher
                    dispatchNotificationToUser(request.getTeacherId(), "TEACHER",
                            "RESULT_UPDATE_APPROVED", "Result Update Approved",
                            "Your result update for " + request.getStudentName() + " (Semester " + request.getSemester() + ") has been approved.");

                    // Notify Student
                    dispatchNotificationToUser(effectiveUid, "STUDENT",
                            "RESULT_UPDATED", "Semester Result Updated",
                            "Your Semester Result has been updated.");

                    // Log Audit Trail
                    logAuditTrail("RESULT_UPDATE_APPROVED", parentResultId, request.getStudentId(),
                            request.getStudentName(), request.getSemester(),
                            adminName != null ? adminName : "System Admin", "ADMIN",
                            STATUS_APPROVED, STATUS_APPROVED, request.getOldVersion(), newVersion,
                            "Approved result update: " + request.getReasonForUpdate());

                    listener.onSuccess("Result update approved and published as version " + newVersion + ".");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Batch commit failed for approving result update: " + e.getMessage());
                    listener.onError("Failed to approve result update: " + e.getMessage());
                });
    }

    // =========================================================================
    // 6. ADMIN: REJECT RESULT UPDATE
    // =========================================================================

    public void rejectResultUpdate(@NonNull ResultUpdateRequest request, String adminUid, String adminName, @NonNull String rejectionReason, @NonNull OnResultOperationListener listener) {
        if (!isNetworkAvailable()) {
            listener.onError("Internet connection required to reject result updates.");
            return;
        }

        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            listener.onError("Please provide a reason for rejecting this update request.");
            return;
        }

        String reqId = request.getRequestId();

        Map<String, Object> reqUpdates = new HashMap<>();
        reqUpdates.put("status", STATUS_UPDATE_REJECTED);
        reqUpdates.put("rejectedAt", FieldValue.serverTimestamp());
        reqUpdates.put("rejectedBy", adminUid != null ? adminUid : "ADMIN");
        reqUpdates.put("rejectionReason", rejectionReason.trim());
        reqUpdates.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_UPDATE_REQUESTS).document(reqId).update(reqUpdates)
                .addOnSuccessListener(aVoid -> {
                    // Parent result in results collection remains UNTOUCHED (v1 stays published to student).

                    // Notify Teacher
                    dispatchNotificationToUser(request.getTeacherId(), "TEACHER",
                            "RESULT_UPDATE_REJECTED", "Result Update Rejected",
                            "Your result update request for " + request.getStudentName() + " was rejected by Admin. Reason: " + rejectionReason);

                    // Log Audit Trail
                    logAuditTrail("RESULT_UPDATE_REJECTED", request.getResultId(), request.getStudentId(),
                            request.getStudentName(), request.getSemester(),
                            adminName != null ? adminName : "System Admin", "ADMIN",
                            STATUS_UPDATE_PENDING_APPROVAL, STATUS_UPDATE_REJECTED,
                            request.getOldVersion(), request.getOldVersion(),
                            rejectionReason.trim());

                    listener.onSuccess("Result update rejected. Student continues viewing current approved result.");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed rejecting result update: " + e.getMessage());
                    listener.onError("Failed to reject result update: " + e.getMessage());
                });
    }

    // =========================================================================
    // 7. FETCHING & QUERIES
    // =========================================================================

    /**
     * Fetches a single result by studentId and semester from Firestore.
     */
    public void fetchResult(int studentId, int semester, @NonNull OnSingleResultLoadedListener listener) {
        String docId = studentId + "_" + semester;
        db.collection(COLLECTION_RESULTS).document(docId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && snapshot.exists()) {
                        Result result = mapDocumentToResult(snapshot);
                        listener.onLoaded(result);
                    } else {
                        // Document does not exist / deleted in Firestore: prune from local cache
                        dbHelper.deleteResult(studentId, semester);
                        listener.onLoaded(null);
                    }
                })
                .addOnFailureListener(e -> {
                    // Network failure / offline: fallback to local cache
                    Result local = dbHelper.getResultForStudentAndSemester(studentId, semester);
                    listener.onLoaded(local);
                });
    }

    /**
     * Queries pending result approval requests for Admin.
     */
    public void fetchPendingResults(@NonNull OnResultsLoadedListener listener) {
        db.collection(COLLECTION_RESULTS)
                .whereEqualTo("status", STATUS_PENDING_APPROVAL)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Result> list = new ArrayList<>();
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Result r = mapDocumentToResult(doc);
                            if (r != null) list.add(r);
                        }
                    }
                    listener.onLoaded(list);
                })
                .addOnFailureListener(e -> listener.onError("Failed fetching pending results: " + e.getMessage()));
    }

    /**
     * Queries all approved results for Admin monitoring.
     */
    public void fetchApprovedResults(@NonNull OnResultsLoadedListener listener) {
        db.collection(COLLECTION_RESULTS)
                .whereEqualTo("status", STATUS_APPROVED)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Result> list = new ArrayList<>();
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Result r = mapDocumentToResult(doc);
                            if (r != null) list.add(r);
                        }
                    }
                    listener.onLoaded(list);
                })
                .addOnFailureListener(e -> listener.onError("Failed fetching approved results: " + e.getMessage()));
    }

    /**
     * Queries pending result update requests for Admin.
     */
    public void fetchPendingUpdateRequests(@NonNull OnUpdateRequestsLoadedListener listener) {
        db.collection(COLLECTION_UPDATE_REQUESTS)
                .whereEqualTo("status", STATUS_UPDATE_PENDING_APPROVAL)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ResultUpdateRequest> list = new ArrayList<>();
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            ResultUpdateRequest req = mapDocumentToUpdateRequest(doc);
                            if (req != null) list.add(req);
                        }
                    }
                    listener.onLoaded(list);
                })
                .addOnFailureListener(e -> listener.onError("Failed fetching update requests: " + e.getMessage()));
    }

    /**
     * Queries APPROVED results for a specific student using their cloud authorization studentUid (Firebase Auth UID).
     * Guaranteed never to return PENDING_APPROVAL, REJECTED, or unapproved update results.
     */
    public void fetchStudentApprovedResult(String studentUid, int semester, @NonNull OnSingleResultLoadedListener listener) {
        if (studentUid == null || studentUid.trim().isEmpty()) {
            listener.onError("Invalid student authorization UID.");
            return;
        }

        final String cleanUid = studentUid.trim();

        // 1. Primary query: whereEqualTo("studentUid", cleanUid)
        db.collection(COLLECTION_RESULTS)
                .whereEqualTo("studentUid", cleanUid)
                .whereEqualTo("semester", semester)
                .get()
                .addOnSuccessListener(q -> {
                    if (q != null && !q.isEmpty()) {
                        for (DocumentSnapshot doc : q.getDocuments()) {
                            String st = doc.getString("status");
                            if (STATUS_APPROVED.equalsIgnoreCase(st) || "PUBLISHED".equalsIgnoreCase(st)) {
                                listener.onLoaded(mapDocumentToResult(doc));
                                return;
                            }
                        }
                    }

                    // 2. Direct document ID query (e.g. numericId_semester or uid_semester)
                    String docId = cleanUid + "_" + semester;
                    db.collection(COLLECTION_RESULTS).document(docId).get()
                            .addOnSuccessListener(snapshot -> {
                                if (snapshot != null && snapshot.exists()) {
                                    String st = snapshot.getString("status");
                                    if (STATUS_APPROVED.equalsIgnoreCase(st) || "PUBLISHED".equalsIgnoreCase(st)) {
                                        listener.onLoaded(mapDocumentToResult(snapshot));
                                        return;
                                    }
                                }

                                // 3. Backward compatibility query: whereEqualTo("studentId", cleanUid)
                                db.collection(COLLECTION_RESULTS)
                                        .whereEqualTo("studentId", cleanUid)
                                        .whereEqualTo("semester", semester)
                                        .get()
                                        .addOnSuccessListener(legacyQ -> {
                                            if (legacyQ != null && !legacyQ.isEmpty()) {
                                                for (DocumentSnapshot doc : legacyQ.getDocuments()) {
                                                    String st = doc.getString("status");
                                                    if (STATUS_APPROVED.equalsIgnoreCase(st) || "PUBLISHED".equalsIgnoreCase(st)) {
                                                        listener.onLoaded(mapDocumentToResult(doc));
                                                        return;
                                                    }
                                                }
                                            }
                                            deleteLocalApprovedResult(cleanUid, semester);
                                            listener.onLoaded(null);
                                        })
                                        .addOnFailureListener(e -> loadLocalApprovedResult(cleanUid, semester, listener));
                            })
                            .addOnFailureListener(e -> loadLocalApprovedResult(cleanUid, semester, listener));
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "fetchStudentApprovedResult cloud query failed: " + e.getMessage() + ", checking local cache.");
                    loadLocalApprovedResult(cleanUid, semester, listener);
                });
    }

    private void deleteLocalApprovedResult(String studentUid, int semester) {
        int numericId = 0;
        try { numericId = Integer.parseInt(studentUid); } catch (Exception ignored) {}
        if (numericId <= 0) {
            com.example.model.Student st = dbHelper.getStudentByFirebaseUid(studentUid);
            if (st != null) numericId = st.getId();
        }
        if (numericId <= 0) {
            com.example.model.Student st = dbHelper.getStudentDetails(studentUid);
            if (st != null) numericId = st.getId();
        }
        if (numericId > 0) {
            dbHelper.deleteResult(numericId, semester);
        }
        dbHelper.deleteResultByUidOrId(studentUid, semester);
    }

    private void loadLocalApprovedResult(String studentUid, int semester, @NonNull OnSingleResultLoadedListener listener) {
        int numericId = 0;
        try { numericId = Integer.parseInt(studentUid); } catch (Exception ignored) {}
        if (numericId <= 0) {
            com.example.model.Student st = dbHelper.getStudentByFirebaseUid(studentUid);
            if (st != null) numericId = st.getId();
        }
        if (numericId <= 0) {
            com.example.model.Student st = dbHelper.getStudentDetails(studentUid);
            if (st != null) numericId = st.getId();
        }
        Result local = numericId > 0 ? dbHelper.getResultForStudentAndSemester(numericId, semester) : null;
        if (local != null && (STATUS_APPROVED.equalsIgnoreCase(local.getStatus()) || "PUBLISHED".equalsIgnoreCase(local.getStatus()))) {
            listener.onLoaded(local);
        } else {
            listener.onLoaded(null);
        }
    }

    /**
     * Backward-compatibility migration:
     * Backfills missing studentUid on legacy Firestore result documents by resolving
     * the student record via student_id or registerNo.
     */
    public void backfillMissingStudentUids() {
        if (!isNetworkAvailable()) return;
        db.collection(COLLECTION_RESULTS).get().addOnSuccessListener(snapshot -> {
            if (snapshot == null || snapshot.isEmpty()) return;
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                String existingUid = doc.getString("studentUid");
                if (existingUid != null && !existingUid.trim().isEmpty()) {
                    continue; // Already migrated
                }

                String legacyStudentId = doc.getString("studentId");
                Long numericStudentId = doc.getLong("student_id");
                String regNo = doc.getString("registerNo");
                if (regNo == null) regNo = doc.getString("reg_no");

                String resolvedUid = null;
                if (numericStudentId != null && numericStudentId > 0) {
                    resolvedUid = dbHelper.getStudentFirebaseUid(numericStudentId.intValue());
                }
                if ((resolvedUid == null || resolvedUid.isEmpty()) && legacyStudentId != null) {
                    try {
                        int num = Integer.parseInt(legacyStudentId);
                        resolvedUid = dbHelper.getStudentFirebaseUid(num);
                    } catch (Exception ignored) {}
                }
                if ((resolvedUid == null || resolvedUid.isEmpty()) && regNo != null && !regNo.isEmpty()) {
                    resolvedUid = dbHelper.getStudentFirebaseUidByRegNo(regNo);
                }

                if (resolvedUid != null && !resolvedUid.trim().isEmpty()) {
                    Log.i(TAG, "Backfilling studentUid=" + resolvedUid + " for result=" + doc.getId());
                    Map<String, Object> update = new HashMap<>();
                    update.put("studentUid", resolvedUid.trim());
                    db.collection(COLLECTION_RESULTS).document(doc.getId()).update(update);
                } else {
                    Log.w(TAG, "Legacy result " + doc.getId() + " requires manual migration: unambiguous Firebase UID could not be resolved.");
                }
            }
        }).addOnFailureListener(e -> Log.w(TAG, "Backfill migration error: " + e.getMessage()));
    }

    // =========================================================================
    // 8. REALTIME BADGE LISTENERS
    // =========================================================================

    public ListenerRegistration listenPendingResultsCount(@NonNull OnCountListener listener) {
        return db.collection(COLLECTION_RESULTS)
                .whereEqualTo("status", STATUS_PENDING_APPROVAL)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        listener.onError(e.getMessage());
                        return;
                    }
                    int count = (snapshots != null) ? snapshots.size() : 0;
                    listener.onCount(count);
                });
    }

    public ListenerRegistration listenPendingUpdateRequestsCount(@NonNull OnCountListener listener) {
        return db.collection(COLLECTION_UPDATE_REQUESTS)
                .whereEqualTo("status", STATUS_UPDATE_PENDING_APPROVAL)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        listener.onError(e.getMessage());
                        return;
                    }
                    int count = (snapshots != null) ? snapshots.size() : 0;
                    listener.onCount(count);
                });
    }

    // =========================================================================
    // 9. NOTIFICATIONS & AUDIT LOGGING DISPATCHERS
    // =========================================================================

    private void dispatchAdminResultApprovalNotification(Result result, String resultId) {
        String notifId = "notif_res_" + System.currentTimeMillis();
        String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());

        Map<String, Object> notifData = new HashMap<>();
        notifData.put("id", Math.abs(notifId.hashCode()));
        notifData.put("type", "RESULT_APPROVAL_REQUEST");
        notifData.put("title", "New Result Approval Required");
        notifData.put("message", "A teacher has submitted a result for " + result.getStudentName() + " (Sem " + result.getSemester() + ") for approval.");
        notifData.put("date", dateStr);
        notifData.put("target_role", "ADMIN");
        notifData.put("targetRole", "ADMIN");
        notifData.put("category", "ACADEMIC");
        notifData.put("resultId", resultId);
        notifData.put("teacherId", result.getTeacherId());
        notifData.put("studentId", String.valueOf(result.getStudentId()));
        notifData.put("read", false);
        notifData.put("is_read", false);
        notifData.put("createdAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_NOTIFICATIONS).document(notifId).set(notifData)
                .addOnFailureListener(e -> Log.w(TAG, "Failed creating admin notification: " + e.getMessage()));
    }

    private void dispatchAdminResultUpdateNotification(ResultUpdateRequest request, String reqId) {
        String notifId = "notif_upd_" + System.currentTimeMillis();
        String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());

        Map<String, Object> notifData = new HashMap<>();
        notifData.put("id", Math.abs(notifId.hashCode()));
        notifData.put("type", "RESULT_UPDATE_APPROVAL_REQUEST");
        notifData.put("title", "New Result Update Approval Required");
        notifData.put("message", "A teacher has requested an update to an already published result for " + request.getStudentName() + " (Sem " + request.getSemester() + ").");
        notifData.put("date", dateStr);
        notifData.put("target_role", "ADMIN");
        notifData.put("targetRole", "ADMIN");
        notifData.put("category", "ACADEMIC");
        notifData.put("requestId", reqId);
        notifData.put("resultId", request.getResultId());
        notifData.put("teacherId", request.getTeacherId());
        notifData.put("studentId", request.getStudentId());
        notifData.put("read", false);
        notifData.put("is_read", false);
        notifData.put("createdAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_NOTIFICATIONS).document(notifId).set(notifData)
                .addOnFailureListener(e -> Log.w(TAG, "Failed creating admin update notification: " + e.getMessage()));
    }

    private void dispatchNotificationToUser(String targetUserId, String targetRole, String type, String title, String message) {
        String notifId = "notif_" + System.currentTimeMillis() + "_" + Math.abs((targetUserId != null ? targetUserId : "user").hashCode());
        String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());

        Map<String, Object> notifData = new HashMap<>();
        notifData.put("id", Math.abs(notifId.hashCode()));
        notifData.put("type", type);
        notifData.put("title", title);
        notifData.put("message", message);
        notifData.put("date", dateStr);
        notifData.put("target_role", targetRole);
        notifData.put("targetRole", targetRole);
        notifData.put("targetUserId", targetUserId != null ? targetUserId : "");
        notifData.put("category", "ACADEMIC");
        notifData.put("read", false);
        notifData.put("is_read", false);
        notifData.put("createdAt", FieldValue.serverTimestamp());

        // Top-level notifications
        db.collection(COLLECTION_NOTIFICATIONS).document(notifId).set(notifData)
                .addOnFailureListener(e -> Log.w(TAG, "Failed writing notification: " + e.getMessage()));

        // Also write into target user's personal notifications if available
        if (targetUserId != null && !targetUserId.trim().isEmpty()) {
            db.collection("users").document(targetUserId).collection("notifications")
                    .document(notifId)
                    .set(notifData)
                    .addOnFailureListener(e -> Log.w(TAG, "Failed writing user subcollection notification: " + e.getMessage()));
        }

        // Cache in local SQLite
        try {
            dbHelper.addNotification(title, message, dateStr, targetRole);
        } catch (Exception ignored) {}
    }

    private void logAuditTrail(String action, String resultId, String studentId, String studentName,
                               int semester, String performedBy, String performedByRole,
                               String oldStatus, String newStatus, int oldVersion, int newVersion, String reason) {
        String logId = "log_" + System.currentTimeMillis();
        Map<String, Object> log = new HashMap<>();
        log.put("logId", logId);
        log.put("action", action);
        log.put("resultId", resultId != null ? resultId : "");
        log.put("studentId", studentId != null ? studentId : "");
        log.put("studentName", studentName != null ? studentName : "");
        log.put("semester", semester);
        log.put("performedBy", performedBy != null ? performedBy : "System");
        log.put("performedByRole", performedByRole != null ? performedByRole : "USER");
        log.put("timestamp", FieldValue.serverTimestamp());
        log.put("oldStatus", oldStatus != null ? oldStatus : "");
        log.put("newStatus", newStatus != null ? newStatus : "");
        log.put("oldVersion", oldVersion);
        log.put("newVersion", newVersion);
        log.put("reason", reason != null ? reason : "");

        db.collection(COLLECTION_AUDIT_LOGS).document(logId).set(log)
                .addOnFailureListener(e -> Log.w(TAG, "Failed writing audit log: " + e.getMessage()));
    }

    // =========================================================================
    // 10. SERIALIZATION & MAPPING HELPERS
    // =========================================================================

    private List<Map<String, Object>> serializeSubjects(List<SubjectGradeItem> subjects) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (subjects == null) return list;
        for (SubjectGradeItem sub : subjects) {
            Map<String, Object> sm = new HashMap<>();
            sm.put("subjectCode", sub.getSubjectCode());
            sm.put("subjectName", sub.getSubjectName());
            sm.put("credits", sub.getCredits());
            sm.put("internalMarks", sub.getInternalMarks());
            sm.put("internal1", sub.getInternal1());
            sm.put("assignment", sub.getAssignment());
            sm.put("modelExam", sub.getModelExam());
            sm.put("universityExam", sub.getUniversityExam());
            sm.put("totalMarks", sub.getTotalMarks());
            sm.put("percentage", sub.getPercentage());
            sm.put("grade", sub.getGrade());
            sm.put("gradePoint", sub.getGradePoint());
            sm.put("creditPoints", sub.getCreditPoints());
            list.add(sm);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private Result mapDocumentToResult(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        Result r = new Result();
        r.setResultId(doc.getId());

        String sId = doc.getString("studentId");
        if (sId == null || sId.isEmpty()) {
            if (doc.contains("student_id")) {
                Object o = doc.get("student_id");
                sId = o != null ? String.valueOf(o) : "";
            }
        }
        r.setStudentId(sId);

        if (doc.contains("student_id") && doc.get("student_id") instanceof Number) {
            r.setStudentNumericId(((Number) doc.get("student_id")).intValue());
        }

        r.setStudentUid(doc.getString("studentUid"));

        r.setStudentName(doc.getString("studentName"));
        String reg = doc.getString("registerNo");
        if (reg == null) reg = doc.getString("reg_no");
        r.setRegisterNo(reg);

        r.setProgramLevel(doc.getString("programLevel"));
        r.setDepartmentId(doc.getString("departmentId"));
        String dept = doc.getString("departmentName");
        if (dept == null) dept = doc.getString("department");
        r.setDepartmentName(dept);

        int sem = 1;
        if (doc.contains("semester") && doc.get("semester") instanceof Number) {
            sem = ((Number) doc.get("semester")).intValue();
        }
        r.setSemester(sem);

        r.setAcademicYear(doc.getString("academicYear"));

        double total = doc.contains("totalMarks") && doc.get("totalMarks") instanceof Number ? ((Number) doc.get("totalMarks")).doubleValue() : 0.0;
        if (total == 0 && doc.contains("total_marks") && doc.get("total_marks") instanceof Number) {
            total = ((Number) doc.get("total_marks")).doubleValue();
        }
        r.setTotalMarks(total);

        r.setPercentage(doc.contains("percentage") && doc.get("percentage") instanceof Number ? ((Number) doc.get("percentage")).doubleValue() : 0.0);
        r.setSgpa(doc.contains("sgpa") && doc.get("sgpa") instanceof Number ? ((Number) doc.get("sgpa")).doubleValue() : 0.0);
        r.setCgpa(doc.contains("cgpa") && doc.get("cgpa") instanceof Number ? ((Number) doc.get("cgpa")).doubleValue() : 0.0);

        r.setStatus(doc.getString("status"));

        int v = 1;
        if (doc.contains("version") && doc.get("version") instanceof Number) {
            v = ((Number) doc.get("version")).intValue();
        }
        r.setVersion(v);

        r.setTeacherId(doc.getString("teacherId"));
        r.setTeacherName(doc.getString("teacherName"));
        r.setPublishedBy(doc.getString("publishedBy"));
        r.setPublishedDate(doc.getString("published_date"));

        r.setApprovedBy(doc.getString("approvedBy"));
        r.setApprovalNote(doc.getString("approvalNote"));
        r.setRejectedBy(doc.getString("rejectedBy"));
        r.setRejectionReason(doc.getString("rejectionReason"));

        r.setSubmittedAt(doc.get("submittedAt"));
        r.setApprovedAt(doc.get("approvedAt"));
        r.setRejectedAt(doc.get("rejectedAt"));

        // Deserialize subjects
        List<SubjectGradeItem> subjects = new ArrayList<>();
        if (doc.contains("subjects") && doc.get("subjects") instanceof List) {
            List<Map<String, Object>> list = (List<Map<String, Object>>) doc.get("subjects");
            for (Map<String, Object> m : list) {
                SubjectGradeItem item = new SubjectGradeItem();
                if (m.get("subjectCode") != null) item.setSubjectCode(m.get("subjectCode").toString());
                if (m.get("subjectName") != null) item.setSubjectName(m.get("subjectName").toString());
                if (m.get("credits") instanceof Number) item.setCredits(((Number) m.get("credits")).intValue());
                if (m.get("internalMarks") instanceof Number) item.setInternalMarks(((Number) m.get("internalMarks")).doubleValue());
                if (m.get("internal1") instanceof Number) item.setInternal1(((Number) m.get("internal1")).doubleValue());
                if (m.get("assignment") instanceof Number) item.setAssignment(((Number) m.get("assignment")).doubleValue());
                if (m.get("modelExam") instanceof Number) item.setModelExam(((Number) m.get("modelExam")).doubleValue());
                if (m.get("universityExam") instanceof Number) item.setUniversityExam(((Number) m.get("universityExam")).doubleValue());
                if (m.get("totalMarks") instanceof Number) item.setTotalMarks(((Number) m.get("totalMarks")).doubleValue());
                if (m.get("percentage") instanceof Number) item.setPercentage(((Number) m.get("percentage")).doubleValue());
                if (m.get("grade") != null) item.setGrade(m.get("grade").toString());
                if (m.get("gradePoint") instanceof Number) item.setGradePoint(((Number) m.get("gradePoint")).intValue());
                subjects.add(item);
            }
        }
        r.setSubjects(subjects);

        return r;
    }

    @SuppressWarnings("unchecked")
    private ResultUpdateRequest mapDocumentToUpdateRequest(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        ResultUpdateRequest req = new ResultUpdateRequest();
        req.setRequestId(doc.getId());
        req.setResultId(doc.getString("resultId"));
        req.setStudentId(doc.getString("studentId"));
        if (doc.contains("student_id") && doc.get("student_id") instanceof Number) {
            req.setStudentNumericId(((Number) doc.get("student_id")).intValue());
        }
        req.setStudentUid(doc.getString("studentUid"));
        req.setStudentName(doc.getString("studentName"));
        req.setRegisterNo(doc.getString("registerNo"));
        req.setProgramLevel(doc.getString("programLevel"));
        req.setDepartmentId(doc.getString("departmentId"));
        req.setDepartmentName(doc.getString("departmentName"));

        if (doc.contains("semester") && doc.get("semester") instanceof Number) {
            req.setSemester(((Number) doc.get("semester")).intValue());
        }
        req.setAcademicYear(doc.getString("academicYear"));

        if (doc.contains("oldVersion") && doc.get("oldVersion") instanceof Number) {
            req.setOldVersion(((Number) doc.get("oldVersion")).intValue());
        }
        if (doc.contains("newVersion") && doc.get("newVersion") instanceof Number) {
            req.setNewVersion(((Number) doc.get("newVersion")).intValue());
        }

        if (doc.contains("oldMarks") && doc.get("oldMarks") instanceof Number) req.setOldMarks(((Number) doc.get("oldMarks")).doubleValue());
        if (doc.contains("newMarks") && doc.get("newMarks") instanceof Number) req.setNewMarks(((Number) doc.get("newMarks")).doubleValue());
        if (doc.contains("oldPercentage") && doc.get("oldPercentage") instanceof Number) req.setOldPercentage(((Number) doc.get("oldPercentage")).doubleValue());
        if (doc.contains("newPercentage") && doc.get("newPercentage") instanceof Number) req.setNewPercentage(((Number) doc.get("newPercentage")).doubleValue());
        if (doc.contains("oldSgpa") && doc.get("oldSgpa") instanceof Number) req.setOldSgpa(((Number) doc.get("oldSgpa")).doubleValue());
        if (doc.contains("newSgpa") && doc.get("newSgpa") instanceof Number) req.setNewSgpa(((Number) doc.get("newSgpa")).doubleValue());
        if (doc.contains("oldCgpa") && doc.get("oldCgpa") instanceof Number) req.setOldCgpa(((Number) doc.get("oldCgpa")).doubleValue());
        if (doc.contains("newCgpa") && doc.get("newCgpa") instanceof Number) req.setNewCgpa(((Number) doc.get("newCgpa")).doubleValue());

        req.setOldGrades(doc.getString("oldGrades"));
        req.setNewGrades(doc.getString("newGrades"));

        req.setReasonForUpdate(doc.getString("reasonForUpdate"));
        req.setTeacherId(doc.getString("teacherId"));
        req.setTeacherName(doc.getString("teacherName"));
        req.setUpdatedBy(doc.getString("updatedBy"));
        req.setStatus(doc.getString("status"));

        req.setSubmittedAt(doc.get("submittedAt"));
        req.setApprovedAt(doc.get("approvedAt"));
        req.setApprovedBy(doc.getString("approvedBy"));
        req.setRejectedAt(doc.get("rejectedAt"));
        req.setRejectedBy(doc.getString("rejectedBy"));
        req.setRejectionReason(doc.getString("rejectionReason"));

        // Deserialize old subjects
        if (doc.contains("oldSubjects") && doc.get("oldSubjects") instanceof List) {
            req.setOldSubjects(deserializeSubjectsList((List<Map<String, Object>>) doc.get("oldSubjects")));
        }
        // Deserialize new subjects
        if (doc.contains("newSubjects") && doc.get("newSubjects") instanceof List) {
            req.setNewSubjects(deserializeSubjectsList((List<Map<String, Object>>) doc.get("newSubjects")));
        }

        return req;
    }

    private List<SubjectGradeItem> deserializeSubjectsList(List<Map<String, Object>> list) {
        List<SubjectGradeItem> subjects = new ArrayList<>();
        if (list == null) return subjects;
        for (Map<String, Object> m : list) {
            SubjectGradeItem item = new SubjectGradeItem();
            if (m.get("subjectCode") != null) item.setSubjectCode(m.get("subjectCode").toString());
            if (m.get("subjectName") != null) item.setSubjectName(m.get("subjectName").toString());
            if (m.get("credits") instanceof Number) item.setCredits(((Number) m.get("credits")).intValue());
            if (m.get("internalMarks") instanceof Number) item.setInternalMarks(((Number) m.get("internalMarks")).doubleValue());
            if (m.get("internal1") instanceof Number) item.setInternal1(((Number) m.get("internal1")).doubleValue());
            if (m.get("assignment") instanceof Number) item.setAssignment(((Number) m.get("assignment")).doubleValue());
            if (m.get("modelExam") instanceof Number) item.setModelExam(((Number) m.get("modelExam")).doubleValue());
            if (m.get("universityExam") instanceof Number) item.setUniversityExam(((Number) m.get("universityExam")).doubleValue());
            if (m.get("totalMarks") instanceof Number) item.setTotalMarks(((Number) m.get("totalMarks")).doubleValue());
            if (m.get("percentage") instanceof Number) item.setPercentage(((Number) m.get("percentage")).doubleValue());
            if (m.get("grade") != null) item.setGrade(m.get("grade").toString());
            if (m.get("gradePoint") instanceof Number) item.setGradePoint(((Number) m.get("gradePoint")).intValue());
            subjects.add(item);
        }
        return subjects;
    }
}
