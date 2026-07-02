package com.lifepilot.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * Persists the running AI understanding for each life domain.
 *
 * List fields are stored as newline-delimited strings. Items containing newlines
 * have their newlines normalised to spaces on write. This avoids a JSON dependency
 * while keeping the format human-readable in DB inspection tools.
 *
 * Composite primary key: (profile_id, domain) — one state document per domain per profile.
 */
@Entity(
    tableName = "domain_life_states",
    primaryKeys = ["profile_id", "domain"],
)
data class DomainLifeStateEntity(
    @ColumnInfo(name = "profile_id") val profileId: String,
    @ColumnInfo(name = "domain") val domain: String,
    @ColumnInfo(name = "current_situation") val currentSituation: String = "",
    @ColumnInfo(name = "current_priorities") val currentPriorities: String = "",
    @ColumnInfo(name = "known_risks") val knownRisks: String = "",
    @ColumnInfo(name = "open_questions") val openQuestions: String = "",
    @ColumnInfo(name = "recommendations") val recommendations: String = "",
    @ColumnInfo(name = "recent_changes") val recentChanges: String = "",
    @ColumnInfo(name = "last_updated") val lastUpdated: Long = 0L,
    @ColumnInfo(name = "version") val version: Int = 0,
)

internal fun String.toStringList(): List<String> =
    split("\n").map { it.trim() }.filter { it.isNotEmpty() }

internal fun List<String>.toStoredString(): String =
    joinToString("\n") { it.replace("\n", " ") }
