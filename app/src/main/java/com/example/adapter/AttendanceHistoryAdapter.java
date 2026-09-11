package com.example.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.model.AttendanceHistoryItem;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AttendanceHistoryAdapter extends RecyclerView.Adapter<AttendanceHistoryAdapter.ViewHolder> {

    private List<AttendanceHistoryItem> items;

    public AttendanceHistoryAdapter(List<AttendanceHistoryItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public void updateList(List<AttendanceHistoryItem> newList) {
        this.items = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendance_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvSubject;
        private final TextView tvDate;
        private final Chip chipHour;
        private final TextView tvPresent;
        private final TextView tvAbsent;
        private final TextView tvPercentage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubject = itemView.findViewById(R.id.tvHistorySubject);
            tvDate = itemView.findViewById(R.id.tvHistoryDate);
            chipHour = itemView.findViewById(R.id.chipHistoryHour);
            tvPresent = itemView.findViewById(R.id.tvHistoryPresent);
            tvAbsent = itemView.findViewById(R.id.tvHistoryAbsent);
            tvPercentage = itemView.findViewById(R.id.tvHistoryPercentage);
        }

        public void bind(AttendanceHistoryItem item) {
            tvSubject.setText(item.getSubjectName());
            tvDate.setText(item.getDate());
            chipHour.setText("Hour " + item.getHour());
            tvPresent.setText(item.getPresentCount() + " Students");
            tvAbsent.setText(item.getAbsentCount() + " Students");
            tvPercentage.setText(String.format(Locale.US, "%.2f%%", item.getPercentage()));
        }
    }
}
