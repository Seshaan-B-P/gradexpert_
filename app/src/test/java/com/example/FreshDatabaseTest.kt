package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.database.DatabaseHelper
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FreshDatabaseTest {

    private lateinit var dbHelper: DatabaseHelper

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        dbHelper = DatabaseHelper(context)
    }

    @Test
    fun testDatabaseIsCleanForFreshTesting() {
        // Teachers list should be empty
        val teachers = dbHelper.allTeachers
        assertTrue("Teachers list should be empty", teachers.isEmpty())

        // Students list should be empty
        val students = dbHelper.allStudents
        assertTrue("Students list should be empty", students.isEmpty())

        // Subjects list should be empty
        val subjects = dbHelper.allSubjects
        assertTrue("Subjects list should be empty", subjects.isEmpty())

        // Teacher activities list should be empty
        val activities = dbHelper.allTeacherActivities
        assertTrue("Activities list should be empty", activities.isEmpty())

        // Toppers list should be empty
        val toppers = dbHelper.toppersList
        assertTrue("Toppers list should be empty", toppers.isEmpty())

        // Shortage attendance list should be empty
        val shortage = dbHelper.studentsWithAttendanceShortage
        assertTrue("Shortage list should be empty", shortage.isEmpty())

        // Notifications list should be empty
        val notifications = dbHelper.allNotifications
        assertTrue("Notifications list should be empty", notifications.isEmpty())

        // Admin account should be ready for testing
        val admin = dbHelper.authenticateAdmin("admin@gradexpert.com", "123456")
        assertNotNull("Admin login should be available for creating new data", admin)
    }
}
