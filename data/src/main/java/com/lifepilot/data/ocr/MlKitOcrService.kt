package com.lifepilot.data.ocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
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

/** File formats the OCR pipeline can handle, detected from magic bytes independent of mime type. */
internal enum class OcrFileFormat { PDF, IMAGE }

/**
 * Detect a document/image format from a file's leading bytes. Authoritative when the declared mime
 * type is missing or generic (e.g. `application/octet-stream` from a FileProvider / scanner URI).
 * Returns null when the bytes match no known format.
 */
internal fun sniffOcrFormat(header: ByteArray): OcrFileFormat? {
    fun at(i: Int): Int = if (i < header.size) header[i].toInt() and 0xFF else -1
    return when {
        // "%PDF"
        at(0) == 0x25 && at(1) == 0x50 && at(2) == 0x44 && at(3) == 0x46 -> OcrFileFormat.PDF
        // JPEG  FF D8 FF
        at(0) == 0xFF && at(1) == 0xD8 && at(2) == 0xFF -> OcrFileFormat.IMAGE
        // PNG   89 50 4E 47
        at(0) == 0x89 && at(1) == 0x50 && at(2) == 0x4E && at(3) == 0x47 -> OcrFileFormat.IMAGE
        // GIF   47 49 46 38  ("GIF8")
        at(0) == 0x47 && at(1) == 0x49 && at(2) == 0x46 && at(3) == 0x38 -> OcrFileFormat.IMAGE
        // BMP   42 4D        ("BM")
        at(0) == 0x42 && at(1) == 0x4D -> OcrFileFormat.IMAGE
        // WEBP  "RIFF"...."WEBP"
        at(0) == 0x52 && at(1) == 0x49 && at(2) == 0x46 && at(3) == 0x46 &&
            at(8) == 0x57 && at(9) == 0x45 && at(10) == 0x42 && at(11) == 0x50 -> OcrFileFormat.IMAGE
        // HEIC/HEIF  ...."ftyp" at offset 4
        at(4) == 0x66 && at(5) == 0x74 && at(6) == 0x79 && at(7) == 0x70 -> OcrFileFormat.IMAGE
        else -> null
    }
}

@Singleton
class MlKitOcrService @Inject constructor() : OcrService {

    private val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val devanagariRecognizer = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())

    override suspend fun extractText(filePath: String, mimeType: String): OcrResult {
        // ContentResolver.getType() is unreliable for FileProvider and document-scanner URIs — it
        // frequently returns "application/octet-stream" and the stored file often has no extension.
        // That silently skipped OCR (a scanned PDF landed in the octet-stream branch, failed an
        // image decode, and returned NotSupported → no AI answer). Sniff the file's magic bytes and
        // trust that over the declared mime type. This is the durable fix for the camera/scan flow.
        val sniffed = sniffOcrFormat(readHeader(filePath))
        return when {
            sniffed == OcrFileFormat.PDF || mimeType == "application/pdf" -> extractFromPdf(filePath)
            sniffed == OcrFileFormat.IMAGE || mimeType.startsWith("image/") -> extractFromImage(filePath)
            else -> {
                // Last resort: attempt a direct image decode.
                if (BitmapFactory.decodeFile(filePath) != null) {
                    extractFromImage(filePath)
                } else {
                    Timber.w("OCR not supported: mime=$mimeType sniffed=$sniffed path=$filePath")
                    OcrResult.NotSupported
                }
            }
        }
    }

    /** Reads up to the first 16 bytes of a file for magic-byte format detection. */
    private fun readHeader(filePath: String): ByteArray = try {
        File(filePath).inputStream().use { input ->
            val buf = ByteArray(16)
            val n = input.read(buf)
            if (n <= 0) ByteArray(0) else buf.copyOf(n)
        }
    } catch (e: Exception) {
        Timber.w(e, "readHeader failed for $filePath")
        ByteArray(0)
    }

    private suspend fun extractFromPdf(filePath: String): OcrResult {
        return try {
            val file = File(filePath)
            if (!file.exists()) return OcrResult.Error("File not found: $filePath")

            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val sb = StringBuilder()
            val scale = 2 // 2x resolution for better OCR quality

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val bmp = Bitmap.createBitmap(
                    page.width * scale,
                    page.height * scale,
                    Bitmap.Config.ARGB_8888,
                )
                bmp.eraseColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val latinText = runRecognizer(latinRecognizer, bmp)
                val devanagariText = runRecognizer(devanagariRecognizer, bmp)
                val pageText = mergeOcrResults(latinText, devanagariText)
                if (pageText.isNotBlank()) sb.append(pageText).append("\n\n")
                bmp.recycle()
                Timber.d("PDF OCR page ${i + 1}/${renderer.pageCount}: ${pageText.length} chars")
            }

            renderer.close()
            pfd.close()

            val text = sb.toString().trim()
            if (text.isBlank()) OcrResult.Error("No text found in PDF")
            else OcrResult.Success(text = text, confidence = 0.9f)
        } catch (e: Exception) {
            Timber.e(e, "PDF OCR failed for $filePath")
            OcrResult.Error(e.message ?: "PDF OCR error")
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
