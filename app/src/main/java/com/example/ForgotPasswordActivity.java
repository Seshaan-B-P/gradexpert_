package com.example;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.model.PasswordResetRequest;
import com.example.model.User;
import com.example.repository.PasswordResetRepository;
import com.example.utils.PortalActivityLogger;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Activity allowing Students and Teachers to securely submit password reset requests to the System Admin.
 * 
 * Verifies matching Email + Register Number / Employee ID before request creation.
 * Prevents spamming duplicate pending requests.
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvHeaderTitle, tvHeaderSubtitle, tvRoleInstruction, tvBackToLogin;
    private ChipGroup chipGroupRole;

    private TextInputLayout tilIdentifier, tilEmail;
    private TextInputEditText etIdentifier, etEmail;

    private ProgressBar progressBarReset;
    private MaterialButton btnResetPassword;

    private PasswordResetRepository resetRepository;
    private PortalActivityLogger activityLogger;

    private String selectedRole = "STUDENT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        resetRepository = PasswordResetRepository.getInstance(this);
        activityLogger = PortalActivityLogger.getInstance(this);

        initViews();
        setupListeners();
        updateRoleFields();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvHeaderSubtitle = findViewById(R.id.tvHeaderSubtitle);
        tvRoleInstruction = findViewById(R.id.tvRoleInstruction);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        chipGroupRole = findViewById(R.id.chipGroupRole);
        tilIdentifier = findViewById(R.id.tilIdentifier);
        tilEmail = findViewById(R.id.tilEmail);
        etIdentifier = findViewById(R.id.etIdentifier);
        etEmail = findViewById(R.id.etEmail);

        progressBarReset = findViewById(R.id.progressBarReset);
        btnResetPassword = findViewById(R.id.btnResetPassword);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        tvBackToLogin.setOnClickListener(v -> finish());

        chipGroupRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipRoleTeacher) {
                selectedRole = "TEACHER";
            } else {
                selectedRole = "STUDENT";
            }
            updateRoleFields();
        });

        btnResetPassword.setOnClickListener(v -> attemptSubmitRequest());
    }

    private void updateRoleFields() {
        tilIdentifier.setError(null);
        tilEmail.setError(null);

        if ("TEACHER".equalsIgnoreCase(selectedRole)) {
            tilIdentifier.setHint("Employee ID (e.g. EMP102)");
            tvRoleInstruction.setText("Enter your Employee ID and registered institutional email to verify faculty identity.");
        } else {
            tilIdentifier.setHint("Register Number (e.g. MCA001)");
            tvRoleInstruction.setText("Enter your Register Number and registered institutional email to verify student identity.");
        }
    }

    private void attemptSubmitRequest() {
        String identifier = etIdentifier.getText() != null ? etIdentifier.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        String idLabel = "TEACHER".equalsIgnoreCase(selectedRole) ? "Employee ID" : "Register Number";

        if (TextUtils.isEmpty(identifier)) {
            tilIdentifier.setError(idLabel + " is required.");
            etIdentifier.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email address is required.");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Please enter a valid email address.");
            etEmail.requestFocus();
            return;
        }

        if (!resetRepository.isNetworkAvailable()) {
            new AlertDialog.Builder(ForgotPasswordActivity.this)
                    .setTitle("Connection Required")
                    .setMessage("Internet connection required to submit password reset request.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        showLoading(true);

        // Step 1: Verify identity against Firestore users collection / local DB
        resetRepository.verifyIdentity(selectedRole, identifier, email, new PasswordResetRepository.OnUserVerifiedListener() {
            @Override
            public void onVerified(User user) {
                // Step 2: Check if user already has an active PENDING request
                checkPendingAndCreateRequest(user, identifier, email);
            }

            @Override
            public void onInvalid(String errorMessage) {
                showLoading(false);
                new AlertDialog.Builder(ForgotPasswordActivity.this)
                        .setTitle("Verification Failed")
                        .setMessage(errorMessage)
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    private void checkPendingAndCreateRequest(User user, String identifier, String email) {
        resetRepository.checkPendingRequest(identifier, email, new PasswordResetRepository.OnCheckPendingListener() {
            @Override
            public void onResult(boolean isPending, PasswordResetRequest existingRequest) {
                if (isPending) {
                    showLoading(false);
                    new AlertDialog.Builder(ForgotPasswordActivity.this)
                            .setTitle("Request Already Pending")
                            .setMessage("Your password reset request is already pending.")
                            .setPositiveButton("OK", (dialog, which) -> finish())
                            .show();
                } else {
                    createNewResetRequest(user, identifier, email);
                }
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                Toast.makeText(ForgotPasswordActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void createNewResetRequest(User user, String identifier, String email) {
        String userId = String.valueOf(user.getId());
        String userName = user.getName() != null ? user.getName() : identifier;

        PasswordResetRequest request = new PasswordResetRequest(
                "",
                userId,
                userName,
                email,
                selectedRole,
                identifier,
                user.getDepartment() != null ? user.getDepartment() : "",
                user.getSemester() != null ? user.getSemester() : ""
        );

        resetRepository.submitRequest(request, new PasswordResetRepository.OnOperationListener() {
            @Override
            public void onSuccess(String message) {
                showLoading(false);

                // Log audit activity
                activityLogger.logPasswordResetRequested(
                        request.getRequestId(),
                        userId,
                        userName,
                        selectedRole,
                        identifier
                );

                Toast.makeText(ForgotPasswordActivity.this, "Password reset request sent to Admin.", Toast.LENGTH_LONG).show();

                new AlertDialog.Builder(ForgotPasswordActivity.this)
                        .setTitle("Request Submitted")
                        .setMessage("Password reset request sent to Admin.")
                        .setPositiveButton("OK", (dialog, which) -> finish())
                        .setCancelable(false)
                        .show();
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                new AlertDialog.Builder(ForgotPasswordActivity.this)
                        .setTitle("Submission Failed")
                        .setMessage(errorMessage)
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBarReset.setVisibility(show ? View.VISIBLE : View.GONE);
        btnResetPassword.setEnabled(!show);
        etIdentifier.setEnabled(!show);
        etEmail.setEnabled(!show);
    }
}
