package com.lifepilot.features.planner.ui

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifepilot.designsystem.components.EmptyState
import com.lifepilot.designsystem.theme.Spacing
import com.lifepilot.domain.model.LifeObject
import com.lifepilot.domain.model.Project
import com.lifepilot.domain.model.Task
import com.lifepilot.domain.model.TaskStatus
import com.lifepilot.features.planner.viewmodel.ProjectWorkspaceViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectWorkspaceScreen(
    onBack: () -> Unit,
    onNavigateToObject: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProjectWorkspaceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val project = uiState.project
    val tabs = listOf("Overview", "Tasks", "Documents", "AI")

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                title = {},
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit project",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Project header
            project?.let {
                ProjectHeader(project = it, tasks = uiState.tasks)
            }

            // Tab row
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedTab,
                edgePadding = Spacing.md,
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = uiState.selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        text = { Text(title) },
                    )
                }
            }

            // Tab content
            when (uiState.selectedTab) {
                0 -> OverviewTab(
                    project = project,
                    tasks = uiState.tasks,
                    linkedObjects = uiState.linkedObjects,
                )
                1 -> WorkspaceTasksTab(
                    tasks = uiState.tasks,
                )
                2 -> DocumentsTab(
                    linkedObjects = uiState.linkedObjects,
                    onNavigateToObject = onNavigateToObject,
                )
                3 -> AiTab(project = project)
            }
        }
    }
}

@Composable
private fun ProjectHeader(
    project: Project,
    tasks: List<Task>,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val daysLeft = project.targetDate?.let { ChronoUnit.DAYS.between(today, it) }
    val openTasks = tasks.count {
        it.status != TaskStatus.COMPLETED && it.status != TaskStatus.DISMISSED
    }
    val progress = if (tasks.isNotEmpty()) {
        ((tasks.size - openTasks).toFloat() / tasks.size * 100).toInt()
    } else 0

    val subtitleParts = buildList {
        project.targetDate?.let {
            add(it.format(DateTimeFormatter.ofPattern("MMM d, yyyy")))
        }
        add("$progress% complete")
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(text = project.emoji, fontSize = 36.sp)
        Column {
            Text(
                text = project.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
            Text(
                text = subtitleParts.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun OverviewTab(
    project: Project?,
    tasks: List<Task>,
    linkedObjects: List<LifeObject>,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val openTasks = tasks.count { it.status != TaskStatus.COMPLETED && it.status != TaskStatus.DISMISSED }
    val daysLeft = project?.targetDate?.let { ChronoUnit.DAYS.between(today, it) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // Stats row
        item(key = "stats") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                StatCard(
                    label = "Open tasks",
                    value = "$openTasks",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = "Documents",
                    value = "${linkedObjects.size}",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = "Days left",
                    value = daysLeft?.let { if (it >= 0) "$it" else "Overdue" } ?: "—",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Next tasks card
        item(key = "next_tasks_header") {
            Text(
                text = "Next tasks",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }

        val nextTasks = tasks
            .filter { it.status != TaskStatus.COMPLETED && it.status != TaskStatus.DISMISSED }
            .take(3)

        if (nextTasks.isEmpty()) {
            item(key = "no_tasks") {
                Text(
                    text = "No open tasks.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(nextTasks, key = { "next_${it.taskId}" }) { task ->
                NextTaskRow(task = task)
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NextTaskRow(
    task: Task,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
            )
            task.dueDate?.let { date ->
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("MMM d")),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WorkspaceTasksTab(
    tasks: List<Task>,
    modifier: Modifier = Modifier,
) {
    val openTasks = tasks.filter {
        it.status != TaskStatus.COMPLETED && it.status != TaskStatus.DISMISSED
    }
    val completedTasks = tasks.filter { it.status == TaskStatus.COMPLETED }

    if (tasks.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            EmptyState(
                icon = Icons.Outlined.CheckCircle,
                title = "No tasks",
                description = "Tasks linked to this project will appear here.",
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        if (openTasks.isNotEmpty()) {
            item(key = "open_header") {
                Text(
                    text = "Open · ${openTasks.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            items(openTasks, key = { "open_${it.taskId}" }) { task ->
                WorkspaceTaskRow(task = task)
            }
        }

        if (completedTasks.isNotEmpty()) {
            item(key = "completed_header") {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Completed · ${completedTasks.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            items(completedTasks, key = { "done_${it.taskId}" }) { task ->
                WorkspaceTaskRow(task = task, dimmed = true)
            }
        }
    }
}

@Composable
private fun WorkspaceTaskRow(
    task: Task,
    dimmed: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val contentAlpha = if (dimmed) 0.5f else 1f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = if (dimmed) {
                MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(18.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            )
            task.dueDate?.let {
                Text(
                    text = it.format(DateTimeFormatter.ofPattern("MMM d")),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                )
            }
        }
        Text(
            text = task.priority.name.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
        )
    }
}

@Composable
private fun DocumentsTab(
    linkedObjects: List<LifeObject>,
    onNavigateToObject: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (linkedObjects.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            EmptyState(
                icon = Icons.Outlined.Article,
                title = "No linked records",
                description = "Link records to this project from the object detail screen.",
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item(key = "docs_header") {
            Text(
                text = "Linked Records · ${linkedObjects.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.xs),
            )
        }
        items(linkedObjects, key = { it.objectId }) { obj ->
            Card(
                onClick = { onNavigateToObject(obj.objectId) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = obj.title,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "${obj.objectType} · ${obj.domain}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AiTab(
    project: Project?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // Scoped context banner
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Column {
                    Text(
                        text = "🎯 Scoped to ${project?.title ?: "this project"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "AI only sees project tasks and linked records.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        Text(
            text = "Coming soon — project-scoped AI chat will be available in a future update.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
