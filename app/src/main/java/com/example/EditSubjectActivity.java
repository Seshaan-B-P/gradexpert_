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
import com.example.model.Semester;
import com.example.model.Subject;
import com.example.repository.SubjectRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for updating an existing course subject in Firebase Firestore.
 */
public class EditSubjectActivity extends AppCompatActivity {

    private static final String TAG = "EditSubjectActivity";

    private FirebaseFirestore db;
    private SubjectRepository repository;

    private Toolbar toolbar;
    private TextInputEditText etName;
    private TextInputEditText etCode;
    private TextInputEditText etDesc;

    private Spinner spDept;
    private Spinner spSem;
    private Spinner spType;
    private Spinner spStatus;

    private TextInputEditText etCredits;
    private TextInputEditText etWeeklyHours;
    private TextInputEditText etTotalHours;

    private ProgressBar progressBar;
    private MaterialButton btnUpdate;

    private String subjectId = "";
    private List<String> deptList = new ArrayList<>();
    private List<String> semList = new ArrayList<>();
    private List<String> typeList = new ArrayList<>();
    private List<String> statusList = new ArrayList<>();

    private String selectedDept = "Master of Computer Applications";
    private String selectedSem = "Semester III";
    private String selectedType = "Theory";
    private String selectedStatus = "ACTIVE";
    private String programLevel = "UG";
    private String departmentId = "";
    private String departmentShortName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_subject);

        db = FirebaseFirestore.getInstance();
        repository = SubjectRepository.getInstance(this);

        readIntentExtras();
        initViews();
        setupToolbar();
        setupWindowInsets();
        setupDepartmentSpinner();
        setupSemesterSpinner();
        setupSubjectTypeSpinner();
        setupStatusSpinner();
        populateFields();

        btnUpdate.setOnClickListener(v -> validateAndPromptUpdate());
    }

    private void readIntentExtras() {
        if (getIntent() != null) {
            subjectId = getIntent().getStringExtra("subjectId");
            if (subjectId == null || subjectId.isEmpty()) {
                int legacyId = getIntent().getIntExtra("subject_id", -1);
                if (legacyId > 0) subjectId = String.valueOf(legacyId);
            }
            String pLevel = getIntent().getStringExtra("programLevel");
            if (pLevel != null && !pLevel.isEmpty()) {
                programLevel = pLevel;
            }
            String dId = getIntent().getStringExtra("departmentId");
            if (dId != null) departmentId = dId;
            String dShort = getIntent().getStringExtra("departmentShortName");
            if (dShort != null) departmentShortName = dShort;
        }
    }

    private void setupWindowInsets() {
        View appBar = findViewById(R.id.appBarEditSubject);
        if (appBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, insets) -> {
                androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                v.setPadding(0, statusBarInsets.top, 0, 0);
                return insets;
            });
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarEditSubject);
        etName = findViewById(R.id.etEditSubjectName);
        etCode = findViewById(R.id.etEditSubjectCode);
        etDesc = findViewById(R.id.etEditSubjectDesc);

        spDept = findViewById(R.id.spEditSubjectDept);
        spSem = findViewById(R.id.spEditSubjectSem);
        spType = findViewById(R.id.spEditSubjectType);
        spStatus = findViewById(R.id.spEditSubjectStatus);

        etCredits = findViewById(R.id.etEditSubjectCredits);
        etWeeklyHours = findViewById(R.id.etEditSubjectWeeklyHours);
        etTotalHours = findViewById(R.id.etEditSubjectTotalHours);

        progressBar = findViewById(R.id.progressBarEditSubject);
        btnUpdate = findViewById(R.id.btnUpdateSubject);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Edit Subject");
            getSupportActionBar().setSubtitle("Update Course Details in Firestore");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupDepartmentSpinner() {
        deptList.clear();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, deptList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDept.setAdapter(adapter);

        db.collection("departments").get().addOnSuccessListener(querySnapshot -> {
            deptList.clear();
            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    Department dept = doc.toObject(Department.class);
                    if (dept != null && "ACTIVE".equalsIgnoreCase(dept.getStatus())) {
                        String name = dept.getDepartmentName();
                        if (!name.isEmpty() && !deptList.contains(name)) {
                            deptList.add(name);
                        }
                    }
                }
            }
            if (deptList.isEmpty()) {
                deptList.add("Master of Computer Applications");
                deptList.add("Computer Science");
                deptList.add("Information Technology");
                deptList.add("Electronics & Communication");
            }
            adapter.notifyDataSetChanged();
            int selIndex = deptList.indexOf(selectedDept);
            if (selIndex >= 0) {
                spDept.setSelection(selIndex);
            }
        }).addOnFailureListener(e -> {
            if (deptList.isEmpty()) {
                deptList.add("Master of Computer Applications");
                deptList.add("Computer Science");
                deptList.add("Information Technology");
                deptList.add("Electronics & Communication");
                adapter.notifyDataSetChanged();
            }
        });

        spDept.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < deptList.size()) {
                    selectedDept = deptList.get(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSemesterSpinner() {
        semList.clear();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, semList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSem.setAdapter(adapter);

        db.collection("semesters").get().addOnSuccessListener(querySnapshot -> {
            semList.clear();
            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    Semester sem = doc.toObject(Semester.class);
                    if (sem != null && "ACTIVE".equalsIgnoreCase(sem.getStatus())) {
                        String title = sem.getSemesterTitle();
                        if (!title.isEmpty() && !semList.contains(title)) {
                            semList.add(title);
                        }
                    }
                }
            }
            if (semList.isEmpty()) {
                semList.add("Semester I");
                semList.add("Semester II");
                semList.add("Semester III");
                semList.add("Semester IV");
                semList.add("Semester V");
                semList.add("Semester VI");
            }
            adapter.notifyDataSetChanged();
            int selIndex = semList.indexOf(selectedSem);
            if (selIndex >= 0) {
                spSem.setSelection(selIndex);
            }
        }).addOnFailureListener(e -> {
            if (semList.isEmpty()) {
                semList.add("Semester I");
                semList.add("Semester II");
                semList.add("Semester III");
                semList.add("Semester IV");
                semList.add("Semester V");
                semList.add("Semester VI");
                adapter.notifyDataSetChanged();
            }
        });

        spSem.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < semList.size()) {
                    selectedSem = semList.get(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSubjectTypeSpinner() {
        typeList.clear();
        typeList.add("Theory");
        typeList.add("Practical");
        typeList.add("Lab");
        typeList.add("Project");
        typeList.add("Elective");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, typeList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spType.setAdapter(adapter);

        spType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedType = typeList.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupStatusSpinner() {
        statusList.clear();
        statusList.add("ACTIVE");
        statusList.add("INACTIVE");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statusList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStatus.setAdapter(adapter);

        spStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedStatus = statusList.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void populateFields() {
        if (getIntent() != null) {
            String name = getIntent().getStringExtra("subjectName");
            String code = getIntent().getStringExtra("subjectCode");
            String desc = getIntent().getStringExtra("description");
            String dept = getIntent().getStringExtra("department");
            String sem = getIntent().getStringExtra("semester");
            String type = getIntent().getStringExtra("subjectType");
            String status = getIntent().getStringExtra("status");

            int credits = getIntent().getIntExtra("credits", 4);
            int weeklyHrs = getIntent().getIntExtra("weeklyHours", 5);
            int totalHrs = getIntent().getIntExtra("totalHours", 60);

            if (name != null) etName.setText(name);
            if (code != null) etCode.setText(code);
            if (desc != null) etDesc.setText(desc);
            etCredits.setText(String.valueOf(credits));
            etWeeklyHours.setText(String.valueOf(weeklyHrs));
            etTotalHours.setText(String.valueOf(totalHrs));

            setSpinnerSelection(spDept, deptList, dept, false, true);
            setSpinnerSelection(spSem, semList, sem, true, false);
            setSpinnerSelection(spType, typeList, type, false, false);
            setSpinnerSelection(spStatus, statusList, status, false, false);
        }
    }

    private void setSpinnerSelection(Spinner spinner, List<String> list, String valueToMatch, boolean isSem, boolean isDept) {
        if (spinner == null || list == null || valueToMatch == null || valueToMatch.isEmpty()) return;
        for (int i = 0; i < list.size(); i++) {
            String item = list.get(i);
            if (isSem) {
                if (Subject.isSemesterMatching(item, valueToMatch)) {
                    spinner.setSelection(i);
                    return;
                }
            } else if (isDept) {
                if (Subject.isDepartmentMatching(item, valueToMatch)) {
                    spinner.setSelection(i);
                    return;
                }
            } else {
                if (item.equalsIgnoreCase(valueToMatch)) {
                    spinner.setSelection(i);
                    return;
                }
            }
        }
    }

    private void validateAndPromptUpdate() {
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
        btnUpdate.setEnabled(false);

        repository.checkSubjectCodeUnique(code, selectedDept, selectedSem, subjectId, new SubjectRepository.OnSubjectCodeCheckListener() {
            @Override
            public void onResult(boolean isUnique) {
                showLoading(false);
                btnUpdate.setEnabled(true);

                if (!isUnique) {
                    etCode.setError("Subject code already exists for this semester & department.");
                    Toast.makeText(EditSubjectActivity.this, "Subject code already exists for this semester & department.", Toast.LENGTH_LONG).show();
                    return;
                }

                promptUpdateConfirmation(name, code, desc, finalCredits, finalWeeklyHours, finalTotalHours);
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                btnUpdate.setEnabled(true);
                Toast.makeText(EditSubjectActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void promptUpdateConfirmation(String name, String code, String desc, int credits, int weeklyHours, int totalHours) {
        new AlertDialog.Builder(this)
                .setTitle("Save Changes?")
                .setMessage("Are you sure you want to update details for " + name + " (" + code + ")?")
                .setPositiveButton("SAVE", (dialog, which) -> saveUpdatesToFirestore(name, code, desc, credits, weeklyHours, totalHours))
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void saveUpdatesToFirestore(String name, String code, String desc, int credits, int weeklyHours, int totalHours) {
        showLoading(true);
        btnUpdate.setEnabled(false);

        Subject subject = new Subject(
                subjectId,
                code,
                name,
                desc,
                selectedDept,
                departmentId,
                departmentShortName,
                programLevel,
                selectedSem,
                credits,
                selectedType,
                weeklyHours,
                totalHours,
                selectedStatus,
                ""
        );

        repository.updateSubject(subject, new SubjectRepository.OnSubjectOperationListener() {
            @Override
            public void onSuccess(String message) {
                showLoading(false);
                btnUpdate.setEnabled(true);
                Toast.makeText(EditSubjectActivity.this, "Subject updated successfully.", Toast.LENGTH_LONG).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                btnUpdate.setEnabled(true);
                Toast.makeText(EditSubjectActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
