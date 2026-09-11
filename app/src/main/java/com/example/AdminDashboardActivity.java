package com.example;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.adapter.RecentActivityAdapter;
import com.example.database.DatabaseHelper;
import com.example.database.FirestoreHelper;
import com.example.model.ActivityItem;
import com.example.model.Department;
import com.example.utils.IdGenerationService;
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

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Main Admin Dashboard Activity for GradeXpert System Administration.
 * Provides live academic metrics for UG and PG classifications.
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private FirebaseFirestore db;

    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;

    private TextView tvWelcomeAdminTitle;
    private TextView tvAdminHeaderDateAcademicYear;
    private ImageView btnHeaderAdminProfile;

    private MaterialCardView cardAdminPasswordResets;
    private com.google.android.material.chip.Chip chipAdminPendingResetCount;
    private com.google.firebase.firestore.ListenerRegistration pendingRequestsListener;

    private TextView tvTotalStudentsCount;
    private TextView tvTotalTeachersCount;
    private TextView tvTotalSubjectsCount;
    private TextView tvTotalDeptsCount;

    // Academic Overview UG / PG Breakdown Counters
    private TextView tvAdminUgStudentsCount;
    private TextView tvAdminPgStudentsCount;
    private TextView tvAdminUgTeachersCount;
    private TextView tvAdminPgTeachersCount;
    private TextView tvAdminUgDeptsCount;
    private TextView tvAdminPgDeptsCount;

    private MaterialCardView cardStatStudents;
    private MaterialCardView cardStatTeachers;
    private MaterialCardView cardStatSubjects;
    private MaterialCardView cardStatDepts;

    private MaterialCardView btnQuickManageUsers;
    private MaterialCardView btnQuickManageTeachers;
    private MaterialCardView btnQuickManageStudents;
    private MaterialCardView btnQuickManageDepts;
    private MaterialCardView btnQuickManageSubjects;
    private MaterialCardView btnQuickAdminAttendance;
    private MaterialCardView btnQuickAdminAssignments;
    private MaterialCardView btnQuickAdminResults;
    private MaterialCardView btnQuickAcademicReports;
    private MaterialCardView btnQuickBroadcastAlerts;
    private MaterialCardView btnQuickPortalActivities;
    private MaterialCardView btnQuickAdminSettings;

    private PieChart pieChartStudentsByDept;
    private BarChart barChartAttendanceOverview;

    private TextView tvViewAllActivities;
    private RecyclerView rvTeacherActivities;
    private RecentActivityAdapter activityAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);
        db = FirebaseFirestore.getInstance();

        if (!sessionManager.isLoggedIn() || !"ADMIN".equalsIgnoreCase(sessionManager.getUserRole())) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_admin_dashboard);

        initViews();
        setupToolbar();
        loadHeaderData();
        setupSwipeRefresh();
        loadFirestoreStats();
        setupAnalyticsCharts();
        setupRecentActivitiesList();
        setupQuickActions();
        listenToPendingResetRequests();
        setupWindowInsets();
        checkAndRequestNotificationPermission();
        MyFirebaseMessagingService.registerFcmToken(this);

        // Background migration check for existing accounts to ensure Login IDs and lookup mapping are synchronized
        IdGenerationService.getInstance().migrateExistingAccountsIfNeeded((count, message) -> {
            if (count > 0) {
                Log.d("AdminDashboard", "Account identity migration: " + message);
            }
        });
    }

    private void checkAndRequestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHeaderData();
        loadFirestoreStats();
        setupRecentActivitiesList();
        MyFirebaseMessagingService.registerFcmToken(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pendingRequestsListener != null) {
            pendingRequestsListener.remove();
        }
    }

    private void setupWindowInsets() {
        View root = findViewById(R.id.swipeRefreshAdminDashboard);
        if (root != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                v.setPadding(0, statusBarInsets.top, 0, 0);
                return insets;
            });
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarAdminDashboard);
        swipeRefresh = findViewById(R.id.swipeRefreshAdminDashboard);

        tvWelcomeAdminTitle = findViewById(R.id.tvWelcomeAdminTitle);
        tvAdminHeaderDateAcademicYear = findViewById(R.id.tvAdminHeaderDateAcademicYear);
        btnHeaderAdminProfile = findViewById(R.id.btnHeaderAdminProfile);

        cardAdminPasswordResets = findViewById(R.id.cardAdminPasswordResets);
        chipAdminPendingResetCount = findViewById(R.id.chipAdminPendingResetCount);

        cardStatStudents = findViewById(R.id.cardStatStudents);
        cardStatTeachers = findViewById(R.id.cardStatTeachers);
        cardStatSubjects = findViewById(R.id.cardStatSubjects);
        cardStatDepts = findViewById(R.id.cardStatDepts);

        tvTotalStudentsCount = findViewById(R.id.tvAdminTotalStudentsCount);
        tvTotalTeachersCount = findViewById(R.id.tvAdminTotalTeachersCount);
        tvTotalSubjectsCount = findViewById(R.id.tvAdminTotalSubjectsCount);
        tvTotalDeptsCount = findViewById(R.id.tvAdminTotalDeptsCount);

        // Academic Overview breakdown views
        tvAdminUgStudentsCount = findViewById(R.id.tvAdminUgStudentsCount);
        tvAdminPgStudentsCount = findViewById(R.id.tvAdminPgStudentsCount);
        tvAdminUgTeachersCount = findViewById(R.id.tvAdminUgTeachersCount);
        tvAdminPgTeachersCount = findViewById(R.id.tvAdminPgTeachersCount);
        tvAdminUgDeptsCount = findViewById(R.id.tvAdminUgDeptsCount);
        tvAdminPgDeptsCount = findViewById(R.id.tvAdminPgDeptsCount);

        btnQuickManageUsers = findViewById(R.id.btnQuickManageUsers);
        btnQuickManageTeachers = findViewById(R.id.btnQuickManageTeachers);
        btnQuickManageStudents = findViewById(R.id.btnQuickManageStudents);
        btnQuickManageDepts = findViewById(R.id.btnQuickManageDepts);
        btnQuickManageSubjects = findViewById(R.id.btnQuickManageSubjects);
        btnQuickAdminAttendance = findViewById(R.id.btnQuickAdminAttendance);
        btnQuickAdminAssignments = findViewById(R.id.btnQuickAdminAssignments);
        btnQuickAdminResults = findViewById(R.id.btnQuickAdminResults);
        btnQuickAcademicReports = findViewById(R.id.btnQuickAcademicReports);
        btnQuickBroadcastAlerts = findViewById(R.id.btnQuickBroadcastAlerts);
        btnQuickPortalActivities = findViewById(R.id.btnQuickPortalActivities);
        btnQuickAdminSettings = findViewById(R.id.btnQuickAdminSettings);

        pieChartStudentsByDept = findViewById(R.id.pieChartStudentsByDept);
        barChartAttendanceOverview = findViewById(R.id.barChartAttendanceOverview);

        tvViewAllActivities = findViewById(R.id.tvAdminViewAllActivities);
        rvTeacherActivities = findViewById(R.id.rvAdminTeacherActivities);
        if (rvTeacherActivities != null) {
            rvTeacherActivities.setLayoutManager(new LinearLayoutManager(this));
        }
    }

    private void listenToPendingResetRequests() {
        try {
            pendingRequestsListener = com.example.repository.PasswordResetRepository.getInstance(this)
                    .listenPendingRequestsCount(new com.example.repository.PasswordResetRepository.OnCountListener() {
                        @Override
                        public void onCount(int count) {
                            if (chipAdminPendingResetCount != null) {
                                if (count > 0) {
                                    chipAdminPendingResetCount.setText("🔴 " + count + " Pending");
                                    chipAdminPendingResetCount.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#EF4444"))); // Red
                                } else {
                                    chipAdminPendingResetCount.setText("0 Pending");
                                    chipAdminPendingResetCount.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#10B981"))); // Green
                                }
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                        }
                    });
        } catch (Exception ignored) {}
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("GradeXpert");
            getSupportActionBar().setSubtitle("Admin Dashboard • 2026-27");
        }
    }

    private void loadHeaderData() {
        String adminName = sessionManager.getUserName();
        tvWelcomeAdminTitle.setText("Welcome, " + (adminName != null && !adminName.isEmpty() ? adminName : "Admin"));

        String dateStr = new SimpleDateFormat("EEEE, dd MMM yyyy", Locale.US).format(new Date());
        tvAdminHeaderDateAcademicYear.setText("Academic Year: 2026-27 • " + dateStr);

        btnHeaderAdminProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminProfileActivity.class));
        });
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(() -> {
            FirestoreHelper.getInstance().syncFirestoreDataToLocal(dbHelper, null);
            loadFirestoreStats();
            setupRecentActivitiesList();
            setupAnalyticsCharts();
            swipeRefresh.setRefreshing(false);
        });
    }

    private void loadFirestoreStats() {
        if (db == null) return;
        // Fetch Students Count and UG/PG breakdown
        try {
            db.collection("students").get().addOnSuccessListener(queryDocumentSnapshots -> {
                int total = 0;
                int ugCount = 0;
                int pgCount = 0;

                if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                    total = queryDocumentSnapshots.size();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String pLevel = doc.getString("programLevel");
                        String dept = doc.getString("department");
                        if (pLevel == null || pLevel.isEmpty()) {
                            pLevel = Department.resolveDefaultProgramLevel(dept, dept);
                        }
                        if ("PG".equalsIgnoreCase(pLevel)) {
                            pgCount++;
                        } else {
                            ugCount++;
                        }
                    }
                }

                if (tvTotalStudentsCount != null) tvTotalStudentsCount.setText(String.valueOf(total));
                if (tvAdminUgStudentsCount != null) tvAdminUgStudentsCount.setText(String.valueOf(ugCount));
                if (tvAdminPgStudentsCount != null) tvAdminPgStudentsCount.setText(String.valueOf(pgCount));
            }).addOnFailureListener(e -> {
                int fallback = dbHelper != null ? dbHelper.getStudentsCount() : 0;
                if (tvTotalStudentsCount != null) tvTotalStudentsCount.setText(String.valueOf(fallback));
                if (tvAdminUgStudentsCount != null) tvAdminUgStudentsCount.setText(String.valueOf(fallback));
                if (tvAdminPgStudentsCount != null) tvAdminPgStudentsCount.setText("0");
            });

            // Fetch Teachers Count and UG/PG breakdown
            db.collection("teachers").get().addOnSuccessListener(queryDocumentSnapshots -> {
                int total = 0;
                int ugCount = 0;
                int pgCount = 0;

                if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                    total = queryDocumentSnapshots.size();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String pLevel = doc.getString("programLevel");
                        String dept = doc.getString("department");
                        if (pLevel == null || pLevel.isEmpty()) {
                            pLevel = Department.resolveDefaultProgramLevel(dept, dept);
                        }
                        if ("PG".equalsIgnoreCase(pLevel)) {
                            pgCount++;
                        } else {
                            ugCount++;
                        }
                    }
                }

                if (tvTotalTeachersCount != null) tvTotalTeachersCount.setText(String.valueOf(total));
                if (tvAdminUgTeachersCount != null) tvAdminUgTeachersCount.setText(String.valueOf(ugCount));
                if (tvAdminPgTeachersCount != null) tvAdminPgTeachersCount.setText(String.valueOf(pgCount));
            }).addOnFailureListener(e -> {
                int fallback = dbHelper != null ? dbHelper.getTeacherCount() : 0;
                if (tvTotalTeachersCount != null) tvTotalTeachersCount.setText(String.valueOf(fallback));
                if (tvAdminUgTeachersCount != null) tvAdminUgTeachersCount.setText(String.valueOf(fallback));
                if (tvAdminPgTeachersCount != null) tvAdminPgTeachersCount.setText("0");
            });

            // Fetch Subjects Count
            db.collection("subjects").get().addOnSuccessListener(queryDocumentSnapshots -> {
                int count = (queryDocumentSnapshots != null) ? queryDocumentSnapshots.size() : 0;
                if (tvTotalSubjectsCount != null) tvTotalSubjectsCount.setText(String.valueOf(count));
            }).addOnFailureListener(e -> {
                if (tvTotalSubjectsCount != null && dbHelper != null) tvTotalSubjectsCount.setText(String.valueOf(dbHelper.getSubjectsCount()));
            });

            // Fetch Departments Count and UG/PG breakdown
            db.collection("departments").get().addOnSuccessListener(queryDocumentSnapshots -> {
                int total = 0;
                int ugCount = 0;
                int pgCount = 0;

                if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                    total = queryDocumentSnapshots.size();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String pLevel = doc.getString("programLevel");
                        String code = doc.getString("shortName");
                        String name = doc.getString("name");
                        if (pLevel == null || pLevel.isEmpty()) {
                            pLevel = Department.resolveDefaultProgramLevel(code, name);
                        }
                        if ("PG".equalsIgnoreCase(pLevel)) {
                            pgCount++;
                        } else {
                            ugCount++;
                        }
                    }
                }

                if (tvTotalDeptsCount != null) tvTotalDeptsCount.setText(String.valueOf(total));
                if (tvAdminUgDeptsCount != null) tvAdminUgDeptsCount.setText(String.valueOf(ugCount));
                if (tvAdminPgDeptsCount != null) tvAdminPgDeptsCount.setText(String.valueOf(pgCount));
            }).addOnFailureListener(e -> {
                if (tvTotalDeptsCount != null) tvTotalDeptsCount.setText("0");
                if (tvAdminUgDeptsCount != null) tvAdminUgDeptsCount.setText("0");
                if (tvAdminPgDeptsCount != null) tvAdminPgDeptsCount.setText("0");
            });
        } catch (Exception ignored) {}
    }

    private void setupQuickActions() {
        if (cardAdminPasswordResets != null) {
            cardAdminPasswordResets.setOnClickListener(v -> startActivity(new Intent(this, PasswordResetRequestsActivity.class)));
        }

        if (cardStatStudents != null) cardStatStudents.setOnClickListener(v -> startActivity(new Intent(this, ManageStudentsActivity.class)));
        if (cardStatTeachers != null) cardStatTeachers.setOnClickListener(v -> startActivity(new Intent(this, ManageTeachersActivity.class)));
        if (cardStatSubjects != null) cardStatSubjects.setOnClickListener(v -> startActivity(new Intent(this, ManageSubjectsActivity.class)));
        if (cardStatDepts != null) cardStatDepts.setOnClickListener(v -> startActivity(new Intent(this, ManageDepartmentsActivity.class)));

        if (btnQuickManageUsers != null) btnQuickManageUsers.setOnClickListener(v -> startActivity(new Intent(this, ManageUsersActivity.class)));
        if (btnQuickManageTeachers != null) btnQuickManageTeachers.setOnClickListener(v -> startActivity(new Intent(this, ManageTeachersActivity.class)));
        if (btnQuickManageStudents != null) btnQuickManageStudents.setOnClickListener(v -> startActivity(new Intent(this, ManageStudentsActivity.class)));
        if (btnQuickManageDepts != null) btnQuickManageDepts.setOnClickListener(v -> startActivity(new Intent(this, ManageDepartmentsActivity.class)));
        if (btnQuickManageSubjects != null) btnQuickManageSubjects.setOnClickListener(v -> startActivity(new Intent(this, ManageSubjectsActivity.class)));
        if (btnQuickAdminAttendance != null) btnQuickAdminAttendance.setOnClickListener(v -> startActivity(new Intent(this, AdminAttendanceActivity.class)));
        if (btnQuickAdminAssignments != null) btnQuickAdminAssignments.setOnClickListener(v -> startActivity(new Intent(this, AdminAssignmentsActivity.class)));
        if (btnQuickAdminResults != null) btnQuickAdminResults.setOnClickListener(v -> startActivity(new Intent(this, AdminResultsActivity.class)));
        if (btnQuickAcademicReports != null) btnQuickAcademicReports.setOnClickListener(v -> startActivity(new Intent(this, AcademicReportsActivity.class)));
        if (btnQuickBroadcastAlerts != null) btnQuickBroadcastAlerts.setOnClickListener(v -> startActivity(new Intent(this, BroadcastAlertsActivity.class)));
        if (btnQuickPortalActivities != null) btnQuickPortalActivities.setOnClickListener(v -> startActivity(new Intent(this, PortalActivitiesActivity.class)));
        if (btnQuickAdminSettings != null) btnQuickAdminSettings.setOnClickListener(v -> startActivity(new Intent(this, AdminSettingsActivity.class)));

        if (tvViewAllActivities != null) tvViewAllActivities.setOnClickListener(v -> startActivity(new Intent(this, PortalActivitiesActivity.class)));
    }

    private void setupAnalyticsCharts() {
        setupStudentsPieChart();
        setupAttendanceBarChart();
    }

    private void setupStudentsPieChart() {
        if (pieChartStudentsByDept == null) return;

        db.collection("students").get().addOnSuccessListener(queryDocumentSnapshots -> {
            Map<String, Integer> deptCounts = new HashMap<>();
            if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    String dept = doc.getString("department");
                    if (dept == null || dept.trim().isEmpty()) dept = "General";
                    deptCounts.put(dept, deptCounts.containsKey(dept) ? deptCounts.get(dept) + 1 : 1);
                }
            } else {
                List<com.example.model.Student> localStudents = dbHelper.getAllStudents();
                for (com.example.model.Student s : localStudents) {
                    String dept = s.getDepartment();
                    if (dept == null || dept.trim().isEmpty()) dept = "General";
                    deptCounts.put(dept, deptCounts.containsKey(dept) ? deptCounts.get(dept) + 1 : 1);
                }
            }

            if (deptCounts.isEmpty()) {
                pieChartStudentsByDept.clear();
                pieChartStudentsByDept.setNoDataText("No students enrolled yet.");
                pieChartStudentsByDept.invalidate();
                return;
            }

            List<PieEntry> entries = new ArrayList<>();
            int[] palette = new int[]{
                    Color.parseColor("#7C3AED"),
                    Color.parseColor("#2563EB"),
                    Color.parseColor("#10B981"),
                    Color.parseColor("#F59E0B"),
                    Color.parseColor("#EF4444"),
                    Color.parseColor("#EC4899")
            };
            List<Integer> colors = new ArrayList<>();
            int colorIdx = 0;

            for (Map.Entry<String, Integer> e : deptCounts.entrySet()) {
                entries.add(new PieEntry(e.getValue().floatValue(), e.getKey()));
                colors.add(palette[colorIdx % palette.length]);
                colorIdx++;
            }

            PieDataSet dataSet = new PieDataSet(entries, "");
            dataSet.setColors(colors);
            dataSet.setValueTextColor(Color.WHITE);
            dataSet.setValueTextSize(12f);

            PieData pieData = new PieData(dataSet);
            pieChartStudentsByDept.setData(pieData);
            pieChartStudentsByDept.getDescription().setEnabled(false);
            pieChartStudentsByDept.setHoleColor(Color.TRANSPARENT);
            pieChartStudentsByDept.setEntryLabelColor(Color.WHITE);
            pieChartStudentsByDept.setEntryLabelTextSize(10f);
            pieChartStudentsByDept.animateY(800);
            pieChartStudentsByDept.invalidate();
        }).addOnFailureListener(e -> {
            pieChartStudentsByDept.clear();
            pieChartStudentsByDept.setNoDataText("No students enrolled yet.");
            pieChartStudentsByDept.invalidate();
        });
    }

    private void setupAttendanceBarChart() {
        if (barChartAttendanceOverview == null) return;

        db.collection("attendance").get().addOnSuccessListener(queryDocumentSnapshots -> {
            Map<String, int[]> deptAttMap = new HashMap<>(); // dept -> [total, present]

            if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    String dept = doc.getString("department");
                    if (dept == null || dept.trim().isEmpty()) dept = "General";
                    String status = doc.getString("status");
                    boolean isPresent = "PRESENT".equalsIgnoreCase(status);

                    if (!deptAttMap.containsKey(dept)) {
                        deptAttMap.put(dept, new int[]{0, 0});
                    }
                    int[] counts = deptAttMap.get(dept);
                    counts[0]++;
                    if (isPresent) counts[1]++;
                }
            }

            if (deptAttMap.isEmpty()) {
                barChartAttendanceOverview.clear();
                barChartAttendanceOverview.setNoDataText("No attendance recorded yet.");
                barChartAttendanceOverview.invalidate();
                return;
            }

            List<BarEntry> entries = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            int idx = 0;

            for (Map.Entry<String, int[]> entry : deptAttMap.entrySet()) {
                int[] counts = entry.getValue();
                float pct = counts[0] > 0 ? (float) ((counts[1] * 100.0) / counts[0]) : 0f;
                entries.add(new BarEntry(idx, pct));
                String label = entry.getKey();
                if (label.length() > 10) label = label.substring(0, 8) + "..";
                labels.add(label);
                idx++;
            }

            BarDataSet dataSet = new BarDataSet(entries, "Attendance %");
            dataSet.setColor(Color.parseColor("#10B981"));
            dataSet.setValueTextColor(Color.parseColor("#1E293B"));
            dataSet.setValueTextSize(10f);

            BarData barData = new BarData(dataSet);
            barData.setBarWidth(0.5f);

            barChartAttendanceOverview.setData(barData);
            barChartAttendanceOverview.getDescription().setEnabled(false);
            barChartAttendanceOverview.getAxisRight().setEnabled(false);

            XAxis xAxis = barChartAttendanceOverview.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
            xAxis.setGranularity(1f);
            xAxis.setDrawGridLines(false);

            barChartAttendanceOverview.animateY(800);
            barChartAttendanceOverview.invalidate();
        }).addOnFailureListener(e -> {
            barChartAttendanceOverview.clear();
            barChartAttendanceOverview.setNoDataText("No attendance recorded yet.");
            barChartAttendanceOverview.invalidate();
        });
    }

    private void setupRecentActivitiesList() {
        if (rvTeacherActivities == null) return;
        try {
            db.collection("portalActivities").limit(5).get().addOnSuccessListener(snapshots -> {
                List<ActivityItem> activities = new ArrayList<>();
                if (snapshots != null && !snapshots.isEmpty()) {
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        try {
                            String user = doc.getString("userName");
                            if (user == null) user = doc.getString("teacherName");
                            String action = doc.getString("actionTitle");
                            if (action == null) action = doc.getString("title");

                            String time = "Recent";
                            Object tsObj = doc.get("timestamp");
                            if (tsObj instanceof com.google.firebase.Timestamp) {
                                Date date = ((com.google.firebase.Timestamp) tsObj).toDate();
                                time = new SimpleDateFormat("hh:mm a, dd MMM", Locale.US).format(date);
                            } else if (tsObj instanceof Date) {
                                time = new SimpleDateFormat("hh:mm a, dd MMM", Locale.US).format((Date) tsObj);
                            } else if (tsObj instanceof String) {
                                time = (String) tsObj;
                            }

                            String cat = doc.getString("category");
                            if (cat == null) cat = doc.getString("entityType");

                            String title = (user != null ? user : "System") + " • " + (action != null ? action : "Action");
                            activities.add(new ActivityItem(title, time, cat != null ? cat : "General", R.drawable.ic_profile));
                        } catch (Exception docEx) {
                            docEx.printStackTrace();
                        }
                    }
                }

                if (activities.isEmpty() && dbHelper != null) {
                    activities = dbHelper.getAllTeacherActivities();
                    if (activities != null && activities.size() > 5) {
                        activities = activities.subList(0, 5);
                    }
                }

                if (rvTeacherActivities != null) {
                    activityAdapter = new RecentActivityAdapter(activities != null ? activities : new ArrayList<>());
                    rvTeacherActivities.setAdapter(activityAdapter);
                }
            }).addOnFailureListener(e -> {
                List<ActivityItem> activities = dbHelper != null ? dbHelper.getAllTeacherActivities() : new ArrayList<>();
                if (activities != null && activities.size() > 5) {
                    activities = activities.subList(0, 5);
                }
                if (rvTeacherActivities != null) {
                    activityAdapter = new RecentActivityAdapter(activities != null ? activities : new ArrayList<>());
                    rvTeacherActivities.setAdapter(activityAdapter);
                }
            });
        } catch (Exception e) {
            List<ActivityItem> activities = dbHelper != null ? dbHelper.getAllTeacherActivities() : new ArrayList<>();
            if (rvTeacherActivities != null) {
                activityAdapter = new RecentActivityAdapter(activities != null ? activities : new ArrayList<>());
                rvTeacherActivities.setAdapter(activityAdapter);
            }
        }
    }
}
