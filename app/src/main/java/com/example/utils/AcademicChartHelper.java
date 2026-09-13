package com.example.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.drawable.GradientDrawable;

import androidx.core.content.ContextCompat;

import com.example.R;
import com.example.database.DatabaseHelper;
import com.example.model.Result;
import com.example.model.SemesterPerformanceTrend;
import com.example.model.Student;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Utility class to configure and render academic performance trends over multiple semesters using MPAndroidChart.
 */
public class AcademicChartHelper {

    public enum MetricMode {
        SGPA,
        CGPA,
        PERCENTAGE,
        COMPARISON
    }

    public static final int COLOR_PRIMARY_INDIGO = Color.parseColor("#4F46E5");
    public static final int COLOR_SECONDARY_EMERALD = Color.parseColor("#10B981");
    public static final int COLOR_AMBER = Color.parseColor("#F59E0B");
    public static final int COLOR_TEXT_MUTED = Color.parseColor("#64748B");
    public static final int COLOR_GRID_LIGHT = Color.parseColor("#E2E8F0");

    /**
     * Loads or synthesizes multi-semester performance trends for a given student from SQLite.
     */
    public static List<SemesterPerformanceTrend> loadMultiSemesterTrends(DatabaseHelper db, int studentId, Student student) {
        List<SemesterPerformanceTrend> trends = new ArrayList<>();
        Map<Integer, SemesterPerformanceTrend> trendMap = new HashMap<>();

        try {
            // 1. Load official published/approved results
            List<Result> results = db.getPublishedResultsForStudent(studentId);
            if (results != null) {
                for (Result r : results) {
                    SemesterPerformanceTrend item = new SemesterPerformanceTrend(
                            r.getSemester(),
                            "Sem " + r.getSemester(),
                            r.getSgpa(),
                            r.getCgpa(),
                            r.getPercentage(),
                            r.getTotalMarks(),
                            20,
                            r.getPercentage() >= 85 ? "Distinction" : (r.getPercentage() >= 70 ? "First Class" : "Pass")
                    );
                    item.setPublishedDate(r.getPublishedDate());
                    trendMap.put(r.getSemester(), item);
                }
            }

            // 2. Check if student has current semester marks not yet in results
            for (int sem = 1; sem <= 8; sem++) {
                if (!trendMap.containsKey(sem)) {
                    List<com.example.model.SubjectGradeItem> marks = db.getStudentSubjectMarksBySemester(studentId, sem);
                    if (marks != null && !marks.isEmpty()) {
                        double totalScore = 0;
                        double totalWeightedGp = 0;
                        int totalCredits = 0;
                        for (com.example.model.SubjectGradeItem m : marks) {
                            totalScore += m.getTotalMarks();
                            int c = m.getCredits() > 0 ? m.getCredits() : 4;
                            totalWeightedGp += (m.getGradePoint() * c);
                            totalCredits += c;
                        }
                        if (totalCredits > 0) {
                            double semSgpa = Math.round((totalWeightedGp / totalCredits) * 100.0) / 100.0;
                            double pct = Math.round((totalScore / (marks.size() * 100.0) * 100.0) * 10.0) / 10.0;
                            SemesterPerformanceTrend item = new SemesterPerformanceTrend(
                                    sem,
                                    "Sem " + sem,
                                    semSgpa,
                                    semSgpa,
                                    pct,
                                    totalScore,
                                    totalCredits,
                                    pct >= 85 ? "Distinction" : (pct >= 70 ? "First Class" : "Pass")
                            );
                            trendMap.put(sem, item);
                        }
                    }
                }
            }

            // 3. Fallback / Demonstration progression if student is new and has fewer than 3 semester entries
            if (trendMap.size() < 3) {
                int currentSem = 4;
                if (student != null && student.getSemester() != null) {
                    try {
                        currentSem = Integer.parseInt(student.getSemester().replaceAll("[^0-9]", ""));
                    } catch (Exception ignored) {}
                }
                if (currentSem <= 1) currentSem = 4;

                double baseSgpa = 8.15;
                if (!trendMap.isEmpty()) {
                    for (SemesterPerformanceTrend existing : trendMap.values()) {
                        baseSgpa = existing.getSgpa();
                        break;
                    }
                }

                double[] defaultSgpas = {
                        Math.max(7.2, baseSgpa - 0.55),
                        Math.max(7.5, baseSgpa - 0.25),
                        baseSgpa,
                        Math.min(9.5, baseSgpa + 0.35),
                        Math.min(9.8, baseSgpa + 0.50)
                };

                for (int s = 1; s <= Math.min(5, Math.max(3, currentSem)); s++) {
                    if (!trendMap.containsKey(s)) {
                        double semSgpa = Math.round(defaultSgpas[(s - 1) % defaultSgpas.length] * 100.0) / 100.0;
                        double semPct = Math.round((semSgpa * 9.5) * 10.0) / 10.0;
                        SemesterPerformanceTrend mockItem = new SemesterPerformanceTrend(
                                s,
                                "Sem " + s,
                                semSgpa,
                                semSgpa,
                                semPct,
                                semPct * 5.0,
                                20,
                                semPct >= 85 ? "Distinction" : "First Class"
                        );
                        mockItem.setEstimated(true);
                        trendMap.put(s, mockItem);
                    }
                }
            }

            trends.addAll(trendMap.values());
            Collections.sort(trends, Comparator.comparingInt(SemesterPerformanceTrend::getSemester));

            // Re-calculate running cumulative CGPA
            double cumulativeGp = 0;
            int cumulativeCredits = 0;
            for (SemesterPerformanceTrend t : trends) {
                int cr = t.getTotalCredits() > 0 ? t.getTotalCredits() : 20;
                cumulativeGp += (t.getSgpa() * cr);
                cumulativeCredits += cr;
                t.setCgpa(Math.round((cumulativeGp / cumulativeCredits) * 100.0) / 100.0);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return trends;
    }

    /**
     * Sets up and renders an MPAndroidChart LineChart with multi-semester performance trends.
     */
    public static void renderAcademicTrendLineChart(Context context, LineChart lineChart, List<SemesterPerformanceTrend> trends, MetricMode mode) {
        if (lineChart == null) return;
        lineChart.clear();
        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setExtraOffsets(12f, 16f, 16f, 12f);

        if (trends == null || trends.isEmpty()) {
            lineChart.setNoDataText("No semester academic performance data found.");
            lineChart.setNoDataTextColor(COLOR_TEXT_MUTED);
            lineChart.invalidate();
            return;
        }

        List<String> xLabels = new ArrayList<>();
        List<Entry> primaryEntries = new ArrayList<>();
        List<Entry> secondaryEntries = new ArrayList<>();

        for (int i = 0; i < trends.size(); i++) {
            SemesterPerformanceTrend item = trends.get(i);
            xLabels.add(item.getSemesterLabel());

            switch (mode) {
                case PERCENTAGE:
                    primaryEntries.add(new Entry(i, (float) item.getPercentage()));
                    break;
                case CGPA:
                    primaryEntries.add(new Entry(i, (float) item.getCgpa()));
                    break;
                case COMPARISON:
                    primaryEntries.add(new Entry(i, (float) item.getSgpa()));
                    secondaryEntries.add(new Entry(i, (float) item.getCgpa()));
                    break;
                case SGPA:
                default:
                    primaryEntries.add(new Entry(i, (float) item.getSgpa()));
                    break;
            }
        }

        LineData lineData = new LineData();

        if (mode == MetricMode.COMPARISON) {
            // SGPA Dataset
            LineDataSet sgpaSet = createStyledDataSet(primaryEntries, "Semester SGPA", COLOR_PRIMARY_INDIGO, true);
            lineData.addDataSet(sgpaSet);

            // CGPA Dataset
            LineDataSet cgpaSet = createStyledDataSet(secondaryEntries, "Cumulative CGPA", COLOR_SECONDARY_EMERALD, false);
            lineData.addDataSet(cgpaSet);
        } else {
            String label;
            int color;
            switch (mode) {
                case PERCENTAGE:
                    label = "Academic Score (%)";
                    color = COLOR_SECONDARY_EMERALD;
                    break;
                case CGPA:
                    label = "Cumulative CGPA";
                    color = COLOR_PRIMARY_INDIGO;
                    break;
                case SGPA:
                default:
                    label = "Semester SGPA (10.0 scale)";
                    color = COLOR_PRIMARY_INDIGO;
                    break;
            }

            LineDataSet singleSet = createStyledDataSet(primaryEntries, label, color, true);
            if (mode == MetricMode.PERCENTAGE) {
                singleSet.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        return String.format(Locale.US, "%.1f%%", value);
                    }
                });
            } else {
                singleSet.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        return String.format(Locale.US, "%.2f", value);
                    }
                });
            }
            lineData.addDataSet(singleSet);
        }

        lineChart.setData(lineData);

        // Configure X-Axis
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
        xAxis.setTextColor(COLOR_TEXT_MUTED);
        xAxis.setTextSize(11f);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);
        xAxis.setAxisLineColor(COLOR_GRID_LIGHT);
        xAxis.setAxisLineWidth(1.2f);
        xAxis.setYOffset(6f);

        // Configure Left Y-Axis
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextColor(COLOR_TEXT_MUTED);
        leftAxis.setTextSize(11f);
        leftAxis.setGridColor(COLOR_GRID_LIGHT);
        leftAxis.setGridLineWidth(0.8f);
        leftAxis.enableGridDashedLine(8f, 6f, 0f);
        leftAxis.setDrawAxisLine(false);
        leftAxis.removeAllLimitLines();

        if (mode == MetricMode.PERCENTAGE) {
            leftAxis.setAxisMinimum(40f);
            leftAxis.setAxisMaximum(100f);
            leftAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return String.format(Locale.US, "%.0f%%", value);
                }
            });

            // Benchmark LimitLine for Distinction
            LimitLine distinctionLine = new LimitLine(75f, "Distinction (75%)");
            distinctionLine.setLineColor(COLOR_AMBER);
            distinctionLine.setLineWidth(1.2f);
            distinctionLine.enableDashedLine(10f, 6f, 0f);
            distinctionLine.setTextColor(COLOR_AMBER);
            distinctionLine.setTextSize(9f);
            leftAxis.addLimitLine(distinctionLine);
        } else {
            leftAxis.setAxisMinimum(0f);
            leftAxis.setAxisMaximum(10.2f);
            leftAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return String.format(Locale.US, "%.1f", value);
                }
            });

            // Benchmark LimitLine for Distinction SGPA
            LimitLine distinctionLine = new LimitLine(8.0f, "Distinction (8.0)");
            distinctionLine.setLineColor(COLOR_AMBER);
            distinctionLine.setLineWidth(1.2f);
            distinctionLine.enableDashedLine(10f, 6f, 0f);
            distinctionLine.setTextColor(COLOR_AMBER);
            distinctionLine.setTextSize(9f);
            leftAxis.addLimitLine(distinctionLine);
        }

        // Disable right Y-Axis for cleaner layout
        lineChart.getAxisRight().setEnabled(false);

        // Configure Legend
        Legend legend = lineChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextColor(COLOR_TEXT_MUTED);
        legend.setTextSize(11f);
        legend.setForm(Legend.LegendForm.CIRCLE);
        legend.setFormSize(8f);
        legend.setXEntrySpace(14f);

        // Attach Interactive MarkerView
        String markerMode = mode == MetricMode.PERCENTAGE ? "PERCENTAGE" : (mode == MetricMode.CGPA ? "CGPA" : "SGPA");
        AcademicTrendMarkerView markerView = new AcademicTrendMarkerView(context, trends, markerMode);
        markerView.setChartView(lineChart);
        lineChart.setMarker(markerView);

        // Animate and refresh
        lineChart.animateY(700, Easing.EaseInOutCubic);
        lineChart.invalidate();
    }

    private static LineDataSet createStyledDataSet(List<Entry> entries, String label, int color, boolean drawFill) {
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.18f);
        dataSet.setColor(color);
        dataSet.setLineWidth(2.8f);

        // Markers / Data points
        dataSet.setCircleColor(color);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleColor(Color.WHITE);
        dataSet.setCircleHoleRadius(2.5f);

        // Values on nodes
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.parseColor("#1E293B"));
        dataSet.setDrawValues(true);

        // Highlighting
        dataSet.setHighlightEnabled(true);
        dataSet.setHighLightColor(color);
        dataSet.setHighlightLineWidth(1.5f);
        dataSet.enableDashedHighlightLine(8f, 6f, 0f);
        dataSet.setDrawHorizontalHighlightIndicator(false);

        // Fill under curve
        if (drawFill) {
            dataSet.setDrawFilled(true);
            dataSet.setFillColor(color);
            dataSet.setFillAlpha(45);
        } else {
            dataSet.setDrawFilled(false);
        }

        return dataSet;
    }
}
