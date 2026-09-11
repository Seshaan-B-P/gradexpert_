package com.example;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.database.DatabaseHelper;
import com.example.utils.SessionManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvRoleBadge, tvUserName, tvUserEmail, tvUserIdentifier;
    private ImageView imgAvatar;
    private Button btnEditProfile;
    private SwitchMaterial switchDarkMode;
    private MaterialCardView cardChangePassword, cardLogout, cardAboutApp;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    private int userId = -1;
    private String userRole = "STUDENT";
    private boolean isTeacher = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);

        setContentView(R.layout.activity_settings);

        userId = sessionManager.getUserId();
        userRole = sessionManager.getUserRole() != null ? sessionManager.getUserRole().toUpperCase() : "STUDENT";
        isTeacher = "TEACHER".equalsIgnoreCase(userRole);

        initViews();
        populateProfileData();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackSettings);
        tvRoleBadge = findViewById(R.id.tvSettingsRoleBadge);
        tvUserName = findViewById(R.id.tvSettingsUserName);
        tvUserEmail = findViewById(R.id.tvSettingsUserEmail);
        tvUserIdentifier = findViewById(R.id.tvSettingsUserIdentifier);
        imgAvatar = findViewById(R.id.imgSettingsAvatar);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        switchDarkMode = findViewById(R.id.switchDarkMode);

        cardChangePassword = findViewById(R.id.cardChangePassword);
        if (cardChangePassword != null && !"ADMIN".equalsIgnoreCase(userRole)) {
            cardChangePassword.setVisibility(View.GONE);
        }
        cardLogout = findViewById(R.id.cardLogout);
        cardAboutApp = findViewById(R.id.cardAboutApp);

        tvRoleBadge.setText(isTeacher ? "FACULTY MODE" : "STUDENT MODE");
        if (switchDarkMode != null) {
            switchDarkMode.setVisibility(View.VISIBLE);
            switchDarkMode.setChecked(sessionManager.isDarkMode());
        }
    }

    private void populateProfileData() {
        String name = sessionManager.getUserName();
        String email = sessionManager.getUserEmail();
        String identifier = sessionManager.getIdentifier();

        tvUserName.setText(name);
        tvUserEmail.setText(email);

        if (isTeacher) {
            tvUserIdentifier.setText("Faculty ID: " + (identifier.isEmpty() ? String.valueOf(userId) : identifier) + " • Computer Science");
        } else {
            tvUserIdentifier.setText("Reg No: " + (identifier.isEmpty() ? "REG202601" : identifier) + " • Semester 5");
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        if (switchDarkMode != null) {
            switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                sessionManager.setDarkMode(isChecked);
                AppCompatDelegate.setDefaultNightMode(isChecked ?
                        AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            });
        }

        // Change Password
        cardChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // Logout
        cardLogout.setOnClickListener(v -> confirmLogout());

        // About App
        cardAboutApp.setOnClickListener(v -> showAboutAppDialog());
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);

        EditText etName = view.findViewById(R.id.etEditProfileName);
        EditText etEmail = view.findViewById(R.id.etEditProfileEmail);
        EditText etPhone = view.findViewById(R.id.etEditProfilePhone);
        Button btnCancel = view.findViewById(R.id.btnCancelEditProfile);
        Button btnSave = view.findViewById(R.id.btnSaveEditProfile);

        etName.setText(sessionManager.getUserName());
        etEmail.setText(sessionManager.getUserEmail());
        etPhone.setText("+1 9876543210");

        AlertDialog dialog = builder.setView(view).create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();

            if (name.isEmpty()) {
                etName.setError("Name is required");
                return;
            }

            if (email.isEmpty()) {
                etEmail.setError("Email is required");
                return;
            }

            boolean updated = dbHelper.updateUserProfileData(userId, isTeacher, name, email, phone);
            if (updated) {
                sessionManager.updateUserProfile(name, email);
                populateProfileData();
                Toast.makeText(SettingsActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toast.makeText(SettingsActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showChangePasswordDialog() {
        String role = sessionManager.getUserRole();
        if ("TEACHER".equalsIgnoreCase(role)) {
            new AlertDialog.Builder(this)
                    .setTitle("Password Change Restricted")
                    .setMessage("Teachers cannot change their own password.\n\nPlease contact the System Admin (admin@gradexpert.com) to update or reset your login password.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        if ("STUDENT".equalsIgnoreCase(role)) {
            new AlertDialog.Builder(this)
                    .setTitle("Password Change Restricted")
                    .setMessage("Students cannot change their own password.\n\nPlease contact your Class Teacher to update or reset your login password.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);

        EditText etCurrent = view.findViewById(R.id.etCurrentPassword);
        EditText etNew = view.findViewById(R.id.etNewPassword);
        EditText etConfirm = view.findViewById(R.id.etConfirmPassword);
        Button btnCancel = view.findViewById(R.id.btnCancelChangePassword);
        Button btnSave = view.findViewById(R.id.btnSaveChangePassword);

        AlertDialog dialog = builder.setView(view).create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String currentPw = etCurrent.getText().toString().trim();
            String newPw = etNew.getText().toString().trim();
            String confirmPw = etConfirm.getText().toString().trim();

            if (currentPw.isEmpty()) {
                etCurrent.setError("Current password required");
                return;
            }

            if (newPw.length() < 4) {
                etNew.setError("Password must be at least 4 characters");
                return;
            }

            if (!newPw.equals(confirmPw)) {
                etConfirm.setError("Passwords do not match");
                return;
            }

            boolean success = dbHelper.changeUserPassword(userId, isTeacher, currentPw, newPw);
            if (success) {
                Toast.makeText(SettingsActivity.this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toast.makeText(SettingsActivity.this, "Incorrect current password. Please try again.", Toast.LENGTH_LONG).show();
            }
        });

        dialog.show();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to log out of GradeXpert?")
                .setPositiveButton("Log Out", (dialog, which) -> {
                    try {
                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                    } catch (Exception ignored) {}
                    sessionManager.logout();
                    Toast.makeText(SettingsActivity.this, "Signed out successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAboutAppDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_about_app, null);

        Button btnClose = view.findViewById(R.id.btnCloseAbout);
        AlertDialog dialog = builder.setView(view).create();

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
