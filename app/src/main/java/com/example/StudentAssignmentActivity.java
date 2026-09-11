package com.example;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adapter.StudentAssignmentAdapter;
import com.example.database.DatabaseHelper;
import com.example.model.Assignment;
import com.example.repository.AssignmentRepository;
import com.example.utils.SessionManager;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Student Activity displaying PUBLISHED assignments from Firebase Firestore.
 */
public class StudentAssignmentActivity extends AppCompatActivity {

    private static final String TAG = "StudentAssignmentAct";

    private AssignmentRepository repository;
    private SessionManager sessionManager;
    private DatabaseHelper dbHelper;

    private Toolbar toolbar;
    private TextView tvTotalAssignCount;
    private TextView tvSubmittedCount;
    private TextView tvPendingCount;

    private ChipGroup chipGroupStatus;
    private RecyclerView rvStudentAssignments;
    private ProgressBar progressBar;

    private StudentAssignmentAdapter assignmentAdapter;
    private List<Assignment> allAssignments = new ArrayList<>();

    private String currentStudentId = "1";
    private String studentDept = "Master of Computer Applications";
    private String studentSem = "Semester III";
    private String currentStatusFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_assignment);

        repository = AssignmentRepository.getInstance(this);
        sessionManager = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);

        int sessionUserId = sessionManager.getUserId();
        if (sessionUserId > 0) {
            currentStudentId = String.valueOf(sessionUserId);
        }

        initViews();
        setupToolbar();
        setupWindowInsets();
        setupFilters();
        setupRecyclerView();
        loadPublishedAssignmentsFromFirestore();
    }

    private void setupWindowInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbarStudentAssign), (v, insets) -> {
            androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
            v.setPadding(0, statusBarInsets.top, 0, 0);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPublishedAssignmentsFromFirestore();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarStudentAssign);
        tvTotalAssignCount = findViewById(R.id.tvTotalAssignCount);
        tvSubmittedCount = findViewById(R.id.tvSubmittedCount);
        tvPendingCount = findViewById(R.id.tvPendingCount);

        chipGroupStatus = findViewById(R.id.chipGroupStatus);
        rvStudentAssignments = findViewById(R.id.rvStudentAssignments);

        rvStudentAssignments.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupFilters() {
        chipGroupStatus.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipFilterPending) {
                currentStatusFilter = "PENDING";
            } else if (checkedId == R.id.chipFilterSubmitted) {
                currentStatusFilter = "SUBMITTED";
            } else {
                currentStatusFilter = "ALL";
            }
            filterAssignments();
        });
    }

    private void setupRecyclerView() {
        assignmentAdapter = new StudentAssignmentAdapter(new ArrayList<>(), new StudentAssignmentAdapter.OnStudentAssignmentClickListener() {
            @Override
            public void onViewTeacherPdf(Assignment assignment) {
                openAssignmentDetails(assignment);
            }

            @Override
            public void onSubmitSolution(Assignment assignment) {
                openAssignmentDetails(assignment);
            }
        });
        rvStudentAssignments.setAdapter(assignmentAdapter);
    }

    private void loadPublishedAssignmentsFromFirestore() {
        repository.fetchStudentAssignments(studentDept, studentSem, new AssignmentRepository.OnAssignmentsLoadedListener() {
            @Override
            public void onSuccess(List<Assignment> assignments) {
                allAssignments = assignments;
                updateSummaryHeader();
                filterAssignments();
            }

            @Override
            public void onError(String errorMessage) {
                Log.w(TAG, "Firestore fetch failed: " + errorMessage + ". Using local SQLite fallback.");
                loadSQLiteFallback();
            }
        });
    }

    private void loadSQLiteFallback() {
        int studentId = sessionManager.getUserId();
        if (studentId <= 0) studentId = 1;
        allAssignments = dbHelper.getAssignmentsForStudent(studentId);
        updateSummaryHeader();
        filterAssignments();
    }

    private void updateSummaryHeader() {
        int total = allAssignments.size();
        int submitted = 0;
        int pending = 0;

        for (Assignment a : allAssignments) {
            if ("SUBMITTED".equalsIgnoreCase(a.getStatus()) || "GRADED".equalsIgnoreCase(a.getStatus()) || "LATE".equalsIgnoreCase(a.getStatus())) {
                submitted++;
            } else {
                pending++;
            }
        }

        tvTotalAssignCount.setText(String.valueOf(total));
        tvSubmittedCount.setText(String.valueOf(submitted));
        tvPendingCount.setText(String.valueOf(pending));
    }

    private void filterAssignments() {
        List<Assignment> filtered = new ArrayList<>();
        for (Assignment a : allAssignments) {
            if ("PENDING".equalsIgnoreCase(currentStatusFilter)) {
                if (!"SUBMITTED".equalsIgnoreCase(a.getStatus()) && !"GRADED".equalsIgnoreCase(a.getStatus()) && !"LATE".equalsIgnoreCase(a.getStatus())) {
                    filtered.add(a);
                }
            } else if ("SUBMITTED".equalsIgnoreCase(currentStatusFilter)) {
                if ("SUBMITTED".equalsIgnoreCase(a.getStatus()) || "GRADED".equalsIgnoreCase(a.getStatus()) || "LATE".equalsIgnoreCase(a.getStatus())) {
                    filtered.add(a);
                }
            } else {
                filtered.add(a);
            }
        }

        if (assignmentAdapter != null) {
            assignmentAdapter.updateList(filtered);
        }
    }

    private void openAssignmentDetails(Assignment assignment) {
        Intent intent = new Intent(this, AssignmentDetailsActivity.class);
        intent.putExtra("assignmentId", assignment.getAssignmentId());
        intent.putExtra("title", assignment.getTitle());
        intent.putExtra("subjectName", assignment.getSubjectName());
        intent.putExtra("description", assignment.getDescription());
        if (assignment.getDueDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.US);
            intent.putExtra("dueDate", sdf.format(assignment.getDueDate().toDate()));
        }
        intent.putExtra("dueTime", assignment.getDueTime());
        intent.putExtra("maxMarks", assignment.getMaxMarks());
        intent.putExtra("attachmentName", assignment.getAttachmentName());
        intent.putExtra("status", assignment.getStatus());
        startActivity(intent);
    }
}
