package com.example.utils;

import android.content.Context;
import android.widget.TextView;

import com.example.R;
import com.example.model.SemesterPerformanceTrend;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import java.util.List;
import java.util.Locale;

/**
 * Interactive popup marker view for MPAndroidChart LineCharts displaying academic performance trend details.
 */
public class AcademicTrendMarkerView extends MarkerView {

    private final TextView tvMarkerSemTitle;
    private final TextView tvMarkerPrimaryMetric;
    private final TextView tvMarkerSecondaryMetric;
    private final List<SemesterPerformanceTrend> trendList;
    private String metricMode = "SGPA";

    public AcademicTrendMarkerView(Context context, List<SemesterPerformanceTrend> trendList, String metricMode) {
        super(context, R.layout.layout_academic_chart_marker);
        this.trendList = trendList;
        this.metricMode = metricMode != null ? metricMode : "SGPA";

        tvMarkerSemTitle = findViewById(R.id.tvMarkerSemTitle);
        tvMarkerPrimaryMetric = findViewById(R.id.tvMarkerPrimaryMetric);
        tvMarkerSecondaryMetric = findViewById(R.id.tvMarkerSecondaryMetric);
    }

    public void setMetricMode(String metricMode) {
        this.metricMode = metricMode != null ? metricMode : "SGPA";
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        int index = Math.round(e.getX());
        if (trendList != null && index >= 0 && index < trendList.size()) {
            SemesterPerformanceTrend item = trendList.get(index);
            tvMarkerSemTitle.setText(String.format(Locale.US, "Semester %d", item.getSemester()));

            if ("PERCENTAGE".equalsIgnoreCase(metricMode)) {
                tvMarkerPrimaryMetric.setText(String.format(Locale.US, "Score: %.1f%%", item.getPercentage()));
                tvMarkerSecondaryMetric.setText(String.format(Locale.US, "SGPA: %.2f • CGPA: %.2f • %s",
                        item.getSgpa(), item.getCgpa(), item.getStatus()));
            } else if ("CGPA".equalsIgnoreCase(metricMode)) {
                tvMarkerPrimaryMetric.setText(String.format(Locale.US, "Cumulative CGPA: %.2f", item.getCgpa()));
                tvMarkerSecondaryMetric.setText(String.format(Locale.US, "Sem SGPA: %.2f • Score: %.1f%% • %s",
                        item.getSgpa(), item.getPercentage(), item.getStatus()));
            } else { // SGPA
                tvMarkerPrimaryMetric.setText(String.format(Locale.US, "Semester SGPA: %.2f", item.getSgpa()));
                tvMarkerSecondaryMetric.setText(String.format(Locale.US, "CGPA: %.2f • Score: %.1f%% • %s",
                        item.getCgpa(), item.getPercentage(), item.getStatus()));
            }
        } else {
            tvMarkerSemTitle.setText("Semester Performance");
            tvMarkerPrimaryMetric.setText(String.format(Locale.US, "Score: %.2f", e.getY()));
            tvMarkerSecondaryMetric.setText("Academic Milestone");
        }
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -(getHeight() + 18f));
    }

    @Override
    public MPPointF getOffsetForDrawingAtPoint(float posX, float posY) {
        MPPointF offset = getOffset();
        float x = offset.x;
        float y = offset.y;

        if (getChartView() != null) {
            float chartWidth = getChartView().getWidth();
            if (posX + x < 0) {
                x = -posX + 10f;
            } else if (posX + getWidth() + x > chartWidth) {
                x = chartWidth - posX - getWidth() - 10f;
            }

            if (posY + y < 0) {
                y = 20f;
            }
        }

        return new MPPointF(x, y);
    }
}
