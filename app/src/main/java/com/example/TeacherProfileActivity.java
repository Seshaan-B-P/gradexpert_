package com.example;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.database.DatabaseHelper;
import com.example.utils.PasswordUtils;
import com.example.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Activity managing Teacher Profile details, Edit Profile, Change Password, and Logout.
 */
public class TeacherProfileActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private DatabaseHelper dbHelper;

    private ImageView btnBack;
    private ImageView imgTeacherProfileAvatar;
    private TextView tvProfileTeacherName, tvProfileDesignation, tvProfileDepartment, tvProfileEmail, tvProfilePhone;
    private Chip chipProfileId;

    private MaterialButton btnEditProfile, btnChangePassword, btnLogoutTeacher;

    private String teacherEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_profile);

        sessionManager = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);

        initViews();
        loadProfileData();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackTeacherProfile);
        imgTeacherProfileAvatar = findViewById(R.id.imgTeacherProfileAvatar);

        tvProfileTeacherName = findViewById(R.id.tvProfileTeacherName);
        tvProfileDesignation = findViewById(R.id.tvProfileDesignation);
        tvProfileDepartment = findViewById(R.id.tvProfileDepartment);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfilePhone = findViewById(R.id.tvProfilePhone);
        chipProfileId = findViewById(R.id.chipProfileId);

        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        if (btnChangePassword != null) {
            btnChangePassword.setVisibility(View.GONE);
        }
        btnLogoutTeacher = findViewById(R.id.btnLogoutTeacher);
    }

    private void loadProfileData() {
        String name = sessionManager.getUserName();
        if (name == null || name.isEmpty()) {
            name = "Dr. Seshaan B P";
        }
        tvProfileTeacherName.setText(name);

        String identifier = sessionManager.getIdentifier();
        if (identifier == null || identifier.isEmpty()) {
            identifier = "TCH1003";
        }
        chipProfileId.setText("Teacher ID: " + identifier);

        teacherEmail = sessionManager.getUserEmail();
        if (teacherEmail == null || teacherEmail.isEmpty()) {
            teacherEmail = "teacher@gradexpert.com";
        }
        tvProfileEmail.setText(teacherEmail);

        // Fetch remaining details from SQLite teachers table
        Cursor cursor = dbHelper.getReadableDatabase().query(DatabaseHelper.TABLE_TEACHERS, null, "email=?", new String[]{teacherEmail}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String dept = cursor.getString(cursor.getColumnIndexOrThrow("department"));
            String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
            if (dept != null && !dept.isEmpty()) {
                tvProfileDepartment.setText(dept + " Department");
                tvProfileDesignation.setText("Professor - " + dept);
            }
            if (phone != null && !phone.isEmpty()) {
                tvProfilePhone.setText(phone);
            }
            cursor.close();
        } else {
            if (cursor != null) cursor.close();
            tvProfileDepartment.setText("Computer Science Department");
            tvProfileDesignation.setText("Professor - Computer Science");
            tvProfilePhone.setText("+91 98765 43210");
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        btnLogoutTeacher.setOnClickListener(v -> confirmLogout());
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_teacher_profile, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextInputEditText etName = view.findViewById(R.id.etTeacherProfileName);
        TextInputEditText etDept = view.findViewById(R.id.etTeacherProfileDept);
        TextInputEditText etPhone = view.findViewById(R.id.etTeacherProfilePhone);

        etName.setText(tvProfileTeacherName.getText().toString());
        etDept.setText("Computer Science");
        etPhone.setText(tvProfilePhone.getText().toString());

        MaterialButton btnCancel = view.findViewById(R.id.btnCancelTeacherProfile);
        MaterialButton btnSave = view.findViewById(R.id.btnSaveTeacherProfile);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String newName = etName.getText() != null ? etName.getText().toString().trim() : "";
            String newDept = etDept.getText() != null ? etDept.getText().toString().trim() : "";
            String newPhone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";

            if (newName.isEmpty()) {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            sessionManager.createLoginSession(sessionManager.getUserId(), newName, teacherEmail, "TEACHER", sessionManager.getIdentifier());
            tvProfileTeacherName.setText(newName);
            if (!newDept.isEmpty()) tvProfileDepartment.setText(newDept + " Department");
            if (!newPhone.isEmpty()) tvProfilePhone.setText(newPhone);

            Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showChangePasswordDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Password Change Restricted")
                .setMessage("Teachers cannot change their password.\n\nOnly the System Administrator can reset or update your password. If you forgot your password, please use the 'Forgot Password' link on the login screen to send a reset request to the Admin.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out of GradeXpert ERP?")
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    try {
                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                    } catch (Exception ignored) {}
                    sessionManager.logout();
                    Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(TeacherProfileActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
