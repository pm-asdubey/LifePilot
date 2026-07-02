package com.lifepilot.domain.repository

import com.lifepilot.domain.model.DomainLifeState
import kotlinx.coroutines.flow.Flow

interface DomainRepository {
    fun observeAllDomainLifeStates(profileId: String): Flow<List<DomainLifeState>>
    fun observeDomainLifeState(profileId: String, domain: String): Flow<DomainLifeState?>
    suspend fun getDomainLifeState(profileId: String, domain: String): DomainLifeState?
    suspend fun upsertDomainLifeState(state: DomainLifeState)
}
