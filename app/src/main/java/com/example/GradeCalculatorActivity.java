package com.example;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adapter.GradeCalculatorAdapter;
import com.example.database.DatabaseHelper;
import com.example.model.Student;
import com.example.model.Subject;
import com.example.model.SubjectGradeItem;
import com.example.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Manual Grade Calculator / What-If SGPA Simulator Activity.
 * Allows students to manually input and test hypothetical Internal, Assignment, and External marks
 * to calculate Estimated Subject Grades, Total Marks, Percentage, and Estimated SGPA.
 *
 * IMPORTANT:
 * All calculations here are strictly for estimation / what-if simulations.
 * This activity DOES NOT modify or overwrite official teacher-published marks or results.
 */
public class GradeCalculatorActivity extends AppCompatActivity implements GradeCalculatorAdapter.OnDataChangeListener {

    private SessionManager sessionManager;
    private DatabaseHelper dbHelper;
    private Student currentStudent;

    // Header & Student Profile Views
    private ImageView btnBackGradeCalc, btnResetGradeCalcHeader;
    private TextView tvCalcStudentName, tvCalcStudentRegNo, tvCalcStudentDeptSem;
    private AutoCompleteTextView actvSelectSemester;

    // Subject RecyclerView & Adapter
    private RecyclerView rvStudentSubjectMarks;
    private TextView tvSubjectCountBadge;
    private GradeCalculatorAdapter adapter;
    private List<SubjectGradeItem> subjectGradeList;

    // Action Buttons
    private MaterialButton btnAddCustomSubject, btnResetCalculator, btnCalculateSgpa;

    // Estimated Result Card
    private MaterialCardView cardEstimatedResult;
    private TextView tvCalcTotalMarks, tvCalcPercentage, tvCalcSgpa;
    private androidx.core.widget.NestedScrollView scrollGradeCalc;

    private int selectedSemester = 3;
    private static final String[] SEMESTER_OPTIONS = new String[]{
            "Semester I", "Semester II", "Semester III", "Semester IV", "Semester V", "Semester VI"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grade_calculator);

        sessionManager = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);

        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        subjectGradeList = new ArrayList<>();

        initViews();
        setupBackButton();
        loadStudentProfile();
        setupSemesterDropdown();
        loadSemesterSubjects(selectedSemester);
        setupActionListeners();
    }

    private void initViews() {
        btnBackGradeCalc = findViewById(R.id.btnBackGradeCalc);
        btnResetGradeCalcHeader = findViewById(R.id.btnResetGradeCalcHeader);
        tvCalcStudentName = findViewById(R.id.tvCalcStudentName);
        tvCalcStudentRegNo = findViewById(R.id.tvCalcStudentRegNo);
        tvCalcStudentDeptSem = findViewById(R.id.tvCalcStudentDeptSem);
        actvSelectSemester = findViewById(R.id.actvSelectSemester);

        tvSubjectCountBadge = findViewById(R.id.tvSubjectCountBadge);
        rvStudentSubjectMarks = findViewById(R.id.rvStudentSubjectMarks);

        btnAddCustomSubject = findViewById(R.id.btnAddCustomSubject);
        btnResetCalculator = findViewById(R.id.btnResetCalculator);
        btnCalculateSgpa = findViewById(R.id.btnCalculateSgpa);

        cardEstimatedResult = findViewById(R.id.cardEstimatedResult);
        tvCalcTotalMarks = findViewById(R.id.tvCalcTotalMarks);
        tvCalcPercentage = findViewById(R.id.tvCalcPercentage);
        tvCalcSgpa = findViewById(R.id.tvCalcSgpa);
        scrollGradeCalc = findViewById(R.id.scrollGradeCalc);

        rvStudentSubjectMarks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GradeCalculatorAdapter(this, subjectGradeList, this);
        rvStudentSubjectMarks.setAdapter(adapter);
    }

    private void setupBackButton() {
        if (btnBackGradeCalc != null) {
            btnBackGradeCalc.setOnClickListener(v -> finish());
        }
        if (btnResetGradeCalcHeader != null) {
            btnResetGradeCalcHeader.setOnClickListener(v -> resetCalculator());
        }
    }

    private void loadStudentProfile() {
        String identifier = sessionManager.getIdentifier();
        currentStudent = dbHelper.getStudentDetails(identifier);

        if (currentStudent == null && sessionManager.getUserId() != -1) {
            currentStudent = dbHelper.getStudentById(sessionManager.getUserId());
        }

        if (currentStudent != null) {
            tvCalcStudentName.setText(currentStudent.getName());
            tvCalcStudentRegNo.setText("Reg No: " + currentStudent.getRegNo());
            tvCalcStudentDeptSem.setText(currentStudent.getDepartment() + " • Semester " + currentStudent.getSemester());
            int parsedSem = 3;
            if (currentStudent.getSemester() != null) {
                try {
                    String numOnly = currentStudent.getSemester().replaceAll("[^0-9]", "").trim();
                    if (!numOnly.isEmpty()) parsedSem = Integer.parseInt(numOnly);
                } catch (Exception ignored) {}
            }
            selectedSemester = parsedSem > 0 ? parsedSem : 3;
        } else {
            tvCalcStudentName.setText(sessionManager.getUserName());
            tvCalcStudentRegNo.setText("Reg No: " + (identifier != null ? identifier : "MCA001"));
            tvCalcStudentDeptSem.setText("Master of Computer Applications • Semester III");
            selectedSemester = 3;
        }
    }

    private void setupSemesterDropdown() {
        ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, SEMESTER_OPTIONS);
        actvSelectSemester.setAdapter(semesterAdapter);

        int defaultIndex = Math.max(0, Math.min(selectedSemester - 1, SEMESTER_OPTIONS.length - 1));
        actvSelectSemester.setText(SEMESTER_OPTIONS[defaultIndex], false);

        actvSelectSemester.setOnItemClickListener((parent, view, position, id) -> {
            selectedSemester = position + 1;
            loadSemesterSubjects(selectedSemester);
        });
    }

    private void loadSemesterSubjects(int semester) {
        subjectGradeList.clear();

        // Load subjects from SQLite curriculum database for selected semester
        List<Subject> dbSubjects = dbHelper.getSubjectsBySemester(semester);

        if (dbSubjects != null && !dbSubjects.isEmpty()) {
            for (Subject s : dbSubjects) {
                SubjectGradeItem item = new SubjectGradeItem(s.getSubjectCode(), s.getSubjectName(), s.getCredits(), 0, 0);
                item.setAssignment(0);
                item.setUniversityExam(0);
                item.setMaxInternal(30);
                item.setMaxExternal(70);
                item.setTotalMarks(0);
                item.recalculate();
                subjectGradeList.add(item);
            }
        } else {
            // Default curriculum fallback for simulation
            subjectGradeList.add(createSubjectItem("CS" + semester + "01", "Core Engineering Subject 1", 4));
            subjectGradeList.add(createSubjectItem("CS" + semester + "02", "Core Engineering Subject 2", 4));
            subjectGradeList.add(createSubjectItem("CS" + semester + "03", "Professional Elective 1", 3));
            subjectGradeList.add(createSubjectItem("CS" + semester + "04", "Open Elective Course", 3));
        }

        adapter.updateData(subjectGradeList);
        updateSubjectCountBadge();
        resetResultCard();
    }

    private SubjectGradeItem createSubjectItem(String code, String name, int credits) {
        SubjectGradeItem item = new SubjectGradeItem(code, name, credits, 0, 0);
        item.setAssignment(0);
        item.setUniversityExam(0);
        item.setMaxInternal(30);
        item.setMaxExternal(70);
        item.setTotalMarks(0);
        item.recalculate();
        return item;
    }

    private void setupActionListeners() {
        // Add Custom Subject Button
        btnAddCustomSubject.setOnClickListener(v -> {
            int newIdx = subjectGradeList.size() + 1;
            SubjectGradeItem customItem = createSubjectItem("ELEC0" + newIdx, "Elective Course " + newIdx, 3);
            adapter.addItem(customItem);
            updateSubjectCountBadge();
            Toast.makeText(this, "Added custom subject row", Toast.LENGTH_SHORT).show();
        });

        // Reset Button
        btnResetCalculator.setOnClickListener(v -> resetCalculator());

        // Calculate SGPA Button
        btnCalculateSgpa.setOnClickListener(v -> performCalculation());
    }

    private void updateSubjectCountBadge() {
        int count = subjectGradeList.size();
        tvSubjectCountBadge.setText(count + (count == 1 ? " Subject" : " Subjects"));
    }

    private void performCalculation() {
        if (subjectGradeList.isEmpty()) {
            Toast.makeText(this, "Please add at least one subject to calculate SGPA.", Toast.LENGTH_SHORT).show();
            return;
        }

        double totalObtainedMarks = 0.0;
        int totalCredits = 0;
        double weightedGradePoints = 0.0;

        for (SubjectGradeItem item : subjectGradeList) {
            double internal = item.getInternal1();
            double assignment = item.getAssignment();
            double external = item.getUniversityExam();

            // Validate component caps
            if (internal > 20) internal = 20;
            if (assignment > 10) assignment = 10;
            if (external > 70) external = 70;

            double subjectTotal = internal + assignment + external;
            item.setTotalMarks(subjectTotal);
            item.recalculate();

            totalObtainedMarks += subjectTotal;
            int credits = item.getCredits() > 0 ? item.getCredits() : 4;
            totalCredits += credits;
            weightedGradePoints += (credits * item.getGradePoint());
        }

        int maximumMarks = subjectGradeList.size() * 100;
        double overallPercentage = maximumMarks > 0 ? (totalObtainedMarks / maximumMarks) * 100.0 : 0.0;
        double estimatedSgpa = totalCredits > 0 ? (weightedGradePoints / totalCredits) : 0.0;

        // Display in Estimated Result Card
        tvCalcTotalMarks.setText(String.format(Locale.US, "%.0f / %d", totalObtainedMarks, maximumMarks));
        tvCalcPercentage.setText(String.format(Locale.US, "%.2f%%", overallPercentage));
        tvCalcSgpa.setText(String.format(Locale.US, "%.2f", estimatedSgpa));

        if (scrollGradeCalc != null && cardEstimatedResult != null) {
            scrollGradeCalc.post(() -> scrollGradeCalc.smoothScrollTo(0, cardEstimatedResult.getTop()));
        }

        Toast.makeText(this, String.format(Locale.US, "Estimated SGPA Calculated: %.2f", estimatedSgpa), Toast.LENGTH_SHORT).show();
    }

    private void resetCalculator() {
        loadSemesterSubjects(selectedSemester);
        resetResultCard();
        Toast.makeText(this, "Calculator reset to initial state.", Toast.LENGTH_SHORT).show();
    }

    private void resetResultCard() {
        tvCalcTotalMarks.setText("0 / " + (subjectGradeList.size() * 100));
        tvCalcPercentage.setText("0.00%");
        tvCalcSgpa.setText("0.00");
    }

    @Override
    public void onDataChanged() {
        // Trigger live count updates if needed
        updateSubjectCountBadge();
    }

    @Override
    public void onItemRemoved(int position) {
        updateSubjectCountBadge();
    }

    // Static Reusable Grade Calculation Helpers
    public static String calculateGrade(double percentage) {
        if (percentage >= 90.0) return "A+";
        if (percentage >= 80.0) return "A";
        if (percentage >= 70.0) return "B+";
        if (percentage >= 60.0) return "B";
        if (percentage >= 50.0) return "C";
        if (percentage >= 40.0) return "D";
        return "F";
    }

    public static int calculateGradePoint(double percentage) {
        if (percentage >= 90.0) return 10;
        if (percentage >= 80.0) return 9;
        if (percentage >= 70.0) return 8;
        if (percentage >= 60.0) return 7;
        if (percentage >= 50.0) return 6;
        if (percentage >= 40.0) return 5;
        return 0;
    }
}
