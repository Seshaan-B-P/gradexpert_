package com.example;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for handling FCM registration token updates, receiving push notifications,
 * creating notification channels, and dispatching deep-link intents.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "gradexpert_academic_alerts";
    private static final String CHANNEL_NAME = "Academic & Exam Alerts";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed FCM registration token: " + token);
        saveTokenToFirestore(this, token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        Map<String, String> data = remoteMessage.getData();
        String type = data.get("type");
        String destination = data.get("destination");

        // Check if this FCM message is a grade alert
        if ("GRADE_UPLOADED".equalsIgnoreCase(type) || "GRADE_ALERT".equalsIgnoreCase(type)
                || "RESULTS".equalsIgnoreCase(destination) || data.containsKey("grade")) {
            String studentName = data.get("studentName");
            String registerNo = data.get("registerNo");
            String subjectName = data.get("subjectName");
            String subjectCode = data.get("subjectCode");
            String grade = data.get("grade");
            String instructorName = data.get("instructorName");

            double totalMarks = 0.0;
            try {
                if (data.containsKey("totalMarks")) {
                    totalMarks = Double.parseDouble(data.get("totalMarks"));
                }
            } catch (Exception ignored) {}

            double percentage = 0.0;
            try {
                if (data.containsKey("percentage")) {
                    percentage = Double.parseDouble(data.get("percentage"));
                }
            } catch (Exception ignored) {}

            double gradePoint = 0.0;
            try {
                if (data.containsKey("gradePoint")) {
                    gradePoint = Double.parseDouble(data.get("gradePoint"));
                }
            } catch (Exception ignored) {}

            int semester = 1;
            try {
                if (data.containsKey("semester")) {
                    semester = Integer.parseInt(data.get("semester"));
                }
            } catch (Exception ignored) {}

            if (subjectName != null && !subjectName.isEmpty()) {
                com.example.utils.GradeNotificationHelper.sendGradeAlertNotification(
                        this,
                        studentName != null ? studentName : "Student",
                        registerNo != null ? registerNo : "",
                        subjectName,
                        subjectCode != null ? subjectCode : "",
                        grade != null ? grade : "A",
                        totalMarks,
                        percentage,
                        gradePoint,
                        instructorName != null ? instructorName : "Instructor",
                        semester
                );
                return;
            }
        }

        String title = "GradeXpert Alert";
        String body = "You have a new academic notification.";

        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (remoteMessage.getNotification().getBody() != null) {
                body = remoteMessage.getNotification().getBody();
            }
        }

        if (data.containsKey("title")) title = data.get("title");
        if (data.containsKey("message")) body = data.get("message");

        String alertId = data.get("alertId");

        showNotification(title, body, destination, alertId);
    }

    private void showNotification(String title, String body, String destination, String alertId) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for GradeXpert exams, assignments, attendance, and general announcements.");
            channel.enableLights(true);
            channel.setLightColor(Color.parseColor("#4F46E5"));
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent;
        if ("PASSWORD_RESET_REQUESTS".equalsIgnoreCase(destination)) {
            intent = new Intent(this, PasswordResetRequestsActivity.class);
        } else if ("LOGIN".equalsIgnoreCase(destination)) {
            intent = new Intent(this, LoginActivity.class);
        } else if ("EXAMS".equalsIgnoreCase(destination)) {
            intent = new Intent(this, NotificationsActivity.class);
        } else if ("ASSIGNMENTS".equalsIgnoreCase(destination)) {
            intent = new Intent(this, StudentAssignmentActivity.class);
        } else if ("ATTENDANCE".equalsIgnoreCase(destination)) {
            intent = new Intent(this, StudentAttendanceActivity.class);
        } else if ("RESULTS".equalsIgnoreCase(destination)) {
            intent = new Intent(this, ResultsActivity.class);
        } else {
            intent = new Intent(this, StudentDashboardActivity.class);
        }

        intent.putExtra("alertId", alertId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent);

        notificationManager.notify((int) (System.currentTimeMillis() % 10000), notificationBuilder.build());
    }

    /**
     * Checks if Google Play Services is available and enabled on the device.
     */
    public static boolean isGooglePlayServicesAvailable(Context context) {
        if (context == null) return false;
        try {
            android.content.pm.PackageManager pm = context.getPackageManager();
            android.content.pm.PackageInfo pi = pm.getPackageInfo("com.google.android.gms", 0);
            if (pi == null || pi.applicationInfo == null || !pi.applicationInfo.enabled) {
                return false;
            }
        } catch (Exception e) {
            // Google Play Services APK is not installed on this device/emulator
            return false;
        }

        try {
            Class<?> gmsClass = Class.forName("com.google.android.gms.common.GoogleApiAvailabilityLight");
            Object instance = gmsClass.getMethod("getInstance").invoke(null);
            Object resultCode = gmsClass.getMethod("isGooglePlayServicesAvailable", Context.class)
                    .invoke(instance, context.getApplicationContext());
            if (resultCode instanceof Integer) {
                return ((Integer) resultCode) == 0;
            }
        } catch (Throwable ignored) {}

        return true;
    }

    /**
     * Registers current device FCM token in Firestore under studentDevices/{uid}/tokens/{tokenId}.
     */
    public static void registerFcmToken(Context context) {
        if (context != null && !isGooglePlayServicesAvailable(context)) {
            Log.i(TAG, "Google Play Services is not available on this environment; skipping FCM token retrieval.");
            return;
        }

        try {
            FirebaseMessaging messaging = FirebaseMessaging.getInstance();
            messaging.setAutoInitEnabled(true);
            messaging.getToken().addOnCompleteListener(task -> {
                if (!task.isSuccessful() || task.getResult() == null) {
                    Log.i(TAG, "FCM registration token retrieval skipped or failed on this environment.");
                    return;
                }
                String token = task.getResult();
                saveTokenToFirestore(context, token);

                try {
                    messaging.subscribeToTopic(com.example.utils.GradeNotificationHelper.TOPIC_ALL_GRADES);
                } catch (Exception ignored) {}
            });
        } catch (Exception e) {
            Log.i(TAG, "Firebase Cloud Messaging token registration skipped: " + e.getMessage());
        }
    }

    private static void saveTokenToFirestore(Context context, String token) {
        if (token == null || token.isEmpty()) return;

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String resolvedUid = currentUser != null ? currentUser.getUid() : null;

        if (resolvedUid == null && context != null) {
            try {
                com.example.utils.SessionManager session = new com.example.utils.SessionManager(context);
                if (session.isLoggedIn()) {
                    resolvedUid = session.getIdentifier();
                    if (resolvedUid == null || resolvedUid.isEmpty()) {
                        resolvedUid = session.getUserEmail();
                    }
                }
            } catch (Exception ignore) {}
        }

        if (resolvedUid == null || resolvedUid.isEmpty()) return;

        final String targetUid = resolvedUid;
        String docId = token.length() > 30 ? token.substring(0, 30) : token;

        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("token", token);
        tokenData.put("deviceModel", Build.MODEL);
        tokenData.put("osVersion", Build.VERSION.RELEASE);
        tokenData.put("updatedAt", FieldValue.serverTimestamp());

        // 1. Save in studentDevices/{uid}/tokens collection
        FirebaseFirestore.getInstance()
                .collection("studentDevices")
                .document(targetUid)
                .collection("tokens")
                .document(docId)
                .set(tokenData)
                .addOnSuccessListener(unused -> Log.d(TAG, "FCM token saved successfully in studentDevices for user: " + targetUid))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving FCM token in studentDevices", e));

        // 2. Save/merge in users/{uid} document
        Map<String, Object> userUpdate = new HashMap<>();
        userUpdate.put("fcmToken", token);
        userUpdate.put("lastActive", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(targetUid)
                .set(userUpdate, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> Log.d(TAG, "FCM token merged successfully in users collection for user: " + targetUid))
                .addOnFailureListener(e -> Log.w(TAG, "Failed merging FCM token in users doc: " + e.getMessage()));
    }
}
