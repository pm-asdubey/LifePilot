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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.lifepilot.designsystem.components.PlannerSkeleton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifepilot.designsystem.components.EmptyState
import com.lifepilot.designsystem.components.SectionHeader
import com.lifepilot.designsystem.components.TaskCard
import com.lifepilot.designsystem.theme.Spacing
import com.lifepilot.designsystem.theme.Warning
import com.lifepilot.domain.model.Goal
import com.lifepilot.domain.model.Task
import com.lifepilot.domain.model.TaskPriority
import com.lifepilot.features.planner.state.TaskFilter
import com.lifepilot.features.planner.viewmodel.PlannerViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    onNavigateToObject: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlannerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val highlightedTaskId = uiState.highlightedTaskId
    var selectedTab by remember { mutableIntStateOf(if (highlightedTaskId.isNullOrBlank()) 0 else 1) }
    val tabs = listOf("Goals", "Tasks")
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

    if (uiState.showCreateGoalSheet) {
        CreateGoalSheet(
            onDismiss = viewModel::hideCreateGoalSheet,
            onCreateGoal = { title, description, deadline ->
                viewModel.createGoal(title, description, deadline)
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

    uiState.editingGoal?.let { goal ->
        GoalDetailSheet(
            goal = goal,
            onDismiss = viewModel::closeEditGoal,
            onSave = { goalId, title, description, deadline ->
                viewModel.saveEditedGoal(goalId, title, description, deadline)
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
                    IconButton(onClick = viewModel::showCreateGoalSheet) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add goal",
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
                    onClick = viewModel::showCreateGoalSheet,
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Create goal")
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
                0 -> GoalsTab(
                    goals = uiState.activeGoals,
                    onCompleteGoal = viewModel::completeGoal,
                    onDismissGoal = viewModel::dismissGoal,
                    onGoalClick = viewModel::openEditGoal,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalsTab(
    goals: List<Goal>,
    onCompleteGoal: (String) -> Unit,
    onDismissGoal: (String) -> Unit,
    onGoalClick: (Goal) -> Unit,  // opens edit sheet
    modifier: Modifier = Modifier,
) {
    if (goals.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            EmptyState(
                icon = Icons.Outlined.EmojiEvents,
                title = "No goals yet",
                description = "Set a goal and LifePilot will help you stay on track with suggested tasks and timely reminders.",
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(goals, key = { it.goalId }) { goal ->
            GoalCard(
                goal = goal,
                onComplete = { onCompleteGoal(goal.goalId) },
                onDismiss = { onDismissGoal(goal.goalId) },
                onClick = { onGoalClick(goal) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalCard(
    goal: Goal,
    onComplete: () -> Unit,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val daysUntilDeadline = goal.deadline?.let { ChronoUnit.DAYS.between(today, it) }
    val deadlineColor = when {
        daysUntilDeadline == null -> MaterialTheme.colorScheme.onSurfaceVariant
        daysUntilDeadline < 0 -> MaterialTheme.colorScheme.error
        daysUntilDeadline <= 7 -> Warning
        else -> MaterialTheme.colorScheme.onSurfaceVariant
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
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = goal.title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row {
                    IconButton(
                        onClick = onComplete,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = "Complete",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            val goalDeadline = goal.deadline
            if (goalDeadline != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        daysUntilDeadline != null && daysUntilDeadline < 0 -> "Overdue"
                        daysUntilDeadline != null && daysUntilDeadline == 0L -> "Due today"
                        daysUntilDeadline != null && daysUntilDeadline <= 7 -> "$daysUntilDeadline days left"
                        else -> "Due ${goalDeadline.format(DateTimeFormatter.ofPattern("MMM d"))}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = deadlineColor,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                LinearProgressIndicator(
                    progress = { goal.progress / 100f },
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${goal.progress}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

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
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                            )
                                    } else {
                                        Modifier
                                    }
                                ),
                        )
                    }
                    } // end else (non-completed)
                }
            }
        }
    }
}

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
                    .padding(horizontal = Spacing.sm),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Reopen",
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
        },
    ) {
        content()
    }
}
