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
import com.example.model.Department;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class DepartmentAdapter extends RecyclerView.Adapter<DepartmentAdapter.ViewHolder> {

    public interface OnDepartmentActionListener {
        void onEditClick(Department department);
        void onToggleStatusClick(Department department);
    }

    private List<Department> departmentList;
    private final OnDepartmentActionListener listener;

    public DepartmentAdapter(List<Department> departmentList, OnDepartmentActionListener listener) {
        this.departmentList = departmentList != null ? departmentList : new ArrayList<>();
        this.listener = listener;
    }

    public void updateData(List<Department> newList) {
        this.departmentList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_department, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(departmentList.get(position));
    }

    @Override
    public int getItemCount() {
        return departmentList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvName;
        private final TextView tvCode;
        private final Chip chipLevel;
        private final Chip chipStatus;
        private final MaterialButton btnEdit;
        private final MaterialButton btnToggleStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvDeptName);
            tvCode = itemView.findViewById(R.id.tvDeptCode);
            chipLevel = itemView.findViewById(R.id.chipDeptLevel);
            chipStatus = itemView.findViewById(R.id.chipDeptStatus);
            btnEdit = itemView.findViewById(R.id.btnEditDept);
            btnToggleStatus = itemView.findViewById(R.id.btnToggleDeptStatus);
        }

        public void bind(Department dept) {
            tvName.setText(dept.getDepartmentName());
            tvCode.setText("Code: " + dept.getDepartmentCode());

            String level = dept.getProgramLevel() != null ? dept.getProgramLevel().toUpperCase() : "UG";
            chipLevel.setText(level);
            if ("PG".equals(level)) {
                chipLevel.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#7C3AED"))); // Purple
            } else {
                chipLevel.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#2563EB"))); // Blue
            }

            String status = dept.getStatus() != null ? dept.getStatus().toUpperCase() : "ACTIVE";
            chipStatus.setText(status);

            if ("ACTIVE".equalsIgnoreCase(status)) {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#10B981"))); // Green
                btnToggleStatus.setText("DEACTIVATE");
            } else {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#EF4444"))); // Red
                btnToggleStatus.setText("ACTIVATE");
            }

            btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEditClick(dept);
            });

            btnToggleStatus.setOnClickListener(v -> {
                if (listener != null) listener.onToggleStatusClick(dept);
            });
        }
    }
}
