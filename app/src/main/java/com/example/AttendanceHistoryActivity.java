package com.example;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adapter.AttendanceHistoryAdapter;
import com.example.model.Attendance;
import com.example.model.AttendanceHistoryItem;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity for viewing past hour-based attendance history records from Firebase Firestore.
 */
public class AttendanceHistoryActivity extends AppCompatActivity {

    private static final String TAG = "AttendanceHistoryAct";

    private FirebaseFirestore db;
    private Toolbar toolbar;
    private Spinner spDept;
    private Spinner spSem;
    private ProgressBar progressBar;
    private LinearLayout layoutEmpty;
    private RecyclerView rvHistory;
    private AttendanceHistoryAdapter adapter;

    private String selectedDept = "Master of Computer Applications";
    private String selectedSem = "Semester III";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_history);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupToolbar();
        setupWindowInsets();
        setupSpinners();
        setupRecyclerView();
        loadHistoryData();
    }

    private void setupWindowInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbarHistory), (v, insets) -> {
            androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
            v.setPadding(0, statusBarInsets.top, 0, 0);
            return insets;
        });
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarHistory);
        spDept = findViewById(R.id.spHistoryDept);
        spSem = findViewById(R.id.spHistorySem);
        progressBar = findViewById(R.id.progressBarHistory);
        layoutEmpty = findViewById(R.id.layoutEmptyHistory);
        rvHistory = findViewById(R.id.rvAttendanceHistory);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSpinners() {
        List<String> depts = new ArrayList<>();
        depts.add("Master of Computer Applications");
        depts.add("Computer Science & Engineering");
        depts.add("Information Technology");
        depts.add("Electronics & Communication");

        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, depts);
        deptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDept.setAdapter(deptAdapter);

        spDept.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDept = depts.get(position);
                loadHistoryData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        List<String> sems = new ArrayList<>();
        sems.add("Semester III");
        sems.add("Semester I");
        sems.add("Semester II");
        sems.add("Semester IV");
        sems.add("Semester V");
        sems.add("Semester VI");

        ArrayAdapter<String> semAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sems);
        semAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSem.setAdapter(semAdapter);

        spSem.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSem = sems.get(position);
                loadHistoryData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupRecyclerView() {
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AttendanceHistoryAdapter(new ArrayList<>());
        rvHistory.setAdapter(adapter);
    }

    private void loadHistoryData() {
        progressBar.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        rvHistory.setVisibility(View.GONE);

        Query query = db.collection("attendance");
        if (selectedDept != null && !selectedDept.isEmpty()) {
            query = query.whereEqualTo("department", selectedDept);
        }
        if (selectedSem != null && !selectedSem.isEmpty()) {
            query = query.whereEqualTo("semester", selectedSem);
        }

        query.get()
                .addOnSuccessListener(querySnapshot -> {
                    progressBar.setVisibility(View.GONE);

                    Map<String, SessionAggregator> aggregatorMap = new HashMap<>();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        try {
                            Attendance att = doc.toObject(Attendance.class);
                            if (att != null && att.getDate() != null && att.getSubjectName() != null) {
                                String sessionKey = att.getSubjectName() + "_" + att.getDate() + "_" + att.getHour();
                                SessionAggregator agg = aggregatorMap.get(sessionKey);
                                if (agg == null) {
                                    agg = new SessionAggregator(att.getDate(), att.getSubjectName(), att.getHour());
                                    aggregatorMap.put(sessionKey, agg);
                                }
                                agg.addRecord(att.getStatus());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing attendance doc in history: " + doc.getId(), e);
                        }
                    }

                    List<AttendanceHistoryItem> historyItems = new ArrayList<>();
                    for (SessionAggregator agg : aggregatorMap.values()) {
                        historyItems.add(agg.toHistoryItem());
                    }

                    if (historyItems.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        rvHistory.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        rvHistory.setVisibility(View.VISIBLE);
                        adapter.updateList(historyItems);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    layoutEmpty.setVisibility(View.VISIBLE);
                    rvHistory.setVisibility(View.GONE);
                    Toast.makeText(AttendanceHistoryActivity.this, "Unable to load attendance history: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private static class SessionAggregator {
        private final String date;
        private final String subjectName;
        private final int hour;
        private int present = 0;
        private int absent = 0;
        private int total = 0;

        public SessionAggregator(String date, String subjectName, int hour) {
            this.date = date;
            this.subjectName = subjectName;
            this.hour = hour;
        }

        public void addRecord(String status) {
            total++;
            if ("PRESENT".equalsIgnoreCase(status)) {
                present++;
            } else if ("ABSENT".equalsIgnoreCase(status)) {
                absent++;
            }
        }

        public AttendanceHistoryItem toHistoryItem() {
            return new AttendanceHistoryItem(date, subjectName, hour, present, absent, total);
        }
    }
}
