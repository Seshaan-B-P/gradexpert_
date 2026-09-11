package com.example.model;

public class ReportRowItem {
    private String title;
    private String subtitle;
    private String badgeText;
    private int badgeBgColor;
    private int badgeTextColor;
    private String metricText;
    private String metricLabel;
    private int iconResId;

    public ReportRowItem(String title, String subtitle, String badgeText, int badgeBgColor, int badgeTextColor, String metricText, String metricLabel, int iconResId) {
        this.title = title;
        this.subtitle = subtitle;
        this.badgeText = badgeText;
        this.badgeBgColor = badgeBgColor;
        this.badgeTextColor = badgeTextColor;
        this.metricText = metricText;
        this.metricLabel = metricLabel;
        this.iconResId = iconResId;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getBadgeText() {
        return badgeText;
    }

    public int getBadgeBgColor() {
        return badgeBgColor;
    }

    public int getBadgeTextColor() {
        return badgeTextColor;
    }

    public String getMetricText() {
        return metricText;
    }

    public String getMetricLabel() {
        return metricLabel;
    }

    public int getIconResId() {
        return iconResId;
    }
}
