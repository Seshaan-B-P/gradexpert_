package com.example.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.model.AssignmentSubmission;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SubmissionAdapter extends RecyclerView.Adapter<SubmissionAdapter.ViewHolder> {

    public interface OnSaveGradeListener {
        void onSaveGrade(AssignmentSubmission submission, double marks, String feedback);
    }

    private List<AssignmentSubmission> submissions;
    private double maxMarks;
    private OnSaveGradeListener listener;

    public SubmissionAdapter(List<AssignmentSubmission> submissions, double maxMarks, OnSaveGradeListener listener) {
        this.submissions = submissions != null ? submissions : new ArrayList<>();
        this.maxMarks = maxMarks;
        this.listener = listener;
    }

    public void updateList(List<AssignmentSubmission> newList, double maxMarks) {
        this.submissions = newList != null ? newList : new ArrayList<>();
        this.maxMarks = maxMarks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_submission, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(submissions.get(position));
    }

    @Override
    public int getItemCount() {
        return submissions.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvStudentName;
        private final TextView tvRegNo;
        private final Chip chipStatus;
        private final TextView tvSubmittedDate;
        private final TextView tvAnswerText;
        private final Chip chipFile;
        private final TextInputEditText etMarks;
        private final TextView tvMaxMarksLabel;
        private final TextInputEditText etFeedback;
        private final MaterialButton btnSaveGrade;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvSubStudentName);
            tvRegNo = itemView.findViewById(R.id.tvSubStudentRegNo);
            chipStatus = itemView.findViewById(R.id.chipSubStatus);
            tvSubmittedDate = itemView.findViewById(R.id.tvSubSubmittedDate);
            tvAnswerText = itemView.findViewById(R.id.tvSubAnswerText);
            chipFile = itemView.findViewById(R.id.chipSubFile);

            etMarks = itemView.findViewById(R.id.etSubMarks);
            tvMaxMarksLabel = itemView.findViewById(R.id.tvSubMaxMarksLabel);
            etFeedback = itemView.findViewById(R.id.etSubFeedback);
            btnSaveGrade = itemView.findViewById(R.id.btnSaveGrade);
        }

        public void bind(AssignmentSubmission item) {
            Context ctx = itemView.getContext();

            tvStudentName.setText(item.getStudentName());
            tvRegNo.setText(item.getRegisterNo());

            // Status chip
            String status = item.getStatus() != null ? item.getStatus().toUpperCase() : "SUBMITTED";
            chipStatus.setText(status);

            if ("GRADED".equals(status)) {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#10B981"))); // Emerald
            } else if ("LATE".equals(status)) {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#EF4444"))); // Red
            } else {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#4F46E5"))); // Indigo
            }

            // Submitted date
            if (item.getSubmittedAt() != null) {
                Date date = item.getSubmittedAt().toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US);
                tvSubmittedDate.setText("Submitted: " + sdf.format(date));
            } else {
                tvSubmittedDate.setText("Submitted");
            }

            // Answer text & File
            if (item.getAnswer() != null && !item.getAnswer().isEmpty()) {
                tvAnswerText.setText(item.getAnswer());
                tvAnswerText.setVisibility(View.VISIBLE);
            } else {
                tvAnswerText.setVisibility(View.GONE);
            }

            if (item.getAttachmentUrl() != null && !item.getAttachmentUrl().isEmpty()) {
                chipFile.setText("Solution File: " + item.getAttachmentUrl());
                chipFile.setVisibility(View.VISIBLE);
            } else {
                chipFile.setVisibility(View.GONE);
            }

            // Marks & Feedback fields
            tvMaxMarksLabel.setText("/ " + (int) maxMarks + " Marks");
            if (item.getMarks() > 0) {
                etMarks.setText(String.valueOf(item.getMarks()));
            } else {
                etMarks.setText("");
            }
            etFeedback.setText(item.getFeedback() != null ? item.getFeedback() : "");

            btnSaveGrade.setOnClickListener(v -> {
                String marksStr = etMarks.getText() != null ? etMarks.getText().toString().trim() : "";
                String feedbackStr = etFeedback.getText() != null ? etFeedback.getText().toString().trim() : "";

                double marks = 0;
                try {
                    marks = Double.parseDouble(marksStr);
                } catch (Exception e) {
                    Toast.makeText(ctx, "Enter valid marks.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (marks < 0 || marks > maxMarks) {
                    Toast.makeText(ctx, "Marks cannot exceed maximum marks (" + (int) maxMarks + ").", Toast.LENGTH_LONG).show();
                    return;
                }

                if (listener != null) {
                    listener.onSaveGrade(item, marks, feedbackStr);
                }
            });
        }
    }
}
