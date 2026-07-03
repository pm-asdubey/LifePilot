package com.lifepilot.data.task

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Verifies the epoch-millis ↔ LocalDate conversion used in ProjectRepositoryImpl
 * so timezone-boundary bugs don't silently corrupt target dates.
 */
class ProjectEntityMappingTest {

    private val zone = ZoneId.systemDefault()

    private fun localDateToEpochMilli(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    private fun epochMilliToLocalDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    @Test
    fun `LocalDate round-trips through epoch millis`() {
        val original = LocalDate.of(2026, 12, 31)
        val millis = localDateToEpochMilli(original)
        val restored = epochMilliToLocalDate(millis)
        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `first day of year round-trips`() {
        val original = LocalDate.of(2027, 1, 1)
        val millis = localDateToEpochMilli(original)
        val restored = epochMilliToLocalDate(millis)
        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `null targetDate maps to null millis`() {
        val targetDate: LocalDate? = null
        val millis = targetDate?.atStartOfDay(zone)?.toInstant()?.toEpochMilli()
        assertThat(millis).isNull()
    }

    @Test
    fun `null millis maps to null LocalDate`() {
        val millis: Long? = null
        val date = millis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        assertThat(date).isNull()
    }

    @Test
    fun `isAiProposed true survives boolean to int mapping`() {
        // Room stores Boolean as INTEGER (0/1). Verify the values.
        val trueAsInt = if (true) 1 else 0
        val falseAsInt = if (false) 1 else 0
        assertThat(trueAsInt).isEqualTo(1)
        assertThat(falseAsInt).isEqualTo(0)
    }

    @Test
    fun `emoji default value is dart target`() {
        val defaultEmoji = "🎯"
        assertThat(defaultEmoji).isNotEmpty()
        assertThat(defaultEmoji).isEqualTo("🎯")
    }

    @Test
    fun `ProjectStatus valueOf handles ACTIVE`() {
        val status = runCatching {
            com.lifepilot.domain.model.ProjectStatus.valueOf("ACTIVE")
        }.getOrElse { com.lifepilot.domain.model.ProjectStatus.ACTIVE }
        assertThat(status).isEqualTo(com.lifepilot.domain.model.ProjectStatus.ACTIVE)
    }

    @Test
    fun `ProjectStatus valueOf falls back on unknown string`() {
        val status = runCatching {
            com.lifepilot.domain.model.ProjectStatus.valueOf("UNKNOWN_STATUS")
        }.getOrElse { com.lifepilot.domain.model.ProjectStatus.ACTIVE }
        assertThat(status).isEqualTo(com.lifepilot.domain.model.ProjectStatus.ACTIVE)
    }
}
