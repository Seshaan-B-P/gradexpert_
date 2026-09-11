package com.example.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.model.Result;
import com.example.model.SubjectGradeItem;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView Adapter for rendering subject result rows in result preview & student results views,
 * as well as overall student exam result records for admin monitoring views.
 */
public class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.ViewHolder> {

    public interface OnResultClickListener {
        void onResultClick(Result result);
    }

    public interface OnAdminResultActionListener {
        void onViewResult(Result result);
        void onApproveResult(Result result);
        void onRejectResult(Result result);
    }

    private static final int VIEW_TYPE_SUBJECT = 1;
    private static final int VIEW_TYPE_RESULT = 2;

    private List<SubjectGradeItem> itemList;
    private List<Result> resultList;
    private OnResultClickListener listener;
    private OnAdminResultActionListener adminActionListener;
    private boolean isResultMode = false;
    private boolean showAdminActions = false;

    /**
     * Constructor for SubjectGradeItem mode (used in PublishResultsActivity & ResultsActivity)
     */
    public ResultAdapter(List<SubjectGradeItem> itemList) {
        this.itemList = itemList != null ? itemList : new ArrayList<>();
        this.isResultMode = false;
    }

    /**
     * Constructor for Result mode (used in AdminResultsActivity)
     */
    public ResultAdapter(List<Result> resultList, OnResultClickListener listener) {
        this.resultList = resultList != null ? resultList : new ArrayList<>();
        this.listener = listener;
        this.isResultMode = true;
        this.showAdminActions = false;
    }

    /**
     * Constructor with admin action listener
     */
    public ResultAdapter(List<Result> resultList, boolean showAdminActions, OnAdminResultActionListener adminActionListener) {
        this.resultList = resultList != null ? resultList : new ArrayList<>();
        this.showAdminActions = showAdminActions;
        this.adminActionListener = adminActionListener;
        this.isResultMode = true;
    }

    public void setShowAdminActions(boolean show) {
        this.showAdminActions = show;
        notifyDataSetChanged();
    }

    @SuppressWarnings("unchecked")
    public void updateData(List<?> newList) {
        if (isResultMode) {
            this.resultList = newList != null ? (List<Result>) newList : new ArrayList<>();
        } else {
            this.itemList = newList != null ? (List<SubjectGradeItem>) newList : new ArrayList<>();
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return isResultMode ? VIEW_TYPE_RESULT : VIEW_TYPE_SUBJECT;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_RESULT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_result, parent, false);
            return new ResultViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_result_subject, parent, false);
            return new SubjectViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (holder instanceof ResultViewHolder && resultList != null && position < resultList.size()) {
            Result r = resultList.get(position);
            ResultViewHolder rHolder = (ResultViewHolder) holder;

            String name = r.getStudentName();
            if (name == null || name.trim().isEmpty()) {
                name = "Student ID: " + r.getStudentId();
            }
            rHolder.tvStudentName.setText(name);

            String regNo = r.getRegNo();
            String dept = r.getDepartment();
            StringBuilder details = new StringBuilder();
            if (regNo != null && !regNo.trim().isEmpty()) {
                details.append("Reg: ").append(regNo).append(" • ");
            }
            if (dept != null && !dept.trim().isEmpty()) {
                details.append(dept).append(" • ");
            }
            details.append("Semester ").append(r.getSemester());
            rHolder.tvDetails.setText(details.toString());

            rHolder.tvMarks.setText(String.format(Locale.US, "%.1f (%.1f%%)", r.getTotalMarks(), r.getPercentage()));
            rHolder.tvSgpa.setText(String.format(Locale.US, "%.2f", r.getSgpa()));
            rHolder.tvCgpa.setText(String.format(Locale.US, "%.2f", r.getCgpa()));

            String status = r.getStatus() != null ? r.getStatus().toUpperCase() : "PENDING_APPROVAL";
            rHolder.chipStatus.setText(status);
            if ("APPROVED".equals(status) || "PUBLISHED".equals(status)) {
                rHolder.chipStatus.setChipBackgroundColor(ColorStateList.valueOf(
                        ContextCompat.getColor(rHolder.itemView.getContext(), R.color.success)));
            } else if ("REJECTED".equals(status)) {
                rHolder.chipStatus.setChipBackgroundColor(ColorStateList.valueOf(
                        ContextCompat.getColor(rHolder.itemView.getContext(), R.color.error)));
            } else {
                rHolder.chipStatus.setText("PENDING");
                rHolder.chipStatus.setChipBackgroundColor(ColorStateList.valueOf(
                        ContextCompat.getColor(rHolder.itemView.getContext(), R.color.warning)));
            }

            if (r.getTeacherName() != null && !r.getTeacherName().trim().isEmpty()) {
                rHolder.tvTeacher.setText("Submitted by: " + r.getTeacherName());
            } else {
                rHolder.tvTeacher.setText("Submitted by: Faculty");
            }

            String date = r.getSubmittedAtString() != null ? r.getSubmittedAtString() : r.getPublishedDate();
            if (date != null && !date.trim().isEmpty()) {
                rHolder.tvDate.setText("Date: " + date);
                rHolder.tvDate.setVisibility(View.VISIBLE);
            } else {
                rHolder.tvDate.setVisibility(View.GONE);
            }

            if (showAdminActions && ("PENDING_APPROVAL".equalsIgnoreCase(r.getStatus()) || "PENDING".equalsIgnoreCase(r.getStatus()) || "DRAFT".equalsIgnoreCase(r.getStatus()))) {
                rHolder.layoutActions.setVisibility(View.VISIBLE);
                rHolder.btnView.setOnClickListener(v -> {
                    if (adminActionListener != null) adminActionListener.onViewResult(r);
                });
                rHolder.btnApprove.setOnClickListener(v -> {
                    if (adminActionListener != null) adminActionListener.onApproveResult(r);
                });
                rHolder.btnReject.setOnClickListener(v -> {
                    if (adminActionListener != null) adminActionListener.onRejectResult(r);
                });
            } else {
                rHolder.layoutActions.setVisibility(View.GONE);
            }

            if (listener != null) {
                rHolder.itemView.setOnClickListener(v -> listener.onResultClick(r));
            }
        } else if (holder instanceof SubjectViewHolder && itemList != null && position < itemList.size()) {
            SubjectGradeItem item = itemList.get(position);
            SubjectViewHolder sHolder = (SubjectViewHolder) holder;

            sHolder.tvSubjectName.setText(item.getSubjectName());
            sHolder.tvSubjectMarks.setText(String.format(Locale.US, "%.0f", item.getTotalMarks()));
            sHolder.tvSubjectGrade.setText(item.getGrade());
            sHolder.tvSubjectGradePoint.setText(String.valueOf(item.getGradePoint()));
            sHolder.tvSubjectCredits.setText(String.valueOf(item.getCredits()));
        }
    }

    @Override
    public int getItemCount() {
        if (isResultMode) {
            return resultList != null ? resultList.size() : 0;
        } else {
            return itemList != null ? itemList.size() : 0;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public static class SubjectViewHolder extends ViewHolder {
        TextView tvSubjectName, tvSubjectMarks, tvSubjectGrade, tvSubjectGradePoint, tvSubjectCredits;

        public SubjectViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubjectName = itemView.findViewById(R.id.tvSubjectName);
            tvSubjectMarks = itemView.findViewById(R.id.tvSubjectMarks);
            tvSubjectGrade = itemView.findViewById(R.id.tvSubjectGrade);
            tvSubjectGradePoint = itemView.findViewById(R.id.tvSubjectGradePoint);
            tvSubjectCredits = itemView.findViewById(R.id.tvSubjectCredits);
        }
    }

    public static class ResultViewHolder extends ViewHolder {
        TextView tvStudentName, tvDetails, tvMarks, tvSgpa, tvCgpa, tvDate, tvTeacher;
        Chip chipStatus;
        View layoutActions;
        View btnView, btnApprove, btnReject;

        public ResultViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvAdminResultStudentName);
            tvDetails = itemView.findViewById(R.id.tvAdminResultDetails);
            tvMarks = itemView.findViewById(R.id.tvAdminResultMarks);
            tvSgpa = itemView.findViewById(R.id.tvAdminResultSGPA);
            tvCgpa = itemView.findViewById(R.id.tvAdminResultCGPA);
            chipStatus = itemView.findViewById(R.id.chipAdminResultStatus);
            tvDate = itemView.findViewById(R.id.tvAdminResultDate);
            tvTeacher = itemView.findViewById(R.id.tvAdminResultTeacher);
            layoutActions = itemView.findViewById(R.id.layoutAdminResultActions);
            btnView = itemView.findViewById(R.id.btnAdminViewResult);
            btnApprove = itemView.findViewById(R.id.btnAdminApproveResult);
            btnReject = itemView.findViewById(R.id.btnAdminRejectResult);
        }
    }
}
