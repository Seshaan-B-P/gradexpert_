package com.example;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.adapter.StudentAdapter;
import com.example.database.DatabaseHelper;
import com.example.model.Student;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Hour-Based Attendance Monitoring Activity for System Admin.
 * Attendance % = Present Hours / Total Hours * 100
 */
public class AdminAttendanceActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private FirebaseFirestore db;

    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvOverallAvg;
    private TextView tvAtRiskCount;
    private ChipGroup chipGroupDept;
    private ProgressBar progressBar;
    private RecyclerView rvAttendanceList;

    private StudentAdapter adapter;
    private List<Student> masterStudentList = new ArrayList<>();
    private List<Student> filteredStudentList = new ArrayList<>();

    private String selectedDept = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_attendance);

        dbHelper = new DatabaseHelper(this);
        db = FirebaseFirestore.getInstance();

        initViews();
        setupToolbar();
        setupWindowInsets();
        setupSwipeRefresh();
        setupDeptFilter();
        setupRecyclerView();
        loadAttendanceData();
    }

    private void setupWindowInsets() {
        View appBar = findViewById(R.id.appBarAdminAttendance);
        if (appBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, insets) -> {
                androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                v.setPadding(0, statusBarInsets.top, 0, 0);
                return insets;
            });
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarAdminAttendance);
        swipeRefresh = findViewById(R.id.swipeRefreshAdminAttendance);
        tvOverallAvg = findViewById(R.id.tvAdminOverallAvgAttendance);
        tvAtRiskCount = findViewById(R.id.tvAdminAtRiskCount);
        chipGroupDept = findViewById(R.id.chipGroupAttendanceDeptFilter);
        progressBar = findViewById(R.id.progressBarAdminAttendance);
        rvAttendanceList = findViewById(R.id.rvAdminAttendanceList);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Attendance Monitoring");
            getSupportActionBar().setSubtitle("Hour-Based System Wide Attendance");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(this::loadAttendanceData);
    }

    private void setupDeptFilter() {
        chipGroupDept.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipDeptMCA) {
                selectedDept = "MCA";
            } else if (checkedId == R.id.chipDeptCSE) {
                selectedDept = "CSE";
            } else if (checkedId == R.id.chipDeptIT) {
                selectedDept = "IT";
            } else {
                selectedDept = "ALL";
            }
            filterAttendanceList();
        });
    }

    private void setupRecyclerView() {
        rvAttendanceList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StudentAdapter(filteredStudentList, null);
        rvAttendanceList.setAdapter(adapter);
    }

    private void loadAttendanceData() {
        showLoading(true);
        masterStudentList = dbHelper.getAllStudents();
        showLoading(false);
        swipeRefresh.setRefreshing(false);

        calculateStats();
        filterAttendanceList();
    }

    private void calculateStats() {
        if (masterStudentList.isEmpty()) {
            tvOverallAvg.setText("0.0%");
            tvAtRiskCount.setText("0 Students");
            return;
        }

        double totalAttendance = 0;
        int atRisk = 0;

        for (Student s : masterStudentList) {
            double att = s.getAttendancePercentage();
            totalAttendance += att;
            if (att < 75.0) {
                atRisk++;
            }
        }

        double avg = totalAttendance / masterStudentList.size();
        tvOverallAvg.setText(String.format("%.1f%%", avg));
        tvAtRiskCount.setText(atRisk + " Student" + (atRisk == 1 ? "" : "s"));
    }

    private void filterAttendanceList() {
        filteredStudentList.clear();
        for (Student s : masterStudentList) {
            if ("ALL".equalsIgnoreCase(selectedDept) || (s.getDepartment() != null && s.getDepartment().equalsIgnoreCase(selectedDept))) {
                filteredStudentList.add(s);
            }
        }
        adapter.updateData(filteredStudentList);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
