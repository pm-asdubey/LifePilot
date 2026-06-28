package com.lifepilot.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lifepilot.data.database.dao.DocumentDao
import com.lifepilot.data.repository.PreferenceManager
import com.lifepilot.domain.engine.LifeStateEngine
import com.lifepilot.domain.ocr.OcrResult
import com.lifepilot.domain.ocr.OcrService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class DocumentOcrWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val documentDao: DocumentDao,
    private val ocrService: OcrService,
    private val lifeStateEngine: LifeStateEngine,
    private val preferenceManager: PreferenceManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val versionId = inputData.getString(KEY_VERSION_ID)
            ?: return Result.failure()

        return try {
            val versionEntity = documentDao.getVersionById(versionId)
                ?: return Result.failure()

            if (versionEntity.ocrText != null) {
                Timber.d("OCR already done for version $versionId")
                return Result.success()
            }

            val ocrResult = ocrService.extractText(
                filePath = versionEntity.filePath,
                mimeType = versionEntity.mimeType,
            )

            when (ocrResult) {
                is OcrResult.Success -> {
                    documentDao.updateVersionOcrText(versionId, ocrResult.text)
                    Timber.d("OCR complete for version $versionId: ${ocrResult.text.length} chars")
                    val objectId = documentDao.getDocumentById(versionEntity.documentId)?.objectId
                    if (objectId != null) {
                        // Signal that AI metadata extraction is ready for user review
                        preferenceManager.setPendingVerification(objectId, versionId)
                        lifeStateEngine.processDocumentIngestion(
                            objectId = objectId,
                            documentId = versionEntity.documentId,
                            ocrText = ocrResult.text,
                        )
                    }
                    Result.success()
                }
                is OcrResult.Error -> {
                    Timber.w("OCR failed for version $versionId: ${ocrResult.message}")
                    if (runAttemptCount < 3) Result.retry() else Result.failure()
                }
                is OcrResult.NotSupported -> {
                    Timber.d("OCR not supported for mime type: ${versionEntity.mimeType}")
                    Result.success()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "OCR worker failed for version $versionId")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_VERSION_ID = "version_id"
        const val WORK_NAME_PREFIX = "document_ocr_"
    }
}
