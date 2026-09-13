package com.example;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adapter.StudentAttendanceLogAdapter;
import com.example.adapter.StudentSubjectAttendanceAdapter;
import com.example.database.DatabaseHelper;
import com.example.model.Attendance;
import com.example.model.MonthlyAttendanceStats;
import com.example.model.StudentAttendanceLog;
import com.example.model.Subject;
import com.example.model.SubjectAttendanceStats;
import com.example.repository.AttendanceRepository;
import com.example.utils.AttendanceNotificationHelper;
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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Student Attendance Activity & Monitoring Module.
 * Allows students to:
 * 1. View official Firestore & local institutional attendance records & monthly charts.
 * 2. Log personal class attendance percentages with total and attended classes.
 * 3. Set custom attendance alert thresholds (default 75%).
 * 4. Receive system notifications and in-app alerts whenever attendance drops below threshold.
 * 5. View recovery calculations (exact consecutive classes needed to recover above threshold).
 */
public class StudentAttendanceActivity extends AppCompatActivity {

    private static final String TAG = "StudentAttendanceAct";

    private AttendanceRepository repository;
    private SessionManager sessionManager;
    private DatabaseHelper dbHelper;
    private StudentSubjectAttendanceAdapter subjectAdapter;
    private StudentAttendanceLogAdapter logsAdapter;

    private Toolbar toolbar;
    private TextView tvOverallPercentage;
    private TextView tvClassesAttended;
    private TextView tvClassesAbsent;
    private TextView tvExamEligibilityBadge;
    private TextView tvRequiredStatus;

    // Threshold & Monitoring Controls
    private View btnChangeThreshold;
    private TextView tvCurrentThresholdValue;
    private MaterialButton btnQuickLogAttendance;
    private View cardAttendanceAlertBanner;
    private TextView tvAlertBannerTitle, tvAlertBannerMessage, tvAlertRecoveryPlan;

    // Charts
    private MaterialButtonToggleGroup toggleMonthlyChartType;
    private MaterialButton btnChartMonthlyBar;
    private MaterialButton btnChartMonthlyLine;
    private BarChart chartMonthlyBar;
    private LineChart chartMonthlyLine;

    // Subject Breakdown
    private RecyclerView rvSubjectAttendance;

    // Logged Attendance List
    private TextView tvAttendanceLogsCount;
    private View layoutEmptyAttendanceLogs;
    private MaterialButton btnEmptyLogAttendance;
    private RecyclerView rvStudentAttendanceLogs;
    private ExtendedFloatingActionButton fabLogAttendance;

    private String currentStudentId = "1";
    private float currentThreshold = 75.0f;

    // Notification Permission Launcher (Android 13+)
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "POST_NOTIFICATIONS permission granted.");
                }
            });

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

        currentThreshold = sessionManager.getAttendanceThreshold();

        initViews();
        setupToolbar();
        setupWindowInsets();
        setupRecyclerViews();
        setupChartToggle();
        setupClickListeners();

        checkAndRequestNotificationPermission();
        loadStudentFirestoreAttendance();
        loadStudentAttendanceLogs();
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
        currentThreshold = sessionManager.getAttendanceThreshold();
        updateThresholdDisplay();
        loadStudentFirestoreAttendance();
        loadStudentAttendanceLogs();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarStudentAtt);
        tvOverallPercentage = findViewById(R.id.tvOverallPercentage);
        tvClassesAttended = findViewById(R.id.tvClassesAttended);
        tvClassesAbsent = findViewById(R.id.tvClassesAbsent);
        tvExamEligibilityBadge = findViewById(R.id.tvExamEligibilityBadge);
        tvRequiredStatus = findViewById(R.id.tvRequiredStatus);

        btnChangeThreshold = findViewById(R.id.btnChangeThreshold);
        tvCurrentThresholdValue = findViewById(R.id.tvCurrentThresholdValue);
        btnQuickLogAttendance = findViewById(R.id.btnQuickLogAttendance);

        cardAttendanceAlertBanner = findViewById(R.id.cardAttendanceAlertBanner);
        tvAlertBannerTitle = findViewById(R.id.tvAlertBannerTitle);
        tvAlertBannerMessage = findViewById(R.id.tvAlertBannerMessage);
        tvAlertRecoveryPlan = findViewById(R.id.tvAlertRecoveryPlan);

        toggleMonthlyChartType = findViewById(R.id.toggleMonthlyChartType);
        btnChartMonthlyBar = findViewById(R.id.btnChartMonthlyBar);
        btnChartMonthlyLine = findViewById(R.id.btnChartMonthlyLine);
        chartMonthlyBar = findViewById(R.id.chartMonthlyBar);
        chartMonthlyLine = findViewById(R.id.chartMonthlyLine);

        rvSubjectAttendance = findViewById(R.id.rvSubjectAttendance);

        tvAttendanceLogsCount = findViewById(R.id.tvAttendanceLogsCount);
        layoutEmptyAttendanceLogs = findViewById(R.id.layoutEmptyAttendanceLogs);
        btnEmptyLogAttendance = findViewById(R.id.btnEmptyLogAttendance);
        rvStudentAttendanceLogs = findViewById(R.id.rvStudentAttendanceLogs);
        fabLogAttendance = findViewById(R.id.fabLogAttendance);

        updateThresholdDisplay();
    }

    private void updateThresholdDisplay() {
        if (tvCurrentThresholdValue != null) {
            tvCurrentThresholdValue.setText(String.format(Locale.US, "%.0f%% Cutoff ✎", currentThreshold));
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerViews() {
        // Subject-wise Breakdown
        rvSubjectAttendance.setLayoutManager(new LinearLayoutManager(this));
        subjectAdapter = new StudentSubjectAttendanceAdapter(new ArrayList<>());
        rvSubjectAttendance.setAdapter(subjectAdapter);

        // Student Attendance Logs
        rvStudentAttendanceLogs.setLayoutManager(new LinearLayoutManager(this));
        logsAdapter = new StudentAttendanceLogAdapter(log -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Attendance Log?")
                    .setMessage(String.format(Locale.US, "Delete attendance log for %s (%.1f%%)?", log.getSubjectName(), log.getPercentage()))
                    .setPositiveButton("Delete", (d, w) -> {
                        dbHelper.deleteStudentAttendanceLog(log.getId());
                        Toast.makeText(this, "Log deleted", Toast.LENGTH_SHORT).show();
                        loadStudentAttendanceLogs();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        rvStudentAttendanceLogs.setAdapter(logsAdapter);
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

    private void setupClickListeners() {
        if (btnChangeThreshold != null) {
            btnChangeThreshold.setOnClickListener(v -> showThresholdSettingsDialog());
        }

        View.OnClickListener openLogDialog = v -> showLogAttendanceDialog();
        if (btnQuickLogAttendance != null) btnQuickLogAttendance.setOnClickListener(openLogDialog);
        if (btnEmptyLogAttendance != null) btnEmptyLogAttendance.setOnClickListener(openLogDialog);
        if (fabLogAttendance != null) fabLogAttendance.setOnClickListener(openLogDialog);
    }

    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    /**
     * Load student attendance logs from SQLite and update the list.
     */
    public void loadStudentAttendanceLogs() {
        int studentIdInt = 1;
        try { studentIdInt = Integer.parseInt(currentStudentId); } catch (Exception ignored) {}

        List<StudentAttendanceLog> logs = dbHelper.getStudentAttendanceLogs(studentIdInt);
        if (logsAdapter != null) {
            logsAdapter.updateLogs(logs);
        }

        if (tvAttendanceLogsCount != null) {
            tvAttendanceLogsCount.setText(logs.size() + " Logs");
        }

        if (logs.isEmpty()) {
            if (layoutEmptyAttendanceLogs != null) layoutEmptyAttendanceLogs.setVisibility(View.VISIBLE);
            if (rvStudentAttendanceLogs != null) rvStudentAttendanceLogs.setVisibility(View.GONE);
        } else {
            if (layoutEmptyAttendanceLogs != null) layoutEmptyAttendanceLogs.setVisibility(View.GONE);
            if (rvStudentAttendanceLogs != null) rvStudentAttendanceLogs.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Open dialog to log class attendance percentage.
     */
    private void showLogAttendanceDialog() {
        checkAndRequestNotificationPermission();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_log_class_attendance, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvDialogThresholdInfo = dialogView.findViewById(R.id.tvDialogThresholdInfo);
        AutoCompleteTextView actvLogSubject = dialogView.findViewById(R.id.actvLogSubject);
        TextInputEditText etTotalClasses = dialogView.findViewById(R.id.etTotalClasses);
        TextInputEditText etAttendedClasses = dialogView.findViewById(R.id.etAttendedClasses);
        TextInputEditText etLogDate = dialogView.findViewById(R.id.etLogDate);
        TextInputEditText etLogNotes = dialogView.findViewById(R.id.etLogNotes);
        TextView tvPreviewCalculatedPct = dialogView.findViewById(R.id.tvPreviewCalculatedPct);
        TextView tvPreviewThresholdStatus = dialogView.findViewById(R.id.tvPreviewThresholdStatus);
        TextView tvPreviewRecoveryAdvice = dialogView.findViewById(R.id.tvPreviewRecoveryAdvice);
        MaterialButton btnCancelLogAttendance = dialogView.findViewById(R.id.btnCancelLogAttendance);
        MaterialButton btnSaveLogAttendance = dialogView.findViewById(R.id.btnSaveLogAttendance);

        tvDialogThresholdInfo.setText(String.format(Locale.US, "Alert Cutoff Threshold: %.0f%%", currentThreshold));

        // Populate Subjects
        List<Subject> subjects = dbHelper.getAllSubjects();
        List<String> subjectNames = new ArrayList<>();
        for (Subject s : subjects) {
            subjectNames.add(s.getName());
        }
        if (subjectNames.isEmpty()) {
            subjectNames.add("Data Structures");
            subjectNames.add("Database Management");
            subjectNames.add("Operating Systems");
            subjectNames.add("Computer Networks");
            subjectNames.add("Software Engineering");
        }
        ArrayAdapter<String> subjectAdapterAuto = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, subjectNames);
        actvLogSubject.setAdapter(subjectAdapterAuto);
        if (!subjectNames.isEmpty()) {
            actvLogSubject.setText(subjectNames.get(0), false);
        }

        // Today's date
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        etLogDate.setText(today);
        etLogDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                String selectedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                etLogDate.setText(selectedDate);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Dynamic Calculation TextWatcher
        TextWatcher calcWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                updateCalcPreview(etTotalClasses, etAttendedClasses, tvPreviewCalculatedPct, tvPreviewThresholdStatus, tvPreviewRecoveryAdvice);
            }
        };
        etTotalClasses.addTextChangedListener(calcWatcher);
        etAttendedClasses.addTextChangedListener(calcWatcher);

        btnCancelLogAttendance.setOnClickListener(v -> dialog.dismiss());

        btnSaveLogAttendance.setOnClickListener(v -> {
            String subjectName = actvLogSubject.getText() != null ? actvLogSubject.getText().toString().trim() : "";
            if (subjectName.isEmpty()) {
                actvLogSubject.setError("Please select or enter a subject name");
                return;
            }

            String totalStr = etTotalClasses.getText() != null ? etTotalClasses.getText().toString().trim() : "";
            String attendedStr = etAttendedClasses.getText() != null ? etAttendedClasses.getText().toString().trim() : "";

            if (totalStr.isEmpty() || attendedStr.isEmpty()) {
                Toast.makeText(this, "Please enter total classes held and classes attended", Toast.LENGTH_SHORT).show();
                return;
            }

            int total, attended;
            try {
                total = Integer.parseInt(totalStr);
                attended = Integer.parseInt(attendedStr);
            } catch (Exception e) {
                Toast.makeText(this, "Invalid numbers", Toast.LENGTH_SHORT).show();
                return;
            }

            if (total <= 0) {
                etTotalClasses.setError("Total classes must be greater than 0");
                return;
            }
            if (attended < 0 || attended > total) {
                etAttendedClasses.setError("Attended classes cannot exceed total classes");
                return;
            }

            double percentage = ((double) attended / total) * 100.0;
            String date = etLogDate.getText() != null ? etLogDate.getText().toString().trim() : today;
            String notes = etLogNotes.getText() != null ? etLogNotes.getText().toString().trim() : "";

            int studentIdInt = 1;
            try { studentIdInt = Integer.parseInt(currentStudentId); } catch (Exception ignored) {}

            dbHelper.addStudentAttendanceLog(studentIdInt, subjectName, total, attended, percentage, date, notes, currentThreshold);
            dialog.dismiss();

            if (percentage < currentThreshold) {
                double target = currentThreshold / 100.0;
                int needed = (int) Math.ceil((target * total - attended) / (1.0 - target));
                AttendanceNotificationHelper.sendLowAttendanceNotification(this, subjectName, percentage, currentThreshold, needed);
                showLowAttendanceWarningDialog(subjectName, percentage, currentThreshold, needed);
            } else {
                Toast.makeText(this, String.format(Locale.US, "Attendance logged successfully! (%.1f%%)", percentage), Toast.LENGTH_SHORT).show();
            }

            loadStudentAttendanceLogs();
            loadStudentFirestoreAttendance();
        });

        dialog.show();
    }

    private void updateCalcPreview(TextInputEditText etTotal, TextInputEditText etAttended,
                                   TextView tvPct, TextView tvStatus, TextView tvAdvice) {
        String totalStr = etTotal.getText() != null ? etTotal.getText().toString().trim() : "";
        String attendedStr = etAttended.getText() != null ? etAttended.getText().toString().trim() : "";

        if (totalStr.isEmpty() || attendedStr.isEmpty()) {
            tvPct.setText("Attendance %: 0.0%");
            tvStatus.setText("Pending Input");
            tvStatus.setBackgroundResource(R.drawable.bg_pill_indigo);
            tvAdvice.setText("Enter total and attended classes to calculate attendance percentage.");
            return;
        }

        try {
            int total = Integer.parseInt(totalStr);
            int attended = Integer.parseInt(attendedStr);
            if (total <= 0 || attended < 0 || attended > total) {
                tvPct.setText("Invalid Numbers");
                return;
            }
            double pct = ((double) attended / total) * 100.0;
            tvPct.setText(String.format(Locale.US, "Attendance: %.1f%%", pct));

            if (pct < currentThreshold) {
                tvStatus.setText(String.format(Locale.US, "⚠ Below %.0f%% Cutoff", currentThreshold));
                tvStatus.setBackgroundResource(R.drawable.bg_pill_rose);
                double target = currentThreshold / 100.0;
                int needed = (int) Math.ceil((target * total - attended) / (1.0 - target));
                tvAdvice.setText(String.format(Locale.US, "⚠️ Shortage Warning: Attend next %d consecutive classes to reach %.0f%%.", needed, currentThreshold));
                tvAdvice.setTextColor(Color.parseColor("#EF4444"));
            } else {
                tvStatus.setText(String.format(Locale.US, "✓ Safe (>=%.0f%%)", currentThreshold));
                tvStatus.setBackgroundResource(R.drawable.bg_pill_emerald);
                double target = currentThreshold / 100.0;
                int canMiss = (int) Math.floor((attended / target) - total);
                if (canMiss > 0) {
                    tvAdvice.setText(String.format(Locale.US, "✓ Safe: You can afford to miss %d class(es) while staying above %.0f%%.", canMiss, currentThreshold));
                } else {
                    tvAdvice.setText(String.format(Locale.US, "✓ Safe: Currently meeting your %.0f%% requirement.", currentThreshold));
                }
                tvAdvice.setTextColor(Color.parseColor("#10B981"));
            }
        } catch (Exception ignored) {}
    }

    /**
     * Dialog to configure custom alert threshold.
     */
    private void showThresholdSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_attendance_threshold, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        ChipGroup chipGroup = dialogView.findViewById(R.id.chipGroupThresholdPresets);
        Chip chip70 = dialogView.findViewById(R.id.chipThreshold70);
        Chip chip75 = dialogView.findViewById(R.id.chipThreshold75);
        Chip chip80 = dialogView.findViewById(R.id.chipThreshold80);
        Chip chip85 = dialogView.findViewById(R.id.chipThreshold85);
        TextInputEditText etCustomThreshold = dialogView.findViewById(R.id.etCustomThreshold);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelThreshold);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSaveThreshold);

        etCustomThreshold.setText(String.format(Locale.US, "%.0f", currentThreshold));

        if (Math.abs(currentThreshold - 70.0f) < 0.1f) chip70.setChecked(true);
        else if (Math.abs(currentThreshold - 75.0f) < 0.1f) chip75.setChecked(true);
        else if (Math.abs(currentThreshold - 80.0f) < 0.1f) chip80.setChecked(true);
        else if (Math.abs(currentThreshold - 85.0f) < 0.1f) chip85.setChecked(true);

        chip70.setOnClickListener(v -> etCustomThreshold.setText("70"));
        chip75.setOnClickListener(v -> etCustomThreshold.setText("75"));
        chip80.setOnClickListener(v -> etCustomThreshold.setText("80"));
        chip85.setOnClickListener(v -> etCustomThreshold.setText("85"));

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String input = etCustomThreshold.getText() != null ? etCustomThreshold.getText().toString().trim() : "";
            if (input.isEmpty()) {
                etCustomThreshold.setError("Please enter a threshold percentage");
                return;
            }
            float val;
            try {
                val = Float.parseFloat(input);
            } catch (Exception e) {
                etCustomThreshold.setError("Invalid number");
                return;
            }
            if (val <= 0 || val > 100) {
                etCustomThreshold.setError("Must be between 1% and 100%");
                return;
            }

            currentThreshold = val;
            sessionManager.setAttendanceThreshold(val);
            updateThresholdDisplay();
            dialog.dismiss();

            Toast.makeText(this, String.format(Locale.US, "Alert threshold updated to %.0f%%", currentThreshold), Toast.LENGTH_SHORT).show();
            loadStudentFirestoreAttendance();
            loadStudentAttendanceLogs();
        });

        dialog.show();
    }

    private void showLowAttendanceWarningDialog(String subjectName, double percentage, double threshold, int classesNeeded) {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Low Attendance Alert")
                .setIcon(R.drawable.ic_info)
                .setMessage(String.format(Locale.US,
                        "Your logged attendance for %s is %.1f%%, which is below your %.0f%% alert threshold!\n\n" +
                        "Recovery Plan: You must attend the next %d consecutive classes to reach %.0f%%.\n\n" +
                        "A system notification and an in-app alert have been dispatched.",
                        subjectName, percentage, threshold, classesNeeded, threshold))
                .setPositiveButton("I Understand", (d, w) -> d.dismiss())
                .show();
    }

    /**
     * Fetch attendance records from Firebase Firestore.
     */
    private void loadStudentFirestoreAttendance() {
        repository.fetchStudentAttendanceHistory(currentStudentId, new AttendanceRepository.OnStudentAttendanceListener() {
            @Override
            public void onSuccess(List<Attendance> records) {
                if (records == null || records.isEmpty()) {
                    Log.d(TAG, "No Firestore records for student " + currentStudentId + ", using SQLite...");
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
        int maxConsecutiveNeeded = 0;
        String maxShortageSubject = "";

        int subIdCounter = 1;
        double target = currentThreshold / 100.0;

        for (Map.Entry<String, int[]> entry : subjectStatsMap.entrySet()) {
            String name = entry.getKey();
            int subTotal = entry.getValue()[0];
            int subPresent = entry.getValue()[1];
            double subPct = subTotal > 0 ? ((double) subPresent / subTotal) * 100.0 : 0.0;

            subjectStatsList.add(new SubjectAttendanceStats(subIdCounter++, name, subTotal, subPresent, subPct));

            if (subPct < currentThreshold) {
                lowAttSubjects.add(name + " (" + String.format(Locale.US, "%.1f%%", subPct) + ")");
                int needed = (int) Math.ceil((target * subTotal - subPresent) / (1.0 - target));
                if (needed > maxConsecutiveNeeded) {
                    maxConsecutiveNeeded = needed;
                    maxShortageSubject = name;
                }
            }
        }

        subjectAdapter.updateList(subjectStatsList);

        evaluateThresholdStatus(overallPct, lowAttSubjects, totalHours, presentHours, maxShortageSubject, maxConsecutiveNeeded);

        // Monthly trends
        int sId = 1;
        try { sId = Integer.parseInt(currentStudentId); } catch (Exception ignored) {}
        List<MonthlyAttendanceStats> monthlyList = dbHelper.getMonthlyAttendanceChart(sId);
        renderMonthlyBarChart(monthlyList);
        renderMonthlyLineChart(monthlyList);
    }

    private void loadSQLiteFallbackData() {
        int studentIdInt = 1;
        try { studentIdInt = Integer.parseInt(currentStudentId); } catch (Exception ignored) {}

        List<SubjectAttendanceStats> subjectStats = dbHelper.getSubjectWiseAttendance(studentIdInt);
        subjectAdapter.updateList(subjectStats);

        int totalClassesHeld = 0;
        int totalClassesAttended = 0;
        List<String> lowAttSubjects = new ArrayList<>();
        int maxConsecutiveNeeded = 0;
        String maxShortageSubject = "";
        double target = currentThreshold / 100.0;

        for (SubjectAttendanceStats sub : subjectStats) {
            totalClassesHeld += sub.getTotalClasses();
            totalClassesAttended += sub.getAttendedClasses();
            if (sub.getPercentage() < currentThreshold) {
                lowAttSubjects.add(sub.getSubjectName() + " (" + String.format(Locale.US, "%.1f%%", sub.getPercentage()) + ")");
                int needed = (int) Math.ceil((target * sub.getTotalClasses() - sub.getAttendedClasses()) / (1.0 - target));
                if (needed > maxConsecutiveNeeded) {
                    maxConsecutiveNeeded = needed;
                    maxShortageSubject = sub.getSubjectName();
                }
            }
        }

        double overallPct = totalClassesHeld > 0 ? ((double) totalClassesAttended / totalClassesHeld) * 100.0 : 0.0;
        int absentCount = totalClassesHeld - totalClassesAttended;

        tvOverallPercentage.setText(String.format(Locale.US, "%.2f%%", overallPct));
        tvClassesAttended.setText("Attended: " + totalClassesAttended + " / " + totalClassesHeld + " Hours");
        tvClassesAbsent.setText("Absent: " + absentCount + " Hours");

        evaluateThresholdStatus(overallPct, lowAttSubjects, totalClassesHeld, totalClassesAttended, maxShortageSubject, maxConsecutiveNeeded);

        List<MonthlyAttendanceStats> monthlyStats = dbHelper.getMonthlyAttendanceChart(studentIdInt);
        renderMonthlyBarChart(monthlyStats);
        renderMonthlyLineChart(monthlyStats);
    }

    private void evaluateThresholdStatus(double overallPct, List<String> lowAttSubjects, int totalHours, int presentHours,
                                         String maxShortageSubject, int maxConsecutiveNeeded) {
        double target = currentThreshold / 100.0;
        boolean hasShortage = overallPct < currentThreshold || !lowAttSubjects.isEmpty();

        if (!hasShortage) {
            if (overallPct >= (currentThreshold + 10.0)) {
                tvExamEligibilityBadge.setText(String.format(Locale.US, "✓ Outstanding (>=%.0f%%)", currentThreshold));
                tvExamEligibilityBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#10B981")));
                tvRequiredStatus.setText(String.format(Locale.US, "Outstanding attendance! All subjects comfortably meet your %.0f%% alert threshold.", currentThreshold));
            } else {
                tvExamEligibilityBadge.setText(String.format(Locale.US, "✓ Meets Cutoff (>=%.0f%%)", currentThreshold));
                tvExamEligibilityBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#10B981")));
                tvRequiredStatus.setText(String.format(Locale.US, "All subjects meet your %.0f%% alert threshold.", currentThreshold));
            }

            if (cardAttendanceAlertBanner != null) {
                cardAttendanceAlertBanner.setVisibility(View.GONE);
            }
        } else {
            tvExamEligibilityBadge.setText(String.format(Locale.US, "⚠ LOW ATTENDANCE (<%.0f%%)", currentThreshold));
            tvExamEligibilityBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#EF4444")));

            if (!lowAttSubjects.isEmpty()) {
                tvRequiredStatus.setText(String.format(Locale.US, "⚠️ Attendance is below your %.0f%% cutoff in %d subject(s): %s.",
                        currentThreshold, lowAttSubjects.size(), TextUtils.join(", ", lowAttSubjects)));
            } else {
                tvRequiredStatus.setText(String.format(Locale.US, "⚠️ Overall attendance (%.2f%%) is below your %.0f%% cutoff threshold.",
                        overallPct, currentThreshold));
            }

            // Show active Alert Banner with recovery calculation
            if (cardAttendanceAlertBanner != null) {
                cardAttendanceAlertBanner.setVisibility(View.VISIBLE);
                tvAlertBannerTitle.setText(String.format(Locale.US, "⚠️ Attendance Shortage (< %.0f%%)", currentThreshold));

                if (!lowAttSubjects.isEmpty()) {
                    tvAlertBannerMessage.setText(String.format(Locale.US,
                            "Your attendance is below the %.0f%% threshold in: %s. Debarment risk if not recovered.",
                            currentThreshold, TextUtils.join(", ", lowAttSubjects)));
                } else {
                    tvAlertBannerMessage.setText(String.format(Locale.US,
                            "Your overall attendance (%.1f%%) has fallen below your %.0f%% threshold cutoff.",
                            overallPct, currentThreshold));
                }

                int overallNeeded = (totalHours > 0 && overallPct < currentThreshold) ?
                        (int) Math.ceil((target * totalHours - presentHours) / (1.0 - target)) : 0;
                int bestNeeded = Math.max(maxConsecutiveNeeded, overallNeeded);

                if (bestNeeded > 0) {
                    tvAlertRecoveryPlan.setText(String.format(Locale.US,
                            "Recovery Plan: Attend the next %d consecutive classes%s to restore eligibility above %.0f%%.",
                            bestNeeded, (maxShortageSubject.isEmpty() ? "" : " in " + maxShortageSubject), currentThreshold));
                } else {
                    tvAlertRecoveryPlan.setText(String.format(Locale.US,
                            "Recovery Plan: Attend all upcoming classes regularly to maintain your %.0f%% cutoff.", currentThreshold));
                }
            }
        }
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
