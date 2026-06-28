package com.lifepilot.data.ocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Canvas
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.lifepilot.domain.ocr.OcrResult
import com.lifepilot.domain.ocr.OcrService
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class MlKitOcrService @Inject constructor() : OcrService {

    private val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val devanagariRecognizer = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())

    override suspend fun extractText(filePath: String, mimeType: String): OcrResult {
        return when {
            mimeType.startsWith("image/") -> extractFromImage(filePath)
            mimeType == "application/pdf" -> OcrResult.NotSupported
            else -> OcrResult.NotSupported
        }
    }

    private suspend fun extractFromImage(filePath: String): OcrResult {
        return try {
            val file = File(filePath)
            if (!file.exists()) return OcrResult.Error("File not found: $filePath")

            val raw = BitmapFactory.decodeFile(filePath)
                ?: return OcrResult.Error("Could not decode image: $filePath")

            val bitmap = correctOrientationAndEnhance(raw, filePath)

            val latinText = runRecognizer(latinRecognizer, bitmap)
            val devanagariText = runRecognizer(devanagariRecognizer, bitmap)

            val combined = mergeOcrResults(latinText, devanagariText)
            Timber.d("OCR extracted ${combined.length} chars (Latin:${latinText.length} Devanagari:${devanagariText.length})")
            OcrResult.Success(text = combined, confidence = 1.0f)
        } catch (e: Exception) {
            Timber.e(e, "OCR exception for $filePath")
            OcrResult.Error(e.message ?: "Unknown error")
        }
    }

    private suspend fun runRecognizer(
        recognizer: com.google.mlkit.vision.text.TextRecognizer,
        bitmap: Bitmap,
    ): String = suspendCancellableCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { continuation.resume(it.text) }
            .addOnFailureListener { continuation.resume("") }
    }

    private fun mergeOcrResults(latin: String, devanagari: String): String {
        return when {
            latin.isBlank() && devanagari.isBlank() -> ""
            latin.isBlank() -> devanagari
            devanagari.isBlank() -> latin
            // Interleave results: devanagari text first (more specific match), then latin
            else -> "$latin\n$devanagari"
        }
    }

    private fun correctOrientationAndEnhance(original: Bitmap, filePath: String): Bitmap {
        val rotated = applyExifRotation(original, filePath)
        return enhanceContrast(rotated)
    }

    private fun applyExifRotation(bitmap: Bitmap, filePath: String): Bitmap {
        return try {
            val exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                else -> return bitmap
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            Timber.w(e, "EXIF rotation failed, using original")
            bitmap
        }
    }

    private fun enhanceContrast(source: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        // Boost contrast and brightness to make document text pop on camera scans
        val contrast = 1.4f
        val brightness = -30f
        val colorMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f,
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }
}
