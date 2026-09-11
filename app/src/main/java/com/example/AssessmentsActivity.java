package com.example;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.database.DatabaseHelper;
import com.example.model.Assessment;
import com.example.model.Subject;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Activity for managing academic evaluations, internal tests, model exams, practicals, and quizzes.
 */
public class AssessmentsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private ImageView btnBack;
    private TextView tvAssessCountHeader;
    private Chip chipPendingCount;
    private RecyclerView rvAssessmentsList;
    private LinearLayout layoutEmptyAssessments;
    private FloatingActionButton fabAddAssessment;

    private AssessmentAdapter adapter;
    private List<Assessment> assessmentList = new ArrayList<>();
    private List<Subject> subjectList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assessments);

        dbHelper = new DatabaseHelper(this);

        initViews();
        setupListeners();
        loadAssessments();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackAssessments);
        tvAssessCountHeader = findViewById(R.id.tvAssessCountHeader);
        chipPendingCount = findViewById(R.id.chipPendingCount);
        rvAssessmentsList = findViewById(R.id.rvAssessmentsList);
        layoutEmptyAssessments = findViewById(R.id.layoutEmptyAssessments);
        fabAddAssessment = findViewById(R.id.fabAddAssessment);

        rvAssessmentsList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AssessmentAdapter();
        rvAssessmentsList.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        fabAddAssessment.setOnClickListener(v -> showAddEditAssessmentDialog(null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAssessments();
    }

    private void loadAssessments() {
        assessmentList = dbHelper.getAllAssessments();
        subjectList = dbHelper.getAllSubjects();

        tvAssessCountHeader.setText("Scheduled Assessments: " + assessmentList.size());

        int pendingCount = 0;
        for (Assessment a : assessmentList) {
            if ("PENDING".equalsIgnoreCase(a.getStatus())) {
                pendingCount++;
            }
        }
        chipPendingCount.setText(pendingCount + " Pending");

        if (assessmentList.isEmpty()) {
            layoutEmptyAssessments.setVisibility(View.VISIBLE);
            rvAssessmentsList.setVisibility(View.GONE);
        } else {
            layoutEmptyAssessments.setVisibility(View.GONE);
            rvAssessmentsList.setVisibility(View.VISIBLE);
        }

        adapter.notifyDataSetChanged();
    }

    private void showAddEditAssessmentDialog(Assessment itemToEdit) {
        boolean isEdit = itemToEdit != null;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_assessment, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tvAssessDialogTitle);
        TextInputEditText etTitle = dialogView.findViewById(R.id.etAssessTitle);
        AutoCompleteTextView actvType = dialogView.findViewById(R.id.actvAssessType);
        AutoCompleteTextView actvSubject = dialogView.findViewById(R.id.actvAssessSubject);
        TextInputEditText etSem = dialogView.findViewById(R.id.etAssessSem);
        TextInputEditText etDate = dialogView.findViewById(R.id.etAssessDate);
        TextInputEditText etMaxMarks = dialogView.findViewById(R.id.etAssessMaxMarks);

        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelAssess);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSaveAssess);

        // Types setup
        String[] types = {"Internal Test", "Model Exam", "Practical", "Assignment", "Quiz"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, types);
        actvType.setAdapter(typeAdapter);

        // Subjects setup
        List<String> subNames = new ArrayList<>();
        for (Subject sub : subjectList) {
            subNames.add(sub.getSubjectCode() + " - " + sub.getSubjectName());
        }
        if (subNames.isEmpty()) {
            subNames.add("No Subjects Available");
        }
        ArrayAdapter<String> subAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, subNames);
        actvSubject.setAdapter(subAdapter);

        if (isEdit) {
            tvTitle.setText("Edit Assessment");
            etTitle.setText(itemToEdit.getTitle());
            actvType.setText(itemToEdit.getType(), false);
            etSem.setText(String.valueOf(itemToEdit.getSemester()));
            etDate.setText(itemToEdit.getDate());
            etMaxMarks.setText(String.valueOf(itemToEdit.getMaxMarks()));
            btnSave.setText("Update Assessment");
        } else {
            tvTitle.setText("Schedule New Assessment");
            actvType.setText("Internal Test", false);
            etSem.setText("3");
            etDate.setText("2026-08-28");
            etMaxMarks.setText("50");
            btnSave.setText("Save Assessment");
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
            String type = actvType.getText().toString().trim();
            String semStr = etSem.getText() != null ? etSem.getText().toString().trim() : "";
            String date = etDate.getText() != null ? etDate.getText().toString().trim() : "";
            String maxMarksStr = etMaxMarks.getText() != null ? etMaxMarks.getText().toString().trim() : "";

            if (title.isEmpty() || type.isEmpty() || semStr.isEmpty() || date.isEmpty() || maxMarksStr.isEmpty()) {
                Toast.makeText(this, "Please fill in all assessment details", Toast.LENGTH_SHORT).show();
                return;
            }

            int sem = 3;
            double maxMarks = 50.0;
            try {
                sem = Integer.parseInt(semStr);
                maxMarks = Double.parseDouble(maxMarksStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid numbers entered", Toast.LENGTH_SHORT).show();
                return;
            }

            int subjectId = 1;
            int selectedSubIdx = actvSubject.getText() != null ? subNames.indexOf(actvSubject.getText().toString()) : 0;
            if (selectedSubIdx >= 0 && selectedSubIdx < subjectList.size()) {
                subjectId = subjectList.get(selectedSubIdx).getId();
            }

            if (isEdit) {
                itemToEdit.setTitle(title);
                itemToEdit.setType(type);
                itemToEdit.setSemester(sem);
                itemToEdit.setDate(date);
                itemToEdit.setMaxMarks(maxMarks);
                itemToEdit.setSubjectId(subjectId);

                boolean success = dbHelper.updateAssessment(itemToEdit);
                if (success) {
                    Toast.makeText(this, "Assessment updated successfully!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadAssessments();
                } else {
                    Toast.makeText(this, "Failed to update assessment", Toast.LENGTH_SHORT).show();
                }
            } else {
                Assessment newAssess = new Assessment(0, title, type, subjectId, sem, date, maxMarks, "PENDING");
                boolean success = dbHelper.addAssessment(newAssess);
                if (success) {
                    dbHelper.logTeacherActivity("Faculty", "Assessment Scheduled", "Scheduled " + type + ": " + title, "ASSESSMENTS");
                    Toast.makeText(this, "Assessment scheduled & saved to SQLite!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadAssessments();
                } else {
                    Toast.makeText(this, "Failed to schedule assessment", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }

    private void confirmDeleteAssessment(Assessment a) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Scheduled Assessment?")
                .setMessage("Are you sure you want to delete assessment: " + a.getTitle() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    boolean success = dbHelper.deleteAssessment(a.getId());
                    if (success) {
                        Toast.makeText(this, "Assessment deleted", Toast.LENGTH_SHORT).show();
                        loadAssessments();
                    } else {
                        Toast.makeText(this, "Failed to delete assessment", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Inner RecyclerView Adapter
    private class AssessmentAdapter extends RecyclerView.Adapter<AssessmentAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_assessment, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Assessment a = assessmentList.get(position);
            holder.tvTitle.setText(a.getTitle());
            String subCode = a.getSubjectCode() != null ? a.getSubjectCode() : "CS50" + a.getSubjectId();
            holder.tvSubSem.setText(a.getType() + " • " + subCode + " • Semester " + a.getSemester());
            holder.tvDate.setText("📅 Date: " + a.getDate());
            holder.tvMaxMarks.setText(String.format(Locale.US, "💯 Max Marks: %.0f", a.getMaxMarks()));

            holder.chipStatus.setText(a.getStatus());

            holder.btnEdit.setOnClickListener(v -> showAddEditAssessmentDialog(a));
            holder.btnDelete.setOnClickListener(v -> confirmDeleteAssessment(a));
        }

        @Override
        public int getItemCount() {
            return assessmentList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvSubSem, tvDate, tvMaxMarks;
            Chip chipStatus;
            ImageView btnEdit, btnDelete;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvAssessTitle);
                tvSubSem = itemView.findViewById(R.id.tvAssessSubSem);
                tvDate = itemView.findViewById(R.id.tvAssessDate);
                tvMaxMarks = itemView.findViewById(R.id.tvAssessMaxMarks);
                chipStatus = itemView.findViewById(R.id.chipAssessStatus);
                btnEdit = itemView.findViewById(R.id.btnEditAssess);
                btnDelete = itemView.findViewById(R.id.btnDeleteAssess);
            }
        }
    }
}
