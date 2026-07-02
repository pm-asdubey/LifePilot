package com.lifepilot.domain.model

import java.time.LocalDate

/**
 * Generic multi-step action plan.
 *
 * ActionPlan is the central platform abstraction for any life event that requires
 * coordinated changes across multiple records, tasks, and domain understandings.
 * Examples: job change, marriage, relocation, passport renewal, visa application.
 */
data class ActionPlan(
    val planId: String,
    val type: ActionPlanType,
    val summary: String,
    val items: List<ActionItem>,
    val clarifyingQuestions: List<ClarifyingQuestion> = emptyList(),
)

enum class ActionPlanType {
    JOB_CHANGE,
    RELOCATION,
    MARRIAGE,
    DIVORCE,
    CHILDBIRTH,
    DEATH_OF_RELATIVE,
    NEW_PROPERTY,
    MAJOR_MEDICAL,
    NEW_FINANCIAL_PRODUCT,
    VISA_IMMIGRATION,
    MAJOR_PURCHASE,
    CUSTOM,
}

/**
 * One unit of work inside an [ActionPlan].
 * Each item maps to an existing single-step [AiProposal] at execution time.
 */
sealed class ActionItem {
    abstract val itemId: String
    abstract val summary: String
    abstract val dependsOn: List<String>
    abstract val isEnabled: Boolean
    abstract val isChecked: Boolean

    /** Update fields on an existing record. */
    data class UpdateRecord(
        override val itemId: String,
        override val summary: String,
        val objectId: String,
        val objectTitle: String,
        val objectType: String,
        val fields: List<ProposedField>,
        override val dependsOn: List<String> = emptyList(),
        override val isEnabled: Boolean = true,
        override val isChecked: Boolean = true,
    ) : ActionItem()

    /** Create a new record. */
    data class CreateRecord(
        override val itemId: String,
        override val summary: String,
        val objectType: String,
        val domain: String,
        val title: String,
        val initialNotes: String? = null,
        override val dependsOn: List<String> = emptyList(),
        override val isEnabled: Boolean = true,
        override val isChecked: Boolean = true,
    ) : ActionItem()

    /** Update the status of an existing record. */
    data class UpdateStatus(
        override val itemId: String,
        override val summary: String,
        val objectId: String,
        val objectTitle: String,
        val newStatus: ObjectStatus,
        override val dependsOn: List<String> = emptyList(),
        override val isEnabled: Boolean = true,
        override val isChecked: Boolean = true,
    ) : ActionItem()

    /** Create a new task in Planner. */
    data class CreateTask(
        override val itemId: String,
        override val summary: String,
        val title: String,
        val description: String? = null,
        val dueDate: LocalDate? = null,
        val priority: TaskPriority = TaskPriority.MEDIUM,
        val goalId: String? = null,
        val objectId: String? = null,
        override val dependsOn: List<String> = emptyList(),
        override val isEnabled: Boolean = true,
        override val isChecked: Boolean = true,
    ) : ActionItem()

    /** Update domain-level understanding after concrete changes. */
    data class UpdateDomainUnderstanding(
        override val itemId: String,
        override val summary: String,
        val domain: String,
        override val dependsOn: List<String> = emptyList(),
        override val isEnabled: Boolean = true,
        override val isChecked: Boolean = true,
    ) : ActionItem()
}

/**
 * A question that determines whether conditional items are enabled or what values they receive.
 */
data class ClarifyingQuestion(
    val questionId: String,
    val text: String,
    val answer: String? = null,
    val affectedItemIds: List<String> = emptyList(),
)
