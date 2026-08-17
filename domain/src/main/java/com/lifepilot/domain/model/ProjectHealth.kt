package com.lifepilot.domain.model

/**
 * Runtime health of a Project, derived from its task completion and target date.
 * Extracted from the Planner UI so the rule is testable and not embedded in a Composable.
 */
enum class ProjectHealth {
    AI_PROPOSED, BEHIND, ON_TRACK;

    companion object {
        /** Fraction (0f–1f) of the project's tasks that are complete. */
        fun progress(openTasks: Int, totalTasks: Int): Float =
            if (totalTasks > 0) (totalTasks - openTasks).toFloat() / totalTasks else 0f

        /**
         * @param isAiProposed    the project was proposed by the AI and not yet adopted
         * @param progress        completion fraction from [progress]
         * @param daysUntilTarget days until the target date, or null if no target set
         */
        fun of(isAiProposed: Boolean, progress: Float, daysUntilTarget: Long?): ProjectHealth = when {
            isAiProposed -> AI_PROPOSED
            daysUntilTarget != null && daysUntilTarget <= 14 && progress < 0.5f -> BEHIND
            else -> ON_TRACK
        }
    }
}
