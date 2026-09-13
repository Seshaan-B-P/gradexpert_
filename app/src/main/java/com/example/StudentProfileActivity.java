package com.example;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.database.DatabaseHelper;
import com.example.model.Student;
import com.example.utils.ProfilePhotoManager;
import com.example.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.Locale;

/**
 * Student Profile Activity for viewing personal & academic credentials,
 * capturing/updating profile photo with the camera, modifying contact info,
 * and accessing academic support.
 */
public class StudentProfileActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private Student currentStudent;

    private ImageView btnBack, btnEditHeader, btnChangeAvatar, imgStudentAvatar;
    private TextView tvName, tvRegNo, tvDeptSem;
    private TextView tvCGPA, tvSGPA, tvAttendance, tvCredits;
    private TextView tvEmail, tvPhone;

    private View rowEditProfile, rowChangePassword, rowHelpSupport;
    private MaterialButton btnLogout;

    private int studentId = 1;
    private Uri currentCaptureUri;

    private ActivityResultLauncher<Uri> takePhotoLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profile);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        studentId = sessionManager.getUserId();

        setupCameraLaunchers();
        initViews();
        loadStudentProfileData();
        setupListeners();
    }

    private void setupCameraLaunchers() {
        takePhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (Boolean.TRUE.equals(success) && currentCaptureUri != null) {
                        handlePhotoCaptured();
                    }
                }
        );

        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (Boolean.TRUE.equals(isGranted)) {
                        launchCamera();
                    } else {
                        Toast.makeText(this, "Camera permission is required to capture a profile photo", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackStudentProfile);
        btnEditHeader = findViewById(R.id.btnEditStudentProfileHeader);
        btnChangeAvatar = findViewById(R.id.btnChangeStudentAvatar);
        imgStudentAvatar = findViewById(R.id.imgStudentProfileAvatar);

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
        btnChangeAvatar.setOnClickListener(v -> showPhotoOptionsDialog());
        if (imgStudentAvatar != null) {
            imgStudentAvatar.setOnClickListener(v -> showPhotoOptionsDialog());
        }

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

        displayAvatar(null);
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

    private void showPhotoOptionsDialog() {
        String existingPhoto = getActiveStudentPhotoPath();
        boolean hasExisting = existingPhoto != null && !existingPhoto.isEmpty();

        String[] options;
        if (hasExisting) {
            options = new String[]{"Take Photo with Camera", "Remove Photo"};
        } else {
            options = new String[]{"Take Photo with Camera"};
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Update Profile Photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermissionAndLaunch();
                    } else if (which == 1) {
                        removeProfilePhoto();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        try {
            currentCaptureUri = ProfilePhotoManager.createTempCaptureUri(this);
            if (currentCaptureUri != null) {
                takePhotoLauncher.launch(currentCaptureUri);
            } else {
                Toast.makeText(this, "Failed to initialize camera capture", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error opening camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void handlePhotoCaptured() {
        String identifier = currentStudent != null && currentStudent.getRegNo() != null && !currentStudent.getRegNo().isEmpty()
                ? currentStudent.getRegNo()
                : sessionManager.getIdentifier();
        if (identifier == null || identifier.trim().isEmpty()) {
            identifier = "student_" + studentId;
        }

        String savedPath = ProfilePhotoManager.processAndSaveAvatar(this, currentCaptureUri, "student", identifier);
        if (savedPath != null) {
            // Update SQLite
            dbHelper.updateStudentPhotoUri(studentId, identifier, savedPath);
            if (currentStudent != null) {
                currentStudent.setPhotoUri(savedPath);
            }
            // Update Session
            sessionManager.setProfilePhotoUri(savedPath);
            // Sync to Firestore
            syncStudentPhotoToFirestore(identifier, savedPath);

            // Display in UI
            displayAvatar(savedPath);
            Toast.makeText(this, "Profile photo updated successfully!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to process photo from camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void removeProfilePhoto() {
        String identifier = currentStudent != null && currentStudent.getRegNo() != null && !currentStudent.getRegNo().isEmpty()
                ? currentStudent.getRegNo()
                : sessionManager.getIdentifier();
        if (identifier == null || identifier.trim().isEmpty()) {
            identifier = "student_" + studentId;
        }

        ProfilePhotoManager.deleteProfilePhoto(this, "student", identifier);
        dbHelper.updateStudentPhotoUri(studentId, identifier, "");
        if (currentStudent != null) {
            currentStudent.setPhotoUri("");
        }
        sessionManager.setProfilePhotoUri(null);
        syncStudentPhotoToFirestore(identifier, "");
        displayAvatar(null);
        Toast.makeText(this, "Profile photo removed", Toast.LENGTH_SHORT).show();
    }

    private String getActiveStudentPhotoPath() {
        String sessionPath = sessionManager.getProfilePhotoUri();
        if (sessionPath != null && !sessionPath.trim().isEmpty()) {
            File f = new File(sessionPath);
            if (f.exists() && f.length() > 0) return sessionPath;
        }

        if (currentStudent != null && currentStudent.getPhotoUri() != null && !currentStudent.getPhotoUri().trim().isEmpty()) {
            File f = new File(currentStudent.getPhotoUri());
            if (f.exists() && f.length() > 0) return currentStudent.getPhotoUri();
        }

        String regNo = currentStudent != null ? currentStudent.getRegNo() : sessionManager.getIdentifier();
        String dbPath = dbHelper.getStudentPhotoUri(studentId, regNo);
        if (dbPath != null && !dbPath.trim().isEmpty()) {
            File f = new File(dbPath);
            if (f.exists() && f.length() > 0) return dbPath;
        }

        // Check internal files default avatar
        if (regNo != null && !regNo.trim().isEmpty()) {
            String safeId = regNo.replaceAll("[^a-zA-Z0-9_-]", "_");
            File defaultFile = new File(getFilesDir(), "profile_photos/student_" + safeId + ".jpg");
            if (defaultFile.exists() && defaultFile.length() > 0) {
                return defaultFile.getAbsolutePath();
            }
        }
        return null;
    }

    private void displayAvatar(String photoPath) {
        if (imgStudentAvatar == null) return;
        if (photoPath == null || photoPath.trim().isEmpty()) {
            photoPath = getActiveStudentPhotoPath();
        }
        ProfilePhotoManager.displayProfilePhoto(this, photoPath, imgStudentAvatar, R.drawable.ic_profile);
    }

    private void syncStudentPhotoToFirestore(String regNo, String photoPath) {
        try {
            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(user.getUid())
                        .update("photoUri", photoPath != null ? photoPath : "",
                                "profileImageUrl", photoPath != null ? photoPath : "");
            }
        } catch (Exception ignored) {}
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
