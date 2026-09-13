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
import com.example.model.AppNotification;
import com.example.model.Student;
import com.example.model.Subject;
import com.example.utils.SessionManager;
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
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Student Dashboard Activity for GradeXpert ERP.
 * Manages student academic profile, SGPA/CGPA cards, attendance & credits progress,
 * MPAndroidChart academic progression line chart, subject bar chart, grade pie chart,
 * quick service utilities grid, recent notifications log, and bottom navigation.
 */
public class StudentDashboardActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private DatabaseHelper dbHelper;
    private Student currentStudent;

    // UI Views
    private TextView tvStudentName, tvStudentRegNo, tvStudentDeptSem;
    private TextView tvStatCGPA, tvStatSGPA, tvStatAttendance, tvStatCredits;
    private LineChart sgpaLineChart;
    private TextView tvTrendCgpaVal, tvTrendPeakVal, tvTrendDeltaVal;
    private com.google.android.material.chip.ChipGroup chipGroupTrendMetric;
    private com.google.android.material.chip.Chip chipTrendSgpa, chipTrendCgpa, chipTrendPct, chipTrendDual;
    private List<com.example.model.SemesterPerformanceTrend> currentTrends;
    private com.example.utils.AcademicChartHelper.MetricMode currentMetricMode = com.example.utils.AcademicChartHelper.MetricMode.SGPA;
    private BarChart subjectMarksBarChart;
    private PieChart gradePieChart;
    private Chip chipPerformanceRating;
    private RecyclerView rvStudentNotifications;
    private BottomNavigationView studentBottomNavigation;
    private ImageView btnStudentNotificationsHeader, btnStudentMenu, btnStudentHeaderLogout;
    private TextView tvStudentUnreadBadge;
    private View cardStudentProfileAvatar;
    private ImageView imgStudentAvatar;
    private TextView tvRefreshNotifications;
    private View btnStudentDashboardLogout;
    private TextView tvAccountStudentName, tvAccountStudentInfo;

    // Quick 2x2 Grid Cards & Subtitles
    private View cardQuickSubjects, cardQuickAssignments, cardQuickAssessments, cardQuickAttendance;
    private TextView tvQuickSubjectsCount, tvQuickAssignmentsCount, tvQuickAssessmentsCount, tvQuickAttendanceCount;

    // Secondary Quick Service Buttons
    private LinearLayout quickResults, quickCalculator, quickNotifications, quickProfile;

    private com.google.firebase.firestore.ListenerRegistration gradeAlertsListener = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);

        // Security Authentication check
        if (!sessionManager.isLoggedIn() || !"STUDENT".equalsIgnoreCase(sessionManager.getUserRole())) {
            navigateToLogin();
            return;
        }

        setContentView(R.layout.activity_student_dashboard);

        checkNotificationPermission();

        try {
            MyFirebaseMessagingService.registerFcmToken(this);
        } catch (Exception ignored) {}

        initViews();
        loadStudentProfile();
        loadAcademicStats();
        setupSGPALineChart();
        setupSubjectMarksBarChart();
        setupGradeDistributionPieChart();
        setupNotificationsList();
        setupQuickAccessClickListeners();
        setupBottomNavigation();
        setupWindowInsets();
    }

    private void checkNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1002);
            }
        }
    }

    private void setupWindowInsets() {
        View header = findViewById(R.id.headerStudentDashboard);
        if (header != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                v.setPadding(v.getPaddingLeft(), statusBarInsets.top + 16, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStudentProfile();
        loadAcademicStats();
        setupSGPALineChart();
        setupSubjectMarksBarChart();
        setupGradeDistributionPieChart();
        setupNotificationsList();
    }

    private void initViews() {
        btnStudentMenu = findViewById(R.id.btnStudentMenu);
        tvStudentUnreadBadge = findViewById(R.id.tvStudentUnreadBadge);
        tvStudentName = findViewById(R.id.tvStudentName);
        tvStudentRegNo = findViewById(R.id.tvStudentRegNo);
        tvStudentDeptSem = findViewById(R.id.tvStudentDeptSem);

        tvStatCGPA = findViewById(R.id.tvStatCGPA);
        tvStatSGPA = findViewById(R.id.tvStatSGPA);
        tvStatAttendance = findViewById(R.id.tvStatAttendance);
        tvStatCredits = findViewById(R.id.tvStatCredits);

        sgpaLineChart = findViewById(R.id.sgpaLineChart);
        tvTrendCgpaVal = findViewById(R.id.tvTrendCgpaVal);
        tvTrendPeakVal = findViewById(R.id.tvTrendPeakVal);
        tvTrendDeltaVal = findViewById(R.id.tvTrendDeltaVal);
        chipGroupTrendMetric = findViewById(R.id.chipGroupTrendMetric);
        chipTrendSgpa = findViewById(R.id.chipTrendSgpa);
        chipTrendCgpa = findViewById(R.id.chipTrendCgpa);
        chipTrendPct = findViewById(R.id.chipTrendPct);
        chipTrendDual = findViewById(R.id.chipTrendDual);

        if (chipGroupTrendMetric != null) {
            chipGroupTrendMetric.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.contains(R.id.chipTrendCgpa)) {
                    currentMetricMode = com.example.utils.AcademicChartHelper.MetricMode.CGPA;
                } else if (checkedIds.contains(R.id.chipTrendPct)) {
                    currentMetricMode = com.example.utils.AcademicChartHelper.MetricMode.PERCENTAGE;
                } else if (checkedIds.contains(R.id.chipTrendDual)) {
                    currentMetricMode = com.example.utils.AcademicChartHelper.MetricMode.COMPARISON;
                } else {
                    currentMetricMode = com.example.utils.AcademicChartHelper.MetricMode.SGPA;
                }
                renderTrendChart();
            });
        }

        subjectMarksBarChart = findViewById(R.id.subjectMarksBarChart);
        gradePieChart = findViewById(R.id.gradePieChart);
        chipPerformanceRating = findViewById(R.id.chipPerformanceRating);

        rvStudentNotifications = findViewById(R.id.rvStudentNotifications);
        studentBottomNavigation = findViewById(R.id.studentBottomNavigation);
        btnStudentNotificationsHeader = findViewById(R.id.btnStudentNotificationsHeader);
        btnStudentHeaderLogout = findViewById(R.id.btnStudentHeaderLogout);
        btnStudentDashboardLogout = findViewById(R.id.btnStudentDashboardLogout);
        tvAccountStudentName = findViewById(R.id.tvAccountStudentName);
        tvAccountStudentInfo = findViewById(R.id.tvAccountStudentInfo);
        cardStudentProfileAvatar = findViewById(R.id.cardStudentAvatar);
        imgStudentAvatar = findViewById(R.id.imgStudentAvatar);
        tvRefreshNotifications = findViewById(R.id.tvRefreshNotifications);

        // Quick Grid Buttons
        cardQuickSubjects = findViewById(R.id.cardQuickSubjects);
        cardQuickAssignments = findViewById(R.id.cardQuickAssignments);
        cardQuickAssessments = findViewById(R.id.cardQuickAssessments);
        cardQuickAttendance = findViewById(R.id.cardQuickAttendance);

        tvQuickSubjectsCount = findViewById(R.id.tvQuickSubjectsCount);
        tvQuickAssignmentsCount = findViewById(R.id.tvQuickAssignmentsCount);
        tvQuickAssessmentsCount = findViewById(R.id.tvQuickAssessmentsCount);
        tvQuickAttendanceCount = findViewById(R.id.tvQuickAttendanceCount);

        quickResults = findViewById(R.id.quickResults);
        quickCalculator = findViewById(R.id.quickCalculator);
        quickNotifications = findViewById(R.id.quickNotifications);
        quickProfile = findViewById(R.id.quickProfile);

        if (btnStudentNotificationsHeader != null) {
            btnStudentNotificationsHeader.setOnClickListener(v -> {
                Intent intent = new Intent(StudentDashboardActivity.this, NotificationsActivity.class);
                startActivity(intent);
            });
        }

        if (cardStudentProfileAvatar != null) {
            cardStudentProfileAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(StudentDashboardActivity.this, StudentProfileActivity.class);
                startActivity(intent);
            });
        }
    }

    private void loadStudentProfile() {
        currentStudent = dbHelper.getStudentDetails(sessionManager.getIdentifier());
        if (currentStudent == null && sessionManager.getUserId() > 0) {
            currentStudent = dbHelper.getStudentById(sessionManager.getUserId());
        }

        if (currentStudent != null) {
            String pLevel = currentStudent.getProgramLevel() != null ? currentStudent.getProgramLevel().toUpperCase() : "UG";
            tvStudentName.setText(currentStudent.getName());
            tvStudentRegNo.setText(currentStudent.getRegisterNo());
            tvStudentDeptSem.setText("[" + pLevel + "] " + currentStudent.getDepartment() + " • " + currentStudent.getSemester());
            if (tvAccountStudentName != null) tvAccountStudentName.setText(currentStudent.getName());
            if (tvAccountStudentInfo != null) tvAccountStudentInfo.setText(currentStudent.getRegisterNo() + " • " + pLevel + " " + currentStudent.getDepartment());

            com.example.utils.GradeNotificationHelper.subscribeStudentToGradeAlerts(this, currentStudent);
            setupGradeAlertsListener(currentStudent.getRegisterNo());
        } else {
            tvStudentName.setText(sessionManager.getUserName());
            tvStudentRegNo.setText(sessionManager.getIdentifier());
            tvStudentDeptSem.setText("Student Portal • GradeXpert");
            if (tvAccountStudentName != null) tvAccountStudentName.setText(sessionManager.getUserName());
            if (tvAccountStudentInfo != null) tvAccountStudentInfo.setText(sessionManager.getIdentifier() + " • Student Session");

            if (sessionManager.getIdentifier() != null) {
                setupGradeAlertsListener(sessionManager.getIdentifier());
            }

            // Query Firestore in background
            String idf = sessionManager.getIdentifier();
            String email = sessionManager.getUserEmail();
            if (idf != null && !idf.isEmpty()) {
                com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("students")
                        .whereEqualTo("registerNo", idf)
                        .get()
                        .addOnSuccessListener(snapshots -> {
                            if (snapshots != null && !snapshots.isEmpty()) {
                                DocumentSnapshot doc = snapshots.getDocuments().get(0);
                                String name = doc.getString("name");
                                String reg = doc.getString("registerNo");
                                String dept = doc.getString("department");
                                String sem = doc.getString("semester");
                                String pLevel = doc.getString("programLevel");
                                if (pLevel == null || pLevel.isEmpty()) pLevel = "UG";
                                pLevel = pLevel.toUpperCase();

                                if (name != null) {
                                    tvStudentName.setText(name);
                                    if (tvAccountStudentName != null) tvAccountStudentName.setText(name);
                                }
                                if (reg != null) tvStudentRegNo.setText(reg);
                                if (dept != null && sem != null) tvStudentDeptSem.setText("[" + pLevel + "] " + dept + " • " + sem);
                                if (reg != null && dept != null && tvAccountStudentInfo != null) {
                                    tvAccountStudentInfo.setText(reg + " • " + pLevel + " " + dept);
                                }
                            }
                        });
            }
        }

        // Update Unread Notifications Badge
        int unreadCount = dbHelper.getUnreadNotificationsCount("STUDENT");
        if (tvStudentUnreadBadge != null) {
            if (unreadCount > 0) {
                tvStudentUnreadBadge.setVisibility(View.VISIBLE);
                tvStudentUnreadBadge.setText(String.valueOf(Math.min(unreadCount, 99)));
            } else {
                tvStudentUnreadBadge.setVisibility(View.GONE);
            }
        }

        if (imgStudentAvatar != null) {
            String photoUri = sessionManager.getProfilePhotoUri();
            String regNo = currentStudent != null ? currentStudent.getRegNo() : sessionManager.getIdentifier();
            if (photoUri == null || photoUri.isEmpty()) {
                if (currentStudent != null && currentStudent.getPhotoUri() != null && !currentStudent.getPhotoUri().isEmpty()) {
                    photoUri = currentStudent.getPhotoUri();
                }
            }
            if (photoUri == null || photoUri.isEmpty()) {
                photoUri = dbHelper.getStudentPhotoUri(sessionManager.getUserId(), regNo);
            }
            if ((photoUri == null || photoUri.isEmpty()) && regNo != null && !regNo.isEmpty()) {
                String safeId = regNo.replaceAll("[^a-zA-Z0-9_-]", "_");
                java.io.File defaultFile = new java.io.File(getFilesDir(), "profile_photos/student_" + safeId + ".jpg");
                if (defaultFile.exists() && defaultFile.length() > 0) {
                    photoUri = defaultFile.getAbsolutePath();
                }
            }
            com.example.utils.ProfilePhotoManager.displayProfilePhoto(this, photoUri, imgStudentAvatar, R.drawable.ic_profile);
        }
    }

    private void loadAcademicStats() {
        int studentId = currentStudent != null ? currentStudent.getId() : (sessionManager.getUserId() > 0 ? sessionManager.getUserId() : 0);
        double cgpa = studentId > 0 ? dbHelper.getLatestCGPA(studentId) : 0.0;
        double sgpa = studentId > 0 ? dbHelper.getLatestSGPA(studentId) : 0.0;
        int attendance = studentId > 0 ? dbHelper.getStudentAttendancePercentage(studentId) : 0;
        int credits = studentId > 0 ? dbHelper.getStudentEarnedCredits(studentId) : 0;

        tvStatCGPA.setText(String.format(java.util.Locale.US, "%.2f", cgpa));
        tvStatSGPA.setText(String.format(java.util.Locale.US, "%.2f", sgpa));
        tvStatAttendance.setText(attendance + "%");
        tvStatCredits.setText(credits + " / 160");

        if (chipPerformanceRating != null) {
            if (cgpa >= 8.5) {
                chipPerformanceRating.setText("Distinction");
            } else if (cgpa >= 7.5) {
                chipPerformanceRating.setText("First Class");
            } else if (cgpa >= 6.0) {
                chipPerformanceRating.setText("Second Class");
            } else if (cgpa > 0.0) {
                chipPerformanceRating.setText("Pass");
            } else {
                chipPerformanceRating.setText("Enrolled");
            }
        }

        // Load Dynamic Quick Access Grid Counts from SQLite
        int enrolledSubjectsCount = dbHelper.getAllSubjects().size();
        if (tvQuickSubjectsCount != null) {
            tvQuickSubjectsCount.setText(enrolledSubjectsCount + " Enrolled");
        }

        int activeAssignmentsCount = dbHelper.getAssignmentsCount();
        if (tvQuickAssignmentsCount != null) {
            tvQuickAssignmentsCount.setText(activeAssignmentsCount + " Active");
        }

        int pendingAssessmentsCount = dbHelper.getPendingAssessmentsCount();
        if (tvQuickAssessmentsCount != null) {
            tvQuickAssessmentsCount.setText(pendingAssessmentsCount + " Pending");
        }

        if (tvQuickAttendanceCount != null) {
            tvQuickAttendanceCount.setText(attendance + "% Overall");
        }
    }

    private void setupSGPALineChart() {
        int studentId = currentStudent != null ? currentStudent.getId() : 1;
        currentTrends = com.example.utils.AcademicChartHelper.loadMultiSemesterTrends(dbHelper, studentId, currentStudent);
        updateTrendBadges();
        renderTrendChart();
    }

    private void updateTrendBadges() {
        if (currentTrends == null || currentTrends.isEmpty()) return;
        double latestCgpa = currentTrends.get(currentTrends.size() - 1).getCgpa();
        double peakSgpa = 0;
        for (com.example.model.SemesterPerformanceTrend t : currentTrends) {
            if (t.getSgpa() > peakSgpa) peakSgpa = t.getSgpa();
        }
        double delta = 0;
        if (currentTrends.size() >= 2) {
            delta = currentTrends.get(currentTrends.size() - 1).getSgpa() - currentTrends.get(currentTrends.size() - 2).getSgpa();
        }

        if (tvTrendCgpaVal != null) {
            tvTrendCgpaVal.setText(String.format(java.util.Locale.US, "%.2f", latestCgpa));
        }
        if (tvTrendPeakVal != null) {
            tvTrendPeakVal.setText(String.format(java.util.Locale.US, "%.2f", peakSgpa));
        }
        if (tvTrendDeltaVal != null) {
            String sign = delta >= 0 ? "+" : "";
            tvTrendDeltaVal.setText(String.format(java.util.Locale.US, "%s%.2f", sign, delta));
            tvTrendDeltaVal.setTextColor(delta >= 0 ? Color.parseColor("#10B981") : Color.parseColor("#EF4444"));
        }
    }

    private void renderTrendChart() {
        if (sgpaLineChart == null) return;
        if (currentTrends == null) {
            setupSGPALineChart();
            return;
        }
        com.example.utils.AcademicChartHelper.renderAcademicTrendLineChart(this, sgpaLineChart, currentTrends, currentMetricMode);
    }

    private void setupSubjectMarksBarChart() {
        if (subjectMarksBarChart == null) return;
        subjectMarksBarChart.clear();
        subjectMarksBarChart.getDescription().setEnabled(false);
        subjectMarksBarChart.setDrawGridBackground(false);
        subjectMarksBarChart.setTouchEnabled(true);

        int studentId = currentStudent != null ? currentStudent.getId() : 1;
        List<com.example.model.SubjectGradeItem> markItems = dbHelper.getStudentSubjectMarks(studentId);

        if (markItems == null || markItems.isEmpty()) {
            subjectMarksBarChart.setNoDataText("No subject marks entered yet.");
            subjectMarksBarChart.invalidate();
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> subjectCodes = new ArrayList<>();

        for (int i = 0; i < markItems.size(); i++) {
            com.example.model.SubjectGradeItem item = markItems.get(i);
            String name = item.getSubjectName() != null ? item.getSubjectName() : "Sub " + (i + 1);
            if (name.length() > 6) name = name.substring(0, 5) + "..";
            subjectCodes.add(name);
            entries.add(new BarEntry(i, (float) item.getTotalMarks()));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Subject Total Marks");
        dataSet.setColor(Color.parseColor("#06B6D4"));
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.parseColor("#0F172A"));

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);
        subjectMarksBarChart.setData(barData);

        XAxis xAxis = subjectMarksBarChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(subjectCodes));
        xAxis.setDrawGridLines(false);

        subjectMarksBarChart.getAxisRight().setEnabled(false);
        subjectMarksBarChart.getAxisLeft().setAxisMinimum(0f);
        subjectMarksBarChart.getAxisLeft().setAxisMaximum(100f);
        subjectMarksBarChart.animateY(800);
        subjectMarksBarChart.invalidate();
    }

    private void setupGradeDistributionPieChart() {
        if (gradePieChart == null) return;
        gradePieChart.clear();
        gradePieChart.getDescription().setEnabled(false);
        gradePieChart.setUsePercentValues(true);
        gradePieChart.setDrawHoleEnabled(true);
        gradePieChart.setHoleColor(Color.TRANSPARENT);
        gradePieChart.setHoleRadius(42f);
        gradePieChart.setTransparentCircleRadius(48f);

        int studentId = currentStudent != null ? currentStudent.getId() : 1;
        List<com.example.model.SubjectGradeItem> markItems = dbHelper.getStudentSubjectMarks(studentId);

        if (markItems == null || markItems.isEmpty()) {
            gradePieChart.setNoDataText("No grades available to calculate distribution.");
            gradePieChart.invalidate();
            return;
        }

        Map<String, Integer> gradeCounts = new HashMap<>();
        gradeCounts.put("A+", 0);
        gradeCounts.put("A", 0);
        gradeCounts.put("B+", 0);
        gradeCounts.put("B", 0);
        gradeCounts.put("C", 0);
        gradeCounts.put("F", 0);

        for (com.example.model.SubjectGradeItem item : markItems) {
            String g = item.getGrade() != null ? item.getGrade().toUpperCase() : "A";
            gradeCounts.put(g, gradeCounts.containsKey(g) ? gradeCounts.get(g) + 1 : 1);
        }

        List<PieEntry> pieEntries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        if (gradeCounts.get("A+") != null && gradeCounts.get("A+") > 0) {
            pieEntries.add(new PieEntry(gradeCounts.get("A+"), "A+"));
            colors.add(Color.parseColor("#10B981")); // Emerald
        }
        if (gradeCounts.get("A") != null && gradeCounts.get("A") > 0) {
            pieEntries.add(new PieEntry(gradeCounts.get("A"), "A"));
            colors.add(Color.parseColor("#3B82F6")); // Blue
        }
        if (gradeCounts.get("B+") != null && gradeCounts.get("B+") > 0) {
            pieEntries.add(new PieEntry(gradeCounts.get("B+"), "B+"));
            colors.add(Color.parseColor("#6366F1")); // Indigo
        }
        if (gradeCounts.get("B") != null && gradeCounts.get("B") > 0) {
            pieEntries.add(new PieEntry(gradeCounts.get("B"), "B"));
            colors.add(Color.parseColor("#F59E0B")); // Amber
        }
        if (gradeCounts.get("C") != null && gradeCounts.get("C") > 0) {
            pieEntries.add(new PieEntry(gradeCounts.get("C"), "C"));
            colors.add(Color.parseColor("#8B5CF6")); // Purple
        }
        if (gradeCounts.get("F") != null && gradeCounts.get("F") > 0) {
            pieEntries.add(new PieEntry(gradeCounts.get("F"), "F"));
            colors.add(Color.parseColor("#EF4444")); // Red
        }

        if (pieEntries.isEmpty()) {
            pieEntries.add(new PieEntry(1, "A+"));
            colors.add(Color.parseColor("#10B981"));
        }

        PieDataSet dataSet = new PieDataSet(pieEntries, "Grade Distribution");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setSliceSpace(3f);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new PercentFormatter(gradePieChart));
        gradePieChart.setData(pieData);
        gradePieChart.animateY(800);
        gradePieChart.invalidate();
    }

    private void setupNotificationsList() {
        List<AppNotification> notifs = dbHelper.getNotificationsForRole("STUDENT");
        List<ActivityItem> list = new ArrayList<>();
        for (AppNotification n : notifs) {
            list.add(new ActivityItem(n.getTitle(), n.getDate(), n.getCategory(), R.drawable.ic_notifications));
        }

        rvStudentNotifications.setLayoutManager(new LinearLayoutManager(this));
        RecentActivityAdapter adapter = new RecentActivityAdapter(list);
        rvStudentNotifications.setAdapter(adapter);
    }

    private void setupQuickAccessClickListeners() {
        if (btnStudentNotificationsHeader != null) {
            btnStudentNotificationsHeader.setOnClickListener(v -> {
                Intent intent = new Intent(StudentDashboardActivity.this, NotificationsActivity.class);
                startActivity(intent);
            });
        }
        if (btnStudentMenu != null) {
            btnStudentMenu.setOnClickListener(v -> {
                Intent intent = new Intent(StudentDashboardActivity.this, StudentProfileActivity.class);
                startActivity(intent);
            });
        }
        if (cardStudentProfileAvatar != null) {
            cardStudentProfileAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(StudentDashboardActivity.this, StudentProfileActivity.class);
                startActivity(intent);
            });
        }
        if (tvRefreshNotifications != null) {
            tvRefreshNotifications.setOnClickListener(v -> {
                loadAcademicStats();
                setupNotificationsList();
                Toast.makeText(this, "Academic stats & alerts refreshed", Toast.LENGTH_SHORT).show();
            });
        }

        // 2x2 Quick Access Grid Card Listeners
        if (cardQuickSubjects != null) {
            cardQuickSubjects.setOnClickListener(v -> {
                Intent intent = new Intent(StudentDashboardActivity.this, SubjectsActivity.class);
                startActivity(intent);
            });
        }
        if (cardQuickAssignments != null) {
            cardQuickAssignments.setOnClickListener(v -> {
                Intent intent = new Intent(StudentDashboardActivity.this, StudentAssignmentActivity.class);
                startActivity(intent);
            });
        }
        if (cardQuickAssessments != null) {
            cardQuickAssessments.setOnClickListener(v -> {
                Intent intent = new Intent(StudentDashboardActivity.this, AssessmentsActivity.class);
                startActivity(intent);
            });
        }
        if (cardQuickAttendance != null) {
            cardQuickAttendance.setOnClickListener(v -> {
                Intent intent = new Intent(StudentDashboardActivity.this, StudentAttendanceActivity.class);
                startActivity(intent);
            });
        }

        if (quickResults != null) {
            quickResults.setOnClickListener(v -> {
                Intent intent = new Intent(StudentDashboardActivity.this, ResultsActivity.class);
                startActivity(intent);
            });
        }
        if (quickCalculator != null) {
            quickCalculator.setOnClickListener(v -> {
                Intent intent = new Intent(StudentDashboardActivity.this, GradeCalculatorActivity.class);
                startActivity(intent);
            });
        }
        if (quickNotifications != null) {
            quickNotifications.setOnClickListener(v -> {
                Intent intent = new Intent(StudentDashboardActivity.this, NotificationsActivity.class);
                startActivity(intent);
            });
        }
        if (quickProfile != null) {
            quickProfile.setOnClickListener(v -> {
                Intent intent = new Intent(StudentDashboardActivity.this, StudentProfileActivity.class);
                startActivity(intent);
            });
        }

        if (btnStudentHeaderLogout != null) {
            btnStudentHeaderLogout.setOnClickListener(v -> confirmLogout());
        }

        if (btnStudentDashboardLogout != null) {
            btnStudentDashboardLogout.setOnClickListener(v -> confirmLogout());
        }
    }

    private void confirmLogout() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("LOGOUT", (dialog, which) -> performLogout())
                .setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void performLogout() {
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
        } catch (Exception e) {
            android.util.Log.e("StudentDashboard", "FirebaseAuth signOut error: " + e.getMessage());
        }

        if (sessionManager != null) {
            sessionManager.logout();
        }

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(StudentDashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showToolMessage(String toolName) {
        Toast.makeText(this, toolName + " clicked", Toast.LENGTH_SHORT).show();
    }

    private void setupBottomNavigation() {
        if (studentBottomNavigation != null) {
            studentBottomNavigation.setSelectedItemId(R.id.nav_student_home);
            studentBottomNavigation.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_student_home) {
                    return true;
                } else if (id == R.id.nav_student_subjects) {
                    Intent intent = new Intent(StudentDashboardActivity.this, SubjectsActivity.class);
                    startActivity(intent);
                    return true;
                } else if (id == R.id.nav_student_attendance) {
                    Intent intent = new Intent(StudentDashboardActivity.this, StudentAttendanceActivity.class);
                    startActivity(intent);
                    return true;
                } else if (id == R.id.nav_student_results) {
                    Intent intent = new Intent(StudentDashboardActivity.this, ResultsActivity.class);
                    startActivity(intent);
                    return true;
                } else if (id == R.id.nav_student_profile) {
                    Intent intent = new Intent(StudentDashboardActivity.this, StudentProfileActivity.class);
                    startActivity(intent);
                    return true;
                }
                return false;
            });
        }
    }

    private void setupGradeAlertsListener(String regNo) {
        if (gradeAlertsListener != null) return;
        if (regNo == null || regNo.trim().isEmpty()) return;

        gradeAlertsListener = com.example.utils.GradeNotificationHelper.listenForStudentGradeAlerts(
                this,
                regNo,
                (subjectName, grade, percentage) -> runOnUiThread(() -> {
                    loadAcademicStats();
                    setupSubjectMarksBarChart();
                    setupSGPALineChart();
                    setupNotificationsList();
                })
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gradeAlertsListener != null) {
            gradeAlertsListener.remove();
            gradeAlertsListener = null;
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(StudentDashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
