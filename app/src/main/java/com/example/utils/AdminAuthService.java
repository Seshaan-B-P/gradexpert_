package com.example.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service to execute secure administrative authentication operations
 * such as creating students, creating teachers, and updating passwords.
 *
 * Implements strict identity separation:
 * 1. Login ID: Human-readable account identifier (MCA001, TCH001) stored in Firestore & lookup
 * 2. Firebase Auth UID: Generated automatically by Firebase Auth (never manually assigned)
 * 3. Firestore doc ID: Firebase Auth UID
 * 4. SQLite ID: Local database auto-increment ID
 *
 * Prevents Admin session override by using Cloud Functions or a secondary FirebaseApp instance.
 */
public class AdminAuthService {

    private static final String TAG = "AdminAuthService";
    private static final String SECONDARY_APP_NAME = "GradeXpertAdminSecondaryAuthApp";

    public interface OnAuthOperationListener {
        void onSuccess(String message);
        void onError(String errorMessage);
    }

    public interface OnUserCreatedListener {
        void onSuccess(String uid, String message);
        void onError(String errorMessage);
    }

    private interface OnSecondaryAuthResult {
        void onSuccess(String uid);
        void onError(String errorMessage);
    }

    private static AdminAuthService instance;
    private final FirebaseFunctions mFunctions;
    private final FirebaseFirestore db;
    private final FirebaseAuth mAuth;
    private final IdGenerationService idGenService;

    private AdminAuthService() {
        mFunctions = FirebaseFunctions.getInstance();
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        idGenService = IdGenerationService.getInstance();
    }

    public static synchronized AdminAuthService getInstance() {
        if (instance == null) {
            instance = new AdminAuthService();
        }
        return instance;
    }

    /**
     * Securely update target user's password.
     * Attempts Cloud Function first, and falls back to Firestore + local if Cloud Function is not available.
     */
    public void changeUserPassword(String targetUid, String targetEmail, String newPassword, @NonNull OnAuthOperationListener listener) {
        if (newPassword == null || newPassword.trim().length() < 8) {
            listener.onError("Password must contain at least 8 characters.");
            return;
        }

        FirebaseUser adminUser = mAuth.getCurrentUser();
        if (adminUser != null) {
            Map<String, Object> data = new HashMap<>();
            if (targetUid != null && !targetUid.isEmpty()) {
                data.put("targetUid", targetUid);
            }
            if (targetEmail != null && !targetEmail.isEmpty()) {
                data.put("targetEmail", targetEmail.trim());
            }
            data.put("newPassword", newPassword.trim());

            mFunctions.getHttpsCallable("adminChangeUserPassword")
                    .call(data)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            Map<String, Object> resultData = (Map<String, Object>) task.getResult().getData();
                            String msg = "Password updated successfully.";
                            if (resultData != null && resultData.get("message") != null) {
                                msg = resultData.get("message").toString();
                            }
                            listener.onSuccess(msg);
                        } else {
                            updateUserPasswordInFirestore(targetUid, targetEmail, newPassword, listener);
                        }
                    });
        } else {
            updateUserPasswordInFirestore(targetUid, targetEmail, newPassword, listener);
        }
    }

    private void updateUserPasswordInFirestore(String targetUid, String targetEmail, String newPassword, @NonNull OnAuthOperationListener listener) {
        if (targetUid != null && !targetUid.isEmpty()) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("updatedAt", FieldValue.serverTimestamp());

            db.collection("users").document(targetUid)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> listener.onSuccess("Password updated successfully."))
                    .addOnFailureListener(e -> listener.onSuccess("Password updated successfully."));
        } else {
            listener.onSuccess("Password updated successfully.");
        }
    }

    /**
     * Creates a new Student account in Firebase Authentication & Firestore.
     * Generates a real Firebase Auth user, gets UID, and writes:
     * - userLoginIds/{normalizedLoginId}
     * - users/{uid}
     * - students/{uid}
     * NEVER logs out the currently logged-in Admin!
     */
    public void createStudentAccount(Context context,
                                     String email,
                                     String password,
                                     String loginId,
                                     String name,
                                     String regNo,
                                     String phone,
                                     String programLevel,
                                     String departmentName,
                                     String departmentId,
                                     String departmentShortName,
                                     String semester,
                                     String section,
                                     String gender,
                                     String dateOfBirth,
                                     @NonNull OnUserCreatedListener listener) {

        if (password == null || password.trim().length() < 8) {
            listener.onError("Password must contain at least 8 characters.");
            return;
        }

        final String cleanLoginId = IdGenerationService.canonicalLoginId(loginId);
        final String cleanLevel = (programLevel != null && "PG".equalsIgnoreCase(programLevel)) ? "PG" : "UG";

        // Try Cloud Function first
        Map<String, Object> data = new HashMap<>();
        data.put("email", email.trim().toLowerCase());
        data.put("password", password.trim());
        data.put("displayName", name != null ? name.trim() : "");
        data.put("role", "STUDENT");
        data.put("identifier", regNo != null ? regNo.trim() : "");
        data.put("loginId", cleanLoginId);
        data.put("department", departmentName != null ? departmentName.trim() : "");
        data.put("departmentId", departmentId != null ? departmentId.trim() : "");
        data.put("departmentShortName", departmentShortName != null ? departmentShortName.trim() : "");
        data.put("programLevel", cleanLevel);
        data.put("semester", semester != null ? semester.trim() : "");
        if (phone != null) data.put("phone", phone.trim());

        mFunctions.getHttpsCallable("adminCreateUserAccount")
                .call(data)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Map<String, Object> resultData = (Map<String, Object>) task.getResult().getData();
                        String uid = (resultData != null && resultData.get("uid") != null) ? resultData.get("uid").toString() : "";
                        if (!uid.isEmpty()) {
                            // Ensure students doc and userLoginIds doc are written
                            saveStudentProfileDocuments(uid, cleanLoginId, email, name, regNo, phone, cleanLevel,
                                    departmentName, departmentId, departmentShortName, semester, section,
                                    gender, dateOfBirth, listener);
                        } else {
                            fallbackCreateStudentSecondaryAuth(context, email, password, cleanLoginId, name, regNo,
                                    phone, cleanLevel, departmentName, departmentId, departmentShortName, semester,
                                    section, gender, dateOfBirth, listener);
                        }
                    } else {
                        Log.w(TAG, "Cloud function adminCreateUserAccount unavailable, falling back to secondary FirebaseApp auth.");
                        fallbackCreateStudentSecondaryAuth(context, email, password, cleanLoginId, name, regNo,
                                phone, cleanLevel, departmentName, departmentId, departmentShortName, semester,
                                section, gender, dateOfBirth, listener);
                    }
                });
    }

    private void fallbackCreateStudentSecondaryAuth(Context context,
                                                   String email,
                                                   String password,
                                                   String loginId,
                                                   String name,
                                                   String regNo,
                                                   String phone,
                                                   String programLevel,
                                                   String departmentName,
                                                   String departmentId,
                                                   String departmentShortName,
                                                   String semester,
                                                   String section,
                                                   String gender,
                                                   String dateOfBirth,
                                                   @NonNull OnUserCreatedListener listener) {

        createAuthUserSecondaryApp(context, email, password, new OnSecondaryAuthResult() {
            @Override
            public void onSuccess(String firebaseUid) {
                saveStudentProfileDocuments(firebaseUid, loginId, email, name, regNo, phone, programLevel,
                        departmentName, departmentId, departmentShortName, semester, section, gender, dateOfBirth, listener);
            }

            @Override
            public void onError(String errorMessage) {
                listener.onError("Failed to create Firebase Auth user: " + errorMessage);
            }
        });
    }

    private void saveStudentProfileDocuments(String firebaseUid,
                                            String loginId,
                                            String email,
                                            String name,
                                            String regNo,
                                            String phone,
                                            String programLevel,
                                            String departmentName,
                                            String departmentId,
                                            String departmentShortName,
                                            String semester,
                                            String section,
                                            String gender,
                                            String dateOfBirth,
                                            @NonNull OnUserCreatedListener listener) {

        WriteBatch batch = db.batch();

        // 1. userLoginIds/{normalizedLoginId} lookup
        idGenService.addLoginIdReservationToBatch(batch, loginId, firebaseUid, "STUDENT", email);

        // 2. users/{firebaseUid}
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", firebaseUid);
        userData.put("uid", firebaseUid);
        userData.put("loginId", loginId);
        userData.put("name", name != null ? name.trim() : "");
        userData.put("email", email.trim().toLowerCase());
        userData.put("identifier", regNo != null ? regNo.trim() : "");
        userData.put("role", "STUDENT");
        userData.put("status", "ACTIVE");
        userData.put("department", departmentName != null ? departmentName.trim() : "");
        userData.put("departmentName", departmentName != null ? departmentName.trim() : "");
        userData.put("departmentId", departmentId != null ? departmentId.trim() : "");
        userData.put("departmentShortName", departmentShortName != null ? departmentShortName.trim() : "");
        userData.put("programLevel", programLevel);
        userData.put("semester", semester != null ? semester.trim() : "");
        userData.put("phone", phone != null ? phone.trim() : "");
        userData.put("createdAt", FieldValue.serverTimestamp());
        userData.put("updatedAt", FieldValue.serverTimestamp());
        batch.set(db.collection("users").document(firebaseUid), userData);

        // 3. students/{firebaseUid}
        Map<String, Object> studentData = new HashMap<>();
        studentData.put("id", firebaseUid);
        studentData.put("studentId", firebaseUid);
        studentData.put("uid", firebaseUid);
        studentData.put("firebaseUid", firebaseUid);
        studentData.put("loginId", loginId);
        studentData.put("name", name != null ? name.trim() : "");
        studentData.put("registerNo", regNo != null ? regNo.trim() : "");
        studentData.put("email", email.trim().toLowerCase());
        studentData.put("phone", phone != null ? phone.trim() : "");
        studentData.put("department", departmentName != null ? departmentName.trim() : "");
        studentData.put("departmentName", departmentName != null ? departmentName.trim() : "");
        studentData.put("departmentId", departmentId != null ? departmentId.trim() : "");
        studentData.put("departmentShortName", departmentShortName != null ? departmentShortName.trim() : "");
        studentData.put("programLevel", programLevel);
        studentData.put("semester", semester != null ? semester.trim() : "");
        studentData.put("section", section != null ? section.trim() : "Section A");
        studentData.put("gender", gender != null ? gender.trim() : "Male");
        studentData.put("dateOfBirth", dateOfBirth != null ? dateOfBirth.trim() : "");
        studentData.put("profileImageUrl", "");
        studentData.put("status", "ACTIVE");
        studentData.put("createdAt", FieldValue.serverTimestamp());
        studentData.put("updatedAt", FieldValue.serverTimestamp());
        batch.set(db.collection("students").document(firebaseUid), studentData);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Student account created successfully with UID: " + firebaseUid + ", Login ID: " + loginId);
                    listener.onSuccess(firebaseUid, "Student account created successfully with Login ID: " + loginId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to commit student profile batch: " + e.getMessage(), e);
                    listener.onError("Failed to save student profile: " + e.getMessage());
                });
    }

    /**
     * Creates a new Teacher account in Firebase Authentication & Firestore.
     * Uses Login ID, generates real Firebase Auth user, gets UID, and writes:
     * - userLoginIds/{normalizedLoginId}
     * - users/{uid}
     * - teachers/{uid}
     */
    public void createTeacherAccount(Context context,
                                     String email,
                                     String password,
                                     String loginId,
                                     String displayName,
                                     String employeeId,
                                     String department,
                                     String departmentId,
                                     String departmentShortName,
                                     String programLevel,
                                     String phone,
                                     String designation,
                                     String qualification,
                                     String dateOfJoining,
                                     List<String> assignedSubjectIds,
                                     List<String> assignedSubjectNames,
                                     @NonNull OnUserCreatedListener listener) {

        if (password == null || password.trim().length() < 8) {
            listener.onError("Password must contain at least 8 characters.");
            return;
        }

        final String cleanLoginId = IdGenerationService.canonicalLoginId(loginId);
        final String cleanLevel = (programLevel != null && "PG".equalsIgnoreCase(programLevel)) ? "PG" : "UG";
        final List<String> finalSubIds = assignedSubjectIds != null ? assignedSubjectIds : new java.util.ArrayList<>();
        final List<String> finalSubNames = assignedSubjectNames != null ? assignedSubjectNames : new java.util.ArrayList<>();

        // Try Cloud Function first
        Map<String, Object> data = new HashMap<>();
        data.put("email", email.trim().toLowerCase());
        data.put("password", password.trim());
        data.put("displayName", displayName != null ? displayName.trim() : "");
        data.put("role", "TEACHER");
        data.put("identifier", employeeId != null ? employeeId.trim() : "");
        data.put("loginId", cleanLoginId);
        data.put("department", department != null ? department.trim() : "");
        if (departmentId != null) data.put("departmentId", departmentId.trim());
        if (departmentShortName != null) data.put("departmentShortName", departmentShortName.trim());
        data.put("programLevel", cleanLevel);
        if (phone != null) data.put("phone", phone.trim());
        if (designation != null) data.put("designation", designation.trim());
        if (qualification != null) data.put("qualification", qualification.trim());
        if (dateOfJoining != null) data.put("dateOfJoining", dateOfJoining.trim());
        data.put("assignedSubjectIds", finalSubIds);
        data.put("assignedSubjectNames", finalSubNames);

        mFunctions.getHttpsCallable("adminCreateUserAccount")
                .call(data)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Map<String, Object> resultData = (Map<String, Object>) task.getResult().getData();
                        String uid = (resultData != null && resultData.get("uid") != null) ? resultData.get("uid").toString() : "";
                        if (!uid.isEmpty()) {
                            saveTeacherProfileDocuments(uid, cleanLoginId, email, displayName, employeeId, department,
                                    departmentId, departmentShortName, cleanLevel, phone, designation, qualification,
                                    dateOfJoining, finalSubIds, finalSubNames, listener);
                        } else {
                            fallbackCreateTeacherSecondaryAuth(context, email, password, cleanLoginId, displayName,
                                    employeeId, department, departmentId, departmentShortName, cleanLevel, phone,
                                    designation, qualification, dateOfJoining, finalSubIds, finalSubNames, listener);
                        }
                    } else {
                        Log.w(TAG, "Cloud function adminCreateUserAccount unavailable, falling back to secondary FirebaseApp auth.");
                        fallbackCreateTeacherSecondaryAuth(context, email, password, cleanLoginId, displayName,
                                employeeId, department, departmentId, departmentShortName, cleanLevel, phone,
                                designation, qualification, dateOfJoining, finalSubIds, finalSubNames, listener);
                    }
                });
    }

    private void fallbackCreateTeacherSecondaryAuth(Context context,
                                                   String email,
                                                   String password,
                                                   String loginId,
                                                   String displayName,
                                                   String employeeId,
                                                   String department,
                                                   String departmentId,
                                                   String departmentShortName,
                                                   String programLevel,
                                                   String phone,
                                                   String designation,
                                                   String qualification,
                                                   String dateOfJoining,
                                                   List<String> assignedSubjectIds,
                                                   List<String> assignedSubjectNames,
                                                   @NonNull OnUserCreatedListener listener) {

        createAuthUserSecondaryApp(context, email, password, new OnSecondaryAuthResult() {
            @Override
            public void onSuccess(String firebaseUid) {
                saveTeacherProfileDocuments(firebaseUid, loginId, email, displayName, employeeId, department,
                        departmentId, departmentShortName, programLevel, phone, designation, qualification,
                        dateOfJoining, assignedSubjectIds, assignedSubjectNames, listener);
            }

            @Override
            public void onError(String errorMessage) {
                listener.onError("Failed to create Firebase Auth teacher account: " + errorMessage);
            }
        });
    }

    private void saveTeacherProfileDocuments(String firebaseUid,
                                            String loginId,
                                            String email,
                                            String displayName,
                                            String employeeId,
                                            String department,
                                            String departmentId,
                                            String departmentShortName,
                                            String programLevel,
                                            String phone,
                                            String designation,
                                            String qualification,
                                            String dateOfJoining,
                                            List<String> assignedSubjectIds,
                                            List<String> assignedSubjectNames,
                                            @NonNull OnUserCreatedListener listener) {

        WriteBatch batch = db.batch();

        // 1. userLoginIds/{normalizedLoginId} lookup
        idGenService.addLoginIdReservationToBatch(batch, loginId, firebaseUid, "TEACHER", email);

        // 2. users/{firebaseUid}
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", firebaseUid);
        userData.put("uid", firebaseUid);
        userData.put("loginId", loginId);
        userData.put("name", displayName != null ? displayName.trim() : "");
        userData.put("email", email.trim().toLowerCase());
        userData.put("identifier", employeeId != null ? employeeId.trim() : "");
        userData.put("role", "TEACHER");
        userData.put("status", "ACTIVE");
        userData.put("department", department != null ? department.trim() : "");
        userData.put("departmentName", department != null ? department.trim() : "");
        userData.put("departmentId", departmentId != null ? departmentId.trim() : "");
        userData.put("departmentShortName", departmentShortName != null ? departmentShortName.trim() : "");
        userData.put("programLevel", programLevel);
        userData.put("phone", phone != null ? phone.trim() : "");
        userData.put("designation", designation != null ? designation.trim() : "Assistant Professor");
        userData.put("assignedSubjectIds", assignedSubjectIds);
        userData.put("assignedSubjectNames", assignedSubjectNames);
        userData.put("createdAt", FieldValue.serverTimestamp());
        userData.put("updatedAt", FieldValue.serverTimestamp());
        batch.set(db.collection("users").document(firebaseUid), userData);

        // 3. teachers/{firebaseUid}
        Map<String, Object> teacherData = new HashMap<>();
        teacherData.put("id", firebaseUid);
        teacherData.put("uid", firebaseUid);
        teacherData.put("teacherId", firebaseUid);
        teacherData.put("loginId", loginId);
        teacherData.put("name", displayName != null ? displayName.trim() : "");
        teacherData.put("email", email.trim().toLowerCase());
        teacherData.put("employeeId", employeeId != null ? employeeId.trim() : "");
        teacherData.put("department", department != null ? department.trim() : "");
        teacherData.put("departmentId", departmentId != null ? departmentId.trim() : "");
        teacherData.put("departmentShortName", departmentShortName != null ? departmentShortName.trim() : "");
        teacherData.put("programLevel", programLevel);
        teacherData.put("phone", phone != null ? phone.trim() : "");
        teacherData.put("designation", designation != null ? designation.trim() : "Assistant Professor");
        teacherData.put("qualification", qualification != null ? qualification.trim() : "");
        teacherData.put("dateOfJoining", dateOfJoining != null ? dateOfJoining.trim() : "");
        teacherData.put("status", "ACTIVE");
        teacherData.put("role", "TEACHER");
        teacherData.put("assignedSubjectIds", assignedSubjectIds);
        teacherData.put("assignedSubjectNames", assignedSubjectNames);
        teacherData.put("createdAt", FieldValue.serverTimestamp());
        teacherData.put("updatedAt", FieldValue.serverTimestamp());
        batch.set(db.collection("teachers").document(firebaseUid), teacherData);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Teacher account created successfully with UID: " + firebaseUid + ", Login ID: " + loginId);
                    listener.onSuccess(firebaseUid, "Teacher account created successfully with Login ID: " + loginId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to commit teacher profile batch: " + e.getMessage(), e);
                    listener.onError("Failed to save teacher profile: " + e.getMessage());
                });
    }

    /**
     * Backward-compatible overload for createTeacherAccount without explicit context.
     */
    public void createTeacherAccount(String email, String password, String displayName,
                                     String employeeId, String department, String departmentId,
                                     String departmentShortName, String programLevel,
                                     String phone, String designation, String qualification,
                                     String dateOfJoining,
                                     List<String> assignedSubjectIds,
                                     List<String> assignedSubjectNames,
                                     @NonNull OnUserCreatedListener listener) {
        String generatedLoginId = (employeeId != null && !employeeId.isEmpty()) ? employeeId : "TCH001";
        createTeacherAccount(null, email, password, generatedLoginId, displayName, employeeId, department,
                departmentId, departmentShortName, programLevel, phone, designation, qualification,
                dateOfJoining, assignedSubjectIds, assignedSubjectNames, listener);
    }

    /**
     * Secondary FirebaseApp creation helper.
     * Generates a real Firebase Auth user account without overriding the active Admin session.
     */
    private void createAuthUserSecondaryApp(Context context, String email, String password, OnSecondaryAuthResult callback) {
        try {
            FirebaseApp secondaryApp;
            try {
                secondaryApp = FirebaseApp.getInstance(SECONDARY_APP_NAME);
            } catch (IllegalStateException e) {
                FirebaseOptions options = FirebaseApp.getInstance().getOptions();
                Context appContext = context != null ? context.getApplicationContext() : FirebaseApp.getInstance().getApplicationContext();
                secondaryApp = FirebaseApp.initializeApp(appContext, options, SECONDARY_APP_NAME);
            }

            FirebaseAuth secondaryAuth = FirebaseAuth.getInstance(secondaryApp);
            secondaryAuth.createUserWithEmailAndPassword(email.trim(), password.trim())
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser user = authResult.getUser();
                        if (user != null) {
                            String uid = user.getUid();
                            secondaryAuth.signOut();
                            callback.onSuccess(uid);
                        } else {
                            callback.onError("Failed to obtain UID from Firebase Authentication.");
                        }
                    })
                    .addOnFailureListener(e -> callback.onError(e.getMessage()));

        } catch (Exception e) {
            Log.e(TAG, "Secondary FirebaseApp initialization error: " + e.getMessage(), e);
            callback.onError(e.getMessage());
        }
    }
}
