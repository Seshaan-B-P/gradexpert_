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

        Map<String, String> data = remoteMessage.getData();
        if (data.containsKey("title")) title = data.get("title");
        if (data.containsKey("message")) body = data.get("message");

        String destination = data.get("destination");
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
     * Registers current device FCM token in Firestore under studentDevices/{uid}/tokens/{tokenId}.
     */
    public static void registerFcmToken(Context context) {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                return;
            }
            String token = task.getResult();
            saveTokenToFirestore(context, token);
        });
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
