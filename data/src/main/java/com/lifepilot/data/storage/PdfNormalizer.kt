package com.lifepilot.data.storage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Normalises stored documents to PDF so every record holds a single, downloadable document format
 * regardless of how it was captured (camera photo, gallery image, scanned PDF, or picked file).
 *
 * Raster images are wrapped into a single-page PDF at their native resolution. Files that are
 * already PDFs (or can't be decoded as an image) are left untouched.
 */
@Singleton
class PdfNormalizer @Inject constructor() {

    /**
     * Returns a new PDF [File] for a raster-image [source], or `null` when no conversion happened
     * (already a PDF, or not a decodable image) — in which case the caller keeps the original file.
     */
    fun toPdf(source: File, mimeType: String): File? {
        if (mimeType == "application/pdf" || looksLikePdf(source)) return null
        val bitmap = runCatching { BitmapFactory.decodeFile(source.absolutePath) }.getOrNull() ?: return null
        val enhanced = enhanceForDocument(bitmap)
        return try {
            val doc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(enhanced.width, enhanced.height, 1).create()
            val page = doc.startPage(pageInfo)
            page.canvas.drawBitmap(enhanced, 0f, 0f, null)
            doc.finishPage(page)

            val out = File(source.parentFile, source.nameWithoutExtension + ".pdf")
            FileOutputStream(out).use { doc.writeTo(it) }
            doc.close()
            if (enhanced != bitmap) enhanced.recycle()
            bitmap.recycle()
            Timber.d("Normalised ${source.name} -> ${out.name} (${out.length()} B)")
            out
        } catch (e: Exception) {
            Timber.e(e, "PDF normalisation failed for ${source.name}")
            null
        }
    }

    /**
     * Applies a light document-style enhancement (contrast boost + slight brightness lift) so
     * camera photos and gallery images stored via this path read like a scan. Returns the original
     * bitmap unchanged if the enhancement can't be allocated.
     */
    private fun enhanceForDocument(src: Bitmap): Bitmap = try {
        val contrast = 1.3f
        val brightness = 8f
        val t = (-0.5f * contrast + 0.5f) * 255f + brightness
        val matrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, t,
                0f, contrast, 0f, 0f, t,
                0f, 0f, contrast, 0f, t,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) }
        canvas.drawBitmap(src, 0f, 0f, paint)
        out
    } catch (e: Throwable) {
        Timber.w(e, "Document enhancement failed; using original bitmap")
        src
    }

    private fun looksLikePdf(file: File): Boolean = try {
        file.inputStream().use { input ->
            val b = ByteArray(4)
            input.read(b) == 4 &&
                b[0] == 0x25.toByte() && b[1] == 0x50.toByte() &&
                b[2] == 0x44.toByte() && b[3] == 0x46.toByte()
        }
    } catch (e: Exception) {
        false
    }
}
