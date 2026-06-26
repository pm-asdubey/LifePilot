package com.lifepilot.domain.ocr

sealed class OcrResult {
    data class Success(val text: String, val confidence: Float) : OcrResult()
    data class Error(val message: String) : OcrResult()
    data object NotSupported : OcrResult()
}

interface OcrService {
    suspend fun extractText(filePath: String, mimeType: String): OcrResult
}
