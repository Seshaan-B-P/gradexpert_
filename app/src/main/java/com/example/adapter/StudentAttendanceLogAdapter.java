package com.example.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.model.StudentAttendanceLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying student attendance monitoring logs.
 */
public class StudentAttendanceLogAdapter extends RecyclerView.Adapter<StudentAttendanceLogAdapter.LogViewHolder> {

    public interface OnLogActionListener {
        void onDeleteLog(StudentAttendanceLog log);
    }

    private List<StudentAttendanceLog> logs = new ArrayList<>();
    private final OnLogActionListener listener;

    public StudentAttendanceLogAdapter(OnLogActionListener listener) {
        this.listener = listener;
    }

    public void updateLogs(List<StudentAttendanceLog> newLogs) {
        this.logs = newLogs != null ? newLogs : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_attendance_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        StudentAttendanceLog item = logs.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvLogSubjectName, tvLogDate, tvLogPercentage;
        private final TextView tvLogClassesCount, tvLogThresholdInfo, tvLogAdvice, tvLogNotes;
        private final LinearLayout layoutLogAdvice;
        private final ImageView ivLogStatusIcon, btnDeleteLog;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLogSubjectName = itemView.findViewById(R.id.tvLogSubjectName);
            tvLogDate = itemView.findViewById(R.id.tvLogDate);
            tvLogPercentage = itemView.findViewById(R.id.tvLogPercentage);
            tvLogClassesCount = itemView.findViewById(R.id.tvLogClassesCount);
            tvLogThresholdInfo = itemView.findViewById(R.id.tvLogThresholdInfo);
            tvLogAdvice = itemView.findViewById(R.id.tvLogAdvice);
            tvLogNotes = itemView.findViewById(R.id.tvLogNotes);
            layoutLogAdvice = itemView.findViewById(R.id.layoutLogAdvice);
            ivLogStatusIcon = itemView.findViewById(R.id.ivLogStatusIcon);
            btnDeleteLog = itemView.findViewById(R.id.btnDeleteLog);
        }

        public void bind(StudentAttendanceLog log, OnLogActionListener listener) {
            tvLogSubjectName.setText(log.getSubjectName());
            tvLogDate.setText("Logged on: " + log.getLoggedDate());
            tvLogPercentage.setText(String.format(Locale.US, "%.1f%%", log.getPercentage()));
            tvLogClassesCount.setText(String.format(Locale.US, "Attended: %d / %d Classes", log.getAttendedClasses(), log.getTotalClasses()));
            tvLogThresholdInfo.setText(String.format(Locale.US, "Threshold: %.0f%%", log.getThreshold()));

            boolean isLow = log.isBelowThreshold();
            if (isLow) {
                tvLogPercentage.setBackgroundResource(R.drawable.bg_pill_rose);
                layoutLogAdvice.setBackgroundColor(Color.parseColor("#FEF2F2"));
                ivLogStatusIcon.setImageResource(R.drawable.ic_info);
                ivLogStatusIcon.setColorFilter(Color.parseColor("#EF4444"));

                int needed = log.getClassesNeededForThreshold();
                if (needed > 0) {
                    tvLogAdvice.setText(String.format(Locale.US, "⚠️ Low Attendance Alert: Attend next %d consecutive classes to reach %.0f%%.", needed, log.getThreshold()));
                } else {
                    tvLogAdvice.setText(String.format(Locale.US, "⚠️ Below %.0f%% cutoff: Attendance is currently critical.", log.getThreshold()));
                }
                tvLogAdvice.setTextColor(Color.parseColor("#991B1B"));
            } else {
                tvLogPercentage.setBackgroundResource(R.drawable.bg_pill_emerald);
                layoutLogAdvice.setBackgroundColor(Color.parseColor("#ECFDF5"));
                ivLogStatusIcon.setImageResource(R.drawable.ic_check_circle);
                ivLogStatusIcon.setColorFilter(Color.parseColor("#10B981"));

                int canMiss = log.getClassesCanAffordToMiss();
                if (canMiss > 0) {
                    tvLogAdvice.setText(String.format(Locale.US, "✓ Safe: Above %.0f%% cutoff. Can miss up to %d class(es) without falling below threshold.", log.getThreshold(), canMiss));
                } else {
                    tvLogAdvice.setText(String.format(Locale.US, "✓ Safe: Currently meeting the %.0f%% requirement.", log.getThreshold()));
                }
                tvLogAdvice.setTextColor(Color.parseColor("#065F46"));
            }

            if (log.getNotes() != null && !log.getNotes().trim().isEmpty()) {
                tvLogNotes.setVisibility(View.VISIBLE);
                tvLogNotes.setText("Note: " + log.getNotes().trim());
            } else {
                tvLogNotes.setVisibility(View.GONE);
            }

            btnDeleteLog.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteLog(log);
                }
            });
        }
    }
}
