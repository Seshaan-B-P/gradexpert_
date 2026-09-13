package com.example;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.adapter.TeacherAdapter;
import com.example.database.DatabaseHelper;
import com.example.model.Teacher;
import com.example.utils.PortalActivityLogger;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity for managing faculty teacher profiles, searching, filtering,
 * updating status (Active/Inactive/Suspended), and launching password changes.
 */
public class ManageTeachersActivity extends AppCompatActivity implements TeacherAdapter.OnTeacherClickListener {

    private DatabaseHelper dbHelper;
    private FirebaseFirestore db;
    private PortalActivityLogger activityLogger;

    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private TextInputEditText etSearch;
    private MaterialButton btnAddTeacher;
    private ChipGroup chipGroupProgramLevel, chipGroupStatus, chipGroupDept;
    private ProgressBar progressBar;
    private TextView tvTotalLabel, tvEmpty, tvEmptySub, btnClearFilters;
    private View layoutEmptyTeachers;
    private MaterialButton btnEmptyResetFilters;
    private RecyclerView rvTeachers;

    private TeacherAdapter adapter;
    private List<Teacher> masterTeacherList = new ArrayList<>();
    private List<Teacher> filteredTeacherList = new ArrayList<>();

    private String selectedProgramLevelFilter = "ALL";
    private String selectedStatusFilter = "ALL";
    private String selectedDeptFilter = "ALL";
    private ListenerRegistration teachersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_teachers);

        dbHelper = new DatabaseHelper(this);
        db = FirebaseFirestore.getInstance();
        activityLogger = PortalActivityLogger.getInstance(this);

        initViews();
        setupToolbar();
        setupWindowInsets();
        setupRecyclerView();
        setupFilters();
        setupSearchListener();
        listenToFirestoreTeachers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTeachers();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (teachersListener != null) {
            teachersListener.remove();
        }
    }

    private void setupWindowInsets() {
        View appBar = findViewById(R.id.appBarTeachers);
        if (appBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, insets) -> {
                androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                v.setPadding(0, statusBarInsets.top, 0, 0);
                return insets;
            });
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarTeachers);
        swipeRefresh = findViewById(R.id.swipeRefreshTeachers);
        etSearch = findViewById(R.id.etSearchTeacher);
        btnAddTeacher = findViewById(R.id.btnAddTeacher);
        chipGroupProgramLevel = findViewById(R.id.chipGroupTeacherProgramLevel);
        chipGroupStatus = findViewById(R.id.chipGroupTeacherStatus);
        chipGroupDept = findViewById(R.id.chipGroupTeacherDept);
        progressBar = findViewById(R.id.progressBarTeachers);
        tvTotalLabel = findViewById(R.id.tvTotalTeachersLabel);
        tvEmpty = findViewById(R.id.tvEmptyTeachers);
        tvEmptySub = findViewById(R.id.tvEmptyTeachersSub);
        btnClearFilters = findViewById(R.id.btnClearFilters);
        layoutEmptyTeachers = findViewById(R.id.layoutEmptyTeachers);
        btnEmptyResetFilters = findViewById(R.id.btnEmptyResetFilters);
        rvTeachers = findViewById(R.id.rvTeachers);

        btnAddTeacher.setOnClickListener(v -> {
            Intent intent = new Intent(ManageTeachersActivity.this, AddTeacherActivity.class);
            startActivity(intent);
        });

        if (btnClearFilters != null) {
            btnClearFilters.setOnClickListener(v -> resetAllFilters());
        }

        if (btnEmptyResetFilters != null) {
            btnEmptyResetFilters.setOnClickListener(v -> resetAllFilters());
        }

        swipeRefresh.setOnRefreshListener(this::loadTeachers);
    }

    private void resetAllFilters() {
        selectedProgramLevelFilter = "ALL";
        selectedStatusFilter = "ALL";
        selectedDeptFilter = "ALL";
        if (etSearch != null) {
            etSearch.setText("");
        }
        if (chipGroupProgramLevel != null) {
            chipGroupProgramLevel.check(R.id.chipTeacherLevelAll);
        }
        if (chipGroupStatus != null) {
            chipGroupStatus.check(R.id.chipTeacherStatusAll);
        }
        if (chipGroupDept != null) {
            chipGroupDept.check(R.id.chipTeacherDeptAll);
        }
        applyFilters();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Manage Teachers");
            getSupportActionBar().setSubtitle("Faculty Credentials & Profiles");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        rvTeachers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TeacherAdapter(filteredTeacherList, this);
        rvTeachers.setAdapter(adapter);
    }

    private void setupFilters() {
        if (chipGroupProgramLevel != null) {
            chipGroupProgramLevel.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.chipTeacherLevelUG) {
                    selectedProgramLevelFilter = "UG";
                } else if (checkedId == R.id.chipTeacherLevelPG) {
                    selectedProgramLevelFilter = "PG";
                } else {
                    selectedProgramLevelFilter = "ALL";
                }
                applyFilters();
            });
        }

        if (chipGroupStatus != null) {
            chipGroupStatus.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.chipTeacherStatusActive) {
                    selectedStatusFilter = "ACTIVE";
                } else if (checkedId == R.id.chipTeacherStatusInactive) {
                    selectedStatusFilter = "INACTIVE";
                } else if (checkedId == R.id.chipTeacherStatusSuspended) {
                    selectedStatusFilter = "SUSPENDED";
                } else {
                    selectedStatusFilter = "ALL";
                }
                applyFilters();
            });
        }

        if (chipGroupDept != null) {
            chipGroupDept.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.chipTeacherDeptMCA) {
                    selectedDeptFilter = "MCA";
                } else if (checkedId == R.id.chipTeacherDeptCSE) {
                    selectedDeptFilter = "CSE";
                } else if (checkedId == R.id.chipTeacherDeptIT) {
                    selectedDeptFilter = "IT";
                } else if (checkedId == R.id.chipTeacherDeptECE) {
                    selectedDeptFilter = "ECE";
                } else if (checkedId == R.id.chipTeacherDeptEEE) {
                    selectedDeptFilter = "EEE";
                } else if (checkedId == R.id.chipTeacherDeptMECH) {
                    selectedDeptFilter = "MECH";
                } else if (checkedId == R.id.chipTeacherDeptCIVIL) {
                    selectedDeptFilter = "CIVIL";
                } else if (checkedId == R.id.chipTeacherDeptMBA) {
                    selectedDeptFilter = "MBA";
                } else if (checkedId != View.NO_ID && checkedId != R.id.chipTeacherDeptAll) {
                    com.google.android.material.chip.Chip chip = group.findViewById(checkedId);
                    if (chip != null && chip.getTag() != null) {
                        selectedDeptFilter = chip.getTag().toString();
                    } else if (chip != null) {
                        selectedDeptFilter = chip.getText().toString();
                    } else {
                        selectedDeptFilter = "ALL";
                    }
                } else {
                    selectedDeptFilter = "ALL";
                }
                applyFilters();
            });
        }
    }

    private void syncDynamicDepartmentChips() {
        if (chipGroupDept == null) return;
        java.util.Set<String> knownCodes = new java.util.HashSet<>(java.util.Arrays.asList(
                "ALL", "MCA", "CSE", "IT", "ECE", "EEE", "MECH", "CIVIL", "MBA"
        ));
        for (Teacher t : masterTeacherList) {
            String shortName = t.getDepartmentShortName();
            if (TextUtils.isEmpty(shortName)) {
                shortName = t.getDepartment();
            }
            if (!TextUtils.isEmpty(shortName)) {
                String code = shortName.trim();
                String upperCode = code.toUpperCase();
                if (!knownCodes.contains(upperCode) && upperCode.length() <= 12) {
                    knownCodes.add(upperCode);
                    com.google.android.material.chip.Chip dynamicChip = new com.google.android.material.chip.Chip(this);
                    dynamicChip.setText(code);
                    dynamicChip.setTag(upperCode);
                    dynamicChip.setCheckable(true);
                    dynamicChip.setClickable(true);
                    chipGroupDept.addView(dynamicChip);
                }
            }
        }
    }

    private void setupSearchListener() {
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

    private Teacher parseTeacherFromDoc(DocumentSnapshot doc) {
        Teacher t = null;
        try {
            t = doc.toObject(Teacher.class);
        } catch (Exception ex) {
            // fallback below
        }
        if (t == null) {
            String name = doc.getString("name");
            if (name == null) name = doc.getString("displayName");
            String email = doc.getString("email");
            String dept = doc.getString("department");
            String phone = doc.getString("phone");
            String empId = doc.getString("employeeId");
            if (empId == null) empId = doc.getString("identifier");
            String status = doc.getString("status");
            t = new Teacher(doc.getId(), name, email, phone, empId, dept, "", "Faculty", "", "", status != null ? status : "ACTIVE");
        }

        if (t != null) {
            t.setUid(doc.getId());
            if (TextUtils.isEmpty(t.getName()) && doc.getString("displayName") != null) {
                t.setName(doc.getString("displayName"));
            }
            if (TextUtils.isEmpty(t.getEmployeeId())) {
                t.setEmployeeId(doc.getString("identifier"));
            }
            if (TextUtils.isEmpty(t.getLoginId()) && doc.getString("loginId") != null) {
                t.setLoginId(doc.getString("loginId"));
            }
            if (TextUtils.isEmpty(t.getDepartmentShortName()) && doc.getString("departmentShortName") != null) {
                t.setDepartmentShortName(doc.getString("departmentShortName"));
            }
            if (TextUtils.isEmpty(t.getDepartmentId()) && doc.getString("departmentId") != null) {
                t.setDepartmentId(doc.getString("departmentId"));
            }
            if (TextUtils.isEmpty(t.getProgramLevel())) {
                String pLevel = doc.getString("programLevel");
                if (!TextUtils.isEmpty(pLevel)) {
                    t.setProgramLevel(pLevel);
                } else {
                    t.setProgramLevel(com.example.model.Department.resolveDefaultProgramLevel(t.getDepartmentShortName(), t.getDepartment()));
                }
            }
            List<String> subIds = (List<String>) doc.get("assignedSubjectIds");
            if (subIds != null) t.setAssignedSubjectIds(subIds);
            List<String> subNames = (List<String>) doc.get("assignedSubjectNames");
            if (subNames != null) t.setAssignedSubjectNames(subNames);
        }
        return t;
    }

    private void listenToFirestoreTeachers() {
        teachersListener = db.collection("teachers")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        loadTeachersFromLocal();
                        return;
                    }
                    if (snapshots != null) {
                        masterTeacherList.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Teacher t = parseTeacherFromDoc(doc);
                            if (t != null) {
                                masterTeacherList.add(t);
                            }
                        }
                        syncDynamicDepartmentChips();
                        applyFilters();
                    }
                });
    }

    private void loadTeachers() {
        showLoading(true);
        db.collection("teachers")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        masterTeacherList.clear();
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Teacher t = parseTeacherFromDoc(doc);
                            if (t != null) {
                                masterTeacherList.add(t);
                            }
                        }
                        syncDynamicDepartmentChips();
                        applyFilters();
                    } else {
                        loadTeachersFromLocal();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);
                    loadTeachersFromLocal();
                });
    }

    private void loadTeachersFromLocal() {
        List<Teacher> localList = dbHelper.getAllTeachers();
        if (localList != null && !localList.isEmpty()) {
            masterTeacherList = localList;
        }
        syncDynamicDepartmentChips();
        applyFilters();
    }

    public static String resolveTeacherProgramLevel(Teacher t) {
        if (t == null) return "UG";
        String level = t.getProgramLevel();
        if (!TextUtils.isEmpty(level) && ("UG".equalsIgnoreCase(level) || "PG".equalsIgnoreCase(level))) {
            return level.toUpperCase();
        }
        return com.example.model.Department.resolveDefaultProgramLevel(t.getDepartmentShortName(), t.getDepartment());
    }

    public static boolean doesTeacherMatchDept(Teacher t, String filterDept) {
        if ("ALL".equalsIgnoreCase(filterDept) || filterDept == null || filterDept.trim().isEmpty()) {
            return true;
        }
        if (t == null) return false;
        filterDept = filterDept.trim().toUpperCase();

        String rawDept = t.getDepartment() != null ? t.getDepartment().trim().toUpperCase() : "";
        String shortDept = t.getDepartmentShortName() != null ? t.getDepartmentShortName().trim().toUpperCase() : "";
        String deptId = t.getDepartmentId() != null ? t.getDepartmentId().trim().toUpperCase() : "";

        // Direct equality or substring on short code, department ID, or raw name
        if (shortDept.equals(filterDept) || shortDept.contains(filterDept)) return true;
        if (deptId.equals(filterDept) || deptId.contains(filterDept)) return true;
        if (rawDept.equals(filterDept) || rawDept.contains(filterDept)) return true;

        // Semantic cross-matching for acronyms and full department names
        switch (filterDept) {
            case "MCA":
                return rawDept.contains("COMPUTER APPLICATION") || rawDept.contains("MCA") || shortDept.contains("MCA");
            case "CSE":
                return rawDept.contains("COMPUTER SCIENCE") || rawDept.contains("CSE") || shortDept.contains("CSE");
            case "IT":
                return rawDept.contains("INFORMATION TECH") || rawDept.contains("INFO TECH") || shortDept.contains("IT") || rawDept.equals("IT");
            case "ECE":
                return rawDept.contains("ELECTRONICS") || rawDept.contains("COMMUNICATION") || rawDept.contains("ECE") || shortDept.contains("ECE");
            case "EEE":
                return (rawDept.contains("ELECTRICAL") && rawDept.contains("ELECTRONIC")) || rawDept.contains("EEE") || shortDept.contains("EEE");
            case "MECH":
            case "MECHANICAL":
                return rawDept.contains("MECHANICAL") || rawDept.contains("MECH") || shortDept.contains("MECH");
            case "CIVIL":
                return rawDept.contains("CIVIL") || shortDept.contains("CIVIL");
            case "MBA":
                return rawDept.contains("BUSINESS") || rawDept.contains("MANAGEMENT") || rawDept.contains("MBA") || shortDept.contains("MBA");
            default:
                return rawDept.contains(filterDept) || shortDept.contains(filterDept) || deptId.contains(filterDept);
        }
    }

    public static boolean doesTeacherMatchLevel(Teacher t, String filterLevel) {
        if ("ALL".equalsIgnoreCase(filterLevel) || filterLevel == null || filterLevel.trim().isEmpty()) {
            return true;
        }
        filterLevel = filterLevel.trim().toUpperCase();
        String level = resolveTeacherProgramLevel(t);
        return filterLevel.equalsIgnoreCase(level);
    }

    public static boolean doesTeacherMatchStatus(Teacher t, String filterStatus) {
        if ("ALL".equalsIgnoreCase(filterStatus) || filterStatus == null || filterStatus.trim().isEmpty()) {
            return true;
        }
        String status = t.getStatus() != null ? t.getStatus().trim().toUpperCase() : "ACTIVE";
        return filterStatus.equalsIgnoreCase(status);
    }

    public static boolean doesTeacherMatchQuery(Teacher t, String query) {
        if (TextUtils.isEmpty(query)) {
            return true;
        }
        query = query.toLowerCase().trim();

        if (t.getName() != null && t.getName().toLowerCase().contains(query)) return true;
        if (t.getEmail() != null && t.getEmail().toLowerCase().contains(query)) return true;
        if (t.getEmployeeId() != null && t.getEmployeeId().toLowerCase().contains(query)) return true;
        if (t.getLoginId() != null && t.getLoginId().toLowerCase().contains(query)) return true;
        if (t.getPhone() != null && t.getPhone().toLowerCase().contains(query)) return true;
        if (t.getDepartment() != null && t.getDepartment().toLowerCase().contains(query)) return true;
        if (t.getDepartmentShortName() != null && t.getDepartmentShortName().toLowerCase().contains(query)) return true;
        if (t.getDesignation() != null && t.getDesignation().toLowerCase().contains(query)) return true;
        if (t.getQualification() != null && t.getQualification().toLowerCase().contains(query)) return true;

        String level = resolveTeacherProgramLevel(t);
        if (level.toLowerCase().contains(query)) return true;

        if (t.getAssignedSubjectNames() != null) {
            for (String subName : t.getAssignedSubjectNames()) {
                if (subName != null && subName.toLowerCase().contains(query)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void applyFilters() {
        String query = etSearch.getText() != null ? etSearch.getText().toString().trim().toLowerCase() : "";
        filteredTeacherList.clear();

        for (Teacher t : masterTeacherList) {
            boolean matchesLevel = doesTeacherMatchLevel(t, selectedProgramLevelFilter);
            boolean matchesStatus = doesTeacherMatchStatus(t, selectedStatusFilter);
            boolean matchesDept = doesTeacherMatchDept(t, selectedDeptFilter);
            boolean matchesQuery = doesTeacherMatchQuery(t, query);

            if (matchesLevel && matchesStatus && matchesDept && matchesQuery) {
                filteredTeacherList.add(t);
            }
        }

        adapter.updateData(filteredTeacherList);

        boolean isFiltered = !"ALL".equalsIgnoreCase(selectedProgramLevelFilter)
                || !"ALL".equalsIgnoreCase(selectedStatusFilter)
                || !"ALL".equalsIgnoreCase(selectedDeptFilter)
                || !query.isEmpty();

        if (btnClearFilters != null) {
            btnClearFilters.setVisibility(isFiltered ? View.VISIBLE : View.GONE);
        }

        if (tvTotalLabel != null) {
            if (isFiltered) {
                tvTotalLabel.setText("Showing " + filteredTeacherList.size() + " of " + masterTeacherList.size() + " Faculty (Filtered)");
            } else {
                tvTotalLabel.setText("All " + filteredTeacherList.size() + " Registered Faculty Members");
            }
        }

        if (filteredTeacherList.isEmpty()) {
            if (layoutEmptyTeachers != null) layoutEmptyTeachers.setVisibility(View.VISIBLE);
            if (rvTeachers != null) rvTeachers.setVisibility(View.GONE);

            if (tvEmpty != null) {
                if (masterTeacherList.isEmpty()) {
                    tvEmpty.setText("No faculty members registered yet.");
                } else {
                    tvEmpty.setText("No faculty members found");
                }
            }
            if (tvEmptySub != null) {
                if (masterTeacherList.isEmpty()) {
                    tvEmptySub.setText("Click the '+ Add' button above to register a faculty member.");
                } else {
                    List<String> activeFilters = new ArrayList<>();
                    if (!"ALL".equalsIgnoreCase(selectedProgramLevelFilter)) activeFilters.add("Level: " + selectedProgramLevelFilter);
                    if (!"ALL".equalsIgnoreCase(selectedDeptFilter)) activeFilters.add("Dept: " + selectedDeptFilter);
                    if (!"ALL".equalsIgnoreCase(selectedStatusFilter)) activeFilters.add("Status: " + selectedStatusFilter);
                    if (!query.isEmpty()) activeFilters.add("\"" + query + "\"");

                    if (activeFilters.isEmpty()) {
                        tvEmptySub.setText("Try adjusting your search query or filter options.");
                    } else {
                        tvEmptySub.setText(TextUtils.join(" • ", activeFilters) + "\nTry adjusting or resetting filters.");
                    }
                }
            }
        } else {
            if (layoutEmptyTeachers != null) layoutEmptyTeachers.setVisibility(View.GONE);
            if (rvTeachers != null) rvTeachers.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onViewClick(Teacher teacher) {
        if (teacher == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("Employee ID: ").append(teacher.getEmployeeId().isEmpty() ? "N/A" : teacher.getEmployeeId())
                .append("\nEmail: ").append(teacher.getEmail())
                .append("\nPhone: ").append(teacher.getPhone() != null ? teacher.getPhone() : "N/A")
                .append("\nDepartment: ").append(teacher.getDepartment())
                .append("\nProgram Level: ").append(teacher.getProgramLevel())
                .append("\nDesignation: ").append(teacher.getDesignation())
                .append("\nQualification: ").append(teacher.getQualification() != null ? teacher.getQualification() : "N/A")
                .append("\nDate of Joining: ").append(teacher.getDateOfJoining() != null ? teacher.getDateOfJoining() : "N/A")
                .append("\nAccount Status: ").append(teacher.getStatus());

        List<String> subNames = teacher.getAssignedSubjectNames();
        List<String> subIds = teacher.getAssignedSubjectIds();

        sb.append("\n\nAssigned Subjects (").append(subIds.size()).append("):");
        if (subIds.isEmpty()) {
            sb.append("\n• None assigned");
        } else {
            for (int i = 0; i < subIds.size(); i++) {
                String name = i < subNames.size() ? subNames.get(i) : "Subject";
                String id = subIds.get(i);
                sb.append("\n• ").append(name).append(" (ID: ").append(id).append(")");
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(teacher.getName())
                .setMessage(sb.toString())
                .setPositiveButton("CLOSE", null)
                .show();
    }

    @Override
    public void onEditClick(Teacher teacher) {
        showEditTeacherDialog(teacher);
    }

    @Override
    public void onChangePasswordClick(Teacher teacher) {
        if (teacher == null) return;
        Intent intent = new Intent(this, ChangeUserPasswordActivity.class);
        intent.putExtra(ChangeUserPasswordActivity.EXTRA_TARGET_UID, teacher.getUid());
        intent.putExtra(ChangeUserPasswordActivity.EXTRA_TARGET_NAME, teacher.getName());
        intent.putExtra(ChangeUserPasswordActivity.EXTRA_TARGET_EMAIL, teacher.getEmail());
        intent.putExtra(ChangeUserPasswordActivity.EXTRA_TARGET_ROLE, "TEACHER");
        intent.putExtra(ChangeUserPasswordActivity.EXTRA_TARGET_IDENTIFIER, teacher.getEmployeeId());
        intent.putExtra(ChangeUserPasswordActivity.EXTRA_TARGET_STATUS, teacher.getStatus());
        startActivity(intent);
    }

    @Override
    public void onStatusClick(Teacher teacher) {
        if (teacher == null) return;

        String[] statuses = new String[]{"ACTIVE", "INACTIVE", "SUSPENDED"};
        int currentIdx = 0;
        for (int i = 0; i < statuses.length; i++) {
            if (statuses[i].equalsIgnoreCase(teacher.getStatus())) {
                currentIdx = i;
                break;
            }
        }

        final int[] selected = {currentIdx};

        new AlertDialog.Builder(this)
                .setTitle("Update Status for " + teacher.getName())
                .setSingleChoiceItems(statuses, currentIdx, (dialog, which) -> selected[0] = which)
                .setPositiveButton("UPDATE", (dialog, which) -> {
                    String newStatus = statuses[selected[0]];
                    updateTeacherStatus(teacher, newStatus);
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void updateTeacherStatus(Teacher teacher, String newStatus) {
        showLoading(true);
        String targetUid = teacher.getUid() != null ? teacher.getUid() : String.valueOf(teacher.getId());

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);

        db.collection("teachers").document(targetUid).update(updates)
                .addOnSuccessListener(aVoid -> {
                    db.collection("users").document(targetUid).update(updates);
                    teacher.setStatus(newStatus);
                    activityLogger.logTeacherStatusChanged(targetUid, teacher.getName(), newStatus);
                    showLoading(false);
                    Toast.makeText(this, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed to update status: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showEditTeacherDialog(Teacher teacher) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_teacher, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tvTeacherDialogTitle);
        TextInputEditText etName = dialogView.findViewById(R.id.etTeacherName);
        TextInputEditText etEmail = dialogView.findViewById(R.id.etTeacherEmail);
        TextInputEditText etDept = dialogView.findViewById(R.id.etTeacherDept);
        TextInputEditText etPhone = dialogView.findViewById(R.id.etTeacherPhone);
        TextInputEditText etPassword = dialogView.findViewById(R.id.etTeacherPassword);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelTeacher);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSaveTeacher);

        com.google.android.material.chip.ChipGroup chipGroupAssigned = dialogView.findViewById(R.id.chipGroupEditAssignedSubjects);
        TextView tvNoSubjects = dialogView.findViewById(R.id.tvEditNoSubjectsNotice);
        MaterialButton btnEditSubjects = dialogView.findViewById(R.id.btnEditAssignedSubjects);

        tvTitle.setText("Edit Teacher Details");
        etName.setText(teacher.getName());
        etEmail.setText(teacher.getEmail());
        etEmail.setEnabled(false); // Email locked to prevent auth desync
        etDept.setText(teacher.getDepartment());
        etPhone.setText(teacher.getPhone());
        etPassword.setVisibility(View.GONE); // Passwords changed exclusively via ChangeUserPasswordActivity
        btnSave.setText("Update Profile");

        // Working copy of assigned subjects
        final List<String> currentSubIds = new ArrayList<>(teacher.getAssignedSubjectIds());
        final List<String> currentSubNames = new ArrayList<>(teacher.getAssignedSubjectNames());

        Runnable refreshChips = () -> {
            chipGroupAssigned.removeAllViews();
            if (currentSubIds.isEmpty()) {
                tvNoSubjects.setVisibility(View.VISIBLE);
            } else {
                tvNoSubjects.setVisibility(View.GONE);
                for (int i = 0; i < currentSubIds.size(); i++) {
                    final int idx = i;
                    final String subId = currentSubIds.get(i);
                    final String subName = (i < currentSubNames.size()) ? currentSubNames.get(i) : subId;

                    com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
                    chip.setText(subName + " ×");
                    chip.setCloseIconVisible(true);
                    chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#EEF2FF")));
                    chip.setTextColor(android.graphics.Color.parseColor("#4338CA"));
                    chip.setCloseIconTint(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4338CA")));

                    chip.setOnCloseIconClickListener(v -> {
                        currentSubIds.remove(idx);
                        if (idx < currentSubNames.size()) {
                            currentSubNames.remove(idx);
                        }
                        chipGroupAssigned.removeView(chip);
                        if (currentSubIds.isEmpty()) {
                            tvNoSubjects.setVisibility(View.VISIBLE);
                        }
                    });

                    chipGroupAssigned.addView(chip);
                }
            }
        };

        refreshChips.run();

        btnEditSubjects.setOnClickListener(v -> {
            showEditAssignedSubjectsPicker(teacher, currentSubIds, currentSubNames, refreshChips);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String dept = etDept.getText() != null ? etDept.getText().toString().trim() : "";
            String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";

            if (TextUtils.isEmpty(name)) {
                etName.setError("Name required");
                etName.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(dept)) {
                etDept.setError("Department required");
                etDept.requestFocus();
                return;
            }

            teacher.setName(name);
            teacher.setDepartment(dept);
            teacher.setPhone(phone);
            teacher.setAssignedSubjectIds(currentSubIds);
            teacher.setAssignedSubjectNames(currentSubNames);

            String targetUid = teacher.getUid() != null ? teacher.getUid() : String.valueOf(teacher.getId());

            com.google.firebase.auth.FirebaseUser currentAdmin = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            String adminUid = currentAdmin != null ? currentAdmin.getUid() : "ADMIN";

            Map<String, Object> updates = new HashMap<>();
            updates.put("name", name);
            updates.put("department", dept);
            updates.put("departmentName", dept);
            updates.put("phone", phone);
            updates.put("assignedSubjectIds", currentSubIds);
            updates.put("assignedSubjectNames", currentSubNames);
            updates.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
            updates.put("updatedBy", adminUid);

            db.collection("teachers").document(targetUid).update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Map<String, Object> userUpdates = new HashMap<>();
                        userUpdates.put("name", name);
                        userUpdates.put("department", dept);
                        userUpdates.put("departmentName", dept);
                        userUpdates.put("phone", phone);
                        userUpdates.put("assignedSubjectIds", currentSubIds);
                        userUpdates.put("assignedSubjectNames", currentSubNames);
                        userUpdates.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
                        db.collection("users").document(targetUid).update(userUpdates);

                        activityLogger.logTeacherProfileUpdated(targetUid, name, dept, teacher.getDesignation());
                        activityLogger.logTeacherSubjectAssignment(
                                targetUid,
                                name,
                                "TEACHER_SUBJECT_ASSIGNMENT_UPDATED",
                                TextUtils.join(",", currentSubIds),
                                TextUtils.join(", ", currentSubNames)
                        );

                        Toast.makeText(ManageTeachersActivity.this, "Teacher profile and subjects updated", Toast.LENGTH_SHORT).show();
                        applyFilters();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(ManageTeachersActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

        dialog.show();
    }

    private void showEditAssignedSubjectsPicker(Teacher teacher, List<String> currentSubIds, List<String> currentSubNames, Runnable onComplete) {
        showLoading(true);
        String pLevel = resolveTeacherProgramLevel(teacher);
        String dept = teacher.getDepartment();

        db.collection("subjects")
                .whereEqualTo("status", "ACTIVE")
                .whereEqualTo("programLevel", pLevel)
                .get()
                .addOnSuccessListener(snapshots -> {
                    showLoading(false);
                    List<com.example.model.Subject> matchingSubjects = new ArrayList<>();
                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            com.example.model.Subject s = doc.toObject(com.example.model.Subject.class);
                            if (s != null) {
                                s.setSubjectId(doc.getId());
                                if (com.example.model.Subject.isDepartmentMatching(dept, s.getDepartment())) {
                                    matchingSubjects.add(s);
                                }
                            }
                        }
                    }

                    if (matchingSubjects.isEmpty()) {
                        // Fallback to local DB
                        for (com.example.model.Subject s : dbHelper.getAllSubjects()) {
                            if ("ACTIVE".equalsIgnoreCase(s.getStatus()) && pLevel.equalsIgnoreCase(s.getProgramLevel()) && com.example.model.Subject.isDepartmentMatching(dept, s.getDepartment())) {
                                matchingSubjects.add(s);
                            }
                        }
                    }

                    if (matchingSubjects.isEmpty()) {
                        Toast.makeText(this, "No active " + pLevel + " subjects available for " + dept, Toast.LENGTH_LONG).show();
                        return;
                    }

                    String[] displayOptions = new String[matchingSubjects.size()];
                    boolean[] checked = new boolean[matchingSubjects.size()];

                    for (int i = 0; i < matchingSubjects.size(); i++) {
                        com.example.model.Subject sub = matchingSubjects.get(i);
                        String sem = (sub.getSemester() != null && !sub.getSemester().isEmpty()) ? " [" + sub.getSemester() + "]" : "";
                        displayOptions[i] = sub.getSubjectName() + " (" + sub.getSubjectCode() + ")" + sem;
                        if (currentSubIds.contains(sub.getSubjectId()) || currentSubIds.contains(sub.getSubjectCode())) {
                            checked[i] = true;
                        }
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("Edit Assigned Subjects (" + pLevel + " • " + dept + ")")
                            .setMultiChoiceItems(displayOptions, checked, (d, which, isChecked) -> {
                                checked[which] = isChecked;
                            })
                            .setPositiveButton("SAVE SELECTION", (d, which) -> {
                                currentSubIds.clear();
                                currentSubNames.clear();
                                for (int i = 0; i < checked.length; i++) {
                                    if (checked[i]) {
                                        com.example.model.Subject chosen = matchingSubjects.get(i);
                                        currentSubIds.add(chosen.getSubjectId());
                                        currentSubNames.add(chosen.getSubjectName());
                                    }
                                }
                                onComplete.run();
                            })
                            .setNegativeButton("CANCEL", null)
                            .show();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed to load subjects: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
