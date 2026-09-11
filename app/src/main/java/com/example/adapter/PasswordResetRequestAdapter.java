package com.example.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.model.PasswordResetRequest;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PasswordResetRequestAdapter extends RecyclerView.Adapter<PasswordResetRequestAdapter.ViewHolder> {

    public interface OnRequestClickListener {
        void onReviewClick(PasswordResetRequest request);
    }

    private List<PasswordResetRequest> requestList;
    private final OnRequestClickListener listener;

    public PasswordResetRequestAdapter(List<PasswordResetRequest> requestList, OnRequestClickListener listener) {
        this.requestList = requestList != null ? requestList : new ArrayList<>();
        this.listener = listener;
    }

    public void updateData(List<PasswordResetRequest> newList) {
        this.requestList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_password_reset_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(requestList.get(position));
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvName;
        private final TextView tvIdentifier;
        private final TextView tvEmail;
        private final TextView tvRequestedTime;
        private final Chip chipRole;
        private final Chip chipStatus;
        private final MaterialButton btnReview;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvRequesterName);
            tvIdentifier = itemView.findViewById(R.id.tvRequesterIdentifier);
            tvEmail = itemView.findViewById(R.id.tvRequesterEmail);
            tvRequestedTime = itemView.findViewById(R.id.tvRequestedTime);
            chipRole = itemView.findViewById(R.id.chipRequesterRole);
            chipStatus = itemView.findViewById(R.id.chipRequestStatus);
            btnReview = itemView.findViewById(R.id.btnReviewRequest);
        }

        public void bind(PasswordResetRequest request) {
            tvName.setText(request.getUserName() != null ? request.getUserName() : "User");
            tvEmail.setText(request.getEmail() != null ? request.getEmail() : "");

            String role = request.getRole() != null ? request.getRole().toUpperCase() : "STUDENT";
            chipRole.setText(role);

            String idStr = request.getIdentifier() != null ? request.getIdentifier() : "N/A";
            if ("TEACHER".equalsIgnoreCase(role)) {
                tvIdentifier.setText("Emp ID: " + idStr);
            } else {
                tvIdentifier.setText("Reg No: " + idStr);
            }

            String status = request.getStatus() != null ? request.getStatus().toUpperCase() : "PENDING";
            chipStatus.setText(status);

            if ("PENDING".equalsIgnoreCase(status)) {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F59E0B"))); // Amber
            } else if ("COMPLETED".equalsIgnoreCase(status) || "APPROVED".equalsIgnoreCase(status)) {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#10B981"))); // Emerald
            } else if ("REJECTED".equalsIgnoreCase(status)) {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#EF4444"))); // Red
            } else {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#64748B"))); // Slate
            }

            Date date = request.getRequestedAt();
            if (date != null) {
                CharSequence relative = DateUtils.getRelativeTimeSpanString(
                        date.getTime(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE
                );
                tvRequestedTime.setText("Requested: " + relative);
            } else {
                tvRequestedTime.setText("Requested: Just now");
            }

            btnReview.setOnClickListener(v -> {
                if (listener != null) listener.onReviewClick(request);
            });
        }
    }
}
