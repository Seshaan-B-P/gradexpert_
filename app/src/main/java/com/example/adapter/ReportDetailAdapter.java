package com.example.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import com.example.R;
import com.example.model.ReportRowItem;

public class ReportDetailAdapter extends RecyclerView.Adapter<ReportDetailAdapter.ViewHolder> {

    private List<ReportRowItem> itemList;

    public ReportDetailAdapter(List<ReportRowItem> itemList) {
        this.itemList = itemList;
    }

    public void updateList(List<ReportRowItem> newList) {
        this.itemList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report_detail_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReportRowItem item = itemList.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvSubtitle.setText(item.getSubtitle());

        if (item.getBadgeText() != null && !item.getBadgeText().isEmpty()) {
            holder.tvBadge.setVisibility(View.VISIBLE);
            holder.tvBadge.setText(item.getBadgeText());
            holder.tvBadge.setBackgroundTintList(ColorStateList.valueOf(item.getBadgeBgColor()));
            holder.tvBadge.setTextColor(item.getBadgeTextColor());
        } else {
            holder.tvBadge.setVisibility(View.GONE);
        }

        holder.tvMetric.setText(item.getMetricText());
        holder.tvLabel.setText(item.getMetricLabel());

        if (item.getIconResId() != 0) {
            holder.ivIcon.setImageResource(item.getIconResId());
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_grade);
        }
    }

    @Override
    public int getItemCount() {
        return itemList != null ? itemList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle, tvBadge, tvMetric, tvLabel;
        ImageView ivIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvReportRowTitle);
            tvSubtitle = itemView.findViewById(R.id.tvReportRowSubtitle);
            tvBadge = itemView.findViewById(R.id.tvReportRowBadge);
            tvMetric = itemView.findViewById(R.id.tvReportRowMetric);
            tvLabel = itemView.findViewById(R.id.tvReportRowLabel);
            ivIcon = itemView.findViewById(R.id.ivReportRowIcon);
        }
    }
}
