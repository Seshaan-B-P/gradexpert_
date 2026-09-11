package com.example;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.repository.PasswordResetRepository;
import com.example.utils.PortalActivityLogger;
import com.example.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Activity for System Admin to review verified request credentials,
 * reject invalid requests, or launch ChangeUserPasswordActivity.
 */
public class PasswordResetRequestDetailsActivity extends AppCompatActivity {

    private static final int RC_CHANGE_PASSWORD = 2002;

    private String requestId = "";
    private String userId = "";
    private String userName = "";
    private String userEmail = "";
    private String userRole = "STUDENT";
    private String userIdentifier = "";
    private String requestStatus = "PENDING";
    private String adminNote = "";

    private Toolbar toolbar;
    private TextView tvRequesterName, tvRequesterEmail, tvRole, tvIdLabel, tvIdentifier, tvRequestId;
    private Chip chipStatus;
    private TextInputEditText etAdminNote;
    private ProgressBar progressBar;
    private LinearLayout layoutActions;
    private MaterialButton btnReject, btnChangePassword;

    private PasswordResetRepository repository;
    private PortalActivityLogger activityLogger;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_reset_request_details);

        repository = PasswordResetRepository.getInstance(this);
        activityLogger = PortalActivityLogger.getInstance(this);
        sessionManager = new SessionManager(this);

        extractIntentData();
        initViews();
        setupToolbar();
        setupWindowInsets();
        populateData();
        setupActions();
    }

    private void extractIntentData() {
        if (getIntent() != null) {
            requestId = getIntent().getStringExtra("extra_request_id");
            if (requestId == null) requestId = "";

            userId = getIntent().getStringExtra("extra_user_id");
            if (userId == null) userId = "";

            userName = getIntent().getStringExtra("extra_user_name");
            if (userName == null) userName = "User";

            userEmail = getIntent().getStringExtra("extra_user_email");
            if (userEmail == null) userEmail = "";

            userRole = getIntent().getStringExtra("extra_user_role");
            if (userRole == null) userRole = "STUDENT";

            userIdentifier = getIntent().getStringExtra("extra_user_identifier");
            if (userIdentifier == null) userIdentifier = "N/A";

            requestStatus = getIntent().getStringExtra("extra_status");
            if (requestStatus == null) requestStatus = "PENDING";

            adminNote = getIntent().getStringExtra("extra_admin_note");
            if (adminNote == null) adminNote = "";
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarRequestDetails);
        tvRequesterName = findViewById(R.id.tvDetailRequesterName);
        tvRequesterEmail = findViewById(R.id.tvDetailRequesterEmail);
        tvRole = findViewById(R.id.tvDetailRole);
        tvIdLabel = findViewById(R.id.tvDetailIdLabel);
        tvIdentifier = findViewById(R.id.tvDetailIdentifier);
        tvRequestId = findViewById(R.id.tvDetailRequestId);
        chipStatus = findViewById(R.id.chipDetailStatus);
        etAdminNote = findViewById(R.id.etAdminNote);
        progressBar = findViewById(R.id.progressBarDetails);
        layoutActions = findViewById(R.id.layoutActionButtons);
        btnReject = findViewById(R.id.btnRejectRequest);
        btnChangePassword = findViewById(R.id.btnChangePasswordAction);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Password Reset Request");
            getSupportActionBar().setSubtitle(userRole + " • " + userName);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupWindowInsets() {
        View appBar = findViewById(R.id.appBarRequestDetails);
        if (appBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, insets) -> {
                androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                v.setPadding(0, statusBarInsets.top, 0, 0);
                return insets;
            });
        }
    }

    private void populateData() {
        tvRequesterName.setText(userName);
        tvRequesterEmail.setText(userEmail);
        tvRole.setText(userRole.toUpperCase());

        if ("TEACHER".equalsIgnoreCase(userRole)) {
            tvIdLabel.setText("Employee ID:");
        } else {
            tvIdLabel.setText("Register Number:");
        }
        tvIdentifier.setText(userIdentifier);
        tvRequestId.setText(requestId);

        String upperStatus = requestStatus.toUpperCase();
        chipStatus.setText(upperStatus);

        if ("PENDING".equalsIgnoreCase(upperStatus)) {
            chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F59E0B"))); // Amber
            layoutActions.setVisibility(View.VISIBLE);
        } else if ("COMPLETED".equalsIgnoreCase(upperStatus)) {
            chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#10B981"))); // Emerald
            layoutActions.setVisibility(View.GONE);
        } else if ("REJECTED".equalsIgnoreCase(upperStatus)) {
            chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#EF4444"))); // Red
            layoutActions.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(adminNote)) {
            etAdminNote.setText(adminNote);
        }
    }

    private void setupActions() {
        btnReject.setOnClickListener(v -> promptRejectConfirmation());
        btnChangePassword.setOnClickListener(v -> launchChangePassword());
    }

    private void promptRejectConfirmation() {
        String note = etAdminNote.getText() != null ? etAdminNote.getText().toString().trim() : "";

        new AlertDialog.Builder(this)
                .setTitle("Reject Password Reset Request?")
                .setMessage("Are you sure you want to reject the password reset request for " + userName + " (" + userIdentifier + ")?")
                .setPositiveButton("REJECT", (dialog, which) -> executeReject(note))
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void executeReject(String note) {
        showLoading(true);
        String adminUid = sessionManager.getIdentifier();
        String adminName = sessionManager.getUserName() != null ? sessionManager.getUserName() : "System Admin";

        repository.updateRequestStatus(requestId, "REJECTED", adminUid, note, new PasswordResetRepository.OnOperationListener() {
            @Override
            public void onSuccess(String message) {
                showLoading(false);

                // Log audit activity
                activityLogger.logPasswordResetRejected(requestId, userId, userName, userRole, note, adminName);

                Toast.makeText(PasswordResetRequestDetailsActivity.this, "Request marked as REJECTED.", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                Toast.makeText(PasswordResetRequestDetailsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void launchChangePassword() {
        Intent intent = new Intent(this, ChangeUserPasswordActivity.class);
        intent.putExtra(ChangeUserPasswordActivity.EXTRA_TARGET_UID, userId);
        intent.putExtra(ChangeUserPasswordActivity.EXTRA_TARGET_NAME, userName);
        intent.putExtra(ChangeUserPasswordActivity.EXTRA_TARGET_EMAIL, userEmail);
        intent.putExtra(ChangeUserPasswordActivity.EXTRA_TARGET_ROLE, userRole);
        intent.putExtra(ChangeUserPasswordActivity.EXTRA_TARGET_IDENTIFIER, userIdentifier);
        intent.putExtra(ChangeUserPasswordActivity.EXTRA_TARGET_STATUS, "ACTIVE");
        intent.putExtra(ChangeUserPasswordActivity.EXTRA_ASSOCIATED_REQUEST_ID, requestId);
        startActivityForResult(intent, RC_CHANGE_PASSWORD);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_CHANGE_PASSWORD && resultCode == RESULT_OK) {
            Toast.makeText(this, "Password reset completed for " + userName, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        layoutActions.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}
