package com.lifepilot.domain.model

/**
 * Deterministic guard that keeps Projects central to the app.
 *
 * The AI is instructed to wrap multi-task plans in a Project, but models don't always comply. This
 * normalizer is the safety net: if a plan would create 2+ tasks and has no [ActionItem.CreateProject],
 * it synthesizes one and links every otherwise-unlinked task/record to it — so grouped work ALWAYS
 * becomes a Project, regardless of the model. Applied at parse time so the injected project is visible
 * in the proposal the user approves.
 */
object ActionPlanNormalizer {

    const val AUTO_PROJECT_ITEM_ID = "auto-project"

    fun ensureProject(plan: ActionPlan): ActionPlan {
        val taskCount = plan.items.count { it is ActionItem.CreateTask }
        val hasProject = plan.items.any { it is ActionItem.CreateProject }
        if (taskCount < 2 || hasProject) return plan

        val domain = deriveDomain(plan)
        val project = ActionItem.CreateProject(
            itemId = AUTO_PROJECT_ITEM_ID,
            summary = "Group these tasks into a project",
            title = deriveTitle(plan),
            description = plan.summary.takeIf { it.isNotBlank() && !it.equals("Action plan", ignoreCase = true) },
            emoji = DomainEmoji.forDomain(domain),
            domain = domain,
        )

        val relinked = plan.items.map { item ->
            when (item) {
                is ActionItem.CreateTask ->
                    if (item.projectItemId == null) item.copy(projectItemId = AUTO_PROJECT_ITEM_ID) else item
                is ActionItem.CreateRecord ->
                    if (item.projectItemId == null) item.copy(projectItemId = AUTO_PROJECT_ITEM_ID) else item
                else -> item
            }
        }
        // Project must execute before the tasks/records that reference it.
        return plan.copy(items = listOf(project) + relinked)
    }

    private fun deriveTitle(plan: ActionPlan): String {
        titleForType(plan.type)?.let { return it }
        val summary = plan.summary.trim().removeSuffix(".")
        if (summary.isNotEmpty() && !summary.equals("Action plan", ignoreCase = true)) {
            return summary.take(60)
        }
        return "New project"
    }

    private fun deriveDomain(plan: ActionPlan): String {
        // Prefer the most common domain among records the plan creates.
        plan.items.filterIsInstance<ActionItem.CreateRecord>()
            .groupingBy { it.domain }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }
        return domainForType(plan.type) ?: "General"
    }

    private fun titleForType(type: ActionPlanType): String? = when (type) {
        ActionPlanType.JOB_CHANGE -> "Job Transition"
        ActionPlanType.RELOCATION -> "Relocation"
        ActionPlanType.MARRIAGE -> "Marriage Planning"
        ActionPlanType.DIVORCE -> "Divorce Process"
        ActionPlanType.CHILDBIRTH -> "New Baby"
        ActionPlanType.DEATH_OF_RELATIVE -> "Estate & Affairs"
        ActionPlanType.NEW_PROPERTY -> "New Property"
        ActionPlanType.MAJOR_MEDICAL -> "Medical Care Plan"
        ActionPlanType.NEW_FINANCIAL_PRODUCT -> "New Financial Product"
        ActionPlanType.VISA_IMMIGRATION -> "Visa & Immigration"
        ActionPlanType.MAJOR_PURCHASE -> "Major Purchase"
        ActionPlanType.CUSTOM -> null
    }

    private fun domainForType(type: ActionPlanType): String? = when (type) {
        ActionPlanType.JOB_CHANGE -> "Career"
        ActionPlanType.RELOCATION -> "Housing"
        ActionPlanType.MARRIAGE, ActionPlanType.DIVORCE -> "Relationships"
        ActionPlanType.CHILDBIRTH -> "Family"
        ActionPlanType.DEATH_OF_RELATIVE -> "Family"
        ActionPlanType.NEW_PROPERTY -> "Housing"
        ActionPlanType.MAJOR_MEDICAL -> "Health"
        ActionPlanType.NEW_FINANCIAL_PRODUCT -> "Finance"
        ActionPlanType.VISA_IMMIGRATION -> "Travel"
        ActionPlanType.MAJOR_PURCHASE -> "Finance"
        ActionPlanType.CUSTOM -> null
    }
}
