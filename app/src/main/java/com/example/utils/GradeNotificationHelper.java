package com.example.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.R;
import com.example.ResultsActivity;
import com.example.database.DatabaseHelper;
import com.example.model.Student;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Manages local grade notification alerts, high-priority notification channels,
 * Firebase Messaging topic subscriptions, and real-time Firestore listener for newly uploaded grades.
 */
public class GradeNotificationHelper {

    private static final String TAG = "GradeNotificationHelper";
    public static final String CHANNEL_ID_GRADES = "gradexpert_grades_alerts";
    public static final String CHANNEL_NAME = "Grade & Academic Results Alerts";
    public static final String TOPIC_ALL_GRADES = "all_grades";

    // Keep track of notified alert IDs in this session to prevent duplicate popups
    private static final Set<String> notifiedAlertIds = new HashSet<>();
    private static long sessionStartTimeMs = System.currentTimeMillis() - 5000;

    /**
     * Creates and registers the dedicated high-priority NotificationChannel for Grade Alerts (API 26+).
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && context != null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID_GRADES,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Instant alerts when instructors upload new subject grades and exam results");
            channel.enableLights(true);
            channel.setLightColor(Color.parseColor("#4F46E5"));
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 350, 200, 350});
            channel.setShowBadge(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Subscribes a student to Firebase Cloud Messaging grade topics.
     */
    public static void subscribeStudentToGradeAlerts(Context context, @Nullable Student student) {
        if (context != null && !com.example.MyFirebaseMessagingService.isGooglePlayServicesAvailable(context)) {
            Log.i(TAG, "Google Play Services not available; skipping FCM topic subscription.");
            return;
        }

        try {
            FirebaseMessaging messaging = FirebaseMessaging.getInstance();
            messaging.subscribeToTopic(TOPIC_ALL_GRADES)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Subscribed to FCM topic: " + TOPIC_ALL_GRADES);
                        }
                    });

            if (student != null) {
                if (student.getRegisterNo() != null && !student.getRegisterNo().trim().isEmpty()) {
                    String cleanRegNo = student.getRegisterNo().trim().replaceAll("[^a-zA-Z0-9-_.~%]", "_");
                    messaging.subscribeToTopic("student_" + cleanRegNo);
                }

                if (student.getId() > 0) {
                    messaging.subscribeToTopic("student_id_" + student.getId());
                }

                if (student.getSemester() != null && !student.getSemester().trim().isEmpty()) {
                    String cleanSem = student.getSemester().trim().replaceAll("[^a-zA-Z0-9-_.~%]", "_");
                    messaging.subscribeToTopic("grades_sem_" + cleanSem);
                }

                if (student.getDepartment() != null && !student.getDepartment().trim().isEmpty()) {
                    String cleanDept = student.getDepartment().trim().replaceAll("[^a-zA-Z0-9-_.~%]", "_");
                    if (cleanDept.length() > 25) cleanDept = cleanDept.substring(0, 25);
                    messaging.subscribeToTopic("grades_dept_" + cleanDept);
                }
            }
        } catch (Exception e) {
            Log.i(TAG, "FCM grade topic subscription skipped: " + e.getMessage());
        }
    }

    /**
     * Dispatches an instructor grade upload event to Firestore and triggers notifications.
     */
    public static void dispatchGradeUploadAlert(
            Context context,
            String studentName,
            String registerNo,
            int studentId,
            String subjectName,
            String subjectCode,
            String grade,
            double totalMarks,
            double percentage,
            double gradePoint,
            String instructorName,
            int semester
    ) {
        if (context == null) return;
        String alertId = "grade_" + System.currentTimeMillis() + "_" + Math.abs((registerNo + "_" + subjectCode).hashCode());
        String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());

        // 1. Prepare data for Firestore gradeAlerts collection
        Map<String, Object> alertData = new HashMap<>();
        alertData.put("alertId", alertId);
        alertData.put("studentName", studentName != null ? studentName : "Student");
        alertData.put("registerNo", registerNo != null ? registerNo : "");
        alertData.put("studentId", studentId);
        alertData.put("subjectName", subjectName != null ? subjectName : "Subject");
        alertData.put("subjectCode", subjectCode != null ? subjectCode : "");
        alertData.put("grade", grade != null ? grade : "A");
        alertData.put("totalMarks", totalMarks);
        alertData.put("percentage", percentage);
        alertData.put("gradePoint", gradePoint);
        alertData.put("instructorName", instructorName != null ? instructorName : "Instructor");
        alertData.put("semester", semester);
        alertData.put("type", "GRADE_UPLOADED");
        alertData.put("createdAt", FieldValue.serverTimestamp());
        alertData.put("read", false);

        // Upload to Firestore gradeAlerts
        try {
            FirebaseFirestore.getInstance().collection("gradeAlerts")
                    .document(alertId)
                    .set(alertData)
                    .addOnSuccessListener(unused -> Log.d(TAG, "Grade alert stored in Firestore successfully: " + alertId))
                    .addOnFailureListener(e -> Log.w(TAG, "Failed storing grade alert in Firestore: " + e.getMessage()));
        } catch (Exception e) {
            Log.w(TAG, "Firestore gradeAlerts exception: " + e.getMessage());
        }

        // 2. Also log in top-level notifications for broadcast center
        try {
            Map<String, Object> notifData = new HashMap<>();
            notifData.put("id", Math.abs(alertId.hashCode()));
            notifData.put("type", "GRADE_UPLOADED");
            notifData.put("title", "New Grade Uploaded: " + subjectName);
            notifData.put("message", "Instructor " + instructorName + " recorded grade " + grade + " (" + String.format(Locale.US, "%.1f%%", percentage) + ") for " + studentName);
            notifData.put("date", dateStr);
            notifData.put("target_role", "STUDENT");
            notifData.put("targetRole", "STUDENT");
            notifData.put("targetRegisterNo", registerNo);
            notifData.put("category", "EXAM");
            notifData.put("createdAt", FieldValue.serverTimestamp());

            FirebaseFirestore.getInstance().collection("notifications")
                    .document("notif_" + alertId)
                    .set(notifData)
                    .addOnFailureListener(e -> Log.w(TAG, "Failed writing general notification: " + e.getMessage()));
        } catch (Exception ignored) {}

        // 3. Record in local SQLite database for in-app Notifications tab
        try {
            DatabaseHelper dbHelper = new DatabaseHelper(context);
            String notifTitle = "New Grade: " + subjectName;
            String notifMsg = "Instructor " + instructorName + " uploaded your grade: " + grade + " (" + String.format(Locale.US, "%.1f%%", percentage) + ", " + totalMarks + " marks).";
            dbHelper.addNotification(notifTitle, notifMsg, dateStr, "STUDENT", "EXAM", instructorName);
        } catch (Exception ignored) {}

        // 4. Trigger local Android push notification for the student
        sendGradeAlertNotification(
                context,
                studentName,
                registerNo,
                subjectName,
                subjectCode,
                grade,
                totalMarks,
                percentage,
                gradePoint,
                instructorName,
                semester
        );
    }

    /**
     * Builds and displays a local high-priority Android notification alerting the student
     * that a new grade was uploaded.
     */
    public static void sendGradeAlertNotification(
            @NonNull Context context,
            String studentName,
            String registerNo,
            String subjectName,
            String subjectCode,
            String grade,
            double totalMarks,
            double percentage,
            double gradePoint,
            String instructorName,
            int semester
    ) {
        if (context == null) return;
        createNotificationChannel(context);

        // Check runtime permission for Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Cannot display grade notification.");
                return;
            }
        }

        // Deep-link intent targeting ResultsActivity
        Intent intent = new Intent(context, ResultsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("studentName", studentName);
        intent.putExtra("registerNo", registerNo);
        intent.putExtra("subjectName", subjectName);
        intent.putExtra("subjectCode", subjectCode);
        intent.putExtra("grade", grade);
        intent.putExtra("totalMarks", totalMarks);
        intent.putExtra("percentage", percentage);
        intent.putExtra("gradePoint", gradePoint);
        intent.putExtra("instructorName", instructorName);
        intent.putExtra("semester", semester);

        int reqCode = (int) (System.currentTimeMillis() % 100000);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = "🎓 New Grade Uploaded: " + (subjectCode != null && !subjectCode.isEmpty() ? subjectCode + " - " : "") + subjectName;
        String contentText = String.format(Locale.US, "Grade: %s (%.1f%%) • Points: %.1f • By: %s",
                grade != null ? grade : "N/A", percentage, gradePoint, instructorName != null ? instructorName : "Instructor");

        String bigText = String.format(
                Locale.US,
                "Instructor %s has uploaded your official grade for %s (%s).\n\n" +
                        "• Letter Grade: %s (Grade Point: %.1f)\n" +
                        "• Total Score: %.1f/100 (%.1f%%)\n" +
                        "• Semester: Semester %d\n\n" +
                        "Tap to open your academic statement of marks and view detailed breakdown.",
                instructorName != null ? instructorName : "Instructor",
                subjectName != null ? subjectName : "Subject",
                subjectCode != null ? subjectCode : "",
                grade != null ? grade : "A",
                gradePoint,
                totalMarks,
                percentage,
                semester > 0 ? semester : 1
        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_GRADES)
                .setSmallIcon(R.drawable.ic_grade)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(Color.parseColor("#4F46E5")) // Brand Indigo
                .setSound(defaultSoundUri)
                .setVibrate(new long[]{0, 350, 200, 350})
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_results, "View Grade Card", pendingIntent);

        try {
            NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
            int notifId = (int) ((registerNo != null ? registerNo.hashCode() : 0) + (subjectCode != null ? subjectCode.hashCode() : 0) + System.currentTimeMillis() % 10000);
            managerCompat.notify(Math.abs(notifId), builder.build());
            Log.d(TAG, "Grade alert notification posted successfully for " + studentName + " (" + subjectName + ")");
        } catch (SecurityException se) {
            Log.w(TAG, "SecurityException while notifying: " + se.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Failed posting grade notification", e);
        }
    }

    /**
     * Interface for listening to real-time grade updates in an Activity.
     */
    public interface OnGradeAlertReceivedListener {
        void onGradeAlertReceived(String subjectName, String grade, double percentage);
    }

    /**
     * Starts listening in real-time to the Firestore `gradeAlerts` collection for grades
     * uploaded for this student's register number.
     * When an instructor uploads a grade, triggers a local notification and invokes callback.
     */
    public static ListenerRegistration listenForStudentGradeAlerts(
            @NonNull Context context,
            @NonNull String registerNo,
            @Nullable OnGradeAlertReceivedListener listener
    ) {
        if (registerNo == null || registerNo.trim().isEmpty()) return null;

        final String cleanRegNo = registerNo.trim();
        createNotificationChannel(context);

        try {
            return FirebaseFirestore.getInstance().collection("gradeAlerts")
                    .whereEqualTo("registerNo", cleanRegNo)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(10)
                    .addSnapshotListener((snapshots, error) -> {
                        if (error != null) {
                            Log.w(TAG, "Listen for grade alerts failed: " + error.getMessage());
                            return;
                        }

                        if (snapshots == null || snapshots.isEmpty()) return;

                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                String docId = dc.getDocument().getId();
                                if (notifiedAlertIds.contains(docId)) {
                                    continue;
                                }

                                // Check creation timestamp to only notify on recent uploads
                                com.google.firebase.Timestamp ts = dc.getDocument().getTimestamp("createdAt");
                                if (ts != null && (ts.toDate().getTime() < sessionStartTimeMs)) {
                                    // Skip old historical alerts
                                    notifiedAlertIds.add(docId);
                                    continue;
                                }

                                notifiedAlertIds.add(docId);

                                String studentName = dc.getDocument().getString("studentName");
                                String regNo = dc.getDocument().getString("registerNo");
                                String subjectName = dc.getDocument().getString("subjectName");
                                String subjectCode = dc.getDocument().getString("subjectCode");
                                String grade = dc.getDocument().getString("grade");
                                Double totalMarks = dc.getDocument().getDouble("totalMarks");
                                Double percentage = dc.getDocument().getDouble("percentage");
                                Double gradePoint = dc.getDocument().getDouble("gradePoint");
                                String instructorName = dc.getDocument().getString("instructorName");
                                Long semester = dc.getDocument().getLong("semester");

                                sendGradeAlertNotification(
                                        context,
                                        studentName != null ? studentName : "Student",
                                        regNo != null ? regNo : cleanRegNo,
                                        subjectName != null ? subjectName : "Subject Result",
                                        subjectCode != null ? subjectCode : "",
                                        grade != null ? grade : "A",
                                        totalMarks != null ? totalMarks : 0.0,
                                        percentage != null ? percentage : 0.0,
                                        gradePoint != null ? gradePoint : 0.0,
                                        instructorName != null ? instructorName : "Instructor",
                                        semester != null ? semester.intValue() : 1
                                );

                                if (listener != null) {
                                    listener.onGradeAlertReceived(
                                            subjectName != null ? subjectName : "Subject",
                                            grade != null ? grade : "A",
                                            percentage != null ? percentage : 0.0
                                    );
                                }
                            }
                        }
                    });
        } catch (Exception e) {
            Log.w(TAG, "Error setting up grade alerts snapshot listener: " + e.getMessage());
            return null;
        }
    }
}
