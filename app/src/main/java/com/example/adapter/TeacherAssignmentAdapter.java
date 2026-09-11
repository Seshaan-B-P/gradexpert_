package com.example.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.model.Assignment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class TeacherAssignmentAdapter extends RecyclerView.Adapter<TeacherAssignmentAdapter.ViewHolder> {

    public interface OnAssignmentActionListener {
        void onViewSubmissions(Assignment assignment);
        void onPublish(Assignment assignment);
        void onClose(Assignment assignment);
        void onDelete(Assignment assignment);
    }

    private List<Assignment> masterList;
    private List<Assignment> filteredList;
    private final OnAssignmentActionListener listener;

    public TeacherAssignmentAdapter(List<Assignment> list, OnAssignmentActionListener listener) {
        this.masterList = list != null ? new ArrayList<>(list) : new ArrayList<>();
        this.filteredList = list != null ? new ArrayList<>(list) : new ArrayList<>();
        this.listener = listener;
    }

    public void updateData(List<Assignment> newList) {
        this.masterList = newList != null ? new ArrayList<>(newList) : new ArrayList<>();
        this.filteredList = new ArrayList<>(this.masterList);
        notifyDataSetChanged();
    }

    public void updateList(List<Assignment> newList) {
        updateData(newList);
    }

    public void filter(String query, String statusFilter) {
        filteredList.clear();
        String q = query != null ? query.trim().toLowerCase() : "";
        String sFilter = statusFilter != null ? statusFilter.trim().toUpperCase() : "ALL";

        for (Assignment a : masterList) {
            boolean matchesStatus = "ALL".equals(sFilter) || sFilter.equalsIgnoreCase(a.getStatus());
            boolean matchesQuery = q.isEmpty() ||
                    (a.getTitle() != null && a.getTitle().toLowerCase().contains(q)) ||
                    (a.getSubjectName() != null && a.getSubjectName().toLowerCase().contains(q)) ||
                    (a.getDepartment() != null && a.getDepartment().toLowerCase().contains(q));

            if (matchesStatus && matchesQuery) {
                filteredList.add(a);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_teacher_assignment, parent, false);
        return new ViewHolder(v);
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

        private final TextView tvTeacher;
        private final TextView tvSubject;
        private final TextView tvMeta;
        private final MaterialButton btnRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTeacher = itemView.findViewById(R.id.tvAssignmentTeacherName);
            tvSubject = itemView.findViewById(R.id.tvAssignmentSubjectDetails);
            tvMeta = itemView.findViewById(R.id.tvAssignmentMeta);
            btnRemove = itemView.findViewById(R.id.btnRemoveAssignment);
        }

        public void bind(Assignment item) {
            tvTeacher.setText(item.getTitle());
            tvSubject.setText(item.getSubjectName() != null ? item.getSubjectName() : "Course");
            tvMeta.setText("Dept: " + item.getDepartment() + " • Sem: " + item.getSemester() + " • Status: " + item.getStatus());

            btnRemove.setText("OPTIONS");
            btnRemove.setOnClickListener(v -> {
                if (listener != null) listener.onViewSubmissions(item);
            });
        }
    }
}
