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

import com.example.adapter.StudentAdapter;
import com.example.model.Department;
import com.example.model.Student;
import com.example.repository.StudentRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for managing student records in GradeXpert ERP.
 * Supports Program Level ("UG" / "PG") filters, Department, and Semester classification.
 */
public class ManageStudentsActivity extends AppCompatActivity implements StudentAdapter.OnStudentClickListener {

    private static final String TAG = "ManageStudentsActivity";

    private StudentRepository repository;
    private FirebaseFirestore db;
    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;

    private TextView tvTotalCount;
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

    private RecyclerView rvStudents;
    private FloatingActionButton fabAddStudent;

    private StudentAdapter adapter;
    private List<Student> loadedStudents = new ArrayList<>();

    private final List<Department> allDepartments = new ArrayList<>();
    private final List<String> deptFilterOptions = new ArrayList<>();
    private ArrayAdapter<String> deptFilterAdapter;

    private final List<String> semFilterOptions = new ArrayList<>();
    private ArrayAdapter<String> semFilterAdapter;

    private String selectedProgramLevel = "ALL";
    private String selectedDept = "All Departments";
    private String selectedSem = "All Semesters";
    private String selectedStatus = "ACTIVE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_students);

        db = FirebaseFirestore.getInstance();
        repository = StudentRepository.getInstance(this);

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
        loadStudentData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStudentData();
    }

    private void setupWindowInsets() {
        View appBar = findViewById(R.id.appBarStudents);
        if (appBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, insets) -> {
                androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                v.setPadding(0, statusBarInsets.top, 0, 0);
                return insets;
            });
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarStudents);
        swipeRefresh = findViewById(R.id.swipeRefreshStudents);

        tvTotalCount = findViewById(R.id.tvSummaryTotalCount);
        tvActiveCount = findViewById(R.id.tvSummaryActiveCount);
        tvInactiveCount = findViewById(R.id.tvSummaryInactiveCount);

        etSearch = findViewById(R.id.etSearchStudent);
        chipGroupProgramLevel = findViewById(R.id.chipGroupStudentProgramLevel);
        spDept = findViewById(R.id.spFilterDepartment);
        spSem = findViewById(R.id.spFilterSemester);
        chipGroupStatus = findViewById(R.id.chipGroupStudentStatus);

        progressBar = findViewById(R.id.progressBarManageStudents);
        layoutEmpty = findViewById(R.id.layoutEmptyState);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        btnClearFilters = findViewById(R.id.btnClearFilters);

        rvStudents = findViewById(R.id.rvStudentsList);
        fabAddStudent = findViewById(R.id.fabAddStudent);

        btnClearFilters.setOnClickListener(v -> resetFilters());
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Manage Students");
            getSupportActionBar().setSubtitle("UG & PG Academic Profiles");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(this::loadStudentData);
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
                if (checkedId == R.id.chipStudentLevelUG) {
                    selectedProgramLevel = "UG";
                } else if (checkedId == R.id.chipStudentLevelPG) {
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
            if (checkedId == R.id.chipFilterInactive) {
                selectedStatus = "INACTIVE";
            } else if (checkedId == R.id.chipFilterAll) {
                selectedStatus = "ALL";
            } else {
                selectedStatus = "ACTIVE";
            }
            applyFilters();
        });
    }

    private void setupRecyclerView() {
        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StudentAdapter(new ArrayList<>(), this);
        rvStudents.setAdapter(adapter);
    }

    private void setupFab() {
        fabAddStudent.setOnClickListener(v -> {
            Intent intent = new Intent(ManageStudentsActivity.this, AddStudentActivity.class);
            startActivity(intent);
        });
    }

    private void loadStudentData() {
        showLoading(true);
        repository.fetchStudents(new StudentRepository.OnStudentsLoadedListener() {
            @Override
            public void onSuccess(List<Student> students) {
                showLoading(false);
                swipeRefresh.setRefreshing(false);
                loadedStudents = students;
                updateSummaryCounts(students);
                adapter.updateData(students);
                applyFilters();
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(ManageStudentsActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSummaryCounts(List<Student> students) {
        int total = students.size();
        int active = 0;
        int inactive = 0;

        for (Student s : students) {
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
                showEmptyState("No students match your search or filters.");
            } else {
                hideEmptyState();
            }
        }
    }

    private void resetFilters() {
        etSearch.setText("");
        if (chipGroupProgramLevel != null) {
            chipGroupProgramLevel.check(R.id.chipStudentLevelAll);
        }
        selectedProgramLevel = "ALL";
        updateDepartmentFilterOptions();
        updateSemesterFilterOptions();
        chipGroupStatus.check(R.id.chipFilterActive);
        selectedStatus = "ACTIVE";
        applyFilters();
    }

    @Override
    public void onViewClick(Student student) {
        Intent intent = new Intent(this, StudentDetailsActivity.class);
        intent.putExtra("studentId", student.getStudentId());
        intent.putExtra("name", student.getName());
        intent.putExtra("registerNo", student.getRegisterNo());
        intent.putExtra("email", student.getEmail());
        intent.putExtra("phone", student.getPhone());
        intent.putExtra("department", student.getDepartment());
        intent.putExtra("departmentId", student.getDepartmentId());
        intent.putExtra("departmentShortName", student.getDepartmentShortName());
        intent.putExtra("programLevel", student.getProgramLevel());
        intent.putExtra("semester", student.getSemester());
        intent.putExtra("section", student.getSection());
        intent.putExtra("gender", student.getGender());
        intent.putExtra("dateOfBirth", student.getDateOfBirth());
        intent.putExtra("status", student.getStatus());
        startActivity(intent);
    }

    @Override
    public void onEditClick(Student student) {
        Intent intent = new Intent(this, EditStudentActivity.class);
        intent.putExtra("studentId", student.getStudentId());
        intent.putExtra("name", student.getName());
        intent.putExtra("registerNo", student.getRegisterNo());
        intent.putExtra("email", student.getEmail());
        intent.putExtra("phone", student.getPhone());
        intent.putExtra("department", student.getDepartment());
        intent.putExtra("departmentId", student.getDepartmentId());
        intent.putExtra("departmentShortName", student.getDepartmentShortName());
        intent.putExtra("programLevel", student.getProgramLevel());
        intent.putExtra("semester", student.getSemester());
        intent.putExtra("section", student.getSection());
        intent.putExtra("gender", student.getGender());
        intent.putExtra("dateOfBirth", student.getDateOfBirth());
        intent.putExtra("status", student.getStatus());
        startActivity(intent);
    }

    @Override
    public void onStatusToggleClick(Student student) {
        boolean isActive = "ACTIVE".equalsIgnoreCase(student.getStatus());
        String title = isActive ? "Deactivate Student?" : "Restore Student?";
        String message = isActive
                ? "Are you sure you want to move " + student.getName() + " (" + student.getRegisterNo() + ") to inactive? Attendance and marks history will be preserved."
                : "Restore " + student.getName() + " (" + student.getRegisterNo() + ") to active status?";
        String actionBtnText = isActive ? "DEACTIVATE" : "RESTORE";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(actionBtnText, (dialog, which) -> {
                    showLoading(true);
                    if (isActive) {
                        repository.deactivateStudent(student.getStudentId(), new StudentRepository.OnStudentOperationListener() {
                            @Override
                            public void onSuccess(String message) {
                                showLoading(false);
                                Toast.makeText(ManageStudentsActivity.this, "Student moved to inactive.", Toast.LENGTH_SHORT).show();
                                loadStudentData();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                showLoading(false);
                                Toast.makeText(ManageStudentsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        repository.restoreStudent(student.getStudentId(), new StudentRepository.OnStudentOperationListener() {
                            @Override
                            public void onSuccess(String message) {
                                showLoading(false);
                                Toast.makeText(ManageStudentsActivity.this, "Student restored successfully.", Toast.LENGTH_SHORT).show();
                                loadStudentData();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                showLoading(false);
                                Toast.makeText(ManageStudentsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
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
        rvStudents.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        layoutEmpty.setVisibility(View.GONE);
        rvStudents.setVisibility(View.VISIBLE);
    }
}
