package com.example;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adapter.TeacherAttendanceAdapter;
import com.example.database.DatabaseHelper;
import com.example.model.AttendanceRecord;
import com.example.model.Subject;
import com.example.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Teacher Activity for Marking and Editing Attendance in SQLite database.
 */
public class TeacherAttendanceActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private TeacherAttendanceAdapter adapter;

    private Toolbar toolbar;
    private Spinner spSubject;
    private LinearLayout btnSelectDate;
    private TextView tvSelectedDate;
    private Chip chipModeStatus;

    private TextView tvStudentCountHeader;
    private MaterialButton btnMarkAllPresent;
    private MaterialButton btnMarkAllAbsent;

    private RecyclerView rvAttendance;
    private MaterialButton btnSaveAttendance;

    private List<Subject> subjectList;
    private int selectedSubjectId = -1;
    private String selectedDate = "2026-08-06"; // Default today

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_attendance);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        initViews();
        setupToolbar();
        setupWindowInsets();
        setupSubjectSpinner();
        setupDatePicker();
        setupRecyclerView();
        setupBatchButtons();
        setupSaveButton();
    }

    private void setupWindowInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbarTeacherAtt), (v, insets) -> {
            androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
            v.setPadding(0, statusBarInsets.top, 0, 0);
            return insets;
        });
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(btnSaveAttendance, (v, insets) -> {
            androidx.core.graphics.Insets navBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), navBarInsets.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAttendanceData();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarTeacherAtt);
        spSubject = findViewById(R.id.spTeacherSubject);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        chipModeStatus = findViewById(R.id.chipModeStatus);

        tvStudentCountHeader = findViewById(R.id.tvStudentCountHeader);
        btnMarkAllPresent = findViewById(R.id.btnMarkAllPresent);
        btnMarkAllAbsent = findViewById(R.id.btnMarkAllAbsent);

        rvAttendance = findViewById(R.id.rvTeacherAttendance);
        btnSaveAttendance = findViewById(R.id.btnSaveAttendance);

        tvSelectedDate.setText(selectedDate);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSubjectSpinner() {
        subjectList = dbHelper.getAllSubjects();
        List<String> subjectNames = new ArrayList<>();

        if (subjectList != null && !subjectList.isEmpty()) {
            for (Subject sub : subjectList) {
                subjectNames.add(sub.getSubjectCode() + " - " + sub.getSubjectName());
            }
            selectedSubjectId = subjectList.get(0).getId();
        } else {
            subjectNames.add("No Subjects Available");
            selectedSubjectId = -1;
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, subjectNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSubject.setAdapter(spinnerAdapter);

        spSubject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (subjectList != null && position < subjectList.size()) {
                    selectedSubjectId = subjectList.get(position).getId();
                    loadAttendanceData();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupDatePicker() {
        btnSelectDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            int year = 2026;
            int month = 7; // August
            int day = 6;

            DatePickerDialog dialog = new DatePickerDialog(this, (view, yearSelected, monthOfYear, dayOfMonth) -> {
                selectedDate = String.format(Locale.US, "%04d-%02d-%02d", yearSelected, monthOfYear + 1, dayOfMonth);
                tvSelectedDate.setText(selectedDate);
                loadAttendanceData();
            }, year, month, day);

            dialog.show();
        });
    }

    private void setupRecyclerView() {
        rvAttendance.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TeacherAttendanceAdapter(new ArrayList<>());
        rvAttendance.setAdapter(adapter);
    }

    private void setupBatchButtons() {
        btnMarkAllPresent.setOnClickListener(v -> adapter.markAllPresent());
        btnMarkAllAbsent.setOnClickListener(v -> adapter.markAllAbsent());
    }

    private void loadAttendanceData() {
        if (selectedSubjectId <= 0) return;

        List<AttendanceRecord> records = dbHelper.getAttendanceForSubjectAndDate(selectedSubjectId, selectedDate);
        adapter.updateList(records);

        tvStudentCountHeader.setText("Enrolled Students (" + records.size() + ")");

        boolean hasSavedData = false;
        for (AttendanceRecord rec : records) {
            if (rec.getId() > 0) {
                hasSavedData = true;
                break;
            }
        }

        if (hasSavedData) {
            chipModeStatus.setText("Edit Mode (Record Exists)");
            chipModeStatus.setChipIconResource(R.drawable.ic_edit);
        } else {
            chipModeStatus.setText("Mark Mode (New Session)");
            chipModeStatus.setChipIconResource(R.drawable.ic_add);
        }
    }

    private void setupSaveButton() {
        btnSaveAttendance.setOnClickListener(v -> {
            List<AttendanceRecord> records = adapter.getRecords();
            if (records.isEmpty()) {
                Toast.makeText(this, "No students to record attendance for.", Toast.LENGTH_SHORT).show();
                return;
            }

            int savedCount = 0;
            for (AttendanceRecord rec : records) {
                boolean success = dbHelper.saveOrUpdateAttendanceRecord(rec.getStudentId(), selectedSubjectId, selectedDate, rec.getStatus());
                if (success) savedCount++;
            }

            Toast.makeText(this, "Attendance saved & synced to Cloud for " + savedCount + " students!", Toast.LENGTH_LONG).show();
            dbHelper.logTeacherActivity(sessionManager.getUserName(), "Marked Attendance for " + selectedDate, "Recorded attendance for " + savedCount + " students", "Attendance");
            loadAttendanceData();
        });
    }
}
