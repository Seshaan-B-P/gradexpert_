package com.example;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
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

import com.example.adapter.AcademicYearAdapter;
import com.example.model.AcademicYear;
import com.example.utils.PortalActivityLogger;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

/**
 * Academic Year Management Activity for System Admin.
 * Enforces exactly ONE academic year marked as CURRENT.
 */
public class AcademicYearActivity extends AppCompatActivity implements AcademicYearAdapter.OnAcademicYearClickListener {

    private FirebaseFirestore db;

    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvCurrentDisplay;
    private ProgressBar progressBar;
    private RecyclerView rvYears;
    private FloatingActionButton fabAdd;

    private AcademicYearAdapter adapter;
    private List<AcademicYear> masterList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_academic_year);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupToolbar();
        setupWindowInsets();
        setupSwipeRefresh();
        setupRecyclerView();
        loadAcademicYears();
    }

    private void setupWindowInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbarAcademicYear), (v, insets) -> {
            androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
            v.setPadding(0, statusBarInsets.top, 0, 0);
            return insets;
        });
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarAcademicYear);
        swipeRefresh = findViewById(R.id.swipeRefreshAcademicYears);
        tvCurrentDisplay = findViewById(R.id.tvCurrentAcademicYearDisplay);
        progressBar = findViewById(R.id.progressBarAcademicYear);
        rvYears = findViewById(R.id.rvAcademicYearsList);
        fabAdd = findViewById(R.id.fabAddAcademicYear);

        fabAdd.setOnClickListener(v -> showAddAcademicYearDialog());
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(this::loadAcademicYears);
    }

    private void setupRecyclerView() {
        rvYears.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AcademicYearAdapter(masterList, this);
        rvYears.setAdapter(adapter);
    }

    private void loadAcademicYears() {
        showLoading(true);
        db.collection("academicYears").get().addOnSuccessListener(queryDocumentSnapshots -> {
            showLoading(false);
            swipeRefresh.setRefreshing(false);
            masterList.clear();

            String activeYear = "2026-27";
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                AcademicYear year = doc.toObject(AcademicYear.class);
                if (year != null) {
                    year.setYearId(doc.getId());
                    masterList.add(year);
                    if (year.isCurrent()) {
                        activeYear = year.getYearTitle();
                    }
                }
            }

            tvCurrentDisplay.setText(activeYear);
            adapter.updateData(masterList);
        }).addOnFailureListener(e -> {
            showLoading(false);
            swipeRefresh.setRefreshing(false);
            adapter.updateData(masterList);
        });
    }

    @Override
    public void onSetCurrentClick(AcademicYear targetYear) {
        if (targetYear == null) return;

        showLoading(true);
        WriteBatch batch = db.batch();

        for (AcademicYear item : masterList) {
            boolean isNewCurrent = item.getYearId().equals(targetYear.getYearId());
            item.setCurrent(isNewCurrent);
            batch.update(db.collection("academicYears").document(item.getYearId()), "isCurrent", isNewCurrent);
        }

        batch.commit().addOnSuccessListener(aVoid -> {
            showLoading(false);
            tvCurrentDisplay.setText(targetYear.getYearTitle());
            adapter.updateData(masterList);
            PortalActivityLogger.getInstance(this).logTeacherActivity(
                    "System Admin",
                    "Academic Year Changed",
                    "Current active academic year set to " + targetYear.getYearTitle(),
                    "System Settings"
            );
            Toast.makeText(this, "Current academic year updated to " + targetYear.getYearTitle(), Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            showLoading(false);
            tvCurrentDisplay.setText(targetYear.getYearTitle());
            adapter.updateData(masterList);
            Toast.makeText(this, "Academic year set to " + targetYear.getYearTitle(), Toast.LENGTH_SHORT).show();
        });
    }

    private void showAddAcademicYearDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_academic_year, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etTitle = view.findViewById(R.id.etAcademicYearInput);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancelAcademicYear);
        MaterialButton btnSave = view.findViewById(R.id.btnSaveAcademicYear);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
            if (TextUtils.isEmpty(title)) {
                etTitle.setError("Title required (e.g. 2027-28)");
                etTitle.requestFocus();
                return;
            }

            showLoading(true);
            String id = title.replaceAll("\\s+", "");
            AcademicYear newYear = new AcademicYear(id, title, false, "ACTIVE");

            db.collection("academicYears").document(id).set(newYear)
                    .addOnSuccessListener(aVoid -> {
                        showLoading(false);
                        PortalActivityLogger.getInstance(this).logTeacherActivity(
                                "System Admin",
                                "Added Academic Year: " + title,
                                "Created academic year record",
                                "System Settings"
                        );
                        Toast.makeText(this, "Academic Year " + title + " added!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadAcademicYears();
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Toast.makeText(this, "Failed to add academic year", Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
