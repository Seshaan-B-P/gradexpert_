package com.example;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.adapter.PortalActivityAdapter;
import com.example.model.PortalActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity displaying full portal activity audit log history with search, category/date filters, and real-time updates.
 */
public class PortalActivitiesActivity extends AppCompatActivity implements PortalActivityAdapter.OnPortalActivityClickListener {

    private static final String TAG = "PortalActivitiesActivity";

    private FirebaseFirestore db;
    private ListenerRegistration listenerRegistration;

    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;

    private TextInputEditText etSearch;
    private Spinner spCategory;
    private Spinner spDate;

    private ProgressBar progressBar;
    private LinearLayout layoutEmpty;
    private TextView tvEmptyMessage;
    private MaterialButton btnClearFilters;

    private RecyclerView rvActivities;
    private PortalActivityAdapter adapter;
    private List<PortalActivity> loadedActivities = new ArrayList<>();

    private String selectedCategory = "All Categories";
    private String selectedDate = "All Time";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portal_activities);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupToolbar();
        setupWindowInsets();
        setupSwipeRefresh();
        setupSearch();
        setupCategorySpinner();
        setupDateSpinner();
        setupRecyclerView();

        listenForPortalActivities();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    private void setupWindowInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbarPortalActivities), (v, insets) -> {
            androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
            v.setPadding(0, statusBarInsets.top, 0, 0);
            return insets;
        });
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarPortalActivities);
        swipeRefresh = findViewById(R.id.swipeRefreshPortalActivities);

        etSearch = findViewById(R.id.etSearchPortalActivities);
        spCategory = findViewById(R.id.spFilterActivityCategory);
        spDate = findViewById(R.id.spFilterActivityDate);

        progressBar = findViewById(R.id.progressBarPortalActivities);
        layoutEmpty = findViewById(R.id.layoutPortalActivityEmptyState);
        tvEmptyMessage = findViewById(R.id.tvPortalActivityEmptyMessage);
        btnClearFilters = findViewById(R.id.btnClearActivityFilters);

        rvActivities = findViewById(R.id.rvPortalActivitiesHistory);

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
        swipeRefresh.setOnRefreshListener(this::listenForPortalActivities);
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

    private void setupCategorySpinner() {
        List<String> categories = new ArrayList<>();
        categories.add("All Categories");
        categories.add("STUDENT");
        categories.add("SUBJECT");
        categories.add("ATTENDANCE");
        categories.add("ASSIGNMENT");
        categories.add("RESULT");
        categories.add("BROADCAST");
        categories.add("REPORT");
        categories.add("AUTHENTICATION");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(adapter);

        spCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = categories.get(position);
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupDateSpinner() {
        List<String> dates = new ArrayList<>();
        dates.add("All Time");
        dates.add("Today");
        dates.add("Yesterday");
        dates.add("Last 7 Days");
        dates.add("Last 30 Days");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, dates);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDate.setAdapter(adapter);

        spDate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDate = dates.get(position);
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupRecyclerView() {
        rvActivities.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PortalActivityAdapter(new ArrayList<>(), this);
        rvActivities.setAdapter(adapter);
    }

    private void listenForPortalActivities() {
        showLoading(true);
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        listenerRegistration = db.collection("portalActivities")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    showLoading(false);
                    swipeRefresh.setRefreshing(false);

                    if (e != null) {
                        Log.e(TAG, "Error listening for portal activities", e);
                        showEmptyState("Unable to load portal activities.");
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        loadedActivities = new ArrayList<>();
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            try {
                                PortalActivity pa = doc.toObject(PortalActivity.class);
                                if (pa != null) {
                                    if (pa.getActivityId() == null || pa.getActivityId().isEmpty()) {
                                        pa.setActivityId(doc.getId());
                                    }
                                    loadedActivities.add(pa);
                                }
                            } catch (Exception docEx) {
                                Log.e(TAG, "Error deserializing portal activity: " + doc.getId(), docEx);
                            }
                        }
                        adapter.updateData(loadedActivities);
                        applyFilters();
                    }
                });
    }

    private void applyFilters() {
        String query = etSearch.getText() != null ? etSearch.getText().toString() : "";
        if (adapter != null) {
            adapter.filter(query, selectedCategory, selectedDate);

            if (adapter.getItemCount() == 0) {
                showEmptyState("No portal activities found matching your search or filters.");
            } else {
                hideEmptyState();
            }
        }
    }

    private void resetFilters() {
        etSearch.setText("");
        spCategory.setSelection(0);
        spDate.setSelection(0);
        selectedCategory = "All Categories";
        selectedDate = "All Time";
        applyFilters();
    }

    @Override
    public void onActivityClick(PortalActivity activity) {
        Intent intent = new Intent(this, PortalActivityDetailsActivity.class);
        intent.putExtra("activityId", activity.getActivityId());
        intent.putExtra("title", activity.getTitle());
        intent.putExtra("description", activity.getDescription());
        intent.putExtra("type", activity.getType());
        intent.putExtra("entityType", activity.getEntityType());
        intent.putExtra("studentName", activity.getStudentName());
        intent.putExtra("registerNo", activity.getRegisterNo());
        intent.putExtra("subjectName", activity.getSubjectName());
        intent.putExtra("teacherName", activity.getTeacherName());

        if (activity.getTimestamp() != null) {
            intent.putExtra("timestampMs", activity.getTimestamp().toDate().getTime());
        }
        startActivity(intent);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showEmptyState(String message) {
        tvEmptyMessage.setText(message);
        layoutEmpty.setVisibility(View.VISIBLE);
        rvActivities.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        layoutEmpty.setVisibility(View.GONE);
        rvActivities.setVisibility(View.VISIBLE);
    }
}
