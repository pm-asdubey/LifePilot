package com.lifepilot.domain.repository

import com.lifepilot.domain.model.LifeObject
import com.lifepilot.domain.model.ObjectStatus
import kotlinx.coroutines.flow.Flow

interface ObjectRepository {
    fun observeObjectsByProfile(profileId: String): Flow<List<LifeObject>>
    fun observeObjectsByDomain(profileId: String, domain: String): Flow<List<LifeObject>>
    fun observeObjectById(objectId: String): Flow<LifeObject?>
    suspend fun getObjectById(objectId: String): LifeObject?
    suspend fun getObjectsByIds(objectIds: List<String>): List<LifeObject>
    suspend fun createObject(
        profileId: String,
        objectType: String,
        domain: String,
        title: String,
        description: String?,
    ): LifeObject
    suspend fun updateObject(lifeObject: LifeObject): LifeObject
    suspend fun updateObjectStatus(objectId: String, status: ObjectStatus)
    suspend fun archiveObject(objectId: String)
    suspend fun deleteObject(objectId: String)
    fun observeObjectCountByDomain(profileId: String): Flow<Map<String, Int>>
}
