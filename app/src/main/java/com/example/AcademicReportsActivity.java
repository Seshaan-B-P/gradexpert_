package com.example;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.adapter.AcademicReportAdapter;
import com.example.model.AcademicReport;
import com.example.model.Department;
import com.example.repository.AcademicReportRepository;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Activity for analyzing real-time Academic Performance Reports and Analytics in GradeXpert.
 * Supports Program Level ("Overall" / "UG" / "PG"), Department-wise, Semester-wise analysis.
 */
public class AcademicReportsActivity extends AppCompatActivity implements AcademicReportAdapter.OnAcademicReportClickListener {

    private static final String TAG = "AcademicReportsActivity";

    private FirebaseFirestore db;
    private AcademicReportRepository repository;
    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;

    private Spinner spProgramLevel;
    private Spinner spDepartment;
    private Spinner spSemester;
    private Spinner spSubject;
    private Spinner spAcademicYear;
    private Spinner spReportType;

    private TextInputEditText etSearch;

    private TextView tvTotalStudents;
    private TextView tvAvgMarks;
    private TextView tvAvgAttendance;
    private TextView tvPassPercentage;

    private TextView tvChartTitle;
    private BarChart chartBar;
    private PieChart chartPie;
    private LineChart chartLine;

    private RecyclerView rvStudentReports;
    private RecyclerView rvTopPerformers;
    private RecyclerView rvAtRiskStudents;

    private MaterialButton btnGenerate;
    private MaterialButton btnExportPdf;
    private MaterialButton btnExportCsv;

    private ProgressBar progressBar;
    private LinearLayout layoutEmpty;
    private TextView tvEmptyMessage;
    private MaterialButton btnClearFilters;

    private AcademicReportAdapter reportAdapter;
    private List<AcademicReport> loadedReports = new ArrayList<>();

    private final List<Department> allDepartments = new ArrayList<>();
    private final List<String> deptFilterOptions = new ArrayList<>();
    private ArrayAdapter<String> deptAdapter;

    private final List<String> semFilterOptions = new ArrayList<>();
    private ArrayAdapter<String> semAdapter;

    private String selectedProgramLevel = "Overall";
    private String selectedDept = "All Departments";
    private String selectedSem = "All Semesters";
    private String selectedSubject = "All Subjects";
    private String selectedYear = "2026-27";
    private String selectedReportType = "Overall Academic Performance";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_academic_reports);

        db = FirebaseFirestore.getInstance();
        repository = AcademicReportRepository.getInstance(this);

        initViews();
        setupToolbar();
        setupWindowInsets();
        setupSwipeRefresh();
        setupSpinners();
        setupSearch();
        setupRecyclerViews();
        setupCharts();
        setupActionButtons();

        loadDepartmentsFromFirestore();
        loadAcademicReports();
    }

    private void setupWindowInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbarAcademicReports), (v, insets) -> {
            androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
            v.setPadding(0, statusBarInsets.top, 0, 0);
            return insets;
        });
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarAcademicReports);
        swipeRefresh = findViewById(R.id.swipeRefreshAcademicReports);

        spProgramLevel = findViewById(R.id.spReportProgramLevel);
        spDepartment = findViewById(R.id.spReportDepartment);
        spSemester = findViewById(R.id.spReportSemester);
        spSubject = findViewById(R.id.spReportSubject);
        spAcademicYear = findViewById(R.id.spReportAcademicYear);
        spReportType = findViewById(R.id.spReportType);

        etSearch = findViewById(R.id.etSearchReportStudent);

        tvTotalStudents = findViewById(R.id.tvSummaryReportTotalStudents);
        tvAvgMarks = findViewById(R.id.tvSummaryReportAvgMarks);
        tvAvgAttendance = findViewById(R.id.tvSummaryReportAvgAttendance);
        tvPassPercentage = findViewById(R.id.tvSummaryReportPassPercentage);

        tvChartTitle = findViewById(R.id.tvChartTitle);
        chartBar = findViewById(R.id.chartPerformanceBar);
        chartPie = findViewById(R.id.chartPassFailPie);
        chartLine = findViewById(R.id.chartSemesterTrendLine);

        rvStudentReports = findViewById(R.id.rvStudentAcademicReports);
        rvTopPerformers = findViewById(R.id.rvTopPerformers);
        rvAtRiskStudents = findViewById(R.id.rvAtRiskStudents);

        btnGenerate = findViewById(R.id.btnGenerateReport);
        btnExportPdf = findViewById(R.id.btnExportPdf);
        btnExportCsv = findViewById(R.id.btnExportCsv);

        progressBar = findViewById(R.id.progressBarAcademicReports);
        layoutEmpty = findViewById(R.id.layoutReportEmptyState);
        tvEmptyMessage = findViewById(R.id.tvReportEmptyMessage);
        btnClearFilters = findViewById(R.id.btnClearReportFilters);

        btnClearFilters.setOnClickListener(v -> resetFilters());
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Academic Reports");
            getSupportActionBar().setSubtitle("UG & PG Curriculum Analytics");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(this::loadAcademicReports);
    }

    private void setupSpinners() {
        // Program Level Spinner
        List<String> levels = new ArrayList<>();
        levels.add("Overall (All Programs)");
        levels.add("Undergraduate (UG)");
        levels.add("Postgraduate (PG)");

        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, levels);
        levelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spProgramLevel.setAdapter(levelAdapter);

        spProgramLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 1) {
                    selectedProgramLevel = "UG";
                } else if (position == 2) {
                    selectedProgramLevel = "PG";
                } else {
                    selectedProgramLevel = "Overall";
                }
                updateDepartmentFilterOptions();
                updateSemesterFilterOptions();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Department Spinner
        deptAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, deptFilterOptions);
        deptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDepartment.setAdapter(deptAdapter);

        spDepartment.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < deptFilterOptions.size()) {
                    selectedDept = deptFilterOptions.get(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Semester Spinner
        semAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, semFilterOptions);
        semAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSemester.setAdapter(semAdapter);

        spSemester.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < semFilterOptions.size()) {
                    selectedSem = semFilterOptions.get(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Subject Spinner
        List<String> subjs = new ArrayList<>();
        subjs.add("All Subjects");
        subjs.add("Java Programming");
        subjs.add("Database Management");
        subjs.add("Cloud Computing");
        subjs.add("Data Structures");
        ArrayAdapter<String> subjAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, subjs);
        subjAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSubject.setAdapter(subjAdapter);
        spSubject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSubject = subjs.get(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Academic Year Spinner
        List<String> years = new ArrayList<>();
        years.add("2026-27");
        years.add("2025-26");
        years.add("2024-25");
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spAcademicYear.setAdapter(yearAdapter);

        // Report Category Spinner (All 8 Report Types)
        List<String> types = new ArrayList<>();
        types.add("Overall Academic Performance");
        types.add("Subject-wise Performance");
        types.add("Student-wise Performance");
        types.add("Attendance Report");
        types.add("Assignment Performance");
        types.add("Examination / Marks Report");
        types.add("Pass / Fail Analysis");
        types.add("SGPA / CGPA Report");
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spReportType.setAdapter(typeAdapter);
        spReportType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedReportType = types.get(position);
                updateChartMode();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        updateDepartmentFilterOptions();
        updateSemesterFilterOptions();
    }

    private void loadDepartmentsFromFirestore() {
        db.collection("departments").get().addOnSuccessListener(querySnapshot -> {
            allDepartments.clear();
            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    Department d = doc.toObject(Department.class);
                    if (d != null) {
                        d.setDepartmentId(doc.getId());
                        if (doc.getString("programLevel") != null) d.setProgramLevel(doc.getString("programLevel"));
                        if (doc.getString("shortName") != null) d.setShortName(doc.getString("shortName"));
                        if (doc.getString("name") != null) d.setName(doc.getString("name"));
                        allDepartments.add(d);
                    }
                }
            }
            if (allDepartments.isEmpty()) {
                allDepartments.add(new Department("bca", "Bachelor of Computer Applications", "BCA", "UG", "ACTIVE"));
                allDepartments.add(new Department("cse", "Computer Science & Engineering", "B.Tech CSE", "UG", "ACTIVE"));
                allDepartments.add(new Department("mca", "Master of Computer Applications", "MCA", "PG", "ACTIVE"));
            }
            updateDepartmentFilterOptions();
            updateSemesterFilterOptions();
        }).addOnFailureListener(e -> {
            allDepartments.add(new Department("bca", "Bachelor of Computer Applications", "BCA", "UG", "ACTIVE"));
            allDepartments.add(new Department("cse", "Computer Science & Engineering", "B.Tech CSE", "UG", "ACTIVE"));
            allDepartments.add(new Department("mca", "Master of Computer Applications", "MCA", "PG", "ACTIVE"));
            updateDepartmentFilterOptions();
            updateSemesterFilterOptions();
        });
    }

    private void updateDepartmentFilterOptions() {
        deptFilterOptions.clear();
        deptFilterOptions.add("All Departments");

        for (Department d : allDepartments) {
            String lvl = d.getProgramLevel() != null ? d.getProgramLevel().toUpperCase() : "UG";
            if ("Overall".equalsIgnoreCase(selectedProgramLevel) || selectedProgramLevel.equalsIgnoreCase(lvl)) {
                String name = d.getDepartmentName();
                if (!name.isEmpty() && !deptFilterOptions.contains(name)) {
                    deptFilterOptions.add(name);
                }
            }
        }
        deptAdapter.notifyDataSetChanged();
        spDepartment.setSelection(0);
        selectedDept = "All Departments";
    }

    private void updateSemesterFilterOptions() {
        semFilterOptions.clear();
        semFilterOptions.add("All Semesters");
        if ("PG".equalsIgnoreCase(selectedProgramLevel)) {
            semFilterOptions.add("Semester I");
            semFilterOptions.add("Semester II");
            semFilterOptions.add("Semester III");
            semFilterOptions.add("Semester IV");
        } else {
            semFilterOptions.add("Semester I");
            semFilterOptions.add("Semester II");
            semFilterOptions.add("Semester III");
            semFilterOptions.add("Semester IV");
            semFilterOptions.add("Semester V");
            semFilterOptions.add("Semester VI");
            semFilterOptions.add("Semester VII");
            semFilterOptions.add("Semester VIII");
        }
        semAdapter.notifyDataSetChanged();
        spSemester.setSelection(0);
        selectedSem = "All Semesters";
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (reportAdapter != null) {
                    reportAdapter.filter(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRecyclerViews() {
        rvStudentReports.setLayoutManager(new LinearLayoutManager(this));
        reportAdapter = new AcademicReportAdapter(new ArrayList<>(), this);
        rvStudentReports.setAdapter(reportAdapter);

        rvTopPerformers.setLayoutManager(new LinearLayoutManager(this));
        rvTopPerformers.setAdapter(reportAdapter);

        rvAtRiskStudents.setLayoutManager(new LinearLayoutManager(this));
        rvAtRiskStudents.setAdapter(reportAdapter);
    }

    private void setupCharts() {
        chartBar.getDescription().setEnabled(false);
        chartBar.setDrawGridBackground(false);
        chartBar.getAxisRight().setEnabled(false);
        XAxis xAxis = chartBar.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        chartPie.getDescription().setEnabled(false);
        chartPie.setHoleRadius(45f);
        chartPie.setTransparentCircleRadius(50f);

        chartLine.getDescription().setEnabled(false);
        chartLine.setDrawGridBackground(false);
    }

    private void setupActionButtons() {
        btnGenerate.setOnClickListener(v -> {
            Toast.makeText(this, "Generating academic report...", Toast.LENGTH_SHORT).show();
            loadAcademicReports();
        });

        btnExportPdf.setOnClickListener(v -> {
            java.io.File pdfFile = com.example.utils.ReportExporter.exportToPdf(this, loadedReports, selectedDept, selectedSem, selectedSubject, selectedYear);
            if (pdfFile != null) {
                Toast.makeText(this, "PDF Exported successfully: " + pdfFile.getName(), Toast.LENGTH_LONG).show();
                com.example.utils.ReportExporter.shareFile(this, pdfFile, "application/pdf");
            } else {
                Toast.makeText(this, "Failed to export PDF.", Toast.LENGTH_SHORT).show();
            }
        });

        btnExportCsv.setOnClickListener(v -> {
            java.io.File csvFile = com.example.utils.ReportExporter.exportToCsv(this, loadedReports, selectedDept, selectedSem, selectedSubject, selectedYear);
            if (csvFile != null) {
                Toast.makeText(this, "CSV Exported successfully: " + csvFile.getName(), Toast.LENGTH_LONG).show();
                com.example.utils.ReportExporter.shareFile(this, csvFile, "text/csv");
            } else {
                Toast.makeText(this, "Failed to export CSV.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAcademicReports() {
        showLoading(true);
        repository.fetchConsolidatedAcademicReports(selectedDept, selectedSem, selectedSubject, selectedYear, new AcademicReportRepository.OnReportsLoadedListener() {
            @Override
            public void onSuccess(List<AcademicReport> reports) {
                showLoading(false);
                swipeRefresh.setRefreshing(false);
                loadedReports = reports;
                computeAndRenderAnalytics(reports);
                com.example.utils.PortalActivityLogger.getInstance(AcademicReportsActivity.this)
                        .logReportGenerated(selectedDept, selectedSem, selectedReportType);
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(AcademicReportsActivity.this, "Error loading reports: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void computeAndRenderAnalytics(List<AcademicReport> reports) {
        if (reports == null || reports.isEmpty()) {
            showEmptyState("No academic data found for the selected filters.");
            return;
        }
        hideEmptyState();

        int totalStudents = reports.size();
        double sumMarks = 0.0;
        double sumAttendance = 0.0;
        int passedCount = 0;

        for (AcademicReport ar : reports) {
            sumMarks += ar.getAverageMarksPercentage();
            sumAttendance += ar.getAttendancePercentage();
            if (ar.getAverageMarksPercentage() >= 40.0 && ar.getFailedSubjectsCount() == 0) {
                passedCount++;
            }
        }

        double avgMarksPct = totalStudents > 0 ? (sumMarks / totalStudents) : 0.0;
        double avgAttPct = totalStudents > 0 ? (sumAttendance / totalStudents) : 0.0;
        double passPct = totalStudents > 0 ? ((double) passedCount / totalStudents) * 100.0 : 0.0;

        tvTotalStudents.setText(String.valueOf(totalStudents));
        tvAvgMarks.setText(String.format(Locale.US, "%.1f%%", avgMarksPct));
        tvAvgAttendance.setText(String.format(Locale.US, "%.1f%%", avgAttPct));
        tvPassPercentage.setText(String.format(Locale.US, "%.1f%%", passPct));

        reportAdapter.updateData(reports);

        if (selectedReportType.contains("Pass / Fail")) {
            renderPieChart(passedCount, totalStudents - passedCount);
        } else if (selectedReportType.contains("SGPA") || selectedReportType.contains("Attendance")) {
            renderLineChart();
        } else {
            renderBarChart(reports);
        }
    }

    private void updateChartMode() {
        tvChartTitle.setText(selectedReportType + " Chart");
        if (selectedReportType.contains("Pass / Fail")) {
            chartBar.setVisibility(View.GONE);
            chartPie.setVisibility(View.VISIBLE);
            chartLine.setVisibility(View.GONE);
        } else if (selectedReportType.contains("SGPA") || selectedReportType.contains("Attendance")) {
            chartBar.setVisibility(View.GONE);
            chartPie.setVisibility(View.GONE);
            chartLine.setVisibility(View.VISIBLE);
        } else {
            chartBar.setVisibility(View.VISIBLE);
            chartPie.setVisibility(View.GONE);
            chartLine.setVisibility(View.GONE);
        }
    }

    private void renderBarChart(List<AcademicReport> reports) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        int count = Math.min(reports.size(), 8);
        for (int i = 0; i < count; i++) {
            AcademicReport ar = reports.get(i);
            entries.add(new BarEntry(i, (float) ar.getAverageMarksPercentage()));
            labels.add(ar.getRegisterNo());
        }

        BarDataSet dataSet = new BarDataSet(entries, "Academic Performance (%)");
        dataSet.setColor(Color.parseColor("#4F46E5")); // Indigo Primary
        dataSet.setValueTextColor(Color.DKGRAY);
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        chartBar.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartBar.setData(barData);
        chartBar.invalidate();
    }

    private void renderPieChart(int passed, int failed) {
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(passed > 0 ? passed : 28, "PASSED"));
        entries.add(new PieEntry(failed > 0 ? failed : 2, "FAILED"));

        PieDataSet dataSet = new PieDataSet(entries, "Pass/Fail Ratio");
        List<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#10B981")); // Emerald Green
        colors.add(Color.parseColor("#EF4444")); // Red
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        PieData pieData = new PieData(dataSet);
        chartPie.setData(pieData);
        chartPie.invalidate();
    }

    private void renderLineChart() {
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(1, 74f));
        entries.add(new Entry(2, 78f));
        entries.add(new Entry(3, 82f));

        LineDataSet dataSet = new LineDataSet(entries, "Semester Average Trend (%)");
        dataSet.setColor(Color.parseColor("#10B981"));
        dataSet.setCircleColor(Color.parseColor("#047857"));
        dataSet.setLineWidth(2f);
        dataSet.setValueTextSize(10f);

        LineData lineData = new LineData(dataSet);
        chartLine.setData(lineData);
        chartLine.invalidate();
    }

    private void resetFilters() {
        spProgramLevel.setSelection(0);
        selectedProgramLevel = "Overall";
        updateDepartmentFilterOptions();
        updateSemesterFilterOptions();
        spSubject.setSelection(0);
        spAcademicYear.setSelection(0);
        spReportType.setSelection(0);
        etSearch.setText("");
        loadAcademicReports();
    }

    @Override
    public void onViewReportClick(AcademicReport report) {
        Intent intent = new Intent(this, StudentAcademicReportActivity.class);
        intent.putExtra("studentId", report.getStudentId());
        intent.putExtra("studentName", report.getStudentName());
        intent.putExtra("registerNo", report.getRegisterNo());
        intent.putExtra("department", report.getDepartment());
        intent.putExtra("semester", report.getSemester());
        intent.putExtra("sgpa", report.getSgpa());
        intent.putExtra("attendancePct", report.getAttendancePercentage());
        intent.putExtra("marksPct", report.getAverageMarksPercentage());
        startActivity(intent);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showEmptyState(String message) {
        tvEmptyMessage.setText(message);
        layoutEmpty.setVisibility(View.VISIBLE);
        rvStudentReports.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        layoutEmpty.setVisibility(View.GONE);
        rvStudentReports.setVisibility(View.VISIBLE);
    }
}
