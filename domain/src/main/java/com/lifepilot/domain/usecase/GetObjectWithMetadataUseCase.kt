package com.lifepilot.domain.usecase

import com.lifepilot.domain.model.LifeObject
import com.lifepilot.domain.model.MetadataEntry
import com.lifepilot.domain.repository.MetadataRepository
import com.lifepilot.domain.repository.ObjectRepository
import javax.inject.Inject

data class ObjectWithMetadata(
    val lifeObject: LifeObject,
    val metadata: List<MetadataEntry>,
)

class GetObjectWithMetadataUseCase @Inject constructor(
    private val objectRepository: ObjectRepository,
    private val metadataRepository: MetadataRepository,
) {
    suspend operator fun invoke(objectId: String): ObjectWithMetadata? {
        val obj = objectRepository.getObjectById(objectId) ?: return null
        val metadata = metadataRepository.getMetadataByObject(objectId)
        return ObjectWithMetadata(lifeObject = obj, metadata = metadata)
    }
}
