package com.example.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.model.ActivityItem;

import java.util.List;

/**
 * Adapter for rendering recent teacher activity logs in a RecyclerView.
 */
public class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.ViewHolder> {

    private final List<ActivityItem> activityList;

    public RecentActivityAdapter(List<ActivityItem> activityList) {
        this.activityList = activityList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ActivityItem item = activityList.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvCategory.setText(item.getCategory());
        holder.tvTime.setText(item.getTimestamp());
        holder.imgIcon.setImageResource(item.getIconResId());
    }

    @Override
    public int getItemCount() {
        return activityList != null ? activityList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView tvTitle, tvCategory, tvTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.imgActivityIcon);
            tvTitle = itemView.findViewById(R.id.tvActivityTitle);
            tvCategory = itemView.findViewById(R.id.tvActivityCategory);
            tvTime = itemView.findViewById(R.id.tvActivityTime);
        }
    }
}
