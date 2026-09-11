package com.example.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.model.AcademicReport;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TopperAdapter extends RecyclerView.Adapter<TopperAdapter.ViewHolder> {

    private List<AcademicReport> topperList;

    public TopperAdapter(List<AcademicReport> topperList) {
        this.topperList = topperList != null ? topperList : new ArrayList<>();
    }

    public void updateData(List<AcademicReport> newList) {
        this.topperList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_topper, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(topperList.get(position), position + 1);
    }

    @Override
    public int getItemCount() {
        return Math.min(topperList.size(), 5);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvRank;
        private final TextView tvName;
        private final TextView tvRegNo;
        private final TextView tvSgpa;
        private final TextView tvPercentage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvTopperRank);
            tvName = itemView.findViewById(R.id.tvTopperName);
            tvRegNo = itemView.findViewById(R.id.tvTopperRegNo);
            tvSgpa = itemView.findViewById(R.id.tvTopperSgpa);
            tvPercentage = itemView.findViewById(R.id.tvTopperPercentage);
        }

        public void bind(AcademicReport report, int rank) {
            tvRank.setText("#" + rank);
            tvName.setText(report.getStudentName());
            tvRegNo.setText("Register No: " + report.getRegisterNo());
            tvSgpa.setText(String.format(Locale.US, "SGPA: %.2f", report.getSgpa()));
            tvPercentage.setText(String.format(Locale.US, "%.1f%%", report.getAverageMarksPercentage()));
        }
    }
}
