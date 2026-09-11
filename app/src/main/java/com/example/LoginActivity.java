package com.example;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.database.DatabaseHelper;
import com.example.model.User;
import com.example.utils.PortalActivityLogger;
import com.example.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import com.example.utils.IdGenerationService;
import java.util.Locale;

/**
 * Single unified LoginActivity for GradeXpert Academic Portal.
 * Authenticates Admin, Teacher, and Student roles via Firestore & Firebase Auth.
 * Supports both human-readable Login ID (e.g. MCA001, TCH001) and Email logins.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText etUsername, etPassword;
    private CheckBox cbRememberMe;
    private TextView tvForgotPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_GradeXpert);
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);
        try {
            db = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "FirebaseFirestore init error: " + e.getMessage());
        }
        try {
            mAuth = FirebaseAuth.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "FirebaseAuth init error: " + e.getMessage());
        }

        // Auto-redirect if already logged in
        if (sessionManager.isLoggedIn()) {
            redirectDashboard(sessionManager.getUserRole());
            return;
        }

        setContentView(R.layout.activity_login);

        initViews();
        setupListeners();
        loadRememberMeData();
    }

    private void initViews() {
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        cbRememberMe = findViewById(R.id.cbRememberMe);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBarLogin);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());

        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }

    private void loadRememberMeData() {
        if (sessionManager.isRememberMeEnabled()) {
            cbRememberMe.setChecked(true);
            etUsername.setText(sessionManager.getSavedUsername());
            etPassword.setText(sessionManager.getSavedPassword());
        }
    }

    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Required");
            etUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Required");
            etPassword.requestFocus();
            return;
        }

        showLoading(true);

        if (username.contains("@")) {
            // Direct email login via Firebase Auth
            if (mAuth != null) {
                mAuth.signInWithEmailAndPassword(username, password)
                        .addOnSuccessListener(authResult -> {
                            FirebaseUser firebaseUser = authResult.getUser();
                            if (firebaseUser != null) {
                                fetchUserFromFirestore(firebaseUser.getUid(), username, password);
                            } else {
                                fallbackLocalAuth(username, password);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.w(TAG, "Firebase Auth sign-in by email failed: " + e.getMessage());
                            queryFirestoreUserByEmailOrIdentifier(username, password);
                        });
            } else {
                queryFirestoreUserByEmailOrIdentifier(username, password);
            }
        } else {
            // Username is a Login ID (e.g. MCA001, TCH001) or identifier
            IdGenerationService.getInstance().resolveEmailForLoginId(username, new IdGenerationService.OnResolvedListener() {
                @Override
                public void onSuccess(String email, String uid, String canonicalLoginId, String role) {
                    if (mAuth != null && !TextUtils.isEmpty(email)) {
                        mAuth.signInWithEmailAndPassword(email, password)
                                .addOnSuccessListener(authResult -> {
                                    FirebaseUser firebaseUser = authResult.getUser();
                                    String authUid = (firebaseUser != null) ? firebaseUser.getUid() : uid;
                                    fetchUserFromFirestore(authUid, canonicalLoginId, password);
                                })
                                .addOnFailureListener(e -> {
                                    Log.w(TAG, "Firebase Auth sign-in failed for resolved email (" + email + "): " + e.getMessage());
                                    // Fallback to local authentication
                                    fallbackLocalAuth(username, password);
                                });
                    } else if (!TextUtils.isEmpty(uid)) {
                        fetchUserFromFirestore(uid, canonicalLoginId, password);
                    } else {
                        queryFirestoreUserByEmailOrIdentifier(username, password);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    Log.d(TAG, "Login ID lookup notice: " + errorMessage);
                    queryFirestoreUserByEmailOrIdentifier(username, password);
                }
            });
        }
    }

    private void queryFirestoreUserByEmailOrIdentifier(String username, String password) {
        if (db == null) {
            fallbackLocalAuth(username, password);
            return;
        }

        // Check users collection by loginId
        db.collection("users")
                .whereEqualTo("loginId", username.toUpperCase(Locale.US))
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap != null && !snap.isEmpty()) {
                        DocumentSnapshot doc = snap.getDocuments().get(0);
                        authenticateResolvedUserDoc(doc, username, password);
                    } else {
                        // Check users collection by email
                        db.collection("users")
                                .whereEqualTo("email", username)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                                        authenticateResolvedUserDoc(doc, username, password);
                                    } else {
                                        // Try querying by identifier or name in Firestore
                                        db.collection("users")
                                                .whereEqualTo("identifier", username)
                                                .limit(1)
                                                .get()
                                                .addOnSuccessListener(snapshots -> {
                                                    if (snapshots != null && !snapshots.isEmpty()) {
                                                        DocumentSnapshot doc = snapshots.getDocuments().get(0);
                                                        authenticateResolvedUserDoc(doc, username, password);
                                                    } else {
                                                        fallbackLocalAuth(username, password);
                                                    }
                                                })
                                                .addOnFailureListener(e -> fallbackLocalAuth(username, password));
                                    }
                                })
                                .addOnFailureListener(e -> fallbackLocalAuth(username, password));
                    }
                })
                .addOnFailureListener(e -> fallbackLocalAuth(username, password));
    }

    private void authenticateResolvedUserDoc(DocumentSnapshot doc, String username, String password) {
        String email = doc.getString("email");
        if (mAuth != null && !TextUtils.isEmpty(email)) {
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser firebaseUser = authResult.getUser();
                        String uid = (firebaseUser != null) ? firebaseUser.getUid() : doc.getId();
                        fetchUserFromFirestore(uid, username, password);
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Auth sign-in failed on resolved user doc: " + e.getMessage());
                        fallbackLocalAuth(username, password);
                    });
        } else {
            processFirestoreUserDoc(doc, username, password);
        }
    }

    private void fetchUserFromFirestore(String uid, String username, String password) {
        if (db == null) {
            fallbackLocalAuth(username, password);
            return;
        }
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        processFirestoreUserDoc(doc, username, password);
                    } else {
                        fallbackLocalAuth(username, password);
                    }
                })
                .addOnFailureListener(e -> fallbackLocalAuth(username, password));
    }

    private void processFirestoreUserDoc(DocumentSnapshot doc, String username, String password) {
        showLoading(false);
        String role = doc.getString("role");
        String status = doc.getString("status");
        String name = doc.getString("name");
        String email = doc.getString("email");
        String identifier = doc.getString("identifier");

        int userId = 0;
        Object idObj = doc.get("id");
        if (idObj instanceof Number) {
            userId = ((Number) idObj).intValue();
        } else if (idObj instanceof String) {
            try {
                userId = Integer.parseInt((String) idObj);
            } catch (Exception e) {
                userId = Math.abs(((String) idObj).hashCode());
            }
        }
        if (userId <= 0 && doc.getId() != null) {
            try {
                userId = Integer.parseInt(doc.getId());
            } catch (Exception e) {
                userId = Math.abs(doc.getId().hashCode());
            }
        }

        if (status == null) status = "ACTIVE";
        String normalizedStatus = status.trim().toUpperCase();

        if ("INACTIVE".equals(normalizedStatus)) {
            Toast.makeText(this, "Your account is inactive. Please contact system administrator.", Toast.LENGTH_LONG).show();
            return;
        }
        if ("SUSPENDED".equals(normalizedStatus)) {
            Toast.makeText(this, "Your account has been suspended. Please contact system administrator.", Toast.LENGTH_LONG).show();
            return;
        }

        if (role == null) role = "STUDENT";

        String loginId = doc.getString("loginId");
        if (loginId != null && !loginId.trim().isEmpty()) {
            identifier = loginId.trim();
        }

        // Save session
        sessionManager.createLoginSession(
                userId,
                name != null ? name : username,
                email != null ? email : username,
                role,
                identifier != null ? identifier : ""
        );

        sessionManager.setFirebaseUid(doc.getId());
        if (loginId != null && !loginId.trim().isEmpty()) {
            sessionManager.setLoginId(loginId.trim());
        } else if (identifier != null && !identifier.trim().isEmpty()) {
            sessionManager.setLoginId(identifier.trim());
        }

        sessionManager.saveRememberMe(
                cbRememberMe.isChecked(),
                username,
                password,
                role
        );

        Toast.makeText(this, "Login successful! Welcome " + (name != null ? name : username), Toast.LENGTH_SHORT).show();
        if ("TEACHER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
            PortalActivityLogger.getInstance(this).logTeacherLogin(name != null ? name : username, email != null ? email : username);
        }
        redirectDashboard(role);
    }

    private void fallbackLocalAuth(String username, String password) {
        showLoading(false);
        // Try authenticating as Admin, then Teacher, then Student locally
        User user = dbHelper.authenticateAdmin(username, password);
        if (user == null) {
            user = dbHelper.authenticateTeacher(username, password);
        }
        if (user == null) {
            user = dbHelper.authenticateStudent(username, password);
        }

        if (user != null) {
            String status = user.getStatus() != null ? user.getStatus().toUpperCase() : "ACTIVE";
            if ("INACTIVE".equalsIgnoreCase(status)) {
                Toast.makeText(this, "Your account is inactive. Please contact system administrator.", Toast.LENGTH_LONG).show();
                return;
            }
            if ("SUSPENDED".equalsIgnoreCase(status)) {
                Toast.makeText(this, "Your account has been suspended. Please contact system administrator.", Toast.LENGTH_LONG).show();
                return;
            }

            // Save Session
            sessionManager.createLoginSession(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole(),
                    user.getIdentifier()
            );

            // Handle Remember Me
            sessionManager.saveRememberMe(
                    cbRememberMe.isChecked(),
                    username,
                    password,
                    user.getRole()
            );

            Toast.makeText(this, "Login successful! Welcome " + user.getName(), Toast.LENGTH_SHORT).show();
            if ("TEACHER".equalsIgnoreCase(user.getRole()) || "ADMIN".equalsIgnoreCase(user.getRole())) {
                PortalActivityLogger.getInstance(this).logTeacherLogin(user.getName(), user.getEmail());
            }
            redirectDashboard(user.getRole());
        } else {
            Toast.makeText(this, "Invalid credentials. Please check your username and password.", Toast.LENGTH_LONG).show();
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (btnLogin != null) {
            btnLogin.setEnabled(!show);
        }
    }

    private void redirectDashboard(String role) {
        if (role == null) role = "STUDENT";
        String normalizedRole = role.trim().toUpperCase();

        Intent intent;
        if ("ADMIN".equals(normalizedRole)) {
            intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
        } else if ("TEACHER".equals(normalizedRole)) {
            intent = new Intent(LoginActivity.this, TeacherDashboardActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, StudentDashboardActivity.class);
        }
        startActivity(intent);
        finish();
    }
}

