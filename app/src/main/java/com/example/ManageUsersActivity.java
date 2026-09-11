package com.example;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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

import com.example.adapter.UserControlAdapter;
import com.example.database.DatabaseHelper;
import com.example.model.Student;
import com.example.model.Teacher;
import com.example.model.User;
import com.example.utils.PortalActivityLogger;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity for System Admin to manage user accounts, status (ACTIVE, INACTIVE, SUSPENDED),
 * and launch admin-controlled password management.
 */
public class ManageUsersActivity extends AppCompatActivity implements UserControlAdapter.OnUserActionListener {

    private static final int REQUEST_CHANGE_PASSWORD = 1001;

    private FirebaseFirestore db;
    private DatabaseHelper dbHelper;

    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupRole;
    private ChipGroup chipGroupStatus;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private RecyclerView rvUsers;

    private UserControlAdapter adapter;
    private List<User> masterUserList = new ArrayList<>();
    private List<User> filteredUserList = new ArrayList<>();

    private String selectedRoleFilter = "ALL";
    private String selectedStatusFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        db = FirebaseFirestore.getInstance();
        dbHelper = new DatabaseHelper(this);

        initViews();
        setupToolbar();
        setupWindowInsets();
        setupSwipeRefresh();
        setupRoleChips();
        setupStatusChips();
        setupRecyclerView();
        setupSearch();
        loadUsers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUsers();
    }

    private void setupWindowInsets() {
        View appBar = findViewById(R.id.appBarUsers);
        if (appBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, insets) -> {
                androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                v.setPadding(0, statusBarInsets.top, 0, 0);
                return insets;
            });
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarManageUsers);
        swipeRefresh = findViewById(R.id.swipeRefreshUsers);
        etSearch = findViewById(R.id.etSearchUsers);
        chipGroupRole = findViewById(R.id.chipGroupUserRole);
        chipGroupStatus = findViewById(R.id.chipGroupUserStatus);
        progressBar = findViewById(R.id.progressBarUsers);
        tvEmpty = findViewById(R.id.tvUsersEmpty);
        rvUsers = findViewById(R.id.rvUsersList);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Manage Users");
            getSupportActionBar().setSubtitle("Account Control & Password Management");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(this::loadUsers);
    }

    private void setupRoleChips() {
        chipGroupRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipRoleAdmin) {
                selectedRoleFilter = "ADMIN";
            } else if (checkedId == R.id.chipRoleTeacher) {
                selectedRoleFilter = "TEACHER";
            } else if (checkedId == R.id.chipRoleStudent) {
                selectedRoleFilter = "STUDENT";
            } else {
                selectedRoleFilter = "ALL";
            }
            applyFilters();
        });
    }

    private void setupStatusChips() {
        if (chipGroupStatus != null) {
            chipGroupStatus.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.chipStatusActive) {
                    selectedStatusFilter = "ACTIVE";
                } else if (checkedId == R.id.chipStatusInactive) {
                    selectedStatusFilter = "INACTIVE";
                } else if (checkedId == R.id.chipStatusSuspended) {
                    selectedStatusFilter = "SUSPENDED";
                } else {
                    selectedStatusFilter = "ALL";
                }
                applyFilters();
            });
        }
    }

    private void setupRecyclerView() {
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserControlAdapter(filteredUserList, this);
        rvUsers.setAdapter(adapter);
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

    private void loadUsers() {
        showLoading(true);
        db.collection("users").get().addOnSuccessListener(queryDocumentSnapshots -> {
            showLoading(false);
            swipeRefresh.setRefreshing(false);
            masterUserList.clear();

            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                User u = null;
                try {
                    u = doc.toObject(User.class);
                } catch (Exception ex) {
                    String name = doc.getString("name");
                    String email = doc.getString("email");
                    String role = doc.getString("role");
                    String idf = doc.getString("identifier");
                    String status = doc.getString("status");
                    u = new User(doc.getId(), name != null ? name : "", email != null ? email : "", "", role != null ? role : "STUDENT", idf != null ? idf : "", status != null ? status : "ACTIVE");
                }
                if (u != null) {
                    u.setUid(doc.getId());
                    masterUserList.add(u);
                }
            }

            applyFilters();
        }).addOnFailureListener(e -> {
            showLoading(false);
            swipeRefresh.setRefreshing(false);
            loadUsersFromSQLite();
        });
    }

    private void loadUsersFromSQLite() {
        masterUserList.clear();
        // Fallback to SQLite teachers & students
        List<Teacher> teachers = dbHelper.getAllTeachers();
        for (Teacher t : teachers) {
            masterUserList.add(new User(t.getId(), t.getName(), t.getEmail(), "", "TEACHER", String.valueOf(t.getId()), "ACTIVE"));
        }
        List<Student> students = dbHelper.getAllStudents();
        for (Student s : students) {
            masterUserList.add(new User(s.getId(), s.getName(), s.getEmail(), "", "STUDENT", s.getRegNo(), "ACTIVE"));
        }
        applyFilters();
    }

    private void applyFilters() {
        String query = etSearch.getText() != null ? etSearch.getText().toString().trim().toLowerCase() : "";
        filteredUserList.clear();

        for (User u : masterUserList) {
            boolean matchesRole = "ALL".equalsIgnoreCase(selectedRoleFilter) || selectedRoleFilter.equalsIgnoreCase(u.getRole());
            boolean matchesStatus = "ALL".equalsIgnoreCase(selectedStatusFilter) || selectedStatusFilter.equalsIgnoreCase(u.getStatus());
            boolean matchesQuery = query.isEmpty() ||
                    (u.getName() != null && u.getName().toLowerCase().contains(query)) ||
                    (u.getEmail() != null && u.getEmail().toLowerCase().contains(query)) ||
                    (u.getIdentifier() != null && u.getIdentifier().toLowerCase().contains(query));

            if (matchesRole && matchesStatus && matchesQuery) {
                filteredUserList.add(u);
            }
        }

        adapter.updateData(filteredUserList);

        if (filteredUserList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvUsers.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvUsers.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onViewClick(User user) {
        if (user == null) return;

        String role = user.getRole() != null ? user.getRole().toUpperCase() : "STUDENT";
        String info = String.format(
                "Name: %s\nEmail: %s\nRole: %s\nIdentifier: %s\nStatus: %s",
                user.getName(),
                user.getEmail(),
                role,
                user.getIdentifier() != null ? user.getIdentifier() : "N/A",
                user.getStatus() != null ? user.getStatus() : "ACTIVE"
        );

        new AlertDialog.Builder(this)
                .setTitle("User Profile Details")
                .setMessage(info)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public void onEditClick(User user) {
        if (user == null) return;
        String role = user.getRole() != null ? user.getRole().toUpperCase() : "STUDENT";

        if ("STUDENT".equalsIgnoreCase(role)) {
            Intent intent = new Intent(this, EditStudentActivity.class);
            intent.putExtra("studentId", user.getIdentifier());
            intent.putExtra("name", user.getName());
            intent.putExtra("registerNo", user.getIdentifier());
            intent.putExtra("email", user.getEmail());
            startActivity(intent);
        } else if ("TEACHER".equalsIgnoreCase(role)) {
            Intent intent = new Intent(this, ManageTeachersActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Admin account profile can be managed in Admin Settings.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onChangePasswordClick(User user) {
        if (user == null) return;

        Intent intent = new Intent(this, ChangeUserPasswordActivity.class);
        intent.putExtra(ChangeUserPasswordActivity.EXTRA_TARGET_UID, String.valueOf(user.getId()));
        intent.putExtra(ChangeUserPasswordActivity.EXTRA_TARGET_NAME, user.getName());
        intent.putExtra(ChangeUserPasswordActivity.EXTRA_TARGET_EMAIL, user.getEmail());
        intent.putExtra(ChangeUserPasswordActivity.EXTRA_TARGET_ROLE, user.getRole());
        intent.putExtra(ChangeUserPasswordActivity.EXTRA_TARGET_IDENTIFIER, user.getIdentifier());
        intent.putExtra(ChangeUserPasswordActivity.EXTRA_TARGET_STATUS, user.getStatus());
        startActivityForResult(intent, REQUEST_CHANGE_PASSWORD);
    }

    @Override
    public void onStatusChangeClick(User user) {
        if (user == null) return;

        String[] statuses = new String[]{"ACTIVE", "INACTIVE", "SUSPENDED"};
        int currentSelection = 0;
        for (int i = 0; i < statuses.length; i++) {
            if (statuses[i].equalsIgnoreCase(user.getStatus())) {
                currentSelection = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Change Status for " + user.getName())
                .setSingleChoiceItems(statuses, currentSelection, (dialog, which) -> {
                    String newStatus = statuses[which];
                    dialog.dismiss();
                    updateUserStatus(user, newStatus);
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void updateUserStatus(User user, String newStatus) {
        showLoading(true);
        user.setStatus(newStatus);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);

        db.collection("users").document(String.valueOf(user.getId())).update(updates)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    PortalActivityLogger.getInstance(this).logTeacherActivity(
                            "System Admin",
                            "Updated User Status: " + user.getName(),
                            "Set " + user.getRole() + " status to " + newStatus,
                            "User Account Control"
                    );
                    Toast.makeText(this, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Updated status to " + newStatus, Toast.LENGTH_SHORT).show();
                    applyFilters();
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
