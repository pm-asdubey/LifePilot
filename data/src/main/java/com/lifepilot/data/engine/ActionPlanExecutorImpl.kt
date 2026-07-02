package com.lifepilot.data.engine

import com.lifepilot.domain.engine.ActionPlanExecutor
import com.lifepilot.domain.engine.DomainLifeStateEngine
import com.lifepilot.domain.engine.PlanningEngine
import com.lifepilot.domain.model.ActionItem
import com.lifepilot.domain.model.ActionPlan
import com.lifepilot.domain.model.AiProposal
import com.lifepilot.domain.model.MetadataSource
import com.lifepilot.domain.model.ObjectStatus
import com.lifepilot.domain.model.ProposedField
import com.lifepilot.domain.model.TaskPriority
import com.lifepilot.domain.model.TaskSource
import com.lifepilot.domain.model.UpdateMode
import com.lifepilot.domain.repository.MetadataRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.data.repository.PreferenceManager
import com.lifepilot.domain.usecase.UpdateObjectStatusUseCase
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActionPlanExecutorImpl @Inject constructor(
    private val preferenceManager: PreferenceManager,
    private val planningEngine: PlanningEngine,
    private val objectRepository: ObjectRepository,
    private val metadataRepository: MetadataRepository,
    private val updateObjectStatusUseCase: UpdateObjectStatusUseCase,
    private val domainLifeStateEngine: DomainLifeStateEngine,
) : ActionPlanExecutor {

    override suspend fun execute(
        plan: ActionPlan,
        onProgress: suspend (item: ActionItem, result: Result<Unit>) -> Unit,
    ): Result<Unit> {
        val profileId = preferenceManager.getActiveProfileId()
            ?: return Result.failure(IllegalStateException("No active profile"))

        val orderedItems = topologicalSort(plan.items.filter { it.isEnabled && it.isChecked })
        if (orderedItems.isEmpty()) {
            return Result.success(Unit)
        }

        val executedSummaries = mutableListOf<String>()
        val affectedDomains = mutableSetOf<String>()
        var anyFailure = false

        for (item in orderedItems) {
            val result = runCatching { executeItem(profileId, item, affectedDomains) }
            if (result.isSuccess) {
                executedSummaries.add(item.summary)
            } else {
                anyFailure = true
                Timber.e(result.exceptionOrNull(), "ActionPlan item failed: ${item.itemId}")
            }
            onProgress(item, result)
        }

        // Update domain understanding for all affected domains.
        if (affectedDomains.isNotEmpty() && executedSummaries.isNotEmpty()) {
            runCatching {
                domainLifeStateEngine.evaluateAndUpdate(
                    profileId = profileId,
                    userMessage = "Action plan approved: ${plan.summary}",
                    aiResponse = executedSummaries.joinToString("\n"),
                    affectedDomains = affectedDomains.toList(),
                )
            }.onFailure { Timber.e(it, "ActionPlan domain update failed") }
        }

        return if (anyFailure) {
            Result.failure(IllegalStateException("One or more action plan items failed"))
        } else {
            Result.success(Unit)
        }
    }

    private suspend fun executeItem(
        profileId: String,
        item: ActionItem,
        affectedDomains: MutableSet<String>,
    ) {
        when (item) {
            is ActionItem.UpdateRecord -> {
                for (field in item.fields) {
                    val finalValue = if (field.mode == UpdateMode.APPEND) {
                        val existing = metadataRepository.getMetadataByField(item.objectId, field.fieldId)?.value
                        if (existing.isNullOrBlank()) field.value else "$existing\n${field.value}"
                    } else {
                        field.value
                    }
                    metadataRepository.upsertMetadata(
                        objectId = item.objectId,
                        fieldId = field.fieldId,
                        value = finalValue,
                        source = MetadataSource.AI_EXTRACTED,
                        confidence = 0.9f,
                    )
                }
                addObjectDomain(item.objectId, affectedDomains)
            }
            is ActionItem.CreateRecord -> {
                objectRepository.createObject(
                    profileId = profileId,
                    objectType = item.objectType,
                    domain = item.domain,
                    title = item.title,
                    description = item.initialNotes,
                )
                affectedDomains.add(item.domain)
            }
            is ActionItem.UpdateStatus -> {
                updateObjectStatusUseCase(
                    objectId = item.objectId,
                    newStatus = item.newStatus,
                ).getOrThrow()
                addObjectDomain(item.objectId, affectedDomains)
            }
            is ActionItem.CreateTask -> {
                planningEngine.createTask(
                    profileId = profileId,
                    title = item.title,
                    description = item.description,
                    dueDate = item.dueDate,
                    goalId = item.goalId,
                    objectId = item.objectId,
                    source = TaskSource.AI_PROPOSED,
                    priority = item.priority,
                ).getOrThrow()
                // Tasks don't directly map to a domain; infer from objectId if present.
                item.objectId?.let { objId ->
                    runCatching { objectRepository.getObjectById(objId)?.domain }?.getOrNull()?.let {
                        affectedDomains.add(it)
                    }
                }
            }
            is ActionItem.UpdateDomainUnderstanding -> {
                affectedDomains.add(item.domain)
            }
        }
    }

    private suspend fun addObjectDomain(objectId: String, affectedDomains: MutableSet<String>) {
        runCatching { objectRepository.getObjectById(objectId)?.domain }
            .getOrNull()
            ?.let { affectedDomains.add(it) }
    }

    /**
     * Topological sort based on [ActionItem.dependsOn].
     * Cycles are broken by keeping the original order for the involved items.
     */
    private fun topologicalSort(items: List<ActionItem>): List<ActionItem> {
        if (items.none { it.dependsOn.isNotEmpty() }) return items

        val itemMap = items.associateBy { it.itemId }
        val visited = mutableSetOf<String>()
        val result = mutableListOf<ActionItem>()

        fun visit(item: ActionItem) {
            if (item.itemId in visited) return
            visited.add(item.itemId)
            item.dependsOn.forEach { depId ->
                itemMap[depId]?.let { visit(it) }
            }
            result.add(item)
        }

        items.forEach { visit(it) }
        return result
    }
}
