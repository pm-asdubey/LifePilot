package com.lifepilot.domain.repository

import com.lifepilot.domain.model.MetadataEntry
import com.lifepilot.domain.model.MetadataSource
import com.lifepilot.domain.model.VerificationStatus
import kotlinx.coroutines.flow.Flow

interface MetadataRepository {
    fun observeMetadataByObject(objectId: String): Flow<List<MetadataEntry>>
    suspend fun getMetadataByObject(objectId: String): List<MetadataEntry>
    suspend fun getMetadataByField(objectId: String, fieldId: String): MetadataEntry?
    suspend fun upsertMetadata(
        objectId: String,
        fieldId: String,
        value: String,
        source: MetadataSource,
        confidence: Float?,
        verificationStatus: VerificationStatus = VerificationStatus.UNVERIFIED,
    ): MetadataEntry
    suspend fun verifyMetadata(metadataId: String): MetadataEntry
    suspend fun rejectMetadata(metadataId: String): MetadataEntry
    suspend fun upsertMetadataBatch(entries: List<MetadataEntry>): List<MetadataEntry>
    suspend fun deleteMetadata(metadataId: String)
    suspend fun getMetadataForObjects(objectIds: List<String>): Map<String, List<MetadataEntry>>
}
