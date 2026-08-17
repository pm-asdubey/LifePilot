package com.lifepilot.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ActionPlanNormalizerTest {

    private fun task(id: String, projectItemId: String? = null) = ActionItem.CreateTask(
        itemId = id,
        summary = "task $id",
        title = "Task $id",
        projectItemId = projectItemId,
    )

    private fun plan(type: ActionPlanType = ActionPlanType.CUSTOM, summary: String = "Action plan", items: List<ActionItem>) =
        ActionPlan(planId = "p", type = type, summary = summary, items = items)

    @Test
    fun `injects a project when 2+ tasks and none present`() {
        val result = ActionPlanNormalizer.ensureProject(plan(items = listOf(task("1"), task("2"))))

        val projects = result.items.filterIsInstance<ActionItem.CreateProject>()
        assertThat(projects).hasSize(1)
        // Project executes first.
        assertThat(result.items.first()).isInstanceOf(ActionItem.CreateProject::class.java)
        // Every task is linked to the synthesized project.
        assertThat(result.items.filterIsInstance<ActionItem.CreateTask>().all {
            it.projectItemId == ActionPlanNormalizer.AUTO_PROJECT_ITEM_ID
        }).isTrue()
    }

    @Test
    fun `does nothing when only one task`() {
        val result = ActionPlanNormalizer.ensureProject(plan(items = listOf(task("1"))))
        assertThat(result.items.filterIsInstance<ActionItem.CreateProject>()).isEmpty()
    }

    @Test
    fun `does nothing when a project already exists`() {
        val existing = ActionItem.CreateProject(itemId = "0", summary = "s", title = "Mine")
        val result = ActionPlanNormalizer.ensureProject(
            plan(items = listOf(existing, task("1", "0"), task("2", "0"))),
        )
        assertThat(result.items.filterIsInstance<ActionItem.CreateProject>()).hasSize(1)
        assertThat(result.items.filterIsInstance<ActionItem.CreateProject>().first().title).isEqualTo("Mine")
    }

    @Test
    fun `titles a known plan type and derives its domain`() {
        val result = ActionPlanNormalizer.ensureProject(
            plan(type = ActionPlanType.JOB_CHANGE, items = listOf(task("1"), task("2"))),
        )
        val project = result.items.filterIsInstance<ActionItem.CreateProject>().first()
        assertThat(project.title).isEqualTo("Job Transition")
        assertThat(project.domain).isEqualTo("Career")
    }

    @Test
    fun `custom plan uses its summary as the project title`() {
        val result = ActionPlanNormalizer.ensureProject(
            plan(summary = "Tax filing 2025-26.", items = listOf(task("1"), task("2"))),
        )
        val project = result.items.filterIsInstance<ActionItem.CreateProject>().first()
        assertThat(project.title).isEqualTo("Tax filing 2025-26")
    }

    @Test
    fun `preserves an explicit projectItemId already set on a task`() {
        val result = ActionPlanNormalizer.ensureProject(
            plan(items = listOf(task("1", projectItemId = "keep"), task("2"))),
        )
        val tasks = result.items.filterIsInstance<ActionItem.CreateTask>()
        assertThat(tasks.first { it.itemId == "1" }.projectItemId).isEqualTo("keep")
        assertThat(tasks.first { it.itemId == "2" }.projectItemId).isEqualTo(ActionPlanNormalizer.AUTO_PROJECT_ITEM_ID)
    }
}
