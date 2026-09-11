package com.example;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.adapter.SemesterAdapter;
import com.example.model.Semester;
import com.example.utils.PortalActivityLogger;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

/**
 * Manage Semesters activity for System Admin in GradeXpert.
 */
public class ManageSemestersActivity extends AppCompatActivity implements SemesterAdapter.OnSemesterClickListener {

    private FirebaseFirestore db;

    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private RecyclerView rvSemesters;
    private FloatingActionButton fabAdd;

    private SemesterAdapter adapter;
    private List<Semester> masterList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_semesters);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupToolbar();
        setupWindowInsets();
        setupSwipeRefresh();
        setupRecyclerView();
        loadSemesters();
    }

    private void setupWindowInsets() {
        View appBar = findViewById(R.id.appBarSemesters);
        if (appBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, insets) -> {
                androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                v.setPadding(0, statusBarInsets.top, 0, 0);
                return insets;
            });
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarSemesters);
        swipeRefresh = findViewById(R.id.swipeRefreshSemesters);
        progressBar = findViewById(R.id.progressBarSemesters);
        rvSemesters = findViewById(R.id.rvSemestersList);
        fabAdd = findViewById(R.id.fabAddSemester);

        fabAdd.setOnClickListener(v -> showAddSemesterDialog());
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Manage Semesters");
            getSupportActionBar().setSubtitle("Academic Term Configuration");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(this::loadSemesters);
    }

    private void setupRecyclerView() {
        rvSemesters.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SemesterAdapter(masterList, this);
        rvSemesters.setAdapter(adapter);
    }

    private void loadSemesters() {
        showLoading(true);
        db.collection("semesters").get().addOnSuccessListener(queryDocumentSnapshots -> {
            showLoading(false);
            swipeRefresh.setRefreshing(false);
            masterList.clear();

            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                Semester sem = doc.toObject(Semester.class);
                if (sem != null) {
                    sem.setSemesterId(doc.getId());
                    masterList.add(sem);
                }
            }

            adapter.updateData(masterList);
        }).addOnFailureListener(e -> {
            showLoading(false);
            swipeRefresh.setRefreshing(false);
            adapter.updateData(masterList);
        });
    }

    @Override
    public void onToggleStatus(Semester semester) {
        if (semester == null) return;
        String newStatus = "ACTIVE".equalsIgnoreCase(semester.getStatus()) ? "INACTIVE" : "ACTIVE";
        semester.setStatus(newStatus);

        showLoading(true);
        db.collection("semesters").document(semester.getSemesterId()).update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    PortalActivityLogger.getInstance(this).logTeacherActivity(
                            "System Admin",
                            "Toggled Semester Status: " + semester.getSemesterTitle(),
                            "Set status to " + newStatus,
                            "Academic Curriculum"
                    );
                    Toast.makeText(this, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                    adapter.updateData(masterList);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Status updated locally to " + newStatus, Toast.LENGTH_SHORT).show();
                    adapter.updateData(masterList);
                });
    }

    private void showAddSemesterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_semester, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etTitle = view.findViewById(R.id.etSemesterTitleInput);
        EditText etNum = view.findViewById(R.id.etSemesterNumInput);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancelSemester);
        MaterialButton btnSave = view.findViewById(R.id.btnSaveSemester);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
            String numStr = etNum.getText() != null ? etNum.getText().toString().trim() : "";

            if (TextUtils.isEmpty(title)) {
                etTitle.setError("Title Required (e.g. Semester VII)");
                etTitle.requestFocus();
                return;
            }
            int parsedNum = 1;
            try {
                parsedNum = Integer.parseInt(numStr);
            } catch (Exception ignored) {}
            final int num = parsedNum;

            showLoading(true);
            String id = "sem" + num;
            Semester newSem = new Semester(id, title, num, "ACTIVE");

            db.collection("semesters").document(id).set(newSem)
                    .addOnSuccessListener(aVoid -> {
                        showLoading(false);
                        PortalActivityLogger.getInstance(this).logTeacherActivity(
                                "System Admin",
                                "Added Semester: " + title,
                                "Semester Number: " + num,
                                "Academic Curriculum"
                        );
                        Toast.makeText(this, "Semester added successfully!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadSemesters();
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Toast.makeText(this, "Failed to add semester", Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
