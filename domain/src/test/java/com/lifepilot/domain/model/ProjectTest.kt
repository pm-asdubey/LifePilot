package com.lifepilot.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ProjectTest {

    private fun makeProject(
        status: ProjectStatus = ProjectStatus.ACTIVE,
        emoji: String = "🎯",
        targetDate: LocalDate? = null,
        isAiProposed: Boolean = false,
    ) = Project(
        projectId = "proj-1",
        profileId = "profile-1",
        title = "Test Project",
        description = null,
        domain = "Finance",
        status = status,
        emoji = emoji,
        targetDate = targetDate,
        isAiProposed = isAiProposed,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    @Test
    fun `default emoji is target dart`() {
        val project = makeProject()
        assertThat(project.emoji).isEqualTo("🎯")
    }

    @Test
    fun `default targetDate is null`() {
        val project = makeProject()
        assertThat(project.targetDate).isNull()
    }

    @Test
    fun `default isAiProposed is false`() {
        val project = makeProject()
        assertThat(project.isAiProposed).isFalse()
    }

    @Test
    fun `ai proposed flag is preserved`() {
        val project = makeProject(isAiProposed = true)
        assertThat(project.isAiProposed).isTrue()
    }

    @Test
    fun `custom emoji is preserved`() {
        val project = makeProject(emoji = "✈️")
        assertThat(project.emoji).isEqualTo("✈️")
    }

    @Test
    fun `targetDate is preserved`() {
        val date = LocalDate.of(2026, 12, 31)
        val project = makeProject(targetDate = date)
        assertThat(project.targetDate).isEqualTo(date)
    }

    @Test
    fun `ACTIVE status is default`() {
        val project = makeProject(status = ProjectStatus.ACTIVE)
        assertThat(project.status).isEqualTo(ProjectStatus.ACTIVE)
    }

    @Test
    fun `COMPLETED status is preserved`() {
        val project = makeProject(status = ProjectStatus.COMPLETED)
        assertThat(project.status).isEqualTo(ProjectStatus.COMPLETED)
    }

    @Test
    fun `ARCHIVED status is preserved`() {
        val project = makeProject(status = ProjectStatus.ARCHIVED)
        assertThat(project.status).isEqualTo(ProjectStatus.ARCHIVED)
    }

    @Test
    fun `copy preserves all fields`() {
        val original = makeProject(
            emoji = "🏠",
            targetDate = LocalDate.of(2027, 6, 1),
            isAiProposed = true,
            status = ProjectStatus.COMPLETED,
        )
        val copy = original.copy(title = "Renamed")
        assertThat(copy.emoji).isEqualTo(original.emoji)
        assertThat(copy.targetDate).isEqualTo(original.targetDate)
        assertThat(copy.isAiProposed).isEqualTo(original.isAiProposed)
        assertThat(copy.status).isEqualTo(original.status)
        assertThat(copy.title).isEqualTo("Renamed")
    }

    @Test
    fun `ProjectStatus enum has three values`() {
        val values = ProjectStatus.entries
        assertThat(values).containsExactly(
            ProjectStatus.ACTIVE,
            ProjectStatus.COMPLETED,
            ProjectStatus.ARCHIVED,
        )
    }
}
