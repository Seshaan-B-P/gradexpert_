package com.example;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Activity displaying detailed individual academic performance transcript and subject breakdown for a student.
 */
public class StudentAcademicReportActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView tvName;
    private TextView tvRegNo;
    private TextView tvDeptSem;

    private TextView tvMarks;
    private TextView tvAttendance;
    private TextView tvSgpa;

    private BarChart chartBar;
    private MaterialButton btnDownloadPdf;
    private MaterialButton btnShareTranscript;

    private String studentName = "Student";
    private String registerNo = "MCA001";
    private String department = "Master of Computer Applications";
    private String semester = "Semester III";
    private double sgpa = 8.7;
    private double attendancePct = 91.0;
    private double marksPct = 86.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_academic_report);

        readIntentExtras();
        initViews();
        setupToolbar();
        setupWindowInsets();
        populateData();
        setupChart();
        setupActions();
    }

    private void readIntentExtras() {
        if (getIntent() != null) {
            String n = getIntent().getStringExtra("studentName");
            String r = getIntent().getStringExtra("registerNo");
            String d = getIntent().getStringExtra("department");
            String s = getIntent().getStringExtra("semester");

            if (n != null && !n.isEmpty()) studentName = n;
            if (r != null && !r.isEmpty()) registerNo = r;
            if (d != null && !d.isEmpty()) department = d;
            if (s != null && !s.isEmpty()) semester = s;

            sgpa = getIntent().getDoubleExtra("sgpa", 8.7);
            attendancePct = getIntent().getDoubleExtra("attendancePct", 91.0);
            marksPct = getIntent().getDoubleExtra("marksPct", 86.0);
        }
    }

    private void setupWindowInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbarStudentReport), (v, insets) -> {
            androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
            v.setPadding(0, statusBarInsets.top, 0, 0);
            return insets;
        });
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarStudentReport);
        tvName = findViewById(R.id.tvIndivStudentName);
        tvRegNo = findViewById(R.id.tvIndivStudentRegNo);
        tvDeptSem = findViewById(R.id.tvIndivStudentDeptSem);

        tvMarks = findViewById(R.id.tvIndivMarksVal);
        tvAttendance = findViewById(R.id.tvIndivAttendanceVal);
        tvSgpa = findViewById(R.id.tvIndivSgpaVal);

        chartBar = findViewById(R.id.chartStudentSubjectBar);
        btnDownloadPdf = findViewById(R.id.btnDownloadStudentPdf);
        btnShareTranscript = findViewById(R.id.btnShareStudentTranscript);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void populateData() {
        tvName.setText(studentName);
        tvRegNo.setText("Register No: " + registerNo);
        tvDeptSem.setText(department + " • " + semester);

        tvMarks.setText(String.format(Locale.US, "%.1f%%", marksPct));
        tvAttendance.setText(String.format(Locale.US, "%.1f%%", attendancePct));
        tvSgpa.setText(String.format(Locale.US, "%.2f", sgpa));
    }

    private void setupChart() {
        chartBar.getDescription().setEnabled(false);
        chartBar.setDrawGridBackground(false);
        chartBar.getAxisRight().setEnabled(false);

        XAxis xAxis = chartBar.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, 88f));
        entries.add(new BarEntry(1, 84f));
        entries.add(new BarEntry(2, 87f));
        entries.add(new BarEntry(3, 80f));

        List<String> subjects = new ArrayList<>();
        subjects.add("Java");
        subjects.add("DBMS");
        subjects.add("Cloud");
        subjects.add("DS");

        BarDataSet dataSet = new BarDataSet(entries, "Subject Marks (%)");
        dataSet.setColor(Color.parseColor("#4F46E5"));
        dataSet.setValueTextColor(Color.DKGRAY);
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        chartBar.getXAxis().setValueFormatter(new IndexAxisValueFormatter(subjects));
        chartBar.setData(barData);
        chartBar.invalidate();
    }

    private void setupActions() {
        btnDownloadPdf.setOnClickListener(v -> {
            Toast.makeText(this, "Downloading PDF Transcript for " + studentName + "...", Toast.LENGTH_LONG).show();
        });

        btnShareTranscript.setOnClickListener(v -> {
            Toast.makeText(this, "Sharing Transcript for " + studentName + "...", Toast.LENGTH_LONG).show();
        });
    }
}
