package com.example.repository;

import android.content.Context;
import android.util.Log;

import com.example.database.DatabaseHelper;
import com.example.model.Subject;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for handling Subject management operations against Firebase Firestore and local SQLite storage.
 */
public class SubjectRepository {

    private static final String TAG = "SubjectRepository";
    private static final String COLLECTION_SUBJECTS = "subjects";

    private static SubjectRepository instance;
    private final FirebaseFirestore db;
    private final CollectionReference subjectsRef;
    private final DatabaseHelper dbHelper;

    public interface OnSubjectsLoadedListener {
        void onSuccess(List<Subject> subjects);
        void onError(String errorMessage);
    }

    public interface OnSubjectOperationListener {
        void onSuccess(String message);
        void onError(String errorMessage);
    }

    public interface OnSubjectCodeCheckListener {
        void onResult(boolean isUnique);
        void onError(String errorMessage);
    }

    private SubjectRepository(Context context) {
        db = FirebaseFirestore.getInstance();
        subjectsRef = db.collection(COLLECTION_SUBJECTS);
        dbHelper = new DatabaseHelper(context.getApplicationContext());
    }

    public static synchronized SubjectRepository getInstance(Context context) {
        if (instance == null) {
            instance = new SubjectRepository(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Fetches all subjects from Firestore. Falls back to local SQLite database if offline or failed.
     */
    public void fetchSubjects(OnSubjectsLoadedListener listener) {
        subjectsRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<Subject> subjects = new ArrayList<>();
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                Subject s = doc.toObject(Subject.class);
                if (s != null) {
                    if (s.getSubjectId() == null || s.getSubjectId().isEmpty()) {
                        s.setSubjectId(doc.getId());
                    }
                    String pLevel = doc.getString("programLevel");
                    if (pLevel != null && !pLevel.trim().isEmpty()) {
                        s.setProgramLevel(pLevel.trim().toUpperCase());
                    }
                    String dShort = doc.getString("departmentShortName");
                    if (dShort != null && !dShort.trim().isEmpty()) {
                        s.setDepartmentShortName(dShort.trim());
                    }
                    String dId = doc.getString("departmentId");
                    if (dId != null && !dId.trim().isEmpty()) {
                        s.setDepartmentId(dId.trim());
                    }
                    if (s.getSubjectName() == null || s.getSubjectName().trim().isEmpty()) {
                        String name = doc.getString("subject_name");
                        if (name == null) name = doc.getString("subjectName");
                        if (name == null) name = doc.getString("name");
                        if (name == null) name = doc.getString("title");
                        if (name != null) s.setSubjectName(name);
                    }
                    if (s.getSubjectCode() == null || s.getSubjectCode().trim().isEmpty()) {
                        String code = doc.getString("subject_code");
                        if (code == null) code = doc.getString("subjectCode");
                        if (code == null) code = doc.getString("code");
                        if (code != null) s.setSubjectCode(code);
                    }
                    subjects.add(s);
                    try {
                        dbHelper.upsertSubject(s);
                    } catch (Exception ignored) {}
                }
            }
            if (listener != null) listener.onSuccess(subjects);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching subjects from Firestore, checking SQLite fallback", e);
            try {
                List<Subject> localSubjects = dbHelper.getAllSubjects();
                if (localSubjects != null && !localSubjects.isEmpty()) {
                    if (listener != null) listener.onSuccess(localSubjects);
                    return;
                }
            } catch (Exception ex) {
                Log.e(TAG, "Failed reading local subjects fallback", ex);
            }
            if (listener != null) listener.onError(e.getMessage());
        });
    }

    /**
     * Checks if subjectCode is unique within department and semester.
     */
    public void checkSubjectCodeUnique(String subjectCode, String department, String semester, String excludeSubjectId, OnSubjectCodeCheckListener listener) {
        subjectsRef.whereEqualTo("subjectCode", subjectCode)
                .get().addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean isUnique = true;
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        if (excludeSubjectId != null && doc.getId().equals(excludeSubjectId)) {
                            continue;
                        }
                        Subject s = doc.toObject(Subject.class);
                        if (s != null) {
                            if (Subject.isSemesterMatching(semester, s.getSemester()) && Subject.isDepartmentMatching(department, s.getDepartment())) {
                                isUnique = false;
                                break;
                            }
                        } else if (doc.getId().equals(excludeSubjectId)) {
                            continue;
                        } else {
                            isUnique = false;
                            break;
                        }
                    }
                    if (listener != null) listener.onResult(isUnique);
                }).addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    /**
     * Adds a new subject to Firestore and local SQLite.
     */
    public void addSubject(Subject subject, OnSubjectOperationListener listener) {
        String docId = (subject.getSubjectId() != null && !subject.getSubjectId().isEmpty())
                ? subject.getSubjectId()
                : "subj_" + System.currentTimeMillis();
        subject.setSubjectId(docId);

        Map<String, Object> data = new HashMap<>();
        data.put("subjectId", docId);
        data.put("subjectCode", subject.getSubjectCode());
        data.put("subjectName", subject.getSubjectName());
        data.put("description", subject.getDescription());
        data.put("department", subject.getDepartment());
        data.put("departmentId", subject.getDepartmentId() != null ? subject.getDepartmentId() : "");
        data.put("departmentShortName", subject.getDepartmentShortName() != null ? subject.getDepartmentShortName() : "");
        data.put("programLevel", subject.getProgramLevel() != null ? subject.getProgramLevel().toUpperCase() : "UG");
        data.put("semester", subject.getSemester());
        data.put("credits", subject.getCredits());
        data.put("subjectType", subject.getSubjectType());
        data.put("weeklyHours", subject.getWeeklyHours());
        data.put("totalHours", subject.getTotalHours());
        data.put("status", subject.getStatus() != null ? subject.getStatus() : "ACTIVE");
        data.put("createdBy", subject.getCreatedBy());
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("updatedAt", FieldValue.serverTimestamp());

        subjectsRef.document(docId).set(data).addOnSuccessListener(unused -> {
            try {
                dbHelper.upsertSubject(subject);
            } catch (Exception ignored) {}
            if (listener != null) listener.onSuccess(docId);
        }).addOnFailureListener(e -> {
            if (listener != null) listener.onError(e.getMessage());
        });
    }

    /**
     * Updates an existing subject document in Firestore and local SQLite.
     */
    public void updateSubject(Subject subject, OnSubjectOperationListener listener) {
        if (subject.getSubjectId() == null || subject.getSubjectId().isEmpty()) {
            if (listener != null) listener.onError("Invalid Subject ID.");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("subjectCode", subject.getSubjectCode());
        updates.put("subjectName", subject.getSubjectName());
        updates.put("description", subject.getDescription());
        updates.put("department", subject.getDepartment());
        if (subject.getDepartmentId() != null && !subject.getDepartmentId().isEmpty()) {
            updates.put("departmentId", subject.getDepartmentId());
        }
        if (subject.getDepartmentShortName() != null && !subject.getDepartmentShortName().isEmpty()) {
            updates.put("departmentShortName", subject.getDepartmentShortName());
        }
        updates.put("programLevel", subject.getProgramLevel() != null ? subject.getProgramLevel().toUpperCase() : "UG");
        updates.put("semester", subject.getSemester());
        updates.put("credits", subject.getCredits());
        updates.put("subjectType", subject.getSubjectType());
        updates.put("weeklyHours", subject.getWeeklyHours());
        updates.put("totalHours", subject.getTotalHours());
        updates.put("status", subject.getStatus());
        updates.put("updatedAt", FieldValue.serverTimestamp());

        subjectsRef.document(subject.getSubjectId()).update(updates).addOnSuccessListener(unused -> {
            try {
                dbHelper.upsertSubject(subject);
            } catch (Exception ignored) {}
            if (listener != null) listener.onSuccess("Subject updated successfully.");
        }).addOnFailureListener(e -> {
            if (listener != null) listener.onError(e.getMessage());
        });
    }

    /**
     * Soft deletes a subject by setting status to INACTIVE.
     */
    public void deactivateSubject(String subjectId, OnSubjectOperationListener listener) {
        subjectsRef.document(subjectId).update("status", "INACTIVE", "updatedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(unused -> {
                    if (listener != null) listener.onSuccess("Subject moved to inactive.");
                }).addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    /**
     * Restores a subject by setting status to ACTIVE.
     */
    public void restoreSubject(String subjectId, OnSubjectOperationListener listener) {
        subjectsRef.document(subjectId).update("status", "ACTIVE", "updatedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(unused -> {
                    if (listener != null) listener.onSuccess("Subject restored successfully.");
                }).addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    /**
     * Permanently deletes a subject from Firestore and local SQLite.
     */
    public void deleteSubject(Subject subject, OnSubjectOperationListener listener) {
        if (subject == null || subject.getSubjectId() == null || subject.getSubjectId().isEmpty()) {
            if (listener != null) listener.onError("Invalid Subject data.");
            return;
        }

        subjectsRef.document(subject.getSubjectId()).delete()
                .addOnSuccessListener(unused -> {
                    try {
                        dbHelper.deleteSubjectByCode(subject.getSubjectCode());
                    } catch (Exception ignored) {}
                    if (listener != null) listener.onSuccess("Subject deleted successfully.");
                }).addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }
}
