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
import com.example.model.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class UserControlAdapter extends RecyclerView.Adapter<UserControlAdapter.ViewHolder> {

    public interface OnUserActionListener {
        void onViewClick(User user);
        void onEditClick(User user);
        void onChangePasswordClick(User user);
        void onStatusChangeClick(User user);
    }

    private List<User> userList;
    private final OnUserActionListener listener;

    public UserControlAdapter(List<User> userList, OnUserActionListener listener) {
        this.userList = userList != null ? userList : new ArrayList<>();
        this.listener = listener;
    }

    public void updateData(List<User> newList) {
        this.userList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_control, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(userList.get(position));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvName;
        private final TextView tvIdentifier;
        private final TextView tvEmail;
        private final TextView tvDept;
        private final Chip chipStatus;
        private final Chip chipRole;
        private final MaterialButton btnViewUser;
        private final MaterialButton btnEditUser;
        private final MaterialButton btnChangeUserPassword;
        private final MaterialButton btnToggleUserStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItemUserName);
            tvIdentifier = itemView.findViewById(R.id.tvItemUserIdentifier);
            tvEmail = itemView.findViewById(R.id.tvItemUserEmail);
            tvDept = itemView.findViewById(R.id.tvItemUserDept);
            chipStatus = itemView.findViewById(R.id.chipUserStatus);
            chipRole = itemView.findViewById(R.id.chipUserRole);
            btnViewUser = itemView.findViewById(R.id.btnViewUser);
            btnEditUser = itemView.findViewById(R.id.btnEditUser);
            btnChangeUserPassword = itemView.findViewById(R.id.btnChangeUserPassword);
            btnToggleUserStatus = itemView.findViewById(R.id.btnToggleUserStatus);
        }

        public void bind(User user) {
            tvName.setText(user.getName() != null ? user.getName() : "User");
            tvEmail.setText(user.getEmail() != null ? user.getEmail() : "");

            String idStr = user.getIdentifier();
            if (idStr == null || idStr.isEmpty()) {
                idStr = String.valueOf(user.getId());
            }

            String role = user.getRole() != null ? user.getRole().toUpperCase() : "STUDENT";
            chipRole.setText(role);

            if ("STUDENT".equalsIgnoreCase(role)) {
                tvIdentifier.setText("Reg No: " + idStr);
            } else if ("TEACHER".equalsIgnoreCase(role)) {
                tvIdentifier.setText("Emp ID: " + idStr);
            } else {
                tvIdentifier.setText("Admin ID: " + idStr);
            }

            tvDept.setText("Identifier: " + idStr);

            String status = user.getStatus() != null ? user.getStatus().toUpperCase() : "ACTIVE";
            chipStatus.setText(status);

            if ("ACTIVE".equalsIgnoreCase(status)) {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#10B981"))); // Green
            } else if ("INACTIVE".equalsIgnoreCase(status)) {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#64748B"))); // Slate
            } else {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#EF4444"))); // Red
            }

            if (btnViewUser != null) {
                btnViewUser.setOnClickListener(v -> {
                    if (listener != null) listener.onViewClick(user);
                });
            }

            if (btnEditUser != null) {
                btnEditUser.setOnClickListener(v -> {
                    if (listener != null) listener.onEditClick(user);
                });
            }

            if (btnChangeUserPassword != null) {
                btnChangeUserPassword.setOnClickListener(v -> {
                    if (listener != null) listener.onChangePasswordClick(user);
                });
            }

            if (btnToggleUserStatus != null) {
                btnToggleUserStatus.setOnClickListener(v -> {
                    if (listener != null) listener.onStatusChangeClick(user);
                });
            }
        }
    }
}
