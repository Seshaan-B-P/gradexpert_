package com.example.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.model.Student;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.ViewHolder> {

    public interface OnStudentClickListener {
        void onViewClick(Student student);
        void onEditClick(Student student);
        void onStatusToggleClick(Student student);
    }

    private List<Student> studentList;
    private List<Student> filteredList;
    private OnStudentClickListener listener;

    public StudentAdapter(List<Student> studentList, OnStudentClickListener listener) {
        this.studentList = studentList != null ? studentList : new ArrayList<>();
        this.filteredList = new ArrayList<>(this.studentList);
        this.listener = listener;
    }

    public void updateData(List<Student> newList) {
        this.studentList = newList != null ? newList : new ArrayList<>();
        this.filteredList = new ArrayList<>(this.studentList);
        notifyDataSetChanged();
    }

    public void filter(String query, String programLevelFilter, String deptFilter, String semFilter, String statusFilter) {
        filteredList.clear();
        String lowerQuery = query != null ? query.toLowerCase().trim() : "";

        for (Student s : studentList) {
            String level = s.getProgramLevel() != null ? s.getProgramLevel().toUpperCase() : "UG";
            boolean matchesLevel = (programLevelFilter == null || "ALL".equalsIgnoreCase(programLevelFilter) || programLevelFilter.equalsIgnoreCase(level));
            boolean matchesDept = (deptFilter == null || "All Departments".equalsIgnoreCase(deptFilter) || "All".equalsIgnoreCase(deptFilter) || deptFilter.equalsIgnoreCase(s.getDepartment()) || deptFilter.equalsIgnoreCase(s.getDepartmentShortName()));
            boolean matchesSem = (semFilter == null || "All Semesters".equalsIgnoreCase(semFilter) || "All".equalsIgnoreCase(semFilter) || semFilter.equalsIgnoreCase(s.getSemester()));
            boolean matchesStatus = (statusFilter == null || "ALL".equalsIgnoreCase(statusFilter) || statusFilter.equalsIgnoreCase(s.getStatus()));

            boolean matchesQuery = lowerQuery.isEmpty() ||
                    (s.getName() != null && s.getName().toLowerCase().contains(lowerQuery)) ||
                    (s.getRegisterNo() != null && s.getRegisterNo().toLowerCase().contains(lowerQuery)) ||
                    (s.getEmail() != null && s.getEmail().toLowerCase().contains(lowerQuery)) ||
                    level.toLowerCase().contains(lowerQuery);

            if (matchesLevel && matchesDept && matchesSem && matchesStatus && matchesQuery) {
                filteredList.add(s);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student, parent, false);
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

        private final TextView tvName;
        private final TextView tvRegNo;
        private final TextView tvDept;
        private final TextView tvSem;
        private final TextView tvEmail;
        private final Chip chipStatus;

        private final MaterialButton btnView;
        private final MaterialButton btnEdit;
        private final MaterialButton btnStatusAction;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvStudentItemName);
            tvRegNo = itemView.findViewById(R.id.tvStudentItemRegNo);
            tvDept = itemView.findViewById(R.id.tvStudentItemDept);
            tvSem = itemView.findViewById(R.id.tvStudentItemSem);
            tvEmail = itemView.findViewById(R.id.tvStudentItemEmail);
            chipStatus = itemView.findViewById(R.id.chipStudentItemStatus);

            btnView = itemView.findViewById(R.id.btnViewStudent);
            btnEdit = itemView.findViewById(R.id.btnEditStudentAction);
            btnStatusAction = itemView.findViewById(R.id.btnStatusStudentAction);
        }

        public void bind(Student student) {
            tvName.setText(student.getName());
            tvRegNo.setText(student.getRegisterNo() != null ? student.getRegisterNo() : "");

            String level = student.getProgramLevel() != null ? student.getProgramLevel().toUpperCase() : "UG";
            String deptDisplay = student.getDepartmentShortName();
            if (TextUtils.isEmpty(deptDisplay)) {
                deptDisplay = student.getDepartment();
            }
            if (TextUtils.isEmpty(deptDisplay)) {
                deptDisplay = "General";
            }

            tvDept.setText(level + " • " + deptDisplay);
            if ("PG".equals(level)) {
                tvDept.setTextColor(Color.parseColor("#7C3AED")); // Purple
            } else {
                tvDept.setTextColor(Color.parseColor("#2563EB")); // Blue
            }

            tvSem.setText(student.getSemester() != null ? student.getSemester() : "");
            tvEmail.setText(student.getEmail() != null ? student.getEmail() : "");

            String status = student.getStatus() != null ? student.getStatus().toUpperCase() : "ACTIVE";
            chipStatus.setText(status);

            if ("ACTIVE".equals(status)) {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#10B981"))); // Green
                btnStatusAction.setText("DEACTIVATE");
                btnStatusAction.setTextColor(Color.parseColor("#EF4444")); // Red
            } else {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#64748B"))); // Slate Grey
                btnStatusAction.setText("RESTORE");
                btnStatusAction.setTextColor(Color.parseColor("#10B981")); // Green
            }

            btnView.setOnClickListener(v -> {
                if (listener != null) listener.onViewClick(student);
            });

            btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEditClick(student);
            });

            btnStatusAction.setOnClickListener(v -> {
                if (listener != null) listener.onStatusToggleClick(student);
            });
        }
    }
}
