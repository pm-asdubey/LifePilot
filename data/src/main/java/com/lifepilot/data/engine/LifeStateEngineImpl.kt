package com.lifepilot.data.engine

import com.lifepilot.domain.engine.AttentionItem
import com.lifepilot.domain.engine.AttentionPriority
import com.lifepilot.domain.engine.LifeStateEngine
import com.lifepilot.domain.model.MetadataEntry
import com.lifepilot.domain.model.ReminderPriority
import com.lifepilot.domain.model.ReminderStatus
import com.lifepilot.domain.repository.MetadataRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.ReminderRepository
import com.lifepilot.domain.repository.TimelineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LifeStateEngineImpl @Inject constructor(
    private val objectRepository: ObjectRepository,
    private val metadataRepository: MetadataRepository,
    private val timelineRepository: TimelineRepository,
    private val reminderRepository: ReminderRepository,
    private val ruleEngine: RuleEngineImpl,
) : LifeStateEngine {

    override suspend fun processDocumentIngestion(
        objectId: String,
        documentId: String,
        ocrText: String?,
    ) {
        Timber.d("Processing document ingestion for object $objectId, doc $documentId")
        evaluateRules(objectId)
    }

    override suspend fun processMetadataUpdate(
        objectId: String,
        updatedFields: List<MetadataEntry>,
    ) {
        Timber.d("Processing metadata update for object $objectId, ${updatedFields.size} fields")
        evaluateRules(objectId)
    }

    override suspend fun processObjectEvent(
        objectId: String,
        eventType: String,
        payload: String,
    ) {
        Timber.d("Processing event $eventType for object $objectId")
        evaluateRules(objectId)
    }

    override suspend fun evaluateRules(objectId: String) {
        ruleEngine.evaluateRemindersForObject(objectId)
    }

    override fun observeAttentionRequired(profileId: String): Flow<List<AttentionItem>> {
        val thirtyDaysLater = Instant.now().plus(30, ChronoUnit.DAYS)
        return combine(
            objectRepository.observeObjectsByProfile(profileId),
            reminderRepository.observeUpcomingReminders(thirtyDaysLater),
        ) { objects, reminders ->
            val items = mutableListOf<AttentionItem>()

            for (reminder in reminders) {
                if (reminder.status != ReminderStatus.SCHEDULED) continue
                val daysUntil = ChronoUnit.DAYS.between(
                    Instant.now(),
                    reminder.triggerDate,
                )
                items.add(
                    AttentionItem(
                        itemId = reminder.reminderId,
                        title = reminder.title,
                        description = reminder.message ?: "Due in $daysUntil days",
                        priority = when (reminder.priority) {
                            ReminderPriority.CRITICAL -> AttentionPriority.CRITICAL
                            ReminderPriority.HIGH -> AttentionPriority.HIGH
                            ReminderPriority.MEDIUM -> AttentionPriority.MEDIUM
                            ReminderPriority.LOW -> AttentionPriority.LOW
                        },
                        objectId = reminder.objectId,
                        actionType = "VIEW_OBJECT",
                    )
                )
            }

            items.sortedBy { it.priority.ordinal }.reversed()
        }
    }
}
