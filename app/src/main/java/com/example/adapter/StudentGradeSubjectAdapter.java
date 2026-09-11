package com.example.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.model.SubjectGradeItem;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for rendering enrolled subject component marks, grades, and grade points
 * inside the Student Grade Calculator view.
 */
public class StudentGradeSubjectAdapter extends RecyclerView.Adapter<StudentGradeSubjectAdapter.ViewHolder> {

    private final Context context;
    private List<SubjectGradeItem> itemList;

    public StudentGradeSubjectAdapter(Context context, List<SubjectGradeItem> itemList) {
        this.context = context;
        this.itemList = itemList != null ? itemList : new ArrayList<>();
    }

    public void updateData(List<SubjectGradeItem> newList) {
        this.itemList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_grade_subject, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SubjectGradeItem item = itemList.get(position);

        String code = item.getSubjectCode() != null && !item.getSubjectCode().isEmpty() ? item.getSubjectCode() : "SUB" + (position + 1);
        String name = item.getSubjectName() != null && !item.getSubjectName().isEmpty() ? item.getSubjectName() : "Core Subject";
        int credits = item.getCredits() > 0 ? item.getCredits() : 4;

        holder.tvSubjectCode.setText(code);
        holder.tvSubjectName.setText(name);

        String grade = item.getGrade() != null ? item.getGrade() : "A";
        int gradePoint = item.getGradePoint();

        holder.chipSubjectGrade.setText("Grade: " + grade);
        applyGradeChipColor(holder.chipSubjectGrade, grade);

        double internal = item.getInternalMarks() > 0 ? item.getInternalMarks() : item.getInternal1();
        double assignment = item.getAssignment() > 0 ? item.getAssignment() : 9;
        double external = item.getExternalMarks() > 0 ? item.getExternalMarks() : item.getUniversityExam();
        double total = item.getTotalMarks();

        holder.tvInternalMarks.setText(String.format(Locale.US, "%.0f / %d", internal, item.getMaxInternal() > 0 ? item.getMaxInternal() : 25));
        holder.tvAssignmentMarks.setText(String.format(Locale.US, "%.0f / 10", assignment));
        holder.tvExternalMarks.setText(String.format(Locale.US, "%.0f / %d", external, item.getMaxExternal() > 0 ? item.getMaxExternal() : 75));

        holder.tvSubjectTotal.setText(String.format(Locale.US, "Total: %.0f / 100 (%.1f%%)", total, item.getPercentage()));
        holder.tvSubjectGradePoint.setText(String.format(Locale.US, "GP: %d • Credits: %d", gradePoint, credits));
    }

    private void applyGradeChipColor(Chip chip, String grade) {
        if ("A+".equalsIgnoreCase(grade) || "O".equalsIgnoreCase(grade)) {
            chip.setChipBackgroundColorResource(R.color.success_light);
            chip.setTextColor(ContextCompat.getColor(context, R.color.success));
            chip.setChipStrokeColorResource(R.color.success);
        } else if ("A".equalsIgnoreCase(grade)) {
            chip.setChipBackgroundColorResource(R.color.primary_light);
            chip.setTextColor(ContextCompat.getColor(context, R.color.primary));
            chip.setChipStrokeColorResource(R.color.primary);
        } else if ("B+".equalsIgnoreCase(grade) || "B".equalsIgnoreCase(grade)) {
            chip.setChipBackgroundColorResource(R.color.secondary_light);
            chip.setTextColor(ContextCompat.getColor(context, R.color.secondary));
            chip.setChipStrokeColorResource(R.color.secondary);
        } else if ("C".equalsIgnoreCase(grade) || "D".equalsIgnoreCase(grade)) {
            chip.setChipBackgroundColorResource(R.color.warning_light);
            chip.setTextColor(ContextCompat.getColor(context, R.color.warning));
            chip.setChipStrokeColorResource(R.color.warning);
        } else {
            chip.setChipBackgroundColorResource(R.color.error_light);
            chip.setTextColor(ContextCompat.getColor(context, R.color.error));
            chip.setChipStrokeColorResource(R.color.error);
        }
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubjectCode, tvSubjectName;
        Chip chipSubjectGrade;
        TextView tvInternalMarks, tvAssignmentMarks, tvExternalMarks;
        TextView tvSubjectTotal, tvSubjectGradePoint;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubjectCode = itemView.findViewById(R.id.tvSubjectCode);
            tvSubjectName = itemView.findViewById(R.id.tvSubjectName);
            chipSubjectGrade = itemView.findViewById(R.id.chipSubjectGrade);
            tvInternalMarks = itemView.findViewById(R.id.tvInternalMarks);
            tvAssignmentMarks = itemView.findViewById(R.id.tvAssignmentMarks);
            tvExternalMarks = itemView.findViewById(R.id.tvExternalMarks);
            tvSubjectTotal = itemView.findViewById(R.id.tvSubjectTotal);
            tvSubjectGradePoint = itemView.findViewById(R.id.tvSubjectGradePoint);
        }
    }
}
