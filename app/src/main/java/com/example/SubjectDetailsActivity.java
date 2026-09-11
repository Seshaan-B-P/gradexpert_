package com.example;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

/**
 * Activity for displaying complete Subject specifications and quick actions to Students, Attendance, Assignments, and Results.
 */
public class SubjectDetailsActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView tvCode;
    private TextView tvName;
    private TextView tvDeptSem;
    private TextView tvDesc;
    private Chip chipStatus;

    private TextView tvCredits;
    private TextView tvType;
    private TextView tvWeeklyHours;
    private TextView tvTotalHours;

    private MaterialButton btnQuickStudents;
    private MaterialButton btnQuickAttendance;
    private MaterialButton btnQuickAssignments;
    private MaterialButton btnQuickResults;

    private String subjectId = "";
    private String subjectCode = "";
    private String subjectName = "";
    private String description = "";
    private String department = "";
    private String semester = "";
    private String programLevel = "UG";
    private int credits = 4;
    private String subjectType = "Theory";
    private int weeklyHours = 5;
    private int totalHours = 60;
    private String status = "ACTIVE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subject_details);

        readIntentExtras();
        initViews();
        setupToolbar();
        setupWindowInsets();
        populateData();
        setupQuickLinks();
    }

    private void readIntentExtras() {
        if (getIntent() != null) {
            subjectId = getIntent().getStringExtra("subjectId");
            subjectCode = getIntent().getStringExtra("subjectCode");
            subjectName = getIntent().getStringExtra("subjectName");
            description = getIntent().getStringExtra("description");
            department = getIntent().getStringExtra("department");
            semester = getIntent().getStringExtra("semester");
            programLevel = getIntent().getStringExtra("programLevel");
            if (programLevel == null || programLevel.isEmpty()) programLevel = "UG";
            credits = getIntent().getIntExtra("credits", 4);
            subjectType = getIntent().getStringExtra("subjectType");
            weeklyHours = getIntent().getIntExtra("weeklyHours", 5);
            totalHours = getIntent().getIntExtra("totalHours", 60);
            status = getIntent().getStringExtra("status");
        }
    }

    private void setupWindowInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbarSubjectDetails), (v, insets) -> {
            androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
            v.setPadding(0, statusBarInsets.top, 0, 0);
            return insets;
        });
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarSubjectDetails);
        tvCode = findViewById(R.id.tvDetailSubjectCode);
        tvName = findViewById(R.id.tvDetailSubjectName);
        tvDeptSem = findViewById(R.id.tvDetailSubjectDeptSem);
        tvDesc = findViewById(R.id.tvDetailSubjectDesc);
        chipStatus = findViewById(R.id.chipDetailSubjectStatus);

        tvCredits = findViewById(R.id.tvDetailCredits);
        tvType = findViewById(R.id.tvDetailType);
        tvWeeklyHours = findViewById(R.id.tvDetailWeeklyHours);
        tvTotalHours = findViewById(R.id.tvDetailTotalHours);

        btnQuickStudents = findViewById(R.id.btnSubjectQuickStudents);
        btnQuickAttendance = findViewById(R.id.btnSubjectQuickAttendance);
        btnQuickAssignments = findViewById(R.id.btnSubjectQuickAssignments);
        btnQuickResults = findViewById(R.id.btnSubjectQuickResults);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void populateData() {
        tvCode.setText(subjectCode != null ? subjectCode : "");
        tvName.setText(subjectName != null ? subjectName : "Subject Details");
        tvDeptSem.setText("[" + programLevel + "] " + (department != null ? department : "") + " • " + (semester != null ? semester : ""));
        tvDesc.setText(description != null && !description.isEmpty() ? description : "No description provided.");

        tvCredits.setText(credits + " Credits");
        tvType.setText(subjectType != null ? subjectType : "Theory");
        tvWeeklyHours.setText(weeklyHours + " Hours / Week");
        tvTotalHours.setText(totalHours + " Hours");

        String st = status != null ? status.toUpperCase() : "ACTIVE";
        chipStatus.setText(st);
        if ("ACTIVE".equalsIgnoreCase(st)) {
            chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#10B981"))); // Green
        } else {
            chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#64748B"))); // Grey
        }
    }

    private void setupQuickLinks() {
        btnQuickStudents.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManageStudentsActivity.class);
            intent.putExtra("department", department);
            intent.putExtra("semester", semester);
            startActivity(intent);
        });

        btnQuickAttendance.setOnClickListener(v -> {
            Intent intent = new Intent(this, AttendanceHistoryActivity.class);
            intent.putExtra("subjectId", subjectId);
            intent.putExtra("department", department);
            intent.putExtra("semester", semester);
            startActivity(intent);
        });

        btnQuickAssignments.setOnClickListener(v -> {
            Intent intent = new Intent(this, TeacherAssignmentActivity.class);
            intent.putExtra("subjectId", subjectId);
            startActivity(intent);
        });

        btnQuickResults.setOnClickListener(v -> {
            Intent intent = new Intent(this, ResultsActivity.class);
            intent.putExtra("subjectId", subjectId);
            startActivity(intent);
        });
    }
}
