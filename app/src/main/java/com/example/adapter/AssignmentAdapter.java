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
import com.example.model.Assignment;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class AssignmentAdapter extends RecyclerView.Adapter<AssignmentAdapter.ViewHolder> {

    public interface OnAssignmentClickListener {
        void onAssignmentClick(Assignment assignment);
    }

    private List<Assignment> list;
    private final OnAssignmentClickListener listener;

    public AssignmentAdapter(List<Assignment> list, OnAssignmentClickListener listener) {
        this.list = list != null ? list : new ArrayList<>();
        this.listener = listener;
    }

    public void updateData(List<Assignment> newList) {
        this.list = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_assignment, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(list.get(position));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvTitle;
        private final TextView tvSubject;
        private final TextView tvDeptSem;
        private final Chip chipStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvAssignmentTitle);
            tvSubject = itemView.findViewById(R.id.tvAssignmentSubject);
            tvDeptSem = itemView.findViewById(R.id.tvAssignmentDeptSem);
            chipStatus = itemView.findViewById(R.id.chipAssignmentStatus);
        }

        public void bind(Assignment assignment) {
            tvTitle.setText(assignment.getTitle() != null ? assignment.getTitle() : "Assignment");
            tvSubject.setText(assignment.getSubjectName() != null ? assignment.getSubjectName() : "Course");
            tvDeptSem.setText((assignment.getDepartment() != null ? assignment.getDepartment() : "Dept") + " • " + (assignment.getSemester() != null ? assignment.getSemester() : "Sem"));

            String status = assignment.getStatus() != null ? assignment.getStatus().toUpperCase() : "PUBLISHED";
            chipStatus.setText(status);

            if ("PUBLISHED".equalsIgnoreCase(status)) {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#10B981"))); // Green
            } else if ("CLOSED".equalsIgnoreCase(status)) {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#EF4444"))); // Red
            } else {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F59E0B"))); // Amber (Draft)
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onAssignmentClick(assignment);
            });
        }
    }
}
