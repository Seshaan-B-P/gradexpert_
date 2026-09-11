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
import com.example.model.Semester;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class SemesterAdapter extends RecyclerView.Adapter<SemesterAdapter.ViewHolder> {

    public interface OnSemesterClickListener {
        void onToggleStatus(Semester semester);
    }

    private List<Semester> list;
    private final OnSemesterClickListener listener;

    public SemesterAdapter(List<Semester> list, OnSemesterClickListener listener) {
        this.list = list != null ? list : new ArrayList<>();
        this.listener = listener;
    }

    public void updateData(List<Semester> newList) {
        this.list = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_semester, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(list.get(position));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvTitle;
        private final TextView tvNumber;
        private final Chip chipStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvSemesterTitle);
            tvNumber = itemView.findViewById(R.id.tvSemesterNumber);
            chipStatus = itemView.findViewById(R.id.chipSemesterStatus);
        }

        public void bind(Semester item) {
            tvTitle.setText(item.getSemesterTitle());
            tvNumber.setText("Term " + item.getSemesterNumber() + " • Active Curriculum");

            String status = item.getStatus() != null ? item.getStatus().toUpperCase() : "ACTIVE";
            chipStatus.setText(status);

            if ("ACTIVE".equalsIgnoreCase(status)) {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#10B981"))); // Green
            } else {
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#EF4444"))); // Red
            }

            chipStatus.setOnClickListener(v -> {
                if (listener != null) listener.onToggleStatus(item);
            });
        }
    }
}
