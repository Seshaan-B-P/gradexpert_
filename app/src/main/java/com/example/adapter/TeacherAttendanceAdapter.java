package com.example.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.model.AttendanceRecord;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for Teacher Attendance marking and editing.
 */
public class TeacherAttendanceAdapter extends RecyclerView.Adapter<TeacherAttendanceAdapter.ViewHolder> {

    private List<AttendanceRecord> records;

    public TeacherAttendanceAdapter(List<AttendanceRecord> records) {
        this.records = records != null ? records : new ArrayList<>();
    }

    public void updateList(List<AttendanceRecord> newList) {
        this.records = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<AttendanceRecord> getRecords() {
        return records;
    }

    public void markAllPresent() {
        for (AttendanceRecord rec : records) {
            rec.setStatus("PRESENT");
        }
        notifyDataSetChanged();
    }

    public void markAllAbsent() {
        for (AttendanceRecord rec : records) {
            rec.setStatus("ABSENT");
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_teacher_attendance, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AttendanceRecord item = records.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvName;
        private final TextView tvRegNo;
        private final MaterialButtonToggleGroup toggleStatus;
        private final com.google.android.material.button.MaterialButton btnPresent;
        private final com.google.android.material.button.MaterialButton btnAbsent;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvStudentName);
            tvRegNo = itemView.findViewById(R.id.tvStudentRegNo);
            toggleStatus = itemView.findViewById(R.id.toggleAttendanceStatus);
            btnPresent = itemView.findViewById(R.id.btnStatusPresent);
            btnAbsent = itemView.findViewById(R.id.btnStatusAbsent);
        }

        public void bind(AttendanceRecord record) {
            tvName.setText(record.getStudentName());
            tvRegNo.setText(record.getStudentRegNo());

            toggleStatus.clearOnButtonCheckedListeners();

            boolean isPresent = record.isPresent();
            updateToggleStyles(isPresent);

            if (isPresent) {
                toggleStatus.check(R.id.btnStatusPresent);
            } else {
                toggleStatus.check(R.id.btnStatusAbsent);
            }

            toggleStatus.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked) {
                    boolean p = (checkedId == R.id.btnStatusPresent);
                    record.setStatus(p ? "PRESENT" : "ABSENT");
                    updateToggleStyles(p);
                }
            });
        }

        private void updateToggleStyles(boolean isPresent) {
            android.content.Context ctx = itemView.getContext();
            if (isPresent) {
                btnPresent.setBackgroundColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.success_light));
                btnPresent.setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.success));
                btnPresent.setStrokeColor(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(ctx, R.color.success)));

                btnAbsent.setBackgroundColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.white));
                btnAbsent.setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.text_secondary));
                btnAbsent.setStrokeColor(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(ctx, R.color.border_color)));
            } else {
                btnAbsent.setBackgroundColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.error_light));
                btnAbsent.setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.error));
                btnAbsent.setStrokeColor(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(ctx, R.color.error)));

                btnPresent.setBackgroundColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.white));
                btnPresent.setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.text_secondary));
                btnPresent.setStrokeColor(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(ctx, R.color.border_color)));
            }
        }
    }
}
