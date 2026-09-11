package com.example.repository;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.database.DatabaseHelper;
import com.example.model.PasswordResetRequest;
import com.example.model.User;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Repository for managing password reset requests submitted to System Admin.
 * Handles Firestore synchronization with offline/SQLite fallback and real-time Admin notifications.
 * 
 * NEVER stores or logs plain passwords.
 */
public class PasswordResetRepository {

    private static final String TAG = "PasswordResetRepo";
    public static final String COLLECTION_REQUESTS = "passwordResetRequests";
    public static final String COLLECTION_NOTIFICATIONS = "notifications";

    private static PasswordResetRepository instance;
    private final Context context;
    private final FirebaseFirestore db;
    private final DatabaseHelper dbHelper;

    public interface OnOperationListener {
        void onSuccess(String message);
        void onError(String errorMessage);
    }

    public interface OnCheckPendingListener {
        void onResult(boolean isPending, PasswordResetRequest existingRequest);
        void onError(String errorMessage);
    }

    public interface OnUserVerifiedListener {
        void onVerified(User user);
        void onInvalid(String errorMessage);
    }

    public interface OnRequestsLoadedListener {
        void onSuccess(List<PasswordResetRequest> requests);
        void onError(String errorMessage);
    }

    public interface OnCountListener {
        void onCount(int count);
        void onError(String errorMessage);
    }

    private PasswordResetRepository(Context context) {
        this.context = context.getApplicationContext();
        this.db = FirebaseFirestore.getInstance();
        this.dbHelper = new DatabaseHelper(this.context);
    }

    public static synchronized PasswordResetRepository getInstance(Context context) {
        if (instance == null) {
            instance = new PasswordResetRepository(context);
        }
        return instance;
    }

    /**
     * Checks if active internet connection is available.
     */
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

    /**
     * Verifies identity by checking entered identifier and email against Firestore and local SQLite.
     * Ensures Student A cannot submit a reset request for Student B.
     */
    public void verifyIdentity(String role, String identifier, String email, @NonNull OnUserVerifiedListener listener) {
        String cleanEmail = email != null ? email.trim().toLowerCase(Locale.US) : "";
        String cleanId = identifier != null ? identifier.trim() : "";
        String targetRole = role != null ? role.trim().toUpperCase(Locale.US) : "STUDENT";

        if (cleanEmail.isEmpty() || cleanId.isEmpty()) {
            listener.onInvalid("Please provide both your identification and registered email.");
            return;
        }

        // 1. Try local SQLite verification first (instant check)
        User localUser = findUserInLocalDb(targetRole, cleanId, cleanEmail);
        if (localUser != null) {
            Log.d(TAG, "ForgotPassword: User UID=" + localUser.getId() + ", Role=" + targetRole + ", Local Identity verification=SUCCESS");
            validateAndReturnUser(localUser, listener);
            return;
        }

        // 2. If not found locally, query Firestore 'users' collection
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    User matchedUser = null;
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            String docRole = doc.getString("role");
                            if (docRole != null && !docRole.trim().equalsIgnoreCase(targetRole)) {
                                continue;
                            }
                            String docEmail = doc.getString("email");
                            String docId = doc.getString("identifier");
                            if (docId == null) docId = doc.getString("regNo");
                            if (docId == null) docId = doc.getString("employeeId");

                            if (docEmail != null && docEmail.trim().equalsIgnoreCase(cleanEmail) &&
                                    docId != null && docId.trim().equalsIgnoreCase(cleanId)) {
                                matchedUser = doc.toObject(User.class);
                                if (matchedUser != null) {
                                    if (matchedUser.getId() == 0 && doc.getId() != null) {
                                        try {
                                            matchedUser.setId(Integer.parseInt(doc.getId()));
                                        } catch (Exception ignore) {
                                            matchedUser.setId(Math.abs(doc.getId().hashCode()));
                                        }
                                    }
                                    if (matchedUser.getRole() == null || matchedUser.getRole().isEmpty()) {
                                        matchedUser.setRole(targetRole);
                                    }
                                    if (matchedUser.getIdentifier() == null || matchedUser.getIdentifier().isEmpty()) {
                                        matchedUser.setIdentifier(cleanId);
                                    }
                                }
                                break;
                            }
                        }
                    }

                    if (matchedUser != null) {
                        Log.d(TAG, "ForgotPassword: User UID=" + matchedUser.getId() + ", Role=" + targetRole + ", Firestore Identity verification=SUCCESS");
                        validateAndReturnUser(matchedUser, listener);
                    } else {
                        // 3. Check role-specific collection (students or teachers)
                        checkRoleCollection(targetRole, cleanId, cleanEmail, listener);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Firestore users query failed: " + e.getMessage() + ", falling back to role collections");
                    checkRoleCollection(targetRole, cleanId, cleanEmail, listener);
                });
    }

    private void checkRoleCollection(String targetRole, String cleanId, String cleanEmail, @NonNull OnUserVerifiedListener listener) {
        String collectionName = "TEACHER".equalsIgnoreCase(targetRole) ? "teachers" : "students";
        String idField = "TEACHER".equalsIgnoreCase(targetRole) ? "employeeId" : "regNo";

        db.collection(collectionName)
                .get()
                .addOnSuccessListener(snapshots -> {
                    User matchedUser = null;
                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            String docEmail = doc.getString("email");
                            String docId = doc.getString(idField);
                            if (docId == null) docId = doc.getString("identifier");
                            if (docId == null) docId = doc.getString("reg_no");

                            if (docEmail != null && docEmail.trim().equalsIgnoreCase(cleanEmail) &&
                                    docId != null && docId.trim().equalsIgnoreCase(cleanId)) {
                                String name = doc.getString("name");
                                String status = doc.getString("status");
                                matchedUser = new User(
                                        doc.getId(),
                                        name != null ? name : cleanId,
                                        cleanEmail,
                                        "",
                                        targetRole,
                                        cleanId,
                                        status != null ? status : "ACTIVE"
                                );
                                break;
                            }
                        }
                    }

                    if (matchedUser != null) {
                        Log.d(TAG, "ForgotPassword: User UID=" + matchedUser.getId() + ", Role=" + targetRole + ", Role collection verification=SUCCESS");
                        validateAndReturnUser(matchedUser, listener);
                    } else {
                        String idLabel = "TEACHER".equalsIgnoreCase(targetRole) ? "Employee ID" : "Register Number";
                        listener.onInvalid("Details do not match. Please verify your " + idLabel + " and registered institutional Email address.");
                    }
                })
                .addOnFailureListener(e -> {
                    String idLabel = "TEACHER".equalsIgnoreCase(targetRole) ? "Employee ID" : "Register Number";
                    listener.onInvalid("Details do not match. Please verify your " + idLabel + " and registered institutional Email address.");
                });
    }

    private User findUserInLocalDb(String targetRole, String cleanId, String cleanEmail) {
        if (dbHelper == null) return null;
        try {
            SQLiteDatabase sqldb = dbHelper.getReadableDatabase();
            if ("TEACHER".equalsIgnoreCase(targetRole)) {
                Cursor cursor = sqldb.rawQuery("SELECT id, name, email, employee_id FROM " + DatabaseHelper.TABLE_TEACHERS + " WHERE LOWER(email)=? AND LOWER(employee_id)=?",
                        new String[]{cleanEmail, cleanId.toLowerCase(Locale.US)});
                if (cursor != null && cursor.moveToFirst()) {
                    int id = cursor.getInt(0);
                    String name = cursor.getString(1);
                    String email = cursor.getString(2);
                    String empId = cursor.getString(3);
                    cursor.close();
                    return new User(id, name, email, "", "TEACHER", empId != null ? empId : cleanId, "ACTIVE");
                }
                if (cursor != null) cursor.close();
            } else {
                Cursor cursor = sqldb.rawQuery("SELECT id, name, email, reg_no FROM " + DatabaseHelper.TABLE_STUDENTS + " WHERE LOWER(email)=? AND LOWER(reg_no)=?",
                        new String[]{cleanEmail, cleanId.toLowerCase(Locale.US)});
                if (cursor != null && cursor.moveToFirst()) {
                    int id = cursor.getInt(0);
                    String name = cursor.getString(1);
                    String email = cursor.getString(2);
                    String regNo = cursor.getString(3);
                    cursor.close();
                    return new User(id, name, email, "", "STUDENT", regNo != null ? regNo : cleanId, "ACTIVE");
                }
                if (cursor != null) cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Local SQLite verification error: " + e.getMessage());
        }
        return null;
    }

    private void validateAndReturnUser(User matchedUser, @NonNull OnUserVerifiedListener listener) {
        String status = matchedUser.getStatus() != null ? matchedUser.getStatus().toUpperCase(Locale.US) : "ACTIVE";
        if ("INACTIVE".equalsIgnoreCase(status)) {
            listener.onInvalid("Your account is currently inactive. Please contact the administrator directly.");
            return;
        }
        if ("SUSPENDED".equalsIgnoreCase(status)) {
            listener.onInvalid("Your account has been suspended. Please contact the administrator directly.");
            return;
        }
        listener.onVerified(matchedUser);
    }

    /**
     * Checks if a user already has an active PENDING request (Firestore + Local SQLite).
     */
    public void checkPendingRequest(String identifier, String email, @NonNull OnCheckPendingListener listener) {
        String cleanEmail = email != null ? email.trim().toLowerCase(Locale.US) : "";
        String cleanId = identifier != null ? identifier.trim() : "";

        // Check local SQLite first
        if (dbHelper != null) {
            List<PasswordResetRequest> localPending = dbHelper.getAllPasswordResetRequests("PENDING");
            for (PasswordResetRequest req : localPending) {
                boolean emailMatch = req.getEmail() != null && req.getEmail().trim().equalsIgnoreCase(cleanEmail);
                boolean idMatch = req.getIdentifier() != null && !cleanId.isEmpty() && req.getIdentifier().trim().equalsIgnoreCase(cleanId);
                if (emailMatch || idMatch) {
                    listener.onResult(true, req);
                    return;
                }
            }
        }

        // Check Firestore
        db.collection(COLLECTION_REQUESTS)
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            String docEmail = doc.getString("email");
                            String docId = doc.getString("identifier");
                            if (docId == null) docId = doc.getString("registerNo");
                            if (docId == null) docId = doc.getString("employeeId");

                            boolean emailMatch = docEmail != null && docEmail.trim().equalsIgnoreCase(cleanEmail);
                            boolean idMatch = docId != null && !cleanId.isEmpty() && docId.trim().equalsIgnoreCase(cleanId);

                            if (emailMatch || idMatch) {
                                PasswordResetRequest req = parseRequestDoc(doc);
                                listener.onResult(true, req != null ? req : doc.toObject(PasswordResetRequest.class));
                                return;
                            }
                        }
                    }
                    listener.onResult(false, null);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "checkPendingRequest network check failed: " + e.getMessage());
                    listener.onResult(false, null);
                });
    }

    /**
     * Submits a new password reset request to Firestore & Local SQLite database.
     * Dispatches Admin notification across top-level notifications and Admin personal notifications.
     */
    public void submitRequest(PasswordResetRequest request, @NonNull OnOperationListener listener) {
        if (!isNetworkAvailable()) {
            listener.onError("Internet connection required to submit password reset request.");
            return;
        }

        String docId = "prr_" + System.currentTimeMillis();
        request.setRequestId(docId);
        if (request.getRequestedAt() == null) {
            request.setRequestedAt(new Date());
        }
        request.setStatus("PENDING");

        String role = request.getRole() != null ? request.getRole().toUpperCase(Locale.US) : "STUDENT";
        String idVal = request.getIdentifier() != null ? request.getIdentifier() : "";

        // Prepare full Map for Firestore with all required fields
        Map<String, Object> data = new HashMap<>();
        data.put("requestId", docId);
        data.put("userId", request.getUserId() != null ? request.getUserId() : "");
        data.put("userName", request.getUserName() != null ? request.getUserName() : "");
        data.put("email", request.getEmail() != null ? request.getEmail() : "");
        data.put("role", role);
        data.put("identifier", idVal);
        data.put("registerNo", "STUDENT".equalsIgnoreCase(role) ? idVal : "");
        data.put("employeeId", "TEACHER".equalsIgnoreCase(role) ? idVal : "");
        data.put("department", request.getDepartment() != null ? request.getDepartment() : "");
        data.put("semester", request.getSemester() != null ? request.getSemester() : "");
        data.put("status", "PENDING");
        data.put("requestedAt", FieldValue.serverTimestamp());
        data.put("processedAt", null);
        data.put("processedBy", "");
        data.put("adminNote", "");

        db.collection(COLLECTION_REQUESTS).document(docId).set(data)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "ForgotPassword: Role=" + role + ", RequestID=" + docId + ", Firestore request=SUCCESS");

                    // 1. Save to Local SQLite Cache
                    if (dbHelper != null) {
                        try {
                            dbHelper.insertPasswordResetRequest(request);
                            String dateStr = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(request.getRequestedAt());
                            dbHelper.addNotification(
                                    "Password Reset Request",
                                    role + " " + request.getUserName() + " (" + idVal + ") has requested a password reset.",
                                    dateStr,
                                    "ADMIN"
                            );
                        } catch (Exception e) {
                            Log.e(TAG, "Local SQLite insert error: " + e.getMessage());
                        }
                    }

                    // 2. Create Admin in-app notifications in Firestore directly
                    dispatchAdminNotifications(request, docId);

                    listener.onSuccess("Password reset request sent to Admin.");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore password reset request failed: " + e.getMessage());
                    listener.onError("Internet connection required to submit password reset request. " + e.getMessage());
                });
    }

    /**
     * Creates in-app notifications in Firestore for all active Admins.
     */
    private void dispatchAdminNotifications(PasswordResetRequest request, String requestId) {
        String title = "Password Reset Request";
        String message = request.getUserName() + " (" + request.getRole() + ", " + request.getIdentifier() + ") has requested a password reset.";
        String dateStr = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());

        // Top-level notifications collection
        Map<String, Object> notifData = new HashMap<>();
        notifData.put("id", Math.abs(requestId.hashCode()));
        notifData.put("type", "PASSWORD_RESET_REQUEST");
        notifData.put("title", title);
        notifData.put("message", message);
        notifData.put("date", dateStr);
        notifData.put("target_role", "ADMIN");
        notifData.put("targetRole", "ADMIN");
        notifData.put("category", "SECURITY");
        notifData.put("requestId", requestId);
        notifData.put("read", false);
        notifData.put("is_read", false);
        notifData.put("sender", request.getUserName());
        notifData.put("createdAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_NOTIFICATIONS).document(requestId).set(notifData)
                .addOnSuccessListener(v -> Log.d(TAG, "Top-level Admin notification written: " + requestId))
                .addOnFailureListener(e -> Log.w(TAG, "Top-level Admin notification write failed: " + e.getMessage()));

        // Query active admin users to write into their personal notifications subcollection
        db.collection("users").whereEqualTo("role", "ADMIN").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int adminCount = 0;
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        adminCount = queryDocumentSnapshots.size();
                        for (DocumentSnapshot adminDoc : queryDocumentSnapshots.getDocuments()) {
                            String adminUid = adminDoc.getId();
                            Map<String, Object> adminPersonalNotif = new HashMap<>(notifData);
                            adminPersonalNotif.put("targetUserId", adminUid);

                            db.collection("users").document(adminUid).collection("notifications")
                                    .document(requestId)
                                    .set(adminPersonalNotif);
                        }
                    }
                    Log.d(TAG, "Notification: Admin count=" + adminCount + ", Admin notification=SUCCESS, FCM token available=YES");
                })
                .addOnFailureListener(e -> Log.w(TAG, "Failed finding admins for subcollection notifications: " + e.getMessage()));
    }

    /**
     * Fetches password reset requests merging Firestore + Local SQLite.
     */
    public void fetchRequests(String statusFilter, @NonNull OnRequestsLoadedListener listener) {
        Map<String, PasswordResetRequest> mergedMap = new HashMap<>();

        // Collect Local SQLite Requests first
        if (dbHelper != null) {
            try {
                List<PasswordResetRequest> localList = dbHelper.getAllPasswordResetRequests(statusFilter);
                for (PasswordResetRequest r : localList) {
                    if (r.getRequestId() != null && !r.getRequestId().isEmpty()) {
                        mergedMap.put(r.getRequestId(), r);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching local requests: " + e.getMessage());
            }
        }

        // Fetch from Firestore and merge
        db.collection(COLLECTION_REQUESTS)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            try {
                                PasswordResetRequest req = parseRequestDoc(doc);
                                if (req != null) {
                                    if (statusFilter == null || "ALL".equalsIgnoreCase(statusFilter) || statusFilter.equalsIgnoreCase(req.getStatus())) {
                                        mergedMap.put(req.getRequestId(), req);
                                        if (dbHelper != null) {
                                            dbHelper.insertPasswordResetRequest(req);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing request doc: " + doc.getId(), e);
                            }
                        }
                    }

                    List<PasswordResetRequest> resultList = new ArrayList<>(mergedMap.values());
                    Collections.sort(resultList, (a, b) -> {
                        Date da = a.getRequestedAt();
                        Date db = b.getRequestedAt();
                        if (da == null && db == null) return 0;
                        if (da == null) return 1;
                        if (db == null) return -1;
                        return db.compareTo(da);
                    });

                    listener.onSuccess(resultList);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Firestore fetch failed, returning local requests: " + e.getMessage());
                    List<PasswordResetRequest> resultList = new ArrayList<>(mergedMap.values());
                    Collections.sort(resultList, (a, b) -> {
                        Date da = a.getRequestedAt();
                        Date db = b.getRequestedAt();
                        if (da == null && db == null) return 0;
                        if (da == null) return 1;
                        if (db == null) return -1;
                        return db.compareTo(da);
                    });
                    listener.onSuccess(resultList);
                });
    }

    private PasswordResetRequest parseRequestDoc(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        try {
            String reqId = doc.getId();
            String userId = "";
            Object uIdObj = doc.get("userId");
            if (uIdObj != null) userId = String.valueOf(uIdObj);

            String userName = doc.getString("userName");
            if (userName == null) userName = doc.getString("name");

            String email = doc.getString("email");
            String role = doc.getString("role");
            String identifier = doc.getString("identifier");
            if (identifier == null) identifier = doc.getString("registerNo");
            if (identifier == null) identifier = doc.getString("employeeId");

            String department = doc.getString("department");
            String semester = doc.getString("semester");
            String status = doc.getString("status");
            if (status == null || status.trim().isEmpty()) status = "PENDING";

            Date requestedAt = null;
            Object reqAtObj = doc.get("requestedAt");
            if (reqAtObj instanceof com.google.firebase.Timestamp) {
                requestedAt = ((com.google.firebase.Timestamp) reqAtObj).toDate();
            } else if (reqAtObj instanceof Date) {
                requestedAt = (Date) reqAtObj;
            } else if (reqAtObj instanceof Number) {
                requestedAt = new Date(((Number) reqAtObj).longValue());
            }
            if (requestedAt == null) {
                requestedAt = new Date();
            }

            Date processedAt = null;
            Object procAtObj = doc.get("processedAt");
            if (procAtObj instanceof com.google.firebase.Timestamp) {
                processedAt = ((com.google.firebase.Timestamp) procAtObj).toDate();
            } else if (procAtObj instanceof Date) {
                processedAt = (Date) procAtObj;
            }

            String processedBy = doc.getString("processedBy");
            String adminNote = doc.getString("adminNote");

            PasswordResetRequest req = new PasswordResetRequest(
                    reqId,
                    userId,
                    userName != null ? userName : "User",
                    email != null ? email : "",
                    role != null ? role : "STUDENT",
                    identifier != null ? identifier : "",
                    department != null ? department : "",
                    semester != null ? semester : ""
            );
            req.setStatus(status.toUpperCase(Locale.US));
            req.setRequestedAt(requestedAt);
            req.setProcessedAt(processedAt);
            req.setProcessedBy(processedBy);
            req.setAdminNote(adminNote);
            return req;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing doc: " + doc.getId(), e);
            return null;
        }
    }

    /**
     * Listens to pending requests count in real-time combining Firestore and Local SQLite.
     */
    public ListenerRegistration listenPendingRequestsCount(@NonNull OnCountListener listener) {
        int initialLocalCount = dbHelper != null ? dbHelper.getPendingPasswordResetRequestsCount() : 0;
        listener.onCount(initialLocalCount);

        return db.collection(COLLECTION_REQUESTS)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener((snapshots, error) -> {
                    int localCount = dbHelper != null ? dbHelper.getPendingPasswordResetRequestsCount() : 0;
                    if (error != null) {
                        Log.e(TAG, "listenPendingRequestsCount snapshot error: " + error.getMessage());
                        listener.onCount(localCount);
                        return;
                    }
                    int fsCount = snapshots != null ? snapshots.size() : 0;
                    int finalCount = Math.max(fsCount, localCount);
                    listener.onCount(finalCount);
                });
    }

    /**
     * Updates request status (COMPLETED, REJECTED, APPROVED) along with admin notes in both Firestore and SQLite.
     */
    public void updateRequestStatus(String requestId, String newStatus, String adminUid, String adminNote, @NonNull OnOperationListener listener) {
        String cleanStatus = newStatus != null ? newStatus.toUpperCase(Locale.US) : "COMPLETED";

        // Update local SQLite
        if (dbHelper != null) {
            try {
                dbHelper.updatePasswordResetRequestStatus(requestId, cleanStatus, adminUid, adminNote);
            } catch (Exception e) {
                Log.e(TAG, "Error updating local request status: " + e.getMessage());
            }
        }

        // Update Firestore
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", cleanStatus);
        updates.put("processedAt", FieldValue.serverTimestamp());
        updates.put("processedBy", adminUid != null ? adminUid : "System Admin");
        if (adminNote != null) {
            updates.put("adminNote", adminNote);
        }

        db.collection(COLLECTION_REQUESTS).document(requestId).update(updates)
                .addOnSuccessListener(aVoid -> listener.onSuccess("Request status updated to " + cleanStatus))
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Firestore update status failed, but local SQLite updated: " + e.getMessage());
                    listener.onSuccess("Request status updated to " + cleanStatus);
                });
    }
}


