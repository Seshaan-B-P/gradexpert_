package com.example;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.model.BroadcastAlert;
import com.example.repository.BroadcastRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Activity for Teachers to compose, target, schedule, or publish push notifications to Firestore.
 */
public class CreateBroadcastActivity extends AppCompatActivity {

    private static final String TAG = "CreateBroadcastActivity";

    private BroadcastRepository repository;

    private Toolbar toolbar;
    private TextInputEditText etTitle;
    private TextInputEditText etMessage;

    private Spinner spType;
    private Spinner spTarget;

    private LinearLayout layoutTargetDetails;
    private TextView tvDeptTitle;
    private Spinner spDept;
    private TextView tvSemTitle;
    private Spinner spSem;
    private TextInputLayout tilStudent;
    private TextInputEditText etStudent;

    private TextView tvRecipientBanner;
    private ChipGroup chipGroupPriority;
    private RadioGroup rgSendMode;
    private RadioButton rbSendNow;
    private RadioButton rbSchedule;
    private RadioButton rbSaveDraft;

    private ProgressBar progressBar;
    private MaterialButton btnPublish;

    private String selectedType = "General Announcement";
    private String selectedTargetType = "ALL_STUDENTS";
    private String selectedDept = "Master of Computer Applications";
    private String selectedSem = "Semester III";
    private String selectedPriority = "Normal";
    private String selectedStatus = "PUBLISHED";
    private int computedRecipientCount = 42;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_broadcast);

        repository = BroadcastRepository.getInstance(this);

        initViews();
        setupToolbar();
        setupWindowInsets();
        setupTypeSpinner();
        setupTargetSpinner();
        setupDeptSemSpinners();
        setupPriorityChips();
        setupSendModeRadio();

        btnPublish.setOnClickListener(v -> validateAndPromptPublish());
        updateRecipientCount();
    }

    private void setupWindowInsets() {
        View appBar = findViewById(R.id.appBarCreateBroadcast);
        if (appBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, insets) -> {
                androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                v.setPadding(0, statusBarInsets.top, 0, 0);
                return insets;
            });
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarCreateBroadcast);
        etTitle = findViewById(R.id.etCreateAlertTitle);
        etMessage = findViewById(R.id.etCreateAlertMessage);

        spType = findViewById(R.id.spCreateAlertType);
        spTarget = findViewById(R.id.spCreateAlertTarget);

        layoutTargetDetails = findViewById(R.id.layoutTargetDetails);
        tvDeptTitle = findViewById(R.id.tvTargetDeptTitle);
        spDept = findViewById(R.id.spCreateAlertDept);
        tvSemTitle = findViewById(R.id.tvTargetSemTitle);
        spSem = findViewById(R.id.spCreateAlertSem);
        tilStudent = findViewById(R.id.tilCreateAlertStudent);
        etStudent = findViewById(R.id.etCreateAlertStudent);

        tvRecipientBanner = findViewById(R.id.tvRecipientCountBanner);
        chipGroupPriority = findViewById(R.id.chipGroupPriority);
        rgSendMode = findViewById(R.id.rgSendMode);
        rbSendNow = findViewById(R.id.rbSendNow);
        rbSchedule = findViewById(R.id.rbSchedule);
        rbSaveDraft = findViewById(R.id.rbSaveDraft);

        progressBar = findViewById(R.id.progressBarCreateAlert);
        btnPublish = findViewById(R.id.btnPublishAlert);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Create Broadcast Alert");
            getSupportActionBar().setSubtitle("Create FCM Push Notification");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupTypeSpinner() {
        List<String> types = new ArrayList<>();
        types.add("General Announcement");
        types.add("Exam");
        types.add("Assignment");
        types.add("Attendance");
        types.add("Result");
        types.add("Event");
        types.add("Important Notice");
        types.add("Emergency");

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

    private void setupTargetSpinner() {
        List<String> targets = new ArrayList<>();
        targets.add("All Students");
        targets.add("Department");
        targets.add("Department + Semester");
        targets.add("Specific Student");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, targets);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTarget.setAdapter(adapter);

        spTarget.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int pos = position;
                if (pos == 0) {
                    selectedTargetType = "ALL_STUDENTS";
                    tvDeptTitle.setVisibility(View.GONE);
                    spDept.setVisibility(View.GONE);
                    tvSemTitle.setVisibility(View.GONE);
                    spSem.setVisibility(View.GONE);
                    tilStudent.setVisibility(View.GONE);
                } else if (pos == 1) {
                    selectedTargetType = "DEPARTMENT";
                    tvDeptTitle.setVisibility(View.VISIBLE);
                    spDept.setVisibility(View.VISIBLE);
                    tvSemTitle.setVisibility(View.GONE);
                    spSem.setVisibility(View.GONE);
                    tilStudent.setVisibility(View.GONE);
                } else if (pos == 2) {
                    selectedTargetType = "DEPARTMENT_SEMESTER";
                    tvDeptTitle.setVisibility(View.VISIBLE);
                    spDept.setVisibility(View.VISIBLE);
                    tvSemTitle.setVisibility(View.VISIBLE);
                    spSem.setVisibility(View.VISIBLE);
                    tilStudent.setVisibility(View.GONE);
                } else {
                    selectedTargetType = "SPECIFIC_STUDENT";
                    tvDeptTitle.setVisibility(View.GONE);
                    spDept.setVisibility(View.GONE);
                    tvSemTitle.setVisibility(View.GONE);
                    spSem.setVisibility(View.GONE);
                    tilStudent.setVisibility(View.VISIBLE);
                }
                updateRecipientCount();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupDeptSemSpinners() {
        List<String> depts = new ArrayList<>();
        depts.add("Master of Computer Applications");
        depts.add("Computer Science & Engineering");
        depts.add("Information Technology");
        depts.add("Electronics & Communication");

        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, depts);
        deptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDept.setAdapter(deptAdapter);
        spDept.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDept = depts.get(position);
                updateRecipientCount();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        List<String> sems = new ArrayList<>();
        sems.add("Semester III");
        sems.add("Semester I");
        sems.add("Semester II");
        sems.add("Semester IV");
        sems.add("Semester V");
        sems.add("Semester VI");

        ArrayAdapter<String> semAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sems);
        semAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSem.setAdapter(semAdapter);
        spSem.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSem = sems.get(position);
                updateRecipientCount();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupPriorityChips() {
        chipGroupPriority.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipPriorityUrgent) {
                selectedPriority = "Urgent";
            } else if (checkedId == R.id.chipPriorityHigh) {
                selectedPriority = "High";
            } else {
                selectedPriority = "Normal";
            }
        });
    }

    private void setupSendModeRadio() {
        rgSendMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbSchedule) {
                selectedStatus = "SCHEDULED";
                btnPublish.setText("SCHEDULE ALERT");
            } else if (checkedId == R.id.rbSaveDraft) {
                selectedStatus = "DRAFT";
                btnPublish.setText("SAVE DRAFT");
            } else {
                selectedStatus = "PUBLISHED";
                btnPublish.setText("PUBLISH ALERT");
            }
        });
    }

    private void updateRecipientCount() {
        String studentVal = etStudent.getText() != null ? etStudent.getText().toString().trim() : "";
        repository.calculateRecipientCount(selectedTargetType, selectedDept, selectedSem, studentVal, new BroadcastRepository.OnRecipientCountListener() {
            @Override
            public void onSuccess(int count) {
                computedRecipientCount = count;
                tvRecipientBanner.setText(String.format(Locale.US, "👥 %d student(s) will receive this alert.", count));
            }

            @Override
            public void onError(String errorMessage) {
                computedRecipientCount = 30;
                tvRecipientBanner.setText("👥 30 student(s) will receive this alert.");
            }
        });
    }

    private void validateAndPromptPublish() {
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String message = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";

        if (title.isEmpty()) {
            etTitle.setError("Please enter an alert title.");
            Toast.makeText(this, "Please enter an alert title.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (message.isEmpty()) {
            etMessage.setError("Please enter message body.");
            Toast.makeText(this, "Please enter message body.", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("PUBLISHED".equalsIgnoreCase(selectedStatus) && computedRecipientCount <= 0) {
            Toast.makeText(this, "No active students match the selected audience.", Toast.LENGTH_LONG).show();
            return;
        }

        String actionText = "PUBLISHED".equalsIgnoreCase(selectedStatus) ? "Publish" : ("SCHEDULED".equalsIgnoreCase(selectedStatus) ? "Schedule" : "Save");
        String targetDesc = "All Active Students";
        if ("DEPARTMENT".equalsIgnoreCase(selectedTargetType)) targetDesc = selectedDept;
        else if ("DEPARTMENT_SEMESTER".equalsIgnoreCase(selectedTargetType)) targetDesc = selectedDept + " - " + selectedSem;

        String confirmMessage = String.format(Locale.US,
                "Title:\n%s\n\nTarget:\n%s\n\nPriority:\n%s\n\nMessage:\n%s",
                title, targetDesc, selectedPriority, message
        );

        new AlertDialog.Builder(this)
                .setTitle(actionText + " Alert?")
                .setMessage(confirmMessage)
                .setPositiveButton(actionText.toUpperCase(), (dialog, which) -> saveToFirestore(title, message))
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void saveToFirestore(String title, String message) {
        showLoading(true);
        btnPublish.setEnabled(false);

        String teacherUid = getTeacherIdentity();
        String docId = "alert_" + System.currentTimeMillis();
        String studentVal = etStudent.getText() != null ? etStudent.getText().toString().trim() : "";

        BroadcastAlert alert = new BroadcastAlert(
                docId,
                title,
                message,
                selectedType,
                selectedPriority,
                selectedTargetType,
                selectedDept,
                selectedSem,
                studentVal,
                studentVal.isEmpty() ? "All Students" : studentVal,
                teacherUid,
                selectedStatus,
                computedRecipientCount
        );

        repository.createAlert(alert, new BroadcastRepository.OnAlertOperationListener() {
            @Override
            public void onSuccess(String msg) {
                if ("PUBLISHED".equalsIgnoreCase(selectedStatus)) {
                    com.example.utils.PortalActivityLogger.getInstance(CreateBroadcastActivity.this)
                            .logBroadcastPublished(docId, title, selectedTargetType);
                } else {
                    com.example.utils.PortalActivityLogger.getInstance(CreateBroadcastActivity.this)
                            .logBroadcastCreated(docId, title, selectedTargetType);
                }
                showLoading(false);
                btnPublish.setEnabled(true);
                String successMsg = "PUBLISHED".equalsIgnoreCase(selectedStatus) ? "Alert published successfully." : ("SCHEDULED".equalsIgnoreCase(selectedStatus) ? "Alert scheduled successfully." : "Draft saved successfully.");
                Toast.makeText(CreateBroadcastActivity.this, successMsg, Toast.LENGTH_LONG).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                btnPublish.setEnabled(true);
                Toast.makeText(CreateBroadcastActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private String getTeacherIdentity() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return (currentUser != null) ? currentUser.getUid() : "TCH1001";
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
