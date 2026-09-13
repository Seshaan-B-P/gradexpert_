package com.example.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.R;
import com.example.StudentAttendanceActivity;

import java.util.Locale;

/**
 * Helper to manage attendance alert notification channels and dispatch high-priority notifications
 * when student attendance falls below the specified threshold.
 */
public class AttendanceNotificationHelper {

    public static final String CHANNEL_ID = "gradexpert_attendance_alerts";
    public static final String CHANNEL_NAME = "Attendance Threshold Alerts";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Critical alerts when your class attendance falls below target threshold");
            channel.enableLights(true);
            channel.setLightColor(Color.parseColor("#EF4444"));
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 250, 250, 250});

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void sendLowAttendanceNotification(Context context, String subjectName, double percentage, double threshold, int classesNeeded) {
        if (context == null) return;
        createNotificationChannel(context);

        Intent intent = new Intent(context, StudentAttendanceActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = "⚠️ Attendance Alert: " + subjectName;
        String content = String.format(Locale.US, "Attendance dropped to %.1f%% (Required: %.0f%%)!", percentage, threshold);
        String detailText;
        if (classesNeeded > 0) {
            detailText = String.format(Locale.US, "Your logged attendance for %s is %.1f%%, below your %.0f%% alert cutoff. You must attend the next %d consecutive classes to restore eligibility.", subjectName, percentage, threshold, classesNeeded);
        } else {
            detailText = String.format(Locale.US, "Your logged attendance for %s is %.1f%%, below your %.0f%% alert cutoff. Please attend upcoming lectures regularly.", subjectName, percentage, threshold);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_attendance)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(detailText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(Color.parseColor("#EF4444"))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        try {
            NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
            managerCompat.notify((int) (System.currentTimeMillis() % 100000), builder.build());
        } catch (SecurityException ignored) {}
    }
}
