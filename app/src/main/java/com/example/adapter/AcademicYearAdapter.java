package com.example.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.model.AcademicYear;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class AcademicYearAdapter extends RecyclerView.Adapter<AcademicYearAdapter.ViewHolder> {

    public interface OnAcademicYearClickListener {
        void onSetCurrentClick(AcademicYear year);
    }

    private List<AcademicYear> list;
    private final OnAcademicYearClickListener listener;

    public AcademicYearAdapter(List<AcademicYear> list, OnAcademicYearClickListener listener) {
        this.list = list != null ? list : new ArrayList<>();
        this.listener = listener;
    }

    public void updateData(List<AcademicYear> newList) {
        this.list = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_academic_year, parent, false);
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
        private final Chip chipCurrent;
        private final MaterialButton btnSetCurrent;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvYearTitle);
            chipCurrent = itemView.findViewById(R.id.chipCurrentBadge);
            btnSetCurrent = itemView.findViewById(R.id.btnSetCurrentYear);
        }

        public void bind(AcademicYear item) {
            tvTitle.setText(item.getYearTitle());

            if (item.isCurrent()) {
                chipCurrent.setVisibility(View.VISIBLE);
                btnSetCurrent.setVisibility(View.GONE);
            } else {
                chipCurrent.setVisibility(View.GONE);
                btnSetCurrent.setVisibility(View.VISIBLE);
            }

            btnSetCurrent.setOnClickListener(v -> {
                if (listener != null) listener.onSetCurrentClick(item);
            });
        }
    }
}
