package com.example.model;

/**
 * ActivityItem represents recent activity events displayed on the dashboard.
 */
public class ActivityItem {
    private String title;
    private String timestamp;
    private String category;
    private int iconResId;

    public ActivityItem(String title, String timestamp, String category, int iconResId) {
        this.title = title;
        this.timestamp = timestamp;
        this.category = category;
        this.iconResId = iconResId;
    }

    public String getTitle() {
        return title;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getCategory() {
        return category;
    }

    public int getIconResId() {
        return iconResId;
    }
}
