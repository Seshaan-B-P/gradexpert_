package com.example;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adapter.TeacherSubjectAssignmentAdapter;
import com.example.database.DatabaseHelper;
import com.example.model.Subject;
import com.example.model.Teacher;
import com.example.model.TeacherAssignment;
import com.example.utils.PortalActivityLogger;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for assigning Faculty Teachers to Academic Subjects in GradeXpert.
 */
public class TeacherSubjectAssignmentActivity extends AppCompatActivity implements TeacherSubjectAssignmentAdapter.OnAssignmentRemoveListener {

    private FirebaseFirestore db;
    private DatabaseHelper dbHelper;

    private Toolbar toolbar;
    private AutoCompleteTextView spTeacher;
    private AutoCompleteTextView spSubject;
    private AutoCompleteTextView spDept;
    private AutoCompleteTextView spSem;
    private MaterialButton btnSubmit;
    private ProgressBar progressBar;
    private RecyclerView rvAssignments;

    private TeacherSubjectAssignmentAdapter adapter;
    private List<TeacherAssignment> assignmentList = new ArrayList<>();

    private List<Teacher> teacherList = new ArrayList<>();
    private List<Subject> subjectList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_subject_assignment);

        db = FirebaseFirestore.getInstance();
        dbHelper = new DatabaseHelper(this);

        initViews();
        setupToolbar();
        setupWindowInsets();
        setupSpinners();
        setupRecyclerView();
        loadTeachersAndSubjects();
        loadAssignments();
    }

    private void setupWindowInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbarTeacherAssignment), (v, insets) -> {
            androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
            v.setPadding(0, statusBarInsets.top, 0, 0);
            return insets;
        });
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarTeacherAssignment);
        spTeacher = findViewById(R.id.spAssignTeacher);
        spSubject = findViewById(R.id.spAssignSubject);
        spDept = findViewById(R.id.spAssignDept);
        spSem = findViewById(R.id.spAssignSem);
        btnSubmit = findViewById(R.id.btnAssignTeacherSubjectSubmit);
        progressBar = findViewById(R.id.progressBarTeacherAssignment);
        rvAssignments = findViewById(R.id.rvTeacherAssignmentsList);

        btnSubmit.setOnClickListener(v -> submitAssignment());
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSpinners() {
        String[] depts = new String[]{"MCA", "CSE", "IT", "ECE"};
        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, depts);
        spDept.setAdapter(deptAdapter);

        String[] sems = new String[]{"Semester I", "Semester II", "Semester III", "Semester IV", "Semester V", "Semester VI"};
        ArrayAdapter<String> semAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, sems);
        spSem.setAdapter(semAdapter);
    }

    private void setupRecyclerView() {
        rvAssignments.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TeacherSubjectAssignmentAdapter(assignmentList, this);
        rvAssignments.setAdapter(adapter);
    }

    private void loadTeachersAndSubjects() {
        db.collection("teachers").get().addOnSuccessListener(queryDocumentSnapshots -> {
            teacherList.clear();
            List<String> teacherNames = new ArrayList<>();
            if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    Teacher t = doc.toObject(Teacher.class);
                    if (t != null) {
                        t.setUid(doc.getId());
                        teacherList.add(t);
                        String empId = t.getEmployeeId() != null && !t.getEmployeeId().isEmpty() ? " [" + t.getEmployeeId() + "]" : "";
                        teacherNames.add(t.getName() + empId + " (" + t.getEmail() + ")");
                    }
                }
            }

            if (teacherNames.isEmpty()) {
                teacherList = dbHelper.getAllTeachers();
                for (Teacher t : teacherList) {
                    teacherNames.add(t.getName() + " (" + t.getEmail() + ")");
                }
            }

            ArrayAdapter<String> tAdapter = new ArrayAdapter<>(TeacherSubjectAssignmentActivity.this, android.R.layout.simple_dropdown_item_1line, teacherNames);
            spTeacher.setAdapter(tAdapter);
        }).addOnFailureListener(e -> {
            teacherList = dbHelper.getAllTeachers();
            List<String> teacherNames = new ArrayList<>();
            for (Teacher t : teacherList) {
                teacherNames.add(t.getName() + " (" + t.getEmail() + ")");
            }
            ArrayAdapter<String> tAdapter = new ArrayAdapter<>(TeacherSubjectAssignmentActivity.this, android.R.layout.simple_dropdown_item_1line, teacherNames);
            spTeacher.setAdapter(tAdapter);
        });

        subjectList = dbHelper.getAllSubjects();
        List<String> subjectNames = new ArrayList<>();
        for (Subject s : subjectList) {
            subjectNames.add(s.getSubjectName() + " (" + s.getSubjectCode() + ")");
        }
        ArrayAdapter<String> sAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, subjectNames);
        spSubject.setAdapter(sAdapter);
    }

    private void loadAssignments() {
        showLoading(true);
        db.collection("teacher_assignments").get().addOnSuccessListener(queryDocumentSnapshots -> {
            showLoading(false);
            assignmentList.clear();
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                TeacherAssignment ta = doc.toObject(TeacherAssignment.class);
                if (ta != null) {
                    ta.setAssignmentId(doc.getId());
                    assignmentList.add(ta);
                }
            }
            adapter.updateData(assignmentList);
        }).addOnFailureListener(e -> {
            showLoading(false);
            adapter.updateData(assignmentList);
        });
    }

    private void submitAssignment() {
        String teacherStr = spTeacher.getText().toString().trim();
        String subjectStr = spSubject.getText().toString().trim();
        String deptStr = spDept.getText().toString().trim();
        String semStr = spSem.getText().toString().trim();

        if (TextUtils.isEmpty(teacherStr)) {
            spTeacher.setError("Select Teacher");
            return;
        }
        if (TextUtils.isEmpty(subjectStr)) {
            spSubject.setError("Select Subject");
            return;
        }

        String parsedTeacherName = teacherStr;
        String parsedTeacherEmail = "";
        if (teacherStr.contains("(") && teacherStr.contains(")")) {
            parsedTeacherName = teacherStr.substring(0, teacherStr.indexOf("(")).trim();
            parsedTeacherEmail = teacherStr.substring(teacherStr.indexOf("(") + 1, teacherStr.indexOf(")")).trim();
        }
        final String teacherName = parsedTeacherName;
        final String teacherEmail = parsedTeacherEmail;

        String parsedSubjectName = subjectStr;
        String parsedSubjectCode = "SUB";
        if (subjectStr.contains("(") && subjectStr.contains(")")) {
            parsedSubjectName = subjectStr.substring(0, subjectStr.indexOf("(")).trim();
            parsedSubjectCode = subjectStr.substring(subjectStr.indexOf("(") + 1, subjectStr.indexOf(")")).trim();
        }
        final String subjectName = parsedSubjectName;
        final String subjectCode = parsedSubjectCode;

        String id = (teacherName + "_" + subjectCode).replaceAll("\\s+", "_").toLowerCase();
        TeacherAssignment ta = new TeacherAssignment(id, teacherName, teacherEmail, subjectCode, subjectName, deptStr.isEmpty() ? "MCA" : deptStr, semStr.isEmpty() ? "Semester III" : semStr, "2026-27", "ACTIVE");

        showLoading(true);
        db.collection("teacher_assignments").document(id).set(ta)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    // Synchronize teacher profile in teachers collection
                    syncTeacherProfileAssignment(teacherEmail, subjectCode, subjectName, true);

                    PortalActivityLogger.getInstance(this).logTeacherActivity(
                            "System Admin",
                            "Assigned Teacher to Subject",
                            teacherName + " assigned to " + subjectName + " (" + subjectCode + ")",
                            "Faculty Assignment"
                    );
                    Toast.makeText(this, "Teacher assigned successfully!", Toast.LENGTH_SHORT).show();
                    spTeacher.setText("");
                    spSubject.setText("");
                    loadAssignments();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed to create assignment", Toast.LENGTH_SHORT).show();
                });
    }

    private void syncTeacherProfileAssignment(String email, String subjectCode, String subjectName, boolean isAdd) {
        if (email == null || email.isEmpty()) return;
        db.collection("teachers")
                .whereEqualTo("email", email.trim().toLowerCase())
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            List<String> subIds = (List<String>) doc.get("assignedSubjectIds");
                            List<String> subNames = (List<String>) doc.get("assignedSubjectNames");
                            if (subIds == null) subIds = new ArrayList<>();
                            else subIds = new ArrayList<>(subIds);
                            if (subNames == null) subNames = new ArrayList<>();
                            else subNames = new ArrayList<>(subNames);

                            if (isAdd) {
                                if (!subIds.contains(subjectCode)) {
                                    subIds.add(subjectCode);
                                    subNames.add(subjectName);
                                }
                            } else {
                                int idx = subIds.indexOf(subjectCode);
                                if (idx >= 0) {
                                    subIds.remove(idx);
                                    if (idx < subNames.size()) subNames.remove(idx);
                                }
                            }

                            java.util.Map<String, Object> updates = new java.util.HashMap<>();
                            updates.put("assignedSubjectIds", subIds);
                            updates.put("assignedSubjectNames", subNames);
                            updates.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
                            db.collection("teachers").document(doc.getId()).update(updates);
                        }
                    }
                });
    }

    @Override
    public void onRemoveClick(TeacherAssignment assignment) {
        if (assignment == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Remove Assignment")
                .setMessage("Are you sure you want to remove assignment for " + assignment.getTeacherName() + "?")
                .setPositiveButton("REMOVE", (dialog, which) -> {
                    showLoading(true);
                    db.collection("teacher_assignments").document(assignment.getAssignmentId()).delete()
                            .addOnSuccessListener(aVoid -> {
                                showLoading(false);
                                syncTeacherProfileAssignment(assignment.getTeacherEmail(), assignment.getSubjectCode(), assignment.getSubjectName(), false);
                                Toast.makeText(this, "Assignment removed", Toast.LENGTH_SHORT).show();
                                loadAssignments();
                            })
                            .addOnFailureListener(e -> {
                                showLoading(false);
                                Toast.makeText(this, "Failed to remove assignment", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
