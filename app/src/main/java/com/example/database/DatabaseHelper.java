package com.example.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.example.model.AppNotification;
import com.example.model.Assignment;
import com.example.model.AssignmentSubmission;
import com.example.model.AttendanceRecord;
import com.example.model.MonthlyAttendanceStats;
import com.example.model.PasswordResetRequest;
import com.example.model.Student;
import com.example.model.StudentAttendanceLog;
import com.example.model.Subject;
import com.example.model.SubjectAttendanceStats;
import com.example.model.SubjectGradeItem;
import com.example.model.TopperItem;
import com.example.model.User;
import com.example.utils.PasswordUtils;

/**
 * SQLite Database Helper managing local ERP database tables:
 * Teacher, Student, Subject, Marks, Attendance, Assignment, Submissions, Results, Notifications.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "gradexpert.db";
    private static final int DATABASE_VERSION = 2;

    // Table Names
    public static final String TABLE_TEACHERS = "teachers";
    public static final String TABLE_STUDENTS = "students";
    public static final String TABLE_SUBJECTS = "subjects";
    public static final String TABLE_MARKS = "marks";
    public static final String TABLE_ATTENDANCE = "attendance";
    public static final String TABLE_ASSIGNMENTS = "assignments";
    public static final String TABLE_SUBMISSIONS = "submissions";
    public static final String TABLE_RESULTS = "results";
    public static final String TABLE_NOTIFICATIONS = "notifications";
    public static final String TABLE_ADMINS = "admins";
    public static final String TABLE_ACTIVITY_LOGS = "activity_logs";
    public static final String TABLE_ASSESSMENTS = "assessments";
    public static final String TABLE_PASSWORD_RESETS = "password_reset_requests";
    public static final String TABLE_STUDENT_ATTENDANCE_LOGS = "student_attendance_logs";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private Set<String> getTableColumns(SQLiteDatabase db, String tableName) {
        Set<String> columns = new HashSet<>();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);
            if (cursor != null) {
                int nameIndex = cursor.getColumnIndex("name");
                while (cursor.moveToNext()) {
                    if (nameIndex != -1) {
                        columns.add(cursor.getString(nameIndex).toLowerCase(Locale.ROOT));
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return columns;
    }

    private void addColumnIfNotExists(SQLiteDatabase db, String tableName, Set<String> existingColumns, String columnName, String columnTypeAndDefault) {
        if (!existingColumns.contains(columnName.toLowerCase(Locale.ROOT))) {
            try {
                db.execSQL("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnTypeAndDefault);
                existingColumns.add(columnName.toLowerCase(Locale.ROOT));
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        try {
            // Safe schema migrations with prior column existence check to prevent duplicate column SQLite errors
            Set<String> resultCols = getTableColumns(db, TABLE_RESULTS);
            addColumnIfNotExists(db, TABLE_RESULTS, resultCols, "total_marks", "REAL DEFAULT 0");
            addColumnIfNotExists(db, TABLE_RESULTS, resultCols, "percentage", "REAL DEFAULT 0");
            addColumnIfNotExists(db, TABLE_RESULTS, resultCols, "total_credits", "INTEGER DEFAULT 0");
            addColumnIfNotExists(db, TABLE_RESULTS, resultCols, "status", "TEXT DEFAULT 'DRAFT'");
            addColumnIfNotExists(db, TABLE_RESULTS, resultCols, "published_date", "TEXT");
            addColumnIfNotExists(db, TABLE_RESULTS, resultCols, "version", "INTEGER DEFAULT 1");
            addColumnIfNotExists(db, TABLE_RESULTS, resultCols, "approval_status", "TEXT DEFAULT 'DRAFT'");
            addColumnIfNotExists(db, TABLE_RESULTS, resultCols, "submitted_at", "TEXT");
            addColumnIfNotExists(db, TABLE_RESULTS, resultCols, "approved_at", "TEXT");
            addColumnIfNotExists(db, TABLE_RESULTS, resultCols, "approved_by", "TEXT");
            addColumnIfNotExists(db, TABLE_RESULTS, resultCols, "rejection_reason", "TEXT");
            addColumnIfNotExists(db, TABLE_RESULTS, resultCols, "student_uid", "TEXT");
            addColumnIfNotExists(db, TABLE_RESULTS, resultCols, "sync_status", "TEXT DEFAULT 'SYNCED'");

            Set<String> studentCols = getTableColumns(db, TABLE_STUDENTS);
            addColumnIfNotExists(db, TABLE_STUDENTS, studentCols, "student_uid", "TEXT");
            addColumnIfNotExists(db, TABLE_STUDENTS, studentCols, "firebase_uid", "TEXT");
            addColumnIfNotExists(db, TABLE_STUDENTS, studentCols, "login_id", "TEXT");
            addColumnIfNotExists(db, TABLE_STUDENTS, studentCols, "reg_no", "TEXT");

            Set<String> teacherCols = getTableColumns(db, TABLE_TEACHERS);
            addColumnIfNotExists(db, TABLE_TEACHERS, teacherCols, "firebase_uid", "TEXT");
            addColumnIfNotExists(db, TABLE_TEACHERS, teacherCols, "login_id", "TEXT");

            Set<String> subjectCols = getTableColumns(db, TABLE_SUBJECTS);
            addColumnIfNotExists(db, TABLE_SUBJECTS, subjectCols, "program_level", "TEXT DEFAULT 'UG'");
            addColumnIfNotExists(db, TABLE_SUBJECTS, subjectCols, "department_id", "TEXT");
            addColumnIfNotExists(db, TABLE_SUBJECTS, subjectCols, "department_short_name", "TEXT");

            try { db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ASSESSMENTS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, type TEXT, subject_id INTEGER, semester INTEGER, date TEXT, max_marks REAL, status TEXT DEFAULT 'PENDING')"); } catch (Exception ignored) {}
            try { db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PASSWORD_RESETS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, request_id TEXT UNIQUE, user_id TEXT, user_name TEXT, email TEXT, role TEXT, identifier TEXT, department TEXT, semester TEXT, status TEXT DEFAULT 'PENDING', requested_at TEXT, processed_at TEXT, processed_by TEXT, admin_note TEXT)"); } catch (Exception ignored) {}
            try { db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_STUDENT_ATTENDANCE_LOGS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER, subject_name TEXT, total_classes INTEGER, attended_classes INTEGER, percentage REAL, logged_date TEXT, notes TEXT, threshold REAL, status TEXT)"); } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Teachers Table
        String CREATE_TEACHERS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_TEACHERS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT, "
                + "email TEXT UNIQUE, "
                + "phone TEXT, "
                + "department TEXT, "
                + "employee_id TEXT UNIQUE, "
                + "password TEXT, "
                + "photo_uri TEXT, "
                + "firebase_uid TEXT, "
                + "login_id TEXT)";
        db.execSQL(CREATE_TEACHERS_TABLE);

        // Students Table
        String CREATE_STUDENTS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_STUDENTS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT, "
                + "email TEXT UNIQUE, "
                + "phone TEXT, "
                + "department TEXT, "
                + "register_number TEXT UNIQUE, "
                + "reg_no TEXT, "
                + "semester INTEGER, "
                + "password TEXT, "
                + "photo_uri TEXT, "
                + "student_uid TEXT, "
                + "firebase_uid TEXT, "
                + "login_id TEXT)";
        db.execSQL(CREATE_STUDENTS_TABLE);

        // Subjects Table
        String CREATE_SUBJECTS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_SUBJECTS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "subject_code TEXT UNIQUE, "
                + "subject_name TEXT, "
                + "credits INTEGER, "
                + "semester INTEGER, "
                + "department TEXT, "
                + "program_level TEXT DEFAULT 'UG', "
                + "department_id TEXT, "
                + "department_short_name TEXT)";
        db.execSQL(CREATE_SUBJECTS_TABLE);

        // Marks Table
        String CREATE_MARKS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_MARKS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "student_id INTEGER, "
                + "subject_id INTEGER, "
                + "internal1 REAL, "
                + "internal2 REAL, "
                + "assignment REAL, "
                + "seminar REAL, "
                + "lab REAL, "
                + "model_exam REAL, "
                + "university_exam REAL, "
                + "total_marks REAL, "
                + "percentage REAL, "
                + "grade TEXT, "
                + "grade_point REAL)";
        db.execSQL(CREATE_MARKS_TABLE);

        // Attendance Table
        String CREATE_ATTENDANCE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_ATTENDANCE + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "student_id INTEGER, "
                + "subject_id INTEGER, "
                + "date TEXT, "
                + "status TEXT)"; // "PRESENT", "ABSENT"
        db.execSQL(CREATE_ATTENDANCE_TABLE);

        // Assignments Table
        String CREATE_ASSIGNMENTS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_ASSIGNMENTS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "title TEXT, "
                + "subject_id INTEGER, "
                + "deadline TEXT, "
                + "description TEXT, "
                + "file_path TEXT)";
        db.execSQL(CREATE_ASSIGNMENTS_TABLE);

        // Submissions Table
        String CREATE_SUBMISSIONS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_SUBMISSIONS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "assignment_id INTEGER, "
                + "student_id INTEGER, "
                + "submission_date TEXT, "
                + "file_path TEXT, "
                + "status TEXT, "
                + "remarks TEXT)";
        db.execSQL(CREATE_SUBMISSIONS_TABLE);

        // Results Table
        String CREATE_RESULTS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_RESULTS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "student_id INTEGER, "
                + "semester INTEGER, "
                + "total_marks REAL DEFAULT 0, "
                + "percentage REAL DEFAULT 0, "
                + "total_credits INTEGER DEFAULT 0, "
                + "sgpa REAL, "
                + "cgpa REAL, "
                + "status TEXT DEFAULT 'DRAFT', "
                + "published_date TEXT, "
                + "version INTEGER DEFAULT 1, "
                + "approval_status TEXT DEFAULT 'DRAFT', "
                + "submitted_at TEXT, "
                + "approved_at TEXT, "
                + "approved_by TEXT, "
                + "rejection_reason TEXT, "
                + "student_uid TEXT, "
                + "sync_status TEXT DEFAULT 'SYNCED')";
        db.execSQL(CREATE_RESULTS_TABLE);

        // Notifications Table
        String CREATE_NOTIFICATIONS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_NOTIFICATIONS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "title TEXT, "
                + "message TEXT, "
                + "date TEXT, "
                + "target_role TEXT)"; // "ALL", "TEACHER", "STUDENT"
        db.execSQL(CREATE_NOTIFICATIONS_TABLE);

        // Admins Table
        String CREATE_ADMINS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_ADMINS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "email TEXT UNIQUE, "
                + "password TEXT)";
        db.execSQL(CREATE_ADMINS_TABLE);

        // Activity Logs Table
        String CREATE_ACTIVITY_LOGS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_ACTIVITY_LOGS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "teacher_name TEXT, "
                + "action_title TEXT, "
                + "description TEXT, "
                + "timestamp TEXT, "
                + "category TEXT)";
        db.execSQL(CREATE_ACTIVITY_LOGS_TABLE);

        // Password Reset Requests Table
        String CREATE_PASSWORD_RESETS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_PASSWORD_RESETS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "request_id TEXT UNIQUE, "
                + "user_id TEXT, "
                + "user_name TEXT, "
                + "email TEXT, "
                + "role TEXT, "
                + "identifier TEXT, "
                + "department TEXT, "
                + "semester TEXT, "
                + "status TEXT DEFAULT 'PENDING', "
                + "requested_at TEXT, "
                + "processed_at TEXT, "
                + "processed_by TEXT, "
                + "admin_note TEXT)";
        db.execSQL(CREATE_PASSWORD_RESETS_TABLE);

        // Student Attendance Logs Table for Class Attendance Monitoring
        String CREATE_STUDENT_ATTENDANCE_LOGS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_STUDENT_ATTENDANCE_LOGS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "student_id INTEGER, "
                + "subject_name TEXT, "
                + "total_classes INTEGER, "
                + "attended_classes INTEGER, "
                + "percentage REAL, "
                + "logged_date TEXT, "
                + "notes TEXT, "
                + "threshold REAL, "
                + "status TEXT)";
        db.execSQL(CREATE_STUDENT_ATTENDANCE_LOGS_TABLE);

        // Seed Default Credentials
        seedDefaultData(db);
    }

    private void seedDefaultData(SQLiteDatabase db) {
        wipeAllDataExceptAdmin(db);
    }

    public void wipeAllDataExceptAdmin(SQLiteDatabase db) {
        try {
            db.delete(TABLE_TEACHERS, null, null);
            db.delete(TABLE_STUDENTS, null, null);
            db.delete(TABLE_SUBJECTS, null, null);
            db.delete(TABLE_MARKS, null, null);
            db.delete(TABLE_ATTENDANCE, null, null);
            db.delete(TABLE_ASSIGNMENTS, null, null);
            db.delete(TABLE_SUBMISSIONS, null, null);
            db.delete(TABLE_RESULTS, null, null);
            db.delete(TABLE_NOTIFICATIONS, null, null);
            db.delete(TABLE_ACTIVITY_LOGS, null, null);
        } catch (Exception ignored) {}

        // Ensure Default Admin Account exists so Admin can login and create Teachers & Students
        ContentValues adminValues = new ContentValues();
        adminValues.put("email", "admin@gradexpert.com");
        adminValues.put("password", PasswordUtils.hashPassword("123456"));
        db.insertWithOnConflict(TABLE_ADMINS, null, adminValues, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void purgeAllLocalData() {
        SQLiteDatabase db = getWritableDatabase();
        wipeAllDataExceptAdmin(db);
    }

    private void insertDefaultSubject(SQLiteDatabase db, String code, String name, int credits, int sem, String dept) {
        ContentValues cv = new ContentValues();
        cv.put("subject_code", code);
        cv.put("subject_name", name);
        cv.put("credits", credits);
        cv.put("semester", sem);
        cv.put("department", dept);
        long id = db.insertWithOnConflict(TABLE_SUBJECTS, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        if (id != -1) {
            try {
                FirestoreHelper.getInstance().syncSubject(new Subject((int) id, code, name, credits, sem, dept));
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            Set<String> studentCols = getTableColumns(db, TABLE_STUDENTS);
            addColumnIfNotExists(db, TABLE_STUDENTS, studentCols, "login_id", "TEXT");
            addColumnIfNotExists(db, TABLE_STUDENTS, studentCols, "firebase_uid", "TEXT");
            addColumnIfNotExists(db, TABLE_STUDENTS, studentCols, "reg_no", "TEXT");

            Set<String> teacherCols = getTableColumns(db, TABLE_TEACHERS);
            addColumnIfNotExists(db, TABLE_TEACHERS, teacherCols, "login_id", "TEXT");
            addColumnIfNotExists(db, TABLE_TEACHERS, teacherCols, "firebase_uid", "TEXT");
        }
    }

    // ==========================================
    // AUTHENTICATION METHODS
    // ==========================================

    public User authenticateTeacher(String emailOrId, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_TEACHERS,
                    new String[]{"id", "name", "email", "password", "login_id"},
                    "(email=? OR id=? OR login_id=? OR employee_id=?)",
                    new String[]{emailOrId, emailOrId, emailOrId, emailOrId},
                    null, null, null);
        } catch (Exception e) {
            // Fallback if older schema
            try {
                cursor = db.query(TABLE_TEACHERS,
                        new String[]{"id", "name", "email", "password"},
                        "(email=? OR id=?)",
                        new String[]{emailOrId, emailOrId},
                        null, null, null);
            } catch (Exception ignored) {}
        }

        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            String email = cursor.getString(cursor.getColumnIndexOrThrow("email"));
            String storedPassword = cursor.getString(cursor.getColumnIndexOrThrow("password"));
            String loginId = "";
            try {
                int colIdx = cursor.getColumnIndex("login_id");
                if (colIdx != -1) loginId = cursor.getString(colIdx);
            } catch (Exception ignored) {}
            cursor.close();

            if (PasswordUtils.verifyPassword(password, storedPassword)) {
                if (PasswordUtils.isLegacyPlainText(storedPassword)) {
                    ContentValues cv = new ContentValues();
                    cv.put("password", PasswordUtils.hashPassword(password));
                    db.update(TABLE_TEACHERS, cv, "id=?", new String[]{String.valueOf(id)});
                }
                String identifier = (loginId != null && !loginId.isEmpty()) ? loginId : String.valueOf(id);
                return new User(id, name, email, password, "TEACHER", identifier);
            }
        }
        if (cursor != null) cursor.close();
        return null;
    }

    public User authenticateStudent(String regNoOrEmail, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_STUDENTS,
                    new String[]{"id", "name", "email", "reg_no", "password", "login_id"},
                    "(reg_no=? OR register_number=? OR email=? OR login_id=?)",
                    new String[]{regNoOrEmail, regNoOrEmail, regNoOrEmail, regNoOrEmail},
                    null, null, null);
        } catch (Exception e) {
            // Fallback if older schema
            try {
                cursor = db.query(TABLE_STUDENTS,
                        new String[]{"id", "name", "email", "reg_no", "password"},
                        "(reg_no=? OR email=?)",
                        new String[]{regNoOrEmail, regNoOrEmail},
                        null, null, null);
            } catch (Exception ignored) {}
        }

        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            String email = cursor.getString(cursor.getColumnIndexOrThrow("email"));
            String regNo = cursor.getString(cursor.getColumnIndexOrThrow("reg_no"));
            String storedPassword = cursor.getString(cursor.getColumnIndexOrThrow("password"));
            String loginId = "";
            try {
                int colIdx = cursor.getColumnIndex("login_id");
                if (colIdx != -1) loginId = cursor.getString(colIdx);
            } catch (Exception ignored) {}
            cursor.close();

            if (PasswordUtils.verifyPassword(password, storedPassword)) {
                if (PasswordUtils.isLegacyPlainText(storedPassword)) {
                    ContentValues cv = new ContentValues();
                    cv.put("password", PasswordUtils.hashPassword(password));
                    db.update(TABLE_STUDENTS, cv, "id=?", new String[]{String.valueOf(id)});
                }
                String identifier = (loginId != null && !loginId.isEmpty()) ? loginId : regNo;
                return new User(id, name, email, password, "STUDENT", identifier);
            }
        }
        if (cursor != null) cursor.close();
        return null;
    }

    public boolean resetTeacherPassword(String emailOrId, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("password", PasswordUtils.hashPassword(newPassword));
        int rows = db.update(TABLE_TEACHERS, cv, "email=? OR employee_id=? OR login_id=?", new String[]{emailOrId, emailOrId, emailOrId});
        return rows > 0;
    }

    public boolean resetStudentPassword(String identifier, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("password", PasswordUtils.hashPassword(newPassword));
        int rows = db.update(TABLE_STUDENTS, cv, "email=? OR reg_no=? OR register_number=? OR login_id=?", new String[]{identifier, identifier, identifier, identifier});
        return rows > 0;
    }

    public boolean changeUserPassword(int userId, boolean isTeacher, String oldPassword, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        String table = isTeacher ? TABLE_TEACHERS : TABLE_STUDENTS;

        Cursor cursor = db.query(table, new String[]{"password"}, "id=?", new String[]{String.valueOf(userId)}, null, null, null);
        boolean isValid = false;
        if (cursor != null && cursor.moveToFirst()) {
            String storedPassword = cursor.getString(cursor.getColumnIndexOrThrow("password"));
            isValid = PasswordUtils.verifyPassword(oldPassword, storedPassword);
        }
        if (cursor != null) cursor.close();

        if (!isValid) return false;

        ContentValues cv = new ContentValues();
        cv.put("password", PasswordUtils.hashPassword(newPassword));
        int updated = db.update(table, cv, "id=?", new String[]{String.valueOf(userId)});
        return updated > 0;
    }

    public boolean updateUserPassword(String identifier, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("password", PasswordUtils.hashPassword(newPassword));
        int sRows = db.update(TABLE_STUDENTS, cv, "email=? OR reg_no=?", new String[]{identifier, identifier});
        if (sRows > 0) return true;
        int tRows = db.update(TABLE_TEACHERS, cv, "email=?", new String[]{identifier});
        return tRows > 0;
    }

    public boolean updateUserProfileData(int userId, boolean isTeacher, String name, String email, String phone) {
        SQLiteDatabase db = this.getWritableDatabase();
        String table = isTeacher ? TABLE_TEACHERS : TABLE_STUDENTS;

        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("email", email);
        if (phone != null && !phone.isEmpty()) {
            cv.put("phone", phone);
        }

        int updated = db.update(table, cv, "id=?", new String[]{String.valueOf(userId)});
        return updated > 0;
    }

    // ==========================================
    // DASHBOARD STATS & COUNTS
    // ==========================================

    public int getStudentsCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_STUDENTS, null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public int getSubjectsCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SUBJECTS, null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public int getAssignmentsCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_ASSIGNMENTS, null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public int getPendingAssessmentsCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ASSESSMENTS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, type TEXT, subject_id INTEGER, semester INTEGER, date TEXT, max_marks REAL, status TEXT DEFAULT 'PENDING')");
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_ASSESSMENTS + " WHERE status = 'PENDING'", null);
            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0);
                cursor.close();
            }
            if (count == 0) {
                Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_STUDENTS + " WHERE id NOT IN (SELECT DISTINCT student_id FROM " + TABLE_MARKS + ")", null);
                if (c != null && c.moveToFirst()) {
                    count = c.getInt(0);
                    c.close();
                }
            }
        } catch (Exception e) {
            count = 0;
        }
        return count;
    }

    // ==========================================
    // STUDENT PROFILE & ACADEMIC DATA (CRUD)
    // ==========================================

    public List<Student> getAllStudents() {
        List<Student> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_STUDENTS, null, null, null, null, null, "name ASC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String regNo = cursor.getString(cursor.getColumnIndexOrThrow("reg_no"));
                String dept = cursor.getString(cursor.getColumnIndexOrThrow("department"));
                int sem = cursor.getInt(cursor.getColumnIndexOrThrow("semester"));
                String email = cursor.getString(cursor.getColumnIndexOrThrow("email"));
                String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
                String photoUri = cursor.getString(cursor.getColumnIndexOrThrow("photo_uri"));
                String studentUid = null;
                int uidCol = cursor.getColumnIndex("student_uid");
                if (uidCol != -1 && !cursor.isNull(uidCol)) {
                    studentUid = cursor.getString(uidCol);
                }
                int fbUidCol = cursor.getColumnIndex("firebase_uid");
                if (fbUidCol != -1 && !cursor.isNull(fbUidCol)) {
                    String val = cursor.getString(fbUidCol);
                    if (val != null && !val.trim().isEmpty()) {
                        studentUid = val.trim();
                    }
                }
                Student st = new Student(id, name, regNo, dept, sem, email, phone, photoUri);
                if (studentUid != null && !studentUid.trim().isEmpty()) {
                    st.setFirebaseUid(studentUid.trim());
                }
                int loginIdCol = cursor.getColumnIndex("login_id");
                if (loginIdCol != -1 && !cursor.isNull(loginIdCol)) {
                    st.setLoginId(cursor.getString(loginIdCol));
                }
                list.add(st);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public List<Student> searchStudents(String query) {
        List<Student> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = "name LIKE ? OR reg_no LIKE ? OR department LIKE ? OR email LIKE ? OR CAST(semester AS TEXT) LIKE ?";
        String wild = "%" + query + "%";
        String[] selectionArgs = new String[]{wild, wild, wild, wild, wild};
        Cursor cursor = db.query(TABLE_STUDENTS, null, selection, selectionArgs, null, null, "name ASC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String regNo = cursor.getString(cursor.getColumnIndexOrThrow("reg_no"));
                String dept = cursor.getString(cursor.getColumnIndexOrThrow("department"));
                int sem = cursor.getInt(cursor.getColumnIndexOrThrow("semester"));
                String email = cursor.getString(cursor.getColumnIndexOrThrow("email"));
                String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
                String photoUri = cursor.getString(cursor.getColumnIndexOrThrow("photo_uri"));
                String studentUid = null;
                int uidCol = cursor.getColumnIndex("student_uid");
                if (uidCol != -1 && !cursor.isNull(uidCol)) {
                    studentUid = cursor.getString(uidCol);
                }
                int fbUidCol = cursor.getColumnIndex("firebase_uid");
                if (fbUidCol != -1 && !cursor.isNull(fbUidCol)) {
                    String val = cursor.getString(fbUidCol);
                    if (val != null && !val.trim().isEmpty()) {
                        studentUid = val.trim();
                    }
                }
                Student st = new Student(id, name, regNo, dept, sem, email, phone, photoUri);
                if (studentUid != null && !studentUid.trim().isEmpty()) {
                    st.setFirebaseUid(studentUid.trim());
                }
                int loginIdCol = cursor.getColumnIndex("login_id");
                if (loginIdCol != -1 && !cursor.isNull(loginIdCol)) {
                    st.setLoginId(cursor.getString(loginIdCol));
                }
                list.add(st);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public long addStudent(Student student, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", student.getName());
        cv.put("reg_no", student.getRegNo());
        cv.put("register_number", student.getRegNo());
        cv.put("department", student.getDepartment());
        cv.put("semester", student.getSemester());
        cv.put("email", student.getEmail());
        cv.put("phone", student.getPhone());
        String rawPwd = (password != null && !password.isEmpty()) ? password : "password123";
        cv.put("password", PasswordUtils.hashPassword(rawPwd));
        cv.put("photo_uri", student.getPhotoUri() != null ? student.getPhotoUri() : "");
        if (student.getFirebaseUid() != null && !student.getFirebaseUid().isEmpty()) {
            cv.put("student_uid", student.getFirebaseUid());
            cv.put("firebase_uid", student.getFirebaseUid());
        } else if (student.getStudentId() != null && !student.getStudentId().isEmpty()) {
            cv.put("student_uid", student.getStudentId());
            cv.put("firebase_uid", student.getStudentId());
        }
        if (student.getLoginId() != null && !student.getLoginId().isEmpty()) {
            cv.put("login_id", student.getLoginId());
        }
        long id = db.insert(TABLE_STUDENTS, null, cv);
        if (id != -1) {
            try {
                Student sSync = new Student(String.valueOf(id), student.getName(), student.getRegNo(), student.getEmail(), student.getPhone(), student.getDepartment(), student.getDepartmentId(), student.getDepartmentShortName(), student.getProgramLevel(), student.getSemester(), student.getSection(), student.getGender(), student.getDateOfBirth(), student.getPhotoUri(), "ACTIVE");
                sSync.setLoginId(student.getLoginId());
                sSync.setFirebaseUid(student.getFirebaseUid());
                FirestoreHelper.getInstance().syncStudent(sSync);
            } catch (Exception ignored) {}
        }
        return id;
    }

    public boolean updateTeacherPassword(String email, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("password", PasswordUtils.hashPassword(newPassword));
        int rows = db.update(TABLE_TEACHERS, cv, "email=?", new String[]{email});
        return rows > 0;
    }

    public boolean updateStudent(Student student) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", student.getName());
        cv.put("reg_no", student.getRegNo());
        cv.put("register_number", student.getRegNo());
        cv.put("department", student.getDepartment());
        cv.put("semester", student.getSemester());
        cv.put("email", student.getEmail());
        cv.put("phone", student.getPhone());
        if (student.getPhotoUri() != null) {
            cv.put("photo_uri", student.getPhotoUri());
        }
        int rows = 0;
        if (student.getId() > 0) {
            rows = db.update(TABLE_STUDENTS, cv, "id=?", new String[]{String.valueOf(student.getId())});
        }
        if (rows == 0 && student.getRegNo() != null && !student.getRegNo().isEmpty()) {
            rows = db.update(TABLE_STUDENTS, cv, "reg_no=? OR register_number=?", new String[]{student.getRegNo(), student.getRegNo()});
        }
        if (rows == 0 && student.getEmail() != null && !student.getEmail().isEmpty()) {
            rows = db.update(TABLE_STUDENTS, cv, "email=?", new String[]{student.getEmail()});
        }
        if (rows > 0) {
            try { FirestoreHelper.getInstance().syncStudent(student); } catch (Exception ignored) {}
        }
        return rows > 0;
    }

    public boolean updateStudentWithPassword(Student student, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", student.getName());
        cv.put("reg_no", student.getRegNo());
        cv.put("register_number", student.getRegNo());
        cv.put("department", student.getDepartment());
        cv.put("semester", student.getSemester());
        cv.put("email", student.getEmail());
        cv.put("phone", student.getPhone());
        if (password != null && !password.isEmpty()) {
            cv.put("password", PasswordUtils.hashPassword(password));
        }
        if (student.getPhotoUri() != null) {
            cv.put("photo_uri", student.getPhotoUri());
        }
        int rows = 0;
        if (student.getId() > 0) {
            rows = db.update(TABLE_STUDENTS, cv, "id=?", new String[]{String.valueOf(student.getId())});
        }
        if (rows == 0 && student.getRegNo() != null && !student.getRegNo().isEmpty()) {
            rows = db.update(TABLE_STUDENTS, cv, "reg_no=? OR register_number=?", new String[]{student.getRegNo(), student.getRegNo()});
        }
        if (rows == 0 && student.getEmail() != null && !student.getEmail().isEmpty()) {
            rows = db.update(TABLE_STUDENTS, cv, "email=?", new String[]{student.getEmail()});
        }
        if (rows > 0) {
            try { FirestoreHelper.getInstance().syncStudent(student); } catch (Exception ignored) {}
        }
        return rows > 0;
    }

    public boolean updateStudentPhotoUri(int studentId, String regNo, String photoUri) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("photo_uri", photoUri != null ? photoUri : "");
        int rows = 0;
        if (studentId > 0) {
            rows = db.update(TABLE_STUDENTS, cv, "id=?", new String[]{String.valueOf(studentId)});
        }
        if (rows == 0 && regNo != null && !regNo.isEmpty()) {
            rows = db.update(TABLE_STUDENTS, cv, "reg_no=? OR register_number=?", new String[]{regNo, regNo});
        }
        return rows > 0;
    }

    public String getStudentPhotoUri(int studentId, String regNo) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            if (studentId > 0) {
                cursor = db.query(TABLE_STUDENTS, new String[]{"photo_uri"}, "id=?", new String[]{String.valueOf(studentId)}, null, null, null);
            }
            if ((cursor == null || !cursor.moveToFirst()) && regNo != null && !regNo.isEmpty()) {
                if (cursor != null) cursor.close();
                cursor = db.query(TABLE_STUDENTS, new String[]{"photo_uri"}, "reg_no=? OR register_number=?", new String[]{regNo, regNo}, null, null, null);
            }
            if (cursor != null && cursor.moveToFirst()) {
                int col = cursor.getColumnIndex("photo_uri");
                if (col != -1 && !cursor.isNull(col)) {
                    return cursor.getString(col);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    public Student getStudentById(int studentId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_STUDENTS, null, "id=?", new String[]{String.valueOf(studentId)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            String regNo = cursor.getString(cursor.getColumnIndexOrThrow("reg_no"));
            String dept = cursor.getString(cursor.getColumnIndexOrThrow("department"));
            int sem = cursor.getInt(cursor.getColumnIndexOrThrow("semester"));
            String email = cursor.getString(cursor.getColumnIndexOrThrow("email"));
            String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
            String photoUri = cursor.getString(cursor.getColumnIndexOrThrow("photo_uri"));
            String studentUid = null;
            int uidCol = cursor.getColumnIndex("student_uid");
            if (uidCol != -1 && !cursor.isNull(uidCol)) {
                studentUid = cursor.getString(uidCol);
            }
            int fbUidCol = cursor.getColumnIndex("firebase_uid");
            if (fbUidCol != -1 && !cursor.isNull(fbUidCol)) {
                String val = cursor.getString(fbUidCol);
                if (val != null && !val.trim().isEmpty()) {
                    studentUid = val.trim();
                }
            }
            Student st = new Student(id, name, regNo, dept, sem, email, phone, photoUri);
            if (studentUid != null && !studentUid.trim().isEmpty()) {
                st.setFirebaseUid(studentUid.trim());
            }
            int loginIdCol = cursor.getColumnIndex("login_id");
            if (loginIdCol != -1 && !cursor.isNull(loginIdCol)) {
                st.setLoginId(cursor.getString(loginIdCol));
            }
            cursor.close();
            return st;
        }
        if (cursor != null) cursor.close();
        return null;
    }

    public boolean deleteStudent(int studentId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Also delete student marks & attendance to maintain integrity
        db.delete(TABLE_MARKS, "student_id=?", new String[]{String.valueOf(studentId)});
        db.delete(TABLE_ATTENDANCE, "student_id=?", new String[]{String.valueOf(studentId)});
        db.delete(TABLE_RESULTS, "student_id=?", new String[]{String.valueOf(studentId)});
        int rows = db.delete(TABLE_STUDENTS, "id=?", new String[]{String.valueOf(studentId)});
        if (rows > 0) {
            try { FirestoreHelper.getInstance().deleteStudent(studentId); } catch (Exception ignored) {}
        }
        return rows > 0;
    }

    public Student getStudentDetails(String regNoOrEmail) {
        if (regNoOrEmail == null || regNoOrEmail.trim().isEmpty()) return null;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_STUDENTS,
                    null,
                    "reg_no=? OR email=?",
                    new String[]{regNoOrEmail.trim(), regNoOrEmail.trim()},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String regNo = cursor.getString(cursor.getColumnIndexOrThrow("reg_no"));
                String dept = cursor.getString(cursor.getColumnIndexOrThrow("department"));
                int sem = cursor.getInt(cursor.getColumnIndexOrThrow("semester"));
                String email = cursor.getString(cursor.getColumnIndexOrThrow("email"));
                String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
                String photoUri = cursor.getString(cursor.getColumnIndexOrThrow("photo_uri"));
                String studentUid = null;
                int uidCol = cursor.getColumnIndex("student_uid");
                if (uidCol != -1 && !cursor.isNull(uidCol)) {
                    studentUid = cursor.getString(uidCol);
                }
                int fbUidCol = cursor.getColumnIndex("firebase_uid");
                if (fbUidCol != -1 && !cursor.isNull(fbUidCol)) {
                    String val = cursor.getString(fbUidCol);
                    if (val != null && !val.trim().isEmpty()) {
                        studentUid = val.trim();
                    }
                }
                Student st = new Student(id, name, regNo, dept, sem, email, phone, photoUri);
                if (studentUid != null && !studentUid.trim().isEmpty()) {
                    st.setFirebaseUid(studentUid.trim());
                }
                int loginIdCol = cursor.getColumnIndex("login_id");
                if (loginIdCol != -1 && !cursor.isNull(loginIdCol)) {
                    st.setLoginId(cursor.getString(loginIdCol));
                }
                return st;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    public String getStudentFirebaseUid(int numericId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_STUDENTS, new String[]{"student_uid"}, "id=?", new String[]{String.valueOf(numericId)}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int col = cursor.getColumnIndex("student_uid");
                if (col != -1 && !cursor.isNull(col)) {
                    String uid = cursor.getString(col);
                    if (uid != null && !uid.trim().isEmpty()) return uid.trim();
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    public String getStudentFirebaseUidByRegNo(String regNo) {
        if (regNo == null || regNo.trim().isEmpty()) return null;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_STUDENTS, new String[]{"student_uid"}, "reg_no=?", new String[]{regNo.trim()}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int col = cursor.getColumnIndex("student_uid");
                if (col != -1 && !cursor.isNull(col)) {
                    String uid = cursor.getString(col);
                    if (uid != null && !uid.trim().isEmpty()) return uid.trim();
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    public Student getStudentByFirebaseUid(String firebaseUid) {
        if (firebaseUid == null || firebaseUid.trim().isEmpty()) return null;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_STUDENTS, null, "student_uid=?", new String[]{firebaseUid.trim()}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String regNo = cursor.getString(cursor.getColumnIndexOrThrow("reg_no"));
                String dept = cursor.getString(cursor.getColumnIndexOrThrow("department"));
                int sem = cursor.getInt(cursor.getColumnIndexOrThrow("semester"));
                String email = cursor.getString(cursor.getColumnIndexOrThrow("email"));
                String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
                String photoUri = cursor.getString(cursor.getColumnIndexOrThrow("photo_uri"));
                Student st = new Student(id, name, regNo, dept, sem, email, phone, photoUri);
                st.setFirebaseUid(firebaseUid.trim());
                return st;
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    public boolean updateStudentContact(int id, String name, String email, String phone) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("email", email);
        cv.put("phone", phone);
        int rows = db.update(TABLE_STUDENTS, cv, "id=?", new String[]{String.valueOf(id)});
        return rows > 0;
    }

    public double getLatestCGPA(int studentId) {
        if (studentId <= 0) return 0.0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        double cgpa = 0.0;
        try {
            cursor = db.rawQuery("SELECT cgpa FROM " + TABLE_RESULTS + " WHERE student_id=? AND (status='APPROVED' OR status='PUBLISHED') ORDER BY semester DESC LIMIT 1", new String[]{String.valueOf(studentId)});
            if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) {
                cgpa = cursor.getDouble(0);
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return cgpa;
    }

    public double getLatestSGPA(int studentId) {
        if (studentId <= 0) return 0.0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        double sgpa = 0.0;
        try {
            cursor = db.rawQuery("SELECT sgpa FROM " + TABLE_RESULTS + " WHERE student_id=? AND (status='APPROVED' OR status='PUBLISHED') ORDER BY semester DESC LIMIT 1", new String[]{String.valueOf(studentId)});
            if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) {
                sgpa = cursor.getDouble(0);
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return sgpa;
    }

    public int getStudentEarnedCredits(int studentId) {
        if (studentId <= 0) return 0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        int credits = 0;
        try {
            cursor = db.rawQuery("SELECT total_credits FROM " + TABLE_RESULTS + " WHERE student_id=? AND (status='APPROVED' OR status='PUBLISHED') ORDER BY semester DESC LIMIT 1", new String[]{String.valueOf(studentId)});
            if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) {
                credits = cursor.getInt(0);
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }

        // If no credit recorded in results, sum from subjects
        if (credits == 0) {
            Cursor cursorSub = null;
            try {
                cursorSub = db.rawQuery("SELECT SUM(credits) FROM " + TABLE_SUBJECTS, null);
                if (cursorSub != null && cursorSub.moveToFirst() && !cursorSub.isNull(0)) {
                    credits = cursorSub.getInt(0);
                }
            } catch (Exception ignored) {
            } finally {
                if (cursorSub != null) cursorSub.close();
            }
        }
        return credits;
    }

    public double calculateCGPAWithNewSemester(int studentId, int currentSem, double currentSgpa, int currentCredits) {
        if (studentId <= 0) return currentSgpa;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        double totalPoints = 0.0;
        int totalCredits = 0;
        try {
            cursor = db.rawQuery("SELECT semester, sgpa, total_credits FROM " + TABLE_RESULTS + " WHERE student_id=? AND (status='APPROVED' OR status='PUBLISHED') AND semester != ?", new String[]{String.valueOf(studentId), String.valueOf(currentSem)});
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    double semSgpa = cursor.getDouble(cursor.getColumnIndexOrThrow("sgpa"));
                    int semCredits = 0;
                    int credCol = cursor.getColumnIndex("total_credits");
                    if (credCol != -1 && !cursor.isNull(credCol)) {
                        semCredits = cursor.getInt(credCol);
                    }
                    if (semCredits <= 0) semCredits = 20; // Default standard credits
                    totalPoints += (semSgpa * semCredits);
                    totalCredits += semCredits;
                } while (cursor.moveToNext());
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }

        int curCred = currentCredits > 0 ? currentCredits : 20;
        totalPoints += (currentSgpa * curCred);
        totalCredits += curCred;

        return totalCredits > 0 ? (totalPoints / totalCredits) : currentSgpa;
    }

    public int getStudentAttendancePercentage(int studentId) {
        if (studentId <= 0) return 0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursorTotal = null;
        Cursor cursorPresent = null;
        int total = 0, present = 0;
        try {
            cursorTotal = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_ATTENDANCE + " WHERE student_id=?", new String[]{String.valueOf(studentId)});
            cursorPresent = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_ATTENDANCE + " WHERE student_id=? AND status='PRESENT'", new String[]{String.valueOf(studentId)});

            if (cursorTotal != null && cursorTotal.moveToFirst()) {
                total = cursorTotal.getInt(0);
            }
            if (cursorPresent != null && cursorPresent.moveToFirst()) {
                present = cursorPresent.getInt(0);
            }
        } catch (Exception ignored) {
        } finally {
            if (cursorTotal != null) cursorTotal.close();
            if (cursorPresent != null) cursorPresent.close();
        }

        if (total == 0) return 0;
        return (int) Math.round(((double) present / total) * 100);
    }

    // ==========================================
    // SUBJECT MANAGEMENT (CRUD)
    // ==========================================

    public List<Subject> getAllSubjects() {
        List<Subject> list = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_SUBJECTS, null, null, null, null, null, "semester ASC, subject_code ASC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String code = cursor.getString(cursor.getColumnIndexOrThrow("subject_code"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("subject_name"));
                int credits = cursor.getInt(cursor.getColumnIndexOrThrow("credits"));
                int sem = cursor.getInt(cursor.getColumnIndexOrThrow("semester"));
                String dept = cursor.getString(cursor.getColumnIndexOrThrow("department"));
                String pLevel = "UG";
                int pCol = cursor.getColumnIndex("program_level");
                if (pCol >= 0) {
                    String val = cursor.getString(pCol);
                    if (val != null && !val.isEmpty()) pLevel = val;
                }
                String dId = "";
                int dIdCol = cursor.getColumnIndex("department_id");
                if (dIdCol >= 0) {
                    String val = cursor.getString(dIdCol);
                    if (val != null) dId = val;
                }
                String dShort = "";
                int dShortCol = cursor.getColumnIndex("department_short_name");
                if (dShortCol >= 0) {
                    String val = cursor.getString(dShortCol);
                    if (val != null) dShort = val;
                }

                Subject s = new Subject(id, code, name, credits, sem, dept);
                s.setProgramLevel(pLevel);
                s.setDepartmentId(dId);
                s.setDepartmentShortName(dShort);
                list.add(s);
            } while (cursor.moveToNext());
            cursor.close();
        }

        return list;
    }

    public List<Subject> searchSubjects(String query) {
        List<Subject> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = "subject_code LIKE ? OR subject_name LIKE ? OR department LIKE ? OR CAST(semester AS TEXT) LIKE ? OR CAST(credits AS TEXT) LIKE ?";
        String wild = "%" + query + "%";
        String[] selectionArgs = new String[]{wild, wild, wild, wild, wild};
        Cursor cursor = db.query(TABLE_SUBJECTS, null, selection, selectionArgs, null, null, "semester ASC, subject_code ASC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String code = cursor.getString(cursor.getColumnIndexOrThrow("subject_code"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("subject_name"));
                int credits = cursor.getInt(cursor.getColumnIndexOrThrow("credits"));
                int sem = cursor.getInt(cursor.getColumnIndexOrThrow("semester"));
                String dept = cursor.getString(cursor.getColumnIndexOrThrow("department"));
                String pLevel = "UG";
                int pCol = cursor.getColumnIndex("program_level");
                if (pCol >= 0) {
                    String val = cursor.getString(pCol);
                    if (val != null && !val.isEmpty()) pLevel = val;
                }
                Subject s = new Subject(id, code, name, credits, sem, dept);
                s.setProgramLevel(pLevel);
                list.add(s);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public long addSubject(Subject subject) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("subject_code", subject.getSubjectCode());
        cv.put("subject_name", subject.getSubjectName());
        cv.put("credits", subject.getCredits());
        cv.put("semester", Subject.parseSemesterNumber(subject.getSemester()));
        cv.put("department", subject.getDepartment());
        cv.put("program_level", subject.getProgramLevel() != null ? subject.getProgramLevel() : "UG");
        cv.put("department_id", subject.getDepartmentId() != null ? subject.getDepartmentId() : "");
        cv.put("department_short_name", subject.getDepartmentShortName() != null ? subject.getDepartmentShortName() : "");
        long id = db.insertWithOnConflict(TABLE_SUBJECTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        if (id != -1) {
            try {
                FirestoreHelper.getInstance().syncSubject(new Subject((int) id, subject.getSubjectCode(), subject.getSubjectName(), subject.getCredits(), subject.getSemester(), subject.getDepartment()));
            } catch (Exception ignored) {}
        }
        return id;
    }

    public void upsertSubject(Subject subject) {
        if (subject == null || subject.getSubjectCode() == null || subject.getSubjectCode().isEmpty()) return;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("subject_code", subject.getSubjectCode());
        cv.put("subject_name", subject.getSubjectName());
        cv.put("credits", subject.getCredits());
        cv.put("semester", Subject.parseSemesterNumber(subject.getSemester()));
        cv.put("department", subject.getDepartment() != null ? subject.getDepartment() : "");
        cv.put("program_level", subject.getProgramLevel() != null ? subject.getProgramLevel() : "UG");
        cv.put("department_id", subject.getDepartmentId() != null ? subject.getDepartmentId() : "");
        cv.put("department_short_name", subject.getDepartmentShortName() != null ? subject.getDepartmentShortName() : "");

        Cursor cursor = db.query(TABLE_SUBJECTS, new String[]{"id"}, "subject_code=?", new String[]{subject.getSubjectCode()}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(0);
            cursor.close();
            db.update(TABLE_SUBJECTS, cv, "id=?", new String[]{String.valueOf(id)});
        } else {
            if (cursor != null) cursor.close();
            db.insert(TABLE_SUBJECTS, null, cv);
        }
    }

    public boolean updateSubject(Subject subject) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("subject_code", subject.getSubjectCode());
        cv.put("subject_name", subject.getSubjectName());
        cv.put("credits", subject.getCredits());
        cv.put("semester", Subject.parseSemesterNumber(subject.getSemester()));
        cv.put("department", subject.getDepartment());
        cv.put("program_level", subject.getProgramLevel() != null ? subject.getProgramLevel() : "UG");
        cv.put("department_id", subject.getDepartmentId() != null ? subject.getDepartmentId() : "");
        cv.put("department_short_name", subject.getDepartmentShortName() != null ? subject.getDepartmentShortName() : "");
        int rows = db.update(TABLE_SUBJECTS, cv, "id=? OR subject_code=?", new String[]{String.valueOf(subject.getId()), subject.getSubjectCode()});
        if (rows > 0) {
            try { FirestoreHelper.getInstance().syncSubject(subject); } catch (Exception ignored) {}
        }
        return rows > 0;
    }

    public boolean deleteSubject(int subjectId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Also clean up marks/attendance for this subject
        db.delete(TABLE_MARKS, "subject_id=?", new String[]{String.valueOf(subjectId)});
        db.delete(TABLE_ATTENDANCE, "subject_id=?", new String[]{String.valueOf(subjectId)});
        int rows = db.delete(TABLE_SUBJECTS, "id=?", new String[]{String.valueOf(subjectId)});
        if (rows > 0) {
            try { FirestoreHelper.getInstance().deleteSubject(subjectId); } catch (Exception ignored) {}
        }
        return rows > 0;
    }

    public boolean deleteSubjectByCode(String subjectCode) {
        if (subjectCode == null || subjectCode.isEmpty()) return false;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_SUBJECTS, new String[]{"id"}, "subject_code=?", new String[]{subjectCode}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(0);
            cursor.close();
            return deleteSubject(id);
        }
        if (cursor != null) cursor.close();
        return false;
    }

    public List<Subject> getSubjectsBySemester(int semester) {
        List<Subject> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SUBJECTS, null, "semester=?", new String[]{String.valueOf(semester)}, null, null, "subject_code ASC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String code = cursor.getString(cursor.getColumnIndexOrThrow("subject_code"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("subject_name"));
                int credits = cursor.getInt(cursor.getColumnIndexOrThrow("credits"));
                int sem = cursor.getInt(cursor.getColumnIndexOrThrow("semester"));
                String dept = cursor.getString(cursor.getColumnIndexOrThrow("department"));
                list.add(new Subject(id, code, name, credits, sem, dept));
            } while (cursor.moveToNext());
            cursor.close();
        }
        if (list.isEmpty()) {
            return list;
        }
        return list;
    }

    // ==========================================
    // ATTENDANCE MODULE DATABASE METHODS
    // ==========================================

    public boolean saveOrUpdateAttendanceRecord(int studentId, int subjectId, String date, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM " + TABLE_ATTENDANCE + " WHERE student_id=? AND subject_id=? AND date=?",
                new String[]{String.valueOf(studentId), String.valueOf(subjectId), date});

        ContentValues cv = new ContentValues();
        cv.put("student_id", studentId);
        cv.put("subject_id", subjectId);
        cv.put("date", date);
        cv.put("status", status.toUpperCase());

        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(0);
            cursor.close();
            int rows = db.update(TABLE_ATTENDANCE, cv, "id=?", new String[]{String.valueOf(id)});
            if (rows > 0) {
                try { FirestoreHelper.getInstance().syncAttendance(studentId, subjectId, date, status); } catch (Exception ignored) {}
            }
            return rows > 0;
        } else {
            if (cursor != null) cursor.close();
            long newId = db.insert(TABLE_ATTENDANCE, null, cv);
            if (newId != -1) {
                try { FirestoreHelper.getInstance().syncAttendance(studentId, subjectId, date, status); } catch (Exception ignored) {}
            }
            return newId != -1;
        }
    }

    public List<AttendanceRecord> getAttendanceForSubjectAndDate(int subjectId, String date) {
        List<AttendanceRecord> list = new ArrayList<>();
        List<Student> students = getAllStudents();

        SQLiteDatabase db = this.getReadableDatabase();

        for (Student st : students) {
            String status = "PRESENT"; // Default initial toggle
            int recordId = 0;

            Cursor cursor = db.rawQuery("SELECT id, status FROM " + TABLE_ATTENDANCE + " WHERE student_id=? AND subject_id=? AND date=?",
                    new String[]{String.valueOf(st.getId()), String.valueOf(subjectId), date});

            if (cursor != null && cursor.moveToFirst()) {
                recordId = cursor.getInt(0);
                status = cursor.getString(1);
                cursor.close();
            } else if (cursor != null) {
                cursor.close();
            }

            list.add(new AttendanceRecord(recordId, st.getId(), st.getName(), st.getRegNo(), subjectId, date, status));
        }
        return list;
    }

    public List<SubjectAttendanceStats> getSubjectWiseAttendance(int studentId) {
        List<SubjectAttendanceStats> statsList = new ArrayList<>();
        List<Subject> subjects = getAllSubjects();
        SQLiteDatabase db = this.getReadableDatabase();

        for (Subject sub : subjects) {
            Cursor cursorTotal = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_ATTENDANCE + " WHERE student_id=? AND subject_id=?",
                    new String[]{String.valueOf(studentId), String.valueOf(sub.getId())});
            Cursor cursorPresent = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_ATTENDANCE + " WHERE student_id=? AND subject_id=? AND status='PRESENT'",
                    new String[]{String.valueOf(studentId), String.valueOf(sub.getId())});

            int total = 0;
            int present = 0;

            if (cursorTotal != null && cursorTotal.moveToFirst()) {
                total = cursorTotal.getInt(0);
                cursorTotal.close();
            }
            if (cursorPresent != null && cursorPresent.moveToFirst()) {
                present = cursorPresent.getInt(0);
                cursorPresent.close();
            }

            statsList.add(new SubjectAttendanceStats(sub.getId(), sub.getSubjectCode(), sub.getSubjectName(), total, present));
        }

        return statsList;
    }

    public List<MonthlyAttendanceStats> getMonthlyAttendanceChart(int studentId) {
        List<MonthlyAttendanceStats> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String[] monthKeys = {"2026-03", "2026-04", "2026-05", "2026-06", "2026-07", "2026-08"};
        String[] monthLabels = {"Mar", "Apr", "May", "Jun", "Jul", "Aug"};

        for (int i = 0; i < monthKeys.length; i++) {
            String mKey = monthKeys[i];
            String mLabel = monthLabels[i];

            Cursor cursorTotal = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_ATTENDANCE + " WHERE student_id=? AND date LIKE ?",
                    new String[]{String.valueOf(studentId), mKey + "%"});
            Cursor cursorPresent = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_ATTENDANCE + " WHERE student_id=? AND date LIKE ? AND status='PRESENT'",
                    new String[]{String.valueOf(studentId), mKey + "%"});

            int total = 0;
            int present = 0;

            if (cursorTotal != null && cursorTotal.moveToFirst()) {
                total = cursorTotal.getInt(0);
                cursorTotal.close();
            }
            if (cursorPresent != null && cursorPresent.moveToFirst()) {
                present = cursorPresent.getInt(0);
                cursorPresent.close();
            }

            list.add(new MonthlyAttendanceStats(mKey, mLabel, total, present));
        }

        return list;
    }

    // ==========================================
    // ASSIGNMENT & NOTIFICATION MODULE METHODS
    // ==========================================

    public void ensureSubmissionsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SUBMISSIONS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "assignment_id INTEGER, "
                + "student_id INTEGER, "
                + "submission_date TEXT, "
                + "file_path TEXT, "
                + "status TEXT, "
                + "remarks TEXT)");
    }

    public boolean addAssignment(String title, int subjectId, String deadline, String description, String filePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("title", title);
        cv.put("subject_id", subjectId);
        cv.put("deadline", deadline);
        cv.put("description", description);
        cv.put("file_path", filePath);

        long id = db.insert(TABLE_ASSIGNMENTS, null, cv);
        if (id != -1) {
            // Auto broadcast notification
            addNotification("New Assignment: " + title, "Deadline: " + deadline + ". PDF attached.", "2026-08-06", "STUDENT");
            try {
                FirestoreHelper.getInstance().syncAssignment(new Assignment((int) id, title, subjectId, "", "", deadline, description, filePath));
            } catch (Exception ignored) {}
            return true;
        }
        return false;
    }

    public List<Assignment> getAllAssignments() {
        List<Assignment> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT a.id, a.title, a.subject_id, s.subject_code, s.subject_name, a.deadline, a.description, a.file_path "
                + "FROM " + TABLE_ASSIGNMENTS + " a "
                + "LEFT JOIN " + TABLE_SUBJECTS + " s ON a.subject_id = s.id "
                + "ORDER BY a.id DESC";

        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String title = cursor.getString(1);
                int subId = cursor.getInt(2);
                String subCode = cursor.getString(3) != null ? cursor.getString(3) : "CS50" + subId;
                String subName = cursor.getString(4) != null ? cursor.getString(4) : "Subject " + subId;
                String deadline = cursor.getString(5);
                String desc = cursor.getString(6);
                String filePath = cursor.getString(7);

                list.add(new Assignment(id, title, subId, subCode, subName, deadline, desc, filePath));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public List<Assignment> getAssignmentsForStudent(int studentId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ensureSubmissionsTable(db);

        List<Assignment> allAssignments = getAllAssignments();

        for (Assignment assign : allAssignments) {
            Cursor cursor = db.rawQuery("SELECT submission_date, file_path, status FROM " + TABLE_SUBMISSIONS + " WHERE assignment_id=? AND student_id=?",
                    new String[]{String.valueOf(assign.getId()), String.valueOf(studentId)});

            if (cursor != null && cursor.moveToFirst()) {
                assign.setSubmissionDate(cursor.getString(0));
                assign.setSubmittedFilePath(cursor.getString(1));
                assign.setStatus(cursor.getString(2) != null ? cursor.getString(2) : "SUBMITTED");
                cursor.close();
            } else {
                if (cursor != null) cursor.close();
                assign.setStatus("PENDING");
            }
        }

        return allAssignments;
    }

    public boolean submitAssignment(int assignmentId, int studentId, String submissionDate, String filePath, String remarks) {
        SQLiteDatabase db = this.getWritableDatabase();
        ensureSubmissionsTable(db);

        // Check deadline to mark as SUBMITTED or LATE
        String status = "SUBMITTED";

        Cursor cursorSub = db.rawQuery("SELECT id FROM " + TABLE_SUBMISSIONS + " WHERE assignment_id=? AND student_id=?",
                new String[]{String.valueOf(assignmentId), String.valueOf(studentId)});

        ContentValues cv = new ContentValues();
        cv.put("assignment_id", assignmentId);
        cv.put("student_id", studentId);
        cv.put("submission_date", submissionDate);
        cv.put("file_path", filePath);
        cv.put("status", status);
        cv.put("remarks", remarks);

        boolean success;
        if (cursorSub != null && cursorSub.moveToFirst()) {
            int subId = cursorSub.getInt(0);
            cursorSub.close();
            success = db.update(TABLE_SUBMISSIONS, cv, "id=?", new String[]{String.valueOf(subId)}) > 0;
        } else {
            if (cursorSub != null) cursorSub.close();
            success = db.insert(TABLE_SUBMISSIONS, null, cv) != -1;
        }

        if (success) {
            addNotification("Assignment Submission Confirmed", "Your PDF submission for assignment ID #" + assignmentId + " was recorded successfully.", submissionDate, "STUDENT");
            try {
                FirestoreHelper.getInstance().syncSubmission(assignmentId, studentId, submissionDate, filePath, status, remarks);
            } catch (Exception ignored) {}
        }

        return success;
    }

    public List<AssignmentSubmission> getSubmissionsForAssignment(int assignmentId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ensureSubmissionsTable(db);

        List<AssignmentSubmission> list = new ArrayList<>();
        String query = "SELECT sub.id, sub.assignment_id, sub.student_id, st.name, st.reg_no, sub.submission_date, sub.file_path, sub.status, sub.remarks "
                + "FROM " + TABLE_SUBMISSIONS + " sub "
                + "LEFT JOIN " + TABLE_STUDENTS + " st ON sub.student_id = st.id "
                + "WHERE sub.assignment_id=?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(assignmentId)});
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                int aId = cursor.getInt(1);
                int stId = cursor.getInt(2);
                String stName = cursor.getString(3) != null ? cursor.getString(3) : "Student #" + stId;
                String regNo = cursor.getString(4) != null ? cursor.getString(4) : "STD" + stId;
                String date = cursor.getString(5);
                String fPath = cursor.getString(6);
                String status = cursor.getString(7);
                String remarks = cursor.getString(8);

                list.add(new AssignmentSubmission(id, aId, stId, stName, regNo, date, fPath, status, remarks));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public boolean addNotification(String title, String message, String date, String targetRole) {
        return addNotification(title, message, date, targetRole, "GENERAL", "Faculty Admin");
    }

    public boolean addNotification(String title, String message, String date, String targetRole, String category, String sender) {
        SQLiteDatabase db = this.getWritableDatabase();
        ensureNotificationTableUpdated(db);
        ContentValues cv = new ContentValues();
        cv.put("title", title);
        cv.put("message", message);
        cv.put("date", date);
        cv.put("target_role", targetRole);
        cv.put("category", category != null ? category : "GENERAL");
        cv.put("sender", sender != null ? sender : "Faculty Admin");
        cv.put("is_read", 0);
        long id = db.insert(TABLE_NOTIFICATIONS, null, cv);
        if (id != -1) {
            try {
                FirestoreHelper.getInstance().syncNotification(new com.example.model.AppNotification((int) id, title, message, date, targetRole, category, sender, false));
            } catch (Exception ignored) {}
            return true;
        }
        return false;
    }


    private String deriveCategoryFromTitle(String title, String msg) {
        if (title == null) return "GENERAL";
        String lower = (title + " " + (msg != null ? msg : "")).toLowerCase();
        if (lower.contains("assignment")) return "ASSIGNMENT";
        if (lower.contains("exam") || lower.contains("mid-term") || lower.contains("test") || lower.contains("result")) return "EXAM";
        if (lower.contains("urgent") || lower.contains("alert") || lower.contains("emergency") || lower.contains("shortage")) return "URGENT";
        if (lower.contains("attendance")) return "ATTENDANCE";
        if (lower.contains("welcome") || lower.contains("portal") || lower.contains("circular")) return "CIRCULAR";
        return "GENERAL";
    }

    // ==========================================
    // REPORT MODULE HELPER METHODS
    // ==========================================

    public void ensureSampleDataForReports() {
        // Purely user driven - no sample data
    }

    public List<TopperItem> getToppersList() {
        List<TopperItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String sql = "SELECT s.id, s.name, s.reg_no, s.department, r.cgpa, r.sgpa "
                + "FROM " + TABLE_STUDENTS + " s "
                + "LEFT JOIN " + TABLE_RESULTS + " r ON s.id = r.student_id "
                + "ORDER BY r.cgpa DESC, s.name ASC";

        Cursor cursor = db.rawQuery(sql, null);
        int rank = 1;

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String name = cursor.getString(1);
                String regNo = cursor.getString(2);
                String dept = cursor.getString(3);
                double cgpa = cursor.isNull(4) ? 0.0 : cursor.getDouble(4);
                double sgpa = cursor.isNull(5) ? 0.0 : cursor.getDouble(5);

                String badge;
                if (rank == 1) {
                    badge = "🥇 Gold Medalist (Rank 1)";
                } else if (rank == 2) {
                    badge = "🥈 Silver Medalist (Rank 2)";
                } else if (rank == 3) {
                    badge = "🥉 Bronze Medalist (Rank 3)";
                } else if (cgpa >= 9.0) {
                    badge = "⭐ Dean's Honor Roll";
                } else if (cgpa >= 8.0) {
                    badge = "🎖️ First Class with Distinction";
                } else {
                    badge = "📜 First Class";
                }

                list.add(new TopperItem(rank, name, regNo, dept, cgpa, sgpa, badge));
                rank++;
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public double getOverallAttendancePercentage() {
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery("SELECT COUNT(*), SUM(CASE WHEN UPPER(status)='PRESENT' THEN 1 ELSE 0 END) FROM " + TABLE_ATTENDANCE, null);
            if (cursor != null && cursor.moveToFirst()) {
                int total = cursor.getInt(0);
                int present = cursor.getInt(1);
                cursor.close();
                if (total > 0) {
                    return Math.round(((double) present / total) * 100.0 * 10.0) / 10.0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public List<Student> getStudentsWithAttendanceShortage() {
        List<Student> allStudents = getAllStudents();
        List<Student> shortageList = new ArrayList<>();

        for (Student s : allStudents) {
            double attPct = getStudentAttendancePercentage(s.getId());
            if (attPct < 75.0) {
                shortageList.add(s);
            }
        }
        return shortageList;
    }

    public double getStudentCgpa(int studentId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT cgpa FROM " + TABLE_RESULTS + " WHERE student_id=? ORDER BY semester DESC LIMIT 1", new String[]{String.valueOf(studentId)});
        double cgpa = 0.0;
        if (cursor != null && cursor.moveToFirst()) {
            cgpa = cursor.getDouble(0);
            cursor.close();
        }
        return cgpa;
    }

    public boolean saveStudentResultsAndMarks(int studentId, int semester, double sgpa, double cgpa, int totalCredits, List<SubjectGradeItem> gradeItems) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_RESULTS, "student_id=? AND semester=?", new String[]{String.valueOf(studentId), String.valueOf(semester)});

            ContentValues res = new ContentValues();
            res.put("student_id", studentId);
            res.put("semester", semester);
            res.put("sgpa", sgpa);
            res.put("cgpa", cgpa);
            res.put("total_credits", totalCredits);
            res.put("status", "PUBLISHED");
            res.put("approval_status", "APPROVED");
            db.insert(TABLE_RESULTS, null, res);

            for (int i = 0; i < gradeItems.size(); i++) {
                SubjectGradeItem item = gradeItems.get(i);
                int subId = i + 1;
                Cursor curSub = null;
                try {
                    String searchCode = item.getSubjectCode() != null ? item.getSubjectCode() : "";
                    String searchName = item.getSubjectName() != null ? item.getSubjectName() : "";
                    curSub = db.rawQuery("SELECT id FROM " + TABLE_SUBJECTS + " WHERE subject_code = ? OR subject_name = ?", new String[]{searchCode, searchName});
                    if (curSub != null && curSub.moveToFirst()) {
                        subId = curSub.getInt(0);
                    } else {
                        ContentValues scv = new ContentValues();
                        scv.put("subject_code", !searchCode.isEmpty() ? searchCode : "SUB" + (i + 1));
                        scv.put("subject_name", !searchName.isEmpty() ? searchName : "Subject " + (i + 1));
                        scv.put("credits", item.getCredits() > 0 ? item.getCredits() : 4);
                        scv.put("semester", semester);
                        scv.put("department", "General");
                        long newSubId = db.insert(TABLE_SUBJECTS, null, scv);
                        if (newSubId > 0) subId = (int) newSubId;
                    }
                } catch (Exception ignored) {
                } finally {
                    if (curSub != null) curSub.close();
                }

                ContentValues m = new ContentValues();
                m.put("student_id", studentId);
                m.put("subject_id", subId);
                m.put("internal1", item.getInternalMarks());
                m.put("university_exam", item.getExternalMarks());
                m.put("total_marks", item.getTotalMarks());
                m.put("percentage", item.getPercentage());
                m.put("grade", item.getGrade());
                m.put("grade_point", (double) item.getGradePoint());

                db.delete(TABLE_MARKS, "student_id=? AND subject_id=?", new String[]{String.valueOf(studentId), String.valueOf(subId)});
                db.insert(TABLE_MARKS, null, m);
            }

            // Create notification for student regarding published result
            ContentValues notif = new ContentValues();
            notif.put("title", "Semester Results Published");
            notif.put("message", String.format(Locale.US, "Internal & External marks published! SGPA: %.2f | CGPA: %.2f", sgpa, cgpa));
            notif.put("date", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new java.util.Date()));
            notif.put("target_role", "STUDENT");
            db.insert(TABLE_NOTIFICATIONS, null, notif);

            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            db.endTransaction();
        }
    }

    public boolean saveOrUpdateMark(int studentId, int subjectId, double internal1, double assignment, double modelExam, double lab, double universityExam, double totalMarks, double percentage, String grade, double gradePoint) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("student_id", studentId);
        cv.put("subject_id", subjectId);
        cv.put("internal1", internal1);
        cv.put("assignment", assignment);
        cv.put("model_exam", modelExam);
        cv.put("lab", lab);
        cv.put("university_exam", universityExam);
        cv.put("total_marks", totalMarks);
        cv.put("percentage", percentage);
        cv.put("grade", grade);
        cv.put("grade_point", gradePoint);

        Cursor cursor = db.query(TABLE_MARKS, new String[]{"id"}, "student_id=? AND subject_id=?", new String[]{String.valueOf(studentId), String.valueOf(subjectId)}, null, null, null);
        boolean exists = (cursor != null && cursor.moveToFirst());
        if (cursor != null) cursor.close();

        long result;
        if (exists) {
            result = db.update(TABLE_MARKS, cv, "student_id=? AND subject_id=?", new String[]{String.valueOf(studentId), String.valueOf(subjectId)});
        } else {
            result = db.insert(TABLE_MARKS, null, cv);
        }
        if (result > 0) {
            try {
                FirestoreHelper.getInstance().syncMark(studentId, subjectId, internal1, assignment, modelExam, lab, universityExam, totalMarks, percentage, grade, gradePoint);
            } catch (Exception ignored) {}
        }
        return result > 0;
    }

    public Cursor getMarkByStudentAndSubject(int studentId, int subjectId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_MARKS, null, "student_id=? AND subject_id=?", new String[]{String.valueOf(studentId), String.valueOf(subjectId)}, null, null, null);
    }

    public boolean deleteMark(int studentId, int subjectId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_MARKS, "student_id=? AND subject_id=?", new String[]{String.valueOf(studentId), String.valueOf(subjectId)});
        if (rows > 0) {
            try { FirestoreHelper.getInstance().deleteMark(studentId, subjectId); } catch (Exception ignored) {}
        }
        return rows > 0;
    }

    public boolean publishOrUpdateResult(int studentId, int semester, double totalMarks, double percentage, double sgpa, double cgpa, String status, String publishedDate) {
        return publishOrUpdateResult(studentId, semester, totalMarks, percentage, sgpa, cgpa, status, publishedDate, "SYNCED", null);
    }

    public boolean publishOrUpdateResult(int studentId, int semester, double totalMarks, double percentage, double sgpa, double cgpa, String status, String publishedDate, String syncStatus, String studentUid) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("student_id", studentId);
        cv.put("semester", semester);
        cv.put("total_marks", totalMarks);
        cv.put("percentage", percentage);
        cv.put("sgpa", sgpa);
        cv.put("cgpa", cgpa);
        cv.put("status", status != null ? status : "DRAFT");
        cv.put("approval_status", status != null ? status : "DRAFT");
        cv.put("published_date", publishedDate);
        if (studentUid != null && !studentUid.isEmpty()) {
            cv.put("student_uid", studentUid);
        }
        if (syncStatus != null) {
            cv.put("sync_status", syncStatus);
        }

        Cursor cursor = db.query(TABLE_RESULTS, new String[]{"id", "version", "student_uid"}, "student_id=? AND semester=?", new String[]{String.valueOf(studentId), String.valueOf(semester)}, null, null, null);
        boolean exists = (cursor != null && cursor.moveToFirst());
        int currentVer = 1;
        String existingUid = null;
        if (exists) {
            try {
                int verCol = cursor.getColumnIndex("version");
                if (verCol != -1) currentVer = cursor.getInt(verCol);
                int uidCol = cursor.getColumnIndex("student_uid");
                if (uidCol != -1 && !cursor.isNull(uidCol)) existingUid = cursor.getString(uidCol);
            } catch (Exception ignored) {}
        }
        if (cursor != null) cursor.close();

        if ((studentUid == null || studentUid.isEmpty()) && existingUid != null) {
            cv.put("student_uid", existingUid);
        }

        cv.put("version", currentVer > 0 ? currentVer : 1);

        long result;
        if (exists) {
            result = db.update(TABLE_RESULTS, cv, "student_id=? AND semester=?", new String[]{String.valueOf(studentId), String.valueOf(semester)});
        } else {
            result = db.insert(TABLE_RESULTS, null, cv);
        }

        if (result > 0 && !"LOCAL_PENDING_SYNC".equalsIgnoreCase(syncStatus)) {
            try {
                String effectiveUid = (studentUid != null && !studentUid.isEmpty()) ? studentUid : (existingUid != null ? existingUid : String.valueOf(studentId));
                com.example.model.Result resObj = new com.example.model.Result(0, effectiveUid, semester, totalMarks, percentage, sgpa, cgpa, status, publishedDate);
                resObj.setStudentNumericId(studentId);
                resObj.setVersion(currentVer > 0 ? currentVer : 1);
                FirestoreHelper.getInstance().syncResult(resObj);
            } catch (Exception ignored) {}
        }

        // Only notify student if result is officially APPROVED / PUBLISHED
        if (result > 0 && ("APPROVED".equalsIgnoreCase(status) || "PUBLISHED".equalsIgnoreCase(status))) {
            ContentValues notif = new ContentValues();
            notif.put("title", "Semester " + semester + " Results Published");
            notif.put("message", String.format(Locale.US, "Your Semester %d results are published! SGPA: %.2f | CGPA: %.2f", semester, sgpa, cgpa));
            notif.put("date", publishedDate);
            notif.put("target_role", "STUDENT");
            db.insert(TABLE_NOTIFICATIONS, null, notif);
        }

        return result > 0;
    }

    public boolean unpublishResult(int studentId, int semester) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("status", "DRAFT");
        cv.put("approval_status", "DRAFT");
        int rows = db.update(TABLE_RESULTS, cv, "student_id=? AND semester=?", new String[]{String.valueOf(studentId), String.valueOf(semester)});
        return rows > 0;
    }

    public com.example.model.Result getResultForStudentAndSemester(int studentId, int semester) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_RESULTS, null, "student_id=? AND semester=?", new String[]{String.valueOf(studentId), String.valueOf(semester)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            double total = cursor.getDouble(cursor.getColumnIndexOrThrow("total_marks"));
            double pct = cursor.getDouble(cursor.getColumnIndexOrThrow("percentage"));
            double sgpa = cursor.getDouble(cursor.getColumnIndexOrThrow("sgpa"));
            double cgpa = cursor.getDouble(cursor.getColumnIndexOrThrow("cgpa"));
            String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
            String date = cursor.getString(cursor.getColumnIndexOrThrow("published_date"));

            int version = 1;
            int verCol = cursor.getColumnIndex("version");
            if (verCol != -1 && !cursor.isNull(verCol)) {
                version = cursor.getInt(verCol);
            }

            String rejReason = null;
            int rejCol = cursor.getColumnIndex("rejection_reason");
            if (rejCol != -1 && !cursor.isNull(rejCol)) {
                rejReason = cursor.getString(rejCol);
            }

            String approvedBy = null;
            int appByCol = cursor.getColumnIndex("approved_by");
            if (appByCol != -1 && !cursor.isNull(appByCol)) {
                approvedBy = cursor.getString(appByCol);
            }

            String studentUid = null;
            int uidCol = cursor.getColumnIndex("student_uid");
            if (uidCol != -1 && !cursor.isNull(uidCol)) {
                studentUid = cursor.getString(uidCol);
            }

            cursor.close();
            com.example.model.Result res = new com.example.model.Result(id, (studentUid != null && !studentUid.isEmpty()) ? studentUid : String.valueOf(studentId), semester, total, pct, sgpa, cgpa, status, date);
            res.setStudentNumericId(studentId);
            if (studentUid != null && !studentUid.isEmpty()) {
                res.setStudentUid(studentUid);
            }
            res.setVersion(version > 0 ? version : 1);
            res.setRejectionReason(rejReason);
            res.setApprovedBy(approvedBy);
            return res;
        }
        if (cursor != null) cursor.close();
        return null;
    }

    public List<com.example.model.Result> getPublishedResultsForStudent(int studentId) {
        List<com.example.model.Result> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_RESULTS, null, "student_id=? AND (status='APPROVED' OR status='PUBLISHED')", new String[]{String.valueOf(studentId)}, null, null, "semester ASC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                int sem = cursor.getInt(cursor.getColumnIndexOrThrow("semester"));
                double total = cursor.getDouble(cursor.getColumnIndexOrThrow("total_marks"));
                double pct = cursor.getDouble(cursor.getColumnIndexOrThrow("percentage"));
                double sgpa = cursor.getDouble(cursor.getColumnIndexOrThrow("sgpa"));
                double cgpa = cursor.getDouble(cursor.getColumnIndexOrThrow("cgpa"));
                String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("published_date"));

                int version = 1;
                int verCol = cursor.getColumnIndex("version");
                if (verCol != -1 && !cursor.isNull(verCol)) {
                    version = cursor.getInt(verCol);
                }

                String studentUid = null;
                int uidCol = cursor.getColumnIndex("student_uid");
                if (uidCol != -1 && !cursor.isNull(uidCol)) {
                    studentUid = cursor.getString(uidCol);
                }

                com.example.model.Result r = new com.example.model.Result(id, (studentUid != null && !studentUid.isEmpty()) ? studentUid : String.valueOf(studentId), sem, total, pct, sgpa, cgpa, status, date);
                r.setStudentNumericId(studentId);
                if (studentUid != null && !studentUid.isEmpty()) {
                    r.setStudentUid(studentUid);
                }
                r.setVersion(version > 0 ? version : 1);
                list.add(r);
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();
        return list;
    }

    public List<com.example.model.Result> getPendingSyncResults() {
        List<com.example.model.Result> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_RESULTS, null, "sync_status='LOCAL_PENDING_SYNC'", null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    int stId = cursor.getInt(cursor.getColumnIndexOrThrow("student_id"));
                    int sem = cursor.getInt(cursor.getColumnIndexOrThrow("semester"));
                    double total = cursor.getDouble(cursor.getColumnIndexOrThrow("total_marks"));
                    double pct = cursor.getDouble(cursor.getColumnIndexOrThrow("percentage"));
                    double sgpa = cursor.getDouble(cursor.getColumnIndexOrThrow("sgpa"));
                    double cgpa = cursor.getDouble(cursor.getColumnIndexOrThrow("cgpa"));
                    String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                    String date = cursor.getString(cursor.getColumnIndexOrThrow("published_date"));
                    String uid = null;
                    int uidCol = cursor.getColumnIndex("student_uid");
                    if (uidCol != -1 && !cursor.isNull(uidCol)) {
                        uid = cursor.getString(uidCol);
                    }
                    com.example.model.Result r = new com.example.model.Result(id, (uid != null && !uid.isEmpty()) ? uid : String.valueOf(stId), sem, total, pct, sgpa, cgpa, status, date);
                    r.setStudentNumericId(stId);
                    if (uid != null && !uid.isEmpty()) {
                        r.setStudentUid(uid);
                    }
                    list.add(r);
                } while (cursor.moveToNext());
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }

    public boolean markResultSynced(int studentId, int semester) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("sync_status", "SYNCED");
        return db.update(TABLE_RESULTS, cv, "student_id=? AND semester=?", new String[]{String.valueOf(studentId), String.valueOf(semester)}) > 0;
    }

    public boolean addAssessment(com.example.model.Assessment a) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("title", a.getTitle());
        cv.put("type", a.getType());
        cv.put("subject_id", a.getSubjectId());
        cv.put("semester", a.getSemester());
        cv.put("date", a.getDate());
        cv.put("max_marks", a.getMaxMarks());
        cv.put("status", a.getStatus() != null ? a.getStatus() : "PENDING");
        long id = db.insert(TABLE_ASSESSMENTS, null, cv);
        if (id != -1) {
            a.setId((int) id);
            try { FirestoreHelper.getInstance().syncAssessment(a); } catch (Exception ignored) {}
        }
        return id != -1;
    }

    public boolean updateAssessment(com.example.model.Assessment a) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("title", a.getTitle());
        cv.put("type", a.getType());
        cv.put("subject_id", a.getSubjectId());
        cv.put("semester", a.getSemester());
        cv.put("date", a.getDate());
        cv.put("max_marks", a.getMaxMarks());
        cv.put("status", a.getStatus());
        int rows = db.update(TABLE_ASSESSMENTS, cv, "id=?", new String[]{String.valueOf(a.getId())});
        if (rows > 0) {
            try { FirestoreHelper.getInstance().syncAssessment(a); } catch (Exception ignored) {}
        }
        return rows > 0;
    }

    public boolean deleteAssessment(int assessmentId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_ASSESSMENTS, "id=?", new String[]{String.valueOf(assessmentId)});
        if (rows > 0) {
            try { FirestoreHelper.getInstance().deleteAssessment(assessmentId); } catch (Exception ignored) {}
        }
        return rows > 0;
    }

    public List<com.example.model.Assessment> getAllAssessments() {
        List<com.example.model.Assessment> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ASSESSMENTS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, type TEXT, subject_id INTEGER, semester INTEGER, date TEXT, max_marks REAL, status TEXT DEFAULT 'PENDING')");
            Cursor cursor = db.rawQuery("SELECT a.*, s.subject_name, s.subject_code FROM " + TABLE_ASSESSMENTS + " a LEFT JOIN " + TABLE_SUBJECTS + " s ON a.subject_id = s.id ORDER BY a.id DESC", null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                    String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                    int subId = cursor.getInt(cursor.getColumnIndexOrThrow("subject_id"));
                    int sem = cursor.getInt(cursor.getColumnIndexOrThrow("semester"));
                    String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                    double maxMarks = cursor.getDouble(cursor.getColumnIndexOrThrow("max_marks"));
                    String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));

                    com.example.model.Assessment a = new com.example.model.Assessment(id, title, type, subId, sem, date, maxMarks, status);
                    a.setSubjectName(cursor.getString(cursor.getColumnIndexOrThrow("subject_name")));
                    a.setSubjectCode(cursor.getString(cursor.getColumnIndexOrThrow("subject_code")));
                    list.add(a);
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<SubjectGradeItem> getStudentSubjectMarks(int studentId) {
        List<SubjectGradeItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery("SELECT m.*, s.subject_code, s.subject_name, s.credits FROM " + TABLE_MARKS + " m LEFT JOIN " + TABLE_SUBJECTS + " s ON m.subject_id = s.id WHERE m.student_id = ?", new String[]{String.valueOf(studentId)});
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String code = cursor.getColumnIndex("subject_code") != -1 ? cursor.getString(cursor.getColumnIndexOrThrow("subject_code")) : "SUB";
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("subject_name"));
                    int credits = cursor.getInt(cursor.getColumnIndexOrThrow("credits"));
                    if (name == null || name.isEmpty()) {
                        int subId = cursor.getInt(cursor.getColumnIndexOrThrow("subject_id"));
                        name = "Course " + subId;
                    }
                    if (credits <= 0) credits = 4;

                    double internal = cursor.getDouble(cursor.getColumnIndexOrThrow("internal1"));
                    double assignment = cursor.getColumnIndex("assignment") != -1 ? cursor.getDouble(cursor.getColumnIndexOrThrow("assignment")) : 10;
                    double modelExam = cursor.getColumnIndex("model_exam") != -1 ? cursor.getDouble(cursor.getColumnIndexOrThrow("model_exam")) : 18;
                    double external = cursor.getDouble(cursor.getColumnIndexOrThrow("university_exam"));
                    double total = cursor.getDouble(cursor.getColumnIndexOrThrow("total_marks"));

                    if (internal == 0 && external == 0 && total > 0) {
                        internal = Math.round(total * 0.25 * 10.0) / 10.0;
                        external = Math.round(total * 0.75 * 10.0) / 10.0;
                    }

                    SubjectGradeItem item = new SubjectGradeItem(code, name, credits, internal, external);
                    item.setAssignment(assignment);
                    item.setModelExam(modelExam);
                    if (total > 0) item.setTotalMarks(total);
                    item.recalculate();
                    list.add(item);
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<SubjectGradeItem> getStudentSubjectMarksBySemester(int studentId, int semester) {
        List<SubjectGradeItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            String query = "SELECT m.*, s.subject_code, s.subject_name, s.credits, s.semester FROM " + TABLE_MARKS + " m "
                    + "INNER JOIN " + TABLE_SUBJECTS + " s ON m.subject_id = s.id "
                    + "WHERE m.student_id = ? AND s.semester = ? "
                    + "ORDER BY s.subject_code ASC";
            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(studentId), String.valueOf(semester)});
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String code = cursor.getString(cursor.getColumnIndexOrThrow("subject_code"));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("subject_name"));
                    int credits = cursor.getInt(cursor.getColumnIndexOrThrow("credits"));
                    if (credits <= 0) credits = 4;

                    double internal1 = cursor.getDouble(cursor.getColumnIndexOrThrow("internal1"));
                    double assignment = cursor.getDouble(cursor.getColumnIndexOrThrow("assignment"));
                    double modelExam = cursor.getDouble(cursor.getColumnIndexOrThrow("model_exam"));
                    double universityExam = cursor.getDouble(cursor.getColumnIndexOrThrow("university_exam"));
                    double total = cursor.getDouble(cursor.getColumnIndexOrThrow("total_marks"));

                    if (total == 0 && (internal1 > 0 || assignment > 0 || modelExam > 0 || universityExam > 0)) {
                        total = internal1 + assignment + modelExam + universityExam;
                    }

                    SubjectGradeItem item = new SubjectGradeItem(code, name, credits, internal1, universityExam);
                    item.setAssignment(assignment);
                    item.setModelExam(modelExam);
                    if (total > 0) item.setTotalMarks(total);
                    item.recalculate();
                    list.add(item);
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public double calculateStudentCgpaFromPublishedSemesters(int studentId) {
        SQLiteDatabase db = this.getReadableDatabase();
        double totalWeightedSgpa = 0.0;
        int totalCredits = 0;
        try {
            Cursor cursor = db.rawQuery("SELECT semester, sgpa FROM " + TABLE_RESULTS + " WHERE student_id=? AND status='PUBLISHED' ORDER BY semester ASC", new String[]{String.valueOf(studentId)});
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int sem = cursor.getInt(0);
                    double sgpa = cursor.getDouble(1);
                    int semCredits = getSemesterTotalCredits(sem);
                    if (semCredits <= 0) semCredits = 20;

                    totalWeightedSgpa += (sgpa * semCredits);
                    totalCredits += semCredits;
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (totalCredits > 0) {
            return Math.round((totalWeightedSgpa / (double) totalCredits) * 100.0) / 100.0;
        }
        return 0.0;
    }

    public int getSemesterTotalCredits(int semester) {
        SQLiteDatabase db = this.getReadableDatabase();
        int total = 0;
        try {
            Cursor cursor = db.rawQuery("SELECT SUM(credits) FROM " + TABLE_SUBJECTS + " WHERE semester=?", new String[]{String.valueOf(semester)});
            if (cursor != null && cursor.moveToFirst()) {
                total = cursor.getInt(0);
                cursor.close();
            }
        } catch (Exception ignored) {}
        return total > 0 ? total : 20;
    }

    // Admin Authentication & Credential Management
    public User authenticateAdmin(String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ADMINS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, email TEXT UNIQUE, password TEXT)");
            Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_ADMINS + " WHERE email=?", new String[]{email});
            if (cursor != null && cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String adminEmail = cursor.getString(cursor.getColumnIndexOrThrow("email"));
                String storedPassword = cursor.getString(cursor.getColumnIndexOrThrow("password"));
                cursor.close();

                if (PasswordUtils.verifyPassword(password, storedPassword)) {
                    if (PasswordUtils.isLegacyPlainText(storedPassword)) {
                        ContentValues cv = new ContentValues();
                        cv.put("password", PasswordUtils.hashPassword(password));
                        db.update(TABLE_ADMINS, cv, "id=?", new String[]{String.valueOf(id)});
                    }
                    return new User(id, "System Admin", adminEmail, password, "ADMIN", "ADMIN001");
                }
            }
            if (cursor != null) cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Fallback check for default admin credentials
        if ("admin@gradexpert.com".equalsIgnoreCase(email) && "123456".equals(password)) {
            return new User(1, "System Admin", "admin@gradexpert.com", "123456", "ADMIN", "ADMIN001");
        }
        return null;
    }

    public boolean updateAdminCredentials(String oldEmail, String newEmail, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ADMINS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, email TEXT UNIQUE, password TEXT)");
            ContentValues cv = new ContentValues();
            cv.put("email", newEmail);
            cv.put("password", PasswordUtils.hashPassword(newPassword));

            int rows = db.update(TABLE_ADMINS, cv, "email=?", new String[]{oldEmail});
            if (rows == 0) {
                cv.put("email", newEmail);
                cv.put("password", PasswordUtils.hashPassword(newPassword));
                db.insert(TABLE_ADMINS, null, cv);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Teacher Management Methods
    public boolean addTeacher(com.example.model.Teacher teacher) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", teacher.getName());
        cv.put("email", teacher.getEmail());
        String rawPwd = teacher.getPassword() != null && !teacher.getPassword().isEmpty() ? teacher.getPassword() : "teacher123";
        cv.put("password", PasswordUtils.hashPassword(rawPwd));
        cv.put("department", teacher.getDepartment());
        cv.put("phone", teacher.getPhone());
        if (teacher.getEmployeeId() != null && !teacher.getEmployeeId().isEmpty()) {
            cv.put("employee_id", teacher.getEmployeeId());
        }
        if (teacher.getLoginId() != null && !teacher.getLoginId().isEmpty()) {
            cv.put("login_id", teacher.getLoginId());
        }
        if (teacher.getFirebaseUid() != null && !teacher.getFirebaseUid().isEmpty()) {
            cv.put("firebase_uid", teacher.getFirebaseUid());
        }
        long result = db.insert(TABLE_TEACHERS, null, cv);
        if (result != -1) {
            try {
                com.example.model.Teacher tSync = new com.example.model.Teacher((int) result, teacher.getName(), teacher.getEmail(), teacher.getPassword(), teacher.getDepartment(), teacher.getPhone());
                tSync.setLoginId(teacher.getLoginId());
                tSync.setFirebaseUid(teacher.getFirebaseUid());
                tSync.setEmployeeId(teacher.getEmployeeId());
                FirestoreHelper.getInstance().syncTeacher(tSync);
            } catch (Exception ignored) {}
        }
        return result != -1;
    }

    public boolean updateTeacher(com.example.model.Teacher teacher) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", teacher.getName());
        cv.put("email", teacher.getEmail());
        if (teacher.getPassword() != null && !teacher.getPassword().isEmpty()) {
            cv.put("password", PasswordUtils.hashPassword(teacher.getPassword()));
        }
        cv.put("department", teacher.getDepartment());
        cv.put("phone", teacher.getPhone());
        if (teacher.getEmployeeId() != null && !teacher.getEmployeeId().isEmpty()) {
            cv.put("employee_id", teacher.getEmployeeId());
        }
        if (teacher.getLoginId() != null && !teacher.getLoginId().isEmpty()) {
            cv.put("login_id", teacher.getLoginId());
        }
        if (teacher.getFirebaseUid() != null && !teacher.getFirebaseUid().isEmpty()) {
            cv.put("firebase_uid", teacher.getFirebaseUid());
        }
        int rows = db.update(TABLE_TEACHERS, cv, "id=?", new String[]{String.valueOf(teacher.getId())});
        if (rows > 0) {
            try { FirestoreHelper.getInstance().syncTeacher(teacher); } catch (Exception ignored) {}
        }
        return rows > 0;
    }

    public boolean deleteTeacher(int teacherId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_TEACHERS, "id=?", new String[]{String.valueOf(teacherId)});
        if (rows > 0) {
            try { FirestoreHelper.getInstance().deleteTeacher(teacherId); } catch (Exception ignored) {}
        }
        return rows > 0;
    }

    public List<com.example.model.Teacher> getAllTeachers() {
        List<com.example.model.Teacher> teachers = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_TEACHERS + " ORDER BY id DESC", null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    String email = cursor.getString(cursor.getColumnIndexOrThrow("email"));
                    String password = cursor.getString(cursor.getColumnIndexOrThrow("password"));
                    String dept = cursor.getString(cursor.getColumnIndexOrThrow("department"));
                    String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
                    com.example.model.Teacher t = new com.example.model.Teacher(id, name, email, password, dept, phone);
                    int empCol = cursor.getColumnIndex("employee_id");
                    if (empCol != -1 && !cursor.isNull(empCol)) {
                        t.setEmployeeId(cursor.getString(empCol));
                    }
                    int loginIdCol = cursor.getColumnIndex("login_id");
                    if (loginIdCol != -1 && !cursor.isNull(loginIdCol)) {
                        t.setLoginId(cursor.getString(loginIdCol));
                    }
                    int fbUidCol = cursor.getColumnIndex("firebase_uid");
                    if (fbUidCol != -1 && !cursor.isNull(fbUidCol)) {
                        t.setFirebaseUid(cursor.getString(fbUidCol));
                    }
                    teachers.add(t);
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return teachers;
    }

    public int getTeacherCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;
        try {
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_TEACHERS, null);
            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0);
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    public boolean updateTeacherPhotoUri(String email, String photoUri) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("photo_uri", photoUri != null ? photoUri : "");
        int rows = db.update(TABLE_TEACHERS, cv, "email=?", new String[]{email});
        return rows > 0;
    }

    public String getTeacherPhotoUri(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_TEACHERS, new String[]{"photo_uri"}, "email=?", new String[]{email}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int col = cursor.getColumnIndex("photo_uri");
                if (col != -1 && !cursor.isNull(col)) {
                    return cursor.getString(col);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    // Teacher Activity Logging
    public void logTeacherActivity(String teacherName, String actionTitle, String description, String category) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ACTIVITY_LOGS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, teacher_name TEXT, action_title TEXT, description TEXT, timestamp TEXT, category TEXT)");
            ContentValues cv = new ContentValues();
            cv.put("teacher_name", teacherName);
            cv.put("action_title", actionTitle);
            cv.put("description", description);
            cv.put("timestamp", "Just now");
            cv.put("category", category);
            db.insert(TABLE_ACTIVITY_LOGS, null, cv);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<com.example.model.ActivityItem> getAllTeacherActivities() {
        List<com.example.model.ActivityItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = null;
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ACTIVITY_LOGS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, teacher_name TEXT, user_name TEXT, action_title TEXT, description TEXT, timestamp TEXT, category TEXT)");
            cursor = db.rawQuery("SELECT * FROM " + TABLE_ACTIVITY_LOGS + " ORDER BY id DESC", null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String teacher = "Admin";
                    if (cursor.getColumnIndex("teacher_name") != -1 && !cursor.isNull(cursor.getColumnIndex("teacher_name"))) {
                        teacher = cursor.getString(cursor.getColumnIndex("teacher_name"));
                    } else if (cursor.getColumnIndex("user_name") != -1 && !cursor.isNull(cursor.getColumnIndex("user_name"))) {
                        teacher = cursor.getString(cursor.getColumnIndex("user_name"));
                    }
                    String title = cursor.getColumnIndex("action_title") != -1 && !cursor.isNull(cursor.getColumnIndex("action_title")) ? cursor.getString(cursor.getColumnIndex("action_title")) : "Action";
                    String timestamp = cursor.getColumnIndex("timestamp") != -1 && !cursor.isNull(cursor.getColumnIndex("timestamp")) ? cursor.getString(cursor.getColumnIndex("timestamp")) : "Recent";
                    String category = cursor.getColumnIndex("category") != -1 && !cursor.isNull(cursor.getColumnIndex("category")) ? cursor.getString(cursor.getColumnIndex("category")) : "General";
                    String fullTitle = (teacher != null ? teacher : "System") + " • " + (title != null ? title : "Action");
                    list.add(new com.example.model.ActivityItem(fullTitle, timestamp != null ? timestamp : "Recent", category != null ? category : "General", com.example.R.drawable.ic_profile));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }

    // ==========================================
    // NOTIFICATIONS MODULE DATABASE METHODS
    // ==========================================

    public int getUnreadNotificationsCount(String role) {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;
        try {
            ensureNotificationTableUpdated(db);
            String query = "SELECT COUNT(*) FROM " + TABLE_NOTIFICATIONS + " WHERE (target_role=? OR target_role='ALL') AND is_read=0";
            Cursor cursor = db.rawQuery(query, new String[]{role});
            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0);
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    public List<com.example.model.AppNotification> getNotificationsForRole(String role) {
        List<com.example.model.AppNotification> list = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ensureNotificationTableUpdated(db);
            Cursor cursor = db.query(TABLE_NOTIFICATIONS, null, "target_role=? OR target_role='ALL'", new String[]{role}, null, null, "id DESC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                    String message = cursor.getString(cursor.getColumnIndexOrThrow("message"));
                    String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                    String target = cursor.getString(cursor.getColumnIndexOrThrow("target_role"));
                    String category = cursor.getColumnIndex("category") != -1 ? cursor.getString(cursor.getColumnIndexOrThrow("category")) : "GENERAL";
                    String sender = cursor.getColumnIndex("sender") != -1 ? cursor.getString(cursor.getColumnIndexOrThrow("sender")) : "Faculty Admin";
                    int isReadInt = cursor.getColumnIndex("is_read") != -1 ? cursor.getInt(cursor.getColumnIndexOrThrow("is_read")) : 0;
                    list.add(new com.example.model.AppNotification(id, title, message, date, target, category, sender, isReadInt == 1));
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public List<com.example.model.AppNotification> getAllNotifications() {
        List<com.example.model.AppNotification> list = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ensureNotificationTableUpdated(db);
            Cursor cursor = db.query(TABLE_NOTIFICATIONS, null, null, null, null, null, "id DESC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                    String message = cursor.getString(cursor.getColumnIndexOrThrow("message"));
                    String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                    String target = cursor.getString(cursor.getColumnIndexOrThrow("target_role"));
                    String category = cursor.getColumnIndex("category") != -1 ? cursor.getString(cursor.getColumnIndexOrThrow("category")) : "GENERAL";
                    String sender = cursor.getColumnIndex("sender") != -1 ? cursor.getString(cursor.getColumnIndexOrThrow("sender")) : "Faculty Admin";
                    int isReadInt = cursor.getColumnIndex("is_read") != -1 ? cursor.getInt(cursor.getColumnIndexOrThrow("is_read")) : 0;
                    list.add(new com.example.model.AppNotification(id, title, message, date, target, category, sender, isReadInt == 1));
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean sendNotification(com.example.model.AppNotification notif) {
        SQLiteDatabase db = this.getWritableDatabase();
        ensureNotificationTableUpdated(db);
        ContentValues cv = new ContentValues();
        cv.put("title", notif.getTitle());
        cv.put("message", notif.getMessage());
        cv.put("date", notif.getDate());
        cv.put("target_role", notif.getTargetRole());
        cv.put("category", notif.getCategory());
        cv.put("sender", notif.getSender());
        cv.put("is_read", 0);
        long id = db.insert(TABLE_NOTIFICATIONS, null, cv);
        if (id != -1) {
            try {
                FirestoreHelper.getInstance().syncNotification(new com.example.model.AppNotification((int) id, notif.getTitle(), notif.getMessage(), notif.getDate(), notif.getTargetRole(), notif.getCategory(), notif.getSender(), false));
            } catch (Exception ignored) {}
        }
        return id != -1;
    }

    public boolean markNotificationAsRead(int notifId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ensureNotificationTableUpdated(db);
        ContentValues cv = new ContentValues();
        cv.put("is_read", 1);
        int rows = db.update(TABLE_NOTIFICATIONS, cv, "id=?", new String[]{String.valueOf(notifId)});
        return rows > 0;
    }

    public boolean deleteNotification(int notifId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_NOTIFICATIONS, "id=?", new String[]{String.valueOf(notifId)});
        if (rows > 0) {
            try { FirestoreHelper.getInstance().deleteNotification(notifId); } catch (Exception ignored) {}
        }
        return rows > 0;
    }

    private void ensureNotificationTableUpdated(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NOTIFICATIONS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, message TEXT, date TEXT, target_role TEXT, category TEXT, sender TEXT, is_read INTEGER DEFAULT 0)");
            Cursor c = db.rawQuery("PRAGMA table_info(" + TABLE_NOTIFICATIONS + ")", null);
            boolean hasCategory = false, hasSender = false, hasIsRead = false;
            if (c != null && c.moveToFirst()) {
                do {
                    String col = c.getString(c.getColumnIndexOrThrow("name"));
                    if ("category".equalsIgnoreCase(col)) hasCategory = true;
                    if ("sender".equalsIgnoreCase(col)) hasSender = true;
                    if ("is_read".equalsIgnoreCase(col)) hasIsRead = true;
                } while (c.moveToNext());
                c.close();
            }
            if (!hasCategory) db.execSQL("ALTER TABLE " + TABLE_NOTIFICATIONS + " ADD COLUMN category TEXT DEFAULT 'GENERAL'");
            if (!hasSender) db.execSQL("ALTER TABLE " + TABLE_NOTIFICATIONS + " ADD COLUMN sender TEXT DEFAULT 'Faculty Admin'");
            if (!hasIsRead) db.execSQL("ALTER TABLE " + TABLE_NOTIFICATIONS + " ADD COLUMN is_read INTEGER DEFAULT 0");
        } catch (Exception ignored) {}
    }

    private void insertDefaultNotification(SQLiteDatabase db, String title, String msg, String date, String role, String cat, String sender) {
        ContentValues cv = new ContentValues();
        cv.put("title", title);
        cv.put("message", msg);
        cv.put("date", date);
        cv.put("target_role", role);
        cv.put("category", cat);
        cv.put("sender", sender);
        cv.put("is_read", 0);
        db.insert(TABLE_NOTIFICATIONS, null, cv);
    }

    /**
     * Retrieves distribution counts for all academic grades (A+, A, B+, B, C, D, F)
     * across student marks and evaluated results.
     */
    public java.util.Map<String, Integer> getGradeDistribution() {
        java.util.Map<String, Integer> map = new java.util.LinkedHashMap<>();
        map.put("A+", 0);
        map.put("A", 0);
        map.put("B+", 0);
        map.put("B", 0);
        map.put("C", 0);
        map.put("D", 0);
        map.put("F", 0);

        SQLiteDatabase db = this.getReadableDatabase();
        int totalFound = 0;

        try {
            // 1. Query from TABLE_MARKS
            Cursor cursor = db.rawQuery("SELECT grade, COUNT(*) FROM " + TABLE_MARKS + " WHERE grade IS NOT NULL AND TRIM(grade) != '' GROUP BY grade", null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String g = cursor.getString(0).toUpperCase(Locale.US).trim();
                    int count = cursor.getInt(1);
                    if (map.containsKey(g)) {
                        map.put(g, map.get(g) + count);
                        totalFound += count;
                    } else if ("O".equals(g)) {
                        map.put("A+", map.get("A+") + count);
                        totalFound += count;
                    }
                } while (cursor.moveToNext());
                cursor.close();
            }

            // 2. Query from TABLE_RESULTS if marks was empty
            if (totalFound == 0) {
                Cursor rCursor = db.rawQuery("SELECT percentage FROM " + TABLE_RESULTS, null);
                if (rCursor != null && rCursor.moveToFirst()) {
                    do {
                        double pct = rCursor.getDouble(0);
                        String g = pct >= 90 ? "A+" : pct >= 80 ? "A" : pct >= 70 ? "B+" : pct >= 60 ? "B" : pct >= 50 ? "C" : pct >= 40 ? "D" : "F";
                        map.put(g, map.get(g) + 1);
                        totalFound++;
                    } while (rCursor.moveToNext());
                    rCursor.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return map;
    }

    // ==========================================
    // CLOUD SYNC UPSERT HELPERS (FIRESTORE -> SQLITE)
    // ==========================================

    public void upsertStudentFromFirestore(int id, String name, String regNo, String dept, int sem, String email, String phone, String photoUri) {
        upsertStudentFromFirestore(id, name, regNo, dept, sem, email, phone, photoUri, null, null);
    }

    public void upsertStudentFromFirestore(int id, String name, String regNo, String dept, int sem, String email, String phone, String photoUri, String studentUid) {
        upsertStudentFromFirestore(id, name, regNo, dept, sem, email, phone, photoUri, studentUid, null);
    }

    public void upsertStudentFromFirestore(int id, String name, String regNo, String dept, int sem, String email, String phone, String photoUri, String studentUid, String loginId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name != null ? name : "");
        cv.put("reg_no", regNo != null ? regNo : "");
        cv.put("department", dept != null ? dept : "");
        cv.put("semester", sem);
        cv.put("email", email != null ? email : "");
        cv.put("phone", phone != null ? phone : "");
        cv.put("photo_uri", photoUri != null ? photoUri : "");
        if (studentUid != null && !studentUid.trim().isEmpty()) {
            cv.put("student_uid", studentUid.trim());
            cv.put("firebase_uid", studentUid.trim());
        }
        if (loginId != null && !loginId.trim().isEmpty()) {
            cv.put("login_id", loginId.trim());
        }

        Cursor cursor = db.query(TABLE_STUDENTS, new String[]{"id"}, "reg_no=? OR (email=? AND email != '') OR id=?",
                new String[]{regNo, email, String.valueOf(id)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int existingId = cursor.getInt(0);
            cursor.close();
            db.update(TABLE_STUDENTS, cv, "id=?", new String[]{String.valueOf(existingId)});
        } else {
            if (cursor != null) cursor.close();
            if (id > 0) cv.put("id", id);
            cv.put("password", PasswordUtils.hashPassword("student123"));
            db.insertWithOnConflict(TABLE_STUDENTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    public void upsertTeacherFromFirestore(int id, String name, String email, String dept, String phone) {
        upsertTeacherFromFirestore(id, name, email, dept, phone, null, null, null);
    }

    public void upsertTeacherFromFirestore(int id, String name, String email, String dept, String phone, String employeeId, String loginId, String firebaseUid) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name != null ? name : "");
        cv.put("email", email != null ? email : "");
        cv.put("department", dept != null ? dept : "");
        cv.put("phone", phone != null ? phone : "");
        if (employeeId != null && !employeeId.trim().isEmpty()) {
            cv.put("employee_id", employeeId.trim());
        }
        if (loginId != null && !loginId.trim().isEmpty()) {
            cv.put("login_id", loginId.trim());
        }
        if (firebaseUid != null && !firebaseUid.trim().isEmpty()) {
            cv.put("firebase_uid", firebaseUid.trim());
        }

        Cursor cursor = db.query(TABLE_TEACHERS, new String[]{"id"}, "email=? OR id=?",
                new String[]{email, String.valueOf(id)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int existingId = cursor.getInt(0);
            cursor.close();
            db.update(TABLE_TEACHERS, cv, "id=?", new String[]{String.valueOf(existingId)});
        } else {
            if (cursor != null) cursor.close();
            if (id > 0) cv.put("id", id);
            cv.put("password", PasswordUtils.hashPassword("teacher123"));
            db.insertWithOnConflict(TABLE_TEACHERS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    public void upsertSubjectFromFirestore(int id, String code, String name, int credits, int sem, String dept) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("subject_code", code != null ? code : "");
        cv.put("subject_name", name != null ? name : "");
        cv.put("credits", credits);
        cv.put("semester", sem);
        cv.put("department", dept != null ? dept : "");

        Cursor cursor = db.query(TABLE_SUBJECTS, new String[]{"id"}, "subject_code=? OR id=?",
                new String[]{code, String.valueOf(id)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int existingId = cursor.getInt(0);
            cursor.close();
            db.update(TABLE_SUBJECTS, cv, "id=?", new String[]{String.valueOf(existingId)});
        } else {
            if (cursor != null) cursor.close();
            if (id > 0) cv.put("id", id);
            db.insertWithOnConflict(TABLE_SUBJECTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    public void upsertAssignmentFromFirestore(int id, String title, int subjectId, String deadline, String description, String filePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("title", title != null ? title : "");
        cv.put("subject_id", subjectId);
        cv.put("deadline", deadline != null ? deadline : "");
        cv.put("description", description != null ? description : "");
        cv.put("file_path", filePath != null ? filePath : "");

        Cursor cursor = db.query(TABLE_ASSIGNMENTS, new String[]{"id"}, "id=?", new String[]{String.valueOf(id)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            cursor.close();
            db.update(TABLE_ASSIGNMENTS, cv, "id=?", new String[]{String.valueOf(id)});
        } else {
            if (cursor != null) cursor.close();
            if (id > 0) cv.put("id", id);
            db.insertWithOnConflict(TABLE_ASSIGNMENTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    public void upsertAttendanceFromFirestore(int studentId, int subjectId, String date, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("student_id", studentId);
        cv.put("subject_id", subjectId);
        cv.put("date", date != null ? date : "");
        cv.put("status", status != null ? status.toUpperCase() : "PRESENT");

        Cursor cursor = db.query(TABLE_ATTENDANCE, new String[]{"id"}, "student_id=? AND subject_id=? AND date=?",
                new String[]{String.valueOf(studentId), String.valueOf(subjectId), date}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int existingId = cursor.getInt(0);
            cursor.close();
            db.update(TABLE_ATTENDANCE, cv, "id=?", new String[]{String.valueOf(existingId)});
        } else {
            if (cursor != null) cursor.close();
            db.insert(TABLE_ATTENDANCE, null, cv);
        }
    }

    public void upsertMarkFromFirestore(int studentId, int subjectId, double internal1, double assignment, double modelExam, double lab, double universityExam, double totalMarks, double percentage, String grade, double gradePoint) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("student_id", studentId);
        cv.put("subject_id", subjectId);
        cv.put("internal1", internal1);
        cv.put("assignment", assignment);
        cv.put("model_exam", modelExam);
        cv.put("lab", lab);
        cv.put("university_exam", universityExam);
        cv.put("total_marks", totalMarks);
        cv.put("percentage", percentage);
        cv.put("grade", grade != null ? grade : "");
        cv.put("grade_point", gradePoint);

        Cursor cursor = db.query(TABLE_MARKS, new String[]{"id"}, "student_id=? AND subject_id=?",
                new String[]{String.valueOf(studentId), String.valueOf(subjectId)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int existingId = cursor.getInt(0);
            cursor.close();
            db.update(TABLE_MARKS, cv, "id=?", new String[]{String.valueOf(existingId)});
        } else {
            if (cursor != null) cursor.close();
            db.insert(TABLE_MARKS, null, cv);
        }
    }

    public void upsertResultFromFirestore(int studentId, int semester, double totalMarks, double percentage, double sgpa, double cgpa, String status, String publishedDate) {
        upsertResultFromFirestore(studentId, semester, totalMarks, percentage, sgpa, cgpa, status, publishedDate, null);
    }

    public void upsertResultFromFirestore(int studentId, int semester, double totalMarks, double percentage, double sgpa, double cgpa, String status, String publishedDate, String studentUid) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("student_id", studentId);
        cv.put("semester", semester);
        cv.put("total_marks", totalMarks);
        cv.put("percentage", percentage);
        cv.put("sgpa", sgpa);
        cv.put("cgpa", cgpa);
        cv.put("status", status != null ? status : "DRAFT");
        cv.put("published_date", publishedDate != null ? publishedDate : "");
        cv.put("sync_status", "SYNCED");
        if (studentUid != null && !studentUid.isEmpty()) {
            cv.put("student_uid", studentUid);
        }

        Cursor cursor = db.query(TABLE_RESULTS, new String[]{"id"}, "student_id=? AND semester=?",
                new String[]{String.valueOf(studentId), String.valueOf(semester)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int existingId = cursor.getInt(0);
            cursor.close();
            db.update(TABLE_RESULTS, cv, "id=?", new String[]{String.valueOf(existingId)});
        } else {
            if (cursor != null) cursor.close();
            db.insert(TABLE_RESULTS, null, cv);
        }
    }

    public void upsertNotificationFromFirestore(int id, String title, String message, String date, String targetRole, String category, String sender, boolean isRead) {
        SQLiteDatabase db = this.getWritableDatabase();
        ensureNotificationTableUpdated(db);
        ContentValues cv = new ContentValues();
        cv.put("title", title != null ? title : "");
        cv.put("message", message != null ? message : "");
        cv.put("date", date != null ? date : "");
        cv.put("target_role", targetRole != null ? targetRole : "ALL");
        cv.put("category", category != null ? category : "GENERAL");
        cv.put("sender", sender != null ? sender : "Admin");
        cv.put("is_read", isRead ? 1 : 0);

        Cursor cursor = db.query(TABLE_NOTIFICATIONS, new String[]{"id"}, "id=?", new String[]{String.valueOf(id)}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            cursor.close();
            db.update(TABLE_NOTIFICATIONS, cv, "id=?", new String[]{String.valueOf(id)});
        } else {
            if (cursor != null) cursor.close();
            if (id > 0) cv.put("id", id);
            db.insertWithOnConflict(TABLE_NOTIFICATIONS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    public void upsertAssessmentFromFirestore(int id, String title, String type, int subjectId, int semester, String date, double maxMarks, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ASSESSMENTS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, type TEXT, subject_id INTEGER, semester INTEGER, date TEXT, max_marks REAL, status TEXT DEFAULT 'PENDING')");
            ContentValues cv = new ContentValues();
            cv.put("title", title != null ? title : "");
            cv.put("type", type != null ? type : "Exam");
            cv.put("subject_id", subjectId);
            cv.put("semester", semester);
            cv.put("date", date != null ? date : "");
            cv.put("max_marks", maxMarks);
            cv.put("status", status != null ? status : "PENDING");

            Cursor cursor = db.query(TABLE_ASSESSMENTS, new String[]{"id"}, "id=?", new String[]{String.valueOf(id)}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                cursor.close();
                db.update(TABLE_ASSESSMENTS, cv, "id=?", new String[]{String.valueOf(id)});
            } else {
                if (cursor != null) cursor.close();
                if (id > 0) cv.put("id", id);
                db.insertWithOnConflict(TABLE_ASSESSMENTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==========================================
    // PASSWORD RESET REQUESTS (LOCAL & SYNC)
    // ==========================================

    public long insertPasswordResetRequest(PasswordResetRequest request) {
        if (request == null) return -1;
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PASSWORD_RESETS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, request_id TEXT UNIQUE, user_id TEXT, user_name TEXT, email TEXT, role TEXT, identifier TEXT, department TEXT, semester TEXT, status TEXT DEFAULT 'PENDING', requested_at TEXT, processed_at TEXT, processed_by TEXT, admin_note TEXT)");
            ContentValues cv = new ContentValues();
            cv.put("request_id", request.getRequestId());
            cv.put("user_id", request.getUserId());
            cv.put("user_name", request.getUserName());
            cv.put("email", request.getEmail());
            cv.put("role", request.getRole());
            cv.put("identifier", request.getIdentifier());
            cv.put("department", request.getDepartment());
            cv.put("semester", request.getSemester());
            cv.put("status", request.getStatus() != null ? request.getStatus() : "PENDING");
            long timeMs = request.getRequestedAt() != null ? request.getRequestedAt().getTime() : System.currentTimeMillis();
            cv.put("requested_at", String.valueOf(timeMs));
            cv.put("processed_by", request.getProcessedBy() != null ? request.getProcessedBy() : "");
            cv.put("admin_note", request.getAdminNote() != null ? request.getAdminNote() : "");

            return db.insertWithOnConflict(TABLE_PASSWORD_RESETS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public List<PasswordResetRequest> getAllPasswordResetRequests(String statusFilter) {
        List<PasswordResetRequest> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PASSWORD_RESETS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, request_id TEXT UNIQUE, user_id TEXT, user_name TEXT, email TEXT, role TEXT, identifier TEXT, department TEXT, semester TEXT, status TEXT DEFAULT 'PENDING', requested_at TEXT, processed_at TEXT, processed_by TEXT, admin_note TEXT)");

            String selection = null;
            String[] selectionArgs = null;
            if (statusFilter != null && !"ALL".equalsIgnoreCase(statusFilter)) {
                selection = "UPPER(status)=?";
                selectionArgs = new String[]{statusFilter.toUpperCase()};
            }

            Cursor cursor = db.query(TABLE_PASSWORD_RESETS, null, selection, selectionArgs, null, null, "id DESC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String reqId = cursor.getString(cursor.getColumnIndexOrThrow("request_id"));
                    String userId = cursor.getString(cursor.getColumnIndexOrThrow("user_id"));
                    String userName = cursor.getString(cursor.getColumnIndexOrThrow("user_name"));
                    String email = cursor.getString(cursor.getColumnIndexOrThrow("email"));
                    String role = cursor.getString(cursor.getColumnIndexOrThrow("role"));
                    String identifier = cursor.getString(cursor.getColumnIndexOrThrow("identifier"));
                    String dept = cursor.getString(cursor.getColumnIndexOrThrow("department"));
                    String sem = cursor.getString(cursor.getColumnIndexOrThrow("semester"));
                    String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                    String reqAtStr = cursor.getString(cursor.getColumnIndexOrThrow("requested_at"));
                    String procBy = cursor.getString(cursor.getColumnIndexOrThrow("processed_by"));
                    String note = cursor.getString(cursor.getColumnIndexOrThrow("admin_note"));

                    PasswordResetRequest req = new PasswordResetRequest(reqId, userId, userName, email, role, identifier, dept, sem);
                    req.setStatus(status != null ? status : "PENDING");
                    if (reqAtStr != null && !reqAtStr.isEmpty()) {
                        try {
                            req.setRequestedAt(new Date(Long.parseLong(reqAtStr)));
                        } catch (Exception ignore) {
                            req.setRequestedAt(new Date());
                        }
                    } else {
                        req.setRequestedAt(new Date());
                    }
                    req.setProcessedBy(procBy);
                    req.setAdminNote(note);
                    list.add(req);
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public int getPendingPasswordResetRequestsCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PASSWORD_RESETS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, request_id TEXT UNIQUE, user_id TEXT, user_name TEXT, email TEXT, role TEXT, identifier TEXT, department TEXT, semester TEXT, status TEXT DEFAULT 'PENDING', requested_at TEXT, processed_at TEXT, processed_by TEXT, admin_note TEXT)");
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PASSWORD_RESETS + " WHERE UPPER(status)='PENDING'", null);
            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0);
                cursor.close();
            }
        } catch (Exception e) {
            count = 0;
        }
        return count;
    }

    public boolean updatePasswordResetRequestStatus(String requestId, String status, String processedBy, String adminNote) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PASSWORD_RESETS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, request_id TEXT UNIQUE, user_id TEXT, user_name TEXT, email TEXT, role TEXT, identifier TEXT, department TEXT, semester TEXT, status TEXT DEFAULT 'PENDING', requested_at TEXT, processed_at TEXT, processed_by TEXT, admin_note TEXT)");
            ContentValues cv = new ContentValues();
            cv.put("status", status.toUpperCase());
            cv.put("processed_at", String.valueOf(System.currentTimeMillis()));
            cv.put("processed_by", processedBy != null ? processedBy : "System Admin");
            if (adminNote != null) cv.put("admin_note", adminNote);

            int rows = db.update(TABLE_PASSWORD_RESETS, cv, "request_id=?", new String[]{requestId});
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ==========================================
    // DELETION & CLOUD RECONCILIATION (PRUNING)
    // ==========================================

    public boolean deleteResult(int studentId, int semester) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_RESULTS, "student_id=? AND semester=?", new String[]{String.valueOf(studentId), String.valueOf(semester)}) > 0;
    }

    public boolean deleteResultByUidOrId(String studentUidOrId, int semester) {
        if (studentUidOrId == null || studentUidOrId.trim().isEmpty()) return false;
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_RESULTS, "(student_uid=? OR student_id=?) AND semester=?", new String[]{studentUidOrId.trim(), studentUidOrId.trim(), String.valueOf(semester)}) > 0;
    }

    public void pruneStudents(Set<String> activeUids, Set<Integer> activeNumericIds, Set<String> activeRegNos) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_STUDENTS, new String[]{"id", "student_uid", "reg_no"}, null, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            List<Integer> idsToDelete = new ArrayList<>();
            do {
                int id = cursor.getInt(0);
                String uid = cursor.isNull(1) ? null : cursor.getString(1);
                String reg = cursor.isNull(2) ? null : cursor.getString(2);

                boolean exists = (activeNumericIds != null && activeNumericIds.contains(id))
                        || (uid != null && activeUids != null && activeUids.contains(uid))
                        || (reg != null && activeRegNos != null && activeRegNos.contains(reg));

                if (!exists) {
                    idsToDelete.add(id);
                }
            } while (cursor.moveToNext());
            cursor.close();

            for (Integer delId : idsToDelete) {
                db.delete(TABLE_STUDENTS, "id=?", new String[]{String.valueOf(delId)});
            }
        } else if (cursor != null) {
            cursor.close();
        }
    }

    public void pruneTeachers(Set<String> activeEmails, Set<Integer> activeNumericIds, Set<String> activeEmpIds) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_TEACHERS, new String[]{"id", "email", "employee_id"}, null, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            List<Integer> idsToDelete = new ArrayList<>();
            do {
                int id = cursor.getInt(0);
                String email = cursor.isNull(1) ? null : cursor.getString(1);
                String empId = cursor.isNull(2) ? null : cursor.getString(2);

                boolean exists = (activeNumericIds != null && activeNumericIds.contains(id))
                        || (email != null && activeEmails != null && activeEmails.contains(email))
                        || (empId != null && activeEmpIds != null && activeEmpIds.contains(empId));

                if (!exists) {
                    idsToDelete.add(id);
                }
            } while (cursor.moveToNext());
            cursor.close();

            for (Integer delId : idsToDelete) {
                db.delete(TABLE_TEACHERS, "id=?", new String[]{String.valueOf(delId)});
            }
        } else if (cursor != null) {
            cursor.close();
        }
    }

    public void pruneSubjects(Set<String> activeCodes, Set<Integer> activeIds) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_SUBJECTS, new String[]{"id", "subject_code"}, null, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            List<Integer> idsToDelete = new ArrayList<>();
            do {
                int id = cursor.getInt(0);
                String code = cursor.isNull(1) ? null : cursor.getString(1);

                boolean exists = (activeIds != null && activeIds.contains(id))
                        || (code != null && activeCodes != null && activeCodes.contains(code));

                if (!exists) {
                    idsToDelete.add(id);
                }
            } while (cursor.moveToNext());
            cursor.close();

            for (Integer delId : idsToDelete) {
                db.delete(TABLE_SUBJECTS, "id=?", new String[]{String.valueOf(delId)});
            }
        } else if (cursor != null) {
            cursor.close();
        }
    }

    public void pruneAssignments(Set<Integer> activeIds) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_ASSIGNMENTS, new String[]{"id"}, null, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            List<Integer> idsToDelete = new ArrayList<>();
            do {
                int id = cursor.getInt(0);
                if (activeIds == null || !activeIds.contains(id)) {
                    idsToDelete.add(id);
                }
            } while (cursor.moveToNext());
            cursor.close();

            for (Integer delId : idsToDelete) {
                db.delete(TABLE_ASSIGNMENTS, "id=?", new String[]{String.valueOf(delId)});
            }
        } else if (cursor != null) {
            cursor.close();
        }
    }

    public void pruneResults(Set<String> activeResultKeys) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_RESULTS, new String[]{"id", "student_id", "semester", "student_uid", "sync_status"}, null, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            List<Integer> idsToDelete = new ArrayList<>();
            do {
                int rowId = cursor.getInt(0);
                int stId = cursor.getInt(1);
                int sem = cursor.getInt(2);
                String sUid = cursor.isNull(3) ? null : cursor.getString(3);
                String syncStatus = cursor.isNull(4) ? null : cursor.getString(4);

                if ("LOCAL_PENDING_SYNC".equalsIgnoreCase(syncStatus)) {
                    continue;
                }

                String key1 = stId + "_" + sem;
                String key2 = (sUid != null && !sUid.isEmpty()) ? (sUid + "_" + sem) : null;

                boolean exists = (activeResultKeys != null) && (activeResultKeys.contains(key1) || (key2 != null && activeResultKeys.contains(key2)));
                if (!exists) {
                    idsToDelete.add(rowId);
                }
            } while (cursor.moveToNext());
            cursor.close();

            for (Integer delId : idsToDelete) {
                db.delete(TABLE_RESULTS, "id=?", new String[]{String.valueOf(delId)});
            }
        } else if (cursor != null) {
            cursor.close();
        }
    }

    public void pruneNotifications(Set<Integer> activeIds) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_NOTIFICATIONS, new String[]{"id"}, null, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            List<Integer> idsToDelete = new ArrayList<>();
            do {
                int id = cursor.getInt(0);
                if (activeIds == null || !activeIds.contains(id)) {
                    idsToDelete.add(id);
                }
            } while (cursor.moveToNext());
            cursor.close();

            for (Integer delId : idsToDelete) {
                db.delete(TABLE_NOTIFICATIONS, "id=?", new String[]{String.valueOf(delId)});
            }
        } else if (cursor != null) {
            cursor.close();
        }
    }

    public void pruneAssessments(Set<Integer> activeIds) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            Cursor cursor = db.query(TABLE_ASSESSMENTS, new String[]{"id"}, null, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                List<Integer> idsToDelete = new ArrayList<>();
                do {
                    int id = cursor.getInt(0);
                    if (activeIds == null || !activeIds.contains(id)) {
                        idsToDelete.add(id);
                    }
                } while (cursor.moveToNext());
                cursor.close();

                for (Integer delId : idsToDelete) {
                    db.delete(TABLE_ASSESSMENTS, "id=?", new String[]{String.valueOf(delId)});
                }
            } else if (cursor != null) {
                cursor.close();
            }
        } catch (Exception ignored) {}
    }

    // ==========================================
    // STUDENT ATTENDANCE MONITORING MODULE
    // ==========================================

    public long addStudentAttendanceLog(int studentId, String subjectName, int totalClasses, int attendedClasses,
                                        double percentage, String loggedDate, String notes, double threshold) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("student_id", studentId);
        cv.put("subject_name", (subjectName != null && !subjectName.trim().isEmpty()) ? subjectName.trim() : "General Subject");
        cv.put("total_classes", Math.max(1, totalClasses));
        cv.put("attended_classes", Math.max(0, attendedClasses));
        cv.put("percentage", percentage);
        String dateStr = (loggedDate != null && !loggedDate.trim().isEmpty()) ? loggedDate.trim() :
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new java.util.Date());
        cv.put("logged_date", dateStr);
        cv.put("notes", notes != null ? notes.trim() : "");
        cv.put("threshold", threshold);

        String status = (percentage < threshold) ? "BELOW_THRESHOLD" : "NORMAL";
        cv.put("status", status);

        long id = db.insert(TABLE_STUDENT_ATTENDANCE_LOGS, null, cv);

        // If attendance falls below threshold, automatically dispatch in-app notification & activity log
        if (percentage < threshold) {
            String alertTitle = "⚠️ Low Attendance Alert: " + subjectName;
            String alertMsg = String.format(Locale.US,
                    "Your logged attendance for %s is %.1f%%, which is below your %.0f%% target threshold. Attend upcoming classes to avoid shortage!",
                    subjectName, percentage, threshold);
            addNotification(alertTitle, alertMsg, dateStr, "STUDENT", "ATTENDANCE_ALERT", "Attendance Monitor");
        }

        return id;
    }

    public List<StudentAttendanceLog> getStudentAttendanceLogs(int studentId) {
        List<StudentAttendanceLog> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_STUDENT_ATTENDANCE_LOGS, null, "student_id=?",
                    new String[]{String.valueOf(studentId)}, null, null, "id DESC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    int sId = cursor.getInt(cursor.getColumnIndexOrThrow("student_id"));
                    String subName = cursor.getString(cursor.getColumnIndexOrThrow("subject_name"));
                    int total = cursor.getInt(cursor.getColumnIndexOrThrow("total_classes"));
                    int attended = cursor.getInt(cursor.getColumnIndexOrThrow("attended_classes"));
                    double pct = cursor.getDouble(cursor.getColumnIndexOrThrow("percentage"));
                    String date = cursor.getString(cursor.getColumnIndexOrThrow("logged_date"));
                    String notes = cursor.getString(cursor.getColumnIndexOrThrow("notes"));
                    double thresh = cursor.getDouble(cursor.getColumnIndexOrThrow("threshold"));
                    String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));

                    list.add(new StudentAttendanceLog(id, sId, subName, total, attended, pct, date, notes, thresh, status));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }

    public boolean deleteStudentAttendanceLog(int logId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_STUDENT_ATTENDANCE_LOGS, "id=?", new String[]{String.valueOf(logId)});
        return rows > 0;
    }

    public int getBelowThresholdAttendanceLogsCount(int studentId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_STUDENT_ATTENDANCE_LOGS
                    + " WHERE student_id=? AND status='BELOW_THRESHOLD'", new String[]{String.valueOf(studentId)});
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return 0;
    }
}


