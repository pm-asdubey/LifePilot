package com.lifepilot.features.planner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifepilot.designsystem.components.EmptyState
import com.lifepilot.designsystem.components.PlannerSkeleton
import com.lifepilot.designsystem.components.TaskCard
import com.lifepilot.designsystem.theme.Spacing
import com.lifepilot.designsystem.theme.Warning
import com.lifepilot.domain.model.Project
import com.lifepilot.domain.model.Task
import com.lifepilot.domain.model.TaskPriority
import com.lifepilot.domain.model.TaskStatus
import com.lifepilot.features.planner.state.TaskFilter
import com.lifepilot.features.planner.viewmodel.PlannerViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val EMOJI_OPTIONS = listOf(
    "✈️", "💼", "🏠", "🎓", "🏋️", "💰", "🎯", "🏥", "👶", "🌍", "🚗", "📚"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    onNavigateToObject: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToProject: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PlannerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val highlightedTaskId = uiState.highlightedTaskId
    var selectedTab by remember { mutableIntStateOf(if (highlightedTaskId.isNullOrBlank()) 0 else 1) }
    val tabs = listOf("Projects", "Tasks")
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.lastCompletedTaskId) {
        val taskId = uiState.lastCompletedTaskId ?: return@LaunchedEffect
        val title = uiState.lastCompletedTaskTitle ?: "Task"
        val result = snackbarHostState.showSnackbar(
            message = "\"$title\" completed",
            actionLabel = "Undo",
            withDismissAction = false,
        )
        when (result) {
            SnackbarResult.ActionPerformed -> viewModel.undoComplete()
            SnackbarResult.Dismissed -> viewModel.clearUndoState()
        }
    }

    LaunchedEffect(uiState.lastReopenedTaskTitle) {
        val title = uiState.lastReopenedTaskTitle ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            message = "\"$title\" moved back to pending",
            withDismissAction = true,
        )
        viewModel.clearReopenState()
    }

    if (uiState.showCreateProjectSheet) {
        CreateProjectSheet(
            onDismiss = viewModel::hideCreateProjectSheet,
            onCreateProject = { title, emoji, description, targetDate ->
                viewModel.createProject(title, emoji, description, targetDate)
            },
        )
    }

    uiState.editingTask?.let { task ->
        TaskDetailSheet(
            task = task,
            onDismiss = viewModel::closeEditTask,
            onSave = { taskId, title, description, dueDate, priority ->
                viewModel.saveEditedTask(taskId, title, description, dueDate, priority)
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Planner",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = viewModel::showCreateProjectSheet,
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Create project")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { innerPadding ->
        if (uiState.isLoading) {
            PlannerSkeleton(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = Spacing.md,
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                    )
                }
            }

            when (selectedTab) {
                0 -> ProjectsTab(
                    projects = uiState.projects,
                    projectTaskCounts = uiState.projectTaskCounts,
                    onProjectClick = onNavigateToProject,
                )
                1 -> TasksTab(
                    tasks = uiState.tasks,
                    selectedFilter = uiState.selectedTaskFilter,
                    highlightedTaskId = highlightedTaskId,
                    onFilterChange = viewModel::setTaskFilter,
                    onCompleteTask = viewModel::completeTask,
                    onReopenTask = viewModel::reopenTask,
                    onEditTask = viewModel::openEditTask,
                )
            }
        }
    }
}

// ── Projects Tab ──────────────────────────────────────────────────────────────

@Composable
private fun ProjectsTab(
    projects: List<Project>,
    projectTaskCounts: Map<String, Pair<Int, Int>>,
    onProjectClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (projects.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            EmptyState(
                icon = Icons.Outlined.FolderOpen,
                title = "No projects yet",
                description = "Tap + to create a project and keep all related tasks and documents together.",
            )
        }
        return
    }

    val userProjects = projects.filter { !it.isAiProposed }
    val aiProjects = projects.filter { it.isAiProposed }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        if (userProjects.isNotEmpty()) {
            item(key = "active_header") {
                Text(
                    text = "Active · ${userProjects.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.xs),
                )
            }
            items(userProjects, key = { it.projectId }) { project ->
                ProjectCard(
                    project = project,
                    taskCounts = projectTaskCounts[project.projectId] ?: Pair(0, 0),
                    onClick = { onProjectClick(project.projectId) },
                )
            }
        }

        if (aiProjects.isNotEmpty()) {
            item(key = "ai_header") {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Proposed by AI · ${aiProjects.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.xs),
                )
            }
            items(aiProjects, key = { it.projectId }) { project ->
                ProjectCard(
                    project = project,
                    taskCounts = projectTaskCounts[project.projectId] ?: Pair(0, 0),
                    onClick = { onProjectClick(project.projectId) },
                )
            }
        }
    }
}

@Composable
private fun ProjectCard(
    project: Project,
    taskCounts: Pair<Int, Int>, // (openTasks, totalTasks)
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val daysUntil = project.targetDate?.let { ChronoUnit.DAYS.between(today, it) }
    val openTasks = taskCounts.first
    val totalTasks = taskCounts.second
    val progress = if (totalTasks > 0) {
        (totalTasks - openTasks).toFloat() / totalTasks
    } else 0f
    val progressPct = (progress * 100).toInt()

    val statusLabel: String
    val statusColor: androidx.compose.ui.graphics.Color
    when {
        project.isAiProposed -> {
            statusLabel = "AI proposed"
            statusColor = MaterialTheme.colorScheme.tertiary
        }
        daysUntil != null && daysUntil <= 14 && progress < 0.5f -> {
            statusLabel = "Behind"
            statusColor = Warning
        }
        else -> {
            statusLabel = "On track"
            statusColor = MaterialTheme.colorScheme.primary
        }
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                // Emoji box
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = project.emoji, fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(Spacing.sm))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 15.sp,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "✅ $openTasks tasks",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                        )
                    }
                    val targetDate = project.targetDate
                    if (targetDate != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            color = Warning.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Text(
                                text = "📅 ${targetDate.format(DateTimeFormatter.ofPattern("MMM d"))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Warning,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 11.sp,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(Spacing.xs))

                // Status chip
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Progress row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                )
                Text(
                    text = "$progressPct%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Create Project Bottom Sheet ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateProjectSheet(
    onDismiss: () -> Unit,
    onCreateProject: (title: String, emoji: String, description: String?, targetDate: LocalDate?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedEmoji by remember { mutableStateOf("🎯") }
    var projectName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = targetDate
                ?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        targetDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                text = "Create Project",
                style = MaterialTheme.typography.titleMedium,
            )

            // Emoji picker
            Text(
                text = "Pick an emoji",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier.height(100.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                gridItems(EMOJI_OPTIONS) { emoji ->
                    val isSelected = emoji == selectedEmoji
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                            .then(
                                if (isSelected) Modifier.border(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(8.dp),
                                ) else Modifier
                            )
                            .clickable { selectedEmoji = emoji },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = emoji, fontSize = 20.sp)
                    }
                }
            }

            OutlinedTextField(
                value = projectName,
                onValueChange = { projectName = it },
                label = { Text("Project name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
            )

            // Target date row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Target date",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = targetDate?.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                            ?: "No deadline",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (targetDate != null) {
                    TextButton(onClick = { targetDate = null }) { Text("Clear") }
                }
            }

            Button(
                onClick = {
                    onCreateProject(
                        projectName,
                        selectedEmoji,
                        description.takeIf { it.isNotBlank() },
                        targetDate,
                    )
                },
                enabled = projectName.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Create Project")
            }

            Spacer(modifier = Modifier.height(Spacing.md))
        }
    }
}

// ── Tasks Tab ─────────────────────────────────────────────────────────────────

@Composable
private fun TasksTab(
    tasks: List<Task>,
    selectedFilter: TaskFilter,
    highlightedTaskId: String?,
    onFilterChange: (TaskFilter) -> Unit,
    onCompleteTask: (String) -> Unit,
    onReopenTask: (String) -> Unit,
    onEditTask: (Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState: LazyListState = rememberLazyListState()

    LaunchedEffect(highlightedTaskId, tasks) {
        val index = tasks.indexOfFirst { it.taskId == highlightedTaskId }
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            TaskFilter.entries.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterChange(filter) },
                    label = {
                        Text(
                            text = when (filter) {
                                TaskFilter.TODAY -> "Today"
                                TaskFilter.THIS_WEEK -> "This week"
                                TaskFilter.ALL -> "All"
                                TaskFilter.COMPLETED -> "Completed"
                            }
                        )
                    },
                )
            }
        }

        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    icon = Icons.Outlined.CheckCircle,
                    title = when (selectedFilter) {
                        TaskFilter.TODAY -> "Clear for today"
                        TaskFilter.THIS_WEEK -> "Nothing this week"
                        TaskFilter.ALL -> "All clear"
                        TaskFilter.COMPLETED -> "No completed tasks"
                    },
                    description = when (selectedFilter) {
                        TaskFilter.TODAY -> "You're on top of things. Any tasks due today will appear here."
                        TaskFilter.THIS_WEEK -> "A quiet week ahead. Tasks added this week will appear here."
                        TaskFilter.ALL -> "No pending tasks. Add one above, or ask LifePilot to suggest what to do next."
                        TaskFilter.COMPLETED -> "Complete a task to see it here."
                    },
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = Spacing.sm,
                    vertical = Spacing.sm,
                ),
            ) {
                items(tasks, key = { it.taskId }) { task ->
                    val isCompleted = selectedFilter == TaskFilter.COMPLETED
                    val isHighlighted = task.taskId == highlightedTaskId
                    val priorityColor = when (task.priority) {
                        TaskPriority.URGENT -> MaterialTheme.colorScheme.error
                        TaskPriority.HIGH -> Warning
                        TaskPriority.MEDIUM -> MaterialTheme.colorScheme.primary
                        TaskPriority.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    if (isCompleted) {
                        SwipeToReopenTask(onReopen = { onReopenTask(task.taskId) }) {
                            TaskCard(
                                title = task.title,
                                dueDateLabel = task.dueDate?.format(DateTimeFormatter.ofPattern("MMM d")),
                                priorityLabel = task.priority.name.lowercase()
                                    .replaceFirstChar { it.uppercase() },
                                priorityColor = priorityColor,
                                isCompleted = true,
                                onComplete = {},
                                onClick = {},
                                modifier = Modifier.padding(horizontal = Spacing.xs),
                            )
                        }
                    } else {
                        SwipeToCompleteTask(
                            onComplete = { onCompleteTask(task.taskId) },
                            enabled = true,
                        ) {
                            TaskCard(
                                title = task.title,
                                dueDateLabel = task.dueDate?.format(DateTimeFormatter.ofPattern("MMM d")),
                                priorityLabel = task.priority.name.lowercase()
                                    .replaceFirstChar { it.uppercase() },
                                priorityColor = priorityColor,
                                isCompleted = false,
                                onComplete = { onCompleteTask(task.taskId) },
                                onClick = { onEditTask(task) },
                                modifier = Modifier
                                    .padding(horizontal = Spacing.xs)
                                    .then(
                                        if (isHighlighted) {
                                            Modifier
                                                .padding(Spacing.xs)
                                                .border(
                                                    width = 2.dp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = RoundedCornerShape(12.dp),
                                                )
                                        } else {
                                            Modifier
                                        }
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Swipe helpers ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToCompleteTask(
    onComplete: () -> Unit,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        content()
        return
    }
    val haptic = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onComplete()
                true
            } else {
                false
            }
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.sm),
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "Complete",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToReopenTask(
    onReopen: () -> Unit,
    content: @Composable () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onReopen()
                true
            } else {
                false
            }
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = Spacing.md),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Text(
                        text = "Reopen",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Reopen",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        },
    ) {
        content()
    }
}
