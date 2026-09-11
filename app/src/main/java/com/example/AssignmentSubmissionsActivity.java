package com.example;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adapter.SubmissionAdapter;
import com.example.model.AssignmentSubmission;
import com.example.repository.AssignmentRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for Teachers to review student submissions, grade answers, and record feedback in Firestore.
 */
public class AssignmentSubmissionsActivity extends AppCompatActivity implements SubmissionAdapter.OnSaveGradeListener {

    private static final String TAG = "AssignSubmissionsAct";

    private AssignmentRepository repository;
    private Toolbar toolbar;
    private TextView tvHeaderTitle;
    private TextView tvHeaderCount;
    private ProgressBar progressBar;
    private LinearLayout layoutEmpty;
    private RecyclerView rvSubmissions;
    private SubmissionAdapter adapter;

    private String assignmentId = "";
    private String assignmentTitle = "Assignment Submissions";
    private double maxMarks = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assignment_submissions);

        repository = AssignmentRepository.getInstance(this);

        if (getIntent() != null) {
            assignmentId = getIntent().getStringExtra("assignmentId");
            if (getIntent().hasExtra("assignmentTitle")) {
                assignmentTitle = getIntent().getStringExtra("assignmentTitle");
            }
            maxMarks = getIntent().getDoubleExtra("maxMarks", 10.0);
        }

        initViews();
        setupToolbar();
        setupWindowInsets();
        setupRecyclerView();
        loadSubmissionsFromFirestore();
    }

    private void setupWindowInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbarSubmissions), (v, insets) -> {
            androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
            v.setPadding(0, statusBarInsets.top, 0, 0);
            return insets;
        });
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarSubmissions);
        tvHeaderTitle = findViewById(R.id.tvSubHeaderTitle);
        tvHeaderCount = findViewById(R.id.tvSubHeaderCount);
        progressBar = findViewById(R.id.progressBarSubmissions);
        layoutEmpty = findViewById(R.id.layoutEmptySubmissions);
        rvSubmissions = findViewById(R.id.rvSubmissions);

        tvHeaderTitle.setText(assignmentTitle);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        rvSubmissions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SubmissionAdapter(new ArrayList<>(), maxMarks, this);
        rvSubmissions.setAdapter(adapter);
    }

    private void loadSubmissionsFromFirestore() {
        if (assignmentId == null || assignmentId.isEmpty()) {
            showEmptyState();
            return;
        }

        showLoading(true);
        repository.fetchSubmissionsForAssignment(assignmentId, new AssignmentRepository.OnSubmissionsLoadedListener() {
            @Override
            public void onSuccess(List<AssignmentSubmission> submissions) {
                showLoading(false);
                tvHeaderCount.setText("Submissions Received: " + submissions.size());

                if (submissions.isEmpty()) {
                    showEmptyState();
                } else {
                    hideEmptyState();
                    adapter.updateList(submissions, maxMarks);
                }
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                showEmptyState();
                Toast.makeText(AssignmentSubmissionsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onSaveGrade(AssignmentSubmission submission, double marks, String feedback) {
        showLoading(true);
        repository.gradeSubmission(submission.getSubmissionId(), marks, feedback, new AssignmentRepository.OnAssignmentOperationListener() {
            @Override
            public void onSuccess(String message) {
                showLoading(false);
                Toast.makeText(AssignmentSubmissionsActivity.this, "Grade & feedback saved successfully.", Toast.LENGTH_LONG).show();
                loadSubmissionsFromFirestore();
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                Toast.makeText(AssignmentSubmissionsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showEmptyState() {
        layoutEmpty.setVisibility(View.VISIBLE);
        rvSubmissions.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        layoutEmpty.setVisibility(View.GONE);
        rvSubmissions.setVisibility(View.VISIBLE);
    }
}
