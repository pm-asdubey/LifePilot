package com.lifepilot.data.storage

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileStorageManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val documentsDir: File
        get() = File(context.filesDir, "documents").also { it.mkdirs() }

    fun copyFromUri(uri: Uri, objectId: String, originalName: String): StoredFile? {
        return try {
            val objectDir = File(documentsDir, objectId).also { it.mkdirs() }
            val sanitizedName = originalName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val destFile = uniqueFile(objectDir, sanitizedName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val checksum = computeSha256(destFile)
            val sizeBytes = destFile.length()

            Timber.d("Stored file: ${destFile.absolutePath} (${sizeBytes}B)")

            StoredFile(
                absolutePath = destFile.absolutePath,
                sizeBytes = sizeBytes,
                checksum = checksum,
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to copy file from URI $uri")
            null
        }
    }

    fun deleteFile(absolutePath: String): Boolean {
        return try {
            File(absolutePath).delete()
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete file $absolutePath")
            false
        }
    }

    fun deleteObjectFiles(objectId: String) {
        try {
            File(documentsDir, objectId).deleteRecursively()
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete object files for $objectId")
        }
    }

    private fun uniqueFile(dir: File, name: String): File {
        var file = File(dir, name)
        var counter = 1
        while (file.exists()) {
            val dotIndex = name.lastIndexOf('.')
            val newName = if (dotIndex >= 0) {
                "${name.substring(0, dotIndex)}_$counter${name.substring(dotIndex)}"
            } else {
                "${name}_$counter"
            }
            file = File(dir, newName)
            counter++
        }
        return file
    }

    private fun computeSha256(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to compute checksum for ${file.name}")
            ""
        }
    }
}

data class StoredFile(
    val absolutePath: String,
    val sizeBytes: Long,
    val checksum: String,
)
