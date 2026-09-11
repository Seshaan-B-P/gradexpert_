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
import java.util.List;
import java.util.Locale;

/**
 * Activity for updating an existing student profile in Firebase Firestore.
 */
public class EditStudentActivity extends AppCompatActivity {

    private static final String TAG = "EditStudentActivity";

    private StudentRepository repository;
    private FirebaseFirestore db;

    private Toolbar toolbar;
    private TextInputEditText etName;
    private TextInputEditText etRegNo;
    private TextInputEditText etEmail;
    private TextInputEditText etPhone;

    private Spinner spProgramLevel;
    private Spinner spDept;
    private Spinner spSem;
    private Spinner spSection;
    private Spinner spStatus;
    private Spinner spGender;

    private LinearLayout btnSelectDob;
    private TextView tvDob;
    private ProgressBar progressBar;
    private MaterialButton btnUpdate;

    private String studentId = "";
    private String selectedProgramLevel = "UG";
    private String selectedDeptName = "";
    private String selectedDeptCode = "";
    private String selectedDeptId = "";
    private String selectedSem = "Semester I";
    private String selectedSection = "Section A";
    private String selectedStatus = "ACTIVE";
    private String selectedGender = "Male";
    private String selectedDob = "";

    private final List<Department> allDepartments = new ArrayList<>();
    private final List<Department> filteredDepartments = new ArrayList<>();
    private final List<String> filteredDeptDisplays = new ArrayList<>();
    private ArrayAdapter<String> deptAdapter;

    private final List<String> semesterList = new ArrayList<>();
    private ArrayAdapter<String> semAdapter;

    private Calendar dobCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_student);

        db = FirebaseFirestore.getInstance();
        repository = StudentRepository.getInstance(this);

        readIntentExtras();
        initViews();
        setupToolbar();
        setupWindowInsets();
        setupProgramLevelSpinner();
        setupDepartmentSpinner();
        setupSemesterSpinner();
        setupSectionSpinner();
        setupStatusSpinner();
        setupGenderSpinner();
        setupDatePicker();

        loadDepartments();
        populateFields();

        btnUpdate.setOnClickListener(v -> validateAndPromptUpdate());
    }

    private void readIntentExtras() {
        if (getIntent() != null) {
            studentId = getIntent().getStringExtra("studentId");
            if (studentId == null || studentId.isEmpty()) {
                int legacyId = getIntent().getIntExtra("student_id", -1);
                if (legacyId > 0) studentId = String.valueOf(legacyId);
            }
            String pLevel = getIntent().getStringExtra("programLevel");
            if (pLevel != null && !pLevel.isEmpty()) {
                selectedProgramLevel = pLevel.toUpperCase();
            }
            String dept = getIntent().getStringExtra("department");
            if (dept != null) selectedDeptName = dept;
            String deptCode = getIntent().getStringExtra("departmentShortName");
            if (deptCode != null) selectedDeptCode = deptCode;
            String deptId = getIntent().getStringExtra("departmentId");
            if (deptId != null) selectedDeptId = deptId;
            String sem = getIntent().getStringExtra("semester");
            if (sem != null) selectedSem = sem;
            String sec = getIntent().getStringExtra("section");
            if (sec != null) selectedSection = sec;
            String gen = getIntent().getStringExtra("gender");
            if (gen != null) selectedGender = gen;
            String st = getIntent().getStringExtra("status");
            if (st != null) selectedStatus = st;
        }
    }

    private void setupWindowInsets() {
        View appBar = findViewById(R.id.appBarEditStudent);
        if (appBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, insets) -> {
                androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                v.setPadding(0, statusBarInsets.top, 0, 0);
                return insets;
            });
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarEditStudent);
        etName = findViewById(R.id.etEditName);
        etRegNo = findViewById(R.id.etEditRegNo);
        etEmail = findViewById(R.id.etEditEmail);
        etPhone = findViewById(R.id.etEditPhone);

        spProgramLevel = findViewById(R.id.spEditProgramLevel);
        spDept = findViewById(R.id.spEditDepartment);
        spSem = findViewById(R.id.spEditSemester);
        spSection = findViewById(R.id.spEditSection);
        spStatus = findViewById(R.id.spEditStatus);
        spGender = findViewById(R.id.spEditGender);

        btnSelectDob = findViewById(R.id.btnEditSelectDob);
        tvDob = findViewById(R.id.tvEditDob);
        progressBar = findViewById(R.id.progressBarEditStudent);
        btnUpdate = findViewById(R.id.btnUpdateStudent);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Edit Student");
            getSupportActionBar().setSubtitle("Update Academic Record in Firestore");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupProgramLevelSpinner() {
        List<String> levels = new ArrayList<>();
        levels.add("UG");
        levels.add("PG");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, levels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spProgramLevel.setAdapter(adapter);

        if ("PG".equalsIgnoreCase(selectedProgramLevel)) {
            spProgramLevel.setSelection(1);
        } else {
            spProgramLevel.setSelection(0);
        }

        spProgramLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String previous = selectedProgramLevel;
                selectedProgramLevel = (position == 1) ? "PG" : "UG";
                if (!selectedProgramLevel.equalsIgnoreCase(previous)) {
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
                    Department d = filteredDepartments.get(position);
                    selectedDeptName = d.getDepartmentName();
                    selectedDeptCode = d.getDepartmentCode();
                    selectedDeptId = d.getDepartmentId();
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
        int index = semesterList.indexOf(selectedSem);
        if (index >= 0) {
            spSem.setSelection(index);
        } else if (!semesterList.isEmpty()) {
            spSem.setSelection(0);
            selectedSem = semesterList.get(0);
        }
    }

    private void loadDepartments() {
        allDepartments.clear();
        db.collection("departments").whereEqualTo("status", "ACTIVE").get().addOnSuccessListener(snapshot -> {
            if (snapshot != null && !snapshot.isEmpty()) {
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Department d = doc.toObject(Department.class);
                    if (d != null) {
                        d.setDepartmentId(doc.getId());
                        if (doc.getString("programLevel") != null) d.setProgramLevel(doc.getString("programLevel"));
                        if (doc.getString("shortName") != null) d.setShortName(doc.getString("shortName"));
                        if (doc.getString("name") != null) d.setName(doc.getString("name"));
                        allDepartments.add(d);
                    }
                }
            }
            populateDefaultDepartmentsIfEmpty();
            updateDepartmentListForLevel(selectedProgramLevel);
        }).addOnFailureListener(e -> {
            populateDefaultDepartmentsIfEmpty();
            updateDepartmentListForLevel(selectedProgramLevel);
        });
    }

    private void populateDefaultDepartmentsIfEmpty() {
        if (allDepartments.isEmpty()) {
            allDepartments.add(new Department("bca", "Bachelor of Computer Applications", "BCA", "UG", "ACTIVE"));
            allDepartments.add(new Department("bcom", "Bachelor of Commerce", "B.Com", "UG", "ACTIVE"));
            allDepartments.add(new Department("bba", "Bachelor of Business Administration", "BBA", "UG", "ACTIVE"));
            allDepartments.add(new Department("bsc_cs", "B.Sc Computer Science", "B.Sc CS", "UG", "ACTIVE"));
            allDepartments.add(new Department("mca", "Master of Computer Applications", "MCA", "PG", "ACTIVE"));
            allDepartments.add(new Department("mcom", "Master of Commerce", "M.Com", "PG", "ACTIVE"));
            allDepartments.add(new Department("mba", "Master of Business Administration", "MBA", "PG", "ACTIVE"));
            allDepartments.add(new Department("msc_cs", "M.Sc Computer Science", "M.Sc CS", "PG", "ACTIVE"));
        }
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
        int foundIndex = -1;
        for (int i = 0; i < filteredDepartments.size(); i++) {
            if (filteredDepartments.get(i).getDepartmentName().equalsIgnoreCase(selectedDeptName) ||
                filteredDepartments.get(i).getDepartmentCode().equalsIgnoreCase(selectedDeptCode)) {
                foundIndex = i;
                break;
            }
        }

        if (foundIndex >= 0) {
            spDept.setSelection(foundIndex);
        } else if (!filteredDepartments.isEmpty()) {
            spDept.setSelection(0);
            Department first = filteredDepartments.get(0);
            selectedDeptName = first.getDepartmentName();
            selectedDeptCode = first.getDepartmentCode();
            selectedDeptId = first.getDepartmentId();
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

    private void setupStatusSpinner() {
        List<String> statuses = new ArrayList<>();
        statuses.add("ACTIVE");
        statuses.add("INACTIVE");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statuses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStatus.setAdapter(adapter);

        spStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedStatus = statuses.get(position);
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

    private void populateFields() {
        if (getIntent() != null) {
            String name = getIntent().getStringExtra("name");
            String regNo = getIntent().getStringExtra("registerNo");
            String email = getIntent().getStringExtra("email");
            String phone = getIntent().getStringExtra("phone");
            String dob = getIntent().getStringExtra("dateOfBirth");

            if (name != null) etName.setText(name);
            if (regNo != null) etRegNo.setText(regNo);
            if (email != null) etEmail.setText(email);
            if (phone != null) etPhone.setText(phone);
            if (dob != null && !dob.isEmpty()) {
                selectedDob = dob;
                tvDob.setText(dob);
            }
        }
    }

    private void validateAndPromptUpdate() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String regNo = etRegNo.getText() != null ? etRegNo.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";

        if (name.isEmpty()) {
            etName.setError("Student name is required.");
            Toast.makeText(this, "Student name is required.", Toast.LENGTH_SHORT).show();
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

        new AlertDialog.Builder(this)
                .setTitle("Save Changes?")
                .setMessage("Are you sure you want to update profile details for " + name + " (" + regNo + ")?")
                .setPositiveButton("SAVE", (dialog, which) -> saveUpdatesToFirestore(name, regNo, email, phone))
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void saveUpdatesToFirestore(String name, String regNo, String email, String phone) {
        showLoading(true);
        btnUpdate.setEnabled(false);

        Student student = new Student(
                studentId,
                name,
                regNo,
                email,
                phone,
                selectedDeptName,
                selectedDeptId,
                selectedDeptCode,
                selectedProgramLevel,
                selectedSem,
                selectedSection,
                selectedGender,
                selectedDob,
                "",
                selectedStatus
        );

        repository.updateStudent(student, new StudentRepository.OnStudentOperationListener() {
            @Override
            public void onSuccess(String message) {
                com.example.utils.PortalActivityLogger.getInstance(EditStudentActivity.this)
                        .logStudentUpdated(studentId, name, regNo);
                showLoading(false);
                btnUpdate.setEnabled(true);
                Toast.makeText(EditStudentActivity.this, "Student updated successfully.", Toast.LENGTH_LONG).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                btnUpdate.setEnabled(true);
                Toast.makeText(EditStudentActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
