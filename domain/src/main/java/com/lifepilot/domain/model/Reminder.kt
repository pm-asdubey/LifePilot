package com.lifepilot.domain.model

import java.time.Instant

data class Reminder(
    val reminderId: String,
    val objectId: String,
    val reminderType: String,
    val triggerDate: Instant,
    val priority: ReminderPriority,
    val status: ReminderStatus,
    val title: String,
    val message: String?,
)

enum class ReminderPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}

enum class ReminderStatus {
    SCHEDULED,
    TRIGGERED,
    DISMISSED,
    SNOOZED,
}
