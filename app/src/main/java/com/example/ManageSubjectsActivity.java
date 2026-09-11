package com.example;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.adapter.SubjectAdapter;
import com.example.database.DatabaseHelper;
import com.example.model.Department;
import com.example.model.Subject;
import com.example.model.Teacher;
import com.example.model.TeacherAssignment;
import com.example.repository.SubjectRepository;
import com.example.utils.PortalActivityLogger;
import com.example.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity for managing curriculum subjects (Admin mode) or faculty teaching load (Teacher mode).
 * Supports Program Level ("UG" / "PG") filters, Department, and Semester classification dynamically.
 * In Teacher Mode:
 *  - Displays ONLY the teacher's currently assigned subjects.
 *  - Supports adding eligible subjects matching teacher's Program Level and Department.
 *  - Supports safely unassigning a subject WITHOUT deleting master subjects, marks, attendance, or results.
 */
public class ManageSubjectsActivity extends AppCompatActivity implements SubjectAdapter.OnSubjectClickListener {

    private static final String TAG = "ManageSubjectsActivity";

    private FirebaseFirestore db;
    private SubjectRepository repository;
    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;

    private TextView tvTotalCount;
    private TextView tvTotalLabel;
    private TextView tvActiveCount;
    private TextView tvInactiveCount;

    private TextInputEditText etSearch;
    private ChipGroup chipGroupProgramLevel;
    private Spinner spDept;
    private Spinner spSem;
    private ChipGroup chipGroupStatus;

    private ProgressBar progressBar;
    private LinearLayout layoutEmpty;
    private TextView tvEmptyMessage;
    private MaterialButton btnClearFilters;

    private RecyclerView rvSubjects;
    private FloatingActionButton fabAddSubject;

    private SubjectAdapter adapter;
    private List<Subject> loadedSubjects = new ArrayList<>();
    private List<Subject> allCurriculumSubjects = new ArrayList<>();

    private final List<Department> allDepartments = new ArrayList<>();
    private final List<String> deptFilterOptions = new ArrayList<>();
    private ArrayAdapter<String> deptFilterAdapter;

    private final List<String> semFilterOptions = new ArrayList<>();
    private ArrayAdapter<String> semFilterAdapter;

    private String selectedProgramLevel = "ALL";
    private String selectedDept = "All Departments";
    private String selectedSem = "All Semesters";
    private String selectedStatus = "ACTIVE";

    // Teacher mode state
    private boolean isTeacherMode = false;
    private Teacher currentTeacher = null;
    private String teacherUid = "";
    private String teacherEmail = "";
    private final List<String> currentAssignedSubjectIds = new ArrayList<>();
    private final List<String> currentAssignedSubjectNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_subjects);

        db = FirebaseFirestore.getInstance();
        repository = SubjectRepository.getInstance(this);
        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        isTeacherMode = "TEACHER".equalsIgnoreCase(sessionManager.getUserRole());

        initViews();
        setupToolbar();
        setupWindowInsets();
        setupSwipeRefresh();
        setupSearch();
        setupProgramLevelChips();
        setupDepartmentSpinner();
        setupSemesterSpinner();
        setupStatusChips();
        setupRecyclerView();
        setupFab();

        loadDepartments();
        loadSubjectData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSubjectData();
    }

    private void setupWindowInsets() {
        View appBar = findViewById(R.id.appBarSubjects);
        if (appBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, insets) -> {
                androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                v.setPadding(0, statusBarInsets.top, 0, 0);
                return insets;
            });
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarSubjects);
        swipeRefresh = findViewById(R.id.swipeRefreshSubjects);

        tvTotalCount = findViewById(R.id.tvSummarySubjectTotalCount);
        tvTotalLabel = findViewById(R.id.tvSummarySubjectTotalLabel);
        tvActiveCount = findViewById(R.id.tvSummarySubjectActiveCount);
        tvInactiveCount = findViewById(R.id.tvSummarySubjectInactiveCount);

        etSearch = findViewById(R.id.etSearchSubject);
        chipGroupProgramLevel = findViewById(R.id.chipGroupSubjectProgramLevel);
        spDept = findViewById(R.id.spFilterDepartment);
        spSem = findViewById(R.id.spFilterSemester);
        chipGroupStatus = findViewById(R.id.chipGroupSubjectStatus);

        progressBar = findViewById(R.id.progressBarManageSubjects);
        layoutEmpty = findViewById(R.id.layoutSubjectEmptyState);
        tvEmptyMessage = findViewById(R.id.tvSubjectEmptyMessage);
        btnClearFilters = findViewById(R.id.btnClearSubjectFilters);

        rvSubjects = findViewById(R.id.rvSubjectsList);
        fabAddSubject = findViewById(R.id.fabAddSubject);

        btnClearFilters.setOnClickListener(v -> {
            if (isTeacherMode && loadedSubjects.isEmpty()) {
                showAssignSubjectPicker();
            } else {
                resetFilters();
            }
        });
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            if (isTeacherMode) {
                getSupportActionBar().setTitle("My Assigned Subjects");
                getSupportActionBar().setSubtitle("Teaching Load & Courses");
                if (tvTotalLabel != null) {
                    tvTotalLabel.setText("Assigned");
                }
            } else {
                getSupportActionBar().setTitle("Manage Subjects");
                getSupportActionBar().setSubtitle("Curriculum & Course Directory");
                if (tvTotalLabel != null) {
                    tvTotalLabel.setText("Total Subjects");
                }
            }
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(this::loadSubjectData);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupProgramLevelChips() {
        if (chipGroupProgramLevel != null) {
            chipGroupProgramLevel.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.chipSubjectLevelUG) {
                    selectedProgramLevel = "UG";
                } else if (checkedId == R.id.chipSubjectLevelPG) {
                    selectedProgramLevel = "PG";
                } else {
                    selectedProgramLevel = "ALL";
                }
                updateDepartmentFilterOptions();
                updateSemesterFilterOptions();
                applyFilters();
            });
        }
    }

    private void setupDepartmentSpinner() {
        deptFilterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, deptFilterOptions);
        deptFilterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDept.setAdapter(deptFilterAdapter);

        spDept.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < deptFilterOptions.size()) {
                    selectedDept = deptFilterOptions.get(position);
                    applyFilters();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSemesterSpinner() {
        semFilterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, semFilterOptions);
        semFilterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSem.setAdapter(semFilterAdapter);

        spSem.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < semFilterOptions.size()) {
                    selectedSem = semFilterOptions.get(position);
                    applyFilters();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadDepartments() {
        db.collection("departments").get().addOnSuccessListener(querySnapshot -> {
            allDepartments.clear();
            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    Department d = doc.toObject(Department.class);
                    if (d != null) {
                        d.setDepartmentId(doc.getId());
                        if (doc.getString("programLevel") != null) d.setProgramLevel(doc.getString("programLevel"));
                        if (doc.getString("shortName") != null) d.setShortName(doc.getString("shortName"));
                        if (doc.getString("name") != null) d.setName(doc.getString("name"));
                        allDepartments.add(d);
                    }
                }
            }
            if (allDepartments.isEmpty()) {
                allDepartments.add(new Department("bca", "Bachelor of Computer Applications", "BCA", "UG", "ACTIVE"));
                allDepartments.add(new Department("cse", "Computer Science & Engineering", "B.Tech CSE", "UG", "ACTIVE"));
                allDepartments.add(new Department("mca", "Master of Computer Applications", "MCA", "PG", "ACTIVE"));
            }
            updateDepartmentFilterOptions();
            updateSemesterFilterOptions();
        }).addOnFailureListener(e -> {
            allDepartments.add(new Department("bca", "Bachelor of Computer Applications", "BCA", "UG", "ACTIVE"));
            allDepartments.add(new Department("cse", "Computer Science & Engineering", "B.Tech CSE", "UG", "ACTIVE"));
            allDepartments.add(new Department("mca", "Master of Computer Applications", "MCA", "PG", "ACTIVE"));
            updateDepartmentFilterOptions();
            updateSemesterFilterOptions();
        });
    }

    private void updateDepartmentFilterOptions() {
        deptFilterOptions.clear();
        deptFilterOptions.add("All Departments");

        for (Department d : allDepartments) {
            String lvl = d.getProgramLevel() != null ? d.getProgramLevel().toUpperCase() : "UG";
            if ("ALL".equalsIgnoreCase(selectedProgramLevel) || selectedProgramLevel.equalsIgnoreCase(lvl)) {
                String name = d.getDepartmentName();
                if (!name.isEmpty() && !deptFilterOptions.contains(name)) {
                    deptFilterOptions.add(name);
                }
            }
        }
        deptFilterAdapter.notifyDataSetChanged();
        spDept.setSelection(0);
        selectedDept = "All Departments";
    }

    private void updateSemesterFilterOptions() {
        semFilterOptions.clear();
        semFilterOptions.add("All Semesters");
        if ("PG".equalsIgnoreCase(selectedProgramLevel)) {
            semFilterOptions.add("Semester I");
            semFilterOptions.add("Semester II");
            semFilterOptions.add("Semester III");
            semFilterOptions.add("Semester IV");
        } else {
            semFilterOptions.add("Semester I");
            semFilterOptions.add("Semester II");
            semFilterOptions.add("Semester III");
            semFilterOptions.add("Semester IV");
            semFilterOptions.add("Semester V");
            semFilterOptions.add("Semester VI");
            semFilterOptions.add("Semester VII");
            semFilterOptions.add("Semester VIII");
        }
        semFilterAdapter.notifyDataSetChanged();
        spSem.setSelection(0);
        selectedSem = "All Semesters";
    }

    private void setupStatusChips() {
        chipGroupStatus.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipFilterSubjectInactive) {
                selectedStatus = "INACTIVE";
            } else if (checkedId == R.id.chipFilterSubjectAll) {
                selectedStatus = "ALL";
            } else {
                selectedStatus = "ACTIVE";
            }
            applyFilters();
        });
    }

    private void setupRecyclerView() {
        rvSubjects.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SubjectAdapter(new ArrayList<>(), this);
        adapter.setTeacherMode(isTeacherMode);
        rvSubjects.setAdapter(adapter);
    }

    private void setupFab() {
        if (isTeacherMode) {
            fabAddSubject.setContentDescription("Assign Subject");
            fabAddSubject.setOnClickListener(v -> showAssignSubjectPicker());
        } else {
            fabAddSubject.setContentDescription("Add Subject");
            fabAddSubject.setOnClickListener(v -> {
                Intent intent = new Intent(ManageSubjectsActivity.this, AddSubjectActivity.class);
                startActivity(intent);
            });
        }
    }

    private void loadSubjectData() {
        showLoading(true);
        if (isTeacherMode) {
            loadTeacherAssignedSubjects();
        } else {
            loadAllCurriculumSubjects();
        }
    }

    private void loadAllCurriculumSubjects() {
        repository.fetchSubjects(new SubjectRepository.OnSubjectsLoadedListener() {
            @Override
            public void onSuccess(List<Subject> subjects) {
                showLoading(false);
                swipeRefresh.setRefreshing(false);
                loadedSubjects = subjects;
                allCurriculumSubjects = new ArrayList<>(subjects);
                updateSummaryCounts(subjects);
                adapter.setTeacherMode(false);
                adapter.updateData(subjects);
                applyFilters();
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(ManageSubjectsActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTeacherAssignedSubjects() {
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        teacherUid = fUser != null ? fUser.getUid() : "";
        teacherEmail = sessionManager.getUserEmail();

        currentAssignedSubjectIds.clear();
        currentAssignedSubjectNames.clear();

        if (!teacherUid.isEmpty()) {
            db.collection("teachers").document(teacherUid).get().addOnSuccessListener(doc -> {
                if (doc != null && doc.exists()) {
                    currentTeacher = doc.toObject(Teacher.class);
                    if (currentTeacher != null) {
                        currentTeacher.setUid(doc.getId());
                        extractTeacherAssignments(doc);
                    }
                    fetchLegacyTeacherAssignments();
                } else {
                    findTeacherByEmailFallback();
                }
            }).addOnFailureListener(e -> findTeacherByEmailFallback());
        } else {
            findTeacherByEmailFallback();
        }
    }

    private void findTeacherByEmailFallback() {
        if (teacherEmail != null && !teacherEmail.trim().isEmpty()) {
            db.collection("teachers").whereEqualTo("email", teacherEmail.trim().toLowerCase())
                    .get().addOnSuccessListener(snapshots -> {
                        if (snapshots != null && !snapshots.isEmpty()) {
                            DocumentSnapshot doc = snapshots.getDocuments().get(0);
                            currentTeacher = doc.toObject(Teacher.class);
                            if (currentTeacher != null) {
                                currentTeacher.setUid(doc.getId());
                                teacherUid = doc.getId();
                                extractTeacherAssignments(doc);
                            }
                        }
                        fetchLegacyTeacherAssignments();
                    }).addOnFailureListener(e -> fetchLegacyTeacherAssignments());
        } else {
            fetchLegacyTeacherAssignments();
        }
    }

    private void extractTeacherAssignments(DocumentSnapshot doc) {
        List<String> ids = (List<String>) doc.get("assignedSubjectIds");
        List<String> names = (List<String>) doc.get("assignedSubjectNames");
        if (ids != null) {
            for (String id : ids) {
                if (id != null && !currentAssignedSubjectIds.contains(id)) {
                    currentAssignedSubjectIds.add(id);
                }
            }
        }
        if (names != null) {
            currentAssignedSubjectNames.addAll(names);
        }
    }

    private void fetchLegacyTeacherAssignments() {
        if (teacherEmail != null && !teacherEmail.trim().isEmpty()) {
            db.collection("teacher_assignments")
                    .whereEqualTo("teacherEmail", teacherEmail.trim().toLowerCase())
                    .get().addOnSuccessListener(snapshots -> {
                        if (snapshots != null && !snapshots.isEmpty()) {
                            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                String code = doc.getString("subjectCode");
                                String name = doc.getString("subjectName");
                                if (code != null && !code.trim().isEmpty() && !currentAssignedSubjectIds.contains(code)) {
                                    currentAssignedSubjectIds.add(code);
                                    if (name != null && !currentAssignedSubjectNames.contains(name)) {
                                        currentAssignedSubjectNames.add(name);
                                    }
                                }
                            }
                        }
                        filterAssignedCurriculumSubjects();
                    }).addOnFailureListener(e -> filterAssignedCurriculumSubjects());
        } else {
            filterAssignedCurriculumSubjects();
        }
    }

    private void filterAssignedCurriculumSubjects() {
        repository.fetchSubjects(new SubjectRepository.OnSubjectsLoadedListener() {
            @Override
            public void onSuccess(List<Subject> subjects) {
                showLoading(false);
                swipeRefresh.setRefreshing(false);
                allCurriculumSubjects = new ArrayList<>(subjects != null ? subjects : new ArrayList<>());

                List<Subject> assignedList = new ArrayList<>();
                if (subjects != null) {
                    for (Subject s : subjects) {
                        if (currentAssignedSubjectIds.contains(s.getSubjectId()) ||
                                currentAssignedSubjectIds.contains(s.getSubjectCode())) {
                            assignedList.add(s);
                        }
                    }
                }

                loadedSubjects = assignedList;
                updateSummaryCounts(loadedSubjects);
                adapter.setTeacherMode(true);
                adapter.updateData(loadedSubjects);
                applyFilters();

                if (loadedSubjects.isEmpty()) {
                    showEmptyState("No subjects currently assigned to your teaching load.\nTap '+' to assign a subject.");
                    if (btnClearFilters != null) {
                        btnClearFilters.setText("ASSIGN SUBJECT");
                        btnClearFilters.setOnClickListener(v -> showAssignSubjectPicker());
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(ManageSubjectsActivity.this, "Error loading subjects: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAssignSubjectPicker() {
        if (currentTeacher == null && (currentAssignedSubjectIds.isEmpty() && teacherUid.isEmpty())) {
            Toast.makeText(this, "Loading profile, please try again in a moment.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Dynamic Program Level & Department resolution (never hardcoded to UG)
        String teacherLevel = "UG";
        String teacherDept = "";
        String teacherDeptId = "";

        if (currentTeacher != null) {
            teacherDept = currentTeacher.getDepartment();
            teacherDeptId = currentTeacher.getDepartmentId();
            if (currentTeacher.getProgramLevel() != null && !currentTeacher.getProgramLevel().trim().isEmpty()) {
                teacherLevel = currentTeacher.getProgramLevel().trim().toUpperCase();
            } else {
                teacherLevel = Department.resolveDefaultProgramLevel(teacherDeptId, teacherDept);
            }
        }

        final String finalLevel = teacherLevel;
        final String finalDept = teacherDept;
        final String finalDeptId = teacherDeptId;

        List<Subject> eligibleSubjects = new ArrayList<>();
        for (Subject s : allCurriculumSubjects) {
            if (!"ACTIVE".equalsIgnoreCase(s.getStatus())) continue;

            // Program Level matching (e.g. PG vs UG)
            String sLevel = s.getProgramLevel() != null ? s.getProgramLevel().toUpperCase() : "UG";
            if (!finalLevel.equalsIgnoreCase(sLevel)) continue;

            // Department matching: prefer departmentId if available, fallback to smart name/shortName matching
            boolean deptMatch = false;
            if (!finalDeptId.isEmpty() && s.getDepartmentId() != null && !s.getDepartmentId().isEmpty()) {
                deptMatch = finalDeptId.equalsIgnoreCase(s.getDepartmentId());
            }
            if (!deptMatch) {
                deptMatch = Subject.isDepartmentMatching(finalDept, s.getDepartment()) ||
                            Subject.isDepartmentMatching(finalDept, s.getDepartmentShortName());
            }
            if (!deptMatch) continue;

            // Exclude already assigned subjects
            if (currentAssignedSubjectIds.contains(s.getSubjectId()) ||
                    currentAssignedSubjectIds.contains(s.getSubjectCode())) {
                continue;
            }

            eligibleSubjects.add(s);
        }

        if (eligibleSubjects.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("No Eligible Subjects")
                    .setMessage("There are no unassigned active " + finalLevel + " subjects available for department: " + (finalDept.isEmpty() ? "All" : finalDept) + ".")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        String[] displayOptions = new String[eligibleSubjects.size()];
        boolean[] checked = new boolean[eligibleSubjects.size()];

        for (int i = 0; i < eligibleSubjects.size(); i++) {
            Subject sub = eligibleSubjects.get(i);
            String sem = (sub.getSemester() != null && !sub.getSemester().isEmpty()) ? " [" + sub.getSemester() + "]" : "";
            displayOptions[i] = sub.getSubjectName() + " (" + sub.getSubjectCode() + ")" + sem;
        }

        new AlertDialog.Builder(this)
                .setTitle("Assign Subjects (" + finalLevel + " • " + (finalDept.isEmpty() ? "All" : finalDept) + ")")
                .setMultiChoiceItems(displayOptions, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton("ASSIGN SELECTED", (dialog, which) -> {
                    List<Subject> selected = new ArrayList<>();
                    for (int i = 0; i < checked.length; i++) {
                        if (checked[i]) {
                            selected.add(eligibleSubjects.get(i));
                        }
                    }
                    if (!selected.isEmpty()) {
                        assignSubjectsToTeacher(selected);
                    }
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void assignSubjectsToTeacher(List<Subject> newSubjects) {
        showLoading(true);

        for (Subject s : newSubjects) {
            if (!currentAssignedSubjectIds.contains(s.getSubjectId()) && !currentAssignedSubjectIds.contains(s.getSubjectCode())) {
                currentAssignedSubjectIds.add(s.getSubjectId());
                currentAssignedSubjectNames.add(s.getSubjectName());
            }
        }

        String targetUid = (currentTeacher != null && currentTeacher.getUid() != null && !currentTeacher.getUid().isEmpty())
                ? currentTeacher.getUid()
                : (!teacherUid.isEmpty() ? teacherUid : sessionManager.getUserEmail());

        Map<String, Object> updates = new HashMap<>();
        updates.put("assignedSubjectIds", currentAssignedSubjectIds);
        updates.put("assignedSubjectNames", currentAssignedSubjectNames);
        updates.put("updatedAt", FieldValue.serverTimestamp());
        updates.put("updatedBy", targetUid);

        db.collection("teachers").document(targetUid).update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Mirror to users collection
                    Map<String, Object> userUpdates = new HashMap<>();
                    userUpdates.put("assignedSubjectIds", currentAssignedSubjectIds);
                    userUpdates.put("assignedSubjectNames", currentAssignedSubjectNames);
                    userUpdates.put("updatedAt", FieldValue.serverTimestamp());
                    db.collection("users").document(targetUid).update(userUpdates);

                    // Synchronize teacher_assignments collection with deterministic ID
                    String teacherName = currentTeacher != null ? currentTeacher.getName() : sessionManager.getUserName();
                    String teacherEmailVal = currentTeacher != null ? currentTeacher.getEmail() : sessionManager.getUserEmail();
                    String deptName = currentTeacher != null ? currentTeacher.getDepartment() : "General";

                    for (Subject sub : newSubjects) {
                        String id = (teacherName + "_" + sub.getSubjectCode()).replaceAll("\\s+", "_").toLowerCase();
                        TeacherAssignment ta = new TeacherAssignment(
                                id,
                                teacherName,
                                teacherEmailVal,
                                sub.getSubjectCode(),
                                sub.getSubjectName(),
                                deptName,
                                sub.getSemester() != null ? sub.getSemester() : "Semester I",
                                "2026-27",
                                "ACTIVE"
                        );
                        db.collection("teacher_assignments").document(id).set(ta);
                    }

                    // Update local SQLite teacher record
                    try {
                        if (currentTeacher != null) {
                            currentTeacher.setAssignedSubjectIds(currentAssignedSubjectIds);
                            currentTeacher.setAssignedSubjectNames(currentAssignedSubjectNames);
                            dbHelper.updateTeacher(currentTeacher);
                        }
                    } catch (Exception ignored) {}

                    PortalActivityLogger.getInstance(this).logTeacherActivity(
                            teacherName,
                            "Assigned Subjects",
                            "Added " + newSubjects.size() + " subject(s) to teaching load",
                            "Faculty Assignment"
                    );

                    showLoading(false);
                    Toast.makeText(this, "Subjects assigned successfully!", Toast.LENGTH_SHORT).show();
                    loadSubjectData();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed to assign subjects: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onUnassignClick(Subject subject) {
        if (subject == null) return;
        String name = subject.getSubjectName() != null ? subject.getSubjectName() : subject.getSubjectCode();
        String code = subject.getSubjectCode() != null ? subject.getSubjectCode() : "";

        String message = "Remove " + name + " (" + code + ") from your teaching load?\n\n"
                + "✓ This ONLY removes your current teaching assignment.\n"
                + "✓ Existing student marks, attendance, assignments, and results remain completely safe.\n"
                + "✓ Master curriculum subject record is NOT deleted.";

        new AlertDialog.Builder(this)
                .setTitle("Remove Subject Assignment?")
                .setMessage(message)
                .setPositiveButton("UNASSIGN", (dialog, which) -> unassignSubjectFromTeacher(subject))
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void unassignSubjectFromTeacher(Subject subject) {
        showLoading(true);
        String subId = subject.getSubjectId();
        String subCode = subject.getSubjectCode();

        // Remove from assigned IDs and Names lists
        int removeIdx = -1;
        if (subId != null && currentAssignedSubjectIds.contains(subId)) {
            removeIdx = currentAssignedSubjectIds.indexOf(subId);
        } else if (subCode != null && currentAssignedSubjectIds.contains(subCode)) {
            removeIdx = currentAssignedSubjectIds.indexOf(subCode);
        }

        if (removeIdx >= 0) {
            currentAssignedSubjectIds.remove(removeIdx);
            if (removeIdx < currentAssignedSubjectNames.size()) {
                currentAssignedSubjectNames.remove(removeIdx);
            }
        }

        // Clean any remaining occurrences of either subId or subCode
        while (subId != null && currentAssignedSubjectIds.contains(subId)) {
            int idx = currentAssignedSubjectIds.indexOf(subId);
            currentAssignedSubjectIds.remove(idx);
            if (idx < currentAssignedSubjectNames.size()) currentAssignedSubjectNames.remove(idx);
        }
        while (subCode != null && currentAssignedSubjectIds.contains(subCode)) {
            int idx = currentAssignedSubjectIds.indexOf(subCode);
            currentAssignedSubjectIds.remove(idx);
            if (idx < currentAssignedSubjectNames.size()) currentAssignedSubjectNames.remove(idx);
        }

        String targetUid = (currentTeacher != null && currentTeacher.getUid() != null && !currentTeacher.getUid().isEmpty())
                ? currentTeacher.getUid()
                : (!teacherUid.isEmpty() ? teacherUid : sessionManager.getUserEmail());

        Map<String, Object> updates = new HashMap<>();
        updates.put("assignedSubjectIds", currentAssignedSubjectIds);
        updates.put("assignedSubjectNames", currentAssignedSubjectNames);
        updates.put("updatedAt", FieldValue.serverTimestamp());
        updates.put("updatedBy", targetUid);

        db.collection("teachers").document(targetUid).update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Mirror to users collection
                    Map<String, Object> userUpdates = new HashMap<>();
                    userUpdates.put("assignedSubjectIds", currentAssignedSubjectIds);
                    userUpdates.put("assignedSubjectNames", currentAssignedSubjectNames);
                    userUpdates.put("updatedAt", FieldValue.serverTimestamp());
                    db.collection("users").document(targetUid).update(userUpdates);

                    // Delete ONLY this teacher's assignment document from legacy teacher_assignments
                    String teacherEmailVal = currentTeacher != null ? currentTeacher.getEmail() : sessionManager.getUserEmail();
                    if (teacherEmailVal != null && !teacherEmailVal.isEmpty()) {
                        db.collection("teacher_assignments")
                                .whereEqualTo("teacherEmail", teacherEmailVal.trim().toLowerCase())
                                .get()
                                .addOnSuccessListener(querySnapshots -> {
                                    if (querySnapshots != null && !querySnapshots.isEmpty()) {
                                        for (DocumentSnapshot doc : querySnapshots.getDocuments()) {
                                            String c = doc.getString("subjectCode");
                                            String id = doc.getString("subjectId");
                                            if ((subCode != null && subCode.equalsIgnoreCase(c)) ||
                                                    (subId != null && subId.equalsIgnoreCase(id))) {
                                                db.collection("teacher_assignments").document(doc.getId()).delete();
                                            }
                                        }
                                    }
                                });
                    }

                    // Update local SQLite teacher record
                    try {
                        if (currentTeacher != null) {
                            currentTeacher.setAssignedSubjectIds(currentAssignedSubjectIds);
                            currentTeacher.setAssignedSubjectNames(currentAssignedSubjectNames);
                            dbHelper.updateTeacher(currentTeacher);
                        }
                    } catch (Exception ignored) {}

                    PortalActivityLogger.getInstance(this).logTeacherActivity(
                            currentTeacher != null ? currentTeacher.getName() : sessionManager.getUserName(),
                            "Unassigned Subject",
                            "Removed " + subject.getSubjectName() + " (" + subject.getSubjectCode() + ") from teaching load",
                            "Faculty Assignment"
                    );

                    showLoading(false);
                    Toast.makeText(this, "Subject removed from your teaching assignments.", Toast.LENGTH_SHORT).show();

                    loadedSubjects.remove(subject);
                    updateSummaryCounts(loadedSubjects);
                    adapter.updateData(loadedSubjects);
                    applyFilters();

                    if (loadedSubjects.isEmpty()) {
                        showEmptyState("No subjects currently assigned to your teaching load.\nTap '+' to assign a subject.");
                        if (btnClearFilters != null) {
                            btnClearFilters.setText("ASSIGN SUBJECT");
                            btnClearFilters.setOnClickListener(v -> showAssignSubjectPicker());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed to unassign subject: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateSummaryCounts(List<Subject> subjects) {
        int total = subjects.size();
        int active = 0;
        int inactive = 0;

        for (Subject s : subjects) {
            if ("INACTIVE".equalsIgnoreCase(s.getStatus())) {
                inactive++;
            } else {
                active++;
            }
        }

        tvTotalCount.setText(String.valueOf(total));
        tvActiveCount.setText(String.valueOf(active));
        tvInactiveCount.setText(String.valueOf(inactive));
    }

    private void applyFilters() {
        String query = etSearch.getText() != null ? etSearch.getText().toString() : "";
        if (adapter != null) {
            adapter.filter(query, selectedProgramLevel, selectedDept, selectedSem, selectedStatus);

            if (adapter.getItemCount() == 0) {
                if (isTeacherMode && loadedSubjects.isEmpty()) {
                    showEmptyState("No subjects currently assigned to your teaching load.\nTap '+' to assign a subject.");
                    if (btnClearFilters != null) {
                        btnClearFilters.setText("ASSIGN SUBJECT");
                        btnClearFilters.setOnClickListener(v -> showAssignSubjectPicker());
                    }
                } else {
                    showEmptyState("No subjects match your search or filters.");
                    if (btnClearFilters != null) {
                        btnClearFilters.setText("CLEAR FILTERS");
                        btnClearFilters.setOnClickListener(v -> resetFilters());
                    }
                }
            } else {
                hideEmptyState();
            }
        }
    }

    private void resetFilters() {
        etSearch.setText("");
        if (chipGroupProgramLevel != null) {
            chipGroupProgramLevel.check(R.id.chipSubjectLevelAll);
        }
        selectedProgramLevel = "ALL";
        updateDepartmentFilterOptions();
        updateSemesterFilterOptions();
        chipGroupStatus.check(R.id.chipFilterSubjectActive);
        selectedStatus = "ACTIVE";
        applyFilters();
    }

    @Override
    public void onViewClick(Subject subject) {
        Intent intent = new Intent(this, SubjectDetailsActivity.class);
        intent.putExtra("subjectId", subject.getSubjectId());
        intent.putExtra("subjectCode", subject.getSubjectCode());
        intent.putExtra("subjectName", subject.getSubjectName());
        intent.putExtra("description", subject.getDescription());
        intent.putExtra("department", subject.getDepartment());
        intent.putExtra("departmentId", subject.getDepartmentId());
        intent.putExtra("departmentShortName", subject.getDepartmentShortName());
        intent.putExtra("programLevel", subject.getProgramLevel());
        intent.putExtra("semester", subject.getSemester());
        intent.putExtra("credits", subject.getCredits());
        intent.putExtra("subjectType", subject.getSubjectType());
        intent.putExtra("weeklyHours", subject.getWeeklyHours());
        intent.putExtra("totalHours", subject.getTotalHours());
        intent.putExtra("status", subject.getStatus());
        startActivity(intent);
    }

    @Override
    public void onEditClick(Subject subject) {
        if (isTeacherMode) {
            Toast.makeText(this, "Curriculum editing is restricted to administrators.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, EditSubjectActivity.class);
        intent.putExtra("subjectId", subject.getSubjectId());
        intent.putExtra("subjectCode", subject.getSubjectCode());
        intent.putExtra("subjectName", subject.getSubjectName());
        intent.putExtra("description", subject.getDescription());
        intent.putExtra("department", subject.getDepartment());
        intent.putExtra("departmentId", subject.getDepartmentId());
        intent.putExtra("departmentShortName", subject.getDepartmentShortName());
        intent.putExtra("programLevel", subject.getProgramLevel());
        intent.putExtra("semester", subject.getSemester());
        intent.putExtra("credits", subject.getCredits());
        intent.putExtra("subjectType", subject.getSubjectType());
        intent.putExtra("weeklyHours", subject.getWeeklyHours());
        intent.putExtra("totalHours", subject.getTotalHours());
        intent.putExtra("status", subject.getStatus());
        startActivity(intent);
    }

    @Override
    public void onStatusToggleClick(Subject subject) {
        if (isTeacherMode) {
            Toast.makeText(this, "Curriculum status modification is restricted to administrators.", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean isActive = "ACTIVE".equalsIgnoreCase(subject.getStatus());
        String title = isActive ? "Deactivate Subject?" : "Restore Subject?";
        String message = isActive
                ? "Deactivate " + subject.getSubjectName() + " (" + subject.getSubjectCode() + ")? Existing attendance, assignments, and results will be preserved."
                : "Restore " + subject.getSubjectName() + " (" + subject.getSubjectCode() + ") to active status?";
        String actionBtnText = isActive ? "DEACTIVATE" : "RESTORE";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(actionBtnText, (dialog, which) -> {
                    showLoading(true);
                    if (isActive) {
                        repository.deactivateSubject(subject.getSubjectId(), new SubjectRepository.OnSubjectOperationListener() {
                            @Override
                            public void onSuccess(String message) {
                                showLoading(false);
                                Toast.makeText(ManageSubjectsActivity.this, "Subject moved to inactive.", Toast.LENGTH_SHORT).show();
                                loadSubjectData();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                showLoading(false);
                                Toast.makeText(ManageSubjectsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        repository.restoreSubject(subject.getSubjectId(), new SubjectRepository.OnSubjectOperationListener() {
                            @Override
                            public void onSuccess(String message) {
                                showLoading(false);
                                Toast.makeText(ManageSubjectsActivity.this, "Subject restored successfully.", Toast.LENGTH_SHORT).show();
                                loadSubjectData();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                showLoading(false);
                                Toast.makeText(ManageSubjectsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    @Override
    public void onDeleteClick(Subject subject) {
        if (isTeacherMode) {
            // Defensive guard: Teachers can NEVER delete curriculum subjects
            Toast.makeText(this, "Teachers cannot delete curriculum master subjects. Use UNASSIGN instead.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (subject == null) return;
        String message = "Are you sure you want to permanently delete " + subject.getSubjectName() + " (" + subject.getSubjectCode() + ")?\n\nThis will remove the course subject record from Cloud Firestore and local database.";

        new AlertDialog.Builder(this)
                .setTitle("Delete Subject?")
                .setMessage(message)
                .setPositiveButton("DELETE", (dialog, which) -> {
                    showLoading(true);
                    repository.deleteSubject(subject, new SubjectRepository.OnSubjectOperationListener() {
                        @Override
                        public void onSuccess(String message) {
                            showLoading(false);
                            Toast.makeText(ManageSubjectsActivity.this, "Subject deleted successfully.", Toast.LENGTH_SHORT).show();
                            loadSubjectData();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            showLoading(false);
                            Toast.makeText(ManageSubjectsActivity.this, "Error deleting subject: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showEmptyState(String message) {
        tvEmptyMessage.setText(message);
        layoutEmpty.setVisibility(View.VISIBLE);
        rvSubjects.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        layoutEmpty.setVisibility(View.GONE);
        rvSubjects.setVisibility(View.VISIBLE);
    }
}
