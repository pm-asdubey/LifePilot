package com.lifepilot.features.home.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifepilot.designsystem.components.EmptyState
import com.lifepilot.designsystem.components.SectionHeader
import com.lifepilot.designsystem.components.TaskCard
import com.lifepilot.designsystem.components.TimelineCard
import com.lifepilot.designsystem.theme.Spacing
import com.lifepilot.designsystem.theme.Warning
import com.lifepilot.domain.model.TaskPriority
import com.lifepilot.features.home.viewmodel.HomeViewModel
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToObject: (String) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onAddObject: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = timeBasedGreeting(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = uiState.profileName.ifBlank { "LifePilot" },
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddObject,
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Object",
                )
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = Spacing.xxl),
        ) {
            item {
                StatsRow(
                    objectCount = uiState.objectCount,
                    pendingTaskCount = uiState.pendingTaskCount,
                    modifier = Modifier.padding(Spacing.md),
                )
            }

            if (uiState.pendingTasks.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    SectionHeader(
                        title = "TASKS",
                        actionLabel = "Library",
                        onAction = onNavigateToLibrary,
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
                items(uiState.pendingTasks, key = { it.taskId }) { task ->
                    val priorityColor = when (task.priority) {
                        TaskPriority.URGENT -> MaterialTheme.colorScheme.error
                        TaskPriority.HIGH -> Warning
                        TaskPriority.MEDIUM -> MaterialTheme.colorScheme.primary
                        TaskPriority.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    TaskCard(
                        title = task.title,
                        dueDateLabel = task.dueDate?.format(DateTimeFormatter.ofPattern("MMM d")),
                        priorityLabel = task.priority.name,
                        priorityColor = priorityColor,
                        isCompleted = false,
                        onComplete = { viewModel.completeTask(task.taskId) },
                        onClick = {},
                        modifier = Modifier.padding(horizontal = Spacing.sm),
                    )
                }
            }

            if (uiState.recentActivity.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    SectionHeader(
                        title = "RECENT ACTIVITY",
                        actionLabel = "Timeline",
                        onAction = {},
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
                items(
                    uiState.recentActivity,
                    key = { it.timelineId },
                ) { entry ->
                    val isLast = uiState.recentActivity.last().timelineId == entry.timelineId
                    TimelineCard(
                        title = entry.title,
                        summary = entry.summary,
                        dateLabel = entry.timestamp
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                        isLast = isLast,
                        onClick = {
                            entry.objectId?.let { onNavigateToObject(it) }
                        },
                        modifier = Modifier.padding(horizontal = Spacing.sm),
                    )
                }
            }

            if (uiState.domainCounts.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    SectionHeader(
                        title = "BY DOMAIN",
                        actionLabel = "Library",
                        onAction = onNavigateToLibrary,
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
                item {
                    DomainDistribution(
                        domainCounts = uiState.domainCounts,
                        modifier = Modifier.padding(horizontal = Spacing.md),
                    )
                }
            }

            if (uiState.pendingTasks.isEmpty() && uiState.recentActivity.isEmpty() && !uiState.isLoading) {
                item {
                    Spacer(modifier = Modifier.height(Spacing.xxl))
                    EmptyState(
                        icon = Icons.Outlined.FolderOpen,
                        title = "Your life dashboard is empty",
                        description = "Start by adding your first document or object to track.",
                        actionLabel = "Add First Object",
                        onAction = onAddObject,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsRow(
    objectCount: Int,
    pendingTaskCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        StatCard(
            label = "Objects",
            value = objectCount.toString(),
            icon = Icons.Outlined.FolderOpen,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = "Pending Tasks",
            value = pendingTaskCount.toString(),
            icon = Icons.Outlined.Assignment,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DomainDistribution(
    domainCounts: Map<String, Int>,
    modifier: Modifier = Modifier,
) {
    val total = domainCounts.values.sum().coerceAtLeast(1)
    val sorted = domainCounts.entries.sortedByDescending { it.value }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            sorted.forEach { (domain, count) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = domain.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.size(Spacing.sm))
                    // Progress bar
                    Canvas(
                        modifier = Modifier
                            .size(width = 80.dp, height = 6.dp),
                    ) {
                        val fraction = count.toFloat() / total
                        val trackColor = androidx.compose.ui.graphics.Color(0x1A6650A4.toInt())
                        val fillColor = androidx.compose.ui.graphics.Color(0xFF6650A4.toInt())
                        drawRect(color = trackColor)
                        drawRect(color = fillColor, size = size.copy(width = size.width * fraction))
                    }
                }
            }
        }
    }
}

private fun timeBasedGreeting(): String {
    return when (LocalTime.now().hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }
}
