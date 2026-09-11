package com.example;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adapter.TeacherAssignmentAdapter;
import com.example.model.Assignment;
import com.example.repository.AssignmentRepository;
import com.example.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Teacher Activity for managing, publishing, filtering, closing, and deleting course assignments in Firestore.
 */
public class TeacherAssignmentActivity extends AppCompatActivity implements TeacherAssignmentAdapter.OnAssignmentActionListener {

    private static final String TAG = "TeacherAssignmentAct";

    private AssignmentRepository repository;
    private SessionManager sessionManager;

    private Toolbar toolbar;
    private MaterialButton btnNavCreateAssign;
    private ChipGroup chipGroupStatus;
    private TextInputEditText etSearch;

    private ProgressBar progressBar;
    private LinearLayout layoutEmpty;
    private TextView tvEmptyMessage;

    private RecyclerView rvAssignments;
    private TeacherAssignmentAdapter adapter;
    private List<Assignment> loadedAssignments = new ArrayList<>();

    private String currentStatusFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_assignment);

        repository = AssignmentRepository.getInstance(this);
        sessionManager = new SessionManager(this);

        initViews();
        setupToolbar();
        setupWindowInsets();
        setupCreateButton();
        setupFilterChips();
        setupSearch();
        setupRecyclerView();

        loadTeacherAssignments();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTeacherAssignments();
    }

    private void setupWindowInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbarTeacherAssign), (v, insets) -> {
            androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
            v.setPadding(0, statusBarInsets.top, 0, 0);
            return insets;
        });
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarTeacherAssign);
        btnNavCreateAssign = findViewById(R.id.btnNavCreateAssign);
        chipGroupStatus = findViewById(R.id.chipGroupTeacherStatus);
        etSearch = findViewById(R.id.etSearchAssignTeacher);

        progressBar = findViewById(R.id.progressBarTeacherAssign);
        layoutEmpty = findViewById(R.id.layoutEmptyTeacherAssign);
        tvEmptyMessage = findViewById(R.id.tvEmptyTeacherAssignMessage);

        rvAssignments = findViewById(R.id.rvTeacherAssignments);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupCreateButton() {
        btnNavCreateAssign.setOnClickListener(v -> {
            Intent intent = new Intent(TeacherAssignmentActivity.this, CreateAssignmentActivity.class);
            startActivity(intent);
        });
    }

    private void setupFilterChips() {
        chipGroupStatus.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipFilterTeacherDraft) {
                currentStatusFilter = "DRAFT";
            } else if (checkedId == R.id.chipFilterTeacherPublished) {
                currentStatusFilter = "PUBLISHED";
            } else if (checkedId == R.id.chipFilterTeacherClosed) {
                currentStatusFilter = "CLOSED";
            } else {
                currentStatusFilter = "ALL";
            }
            applyFilters();
        });
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

    private void setupRecyclerView() {
        rvAssignments.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TeacherAssignmentAdapter(new ArrayList<>(), this);
        rvAssignments.setAdapter(adapter);
    }

    private void loadTeacherAssignments() {
        showLoading(true);
        String teacherUid = getTeacherIdentity();

        repository.fetchTeacherAssignments(teacherUid, "ALL", new AssignmentRepository.OnAssignmentsLoadedListener() {
            @Override
            public void onSuccess(List<Assignment> assignments) {
                showLoading(false);
                loadedAssignments = assignments;
                adapter.updateData(assignments);
                applyFilters();
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                showEmptyState("No assignments created yet.");
            }
        });
    }

    private void applyFilters() {
        String query = etSearch.getText() != null ? etSearch.getText().toString() : "";
        if (adapter != null) {
            adapter.filter(query, currentStatusFilter);

            if (adapter.getItemCount() == 0) {
                showEmptyState("No assignments match your criteria.");
            } else {
                hideEmptyState();
            }
        }
    }

    @Override
    public void onViewSubmissions(Assignment assignment) {
        Intent intent = new Intent(this, AssignmentSubmissionsActivity.class);
        intent.putExtra("assignmentId", assignment.getAssignmentId());
        intent.putExtra("assignmentTitle", assignment.getTitle());
        intent.putExtra("maxMarks", assignment.getMaxMarks());
        startActivity(intent);
    }

    @Override
    public void onPublish(Assignment assignment) {
        new AlertDialog.Builder(this)
                .setTitle("Publish Assignment?")
                .setMessage("Are you sure you want to publish \"" + assignment.getTitle() + "\"? It will become visible to all enrolled students.")
                .setPositiveButton("PUBLISH", (dialog, which) -> {
                    showLoading(true);
                    repository.updateAssignmentStatus(assignment.getAssignmentId(), "PUBLISHED", new AssignmentRepository.OnAssignmentOperationListener() {
                        @Override
                        public void onSuccess(String message) {
                            showLoading(false);
                            Toast.makeText(TeacherAssignmentActivity.this, "Assignment published successfully.", Toast.LENGTH_SHORT).show();
                            loadTeacherAssignments();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            showLoading(false);
                            Toast.makeText(TeacherAssignmentActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    @Override
    public void onClose(Assignment assignment) {
        new AlertDialog.Builder(this)
                .setTitle("Close Assignment?")
                .setMessage("Closing \"" + assignment.getTitle() + "\" will prevent students from submitting further work.")
                .setPositiveButton("CLOSE", (dialog, which) -> {
                    showLoading(true);
                    repository.updateAssignmentStatus(assignment.getAssignmentId(), "CLOSED", new AssignmentRepository.OnAssignmentOperationListener() {
                        @Override
                        public void onSuccess(String message) {
                            showLoading(false);
                            Toast.makeText(TeacherAssignmentActivity.this, "Assignment closed.", Toast.LENGTH_SHORT).show();
                            loadTeacherAssignments();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            showLoading(false);
                            Toast.makeText(TeacherAssignmentActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    @Override
    public void onDelete(Assignment assignment) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Assignment?")
                .setMessage("This action cannot be undone.")
                .setPositiveButton("DELETE", (dialog, which) -> {
                    showLoading(true);
                    repository.deleteAssignment(assignment.getAssignmentId(), new AssignmentRepository.OnAssignmentOperationListener() {
                        @Override
                        public void onSuccess(String message) {
                            showLoading(false);
                            Toast.makeText(TeacherAssignmentActivity.this, "Assignment deleted.", Toast.LENGTH_SHORT).show();
                            loadTeacherAssignments();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            showLoading(false);
                            Toast.makeText(TeacherAssignmentActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private String getTeacherIdentity() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            return currentUser.getUid();
        }
        String sessionEmail = sessionManager.getUserEmail();
        return (sessionEmail != null && !sessionEmail.isEmpty()) ? sessionEmail : "TCH1001";
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showEmptyState(String message) {
        tvEmptyMessage.setText(message);
        layoutEmpty.setVisibility(View.VISIBLE);
        rvAssignments.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        layoutEmpty.setVisibility(View.GONE);
        rvAssignments.setVisibility(View.VISIBLE);
    }
}
