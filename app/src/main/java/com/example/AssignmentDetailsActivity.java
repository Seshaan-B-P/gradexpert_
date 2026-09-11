package com.example;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.model.AssignmentSubmission;
import com.example.repository.AssignmentRepository;
import com.example.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Activity for Students to view Assignment Details and submit solutions.
 */
public class AssignmentDetailsActivity extends AppCompatActivity {

    private static final String TAG = "AssignmentDetailsAct";

    private AssignmentRepository repository;
    private SessionManager sessionManager;

    private Toolbar toolbar;
    private TextView tvSubject;
    private TextView tvTitle;
    private TextView tvDeadline;
    private TextView tvDesc;
    private Chip chipStatus;
    private Chip chipAttachment;

    private TextInputEditText etAnswer;
    private MaterialButton btnPickFile;
    private Chip chipStudentFile;
    private LinearLayout layoutGradeResult;
    private TextView tvScore;
    private TextView tvFeedback;
    private MaterialButton btnSubmit;
    private ProgressBar progressBar;

    private String assignmentId = "";
    private String assignmentTitle = "";
    private String subjectName = "";
    private String description = "";
    private String dueDateStr = "";
    private String dueTimeStr = "";
    private double maxMarks = 10;
    private String attachmentName = "";
    private String assignmentStatus = "PUBLISHED";

    private String currentStudentId = "1";
    private String attachedFileUrl = "";
    private String attachedFileName = "";

    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    attachedFileName = getFileNameFromUri(uri);
                    attachedFileUrl = uri.toString();
                    chipStudentFile.setText(attachedFileName);
                    chipStudentFile.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Attachment selected: " + attachedFileName, Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assignment_details);

        repository = AssignmentRepository.getInstance(this);
        sessionManager = new SessionManager(this);

        int sessionUserId = sessionManager.getUserId();
        if (sessionUserId > 0) {
            currentStudentId = String.valueOf(sessionUserId);
        }

        readIntentExtras();
        initViews();
        setupToolbar();
        setupWindowInsets();
        populateAssignmentData();
        checkExistingSubmission();
    }

    private void readIntentExtras() {
        if (getIntent() != null) {
            assignmentId = getIntent().getStringExtra("assignmentId");
            assignmentTitle = getIntent().getStringExtra("title");
            subjectName = getIntent().getStringExtra("subjectName");
            description = getIntent().getStringExtra("description");
            dueDateStr = getIntent().getStringExtra("dueDate");
            dueTimeStr = getIntent().getStringExtra("dueTime");
            maxMarks = getIntent().getDoubleExtra("maxMarks", 10.0);
            attachmentName = getIntent().getStringExtra("attachmentName");
            assignmentStatus = getIntent().getStringExtra("status");
        }
    }

    private void setupWindowInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbarDetails), (v, insets) -> {
            androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
            v.setPadding(0, statusBarInsets.top, 0, 0);
            return insets;
        });
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarDetails);
        tvSubject = findViewById(R.id.tvDetailSubject);
        tvTitle = findViewById(R.id.tvDetailTitle);
        tvDeadline = findViewById(R.id.tvDetailDeadline);
        tvDesc = findViewById(R.id.tvDetailDesc);
        chipStatus = findViewById(R.id.chipDetailStatus);
        chipAttachment = findViewById(R.id.chipDetailAttachment);

        etAnswer = findViewById(R.id.etStudentAnswer);
        btnPickFile = findViewById(R.id.btnPickStudentFile);
        chipStudentFile = findViewById(R.id.chipStudentFile);
        layoutGradeResult = findViewById(R.id.layoutGradeFeedbackResult);
        tvScore = findViewById(R.id.tvGradedScore);
        tvFeedback = findViewById(R.id.tvGradedFeedback);
        btnSubmit = findViewById(R.id.btnSubmitAssignment);
        progressBar = findViewById(R.id.progressBarDetails);

        btnPickFile.setOnClickListener(v -> filePickerLauncher.launch("*/*"));
        btnSubmit.setOnClickListener(v -> submitSolution());
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void populateAssignmentData() {
        tvSubject.setText(subjectName != null ? subjectName : "Subject");
        tvTitle.setText(assignmentTitle != null ? assignmentTitle : "Assignment");
        tvDesc.setText(description != null ? description : "No description provided.");
        tvDeadline.setText("Due: " + (dueDateStr != null ? dueDateStr : "N/A") + " " + (dueTimeStr != null ? dueTimeStr : "") + " • Max Marks: " + (int) maxMarks);

        if (attachmentName != null && !attachmentName.isEmpty()) {
            chipAttachment.setText("Attachment: " + attachmentName);
            chipAttachment.setVisibility(View.VISIBLE);
        } else {
            chipAttachment.setVisibility(View.GONE);
        }
    }

    private void checkExistingSubmission() {
        if (assignmentId == null || assignmentId.isEmpty()) return;

        showLoading(true);
        repository.checkStudentSubmission(currentStudentId, assignmentId, new AssignmentRepository.OnStudentSubmissionCheckListener() {
            @Override
            public void onChecked(AssignmentSubmission existingSub) {
                showLoading(false);
                if (existingSub != null) {
                    etAnswer.setText(existingSub.getAnswer());
                    if (existingSub.getAttachmentUrl() != null && !existingSub.getAttachmentUrl().isEmpty()) {
                        chipStudentFile.setText("Submitted File: " + existingSub.getAttachmentUrl());
                        chipStudentFile.setVisibility(View.VISIBLE);
                    }

                    String subStatus = existingSub.getStatus();
                    chipStatus.setText(subStatus);

                    if ("GRADED".equalsIgnoreCase(subStatus)) {
                        layoutGradeResult.setVisibility(View.VISIBLE);
                        tvScore.setText("Marks: " + existingSub.getMarks() + " / " + maxMarks);
                        tvFeedback.setText("Feedback: " + (existingSub.getFeedback() != null ? existingSub.getFeedback() : "No feedback."));
                        btnSubmit.setEnabled(false);
                        btnSubmit.setText("ASSIGNMENT GRADED");
                    } else {
                        btnSubmit.setText("RESUBMIT ASSIGNMENT");
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
            }
        });
    }

    private void submitSolution() {
        if ("CLOSED".equalsIgnoreCase(assignmentStatus)) {
            Toast.makeText(this, "This assignment is closed for submissions.", Toast.LENGTH_LONG).show();
            return;
        }

        String answer = etAnswer.getText() != null ? etAnswer.getText().toString().trim() : "";
        if (answer.isEmpty() && attachedFileName.isEmpty()) {
            Toast.makeText(this, "Please enter your answer text or attach a solution file.", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        btnSubmit.setEnabled(false);

        String subStatus = "SUBMITTED";
        // Check deadline
        Calendar now = Calendar.getInstance();

        String studentName = sessionManager.getUserName();
        if (studentName == null || studentName.isEmpty()) studentName = "Student";
        String regNo = sessionManager.getIdentifier();
        if (regNo == null || regNo.isEmpty()) regNo = "REG" + currentStudentId;

        AssignmentSubmission sub = new AssignmentSubmission(
                "sub_" + currentStudentId + "_" + assignmentId,
                assignmentId,
                currentStudentId,
                studentName,
                regNo,
                answer,
                attachedFileName.isEmpty() ? attachedFileUrl : attachedFileName,
                subStatus
        );

        repository.submitAssignment(sub, new AssignmentRepository.OnAssignmentOperationListener() {
            @Override
            public void onSuccess(String message) {
                showLoading(false);
                btnSubmit.setEnabled(true);
                Toast.makeText(AssignmentDetailsActivity.this, "Assignment submitted successfully!", Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                btnSubmit.setEnabled(true);
                Toast.makeText(AssignmentDetailsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private String getFileNameFromUri(Uri uri) {
        String path = uri.getPath();
        if (path != null) {
            int cut = path.lastIndexOf('/');
            if (cut != -1) {
                return path.substring(cut + 1);
            }
        }
        return "student_solution.pdf";
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
