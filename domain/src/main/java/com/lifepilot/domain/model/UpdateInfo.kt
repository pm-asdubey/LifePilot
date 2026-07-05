package com.lifepilot.domain.model

data class UpdateInfo(
    val latestVersion: String,
    val releaseUrl: String,
    val publishedAt: String,
    val releaseNotes: String,
    /** Direct browser_download_url for the .apk asset, if present in the GitHub Release. */
    val apkDownloadUrl: String? = null,
)
