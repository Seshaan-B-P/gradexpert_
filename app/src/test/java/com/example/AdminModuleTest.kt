package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.database.DatabaseHelper
import com.example.model.Teacher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AdminModuleTest {

    private lateinit var dbHelper: DatabaseHelper

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        dbHelper = DatabaseHelper(context)
    }

    @Test
    fun testAdminLogin_DefaultCredentials() {
        val admin = dbHelper.authenticateAdmin("admin@gradexpert.com", "123456")
        assertNotNull("Admin should authenticate with default credentials", admin)
        assertEquals("ADMIN", admin?.role)
        assertEquals("admin@gradexpert.com", admin?.email)
    }

    @Test
    fun testUpdateAdminCredentials() {
        val updated = dbHelper.updateAdminCredentials("admin@gradexpert.com", "admin_new@gradexpert.com", "newpass123")
        assertTrue("Admin credentials should update", updated)

        val newAdmin = dbHelper.authenticateAdmin("admin_new@gradexpert.com", "newpass123")
        assertNotNull("New admin credentials should be valid", newAdmin)
    }

    @Test
    fun testTeacherCRUD_Operations() {
        // 1. Add Teacher
        val newTeacher = Teacher(0, "Dr. Grace Hopper", "grace@gradexpert.com", "grace123", "Computer Science", "+1 555666777")
        val added = dbHelper.addTeacher(newTeacher)
        assertTrue("Teacher should be added successfully", added)

        // 2. Read Teachers
        val teachers = dbHelper.allTeachers
        assertTrue("Teachers list should not be empty", teachers.isNotEmpty())
        val found = teachers.any { it.email == "grace@gradexpert.com" }
        assertTrue("Newly added teacher should exist in list", found)

        // 3. Delete Teacher
        val target = teachers.first { it.email == "grace@gradexpert.com" }
        val deleted = dbHelper.deleteTeacher(target.id)
        assertTrue("Teacher should be deleted", deleted)
    }

    @Test
    fun testTeacherActivityLogging() {
        dbHelper.logTeacherActivity("Dr. Alan Turing", "Marked Test Attendance", "Semester 5 Data Structures", "Attendance")
        val activities = dbHelper.allTeacherActivities
        assertTrue("Teacher activities should be retrieved", activities.isNotEmpty())
    }
}
