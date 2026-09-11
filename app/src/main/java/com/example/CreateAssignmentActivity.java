package com.example;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.model.Department;
import com.example.model.Semester;
import com.example.model.Assignment;
import com.example.model.Subject;
import com.example.repository.AttendanceRepository;
import com.example.repository.AssignmentRepository;
import com.example.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity for creating and publishing course assignments to Firebase Firestore.
 */
public class CreateAssignmentActivity extends AppCompatActivity {

    private static final String TAG = "CreateAssignmentActivity";

    private FirebaseFirestore db;
    private AssignmentRepository assignmentRepository;
    private AttendanceRepository attendanceRepository;
    private SessionManager sessionManager;

    private Toolbar toolbar;
    private TextInputEditText etAssignTitle;
    private TextInputEditText etAssignDesc;

    private Spinner spAssignDept;
    private Spinner spAssignSem;
    private Spinner spAssignSubject;
    private Spinner spAssignType;

    private LinearLayout btnSelectDueDate;
    private TextView tvDueDate;
    private LinearLayout btnSelectDueTime;
    private TextView tvDueTime;
    private TextInputEditText etMaxMarks;

    private MaterialButton btnAddAttachment;
    private Chip chipAttachment;
    private ProgressBar progressBar;

    private MaterialButton btnSaveDraft;
    private MaterialButton btnCreatePublish;

    private List<Subject> loadedSubjects = new ArrayList<>();
    private Subject selectedSubject = null;

    private String selectedDept = "Master of Computer Applications";
    private String selectedSem = "Semester III";
    private String selectedType = "Assignment";

    private Calendar dueCalendar = Calendar.getInstance();
    private boolean isDateSelected = false;
    private boolean isTimeSelected = false;

    private String attachedFileName = "";
    private String attachedFileUrl = "";

    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    attachedFileName = getFileNameFromUri(uri);
                    attachedFileUrl = uri.toString();
                    chipAttachment.setText(attachedFileName);
                    chipAttachment.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Attachment selected: " + attachedFileName, Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_assignment);

        db = FirebaseFirestore.getInstance();
        assignmentRepository = AssignmentRepository.getInstance(this);
        attendanceRepository = AttendanceRepository.getInstance(this);
        sessionManager = new SessionManager(this);

        initViews();
        setupToolbar();
        setupWindowInsets();
        setupDepartmentSpinner();
        setupSemesterSpinner();
        setupAssignmentTypeSpinner();
        setupDatePicker();
        setupTimePicker();
        setupAttachmentPicker();
        setupActionButtons();

        // Default set due date to 5 days from today at 11:59 PM
        dueCalendar.add(Calendar.DAY_OF_MONTH, 5);
        dueCalendar.set(Calendar.HOUR_OF_DAY, 23);
        dueCalendar.set(Calendar.MINUTE, 59);

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.US);
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.US);
        tvDueDate.setText(dateFormat.format(dueCalendar.getTime()));
        tvDueTime.setText(timeFormat.format(dueCalendar.getTime()));

        loadSubjects();
    }

    private void setupWindowInsets() {
        View appBar = findViewById(R.id.appBarCreateAssign);
        if (appBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, insets) -> {
                androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                v.setPadding(0, statusBarInsets.top, 0, 0);
                return insets;
            });
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarCreateAssign);
        etAssignTitle = findViewById(R.id.etAssignTitle);
        etAssignDesc = findViewById(R.id.etAssignDesc);

        spAssignDept = findViewById(R.id.spAssignDept);
        spAssignSem = findViewById(R.id.spAssignSem);
        spAssignSubject = findViewById(R.id.spAssignSubject);
        spAssignType = findViewById(R.id.spAssignType);

        btnSelectDueDate = findViewById(R.id.btnSelectDueDate);
        tvDueDate = findViewById(R.id.tvDueDate);
        btnSelectDueTime = findViewById(R.id.btnSelectDueTime);
        tvDueTime = findViewById(R.id.tvDueTime);
        etMaxMarks = findViewById(R.id.etMaxMarks);

        btnAddAttachment = findViewById(R.id.btnAddAttachment);
        chipAttachment = findViewById(R.id.chipAttachment);
        progressBar = findViewById(R.id.progressBarCreateAssign);

        btnSaveDraft = findViewById(R.id.btnSaveDraft);
        btnCreatePublish = findViewById(R.id.btnCreatePublish);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Create Assignment");
            getSupportActionBar().setSubtitle("Create & Publish Course Assignments");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupDepartmentSpinner() {
        List<String> depts = new ArrayList<>();
        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, depts);
        deptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spAssignDept.setAdapter(deptAdapter);

        db.collection("departments").get().addOnSuccessListener(querySnapshot -> {
            depts.clear();
            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    Department dept = doc.toObject(Department.class);
                    if (dept != null && "ACTIVE".equalsIgnoreCase(dept.getStatus())) {
                        String name = dept.getDepartmentName();
                        if (!name.isEmpty() && !depts.contains(name)) {
                            depts.add(name);
                        }
                    }
                }
            }
            if (depts.isEmpty()) {
                depts.add("Master of Computer Applications");
                depts.add("Computer Science");
                depts.add("Information Technology");
                depts.add("Electronics & Communication");
            }
            deptAdapter.notifyDataSetChanged();
            if (!depts.isEmpty()) {
                selectedDept = depts.get(0);
                loadSubjects();
            }
        }).addOnFailureListener(e -> {
            if (depts.isEmpty()) {
                depts.add("Master of Computer Applications");
                depts.add("Computer Science");
                depts.add("Information Technology");
                depts.add("Electronics & Communication");
                deptAdapter.notifyDataSetChanged();
                selectedDept = depts.get(0);
            }
        });

        spAssignDept.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < depts.size()) {
                    selectedDept = depts.get(position);
                    loadSubjects();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSemesterSpinner() {
        List<String> sems = new ArrayList<>();
        ArrayAdapter<String> semAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sems);
        semAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spAssignSem.setAdapter(semAdapter);

        db.collection("semesters").get().addOnSuccessListener(querySnapshot -> {
            sems.clear();
            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    Semester sem = doc.toObject(Semester.class);
                    if (sem != null && "ACTIVE".equalsIgnoreCase(sem.getStatus())) {
                        String title = sem.getSemesterTitle();
                        if (!title.isEmpty() && !sems.contains(title)) {
                            sems.add(title);
                        }
                    }
                }
            }
            if (sems.isEmpty()) {
                sems.add("Semester I");
                sems.add("Semester II");
                sems.add("Semester III");
                sems.add("Semester IV");
                sems.add("Semester V");
                sems.add("Semester VI");
            }
            semAdapter.notifyDataSetChanged();
            if (!sems.isEmpty()) {
                selectedSem = sems.get(0);
                loadSubjects();
            }
        }).addOnFailureListener(e -> {
            if (sems.isEmpty()) {
                sems.add("Semester I");
                sems.add("Semester II");
                sems.add("Semester III");
                sems.add("Semester IV");
                sems.add("Semester V");
                sems.add("Semester VI");
                semAdapter.notifyDataSetChanged();
                selectedSem = sems.get(0);
            }
        });

        spAssignSem.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < sems.size()) {
                    selectedSem = sems.get(position);
                    loadSubjects();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupAssignmentTypeSpinner() {
        List<String> types = new ArrayList<>();
        types.add("Assignment");
        types.add("Homework");
        types.add("Lab Work");
        types.add("Project");
        types.add("Quiz");
        types.add("Practice");

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spAssignType.setAdapter(typeAdapter);

        spAssignType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedType = types.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private List<String> teacherAssignedSubjectIds = new ArrayList<>();
    private boolean isTeacherUser = false;
    private boolean teacherPermissionsLoaded = false;

    private void loadSubjects() {
        if (!teacherPermissionsLoaded) {
            isTeacherUser = "TEACHER".equalsIgnoreCase(sessionManager.getUserRole());
            if (isTeacherUser) {
                String uid = "";
                FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
                if (fUser != null) uid = fUser.getUid();
                else uid = sessionManager.getIdentifier();

                if (!uid.isEmpty()) {
                    db.collection("teachers").document(uid).get().addOnSuccessListener(doc -> {
                        teacherPermissionsLoaded = true;
                        if (doc != null && doc.exists()) {
                            List<String> ids = (List<String>) doc.get("assignedSubjectIds");
                            if (ids != null) teacherAssignedSubjectIds = ids;
                        }
                        loadSubjectsInternal();
                    }).addOnFailureListener(e -> {
                        teacherPermissionsLoaded = true;
                        loadSubjectsInternal();
                    });
                    return;
                }
            }
            teacherPermissionsLoaded = true;
        }
        loadSubjectsInternal();
    }

    private void loadSubjectsInternal() {
        showLoading(true);
        attendanceRepository.fetchSubjects(selectedDept, selectedSem, new AttendanceRepository.OnSubjectsLoadedListener() {
            @Override
            public void onSuccess(List<Subject> subjects) {
                showLoading(false);
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
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                Toast.makeText(CreateAssignmentActivity.this, "Error loading subjects: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSubjectSpinner(List<Subject> subjects) {
        List<String> subjectNames = new ArrayList<>();
        if (subjects != null && !subjects.isEmpty()) {
            for (Subject s : subjects) {
                subjectNames.add(s.getSubjectCode() + " - " + s.getSubjectName());
            }
            selectedSubject = subjects.get(0);
        } else {
            subjectNames.add("No Subjects Available");
            selectedSubject = null;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, subjectNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spAssignSubject.setAdapter(adapter);

        spAssignSubject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (loadedSubjects != null && position < loadedSubjects.size()) {
                    selectedSubject = loadedSubjects.get(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupDatePicker() {
        btnSelectDueDate.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();

            DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar chosenCal = Calendar.getInstance();
                chosenCal.set(year, month, dayOfMonth, 0, 0, 0);

                Calendar todayStart = Calendar.getInstance();
                todayStart.set(Calendar.HOUR_OF_DAY, 0);
                todayStart.set(Calendar.MINUTE, 0);
                todayStart.set(Calendar.SECOND, 0);
                todayStart.set(Calendar.MILLISECOND, 0);

                if (chosenCal.before(todayStart)) {
                    Toast.makeText(CreateAssignmentActivity.this, "Due date cannot be in the past.", Toast.LENGTH_LONG).show();
                    return;
                }

                dueCalendar.set(Calendar.YEAR, year);
                dueCalendar.set(Calendar.MONTH, month);
                dueCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                isDateSelected = true;
                updateDateText();
            }, dueCalendar.get(Calendar.YEAR), dueCalendar.get(Calendar.MONTH), dueCalendar.get(Calendar.DAY_OF_MONTH));

            dialog.getDatePicker().setMinDate(now.getTimeInMillis() - 1000);
            dialog.show();
        });
    }

    private void setupTimePicker() {
        btnSelectDueTime.setOnClickListener(v -> {
            int hour = dueCalendar.get(Calendar.HOUR_OF_DAY);
            int minute = dueCalendar.get(Calendar.MINUTE);

            TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minuteOfHour) -> {
                Calendar testCal = (Calendar) dueCalendar.clone();
                testCal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                testCal.set(Calendar.MINUTE, minuteOfHour);

                Calendar now = Calendar.getInstance();

                if (testCal.before(now)) {
                    Toast.makeText(CreateAssignmentActivity.this, "Please select a future due time.", Toast.LENGTH_LONG).show();
                    return;
                }

                dueCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                dueCalendar.set(Calendar.MINUTE, minuteOfHour);
                isTimeSelected = true;
                updateTimeText();
            }, hour, minute, false);

            dialog.show();
        });
    }

    private void updateDateText() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.US);
        tvDueDate.setText(sdf.format(dueCalendar.getTime()));
    }

    private void updateTimeText() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
        tvDueTime.setText(sdf.format(dueCalendar.getTime()));
    }

    private void setupAttachmentPicker() {
        btnAddAttachment.setOnClickListener(v -> filePickerLauncher.launch("*/*"));
    }

    private void setupActionButtons() {
        btnSaveDraft.setOnClickListener(v -> saveAssignmentWithStatus("DRAFT"));
        btnCreatePublish.setOnClickListener(v -> promptPublishConfirmation());
    }

    private Assignment validateAndBuildAssignment(String targetStatus) {
        String title = etAssignTitle.getText() != null ? etAssignTitle.getText().toString().trim() : "";
        String desc = etAssignDesc.getText() != null ? etAssignDesc.getText().toString().trim() : "";
        String maxMarksStr = etMaxMarks.getText() != null ? etMaxMarks.getText().toString().trim() : "";

        if (title.isEmpty()) {
            etAssignTitle.setError("Please enter assignment title.");
            Toast.makeText(this, "Please enter assignment title.", Toast.LENGTH_SHORT).show();
            return null;
        }

        if (desc.isEmpty()) {
            etAssignDesc.setError("Please enter assignment description.");
            Toast.makeText(this, "Please enter assignment description.", Toast.LENGTH_SHORT).show();
            return null;
        }

        if (selectedSubject == null) {
            Toast.makeText(this, "Please select a subject.", Toast.LENGTH_SHORT).show();
            return null;
        }

        double maxMarks = 0;
        try {
            maxMarks = Double.parseDouble(maxMarksStr);
        } catch (Exception ignored) {}

        if (maxMarks <= 0) {
            etMaxMarks.setError("Enter a valid maximum mark.");
            Toast.makeText(this, "Enter a valid maximum mark.", Toast.LENGTH_SHORT).show();
            return null;
        }

        if (!isDateSelected) {
            Toast.makeText(this, "Please select a due date.", Toast.LENGTH_SHORT).show();
            return null;
        }

        Calendar now = Calendar.getInstance();
        if (dueCalendar.before(now)) {
            Toast.makeText(this, "Due date cannot be in the past.", Toast.LENGTH_SHORT).show();
            return null;
        }

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
        String dueTimeStr = timeFormat.format(dueCalendar.getTime());

        String teacherUid = getTeacherIdentity();
        String docId = "assign_" + System.currentTimeMillis();

        return new Assignment(
                docId,
                title,
                desc,
                selectedDept,
                selectedSem,
                String.valueOf(selectedSubject.getId()),
                selectedSubject.getSubjectName(),
                selectedType,
                new Timestamp(dueCalendar.getTime()),
                dueTimeStr,
                maxMarks,
                attachedFileUrl,
                attachedFileName,
                teacherUid,
                targetStatus
        );
    }

    private void saveAssignmentWithStatus(String status) {
        Assignment assignment = validateAndBuildAssignment(status);
        if (assignment == null) return;

        showLoading(true);
        btnSaveDraft.setEnabled(false);
        btnCreatePublish.setEnabled(false);

        assignmentRepository.saveAssignment(assignment, new AssignmentRepository.OnAssignmentOperationListener() {
            @Override
            public void onSuccess(String docId) {
                showLoading(false);
                btnSaveDraft.setEnabled(true);
                btnCreatePublish.setEnabled(true);

                if ("DRAFT".equalsIgnoreCase(status)) {
                    Toast.makeText(CreateAssignmentActivity.this, "Assignment saved as draft.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(CreateAssignmentActivity.this, "Assignment published successfully.", Toast.LENGTH_LONG).show();
                }

                finish();
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                btnSaveDraft.setEnabled(true);
                btnCreatePublish.setEnabled(true);
                Toast.makeText(CreateAssignmentActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void promptPublishConfirmation() {
        Assignment assignment = validateAndBuildAssignment("PUBLISHED");
        if (assignment == null) return;

        SimpleDateFormat dateFmt = new SimpleDateFormat("dd MMM yyyy", Locale.US);
        SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a", Locale.US);

        String message = String.format(Locale.US,
                "Title:\n%s\n\nSubject:\n%s\n\nSemester:\n%s\n\nDue:\n%s, %s\n\nMaximum Marks:\n%.0f",
                assignment.getTitle(),
                assignment.getSubjectName(),
                assignment.getSemester(),
                dateFmt.format(dueCalendar.getTime()),
                timeFmt.format(dueCalendar.getTime()),
                assignment.getMaxMarks()
        );

        new AlertDialog.Builder(this)
                .setTitle("Publish Assignment?")
                .setMessage(message)
                .setPositiveButton("PUBLISH", (dialog, which) -> saveAssignmentWithStatus("PUBLISHED"))
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private String getTeacherIdentity() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            return currentUser.getUid();
        }
        String sessionEmail = sessionManager.getUserEmail();
        return (sessionEmail != null && !sessionEmail.isEmpty()) ? sessionEmail : "TCH1001";
    }

    private String getFileNameFromUri(Uri uri) {
        String path = uri.getPath();
        if (path != null) {
            int cut = path.lastIndexOf('/');
            if (cut != -1) {
                return path.substring(cut + 1);
            }
        }
        return "assignment_attachment.pdf";
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
