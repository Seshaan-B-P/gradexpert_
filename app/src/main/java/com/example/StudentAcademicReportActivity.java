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
    private java.io.File lastExportedFile = null;

    private String studentName = "Student";
    private String registerNo = "MCA001";
    private String department = "Master of Computer Applications";
    private String semester = "Semester III";
    private int studentId = 0;
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

            studentId = getIntent().getIntExtra("studentId", getIntent().getIntExtra("student_id", 0));
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
        btnDownloadPdf.setOnClickListener(v -> exportPdf());

        btnShareTranscript.setOnClickListener(v -> shareTranscript());
    }

    private void exportPdf() {
        List<com.example.model.SubjectGradeItem> subjects = getSubjectsList();

        double cgpa = sgpa > 0 ? (sgpa + 0.1) : 8.8;
        double totalMarks = (marksPct / 100.0) * (subjects.size() * 100);

        java.io.File pdfFile = com.example.utils.PdfReportGenerator.generateStudentGradeReportPdf(
                this,
                studentName,
                registerNo,
                department,
                semester,
                sgpa,
                cgpa,
                marksPct,
                totalMarks,
                subjects
        );

        if (pdfFile != null && pdfFile.exists()) {
            lastExportedFile = pdfFile;
            Toast.makeText(this, "PDF Grade Report saved to: Documents/Grade_Reports/" + pdfFile.getName(), Toast.LENGTH_SHORT).show();
            com.example.utils.PdfReportGenerator.showExportSuccessDialog(this, pdfFile, studentName, semester);
        } else {
            Toast.makeText(this, "Failed to generate PDF grade report.", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareTranscript() {
        if (lastExportedFile != null && lastExportedFile.exists()) {
            com.example.utils.PdfReportGenerator.sharePdfFile(this, lastExportedFile, "Grade Report - " + studentName);
        } else {
            List<com.example.model.SubjectGradeItem> subjects = getSubjectsList();

            double cgpa = sgpa > 0 ? (sgpa + 0.1) : 8.8;
            double totalMarks = (marksPct / 100.0) * (subjects.size() * 100);

            java.io.File pdfFile = com.example.utils.PdfReportGenerator.generateStudentGradeReportPdf(
                    this,
                    studentName,
                    registerNo,
                    department,
                    semester,
                    sgpa,
                    cgpa,
                    marksPct,
                    totalMarks,
                    subjects
            );

            if (pdfFile != null && pdfFile.exists()) {
                lastExportedFile = pdfFile;
                com.example.utils.PdfReportGenerator.sharePdfFile(this, pdfFile, "Grade Report - " + studentName);
            } else {
                Toast.makeText(this, "Failed to export PDF transcript.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private List<com.example.model.SubjectGradeItem> getSubjectsList() {
        List<com.example.model.SubjectGradeItem> subjects = null;
        if (studentId > 0) {
            try {
                com.example.database.DatabaseHelper dbHelper = new com.example.database.DatabaseHelper(this);
                subjects = dbHelper.getStudentSubjectMarks(studentId);
            } catch (Exception ignored) {}
        }
        if (subjects == null || subjects.isEmpty()) {
            subjects = new ArrayList<>();
            subjects.add(new com.example.model.SubjectGradeItem("CS501", "Advanced Java & Frameworks", 4, 28.0, 62.0));
            subjects.add(new com.example.model.SubjectGradeItem("CS502", "Database Management Systems", 4, 26.0, 58.0));
            subjects.add(new com.example.model.SubjectGradeItem("CS503", "Cloud Computing & Modern DevOps", 4, 28.0, 64.0));
            subjects.add(new com.example.model.SubjectGradeItem("CS504", "Data Structures & Algorithm Design", 3, 25.0, 56.0));
            subjects.add(new com.example.model.SubjectGradeItem("CS505", "Web Technologies & App Lab", 3, 29.0, 66.0));
        }
        return subjects;
    }

}
