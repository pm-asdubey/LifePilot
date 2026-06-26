package com.lifepilot.data.mapper

import com.lifepilot.data.database.entity.ProfileEntity
import com.lifepilot.domain.model.Profile
import java.time.Instant

fun ProfileEntity.toDomain(): Profile = Profile(
    profileId = profileId,
    displayName = displayName,
    avatarPath = avatarPath,
    isPrimary = isPrimary,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

fun Profile.toEntity(): ProfileEntity = ProfileEntity(
    profileId = profileId,
    displayName = displayName,
    avatarPath = avatarPath,
    isPrimary = isPrimary,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
)
