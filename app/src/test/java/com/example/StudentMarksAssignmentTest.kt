package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.database.DatabaseHelper
import com.example.model.Student
import com.example.model.SubjectGradeItem
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
class StudentMarksAssignmentTest {

    private lateinit var dbHelper: DatabaseHelper

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        dbHelper = DatabaseHelper(context)
    }

    @Test
    fun testTeacherAssignsMarksToStudent_StudentReadsOnly() {
        // 1. Teacher registers student
        val newStudent = Student(0, "Alice Smith", "REG202610", "Computer Science", 5, "alice@gradexpert.com", "+1 987654", "")
        dbHelper.addStudent(newStudent, "alice123")

        val registeredStudent = dbHelper.allStudents.first { it.regNo == "REG202610" }

        // 2. Teacher assigns marks for Alice
        val marksList = listOf(
            SubjectGradeItem("Data Structures", 4, 95),
            SubjectGradeItem("Database Systems", 4, 88)
        )
        val saved = dbHelper.saveStudentResultsAndMarks(registeredStudent.id, 5, 9.50, 9.40, 20, marksList)
        assertTrue("Teacher should successfully save marks for Alice", saved)

        // 3. Student logs in and reads marks (Read-Only)
        val studentAuth = dbHelper.authenticateStudent("alice@gradexpert.com", "alice123")
        assertNotNull("Student Alice should authenticate", studentAuth)

        val readCgpa = dbHelper.getLatestCGPA(registeredStudent.id)
        val readSgpa = dbHelper.getLatestSGPA(registeredStudent.id)

        assertEquals(9.40, readCgpa, 0.01)
        assertEquals(9.50, readSgpa, 0.01)
    }
}
