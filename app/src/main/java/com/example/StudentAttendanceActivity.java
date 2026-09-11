package com.example;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adapter.StudentSubjectAttendanceAdapter;
import com.example.database.DatabaseHelper;
import com.example.model.Attendance;
import com.example.model.MonthlyAttendanceStats;
import com.example.model.SubjectAttendanceStats;
import com.example.repository.AttendanceRepository;
import com.example.utils.SessionManager;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Student Activity displaying Firestore Hour-Based Attendance, Overall %, Subject-wise breakdown, and Low Attendance Warnings.
 */
public class StudentAttendanceActivity extends AppCompatActivity {

    private static final String TAG = "StudentAttendanceAct";

    private AttendanceRepository repository;
    private SessionManager sessionManager;
    private DatabaseHelper dbHelper;
    private StudentSubjectAttendanceAdapter adapter;

    private Toolbar toolbar;
    private TextView tvOverallPercentage;
    private TextView tvClassesAttended;
    private TextView tvClassesAbsent;
    private TextView tvExamEligibilityBadge;
    private TextView tvRequiredStatus;

    private MaterialButtonToggleGroup toggleMonthlyChartType;
    private MaterialButton btnChartMonthlyBar;
    private MaterialButton btnChartMonthlyLine;
    private BarChart chartMonthlyBar;
    private LineChart chartMonthlyLine;

    private RecyclerView rvSubjectAttendance;

    private String currentStudentId = "1"; // Default fallback logged-in student ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_attendance);

        repository = AttendanceRepository.getInstance(this);
        sessionManager = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);

        int sessionUserId = sessionManager.getUserId();
        if (sessionUserId > 0) {
            currentStudentId = String.valueOf(sessionUserId);
        }

        initViews();
        setupToolbar();
        setupWindowInsets();
        setupRecyclerView();
        setupChartToggle();
        loadStudentFirestoreAttendance();
    }

    private void setupWindowInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbarStudentAtt), (v, insets) -> {
            androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
            v.setPadding(0, statusBarInsets.top, 0, 0);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStudentFirestoreAttendance();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarStudentAtt);
        tvOverallPercentage = findViewById(R.id.tvOverallPercentage);
        tvClassesAttended = findViewById(R.id.tvClassesAttended);
        tvClassesAbsent = findViewById(R.id.tvClassesAbsent);
        tvExamEligibilityBadge = findViewById(R.id.tvExamEligibilityBadge);
        tvRequiredStatus = findViewById(R.id.tvRequiredStatus);

        toggleMonthlyChartType = findViewById(R.id.toggleMonthlyChartType);
        btnChartMonthlyBar = findViewById(R.id.btnChartMonthlyBar);
        btnChartMonthlyLine = findViewById(R.id.btnChartMonthlyLine);
        chartMonthlyBar = findViewById(R.id.chartMonthlyBar);
        chartMonthlyLine = findViewById(R.id.chartMonthlyLine);

        rvSubjectAttendance = findViewById(R.id.rvSubjectAttendance);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        rvSubjectAttendance.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StudentSubjectAttendanceAdapter(new ArrayList<>());
        rvSubjectAttendance.setAdapter(adapter);
    }

    private void setupChartToggle() {
        toggleMonthlyChartType.check(R.id.btnChartMonthlyBar);
        toggleMonthlyChartType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnChartMonthlyBar) {
                    chartMonthlyBar.setVisibility(View.VISIBLE);
                    chartMonthlyLine.setVisibility(View.GONE);
                } else if (checkedId == R.id.btnChartMonthlyLine) {
                    chartMonthlyBar.setVisibility(View.GONE);
                    chartMonthlyLine.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    /**
     * Fetch ONLY the logged-in student's attendance records from Firebase Firestore.
     */
    private void loadStudentFirestoreAttendance() {
        repository.fetchStudentAttendanceHistory(currentStudentId, new AttendanceRepository.OnStudentAttendanceListener() {
            @Override
            public void onSuccess(List<Attendance> records) {
                if (records == null || records.isEmpty()) {
                    Log.d(TAG, "No Firestore records for student " + currentStudentId + ", falling back to SQLite calculation...");
                    loadSQLiteFallbackData();
                    return;
                }

                processAttendanceData(records);
            }

            @Override
            public void onError(String errorMessage) {
                Log.w(TAG, "Firestore fetch error: " + errorMessage + ". Using local SQLite backup.");
                loadSQLiteFallbackData();
            }
        });
    }

    private void processAttendanceData(List<Attendance> records) {
        int totalHours = 0;
        int presentHours = 0;
        int absentHours = 0;

        Map<String, int[]> subjectStatsMap = new HashMap<>(); // subjectName -> [total, present]
        Map<String, double[]> monthlyStatsMap = new HashMap<>(); // monthLabel -> [sumPct, count]

        for (Attendance att : records) {
            String subName = att.getSubjectName() != null ? att.getSubjectName() : "Subject " + att.getSubjectId();

            if (!subjectStatsMap.containsKey(subName)) {
                subjectStatsMap.put(subName, new int[]{0, 0});
            }
            int[] subArr = subjectStatsMap.get(subName);

            totalHours++;
            subArr[0]++;

            if (att.isPresent()) {
                presentHours++;
                subArr[1]++;
            } else if (att.isAbsent()) {
                absentHours++;
            }
        }

        double overallPct = totalHours > 0 ? ((double) presentHours / totalHours) * 100.0 : 0.0;

        tvOverallPercentage.setText(String.format(Locale.US, "%.2f%%", overallPct));
        tvClassesAttended.setText("Attended: " + presentHours + " / " + totalHours + " Hours");
        tvClassesAbsent.setText("Absent: " + absentHours + " Hours");

        // Subject-Wise list
        List<SubjectAttendanceStats> subjectStatsList = new ArrayList<>();
        List<String> lowAttSubjects = new ArrayList<>();

        int subIdCounter = 1;
        for (Map.Entry<String, int[]> entry : subjectStatsMap.entrySet()) {
            String name = entry.getKey();
            int subTotal = entry.getValue()[0];
            int subPresent = entry.getValue()[1];
            double subPct = subTotal > 0 ? ((double) subPresent / subTotal) * 100.0 : 0.0;

            subjectStatsList.add(new SubjectAttendanceStats(subIdCounter++, name, subTotal, subPresent, subPct));

            if (subPct < 75.0) {
                lowAttSubjects.add(name + " (" + String.format(Locale.US, "%.1f%%", subPct) + ")");
            }
        }

        adapter.updateList(subjectStatsList);

        // Low Attendance Warning Threshold Rule (75%)
        if (overallPct >= 85.0 && lowAttSubjects.isEmpty()) {
            tvExamEligibilityBadge.setText("✓ Excellent Attendance");
            tvExamEligibilityBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#10B981")));
            tvRequiredStatus.setText("Outstanding attendance record! Eligible for all university honors.");
        } else if (overallPct >= 75.0 && lowAttSubjects.isEmpty()) {
            tvExamEligibilityBadge.setText("✓ Attendance Good");
            tvExamEligibilityBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#10B981")));
            tvRequiredStatus.setText("All subjects meet the mandatory 75% attendance threshold.");
        } else {
            tvExamEligibilityBadge.setText("⚠ LOW ATTENDANCE (<75%)");
            tvExamEligibilityBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#EF4444")));
            if (!lowAttSubjects.isEmpty()) {
                tvRequiredStatus.setText("⚠️ Your attendance is below the required 75% cutoff in " + lowAttSubjects.size() + " subject(s): " + TextUtils.join(", ", lowAttSubjects) + ".");
            } else {
                tvRequiredStatus.setText("⚠️ Your attendance (" + String.format(Locale.US, "%.2f%%", overallPct) + ") is below the required 75% cutoff.");
            }
        }

        // Monthly trends
        List<MonthlyAttendanceStats> monthlyList = dbHelper.getMonthlyAttendanceChart(Integer.parseInt(currentStudentId));
        renderMonthlyBarChart(monthlyList);
        renderMonthlyLineChart(monthlyList);
    }

    private void loadSQLiteFallbackData() {
        int studentIdInt = 1;
        try { studentIdInt = Integer.parseInt(currentStudentId); } catch (Exception ignored) {}

        List<SubjectAttendanceStats> subjectStats = dbHelper.getSubjectWiseAttendance(studentIdInt);
        adapter.updateList(subjectStats);

        int totalClassesHeld = 0;
        int totalClassesAttended = 0;
        List<String> lowAttSubjects = new ArrayList<>();

        for (SubjectAttendanceStats sub : subjectStats) {
            totalClassesHeld += sub.getTotalClasses();
            totalClassesAttended += sub.getAttendedClasses();
            if (sub.getPercentage() < 75) {
                lowAttSubjects.add(sub.getSubjectName() + " (" + String.format(Locale.US, "%.1f%%", sub.getPercentage()) + ")");
            }
        }

        double overallPct = totalClassesHeld > 0 ? ((double) totalClassesAttended / totalClassesHeld) * 100.0 : 0.0;
        int absentCount = totalClassesHeld - totalClassesAttended;

        tvOverallPercentage.setText(String.format(Locale.US, "%.2f%%", overallPct));
        tvClassesAttended.setText("Attended: " + totalClassesAttended + " / " + totalClassesHeld + " Hours");
        tvClassesAbsent.setText("Absent: " + absentCount + " Hours");

        if (overallPct >= 75.0 && lowAttSubjects.isEmpty()) {
            tvExamEligibilityBadge.setText("✓ Attendance Good");
            tvExamEligibilityBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#10B981")));
            tvRequiredStatus.setText("All subjects meet the mandatory 75% attendance threshold.");
        } else {
            tvExamEligibilityBadge.setText("⚠ LOW ATTENDANCE (<75%)");
            tvExamEligibilityBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#EF4444")));
            tvRequiredStatus.setText("⚠️ Your attendance is below the required 75% cutoff.");
        }

        List<MonthlyAttendanceStats> monthlyStats = dbHelper.getMonthlyAttendanceChart(studentIdInt);
        renderMonthlyBarChart(monthlyStats);
        renderMonthlyLineChart(monthlyStats);
    }

    private void renderMonthlyBarChart(List<MonthlyAttendanceStats> monthlyStats) {
        if (monthlyStats == null || monthlyStats.isEmpty()) {
            chartMonthlyBar.clear();
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < monthlyStats.size(); i++) {
            MonthlyAttendanceStats m = monthlyStats.get(i);
            entries.add(new BarEntry(i, m.getPercentage()));
            labels.add(m.getMonthLabel());
        }

        BarDataSet dataSet = new BarDataSet(entries, "Monthly Attendance %");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(11f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.45f);

        chartMonthlyBar.setData(barData);
        chartMonthlyBar.getDescription().setEnabled(false);
        chartMonthlyBar.setFitBars(true);

        XAxis xAxis = chartMonthlyBar.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        chartMonthlyBar.getAxisLeft().setAxisMinimum(0f);
        chartMonthlyBar.getAxisLeft().setAxisMaximum(100f);
        chartMonthlyBar.getAxisRight().setEnabled(false);
        chartMonthlyBar.animateY(700);
        chartMonthlyBar.invalidate();
    }

    private void renderMonthlyLineChart(List<MonthlyAttendanceStats> monthlyStats) {
        if (monthlyStats == null || monthlyStats.isEmpty()) {
            chartMonthlyLine.clear();
            return;
        }

        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < monthlyStats.size(); i++) {
            MonthlyAttendanceStats m = monthlyStats.get(i);
            entries.add(new Entry(i, m.getPercentage()));
            labels.add(m.getMonthLabel());
        }

        LineDataSet dataSet = new LineDataSet(entries, "Attendance % Trend");
        dataSet.setColor(Color.parseColor("#1B5E20"));
        dataSet.setCircleColor(Color.parseColor("#2E7D32"));
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(true);
        dataSet.setValueTextSize(11f);

        LineData lineData = new LineData(dataSet);
        chartMonthlyLine.setData(lineData);
        chartMonthlyLine.getDescription().setEnabled(false);

        XAxis xAxis = chartMonthlyLine.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        chartMonthlyLine.getAxisLeft().setAxisMinimum(0f);
        chartMonthlyLine.getAxisLeft().setAxisMaximum(100f);
        chartMonthlyLine.getAxisRight().setEnabled(false);
        chartMonthlyLine.animateX(700);
        chartMonthlyLine.invalidate();
    }
}
