package com.example;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.adapter.BroadcastAlertAdapter;
import com.example.model.BroadcastAlert;
import com.example.repository.BroadcastRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for Teachers to view, filter, and manage FCM Broadcast Alerts in GradeXpert.
 */
public class BroadcastAlertsActivity extends AppCompatActivity implements BroadcastAlertAdapter.OnBroadcastAlertClickListener {

    private static final String TAG = "BroadcastAlertsActivity";

    private BroadcastRepository repository;
    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;

    private TextView tvTotalCount;
    private TextView tvPublishedCount;
    private TextView tvScheduledCount;
    private TextView tvDraftCount;

    private TextInputEditText etSearch;
    private Spinner spStatus;
    private Spinner spTarget;

    private ProgressBar progressBar;
    private LinearLayout layoutEmpty;
    private TextView tvEmptyMessage;
    private MaterialButton btnClearFilters;

    private RecyclerView rvAlerts;
    private FloatingActionButton fabCreateAlert;

    private BroadcastAlertAdapter adapter;
    private List<BroadcastAlert> loadedAlerts = new ArrayList<>();

    private String selectedStatus = "ALL";
    private String selectedTarget = "All Targets";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_broadcast_alerts);

        repository = BroadcastRepository.getInstance(this);

        initViews();
        setupToolbar();
        setupWindowInsets();
        setupSwipeRefresh();
        setupSearch();
        setupStatusSpinner();
        setupTargetSpinner();
        setupRecyclerView();
        setupFab();

        loadAlertData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAlertData();
    }

    private void setupWindowInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbarBroadcastAlerts), (v, insets) -> {
            androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
            v.setPadding(0, statusBarInsets.top, 0, 0);
            return insets;
        });
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarBroadcastAlerts);
        swipeRefresh = findViewById(R.id.swipeRefreshBroadcastAlerts);

        tvTotalCount = findViewById(R.id.tvSummaryAlertTotal);
        tvPublishedCount = findViewById(R.id.tvSummaryAlertPublished);
        tvScheduledCount = findViewById(R.id.tvSummaryAlertScheduled);
        tvDraftCount = findViewById(R.id.tvSummaryAlertDraft);

        etSearch = findViewById(R.id.etSearchBroadcastAlert);
        spStatus = findViewById(R.id.spFilterAlertStatus);
        spTarget = findViewById(R.id.spFilterAlertTarget);

        progressBar = findViewById(R.id.progressBarBroadcastAlerts);
        layoutEmpty = findViewById(R.id.layoutAlertEmptyState);
        tvEmptyMessage = findViewById(R.id.tvAlertEmptyMessage);
        btnClearFilters = findViewById(R.id.btnClearAlertFilters);

        rvAlerts = findViewById(R.id.rvBroadcastAlertsList);
        fabCreateAlert = findViewById(R.id.fabCreateAlert);

        btnClearFilters.setOnClickListener(v -> resetFilters());
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(this::loadAlertData);
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

    private void setupStatusSpinner() {
        List<String> statuses = new ArrayList<>();
        statuses.add("ALL");
        statuses.add("PUBLISHED");
        statuses.add("SCHEDULED");
        statuses.add("DRAFT");
        statuses.add("CANCELLED");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statuses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStatus.setAdapter(adapter);

        spStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedStatus = statuses.get(position);
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupTargetSpinner() {
        List<String> targets = new ArrayList<>();
        targets.add("All Targets");
        targets.add("ALL_STUDENTS");
        targets.add("DEPARTMENT");
        targets.add("DEPARTMENT_SEMESTER");
        targets.add("SPECIFIC_STUDENT");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, targets);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTarget.setAdapter(adapter);

        spTarget.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedTarget = targets.get(position);
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupRecyclerView() {
        rvAlerts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BroadcastAlertAdapter(new ArrayList<>(), this);
        rvAlerts.setAdapter(adapter);
    }

    private void setupFab() {
        fabCreateAlert.setOnClickListener(v -> {
            Intent intent = new Intent(BroadcastAlertsActivity.this, CreateBroadcastActivity.class);
            startActivity(intent);
        });
    }

    private void loadAlertData() {
        showLoading(true);
        repository.fetchAlerts(new BroadcastRepository.OnAlertsLoadedListener() {
            @Override
            public void onSuccess(List<BroadcastAlert> alerts) {
                showLoading(false);
                swipeRefresh.setRefreshing(false);
                loadedAlerts = alerts;
                updateSummaryCounts(alerts);
                adapter.updateData(alerts);
                applyFilters();
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(BroadcastAlertsActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSummaryCounts(List<BroadcastAlert> alerts) {
        int total = alerts.size();
        int published = 0;
        int scheduled = 0;
        int draft = 0;

        for (BroadcastAlert a : alerts) {
            if ("PUBLISHED".equalsIgnoreCase(a.getStatus())) published++;
            else if ("SCHEDULED".equalsIgnoreCase(a.getStatus())) scheduled++;
            else if ("DRAFT".equalsIgnoreCase(a.getStatus())) draft++;
        }

        tvTotalCount.setText(String.valueOf(total));
        tvPublishedCount.setText(String.valueOf(published));
        tvScheduledCount.setText(String.valueOf(scheduled));
        tvDraftCount.setText(String.valueOf(draft));
    }

    private void applyFilters() {
        String query = etSearch.getText() != null ? etSearch.getText().toString() : "";
        if (adapter != null) {
            adapter.filter(query, selectedStatus, selectedTarget);

            if (adapter.getItemCount() == 0) {
                showEmptyState("No broadcast alerts match your search or filters.");
            } else {
                hideEmptyState();
            }
        }
    }

    private void resetFilters() {
        etSearch.setText("");
        spStatus.setSelection(0);
        spTarget.setSelection(0);
        selectedStatus = "ALL";
        selectedTarget = "All Targets";
        applyFilters();
    }

    @Override
    public void onViewClick(BroadcastAlert alert) {
        Intent intent = new Intent(this, BroadcastDetailsActivity.class);
        intent.putExtra("alertId", alert.getAlertId());
        intent.putExtra("title", alert.getTitle());
        intent.putExtra("message", alert.getMessage());
        intent.putExtra("type", alert.getType());
        intent.putExtra("priority", alert.getPriority());
        intent.putExtra("targetType", alert.getTargetType());
        intent.putExtra("department", alert.getDepartment());
        intent.putExtra("semester", alert.getSemester());
        intent.putExtra("studentName", alert.getStudentName());
        intent.putExtra("status", alert.getStatus());
        intent.putExtra("recipientCount", alert.getRecipientCount());
        startActivity(intent);
    }

    @Override
    public void onActionClick(BroadcastAlert alert) {
        if ("SCHEDULED".equalsIgnoreCase(alert.getStatus())) {
            new AlertDialog.Builder(this)
                    .setTitle("Cancel Scheduled Alert?")
                    .setMessage("Are you sure you want to cancel scheduled broadcast \"" + alert.getTitle() + "\"? Push notification will not be sent.")
                    .setPositiveButton("CANCEL ALERT", (dialog, which) -> {
                        showLoading(true);
                        repository.cancelScheduledAlert(alert.getAlertId(), new BroadcastRepository.OnAlertOperationListener() {
                            @Override
                            public void onSuccess(String message) {
                                showLoading(false);
                                Toast.makeText(BroadcastAlertsActivity.this, "Scheduled alert cancelled.", Toast.LENGTH_SHORT).show();
                                loadAlertData();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                showLoading(false);
                                Toast.makeText(BroadcastAlertsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("KEEP SCHEDULED", null)
                    .show();
        } else if ("DRAFT".equalsIgnoreCase(alert.getStatus())) {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Draft Alert?")
                    .setMessage("Are you sure you want to delete draft \"" + alert.getTitle() + "\"?")
                    .setPositiveButton("DELETE", (dialog, which) -> {
                        showLoading(true);
                        repository.deleteDraftAlert(alert.getAlertId(), new BroadcastRepository.OnAlertOperationListener() {
                            @Override
                            public void onSuccess(String message) {
                                showLoading(false);
                                Toast.makeText(BroadcastAlertsActivity.this, "Draft deleted successfully.", Toast.LENGTH_SHORT).show();
                                loadAlertData();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                showLoading(false);
                                Toast.makeText(BroadcastAlertsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("CANCEL", null)
                    .show();
        } else {
            onViewClick(alert);
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showEmptyState(String message) {
        tvEmptyMessage.setText(message);
        layoutEmpty.setVisibility(View.VISIBLE);
        rvAlerts.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        layoutEmpty.setVisibility(View.GONE);
        rvAlerts.setVisibility(View.VISIBLE);
    }
}
