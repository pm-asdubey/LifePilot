package com.lifepilot.data.ocr

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Regression guard for the OCR file-format sniffer.
 *
 * The camera/scan flow repeatedly broke because `ContentResolver.getType()` returned
 * `application/octet-stream` for scanner/FileProvider URIs (and the stored file had no extension),
 * so a scanned PDF was never routed to the PDF extractor and OCR silently produced nothing.
 * The sniffer detects the real format from magic bytes so OCR runs regardless of the declared mime.
 */
class OcrFileFormatSnifferTest {

    @Test
    fun `detects PDF from percent-PDF header`() {
        val header = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x37) // "%PDF-1.7"
        assertThat(sniffOcrFormat(header)).isEqualTo(OcrFileFormat.PDF)
    }

    @Test
    fun `detects JPEG from FFD8FF header`() {
        val header = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        assertThat(sniffOcrFormat(header)).isEqualTo(OcrFileFormat.IMAGE)
    }

    @Test
    fun `detects PNG from signature`() {
        val header = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        assertThat(sniffOcrFormat(header)).isEqualTo(OcrFileFormat.IMAGE)
    }

    @Test
    fun `detects WEBP from RIFF-WEBP`() {
        val header = byteArrayOf(
            0x52, 0x49, 0x46, 0x46, // RIFF
            0x00, 0x00, 0x00, 0x00, // size
            0x57, 0x45, 0x42, 0x50, // WEBP
        )
        assertThat(sniffOcrFormat(header)).isEqualTo(OcrFileFormat.IMAGE)
    }

    @Test
    fun `detects HEIC from ftyp box`() {
        val header = byteArrayOf(0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70) // ....ftyp
        assertThat(sniffOcrFormat(header)).isEqualTo(OcrFileFormat.IMAGE)
    }

    @Test
    fun `returns null for unknown or empty bytes`() {
        assertThat(sniffOcrFormat(ByteArray(0))).isNull()
        assertThat(sniffOcrFormat(byteArrayOf(0x00, 0x01, 0x02, 0x03))).isNull()
        // A short buffer must not crash the offset lookups.
        assertThat(sniffOcrFormat(byteArrayOf(0x25))).isNull()
    }
}
