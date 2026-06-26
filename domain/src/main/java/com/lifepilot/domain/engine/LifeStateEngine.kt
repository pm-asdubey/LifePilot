package com.lifepilot.domain.engine

import com.lifepilot.domain.model.Document
import com.lifepilot.domain.model.Event
import com.lifepilot.domain.model.LifeObject
import com.lifepilot.domain.model.MetadataEntry
import com.lifepilot.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface LifeStateEngine {
    suspend fun processDocumentIngestion(
        objectId: String,
        documentId: String,
        ocrText: String?,
    )

    suspend fun processMetadataUpdate(
        objectId: String,
        updatedFields: List<MetadataEntry>,
    )

    suspend fun processObjectEvent(
        objectId: String,
        eventType: String,
        payload: String,
    )

    suspend fun evaluateRules(objectId: String)

    fun observeAttentionRequired(profileId: String): Flow<List<AttentionItem>>
}

data class AttentionItem(
    val itemId: String,
    val title: String,
    val description: String,
    val priority: AttentionPriority,
    val objectId: String?,
    val actionType: String,
)

enum class AttentionPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}
