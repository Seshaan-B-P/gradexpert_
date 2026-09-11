package com.example;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.adapter.DepartmentAdapter;
import com.example.model.Department;
import com.example.utils.PortalActivityLogger;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for managing academic departments & degree programs catalog in GradeXpert.
 * Supports Program Level classification (UG / PG).
 */
public class ManageDepartmentsActivity extends AppCompatActivity implements DepartmentAdapter.OnDepartmentActionListener {

    private FirebaseFirestore db;

    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupLevel;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private RecyclerView rvDepts;
    private FloatingActionButton fabAdd;

    private DepartmentAdapter adapter;
    private List<Department> masterList = new ArrayList<>();
    private List<Department> filteredList = new ArrayList<>();

    private String selectedLevelFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_departments);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupToolbar();
        setupWindowInsets();
        setupSwipeRefresh();
        setupRecyclerView();
        setupFilters();
        setupSearch();
        loadDepartments();
    }

    private void setupWindowInsets() {
        View appBar = findViewById(R.id.appBarDepartments);
        if (appBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, insets) -> {
                androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                v.setPadding(0, statusBarInsets.top, 0, 0);
                return insets;
            });
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarDepts);
        swipeRefresh = findViewById(R.id.swipeRefreshDepts);
        etSearch = findViewById(R.id.etSearchDept);
        chipGroupLevel = findViewById(R.id.chipGroupDeptProgramLevel);
        progressBar = findViewById(R.id.progressBarDepts);
        tvEmpty = findViewById(R.id.tvDeptsEmpty);
        rvDepts = findViewById(R.id.rvDeptsList);
        fabAdd = findViewById(R.id.fabAddDept);

        fabAdd.setOnClickListener(v -> showAddEditDialog(null));
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Manage Departments");
            getSupportActionBar().setSubtitle("UG & PG Academic Programs");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(this::loadDepartments);
    }

    private void setupRecyclerView() {
        rvDepts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DepartmentAdapter(filteredList, this);
        rvDepts.setAdapter(adapter);
    }

    private void setupFilters() {
        chipGroupLevel.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipDeptLevelUG) {
                selectedLevelFilter = "UG";
            } else if (checkedId == R.id.chipDeptLevelPG) {
                selectedLevelFilter = "PG";
            } else {
                selectedLevelFilter = "ALL";
            }
            filterDepartments(etSearch.getText() != null ? etSearch.getText().toString() : "");
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterDepartments(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadDepartments() {
        showLoading(true);
        db.collection("departments").get().addOnSuccessListener(queryDocumentSnapshots -> {
            showLoading(false);
            swipeRefresh.setRefreshing(false);
            masterList.clear();

            if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    Department dept = doc.toObject(Department.class);
                    if (dept != null) {
                        dept.setDepartmentId(doc.getId());
                        if (doc.getString("shortName") != null) {
                            dept.setShortName(doc.getString("shortName"));
                        }
                        if (doc.getString("name") != null) {
                            dept.setName(doc.getString("name"));
                        }
                        if (doc.getString("programLevel") != null) {
                            dept.setProgramLevel(doc.getString("programLevel"));
                        }
                        masterList.add(dept);
                    }
                }
            }

            filterDepartments(etSearch.getText() != null ? etSearch.getText().toString() : "");
        }).addOnFailureListener(e -> {
            showLoading(false);
            swipeRefresh.setRefreshing(false);
            filterDepartments("");
        });
    }

    private void filterDepartments(String query) {
        filteredList.clear();
        String q = query != null ? query.trim().toLowerCase() : "";

        for (Department d : masterList) {
            String level = d.getProgramLevel() != null ? d.getProgramLevel().toUpperCase() : "UG";
            boolean matchesLevel = "ALL".equalsIgnoreCase(selectedLevelFilter) || selectedLevelFilter.equalsIgnoreCase(level);
            boolean matchesQuery = q.isEmpty() ||
                    d.getDepartmentName().toLowerCase().contains(q) ||
                    d.getDepartmentCode().toLowerCase().contains(q) ||
                    level.toLowerCase().contains(q);

            if (matchesLevel && matchesQuery) {
                filteredList.add(d);
            }
        }

        adapter.updateData(filteredList);

        if (filteredList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvDepts.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvDepts.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onEditClick(Department department) {
        showAddEditDialog(department);
    }

    @Override
    public void onToggleStatusClick(Department department) {
        if (department == null) return;
        String newStatus = "ACTIVE".equalsIgnoreCase(department.getStatus()) ? "INACTIVE" : "ACTIVE";
        department.setStatus(newStatus);

        showLoading(true);
        db.collection("departments").document(department.getDepartmentId()).update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    PortalActivityLogger.getInstance(this).logTeacherActivity(
                            "System Admin",
                            "Toggled Department Status: " + department.getDepartmentName(),
                            "Set status to " + newStatus,
                            "Department Management"
                    );
                    Toast.makeText(this, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                    filterDepartments(etSearch.getText() != null ? etSearch.getText().toString() : "");
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed to update status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    filterDepartments(etSearch.getText() != null ? etSearch.getText().toString() : "");
                });
    }

    private void showAddEditDialog(Department existingDept) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_department, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tvDeptDialogTitle);
        AutoCompleteTextView actvLevel = dialogView.findViewById(R.id.actvDeptProgramLevel);
        TextInputEditText etName = dialogView.findViewById(R.id.etDeptNameInput);
        TextInputEditText etCode = dialogView.findViewById(R.id.etDeptCodeInput);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelDept);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSaveDept);

        // Setup Program Level Dropdown
        String[] levels = new String[]{"UG", "PG"};
        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, levels);
        actvLevel.setAdapter(levelAdapter);

        boolean isEdit = existingDept != null;
        if (isEdit) {
            tvTitle.setText("Edit Department");
            actvLevel.setText(existingDept.getProgramLevel(), false);
            etName.setText(existingDept.getDepartmentName());
            etCode.setText(existingDept.getDepartmentCode());
            btnSave.setText("UPDATE");
        } else {
            actvLevel.setText("UG", false);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String rawLevel = actvLevel.getText() != null ? actvLevel.getText().toString().trim().toUpperCase() : "UG";
            final String level = "PG".equalsIgnoreCase(rawLevel) ? "PG" : "UG";
            final String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            final String code = etCode.getText() != null ? etCode.getText().toString().trim() : "";

            if (TextUtils.isEmpty(name)) {
                etName.setError("Department Name Required");
                etName.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(code)) {
                etCode.setError("Department Short Name / Code Required");
                etCode.requestFocus();
                return;
            }

            showLoading(true);
            if (isEdit) {
                existingDept.setProgramLevel(level);
                existingDept.setDepartmentName(name);
                existingDept.setDepartmentCode(code);
                existingDept.setName(name);
                existingDept.setShortName(code);

                db.collection("departments").document(existingDept.getDepartmentId()).set(existingDept)
                        .addOnSuccessListener(aVoid -> {
                            showLoading(false);
                            PortalActivityLogger.getInstance(this).logTeacherActivity(
                                    "System Admin",
                                    "Updated Department: " + name + " [" + level + "]",
                                    "Code: " + code,
                                    "Department Management"
                            );
                            Toast.makeText(this, "Department updated successfully", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            loadDepartments();
                        })
                        .addOnFailureListener(e -> {
                            showLoading(false);
                            Toast.makeText(this, "Failed to update department", Toast.LENGTH_SHORT).show();
                        });
            } else {
                String id = code.toLowerCase().replaceAll("[^a-z0-9]", "");
                Department newDept = new Department(id, name, code, level, "ACTIVE");
                db.collection("departments").document(id).set(newDept)
                        .addOnSuccessListener(aVoid -> {
                            showLoading(false);
                            PortalActivityLogger.getInstance(this).logTeacherActivity(
                                    "System Admin",
                                    "Created " + level + " Department: " + name,
                                    "Code: " + code,
                                    "Department Management"
                            );
                            Toast.makeText(this, "New " + level + " department created successfully!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            loadDepartments();
                        })
                        .addOnFailureListener(e -> {
                            showLoading(false);
                            Toast.makeText(this, "Failed to create department", Toast.LENGTH_SHORT).show();
                        });
            }
        });

        dialog.show();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
