package com.example;

import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.database.DatabaseHelper;
import com.example.database.FirestoreHelper;
import com.example.model.Department;
import com.example.model.Student;
import com.example.model.Subject;
import com.example.repository.StudentRepository;
import com.example.repository.SubjectRepository;
import com.example.utils.PortalActivityLogger;
import com.example.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Activity for adding, calculating, and editing student subject marks in GradeXpert ERP.
 * Supports Teacher-Subject assignment restrictions, Cloud Firestore primary student retrieval,
 * dynamic UG/PG Program Level, Department, and Semester filtering, and local SQLite cache fallback.
 */
public class AddMarksActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private StudentRepository studentRepository;
    private SubjectRepository subjectRepository;

    private ImageView btnBack;
    private AutoCompleteTextView actvDepartment, actvSemester, actvStudent, actvSubject;
    private TextInputEditText etInternalMarks, etAssignmentMarks, etModelExamMarks, etLabMarks, etUniversityExamMarks;
    private TextView tvTotalMarks, tvPercentage, tvGrade, tvGradePoint;
    private MaterialButton btnSaveMarks, btnDeleteMarks;

    // Student selection list & state views
    private LinearLayout layoutStudentLoading;
    private TextView tvStudentLoadingStatus, tvStudentEmptyStatus;
    private RecyclerView rvStudentsList;
    private StudentMarksSelectionAdapter studentSelectionAdapter;

    private List<Subject> subjectList = new ArrayList<>();
    private List<Student> cachedAllStudents = new ArrayList<>();
    private List<Student> currentSubjectStudents = new ArrayList<>();
    private List<String> teacherAssignedSubjectIds = new ArrayList<>();
    private boolean isTeacherUser = false;

    private Student selectedStudent = null;
    private Subject selectedSubject = null;
    private boolean isExistingRecord = false;
    private boolean isInitialStudentsLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_marks);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);
        studentRepository = StudentRepository.getInstance(this);
        subjectRepository = SubjectRepository.getInstance(this);

        initViews();
        setupRecyclerView();
        setupRealtimeCalculation();
        setupListeners();
        checkTeacherAssignmentAuthorization();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackAddMarks);

        actvDepartment = findViewById(R.id.actvDepartment);
        actvSemester = findViewById(R.id.actvSemester);
        actvStudent = findViewById(R.id.actvStudent);
        actvSubject = findViewById(R.id.actvSubject);

        layoutStudentLoading = findViewById(R.id.layoutStudentLoading);
        tvStudentLoadingStatus = findViewById(R.id.tvStudentLoadingStatus);
        tvStudentEmptyStatus = findViewById(R.id.tvStudentEmptyStatus);
        rvStudentsList = findViewById(R.id.rvStudentsList);

        etInternalMarks = findViewById(R.id.etInternalMarks);
        etAssignmentMarks = findViewById(R.id.etAssignmentMarks);
        etModelExamMarks = findViewById(R.id.etModelExamMarks);
        etLabMarks = findViewById(R.id.etLabMarks);
        etUniversityExamMarks = findViewById(R.id.etUniversityExamMarks);

        tvTotalMarks = findViewById(R.id.tvTotalMarks);
        tvPercentage = findViewById(R.id.tvPercentage);
        tvGrade = findViewById(R.id.tvGrade);
        tvGradePoint = findViewById(R.id.tvGradePoint);

        btnSaveMarks = findViewById(R.id.btnSaveMarks);
        btnDeleteMarks = findViewById(R.id.btnDeleteMarks);
    }

    private void setupRecyclerView() {
        rvStudentsList.setLayoutManager(new LinearLayoutManager(this));
        studentSelectionAdapter = new StudentMarksSelectionAdapter(currentSubjectStudents);
        rvStudentsList.setAdapter(studentSelectionAdapter);
    }

    /**
     * Verifies teacher identity and queries all existing teacher-subject assignment sources:
     * 1. teachers/{uid} assignedSubjectIds
     * 2. teachers collection where email == teacherEmail
     * 3. teacher_assignments collection where teacherEmail == teacherEmail
     */
    private void checkTeacherAssignmentAuthorization() {
        isTeacherUser = "TEACHER".equalsIgnoreCase(sessionManager.getUserRole());

        if (isTeacherUser) {
            FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
            String uid = fUser != null ? fUser.getUid() : "";
            String email = sessionManager.getUserEmail();

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            teacherAssignedSubjectIds.clear();

            if (!uid.isEmpty()) {
                db.collection("teachers").document(uid).get().addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        List<String> ids = (List<String>) doc.get("assignedSubjectIds");
                        if (ids != null) {
                            for (String id : ids) {
                                if (id != null && !teacherAssignedSubjectIds.contains(id)) {
                                    teacherAssignedSubjectIds.add(id);
                                }
                            }
                        }
                    }
                    checkTeacherAssignmentsByEmail(email);
                }).addOnFailureListener(e -> checkTeacherAssignmentsByEmail(email));
            } else {
                checkTeacherAssignmentsByEmail(email);
            }
        } else {
            setupDropdownsAndLoadSubjects();
        }
    }

    private void checkTeacherAssignmentsByEmail(String email) {
        if (email != null && !email.trim().isEmpty()) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("teachers")
                    .whereEqualTo("email", email.trim().toLowerCase())
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        if (snapshots != null && !snapshots.isEmpty()) {
                            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                List<String> ids = (List<String>) doc.get("assignedSubjectIds");
                                if (ids != null) {
                                    for (String id : ids) {
                                        if (id != null && !teacherAssignedSubjectIds.contains(id)) {
                                            teacherAssignedSubjectIds.add(id);
                                        }
                                    }
                                }
                            }
                        }
                        checkTeacherAssignmentsCollection(email);
                    })
                    .addOnFailureListener(e -> checkTeacherAssignmentsCollection(email));
        } else {
            setupDropdownsAndLoadSubjects();
        }
    }

    private void checkTeacherAssignmentsCollection(String email) {
        if (email != null && !email.trim().isEmpty()) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("teacher_assignments")
                    .whereEqualTo("teacherEmail", email.trim().toLowerCase())
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        if (snapshots != null && !snapshots.isEmpty()) {
                            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                String code = doc.getString("subjectCode");
                                if (code != null && !code.trim().isEmpty() && !teacherAssignedSubjectIds.contains(code)) {
                                    teacherAssignedSubjectIds.add(code);
                                }
                            }
                        }
                        setupDropdownsAndLoadSubjects();
                    })
                    .addOnFailureListener(e -> setupDropdownsAndLoadSubjects());
        } else {
            setupDropdownsAndLoadSubjects();
        }
    }

    private void setupDropdownsAndLoadSubjects() {
        String[] departments = {
                "Master of Computer Applications",
                "Computer Science & Engineering",
                "Bachelor of Computer Applications",
                "Information Technology",
                "Electronics & Communication"
        };
        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, departments);
        actvDepartment.setAdapter(deptAdapter);

        String[] semesters = {
                "Semester 1", "Semester 2", "Semester 3", "Semester 4",
                "Semester 5", "Semester 6", "Semester 7", "Semester 8",
                "Semester I", "Semester II", "Semester III", "Semester IV"
        };
        ArrayAdapter<String> semAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, semesters);
        actvSemester.setAdapter(semAdapter);

        loadSubjectsFromRepository();
        preloadStudentsFromRepository();
    }

    private void loadSubjectsFromRepository() {
        subjectRepository.fetchSubjects(new SubjectRepository.OnSubjectsLoadedListener() {
            @Override
            public void onSuccess(List<Subject> subjects) {
                subjectList.clear();
                if (subjects != null) {
                    for (Subject sub : subjects) {
                        if ("INACTIVE".equalsIgnoreCase(sub.getStatus())) continue;

                        if (isTeacherUser && !teacherAssignedSubjectIds.isEmpty()) {
                            if (teacherAssignedSubjectIds.contains(sub.getSubjectId()) ||
                                    teacherAssignedSubjectIds.contains(sub.getSubjectCode())) {
                                subjectList.add(sub);
                            }
                        } else if (!isTeacherUser) {
                            subjectList.add(sub);
                        }
                    }

                    // If teacher user but no assignments were found in Firestore, fallback to check all active subjects
                    if (isTeacherUser && subjectList.isEmpty()) {
                        for (Subject sub : subjects) {
                            if (!"INACTIVE".equalsIgnoreCase(sub.getStatus())) {
                                if (teacherAssignedSubjectIds.contains(sub.getSubjectId()) ||
                                        teacherAssignedSubjectIds.contains(sub.getSubjectCode())) {
                                    subjectList.add(sub);
                                }
                            }
                        }
                    }
                }

                populateSubjectDropdown(subjectList);
            }

            @Override
            public void onError(String errorMessage) {
                // SQLite fallback
                List<Subject> localSubs = dbHelper.getAllSubjects();
                subjectList.clear();
                if (localSubs != null) {
                    for (Subject sub : localSubs) {
                        if ("INACTIVE".equalsIgnoreCase(sub.getStatus())) continue;
                        if (isTeacherUser && !teacherAssignedSubjectIds.isEmpty()) {
                            if (teacherAssignedSubjectIds.contains(sub.getSubjectId()) ||
                                    teacherAssignedSubjectIds.contains(sub.getSubjectCode())) {
                                subjectList.add(sub);
                            }
                        } else {
                            subjectList.add(sub);
                        }
                    }
                }
                populateSubjectDropdown(subjectList);
            }
        });
    }

    private void preloadStudentsFromRepository() {
        studentRepository.fetchStudents(new StudentRepository.OnStudentsLoadedListener() {
            @Override
            public void onSuccess(List<Student> students) {
                cachedAllStudents = students != null ? students : new ArrayList<>();
                isInitialStudentsLoaded = true;
                if (selectedSubject != null) {
                    filterAndDisplayStudentsForSubject(selectedSubject);
                }
            }

            @Override
            public void onError(String errorMessage) {
                cachedAllStudents = dbHelper.getAllStudents();
                isInitialStudentsLoaded = true;
                if (selectedSubject != null) {
                    filterAndDisplayStudentsForSubject(selectedSubject);
                }
            }
        });
    }

    private void populateSubjectDropdown(List<Subject> subjects) {
        List<String> subjectNames = new ArrayList<>();
        if (subjects != null && !subjects.isEmpty()) {
            for (Subject s : subjects) {
                subjectNames.add(s.getSubjectCode() + " - " + s.getSubjectName());
            }
        } else {
            subjectNames.add(isTeacherUser ? "No assigned subjects available" : "No subjects available");
        }
        ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, subjectNames);
        actvSubject.setAdapter(subjectAdapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSaveMarks.setOnClickListener(v -> saveMarks());
        btnDeleteMarks.setOnClickListener(v -> confirmDeleteMarks());

        // When Teacher selects a subject, trigger student loading and filtering immediately
        actvSubject.setOnItemClickListener((parent, view, position, id) -> {
            String selectedSub = (String) parent.getItemAtPosition(position);
            Subject matched = null;
            for (Subject s : subjectList) {
                String fullLabel = s.getSubjectCode() + " - " + s.getSubjectName();
                if (fullLabel.equals(selectedSub) ||
                        (s.getSubjectCode() != null && s.getSubjectCode().equalsIgnoreCase(selectedSub)) ||
                        (s.getSubjectName() != null && s.getSubjectName().equalsIgnoreCase(selectedSub))) {
                    matched = s;
                    break;
                }
            }

            if (matched != null) {
                if (isTeacherUser && !teacherAssignedSubjectIds.isEmpty() &&
                        !teacherAssignedSubjectIds.contains(matched.getSubjectId()) &&
                        !teacherAssignedSubjectIds.contains(matched.getSubjectCode())) {
                    Toast.makeText(this, "You are not authorized to enter marks for this subject.", Toast.LENGTH_LONG).show();
                    actvSubject.setText("", false);
                    selectedSubject = null;
                    return;
                }
                onSubjectSelected(matched);
            }
        });

        actvStudent.setOnItemClickListener((parent, view, position, id) -> {
            String selectedName = (String) parent.getItemAtPosition(position);
            for (Student s : currentSubjectStudents) {
                String formatted = s.getName() + " (" + s.getRegNo() + ")";
                if (formatted.equals(selectedName)) {
                    onStudentSelected(s);
                    break;
                }
            }
        });

        actvDepartment.setOnItemClickListener((parent, view, position, id) -> {
            if (selectedSubject != null) {
                filterAndDisplayStudentsForSubject(selectedSubject);
            }
        });

        actvSemester.setOnItemClickListener((parent, view, position, id) -> {
            if (selectedSubject != null) {
                filterAndDisplayStudentsForSubject(selectedSubject);
            }
        });
    }

    /**
     * Handles subject selection: sets department/semester scope and fetches students.
     */
    private void onSubjectSelected(@NonNull Subject subject) {
        selectedSubject = subject;

        // Automatically synchronize Department and Semester dropdowns with subject's values
        if (subject.getDepartment() != null && !subject.getDepartment().trim().isEmpty()) {
            actvDepartment.setText(subject.getDepartment(), false);
        }
        if (subject.getSemester() != null && !subject.getSemester().trim().isEmpty()) {
            actvSemester.setText(subject.getSemester(), false);
        }

        // Reset student selection & input fields
        selectedStudent = null;
        actvStudent.setText("", false);
        resetMarksInputFields();

        // Show loading state
        showStudentsLoading(true);

        // Fetch students via StudentRepository (Firestore primary, SQLite fallback)
        studentRepository.fetchStudents(new StudentRepository.OnStudentsLoadedListener() {
            @Override
            public void onSuccess(List<Student> students) {
                cachedAllStudents = students != null ? students : new ArrayList<>();
                isInitialStudentsLoaded = true;
                filterAndDisplayStudentsForSubject(subject);
            }

            @Override
            public void onError(String errorMessage) {
                cachedAllStudents = dbHelper.getAllStudents();
                isInitialStudentsLoaded = true;
                filterAndDisplayStudentsForSubject(subject);
            }
        });
    }

    /**
     * Filters students using dynamic Program Level (UG/PG), Department, Semester, and Status.
     */
    private void filterAndDisplayStudentsForSubject(@NonNull Subject subject) {
        showStudentsLoading(false);

        String subjectProg = subject.getProgramLevel();
        if (subjectProg == null || subjectProg.trim().isEmpty()) {
            subjectProg = Department.resolveDefaultProgramLevel(subject.getDepartmentId(), subject.getDepartment());
        }

        String subjectDept = subject.getDepartment();
        String subjectDeptId = subject.getDepartmentId();
        String subjectDeptShort = subject.getDepartmentShortName();
        String subjectSem = subject.getSemester();

        List<Student> eligibleStudents = new ArrayList<>();

        for (Student s : cachedAllStudents) {
            if (s == null) continue;
            if ("INACTIVE".equalsIgnoreCase(s.getStatus())) continue;

            // 1. Program Level Check (UG vs PG dynamically)
            String studentProg = s.getProgramLevel();
            if (studentProg == null || studentProg.trim().isEmpty()) {
                studentProg = Department.resolveDefaultProgramLevel(s.getDepartmentId(), s.getDepartment());
            }
            if (subjectProg != null && studentProg != null) {
                if (!subjectProg.trim().equalsIgnoreCase(studentProg.trim())) {
                    continue; // Never mix UG and PG
                }
            }

            // 2. Department Check
            boolean deptMatch = false;
            // Check departmentId first
            if (subjectDeptId != null && !subjectDeptId.trim().isEmpty() &&
                    s.getDepartmentId() != null && !s.getDepartmentId().trim().isEmpty()) {
                if (subjectDeptId.trim().equalsIgnoreCase(s.getDepartmentId().trim())) {
                    deptMatch = true;
                }
            }
            // Check departmentShortName
            if (!deptMatch && subjectDeptShort != null && !subjectDeptShort.trim().isEmpty() &&
                    s.getDepartmentShortName() != null && !s.getDepartmentShortName().trim().isEmpty()) {
                if (subjectDeptShort.trim().equalsIgnoreCase(s.getDepartmentShortName().trim())) {
                    deptMatch = true;
                }
            }
            // Fallback to Department helper and string matching
            if (!deptMatch) {
                deptMatch = Subject.isDepartmentMatching(subjectDept, s.getDepartment());
            }
            if (!deptMatch) continue;

            // 3. Semester Check (handles 'Semester 3', 'Semester III', '3')
            boolean semMatch = Subject.isSemesterMatching(subjectSem, s.getSemester());
            if (!semMatch) continue;

            eligibleStudents.add(s);
        }

        currentSubjectStudents = eligibleStudents;

        if (eligibleStudents.isEmpty()) {
            tvStudentEmptyStatus.setText("No students found for this subject (" + subject.getSubjectCode() + ").");
            tvStudentEmptyStatus.setVisibility(View.VISIBLE);
            rvStudentsList.setVisibility(View.GONE);
            populateStudentDropdown(new ArrayList<>());
        } else {
            tvStudentEmptyStatus.setVisibility(View.GONE);
            rvStudentsList.setVisibility(View.VISIBLE);
            studentSelectionAdapter.updateData(eligibleStudents);
            populateStudentDropdown(eligibleStudents);
        }
    }

    private void showStudentsLoading(boolean isLoading) {
        if (isLoading) {
            layoutStudentLoading.setVisibility(View.VISIBLE);
            tvStudentLoadingStatus.setText("Loading students...");
            tvStudentEmptyStatus.setVisibility(View.GONE);
            rvStudentsList.setVisibility(View.GONE);
        } else {
            layoutStudentLoading.setVisibility(View.GONE);
        }
    }

    private void populateStudentDropdown(List<Student> students) {
        List<String> names = new ArrayList<>();
        for (Student s : students) {
            names.add(s.getName() + " (" + s.getRegNo() + ")");
        }
        ArrayAdapter<String> studentAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names);
        actvStudent.setAdapter(studentAdapter);
    }

    /**
     * Handles student selection, updates selection state, and checks for existing marks.
     */
    private void onStudentSelected(@NonNull Student student) {
        selectedStudent = student;
        actvStudent.setText(student.getName() + " (" + student.getRegNo() + ")", false);
        studentSelectionAdapter.notifyDataSetChanged();
        checkExistingMarkRecord();
    }

    private void checkExistingMarkRecord() {
        if (selectedStudent == null || selectedSubject == null) return;

        // Check SQLite first
        Cursor cursor = dbHelper.getMarkByStudentAndSubject(selectedStudent.getId(), selectedSubject.getId());
        if (cursor != null && cursor.moveToFirst()) {
            isExistingRecord = true;
            etInternalMarks.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndexOrThrow("internal1"))));
            etAssignmentMarks.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndexOrThrow("assignment"))));
            etModelExamMarks.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndexOrThrow("model_exam"))));
            etLabMarks.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndexOrThrow("lab"))));
            etUniversityExamMarks.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndexOrThrow("university_exam"))));

            btnSaveMarks.setText("Update Subject Marks");
            btnDeleteMarks.setVisibility(View.VISIBLE);
            calculateTotalAndGrade();
            cursor.close();
            return;
        }
        if (cursor != null) cursor.close();

        // Check Firestore marks collection
        String docId = selectedStudent.getStudentId() + "_" + selectedSubject.getSubjectId();
        FirebaseFirestore.getInstance().collection("marks").document(docId).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        isExistingRecord = true;
                        Double internal = doc.getDouble("internal1");
                        Double assignment = doc.getDouble("assignment");
                        Double modelExam = doc.getDouble("model_exam");
                        Double lab = doc.getDouble("lab");
                        Double university = doc.getDouble("university_exam");

                        etInternalMarks.setText(String.valueOf(internal != null ? internal : 0.0));
                        etAssignmentMarks.setText(String.valueOf(assignment != null ? assignment : 0.0));
                        etModelExamMarks.setText(String.valueOf(modelExam != null ? modelExam : 0.0));
                        etLabMarks.setText(String.valueOf(lab != null ? lab : 0.0));
                        etUniversityExamMarks.setText(String.valueOf(university != null ? university : 0.0));

                        btnSaveMarks.setText("Update Subject Marks");
                        btnDeleteMarks.setVisibility(View.VISIBLE);
                        calculateTotalAndGrade();
                    } else {
                        isExistingRecord = false;
                        btnSaveMarks.setText("Save Subject Marks");
                        btnDeleteMarks.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    isExistingRecord = false;
                    btnSaveMarks.setText("Save Subject Marks");
                    btnDeleteMarks.setVisibility(View.GONE);
                });
    }

    private void resetMarksInputFields() {
        etInternalMarks.setText("");
        etAssignmentMarks.setText("");
        etModelExamMarks.setText("");
        etLabMarks.setText("");
        etUniversityExamMarks.setText("");

        tvTotalMarks.setText("0.0");
        tvPercentage.setText("0.0%");
        tvGrade.setText("F");
        tvGradePoint.setText("0");

        btnSaveMarks.setText("Save Subject Marks");
        btnDeleteMarks.setVisibility(View.GONE);
        isExistingRecord = false;
    }

    private void setupRealtimeCalculation() {
        TextWatcher markWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateTotalAndGrade();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etInternalMarks.addTextChangedListener(markWatcher);
        etAssignmentMarks.addTextChangedListener(markWatcher);
        etModelExamMarks.addTextChangedListener(markWatcher);
        etLabMarks.addTextChangedListener(markWatcher);
        etUniversityExamMarks.addTextChangedListener(markWatcher);
    }

    private void calculateTotalAndGrade() {
        double internal = parseDouble(etInternalMarks.getText() != null ? etInternalMarks.getText().toString() : "");
        double assignment = parseDouble(etAssignmentMarks.getText() != null ? etAssignmentMarks.getText().toString() : "");
        double modelExam = parseDouble(etModelExamMarks.getText() != null ? etModelExamMarks.getText().toString() : "");
        double lab = parseDouble(etLabMarks.getText() != null ? etLabMarks.getText().toString() : "");
        double university = parseDouble(etUniversityExamMarks.getText() != null ? etUniversityExamMarks.getText().toString() : "");

        double total = internal + assignment + modelExam + lab + university;
        double percentage = Math.min(100.0, total);

        String grade;
        int gradePoint;

        if (percentage >= 90) {
            grade = "A+";
            gradePoint = 10;
        } else if (percentage >= 80) {
            grade = "A";
            gradePoint = 9;
        } else if (percentage >= 70) {
            grade = "B+";
            gradePoint = 8;
        } else if (percentage >= 60) {
            grade = "B";
            gradePoint = 7;
        } else if (percentage >= 50) {
            grade = "C";
            gradePoint = 6;
        } else if (percentage >= 40) {
            grade = "D";
            gradePoint = 5;
        } else {
            grade = "F";
            gradePoint = 0;
        }

        tvTotalMarks.setText(String.format(Locale.US, "%.1f", total));
        tvPercentage.setText(String.format(Locale.US, "%.1f%%", percentage));
        tvGrade.setText(grade);
        tvGradePoint.setText(String.valueOf(gradePoint));
    }

    private double parseDouble(String str) {
        if (str == null || str.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(str.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private void saveMarks() {
        if (selectedSubject == null) {
            Toast.makeText(this, "Please select an assigned subject first.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedStudent == null) {
            Toast.makeText(this, "Please select a student.", Toast.LENGTH_SHORT).show();
            return;
        }

        double internal = parseDouble(etInternalMarks.getText() != null ? etInternalMarks.getText().toString() : "");
        double assignment = parseDouble(etAssignmentMarks.getText() != null ? etAssignmentMarks.getText().toString() : "");
        double modelExam = parseDouble(etModelExamMarks.getText() != null ? etModelExamMarks.getText().toString() : "");
        double lab = parseDouble(etLabMarks.getText() != null ? etLabMarks.getText().toString() : "");
        double university = parseDouble(etUniversityExamMarks.getText() != null ? etUniversityExamMarks.getText().toString() : "");

        double total = internal + assignment + modelExam + lab + university;
        double percentage = Math.min(100.0, total);
        String grade = tvGrade.getText().toString();
        double gradePoint = parseDouble(tvGradePoint.getText().toString());

        // 1. Save to local SQLite
        boolean success = dbHelper.saveOrUpdateMark(selectedStudent.getId(), selectedSubject.getId(),
                internal, assignment, modelExam, lab, university, total, percentage, grade, gradePoint);

        // 2. Save directly to Firestore marks collection
        String docId = selectedStudent.getStudentId() + "_" + selectedSubject.getSubjectId();
        Map<String, Object> data = new HashMap<>();
        data.put("student_id", selectedStudent.getId());
        data.put("subject_id", selectedSubject.getId());
        data.put("studentId", selectedStudent.getStudentId());
        data.put("subjectId", selectedSubject.getSubjectId());
        data.put("studentName", selectedStudent.getName());
        data.put("registerNo", selectedStudent.getRegNo());
        data.put("subjectCode", selectedSubject.getSubjectCode());
        data.put("subjectName", selectedSubject.getSubjectName());
        data.put("department", selectedSubject.getDepartment());
        data.put("departmentId", selectedSubject.getDepartmentId() != null ? selectedSubject.getDepartmentId() : "");
        data.put("departmentShortName", selectedSubject.getDepartmentShortName() != null ? selectedSubject.getDepartmentShortName() : "");
        data.put("programLevel", selectedSubject.getProgramLevel());
        data.put("semester", selectedSubject.getSemester());
        data.put("internal1", internal);
        data.put("assignment", assignment);
        data.put("model_exam", modelExam);
        data.put("lab", lab);
        data.put("university_exam", university);
        data.put("total_marks", total);
        data.put("percentage", percentage);
        data.put("grade", grade);
        data.put("grade_point", gradePoint);
        data.put("updatedAt", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance().collection("marks")
                .document(docId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    try {
                        FirestoreHelper.getInstance().syncMark(selectedStudent.getId(), selectedSubject.getId(),
                                internal, assignment, modelExam, lab, university, total, percentage, grade, gradePoint);
                    } catch (Exception ignored) {}
                });

        if (success) {
            Toast.makeText(this, "Marks saved successfully for " + selectedStudent.getName(), Toast.LENGTH_SHORT).show();
            dbHelper.logTeacherActivity("Teacher", "Marks Recorded",
                    "Recorded marks for " + selectedStudent.getName() + " in " + selectedSubject.getSubjectName(), "MARKS");
            PortalActivityLogger.getInstance(this).logTeacherActivity(
                    sessionManager.getUserName(),
                    "Marks Recorded",
                    "Recorded marks for " + selectedStudent.getName() + " in " + selectedSubject.getSubjectName(),
                    "Marks"
            );
            isExistingRecord = true;
            btnSaveMarks.setText("Update Subject Marks");
            btnDeleteMarks.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(this, "Marks saved to cloud.", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDeleteMarks() {
        if (selectedStudent == null || selectedSubject == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Delete Entered Marks?")
                .setMessage("Are you sure you want to delete marks for " + selectedStudent.getName() + " in " + selectedSubject.getSubjectName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    boolean deleted = dbHelper.deleteMark(selectedStudent.getId(), selectedSubject.getId());
                    String docId = selectedStudent.getStudentId() + "_" + selectedSubject.getSubjectId();
                    FirebaseFirestore.getInstance().collection("marks").document(docId).delete();
                    try {
                        FirestoreHelper.getInstance().deleteMark(selectedStudent.getId(), selectedSubject.getId());
                    } catch (Exception ignored) {}

                    Toast.makeText(this, "Marks record deleted", Toast.LENGTH_SHORT).show();
                    resetMarksInputFields();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Inner RecyclerView Adapter displaying students eligible for the selected subject.
     */
    private class StudentMarksSelectionAdapter extends RecyclerView.Adapter<StudentMarksSelectionAdapter.ViewHolder> {

        private List<Student> students;

        StudentMarksSelectionAdapter(List<Student> students) {
            this.students = students != null ? students : new ArrayList<>();
        }

        public void updateData(List<Student> newStudents) {
            this.students = newStudents != null ? newStudents : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_select, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Student s = students.get(position);
            holder.tvName.setText(s.getName());
            String subInfo = s.getRegNo() + " • " + s.getDepartment() + " • " + s.getSemester();
            holder.tvSub.setText(subInfo);

            boolean isSelected = selectedStudent != null && (
                    (selectedStudent.getStudentId() != null && selectedStudent.getStudentId().equals(s.getStudentId())) ||
                            (selectedStudent.getRegNo() != null && !selectedStudent.getRegNo().isEmpty() &&
                                    selectedStudent.getRegNo().equalsIgnoreCase(s.getRegNo()))
            );

            int strokeColor = androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), isSelected ? R.color.primary : R.color.border_color);
            holder.cardView.setStrokeColor(strokeColor);
            holder.cardView.setStrokeWidth(isSelected ? 4 : 1);
            holder.ivStatus.setVisibility(isSelected ? View.VISIBLE : View.GONE);

            holder.itemView.setOnClickListener(v -> onStudentSelected(s));
        }

        @Override
        public int getItemCount() {
            return students.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView cardView;
            TextView tvName, tvSub;
            ImageView ivStatus;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                cardView = (MaterialCardView) itemView;
                tvName = itemView.findViewById(R.id.tvStudentSelectName);
                tvSub = itemView.findViewById(R.id.tvStudentSelectSub);
                ivStatus = itemView.findViewById(R.id.ivSelectStatus);
            }
        }
    }
}
