package com.example;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.database.DatabaseHelper;
import com.example.model.Department;
import com.example.model.Teacher;
import com.example.utils.AdminAuthService;
import com.example.utils.PortalActivityLogger;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity for System Administrator to provision and create new Faculty Teacher accounts.
 * Supports Program Level ("UG" / "PG") with dependent Department loading.
 */
public class AddTeacherActivity extends AppCompatActivity {

    private Toolbar toolbar;

    // Personal Information Views
    private TextInputLayout tilTeacherName, tilTeacherPhone, tilTeacherEmail;
    private TextInputEditText etTeacherName, etTeacherPhone, etTeacherEmail;

    // Professional Information Views
    private TextInputLayout tilTeacherEmployeeId, tilTeacherLoginId, tilTeacherProgramLevel, tilTeacherDept, tilTeacherDesignation, tilTeacherQualification, tilTeacherDoj;
    private TextInputEditText etTeacherEmployeeId, etTeacherLoginId, etTeacherQualification, etTeacherDoj;
    private AutoCompleteTextView actvTeacherProgramLevel, actvTeacherDept, actvTeacherDesignation;

    // Account Credentials Views
    private TextInputLayout tilTeacherInitialPassword, tilTeacherConfirmPassword;
    private TextInputEditText etTeacherInitialPassword, etTeacherConfirmPassword;

    // Password Strength Views
    private LinearLayout layoutPasswordStrength;
    private ProgressBar pbPasswordStrength;
    private TextView tvPasswordStrength;

    // Assigned Subjects Views
    private com.google.android.material.chip.ChipGroup chipGroupSelectedSubjects;
    private TextView tvNoSubjectsSelectedPrompt;
    private MaterialButton btnOpenSubjectPicker;

    // Loading & Action Buttons
    private LinearLayout layoutLoading, layoutActions;
    private MaterialButton btnCancel, btnCreateTeacher;

    private FirebaseFirestore db;
    private DatabaseHelper dbHelper;
    private PortalActivityLogger activityLogger;
    private AdminAuthService authService;

    private final List<Department> allDepartments = new ArrayList<>();
    private final List<String> currentFilteredDeptDisplays = new ArrayList<>();
    private final List<Department> currentFilteredDeptObjects = new ArrayList<>();

    // Available and selected subjects for this teacher
    private final List<com.example.model.Subject> availableSubjects = new ArrayList<>();
    private final List<com.example.model.Subject> selectedSubjects = new ArrayList<>();

    private String selectedProgramLevel = "UG";
    private String selectedDeptId = "";
    private String selectedDeptName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_teacher);

        db = FirebaseFirestore.getInstance();
        dbHelper = new DatabaseHelper(this);
        activityLogger = PortalActivityLogger.getInstance(this);
        authService = AdminAuthService.getInstance();

        initViews();
        setupToolbar();
        setupWindowInsets();
        setupProgramLevelDropdown();
        loadDepartmentsFromFirestore();
        setupDesignationDropdown();
        setupDatePicker();
        setupSubjectPicker();
        setupPasswordStrengthWatcher();
        setupActions();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarAddTeacher);

        tilTeacherName = findViewById(R.id.tilTeacherName);
        tilTeacherPhone = findViewById(R.id.tilTeacherPhone);
        tilTeacherEmail = findViewById(R.id.tilTeacherEmail);
        etTeacherName = findViewById(R.id.etTeacherName);
        etTeacherPhone = findViewById(R.id.etTeacherPhone);
        etTeacherEmail = findViewById(R.id.etTeacherEmail);

        tilTeacherEmployeeId = findViewById(R.id.tilTeacherEmployeeId);
        tilTeacherLoginId = findViewById(R.id.tilTeacherLoginId);
        tilTeacherProgramLevel = findViewById(R.id.tilTeacherProgramLevel);
        tilTeacherDept = findViewById(R.id.tilTeacherDept);
        tilTeacherDesignation = findViewById(R.id.tilTeacherDesignation);
        tilTeacherQualification = findViewById(R.id.tilTeacherQualification);
        tilTeacherDoj = findViewById(R.id.tilTeacherDoj);

        etTeacherEmployeeId = findViewById(R.id.etTeacherEmployeeId);
        etTeacherLoginId = findViewById(R.id.etTeacherLoginId);
        etTeacherQualification = findViewById(R.id.etTeacherQualification);
        etTeacherDoj = findViewById(R.id.etTeacherDoj);
        actvTeacherProgramLevel = findViewById(R.id.actvTeacherProgramLevel);
        actvTeacherDept = findViewById(R.id.actvTeacherDept);
        actvTeacherDesignation = findViewById(R.id.actvTeacherDesignation);

        tilTeacherInitialPassword = findViewById(R.id.tilTeacherInitialPassword);
        tilTeacherConfirmPassword = findViewById(R.id.tilTeacherConfirmPassword);
        etTeacherInitialPassword = findViewById(R.id.etTeacherInitialPassword);
        etTeacherConfirmPassword = findViewById(R.id.etTeacherConfirmPassword);

        layoutPasswordStrength = findViewById(R.id.layoutTeacherPasswordStrength);
        pbPasswordStrength = findViewById(R.id.pbTeacherPasswordStrength);
        tvPasswordStrength = findViewById(R.id.tvTeacherPasswordStrength);

        chipGroupSelectedSubjects = findViewById(R.id.chipGroupSelectedSubjects);
        tvNoSubjectsSelectedPrompt = findViewById(R.id.tvNoSubjectsSelectedPrompt);
        btnOpenSubjectPicker = findViewById(R.id.btnOpenSubjectPicker);

        layoutLoading = findViewById(R.id.layoutTeacherCreationLoading);
        layoutActions = findViewById(R.id.layoutTeacherFormActions);
        btnCancel = findViewById(R.id.btnCancelTeacherForm);
        btnCreateTeacher = findViewById(R.id.btnCreateTeacherSubmit);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Add New Teacher");
            getSupportActionBar().setSubtitle("Faculty Provisioning & Credentials");
        }
        toolbar.setNavigationOnClickListener(v -> handleCancel());
    }

    private void setupWindowInsets() {
        View appBar = findViewById(R.id.appBarAddTeacher);
        if (appBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, insets) -> {
                androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                v.setPadding(0, statusBarInsets.top, 0, 0);
                return insets;
            });
        }
    }

    private void setupProgramLevelDropdown() {
        String[] levels = new String[]{"UG", "PG"};
        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, levels);
        actvTeacherProgramLevel.setAdapter(levelAdapter);
        actvTeacherProgramLevel.setText("UG", false);
        selectedProgramLevel = "UG";

        actvTeacherProgramLevel.setOnItemClickListener((parent, view, position, id) -> {
            String newLevel = (String) parent.getItemAtPosition(position);
            if (!newLevel.equalsIgnoreCase(selectedProgramLevel)) {
                selectedProgramLevel = newLevel;
                // Clear previous department selection on program level change
                actvTeacherDept.setText("", false);
                tilTeacherDept.setError(null);
                selectedDeptId = "";
                selectedDeptName = "";
                // Clear incompatible subjects on program level change
                selectedSubjects.clear();
                availableSubjects.clear();
                updateSelectedSubjectChips();
                filterDepartmentsByProgramLevel(selectedProgramLevel);
            }
        });
    }

    private void loadDepartmentsFromFirestore() {
        allDepartments.clear();

        db.collection("departments")
                .whereEqualTo("status", "ACTIVE")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
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

                    filterDepartmentsByProgramLevel(selectedProgramLevel);
                })
                .addOnFailureListener(e -> {
                    filterDepartmentsByProgramLevel(selectedProgramLevel);
                });
    }

    private void filterDepartmentsByProgramLevel(String programLevel) {
        currentFilteredDeptDisplays.clear();
        currentFilteredDeptObjects.clear();

        for (Department d : allDepartments) {
            String lvl = d.getProgramLevel() != null ? d.getProgramLevel().toUpperCase() : "UG";
            if (programLevel.equalsIgnoreCase(lvl)) {
                currentFilteredDeptObjects.add(d);
                String code = d.getDepartmentCode();
                String name = d.getDepartmentName();
                String display = (!code.isEmpty()) ? code + " - " + name : name;
                currentFilteredDeptDisplays.add(display);
            }
        }

        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(
                AddTeacherActivity.this,
                android.R.layout.simple_dropdown_item_1line,
                currentFilteredDeptDisplays
        );
        actvTeacherDept.setAdapter(deptAdapter);

        actvTeacherDept.setOnItemClickListener((parent, view, position, id) -> {
            tilTeacherDept.setError(null);
            if (position >= 0 && position < currentFilteredDeptObjects.size()) {
                Department dept = currentFilteredDeptObjects.get(position);
                selectedDeptId = dept.getDepartmentId();
                selectedDeptName = dept.getDepartmentName();
            } else {
                String val = (String) parent.getItemAtPosition(position);
                selectedDeptName = val;
                selectedDeptId = val.toLowerCase().replaceAll("[^a-z0-9]", "");
            }
            // Clear previous subject selections when department changes to maintain integrity
            selectedSubjects.clear();
            updateSelectedSubjectChips();
            loadAvailableSubjects();
        });
    }

    private void setupSubjectPicker() {
        btnOpenSubjectPicker.setOnClickListener(v -> {
            String deptVal = actvTeacherDept.getText() != null ? actvTeacherDept.getText().toString().trim() : "";
            if (TextUtils.isEmpty(deptVal)) {
                tilTeacherDept.setError("Please select a " + selectedProgramLevel + " department first.");
                actvTeacherDept.requestFocus();
                Toast.makeText(this, "Please select Program Level and Department before assigning subjects.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (availableSubjects.isEmpty()) {
                loadAvailableSubjectsAndShowDialog();
            } else {
                showSubjectSelectionDialog();
            }
        });
    }

    private void loadAvailableSubjects() {
        availableSubjects.clear();
        String deptVal = actvTeacherDept.getText() != null ? actvTeacherDept.getText().toString().trim() : "";
        if (TextUtils.isEmpty(deptVal)) return;

        resolveSelectedDept(deptVal);

        db.collection("subjects")
                .whereEqualTo("status", "ACTIVE")
                .whereEqualTo("programLevel", selectedProgramLevel)
                .get()
                .addOnSuccessListener(snapshots -> {
                    availableSubjects.clear();
                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            com.example.model.Subject s = doc.toObject(com.example.model.Subject.class);
                            if (s != null) {
                                s.setSubjectId(doc.getId());
                                if (isSubjectMatchingTeacher(s)) {
                                    availableSubjects.add(s);
                                }
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Fallback to local DB filtered
                    availableSubjects.clear();
                    for (com.example.model.Subject s : dbHelper.getAllSubjects()) {
                        if ("ACTIVE".equalsIgnoreCase(s.getStatus()) && selectedProgramLevel.equalsIgnoreCase(s.getProgramLevel()) && isSubjectMatchingTeacher(s)) {
                            availableSubjects.add(s);
                        }
                    }
                });
    }

    private void loadAvailableSubjectsAndShowDialog() {
        String deptVal = actvTeacherDept.getText() != null ? actvTeacherDept.getText().toString().trim() : "";
        resolveSelectedDept(deptVal);

        showLoading(true);
        db.collection("subjects")
                .whereEqualTo("status", "ACTIVE")
                .whereEqualTo("programLevel", selectedProgramLevel)
                .get()
                .addOnSuccessListener(snapshots -> {
                    showLoading(false);
                    availableSubjects.clear();
                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            com.example.model.Subject s = doc.toObject(com.example.model.Subject.class);
                            if (s != null) {
                                s.setSubjectId(doc.getId());
                                if (isSubjectMatchingTeacher(s)) {
                                    availableSubjects.add(s);
                                }
                            }
                        }
                    }
                    if (availableSubjects.isEmpty()) {
                        // Fallback to local DB filtered
                        for (com.example.model.Subject s : dbHelper.getAllSubjects()) {
                            if ("ACTIVE".equalsIgnoreCase(s.getStatus()) && selectedProgramLevel.equalsIgnoreCase(s.getProgramLevel()) && isSubjectMatchingTeacher(s)) {
                                availableSubjects.add(s);
                            }
                        }
                    }

                    if (availableSubjects.isEmpty()) {
                        Toast.makeText(this, "No active subjects found for " + selectedProgramLevel + " - " + (selectedDeptName.isEmpty() ? deptVal : selectedDeptName), Toast.LENGTH_LONG).show();
                    } else {
                        showSubjectSelectionDialog();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    availableSubjects.clear();
                    for (com.example.model.Subject s : dbHelper.getAllSubjects()) {
                        if ("ACTIVE".equalsIgnoreCase(s.getStatus()) && selectedProgramLevel.equalsIgnoreCase(s.getProgramLevel()) && isSubjectMatchingTeacher(s)) {
                            availableSubjects.add(s);
                        }
                    }
                    if (availableSubjects.isEmpty()) {
                        Toast.makeText(this, "No subjects available for this department.", Toast.LENGTH_SHORT).show();
                    } else {
                        showSubjectSelectionDialog();
                    }
                });
    }

    private void resolveSelectedDept(String deptVal) {
        int selectedIdx = currentFilteredDeptDisplays.indexOf(deptVal);
        if (selectedIdx >= 0 && selectedIdx < currentFilteredDeptObjects.size()) {
            Department chosen = currentFilteredDeptObjects.get(selectedIdx);
            selectedDeptName = chosen.getDepartmentName();
            selectedDeptId = chosen.getDepartmentId();
        } else {
            if (deptVal.contains("-")) {
                selectedDeptId = deptVal.substring(0, deptVal.indexOf("-")).trim();
                selectedDeptName = deptVal.substring(deptVal.indexOf("-") + 1).trim();
            } else {
                selectedDeptName = deptVal;
                selectedDeptId = deptVal.toLowerCase().replaceAll("[^a-z0-9]", "");
            }
        }
    }

    private boolean isSubjectMatchingTeacher(com.example.model.Subject s) {
        if (s == null) return false;
        // Verify Program Level matches strictly
        String sLevel = s.getProgramLevel() != null ? s.getProgramLevel().toUpperCase() : "UG";
        if (!selectedProgramLevel.equalsIgnoreCase(sLevel)) {
            return false;
        }

        // Verify Department matches
        if (selectedDeptId != null && !selectedDeptId.isEmpty() && selectedDeptId.equalsIgnoreCase(s.getDepartmentId())) {
            return true;
        }
        if (selectedDeptName != null && !selectedDeptName.isEmpty() && com.example.model.Subject.isDepartmentMatching(selectedDeptName, s.getDepartment())) {
            return true;
        }
        String deptVal = actvTeacherDept.getText() != null ? actvTeacherDept.getText().toString().trim() : "";
        return com.example.model.Subject.isDepartmentMatching(deptVal, s.getDepartment());
    }

    private void showSubjectSelectionDialog() {
        if (availableSubjects.isEmpty()) {
            Toast.makeText(this, "No subjects found for selection.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] subjectDisplayNames = new String[availableSubjects.size()];
        boolean[] checkedItems = new boolean[availableSubjects.size()];

        for (int i = 0; i < availableSubjects.size(); i++) {
            com.example.model.Subject sub = availableSubjects.get(i);
            String sem = (sub.getSemester() != null && !sub.getSemester().isEmpty()) ? " [" + sub.getSemester() + "]" : "";
            subjectDisplayNames[i] = sub.getSubjectName() + " (" + sub.getSubjectCode() + ")" + sem;

            // Check if already selected
            for (com.example.model.Subject sel : selectedSubjects) {
                if (sel.getSubjectId().equals(sub.getSubjectId())) {
                    checkedItems[i] = true;
                    break;
                }
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Subjects (" + selectedProgramLevel + " • " + (selectedDeptName.isEmpty() ? selectedProgramLevel : selectedDeptName) + ")")
                .setMultiChoiceItems(subjectDisplayNames, checkedItems, (dialog, which, isChecked) -> {
                    checkedItems[which] = isChecked;
                })
                .setPositiveButton("ASSIGN SELECTED", (dialog, which) -> {
                    selectedSubjects.clear();
                    for (int i = 0; i < checkedItems.length; i++) {
                        if (checkedItems[i]) {
                            selectedSubjects.add(availableSubjects.get(i));
                        }
                    }
                    updateSelectedSubjectChips();
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void updateSelectedSubjectChips() {
        chipGroupSelectedSubjects.removeAllViews();

        if (selectedSubjects.isEmpty()) {
            tvNoSubjectsSelectedPrompt.setVisibility(View.VISIBLE);
            return;
        }

        tvNoSubjectsSelectedPrompt.setVisibility(View.GONE);

        for (com.example.model.Subject s : selectedSubjects) {
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
            chip.setText(s.getSubjectName() + " (" + s.getSubjectCode() + ")");
            chip.setCloseIconVisible(true);
            chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#EEF2FF"))); // Soft indigo
            chip.setTextColor(Color.parseColor("#4338CA"));
            chip.setCloseIconTint(android.content.res.ColorStateList.valueOf(Color.parseColor("#4338CA")));

            chip.setOnCloseIconClickListener(v -> {
                selectedSubjects.remove(s);
                updateSelectedSubjectChips();
            });

            chipGroupSelectedSubjects.addView(chip);
        }
    }

    private void setupDesignationDropdown() {
        String[] designations = new String[]{
                "Assistant Professor",
                "Associate Professor",
                "Professor",
                "Head of Department",
                "Lecturer",
                "Senior Lecturer",
                "Adjunct Faculty",
                "Dean of Academics"
        };

        ArrayAdapter<String> desigAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                designations
        );
        actvTeacherDesignation.setAdapter(desigAdapter);
    }

    private void setupDatePicker() {
        View.OnClickListener datePickerListener = v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Date of Joining")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
                etTeacherDoj.setText(sdf.format(new Date(selection)));
                tilTeacherDoj.setError(null);
            });

            datePicker.show(getSupportFragmentManager(), "TEACHER_DOJ_PICKER");
        };

        etTeacherDoj.setOnClickListener(datePickerListener);
        tilTeacherDoj.setEndIconOnClickListener(datePickerListener);
    }

    private void setupPasswordStrengthWatcher() {
        etTeacherInitialPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilTeacherInitialPassword.setError(null);
                updatePasswordStrength(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etTeacherConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilTeacherConfirmPassword.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updatePasswordStrength(String password) {
        if (TextUtils.isEmpty(password)) {
            layoutPasswordStrength.setVisibility(View.GONE);
            return;
        }

        layoutPasswordStrength.setVisibility(View.VISIBLE);
        int score = 0;
        int len = password.length();

        if (len >= 8) score += 30;
        if (len >= 12) score += 20;

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }

        if (hasUpper && hasLower) score += 20;
        if (hasDigit) score += 15;
        if (hasSpecial) score += 15;

        score = Math.min(score, 100);
        pbPasswordStrength.setProgress(score);

        if (score < 40) {
            pbPasswordStrength.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#EF4444")));
            tvPasswordStrength.setText("Strength: Weak (Minimum 8 characters)");
            tvPasswordStrength.setTextColor(Color.parseColor("#EF4444"));
        } else if (score < 75) {
            pbPasswordStrength.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#F59E0B")));
            tvPasswordStrength.setText("Strength: Medium (Good password)");
            tvPasswordStrength.setTextColor(Color.parseColor("#D97706"));
        } else {
            pbPasswordStrength.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#10B981")));
            tvPasswordStrength.setText("Strength: Strong (Excellent)");
            tvPasswordStrength.setTextColor(Color.parseColor("#059669"));
        }
    }

    private void setupActions() {
        btnCancel.setOnClickListener(v -> handleCancel());
        btnCreateTeacher.setOnClickListener(v -> attemptCreateTeacher());
    }

    private void handleCancel() {
        String name = etTeacherName.getText() != null ? etTeacherName.getText().toString().trim() : "";
        String email = etTeacherEmail.getText() != null ? etTeacherEmail.getText().toString().trim() : "";

        if (!TextUtils.isEmpty(name) || !TextUtils.isEmpty(email)) {
            new AlertDialog.Builder(this)
                    .setTitle("Discard Teacher Details?")
                    .setMessage("Are you sure you want to discard unsaved teacher profile data?")
                    .setPositiveButton("DISCARD", (dialog, which) -> finish())
                    .setNegativeButton("KEEP EDITING", null)
                    .show();
        } else {
            finish();
        }
    }

    private void attemptCreateTeacher() {
        if (!com.example.utils.IdGenerationService.isNetworkAvailable(this)) {
            showError("Internet connection is required to create Student/Teacher accounts.");
            return;
        }

        String name = etTeacherName.getText() != null ? etTeacherName.getText().toString().trim() : "";
        String phone = etTeacherPhone.getText() != null ? etTeacherPhone.getText().toString().trim() : "";
        String email = etTeacherEmail.getText() != null ? etTeacherEmail.getText().toString().trim().toLowerCase(Locale.US) : "";
        String empId = etTeacherEmployeeId.getText() != null ? etTeacherEmployeeId.getText().toString().trim().toUpperCase(Locale.US) : "";
        String enteredLoginId = etTeacherLoginId != null && etTeacherLoginId.getText() != null ? etTeacherLoginId.getText().toString().trim().toUpperCase(Locale.US) : "";
        String programLevel = actvTeacherProgramLevel.getText() != null ? actvTeacherProgramLevel.getText().toString().trim().toUpperCase() : "UG";
        String deptRaw = actvTeacherDept.getText() != null ? actvTeacherDept.getText().toString().trim() : "";
        String designation = actvTeacherDesignation.getText() != null ? actvTeacherDesignation.getText().toString().trim() : "";
        String qualification = etTeacherQualification.getText() != null ? etTeacherQualification.getText().toString().trim() : "";
        String doj = etTeacherDoj.getText() != null ? etTeacherDoj.getText().toString().trim() : "";
        String password = etTeacherInitialPassword.getText() != null ? etTeacherInitialPassword.getText().toString().trim() : "";
        String confirmPassword = etTeacherConfirmPassword.getText() != null ? etTeacherConfirmPassword.getText().toString().trim() : "";

        if (!"UG".equals(programLevel) && !"PG".equals(programLevel)) {
            programLevel = "UG";
        }

        // 1. Validation
        if (TextUtils.isEmpty(name)) {
            tilTeacherName.setError("Full name is required.");
            etTeacherName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            tilTeacherPhone.setError("Mobile number is required.");
            etTeacherPhone.requestFocus();
            return;
        }

        if (!phone.matches("^[6-9]\\d{9}$")) {
            tilTeacherPhone.setError("Please enter a valid 10-digit mobile number.");
            etTeacherPhone.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            tilTeacherEmail.setError("Email address is required.");
            etTeacherEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilTeacherEmail.setError("Please enter a valid institutional email address.");
            etTeacherEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(empId)) {
            tilTeacherEmployeeId.setError("Employee ID is required (e.g. EMP102).");
            etTeacherEmployeeId.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(deptRaw)) {
            tilTeacherDept.setError("Please select a " + programLevel + " department.");
            actvTeacherDept.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(designation)) {
            tilTeacherDesignation.setError("Please specify designation.");
            actvTeacherDesignation.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(qualification)) {
            tilTeacherQualification.setError("Educational qualification is required.");
            etTeacherQualification.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(doj)) {
            tilTeacherDoj.setError("Date of Joining is required.");
            etTeacherDoj.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 8) {
            tilTeacherInitialPassword.setError("Initial password must contain at least 8 characters.");
            etTeacherInitialPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            tilTeacherConfirmPassword.setError("Please confirm the initial password.");
            etTeacherConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            tilTeacherConfirmPassword.setError("Passwords do not match.");
            etTeacherConfirmPassword.requestFocus();
            return;
        }

        // Match selected Department Object
        String resolvedDeptName = deptRaw;
        String resolvedDeptCode = "";
        String resolvedDeptId = "";

        int selectedIdx = currentFilteredDeptDisplays.indexOf(deptRaw);
        if (selectedIdx >= 0 && selectedIdx < currentFilteredDeptObjects.size()) {
            Department chosen = currentFilteredDeptObjects.get(selectedIdx);
            resolvedDeptName = chosen.getDepartmentName();
            resolvedDeptCode = chosen.getDepartmentCode();
            resolvedDeptId = chosen.getDepartmentId();
        } else {
            if (deptRaw.contains("-")) {
                resolvedDeptCode = deptRaw.substring(0, deptRaw.indexOf("-")).trim();
                resolvedDeptName = deptRaw.substring(deptRaw.indexOf("-") + 1).trim();
            }
            resolvedDeptId = resolvedDeptCode.toLowerCase().replaceAll("[^a-z0-9]", "");
        }

        final String department = resolvedDeptName;
        final String departmentId = resolvedDeptId;
        final String departmentShortName = resolvedDeptCode;
        final String finalProgramLevel = programLevel;

        // Collect selected subjects
        final List<String> assignedSubjectIds = new ArrayList<>();
        final List<String> assignedSubjectNames = new ArrayList<>();
        for (com.example.model.Subject sub : selectedSubjects) {
            assignedSubjectIds.add(sub.getSubjectId());
            assignedSubjectNames.add(sub.getSubjectName());
        }

        // Set Loading State
        showLoading(true);

        // Step 1: Validate or generate Login ID
        if (!enteredLoginId.isEmpty()) {
            com.example.utils.IdGenerationService.getInstance().isLoginIdAvailable(enteredLoginId, new com.example.utils.IdGenerationService.OnAvailabilityListener() {
                @Override
                public void onResult(boolean isAvailable) {
                    if (!isAvailable) {
                        showLoading(false);
                        tilTeacherLoginId.setError("Login ID already exists. Please choose another ID.");
                        showError("Login ID already exists. Please choose another ID.");
                        return;
                    }
                    proceedWithTeacherValidation(enteredLoginId, name, phone, email, empId, department, departmentId, departmentShortName, finalProgramLevel, designation, qualification, doj, password, assignedSubjectIds, assignedSubjectNames);
                }

                @Override
                public void onError(String errorMessage) {
                    showLoading(false);
                    showError("Login ID validation error: " + errorMessage);
                }
            });
        } else {
            com.example.utils.IdGenerationService.getInstance().generateNextLoginId("TCH", true, new com.example.utils.IdGenerationService.OnIdGeneratedListener() {
                @Override
                public void onSuccess(String generatedLoginId) {
                    proceedWithTeacherValidation(generatedLoginId, name, phone, email, empId, department, departmentId, departmentShortName, finalProgramLevel, designation, qualification, doj, password, assignedSubjectIds, assignedSubjectNames);
                }

                @Override
                public void onError(String errorMessage) {
                    showLoading(false);
                    showError("Login ID generation error: " + errorMessage);
                }
            });
        }
    }

    private void proceedWithTeacherValidation(String loginId, String name, String phone, String email, String empId,
                                             String department, String departmentId, String departmentShortName,
                                             String finalProgramLevel, String designation, String qualification,
                                             String doj, String password,
                                             List<String> assignedSubjectIds, List<String> assignedSubjectNames) {

        // Validate unique Employee ID and Email in Firestore before creating
        validateUniqueEmployeeIdAndEmail(empId, email, () -> {
            showLoading(false);

            // Show confirmation dialog with final Login ID explicitly
            String confirmMessage = String.format(Locale.US,
                    "Teacher account will be created with Login ID: %s\n\nName: %s\nEmployee ID: %s\nDepartment: %s\nEmail: %s",
                    loginId, name, empId, department, email
            );

            new AlertDialog.Builder(this)
                    .setTitle("Create Teacher Account")
                    .setMessage(confirmMessage)
                    .setPositiveButton("CREATE ACCOUNT", (dialog, which) -> {
                        showLoading(true);
                        authService.createTeacherAccount(
                                AddTeacherActivity.this,
                                email,
                                password,
                                loginId,
                                name,
                                empId,
                                department,
                                departmentId,
                                departmentShortName,
                                finalProgramLevel,
                                phone,
                                designation,
                                qualification,
                                doj,
                                assignedSubjectIds,
                                assignedSubjectNames,
                                new AdminAuthService.OnUserCreatedListener() {
                                    @Override
                                    public void onSuccess(String uid, String message) {
                                        showLoading(false);

                                        // Sync local SQLite cache
                                        Teacher localTeacher = new Teacher(
                                                0,
                                                name,
                                                email,
                                                password,
                                                department,
                                                phone
                                        );
                                        localTeacher.setEmployeeId(empId);
                                        localTeacher.setDepartmentShortName(departmentShortName);
                                        localTeacher.setProgramLevel(finalProgramLevel);
                                        localTeacher.setDesignation(designation);
                                        localTeacher.setQualification(qualification);
                                        localTeacher.setDateOfJoining(doj);
                                        localTeacher.setUid(uid);
                                        localTeacher.setLoginId(loginId);
                                        localTeacher.setAssignedSubjectIds(assignedSubjectIds);
                                        localTeacher.setAssignedSubjectNames(assignedSubjectNames);
                                        try {
                                            dbHelper.addTeacher(localTeacher);
                                        } catch (Exception ignore) {}

                                        // Log Audit Activity in portalActivities
                                        activityLogger.logTeacherAccountCreated(
                                                uid != null && !uid.isEmpty() ? uid : empId,
                                                name,
                                                email,
                                                "[" + finalProgramLevel + "] " + department
                                        );

                                        // Log Subject Assignment Audit
                                        if (!assignedSubjectIds.isEmpty()) {
                                            activityLogger.logTeacherSubjectAssignment(
                                                    uid != null && !uid.isEmpty() ? uid : empId,
                                                    name,
                                                    "TEACHER_SUBJECT_ASSIGNED",
                                                    TextUtils.join(",", assignedSubjectIds),
                                                    TextUtils.join(", ", assignedSubjectNames)
                                            );
                                        }

                                        showSuccessDialog(name, empId, loginId, finalProgramLevel, departmentShortName.isEmpty() ? department : departmentShortName, assignedSubjectNames);
                                    }

                                    @Override
                                    public void onError(String errorMessage) {
                                        showLoading(false);
                                        showError(errorMessage);
                                    }
                                }
                        );
                    })
                    .setNegativeButton("CANCEL", null)
                    .show();
        });
    }

    private void validateUniqueEmployeeIdAndEmail(String empId, String email, Runnable onValid) {
        db.collection("teachers")
                .whereEqualTo("employeeId", empId)
                .get()
                .addOnSuccessListener(queryEmpSnapshots -> {
                    if (queryEmpSnapshots != null && !queryEmpSnapshots.isEmpty()) {
                        showLoading(false);
                        tilTeacherEmployeeId.setError("Employee ID already exists.");
                        etTeacherEmployeeId.requestFocus();
                        showError("Employee ID (" + empId + ") is already registered to another faculty member.");
                        return;
                    }

                    db.collection("users")
                            .whereEqualTo("email", email)
                            .get()
                            .addOnSuccessListener(queryEmailSnapshots -> {
                                if (queryEmailSnapshots != null && !queryEmailSnapshots.isEmpty()) {
                                    showLoading(false);
                                    tilTeacherEmail.setError("Email already registered.");
                                    etTeacherEmail.requestFocus();
                                    showError("This email address is already associated with an existing account.");
                                    return;
                                }

                                onValid.run();
                            })
                            .addOnFailureListener(e -> onValid.run());
                })
                .addOnFailureListener(e -> onValid.run());
    }

    private void showSuccessDialog(String teacherName, String empId, String loginId, String programLevel, String dept, List<String> assignedSubjects) {
        StringBuilder sb = new StringBuilder();
        sb.append("Faculty member ").append(teacherName).append(" has been successfully provisioned.\n\n");
        sb.append("Login ID: ").append(loginId).append("\n");
        sb.append("Employee ID: ").append(empId).append("\n");
        sb.append("Program: ").append(programLevel).append(" ").append(dept).append("\n\n");
        if (assignedSubjects != null && !assignedSubjects.isEmpty()) {
            sb.append("Assigned Subjects (").append(assignedSubjects.size()).append("):\n");
            for (String s : assignedSubjects) {
                sb.append("• ").append(s).append("\n");
            }
        } else {
            sb.append("No subjects currently assigned.");
        }

        new AlertDialog.Builder(this)
                .setTitle("Teacher Account Created")
                .setMessage(sb.toString().trim())
                .setPositiveButton("DONE", (dialog, which) -> {
                    setResult(RESULT_OK);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void showLoading(boolean isLoading) {
        layoutLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        layoutActions.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        btnCreateTeacher.setEnabled(!isLoading);
        btnCancel.setEnabled(!isLoading);

        etTeacherName.setEnabled(!isLoading);
        etTeacherPhone.setEnabled(!isLoading);
        etTeacherEmail.setEnabled(!isLoading);
        etTeacherEmployeeId.setEnabled(!isLoading);
        actvTeacherProgramLevel.setEnabled(!isLoading);
        actvTeacherDept.setEnabled(!isLoading);
        actvTeacherDesignation.setEnabled(!isLoading);
        etTeacherQualification.setEnabled(!isLoading);
        etTeacherDoj.setEnabled(!isLoading);
        etTeacherInitialPassword.setEnabled(!isLoading);
        etTeacherConfirmPassword.setEnabled(!isLoading);
    }

    private void showError(String message) {
        View coordinator = findViewById(R.id.coordinatorAddTeacher);
        if (coordinator != null) {
            Snackbar.make(coordinator, message, Snackbar.LENGTH_LONG)
                    .setBackgroundTint(Color.parseColor("#EF4444"))
                    .setTextColor(Color.WHITE)
                    .show();
        } else {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }
}
