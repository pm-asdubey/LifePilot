package com.lifepilot.data.engine

import com.lifepilot.domain.engine.SchemaEngine
import com.lifepilot.domain.model.Reminder
import com.lifepilot.domain.model.ReminderPriority
import com.lifepilot.domain.model.ReminderStatus
import com.lifepilot.domain.model.schema.ReminderRule
import com.lifepilot.domain.repository.MetadataRepository
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.ReminderRepository
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleEngineImpl @Inject constructor(
    private val objectRepository: ObjectRepository,
    private val metadataRepository: MetadataRepository,
    private val reminderRepository: ReminderRepository,
    private val schemaEngine: SchemaEngine,
) {
    suspend fun evaluateRemindersForObject(objectId: String) {
        val obj = objectRepository.getObjectById(objectId) ?: return
        val schema = schemaEngine.getSchema(obj.objectType) ?: return
        val metadata = metadataRepository.getMetadataByObject(objectId)

        for (rule in schema.reminderRules) {
            try {
                evaluateRule(objectId, rule, metadata.associate { it.fieldId to it.value })
            } catch (e: Exception) {
                Timber.e(e, "Error evaluating rule ${rule.ruleId} for $objectId")
            }
        }
    }

    private suspend fun evaluateRule(
        objectId: String,
        rule: ReminderRule,
        metadataValues: Map<String, String>,
    ) {
        val fieldValue = metadataValues[rule.triggerField] ?: return
        val triggerDate = parseDate(fieldValue) ?: return
        val reminderDate = triggerDate.plusDays(rule.offsetDays.toLong())

        if (reminderDate.isBefore(LocalDate.now())) return

        val existing = reminderRepository.getRemindersForObject(objectId)
        val alreadyExists = existing.any { it.reminderType == rule.ruleId }
        if (alreadyExists) return

        val triggerInstant = reminderDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val reminder = Reminder(
            reminderId = UUID.randomUUID().toString(),
            objectId = objectId,
            reminderType = rule.ruleId,
            triggerDate = triggerInstant,
            priority = ReminderPriority.valueOf(rule.priority),
            status = ReminderStatus.SCHEDULED,
            title = rule.title,
            message = rule.messageTemplate,
        )
        reminderRepository.createReminder(reminder)
        Timber.d("Created reminder ${rule.ruleId} for object $objectId, triggers $reminderDate")
    }

    private fun parseDate(value: String): LocalDate? {
        val formats = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("d MMM yyyy"),
            DateTimeFormatter.ofPattern("d MMMM yyyy"),
        )
        for (fmt in formats) {
            try {
                return LocalDate.parse(value, fmt)
            } catch (_: Exception) {}
        }
        return null
    }
}
