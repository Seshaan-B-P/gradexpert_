package com.example;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.database.DatabaseHelper;
import com.example.model.Student;
import com.example.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

/**
 * Student Profile Activity for viewing personal & academic credentials,
 * modifying contact info, updating passwords, and accessing academic support.
 */
public class StudentProfileActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private Student currentStudent;

    private ImageView btnBack, btnEditHeader, btnChangeAvatar;
    private TextView tvName, tvRegNo, tvDeptSem;
    private TextView tvCGPA, tvSGPA, tvAttendance, tvCredits;
    private TextView tvEmail, tvPhone;

    private View rowEditProfile, rowChangePassword, rowHelpSupport;
    private MaterialButton btnLogout;

    private int studentId = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profile);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        studentId = sessionManager.getUserId();

        initViews();
        loadStudentProfileData();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackStudentProfile);
        btnEditHeader = findViewById(R.id.btnEditStudentProfileHeader);
        btnChangeAvatar = findViewById(R.id.btnChangeStudentAvatar);

        tvName = findViewById(R.id.tvProfileStudentName);
        tvRegNo = findViewById(R.id.tvProfileStudentRegNo);
        tvDeptSem = findViewById(R.id.tvProfileStudentDeptSem);

        tvCGPA = findViewById(R.id.tvProfileCGPA);
        tvSGPA = findViewById(R.id.tvProfileSGPA);
        tvAttendance = findViewById(R.id.tvProfileAttendance);
        tvCredits = findViewById(R.id.tvProfileCredits);

        tvEmail = findViewById(R.id.tvProfileStudentEmail);
        tvPhone = findViewById(R.id.tvProfileStudentPhone);

        rowEditProfile = findViewById(R.id.rowEditProfile);
        rowChangePassword = findViewById(R.id.rowChangePassword);
        if (rowChangePassword != null) {
            rowChangePassword.setVisibility(View.GONE);
        }
        rowHelpSupport = findViewById(R.id.rowHelpSupport);
        btnLogout = findViewById(R.id.btnStudentLogout);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnEditHeader.setOnClickListener(v -> showEditProfileDialog());
        btnChangeAvatar.setOnClickListener(v -> showAvatarChangeToast());

        rowEditProfile.setOnClickListener(v -> showEditProfileDialog());
        rowChangePassword.setOnClickListener(v -> showChangePasswordRestrictedDialog());
        rowHelpSupport.setOnClickListener(v -> showHelpSupportDialog());

        btnLogout.setOnClickListener(v -> confirmLogout());
    }

    private void loadStudentProfileData() {
        currentStudent = dbHelper.getStudentDetails(sessionManager.getIdentifier());
        if (currentStudent == null && sessionManager.getUserId() > 0) {
            currentStudent = dbHelper.getStudentById(sessionManager.getUserId());
        }

        if (currentStudent != null) {
            studentId = currentStudent.getId();
            tvName.setText(currentStudent.getName());
            tvRegNo.setText("Register No: " + currentStudent.getRegNo());
            tvDeptSem.setText(currentStudent.getDepartment() + " • Semester " + currentStudent.getSemester());
            tvEmail.setText(currentStudent.getEmail() != null ? currentStudent.getEmail() : "student@gradexpert.edu");
            tvPhone.setText(currentStudent.getPhone() != null ? currentStudent.getPhone() : "+91 98765 43210");
        } else {
            tvName.setText(sessionManager.getUserName());
            tvRegNo.setText("Register No: " + sessionManager.getIdentifier());
            tvDeptSem.setText("Master of Computer Applications • Semester III");
            tvEmail.setText(sessionManager.getUserEmail());
            tvPhone.setText("+91 98765 43210");
        }

        // Academic Snapshot from SQLite
        double cgpa = dbHelper.getLatestCGPA(studentId);
        double sgpa = dbHelper.getLatestSGPA(studentId);
        int att = dbHelper.getStudentAttendancePercentage(studentId);
        int credits = dbHelper.getStudentEarnedCredits(studentId);

        tvCGPA.setText(String.format(Locale.US, "%.2f", cgpa));
        tvSGPA.setText(String.format(Locale.US, "%.2f", sgpa));
        tvAttendance.setText(att + "%");
        tvCredits.setText(credits + " / 160");
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_student_profile, null);

        EditText etName = dialogView.findViewById(R.id.etDialogStudentName);
        EditText etEmail = dialogView.findViewById(R.id.etDialogStudentEmail);
        EditText etPhone = dialogView.findViewById(R.id.etDialogStudentPhone);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelEditStudentProfile);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSaveEditStudentProfile);

        if (currentStudent != null) {
            etName.setText(currentStudent.getName());
            etEmail.setText(currentStudent.getEmail());
            etPhone.setText(currentStudent.getPhone());
        } else {
            etName.setText(sessionManager.getUserName());
            etEmail.setText(sessionManager.getUserEmail());
            etPhone.setText("+91 98765 43210");
        }

        AlertDialog dialog = builder.setView(dialogView).create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            String newEmail = etEmail.getText().toString().trim();
            String newPhone = etPhone.getText().toString().trim();

            if (newName.isEmpty()) {
                etName.setError("Name is required");
                return;
            }
            if (newEmail.isEmpty()) {
                etEmail.setError("Email is required");
                return;
            }

            boolean updated = dbHelper.updateStudentContact(studentId, newName, newEmail, newPhone);
            if (updated) {
                sessionManager.createLoginSession(studentId, newName, newEmail, "STUDENT", sessionManager.getIdentifier());
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                loadStudentProfileData();
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showChangePasswordRestrictedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Password Change Restricted")
                .setMessage("Students cannot change their password.\n\nOnly the System Administrator can reset or update your password. If you forgot your password, please use the 'Forgot Password' link on the login screen to send a reset request to the Admin.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showHelpSupportDialog() {
        new AlertDialog.Builder(this)
                .setTitle("GradeXpert Academic Support")
                .setMessage("Need assistance with courses, marks discrepancy, or exam registration?\n\n"
                        + "📧 Academic Office: support@gradexpert.edu\n"
                        + "📞 Helpdesk: +91 80 2345 6789\n"
                        + "🕒 Office Hours: Mon - Fri, 9:00 AM - 5:00 PM")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showAvatarChangeToast() {
        Toast.makeText(this, "Student profile avatar can be modified via University ERP admin.", Toast.LENGTH_SHORT).show();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("LOGOUT", (dialog, which) -> {
                    try {
                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                    } catch (Exception ignored) {}
                    sessionManager.logout();
                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(StudentProfileActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
