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
import com.example.model.AcademicReport;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AcademicReportAdapter extends RecyclerView.Adapter<AcademicReportAdapter.ViewHolder> {

    public interface OnAcademicReportClickListener {
        void onViewReportClick(AcademicReport report);
    }

    private List<AcademicReport> reportList;
    private List<AcademicReport> filteredList;
    private OnAcademicReportClickListener listener;

    public AcademicReportAdapter(List<AcademicReport> reportList, OnAcademicReportClickListener listener) {
        this.reportList = reportList != null ? reportList : new ArrayList<>();
        this.filteredList = new ArrayList<>(this.reportList);
        this.listener = listener;
    }

    public void updateData(List<AcademicReport> newList) {
        this.reportList = newList != null ? newList : new ArrayList<>();
        this.filteredList = new ArrayList<>(this.reportList);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        filteredList.clear();
        String lower = query != null ? query.toLowerCase().trim() : "";

        for (AcademicReport r : reportList) {
            boolean matchesName = r.getStudentName() != null && r.getStudentName().toLowerCase().contains(lower);
            boolean matchesRegNo = r.getRegisterNo() != null && r.getRegisterNo().toLowerCase().contains(lower);

            if (lower.isEmpty() || matchesName || matchesRegNo) {
                filteredList.add(r);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_academic_report, parent, false);
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
        private final TextView tvDeptSem;
        private final Chip chipStatus;

        private final TextView tvMarksVal;
        private final TextView tvAttendanceVal;
        private final TextView tvSgpaVal;
        private final MaterialButton btnViewReport;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvReportStudentName);
            tvRegNo = itemView.findViewById(R.id.tvReportStudentRegNo);
            tvDeptSem = itemView.findViewById(R.id.tvReportStudentDeptSem);
            chipStatus = itemView.findViewById(R.id.chipReportStatus);

            tvMarksVal = itemView.findViewById(R.id.tvReportMarksVal);
            tvAttendanceVal = itemView.findViewById(R.id.tvReportAttendanceVal);
            tvSgpaVal = itemView.findViewById(R.id.tvReportSgpaVal);
            btnViewReport = itemView.findViewById(R.id.btnViewStudentReport);
        }

        public void bind(AcademicReport report) {
            tvName.setText(report.getStudentName());
            tvRegNo.setText("Register No: " + report.getRegisterNo());
            tvDeptSem.setText(report.getDepartment() + " • " + report.getSemester());

            tvMarksVal.setText(String.format(Locale.US, "%.1f%%", report.getAverageMarksPercentage()));
            tvAttendanceVal.setText(String.format(Locale.US, "%.1f%%", report.getAttendancePercentage()));
            tvSgpaVal.setText(String.format(Locale.US, "%.2f", report.getSgpa()));

            String status = report.getAcademicStatus() != null ? report.getAcademicStatus() : "AVERAGE";
            chipStatus.setText(status);

            if ("EXCELLENT".equalsIgnoreCase(status)) {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#10B981"))); // Emerald Green
            } else if ("GOOD".equalsIgnoreCase(status)) {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#3B82F6"))); // Blue
            } else if ("AVERAGE".equalsIgnoreCase(status)) {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F59E0B"))); // Amber
            } else {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#EF4444"))); // Red
            }

            btnViewReport.setOnClickListener(v -> {
                if (listener != null) listener.onViewReportClick(report);
            });
        }
    }
}
