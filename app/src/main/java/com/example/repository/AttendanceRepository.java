package com.example.repository;

import android.content.Context;
import android.util.Log;

import com.example.database.DatabaseHelper;
import com.example.model.Attendance;
import com.example.model.Student;
import com.example.model.Subject;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository class handling all Firebase Firestore operations for Hour-Based Attendance.
 */
public class AttendanceRepository {

    private static final String TAG = "AttendanceRepository";
    private static AttendanceRepository instance;

    private final FirebaseFirestore db;
    private final DatabaseHelper dbHelper;

    public interface OnStudentsLoadedListener {
        void onSuccess(List<Student> students);
        void onError(String errorMessage);
    }

    public interface OnSubjectsLoadedListener {
        void onSuccess(List<Subject> subjects);
        void onError(String errorMessage);
    }

    public interface OnAttendanceLoadedListener {
        void onSuccess(Map<String, Attendance> existingAttendanceMap);
        void onError(String errorMessage);
    }

    public interface OnSaveCompleteListener {
        void onSuccess(int totalSaved, boolean isUpdate);
        void onError(String errorMessage);
    }

    public interface OnStudentAttendanceListener {
        void onSuccess(List<Attendance> records);
        void onError(String errorMessage);
    }

    private AttendanceRepository(Context context) {
        db = FirebaseFirestore.getInstance();
        dbHelper = new DatabaseHelper(context.getApplicationContext());
    }

    public static synchronized AttendanceRepository getInstance(Context context) {
        if (instance == null) {
            instance = new AttendanceRepository(context);
        }
        return instance;
    }

    /**
     * Fetch students from Firestore filtered by department & semester.
     * Fallback to SQLite if Firestore is empty or offline.
     */
    public void fetchStudents(String department, String semester, OnStudentsLoadedListener listener) {
        Log.d(TAG, "Fetching students for Dept: " + department + ", Sem: " + semester);
        Query query = db.collection("students");

        if (department != null && !department.isEmpty() && !"All Departments".equalsIgnoreCase(department)) {
            query = query.whereEqualTo("department", department);
        }
        if (semester != null && !semester.isEmpty() && !"All Semesters".equalsIgnoreCase(semester)) {
            query = query.whereEqualTo("semester", semester);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Student> students = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        try {
                            int id = 0;
                            Object idObj = doc.get("id");
                            if (idObj instanceof Number) {
                                id = ((Number) idObj).intValue();
                            } else if (idObj instanceof String) {
                                try { id = Integer.parseInt((String) idObj); } catch (Exception e) { id = Math.abs(((String) idObj).hashCode()); }
                            }
                            if (id <= 0 && doc.getId() != null) {
                                try { id = Integer.parseInt(doc.getId()); } catch (Exception e) { id = Math.abs(doc.getId().hashCode()); }
                            }

                            String name = doc.getString("name");
                            String regNo = doc.getString("reg_no");
                            if (regNo == null) regNo = doc.getString("registerNo");
                            String dept = doc.getString("department");
                            String sem = "1";
                            Object semObj = doc.get("semester");
                            if (semObj != null) sem = String.valueOf(semObj);
                            String email = doc.getString("email");
                            String phone = doc.getString("phone");
                            String photo = doc.getString("photo_uri");
                            if (photo == null) photo = doc.getString("profileImageUrl");

                            students.add(new Student(id, name, regNo, dept, sem, email, phone, photo));
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing student document: " + doc.getId(), e);
                        }
                    }

                    if (students.isEmpty()) {
                        Log.d(TAG, "Firestore returned 0 students, falling back to local SQLite...");
                        students = dbHelper.getAllStudents();
                        if (department != null && !department.isEmpty() && !"All Departments".equalsIgnoreCase(department)) {
                            List<Student> filtered = new ArrayList<>();
                            for (Student s : students) {
                                if (department.equalsIgnoreCase(s.getDepartment()) &&
                                        (semester == null || semester.equalsIgnoreCase(s.getSemester()))) {
                                    filtered.add(s);
                                }
                            }
                            students = filtered;
                        }
                    }

                    if (listener != null) listener.onSuccess(students);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Firestore student fetch failed: " + e.getMessage() + ". Using SQLite fallback.");
                    List<Student> students = dbHelper.getAllStudents();
                    if (listener != null) listener.onSuccess(students);
                });
    }

    /**
     * Fetch subjects from Firestore filtered by department & semester.
     * Fallback to SQLite if empty.
     */
    public void fetchSubjects(String department, String semester, OnSubjectsLoadedListener listener) {
        Log.d(TAG, "Fetching subjects for Dept: " + department + ", Sem: " + semester);
        Query query = db.collection("subjects");

        if (department != null && !department.isEmpty() && !"All Departments".equalsIgnoreCase(department)) {
            query = query.whereEqualTo("department", department);
        }
        if (semester != null && !semester.isEmpty() && !"All Semesters".equalsIgnoreCase(semester)) {
            query = query.whereEqualTo("semester", semester);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Subject> subjects = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        try {
                            String status = doc.getString("status");
                            if ("INACTIVE".equalsIgnoreCase(status)) {
                                continue;
                            }

                            String code = doc.getString("subjectCode");
                            if (code == null) code = doc.getString("subject_code");
                            if (code == null) code = "CS501";

                            String name = doc.getString("subjectName");
                            if (name == null) name = doc.getString("subject_name");
                            if (name == null) name = "Subject";

                            int credits = doc.contains("credits") && doc.get("credits") != null ? doc.getLong("credits").intValue() : 4;
                            String semStr = doc.getString("semester");
                            String deptStr = doc.getString("department");

                            Subject s = new Subject(0, code, name, credits, 3, deptStr != null ? deptStr : department);
                            s.setSubjectId(doc.getId());
                            s.setSemester(semStr != null ? semStr : semester);
                            s.setStatus(status != null ? status : "ACTIVE");
                            subjects.add(s);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing subject document: " + doc.getId(), e);
                        }
                    }

                    if (subjects.isEmpty()) {
                        Log.d(TAG, "Firestore returned 0 subjects, falling back to local SQLite...");
                        subjects = dbHelper.getAllSubjects();
                    }

                    if (listener != null) listener.onSuccess(subjects);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Firestore subject fetch failed: " + e.getMessage() + ". Using SQLite fallback.");
                    List<Subject> subjects = dbHelper.getAllSubjects();
                    if (listener != null) listener.onSuccess(subjects);
                });
    }

    /**
     * Fetch existing attendance for (subjectId, date, hour) from Firestore.
     * Returns a map keyed by studentId -> Attendance object.
     */
    public void fetchHourAttendance(String subjectId, String date, int hour, OnAttendanceLoadedListener listener) {
        Log.d(TAG, "Querying Firestore attendance for Subject: " + subjectId + ", Date: " + date + ", Hour: " + hour);

        db.collection("attendance")
                .whereEqualTo("subjectId", subjectId)
                .whereEqualTo("date", date)
                .whereEqualTo("hour", hour)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, Attendance> resultMap = new HashMap<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        try {
                            Attendance att = doc.toObject(Attendance.class);
                            if (att != null && att.getStudentId() != null) {
                                resultMap.put(att.getStudentId(), att);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing attendance doc: " + doc.getId(), e);
                        }
                    }
                    if (listener != null) listener.onSuccess(resultMap);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching hour attendance: " + e.getMessage());
                    if (listener != null) listener.onError("Unable to connect to server. Please check your internet connection.");
                });
    }

    /**
     * Batch save/update attendance records in Firestore using deterministic document IDs.
     * Prevents duplicates for: studentId + subjectId + date + hour.
     */
    public void saveAttendanceBatch(List<Attendance> records, boolean isUpdate, OnSaveCompleteListener listener) {
        if (records == null || records.isEmpty()) {
            if (listener != null) listener.onError("No attendance records to save.");
            return;
        }

        WriteBatch batch = db.batch();
        for (Attendance rec : records) {
            String docId = Attendance.generateDocumentId(rec.getStudentId(), rec.getSubjectId(), rec.getDate(), rec.getHour());
            rec.setAttendanceId(docId);

            Map<String, Object> data = new HashMap<>();
            data.put("attendanceId", docId);
            data.put("studentId", rec.getStudentId());
            data.put("studentName", rec.getStudentName());
            data.put("registerNo", rec.getRegisterNo());
            data.put("subjectId", rec.getSubjectId());
            data.put("subjectName", rec.getSubjectName());
            data.put("department", rec.getDepartment());
            data.put("semester", rec.getSemester());
            data.put("date", rec.getDate());
            data.put("hour", rec.getHour());
            data.put("status", rec.getStatus());
            data.put("markedBy", rec.getMarkedBy() != null ? rec.getMarkedBy() : "Unknown");
            data.put("updatedAt", FieldValue.serverTimestamp());

            if (!isUpdate) {
                data.put("createdAt", FieldValue.serverTimestamp());
            }

            batch.set(db.collection("attendance").document(docId), data, SetOptions.merge());

            // Also mirror to local SQLite for offline access
            try {
                dbHelper.saveOrUpdateAttendanceRecord(
                        Integer.parseInt(rec.getStudentId()),
                        Integer.parseInt(rec.getSubjectId()),
                        rec.getDate(),
                        rec.getStatus()
                );
            } catch (Exception ignored) {}
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Batch attendance write successful! Count: " + records.size());
                    if (listener != null) listener.onSuccess(records.size(), isUpdate);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Batch attendance write failed: " + e.getMessage());
                    if (listener != null) listener.onError("Failed to save attendance: " + e.getMessage());
                });
    }

    /**
     * Fetch all attendance records for a specific student from Firestore.
     */
    public void fetchStudentAttendanceHistory(String studentId, OnStudentAttendanceListener listener) {
        Log.d(TAG, "Fetching attendance history for Student ID: " + studentId);

        db.collection("attendance")
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Attendance> records = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        try {
                            Attendance att = doc.toObject(Attendance.class);
                            if (att != null) {
                                records.add(att);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing student attendance record: " + doc.getId(), e);
                        }
                    }
                    if (listener != null) listener.onSuccess(records);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading student attendance history: " + e.getMessage());
                    if (listener != null) listener.onError("Unable to load student attendance from server.");
                });
    }
}
