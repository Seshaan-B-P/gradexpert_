package com.example.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.model.BroadcastAlert;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class BroadcastAlertAdapter extends RecyclerView.Adapter<BroadcastAlertAdapter.ViewHolder> {

    public interface OnBroadcastAlertClickListener {
        void onViewClick(BroadcastAlert alert);
        void onActionClick(BroadcastAlert alert);
    }

    private List<BroadcastAlert> alertList;
    private List<BroadcastAlert> filteredList;
    private OnBroadcastAlertClickListener listener;

    public BroadcastAlertAdapter(List<BroadcastAlert> alertList, OnBroadcastAlertClickListener listener) {
        this.alertList = alertList != null ? alertList : new ArrayList<>();
        this.filteredList = new ArrayList<>(this.alertList);
        this.listener = listener;
    }

    public void updateData(List<BroadcastAlert> newList) {
        this.alertList = newList != null ? newList : new ArrayList<>();
        this.filteredList = new ArrayList<>(this.alertList);
        notifyDataSetChanged();
    }

    public void filter(String query, String statusFilter, String targetFilter) {
        filteredList.clear();
        String lowerQuery = query != null ? query.toLowerCase().trim() : "";

        for (BroadcastAlert a : alertList) {
            boolean matchesStatus = (statusFilter == null || "ALL".equalsIgnoreCase(statusFilter) || statusFilter.equalsIgnoreCase(a.getStatus()));
            boolean matchesTarget = (targetFilter == null || "All Targets".equalsIgnoreCase(targetFilter) || targetFilter.equalsIgnoreCase(a.getTargetType()));

            boolean matchesQuery = lowerQuery.isEmpty() ||
                    (a.getTitle() != null && a.getTitle().toLowerCase().contains(lowerQuery)) ||
                    (a.getMessage() != null && a.getMessage().toLowerCase().contains(lowerQuery));

            if (matchesStatus && matchesTarget && matchesQuery) {
                filteredList.add(a);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_broadcast_alert, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(filteredList.get(position));
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvTitle;
        private final TextView tvMessage;
        private final TextView tvTarget;
        private final TextView tvPriority;
        private final Chip chipStatus;
        private final MaterialButton btnView;
        private final MaterialButton btnAction;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvAlertItemTitle);
            tvMessage = itemView.findViewById(R.id.tvAlertItemMessage);
            tvTarget = itemView.findViewById(R.id.tvAlertItemTarget);
            tvPriority = itemView.findViewById(R.id.tvAlertItemPriority);
            chipStatus = itemView.findViewById(R.id.chipAlertItemStatus);
            btnView = itemView.findViewById(R.id.btnViewAlert);
            btnAction = itemView.findViewById(R.id.btnActionAlert);
        }

        public void bind(BroadcastAlert alert) {
            tvTitle.setText("📢 " + alert.getTitle());
            tvMessage.setText(alert.getMessage());

            String targetStr = "Target: All Students";
            if ("DEPARTMENT".equalsIgnoreCase(alert.getTargetType())) {
                targetStr = "Target: " + (alert.getDepartment() != null ? alert.getDepartment() : "Dept");
            } else if ("DEPARTMENT_SEMESTER".equalsIgnoreCase(alert.getTargetType())) {
                targetStr = "Target: " + (alert.getDepartment() != null ? alert.getDepartment() : "Dept") + " - " + (alert.getSemester() != null ? alert.getSemester() : "Sem");
            } else if ("SPECIFIC_STUDENT".equalsIgnoreCase(alert.getTargetType())) {
                targetStr = "Target: " + (alert.getStudentName() != null ? alert.getStudentName() : "Student");
            }
            tvTarget.setText(targetStr);

            String priority = alert.getPriority() != null ? alert.getPriority().toUpperCase() : "NORMAL";
            tvPriority.setText("Priority: " + priority);
            if ("URGENT".equalsIgnoreCase(priority)) {
                tvPriority.setTextColor(Color.parseColor("#EF4444")); // Red
            } else if ("HIGH".equalsIgnoreCase(priority)) {
                tvPriority.setTextColor(Color.parseColor("#F59E0B")); // Amber
            } else {
                tvPriority.setTextColor(Color.parseColor("#4F46E5")); // Indigo
            }

            String status = alert.getStatus() != null ? alert.getStatus().toUpperCase() : "PUBLISHED";
            chipStatus.setText(status);

            if ("PUBLISHED".equals(status)) {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#10B981"))); // Green
                btnAction.setText("VIEW DETAILS");
                btnAction.setTextColor(Color.parseColor("#4F46E5"));
            } else if ("SCHEDULED".equals(status)) {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F59E0B"))); // Amber
                btnAction.setText("CANCEL");
                btnAction.setTextColor(Color.parseColor("#EF4444"));
            } else if ("DRAFT".equals(status)) {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#64748B"))); // Slate
                btnAction.setText("DELETE DRAFT");
                btnAction.setTextColor(Color.parseColor("#EF4444"));
            } else {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#94A3B8")));
                btnAction.setText("VIEW");
                btnAction.setTextColor(Color.parseColor("#64748B"));
            }

            btnView.setOnClickListener(v -> {
                if (listener != null) listener.onViewClick(alert);
            });

            btnAction.setOnClickListener(v -> {
                if (listener != null) listener.onActionClick(alert);
            });
        }
    }
}
