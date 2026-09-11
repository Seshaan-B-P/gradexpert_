package com.example;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.adapter.PasswordResetRequestAdapter;
import com.example.model.PasswordResetRequest;
import com.example.repository.PasswordResetRepository;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for System Admin to browse, filter, search, and review password reset requests.
 */
public class PasswordResetRequestsActivity extends AppCompatActivity implements PasswordResetRequestAdapter.OnRequestClickListener {

    private PasswordResetRepository repository;

    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupStatus;
    private ChipGroup chipGroupRole;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private RecyclerView rvRequests;

    private PasswordResetRequestAdapter adapter;
    private List<PasswordResetRequest> masterList = new ArrayList<>();
    private List<PasswordResetRequest> filteredList = new ArrayList<>();

    private String selectedStatusFilter = "PENDING";
    private String selectedRoleFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_reset_requests);

        repository = PasswordResetRepository.getInstance(this);

        initViews();
        setupToolbar();
        setupWindowInsets();
        setupSwipeRefresh();
        setupStatusTabs();
        setupRoleChips();
        setupRecyclerView();
        setupSearch();
        loadRequests();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRequests();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarPasswordResetRequests);
        swipeRefresh = findViewById(R.id.swipeRefreshRequests);
        etSearch = findViewById(R.id.etSearchRequests);
        chipGroupStatus = findViewById(R.id.chipGroupRequestStatus);
        chipGroupRole = findViewById(R.id.chipGroupRequestRole);
        progressBar = findViewById(R.id.progressBarRequests);
        tvEmpty = findViewById(R.id.tvRequestsEmpty);
        rvRequests = findViewById(R.id.rvRequestsList);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Password Reset Requests");
            getSupportActionBar().setSubtitle("Review & Authorize Password Requests");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupWindowInsets() {
        View appBar = findViewById(R.id.appBarPasswordResetRequests);
        if (appBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, insets) -> {
                androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                v.setPadding(0, statusBarInsets.top, 0, 0);
                return insets;
            });
        }
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(this::loadRequests);
    }

    private void setupStatusTabs() {
        chipGroupStatus.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipTabCompleted) {
                selectedStatusFilter = "COMPLETED";
            } else if (checkedId == R.id.chipTabRejected) {
                selectedStatusFilter = "REJECTED";
            } else if (checkedId == R.id.chipTabAll) {
                selectedStatusFilter = "ALL";
            } else {
                selectedStatusFilter = "PENDING";
            }
            applyFilters();
        });
    }

    private void setupRoleChips() {
        chipGroupRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipRoleFilterStudent) {
                selectedRoleFilter = "STUDENT";
            } else if (checkedId == R.id.chipRoleFilterTeacher) {
                selectedRoleFilter = "TEACHER";
            } else {
                selectedRoleFilter = "ALL";
            }
            applyFilters();
        });
    }

    private void setupRecyclerView() {
        rvRequests.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PasswordResetRequestAdapter(filteredList, this);
        rvRequests.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadRequests() {
        showLoading(true);
        repository.fetchRequests("ALL", new PasswordResetRepository.OnRequestsLoadedListener() {
            @Override
            public void onSuccess(List<PasswordResetRequest> requests) {
                showLoading(false);
                swipeRefresh.setRefreshing(false);
                masterList = requests;
                applyFilters();
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(PasswordResetRequestsActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilters() {
        String query = etSearch.getText() != null ? etSearch.getText().toString().trim().toLowerCase() : "";
        filteredList.clear();

        for (PasswordResetRequest req : masterList) {
            boolean matchesStatus = "ALL".equalsIgnoreCase(selectedStatusFilter) || selectedStatusFilter.equalsIgnoreCase(req.getStatus());
            boolean matchesRole = "ALL".equalsIgnoreCase(selectedRoleFilter) || selectedRoleFilter.equalsIgnoreCase(req.getRole());
            boolean matchesQuery = query.isEmpty() ||
                    (req.getUserName() != null && req.getUserName().toLowerCase().contains(query)) ||
                    (req.getEmail() != null && req.getEmail().toLowerCase().contains(query)) ||
                    (req.getIdentifier() != null && req.getIdentifier().toLowerCase().contains(query));

            if (matchesStatus && matchesRole && matchesQuery) {
                filteredList.add(req);
            }
        }

        adapter.updateData(filteredList);

        if (filteredList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvRequests.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvRequests.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onReviewClick(PasswordResetRequest request) {
        if (request == null) return;

        Intent intent = new Intent(this, PasswordResetRequestDetailsActivity.class);
        intent.putExtra("extra_request_id", request.getRequestId());
        intent.putExtra("extra_user_id", request.getUserId());
        intent.putExtra("extra_user_name", request.getUserName());
        intent.putExtra("extra_user_email", request.getEmail());
        intent.putExtra("extra_user_role", request.getRole());
        intent.putExtra("extra_user_identifier", request.getIdentifier());
        intent.putExtra("extra_status", request.getStatus());
        intent.putExtra("extra_admin_note", request.getAdminNote());
        startActivity(intent);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
