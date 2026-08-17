package com.lifepilot.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProjectStatusTest {

    @Test
    fun `progress is completed over total`() {
        assertThat(ProjectHealth.progress(openTasks = 1, totalTasks = 4)).isWithin(0.001f).of(0.75f)
        assertThat(ProjectHealth.progress(openTasks = 0, totalTasks = 2)).isWithin(0.001f).of(1f)
    }

    @Test
    fun `progress is zero when there are no tasks`() {
        assertThat(ProjectHealth.progress(openTasks = 0, totalTasks = 0)).isEqualTo(0f)
    }

    @Test
    fun `ai-proposed projects are AI_PROPOSED regardless of dates`() {
        assertThat(ProjectHealth.of(isAiProposed = true, progress = 0.9f, daysUntilTarget = 100))
            .isEqualTo(ProjectHealth.AI_PROPOSED)
    }

    @Test
    fun `behind when target is near and under half done`() {
        assertThat(ProjectHealth.of(isAiProposed = false, progress = 0.3f, daysUntilTarget = 10))
            .isEqualTo(ProjectHealth.BEHIND)
    }

    @Test
    fun `on track when ahead or no near deadline`() {
        assertThat(ProjectHealth.of(isAiProposed = false, progress = 0.6f, daysUntilTarget = 5))
            .isEqualTo(ProjectHealth.ON_TRACK)
        assertThat(ProjectHealth.of(isAiProposed = false, progress = 0.1f, daysUntilTarget = null))
            .isEqualTo(ProjectHealth.ON_TRACK)
        assertThat(ProjectHealth.of(isAiProposed = false, progress = 0.1f, daysUntilTarget = 60))
            .isEqualTo(ProjectHealth.ON_TRACK)
    }
}
