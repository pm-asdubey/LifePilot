package com.lifepilot.data.repository

import com.lifepilot.data.database.dao.DomainLifeStateDao
import com.lifepilot.data.database.entity.DomainLifeStateEntity
import com.lifepilot.data.database.entity.toStoredString
import com.lifepilot.data.database.entity.toStringList
import com.lifepilot.domain.model.DomainLifeState
import com.lifepilot.domain.repository.DomainRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class DomainRepositoryImpl @Inject constructor(
    private val dao: DomainLifeStateDao,
) : DomainRepository {

    override fun observeAllDomainLifeStates(profileId: String): Flow<List<DomainLifeState>> =
        dao.observeAllByProfile(profileId).map { entities -> entities.map { it.toDomain() } }

    override fun observeDomainLifeState(profileId: String, domain: String): Flow<DomainLifeState?> =
        dao.observeByDomain(profileId, domain).map { it?.toDomain() }

    override suspend fun getDomainLifeState(profileId: String, domain: String): DomainLifeState? =
        dao.getByDomain(profileId, domain)?.toDomain()

    override suspend fun upsertDomainLifeState(state: DomainLifeState) {
        dao.upsert(state.toEntity())
    }

    private fun DomainLifeStateEntity.toDomain(): DomainLifeState = DomainLifeState(
        profileId = profileId,
        domain = domain,
        currentSituation = currentSituation,
        currentPriorities = currentPriorities.toStringList(),
        knownRisks = knownRisks.toStringList(),
        openQuestions = openQuestions.toStringList(),
        recommendations = recommendations.toStringList(),
        recentChanges = recentChanges.toStringList(),
        lastUpdated = Instant.ofEpochMilli(lastUpdated),
        version = version,
    )

    private fun DomainLifeState.toEntity(): DomainLifeStateEntity = DomainLifeStateEntity(
        profileId = profileId,
        domain = domain,
        currentSituation = currentSituation,
        currentPriorities = currentPriorities.toStoredString(),
        knownRisks = knownRisks.toStoredString(),
        openQuestions = openQuestions.toStoredString(),
        recommendations = recommendations.toStoredString(),
        recentChanges = recentChanges.toStoredString(),
        lastUpdated = lastUpdated.toEpochMilli(),
        version = version,
    )
}
