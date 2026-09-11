package com.example;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adapter.ResultAdapter;
import com.example.database.DatabaseHelper;
import com.example.model.Result;
import com.example.model.ResultUpdateRequest;
import com.example.model.Student;
import com.example.model.SubjectGradeItem;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Teacher Activity for reviewing, publishing, and unpublishing student semester exam results.
 */
public class PublishResultsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;

    private ImageView btnBack;
    private AutoCompleteTextView actvSemester, actvDepartment;
    private EditText etSearchStudent;
    private ImageView btnClearSearch;

    private RecyclerView rvSelectStudents;
    private TextView tvNoStudentsFound;
    private StudentSearchAdapter studentAdapter;
    private List<Student> filteredStudents = new ArrayList<>();

    private MaterialCardView cardResultPreview;
    private TextView tvPreviewStudentName, tvPreviewStudentDetails, tvPublishedDate;
    private Chip chipResultStatus, chipResultVersion;
    private MaterialCardView cardRejectionReason, cardPendingNotice;
    private TextView tvRejectionReasonText, tvPendingNoticeTitle, tvPendingNoticeDesc, tvSyncStatusBanner;
    private RecyclerView rvPreviewSubjectResults;
    private ResultAdapter previewSubjectAdapter;
    private List<SubjectGradeItem> previewSubjectsList = new ArrayList<>();
    private TextView tvNoMarksWarning;

    private TextView tvPreviewTotalMarks, tvPreviewPercentage, tvPreviewSGPA, tvPreviewCGPA;
    private MaterialButton btnPublishResult, btnUnpublishResult, btnRequestUpdate;

    private Student selectedStudent = null;
    private int selectedSemester = 3;
    private String selectedDepartment = "Master of Computer Applications";
    private Result currentResult = null;
    private com.example.repository.ResultRepository resultRepository;
    private com.example.utils.SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish_results);

        dbHelper = new DatabaseHelper(this);

        resultRepository = com.example.repository.ResultRepository.getInstance(this);
        sessionManager = new com.example.utils.SessionManager(this);

        initViews();
        setupDropdowns();
        setupSearchAndList();
        setupListeners();
        loadStudentList();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackPublish);
        actvSemester = findViewById(R.id.actvSemester);
        actvDepartment = findViewById(R.id.actvDepartment);
        etSearchStudent = findViewById(R.id.etSearchStudent);
        btnClearSearch = findViewById(R.id.btnClearSearch);

        rvSelectStudents = findViewById(R.id.rvSelectStudents);
        tvNoStudentsFound = findViewById(R.id.tvNoStudentsFound);

        cardResultPreview = findViewById(R.id.cardResultPreview);
        tvPreviewStudentName = findViewById(R.id.tvPreviewStudentName);
        tvPreviewStudentDetails = findViewById(R.id.tvPreviewStudentDetails);
        tvPublishedDate = findViewById(R.id.tvPublishedDate);
        chipResultStatus = findViewById(R.id.chipResultStatus);
        chipResultVersion = findViewById(R.id.chipResultVersion);

        cardRejectionReason = findViewById(R.id.cardRejectionReason);
        tvRejectionReasonText = findViewById(R.id.tvRejectionReasonText);
        cardPendingNotice = findViewById(R.id.cardPendingNotice);
        tvPendingNoticeTitle = findViewById(R.id.tvPendingNoticeTitle);
        tvPendingNoticeDesc = findViewById(R.id.tvPendingNoticeDesc);
        tvSyncStatusBanner = findViewById(R.id.tvSyncStatusBanner);

        rvPreviewSubjectResults = findViewById(R.id.rvPreviewSubjectResults);
        tvNoMarksWarning = findViewById(R.id.tvNoMarksWarning);

        tvPreviewTotalMarks = findViewById(R.id.tvPreviewTotalMarks);
        tvPreviewPercentage = findViewById(R.id.tvPreviewPercentage);
        tvPreviewSGPA = findViewById(R.id.tvPreviewSGPA);
        tvPreviewCGPA = findViewById(R.id.tvPreviewCGPA);

        btnPublishResult = findViewById(R.id.btnPublishResult);
        btnUnpublishResult = findViewById(R.id.btnUnpublishResult);
        btnRequestUpdate = findViewById(R.id.btnRequestUpdate);

        rvPreviewSubjectResults.setLayoutManager(new LinearLayoutManager(this));
        previewSubjectAdapter = new ResultAdapter(previewSubjectsList);
        rvPreviewSubjectResults.setAdapter(previewSubjectAdapter);
    }

    private void setupDropdowns() {
        String[] semOptions = {"Semester I", "Semester II", "Semester III", "Semester IV", "Semester V", "Semester VI"};
        ArrayAdapter<String> semAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, semOptions);
        actvSemester.setAdapter(semAdapter);
        actvSemester.setText("Semester III", false);

        String[] deptOptions = {"Master of Computer Applications", "Computer Science", "Information Technology", "Electronics"};
        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, deptOptions);
        actvDepartment.setAdapter(deptAdapter);
        actvDepartment.setText("Master of Computer Applications", false);

        actvSemester.setOnItemClickListener((parent, view, position, id) -> {
            selectedSemester = position + 1;
            loadStudentList();
        });

        actvDepartment.setOnItemClickListener((parent, view, position, id) -> {
            selectedDepartment = (String) parent.getItemAtPosition(position);
            loadStudentList();
        });
    }

    private void setupSearchAndList() {
        rvSelectStudents.setLayoutManager(new LinearLayoutManager(this));
        studentAdapter = new StudentSearchAdapter();
        rvSelectStudents.setAdapter(studentAdapter);

        etSearchStudent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                btnClearSearch.setVisibility(query.length() > 0 ? View.VISIBLE : View.GONE);
                loadStudentList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> {
            etSearchStudent.setText("");
            loadStudentList();
        });
    }

    private void loadStudentList() {
        String query = etSearchStudent.getText() != null ? etSearchStudent.getText().toString().trim() : "";
        List<Student> allStudents = dbHelper.getAllStudents();
        filteredStudents.clear();

        for (Student s : allStudents) {
            boolean matchesDept = selectedDepartment.isEmpty() || (s.getDepartment() != null && s.getDepartment().equalsIgnoreCase(selectedDepartment));
            int studentSem = 0;
            if (s.getSemester() != null) {
                try {
                    String digits = s.getSemester().replaceAll("[^0-9]", "").trim();
                    if (!digits.isEmpty()) studentSem = Integer.parseInt(digits);
                } catch (Exception ignored) {}
            }
            boolean matchesSem = selectedSemester == 0 || studentSem == selectedSemester || (s.getSemester() != null && s.getSemester().contains(String.valueOf(selectedSemester)));
            boolean matchesQuery = query.isEmpty() ||
                    s.getName().toLowerCase().contains(query.toLowerCase()) ||
                    s.getRegNo().toLowerCase().contains(query.toLowerCase());

            if (matchesSem && matchesQuery) {
                filteredStudents.add(s);
            }
        }

        if (filteredStudents.isEmpty()) {
            tvNoStudentsFound.setVisibility(View.VISIBLE);
            rvSelectStudents.setVisibility(View.GONE);
        } else {
            tvNoStudentsFound.setVisibility(View.GONE);
            rvSelectStudents.setVisibility(View.VISIBLE);
        }

        studentAdapter.notifyDataSetChanged();

        if (selectedStudent != null) {
            boolean stillPresent = false;
            for (Student s : filteredStudents) {
                if (s.getId() == selectedStudent.getId()) {
                    stillPresent = true;
                    break;
                }
            }
            if (!stillPresent && !filteredStudents.isEmpty()) {
                selectStudent(filteredStudents.get(0));
            } else if (filteredStudents.isEmpty()) {
                clearPreviewCard();
            }
        } else if (!filteredStudents.isEmpty()) {
            selectStudent(filteredStudents.get(0));
        } else {
            clearPreviewCard();
        }
    }

    private void selectStudent(Student student) {
        this.selectedStudent = student;
        studentAdapter.notifyDataSetChanged();
        loadStudentResultPreview();
    }

    private void loadStudentResultPreview() {
        if (selectedStudent == null) {
            clearPreviewCard();
            return;
        }

        tvPreviewStudentName.setText(selectedStudent.getName());
        tvPreviewStudentDetails.setText("Reg No: " + selectedStudent.getRegNo() + " • " + selectedStudent.getDepartment() + " • " + selectedStudent.getProgramLevel() + " • Semester " + selectedSemester);

        previewSubjectsList = dbHelper.getStudentSubjectMarks(selectedStudent.getId());
        previewSubjectAdapter.updateData(previewSubjectsList);

        if (previewSubjectsList.isEmpty()) {
            tvNoMarksWarning.setVisibility(View.VISIBLE);
            rvPreviewSubjectResults.setVisibility(View.GONE);
            tvPreviewTotalMarks.setText("--");
            tvPreviewPercentage.setText("--");
            tvPreviewSGPA.setText("--");
            tvPreviewCGPA.setText("--");
            chipResultStatus.setText("NO MARKS");
            chipResultStatus.setChipBackgroundColorResource(R.color.error_light);
            chipResultStatus.setTextColor(getResources().getColor(R.color.error));
            chipResultVersion.setVisibility(View.GONE);
            cardRejectionReason.setVisibility(View.GONE);
            cardPendingNotice.setVisibility(View.GONE);
            btnPublishResult.setEnabled(false);
            btnRequestUpdate.setVisibility(View.GONE);
            btnUnpublishResult.setVisibility(View.GONE);
            return;
        }

        tvNoMarksWarning.setVisibility(View.GONE);
        rvPreviewSubjectResults.setVisibility(View.VISIBLE);

        double totalMarks = 0;
        int totalCredits = 0;
        double totalPoints = 0;
        double maxTotalMarks = 0;

        for (SubjectGradeItem item : previewSubjectsList) {
            totalMarks += item.getTotalMarks();
            totalCredits += item.getCredits();
            totalPoints += (item.getCredits() * item.getGradePoint());
            int itemMax = item.getMaxInternal() + item.getMaxExternal();
            maxTotalMarks += (itemMax > 0 ? itemMax : 100);
        }

        double sgpa = totalCredits > 0 ? (totalPoints / totalCredits) : 0.0;
        double cgpa = dbHelper.calculateCGPAWithNewSemester(selectedStudent.getId(), selectedSemester, sgpa, totalCredits);
        double percentage = maxTotalMarks > 0 ? (totalMarks / maxTotalMarks) * 100.0 : 0.0;

        tvPreviewTotalMarks.setText(String.format(Locale.US, "%.0f", totalMarks));
        tvPreviewPercentage.setText(String.format(Locale.US, "%.1f%%", percentage));
        tvPreviewSGPA.setText(String.format(Locale.US, "%.2f", sgpa));
        tvPreviewCGPA.setText(String.format(Locale.US, "%.2f", cgpa));

        // Network connectivity indicator
        if (!resultRepository.isNetworkAvailable()) {
            tvSyncStatusBanner.setVisibility(View.VISIBLE);
            tvSyncStatusBanner.setText("Offline Mode: Submissions will be cached locally.");
        } else {
            tvSyncStatusBanner.setVisibility(View.GONE);
        }

        // Fetch official result state from Firestore (with local fallback)
        resultRepository.fetchResult(selectedStudent.getId(), selectedSemester, new com.example.repository.ResultRepository.OnSingleResultLoadedListener() {
            @Override
            public void onLoaded(Result result) {
                currentResult = result;
                updateResultUI(result);
            }

            @Override
            public void onError(String errorMessage) {
                currentResult = dbHelper.getResultForStudentAndSemester(selectedStudent.getId(), selectedSemester);
                updateResultUI(currentResult);
            }
        });
    }

    private void updateResultUI(Result result) {
        if (result != null) {
            String st = result.getStatus() != null ? result.getStatus().toUpperCase(Locale.US) : "DRAFT";
            int ver = result.getVersion() > 0 ? result.getVersion() : 1;

            chipResultVersion.setText("v" + ver);
            chipResultVersion.setVisibility(View.VISIBLE);

            if (com.example.repository.ResultRepository.STATUS_APPROVED.equals(st) || "PUBLISHED".equals(st)) {
                chipResultStatus.setText("APPROVED");
                chipResultStatus.setChipBackgroundColorResource(R.color.primary_light);
                chipResultStatus.setTextColor(getResources().getColor(R.color.primary));
                tvPublishedDate.setText("Approved & Published: " + (result.getPublishedDate() != null ? result.getPublishedDate() : "Recently"));

                cardPendingNotice.setVisibility(View.GONE);
                cardRejectionReason.setVisibility(View.GONE);

                // IMPORTANT BUSINESS RULE:
                // Once APPROVED, Teacher CANNOT directly edit or publish over it!
                // Teacher must use "Request Result Update".
                btnPublishResult.setVisibility(View.GONE);
                btnRequestUpdate.setVisibility(View.VISIBLE);
                btnRequestUpdate.setEnabled(true);
                btnUnpublishResult.setVisibility(View.GONE);

            } else if (com.example.repository.ResultRepository.STATUS_PENDING_APPROVAL.equals(st)) {
                chipResultStatus.setText("PENDING APPROVAL");
                chipResultStatus.setChipBackgroundColorResource(R.color.warning_light);
                chipResultStatus.setTextColor(getResources().getColor(R.color.warning));
                tvPublishedDate.setText("Status: Pending Admin Approval (Invisible to Student)");

                cardPendingNotice.setVisibility(View.VISIBLE);
                tvPendingNoticeTitle.setText("⏳ Awaiting Admin Approval");
                tvPendingNoticeDesc.setText("This result has been submitted to the Admin for approval. Students cannot see this result until approved.");
                cardRejectionReason.setVisibility(View.GONE);

                btnPublishResult.setVisibility(View.VISIBLE);
                btnPublishResult.setEnabled(true);
                btnPublishResult.setText("Resubmit for Approval");
                btnRequestUpdate.setVisibility(View.GONE);
                btnUnpublishResult.setVisibility(View.GONE);

            } else if (com.example.repository.ResultRepository.STATUS_REJECTED.equals(st)) {
                chipResultStatus.setText("REJECTED");
                chipResultStatus.setChipBackgroundColorResource(R.color.error_light);
                chipResultStatus.setTextColor(getResources().getColor(R.color.error));
                tvPublishedDate.setText("Status: REJECTED by Admin");

                cardRejectionReason.setVisibility(View.VISIBLE);
                String reason = result.getRejectionReason();
                tvRejectionReasonText.setText("Reason: " + (reason != null && !reason.trim().isEmpty() ? reason : "Marks require review."));
                cardPendingNotice.setVisibility(View.GONE);

                btnPublishResult.setVisibility(View.VISIBLE);
                btnPublishResult.setEnabled(true);
                btnPublishResult.setText("Resubmit for Approval");
                btnRequestUpdate.setVisibility(View.GONE);
                btnUnpublishResult.setVisibility(View.GONE);

            } else {
                // DRAFT
                chipResultStatus.setText("DRAFT");
                chipResultStatus.setChipBackgroundColorResource(R.color.gray_200);
                chipResultStatus.setTextColor(getResources().getColor(R.color.text_secondary));
                tvPublishedDate.setText("Status: DRAFT (Not submitted)");
                chipResultVersion.setVisibility(View.GONE);

                cardPendingNotice.setVisibility(View.GONE);
                cardRejectionReason.setVisibility(View.GONE);

                btnPublishResult.setVisibility(View.VISIBLE);
                btnPublishResult.setEnabled(true);
                btnPublishResult.setText("Submit for Approval");
                btnRequestUpdate.setVisibility(View.GONE);
                btnUnpublishResult.setVisibility(View.GONE);
            }
        } else {
            chipResultStatus.setText("DRAFT");
            chipResultStatus.setChipBackgroundColorResource(R.color.gray_200);
            chipResultStatus.setTextColor(getResources().getColor(R.color.text_secondary));
            chipResultVersion.setVisibility(View.GONE);
            tvPublishedDate.setText("Status: DRAFT (Not submitted)");

            cardPendingNotice.setVisibility(View.GONE);
            cardRejectionReason.setVisibility(View.GONE);

            btnPublishResult.setVisibility(View.VISIBLE);
            btnPublishResult.setEnabled(true);
            btnPublishResult.setText("Submit for Approval");
            btnRequestUpdate.setVisibility(View.GONE);
            btnUnpublishResult.setVisibility(View.GONE);
        }
    }

    private void clearPreviewCard() {
        selectedStudent = null;
        tvPreviewStudentName.setText("Select a Student");
        tvPreviewStudentDetails.setText("Reg No: -- • Department • Semester --");
        previewSubjectsList.clear();
        previewSubjectAdapter.updateData(previewSubjectsList);
        tvPreviewTotalMarks.setText("--");
        tvPreviewPercentage.setText("--");
        tvPreviewSGPA.setText("--");
        tvPreviewCGPA.setText("--");
        chipResultStatus.setText("DRAFT");
        chipResultVersion.setVisibility(View.GONE);
        cardRejectionReason.setVisibility(View.GONE);
        cardPendingNotice.setVisibility(View.GONE);
        tvPublishedDate.setText("No student selected");
        btnPublishResult.setEnabled(false);
        btnRequestUpdate.setVisibility(View.GONE);
        btnUnpublishResult.setVisibility(View.GONE);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnPublishResult.setOnClickListener(v -> {
            if (selectedStudent == null) {
                Toast.makeText(this, "Please select a student first", Toast.LENGTH_SHORT).show();
                return;
            }

            if (previewSubjectsList.isEmpty()) {
                Toast.makeText(this, "Cannot submit: No marks recorded for this student", Toast.LENGTH_LONG).show();
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Submit Result for Approval?")
                    .setMessage("Are you sure you want to submit Semester " + selectedSemester + " results for " + selectedStudent.getName() + " (" + selectedStudent.getRegNo() + ") for Admin approval? The result will only become visible to the student after Admin approval.")
                    .setPositiveButton("Submit for Approval", (dialog, which) -> submitResultForApproval())
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnRequestUpdate.setOnClickListener(v -> showRequestResultUpdateDialog());
    }

    private String resolveStudentFirebaseUid(Student student) {
        if (student == null) return "";
        if (student.getFirebaseUid() != null && !student.getFirebaseUid().trim().isEmpty()) {
            return student.getFirebaseUid().trim();
        }
        String uidFromDb = dbHelper.getStudentFirebaseUid(student.getId());
        if (uidFromDb != null && !uidFromDb.trim().isEmpty()) {
            student.setFirebaseUid(uidFromDb.trim());
            return uidFromDb.trim();
        }
        String uidByReg = dbHelper.getStudentFirebaseUidByRegNo(student.getRegNo());
        if (uidByReg != null && !uidByReg.trim().isEmpty()) {
            student.setFirebaseUid(uidByReg.trim());
            return uidByReg.trim();
        }
        if (student.getStudentId() != null && !student.getStudentId().trim().isEmpty()) {
            return student.getStudentId().trim();
        }
        return String.valueOf(student.getId());
    }

    private void submitResultForApproval() {
        if (selectedStudent == null || previewSubjectsList.isEmpty()) return;

        double totalMarks = 0;
        int totalCredits = 0;
        double totalPoints = 0;
        double maxTotalMarks = 0;

        for (SubjectGradeItem item : previewSubjectsList) {
            totalMarks += item.getTotalMarks();
            totalCredits += item.getCredits();
            totalPoints += (item.getCredits() * item.getGradePoint());
            int itemMax = item.getMaxInternal() + item.getMaxExternal();
            maxTotalMarks += (itemMax > 0 ? itemMax : 100);
        }

        double sgpa = totalCredits > 0 ? (totalPoints / totalCredits) : 0.0;
        double cgpa = dbHelper.calculateCGPAWithNewSemester(selectedStudent.getId(), selectedSemester, sgpa, totalCredits);
        double percentage = maxTotalMarks > 0 ? (totalMarks / maxTotalMarks) * 100.0 : 0.0;

        String resolvedStudentUid = resolveStudentFirebaseUid(selectedStudent);

        String teacherUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid()
                : String.valueOf(sessionManager.getUserId());

        Result result = new Result();
        result.setStudentId(String.valueOf(selectedStudent.getId()));
        result.setStudentNumericId(selectedStudent.getId());
        result.setStudentUid(resolvedStudentUid);
        result.setSemester(selectedSemester);
        result.setResultId(selectedStudent.getId() + "_" + selectedSemester);
        result.setStudentName(selectedStudent.getName());
        result.setRegisterNo(selectedStudent.getRegNo());
        result.setProgramLevel(selectedStudent.getProgramLevel()); // Preserves UG / PG dynamically
        result.setDepartmentName(selectedStudent.getDepartment());
        result.setDepartmentId(selectedStudent.getDepartmentId());
        result.setAcademicYear("2025-2026");
        result.setSubjects(new ArrayList<>(previewSubjectsList));
        result.setTotalMarks(totalMarks);
        result.setPercentage(percentage);
        result.setSgpa(sgpa);
        result.setCgpa(cgpa);
        result.setVersion(currentResult != null && currentResult.getVersion() > 0 ? currentResult.getVersion() : 1);
        result.setTeacherId(teacherUid);
        result.setTeacherName(sessionManager.getUserName());
        result.setPublishedBy(sessionManager.getUserName());

        resultRepository.submitResultForApproval(result, new com.example.repository.ResultRepository.OnResultOperationListener() {
            @Override
            public void onSuccess(String message) {
                Snackbar.make(cardResultPreview, message, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(getResources().getColor(R.color.primary))
                        .setTextColor(getResources().getColor(R.color.white))
                        .show();
                loadStudentResultPreview();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(PublishResultsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                loadStudentResultPreview();
            }
        });
    }

    private void showRequestResultUpdateDialog() {
        if (selectedStudent == null || currentResult == null) return;

        double newTotalMarks = 0;
        int newCredits = 0;
        double newPoints = 0;
        double maxTotalMarks = 0;

        for (SubjectGradeItem item : previewSubjectsList) {
            newTotalMarks += item.getTotalMarks();
            newCredits += item.getCredits();
            newPoints += (item.getCredits() * item.getGradePoint());
            int itemMax = item.getMaxInternal() + item.getMaxExternal();
            maxTotalMarks += (itemMax > 0 ? itemMax : 100);
        }

        final double newSgpa = newCredits > 0 ? (newPoints / newCredits) : 0.0;
        final double newCgpa = dbHelper.calculateCGPAWithNewSemester(selectedStudent.getId(), selectedSemester, newSgpa, newCredits);
        final double newPct = maxTotalMarks > 0 ? (newTotalMarks / maxTotalMarks) * 100.0 : 0.0;
        final double finalNewTotalMarks = newTotalMarks;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_request_result_update, null);
        TextView tvDetails = dialogView.findViewById(R.id.tvDialogUpdateStudentDetails);
        TextView tvVerComp = dialogView.findViewById(R.id.tvDialogVersionComparison);
        TextView tvMarksComp = dialogView.findViewById(R.id.tvDialogMarksComparison);
        TextView tvSgpaComp = dialogView.findViewById(R.id.tvDialogSgpaComparison);
        EditText etReason = dialogView.findViewById(R.id.etUpdateReason);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnDialogCancelUpdate);
        MaterialButton btnSubmit = dialogView.findViewById(R.id.btnDialogSubmitUpdate);

        int oldVer = currentResult.getVersion();
        int nextVer = oldVer + 1;

        tvDetails.setText(selectedStudent.getName() + " • " + selectedStudent.getRegNo() + " • Semester " + selectedSemester + " (" + selectedStudent.getProgramLevel() + ")");
        tvVerComp.setText("Version: v" + oldVer + " → v" + nextVer);
        tvMarksComp.setText(String.format(Locale.US, "Marks: %.0f → %.0f", currentResult.getTotalMarks(), finalNewTotalMarks));
        tvSgpaComp.setText(String.format(Locale.US, "SGPA: %.2f → %.2f", currentResult.getSgpa(), newSgpa));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            String reason = etReason.getText() != null ? etReason.getText().toString().trim() : "";
            if (reason.isEmpty()) {
                etReason.setError("Please provide a valid reason for updating this result.");
                return;
            }

            String resolvedStudentUid = resolveStudentFirebaseUid(selectedStudent);

            String teacherUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                    ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : String.valueOf(sessionManager.getUserId());

            ResultUpdateRequest req = new ResultUpdateRequest();
            req.setResultId(currentResult.getResultId());
            req.setStudentId(String.valueOf(selectedStudent.getId()));
            req.setStudentNumericId(selectedStudent.getId());
            req.setStudentUid(resolvedStudentUid);
            req.setStudentName(selectedStudent.getName());
            req.setRegisterNo(selectedStudent.getRegNo());
            req.setProgramLevel(selectedStudent.getProgramLevel());
            req.setDepartmentId(selectedStudent.getDepartmentId());
            req.setDepartmentName(selectedStudent.getDepartment());
            req.setSemester(selectedSemester);
            req.setAcademicYear(currentResult.getAcademicYear());

            req.setOldVersion(oldVer);
            req.setNewVersion(nextVer);

            req.setOldMarks(currentResult.getTotalMarks());
            req.setNewMarks(finalNewTotalMarks);
            req.setOldPercentage(currentResult.getPercentage());
            req.setNewPercentage(newPct);
            req.setOldSgpa(currentResult.getSgpa());
            req.setNewSgpa(newSgpa);
            req.setOldCgpa(currentResult.getCgpa());
            req.setNewCgpa(newCgpa);

            req.setOldSubjects(currentResult.getSubjects());
            req.setNewSubjects(new ArrayList<>(previewSubjectsList));
            req.setReasonForUpdate(reason);
            req.setTeacherId(teacherUid);
            req.setTeacherName(sessionManager.getUserName());

            dialog.dismiss();

            resultRepository.submitResultUpdateRequest(req, new com.example.repository.ResultRepository.OnResultOperationListener() {
                @Override
                public void onSuccess(String message) {
                    Snackbar.make(cardResultPreview, message, Snackbar.LENGTH_LONG)
                            .setBackgroundTint(getResources().getColor(R.color.secondary))
                            .setTextColor(getResources().getColor(R.color.white))
                            .show();
                    loadStudentResultPreview();
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(PublishResultsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        });

        dialog.show();
    }

    // Inner Adapter for searching & selecting students
    private class StudentSearchAdapter extends RecyclerView.Adapter<StudentSearchAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_select, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Student s = filteredStudents.get(position);
            holder.tvName.setText(s.getName());
            holder.tvSub.setText(s.getRegNo() + " • " + s.getDepartment() + " • Sem " + s.getSemester());

            boolean isSelected = selectedStudent != null && selectedStudent.getId() == s.getId();
            holder.cardView.setStrokeColor(getResources().getColor(isSelected ? R.color.primary : R.color.border_color));
            holder.cardView.setStrokeWidth(isSelected ? 4 : 1);
            holder.ivStatus.setVisibility(isSelected ? View.VISIBLE : View.GONE);

            holder.itemView.setOnClickListener(v -> selectStudent(s));
        }

        @Override
        public int getItemCount() {
            return filteredStudents.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView cardView;
            TextView tvName, tvSub;
            ImageView ivStatus;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                cardView = (MaterialCardView) itemView;
                tvName = itemView.findViewById(R.id.tvStudentSelectName);
                tvSub = itemView.findViewById(R.id.tvStudentSelectSub);
                ivStatus = itemView.findViewById(R.id.ivSelectStatus);
            }
        }
    }
}
