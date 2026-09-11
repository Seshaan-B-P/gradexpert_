package com.example;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.adapter.ResultAdapter;
import com.example.adapter.ResultUpdateRequestAdapter;
import com.example.adapter.SubjectDiffAdapter;
import com.example.model.Result;
import com.example.model.ResultUpdateRequest;
import com.example.repository.ResultRepository;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Result Control and Monitoring activity for System Admin.
 * Handles three tabs:
 * 1. Pending Approvals (Review, Approve, Reject new result submissions)
 * 2. Result Update Requests (Compare Old vs New, Approve Update, Reject Update)
 * 3. Approved Results (Audit view of all published/approved results)
 */
public class AdminResultsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ResultRepository resultRepository;

    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvPublishedCount;
    private TextView tvDraftCount;
    private ProgressBar progressBar;
    private TextView tvSectionHeader;
    private TextView tvEmptyState;
    private TabLayout tabLayout;

    private RecyclerView rvResults;
    private RecyclerView rvUpdateRequests;

    private ResultAdapter resultsAdapter;
    private ResultUpdateRequestAdapter updateRequestsAdapter;

    private List<Result> pendingResultsList = new ArrayList<>();
    private List<Result> approvedResultsList = new ArrayList<>();
    private List<ResultUpdateRequest> updateRequestsList = new ArrayList<>();

    private int currentTabPosition = 0; // 0: Pending, 1: Update Requests, 2: Approved Results

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_results);

        db = FirebaseFirestore.getInstance();
        resultRepository = ResultRepository.getInstance(this);

        initViews();
        setupToolbar();
        setupWindowInsets();
        setupTabs();
        setupSwipeRefresh();
        setupRecyclerViews();
        loadAllData();
    }

    private void setupWindowInsets() {
        View appBar = findViewById(R.id.appBarAdminResults);
        if (appBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, insets) -> {
                androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                v.setPadding(0, statusBarInsets.top, 0, 0);
                return insets;
            });
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarAdminResults);
        swipeRefresh = findViewById(R.id.swipeRefreshAdminResults);
        tvPublishedCount = findViewById(R.id.tvAdminPublishedResultsCount);
        tvDraftCount = findViewById(R.id.tvAdminDraftResultsCount);
        progressBar = findViewById(R.id.progressBarAdminResults);
        tvSectionHeader = findViewById(R.id.tvAdminSectionHeader);
        tvEmptyState = findViewById(R.id.tvAdminEmptyState);
        tabLayout = findViewById(R.id.tabLayoutAdminResults);
        rvResults = findViewById(R.id.rvAdminResultsList);
        rvUpdateRequests = findViewById(R.id.rvAdminUpdateRequestsList);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Result Control & Monitoring");
            getSupportActionBar().setSubtitle("Approval & Version Update Governance");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTabPosition = tab.getPosition();
                displayCurrentTabContent();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(this::loadAllData);
    }

    private void setupRecyclerViews() {
        rvResults.setLayoutManager(new LinearLayoutManager(this));
        resultsAdapter = new ResultAdapter(pendingResultsList, true, new ResultAdapter.OnAdminResultActionListener() {
            @Override
            public void onViewResult(Result result) {
                viewResultDetails(result);
            }

            @Override
            public void onApproveResult(Result result) {
                showApproveConfirmationDialog(result);
            }

            @Override
            public void onRejectResult(Result result) {
                showRejectDialog(result);
            }
        });
        rvResults.setAdapter(resultsAdapter);

        rvUpdateRequests.setLayoutManager(new LinearLayoutManager(this));
        updateRequestsAdapter = new ResultUpdateRequestAdapter(updateRequestsList, new ResultUpdateRequestAdapter.OnUpdateRequestActionListener() {
            @Override
            public void onCompare(ResultUpdateRequest request) {
                showDiffComparisonDialog(request);
            }

            @Override
            public void onApprove(ResultUpdateRequest request) {
                showApproveUpdateDialog(request);
            }

            @Override
            public void onReject(ResultUpdateRequest request) {
                showRejectUpdateDialog(request);
            }
        });
        rvUpdateRequests.setAdapter(updateRequestsAdapter);
    }

    private void loadAllData() {
        showLoading(true);

        // Fetch counts & lists
        resultRepository.fetchPendingResults(new ResultRepository.OnResultsLoadedListener() {
            @Override
            public void onLoaded(List<Result> results) {
                pendingResultsList.clear();
                if (results != null) pendingResultsList.addAll(results);
                updateSummaryBadges();
                if (currentTabPosition == 0) displayCurrentTabContent();
            }

            @Override
            public void onError(String errorMessage) {}
        });

        resultRepository.fetchPendingUpdateRequests(new ResultRepository.OnUpdateRequestsLoadedListener() {
            @Override
            public void onLoaded(List<ResultUpdateRequest> requests) {
                updateRequestsList.clear();
                if (requests != null) updateRequestsList.addAll(requests);
                updateSummaryBadges();
                if (currentTabPosition == 1) displayCurrentTabContent();
            }

            @Override
            public void onError(String errorMessage) {}
        });

        // Fetch Approved Results
        db.collection("results")
                .whereIn("status", java.util.Arrays.asList("APPROVED", "PUBLISHED"))
                .get()
                .addOnSuccessListener(snapshots -> {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);
                    approvedResultsList.clear();
                    for (DocumentSnapshot doc : snapshots) {
                        Result r = doc.toObject(Result.class);
                        if (r != null) approvedResultsList.add(r);
                    }
                    updateSummaryBadges();
                    if (currentTabPosition == 2) displayCurrentTabContent();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);
                });
    }

    private void updateSummaryBadges() {
        int approvedCount = approvedResultsList.size();
        int pendingCount = pendingResultsList.size() + updateRequestsList.size();

        tvPublishedCount.setText(String.valueOf(approvedCount));
        tvDraftCount.setText(String.valueOf(pendingCount));
    }

    private void displayCurrentTabContent() {
        showLoading(false);
        if (currentTabPosition == 0) {
            // Tab 0: Pending Approvals
            tvSectionHeader.setText("Pending Approval Queue (" + pendingResultsList.size() + ")");
            rvResults.setVisibility(View.VISIBLE);
            rvUpdateRequests.setVisibility(View.GONE);

            resultsAdapter.setShowAdminActions(true);
            resultsAdapter.updateData(pendingResultsList);

            if (pendingResultsList.isEmpty()) {
                tvEmptyState.setText("No results pending approval.");
                tvEmptyState.setVisibility(View.VISIBLE);
            } else {
                tvEmptyState.setVisibility(View.GONE);
            }
        } else if (currentTabPosition == 1) {
            // Tab 1: Update Requests
            tvSectionHeader.setText("Result Update Requests (" + updateRequestsList.size() + ")");
            rvResults.setVisibility(View.GONE);
            rvUpdateRequests.setVisibility(View.VISIBLE);

            updateRequestsAdapter.updateData(updateRequestsList);

            if (updateRequestsList.isEmpty()) {
                tvEmptyState.setText("No result update requests found.");
                tvEmptyState.setVisibility(View.VISIBLE);
            } else {
                tvEmptyState.setVisibility(View.GONE);
            }
        } else {
            // Tab 2: Approved Results
            tvSectionHeader.setText("Approved / Published Results (" + approvedResultsList.size() + ")");
            rvResults.setVisibility(View.VISIBLE);
            rvUpdateRequests.setVisibility(View.GONE);

            resultsAdapter.setShowAdminActions(false);
            resultsAdapter.updateData(approvedResultsList);

            if (approvedResultsList.isEmpty()) {
                tvEmptyState.setText("No approved results found in the system.");
                tvEmptyState.setVisibility(View.VISIBLE);
            } else {
                tvEmptyState.setVisibility(View.GONE);
            }
        }
    }

    // ==========================================
    // ACTION 1: APPROVE NEW RESULT
    // ==========================================
    private void showApproveConfirmationDialog(Result result) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_approve_result, null);
        EditText etNote = dialogView.findViewById(R.id.etApprovalNote);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Approve Semester Result")
                .setMessage("Are you sure you want to approve and publish the result for " + result.getStudentName() + "? This will make it officially visible to the student.")
                .setView(dialogView)
                .setPositiveButton("Approve Result", (dialog, which) -> {
                    String note = etNote != null ? etNote.getText().toString().trim() : "";
                    approveResult(result, note);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void approveResult(Result result, String note) {
        showLoading(true);
        String adminUid = FirebaseAuth.getInstance().getUid();
        resultRepository.approveResult(result, adminUid, "System Admin", note, new ResultRepository.OnResultOperationListener() {
            @Override
            public void onSuccess(String message) {
                showLoading(false);
                Toast.makeText(AdminResultsActivity.this, "Result approved successfully!", Toast.LENGTH_SHORT).show();
                loadAllData();
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                Toast.makeText(AdminResultsActivity.this, "Approval failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ==========================================
    // ACTION 2: REJECT NEW RESULT
    // ==========================================
    private void showRejectDialog(Result result) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reject_reason, null);
        EditText etReason = dialogView.findViewById(R.id.etRejectReason);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Reject Result Submission")
                .setMessage("Please enter the reason for rejecting this result. The faculty will be notified to correct and resubmit marks.")
                .setView(dialogView)
                .setPositiveButton("Reject Result", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String reason = etReason.getText().toString().trim();
                if (reason.isEmpty()) {
                    etReason.setError("Rejection reason is required");
                    return;
                }
                dialog.dismiss();
                rejectResult(result, reason);
            });
        });

        dialog.show();
    }

    private void rejectResult(Result result, String reason) {
        showLoading(true);
        String adminUid = FirebaseAuth.getInstance().getUid();
        resultRepository.rejectResult(result, adminUid, "System Admin", reason, new ResultRepository.OnResultOperationListener() {
            @Override
            public void onSuccess(String message) {
                showLoading(false);
                Toast.makeText(AdminResultsActivity.this, "Result rejected. Faculty notified.", Toast.LENGTH_SHORT).show();
                loadAllData();
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                Toast.makeText(AdminResultsActivity.this, "Failed to reject: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ==========================================
    // ACTION 3: COMPARE OLD VS NEW DIFF
    // ==========================================
    private void showDiffComparisonDialog(ResultUpdateRequest req) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_result_diff_comparison);

        TextView tvStudent = dialog.findViewById(R.id.tvDiffStudentInfo);
        Chip chipVersion = dialog.findViewById(R.id.chipDiffVersionBadge);
        TextView tvReason = dialog.findViewById(R.id.tvDiffReason);
        TextView tvTotalMarks = dialog.findViewById(R.id.tvDiffTotalMarks);
        TextView tvSgpa = dialog.findViewById(R.id.tvDiffSGPA);
        TextView tvCgpa = dialog.findViewById(R.id.tvDiffCGPA);
        RecyclerView rvDiff = dialog.findViewById(R.id.rvDiffSubjectsList);
        View btnClose = dialog.findViewById(R.id.btnDiffClose);

        String reg = req.getRegisterNo() != null ? req.getRegisterNo() : "";
        int sem = req.getSemester();
        tvStudent.setText((req.getStudentName() != null ? req.getStudentName() : "Student") + " • " + reg + " • Sem " + sem);

        int oldV = req.getOldVersion() > 0 ? req.getOldVersion() : 1;
        int newV = req.getNewVersion() > 0 ? req.getNewVersion() : (oldV + 1);
        chipVersion.setText("v" + oldV + " → v" + newV);

        tvReason.setText(req.getReasonForUpdate() != null ? req.getReasonForUpdate() : "No reason provided");

        tvTotalMarks.setText(String.format(Locale.US, "%.0f → %.0f", req.getOldMarks(), req.getNewMarks()));
        tvSgpa.setText(String.format(Locale.US, "%.2f → %.2f", req.getOldSgpa(), req.getNewSgpa()));
        tvCgpa.setText(String.format(Locale.US, "%.2f → %.2f", req.getOldCgpa(), req.getNewCgpa()));

        // Populate Subject Diff list by matching old vs new subjects
        List<SubjectDiffAdapter.SubjectDiffItem> diffItems = new ArrayList<>();
        List<com.example.model.SubjectGradeItem> oldSubs = req.getOldSubjects();
        List<com.example.model.SubjectGradeItem> newSubs = req.getNewSubjects();

        Map<String, com.example.model.SubjectGradeItem> oldSubMap = new java.util.HashMap<>();
        if (oldSubs != null) {
            for (com.example.model.SubjectGradeItem s : oldSubs) {
                if (s.getSubjectName() != null) oldSubMap.put(s.getSubjectName().trim(), s);
            }
        }

        if (newSubs != null) {
            for (com.example.model.SubjectGradeItem newS : newSubs) {
                String subName = newS.getSubjectName() != null ? newS.getSubjectName().trim() : "Subject";
                com.example.model.SubjectGradeItem oldS = oldSubMap.get(subName);

                double oMark = oldS != null ? oldS.getTotalMarks() : 0;
                String oGrade = oldS != null ? oldS.getGrade() : "-";

                double nMark = newS.getTotalMarks();
                String nGrade = newS.getGrade() != null ? newS.getGrade() : "-";

                diffItems.add(new SubjectDiffAdapter.SubjectDiffItem(subName, oMark, nMark, oGrade, nGrade));
            }
        }

        rvDiff.setLayoutManager(new LinearLayoutManager(this));
        rvDiff.setAdapter(new SubjectDiffAdapter(diffItems));

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.95), android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    // ==========================================
    // ACTION 4: APPROVE UPDATE REQUEST
    // ==========================================
    private void showApproveUpdateDialog(ResultUpdateRequest req) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Approve Result Revision")
                .setMessage("Are you sure you want to approve this result update for " + req.getStudentName() + "?\n\nThis will publish Version " + req.getNewVersion() + " to the student.")
                .setPositiveButton("Approve & Publish v" + req.getNewVersion(), (dialog, which) -> {
                    approveUpdateRequest(req);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void approveUpdateRequest(ResultUpdateRequest req) {
        showLoading(true);
        String adminUid = FirebaseAuth.getInstance().getUid();
        resultRepository.approveResultUpdate(req, adminUid, "System Admin", new ResultRepository.OnResultOperationListener() {
            @Override
            public void onSuccess(String message) {
                showLoading(false);
                Toast.makeText(AdminResultsActivity.this, "Result revision approved and published as v" + req.getNewVersion() + "!", Toast.LENGTH_SHORT).show();
                loadAllData();
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                Toast.makeText(AdminResultsActivity.this, "Failed to approve revision: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ==========================================
    // ACTION 5: REJECT UPDATE REQUEST
    // ==========================================
    private void showRejectUpdateDialog(ResultUpdateRequest req) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reject_reason, null);
        EditText etReason = dialogView.findViewById(R.id.etRejectReason);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Reject Result Revision")
                .setMessage("Provide the reason for rejecting this update request. The parent published result (v" + req.getOldVersion() + ") will remain unchanged.")
                .setView(dialogView)
                .setPositiveButton("Reject Update", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String reason = etReason.getText().toString().trim();
                if (reason.isEmpty()) {
                    etReason.setError("Rejection reason is required");
                    return;
                }
                dialog.dismiss();
                rejectUpdateRequest(req, reason);
            });
        });

        dialog.show();
    }

    private void rejectUpdateRequest(ResultUpdateRequest req, String reason) {
        showLoading(true);
        String adminUid = FirebaseAuth.getInstance().getUid();
        resultRepository.rejectResultUpdate(req, adminUid, "System Admin", reason, new ResultRepository.OnResultOperationListener() {
            @Override
            public void onSuccess(String message) {
                showLoading(false);
                Toast.makeText(AdminResultsActivity.this, "Update request rejected. Parent result remains active.", Toast.LENGTH_SHORT).show();
                loadAllData();
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                Toast.makeText(AdminResultsActivity.this, "Failed to reject update: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void viewResultDetails(Result result) {
        if (result == null) return;
        int numericId = result.getStudentNumericId() > 0 ? result.getStudentNumericId() : result.getStudentIdAsInt();
        Intent intent = new Intent(this, ResultsActivity.class);
        intent.putExtra("student_id", numericId);
        intent.putExtra("studentId", numericId);
        intent.putExtra("registerNo", result.getRegisterNo());
        intent.putExtra("studentUid", result.getStudentUid());
        intent.putExtra("semester", result.getSemester());
        startActivity(intent);
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}
