package com.example;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Patterns;
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

import com.example.model.Department;
import com.example.model.Student;
import com.example.repository.StudentRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Activity for registering a new student profile in Firebase Firestore.
 * Supports Program Level ("UG" / "PG") with cascading Department and adaptive Semesters.
 */
public class AddStudentActivity extends AppCompatActivity {

    private static final String TAG = "AddStudentActivity";

    private FirebaseFirestore db;
    private StudentRepository repository;

    private Toolbar toolbar;
    private TextInputEditText etName;
    private TextInputEditText etRegNo;
    private TextInputEditText etLoginId;
    private TextInputEditText etEmail;
    private TextInputEditText etPhone;
    private TextInputEditText etPassword;

    private Spinner spProgramLevel;
    private Spinner spDept;
    private Spinner spSem;
    private Spinner spSection;
    private Spinner spGender;

    private LinearLayout btnSelectDob;
    private TextView tvDob;
    private ProgressBar progressBar;
    private MaterialButton btnSave;

    private final List<Department> allDepartments = new ArrayList<>();
    private final List<Department> filteredDepartments = new ArrayList<>();
    private final List<String> filteredDeptDisplays = new ArrayList<>();
    private ArrayAdapter<String> deptAdapter;

    private final List<String> semesterList = new ArrayList<>();
    private ArrayAdapter<String> semAdapter;

    // Must be selected from dropdown (never hardcoded to UG)
    private String selectedProgramLevel = "";
    private String selectedDeptName = "";
    private String selectedDeptCode = "";
    private String selectedDeptId = "";
    private String selectedSem = "";
    private String selectedSection = "Section A";
    private String selectedGender = "Male";
    private String selectedDob = "";

    private Calendar dobCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_student);

        db = FirebaseFirestore.getInstance();
        repository = StudentRepository.getInstance(this);

        initViews();
        setupToolbar();
        setupWindowInsets();
        setupProgramLevelSpinner();
        setupDepartmentSpinner();
        setupSemesterSpinner();
        setupSectionSpinner();
        setupGenderSpinner();
        setupDatePicker();

        loadDepartmentsFromFirestore();

        btnSave.setOnClickListener(v -> validateAndPromptAdd());
    }

    private void setupWindowInsets() {
        View appBar = findViewById(R.id.appBarAddStudent);
        if (appBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, insets) -> {
                androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                v.setPadding(0, statusBarInsets.top, 0, 0);
                return insets;
            });
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarAddStudent);
        etName = findViewById(R.id.etName);
        etRegNo = findViewById(R.id.etRegNo);
        etLoginId = findViewById(R.id.etLoginId);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);

        spProgramLevel = findViewById(R.id.spAddProgramLevel);
        spDept = findViewById(R.id.spAddDepartment);
        spSem = findViewById(R.id.spAddSemester);
        spSection = findViewById(R.id.spAddSection);
        spGender = findViewById(R.id.spAddGender);

        btnSelectDob = findViewById(R.id.btnSelectDob);
        tvDob = findViewById(R.id.tvAddDob);
        progressBar = findViewById(R.id.progressBarAddStudent);
        btnSave = findViewById(R.id.btnSaveStudent);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Add Student");
            getSupportActionBar().setSubtitle("Enroll New Student Profile into Firestore");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupProgramLevelSpinner() {
        List<String> levels = new ArrayList<>();
        levels.add("Select Program Level");
        levels.add("UG");
        levels.add("PG");

        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, levels);
        levelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spProgramLevel.setAdapter(levelAdapter);

        spProgramLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 1) {
                    selectedProgramLevel = "UG";
                } else if (position == 2) {
                    selectedProgramLevel = "PG";
                } else {
                    selectedProgramLevel = "";
                }

                android.util.Log.d(TAG, "Selected Program Level = " + (selectedProgramLevel.isEmpty() ? "None" : selectedProgramLevel));

                // When Program Level changes, clear previously selected department and reload
                selectedDeptName = "";
                selectedDeptCode = "";
                selectedDeptId = "";

                updateDepartmentListForLevel(selectedProgramLevel);
                updateSemesterListForLevel(selectedProgramLevel);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedProgramLevel = "";
            }
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
                } else {
                    selectedDeptName = "";
                    selectedDeptCode = "";
                    selectedDeptId = "";
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedDeptName = "";
                selectedDeptCode = "";
                selectedDeptId = "";
            }
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
                } else {
                    selectedSem = "";
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedSem = "";
            }
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
        } else if ("UG".equalsIgnoreCase(programLevel)) {
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
        } else {
            selectedSem = "";
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

                    populateDefaultDepartmentsIfEmpty();
                    updateDepartmentListForLevel(selectedProgramLevel);
                })
                .addOnFailureListener(e -> {
                    populateDefaultDepartmentsIfEmpty();
                    updateDepartmentListForLevel(selectedProgramLevel);
                });
    }

    private void populateDefaultDepartmentsIfEmpty() {
        if (allDepartments.isEmpty()) {
            // UG Departments
            allDepartments.add(new Department("bca", "Bachelor of Computer Applications", "BCA", "UG", "ACTIVE"));
            allDepartments.add(new Department("bcom", "Bachelor of Commerce", "B.Com", "UG", "ACTIVE"));
            allDepartments.add(new Department("bba", "Bachelor of Business Administration", "BBA", "UG", "ACTIVE"));
            allDepartments.add(new Department("bsc_cs", "B.Sc Computer Science", "B.Sc CS", "UG", "ACTIVE"));
            // PG Departments
            allDepartments.add(new Department("mca", "Master of Computer Applications", "MCA", "PG", "ACTIVE"));
            allDepartments.add(new Department("mcom", "Master of Commerce", "M.Com", "PG", "ACTIVE"));
            allDepartments.add(new Department("mba", "Master of Business Administration", "MBA", "PG", "ACTIVE"));
            allDepartments.add(new Department("msc_cs", "M.Sc Computer Science", "M.Sc CS", "PG", "ACTIVE"));
        }
    }

    private void updateDepartmentListForLevel(String level) {
        filteredDepartments.clear();
        filteredDeptDisplays.clear();

        if (level != null && !level.isEmpty()) {
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
        }

        deptAdapter.notifyDataSetChanged();
        if (!filteredDepartments.isEmpty()) {
            spDept.setSelection(0);
            Department first = filteredDepartments.get(0);
            selectedDeptName = first.getDepartmentName();
            selectedDeptCode = first.getDepartmentCode();
            selectedDeptId = first.getDepartmentId();
        } else {
            selectedDeptName = "";
            selectedDeptCode = "";
            selectedDeptId = "";
        }
    }

    private void setupSectionSpinner() {
        List<String> sections = new ArrayList<>();
        sections.add("Section A");
        sections.add("Section B");
        sections.add("Section C");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sections);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSection.setAdapter(adapter);

        spSection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSection = sections.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupGenderSpinner() {
        List<String> genders = new ArrayList<>();
        genders.add("Male");
        genders.add("Female");
        genders.add("Other");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, genders);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spGender.setAdapter(adapter);

        spGender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedGender = genders.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupDatePicker() {
        dobCalendar.set(2002, Calendar.JANUARY, 1);
        btnSelectDob.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                dobCalendar.set(Calendar.YEAR, year);
                dobCalendar.set(Calendar.MONTH, month);
                dobCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                selectedDob = sdf.format(dobCalendar.getTime());
                tvDob.setText(selectedDob);
            }, dobCalendar.get(Calendar.YEAR), dobCalendar.get(Calendar.MONTH), dobCalendar.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });
    }

    private void validateAndPromptAdd() {
        if (!com.example.utils.IdGenerationService.isNetworkAvailable(this)) {
            Toast.makeText(this, "Internet connection is required to create Student/Teacher accounts.", Toast.LENGTH_LONG).show();
            return;
        }

        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String regNo = etRegNo.getText() != null ? etRegNo.getText().toString().trim().toUpperCase(Locale.US) : "";
        String enteredLoginId = etLoginId != null && etLoginId.getText() != null ? etLoginId.getText().toString().trim().toUpperCase(Locale.US) : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim().toLowerCase(Locale.US) : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (name.isEmpty()) {
            etName.setError("Student name is required.");
            Toast.makeText(this, "Student name is required.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (regNo.isEmpty()) {
            etRegNo.setError("Register number is required.");
            Toast.makeText(this, "Register number is required.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Valid email address is required.");
            Toast.makeText(this, "Valid email address is required.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (phone.isEmpty() || phone.length() < 7) {
            etPhone.setError("Valid phone number is required.");
            Toast.makeText(this, "Valid phone number is required.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty() || password.length() < 8) {
            etPassword.setError("Initial password must contain at least 8 characters.");
            Toast.makeText(this, "Initial password must contain at least 8 characters.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Program Level Validation
        String programLevelVal = spProgramLevel.getSelectedItem() != null ? spProgramLevel.getSelectedItem().toString() : "";
        if (programLevelVal.equals("Select Program Level") || (!"UG".equalsIgnoreCase(selectedProgramLevel) && !"PG".equalsIgnoreCase(selectedProgramLevel))) {
            Toast.makeText(this, "Please select Program Level (UG or PG).", Toast.LENGTH_SHORT).show();
            return;
        }

        // Department Validation
        if (selectedDeptName.isEmpty() || filteredDepartments.isEmpty()) {
            Toast.makeText(this, "Please select a valid Department.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Semester Validation
        if (selectedSem.isEmpty()) {
            Toast.makeText(this, "Please select a Semester.", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        btnSave.setEnabled(false);

        // Step 1: Validate or generate Login ID
        if (!enteredLoginId.isEmpty()) {
            com.example.utils.IdGenerationService.getInstance().isLoginIdAvailable(enteredLoginId, new com.example.utils.IdGenerationService.OnAvailabilityListener() {
                @Override
                public void onResult(boolean isAvailable) {
                    if (!isAvailable) {
                        showLoading(false);
                        btnSave.setEnabled(true);
                        etLoginId.setError("Login ID already exists. Please choose another ID.");
                        Toast.makeText(AddStudentActivity.this, "Login ID already exists. Please choose another ID.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    // Verify Register Number uniqueness
                    checkRegNoAndConfirm(name, regNo, enteredLoginId, email, phone, password);
                }

                @Override
                public void onError(String errorMessage) {
                    showLoading(false);
                    btnSave.setEnabled(true);
                    Toast.makeText(AddStudentActivity.this, "Login ID validation error: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Automatic generation based on department code/name (never hardcoded to UG)
            String deptPrefix = (selectedDeptCode != null && !selectedDeptCode.isEmpty()) ? selectedDeptCode : selectedDeptName;
            com.example.utils.IdGenerationService.getInstance().generateNextLoginId(deptPrefix, false, new com.example.utils.IdGenerationService.OnIdGeneratedListener() {
                @Override
                public void onSuccess(String generatedLoginId) {
                    checkRegNoAndConfirm(name, regNo, generatedLoginId, email, phone, password);
                }

                @Override
                public void onError(String errorMessage) {
                    showLoading(false);
                    btnSave.setEnabled(true);
                    Toast.makeText(AddStudentActivity.this, "Login ID generation error: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void checkRegNoAndConfirm(String name, String regNo, String loginId, String email, String phone, String password) {
        repository.checkRegisterNoUnique(regNo, null, new StudentRepository.OnRegNoCheckListener() {
            @Override
            public void onResult(boolean isUnique) {
                showLoading(false);
                btnSave.setEnabled(true);

                if (!isUnique) {
                    etRegNo.setError("Student with this register number already exists.");
                    Toast.makeText(AddStudentActivity.this, "Student with this register number already exists.", Toast.LENGTH_LONG).show();
                    return;
                }

                promptAddConfirmation(name, regNo, loginId, email, phone, password);
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                btnSave.setEnabled(true);
                Toast.makeText(AddStudentActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void promptAddConfirmation(String name, String regNo, String loginId, String email, String phone, String password) {
        String message = String.format(Locale.US,
                "Student account will be created with Login ID: %s\n\nName: %s\nRegister No: %s\nProgram Level: %s\nDepartment: %s\nSemester: %s\nEmail: %s",
                loginId, name, regNo, selectedProgramLevel, selectedDeptName, selectedSem, email
        );

        new AlertDialog.Builder(this)
                .setTitle("Create Student Account")
                .setMessage(message)
                .setPositiveButton("CREATE ACCOUNT", (dialog, which) -> executeStudentCreation(name, regNo, loginId, email, phone, password))
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void executeStudentCreation(String name, String regNo, String loginId, String email, String phone, String password) {
        showLoading(true);
        btnSave.setEnabled(false);

        final String finalProgramLevel = selectedProgramLevel;

        com.example.utils.AdminAuthService.getInstance().createStudentAccount(
                this,
                email,
                password,
                loginId,
                name,
                regNo,
                phone,
                finalProgramLevel,
                selectedDeptName,
                selectedDeptId,
                selectedDeptCode,
                selectedSem,
                selectedSection,
                selectedGender,
                selectedDob,
                new com.example.utils.AdminAuthService.OnUserCreatedListener() {
                    @Override
                    public void onSuccess(String firebaseUid, String message) {
                        // Local SQLite DB Cache
                        Student student = new Student(
                                firebaseUid,
                                name,
                                regNo,
                                email,
                                phone,
                                selectedDeptName,
                                selectedDeptId,
                                selectedDeptCode,
                                finalProgramLevel,
                                selectedSem,
                                selectedSection,
                                selectedGender,
                                selectedDob,
                                "",
                                "ACTIVE"
                        );
                        student.setFirebaseUid(firebaseUid);
                        student.setLoginId(loginId);

                        new com.example.database.DatabaseHelper(AddStudentActivity.this).addStudent(student, password);

                        // Audit Log
                        com.example.utils.PortalActivityLogger.getInstance(AddStudentActivity.this)
                                .logStudentAccountCreated(firebaseUid, name, regNo, "[" + finalProgramLevel + "] " + selectedDeptName);

                        showLoading(false);
                        btnSave.setEnabled(true);
                        Toast.makeText(AddStudentActivity.this, "Student account created successfully with Login ID: " + loginId, Toast.LENGTH_LONG).show();
                        setResult(RESULT_OK);
                        finish();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        showLoading(false);
                        btnSave.setEnabled(true);
                        Toast.makeText(AddStudentActivity.this, "Creation Failed: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
