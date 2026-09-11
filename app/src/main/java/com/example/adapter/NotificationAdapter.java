package com.example.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import com.example.R;
import com.example.model.AppNotification;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface OnNotificationClickListener {
        void onItemClick(AppNotification notification);
        void onDeleteClick(AppNotification notification, int position);
    }

    private List<AppNotification> list;
    private OnNotificationClickListener listener;

    public NotificationAdapter(List<AppNotification> list) {
        this.list = list;
    }

    public NotificationAdapter(List<AppNotification> list, OnNotificationClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    public void updateData(List<AppNotification> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppNotification notif = list.get(position);
        holder.tvTitle.setText(notif.getTitle());
        holder.tvMessage.setText(notif.getMessage());
        holder.tvDate.setText(notif.getDate());
        holder.tvTargetRole.setText(notif.getTargetRole() != null ? notif.getTargetRole() : "ALL");
        holder.tvSender.setText("Sent by: " + (notif.getSender() != null ? notif.getSender() : "Faculty Admin"));

        String cat = notif.getCategory() != null ? notif.getCategory().toUpperCase() : "GENERAL";
        holder.tvCategory.setText(cat);

        // Styling based on category
        switch (cat) {
            case "URGENT":
                holder.tvCategory.setTextColor(Color.parseColor("#D32F2F"));
                holder.ivIcon.setImageResource(R.drawable.ic_notifications);
                holder.ivIcon.setColorFilter(Color.parseColor("#D32F2F"));
                break;
            case "EXAM":
                holder.tvCategory.setTextColor(Color.parseColor("#7B1FA2"));
                holder.ivIcon.setImageResource(R.drawable.ic_grade);
                holder.ivIcon.setColorFilter(Color.parseColor("#7B1FA2"));
                break;
            case "ASSIGNMENT":
                holder.tvCategory.setTextColor(Color.parseColor("#1976D2"));
                holder.ivIcon.setImageResource(R.drawable.ic_assignment);
                holder.ivIcon.setColorFilter(Color.parseColor("#1976D2"));
                break;
            case "ATTENDANCE":
                holder.tvCategory.setTextColor(Color.parseColor("#F57C00"));
                holder.ivIcon.setImageResource(R.drawable.ic_attendance);
                holder.ivIcon.setColorFilter(Color.parseColor("#F57C00"));
                break;
            default: // GENERAL or CIRCULAR
                holder.tvCategory.setTextColor(Color.parseColor("#00796B"));
                holder.ivIcon.setImageResource(R.drawable.ic_notifications);
                holder.ivIcon.setColorFilter(Color.parseColor("#00796B"));
                break;
        }

        // Unread dot
        holder.viewUnreadDot.setVisibility(notif.isRead() ? View.GONE : View.VISIBLE);

        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            notif.setRead(true);
            holder.viewUnreadDot.setVisibility(View.GONE);
            if (listener != null) {
                listener.onItemClick(notif);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(notif, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvDate, tvCategory, tvTargetRole, tvSender;
        ImageView ivIcon;
        View viewUnreadDot;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotifTitle);
            tvMessage = itemView.findViewById(R.id.tvNotifMessage);
            tvDate = itemView.findViewById(R.id.tvNotifDate);
            tvCategory = itemView.findViewById(R.id.tvNotifCategory);
            tvTargetRole = itemView.findViewById(R.id.tvNotifTargetRole);
            tvSender = itemView.findViewById(R.id.tvNotifSender);
            ivIcon = itemView.findViewById(R.id.ivNotifIcon);
            viewUnreadDot = itemView.findViewById(R.id.viewUnreadDot);
            btnDelete = itemView.findViewById(R.id.btnDeleteNotif);
        }
    }
}

