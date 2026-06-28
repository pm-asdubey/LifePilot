package com.lifepilot.domain.model

import java.time.LocalDate

/**
 * All structured proposals AI can make. Every proposal requires explicit user approval
 * before being executed through the PlanningEngine or Life State Engine.
 *
 * The canonical pipeline:
 *   AI reasons → emits AiProposal → user approves → PlanningEngine / LifeStateEngine executes
 */
sealed class AiProposal {
    abstract val proposalId: String
    abstract val summary: String

    /** Update metadata fields on a tracked record. */
    data class MetadataUpdate(
        override val proposalId: String,
        override val summary: String,
        val objectId: String,
        val objectTitle: String,
        val objectType: String,
        val fields: List<ProposedField>,
    ) : AiProposal()

    /** Create a new Goal in Planner. Routed through PlanningEngine after approval. */
    data class GoalProposal(
        override val proposalId: String,
        override val summary: String,
        val title: String,
        val description: String?,
        val deadline: LocalDate?,
        val estimatedWeeks: Int?,
        val suggestedTasks: List<String>,
        val linkedObjectId: String?,
    ) : AiProposal()

    /** Complete an existing Task in Planner. Routed through PlanningEngine after approval. */
    data class TaskCompletion(
        override val proposalId: String,
        override val summary: String,
        val taskId: String,
        val taskTitle: String,
        val goalId: String?,
    ) : AiProposal()

    /** Create a new task. Routed through PlanningEngine after approval. */
    data class TaskCreation(
        override val proposalId: String,
        override val summary: String,
        val title: String,
        val description: String?,
        val dueDate: LocalDate?,
        val goalId: String?,
        val objectId: String?,
    ) : AiProposal()

    /** Create a new tracked Object (record) when the user mentions a new life entity. */
    data class ObjectCreation(
        override val proposalId: String,
        override val summary: String,
        val objectType: String,
        val domain: String,
        val title: String,
        val initialNotes: String?,
    ) : AiProposal()
}

// ── Supporting types ──────────────────────────────────────────────────────────

data class ProposedField(
    val fieldId: String,
    val displayName: String,
    val value: String,
    val mode: UpdateMode = UpdateMode.SET,
)

enum class UpdateMode { SET, APPEND }

// ── Backward-compat alias so existing code referencing ProposedAction compiles ─

typealias ProposedAction = AiProposal.MetadataUpdate
