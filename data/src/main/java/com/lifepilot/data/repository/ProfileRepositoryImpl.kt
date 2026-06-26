package com.lifepilot.data.repository

import com.lifepilot.data.database.dao.ProfileDao
import com.lifepilot.data.mapper.toDomain
import com.lifepilot.data.mapper.toEntity
import com.lifepilot.domain.model.Profile
import com.lifepilot.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val profileDao: ProfileDao,
) : ProfileRepository {

    override fun observeProfiles(): Flow<List<Profile>> =
        profileDao.observeProfiles().map { entities -> entities.map { it.toDomain() } }

    override fun observeActiveProfile(): Flow<Profile?> =
        profileDao.observePrimaryProfile().map { it?.toDomain() }

    override suspend fun getProfileById(profileId: String): Profile? =
        profileDao.getProfileById(profileId)?.toDomain()

    override suspend fun createProfile(displayName: String, isPrimary: Boolean): Profile {
        val now = Instant.now()
        val profile = Profile(
            profileId = UUID.randomUUID().toString(),
            displayName = displayName,
            avatarPath = null,
            isPrimary = isPrimary,
            createdAt = now,
            updatedAt = now,
        )
        if (isPrimary) {
            profileDao.clearAllPrimary()
        }
        profileDao.insertProfile(profile.toEntity())
        return profile
    }

    override suspend fun updateProfile(profile: Profile): Profile {
        val updated = profile.copy(updatedAt = Instant.now())
        profileDao.updateProfile(updated.toEntity())
        return updated
    }

    override suspend fun deleteProfile(profileId: String) {
        profileDao.deleteProfile(profileId)
    }

    override suspend fun setActiveProfile(profileId: String) {
        profileDao.clearAllPrimary()
        profileDao.setPrimary(profileId)
    }
}
