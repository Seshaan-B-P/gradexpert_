package com.example;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.database.DatabaseHelper;
import com.example.model.Student;
import com.example.model.Subject;
import com.example.utils.SessionManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity displaying enrolled subjects filtered by the logged-in student's department and semester.
 */
public class SubjectsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private Student currentStudent;

    private ImageView btnBack;
    private TextView tvSubjectsSubHeader;
    private TextInputEditText etSearch;
    private TextView tvEnrolledHeaderCount;
    private Chip chipSemesterBadge;
    private RecyclerView rvStudentSubjects;
    private LinearLayout layoutEmptySubjects;

    private SubjectItemAdapter adapter;
    private List<Subject> masterSubjectList = new ArrayList<>();
    private List<Subject> filteredSubjectList = new ArrayList<>();
    private int currentSemester = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subjects);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        initViews();
        loadStudentAndSubjects();
        setupSearchFilter();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackSubjects);
        tvSubjectsSubHeader = findViewById(R.id.tvSubjectsSubHeader);
        etSearch = findViewById(R.id.etSearchSubjects);
        tvEnrolledHeaderCount = findViewById(R.id.tvEnrolledHeaderCount);
        chipSemesterBadge = findViewById(R.id.chipSemesterBadge);
        rvStudentSubjects = findViewById(R.id.rvStudentSubjects);
        layoutEmptySubjects = findViewById(R.id.layoutEmptySubjects);

        btnBack.setOnClickListener(v -> finish());

        rvStudentSubjects.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SubjectItemAdapter();
        rvStudentSubjects.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStudentAndSubjects();
    }

    private void loadStudentAndSubjects() {
        currentStudent = dbHelper.getStudentDetails(sessionManager.getIdentifier());
        if (currentStudent == null && sessionManager.getUserId() > 0) {
            currentStudent = dbHelper.getStudentById(sessionManager.getUserId());
        }

        if (currentStudent != null) {
            int parsedSem = 3;
            if (currentStudent.getSemester() != null) {
                try {
                    String numOnly = currentStudent.getSemester().replaceAll("[^0-9]", "").trim();
                    if (!numOnly.isEmpty()) parsedSem = Integer.parseInt(numOnly);
                } catch (Exception ignored) {}
            }
            currentSemester = parsedSem > 0 ? parsedSem : 3;
            tvSubjectsSubHeader.setText(currentStudent.getDepartment() + " • Semester " + currentSemester);
            chipSemesterBadge.setText("Semester " + currentSemester);
        } else {
            chipSemesterBadge.setText("Semester " + currentSemester);
        }

        // Query only subjects matching student's semester
        masterSubjectList = dbHelper.getSubjectsBySemester(currentSemester);
        applyFilter("");
    }

    private void setupSearchFilter() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter(s.toString().trim().toLowerCase());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void applyFilter(String query) {
        filteredSubjectList.clear();
        if (query.isEmpty()) {
            filteredSubjectList.addAll(masterSubjectList);
        } else {
            for (Subject sub : masterSubjectList) {
                if ((sub.getSubjectName() != null && sub.getSubjectName().toLowerCase().contains(query))
                        || (sub.getSubjectCode() != null && sub.getSubjectCode().toLowerCase().contains(query))) {
                    filteredSubjectList.add(sub);
                }
            }
        }

        tvEnrolledHeaderCount.setText("Current Semester Subjects (" + filteredSubjectList.size() + ")");

        if (filteredSubjectList.isEmpty()) {
            rvStudentSubjects.setVisibility(View.GONE);
            layoutEmptySubjects.setVisibility(View.VISIBLE);
        } else {
            rvStudentSubjects.setVisibility(View.VISIBLE);
            layoutEmptySubjects.setVisibility(View.GONE);
        }

        adapter.notifyDataSetChanged();
    }

    private class SubjectItemAdapter extends RecyclerView.Adapter<SubjectItemAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_subject, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Subject s = filteredSubjectList.get(position);
            holder.tvCode.setText(s.getSubjectCode());
            holder.tvName.setText(s.getSubjectName());
            holder.chipCredits.setText(s.getCredits() + " Credits");
            holder.tvSemDept.setText("Semester " + s.getSemester() + " • Core Course");
            holder.tvFaculty.setText("👨‍🏫 Faculty: Dr. Seshaan B P");
        }

        @Override
        public int getItemCount() {
            return filteredSubjectList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvCode, tvName, tvFaculty, tvSemDept;
            Chip chipCredits;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvCode = itemView.findViewById(R.id.tvSubjectCode);
                tvName = itemView.findViewById(R.id.tvSubjectName);
                chipCredits = itemView.findViewById(R.id.chipSubjectCredits);
                tvFaculty = itemView.findViewById(R.id.tvSubjectFaculty);
                tvSemDept = itemView.findViewById(R.id.tvSubjectSemDept);
            }
        }
    }
}
