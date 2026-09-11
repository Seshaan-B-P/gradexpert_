package com.example.repository;

import android.content.Context;
import android.util.Log;

import com.example.model.Student;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for student management operations against Firebase Firestore.
 */
public class StudentRepository {

    private static final String TAG = "StudentRepository";
    private static final String COLLECTION_STUDENTS = "students";

    private static StudentRepository instance;
    private final FirebaseFirestore db;
    private final CollectionReference studentsRef;

    public interface OnStudentsLoadedListener {
        void onSuccess(List<Student> students);
        void onError(String errorMessage);
    }

    public interface OnStudentOperationListener {
        void onSuccess(String message);
        void onError(String errorMessage);
    }

    public interface OnRegNoCheckListener {
        void onResult(boolean isUnique);
        void onError(String errorMessage);
    }

    private static final String COLLECTION_USERS = "users";

    private StudentRepository(Context context) {
        db = FirebaseFirestore.getInstance();
        studentsRef = db.collection(COLLECTION_STUDENTS);
    }

    public static synchronized StudentRepository getInstance(Context context) {
        if (instance == null) {
            instance = new StudentRepository(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Fetches all students from Firestore collection.
     */
    public void fetchStudents(OnStudentsLoadedListener listener) {
        studentsRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<Student> students = new ArrayList<>();
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                Student s = doc.toObject(Student.class);
                if (s != null) {
                    if (s.getStudentId() == null || s.getStudentId().isEmpty()) {
                        s.setStudentId(doc.getId());
                    }
                    if (doc.getString("programLevel") != null) {
                        s.setProgramLevel(doc.getString("programLevel"));
                    }
                    if (doc.getString("departmentId") != null) {
                        s.setDepartmentId(doc.getString("departmentId"));
                    }
                    if (doc.getString("departmentShortName") != null) {
                        s.setDepartmentShortName(doc.getString("departmentShortName"));
                    }
                    students.add(s);
                }
            }
            if (listener != null) listener.onSuccess(students);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching students", e);
            if (listener != null) listener.onError(e.getMessage());
        });
    }

    /**
     * Checks if registerNo is unique across students collection.
     */
    public void checkRegisterNoUnique(String registerNo, String excludeStudentId, OnRegNoCheckListener listener) {
        studentsRef.whereEqualTo("registerNo", registerNo).get().addOnSuccessListener(queryDocumentSnapshots -> {
            boolean isUnique = true;
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                if (excludeStudentId != null && doc.getId().equals(excludeStudentId)) {
                    continue;
                }
                isUnique = false;
                break;
            }
            if (listener != null) listener.onResult(isUnique);
        }).addOnFailureListener(e -> {
            if (listener != null) listener.onError(e.getMessage());
        });
    }

    /**
     * Adds a new student document to Firestore.
     */
    public void addStudent(Student student, OnStudentOperationListener listener) {
        String docId = (student.getStudentId() != null && !student.getStudentId().isEmpty())
                ? student.getStudentId()
                : "std_" + System.currentTimeMillis();
        student.setStudentId(docId);

        String programLevel = student.getProgramLevel();
        if (programLevel == null || (!"PG".equalsIgnoreCase(programLevel) && !"UG".equalsIgnoreCase(programLevel))) {
            programLevel = "UG";
        } else {
            programLevel = programLevel.toUpperCase();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("studentId", docId);
        data.put("uid", docId);
        data.put("name", student.getName());
        data.put("registerNo", student.getRegisterNo());
        data.put("email", student.getEmail());
        data.put("phone", student.getPhone());
        data.put("department", student.getDepartment());
        data.put("departmentName", student.getDepartment());
        data.put("departmentId", student.getDepartmentId() != null ? student.getDepartmentId() : "");
        data.put("departmentShortName", student.getDepartmentShortName() != null ? student.getDepartmentShortName() : "");
        data.put("programLevel", programLevel);
        data.put("semester", student.getSemester());
        data.put("section", student.getSection());
        data.put("gender", student.getGender());
        data.put("dateOfBirth", student.getDateOfBirth());
        data.put("profileImageUrl", student.getProfileImageUrl());
        data.put("status", student.getStatus() != null ? student.getStatus() : "ACTIVE");
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("updatedAt", FieldValue.serverTimestamp());

        studentsRef.document(docId).set(data).addOnSuccessListener(unused -> {
            if (listener != null) listener.onSuccess(docId);
        }).addOnFailureListener(e -> {
            if (listener != null) listener.onError(e.getMessage());
        });
    }

    /**
     * Updates an existing student document in Firestore.
     */
    public void updateStudent(Student student, OnStudentOperationListener listener) {
        if (student.getStudentId() == null || student.getStudentId().isEmpty()) {
            if (listener != null) listener.onError("Invalid Student ID.");
            return;
        }

        String programLevel = student.getProgramLevel();
        if (programLevel == null || (!"PG".equalsIgnoreCase(programLevel) && !"UG".equalsIgnoreCase(programLevel))) {
            programLevel = "UG";
        } else {
            programLevel = programLevel.toUpperCase();
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", student.getName());
        updates.put("registerNo", student.getRegisterNo());
        updates.put("email", student.getEmail());
        updates.put("phone", student.getPhone());
        updates.put("department", student.getDepartment());
        updates.put("departmentName", student.getDepartment());
        updates.put("departmentId", student.getDepartmentId() != null ? student.getDepartmentId() : "");
        updates.put("departmentShortName", student.getDepartmentShortName() != null ? student.getDepartmentShortName() : "");
        updates.put("programLevel", programLevel);
        updates.put("semester", student.getSemester());
        updates.put("section", student.getSection());
        updates.put("gender", student.getGender());
        updates.put("dateOfBirth", student.getDateOfBirth());
        updates.put("profileImageUrl", student.getProfileImageUrl());
        updates.put("status", student.getStatus());
        updates.put("updatedAt", FieldValue.serverTimestamp());

        final String finalProgramLevel = programLevel;
        studentsRef.document(student.getStudentId()).update(updates).addOnSuccessListener(unused -> {
            // Keep users collection in sync
            Map<String, Object> userUpdates = new HashMap<>();
            userUpdates.put("name", student.getName());
            userUpdates.put("email", student.getEmail());
            userUpdates.put("department", student.getDepartment());
            userUpdates.put("departmentName", student.getDepartment());
            userUpdates.put("departmentId", student.getDepartmentId() != null ? student.getDepartmentId() : "");
            userUpdates.put("departmentShortName", student.getDepartmentShortName() != null ? student.getDepartmentShortName() : "");
            userUpdates.put("programLevel", finalProgramLevel);
            userUpdates.put("semester", student.getSemester());
            userUpdates.put("status", student.getStatus());

            db.collection(COLLECTION_USERS).document(student.getStudentId()).update(userUpdates)
                    .addOnCompleteListener(t -> {
                        if (listener != null) listener.onSuccess("Student updated successfully.");
                    });
        }).addOnFailureListener(e -> {
            if (listener != null) listener.onError(e.getMessage());
        });
    }

    /**
     * Soft deletes student by setting status to INACTIVE.
     */
    public void deactivateStudent(String studentId, OnStudentOperationListener listener) {
        studentsRef.document(studentId).update("status", "INACTIVE", "updatedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(unused -> {
                    db.collection(COLLECTION_USERS).document(studentId).update("status", "INACTIVE");
                    if (listener != null) listener.onSuccess("Student moved to inactive.");
                }).addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    /**
     * Restores student by setting status to ACTIVE.
     */
    public void restoreStudent(String studentId, OnStudentOperationListener listener) {
        studentsRef.document(studentId).update("status", "ACTIVE", "updatedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(unused -> {
                    db.collection(COLLECTION_USERS).document(studentId).update("status", "ACTIVE");
                    if (listener != null) listener.onSuccess("Student restored successfully.");
                }).addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }
}
