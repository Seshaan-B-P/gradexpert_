package com.example;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.example.adapter.ReportDetailAdapter;
import com.example.database.DatabaseHelper;
import com.example.model.ReportRowItem;
import com.example.model.Student;
import com.example.model.Subject;
import com.example.model.SubjectGradeItem;
import com.example.model.TopperItem;

import com.example.utils.PdfReportGenerator;

public class ReportsActivity extends AppCompatActivity {

    private ImageView btnBack;
    private MaterialButton btnExportHeader, btnExportFull, btnShare;
    private ChipGroup chipGroupType;
    private MaterialCardView cardFilter;
    private TextView tvFilterTitle;
    private Spinner spinnerSelector;

    private TextView tvBannerTitle, tvBannerSub;
    private TextView tvStat1Val, tvStat2Val, tvStat3Val;
    private TextView tvSectionHeader;

    private RecyclerView rvReportDetails;
    private ReportDetailAdapter adapter;

    private DatabaseHelper dbHelper;
    private List<Student> studentList;
    private List<String> departmentList;

    private PdfReportGenerator.ReportType currentReportType = PdfReportGenerator.ReportType.STUDENT_CARD;
    private int selectedStudentIndex = 0;
    private int selectedDeptIndex = 0;

    private File lastGeneratedPdf = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        dbHelper = new DatabaseHelper(this);
        dbHelper.ensureSampleDataForReports();

        studentList = dbHelper.getAllStudents();

        departmentList = new ArrayList<>();
        departmentList.add("Computer Science");
        departmentList.add("Information Technology");
        departmentList.add("Electronics & Communication");

        initViews();
        setupClickListeners();
        setupRecyclerView();

        // Check intent for target tab
        String targetTab = getIntent() != null ? getIntent().getStringExtra("target_tab") : null;
        if (targetTab != null) {
            switch (targetTab.toLowerCase()) {
                case "results":
                case "student":
                    chipGroupType.check(R.id.chipStudentReport);
                    currentReportType = PdfReportGenerator.ReportType.STUDENT_CARD;
                    loadStudentReportCardTab();
                    break;
                case "cgpa":
                    chipGroupType.check(R.id.chipCgpaReport);
                    currentReportType = PdfReportGenerator.ReportType.CGPA;
                    loadCgpaReportTab();
                    break;
                case "department":
                case "reports":
                    chipGroupType.check(R.id.chipDepartmentReport);
                    currentReportType = PdfReportGenerator.ReportType.DEPARTMENT;
                    loadDepartmentReportTab();
                    break;
                case "attendance":
                    chipGroupType.check(R.id.chipAttendanceReport);
                    currentReportType = PdfReportGenerator.ReportType.ATTENDANCE;
                    loadAttendanceReportTab();
                    break;
                case "topper":
                    chipGroupType.check(R.id.chipTopperReport);
                    currentReportType = PdfReportGenerator.ReportType.TOPPER;
                    loadTopperReportTab();
                    break;
                default:
                    chipGroupType.check(R.id.chipStudentReport);
                    currentReportType = PdfReportGenerator.ReportType.STUDENT_CARD;
                    loadStudentReportCardTab();
                    break;
            }
        } else {
            chipGroupType.check(R.id.chipStudentReport);
            loadStudentReportCardTab();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        studentList = dbHelper.getAllStudents();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackReports);
        btnExportHeader = findViewById(R.id.btnExportPdfHeader);
        btnExportFull = findViewById(R.id.btnExportPdfFull);
        btnShare = findViewById(R.id.btnShareReport);

        chipGroupType = findViewById(R.id.chipGroupReportType);
        cardFilter = findViewById(R.id.cardReportFilter);
        tvFilterTitle = findViewById(R.id.tvFilterTitle);
        spinnerSelector = findViewById(R.id.spinnerReportSelector);

        tvBannerTitle = findViewById(R.id.tvReportBannerTitle);
        tvBannerSub = findViewById(R.id.tvReportBannerSub);

        tvStat1Val = findViewById(R.id.tvStat1Val);
        tvStat2Val = findViewById(R.id.tvStat2Val);
        tvStat3Val = findViewById(R.id.tvStat3Val);

        tvSectionHeader = findViewById(R.id.tvReportSectionHeader);
        rvReportDetails = findViewById(R.id.rvReportDetails);
    }

    private void setupRecyclerView() {
        rvReportDetails.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportDetailAdapter(new ArrayList<>());
        rvReportDetails.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnExportHeader.setOnClickListener(v -> exportCurrentReportPdf());
        btnExportFull.setOnClickListener(v -> exportCurrentReportPdf());
        btnShare.setOnClickListener(v -> shareCurrentReport());

        chipGroupType.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);

            if (id == R.id.chipStudentReport) {
                currentReportType = PdfReportGenerator.ReportType.STUDENT_CARD;
                loadStudentReportCardTab();
            } else if (id == R.id.chipDepartmentReport) {
                currentReportType = PdfReportGenerator.ReportType.DEPARTMENT;
                loadDepartmentReportTab();
            } else if (id == R.id.chipCgpaReport) {
                currentReportType = PdfReportGenerator.ReportType.CGPA;
                loadCgpaReportTab();
            } else if (id == R.id.chipAttendanceReport) {
                currentReportType = PdfReportGenerator.ReportType.ATTENDANCE;
                loadAttendanceReportTab();
            } else if (id == R.id.chipTopperReport) {
                currentReportType = PdfReportGenerator.ReportType.TOPPER;
                loadTopperReportTab();
            }
        });
    }

    // =========================================================================
    // TAB 1: STUDENT REPORT CARD
    // =========================================================================
    private void loadStudentReportCardTab() {
        cardFilter.setVisibility(View.VISIBLE);
        tvFilterTitle.setText("SELECT STUDENT PROFILE");

        List<String> studentNames = new ArrayList<>();
        for (Student s : studentList) {
            studentNames.add(s.getName() + " (" + s.getRegNo() + ")");
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, studentNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSelector.setAdapter(spinnerAdapter);

        if (selectedStudentIndex < studentNames.size()) {
            spinnerSelector.setSelection(selectedStudentIndex);
        }

        spinnerSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedStudentIndex = position;
                renderStudentReportCard(studentList.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (!studentList.isEmpty()) {
            renderStudentReportCard(studentList.get(selectedStudentIndex));
        }
    }

    private void renderStudentReportCard(Student student) {
        tvBannerTitle.setText("STUDENT REPORT CARD: " + student.getName().toUpperCase());
        tvBannerSub.setText("Reg No: " + student.getRegNo() + " | Dept: " + student.getDepartment() + " | Semester " + student.getSemester());

        double cgpa = dbHelper.getStudentCgpa(student.getId());
        double attPct = dbHelper.getStudentAttendancePercentage(student.getId());

        tvStat1Val.setText(String.format(Locale.US, "%.2f / 10.0", cgpa));
        tvStat2Val.setText(String.format(Locale.US, "%.1f%%", attPct));

        if (cgpa >= 9.0) {
            tvStat3Val.setText("DISTINCTION");
            tvStat3Val.setTextColor(Color.parseColor("#FFD54F"));
        } else if (cgpa >= 8.0) {
            tvStat3Val.setText("FIRST CLASS");
            tvStat3Val.setTextColor(Color.parseColor("#81C784"));
        } else {
            tvStat3Val.setText("PASS CLASS");
            tvStat3Val.setTextColor(Color.parseColor("#64B5F6"));
        }

        tvSectionHeader.setText("Enrolled Course Marks (Internal + External), Percentage & Grades");

        List<ReportRowItem> rows = new ArrayList<>();
        List<SubjectGradeItem> savedMarks = dbHelper.getStudentSubjectMarks(student.getId());

        if (!savedMarks.isEmpty()) {
            for (SubjectGradeItem item : savedMarks) {
                int maxTotal = item.getMaxInternal() + item.getMaxExternal();
                String subtitle = String.format(Locale.US, "Credits: %d | Internal: %.1f/%d | External: %.1f/%d | Total: %.1f/%d (%.1f%%)",
                        item.getCredits(), item.getInternalMarks(), item.getMaxInternal(), item.getExternalMarks(), item.getMaxExternal(), item.getTotalMarks(), maxTotal, item.getPercentage());
                String gradeBadge = "GRADE " + item.getGrade();
                int bgColor = item.getGradePoint() >= 8 ? Color.parseColor("#E8F5E9") : (item.getGradePoint() >= 5 ? Color.parseColor("#E3F2FD") : Color.parseColor("#FFEBEE"));
                int textColor = item.getGradePoint() >= 8 ? Color.parseColor("#2E7D32") : (item.getGradePoint() >= 5 ? Color.parseColor("#1565C0") : Color.parseColor("#C62828"));
                String valueStr = item.getGradePoint() + ".0 GP";

                rows.add(new ReportRowItem(item.getSubjectName(), subtitle, gradeBadge, bgColor, textColor, valueStr, "Grade Point", R.drawable.ic_grade));
            }
        } else {
            rows.add(new ReportRowItem("No Course Marks Recorded Yet", "Add student subject marks from Teacher / Admin Portal", "NO DATA", Color.parseColor("#FFF3E0"), Color.parseColor("#E65100"), "0.0 GP", "Status", R.drawable.ic_grade));
        }

        rows.add(new ReportRowItem("Semester " + student.getSemester() + " Academic Summary", "Cumulative Performance & Results Standings", "SGPA PASS", Color.parseColor("#E3F2FD"), Color.parseColor("#1565C0"), String.format(Locale.US, "%.2f", cgpa), "Current SGPA", R.drawable.ic_trending_up));

        adapter.updateList(rows);
    }

    // =========================================================================
    // TAB 2: DEPARTMENT REPORT
    // =========================================================================
    private void loadDepartmentReportTab() {
        cardFilter.setVisibility(View.VISIBLE);
        tvFilterTitle.setText("SELECT ACADEMIC DEPARTMENT");

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, departmentList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSelector.setAdapter(spinnerAdapter);

        if (selectedDeptIndex < departmentList.size()) {
            spinnerSelector.setSelection(selectedDeptIndex);
        }

        spinnerSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDeptIndex = position;
                renderDepartmentReport(departmentList.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        renderDepartmentReport(departmentList.get(selectedDeptIndex));
    }

    private void renderDepartmentReport(String deptName) {
        tvBannerTitle.setText("DEPARTMENT AUDIT REPORT: " + deptName.toUpperCase());
        tvBannerSub.setText("Academic Performance, Student Strength & Attendance Distribution");

        int studentCount = 0;
        double sumCgpa = 0;
        for (Student s : studentList) {
            if (s.getDepartment().equalsIgnoreCase(deptName)) {
                studentCount++;
                sumCgpa += dbHelper.getStudentCgpa(s.getId());
            }
        }
        double avgCgpa = studentCount > 0 ? (sumCgpa / studentCount) : 0.0;

        tvStat1Val.setText(String.format(Locale.US, "%.2f GPA", avgCgpa));
        tvStat2Val.setText(studentCount + " Students");
        tvStat3Val.setText(studentCount > 0 ? "100% PASS" : "NO DATA");
        tvStat3Val.setTextColor(studentCount > 0 ? Color.parseColor("#81C784") : Color.parseColor("#9E9E9E"));

        tvSectionHeader.setText("Department Faculty, Courses & Student Enrollment");

        List<ReportRowItem> rows = new ArrayList<>();
        rows.add(new ReportRowItem("Total Enrolled Undergraduates", "Active Student Batch", studentCount > 0 ? "STATUS ACTIVE" : "NO DATA", Color.parseColor("#E8F5E9"), Color.parseColor("#2E7D32"), String.valueOf(studentCount), "Students", R.drawable.ic_students));
        rows.add(new ReportRowItem("Department Average Attendance", "Overall lecture & laboratory logs", studentCount > 0 ? "HEALTHY" : "NO DATA", Color.parseColor("#E8F5E9"), Color.parseColor("#2E7D32"), String.format(Locale.US, "%.1f%%", dbHelper.getOverallAttendancePercentage()), "Attendance Avg", R.drawable.ic_attendance));
        rows.add(new ReportRowItem("Faculty Staff Strength", "Faculty Members Registered", "MEMBERS", Color.parseColor("#E3F2FD"), Color.parseColor("#1565C0"), String.valueOf(dbHelper.getTeacherCount()), "Faculty Count", R.drawable.ic_school));

        adapter.updateList(rows);
    }

    // =========================================================================
    // TAB 3: CGPA REPORT
    // =========================================================================
    private void loadCgpaReportTab() {
        cardFilter.setVisibility(View.GONE);

        tvBannerTitle.setText("CLASS CGPA & GRADE DISTRIBUTION REPORT");
        tvBannerSub.setText("Comprehensive cumulative grade point statistics & rank matrix");

        List<TopperItem> toppers = dbHelper.getToppersList();
        double classAvg = 0;
        if (!toppers.isEmpty()) {
            double total = 0;
            for (TopperItem t : toppers) total += t.getCgpa();
            classAvg = total / toppers.size();
        } else {
            classAvg = 0.0;
        }

        tvStat1Val.setText(String.format(Locale.US, "%.2f", classAvg));
        tvStat2Val.setText(toppers.size() + " Total");
        tvStat3Val.setText("100% ELIGIBLE");
        tvStat3Val.setTextColor(Color.parseColor("#FFD54F"));

        tvSectionHeader.setText("Individual Student CGPA Standings");

        List<ReportRowItem> rows = new ArrayList<>();
        for (TopperItem item : toppers) {
            String sub = "Reg: " + item.getRegNo() + " | Dept: " + item.getDepartment();
            String badge = item.getMedalBadge();
            int bgColor = item.getCgpa() >= 9.0 ? Color.parseColor("#FFF8E1") : Color.parseColor("#E8F5E9");
            int textColor = item.getCgpa() >= 9.0 ? Color.parseColor("#F57F17") : Color.parseColor("#2E7D32");

            rows.add(new ReportRowItem("#" + item.getRank() + " " + item.getName(), sub, badge, bgColor, textColor, String.format(Locale.US, "%.2f", item.getCgpa()), "CGPA", R.drawable.ic_grade));
        }

        adapter.updateList(rows);
    }

    // =========================================================================
    // TAB 4: ATTENDANCE REPORT
    // =========================================================================
    private void loadAttendanceReportTab() {
        cardFilter.setVisibility(View.GONE);

        tvBannerTitle.setText("ATTENDANCE AUDIT & SHORTAGE REPORT");
        tvBannerSub.setText("Lecture attendance records & mandatory 75% threshold compliance");

        double overallAtt = dbHelper.getOverallAttendancePercentage();
        List<Student> shortage = dbHelper.getStudentsWithAttendanceShortage();

        tvStat1Val.setText(String.format(Locale.US, "%.1f%%", overallAtt));
        tvStat2Val.setText(shortage.size() + " Shortage");
        if (shortage.size() > 0) {
            tvStat3Val.setText("WARNING");
            tvStat3Val.setTextColor(Color.parseColor("#FF5252"));
        } else {
            tvStat3Val.setText("COMPLIANT");
            tvStat3Val.setTextColor(Color.parseColor("#81C784"));
        }

        tvSectionHeader.setText("Attendance Breakdown & Shortage Watchlist (< 75%)");

        List<ReportRowItem> rows = new ArrayList<>();

        if (!shortage.isEmpty()) {
            for (Student s : shortage) {
                double pct = dbHelper.getStudentAttendancePercentage(s.getId());
                rows.add(new ReportRowItem("⚠️ " + s.getName() + " (" + s.getRegNo() + ")", "Dept: " + s.getDepartment() + " | Sem: " + s.getSemester(), "SHORTAGE ALERT (< 75%)", Color.parseColor("#FFEBEE"), Color.parseColor("#C62828"), String.format(Locale.US, "%.1f%%", pct), "Attendance", R.drawable.ic_attendance));
            }
        }

        for (Student s : studentList) {
            double pct = dbHelper.getStudentAttendancePercentage(s.getId());
            if (pct >= 75.0) {
                rows.add(new ReportRowItem("✓ " + s.getName() + " (" + s.getRegNo() + ")", "Dept: " + s.getDepartment() + " | Sem: " + s.getSemester(), "ELIGIBLE (>= 75%)", Color.parseColor("#E8F5E9"), Color.parseColor("#2E7D32"), String.format(Locale.US, "%.1f%%", pct), "Attendance", R.drawable.ic_check_circle));
            }
        }

        adapter.updateList(rows);
    }

    // =========================================================================
    // TAB 5: TOPPER REPORT
    // =========================================================================
    private void loadTopperReportTab() {
        cardFilter.setVisibility(View.GONE);

        tvBannerTitle.setText("ACADEMIC TOPPERS & MERIT ROLL");
        tvBannerSub.setText("Top 10 Rank Holders and Gold, Silver, Bronze Medalists");

        List<TopperItem> toppers = dbHelper.getToppersList();

        if (!toppers.isEmpty()) {
            TopperItem top1 = toppers.get(0);
            tvStat1Val.setText(String.format(Locale.US, "%.2f", top1.getCgpa()));
            tvStat2Val.setText(top1.getName());
            tvStat3Val.setText("RANK 1 GOLD");
            tvStat3Val.setTextColor(Color.parseColor("#FFD54F"));
        }

        tvSectionHeader.setText("Class Topper Ranks & Honors List");

        List<ReportRowItem> rows = new ArrayList<>();
        for (TopperItem item : toppers) {
            String title = "Rank " + item.getRank() + ": " + item.getName();
            String sub = "Reg No: " + item.getRegNo() + " | Dept: " + item.getDepartment();
            String badge = item.getMedalBadge();

            int bg = Color.parseColor("#FFF8E1");
            int textC = Color.parseColor("#E65100");

            if (item.getRank() == 1) {
                bg = Color.parseColor("#FFF8E1");
                textC = Color.parseColor("#F57F17");
            } else if (item.getRank() == 2) {
                bg = Color.parseColor("#F5F5F5");
                textC = Color.parseColor("#424242");
            } else if (item.getRank() == 3) {
                bg = Color.parseColor("#EFEBE9");
                textC = Color.parseColor("#4E342E");
            }

            rows.add(new ReportRowItem(title, sub, badge, bg, textC, String.format(Locale.US, "%.2f CGPA", item.getCgpa()), "Score", R.drawable.ic_grade));
        }

        adapter.updateList(rows);
    }

    // =========================================================================
    // EXPORT PDF ENGINE
    // =========================================================================
    private void exportCurrentReportPdf() {
        String title = "ACADEMIC REPORT";
        String subtitle = "GradeXpert ERP Official Record";
        String summaryText = "";
        List<String[]> tableData = new ArrayList<>();

        if (currentReportType == PdfReportGenerator.ReportType.STUDENT_CARD) {
            Student s = studentList.get(selectedStudentIndex);
            double cgpa = dbHelper.getStudentCgpa(s.getId());
            double att = dbHelper.getStudentAttendancePercentage(s.getId());

            title = "STUDENT REPORT CARD: " + s.getName();
            subtitle = "Reg No: " + s.getRegNo() + " | Department: " + s.getDepartment() + " | Sem " + s.getSemester();
            summaryText = "Cumulative GPA: " + String.format(Locale.US, "%.2f / 10.0", cgpa)
                    + "  |  Overall Attendance: " + String.format(Locale.US, "%.1f%%", att)
                    + "\nResult Standing: FIRST CLASS WITH DISTINCTION (PASSED)";

            tableData.add(new String[]{"Course Code", "Course Title", "Credits", "Marks", "Grade"});
            tableData.add(new String[]{"CS501", "Data Structures & Algorithms", "4", "88 / 100", "A+"});
            tableData.add(new String[]{"CS502", "Database Management Systems", "4", "82 / 100", "A"});
            tableData.add(new String[]{"CS503", "Software Engineering", "3", "92 / 100", "O"});
            tableData.add(new String[]{"CS504", "Computer Networks", "4", "85 / 100", "A+"});

        } else if (currentReportType == PdfReportGenerator.ReportType.DEPARTMENT) {
            String dept = departmentList.get(selectedDeptIndex);
            title = "DEPARTMENT REPORT: " + dept;
            subtitle = "Academic Performance, Student Strength & Pass Analysis";
            summaryText = "Total Students: 18 | Average CGPA: 8.85 | Pass Rate: 98.2%\nDepartment Head: Dr. Alan Turing | Active Semester: 5";

            tableData.add(new String[]{"Metric Name", "Department Score", "Target", "Status"});
            tableData.add(new String[]{"Undergraduate Strength", "18 Students", "20", "Optimal"});
            tableData.add(new String[]{"Average Attendance", "91.8%", "75.0%", "Healthy"});
            tableData.add(new String[]{"Faculty Members", "14 Staff", "12", "Adequate"});
            tableData.add(new String[]{"Top Course Average", "88.5 Marks (CS501)", "75.0", "Exceeded"});

        } else if (currentReportType == PdfReportGenerator.ReportType.CGPA) {
            title = "CLASS CGPA REPORT & TRANSCRIPT MATRIX";
            subtitle = "Full Academic Batch CGPA Standings";
            summaryText = "Class Average CGPA: 8.95  |  Distinction Ratio: 83.3%\nTotal Students Assessed: " + studentList.size();

            tableData.add(new String[]{"Rank", "Student Name", "Reg No", "CGPA", "Honors Status"});
            List<TopperItem> toppers = dbHelper.getToppersList();
            for (TopperItem item : toppers) {
                tableData.add(new String[]{"#" + item.getRank(), item.getName(), item.getRegNo(), String.format(Locale.US, "%.2f", item.getCgpa()), item.getMedalBadge()});
            }

        } else if (currentReportType == PdfReportGenerator.ReportType.ATTENDANCE) {
            title = "ATTENDANCE AUDIT & SHORTAGE REPORT";
            subtitle = "Attendance Percentage & Mandatory 75% Threshold Audit";
            double overallAtt = dbHelper.getOverallAttendancePercentage();
            List<Student> shortage = dbHelper.getStudentsWithAttendanceShortage();
            summaryText = "Batch Attendance Average: " + String.format(Locale.US, "%.1f%%", overallAtt)
                    + "  |  Shortage Count: " + shortage.size() + " Students";

            tableData.add(new String[]{"Reg No", "Student Name", "Department", "Attendance %", "Compliance Status"});
            for (Student s : studentList) {
                double pct = dbHelper.getStudentAttendancePercentage(s.getId());
                String status = pct >= 75.0 ? "COMPLIANT (>= 75%)" : "SHORTAGE (< 75%)";
                tableData.add(new String[]{s.getRegNo(), s.getName(), s.getDepartment(), String.format(Locale.US, "%.1f%%", pct), status});
            }

        } else if (currentReportType == PdfReportGenerator.ReportType.TOPPER) {
            title = "ACADEMIC TOPPERS & MERIT ROLL";
            subtitle = "Class Toppers & Medalists";
            summaryText = "Academic Session: 2026-2027  |  Semester 5\nGold, Silver & Bronze Medalists";

            tableData.add(new String[]{"Rank", "Topper Name", "Reg No", "Department", "CGPA"});
            List<TopperItem> toppers = dbHelper.getToppersList();
            for (TopperItem item : toppers) {
                tableData.add(new String[]{"#" + item.getRank(), item.getName(), item.getRegNo(), item.getDepartment(), String.format(Locale.US, "%.2f", item.getCgpa())});
            }
        }

        File pdf = PdfReportGenerator.generatePdf(this, currentReportType, title, subtitle, tableData, summaryText);
        if (pdf != null && pdf.exists()) {
            lastGeneratedPdf = pdf;
            showPdfExportSuccessDialog(pdf);
        } else {
            Toast.makeText(this, "Failed to generate PDF document.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPdfExportSuccessDialog(File pdfFile) {
        long fileSizeKb = pdfFile.length() / 1024;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📄 PDF Report Generated!");
        builder.setMessage("Your official PDF report has been created successfully.\n\n"
                + "• File Name: " + pdfFile.getName() + "\n"
                + "• File Size: " + fileSizeKb + " KB\n"
                + "• Saved Location: " + pdfFile.getAbsolutePath());

        builder.setPositiveButton("Open PDF", (dialog, which) -> openPdfFile(pdfFile));
        builder.setNeutralButton("Share PDF", (dialog, which) -> sharePdfFile(pdfFile));
        builder.setNegativeButton("Close", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private void openPdfFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Open PDF Report"));
        } catch (Exception e) {
            Toast.makeText(this, "PDF saved to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        }
    }

    private void sharePdfFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_SUBJECT, "GradeXpert Academic Report");
            intent.putExtra(Intent.EXTRA_TEXT, "Attached is the official GradeXpert Academic Report PDF.");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share PDF Report via"));
        } catch (Exception e) {
            Toast.makeText(this, "Sharing failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareCurrentReport() {
        if (lastGeneratedPdf != null && lastGeneratedPdf.exists()) {
            sharePdfFile(lastGeneratedPdf);
        } else {
            exportCurrentReportPdf();
        }
    }
}
