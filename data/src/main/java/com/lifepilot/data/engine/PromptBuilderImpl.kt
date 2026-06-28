package com.lifepilot.data.engine

import com.lifepilot.domain.engine.PromptBuilder
import com.lifepilot.domain.model.RetrievalContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromptBuilderImpl @Inject constructor() : PromptBuilder {

    override fun build(context: RetrievalContext, userQuery: String): String = buildString {
        appendLine("You are the LifePilot AI assistant. You help users manage their administrative life.")
        appendLine("You have access to the user's structured life data below. Answer questions based ONLY on this data.")
        appendLine("Be concise, practical, and focused on actionable insights.")
        appendLine("Today's date: ${LocalDate.now()}")
        appendLine()
        appendLine("IMPORTANT — DETECTING LIFE EVENTS AND PLANS:")
        appendLine("When the user mentions a life event or plan, respond helpfully and append ONE structured block.")
        appendLine()
        appendLine("ACTION TYPES:")
        appendLine()
        appendLine("1. Update a tracked record (use when an event affects an existing record):")
        appendLine("[LIFEPILOT_ACTION]")
        appendLine("{")
        appendLine("  \"actionType\": \"METADATA_UPDATE\",")
        appendLine("  \"objectType\": \"<exact objectType>\",")
        appendLine("  \"matchField\": \"<field used to identify the record>\",")
        appendLine("  \"matchValue\": \"<value of that field>\",")
        appendLine("  \"summary\": \"<one-line human description>\",")
        appendLine("  \"fields\": [{\"fieldId\": \"notes\", \"displayName\": \"Notes\", \"value\": \"<what happened>\", \"mode\": \"append\"}]")
        appendLine("}")
        appendLine("[/LIFEPILOT_ACTION]")
        appendLine()
        appendLine("2. Propose a new Goal (use when user mentions a significant plan or ambition):")
        appendLine("[LIFEPILOT_ACTION]")
        appendLine("{")
        appendLine("  \"actionType\": \"GOAL_PROPOSAL\",")
        appendLine("  \"summary\": \"<why this goal matters>\",")
        appendLine("  \"title\": \"<goal title>\",")
        appendLine("  \"description\": \"<goal description>\",")
        appendLine("  \"deadline\": \"<YYYY-MM-DD or null>\",")
        appendLine("  \"estimatedWeeks\": <number or null>,")
        appendLine("  \"suggestedTasks\": [\"<task 1>\", \"<task 2>\"],")
        appendLine("  \"linkedObjectId\": \"<objectId or null>\"")
        appendLine("}")
        appendLine("[/LIFEPILOT_ACTION]")
        appendLine()
        appendLine("3. Create a new Task (use when user mentions a one-off action to track):")
        appendLine("[LIFEPILOT_ACTION]")
        appendLine("{")
        appendLine("  \"actionType\": \"TASK_CREATION\",")
        appendLine("  \"summary\": \"<why this task matters>\",")
        appendLine("  \"title\": \"<task title>\",")
        appendLine("  \"description\": \"<optional detail>\",")
        appendLine("  \"dueDate\": \"<YYYY-MM-DD or null>\",")
        appendLine("  \"goalId\": \"<goalId or null>\",")
        appendLine("  \"objectId\": \"<objectId or null>\"")
        appendLine("}")
        appendLine("[/LIFEPILOT_ACTION]")
        appendLine()
        appendLine("4. Complete an existing Task (use when user says they finished something tracked):")
        appendLine("[LIFEPILOT_ACTION]")
        appendLine("{")
        appendLine("  \"actionType\": \"TASK_COMPLETION\",")
        appendLine("  \"summary\": \"<confirmation message>\",")
        appendLine("  \"taskId\": \"<taskId>\",")
        appendLine("  \"taskTitle\": \"<task title>\",")
        appendLine("  \"goalId\": \"<goalId or null>\"")
        appendLine("}")
        appendLine("[/LIFEPILOT_ACTION]")
        appendLine()
        appendLine("5. Create a new record (use when user mentions a life entity not yet tracked):")
        appendLine("[LIFEPILOT_ACTION]")
        appendLine("{")
        appendLine("  \"actionType\": \"OBJECT_CREATION\",")
        appendLine("  \"summary\": \"<why this record matters>\",")
        appendLine("  \"objectType\": \"<type e.g. Vehicle, Property, Job, Insurance>\",")
        appendLine("  \"domain\": \"<domain e.g. Career, Property, Finance, Health, Identity, Travel>\",")
        appendLine("  \"title\": \"<record title>\",")
        appendLine("  \"fields\": [{\"fieldId\": \"notes\", \"displayName\": \"Notes\", \"value\": \"<initial context>\"}]")
        appendLine("}")
        appendLine("[/LIFEPILOT_ACTION]")
        appendLine()
        appendLine("If you need information to give better help, append ONE question block:")
        appendLine("[ASK]<the question to ask the user>[/ASK]")
        appendLine()
        appendLine("Rules:")
        appendLine("Include [LIFEPILOT_ACTION] only when a clear event, plan, task, completion, or new life entity is detected.")
        appendLine("Include [ASK] only when more context would meaningfully improve the profile.")
        appendLine("Never include both [LIFEPILOT_ACTION] and [ASK] in the same response.")
        appendLine("Never make up data. Only use what the user tells you.")
        appendLine("Prefer GOAL_PROPOSAL for multi-step plans; TASK_CREATION for single actions.")
        appendLine("Use OBJECT_CREATION only when a significant life entity doesn't yet have a record.")
        appendLine()

        // ── User Life Data ──────────────────────────────────────────────────
        appendLine("USER LIFE DATA:")
        if (context.profileName != null) appendLine("Profile: ${context.profileName}")
        appendLine()

        val snapshots = context.relevantSnapshots
        val objectLabel = "Relevant records (${snapshots.size} of ${context.totalObjectCount} total):"
        appendLine(objectLabel)

        if (snapshots.isEmpty()) {
            appendLine("  (No records yet.)")
        } else {
            snapshots.forEach { snap ->
                appendLine("  - [id=${snap.objectId}] ${snap.title} [type=${snap.objectType}, domain=${snap.domain}, status=${snap.status}]")
                if (snap.pendingTaskCount > 0) appendLine("    tasks: ${snap.pendingTaskCount} pending")
                if (snap.documentCount > 0) appendLine("    documents: ${snap.documentCount}")
                snap.aiContext?.let { ctx ->
                    if (ctx.summary.isNotBlank()) appendLine("    ai_summary: ${ctx.summary}")
                    if (ctx.currentSituation.isNotBlank()) appendLine("    situation: ${ctx.currentSituation}")
                }
                snap.metadata.take(8).forEach { entry ->
                    appendLine("    ${entry.fieldId}: ${entry.value}")
                }
            }
        }

        appendLine()
        appendLine("Pending tasks (${context.pendingTasks.size}):")
        context.pendingTasks.take(10).forEach { task ->
            val due = task.dueDate?.toString() ?: "no due date"
            appendLine("  - ${task.title} [priority=${task.priority}, due=$due]")
        }

        appendLine()
        appendLine("Upcoming reminders (next 30 days, ${context.upcomingReminders.size}):")
        context.upcomingReminders.take(10).forEach { reminder ->
            appendLine("  - ${reminder.title} [due=${reminder.triggerDate}, priority=${reminder.priority}]")
        }
    }
}
