package com.example;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.database.DatabaseHelper;
import com.example.utils.AdminAuthService;
import com.example.utils.PortalActivityLogger;
import com.example.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Admin-controlled password management screen.
 * Allows System Admin to set, change, or reset passwords for Student and Teacher accounts
 * via secure Firebase Cloud Functions without exposing plaintext passwords.
 */
public class ChangeUserPasswordActivity extends AppCompatActivity {

    public static final String EXTRA_TARGET_UID = "extra_target_uid";
    public static final String EXTRA_TARGET_NAME = "extra_target_name";
    public static final String EXTRA_TARGET_EMAIL = "extra_target_email";
    public static final String EXTRA_TARGET_ROLE = "extra_target_role";
    public static final String EXTRA_TARGET_IDENTIFIER = "extra_target_identifier";
    public static final String EXTRA_TARGET_STATUS = "extra_target_status";
    public static final String EXTRA_ASSOCIATED_REQUEST_ID = "extra_associated_request_id";

    private String targetUid = "";
    private String targetName = "User";
    private String targetEmail = "";
    private String targetRole = "STUDENT";
    private String targetIdentifier = "";
    private String targetStatus = "ACTIVE";
    private String associatedRequestId = "";

    private Toolbar toolbar;
    private TextView tvTargetUserName, tvTargetUserEmail, tvTargetUserIdentifier, tvTargetUserStatus;
    private Chip chipTargetUserRole;

    private TextInputLayout tilNewPassword, tilConfirmPassword;
    private TextInputEditText etNewPassword, etConfirmPassword;

    private LinearLayout layoutPasswordStrength;
    private ProgressBar pbPasswordStrength;
    private TextView tvPasswordStrength;

    private LinearLayout layoutLoadingState;
    private MaterialButton btnCancel, btnUpdatePassword;

    private SessionManager sessionManager;
    private DatabaseHelper dbHelper;
    private PortalActivityLogger activityLogger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_user_password);

        sessionManager = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);
        activityLogger = PortalActivityLogger.getInstance(this);

        extractIntentData();
        initViews();
        setupToolbar();
        setupWindowInsets();
        populateTargetUserInfo();
        setupPasswordValidationListeners();
        setupActions();
    }

    private void extractIntentData() {
        if (getIntent() != null) {
            targetUid = getIntent().getStringExtra(EXTRA_TARGET_UID);
            if (targetUid == null) targetUid = "";

            String name = getIntent().getStringExtra(EXTRA_TARGET_NAME);
            if (name != null && !name.isEmpty()) targetName = name;

            String email = getIntent().getStringExtra(EXTRA_TARGET_EMAIL);
            if (email != null && !email.isEmpty()) targetEmail = email;

            String role = getIntent().getStringExtra(EXTRA_TARGET_ROLE);
            if (role != null && !role.isEmpty()) targetRole = role;

            String id = getIntent().getStringExtra(EXTRA_TARGET_IDENTIFIER);
            if (id != null && !id.isEmpty()) targetIdentifier = id;

            String status = getIntent().getStringExtra(EXTRA_TARGET_STATUS);
            if (status != null && !status.isEmpty()) targetStatus = status;

            String reqId = getIntent().getStringExtra(EXTRA_ASSOCIATED_REQUEST_ID);
            if (reqId != null && !reqId.isEmpty()) associatedRequestId = reqId;
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarChangePassword);
        tvTargetUserName = findViewById(R.id.tvTargetUserName);
        tvTargetUserEmail = findViewById(R.id.tvTargetUserEmail);
        tvTargetUserIdentifier = findViewById(R.id.tvTargetUserIdentifier);
        tvTargetUserStatus = findViewById(R.id.tvTargetUserStatus);
        chipTargetUserRole = findViewById(R.id.chipTargetUserRole);

        tilNewPassword = findViewById(R.id.tilNewPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        layoutPasswordStrength = findViewById(R.id.layoutPasswordStrength);
        pbPasswordStrength = findViewById(R.id.pbPasswordStrength);
        tvPasswordStrength = findViewById(R.id.tvPasswordStrength);

        layoutLoadingState = findViewById(R.id.layoutLoadingState);
        btnCancel = findViewById(R.id.btnCancelChangePassword);
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Change User Password");
            getSupportActionBar().setSubtitle(targetRole + " Account • " + targetName);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupWindowInsets() {
        View appBar = findViewById(R.id.appBarChangePassword);
        if (appBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, insets) -> {
                androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                v.setPadding(0, statusBarInsets.top, 0, 0);
                return insets;
            });
        }
    }

    private void populateTargetUserInfo() {
        tvTargetUserName.setText(targetName);
        tvTargetUserEmail.setText(targetEmail.isEmpty() ? "No email registered" : targetEmail);
        tvTargetUserIdentifier.setText("ID / Reg No: " + (targetIdentifier.isEmpty() ? "N/A" : targetIdentifier));

        String upperRole = targetRole.toUpperCase();
        chipTargetUserRole.setText(upperRole);

        String upperStatus = targetStatus.toUpperCase();
        tvTargetUserStatus.setText("Status: " + upperStatus);
        if ("ACTIVE".equalsIgnoreCase(upperStatus)) {
            tvTargetUserStatus.setTextColor(Color.parseColor("#10B981")); // Emerald
        } else if ("INACTIVE".equalsIgnoreCase(upperStatus)) {
            tvTargetUserStatus.setTextColor(Color.parseColor("#64748B")); // Slate
        } else {
            tvTargetUserStatus.setTextColor(Color.parseColor("#EF4444")); // Red
        }
    }

    private void setupPasswordValidationListeners() {
        etNewPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilNewPassword.setError(null);
                updatePasswordStrength(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilConfirmPassword.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updatePasswordStrength(String password) {
        if (TextUtils.isEmpty(password)) {
            layoutPasswordStrength.setVisibility(View.GONE);
            return;
        }

        layoutPasswordStrength.setVisibility(View.VISIBLE);
        int score = calculatePasswordStrength(password);

        pbPasswordStrength.setProgress(score);

        if (score < 40) {
            pbPasswordStrength.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#EF4444"))); // Red
            tvPasswordStrength.setText("Strength: Weak (Minimum 8 characters)");
            tvPasswordStrength.setTextColor(Color.parseColor("#EF4444"));
        } else if (score < 75) {
            pbPasswordStrength.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#F59E0B"))); // Amber
            tvPasswordStrength.setText("Strength: Medium (Good password)");
            tvPasswordStrength.setTextColor(Color.parseColor("#D97706"));
        } else {
            pbPasswordStrength.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#10B981"))); // Green
            tvPasswordStrength.setText("Strength: Strong (Excellent)");
            tvPasswordStrength.setTextColor(Color.parseColor("#059669"));
        }
    }

    private int calculatePasswordStrength(String password) {
        int score = 0;
        int len = password.length();

        if (len >= 8) score += 30;
        if (len >= 12) score += 20;

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }

        if (hasUpper && hasLower) score += 20;
        if (hasDigit) score += 15;
        if (hasSpecial) score += 15;

        return Math.min(score, 100);
    }

    private void setupActions() {
        btnCancel.setOnClickListener(v -> finish());
        btnUpdatePassword.setOnClickListener(v -> attemptPasswordUpdate());
    }

    private void attemptPasswordUpdate() {
        String newPassword = etNewPassword.getText() != null ? etNewPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

        // Validation Rules
        if (TextUtils.isEmpty(newPassword)) {
            tilNewPassword.setError("Password cannot be empty.");
            etNewPassword.requestFocus();
            return;
        }

        if (newPassword.length() < 8) {
            tilNewPassword.setError("Password must contain at least 8 characters.");
            etNewPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError("Please confirm the new password.");
            etConfirmPassword.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            tilConfirmPassword.setError("Passwords do not match.");
            etConfirmPassword.requestFocus();
            return;
        }

        // Set Loading State
        setLoadingState(true);

        FirebaseUser currentAdmin = FirebaseAuth.getInstance().getCurrentUser();
        String adminUid = currentAdmin != null ? currentAdmin.getUid() : sessionManager.getIdentifier();
        String adminName = sessionManager.getUserName() != null ? sessionManager.getUserName() : "System Admin";

        // Call Secure Cloud Function Backend
        AdminAuthService.getInstance().changeUserPassword(
                targetUid,
                targetEmail,
                newPassword,
                new AdminAuthService.OnAuthOperationListener() {
                    @Override
                    public void onSuccess(String message) {
                        setLoadingState(false);

                        // Sync local SQLite cache for offline queries if applicable
                        syncLocalDatabase(newPassword);

                        // Create audit activity in Firestore portalActivities (WITHOUT PASSWORD)
                        activityLogger.logPasswordChanged(
                                targetUid.isEmpty() ? targetEmail : targetUid,
                                targetName,
                                targetRole,
                                adminUid,
                                adminName
                        );

                        // If opened from a Password Reset Request, mark it COMPLETED
                        if (associatedRequestId != null && !associatedRequestId.isEmpty()) {
                            com.example.repository.PasswordResetRepository.getInstance(ChangeUserPasswordActivity.this)
                                    .updateRequestStatus(associatedRequestId, "COMPLETED", adminUid, "Password updated by Admin", new com.example.repository.PasswordResetRepository.OnOperationListener() {
                                        @Override
                                        public void onSuccess(String msg) {
                                            activityLogger.logPasswordResetCompleted(associatedRequestId, targetUid, targetName, targetRole, adminName);
                                        }

                                        @Override
                                        public void onError(String error) {
                                        }
                                    });
                        }

                        Toast.makeText(ChangeUserPasswordActivity.this,
                                "Password updated successfully for " + targetName,
                                Toast.LENGTH_LONG).show();

                        setResult(RESULT_OK);
                        finish();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        setLoadingState(false);
                        showErrorMessage(errorMessage);
                    }
                }
        );
    }

    private void syncLocalDatabase(String newPassword) {
        try {
            if ("STUDENT".equalsIgnoreCase(targetRole) && !targetIdentifier.isEmpty()) {
                dbHelper.resetStudentPassword(targetIdentifier, newPassword);
            } else if ("TEACHER".equalsIgnoreCase(targetRole) && !targetEmail.isEmpty()) {
                dbHelper.resetTeacherPassword(targetEmail, newPassword);
            }
            if (!targetEmail.isEmpty()) {
                dbHelper.updateUserPassword(targetEmail, newPassword);
            }
        } catch (Exception ignore) {
        }
    }

    private void setLoadingState(boolean isLoading) {
        btnUpdatePassword.setEnabled(!isLoading);
        btnCancel.setEnabled(!isLoading);
        etNewPassword.setEnabled(!isLoading);
        etConfirmPassword.setEnabled(!isLoading);
        layoutLoadingState.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void showErrorMessage(String message) {
        View coordinator = findViewById(R.id.coordinatorChangePassword);
        if (coordinator != null) {
            Snackbar.make(coordinator, message, Snackbar.LENGTH_LONG)
                    .setBackgroundTint(Color.parseColor("#EF4444"))
                    .setTextColor(Color.WHITE)
                    .show();
        } else {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }
}
