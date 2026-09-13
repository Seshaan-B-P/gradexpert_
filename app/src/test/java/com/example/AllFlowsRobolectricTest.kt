package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.database.DatabaseHelper
import com.example.model.PasswordResetRequest
import com.example.model.Student
import com.example.model.Subject
import com.example.model.SubjectGradeItem
import com.example.model.Teacher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AllFlowsRobolectricTest {

    private lateinit var context: Context
    private lateinit var dbHelper: DatabaseHelper

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        dbHelper = DatabaseHelper(context)
    }

    @Test
    fun testFlow1_AdminAuthenticationAndSettings() {
        // 1. Authenticate default admin
        val admin = dbHelper.authenticateAdmin("admin@gradexpert.com", "123456")
        assertNotNull("Admin should authenticate with default password", admin)
        assertEquals("ADMIN", admin?.role)

        // 2. Reject invalid admin credentials
        val wrongAdmin = dbHelper.authenticateAdmin("admin@gradexpert.com", "wrongpass")
        assertNull("Admin should not authenticate with wrong password", wrongAdmin)

        // 3. Update admin password
        val updated = dbHelper.updateAdminCredentials("admin@gradexpert.com", "admin_updated@gradexpert.com", "adminSecure2026")
        assertTrue("Admin credentials should update", updated)

        val updatedAdmin = dbHelper.authenticateAdmin("admin_updated@gradexpert.com", "adminSecure2026")
        assertNotNull("Updated admin should authenticate", updatedAdmin)
    }

    @Test
    fun testFlow2_TeacherManagementAndAuthentication() {
        // 1. Create Teacher
        val teacher = Teacher(0, "Prof. Katherine Johnson", "kjohnson@gradexpert.com", "orbit123", "Mathematics", "+1 1234567890")
        teacher.loginId = "TCH099"
        teacher.employeeId = "EMP099"
        val added = dbHelper.addTeacher(teacher)
        assertTrue("Teacher should be added", added)

        // 2. Authenticate by Email
        val authByEmail = dbHelper.authenticateTeacher("kjohnson@gradexpert.com", "orbit123")
        assertNotNull("Teacher should authenticate by email", authByEmail)
        assertEquals("TEACHER", authByEmail?.role)

        // 3. Authenticate by Login ID / Employee ID
        val authByLoginId = dbHelper.authenticateTeacher("TCH099", "orbit123")
        assertNotNull("Teacher should authenticate by login ID", authByLoginId)

        // 4. Update Teacher Details
        val allTeachers = dbHelper.allTeachers
        val currentTeacher = allTeachers.first { it.email == "kjohnson@gradexpert.com" }
        currentTeacher.name = "Dr. Katherine Johnson"
        currentTeacher.department = "Applied Mathematics"
        val updated = dbHelper.updateTeacher(currentTeacher)
        assertTrue("Teacher details should update", updated)

        // 5. Password Reset
        val pwdReset = dbHelper.resetTeacherPassword("kjohnson@gradexpert.com", "newOrbit456")
        assertTrue("Teacher password should reset", pwdReset)
        val reAuth = dbHelper.authenticateTeacher("kjohnson@gradexpert.com", "newOrbit456")
        assertNotNull("Teacher should authenticate with new password", reAuth)

        // 6. Delete Teacher
        val deleted = dbHelper.deleteTeacher(currentTeacher.id)
        assertTrue("Teacher should be deleted", deleted)
    }

    @Test
    fun testFlow3_StudentManagementAndAuthentication() {
        // 1. Add Student
        val student = Student(0, "Robert Down", "REG202655", "Computer Science", 4, "robert@gradexpert.com", "+1 5551234", "")
        student.loginId = "CS202655"
        val studentId = dbHelper.addStudent(student, "studentPass123")
        assertTrue("Student ID should be valid", studentId > 0)

        // 2. Authenticate by Email
        val authByEmail = dbHelper.authenticateStudent("robert@gradexpert.com", "studentPass123")
        assertNotNull("Student should authenticate by email", authByEmail)
        assertEquals("STUDENT", authByEmail?.role)

        // 3. Authenticate by Reg No
        val authByRegNo = dbHelper.authenticateStudent("REG202655", "studentPass123")
        assertNotNull("Student should authenticate by register number", authByRegNo)

        // 4. Authenticate by Login ID
        val authByLoginId = dbHelper.authenticateStudent("CS202655", "studentPass123")
        assertNotNull("Student should authenticate by login ID", authByLoginId)

        // 5. Update Student details
        val storedStudent = dbHelper.allStudents.first { it.regNo == "REG202655" }
        storedStudent.phone = "+1 9998887"
        storedStudent.semester = "Semester 5"
        val updated = dbHelper.updateStudent(storedStudent)
        assertTrue("Student should be updated", updated)

        // 6. Reset Student password
        val reset = dbHelper.resetStudentPassword("REG202655", "robertNewPass789")
        assertTrue("Password should reset", reset)
        val newAuth = dbHelper.authenticateStudent("REG202655", "robertNewPass789")
        assertNotNull("Student should authenticate with new password", newAuth)
    }

    @Test
    fun testFlow4_SubjectCurriculumManagement() {
        // 1. Add Subjects
        val sub1 = Subject(0, "CS401", "Operating Systems", 4, 4, "Computer Science")
        val subId1 = dbHelper.addSubject(sub1)
        assertTrue("Subject 1 should be added", subId1 > 0)

        val sub2 = Subject(0, "CS402", "Computer Networks", 4, 4, "Computer Science")
        val subId2 = dbHelper.addSubject(sub2)
        assertTrue("Subject 2 should be added", subId2 > 0)

        // 2. Retrieve by Semester
        val sem4Subjects = dbHelper.getSubjectsBySemester(4)
        assertTrue("Should return subjects for semester 4", sem4Subjects.size >= 2)

        // 3. Search Subject
        val searchResult = dbHelper.searchSubjects("Networks")
        assertEquals(1, searchResult.size)
        assertEquals("CS402", searchResult[0].subjectCode)

        // 4. Update Subject
        sub1.id = subId1.toInt()
        sub1.subjectName = "Advanced Operating Systems"
        val updated = dbHelper.updateSubject(sub1)
        assertTrue("Subject should be updated", updated)

        // 5. Delete Subject
        val deleted = dbHelper.deleteSubject(subId2.toInt())
        assertTrue("Subject should be deleted", deleted)
    }

    @Test
    fun testFlow5_AttendanceTrackingAndShortage() {
        // 1. Create a Student and Subject
        val student = Student(0, "Emma Watson", "REG202688", "Electrical", 3, "emma@gradexpert.com", "+1 4443332", "")
        val studentId = dbHelper.addStudent(student, "pass123").toInt()

        val subject = Subject(0, "EE301", "Circuit Theory", 4, 3, "Electrical")
        val subjectId = dbHelper.addSubject(subject).toInt()

        // 2. Mark attendance records (3 PRESENT, 1 ABSENT => 75%)
        dbHelper.saveOrUpdateAttendanceRecord(studentId, subjectId, "2026-09-01", "PRESENT")
        dbHelper.saveOrUpdateAttendanceRecord(studentId, subjectId, "2026-09-02", "PRESENT")
        dbHelper.saveOrUpdateAttendanceRecord(studentId, subjectId, "2026-09-03", "PRESENT")
        dbHelper.saveOrUpdateAttendanceRecord(studentId, subjectId, "2026-09-04", "ABSENT")

        // 3. Verify student attendance percentage
        val pct = dbHelper.getStudentAttendancePercentage(studentId)
        assertEquals(75, pct)

        // 4. Add another ABSENT => 3 / 5 = 60% => Shortage (< 75%)
        dbHelper.saveOrUpdateAttendanceRecord(studentId, subjectId, "2026-09-05", "ABSENT")
        val newPct = dbHelper.getStudentAttendancePercentage(studentId)
        assertEquals(60, newPct)

        val shortageList = dbHelper.studentsWithAttendanceShortage
        val foundShortage = shortageList.any { it.id == studentId }
        assertTrue("Student with 60% attendance should be in shortage list", foundShortage)
    }

    @Test
    fun testFlow6_MarksResultsAndCGPA() {
        // 1. Register student
        val student = Student(0, "David Miller", "REG202699", "Information Tech", 6, "david@gradexpert.com", "+1 7776665", "")
        val studentId = dbHelper.addStudent(student, "pass123").toInt()

        // 2. Enter grades
        val grades = listOf(
            SubjectGradeItem("Cloud Computing", 4, 92),
            SubjectGradeItem("Artificial Intelligence", 4, 96)
        )
        val saved = dbHelper.saveStudentResultsAndMarks(studentId, 6, 9.60, 9.35, 24, grades)
        assertTrue("Results and marks should save", saved)

        // 3. Student reads CGPA and SGPA
        val cgpa = dbHelper.getLatestCGPA(studentId)
        val sgpa = dbHelper.getLatestSGPA(studentId)
        assertEquals(9.35, cgpa, 0.01)
        assertEquals(9.60, sgpa, 0.01)

        // 4. Student reads marks
        val marks = dbHelper.getStudentSubjectMarksBySemester(studentId, 6)
        assertEquals(2, marks.size)
    }

    @Test
    fun testFlow7_AssignmentsWorkflow() {
        // 1. Create Subject and Student
        val subject = Subject(0, "CS505", "Compiler Design", 4, 5, "Computer Science")
        val subjectId = dbHelper.addSubject(subject).toInt()

        val student = Student(0, "Sophia Brown", "REG202644", "Computer Science", 5, "sophia@gradexpert.com", "+1 3332221", "")
        val studentId = dbHelper.addStudent(student, "pass123").toInt()

        // 2. Teacher creates Assignment
        val assignmentAdded = dbHelper.addAssignment("Lexical Analyzer", subjectId, "2026-09-20", "Implement DFA in C", "/assignments/dfa.pdf")
        assertTrue("Assignment should be added", assignmentAdded)

        val allAssignments = dbHelper.allAssignments
        assertTrue("Assignments should exist", allAssignments.isNotEmpty())
        val assignment = allAssignments.first { it.title == "Lexical Analyzer" }

        // 3. Student queries assignments
        val studentAssignments = dbHelper.getAssignmentsForStudent(studentId)
        assertTrue("Student should see assignments", studentAssignments.isNotEmpty())

        // 4. Student submits assignment
        val submitted = dbHelper.submitAssignment(assignment.id, studentId, "2026-09-18", "/submissions/sophia_dfa.pdf", "Completed on time")
        assertTrue("Assignment submission should succeed", submitted)

        // 5. Teacher checks submissions
        val submissions = dbHelper.getSubmissionsForAssignment(assignment.id)
        assertTrue("Teacher should see submissions", submissions.isNotEmpty())
        val sophiaSub = submissions.first { it.studentId == studentId.toString() }
        assertEquals("SUBMITTED", sophiaSub.status)
    }

    @Test
    fun testFlow8_BroadcastAlertsAndNotifications() {
        // 1. Admin/Teacher adds notification
        val added = dbHelper.addNotification("Campus Holiday", "College closed tomorrow due to rain", "2026-09-11", "STUDENT", "General", "Principal")
        assertTrue("Notification should be added", added)

        // 2. Query unread notifications for STUDENT
        val unreadCount = dbHelper.getUnreadNotificationsCount("STUDENT")
        assertTrue("Unread count should be >= 1", unreadCount >= 1)

        val notifications = dbHelper.getNotificationsForRole("STUDENT")
        assertTrue("Notifications list should have items", notifications.isNotEmpty())

        // 3. Mark as read
        val firstNotif = notifications.first()
        val marked = dbHelper.markNotificationAsRead(firstNotif.id)
        assertTrue("Notification should be marked as read", marked)
    }

    @Test
    fun testFlow9_PasswordResetRequestWorkflow() {
        // 1. Student requests password reset
        val req = PasswordResetRequest("REQ_999", "STU_100", "John Doe", "john@gradexpert.com", "STUDENT", "REG202677", "IT", "Semester 3")
        val inserted = dbHelper.insertPasswordResetRequest(req)
        assertTrue("Request should be inserted", inserted > 0)

        // 2. Admin retrieves pending requests
        val pendingCount = dbHelper.getPendingPasswordResetRequestsCount()
        assertTrue("Pending count should be >= 1", pendingCount >= 1)

        val pendingRequests = dbHelper.getAllPasswordResetRequests("PENDING")
        assertTrue("Pending list should contain request", pendingRequests.any { it.requestId == "REQ_999" })

        // 3. Admin updates request to COMPLETED
        val updated = dbHelper.updatePasswordResetRequestStatus("REQ_999", "COMPLETED", "Admin Officer", "Verified student identity in person")
        assertTrue("Status should update to COMPLETED", updated)

        // 4. Verify request is no longer pending
        val afterPending = dbHelper.getAllPasswordResetRequests("PENDING")
        assertTrue("Should not be in pending list", afterPending.none { it.requestId == "REQ_999" })
    }
}
