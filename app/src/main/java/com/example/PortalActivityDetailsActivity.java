package com.example;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Activity for displaying complete audit log specifications for a specific portal activity event.
 */
public class PortalActivityDetailsActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView tvTitle;
    private TextView tvDesc;
    private TextView tvType;
    private Chip chipCategory;

    private LinearLayout layoutStudent;
    private TextView tvStudent;
    private LinearLayout layoutSubject;
    private TextView tvSubject;
    private TextView tvTeacher;
    private TextView tvTimestamp;

    private String title = "";
    private String description = "";
    private String type = "GENERAL";
    private String entityType = "GENERAL";
    private String studentName = "";
    private String registerNo = "";
    private String subjectName = "";
    private String teacherName = "Faculty Admin";
    private long timestampMs = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portal_activity_details);

        readIntentExtras();
        initViews();
        setupToolbar();
        setupWindowInsets();
        populateData();
    }

    private void readIntentExtras() {
        if (getIntent() != null) {
            title = getIntent().getStringExtra("title");
            description = getIntent().getStringExtra("description");
            type = getIntent().getStringExtra("type");
            entityType = getIntent().getStringExtra("entityType");
            studentName = getIntent().getStringExtra("studentName");
            registerNo = getIntent().getStringExtra("registerNo");
            subjectName = getIntent().getStringExtra("subjectName");
            teacherName = getIntent().getStringExtra("teacherName");
            timestampMs = getIntent().getLongExtra("timestampMs", System.currentTimeMillis());
        }
    }

    private void setupWindowInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbarPortalActivityDetails), (v, insets) -> {
            androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
            v.setPadding(0, statusBarInsets.top, 0, 0);
            return insets;
        });
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarPortalActivityDetails);
        tvTitle = findViewById(R.id.tvActDetailTitle);
        tvDesc = findViewById(R.id.tvActDetailDesc);
        tvType = findViewById(R.id.tvActDetailType);
        chipCategory = findViewById(R.id.chipActDetailCategory);

        layoutStudent = findViewById(R.id.layoutStudentInfo);
        tvStudent = findViewById(R.id.tvActDetailStudent);
        layoutSubject = findViewById(R.id.layoutSubjectInfo);
        tvSubject = findViewById(R.id.tvActDetailSubject);
        tvTeacher = findViewById(R.id.tvActDetailTeacher);
        tvTimestamp = findViewById(R.id.tvActDetailTimestamp);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void populateData() {
        tvTitle.setText(title != null ? title : "Portal Activity");
        tvDesc.setText(description != null ? description : "");
        tvType.setText(type != null ? type : "GENERAL");
        chipCategory.setText(entityType != null ? entityType.toUpperCase() : "GENERAL");

        if (studentName != null && !studentName.isEmpty()) {
            String s = studentName + (registerNo != null && !registerNo.isEmpty() ? " (" + registerNo + ")" : "");
            tvStudent.setText(s);
            layoutStudent.setVisibility(View.VISIBLE);
        } else {
            layoutStudent.setVisibility(View.GONE);
        }

        if (subjectName != null && !subjectName.isEmpty()) {
            tvSubject.setText(subjectName);
            layoutSubject.setVisibility(View.VISIBLE);
        } else {
            layoutSubject.setVisibility(View.GONE);
        }

        tvTeacher.setText(teacherName != null && !teacherName.isEmpty() ? teacherName : "Faculty Admin");

        if (timestampMs > 0) {
            String dateStr = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(new Date(timestampMs));
            tvTimestamp.setText(dateStr);
        } else {
            tvTimestamp.setText("Just now");
        }
    }
}
