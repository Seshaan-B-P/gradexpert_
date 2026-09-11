package com.example;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.repository.BroadcastRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.Locale;

/**
 * Activity for displaying complete broadcast specifications and handling cancellation/deletion actions.
 */
public class BroadcastDetailsActivity extends AppCompatActivity {

    private BroadcastRepository repository;

    private Toolbar toolbar;
    private TextView tvTitle;
    private TextView tvMessage;
    private TextView tvType;
    private Chip chipStatus;

    private TextView tvPriority;
    private TextView tvTarget;
    private TextView tvRecipients;

    private MaterialButton btnAction;

    private String alertId = "";
    private String title = "";
    private String message = "";
    private String type = "General Announcement";
    private String priority = "Normal";
    private String targetType = "ALL_STUDENTS";
    private String department = "";
    private String semester = "";
    private String studentName = "";
    private String status = "PUBLISHED";
    private int recipientCount = 42;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_broadcast_details);

        repository = BroadcastRepository.getInstance(this);

        readIntentExtras();
        initViews();
        setupToolbar();
        setupWindowInsets();
        populateData();
        setupActions();
    }

    private void readIntentExtras() {
        if (getIntent() != null) {
            alertId = getIntent().getStringExtra("alertId");
            title = getIntent().getStringExtra("title");
            message = getIntent().getStringExtra("message");
            type = getIntent().getStringExtra("type");
            priority = getIntent().getStringExtra("priority");
            targetType = getIntent().getStringExtra("targetType");
            department = getIntent().getStringExtra("department");
            semester = getIntent().getStringExtra("semester");
            studentName = getIntent().getStringExtra("studentName");
            status = getIntent().getStringExtra("status");
            recipientCount = getIntent().getIntExtra("recipientCount", 42);
        }
    }

    private void setupWindowInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbarBroadcastDetails), (v, insets) -> {
            androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
            v.setPadding(0, statusBarInsets.top, 0, 0);
            return insets;
        });
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarBroadcastDetails);
        tvTitle = findViewById(R.id.tvDetailTitle);
        tvMessage = findViewById(R.id.tvDetailMessage);
        tvType = findViewById(R.id.tvDetailType);
        chipStatus = findViewById(R.id.chipDetailStatus);

        tvPriority = findViewById(R.id.tvDetailPriority);
        tvTarget = findViewById(R.id.tvDetailTarget);
        tvRecipients = findViewById(R.id.tvDetailRecipients);

        btnAction = findViewById(R.id.btnCancelOrDeleteAlert);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void populateData() {
        tvTitle.setText(title != null ? title : "Broadcast Alert");
        tvMessage.setText(message != null ? message : "");
        tvType.setText(type != null ? type : "General Announcement");

        String p = priority != null ? priority : "Normal";
        tvPriority.setText(p);
        if ("URGENT".equalsIgnoreCase(p)) {
            tvPriority.setTextColor(Color.parseColor("#EF4444")); // Red
        } else if ("HIGH".equalsIgnoreCase(p)) {
            tvPriority.setTextColor(Color.parseColor("#F59E0B")); // Amber
        } else {
            tvPriority.setTextColor(Color.parseColor("#4F46E5")); // Indigo
        }

        String targetStr = "All Active Students";
        if ("DEPARTMENT".equalsIgnoreCase(targetType)) {
            targetStr = department != null ? department : "Department";
        } else if ("DEPARTMENT_SEMESTER".equalsIgnoreCase(targetType)) {
            targetStr = (department != null ? department : "Dept") + " - " + (semester != null ? semester : "Sem");
        } else if ("SPECIFIC_STUDENT".equalsIgnoreCase(targetType)) {
            targetStr = studentName != null ? studentName : "Specific Student";
        }
        tvTarget.setText(targetStr);

        tvRecipients.setText(String.format(Locale.US, "%d Active Student(s)", recipientCount));

        String st = status != null ? status.toUpperCase() : "PUBLISHED";
        chipStatus.setText(st);

        if ("PUBLISHED".equals(st)) {
            chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#10B981"))); // Green
            btnAction.setVisibility(View.GONE);
        } else if ("SCHEDULED".equals(st)) {
            chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F59E0B"))); // Amber
            btnAction.setText("CANCEL SCHEDULED ALERT");
            btnAction.setVisibility(View.VISIBLE);
        } else if ("DRAFT".equals(st)) {
            chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#64748B"))); // Slate
            btnAction.setText("DELETE DRAFT ALERT");
            btnAction.setVisibility(View.VISIBLE);
        } else {
            chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#94A3B8")));
            btnAction.setVisibility(View.GONE);
        }
    }

    private void setupActions() {
        btnAction.setOnClickListener(v -> {
            if ("SCHEDULED".equalsIgnoreCase(status)) {
                new AlertDialog.Builder(this)
                        .setTitle("Cancel Scheduled Alert?")
                        .setMessage("Are you sure you want to cancel this scheduled alert?")
                        .setPositiveButton("CANCEL ALERT", (dialog, which) -> {
                            repository.cancelScheduledAlert(alertId, new BroadcastRepository.OnAlertOperationListener() {
                                @Override
                                public void onSuccess(String message) {
                                    Toast.makeText(BroadcastDetailsActivity.this, "Scheduled alert cancelled.", Toast.LENGTH_SHORT).show();
                                    finish();
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    Toast.makeText(BroadcastDetailsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("KEEP SCHEDULED", null)
                        .show();
            } else if ("DRAFT".equalsIgnoreCase(status)) {
                new AlertDialog.Builder(this)
                        .setTitle("Delete Draft Alert?")
                        .setMessage("Are you sure you want to delete this draft?")
                        .setPositiveButton("DELETE", (dialog, which) -> {
                            repository.deleteDraftAlert(alertId, new BroadcastRepository.OnAlertOperationListener() {
                                @Override
                                public void onSuccess(String message) {
                                    Toast.makeText(BroadcastDetailsActivity.this, "Draft deleted successfully.", Toast.LENGTH_SHORT).show();
                                    finish();
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    Toast.makeText(BroadcastDetailsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("CANCEL", null)
                        .show();
            }
        });
    }
}
