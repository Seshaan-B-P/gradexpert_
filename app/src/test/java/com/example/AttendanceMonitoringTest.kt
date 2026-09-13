package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.database.DatabaseHelper
import com.example.model.StudentAttendanceLog
import com.example.utils.AttendanceNotificationHelper
import com.example.utils.SessionManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AttendanceMonitoringTest {

    private lateinit var context: Context
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("gradexpert.db")
        dbHelper = DatabaseHelper(context)
        sessionManager = SessionManager(context)
    }

    @Test
    fun testAttendanceThresholdConfiguration() {
        // Default threshold should be 75.0%
        val defaultThreshold = sessionManager.attendanceThreshold
        assertEquals(75.0f, defaultThreshold, 0.01f)

        // Custom threshold update (e.g. 80.0%)
        sessionManager.attendanceThreshold = 80.0f
        assertEquals(80.0f, sessionManager.attendanceThreshold, 0.01f)

        // Lenient threshold (e.g. 70.0%)
        sessionManager.attendanceThreshold = 70.0f
        assertEquals(70.0f, sessionManager.attendanceThreshold, 0.01f)
    }

    @Test
    fun testAttendanceLogCalculationAndWarningBelowThreshold() {
        val studentId = 1
        val subject = "Operating Systems"
        val totalClasses = 20
        val attendedClasses = 13 // 65.0%
        val percentage = (attendedClasses.toDouble() / totalClasses.toDouble()) * 100.0
        val threshold = 75.0

        val logId = dbHelper.addStudentAttendanceLog(
            studentId, subject, totalClasses, attendedClasses,
            percentage, "2026-09-11 10:00", "Missed labs due to sickness", threshold
        )

        assertTrue("Log ID should be greater than 0", logId > 0)

        val logs = dbHelper.getStudentAttendanceLogs(studentId)
        assertFalse("Logs list should not be empty", logs.isEmpty())

        val log = logs[0]
        assertEquals(subject, log.subjectName)
        assertEquals(20, log.totalClasses)
        assertEquals(13, log.attendedClasses)
        assertEquals(65.0, log.percentage, 0.01)
        assertEquals(75.0, log.threshold, 0.01)
        assertEquals("BELOW_THRESHOLD", log.status)
        assertTrue(log.isBelowThreshold)

        // Classes needed to recover to 75%:
        // target = 0.75
        // needed = ceil((0.75 * 20 - 13) / (1 - 0.75)) = ceil((15 - 13) / 0.25) = ceil(2 / 0.25) = 8
        assertEquals(8, log.classesNeededForThreshold)
        assertEquals(0, log.classesCanAffordToMiss)

        // Verify that in-app notification was generated for low attendance
        val notifications = dbHelper.getNotificationsForRole("STUDENT")
        val alertNotif = notifications.find { it.category == "ATTENDANCE_ALERT" }
        assertNotNull("Low attendance alert notification must be automatically generated", alertNotif)
        assertTrue("Notification message should mention subject and percentage",
            alertNotif!!.message.contains("Operating Systems") && alertNotif.message.contains("65.0%"))
    }

    @Test
    fun testAttendanceLogSafeAboveThreshold() {
        val studentId = 1
        val subject = "Database Systems"
        val totalClasses = 30
        val attendedClasses = 27 // 90.0%
        val percentage = (attendedClasses.toDouble() / totalClasses.toDouble()) * 100.0
        val threshold = 75.0

        val logId = dbHelper.addStudentAttendanceLog(
            studentId, subject, totalClasses, attendedClasses,
            percentage, "2026-09-11 11:00", "Good attendance record", threshold
        )

        assertTrue(logId > 0)

        val logs = dbHelper.getStudentAttendanceLogs(studentId)
        val log = logs.find { it.subjectName == subject }
        assertNotNull(log)
        assertEquals("NORMAL", log!!.status)
        assertFalse(log.isBelowThreshold)
        assertEquals(0, log.classesNeededForThreshold)

        // Can miss = floor((27 / 0.75) - 30) = floor(36 - 30) = 6 classes
        assertEquals(6, log.classesCanAffordToMiss)
    }

    @Test
    fun testDeleteStudentAttendanceLog() {
        val studentId = 1
        val logId = dbHelper.addStudentAttendanceLog(
            studentId, "Algorithms", 15, 12, 80.0, "2026-09-11", "", 75.0
        )
        assertTrue(logId > 0)

        var logs = dbHelper.getStudentAttendanceLogs(studentId)
        val initialCount = logs.size

        val deleted = dbHelper.deleteStudentAttendanceLog(logId.toInt())
        assertTrue("Delete operation should succeed", deleted)

        logs = dbHelper.getStudentAttendanceLogs(studentId)
        assertEquals(initialCount - 1, logs.size)
    }

    @Test
    fun testBelowThresholdLogsCounter() {
        val studentId = 2
        assertEquals(0, dbHelper.getBelowThresholdAttendanceLogsCount(studentId))

        // Add 1 normal log
        dbHelper.addStudentAttendanceLog(studentId, "Sub 1", 20, 18, 90.0, "2026-09-11", "", 75.0)
        assertEquals(0, dbHelper.getBelowThresholdAttendanceLogsCount(studentId))

        // Add 2 below threshold logs
        dbHelper.addStudentAttendanceLog(studentId, "Sub 2", 20, 10, 50.0, "2026-09-11", "", 75.0)
        dbHelper.addStudentAttendanceLog(studentId, "Sub 3", 20, 12, 60.0, "2026-09-11", "", 75.0)

        assertEquals(2, dbHelper.getBelowThresholdAttendanceLogsCount(studentId))
    }

    @Test
    fun testNotificationHelperChannelCreation() {
        AttendanceNotificationHelper.createNotificationChannel(context)
        // Verify no crash during notification dispatch attempt
        AttendanceNotificationHelper.sendLowAttendanceNotification(
            context, "Software Engineering", 60.0, 75.0, 4
        )
    }
}
