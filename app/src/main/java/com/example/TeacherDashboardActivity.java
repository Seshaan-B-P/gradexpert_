package com.example;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adapter.RecentActivityAdapter;
import com.example.database.DatabaseHelper;
import com.example.model.ActivityItem;
import com.example.utils.SessionManager;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Teacher Dashboard Activity for GradeXpert ERP.
 * Manages stats, performance analytics charts (MPAndroidChart), quick tools,
 * recent activities log, and navigation.
 */
public class TeacherDashboardActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private DatabaseHelper dbHelper;

    // UI Views
    private TextView tvTeacherName, tvTeacherDept;
    private TextView tvStatStudents, tvStatSubjects, tvStatAssignments, tvStatPendingAssessments;
    private BarChart gradeDistributionBarChart;
    private PieChart attendancePieChart;
    private RecyclerView rvRecentActivities;
    private BottomNavigationView teacherBottomNavigation;
    private ImageView btnTeacherNotificationsHeader;
    private View cardProfileAvatar;
    private TextView tvViewAllActivities;

    // Tool Buttons
    private LinearLayout toolManageStudents, toolManageSubjects, toolAssignments, toolEnterMarks;
    private LinearLayout toolAttendance, toolPublishResults, toolReports, toolNotifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);

        // Security check
        if (!sessionManager.isLoggedIn() || !"TEACHER".equalsIgnoreCase(sessionManager.getUserRole())) {
            navigateToLogin();
            return;
        }

        setContentView(R.layout.activity_teacher_dashboard);

        initViews();
        populateProfileData();
        loadStatistics();
        setupGradeDistributionChart();
        setupAttendancePieChart();
        setupRecentActivitiesList();
        setupToolClickListeners();
        setupBottomNavigation();
        setupWindowInsets();
    }

    private void setupWindowInsets() {
        View header = findViewById(R.id.headerTeacherDashboard);
        if (header != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                v.setPadding(v.getPaddingLeft(), statusBarInsets.top + 20, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }
    }

    private void initViews() {
        tvTeacherName = findViewById(R.id.tvTeacherName);
        tvTeacherDept = findViewById(R.id.tvTeacherDept);

        tvStatStudents = findViewById(R.id.tvStatStudents);
        tvStatSubjects = findViewById(R.id.tvStatSubjects);
        tvStatAssignments = findViewById(R.id.tvStatAssignments);
        tvStatPendingAssessments = findViewById(R.id.tvStatPendingAssessments);

        gradeDistributionBarChart = findViewById(R.id.gradeDistributionBarChart);
        attendancePieChart = findViewById(R.id.attendancePieChart);
        rvRecentActivities = findViewById(R.id.rvRecentActivities);
        teacherBottomNavigation = findViewById(R.id.teacherBottomNavigation);
        btnTeacherNotificationsHeader = findViewById(R.id.btnTeacherNotificationsHeader);
        cardProfileAvatar = findViewById(R.id.cardProfileAvatar);
        tvViewAllActivities = findViewById(R.id.tvViewAllActivities);

        // Tools
        toolManageStudents = findViewById(R.id.toolManageStudents);
        toolManageSubjects = findViewById(R.id.toolManageSubjects);
        toolAssignments = findViewById(R.id.toolAssignments);
        toolEnterMarks = findViewById(R.id.toolEnterMarks);
        toolAttendance = findViewById(R.id.toolAttendance);
        toolPublishResults = findViewById(R.id.toolPublishResults);
        toolReports = findViewById(R.id.toolReports);
        toolNotifications = findViewById(R.id.toolNotifications);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh all dashboard metrics, charts, and activity log on returning from child screens
        populateProfileData();
        loadStatistics();
        setupGradeDistributionChart();
        setupAttendancePieChart();
        setupRecentActivitiesList();
    }

    private void populateProfileData() {
        String name = sessionManager.getUserName();
        if (name == null || name.isEmpty()) {
            name = "Faculty Member";
        }
        tvTeacherName.setText(name);
        String identifier = sessionManager.getIdentifier();
        if (identifier != null && !identifier.isEmpty()) {
            tvTeacherDept.setText("Faculty ID: " + identifier);
        } else {
            tvTeacherDept.setText("Faculty Portal • GradeXpert");
        }
    }

    private void loadStatistics() {
        int studentsCount = dbHelper.getStudentsCount();
        int subjectsCount = dbHelper.getSubjectsCount();
        int assignmentsCount = dbHelper.getAssignmentsCount();
        int pendingAssessments = dbHelper.getPendingAssessmentsCount();

        tvStatStudents.setText(String.valueOf(studentsCount));
        tvStatSubjects.setText(String.valueOf(subjectsCount));
        tvStatAssignments.setText(String.valueOf(assignmentsCount));
        tvStatPendingAssessments.setText(String.valueOf(pendingAssessments));
    }

    private void setupGradeDistributionChart() {
        java.util.Map<String, Integer> distMap = dbHelper.getGradeDistribution();
        String[] grades = new String[]{"A+", "A", "B+", "B", "C", "D", "F"};

        int totalEntries = 0;
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < grades.length; i++) {
            int count = distMap.containsKey(grades[i]) ? distMap.get(grades[i]) : 0;
            totalEntries += count;
            entries.add(new BarEntry(i, count));
        }

        if (totalEntries == 0) {
            gradeDistributionBarChart.clear();
            gradeDistributionBarChart.setNoDataText("No grades recorded yet.");
            gradeDistributionBarChart.invalidate();
            return;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Students");
        dataSet.setColors(new int[]{
                Color.parseColor("#10B981"), // A+ Emerald Green
                Color.parseColor("#4F46E5"), // A Indigo
                Color.parseColor("#06B6D4"), // B+ Cyan
                Color.parseColor("#8B5CF6"), // B Violet
                Color.parseColor("#F59E0B"), // C Amber
                Color.parseColor("#F97316"), // D Orange
                Color.parseColor("#EF4444")  // F Crimson Red
        });
        dataSet.setValueTextColor(Color.parseColor("#1E293B"));
        dataSet.setValueTextSize(11f);
        dataSet.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.55f);

        gradeDistributionBarChart.setData(barData);
        gradeDistributionBarChart.getDescription().setEnabled(false);
        gradeDistributionBarChart.getLegend().setEnabled(false);
        gradeDistributionBarChart.setDrawGridBackground(false);
        gradeDistributionBarChart.setDrawBarShadow(false);
        gradeDistributionBarChart.setFitBars(true);
        gradeDistributionBarChart.setExtraOffsets(0, 10, 0, 10);

        XAxis xAxis = gradeDistributionBarChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(grades));
        xAxis.setTextColor(Color.parseColor("#64748B"));
        xAxis.setTextSize(11f);

        com.github.mikephil.charting.components.YAxis leftAxis = gradeDistributionBarChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(1f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#E2E8F0"));
        leftAxis.setTextColor(Color.parseColor("#64748B"));
        leftAxis.setTextSize(10f);

        gradeDistributionBarChart.getAxisRight().setEnabled(false);
        gradeDistributionBarChart.animateY(1000);
        gradeDistributionBarChart.invalidate();
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }

    private void setupAttendancePieChart() {
        double overallAtt = dbHelper.getOverallAttendancePercentage();
        if (overallAtt <= 0.0) {
            attendancePieChart.clear();
            attendancePieChart.setNoDataText("No attendance recorded yet");
            attendancePieChart.invalidate();
            return;
        }

        List<PieEntry> pieEntries = new ArrayList<>();
        pieEntries.add(new PieEntry((float) overallAtt, "Present %"));
        pieEntries.add(new PieEntry((float) (100.0 - overallAtt), "Absent %"));

        PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
        pieDataSet.setColors(new int[]{
                Color.parseColor("#0F9D58"), // Present Green
                Color.parseColor("#D93025")  // Absent Red
        });
        pieDataSet.setValueTextColor(Color.WHITE);
        pieDataSet.setValueTextSize(13f);

        PieData pieData = new PieData(pieDataSet);
        attendancePieChart.setData(pieData);
        attendancePieChart.getDescription().setEnabled(false);
        attendancePieChart.setHoleRadius(45f);
        attendancePieChart.setTransparentCircleRadius(50f);
        attendancePieChart.setCenterText(String.format(Locale.US, "Batch\n%.1f%% Avg", overallAtt));
        attendancePieChart.setCenterTextSize(14f);
        attendancePieChart.setCenterTextColor(Color.parseColor("#202124"));
        attendancePieChart.animateXY(1000, 1000);
        attendancePieChart.invalidate();
    }

    private void setupRecentActivitiesList() {
        rvRecentActivities.setLayoutManager(new LinearLayoutManager(this));
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("portalActivities")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(5)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null || queryDocumentSnapshots == null) return;
                    List<com.example.model.PortalActivity> list = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        com.example.model.PortalActivity pa = doc.toObject(com.example.model.PortalActivity.class);
                        if (pa != null) list.add(pa);
                    }
                    com.example.adapter.PortalActivityAdapter adapter = new com.example.adapter.PortalActivityAdapter(list, activity -> {
                        Intent intent = new Intent(TeacherDashboardActivity.this, PortalActivityDetailsActivity.class);
                        intent.putExtra("title", activity.getTitle());
                        intent.putExtra("description", activity.getDescription());
                        intent.putExtra("type", activity.getType());
                        intent.putExtra("entityType", activity.getEntityType());
                        intent.putExtra("studentName", activity.getStudentName());
                        intent.putExtra("registerNo", activity.getRegisterNo());
                        intent.putExtra("subjectName", activity.getSubjectName());
                        intent.putExtra("teacherName", activity.getTeacherName());
                        if (activity.getTimestamp() != null) {
                            intent.putExtra("timestampMs", activity.getTimestamp().toDate().getTime());
                        }
                        startActivity(intent);
                    });
                    rvRecentActivities.setAdapter(adapter);
                });
    }

    private void setupToolClickListeners() {
        btnTeacherNotificationsHeader.setOnClickListener(v -> {
            Intent intent = new Intent(TeacherDashboardActivity.this, NotificationsActivity.class);
            startActivity(intent);
        });
        if (cardProfileAvatar != null) {
            cardProfileAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(TeacherDashboardActivity.this, SettingsActivity.class);
                startActivity(intent);
            });
        }
        tvViewAllActivities.setOnClickListener(v -> {
            Intent intent = new Intent(TeacherDashboardActivity.this, PortalActivitiesActivity.class);
            startActivity(intent);
        });

        toolManageStudents.setOnClickListener(v -> {
            Intent intent = new Intent(TeacherDashboardActivity.this, ManageStudentsActivity.class);
            startActivity(intent);
        });
        toolManageSubjects.setOnClickListener(v -> {
            Intent intent = new Intent(TeacherDashboardActivity.this, ManageSubjectsActivity.class);
            startActivity(intent);
        });
        toolAssignments.setOnClickListener(v -> {
            Intent intent = new Intent(TeacherDashboardActivity.this, TeacherAssignmentActivity.class);
            startActivity(intent);
        });
        toolEnterMarks.setOnClickListener(v -> {
            Intent intent = new Intent(TeacherDashboardActivity.this, AddMarksActivity.class);
            startActivity(intent);
        });
        toolAttendance.setOnClickListener(v -> {
            Intent intent = new Intent(TeacherDashboardActivity.this, MarkAttendanceActivity.class);
            startActivity(intent);
        });
        toolPublishResults.setOnClickListener(v -> {
            Intent intent = new Intent(TeacherDashboardActivity.this, PublishResultsActivity.class);
            startActivity(intent);
        });
        toolReports.setOnClickListener(v -> {
            Intent intent = new Intent(TeacherDashboardActivity.this, ReportsActivity.class);
            intent.putExtra("target_tab", "department");
            startActivity(intent);
        });
        toolNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(TeacherDashboardActivity.this, BroadcastAlertsActivity.class);
            startActivity(intent);
        });
    }

    private void showToolMessage(String toolName) {
        Toast.makeText(this, toolName + " clicked", Toast.LENGTH_SHORT).show();
    }

    private void setupBottomNavigation() {
        teacherBottomNavigation.setSelectedItemId(R.id.nav_teacher_home);
        teacherBottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_teacher_home) {
                return true;
            } else if (id == R.id.nav_teacher_students) {
                Intent intent = new Intent(TeacherDashboardActivity.this, ManageStudentsActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_teacher_marks) {
                Intent intent = new Intent(TeacherDashboardActivity.this, AddMarksActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_teacher_reports) {
                Intent intent = new Intent(TeacherDashboardActivity.this, ReportsActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_teacher_profile) {
                Intent intent = new Intent(TeacherDashboardActivity.this, TeacherProfileActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(TeacherDashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
