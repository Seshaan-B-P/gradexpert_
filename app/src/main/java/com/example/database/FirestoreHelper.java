package com.example.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.model.AppNotification;
import com.example.model.Assessment;
import com.example.model.Assignment;
import com.example.model.AssignmentSubmission;
import com.example.model.AttendanceRecord;
import com.example.model.Result;
import com.example.model.Student;
import com.example.model.Subject;
import com.example.model.Teacher;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Singleton helper for syncing all GradeXpert local database records with Firebase Cloud Firestore.
 */
public class FirestoreHelper {

    private static final String TAG = "FirestoreHelper";
    private static FirestoreHelper instance;
    private final FirebaseFirestore db;

    public interface OnSyncCompleteListener {
        void onSyncSuccess(int totalSynced);
        void onSyncFailure(String errorMessage);
    }

    private FirestoreHelper() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirestoreHelper getInstance() {
        if (instance == null) {
            instance = new FirestoreHelper();
        }
        return instance;
    }

    // ==========================================
    // STUDENTS SYNC
    // ==========================================

    public void syncStudent(Student student) {
        if (student == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("id", student.getId());
        data.put("name", student.getName());
        data.put("reg_no", student.getRegNo());
        data.put("registerNo", student.getRegNo());
        data.put("department", student.getDepartment());
        data.put("departmentId", student.getDepartmentId() != null ? student.getDepartmentId() : "");
        data.put("departmentShortName", student.getDepartmentShortName() != null ? student.getDepartmentShortName() : "");
        data.put("programLevel", student.getProgramLevel());
        data.put("semester", student.getSemester());
        data.put("email", student.getEmail());
        data.put("phone", student.getPhone());
        data.put("photo_uri", student.getPhotoUri() != null ? student.getPhotoUri() : "");

        if (student.getLoginId() != null && !student.getLoginId().isEmpty()) {
            data.put("loginId", student.getLoginId());
        }
        if (student.getFirebaseUid() != null && !student.getFirebaseUid().isEmpty()) {
            data.put("uid", student.getFirebaseUid());
            data.put("firebaseUid", student.getFirebaseUid());
        }

        String docId = (student.getFirebaseUid() != null && !student.getFirebaseUid().trim().isEmpty())
                ? student.getFirebaseUid().trim()
                : String.valueOf(student.getId());

        db.collection("students")
                .document(docId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Student synced to Firestore: " + student.getName() + " [Doc: " + docId + "]"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to sync student: " + e.getMessage()));
    }

    public void deleteStudent(int studentId) {
        db.collection("students").document(String.valueOf(studentId)).delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Student deleted from Firestore: " + studentId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to delete student from Firestore: " + e.getMessage()));
    }

    // ==========================================
    // TEACHERS SYNC
    // ==========================================

    public void syncTeacher(Teacher teacher) {
        if (teacher == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("id", teacher.getId());
        data.put("name", teacher.getName());
        data.put("email", teacher.getEmail());
        data.put("department", teacher.getDepartment());
        data.put("phone", teacher.getPhone());

        if (teacher.getEmployeeId() != null && !teacher.getEmployeeId().isEmpty()) {
            data.put("employeeId", teacher.getEmployeeId());
        }
        if (teacher.getLoginId() != null && !teacher.getLoginId().isEmpty()) {
            data.put("loginId", teacher.getLoginId());
        }
        if (teacher.getFirebaseUid() != null && !teacher.getFirebaseUid().isEmpty()) {
            data.put("uid", teacher.getFirebaseUid());
            data.put("firebaseUid", teacher.getFirebaseUid());
        }

        String docId = (teacher.getFirebaseUid() != null && !teacher.getFirebaseUid().trim().isEmpty())
                ? teacher.getFirebaseUid().trim()
                : String.valueOf(teacher.getId());

        db.collection("teachers")
                .document(docId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Teacher synced to Firestore: " + teacher.getName() + " [Doc: " + docId + "]"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to sync teacher: " + e.getMessage()));
    }

    public void deleteTeacher(int teacherId) {
        db.collection("teachers").document(String.valueOf(teacherId)).delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Teacher deleted from Firestore: " + teacherId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to delete teacher from Firestore: " + e.getMessage()));
    }

    // ==========================================
    // SUBJECTS SYNC
    // ==========================================

    public void syncSubject(Subject subject) {
        if (subject == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("id", subject.getId());
        data.put("subject_code", subject.getSubjectCode());
        data.put("subject_name", subject.getSubjectName());
        data.put("credits", subject.getCredits());
        data.put("semester", subject.getSemester());
        data.put("department", subject.getDepartment());

        db.collection("subjects")
                .document(String.valueOf(subject.getId()))
                .set(data)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Subject synced to Firestore: " + subject.getSubjectCode()))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to sync subject: " + e.getMessage()));
    }

    public void deleteSubject(int subjectId) {
        db.collection("subjects").document(String.valueOf(subjectId)).delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Subject deleted from Firestore: " + subjectId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to delete subject from Firestore: " + e.getMessage()));
    }

    // ==========================================
    // MARKS SYNC
    // ==========================================

    public void syncMark(int studentId, int subjectId, double internal1, double assignment, double modelExam, double lab, double universityExam, double totalMarks, double percentage, String grade, double gradePoint) {
        Map<String, Object> data = new HashMap<>();
        data.put("student_id", studentId);
        data.put("subject_id", subjectId);
        data.put("internal1", internal1);
        data.put("assignment", assignment);
        data.put("model_exam", modelExam);
        data.put("lab", lab);
        data.put("university_exam", universityExam);
        data.put("total_marks", totalMarks);
        data.put("percentage", percentage);
        data.put("grade", grade != null ? grade : "");
        data.put("grade_point", gradePoint);

        String docId = studentId + "_" + subjectId;
        db.collection("marks")
                .document(docId)
                .set(data)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Marks synced to Firestore for student " + studentId + " sub " + subjectId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to sync mark: " + e.getMessage()));
    }

    public void deleteMark(int studentId, int subjectId) {
        String docId = studentId + "_" + subjectId;
        db.collection("marks").document(docId).delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Mark deleted from Firestore: " + docId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to delete mark: " + e.getMessage()));
    }

    // ==========================================
    // ATTENDANCE SYNC
    // ==========================================

    public void syncAttendance(int studentId, int subjectId, String date, String status) {
        Map<String, Object> data = new HashMap<>();
        data.put("student_id", studentId);
        data.put("subject_id", subjectId);
        data.put("date", date);
        data.put("status", status != null ? status.toUpperCase() : "PRESENT");

        String docId = studentId + "_" + subjectId + "_" + (date != null ? date.replace("-", "_") : "nodate");
        db.collection("attendance")
                .document(docId)
                .set(data)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Attendance synced to Firestore: " + docId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to sync attendance: " + e.getMessage()));
    }

    // ==========================================
    // ASSIGNMENT & SUBMISSIONS SYNC
    // ==========================================

    public void syncAssignment(Assignment assignment) {
        if (assignment == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("id", assignment.getId());
        data.put("title", assignment.getTitle());
        data.put("subject_id", assignment.getSubjectId());
        data.put("deadline", assignment.getDeadline());
        data.put("description", assignment.getDescription());
        data.put("file_path", assignment.getFilePath() != null ? assignment.getFilePath() : "");

        db.collection("assignments")
                .document(String.valueOf(assignment.getId()))
                .set(data)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Assignment synced to Firestore: " + assignment.getTitle()))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to sync assignment: " + e.getMessage()));
    }

    public void syncSubmission(int assignmentId, int studentId, String submissionDate, String filePath, String status, String remarks) {
        Map<String, Object> data = new HashMap<>();
        data.put("assignment_id", assignmentId);
        data.put("student_id", studentId);
        data.put("submission_date", submissionDate != null ? submissionDate : "");
        data.put("file_path", filePath != null ? filePath : "");
        data.put("status", status != null ? status : "SUBMITTED");
        data.put("remarks", remarks != null ? remarks : "");

        String docId = assignmentId + "_" + studentId;
        db.collection("submissions")
                .document(docId)
                .set(data)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Submission synced to Firestore: " + docId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to sync submission: " + e.getMessage()));
    }

    // ==========================================
    // RESULTS SYNC
    // ==========================================

    public void syncResult(Result result) {
        if (result == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("id", result.getId());
        data.put("resultId", result.getResultId());
        data.put("student_id", result.getStudentNumericId());
        data.put("studentId", result.getStudentId());
        data.put("studentName", result.getStudentName() != null ? result.getStudentName() : "");
        data.put("registerNo", result.getRegisterNo() != null ? result.getRegisterNo() : "");
        data.put("reg_no", result.getRegisterNo() != null ? result.getRegisterNo() : "");
        data.put("programLevel", result.getProgramLevel() != null ? result.getProgramLevel() : "UG");
        data.put("departmentId", result.getDepartmentId() != null ? result.getDepartmentId() : "");
        data.put("departmentName", result.getDepartmentName() != null ? result.getDepartmentName() : "");
        data.put("department", result.getDepartmentName() != null ? result.getDepartmentName() : "");
        data.put("semester", result.getSemester());
        data.put("academicYear", result.getAcademicYear() != null ? result.getAcademicYear() : "2025-2026");
        data.put("total_marks", result.getTotalMarks());
        data.put("totalMarks", result.getTotalMarks());
        data.put("percentage", result.getPercentage());
        data.put("sgpa", result.getSgpa());
        data.put("cgpa", result.getCgpa());
        data.put("status", result.getStatus() != null ? result.getStatus() : "DRAFT");
        data.put("version", result.getVersion() > 0 ? result.getVersion() : 1);
        data.put("publishedBy", result.getPublishedBy() != null ? result.getPublishedBy() : "");
        data.put("teacherId", result.getTeacherId() != null ? result.getTeacherId() : "");
        data.put("teacherName", result.getTeacherName() != null ? result.getTeacherName() : "");
        data.put("published_date", result.getPublishedDate() != null ? result.getPublishedDate() : "");

        if (result.getStudentUid() != null && !result.getStudentUid().isEmpty()) {
            data.put("studentUid", result.getStudentUid());
        }

        String resolvedDocId = result.getResultId();
        if (resolvedDocId == null || resolvedDocId.isEmpty()) {
            resolvedDocId = result.getStudentId() + "_" + result.getSemester();
        }
        final String docId = resolvedDocId;
        db.collection("results")
                .document(docId)
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Result synced to Firestore: " + docId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to sync result: " + e.getMessage()));
    }

    // ==========================================
    // NOTIFICATIONS SYNC
    // ==========================================

    public void syncNotification(AppNotification notif) {
        if (notif == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("id", notif.getId());
        data.put("title", notif.getTitle());
        data.put("message", notif.getMessage());
        data.put("date", notif.getDate());
        data.put("target_role", notif.getTargetRole());
        data.put("category", notif.getCategory());
        data.put("sender", notif.getSender());
        data.put("is_read", notif.isRead());

        db.collection("notifications")
                .document(String.valueOf(notif.getId()))
                .set(data)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification synced to Firestore: " + notif.getTitle()))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to sync notification: " + e.getMessage()));
    }

    public void deleteNotification(int notifId) {
        db.collection("notifications").document(String.valueOf(notifId)).delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification deleted from Firestore: " + notifId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to delete notification: " + e.getMessage()));
    }

    // ==========================================
    // ASSESSMENTS SYNC
    // ==========================================

    public void syncAssessment(Assessment a) {
        if (a == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("id", a.getId());
        data.put("title", a.getTitle());
        data.put("type", a.getType());
        data.put("subject_id", a.getSubjectId());
        data.put("semester", a.getSemester());
        data.put("date", a.getDate());
        data.put("max_marks", a.getMaxMarks());
        data.put("status", a.getStatus() != null ? a.getStatus() : "PENDING");

        db.collection("assessments")
                .document(String.valueOf(a.getId()))
                .set(data)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Assessment synced to Firestore: " + a.getTitle()))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to sync assessment: " + e.getMessage()));
    }

    public void deleteAssessment(int assessmentId) {
        db.collection("assessments").document(String.valueOf(assessmentId)).delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Assessment deleted from Firestore: " + assessmentId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to delete assessment: " + e.getMessage()));
    }

    // ==========================================
    // BULK SYNC ALL LOCAL DATA TO FIRESTORE
    // ==========================================

    /**
     * Bulk syncs ALL local SQLite database collections to Firebase Cloud Firestore.
     */
    public void syncAllLocalData(DatabaseHelper dbHelper, OnSyncCompleteListener listener) {
        try {
            WriteBatch batch = db.batch();
            int count = 0;

            // 1. Students
            List<Student> students = dbHelper.getAllStudents();
            for (Student s : students) {
                Map<String, Object> data = new HashMap<>();
                data.put("id", s.getId());
                data.put("name", s.getName());
                data.put("reg_no", s.getRegNo());
                data.put("department", s.getDepartment());
                data.put("departmentId", s.getDepartmentId() != null ? s.getDepartmentId() : "");
                data.put("departmentShortName", s.getDepartmentShortName() != null ? s.getDepartmentShortName() : "");
                data.put("programLevel", s.getProgramLevel());
                data.put("semester", s.getSemester());
                data.put("email", s.getEmail());
                data.put("phone", s.getPhone());
                data.put("photo_uri", s.getPhotoUri() != null ? s.getPhotoUri() : "");
                batch.set(db.collection("students").document(String.valueOf(s.getId())), data);
                count++;
            }

            // 2. Teachers
            List<Teacher> teachers = dbHelper.getAllTeachers();
            for (Teacher t : teachers) {
                Map<String, Object> data = new HashMap<>();
                data.put("id", t.getId());
                data.put("name", t.getName());
                data.put("email", t.getEmail());
                data.put("department", t.getDepartment());
                data.put("phone", t.getPhone());
                batch.set(db.collection("teachers").document(String.valueOf(t.getId())), data);
                count++;
            }

            // 3. Subjects
            List<Subject> subjects = dbHelper.getAllSubjects();
            for (Subject sub : subjects) {
                Map<String, Object> data = new HashMap<>();
                data.put("id", sub.getId());
                data.put("subject_code", sub.getSubjectCode());
                data.put("subject_name", sub.getSubjectName());
                data.put("credits", sub.getCredits());
                data.put("semester", sub.getSemester());
                data.put("department", sub.getDepartment());
                batch.set(db.collection("subjects").document(String.valueOf(sub.getId())), data);
                count++;
            }

            // 4. Assignments
            List<Assignment> assignments = dbHelper.getAllAssignments();
            for (Assignment a : assignments) {
                Map<String, Object> data = new HashMap<>();
                data.put("id", a.getId());
                data.put("title", a.getTitle());
                data.put("subject_id", a.getSubjectId());
                data.put("deadline", a.getDeadline());
                data.put("description", a.getDescription());
                data.put("file_path", a.getFilePath() != null ? a.getFilePath() : "");
                batch.set(db.collection("assignments").document(String.valueOf(a.getId())), data);
                count++;
            }

            // 5. Notifications
            List<AppNotification> notifications = dbHelper.getAllNotifications();
            for (AppNotification n : notifications) {
                Map<String, Object> data = new HashMap<>();
                data.put("id", n.getId());
                data.put("title", n.getTitle());
                data.put("message", n.getMessage());
                data.put("date", n.getDate());
                data.put("target_role", n.getTargetRole());
                data.put("category", n.getCategory());
                data.put("sender", n.getSender());
                data.put("is_read", n.isRead());
                batch.set(db.collection("notifications").document(String.valueOf(n.getId())), data);
                count++;
            }

            // 6. Assessments
            List<Assessment> assessments = dbHelper.getAllAssessments();
            for (Assessment ass : assessments) {
                Map<String, Object> data = new HashMap<>();
                data.put("id", ass.getId());
                data.put("title", ass.getTitle());
                data.put("type", ass.getType());
                data.put("subject_id", ass.getSubjectId());
                data.put("semester", ass.getSemester());
                data.put("date", ass.getDate());
                data.put("max_marks", ass.getMaxMarks());
                data.put("status", ass.getStatus());
                batch.set(db.collection("assessments").document(String.valueOf(ass.getId())), data);
                count++;
            }

            // 7. Marks Table Sync
            SQLiteDatabase rawDb = dbHelper.getReadableDatabase();
            Cursor mCursor = rawDb.query(DatabaseHelper.TABLE_MARKS, null, null, null, null, null, null);
            if (mCursor != null && mCursor.moveToFirst()) {
                do {
                    int stId = mCursor.getInt(mCursor.getColumnIndexOrThrow("student_id"));
                    int sbId = mCursor.getInt(mCursor.getColumnIndexOrThrow("subject_id"));
                    Map<String, Object> data = new HashMap<>();
                    data.put("student_id", stId);
                    data.put("subject_id", sbId);
                    data.put("internal1", mCursor.getDouble(mCursor.getColumnIndexOrThrow("internal1")));
                    data.put("assignment", mCursor.getColumnIndex("assignment") != -1 ? mCursor.getDouble(mCursor.getColumnIndexOrThrow("assignment")) : 0);
                    data.put("model_exam", mCursor.getColumnIndex("model_exam") != -1 ? mCursor.getDouble(mCursor.getColumnIndexOrThrow("model_exam")) : 0);
                    data.put("lab", mCursor.getColumnIndex("lab") != -1 ? mCursor.getDouble(mCursor.getColumnIndexOrThrow("lab")) : 0);
                    data.put("university_exam", mCursor.getDouble(mCursor.getColumnIndexOrThrow("university_exam")));
                    data.put("total_marks", mCursor.getDouble(mCursor.getColumnIndexOrThrow("total_marks")));
                    data.put("percentage", mCursor.getDouble(mCursor.getColumnIndexOrThrow("percentage")));
                    data.put("grade", mCursor.getString(mCursor.getColumnIndexOrThrow("grade")));
                    data.put("grade_point", mCursor.getDouble(mCursor.getColumnIndexOrThrow("grade_point")));

                    batch.set(db.collection("marks").document(stId + "_" + sbId), data);
                    count++;
                } while (mCursor.moveToNext());
                mCursor.close();
            }

            // 8. Attendance Table Sync
            Cursor attCursor = rawDb.query(DatabaseHelper.TABLE_ATTENDANCE, null, null, null, null, null, null);
            if (attCursor != null && attCursor.moveToFirst()) {
                do {
                    int stId = attCursor.getInt(attCursor.getColumnIndexOrThrow("student_id"));
                    int sbId = attCursor.getInt(attCursor.getColumnIndexOrThrow("subject_id"));
                    String date = attCursor.getString(attCursor.getColumnIndexOrThrow("date"));
                    String status = attCursor.getString(attCursor.getColumnIndexOrThrow("status"));

                    Map<String, Object> data = new HashMap<>();
                    data.put("student_id", stId);
                    data.put("subject_id", sbId);
                    data.put("date", date);
                    data.put("status", status);

                    String docId = stId + "_" + sbId + "_" + (date != null ? date.replace("-", "_") : "nodate");
                    batch.set(db.collection("attendance").document(docId), data);
                    count++;
                } while (attCursor.moveToNext());
                attCursor.close();
            }

            // 9. Results Table Sync
            Cursor resCursor = rawDb.query(DatabaseHelper.TABLE_RESULTS, null, null, null, null, null, null);
            if (resCursor != null && resCursor.moveToFirst()) {
                do {
                    int stId = resCursor.getInt(resCursor.getColumnIndexOrThrow("student_id"));
                    int sem = resCursor.getInt(resCursor.getColumnIndexOrThrow("semester"));
                    String studentUid = null;
                    int uidCol = resCursor.getColumnIndex("student_uid");
                    if (uidCol != -1 && !resCursor.isNull(uidCol)) {
                        studentUid = resCursor.getString(uidCol);
                    }
                    String effectiveStudentId = (studentUid != null && !studentUid.isEmpty()) ? studentUid : String.valueOf(stId);

                    Map<String, Object> data = new HashMap<>();
                    data.put("student_id", stId);
                    data.put("studentId", effectiveStudentId);
                    if (studentUid != null && !studentUid.isEmpty()) {
                        data.put("studentUid", studentUid);
                    }
                    data.put("semester", sem);
                    data.put("total_marks", resCursor.getDouble(resCursor.getColumnIndexOrThrow("total_marks")));
                    data.put("percentage", resCursor.getDouble(resCursor.getColumnIndexOrThrow("percentage")));
                    data.put("sgpa", resCursor.getDouble(resCursor.getColumnIndexOrThrow("sgpa")));
                    data.put("cgpa", resCursor.getDouble(resCursor.getColumnIndexOrThrow("cgpa")));
                    data.put("status", resCursor.getString(resCursor.getColumnIndexOrThrow("status")));
                    data.put("published_date", resCursor.getString(resCursor.getColumnIndexOrThrow("published_date")));

                    batch.set(db.collection("results").document(effectiveStudentId + "_" + sem), data, com.google.firebase.firestore.SetOptions.merge());
                    count++;
                } while (resCursor.moveToNext());
                resCursor.close();
            }

            // 10. Submissions Table Sync
            try {
                Cursor subCursor = rawDb.query(DatabaseHelper.TABLE_SUBMISSIONS, null, null, null, null, null, null);
                if (subCursor != null && subCursor.moveToFirst()) {
                    do {
                        int aId = subCursor.getInt(subCursor.getColumnIndexOrThrow("assignment_id"));
                        int stId = subCursor.getInt(subCursor.getColumnIndexOrThrow("student_id"));
                        Map<String, Object> data = new HashMap<>();
                        data.put("assignment_id", aId);
                        data.put("student_id", stId);
                        data.put("submission_date", subCursor.getString(subCursor.getColumnIndexOrThrow("submission_date")));
                        data.put("file_path", subCursor.getString(subCursor.getColumnIndexOrThrow("file_path")));
                        data.put("status", subCursor.getString(subCursor.getColumnIndexOrThrow("status")));
                        data.put("remarks", subCursor.getString(subCursor.getColumnIndexOrThrow("remarks")));

                        batch.set(db.collection("submissions").document(aId + "_" + stId), data);
                        count++;
                    } while (subCursor.moveToNext());
                    subCursor.close();
                }
            } catch (Exception ignored) {}

            final int totalSynced = count;
            batch.commit()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Bulk sync completed successfully. Total records synced to Firestore: " + totalSynced);
                        if (listener != null) listener.onSyncSuccess(totalSynced);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Bulk sync failed: " + e.getMessage());
                        if (listener != null) listener.onSyncFailure(e.getMessage());
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error initiating bulk sync: " + e.getMessage());
            if (listener != null) listener.onSyncFailure(e.getMessage());
        }
    }

    /**
     * Downloads and mirrors all Firestore collections into the local SQLite database cache.
     */
    public void syncFirestoreDataToLocal(DatabaseHelper dbHelper, OnSyncCompleteListener listener) {
        if (dbHelper == null) return;
        try {
            // 1. Students
            db.collection("students").get().addOnSuccessListener(snapshots -> {
                Set<String> activeUids = new HashSet<>();
                Set<Integer> activeIds = new HashSet<>();
                Set<String> activeRegNos = new HashSet<>();
                if (snapshots != null) {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                        try {
                            int id = 0;
                            if (doc.contains("id") && doc.get("id") instanceof Number) {
                                id = doc.getLong("id").intValue();
                            } else {
                                try { id = Integer.parseInt(doc.getId()); } catch (Exception e) { id = Math.abs(doc.getId().hashCode()); }
                            }
                            String name = doc.getString("name");
                            String regNo = doc.getString("registerNo");
                            if (regNo == null) regNo = doc.getString("reg_no");
                            String dept = doc.getString("department");
                            int sem = 1;
                            if (doc.contains("semester")) {
                                Object semObj = doc.get("semester");
                                if (semObj instanceof Number) sem = ((Number) semObj).intValue();
                                else if (semObj instanceof String) {
                                    String digits = ((String) semObj).replaceAll("[^0-9]", "");
                                    if (!digits.isEmpty()) sem = Integer.parseInt(digits);
                                }
                            }
                            String email = doc.getString("email");
                            String phone = doc.getString("phone");
                            String photo = doc.getString("profileImageUrl");
                            if (photo == null) photo = doc.getString("photo_uri");

                            String studentUid = doc.getId();
                            if (studentUid != null) activeUids.add(studentUid);
                            if (doc.getString("studentUid") != null) activeUids.add(doc.getString("studentUid"));
                            if (id > 0) activeIds.add(id);
                            if (regNo != null) activeRegNos.add(regNo);

                            dbHelper.upsertStudentFromFirestore(id, name, regNo, dept, sem, email, phone, photo, studentUid);
                        } catch (Exception e) {
                            Log.e(TAG, "Error caching student from Firestore", e);
                        }
                    }
                }
                dbHelper.pruneStudents(activeUids, activeIds, activeRegNos);
            });

            // 2. Teachers
            db.collection("teachers").get().addOnSuccessListener(snapshots -> {
                Set<String> activeEmails = new HashSet<>();
                Set<Integer> activeIds = new HashSet<>();
                Set<String> activeEmpIds = new HashSet<>();
                if (snapshots != null) {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                        try {
                            int id = 0;
                            if (doc.contains("id") && doc.get("id") instanceof Number) {
                                id = doc.getLong("id").intValue();
                            } else {
                                try { id = Integer.parseInt(doc.getId()); } catch (Exception e) { id = Math.abs(doc.getId().hashCode()); }
                            }
                            String name = doc.getString("name");
                            String email = doc.getString("email");
                            String dept = doc.getString("department");
                            String phone = doc.getString("phone");
                            String empId = doc.getString("employeeId");
                            if (empId == null) empId = doc.getString("identifier");

                            if (email != null) activeEmails.add(email);
                            if (id > 0) activeIds.add(id);
                            if (empId != null) activeEmpIds.add(empId);

                            dbHelper.upsertTeacherFromFirestore(id, name, email, dept, phone);
                        } catch (Exception e) {
                            Log.e(TAG, "Error caching teacher from Firestore", e);
                        }
                    }
                }
                dbHelper.pruneTeachers(activeEmails, activeIds, activeEmpIds);
            });

            // 3. Subjects
            db.collection("subjects").get().addOnSuccessListener(snapshots -> {
                Set<String> activeCodes = new HashSet<>();
                Set<Integer> activeIds = new HashSet<>();
                if (snapshots != null) {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                        try {
                            int id = 0;
                            if (doc.contains("id") && doc.get("id") instanceof Number) {
                                id = doc.getLong("id").intValue();
                            } else {
                                try { id = Integer.parseInt(doc.getId()); } catch (Exception e) { id = Math.abs(doc.getId().hashCode()); }
                            }
                            String code = doc.getString("subjectCode");
                            if (code == null) code = doc.getString("subject_code");
                            String name = doc.getString("subjectName");
                            if (name == null) name = doc.getString("subject_name");
                            int credits = doc.contains("credits") && doc.get("credits") instanceof Number ? doc.getLong("credits").intValue() : 4;
                            int sem = 1;
                            if (doc.contains("semester")) {
                                Object semObj = doc.get("semester");
                                if (semObj instanceof Number) sem = ((Number) semObj).intValue();
                                else if (semObj instanceof String) {
                                    String digits = ((String) semObj).replaceAll("[^0-9]", "");
                                    if (!digits.isEmpty()) sem = Integer.parseInt(digits);
                                }
                            }
                            String dept = doc.getString("department");

                            if (code != null) activeCodes.add(code);
                            if (id > 0) activeIds.add(id);

                            dbHelper.upsertSubjectFromFirestore(id, code, name, credits, sem, dept);
                        } catch (Exception e) {
                            Log.e(TAG, "Error caching subject from Firestore", e);
                        }
                    }
                }
                dbHelper.pruneSubjects(activeCodes, activeIds);
            });

            // 4. Assignments
            db.collection("assignments").get().addOnSuccessListener(snapshots -> {
                Set<Integer> activeIds = new HashSet<>();
                if (snapshots != null) {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                        try {
                            int id = 0;
                            if (doc.contains("id") && doc.get("id") instanceof Number) {
                                id = doc.getLong("id").intValue();
                            } else {
                                try { id = Integer.parseInt(doc.getId()); } catch (Exception e) { id = Math.abs(doc.getId().hashCode()); }
                            }
                            String title = doc.getString("title");
                            int subId = doc.contains("subject_id") && doc.get("subject_id") instanceof Number ? doc.getLong("subject_id").intValue() : 1;
                            String deadline = doc.getString("dueDate");
                            if (deadline == null) deadline = doc.getString("deadline");
                            String desc = doc.getString("description");
                            String filePath = doc.getString("attachmentUrl");
                            if (filePath == null) filePath = doc.getString("file_path");

                            if (id > 0) activeIds.add(id);

                            dbHelper.upsertAssignmentFromFirestore(id, title, subId, deadline, desc, filePath);
                        } catch (Exception e) {
                            Log.e(TAG, "Error caching assignment from Firestore", e);
                        }
                    }
                }
                dbHelper.pruneAssignments(activeIds);
            });

            // 5. Attendance
            db.collection("attendance").get().addOnSuccessListener(snapshots -> {
                if (snapshots != null) {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                        try {
                            int stId = 0;
                            if (doc.contains("student_id") && doc.get("student_id") instanceof Number) {
                                stId = doc.getLong("student_id").intValue();
                            } else if (doc.contains("studentId")) {
                                try { stId = Integer.parseInt(doc.getString("studentId")); } catch (Exception e) { stId = Math.abs(doc.getString("studentId").hashCode()); }
                            }
                            int sbId = 0;
                            if (doc.contains("subject_id") && doc.get("subject_id") instanceof Number) {
                                sbId = doc.getLong("subject_id").intValue();
                            } else if (doc.contains("subjectId")) {
                                try { sbId = Integer.parseInt(doc.getString("subjectId")); } catch (Exception e) { sbId = Math.abs(doc.getString("subjectId").hashCode()); }
                            }
                            String date = doc.getString("date");
                            String status = doc.getString("status");

                            if (stId > 0 && sbId > 0 && date != null) {
                                dbHelper.upsertAttendanceFromFirestore(stId, sbId, date, status);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error caching attendance from Firestore", e);
                        }
                    }
                }
            });

            // 6. Marks
            db.collection("marks").get().addOnSuccessListener(snapshots -> {
                if (snapshots != null) {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                        try {
                            int stId = doc.contains("student_id") && doc.get("student_id") instanceof Number ? doc.getLong("student_id").intValue() : 0;
                            int sbId = doc.contains("subject_id") && doc.get("subject_id") instanceof Number ? doc.getLong("subject_id").intValue() : 0;
                            double internal1 = doc.contains("internal1") && doc.get("internal1") instanceof Number ? ((Number) doc.get("internal1")).doubleValue() : 0;
                            double assignment = doc.contains("assignment") && doc.get("assignment") instanceof Number ? ((Number) doc.get("assignment")).doubleValue() : 0;
                            double modelExam = doc.contains("model_exam") && doc.get("model_exam") instanceof Number ? ((Number) doc.get("model_exam")).doubleValue() : 0;
                            double lab = doc.contains("lab") && doc.get("lab") instanceof Number ? ((Number) doc.get("lab")).doubleValue() : 0;
                            double universityExam = doc.contains("university_exam") && doc.get("university_exam") instanceof Number ? ((Number) doc.get("university_exam")).doubleValue() : 0;
                            double totalMarks = doc.contains("total_marks") && doc.get("total_marks") instanceof Number ? ((Number) doc.get("total_marks")).doubleValue() : (internal1 + assignment + modelExam + lab + universityExam);
                            double percentage = doc.contains("percentage") && doc.get("percentage") instanceof Number ? ((Number) doc.get("percentage")).doubleValue() : totalMarks;
                            String grade = doc.getString("grade");
                            double gradePoint = doc.contains("grade_point") && doc.get("grade_point") instanceof Number ? ((Number) doc.get("grade_point")).doubleValue() : 0;

                            if (stId > 0 && sbId > 0) {
                                dbHelper.upsertMarkFromFirestore(stId, sbId, internal1, assignment, modelExam, lab, universityExam, totalMarks, percentage, grade, gradePoint);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error caching marks from Firestore", e);
                        }
                    }
                }
            });

            // 7. Results
            db.collection("results").get().addOnSuccessListener(snapshots -> {
                Set<String> activeResultKeys = new HashSet<>();
                if (snapshots != null) {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                        try {
                            int stId = doc.contains("student_id") && doc.get("student_id") instanceof Number ? doc.getLong("student_id").intValue() : 0;
                            String studentUid = doc.getString("studentUid");
                            if (studentUid == null || studentUid.isEmpty()) {
                                studentUid = doc.getString("studentId");
                            }
                            if (stId == 0 && studentUid != null) {
                                try { stId = Integer.parseInt(studentUid); } catch (Exception ignored) {}
                                if (stId == 0) {
                                    Student stObj = dbHelper.getStudentByFirebaseUid(studentUid);
                                    if (stObj != null) stId = stObj.getId();
                                    if (stId == 0) {
                                        stObj = dbHelper.getStudentDetails(studentUid);
                                        if (stObj != null) stId = stObj.getId();
                                    }
                                }
                            }
                            if (stId == 0 && doc.contains("registerNo")) {
                                String rNo = doc.getString("registerNo");
                                Student stObj = dbHelper.getStudentDetails(rNo);
                                if (stObj != null) stId = stObj.getId();
                            }
                            int sem = doc.contains("semester") && doc.get("semester") instanceof Number ? doc.getLong("semester").intValue() : 1;
                            double totalMarks = doc.contains("total_marks") && doc.get("total_marks") instanceof Number ? ((Number) doc.get("total_marks")).doubleValue() : 0;
                            double percentage = doc.contains("percentage") && doc.get("percentage") instanceof Number ? ((Number) doc.get("percentage")).doubleValue() : 0;
                            double sgpa = doc.contains("sgpa") && doc.get("sgpa") instanceof Number ? ((Number) doc.get("sgpa")).doubleValue() : 0;
                            double cgpa = doc.contains("cgpa") && doc.get("cgpa") instanceof Number ? ((Number) doc.get("cgpa")).doubleValue() : 0;
                            String status = doc.getString("status");
                            String pubDate = doc.getString("published_date");

                            if (stId > 0) {
                                activeResultKeys.add(stId + "_" + sem);
                                if (studentUid != null && !studentUid.isEmpty()) {
                                    activeResultKeys.add(studentUid + "_" + sem);
                                }
                                activeResultKeys.add(doc.getId());
                                dbHelper.upsertResultFromFirestore(stId, sem, totalMarks, percentage, sgpa, cgpa, status, pubDate, studentUid);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error caching results from Firestore", e);
                        }
                    }
                }
                dbHelper.pruneResults(activeResultKeys);
            });

            // 8. Notifications
            db.collection("notifications").get().addOnSuccessListener(snapshots -> {
                Set<Integer> activeIds = new HashSet<>();
                if (snapshots != null) {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                        try {
                            int id = 0;
                            if (doc.contains("id") && doc.get("id") instanceof Number) {
                                id = doc.getLong("id").intValue();
                            } else {
                                try { id = Integer.parseInt(doc.getId()); } catch (Exception e) { id = Math.abs(doc.getId().hashCode()); }
                            }
                            String title = doc.getString("title");
                            String message = doc.getString("message");
                            String date = doc.getString("date");
                            String targetRole = doc.getString("target_role");
                            String category = doc.getString("category");
                            String sender = doc.getString("sender");
                            boolean isRead = doc.contains("is_read") && Boolean.TRUE.equals(doc.getBoolean("is_read"));

                            if (id > 0) activeIds.add(id);

                            dbHelper.upsertNotificationFromFirestore(id, title, message, date, targetRole, category, sender, isRead);
                        } catch (Exception e) {
                            Log.e(TAG, "Error caching notification from Firestore", e);
                        }
                    }
                }
                dbHelper.pruneNotifications(activeIds);
            });

            // 9. Assessments
            db.collection("assessments").get().addOnSuccessListener(snapshots -> {
                Set<Integer> activeIds = new HashSet<>();
                if (snapshots != null) {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                        try {
                            int id = 0;
                            if (doc.contains("id") && doc.get("id") instanceof Number) {
                                id = doc.getLong("id").intValue();
                            } else {
                                try { id = Integer.parseInt(doc.getId()); } catch (Exception e) { id = Math.abs(doc.getId().hashCode()); }
                            }
                            String title = doc.getString("title");
                            String type = doc.getString("type");
                            int subId = doc.contains("subject_id") && doc.get("subject_id") instanceof Number ? doc.getLong("subject_id").intValue() : 1;
                            int sem = doc.contains("semester") && doc.get("semester") instanceof Number ? doc.getLong("semester").intValue() : 1;
                            String date = doc.getString("date");
                            double maxMarks = doc.contains("max_marks") && doc.get("max_marks") instanceof Number ? ((Number) doc.get("max_marks")).doubleValue() : 50;
                            String status = doc.getString("status");

                            if (id > 0) activeIds.add(id);

                            dbHelper.upsertAssessmentFromFirestore(id, title, type, subId, sem, date, maxMarks, status);
                        } catch (Exception e) {
                            Log.e(TAG, "Error caching assessment from Firestore", e);
                        }
                    }
                }
                dbHelper.pruneAssessments(activeIds);
            });

            if (listener != null) listener.onSyncSuccess(1);
        } catch (Exception e) {
            Log.e(TAG, "Error in syncFirestoreDataToLocal", e);
            if (listener != null) listener.onSyncFailure(e.getMessage());
        }
    }

    /**
     * Purges all remote Firestore documents across all app collections for a fresh testing start.
     */
    public void purgeFirestoreCollections() {
        String[] collections = {"students", "teachers", "subjects", "departments", "semesters", "academicYears", "results", "notifications", "assignments", "submissions", "assignment_submissions", "assessments", "activity_logs", "marks", "portalActivities"};
        for (String col : collections) {
            try {
                db.collection(col).get().addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        WriteBatch batch = db.batch();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            batch.delete(doc.getReference());
                        }
                        batch.commit().addOnSuccessListener(aVoid -> Log.d(TAG, "Firestore collection purged: " + col));
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error purging collection " + col + ": " + e.getMessage());
            }
        }
    }
}

