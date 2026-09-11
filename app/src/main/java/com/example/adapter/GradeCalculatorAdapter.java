package com.example.adapter;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;
import com.example.model.SubjectGradeItem;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for Manual What-If Grade Calculator subject cards with real-time numeric entry,
 * live component calculation, and instant grade point updates.
 */
public class GradeCalculatorAdapter extends RecyclerView.Adapter<GradeCalculatorAdapter.ViewHolder> {

    private final Context context;
    private List<SubjectGradeItem> itemList;
    private final OnDataChangeListener listener;

    public interface OnDataChangeListener {
        void onDataChanged();
        void onItemRemoved(int position);
    }

    public GradeCalculatorAdapter(Context context, List<SubjectGradeItem> itemList, OnDataChangeListener listener) {
        this.context = context;
        this.itemList = itemList != null ? itemList : new ArrayList<>();
        this.listener = listener;
    }

    public void updateData(List<SubjectGradeItem> newList) {
        this.itemList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<SubjectGradeItem> getItemList() {
        return itemList;
    }

    public void addItem(SubjectGradeItem item) {
        itemList.add(item);
        notifyItemInserted(itemList.size() - 1);
        if (listener != null) listener.onDataChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < itemList.size()) {
            itemList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, itemList.size());
            if (listener != null) {
                listener.onItemRemoved(position);
                listener.onDataChanged();
            }
        }
    }

    public void clearAllMarks() {
        for (SubjectGradeItem item : itemList) {
            item.setInternal1(0);
            item.setAssignment(0);
            item.setModelExam(0);
            item.setUniversityExam(0);
            item.setTotalMarks(0);
            item.recalculate();
        }
        notifyDataSetChanged();
        if (listener != null) listener.onDataChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calculator_subject, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SubjectGradeItem item = itemList.get(position);
        holder.bind(item, position);
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvSubjectCode;
        private final TextView tvSubjectName;
        private final ImageView btnRemoveCalcSubject;
        private final TextInputLayout tilCalcInternal;
        private final TextInputLayout tilCalcAssignment;
        private final TextInputLayout tilCalcExternal;
        private final TextInputEditText etCalcInternal;
        private final TextInputEditText etCalcAssignment;
        private final TextInputEditText etCalcExternal;
        private final TextView tvCalcSubjectTotal;
        private final Chip chipCalcGrade;
        private final TextView tvCalcCreditsBadge;

        private TextWatcher internalWatcher;
        private TextWatcher assignmentWatcher;
        private TextWatcher externalWatcher;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubjectCode = itemView.findViewById(R.id.tvCalcSubjectCode);
            tvSubjectName = itemView.findViewById(R.id.tvCalcSubjectName);
            btnRemoveCalcSubject = itemView.findViewById(R.id.btnRemoveCalcSubject);
            tilCalcInternal = itemView.findViewById(R.id.tilCalcInternal);
            tilCalcAssignment = itemView.findViewById(R.id.tilCalcAssignment);
            tilCalcExternal = itemView.findViewById(R.id.tilCalcExternal);
            etCalcInternal = itemView.findViewById(R.id.etCalcInternal);
            etCalcAssignment = itemView.findViewById(R.id.etCalcAssignment);
            etCalcExternal = itemView.findViewById(R.id.etCalcExternal);
            tvCalcSubjectTotal = itemView.findViewById(R.id.tvCalcSubjectTotal);
            chipCalcGrade = itemView.findViewById(R.id.chipCalcGrade);
            tvCalcCreditsBadge = itemView.findViewById(R.id.tvCalcCreditsBadge);
        }

        public void bind(SubjectGradeItem item, int position) {
            // Remove previous watchers
            if (internalWatcher != null) etCalcInternal.removeTextChangedListener(internalWatcher);
            if (assignmentWatcher != null) etCalcAssignment.removeTextChangedListener(assignmentWatcher);
            if (externalWatcher != null) etCalcExternal.removeTextChangedListener(externalWatcher);

            String code = item.getSubjectCode() != null && !item.getSubjectCode().isEmpty() ? item.getSubjectCode() : "SUB" + (position + 1);
            String name = item.getSubjectName() != null && !item.getSubjectName().isEmpty() ? item.getSubjectName() : "Course " + (position + 1);
            int credits = item.getCredits() > 0 ? item.getCredits() : 4;

            tvSubjectCode.setText(code);
            tvSubjectName.setText(name);
            tvCalcCreditsBadge.setText(credits + " Credits");

            // Populate current values if non-zero
            etCalcInternal.setText(item.getInternal1() > 0 ? String.format(Locale.US, "%.0f", item.getInternal1()) : "");
            etCalcAssignment.setText(item.getAssignment() > 0 ? String.format(Locale.US, "%.0f", item.getAssignment()) : "");
            etCalcExternal.setText(item.getUniversityExam() > 0 ? String.format(Locale.US, "%.0f", item.getUniversityExam()) : "");

            updateSubjectPill(item);

            btnRemoveCalcSubject.setOnClickListener(v -> removeItem(getAdapterPosition()));

            // Internal Marks Watcher (Max: 20)
            internalWatcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        String input = s.toString().trim();
                        if (input.isEmpty()) {
                            item.setInternal1(0);
                            tilCalcInternal.setError(null);
                        } else {
                            double val = Double.parseDouble(input);
                            if (val < 0) {
                                tilCalcInternal.setError("≥ 0");
                                item.setInternal1(0);
                            } else if (val > 20) {
                                tilCalcInternal.setError("Max 20");
                                item.setInternal1(20);
                            } else {
                                tilCalcInternal.setError(null);
                                item.setInternal1(val);
                            }
                        }
                    } catch (NumberFormatException e) {
                        item.setInternal1(0);
                    }
                    recalculateItem(item);
                }
                @Override public void afterTextChanged(Editable s) {}
            };

            // Assignment Marks Watcher (Max: 10)
            assignmentWatcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        String input = s.toString().trim();
                        if (input.isEmpty()) {
                            item.setAssignment(0);
                            tilCalcAssignment.setError(null);
                        } else {
                            double val = Double.parseDouble(input);
                            if (val < 0) {
                                tilCalcAssignment.setError("≥ 0");
                                item.setAssignment(0);
                            } else if (val > 10) {
                                tilCalcAssignment.setError("Max 10");
                                item.setAssignment(10);
                            } else {
                                tilCalcAssignment.setError(null);
                                item.setAssignment(val);
                            }
                        }
                    } catch (NumberFormatException e) {
                        item.setAssignment(0);
                    }
                    recalculateItem(item);
                }
                @Override public void afterTextChanged(Editable s) {}
            };

            // External Marks Watcher (Max: 70)
            externalWatcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        String input = s.toString().trim();
                        if (input.isEmpty()) {
                            item.setUniversityExam(0);
                            tilCalcExternal.setError(null);
                        } else {
                            double val = Double.parseDouble(input);
                            if (val < 0) {
                                tilCalcExternal.setError("≥ 0");
                                item.setUniversityExam(0);
                            } else if (val > 70) {
                                tilCalcExternal.setError("Max 70");
                                item.setUniversityExam(70);
                            } else {
                                tilCalcExternal.setError(null);
                                item.setUniversityExam(val);
                            }
                        }
                    } catch (NumberFormatException e) {
                        item.setUniversityExam(0);
                    }
                    recalculateItem(item);
                }
                @Override public void afterTextChanged(Editable s) {}
            };

            etCalcInternal.addTextChangedListener(internalWatcher);
            etCalcAssignment.addTextChangedListener(assignmentWatcher);
            etCalcExternal.addTextChangedListener(externalWatcher);
        }

        private void recalculateItem(SubjectGradeItem item) {
            double total = item.getInternal1() + item.getAssignment() + item.getUniversityExam();
            item.setTotalMarks(total);
            item.setMaxInternal(30);
            item.setMaxExternal(70);
            item.recalculate();
            updateSubjectPill(item);
            if (listener != null) listener.onDataChanged();
        }

        private void updateSubjectPill(SubjectGradeItem item) {
            double total = item.getTotalMarks();
            tvCalcSubjectTotal.setText(String.format(Locale.US, "Total: %.0f / 100", total));

            String grade = item.getGrade() != null ? item.getGrade() : "F";
            int gradePoint = item.getGradePoint();

            chipCalcGrade.setText(String.format(Locale.US, "Grade: %s (GP: %d)", grade, gradePoint));
            applyGradeChipColor(chipCalcGrade, grade);
        }

        private void applyGradeChipColor(Chip chip, String grade) {
            if ("A+".equalsIgnoreCase(grade) || "O".equalsIgnoreCase(grade)) {
                chip.setChipBackgroundColorResource(R.color.success_light);
                chip.setTextColor(ContextCompat.getColor(context, R.color.success));
                chip.setChipStrokeColorResource(R.color.success);
            } else if ("A".equalsIgnoreCase(grade)) {
                chip.setChipBackgroundColorResource(R.color.primary_light);
                chip.setTextColor(ContextCompat.getColor(context, R.color.primary));
                chip.setChipStrokeColorResource(R.color.primary);
            } else if ("B+".equalsIgnoreCase(grade) || "B".equalsIgnoreCase(grade)) {
                chip.setChipBackgroundColorResource(R.color.secondary_light);
                chip.setTextColor(ContextCompat.getColor(context, R.color.secondary));
                chip.setChipStrokeColorResource(R.color.secondary);
            } else if ("C".equalsIgnoreCase(grade) || "D".equalsIgnoreCase(grade)) {
                chip.setChipBackgroundColorResource(R.color.warning_light);
                chip.setTextColor(ContextCompat.getColor(context, R.color.warning));
                chip.setChipStrokeColorResource(R.color.warning);
            } else {
                chip.setChipBackgroundColorResource(R.color.error_light);
                chip.setTextColor(ContextCompat.getColor(context, R.color.error));
                chip.setChipStrokeColorResource(R.color.error);
            }
        }
    }
}
