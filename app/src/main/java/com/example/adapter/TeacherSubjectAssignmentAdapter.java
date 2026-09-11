package com.example.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.model.TeacherAssignment;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class TeacherSubjectAssignmentAdapter extends RecyclerView.Adapter<TeacherSubjectAssignmentAdapter.ViewHolder> {

    public interface OnAssignmentRemoveListener {
        void onRemoveClick(TeacherAssignment assignment);
    }

    private List<TeacherAssignment> list;
    private final OnAssignmentRemoveListener listener;

    public TeacherSubjectAssignmentAdapter(List<TeacherAssignment> list, OnAssignmentRemoveListener listener) {
        this.list = list != null ? list : new ArrayList<>();
        this.listener = listener;
    }

    public void updateData(List<TeacherAssignment> newList) {
        this.list = newList != null ? newList : new ArrayList<>();
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
        holder.bind(list.get(position));
    }

    @Override
    public int getItemCount() {
        return list.size();
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

        public void bind(TeacherAssignment item) {
            tvTeacher.setText("Faculty: " + item.getTeacherName());
            tvSubject.setText(item.getSubjectName() + " (" + item.getSubjectCode() + ")");
            tvMeta.setText("Dept: " + item.getDepartment() + " • Sem: " + item.getSemester() + " • AY: " + item.getAcademicYear());

            btnRemove.setOnClickListener(v -> {
                if (listener != null) listener.onRemoveClick(item);
            });
        }
    }
}
