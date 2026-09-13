package com.example

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.example.database.DatabaseHelper
import com.example.model.Student
import com.example.utils.GradeNotificationHelper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GradeNotificationTest {

    private lateinit var context: Context
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(context as Application).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        dbHelper = DatabaseHelper(context)
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Test
    fun testGradeNotificationChannelCreation() {
        GradeNotificationHelper.createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(GradeNotificationHelper.CHANNEL_ID_GRADES)
            assertNotNull("Grade alert notification channel should exist", channel)
            assertEquals("Channel ID should match", GradeNotificationHelper.CHANNEL_ID_GRADES, channel.id)
            assertEquals("High importance expected for grade alerts", NotificationManager.IMPORTANCE_HIGH, channel.importance)
        }
    }

    @Test
    fun testSendGradeAlertNotificationPostsSystemNotification() {
        GradeNotificationHelper.sendGradeAlertNotification(
            context,
            "John Doe",
            "MCA001",
            "Advanced Java & Frameworks",
            "CS501",
            "A+",
            88.0,
            88.0,
            9.0,
            "Dr. Alan Turing",
            3
        )

        val shadowNotificationManager = shadowOf(notificationManager)
        val notifications = shadowNotificationManager.allNotifications
        assertTrue("At least one notification should be posted", notifications.isNotEmpty())

        val notification = notifications.last()
        val title = notification.extras.getString("android.title")
        val text = notification.extras.getCharSequence("android.text")?.toString()

        assertNotNull(title)
        assertTrue("Notification title should mention subject or grade", title!!.contains("CS501") || title.contains("Advanced Java"))
        assertNotNull(text)
        assertTrue("Notification text should mention grade or score", text!!.contains("A+") || text.contains("88.0%"))
    }

    @Test
    fun testDispatchGradeUploadAlertInsertsInLocalDatabase() {
        val initialCount = dbHelper.getUnreadNotificationsCount("STUDENT")

        GradeNotificationHelper.dispatchGradeUploadAlert(
            context,
            "Jane Smith",
            "MCA002",
            2,
            "Cloud Computing & DevOps",
            "CS503",
            "O",
            96.0,
            96.0,
            10.0,
            "Prof. Ada Lovelace",
            3
        )

        val updatedCount = dbHelper.getUnreadNotificationsCount("STUDENT")
        assertTrue("Unread student notifications should increase", updatedCount > initialCount)

        val notificationsList = dbHelper.allNotifications
        val hasGradeNotification = notificationsList.any { it.title.contains("Cloud Computing") && it.sender == "Prof. Ada Lovelace" }
        assertTrue("Database notifications should contain the new grade entry", hasGradeNotification)
    }

    @Test
    fun testSubscribeStudentToGradeAlertsDoesNotCrash() {
        val student = Student()
        student.setId(1)
        student.setName("Alice")
        student.setRegisterNo("MCA001")
        student.setDepartment("MCA")
        student.setSemester("3")

        // Subscribing to topics should execute without throwing exceptions
        GradeNotificationHelper.subscribeStudentToGradeAlerts(context, student)
    }
}
