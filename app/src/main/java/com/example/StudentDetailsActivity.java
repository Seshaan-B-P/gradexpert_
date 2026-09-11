package com.example;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

/**
 * Activity for displaying complete Student Profile details and quick links to Attendance, Assignments, and Results.
 */
public class StudentDetailsActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView tvName;
    private TextView tvRegNo;
    private Chip chipStatus;

    private TextView tvEmail;
    private TextView tvPhone;
    private TextView tvDept;
    private TextView tvSemSec;
    private TextView tvGenderDob;

    private MaterialButton btnQuickAttendance;
    private MaterialButton btnQuickAssignments;
    private MaterialButton btnQuickResults;

    private String studentId = "";
    private String studentName = "";
    private String regNo = "";
    private String email = "";
    private String phone = "";
    private String department = "";
    private String programLevel = "UG";
    private String semester = "";
    private String section = "";
    private String gender = "";
    private String dob = "";
    private String status = "ACTIVE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_details);

        readIntentExtras();
        initViews();
        setupToolbar();
        setupWindowInsets();
        populateData();
        setupQuickLinks();
    }

    private void readIntentExtras() {
        if (getIntent() != null) {
            studentId = getIntent().getStringExtra("studentId");
            studentName = getIntent().getStringExtra("name");
            regNo = getIntent().getStringExtra("registerNo");
            email = getIntent().getStringExtra("email");
            phone = getIntent().getStringExtra("phone");
            department = getIntent().getStringExtra("department");
            String pLevel = getIntent().getStringExtra("programLevel");
            if (pLevel != null && !pLevel.isEmpty()) {
                programLevel = pLevel.toUpperCase();
            }
            semester = getIntent().getStringExtra("semester");
            section = getIntent().getStringExtra("section");
            gender = getIntent().getStringExtra("gender");
            dob = getIntent().getStringExtra("dateOfBirth");
            status = getIntent().getStringExtra("status");
        }
    }

    private void setupWindowInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbarStudentDetails), (v, insets) -> {
            androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
            v.setPadding(0, statusBarInsets.top, 0, 0);
            return insets;
        });
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarStudentDetails);
        tvName = findViewById(R.id.tvDetailStudentName);
        tvRegNo = findViewById(R.id.tvDetailStudentRegNo);
        chipStatus = findViewById(R.id.chipDetailStudentStatus);

        tvEmail = findViewById(R.id.tvDetailEmail);
        tvPhone = findViewById(R.id.tvDetailPhone);
        tvDept = findViewById(R.id.tvDetailDept);
        tvSemSec = findViewById(R.id.tvDetailSemSec);
        tvGenderDob = findViewById(R.id.tvDetailGenderDob);

        btnQuickAttendance = findViewById(R.id.btnQuickAttendance);
        btnQuickAssignments = findViewById(R.id.btnQuickAssignments);
        btnQuickResults = findViewById(R.id.btnQuickResults);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void populateData() {
        tvName.setText(studentName != null ? studentName : "Student");
        tvRegNo.setText("Register No: " + (regNo != null ? regNo : "N/A"));
        tvEmail.setText(email != null && !email.isEmpty() ? email : "N/A");
        tvPhone.setText(phone != null && !phone.isEmpty() ? phone : "N/A");

        String levelText = (programLevel != null && !programLevel.isEmpty()) ? "[" + programLevel.toUpperCase() + "] " : "";
        tvDept.setText(levelText + (department != null ? department : "Department"));

        String secText = (section != null && !section.isEmpty()) ? " (" + section + ")" : "";
        tvSemSec.setText((semester != null ? semester : "Semester") + secText);

        String genText = (gender != null && !gender.isEmpty()) ? gender : "N/A";
        String dobText = (dob != null && !dob.isEmpty()) ? dob : "N/A";
        tvGenderDob.setText(genText + " • " + dobText);

        String st = status != null ? status.toUpperCase() : "ACTIVE";
        chipStatus.setText(st);
        if ("ACTIVE".equalsIgnoreCase(st)) {
            chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#10B981"))); // Green
        } else {
            chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#64748B"))); // Grey
        }
    }

    private void setupQuickLinks() {
        btnQuickAttendance.setOnClickListener(v -> {
            Intent intent = new Intent(this, StudentAttendanceActivity.class);
            intent.putExtra("studentId", studentId);
            intent.putExtra("registerNo", regNo);
            intent.putExtra("studentName", studentName);
            startActivity(intent);
        });

        btnQuickAssignments.setOnClickListener(v -> {
            Intent intent = new Intent(this, StudentAssignmentActivity.class);
            intent.putExtra("studentId", studentId);
            intent.putExtra("department", department);
            intent.putExtra("semester", semester);
            startActivity(intent);
        });

        btnQuickResults.setOnClickListener(v -> {
            Intent intent = new Intent(this, ResultsActivity.class);
            intent.putExtra("studentId", studentId);
            intent.putExtra("registerNo", regNo);
            startActivity(intent);
        });
    }
}
