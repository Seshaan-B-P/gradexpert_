package com.example;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.example.adapter.NotificationAdapter;
import com.example.database.DatabaseHelper;
import com.example.model.AppNotification;
import com.example.model.User;
import com.example.utils.SessionManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class NotificationsActivity extends AppCompatActivity implements NotificationAdapter.OnNotificationClickListener {

    private ImageButton btnBack;
    private TextView tvRoleBadge, tvTotalCount, tvUnreadCount, tvBroadcastsCount;
    private EditText etSearch;
    private ChipGroup chipGroupCategory;
    private RecyclerView rvNotifications;
    private LinearLayout layoutEmpty;
    private ExtendedFloatingActionButton fabSendNotification;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private NotificationAdapter adapter;

    private List<AppNotification> masterList = new ArrayList<>();
    private List<AppNotification> filteredList = new ArrayList<>();

    private String userRole = "STUDENT";
    private String userName = "Faculty Admin";
    private String selectedCategoryFilter = "ALL";
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        if (sessionManager.isLoggedIn()) {
            userRole = sessionManager.getUserRole() != null ? sessionManager.getUserRole().toUpperCase() : "STUDENT";
            userName = sessionManager.getUserName() != null ? sessionManager.getUserName() : "Faculty Admin";
        }

        initViews();
        setupUserRoleUI();
        setupRecyclerView();
        setupCategoryChips();
        setupSearchFilter();
        loadNotificationsFromDatabase();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotificationsFromDatabase();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackNotifications);
        tvRoleBadge = findViewById(R.id.tvRoleBadge);
        tvTotalCount = findViewById(R.id.tvTotalNotifCount);
        tvUnreadCount = findViewById(R.id.tvUnreadNotifCount);
        tvBroadcastsCount = findViewById(R.id.tvBroadcastsCount);
        etSearch = findViewById(R.id.etSearchNotification);
        chipGroupCategory = findViewById(R.id.chipGroupNotifCategory);
        rvNotifications = findViewById(R.id.rvNotifications);
        layoutEmpty = findViewById(R.id.layoutEmptyNotifications);
        fabSendNotification = findViewById(R.id.fabSendNotification);

        btnBack.setOnClickListener(v -> finish());
        fabSendNotification.setOnClickListener(v -> showSendNotificationDialog());
    }

    private void setupUserRoleUI() {
        if ("ADMIN".equals(userRole)) {
            tvRoleBadge.setText("ADMIN MODE");
            fabSendNotification.setVisibility(View.VISIBLE);
        } else if ("TEACHER".equals(userRole)) {
            tvRoleBadge.setText("FACULTY MODE");
            fabSendNotification.setVisibility(View.VISIBLE);
        } else {
            tvRoleBadge.setText("STUDENT MODE");
            fabSendNotification.setVisibility(View.GONE);
        }
    }

    private void setupRecyclerView() {
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(filteredList, this);
        rvNotifications.setAdapter(adapter);
    }

    private void setupCategoryChips() {
        chipGroupCategory.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedCategoryFilter = "ALL";
            } else {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chipCatUrgent) {
                    selectedCategoryFilter = "URGENT";
                } else if (checkedId == R.id.chipCatExams) {
                    selectedCategoryFilter = "EXAM";
                } else if (checkedId == R.id.chipCatAssignments) {
                    selectedCategoryFilter = "ASSIGNMENT";
                } else if (checkedId == R.id.chipCatGeneral) {
                    selectedCategoryFilter = "GENERAL";
                } else {
                    selectedCategoryFilter = "ALL";
                }
            }
            applyFilters();
        });
    }

    private void setupSearchFilter() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadNotificationsFromDatabase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        final String finalUid;
        if (currentUser != null && currentUser.getUid() != null && !currentUser.getUid().isEmpty()) {
            finalUid = currentUser.getUid();
        } else if (sessionManager.getIdentifier() != null && !sessionManager.getIdentifier().isEmpty()) {
            finalUid = sessionManager.getIdentifier();
        } else if (sessionManager.getUserEmail() != null) {
            finalUid = sessionManager.getUserEmail();
        } else {
            finalUid = "";
        }

        // Start with local SQLite notifications
        masterList = "ADMIN".equalsIgnoreCase(userRole) ? dbHelper.getAllNotifications() : dbHelper.getNotificationsForRole(userRole);
        applyFilters();

        // Fetch from Firestore top-level notifications
        FirebaseFirestore.getInstance().collection("notifications")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        Map<Integer, AppNotification> notifMap = new HashMap<>();
                        for (AppNotification n : masterList) {
                            notifMap.put(n.getId(), n);
                        }

                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            String targetRole = doc.getString("target_role");
                            if (targetRole == null) targetRole = doc.getString("targetRole");
                            String targetUid = doc.getString("targetUserId");

                            boolean isMatch = "ADMIN".equalsIgnoreCase(userRole)
                                    || "ALL".equalsIgnoreCase(targetRole)
                                    || (userRole != null && userRole.equalsIgnoreCase(targetRole))
                                    || (!finalUid.isEmpty() && finalUid.equalsIgnoreCase(targetUid));

                            if (isMatch) {
                                String title = doc.getString("title");
                                String message = doc.getString("message");
                                String category = doc.getString("category");
                                String date = doc.getString("date");
                                String sender = doc.getString("sender");
                                Boolean read = doc.getBoolean("read");
                                if (read == null) read = doc.getBoolean("is_read");

                                int notifId = Math.abs(doc.getId().hashCode());
                                AppNotification notif = new AppNotification(
                                        notifId,
                                        title != null ? title : "Notification",
                                        message != null ? message : "",
                                        date != null ? date : "Recent",
                                        targetRole != null ? targetRole : userRole,
                                        category != null ? category : "GENERAL",
                                        sender != null ? sender : "System Admin",
                                        read != null ? read : false
                                );
                                notifMap.put(notifId, notif);
                            }
                        }

                        masterList = new ArrayList<>(notifMap.values());
                        applyFilters();
                    }
                })
                .addOnFailureListener(e -> {
                    // Local SQLite fallback already set
                });
    }

    private void updateSummaryStats() {
        int total = masterList.size();
        int unread = 0;
        int broadcasts = 0;

        for (AppNotification notif : masterList) {
            if (!notif.isRead()) unread++;
            if ("ALL".equalsIgnoreCase(notif.getTargetRole())) broadcasts++;
        }

        tvTotalCount.setText(String.valueOf(total));
        tvUnreadCount.setText(String.valueOf(unread));
        tvBroadcastsCount.setText(String.valueOf(broadcasts));
    }

    private void applyFilters() {
        filteredList.clear();

        for (AppNotification notif : masterList) {
            boolean matchesCategory = "ALL".equals(selectedCategoryFilter)
                    || selectedCategoryFilter.equalsIgnoreCase(notif.getCategory());

            boolean matchesSearch = searchQuery.isEmpty()
                    || (notif.getTitle() != null && notif.getTitle().toLowerCase().contains(searchQuery))
                    || (notif.getMessage() != null && notif.getMessage().toLowerCase().contains(searchQuery));

            if (matchesCategory && matchesSearch) {
                filteredList.add(notif);
            }
        }

        adapter.updateData(filteredList);

        if (filteredList.isEmpty()) {
            rvNotifications.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            rvNotifications.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        }

        updateSummaryStats();
    }

    @Override
    public void onItemClick(AppNotification notification) {
        showViewNotificationDialog(notification);
        updateSummaryStats();
    }

    @Override
    public void onDeleteClick(AppNotification notification, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Notification")
                .setMessage("Are you sure you want to delete this notification from SQLite storage?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    boolean success = dbHelper.deleteNotification(notification.getId());
                    if (success) {
                        Toast.makeText(this, "Notification deleted", Toast.LENGTH_SHORT).show();
                        loadNotificationsFromDatabase();
                    } else {
                        Toast.makeText(this, "Failed to delete notification", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showViewNotificationDialog(AppNotification notif) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_view_notification, null);

        TextView tvCategory = view.findViewById(R.id.tvDetailCategory);
        TextView tvTargetRole = view.findViewById(R.id.tvDetailTargetRole);
        TextView tvDate = view.findViewById(R.id.tvDetailDate);
        TextView tvTitle = view.findViewById(R.id.tvDetailTitle);
        TextView tvSender = view.findViewById(R.id.tvDetailSender);
        TextView tvMessage = view.findViewById(R.id.tvDetailMessage);
        Button btnClose = view.findViewById(R.id.btnDetailClose);

        tvCategory.setText(notif.getCategory() != null ? notif.getCategory().toUpperCase() : "GENERAL");
        tvTargetRole.setText(notif.getTargetRole() != null ? notif.getTargetRole() : "ALL");
        tvDate.setText(notif.getDate() != null ? notif.getDate() : "");
        tvTitle.setText(notif.getTitle());
        tvSender.setText("Published by: " + (notif.getSender() != null ? notif.getSender() : userName));
        tvMessage.setText(notif.getMessage());

        AlertDialog dialog = builder.setView(view).create();
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showSendNotificationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_send_notification, null);

        EditText etTitle = view.findViewById(R.id.etDialogNotifTitle);
        EditText etMessage = view.findViewById(R.id.etDialogNotifMessage);
        Spinner spinnerTarget = view.findViewById(R.id.spinnerDialogTargetRole);
        Spinner spinnerCategory = view.findViewById(R.id.spinnerDialogCategory);
        EditText etSender = view.findViewById(R.id.etDialogSender);
        EditText etDate = view.findViewById(R.id.etDialogDate);
        Button btnCancel = view.findViewById(R.id.btnDialogCancelNotif);
        Button btnSend = view.findViewById(R.id.btnDialogSendNotif);

        // Pre-fill default values
        etSender.setText(userName);
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        etDate.setText(currentDate);

        // Populate Spinners
        String[] targetOptions = {"STUDENT", "ALL", "TEACHER"};
        ArrayAdapter<String> targetAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, targetOptions);
        spinnerTarget.setAdapter(targetAdapter);

        String[] categoryOptions = {"GENERAL", "URGENT", "EXAM", "ASSIGNMENT", "ATTENDANCE"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categoryOptions);
        spinnerCategory.setAdapter(categoryAdapter);

        AlertDialog dialog = builder.setView(view).create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSend.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String message = etMessage.getText().toString().trim();
            String targetRole = spinnerTarget.getSelectedItem().toString();
            String category = spinnerCategory.getSelectedItem().toString();
            String sender = etSender.getText().toString().trim();
            String date = etDate.getText().toString().trim();

            if (title.isEmpty()) {
                etTitle.setError("Title is required");
                etTitle.requestFocus();
                return;
            }

            if (message.isEmpty()) {
                etMessage.setError("Message body is required");
                etMessage.requestFocus();
                return;
            }

            if (date.isEmpty()) {
                date = currentDate;
            }

            boolean inserted = dbHelper.addNotification(title, message, date, targetRole, category, sender);
            if (inserted) {
                Toast.makeText(NotificationsActivity.this, "Notification Broadcast Sent Successfully!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadNotificationsFromDatabase();
            } else {
                Toast.makeText(NotificationsActivity.this, "Failed to send notification", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
}
