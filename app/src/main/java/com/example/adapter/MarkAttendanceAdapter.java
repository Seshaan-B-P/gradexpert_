package com.example.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.model.Attendance;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for Hour-Based Attendance marking in MarkAttendanceActivity.
 */
public class MarkAttendanceAdapter extends RecyclerView.Adapter<MarkAttendanceAdapter.ViewHolder> {

    public interface OnStatusChangeListener {
        void onStatusChanged();
    }

    private List<Attendance> attendanceList;
    private List<Attendance> filteredList;
    private OnStatusChangeListener statusChangeListener;

    public MarkAttendanceAdapter(List<Attendance> attendanceList, OnStatusChangeListener statusChangeListener) {
        this.attendanceList = attendanceList != null ? attendanceList : new ArrayList<>();
        this.filteredList = new ArrayList<>(this.attendanceList);
        this.statusChangeListener = statusChangeListener;
    }

    public void updateList(List<Attendance> newList) {
        this.attendanceList = newList != null ? newList : new ArrayList<>();
        this.filteredList = new ArrayList<>(this.attendanceList);
        notifyDataSetChanged();
        if (statusChangeListener != null) statusChangeListener.onStatusChanged();
    }

    public void filter(String query) {
        filteredList.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(attendanceList);
        } else {
            String lower = query.toLowerCase().trim();
            for (Attendance item : attendanceList) {
                if ((item.getStudentName() != null && item.getStudentName().toLowerCase().contains(lower)) ||
                    (item.getRegisterNo() != null && item.getRegisterNo().toLowerCase().contains(lower))) {
                    filteredList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    public List<Attendance> getAllRecords() {
        return attendanceList;
    }

    public void markAllPresent() {
        for (Attendance item : attendanceList) {
            item.setStatus("PRESENT");
        }
        notifyDataSetChanged();
        if (statusChangeListener != null) statusChangeListener.onStatusChanged();
    }

    public void markAllAbsent() {
        for (Attendance item : attendanceList) {
            item.setStatus("ABSENT");
        }
        notifyDataSetChanged();
        if (statusChangeListener != null) statusChangeListener.onStatusChanged();
    }

    public int getPresentCount() {
        int count = 0;
        for (Attendance item : attendanceList) {
            if ("PRESENT".equalsIgnoreCase(item.getStatus())) {
                count++;
            }
        }
        return count;
    }

    public int getAbsentCount() {
        int count = 0;
        for (Attendance item : attendanceList) {
            if ("ABSENT".equalsIgnoreCase(item.getStatus())) {
                count++;
            }
        }
        return count;
    }

    public boolean hasUnmarkedStudents() {
        for (Attendance item : attendanceList) {
            if (item.isUnmarked()) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_teacher_attendance, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Attendance item = filteredList.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvName;
        private final TextView tvRegNo;
        private final MaterialButtonToggleGroup toggleStatus;
        private final MaterialButton btnPresent;
        private final MaterialButton btnAbsent;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvStudentName);
            tvRegNo = itemView.findViewById(R.id.tvStudentRegNo);
            toggleStatus = itemView.findViewById(R.id.toggleAttendanceStatus);
            btnPresent = itemView.findViewById(R.id.btnStatusPresent);
            btnAbsent = itemView.findViewById(R.id.btnStatusAbsent);
        }

        public void bind(Attendance record) {
            tvName.setText(record.getStudentName());
            tvRegNo.setText(record.getRegisterNo());

            toggleStatus.clearOnButtonCheckedListeners();

            if (record.isPresent()) {
                toggleStatus.check(R.id.btnStatusPresent);
                updateToggleStyles(true, false);
            } else if (record.isAbsent()) {
                toggleStatus.check(R.id.btnStatusAbsent);
                updateToggleStyles(false, true);
            } else {
                toggleStatus.clearChecked();
                updateToggleStyles(false, false);
            }

            toggleStatus.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked) {
                    if (checkedId == R.id.btnStatusPresent) {
                        record.setStatus("PRESENT");
                        updateToggleStyles(true, false);
                    } else if (checkedId == R.id.btnStatusAbsent) {
                        record.setStatus("ABSENT");
                        updateToggleStyles(false, true);
                    }
                    if (statusChangeListener != null) statusChangeListener.onStatusChanged();
                }
            });
        }

        private void updateToggleStyles(boolean isPresent, boolean isAbsent) {
            Context ctx = itemView.getContext();

            if (isPresent) {
                btnPresent.setBackgroundColor(ContextCompat.getColor(ctx, R.color.success_light));
                btnPresent.setTextColor(ContextCompat.getColor(ctx, R.color.success));
                btnPresent.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.success)));

                btnAbsent.setBackgroundColor(ContextCompat.getColor(ctx, R.color.white));
                btnAbsent.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary));
                btnAbsent.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.border_color)));
            } else if (isAbsent) {
                btnAbsent.setBackgroundColor(ContextCompat.getColor(ctx, R.color.error_light));
                btnAbsent.setTextColor(ContextCompat.getColor(ctx, R.color.error));
                btnAbsent.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.error)));

                btnPresent.setBackgroundColor(ContextCompat.getColor(ctx, R.color.white));
                btnPresent.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary));
                btnPresent.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.border_color)));
            } else {
                // Unmarked state
                btnPresent.setBackgroundColor(ContextCompat.getColor(ctx, R.color.white));
                btnPresent.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary));
                btnPresent.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.border_color)));

                btnAbsent.setBackgroundColor(ContextCompat.getColor(ctx, R.color.white));
                btnAbsent.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary));
                btnAbsent.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.border_color)));
            }
        }
    }
}
