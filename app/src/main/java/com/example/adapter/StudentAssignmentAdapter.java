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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StudentAssignmentAdapter extends RecyclerView.Adapter<StudentAssignmentAdapter.ViewHolder> {

    public interface OnStudentAssignmentClickListener {
        void onViewTeacherPdf(Assignment assignment);
        void onSubmitSolution(Assignment assignment);
    }

    private List<Assignment> list;
    private OnStudentAssignmentClickListener listener;

    public StudentAssignmentAdapter(List<Assignment> list, OnStudentAssignmentClickListener listener) {
        this.list = list != null ? list : new ArrayList<>();
        this.listener = listener;
    }

    public void updateList(List<Assignment> newList) {
        this.list = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_assignment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Assignment assignment = list.get(position);
        holder.bind(assignment);
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvSubCode;
        private final TextView tvStatusBadge;
        private final TextView tvTitle;
        private final TextView tvDesc;
        private final TextView tvDeadline;
        private final TextView tvSubmissionDate;
        private final Chip chipAttachment;
        private final MaterialButton btnSubmit;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubCode = itemView.findViewById(R.id.tvStudentSubCode);
            tvStatusBadge = itemView.findViewById(R.id.tvStudentStatusBadge);
            tvTitle = itemView.findViewById(R.id.tvStudentAssignTitle);
            tvDesc = itemView.findViewById(R.id.tvStudentAssignDesc);
            tvDeadline = itemView.findViewById(R.id.tvStudentDeadline);
            tvSubmissionDate = itemView.findViewById(R.id.tvSubmissionDate);
            chipAttachment = itemView.findViewById(R.id.chipTeacherAttachment);
            btnSubmit = itemView.findViewById(R.id.btnSubmitSolution);
        }

        public void bind(Assignment assignment) {
            tvSubCode.setText(assignment.getSubjectName() != null ? assignment.getSubjectName() : "Course");
            tvTitle.setText(assignment.getTitle());
            tvDesc.setText(assignment.getDescription());

            if (assignment.getDueDate() != null) {
                Date date = assignment.getDueDate().toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.US);
                tvDeadline.setText("Deadline: " + sdf.format(date));
            } else {
                tvDeadline.setText("No Deadline");
            }

            String status = assignment.getStatus() != null ? assignment.getStatus().toUpperCase() : "PENDING";
            tvStatusBadge.setText(status);

            if ("SUBMITTED".equalsIgnoreCase(status) || "GRADED".equalsIgnoreCase(status)) {
                tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#10B981"))); // Green
                btnSubmit.setText("View / Resubmit");
            } else if ("LATE".equalsIgnoreCase(status)) {
                tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EF4444"))); // Red
                btnSubmit.setText("View / Resubmit");
            } else {
                tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F59E0B"))); // Orange
                btnSubmit.setText("View & Submit");
            }

            String file = assignment.getAttachmentName();
            if (file != null && !file.isEmpty()) {
                chipAttachment.setText(file);
                chipAttachment.setVisibility(View.VISIBLE);
            } else {
                chipAttachment.setVisibility(View.GONE);
            }

            chipAttachment.setOnClickListener(v -> {
                if (listener != null) listener.onViewTeacherPdf(assignment);
            });

            btnSubmit.setOnClickListener(v -> {
                if (listener != null) listener.onSubmitSolution(assignment);
            });
        }
    }
}
