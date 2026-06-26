package com.lifepilot.domain.model

import java.time.Instant

data class Profile(
    val profileId: String,
    val displayName: String,
    val avatarPath: String?,
    val isPrimary: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
