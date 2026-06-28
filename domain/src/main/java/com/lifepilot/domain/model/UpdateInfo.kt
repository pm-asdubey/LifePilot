package com.lifepilot.domain.model

data class UpdateInfo(
    val latestVersion: String,
    val releaseUrl: String,
    val publishedAt: String,
    val releaseNotes: String,
)
