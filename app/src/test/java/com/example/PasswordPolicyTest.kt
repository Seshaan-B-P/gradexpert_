package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.database.DatabaseHelper
import com.example.model.Student
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
class PasswordPolicyTest {

    private lateinit var dbHelper: DatabaseHelper

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        dbHelper = DatabaseHelper(context)
    }

    @Test
    fun testAdminCanChangeTeacherPassword() {
        // 1. Admin creates teacher with initial password
        val teacher = Teacher(0, "Test Teacher", "teacher_test@gradexpert.com", "pass1", "CS", "+1 1234")
        dbHelper.addTeacher(teacher)

        // 2. Admin updates teacher password
        val addedTeacher = dbHelper.allTeachers.first { it.email == "teacher_test@gradexpert.com" }
        addedTeacher.password = "newPassAdminSet123"
        val updated = dbHelper.updateTeacher(addedTeacher)
        assertTrue("Admin should be able to update teacher password", updated)

        // 3. Teacher logs in with new password set by Admin
        val authTeacher = dbHelper.authenticateTeacher("teacher_test@gradexpert.com", "newPassAdminSet123")
        assertNotNull("Teacher should authenticate with new password set by Admin", authTeacher)
    }

    @Test
    fun testTeacherCanChangeStudentPassword() {
        // 1. Teacher creates student with initial password
        val student = Student(0, "Test Student", "REG999", "CS", 1, "student_test@gradexpert.com", "+1 999", "")
        dbHelper.addStudent(student, "studentPass1")

        // 2. Teacher updates student password
        val addedStudent = dbHelper.allStudents.first { it.regNo == "REG999" }
        val updated = dbHelper.updateStudentWithPassword(addedStudent, "newTeacherSetPass456")
        assertTrue("Teacher should be able to update student password", updated)

        // 3. Student logs in with new password set by Teacher
        val authStudent = dbHelper.authenticateStudent("student_test@gradexpert.com", "newTeacherSetPass456")
        assertNotNull("Student should authenticate with new password set by Teacher", authStudent)
    }
}
