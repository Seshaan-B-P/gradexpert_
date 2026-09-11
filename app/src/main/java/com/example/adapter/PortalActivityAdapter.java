package com.example.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.model.PortalActivity;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PortalActivityAdapter extends RecyclerView.Adapter<PortalActivityAdapter.ViewHolder> {

    public interface OnPortalActivityClickListener {
        void onActivityClick(PortalActivity activity);
    }

    private List<PortalActivity> activityList;
    private List<PortalActivity> filteredList;
    private OnPortalActivityClickListener listener;

    public PortalActivityAdapter(List<PortalActivity> activityList, OnPortalActivityClickListener listener) {
        this.activityList = activityList != null ? activityList : new ArrayList<>();
        this.filteredList = new ArrayList<>(this.activityList);
        this.listener = listener;
    }

    public void updateData(List<PortalActivity> newList) {
        this.activityList = newList != null ? newList : new ArrayList<>();
        this.filteredList = new ArrayList<>(this.activityList);
        notifyDataSetChanged();
    }

    public void filter(String query, String categoryFilter, String dateFilter) {
        filteredList.clear();
        String lowerQuery = query != null ? query.toLowerCase().trim() : "";

        long now = System.currentTimeMillis();
        long oneDayMs = 24 * 60 * 60 * 1000L;

        for (PortalActivity act : activityList) {
            boolean matchesCategory = (categoryFilter == null || "All Categories".equalsIgnoreCase(categoryFilter) || categoryFilter.equalsIgnoreCase(act.getEntityType()));

            boolean matchesDate = true;
            if (act.getTimestamp() != null) {
                long actTime = act.getTimestamp().toDate().getTime();
                if ("Today".equalsIgnoreCase(dateFilter)) {
                    matchesDate = (now - actTime) <= oneDayMs;
                } else if ("Yesterday".equalsIgnoreCase(dateFilter)) {
                    long diff = now - actTime;
                    matchesDate = diff > oneDayMs && diff <= (2 * oneDayMs);
                } else if ("Last 7 Days".equalsIgnoreCase(dateFilter)) {
                    matchesDate = (now - actTime) <= (7 * oneDayMs);
                } else if ("Last 30 Days".equalsIgnoreCase(dateFilter)) {
                    matchesDate = (now - actTime) <= (30 * oneDayMs);
                }
            }

            boolean matchesQuery = lowerQuery.isEmpty() ||
                    (act.getTitle() != null && act.getTitle().toLowerCase().contains(lowerQuery)) ||
                    (act.getDescription() != null && act.getDescription().toLowerCase().contains(lowerQuery)) ||
                    (act.getStudentName() != null && act.getStudentName().toLowerCase().contains(lowerQuery)) ||
                    (act.getRegisterNo() != null && act.getRegisterNo().toLowerCase().contains(lowerQuery)) ||
                    (act.getSubjectName() != null && act.getSubjectName().toLowerCase().contains(lowerQuery));

            if (matchesCategory && matchesDate && matchesQuery) {
                filteredList.add(act);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_portal_activity, parent, false);
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

        private final FrameLayout layoutIconBg;
        private final ImageView ivIcon;
        private final TextView tvTitle;
        private final TextView tvTime;
        private final TextView tvDesc;
        private final TextView tvEntity;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutIconBg = itemView.findViewById(R.id.layoutActivityIconBg);
            ivIcon = itemView.findViewById(R.id.ivActivityIcon);
            tvTitle = itemView.findViewById(R.id.tvActivityTitle);
            tvTime = itemView.findViewById(R.id.tvActivityTime);
            tvDesc = itemView.findViewById(R.id.tvActivityDesc);
            tvEntity = itemView.findViewById(R.id.tvActivityEntity);
        }

        public void bind(PortalActivity activity) {
            tvTitle.setText(activity.getTitle());
            tvDesc.setText(activity.getDescription());

            // Format relative date
            Timestamp ts = activity.getTimestamp();
            if (ts != null) {
                long timeMs = ts.toDate().getTime();
                CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                        timeMs, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE
                );
                tvTime.setText(relativeTime);
            } else {
                tvTime.setText("Just now");
            }

            // Target Entity line
            String entityStr = "";
            if (activity.getStudentName() != null && !activity.getStudentName().isEmpty()) {
                entityStr = "Student: " + activity.getStudentName() + (activity.getRegisterNo() != null ? " (" + activity.getRegisterNo() + ")" : "");
            } else if (activity.getSubjectName() != null && !activity.getSubjectName().isEmpty()) {
                entityStr = "Subject: " + activity.getSubjectName();
            } else if (activity.getEntityName() != null && !activity.getEntityName().isEmpty()) {
                entityStr = "Target: " + activity.getEntityName();
            }
            if (!entityStr.isEmpty()) {
                tvEntity.setText(entityStr);
                tvEntity.setVisibility(View.VISIBLE);
            } else {
                tvEntity.setVisibility(View.GONE);
            }

            // Icon & Color styling by Entity Type
            String type = activity.getEntityType() != null ? activity.getEntityType().toUpperCase() : "GENERAL";
            int iconRes = R.drawable.ic_report;
            String colorHex = "#4F46E5"; // Default Indigo

            if ("STUDENT".equals(type)) {
                iconRes = R.drawable.ic_students;
                colorHex = "#10B981"; // Emerald Green
            } else if ("SUBJECT".equals(type)) {
                iconRes = R.drawable.ic_subjects;
                colorHex = "#3B82F6"; // Blue
            } else if ("ATTENDANCE".equals(type)) {
                iconRes = R.drawable.ic_attendance;
                colorHex = "#F59E0B"; // Amber
            } else if ("ASSIGNMENT".equals(type)) {
                iconRes = R.drawable.ic_assignment;
                colorHex = "#8B5CF6"; // Purple
            } else if ("RESULT".equals(type)) {
                iconRes = R.drawable.ic_results;
                colorHex = "#EC4899"; // Pink
            } else if ("BROADCAST".equals(type)) {
                iconRes = R.drawable.ic_notifications;
                colorHex = "#06B6D4"; // Cyan
            } else if ("REPORT".equals(type)) {
                iconRes = R.drawable.ic_report;
                colorHex = "#6366F1"; // Indigo
            } else if ("AUTHENTICATION".equals(type)) {
                iconRes = R.drawable.ic_lock;
                colorHex = "#64748B"; // Slate
            }

            ivIcon.setImageResource(iconRes);

            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(Color.parseColor(colorHex));
            layoutIconBg.setBackground(drawable);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onActivityClick(activity);
            });
        }
    }
}
