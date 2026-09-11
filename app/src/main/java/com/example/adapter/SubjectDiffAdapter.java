package com.example.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for rendering individual subject diff rows in result comparison dialog.
 */
public class SubjectDiffAdapter extends RecyclerView.Adapter<SubjectDiffAdapter.ViewHolder> {

    public static class SubjectDiffItem {
        public String subjectName;
        public double oldMarks;
        public double newMarks;
        public String oldGrade;
        public String newGrade;
        public boolean isChanged;

        public SubjectDiffItem(String subjectName, double oldMarks, double newMarks, String oldGrade, String newGrade) {
            this.subjectName = subjectName;
            this.oldMarks = oldMarks;
            this.newMarks = newMarks;
            this.oldGrade = oldGrade != null ? oldGrade : "-";
            this.newGrade = newGrade != null ? newGrade : "-";
            this.isChanged = (Math.abs(oldMarks - newMarks) > 0.01) || !this.oldGrade.equalsIgnoreCase(this.newGrade);
        }
    }

    private List<SubjectDiffItem> diffList;

    public SubjectDiffAdapter(List<SubjectDiffItem> diffList) {
        this.diffList = diffList != null ? diffList : new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_result_diff_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position >= diffList.size()) return;
        SubjectDiffItem item = diffList.get(position);

        holder.tvSubjectName.setText(item.subjectName != null ? item.subjectName : "Subject");
        holder.tvOldMarks.setText(String.format(java.util.Locale.US, "%.0f", item.oldMarks));
        holder.tvOldGrade.setText("Grade: " + item.oldGrade);

        holder.tvNewMarks.setText(String.format(java.util.Locale.US, "%.0f", item.newMarks));
        holder.tvNewGrade.setText("Grade: " + item.newGrade);

        if (item.isChanged) {
            // Highlight changed subject with a subtle highlight background and bold indicator
            holder.layoutContainer.setBackgroundColor(Color.parseColor("#FEF3C7")); // light warning yellow
            holder.tvNewMarks.setTextColor(Color.parseColor("#4F46E5")); // Indigo
            holder.tvNewGrade.setTextColor(Color.parseColor("#4F46E5"));
            holder.tvDiffArrow.setTextColor(Color.parseColor("#F59E0B")); // Amber arrow
        } else {
            holder.layoutContainer.setBackgroundColor(Color.TRANSPARENT);
            holder.tvNewMarks.setTextColor(Color.parseColor("#374151"));
            holder.tvNewGrade.setTextColor(Color.parseColor("#6B7280"));
            holder.tvDiffArrow.setTextColor(Color.parseColor("#D1D5DB"));
        }
    }

    @Override
    public int getItemCount() {
        return diffList != null ? diffList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View layoutContainer;
        TextView tvSubjectName, tvOldMarks, tvOldGrade, tvDiffArrow, tvNewMarks, tvNewGrade;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutContainer = itemView.findViewById(R.id.layoutDiffRowContainer);
            tvSubjectName = itemView.findViewById(R.id.tvDiffSubjectName);
            tvOldMarks = itemView.findViewById(R.id.tvDiffOldMarks);
            tvOldGrade = itemView.findViewById(R.id.tvDiffOldGrade);
            tvDiffArrow = itemView.findViewById(R.id.tvDiffArrow);
            tvNewMarks = itemView.findViewById(R.id.tvDiffNewMarks);
            tvNewGrade = itemView.findViewById(R.id.tvDiffNewGrade);
        }
    }
}
