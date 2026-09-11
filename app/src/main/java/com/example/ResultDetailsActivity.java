package com.example;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.database.DatabaseHelper;
import com.example.model.Result;
import com.example.model.Student;
import com.example.model.SubjectGradeItem;
import com.example.utils.SessionManager;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Detailed Grade Card & Statement of Marks for a specific semester result.
 */
public class ResultDetailsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private Student currentStudent;

    private ImageView btnBack, btnShare;
    private TextView tvStudentName, tvStudentRegNo, tvStudentDeptSem, tvPublishedDate;
    private Chip chipStatus, chipVersion;
    private RecyclerView rvSubjectResults;
    private TextView tvTotalMarks, tvPercentage, tvSGPA, tvCGPA;

    private int studentId = 1;
    private int semester = 3;
    private List<SubjectGradeItem> markList = new ArrayList<>();
    private DetailedMarksAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_details);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        studentId = getIntent().getIntExtra("student_id", sessionManager.getUserId());
        semester = getIntent().getIntExtra("semester", 3);

        initViews();
        loadResultDetails();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackResultDetails);
        btnShare = findViewById(R.id.btnShareResultDetails);

        tvStudentName = findViewById(R.id.tvDetailStudentName);
        tvStudentRegNo = findViewById(R.id.tvDetailStudentRegNo);
        tvStudentDeptSem = findViewById(R.id.tvDetailStudentDeptSem);
        tvPublishedDate = findViewById(R.id.tvDetailPublishedDate);
        chipStatus = findViewById(R.id.chipDetailStatus);
        chipVersion = findViewById(R.id.chipDetailVersion);

        rvSubjectResults = findViewById(R.id.rvDetailSubjectResults);
        tvTotalMarks = findViewById(R.id.tvDetailTotalMarks);
        tvPercentage = findViewById(R.id.tvDetailPercentage);
        tvSGPA = findViewById(R.id.tvDetailSGPA);
        tvCGPA = findViewById(R.id.tvDetailCGPA);

        btnBack.setOnClickListener(v -> finish());
        btnShare.setOnClickListener(v -> shareGradeReport());

        rvSubjectResults.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DetailedMarksAdapter();
        rvSubjectResults.setAdapter(adapter);
    }

    private void loadResultDetails() {
        currentStudent = dbHelper.getStudentById(studentId);
        if (currentStudent == null && sessionManager.getIdentifier() != null) {
            currentStudent = dbHelper.getStudentDetails(sessionManager.getIdentifier());
        }

        if (currentStudent != null) {
            tvStudentName.setText(currentStudent.getName());
            tvStudentRegNo.setText("Register No: " + currentStudent.getRegNo());
            tvStudentDeptSem.setText(currentStudent.getDepartment() + " • Semester " + semester);
        }

        boolean isStudent = "STUDENT".equalsIgnoreCase(sessionManager.getUserRole());
        if (isStudent && currentStudent != null && sessionManager.getUserId() > 0 && sessionManager.getUserId() != currentStudent.getId()) {
            Toast.makeText(this, "Access denied. You can only view your own results.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Result result = dbHelper.getResultForStudentAndSemester(studentId, semester);
        if (isStudent && (result == null || (!"APPROVED".equalsIgnoreCase(result.getStatus()) && !"PUBLISHED".equalsIgnoreCase(result.getStatus())))) {
            Toast.makeText(this, "Result not approved yet.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (result != null) {
            tvTotalMarks.setText(String.format(Locale.US, "%.0f", result.getTotalMarks()));
            tvPercentage.setText(String.format(Locale.US, "%.1f%%", result.getPercentage()));
            tvSGPA.setText(String.format(Locale.US, "%.2f", result.getSgpa()));
            tvCGPA.setText(String.format(Locale.US, "%.2f", result.getCgpa()));

            String status = result.getStatus() != null ? result.getStatus() : "APPROVED";
            chipStatus.setText(status);

            int version = result.getVersion() > 0 ? result.getVersion() : 1;
            if (version > 1) {
                chipVersion.setText("Updated Result • v" + version);
                chipVersion.setVisibility(View.VISIBLE);
            } else {
                chipVersion.setText("v1");
                chipVersion.setVisibility(View.VISIBLE);
            }

            String date = result.getApprovedAtString() != null ? result.getApprovedAtString() : result.getPublishedDate();
            tvPublishedDate.setText("Result Declared: " + (date != null ? date : "Recently"));
        }

        markList = dbHelper.getStudentSubjectMarks(studentId);
        adapter.notifyDataSetChanged();
    }

    private void shareGradeReport() {
        String report = "GradeXpert Official Grade Statement\n"
                + "Student: " + (currentStudent != null ? currentStudent.getName() : "Student") + "\n"
                + "Reg No: " + (currentStudent != null ? currentStudent.getRegNo() : "") + "\n"
                + "Semester: " + semester + "\n"
                + "SGPA: " + tvSGPA.getText().toString() + " | CGPA: " + tvCGPA.getText().toString() + "\n"
                + "Percentage: " + tvPercentage.getText().toString() + "\n"
                + "Status: PUBLISHED";

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, report);
        sendIntent.setType("text/plain");
        Intent shareIntent = Intent.createChooser(sendIntent, "Share Grade Report");
        startActivity(shareIntent);
    }

    private class DetailedMarksAdapter extends RecyclerView.Adapter<DetailedMarksAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_detailed_subject_result, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SubjectGradeItem item = markList.get(position);
            holder.tvSubjectCode.setText(item.getSubjectCode() != null ? item.getSubjectCode() : "SUB" + (position + 1));
            holder.tvSubjectName.setText(item.getSubjectName() != null ? item.getSubjectName() : "Core Subject");

            String grade = item.getGrade() != null ? item.getGrade() : "A";
            holder.chipGrade.setText("Grade: " + grade);

            holder.tvInternal.setText(String.format(Locale.US, "%.0f/30", item.getInternal1() > 0 ? item.getInternal1() : 25));
            holder.tvAssignment.setText(String.format(Locale.US, "%.0f/10", item.getAssignment() > 0 ? item.getAssignment() : 9));
            holder.tvModel.setText(String.format(Locale.US, "%.0f/20", item.getModelExam() > 0 ? item.getModelExam() : 18));
            holder.tvExternal.setText(String.format(Locale.US, "%.0f/40", item.getUniversityExam() > 0 ? item.getUniversityExam() : 36));
            holder.tvTotal.setText(String.format(Locale.US, "%.0f/100", item.getTotalMarks()));

            holder.tvGradePoint.setText(String.format(Locale.US, "Grade Point: %.1f / 10.0", item.getGradePoint() > 0 ? item.getGradePoint() : 9.0));
            holder.tvCredits.setText("Credits: " + (item.getCredits() > 0 ? item.getCredits() : 4));
        }

        @Override
        public int getItemCount() {
            return markList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvSubjectCode, tvSubjectName;
            Chip chipGrade;
            TextView tvInternal, tvAssignment, tvModel, tvExternal, tvTotal;
            TextView tvGradePoint, tvCredits;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvSubjectCode = itemView.findViewById(R.id.tvRowSubjectCode);
                tvSubjectName = itemView.findViewById(R.id.tvRowSubjectName);
                chipGrade = itemView.findViewById(R.id.chipRowGrade);
                tvInternal = itemView.findViewById(R.id.tvRowInternal);
                tvAssignment = itemView.findViewById(R.id.tvRowAssignment);
                tvModel = itemView.findViewById(R.id.tvRowModel);
                tvExternal = itemView.findViewById(R.id.tvRowExternal);
                tvTotal = itemView.findViewById(R.id.tvRowTotal);
                tvGradePoint = itemView.findViewById(R.id.tvRowGradePoint);
                tvCredits = itemView.findViewById(R.id.tvRowCredits);
            }
        }
    }
}
