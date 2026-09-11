package com.example.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.model.SubjectAttendanceStats;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for Student Subject-wise Attendance breakdown.
 */
public class StudentSubjectAttendanceAdapter extends RecyclerView.Adapter<StudentSubjectAttendanceAdapter.ViewHolder> {

    private List<SubjectAttendanceStats> list;

    public StudentSubjectAttendanceAdapter(List<SubjectAttendanceStats> list) {
        this.list = list != null ? list : new ArrayList<>();
    }

    public void updateList(List<SubjectAttendanceStats> newList) {
        this.list = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_subject_attendance, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SubjectAttendanceStats item = list.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvSubCode;
        private final TextView tvSubName;
        private final TextView tvSubStatusPill;
        private final ProgressBar pbAttendance;
        private final TextView tvCounts;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubCode = itemView.findViewById(R.id.tvSubCode);
            tvSubName = itemView.findViewById(R.id.tvSubName);
            tvSubStatusPill = itemView.findViewById(R.id.tvSubStatusPill);
            pbAttendance = itemView.findViewById(R.id.pbSubjectAttendance);
            tvCounts = itemView.findViewById(R.id.tvSubjectCounts);
        }

        public void bind(SubjectAttendanceStats item) {
            tvSubCode.setText(item.getSubjectCode());
            tvSubName.setText(item.getSubjectName());
            tvCounts.setText(item.getAttendedClasses() + " / " + item.getTotalClasses() + " Classes");

            int pct = item.getPercentage();
            pbAttendance.setProgress(pct);

            String statusText;
            int badgeColor;

            if (pct >= 85) {
                statusText = "Excellent (" + pct + "%)";
                badgeColor = Color.parseColor("#10B981"); // Green
            } else if (pct >= 75) {
                statusText = "Good (" + pct + "%)";
                badgeColor = Color.parseColor("#0EA5E9"); // Sky Blue
            } else if (pct >= 65) {
                statusText = "⚠️ LOW ATTENDANCE (" + pct + "%)";
                badgeColor = Color.parseColor("#F59E0B"); // Amber
            } else {
                statusText = "🚨 CRITICAL (<75% Cutoff: " + pct + "%)";
                badgeColor = Color.parseColor("#EF4444"); // Bright Red
            }

            tvSubStatusPill.setText(statusText);
            tvSubStatusPill.setBackgroundTintList(ColorStateList.valueOf(badgeColor));
            pbAttendance.setProgressTintList(ColorStateList.valueOf(badgeColor));
        }
    }
}
