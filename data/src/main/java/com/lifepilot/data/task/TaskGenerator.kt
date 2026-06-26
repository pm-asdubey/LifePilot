package com.lifepilot.data.task

import com.lifepilot.data.database.dao.TaskDao
import com.lifepilot.data.database.entity.TaskEntity
import com.lifepilot.domain.engine.SchemaEngine
import com.lifepilot.domain.model.LifeObject
import timber.log.Timber
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskGenerator @Inject constructor(
    private val taskDao: TaskDao,
    private val schemaEngine: SchemaEngine,
) {
    private val objectCreationTasks: Map<String, List<TaskTemplate>> = mapOf(
        "passport" to listOf(
            TaskTemplate("Scan and upload passport document", "PHOTO", 7),
            TaskTemplate("Record expiry date", "METADATA", 3),
        ),
        "insurance" to listOf(
            TaskTemplate("Upload insurance certificate", "DOCUMENT", 7),
            TaskTemplate("Record policy start and end dates", "METADATA", 3),
        ),
        "job" to listOf(
            TaskTemplate("Upload employment contract", "DOCUMENT", 14),
            TaskTemplate("Record start date and employer details", "METADATA", 3),
        ),
        "property" to listOf(
            TaskTemplate("Upload title deed or lease agreement", "DOCUMENT", 14),
            TaskTemplate("Record property address and tenure details", "METADATA", 5),
        ),
        "vehicle" to listOf(
            TaskTemplate("Upload V5C / registration document", "DOCUMENT", 7),
            TaskTemplate("Record registration number and insurance expiry", "METADATA", 3),
        ),
        "bank_account" to listOf(
            TaskTemplate("Upload recent bank statement", "DOCUMENT", 7),
        ),
        "loan" to listOf(
            TaskTemplate("Upload loan agreement", "DOCUMENT", 7),
            TaskTemplate("Record interest rate and EMI due date", "METADATA", 3),
        ),
        "subscription" to listOf(
            TaskTemplate("Record renewal date", "METADATA", 1),
        ),
        "education" to listOf(
            TaskTemplate("Upload certificate or diploma", "DOCUMENT", 14),
        ),
        "health" to listOf(
            TaskTemplate("Upload health record or test result", "DOCUMENT", 7),
        ),
        "tax" to listOf(
            TaskTemplate("Upload tax assessment or return document", "DOCUMENT", 14),
            TaskTemplate("Record tax year and filing status", "METADATA", 3),
        ),
        "investment" to listOf(
            TaskTemplate("Upload investment statement", "DOCUMENT", 14),
            TaskTemplate("Record current value and maturity date", "METADATA", 5),
        ),
    )

    suspend fun generateObjectCreationTasks(obj: LifeObject) {
        val templates = objectCreationTasks[obj.objectType] ?: return
        Timber.d("Generating ${templates.size} tasks for ${obj.objectType} ${obj.objectId}")

        for (template in templates) {
            val existingTasks = taskDao.observeTasksByObject(obj.objectId)
            val taskId = UUID.randomUUID().toString()
            val dueDate = Instant.now().plus(template.dueDays.toLong(), ChronoUnit.DAYS)

            taskDao.insertTask(
                TaskEntity(
                    taskId = taskId,
                    profileId = obj.profileId,
                    objectId = obj.objectId,
                    title = template.title,
                    description = "Created automatically when ${obj.title} was added",
                    priority = if (template.dueDays <= 3) "HIGH" else "MEDIUM",
                    dueDate = dueDate.toEpochMilli(),
                    status = "PENDING",
                    source = "RULE_ENGINE",
                    completedAt = null,
                )
            )
        }
    }
}

private data class TaskTemplate(
    val title: String,
    val category: String,
    val dueDays: Int,
)
