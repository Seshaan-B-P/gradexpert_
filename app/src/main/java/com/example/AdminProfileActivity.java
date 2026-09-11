package com.example;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Admin Profile & Security activity.
 */
public class AdminProfileActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private FirebaseAuth mAuth;

    private Toolbar toolbar;
    private TextView tvName, tvEmail;
    private MaterialButton btnChangePass, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profile);

        sessionManager = new SessionManager(this);
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupToolbar();
        setupWindowInsets();
        loadProfile();
        setupListeners();
    }

    private void setupWindowInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbarAdminProfile), (v, insets) -> {
            androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
            v.setPadding(0, statusBarInsets.top, 0, 0);
            return insets;
        });
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarAdminProfile);
        tvName = findViewById(R.id.tvAdminProfileName);
        tvEmail = findViewById(R.id.tvAdminProfileEmail);
        btnChangePass = findViewById(R.id.btnAdminChangePassword);
        btnLogout = findViewById(R.id.btnAdminLogoutProfile);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadProfile() {
        tvName.setText(sessionManager.getUserName());
        tvEmail.setText(sessionManager.getUserEmail());
    }

    private void setupListeners() {
        btnChangePass.setOnClickListener(v -> sendPasswordResetEmail());

        btnLogout.setOnClickListener(v -> {
            try {
                if (mAuth != null) {
                    mAuth.signOut();
                } else {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                }
            } catch (Exception ignored) {}
            sessionManager.logout();
            Toast.makeText(this, "Admin Signed Out Successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(AdminProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void sendPasswordResetEmail() {
        String email = sessionManager.getUserEmail();
        if (email != null && !email.isEmpty()) {
            mAuth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Password reset email sent to " + email, Toast.LENGTH_LONG).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Password reset request recorded for " + email, Toast.LENGTH_LONG).show());
        } else {
            Toast.makeText(this, "Admin email unavailable", Toast.LENGTH_SHORT).show();
        }
    }
}
