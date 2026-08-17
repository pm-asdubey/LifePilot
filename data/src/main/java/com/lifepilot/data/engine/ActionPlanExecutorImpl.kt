package com.lifepilot.data.engine

import com.lifepilot.domain.engine.ActionPlanExecutor
import com.lifepilot.domain.engine.DomainLifeStateEngine
import com.lifepilot.domain.engine.PlanningEngine
import com.lifepilot.domain.model.ActionItem
import com.lifepilot.domain.model.ActionPlan
import com.lifepilot.domain.model.AiProposal
import com.lifepilot.domain.model.DomainEmoji
import com.lifepilot.domain.model.MetadataMerge
import com.lifepilot.domain.model.MetadataSource
import com.lifepilot.domain.model.ObjectStatus
import com.lifepilot.domain.model.ProposedField
import com.lifepilot.domain.model.TaskPriority
import com.lifepilot.domain.model.TaskSource
import com.lifepilot.domain.model.UpdateMode
import com.lifepilot.domain.repository.MetadataRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.ProjectRepository
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
    private val projectRepository: ProjectRepository,
    private val updateObjectStatusUseCase: UpdateObjectStatusUseCase,
    private val domainLifeStateEngine: DomainLifeStateEngine,
    private val lifeStateEngine: com.lifepilot.domain.engine.LifeStateEngine,
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
        // Maps CREATE_PROJECT itemId → created projectId so tasks can reference it.
        val createdProjectIds = mutableMapOf<String, String>()
        var anyFailure = false

        for (item in orderedItems) {
            val result = runCatching { executeItem(profileId, item, affectedDomains, createdProjectIds) }
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
        createdProjectIds: MutableMap<String, String>,
    ) {
        when (item) {
            is ActionItem.CreateProject -> {
                // If the AI left the emoji as the generic default, derive one from the domain.
                val emoji = item.emoji.takeIf { it.isNotBlank() && it != DomainEmoji.DEFAULT }
                    ?: DomainEmoji.forDomain(item.domain)
                val project = projectRepository.createProject(
                    profileId = profileId,
                    title = item.title,
                    description = item.description,
                    domain = item.domain,
                    emoji = emoji,
                    targetDate = item.targetDate,
                    isAiProposed = true,
                )
                createdProjectIds[item.itemId] = project.projectId
                item.domain?.let { affectedDomains.add(it) }
            }
            is ActionItem.UpdateRecord -> {
                for (field in item.fields) {
                    val existing = metadataRepository.getMetadataByField(item.objectId, field.fieldId)?.value
                    val finalValue = MetadataMerge.merge(field.mode, existing, field.value)
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
                val obj = objectRepository.createObject(
                    profileId = profileId,
                    objectType = item.objectType,
                    domain = item.domain,
                    title = item.title,
                    description = item.initialNotes,
                )
                affectedDomains.add(item.domain)
                // Enter the Life State Engine (tasks + reminders) so plan-created records aren't
                // second-class to manually created ones.
                runCatching {
                    lifeStateEngine.processObjectEvent(
                        objectId = obj.objectId,
                        eventType = "OBJECT_CREATED",
                        payload = "{\"objectType\":\"${item.objectType}\"}",
                    )
                }.onFailure { Timber.w(it, "Life State Engine event failed for ${obj.objectId}") }
                // Link to project so it surfaces in Project Workspace Documents tab.
                // The record still lives in Library as the source of truth.
                item.projectItemId?.let { projItemId ->
                    createdProjectIds[projItemId]?.let { projId ->
                        runCatching { projectRepository.linkObject(projId, obj.objectId) }
                            .onFailure { Timber.w(it, "Failed to link record ${obj.objectId} to project $projId") }
                    }
                }
            }
            is ActionItem.UpdateStatus -> {
                updateObjectStatusUseCase(
                    objectId = item.objectId,
                    newStatus = item.newStatus,
                ).getOrThrow()
                addObjectDomain(item.objectId, affectedDomains)
            }
            is ActionItem.CreateTask -> {
                val resolvedProjectId = item.projectItemId?.let { createdProjectIds[it] }
                val task = planningEngine.createTask(
                    profileId = profileId,
                    title = item.title,
                    description = item.description,
                    dueDate = item.dueDate,
                    goalId = item.goalId,
                    objectId = item.objectId,
                    projectId = resolvedProjectId,
                    source = TaskSource.AI_PROPOSED,
                    priority = item.priority,
                ).getOrThrow()
                // Also register the link in ProjectDao so observeTasksForProject works.
                resolvedProjectId?.let { projId ->
                    runCatching { projectRepository.linkTask(projId, task.taskId) }
                        .onFailure { Timber.w(it, "Failed to link task ${task.taskId} to project $projId") }
                }
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
