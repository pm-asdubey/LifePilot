package com.lifepilot.data.storage

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FileStorageManagerTest {

    private lateinit var manager: FileStorageManager
    private lateinit var context: android.content.Context
    private lateinit var testDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        manager = FileStorageManager(context)
        testDir = File(context.filesDir, "documents/test_object_id")
        testDir.deleteRecursively()
    }

    @After
    fun tearDown() {
        File(context.filesDir, "documents/test_object_id").deleteRecursively()
    }

    @Test
    fun copyFromUri_storesTxtFile() {
        val tempFile = createTempFile("hello, lifepilot")
        val uri = Uri.fromFile(tempFile)

        val result = manager.copyFromUri(uri, "test_object_id", "test.txt")

        assertNotNull(result)
        assertTrue(result!!.sizeBytes > 0)
        assertTrue(File(result.absolutePath).exists())
        assertTrue(result.absolutePath.endsWith("test.txt"))
    }

    @Test
    fun copyFromUri_sanitizesFileName() {
        val tempFile = createTempFile("data")
        val uri = Uri.fromFile(tempFile)

        val result = manager.copyFromUri(uri, "test_object_id", "file with spaces & symbols!.txt")

        assertNotNull(result)
        assertFalse("File name should not contain spaces", result!!.absolutePath.contains(" "))
        assertFalse("File name should not contain &", result.absolutePath.contains("&"))
        assertFalse("File name should not contain !", result.absolutePath.contains("!"))
    }

    @Test
    fun copyFromUri_deduplicatesFileNames() {
        val tempFile1 = createTempFile("content1")
        val tempFile2 = createTempFile("content2")
        val uri1 = Uri.fromFile(tempFile1)
        val uri2 = Uri.fromFile(tempFile2)

        val result1 = manager.copyFromUri(uri1, "test_object_id", "document.pdf")
        val result2 = manager.copyFromUri(uri2, "test_object_id", "document.pdf")

        assertNotNull(result1)
        assertNotNull(result2)
        assertFalse("Duplicate files should have different paths", result1!!.absolutePath == result2!!.absolutePath)
        assertTrue("Second file should exist", File(result2.absolutePath).exists())
    }

    @Test
    fun copyFromUri_computesNonEmptyChecksum() {
        val tempFile = createTempFile("checksum test content")
        val uri = Uri.fromFile(tempFile)

        val result = manager.copyFromUri(uri, "test_object_id", "file.txt")

        assertNotNull(result)
        assertTrue("Checksum should not be empty", result!!.checksum.isNotEmpty())
        assertEquals("SHA-256 checksum should be 64 hex chars", 64, result.checksum.length)
    }

    @Test
    fun deleteFile_removesFile() {
        val tempFile = createTempFile("to be deleted")
        val result = manager.copyFromUri(Uri.fromFile(tempFile), "test_object_id", "delete_me.txt")
        assertNotNull(result)
        assertTrue(File(result!!.absolutePath).exists())

        val deleted = manager.deleteFile(result.absolutePath)

        assertTrue(deleted)
        assertFalse(File(result.absolutePath).exists())
    }

    @Test
    fun deleteObjectFiles_removesAllFilesForObject() {
        val tempFile1 = createTempFile("file1")
        val tempFile2 = createTempFile("file2")
        manager.copyFromUri(Uri.fromFile(tempFile1), "test_object_id", "doc1.txt")
        manager.copyFromUri(Uri.fromFile(tempFile2), "test_object_id", "doc2.txt")

        assertTrue("Object directory should exist", testDir.exists())

        manager.deleteObjectFiles("test_object_id")

        assertFalse("Object directory should be deleted", testDir.exists())
    }

    @Test
    fun copyFromUri_invalidUri_returnsNull() {
        val nonExistentUri = Uri.parse("file:///nonexistent/path/file.txt")
        val result = manager.copyFromUri(nonExistentUri, "test_object_id", "file.txt")
        assertNull(result)
    }

    private fun createTempFile(content: String): File {
        val file = File.createTempFile("lifepilot_test_", ".tmp", context.cacheDir)
        file.writeText(content)
        file.deleteOnExit()
        return file
    }
}
