package com.example;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.adapter.AssignmentAdapter;
import com.example.model.Assignment;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Assignment Analytics and Monitoring activity for System Admin.
 */
public class AdminAssignmentsActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvTotalCount;
    private TextView tvPublishedCount;
    private ProgressBar progressBar;
    private RecyclerView rvAssignments;

    private AssignmentAdapter adapter;
    private List<Assignment> assignmentList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_assignments);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupToolbar();
        setupWindowInsets();
        setupSwipeRefresh();
        setupRecyclerView();
        loadAssignments();
    }

    private void setupWindowInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.appBarAdminAssignments), (v, insets) -> {
            androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
            v.setPadding(0, statusBarInsets.top, 0, 0);
            return insets;
        });
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarAdminAssignments);
        swipeRefresh = findViewById(R.id.swipeRefreshAdminAssignments);
        tvTotalCount = findViewById(R.id.tvAdminTotalAssignments);
        tvPublishedCount = findViewById(R.id.tvAdminPublishedAssignments);
        progressBar = findViewById(R.id.progressBarAdminAssignments);
        rvAssignments = findViewById(R.id.rvAdminAssignmentsList);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Assignment Analytics");
            getSupportActionBar().setSubtitle("Monitor Assignments & Submissions");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(this::loadAssignments);
    }

    private void setupRecyclerView() {
        rvAssignments.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AssignmentAdapter(assignmentList, null);
        rvAssignments.setAdapter(adapter);
    }

    private void loadAssignments() {
        showLoading(true);
        db.collection("assignments").get().addOnSuccessListener(queryDocumentSnapshots -> {
            showLoading(false);
            swipeRefresh.setRefreshing(false);
            assignmentList.clear();

            int published = 0;
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                Assignment a = doc.toObject(Assignment.class);
                if (a != null) {
                    assignmentList.add(a);
                    if ("PUBLISHED".equalsIgnoreCase(a.getStatus())) {
                        published++;
                    }
                }
            }

            tvTotalCount.setText(String.valueOf(assignmentList.size()));
            tvPublishedCount.setText(String.valueOf(published));
            adapter.updateData(assignmentList);
        }).addOnFailureListener(e -> {
            showLoading(false);
            swipeRefresh.setRefreshing(false);
            adapter.updateData(assignmentList);
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
