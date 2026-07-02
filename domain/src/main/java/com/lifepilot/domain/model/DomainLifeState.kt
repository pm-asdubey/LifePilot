package com.lifepilot.domain.model

import java.time.Instant

/**
 * The continuously-maintained understanding for a single life domain (e.g. "Career", "Identity").
 *
 * Objects own facts. Domains own understanding.
 *
 * Updated by DomainLifeStateEngine after conversations, OCR pipelines, and action approvals.
 * Retrieved as the highest-priority context in every AI prompt.
 */
data class DomainLifeState(
    val profileId: String,
    val domain: String,
    val currentSituation: String = "",
    val currentPriorities: List<String> = emptyList(),
    val knownRisks: List<String> = emptyList(),
    val openQuestions: List<String> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val recentChanges: List<String> = emptyList(),
    val lastUpdated: Instant = Instant.EPOCH,
    val version: Int = 0,
)
