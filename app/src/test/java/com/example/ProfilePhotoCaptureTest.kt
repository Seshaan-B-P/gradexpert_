package com.example

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.database.DatabaseHelper
import com.example.model.Student
import com.example.model.Teacher
import com.example.utils.ProfilePhotoManager
import com.example.utils.SessionManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProfilePhotoCaptureTest {

    private lateinit var context: Context
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dbHelper = DatabaseHelper(context)
        sessionManager = SessionManager(context)
    }

    @Test
    fun testTempCaptureUriGeneration() {
        val captureUri = ProfilePhotoManager.createTempCaptureUri(context)
        assertNotNull("Temp capture URI should not be null", captureUri)
        assertTrue("Capture URI should use FileProvider content scheme", captureUri.toString().startsWith("content://"))
    }

    @Test
    fun testStudentPhotoDatabasePersistence() {
        // Register/Insert dummy student
        val regNo = "TEST_REG_101"
        val photoPath = "/data/user/0/com.example/files/profile_photos/student_test.jpg"
        val student = Student(999, "Test Student", regNo, "Computer Science", 3, "test_student@example.com", "9876543210", "")
        dbHelper.addStudent(student, "studentPass123")

        // Update photo URI
        val updated = dbHelper.updateStudentPhotoUri(999, regNo, photoPath)
        assertTrue("Updating student photo URI should succeed", updated)

        // Retrieve photo URI
        val retrieved = dbHelper.getStudentPhotoUri(999, regNo)
        assertEquals("Retrieved photo URI should match the updated value", photoPath, retrieved)
    }

    @Test
    fun testTeacherPhotoDatabasePersistence() {
        val teacherEmail = "test_teacher@example.com"
        val photoPath = "/data/user/0/com.example/files/profile_photos/teacher_test.jpg"

        val teacher = Teacher(888, "Prof. Test", teacherEmail, "pass123", "Computer Science", "9876543210")
        dbHelper.addTeacher(teacher)

        // Update photo URI
        val updated = dbHelper.updateTeacherPhotoUri(teacherEmail, photoPath)
        assertTrue("Updating teacher photo URI should succeed", updated)

        // Retrieve photo URI
        val retrieved = dbHelper.getTeacherPhotoUri(teacherEmail)
        assertEquals("Retrieved teacher photo URI should match the updated value", photoPath, retrieved)
    }

    @Test
    fun testSessionManagerProfilePhotoUri() {
        val testUri = "/storage/emulated/0/Android/data/test_avatar.jpg"
        sessionManager.setProfilePhotoUri(testUri)
        assertEquals(testUri, sessionManager.getProfilePhotoUri())

        // Test removal
        sessionManager.setProfilePhotoUri(null)
        assertNull(sessionManager.getProfilePhotoUri())
    }

    @Test
    fun testProcessAndSaveAvatarFlow() {
        // Create a bitmap and write it to a temp cache file
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val tempSource = File(context.cacheDir, "test_input_avatar.jpg")
        val out = FileOutputStream(tempSource)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        out.flush()
        out.close()

        val sourceUri = Uri.fromFile(tempSource)
        val savedPath = ProfilePhotoManager.processAndSaveAvatar(context, sourceUri, "student", "REG_12345")

        assertNotNull("Saved avatar path should not be null", savedPath)
        val savedFile = File(savedPath!!)
        assertTrue("Saved avatar file should exist", savedFile.exists())
        assertTrue("Saved avatar file should have positive byte length", savedFile.length() > 0)

        // Test delete
        val deleted = ProfilePhotoManager.deleteProfilePhoto(context, "student", "REG_12345")
        assertTrue("Avatar deletion should return true", deleted)
        assertFalse("Avatar file should no longer exist after deletion", savedFile.exists())
    }
}
