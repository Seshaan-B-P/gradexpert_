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
import com.example.model.Subject;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.ViewHolder> {

    public interface OnSubjectClickListener {
        void onViewClick(Subject subject);
        void onEditClick(Subject subject);
        void onStatusToggleClick(Subject subject);
        void onDeleteClick(Subject subject);
        default void onUnassignClick(Subject subject) {}
    }

    private List<Subject> subjectList;
    private List<Subject> filteredList;
    private OnSubjectClickListener listener;
    private boolean isTeacherMode = false;

    public SubjectAdapter(List<Subject> subjectList, OnSubjectClickListener listener) {
        this.subjectList = subjectList != null ? subjectList : new ArrayList<>();
        this.filteredList = new ArrayList<>(this.subjectList);
        this.listener = listener;
    }

    public void setTeacherMode(boolean teacherMode) {
        this.isTeacherMode = teacherMode;
        notifyDataSetChanged();
    }

    public void updateData(List<Subject> newList) {
        this.subjectList = newList != null ? newList : new ArrayList<>();
        this.filteredList = new ArrayList<>(this.subjectList);
        notifyDataSetChanged();
    }

    public void filter(String query, String programLevelFilter, String deptFilter, String semFilter, String statusFilter) {
        filteredList.clear();
        String lowerQuery = query != null ? query.toLowerCase().trim() : "";

        for (Subject s : subjectList) {
            String level = s.getProgramLevel() != null ? s.getProgramLevel().toUpperCase() : "UG";
            boolean matchesLevel = (programLevelFilter == null || "ALL".equalsIgnoreCase(programLevelFilter) || programLevelFilter.equalsIgnoreCase(level));
            boolean matchesDept = Subject.isDepartmentMatching(deptFilter, s.getDepartment()) || Subject.isDepartmentMatching(deptFilter, s.getDepartmentShortName());
            boolean matchesSem = Subject.isSemesterMatching(semFilter, s.getSemester());
            boolean matchesStatus = (statusFilter == null || "ALL".equalsIgnoreCase(statusFilter) || statusFilter.equalsIgnoreCase(s.getStatus()));

            boolean matchesQuery = lowerQuery.isEmpty() ||
                    (s.getSubjectName() != null && s.getSubjectName().toLowerCase().contains(lowerQuery)) ||
                    (s.getSubjectCode() != null && s.getSubjectCode().toLowerCase().contains(lowerQuery)) ||
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subject, parent, false);
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
        private final TextView tvCode;
        private final TextView tvDept;
        private final TextView tvSem;
        private final TextView tvCreditsType;
        private final Chip chipStatus;

        private final MaterialButton btnView;
        private final MaterialButton btnEdit;
        private final MaterialButton btnStatusAction;
        private final MaterialButton btnDeleteAction;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvSubjectItemName);
            tvCode = itemView.findViewById(R.id.tvSubjectItemCode);
            tvDept = itemView.findViewById(R.id.tvSubjectItemDept);
            tvSem = itemView.findViewById(R.id.tvSubjectItemSem);
            tvCreditsType = itemView.findViewById(R.id.tvSubjectItemCreditsType);
            chipStatus = itemView.findViewById(R.id.chipSubjectItemStatus);

            btnView = itemView.findViewById(R.id.btnViewSubject);
            btnEdit = itemView.findViewById(R.id.btnEditSubjectAction);
            btnStatusAction = itemView.findViewById(R.id.btnStatusSubjectAction);
            btnDeleteAction = itemView.findViewById(R.id.btnDeleteSubjectAction);
        }

        public void bind(Subject subject) {
            String name = subject.getSubjectName();
            if (name == null || name.trim().isEmpty()) {
                name = subject.getName();
            }
            if (name == null || name.trim().isEmpty()) {
                name = subject.getSubjectCode();
            }
            if (name == null || name.trim().isEmpty()) {
                name = "Subject " + (subject.getSubjectId() != null ? subject.getSubjectId() : "");
            }
            tvName.setText(name);

            String code = subject.getSubjectCode();
            if (code == null || code.trim().isEmpty()) {
                code = subject.getCode();
            }
            if (code != null && !code.trim().isEmpty()) {
                tvCode.setText("Subject Code: " + code);
            } else {
                tvCode.setText("Subject Code: " + (subject.getSubjectId() != null ? subject.getSubjectId() : "N/A"));
            }

            String level = subject.getProgramLevel() != null ? subject.getProgramLevel().toUpperCase() : "UG";
            String deptName = !TextUtils.isEmpty(subject.getDepartmentShortName()) ? subject.getDepartmentShortName() : subject.getDepartment();
            tvDept.setText("Program: [" + level + "] " + (deptName != null ? deptName : ""));

            tvSem.setText("Semester: " + (subject.getSemester() != null ? subject.getSemester() : ""));

            String creditsTypeStr = "Credits: " + subject.getCredits() + " • Type: " + subject.getSubjectType();
            if (subject.getWeeklyHours() > 0) {
                creditsTypeStr += " • Weekly Hours: " + subject.getWeeklyHours();
            }
            tvCreditsType.setText(creditsTypeStr);

            String status = subject.getStatus() != null ? subject.getStatus().toUpperCase() : "ACTIVE";
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
                if (listener != null) listener.onViewClick(subject);
            });

            if (isTeacherMode) {
                btnEdit.setVisibility(View.GONE);
                btnStatusAction.setVisibility(View.GONE);
                if (btnDeleteAction != null) {
                    btnDeleteAction.setVisibility(View.VISIBLE);
                    btnDeleteAction.setText("UNASSIGN");
                    btnDeleteAction.setTextColor(Color.parseColor("#DC2626"));
                    btnDeleteAction.setOnClickListener(v -> {
                        if (listener != null) listener.onUnassignClick(subject);
                    });
                }
            } else {
                btnEdit.setVisibility(View.VISIBLE);
                btnStatusAction.setVisibility(View.VISIBLE);
                btnEdit.setOnClickListener(v -> {
                    if (listener != null) listener.onEditClick(subject);
                });
                btnStatusAction.setOnClickListener(v -> {
                    if (listener != null) listener.onStatusToggleClick(subject);
                });
                if (btnDeleteAction != null) {
                    btnDeleteAction.setVisibility(View.VISIBLE);
                    btnDeleteAction.setText("DELETE");
                    btnDeleteAction.setTextColor(Color.parseColor("#EF4444"));
                    btnDeleteAction.setOnClickListener(v -> {
                        if (listener != null) listener.onDeleteClick(subject);
                    });
                }
            }
        }
    }
}
