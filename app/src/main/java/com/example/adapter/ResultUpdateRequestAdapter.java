package com.example.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.model.ResultUpdateRequest;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying pending Result Update Requests in AdminResultsActivity.
 */
public class ResultUpdateRequestAdapter extends RecyclerView.Adapter<ResultUpdateRequestAdapter.ViewHolder> {

    public interface OnUpdateRequestActionListener {
        void onCompare(ResultUpdateRequest request);
        void onApprove(ResultUpdateRequest request);
        void onReject(ResultUpdateRequest request);
    }

    private List<ResultUpdateRequest> requestList;
    private final OnUpdateRequestActionListener listener;

    public ResultUpdateRequestAdapter(List<ResultUpdateRequest> requestList, OnUpdateRequestActionListener listener) {
        this.requestList = requestList != null ? requestList : new ArrayList<>();
        this.listener = listener;
    }

    public void updateData(List<ResultUpdateRequest> newList) {
        this.requestList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_result_update_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position >= requestList.size()) return;
        ResultUpdateRequest req = requestList.get(position);

        String name = req.getStudentName();
        if (name == null || name.trim().isEmpty()) {
            name = "Student ID: " + req.getStudentId();
        }
        holder.tvStudentName.setText(name);

        String reg = req.getRegisterNo() != null ? req.getRegisterNo() : "";
        String dept = req.getDepartmentName() != null ? req.getDepartmentName() : "";
        int sem = req.getSemester();
        String prog = req.getProgramLevel() != null ? req.getProgramLevel().toUpperCase() : "UG";

        holder.tvDetails.setText("Reg: " + reg + " • " + dept + " (" + prog + ") • Sem " + sem);

        int oldV = req.getOldVersion() > 0 ? req.getOldVersion() : 1;
        int newV = req.getNewVersion() > 0 ? req.getNewVersion() : (oldV + 1);
        holder.chipVersionTag.setText("v" + oldV + " → v" + newV);

        String reason = req.getReasonForUpdate();
        holder.tvReason.setText(reason != null && !reason.trim().isEmpty() ? reason : "No reason provided");

        String teacher = req.getTeacherName();
        holder.tvTeacher.setText("Requested by: " + (teacher != null && !teacher.trim().isEmpty() ? teacher : "Faculty"));

        String dateStr = req.getSubmittedAtString();
        holder.tvDate.setText(dateStr != null ? "Date: " + dateStr : "Date: Pending");

        holder.btnCompare.setOnClickListener(v -> {
            if (listener != null) listener.onCompare(req);
        });

        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) listener.onReject(req);
        });

        holder.btnApprove.setOnClickListener(v -> {
            if (listener != null) listener.onApprove(req);
        });
    }

    @Override
    public int getItemCount() {
        return requestList != null ? requestList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvDetails, tvReason, tvTeacher, tvDate;
        Chip chipVersionTag;
        MaterialButton btnCompare, btnReject, btnApprove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvAdminUpdateStudentName);
            tvDetails = itemView.findViewById(R.id.tvAdminUpdateDetails);
            tvReason = itemView.findViewById(R.id.tvAdminUpdateReason);
            tvTeacher = itemView.findViewById(R.id.tvAdminUpdateTeacher);
            tvDate = itemView.findViewById(R.id.tvAdminUpdateDate);
            chipVersionTag = itemView.findViewById(R.id.chipAdminUpdateVersionTag);
            btnCompare = itemView.findViewById(R.id.btnAdminCompareUpdate);
            btnReject = itemView.findViewById(R.id.btnAdminRejectUpdate);
            btnApprove = itemView.findViewById(R.id.btnAdminApproveUpdate);
        }
    }
}
