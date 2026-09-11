package com.example;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adapter.ResultAdapter;
import com.example.database.DatabaseHelper;
import com.example.model.Result;
import com.example.model.Student;
import com.example.model.SubjectGradeItem;
import com.example.utils.SessionManager;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Student Activity for viewing official published semester results.
 */
public class ResultsActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private DatabaseHelper dbHelper;
    private Student currentStudent;

    private ImageView btnBack;
    private AutoCompleteTextView actvStudentSemester;

    private LinearLayout layoutResultsContent;
    private TextView tvStudentResultSemTitle, tvStudentResultSubTitle;
    private Chip chipStudentResultStatus;
    private RecyclerView rvStudentSubjectResults;
    private ResultAdapter subjectAdapter;
    private List<SubjectGradeItem> subjectList = new ArrayList<>();

    private TextView tvStudentTotalMarks, tvStudentPercentage, tvStudentSGPA, tvStudentCGPA;
    private TextView tvStudentPublishedDate;

    private LinearLayout layoutEmptyStateResults;
    private int selectedSemester = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        sessionManager = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);

        initViews();
        loadStudentInfo();
        setupSemesterDropdown();
        setupListeners();
        loadPublishedResult();
    }

    private Chip chipStudentResultVersion;

    private void initViews() {
        btnBack = findViewById(R.id.btnBackResults);
        actvStudentSemester = findViewById(R.id.actvStudentSemester);

        layoutResultsContent = findViewById(R.id.layoutResultsContent);
        tvStudentResultSemTitle = findViewById(R.id.tvStudentResultSemTitle);
        tvStudentResultSubTitle = findViewById(R.id.tvStudentResultSubTitle);
        chipStudentResultStatus = findViewById(R.id.chipStudentResultStatus);
        chipStudentResultVersion = findViewById(R.id.chipStudentResultVersion);

        rvStudentSubjectResults = findViewById(R.id.rvStudentSubjectResults);
        tvStudentTotalMarks = findViewById(R.id.tvStudentTotalMarks);
        tvStudentPercentage = findViewById(R.id.tvStudentPercentage);
        tvStudentSGPA = findViewById(R.id.tvStudentSGPA);
        tvStudentCGPA = findViewById(R.id.tvStudentCGPA);
        tvStudentPublishedDate = findViewById(R.id.tvStudentPublishedDate);

        layoutEmptyStateResults = findViewById(R.id.layoutEmptyStateResults);

        rvStudentSubjectResults.setLayoutManager(new LinearLayoutManager(this));
        subjectAdapter = new ResultAdapter(subjectList);
        rvStudentSubjectResults.setAdapter(subjectAdapter);
    }

    private void loadStudentInfo() {
        Intent intent = getIntent();
        if (intent != null) {
            int passedId = intent.getIntExtra("student_id", intent.getIntExtra("studentId", 0));
            if (passedId > 0) {
                currentStudent = dbHelper.getStudentById(passedId);
            }
            String passedUid = intent.getStringExtra("studentUid");
            if (currentStudent == null && passedUid != null && !passedUid.isEmpty()) {
                currentStudent = dbHelper.getStudentByFirebaseUid(passedUid);
            }
            String passedReg = intent.getStringExtra("registerNo");
            if (currentStudent == null && passedReg != null && !passedReg.isEmpty()) {
                currentStudent = dbHelper.getStudentDetails(passedReg);
            }
            int passedSem = intent.getIntExtra("semester", 0);
            if (passedSem > 0) {
                selectedSemester = passedSem;
            }
        }

        if (currentStudent == null) {
            String identifier = sessionManager.getIdentifier();
            if (identifier != null && !identifier.isEmpty()) {
                currentStudent = dbHelper.getStudentDetails(identifier);
            }
        }
        if (currentStudent == null && sessionManager.getUserId() > 0) {
            currentStudent = dbHelper.getStudentById(sessionManager.getUserId());
        }
        if (currentStudent == null && com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
            currentStudent = dbHelper.getStudentByFirebaseUid(uid);
        }
        if (currentStudent != null && currentStudent.getSemester() != null && selectedSemester <= 0) {
            try {
                String digits = currentStudent.getSemester().replaceAll("[^0-9]", "").trim();
                if (!digits.isEmpty()) {
                    selectedSemester = Integer.parseInt(digits);
                }
            } catch (Exception ignored) {}
        }
    }

    private void setupSemesterDropdown() {
        String[] semOptions = {"Semester I", "Semester II", "Semester III", "Semester IV", "Semester V", "Semester VI", "Semester VII", "Semester VIII"};
        ArrayAdapter<String> semAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, semOptions);
        actvStudentSemester.setAdapter(semAdapter);

        int defaultIdx = Math.max(0, Math.min(selectedSemester - 1, semOptions.length - 1));
        actvStudentSemester.setText(semOptions[defaultIdx], false);

        actvStudentSemester.setOnItemClickListener((parent, view, position, id) -> {
            selectedSemester = position + 1;
            loadPublishedResult();
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadPublishedResult() {
        // Resolve student's Firebase Auth UID for cloud authorization
        String studentUid = null;
        String userRole = sessionManager.getUserRole();
        if ("STUDENT".equalsIgnoreCase(userRole) && com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
            studentUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else if (currentStudent != null && currentStudent.getFirebaseUid() != null && !currentStudent.getFirebaseUid().isEmpty()) {
            studentUid = currentStudent.getFirebaseUid();
        } else if (currentStudent != null && currentStudent.getStudentId() != null && !currentStudent.getStudentId().isEmpty()) {
            studentUid = currentStudent.getStudentId();
        } else if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
            studentUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // Verify local identity belongs to student
        int localNumericId = currentStudent != null ? currentStudent.getId() : sessionManager.getUserId();
        if (localNumericId <= 0 && studentUid != null) {
            com.example.model.Student matchedStudent = dbHelper.getStudentByFirebaseUid(studentUid);
            if (matchedStudent != null) {
                localNumericId = matchedStudent.getId();
                currentStudent = matchedStudent;
            }
        }

        final int targetLocalId = localNumericId;

        // First render local cached result ONLY if approved and belongs to current authenticated student
        Result localResult = targetLocalId > 0 ? dbHelper.getResultForStudentAndSemester(targetLocalId, selectedSemester) : null;
        if (localResult != null && ("APPROVED".equalsIgnoreCase(localResult.getStatus()) || "PUBLISHED".equalsIgnoreCase(localResult.getStatus()))) {
            displayApprovedResult(localResult, targetLocalId);
        } else {
            layoutResultsContent.setVisibility(View.GONE);
            layoutEmptyStateResults.setVisibility(View.VISIBLE);
        }

        // Securely query Firestore using the authenticated Firebase UID
        if (studentUid != null && !studentUid.isEmpty()) {
            com.example.repository.ResultRepository.getInstance(this).fetchStudentApprovedResult(studentUid, selectedSemester, new com.example.repository.ResultRepository.OnSingleResultLoadedListener() {
                @Override
                public void onLoaded(Result cloudResult) {
                    if (cloudResult != null && ("APPROVED".equalsIgnoreCase(cloudResult.getStatus()) || "PUBLISHED".equalsIgnoreCase(cloudResult.getStatus()))) {
                        displayApprovedResult(cloudResult, targetLocalId);
                    } else {
                        layoutResultsContent.setVisibility(View.GONE);
                        layoutEmptyStateResults.setVisibility(View.VISIBLE);
                        if (targetLocalId > 0) {
                            dbHelper.deleteResult(targetLocalId, selectedSemester);
                        }
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    // If cloud query failed (e.g. offline), localResult display remains active
                }
            });
        }
    }

    private void displayApprovedResult(Result result, int studentId) {
        layoutResultsContent.setVisibility(View.VISIBLE);
        layoutEmptyStateResults.setVisibility(View.GONE);

        tvStudentResultSemTitle.setText("SEMESTER " + getRomanSemester(selectedSemester));
        tvStudentResultSubTitle.setText("Official Grade Card • " + (currentStudent != null ? currentStudent.getDepartment() : "Computer Science"));
        chipStudentResultStatus.setText("Approved");

        int version = result.getVersion() > 0 ? result.getVersion() : 1;
        if (version > 1) {
            chipStudentResultVersion.setText("Updated Result • v" + version);
            chipStudentResultVersion.setVisibility(View.VISIBLE);
        } else {
            chipStudentResultVersion.setVisibility(View.GONE);
        }

        if (result.getSubjects() != null && !result.getSubjects().isEmpty()) {
            subjectList = new ArrayList<>(result.getSubjects());
            subjectAdapter.updateData(subjectList);
        } else {
            subjectList = dbHelper.getStudentSubjectMarks(studentId);
            subjectAdapter.updateData(subjectList);
        }

        tvStudentTotalMarks.setText(String.format(Locale.US, "%.0f", result.getTotalMarks()));
        tvStudentPercentage.setText(String.format(Locale.US, "%.1f%%", result.getPercentage()));
        tvStudentSGPA.setText(String.format(Locale.US, "%.2f", result.getSgpa()));
        tvStudentCGPA.setText(String.format(Locale.US, "%.2f", result.getCgpa()));

        String pubDate = result.getApprovedAtString() != null ? result.getApprovedAtString() : result.getPublishedDate();
        tvStudentPublishedDate.setText("Approved on: " + (pubDate != null ? pubDate : "Recently"));

        layoutResultsContent.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(ResultsActivity.this, ResultDetailsActivity.class);
            intent.putExtra("student_id", studentId);
            intent.putExtra("semester", selectedSemester);
            startActivity(intent);
        });
    }

    private String getRomanSemester(int sem) {
        switch (sem) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            case 6: return "VI";
            case 7: return "VII";
            case 8: return "VIII";
            default: return String.valueOf(sem);
        }
    }
}
