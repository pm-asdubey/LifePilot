package com.lifepilot.domain.usecase

import com.lifepilot.domain.model.DashboardData
import com.lifepilot.domain.model.Task
import com.lifepilot.domain.model.TimelineEntry
import com.lifepilot.domain.repository.ObjectRepository
import com.lifepilot.domain.repository.ReminderRepository
import com.lifepilot.domain.repository.TaskRepository
import com.lifepilot.domain.repository.TimelineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class GetDashboardDataUseCase @Inject constructor(
    private val objectRepository: ObjectRepository,
    private val taskRepository: TaskRepository,
    private val reminderRepository: ReminderRepository,
    private val timelineRepository: TimelineRepository,
) {
    operator fun invoke(profileId: String): Flow<DashboardData> {
        val thirtyDaysLater = Instant.now().plus(30, ChronoUnit.DAYS)
        return combine(
            objectRepository.observeObjectsByProfile(profileId),
            taskRepository.observePendingTasks(profileId),
            reminderRepository.observeUpcomingReminders(thirtyDaysLater),
            timelineRepository.observeTimeline(profileId),
        ) { objects, tasks, reminders, timeline ->
            DashboardData(
                objectCount = objects.size,
                pendingTaskCount = tasks.size,
                pendingTasks = tasks.take(5),
                upcomingReminderCount = reminders.size,
                recentActivity = timeline.take(10),
                domainCounts = objects.groupBy { it.domain }.mapValues { it.value.size },
            )
        }
    }
}
