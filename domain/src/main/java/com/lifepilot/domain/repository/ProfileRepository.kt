package com.lifepilot.domain.repository

import com.lifepilot.domain.model.Profile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfiles(): Flow<List<Profile>>
    fun observeActiveProfile(): Flow<Profile?>
    suspend fun getProfileById(profileId: String): Profile?
    suspend fun createProfile(displayName: String, isPrimary: Boolean): Profile
    suspend fun updateProfile(profile: Profile): Profile
    suspend fun deleteProfile(profileId: String)
    suspend fun setActiveProfile(profileId: String)
}
