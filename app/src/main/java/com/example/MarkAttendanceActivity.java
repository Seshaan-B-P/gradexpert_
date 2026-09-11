package com.example;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adapter.MarkAttendanceAdapter;
import com.example.database.DatabaseHelper;
import com.example.model.Attendance;
import com.example.model.Student;
import com.example.model.Subject;
import com.example.repository.AttendanceRepository;
import com.example.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Modern Activity for Hour-Based Attendance Marking and Editing in Firebase Firestore.
 */
public class MarkAttendanceActivity extends AppCompatActivity implements MarkAttendanceAdapter.OnStatusChangeListener {

    private static final String TAG = "MarkAttendanceActivity";

    private AttendanceRepository repository;
    private SessionManager sessionManager;
    private DatabaseHelper dbHelper;

    private Toolbar toolbar;
    private Spinner spDepartment;
    private Spinner spSemester;
    private Spinner spSubject;
    private Spinner spHour;

    private LinearLayout btnSelectDate;
    private TextView tvSelectedDate;
    private Chip chipModeStatus;

    private TextView tvTotalStudents;
    private TextView tvPresentCount;
    private TextView tvAbsentCount;
    private TextView tvAttendancePercentage;

    private TextInputEditText etSearchStudent;
    private TextView tvStudentCountHeader;
    private MaterialButton btnMarkAllPresent;
    private MaterialButton btnMarkAllAbsent;

    private ProgressBar progressBarLoading;
    private LinearLayout layoutEmptyState;
    private TextView tvEmptyStateMessage;

    private RecyclerView rvAttendance;
    private MarkAttendanceAdapter adapter;
    private MaterialButton btnSaveAttendance;

    private List<Student> loadedStudents = new ArrayList<>();
    private List<Subject> loadedSubjects = new ArrayList<>();

    private String selectedDepartment = "Master of Computer Applications";
    private String selectedSemester = "Semester III";
    private Subject selectedSubject = null;
    private String selectedDate = ""; // YYYY-MM-DD
    private int selectedHour = 1;

    private boolean isUpdateMode = false;
    private Calendar calendarToday = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mark_attendance);

        repository = AttendanceRepository.getInstance(this);
        sessionManager = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        selectedDate = sdf.format(new Date());

        initViews();
        setupToolbar();
        setupWindowInsets();
        setupDepartmentSpinner();
        setupSemesterSpinner();
        setupHourSpinner();
        setupDatePicker();
        setupSearchFilter();
        setupRecyclerView();
        setupBatchButtons();
        setupSaveButton();

        // Initial load
        loadSubjectsAndStudents();
    }

    private void setupWindowInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbarMarkAtt), (v, insets) -> {
            androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
            v.setPadding(0, statusBarInsets.top, 0, 0);
            return insets;
        });
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarMarkAtt);
        spDepartment = findViewById(R.id.spDepartment);
        spSemester = findViewById(R.id.spSemester);
        spSubject = findViewById(R.id.spSubject);
        spHour = findViewById(R.id.spHour);

        btnSelectDate = findViewById(R.id.btnSelectDate);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        chipModeStatus = findViewById(R.id.chipModeStatus);

        tvTotalStudents = findViewById(R.id.tvTotalStudents);
        tvPresentCount = findViewById(R.id.tvPresentCount);
        tvAbsentCount = findViewById(R.id.tvAbsentCount);
        tvAttendancePercentage = findViewById(R.id.tvAttendancePercentage);

        etSearchStudent = findViewById(R.id.etSearchStudent);
        tvStudentCountHeader = findViewById(R.id.tvStudentCountHeader);
        btnMarkAllPresent = findViewById(R.id.btnMarkAllPresent);
        btnMarkAllAbsent = findViewById(R.id.btnMarkAllAbsent);

        progressBarLoading = findViewById(R.id.progressBarLoading);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        tvEmptyStateMessage = findViewById(R.id.tvEmptyStateMessage);

        rvAttendance = findViewById(R.id.rvAttendance);
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

    private void setupDepartmentSpinner() {
        List<String> departments = new ArrayList<>();
        departments.add("Master of Computer Applications");
        departments.add("Computer Science & Engineering");
        departments.add("Information Technology");
        departments.add("Electronics & Communication");

        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, departments);
        deptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDepartment.setAdapter(deptAdapter);

        spDepartment.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDepartment = departments.get(position);
                loadSubjectsAndStudents();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSemesterSpinner() {
        List<String> semesters = new ArrayList<>();
        semesters.add("Semester III");
        semesters.add("Semester I");
        semesters.add("Semester II");
        semesters.add("Semester IV");
        semesters.add("Semester V");
        semesters.add("Semester VI");

        ArrayAdapter<String> semAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, semesters);
        semAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSemester.setAdapter(semAdapter);

        spSemester.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSemester = semesters.get(position);
                loadSubjectsAndStudents();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupHourSpinner() {
        List<String> hours = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            hours.add("Hour " + i);
        }

        ArrayAdapter<String> hourAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, hours);
        hourAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spHour.setAdapter(hourAdapter);

        spHour.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedHour = position + 1;
                loadHourAttendanceFromFirestore();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupDatePicker() {
        btnSelectDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                Date parsedDate = sdf.parse(selectedDate);
                if (parsedDate != null) cal.setTime(parsedDate);
            } catch (Exception ignored) {}

            DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar chosenCal = Calendar.getInstance();
                chosenCal.set(year, month, dayOfMonth, 23, 59, 59);

                Calendar todayMax = Calendar.getInstance();
                todayMax.set(Calendar.HOUR_OF_DAY, 23);
                todayMax.set(Calendar.MINUTE, 59);
                todayMax.set(Calendar.SECOND, 59);

                if (chosenCal.after(todayMax)) {
                    Toast.makeText(MarkAttendanceActivity.this, "Attendance cannot be marked for a future date.", Toast.LENGTH_LONG).show();
                    return;
                }

                selectedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                tvSelectedDate.setText(selectedDate);
                loadHourAttendanceFromFirestore();
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

            // Prevent future dates in DatePicker UI
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            datePickerDialog.show();
        });
    }

    private void setupSearchFilter() {
        etSearchStudent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) {
                    adapter.filter(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRecyclerView() {
        rvAttendance.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MarkAttendanceAdapter(new ArrayList<>(), this);
        rvAttendance.setAdapter(adapter);
    }

    private void setupBatchButtons() {
        btnMarkAllPresent.setOnClickListener(v -> adapter.markAllPresent());
        btnMarkAllAbsent.setOnClickListener(v -> adapter.markAllAbsent());
    }

    private List<String> teacherAssignedSubjectIds = new ArrayList<>();
    private boolean isTeacherUser = false;
    private boolean teacherPermissionsLoaded = false;

    /**
     * Load subjects and students from Firestore according to selected department and semester.
     */
    private void loadSubjectsAndStudents() {
        if (!teacherPermissionsLoaded) {
            isTeacherUser = "TEACHER".equalsIgnoreCase(sessionManager.getUserRole());
            if (isTeacherUser) {
                String uid = "";
                FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
                if (fUser != null) uid = fUser.getUid();
                else uid = sessionManager.getIdentifier();

                if (!uid.isEmpty()) {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("teachers")
                            .document(uid)
                            .get()
                            .addOnSuccessListener(doc -> {
                                teacherPermissionsLoaded = true;
                                if (doc != null && doc.exists()) {
                                    List<String> ids = (List<String>) doc.get("assignedSubjectIds");
                                    if (ids != null) teacherAssignedSubjectIds = ids;
                                }
                                fetchSubjectsAndStudentsInternal();
                            })
                            .addOnFailureListener(e -> {
                                teacherPermissionsLoaded = true;
                                fetchSubjectsAndStudentsInternal();
                            });
                    return;
                }
            }
            teacherPermissionsLoaded = true;
        }

        fetchSubjectsAndStudentsInternal();
    }

    private void fetchSubjectsAndStudentsInternal() {
        showLoading(true);

        repository.fetchSubjects(selectedDepartment, selectedSemester, new AttendanceRepository.OnSubjectsLoadedListener() {
            @Override
            public void onSuccess(List<Subject> subjects) {
                List<Subject> filtered = new ArrayList<>();
                if (subjects != null) {
                    for (Subject s : subjects) {
                        if ("INACTIVE".equalsIgnoreCase(s.getStatus())) continue;
                        if (isTeacherUser && !teacherAssignedSubjectIds.isEmpty()) {
                            if (teacherAssignedSubjectIds.contains(s.getSubjectId()) || teacherAssignedSubjectIds.contains(s.getSubjectCode())) {
                                filtered.add(s);
                            }
                        } else {
                            filtered.add(s);
                        }
                    }
                }

                loadedSubjects = filtered;
                setupSubjectSpinner(filtered);

                // Fetch students
                repository.fetchStudents(selectedDepartment, selectedSemester, new AttendanceRepository.OnStudentsLoadedListener() {
                    @Override
                    public void onSuccess(List<Student> students) {
                        loadedStudents = students;
                        loadHourAttendanceFromFirestore();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        showLoading(false);
                        showEmptyState("Error loading students: " + errorMessage);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                showEmptyState("No subjects found.");
            }
        });
    }

    private void setupSubjectSpinner(List<Subject> subjects) {
        List<String> subjectDisplayNames = new ArrayList<>();
        if (subjects != null && !subjects.isEmpty()) {
            for (Subject s : subjects) {
                subjectDisplayNames.add(s.getSubjectCode() + " - " + s.getSubjectName());
            }
            selectedSubject = subjects.get(0);
        } else {
            subjectDisplayNames.add("No Subjects Available");
            selectedSubject = null;
        }

        ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, subjectDisplayNames);
        subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSubject.setAdapter(subjectAdapter);

        spSubject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (loadedSubjects != null && position < loadedSubjects.size()) {
                    selectedSubject = loadedSubjects.get(position);
                    loadHourAttendanceFromFirestore();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /**
     * Queries Firestore for attendance records matching (subjectId, date, hour).
     */
    private void loadHourAttendanceFromFirestore() {
        if (selectedSubject == null || loadedStudents == null) {
            showLoading(false);
            return;
        }

        if (loadedStudents.isEmpty()) {
            showLoading(false);
            showEmptyState("No students found for " + selectedDepartment + " (" + selectedSemester + ").");
            adapter.updateList(new ArrayList<>());
            updateSummaryStats();
            return;
        }

        showLoading(true);
        String subjectIdStr = String.valueOf(selectedSubject.getId());

        repository.fetchHourAttendance(subjectIdStr, selectedDate, selectedHour, new AttendanceRepository.OnAttendanceLoadedListener() {
            @Override
            public void onSuccess(Map<String, Attendance> existingMap) {
                showLoading(false);
                hideEmptyState();

                isUpdateMode = (existingMap != null && !existingMap.isEmpty());

                List<Attendance> displayList = new ArrayList<>();
                for (Student student : loadedStudents) {
                    String stIdStr = String.valueOf(student.getId());
                    if (existingMap != null && existingMap.containsKey(stIdStr)) {
                        displayList.add(existingMap.get(stIdStr));
                    } else {
                        String docId = Attendance.generateDocumentId(stIdStr, subjectIdStr, selectedDate, selectedHour);
                        Attendance newRecord = new Attendance(
                                docId,
                                stIdStr,
                                student.getName(),
                                student.getRegNo(),
                                subjectIdStr,
                                selectedSubject.getSubjectName(),
                                selectedDepartment,
                                selectedSemester,
                                selectedDate,
                                selectedHour,
                                "UNMARKED",
                                getTeacherIdentity()
                        );
                        displayList.add(newRecord);
                    }
                }

                adapter.updateList(displayList);
                tvStudentCountHeader.setText("Enrolled Students (" + displayList.size() + ")");

                if (isUpdateMode) {
                    chipModeStatus.setText("Edit Mode (Record Exists)");
                    chipModeStatus.setChipIconResource(R.drawable.ic_edit);
                    btnSaveAttendance.setText("UPDATE ATTENDANCE");
                } else {
                    chipModeStatus.setText("Mark Mode (New Session)");
                    chipModeStatus.setChipIconResource(R.drawable.ic_add);
                    btnSaveAttendance.setText("SAVE ATTENDANCE");
                }

                updateSummaryStats();
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                Toast.makeText(MarkAttendanceActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onStatusChanged() {
        updateSummaryStats();
    }

    /**
     * Calculate and display real-time total, present, absent, and attendance % stats.
     */
    private void updateSummaryStats() {
        int total = adapter != null ? adapter.getAllRecords().size() : 0;
        int present = adapter != null ? adapter.getPresentCount() : 0;
        int absent = adapter != null ? adapter.getAbsentCount() : 0;

        double percentage = total > 0 ? ((double) present / total) * 100.0 : 0.0;

        tvTotalStudents.setText(String.valueOf(total));
        tvPresentCount.setText(String.valueOf(present));
        tvAbsentCount.setText(String.valueOf(absent));
        tvAttendancePercentage.setText(String.format(Locale.US, "%.2f%%", percentage));
    }

    private void setupSaveButton() {
        btnSaveAttendance.setOnClickListener(v -> validateAndConfirmSave());
    }

    private void validateAndConfirmSave() {
        if (selectedSubject == null) {
            Toast.makeText(this, "Please select a subject.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Attendance> records = adapter.getAllRecords();
        if (records == null || records.isEmpty()) {
            Toast.makeText(this, "No students available to mark attendance.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (adapter.hasUnmarkedStudents()) {
            Toast.makeText(this, "Please mark attendance for all students.", Toast.LENGTH_LONG).show();
            return;
        }

        int total = records.size();
        int present = adapter.getPresentCount();
        int absent = adapter.getAbsentCount();

        String dialogTitle = isUpdateMode ? "Update Attendance?" : "Save Attendance?";
        String message = String.format(Locale.US,
                "Subject:\n%s\n\nDate:\n%s\n\nHour:\n%d\n\nPresent: %d\nAbsent: %d\nTotal Students: %d\nAttendance: %.2f%%",
                selectedSubject.getSubjectName(),
                selectedDate,
                selectedHour,
                present,
                absent,
                total,
                (present * 100.0 / total)
        );

        new AlertDialog.Builder(this)
                .setTitle(dialogTitle)
                .setMessage(message)
                .setPositiveButton(isUpdateMode ? "UPDATE" : "SAVE", (dialog, which) -> executeSaveBatch(records))
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void executeSaveBatch(List<Attendance> records) {
        btnSaveAttendance.setEnabled(false);
        btnSaveAttendance.setText("Saving attendance...");
        showLoading(true);

        String teacherId = getTeacherIdentity();
        for (Attendance rec : records) {
            rec.setMarkedBy(teacherId);
            rec.setDepartment(selectedDepartment);
            rec.setSemester(selectedSemester);
            rec.setSubjectId(String.valueOf(selectedSubject.getId()));
            rec.setSubjectName(selectedSubject.getSubjectName());
            rec.setDate(selectedDate);
            rec.setHour(selectedHour);
        }

        repository.saveAttendanceBatch(records, isUpdateMode, new AttendanceRepository.OnSaveCompleteListener() {
            @Override
            public void onSuccess(int totalSaved, boolean wasUpdate) {
                showLoading(false);
                btnSaveAttendance.setEnabled(true);

                String msg = wasUpdate ? "Attendance updated successfully." : "Attendance saved successfully.";
                Toast.makeText(MarkAttendanceActivity.this, msg, Toast.LENGTH_LONG).show();

                // Log teacher activity locally
                dbHelper.logTeacherActivity(
                        sessionManager.getUserName(),
                        (wasUpdate ? "Updated" : "Marked") + " Hour " + selectedHour + " Attendance",
                        "Recorded attendance for " + totalSaved + " students in " + selectedSubject.getSubjectName(),
                        "Attendance"
                );

                loadHourAttendanceFromFirestore();
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                btnSaveAttendance.setEnabled(true);
                btnSaveAttendance.setText(isUpdateMode ? "UPDATE ATTENDANCE" : "SAVE ATTENDANCE");
                Toast.makeText(MarkAttendanceActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private String getTeacherIdentity() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            if (currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
                return currentUser.getEmail();
            }
            return currentUser.getUid();
        }
        String sessionUser = sessionManager.getUserEmail();
        return (sessionUser != null && !sessionUser.isEmpty()) ? sessionUser : "Teacher";
    }

    private void showLoading(boolean show) {
        progressBarLoading.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showEmptyState(String message) {
        tvEmptyStateMessage.setText(message);
        layoutEmptyState.setVisibility(View.VISIBLE);
        rvAttendance.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        layoutEmptyState.setVisibility(View.GONE);
        rvAttendance.setVisibility(View.VISIBLE);
    }
}
