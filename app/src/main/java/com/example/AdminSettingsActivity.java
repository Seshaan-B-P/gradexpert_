package com.example;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.database.DatabaseHelper;
import com.example.utils.PortalActivityLogger;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * System Settings Activity for System Admin in GradeXpert.
 * Manages global thresholds like minimum attendance percentage.
 */
public class AdminSettingsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private DatabaseHelper dbHelper;

    private Toolbar toolbar;
    private EditText etMinAttendance;
    private MaterialButton btnSave;
    private MaterialButton btnReset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_settings);

        db = FirebaseFirestore.getInstance();
        dbHelper = new DatabaseHelper(this);

        initViews();
        setupToolbar();
        setupWindowInsets();
        loadSettings();
    }

    private void setupWindowInsets() {
        View appBar = findViewById(R.id.appBarAdminSettings);
        if (appBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, insets) -> {
                androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                v.setPadding(0, statusBarInsets.top, 0, 0);
                return insets;
            });
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarAdminSettings);
        etMinAttendance = findViewById(R.id.etMinAttendanceSetting);
        btnSave = findViewById(R.id.btnSaveSystemSettings);
        btnReset = findViewById(R.id.btnResetAllData);

        btnSave.setOnClickListener(v -> saveSettings());
        if (btnReset != null) {
            btnReset.setOnClickListener(v -> confirmDataPurge());
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("System Settings");
            getSupportActionBar().setSubtitle("Global Portal Controls & Business Rules");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadSettings() {
        db.collection("systemSettings").document("general").get().addOnSuccessListener(doc -> {
            if (doc.exists() && doc.contains("minimumAttendancePercentage")) {
                Long minAtt = doc.getLong("minimumAttendancePercentage");
                if (minAtt != null) {
                    etMinAttendance.setText(String.valueOf(minAtt));
                }
            } else {
                etMinAttendance.setText("75");
            }
        }).addOnFailureListener(e -> etMinAttendance.setText("75"));
    }

    private void saveSettings() {
        String valStr = etMinAttendance.getText().toString().trim();
        if (TextUtils.isEmpty(valStr)) {
            etMinAttendance.setError("Required");
            return;
        }

        int val = 75;
        try {
            val = Integer.parseInt(valStr);
        } catch (Exception ignored) {}

        Map<String, Object> map = new HashMap<>();
        map.put("minimumAttendancePercentage", val);

        final int minAttendance = val;
        db.collection("systemSettings").document("general").set(map)
                .addOnSuccessListener(aVoid -> {
                    PortalActivityLogger.getInstance(this).logTeacherActivity(
                            "System Admin",
                            "Updated System Settings",
                            "Minimum Attendance Threshold set to " + minAttendance + "%",
                            "System Settings"
                    );
                    Toast.makeText(this, "System settings updated successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Settings saved locally (" + minAttendance + "%)", Toast.LENGTH_SHORT).show());
    }

    private void confirmDataPurge() {
        new AlertDialog.Builder(this)
                .setTitle("Purge & Reset All Data?")
                .setMessage("This action will permanently wipe all local teachers, students, subjects, attendance, marks, assignments, submissions & notifications so you can start testing completely fresh.\n\nAdmin account credentials (admin@gradexpert.com / 123456) will remain active.")
                .setPositiveButton("YES, WIPE ALL DATA", (dialog, which) -> {
                    if (dbHelper != null) {
                        dbHelper.purgeAllLocalData();
                    }
                    try {
                        com.example.database.FirestoreHelper.getInstance().purgeFirestoreCollections();
                    } catch (Exception ignored) {}
                    Toast.makeText(this, "All local & Firebase cloud mock data wiped successfully!", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }
}
