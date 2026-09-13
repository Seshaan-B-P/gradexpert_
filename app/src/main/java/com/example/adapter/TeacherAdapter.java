package com.example.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.model.Teacher;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class TeacherAdapter extends RecyclerView.Adapter<TeacherAdapter.TeacherViewHolder> {

    public interface OnTeacherClickListener {
        void onViewClick(Teacher teacher);
        void onEditClick(Teacher teacher);
        void onChangePasswordClick(Teacher teacher);
        void onStatusClick(Teacher teacher);
    }

    private List<Teacher> teacherList;
    private final OnTeacherClickListener listener;

    public TeacherAdapter(List<Teacher> teacherList, OnTeacherClickListener listener) {
        this.teacherList = teacherList != null ? teacherList : new ArrayList<>();
        this.listener = listener;
    }

    public void updateData(List<Teacher> newList) {
        this.teacherList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TeacherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_teacher, parent, false);
        return new TeacherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TeacherViewHolder holder, int position) {
        holder.bind(teacherList.get(position));
    }

    @Override
    public int getItemCount() {
        return teacherList.size();
    }

    class TeacherViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvEmployeeId;
        private final TextView tvDesignation;
        private final TextView tvEmail;
        private final Chip chipStatus;
        private final Chip chipDept;
        private final Chip chipSubjectsCount;
        private final MaterialButton btnView;
        private final MaterialButton btnEdit;
        private final MaterialButton btnChangePassword;
        private final MaterialButton btnStatus;

        public TeacherViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTeacherItemName);
            tvEmployeeId = itemView.findViewById(R.id.tvTeacherItemEmployeeId);
            tvDesignation = itemView.findViewById(R.id.tvTeacherItemDesignation);
            tvEmail = itemView.findViewById(R.id.tvTeacherItemEmail);
            chipStatus = itemView.findViewById(R.id.chipTeacherStatus);
            chipDept = itemView.findViewById(R.id.chipTeacherDept);
            chipSubjectsCount = itemView.findViewById(R.id.chipTeacherSubjectsCount);
            btnView = itemView.findViewById(R.id.btnViewTeacher);
            btnEdit = itemView.findViewById(R.id.btnEditTeacher);
            btnChangePassword = itemView.findViewById(R.id.btnChangePasswordTeacher);
            btnStatus = itemView.findViewById(R.id.btnStatusTeacher);
        }

        public void bind(Teacher teacher) {
            tvName.setText(teacher.getName());

            String empId = teacher.getEmployeeId();
            if (TextUtils.isEmpty(empId)) {
                empId = "ID: " + teacher.getId();
            } else {
                empId = "ID: " + empId;
            }
            tvEmployeeId.setText(empId);

            String level = teacher.getProgramLevel();
            if (TextUtils.isEmpty(level)) {
                level = com.example.model.Department.resolveDefaultProgramLevel(teacher.getDepartmentShortName(), teacher.getDepartment());
            }

            String dept = teacher.getDepartmentShortName();
            if (TextUtils.isEmpty(dept)) {
                dept = teacher.getDepartment();
            }
            if (TextUtils.isEmpty(dept)) {
                dept = "General";
            }

            chipDept.setText(level + " • " + dept);
            if ("PG".equalsIgnoreCase(level)) {
                chipDept.setTextColor(Color.parseColor("#7C3AED")); // Purple text
            } else {
                chipDept.setTextColor(Color.parseColor("#2563EB")); // Blue text
            }

            int count = teacher.getAssignedSubjectIds().size();
            if (chipSubjectsCount != null) {
                chipSubjectsCount.setText(count + (count == 1 ? " Subject" : " Subjects"));
            }

            String desig = !TextUtils.isEmpty(teacher.getDesignation()) ? teacher.getDesignation() : "Faculty";
            tvDesignation.setText(desig);

            tvEmail.setText(teacher.getEmail());

            String status = teacher.getStatus() != null ? teacher.getStatus().toUpperCase() : "ACTIVE";
            chipStatus.setText(status);

            if ("ACTIVE".equalsIgnoreCase(status)) {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#10B981"))); // Emerald
            } else if ("INACTIVE".equalsIgnoreCase(status)) {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#64748B"))); // Slate
            } else if ("SUSPENDED".equalsIgnoreCase(status)) {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#EF4444"))); // Red
            } else {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#10B981")));
            }

            btnView.setOnClickListener(v -> {
                if (listener != null) listener.onViewClick(teacher);
            });

            btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEditClick(teacher);
            });

            btnChangePassword.setOnClickListener(v -> {
                if (listener != null) listener.onChangePasswordClick(teacher);
            });

            btnStatus.setOnClickListener(v -> {
                if (listener != null) listener.onStatusClick(teacher);
            });
        }
    }
}
