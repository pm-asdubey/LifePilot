package com.lifepilot.data.ocr

import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

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
            if (!file.exists()) {
                return OcrResult.Error("File not found: $filePath")
            }

            val bitmap = BitmapFactory.decodeFile(filePath)
                ?: return OcrResult.Error("Could not decode image: $filePath")

            val image = InputImage.fromBitmap(bitmap, 0)

            suspendCancellableCoroutine { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val text = visionText.text
                        Timber.d("OCR extracted ${text.length} characters from $filePath")
                        continuation.resume(
                            OcrResult.Success(text = text, confidence = 1.0f)
                        )
                    }
                    .addOnFailureListener { exception ->
                        Timber.e(exception, "OCR failed for $filePath")
                        continuation.resume(
                            OcrResult.Error(exception.message ?: "OCR failed")
                        )
                    }

                continuation.invokeOnCancellation {
                    recognizer.close()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "OCR exception for $filePath")
            OcrResult.Error(e.message ?: "Unknown error")
        }
    }
}
