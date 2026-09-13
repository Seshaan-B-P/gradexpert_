package com.example

import com.example.model.Teacher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ManageTeachersFilterTest {

    @Test
    fun testDepartmentFiltering() {
        val mcaTeacher = Teacher(1, "Dr. Ramesh", "ramesh@gradexpert.com", "pass123", "Master of Computer Applications", "9876543210").apply {
            departmentShortName = "MCA"
        }
        val cseTeacher = Teacher(2, "Prof. Suresh", "suresh@gradexpert.com", "pass123", "Computer Science and Engineering", "9876543211").apply {
            departmentShortName = "CSE"
        }
        val eceTeacher = Teacher(3, "Dr. Priya", "priya@gradexpert.com", "pass123", "Electronics & Communication Engineering", "9876543212").apply {
            departmentShortName = "ECE"
        }

        // Test MCA filter
        assertTrue("MCA filter should match MCA teacher", ManageTeachersActivity.doesTeacherMatchDept(mcaTeacher, "MCA"))
        assertFalse("MCA filter should not match CSE teacher", ManageTeachersActivity.doesTeacherMatchDept(cseTeacher, "MCA"))

        // Test CSE filter
        assertTrue("CSE filter should match CSE teacher", ManageTeachersActivity.doesTeacherMatchDept(cseTeacher, "CSE"))
        assertFalse("CSE filter should not match ECE teacher", ManageTeachersActivity.doesTeacherMatchDept(eceTeacher, "CSE"))

        // Test ALL filter
        assertTrue("ALL filter should match MCA teacher", ManageTeachersActivity.doesTeacherMatchDept(mcaTeacher, "ALL"))
        assertTrue("ALL filter should match CSE teacher", ManageTeachersActivity.doesTeacherMatchDept(cseTeacher, "ALL"))
    }

    @Test
    fun testProgramLevelFiltering() {
        val pgTeacher = Teacher(1, "Dr. Alan", "alan@gradexpert.com", "pass123", "Master of Computer Applications", "9876543210").apply {
            departmentShortName = "MCA"
            programLevel = "PG"
        }
        val ugTeacher = Teacher(2, "Prof. Ada", "ada@gradexpert.com", "pass123", "Computer Science and Engineering", "9876543211").apply {
            departmentShortName = "CSE"
            programLevel = "UG"
        }
        val unassignedPgTeacher = Teacher(3, "Dr. John", "john@gradexpert.com", "pass123", "Master of Business Administration", "9876543212").apply {
            departmentShortName = "MBA"
            programLevel = null // Should resolve to PG automatically based on MBA
        }

        // Test PG filter
        assertTrue("PG filter should match explicit PG teacher", ManageTeachersActivity.doesTeacherMatchLevel(pgTeacher, "PG"))
        assertFalse("PG filter should not match UG teacher", ManageTeachersActivity.doesTeacherMatchLevel(ugTeacher, "PG"))
        assertTrue("PG filter should match MBA teacher with null level by default resolution", ManageTeachersActivity.doesTeacherMatchLevel(unassignedPgTeacher, "PG"))

        // Test UG filter
        assertTrue("UG filter should match UG teacher", ManageTeachersActivity.doesTeacherMatchLevel(ugTeacher, "UG"))
        assertFalse("UG filter should not match PG teacher", ManageTeachersActivity.doesTeacherMatchLevel(pgTeacher, "UG"))

        // Test ALL filter
        assertTrue("ALL filter should match any level", ManageTeachersActivity.doesTeacherMatchLevel(pgTeacher, "ALL"))
        assertTrue("ALL filter should match any level", ManageTeachersActivity.doesTeacherMatchLevel(ugTeacher, "ALL"))
    }

    @Test
    fun testStatusFiltering() {
        val activeTeacher = Teacher(1, "Teacher Active", "act@test.com", "pass", "CSE", "123").apply {
            status = "ACTIVE"
        }
        val inactiveTeacher = Teacher(2, "Teacher Inactive", "inact@test.com", "pass", "CSE", "123").apply {
            status = "INACTIVE"
        }
        val suspendedTeacher = Teacher(3, "Teacher Suspended", "susp@test.com", "pass", "CSE", "123").apply {
            status = "SUSPENDED"
        }

        assertTrue(ManageTeachersActivity.doesTeacherMatchStatus(activeTeacher, "ACTIVE"))
        assertFalse(ManageTeachersActivity.doesTeacherMatchStatus(inactiveTeacher, "ACTIVE"))

        assertTrue(ManageTeachersActivity.doesTeacherMatchStatus(inactiveTeacher, "INACTIVE"))
        assertFalse(ManageTeachersActivity.doesTeacherMatchStatus(activeTeacher, "INACTIVE"))

        assertTrue(ManageTeachersActivity.doesTeacherMatchStatus(suspendedTeacher, "SUSPENDED"))

        assertTrue(ManageTeachersActivity.doesTeacherMatchStatus(activeTeacher, "ALL"))
        assertTrue(ManageTeachersActivity.doesTeacherMatchStatus(suspendedTeacher, "ALL"))
    }

    @Test
    fun testSearchQueryMatching() {
        val teacher = Teacher(10, "Dr. Robert Downey", "rdj@gradexpert.com", "pass123", "Mechanical Engineering", "9988776655").apply {
            departmentShortName = "MECH"
            employeeId = "EMP-9021"
            designation = "Associate Professor"
            qualification = "Ph.D. Robotics"
            assignedSubjectNames = listOf("Fluid Mechanics", "Thermodynamics")
        }

        // Match by name
        assertTrue(ManageTeachersActivity.doesTeacherMatchQuery(teacher, "Robert"))
        assertTrue(ManageTeachersActivity.doesTeacherMatchQuery(teacher, "downey"))

        // Match by email
        assertTrue(ManageTeachersActivity.doesTeacherMatchQuery(teacher, "rdj@gradexpert.com"))

        // Match by employee id
        assertTrue(ManageTeachersActivity.doesTeacherMatchQuery(teacher, "EMP-9021"))
        assertTrue(ManageTeachersActivity.doesTeacherMatchQuery(teacher, "9021"))

        // Match by department or short name
        assertTrue(ManageTeachersActivity.doesTeacherMatchQuery(teacher, "mech"))
        assertTrue(ManageTeachersActivity.doesTeacherMatchQuery(teacher, "Mechanical"))

        // Match by subject name
        assertTrue(ManageTeachersActivity.doesTeacherMatchQuery(teacher, "Thermodynamics"))
        assertTrue(ManageTeachersActivity.doesTeacherMatchQuery(teacher, "fluid"))

        // Non-matching query
        assertFalse(ManageTeachersActivity.doesTeacherMatchQuery(teacher, "Computer Science"))
    }
}
