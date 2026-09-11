package com.example;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.model.Department;
import com.example.model.Subject;
import com.example.repository.SubjectRepository;
import com.example.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Activity for creating a new course subject module in Firebase Firestore.
 * Supports Program Level ("UG" / "PG") with cascading Departments and adaptive Semesters.
 */
public class AddSubjectActivity extends AppCompatActivity {

    private static final String TAG = "AddSubjectActivity";

    private FirebaseFirestore db;
    private SubjectRepository repository;
    private SessionManager sessionManager;

    private Toolbar toolbar;
    private TextInputEditText etName;
    private TextInputEditText etCode;
    private TextInputEditText etDesc;

    private Spinner spProgramLevel;
    private Spinner spDept;
    private Spinner spSem;
    private Spinner spType;

    private TextInputEditText etCredits;
    private TextInputEditText etWeeklyHours;
    private TextInputEditText etTotalHours;

    private ProgressBar progressBar;
    private MaterialButton btnSave;

    private final List<Department> allDepartments = new ArrayList<>();
    private final List<Department> filteredDepartments = new ArrayList<>();
    private final List<String> filteredDeptDisplays = new ArrayList<>();
    private ArrayAdapter<String> deptAdapter;

    private final List<String> semesterList = new ArrayList<>();
    private ArrayAdapter<String> semAdapter;

    private String selectedProgramLevel = "UG";
    private String selectedDeptName = "Bachelor of Computer Applications";
    private String selectedDeptCode = "BCA";
    private String selectedDeptId = "bca";
    private String selectedSem = "Semester I";
    private String selectedType = "Theory";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_subject);

        db = FirebaseFirestore.getInstance();
        repository = SubjectRepository.getInstance(this);
        sessionManager = new SessionManager(this);

        initViews();
        setupToolbar();
        setupWindowInsets();
        setupProgramLevelSpinner();
        setupDepartmentSpinner();
        setupSemesterSpinner();
        setupSubjectTypeSpinner();

        loadDepartmentsFromFirestore();

        btnSave.setOnClickListener(v -> validateAndPromptAdd());
    }

    private void setupWindowInsets() {
        View appBar = findViewById(R.id.appBarAddSubject);
        if (appBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, insets) -> {
                androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                v.setPadding(0, statusBarInsets.top, 0, 0);
                return insets;
            });
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarAddSubject);
        etName = findViewById(R.id.etAddSubjectName);
        etCode = findViewById(R.id.etAddSubjectCode);
        etDesc = findViewById(R.id.etAddSubjectDesc);

        spProgramLevel = findViewById(R.id.spAddSubjectProgramLevel);
        spDept = findViewById(R.id.spAddSubjectDept);
        spSem = findViewById(R.id.spAddSubjectSem);
        spType = findViewById(R.id.spAddSubjectType);

        etCredits = findViewById(R.id.etAddSubjectCredits);
        etWeeklyHours = findViewById(R.id.etAddSubjectWeeklyHours);
        etTotalHours = findViewById(R.id.etAddSubjectTotalHours);

        progressBar = findViewById(R.id.progressBarAddSubject);
        btnSave = findViewById(R.id.btnSaveSubject);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Add Subject");
            getSupportActionBar().setSubtitle("Create New Course Module in Firestore");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupProgramLevelSpinner() {
        List<String> levels = new ArrayList<>();
        levels.add("UG - Undergraduate");
        levels.add("PG - Postgraduate");

        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, levels);
        levelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spProgramLevel.setAdapter(levelAdapter);

        spProgramLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String previous = selectedProgramLevel;
                selectedProgramLevel = (position == 1) ? "PG" : "UG";
                if (!selectedProgramLevel.equalsIgnoreCase(previous) || filteredDepartments.isEmpty()) {
                    updateDepartmentListForLevel(selectedProgramLevel);
                    updateSemesterListForLevel(selectedProgramLevel);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupDepartmentSpinner() {
        deptAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filteredDeptDisplays);
        deptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDept.setAdapter(deptAdapter);

        spDept.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < filteredDepartments.size()) {
                    Department dept = filteredDepartments.get(position);
                    selectedDeptName = dept.getDepartmentName();
                    selectedDeptCode = dept.getDepartmentCode();
                    selectedDeptId = dept.getDepartmentId();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSemesterSpinner() {
        semAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, semesterList);
        semAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSem.setAdapter(semAdapter);

        spSem.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < semesterList.size()) {
                    selectedSem = semesterList.get(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        updateSemesterListForLevel(selectedProgramLevel);
    }

    private void updateSemesterListForLevel(String programLevel) {
        semesterList.clear();
        if ("PG".equalsIgnoreCase(programLevel)) {
            semesterList.add("Semester I");
            semesterList.add("Semester II");
            semesterList.add("Semester III");
            semesterList.add("Semester IV");
        } else {
            semesterList.add("Semester I");
            semesterList.add("Semester II");
            semesterList.add("Semester III");
            semesterList.add("Semester IV");
            semesterList.add("Semester V");
            semesterList.add("Semester VI");
            semesterList.add("Semester VII");
            semesterList.add("Semester VIII");
        }
        semAdapter.notifyDataSetChanged();
        if (!semesterList.isEmpty()) {
            selectedSem = semesterList.get(0);
            spSem.setSelection(0);
        }
    }

    private void loadDepartmentsFromFirestore() {
        allDepartments.clear();
        db.collection("departments")
                .whereEqualTo("status", "ACTIVE")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Department dept = doc.toObject(Department.class);
                            if (dept != null) {
                                dept.setDepartmentId(doc.getId());
                                if (doc.getString("programLevel") != null) {
                                    dept.setProgramLevel(doc.getString("programLevel"));
                                }
                                if (doc.getString("shortName") != null) {
                                    dept.setShortName(doc.getString("shortName"));
                                }
                                if (doc.getString("name") != null) {
                                    dept.setName(doc.getString("name"));
                                }
                                allDepartments.add(dept);
                            }
                        }
                    }

                    updateDepartmentListForLevel(selectedProgramLevel);
                })
                .addOnFailureListener(e -> {
                    updateDepartmentListForLevel(selectedProgramLevel);
                });
    }

    private void updateDepartmentListForLevel(String level) {
        filteredDepartments.clear();
        filteredDeptDisplays.clear();

        for (Department d : allDepartments) {
            String dLevel = d.getProgramLevel() != null ? d.getProgramLevel().toUpperCase() : "UG";
            if (level.equalsIgnoreCase(dLevel)) {
                filteredDepartments.add(d);
                String code = d.getDepartmentCode();
                String name = d.getDepartmentName();
                String display = (!code.isEmpty()) ? code + " - " + name : name;
                filteredDeptDisplays.add(display);
            }
        }

        deptAdapter.notifyDataSetChanged();
        if (!filteredDepartments.isEmpty()) {
            spDept.setSelection(0);
            Department first = filteredDepartments.get(0);
            selectedDeptName = first.getDepartmentName();
            selectedDeptCode = first.getDepartmentCode();
            selectedDeptId = first.getDepartmentId();
        }
    }

    private void setupSubjectTypeSpinner() {
        List<String> types = new ArrayList<>();
        types.add("Theory");
        types.add("Practical");
        types.add("Lab");
        types.add("Project");
        types.add("Elective");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spType.setAdapter(adapter);

        spType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedType = types.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void validateAndPromptAdd() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String code = etCode.getText() != null ? etCode.getText().toString().trim() : "";
        String desc = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";
        String creditsStr = etCredits.getText() != null ? etCredits.getText().toString().trim() : "";
        String weeklyHrsStr = etWeeklyHours.getText() != null ? etWeeklyHours.getText().toString().trim() : "";
        String totalHrsStr = etTotalHours.getText() != null ? etTotalHours.getText().toString().trim() : "";

        if (name.isEmpty()) {
            etName.setError("Please enter subject name.");
            Toast.makeText(this, "Please enter subject name.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (code.isEmpty()) {
            etCode.setError("Please enter subject code.");
            Toast.makeText(this, "Please enter subject code.", Toast.LENGTH_SHORT).show();
            return;
        }

        int credits = 0;
        try {
            credits = Integer.parseInt(creditsStr);
        } catch (Exception ignored) {}

        if (credits <= 0) {
            etCredits.setError("Enter a valid credit value.");
            Toast.makeText(this, "Enter a valid credit value.", Toast.LENGTH_SHORT).show();
            return;
        }

        int weeklyHours = 0;
        try {
            weeklyHours = Integer.parseInt(weeklyHrsStr);
        } catch (Exception ignored) {}

        if (weeklyHours <= 0) {
            etWeeklyHours.setError("Enter valid weekly hours.");
            Toast.makeText(this, "Enter valid weekly hours.", Toast.LENGTH_SHORT).show();
            return;
        }

        int totalHours = weeklyHours * 12;
        try {
            if (!totalHrsStr.isEmpty()) totalHours = Integer.parseInt(totalHrsStr);
        } catch (Exception ignored) {}

        int finalCredits = credits;
        int finalWeeklyHours = weeklyHours;
        int finalTotalHours = totalHours;

        showLoading(true);
        btnSave.setEnabled(false);

        // Verify uniqueness of Subject Code within Department + Semester
        repository.checkSubjectCodeUnique(code, selectedDeptName, selectedSem, null, new SubjectRepository.OnSubjectCodeCheckListener() {
            @Override
            public void onResult(boolean isUnique) {
                showLoading(false);
                btnSave.setEnabled(true);

                if (!isUnique) {
                    etCode.setError("Subject code already exists for this semester.");
                    Toast.makeText(AddSubjectActivity.this, "Subject code already exists for this semester.", Toast.LENGTH_LONG).show();
                    return;
                }

                promptAddConfirmation(name, code, desc, finalCredits, finalWeeklyHours, finalTotalHours);
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                btnSave.setEnabled(true);
                Toast.makeText(AddSubjectActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void promptAddConfirmation(String name, String code, String desc, int credits, int weeklyHours, int totalHours) {
        String message = String.format(Locale.US,
                "Subject:\n%s\n\nCode:\n%s\n\nProgram Level:\n%s\n\nDepartment:\n%s\n\nSemester:\n%s\n\nCredits:\n%d",
                name, code, selectedProgramLevel, selectedDeptName, selectedSem, credits
        );

        new AlertDialog.Builder(this)
                .setTitle("Add Subject?")
                .setMessage(message)
                .setPositiveButton("ADD", (dialog, which) -> saveToFirestore(name, code, desc, credits, weeklyHours, totalHours))
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void saveToFirestore(String name, String code, String desc, int credits, int weeklyHours, int totalHours) {
        showLoading(true);
        btnSave.setEnabled(false);

        String teacherUid = getTeacherIdentity();
        String docId = "subj_" + System.currentTimeMillis();

        Subject subject = new Subject(
                docId,
                code,
                name,
                desc,
                selectedDeptName,
                selectedDeptId,
                selectedDeptCode,
                selectedProgramLevel,
                selectedSem,
                credits,
                selectedType,
                weeklyHours,
                totalHours,
                "ACTIVE",
                teacherUid
        );

        repository.addSubject(subject, new SubjectRepository.OnSubjectOperationListener() {
            @Override
            public void onSuccess(String message) {
                com.example.utils.PortalActivityLogger.getInstance(AddSubjectActivity.this)
                        .logSubjectCreated(docId, name, code, "[" + selectedProgramLevel + "] " + selectedDeptName, selectedSem);
                showLoading(false);
                btnSave.setEnabled(true);
                Toast.makeText(AddSubjectActivity.this, "Subject added successfully.", Toast.LENGTH_LONG).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                btnSave.setEnabled(true);
                Toast.makeText(AddSubjectActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private String getTeacherIdentity() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            return currentUser.getUid();
        }
        String sessionEmail = sessionManager.getUserEmail();
        return (sessionEmail != null && !sessionEmail.isEmpty()) ? sessionEmail : "TCH1001";
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
