package com.lifepilot.features.object.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifepilot.designsystem.components.EmptyState
import com.lifepilot.designsystem.components.StatusChip
import com.lifepilot.designsystem.components.TaskCard
import com.lifepilot.designsystem.components.TimelineCard
import com.lifepilot.designsystem.theme.Spacing
import com.lifepilot.designsystem.theme.StatusActive
import com.lifepilot.designsystem.theme.StatusArchived
import com.lifepilot.designsystem.theme.StatusExpired
import com.lifepilot.designsystem.theme.StatusRenewalDue
import com.lifepilot.domain.model.Document
import com.lifepilot.domain.model.LifeObject
import com.lifepilot.domain.model.MetadataEntry
import com.lifepilot.domain.model.ObjectStatus
import com.lifepilot.domain.model.Relationship
import com.lifepilot.domain.model.Task
import com.lifepilot.domain.model.TimelineEntry
import com.lifepilot.features.object.state.ObjectDetailTab
import com.lifepilot.features.object.viewmodel.ObjectDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDocument: (String) -> Unit,
    onUploadDocument: (String) -> Unit,
    onEditMetadata: (String) -> Unit = {},
    onArchived: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ObjectDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val allProfileObjects by viewModel.allProfileObjects.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Object") },
            text = {
                Text("This will permanently delete \"${uiState.lifeObject?.title}\". This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteObject { onNavigateBack() }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (uiState.showLinkObjectSheet) {
        LinkObjectSheet(
            availableObjects = allProfileObjects,
            onLink = { targetId, relType -> viewModel.linkObject(targetId, relType) },
            onDismiss = viewModel::hideLinkObjectSheet,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.lifeObject?.title ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    if (uiState.lifeObject != null && uiState.selectedTab == ObjectDetailTab.OVERVIEW) {
                        IconButton(
                            onClick = { onEditMetadata(uiState.lifeObject!!.objectId) }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit Details",
                            )
                        }
                    }
                    if (uiState.lifeObject != null) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "More options",
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Archive") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.archiveObject(onArchived)
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Delete",
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            if (uiState.selectedTab == ObjectDetailTab.DOCUMENTS && uiState.lifeObject != null) {
                FloatingActionButton(
                    onClick = { onUploadDocument(uiState.lifeObject!!.objectId) },
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Upload Document",
                    )
                }
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

        val obj = uiState.lifeObject
        if (obj == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Object not found")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val statusColor = when (obj.status) {
                    ObjectStatus.ACTIVE -> StatusActive
                    ObjectStatus.RENEWAL_DUE -> StatusRenewalDue
                    ObjectStatus.EXPIRED -> StatusExpired
                    else -> StatusArchived
                }
                StatusChip(
                    label = obj.status.name.replace("_", " "),
                    color = statusColor,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = obj.objectType,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val tabs = ObjectDetailTab.entries
            val selectedIndex = tabs.indexOf(uiState.selectedTab)
            ScrollableTabRow(
                selectedTabIndex = selectedIndex,
                containerColor = MaterialTheme.colorScheme.background,
                edgePadding = Spacing.md,
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedIndex == index,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text(tab.label) },
                    )
                }
            }

            HorizontalDivider()

            when (uiState.selectedTab) {
                ObjectDetailTab.OVERVIEW -> OverviewTab(
                    metadata = obj.metadata,
                    description = obj.description,
                )
                ObjectDetailTab.DOCUMENTS -> DocumentsTab(
                    documents = uiState.documents,
                    onDocumentClick = onNavigateToDocument,
                )
                ObjectDetailTab.TIMELINE -> TimelineTab(
                    timeline = uiState.timeline,
                )
                ObjectDetailTab.TASKS -> TasksTab(
                    tasks = uiState.tasks,
                    onCompleteTask = { taskId -> viewModel.completeTask(taskId) },
                )
                ObjectDetailTab.RELATIONSHIPS -> RelationshipsTab(
                    relationships = uiState.relationships,
                    relatedObjects = uiState.relatedObjects,
                    currentObjectId = obj.objectId,
                    onUnlink = { relationshipId -> viewModel.unlinkObject(relationshipId) },
                    onLink = { viewModel.showLinkObjectSheet() },
                )
            }
        }
    }
}

@Composable
private fun OverviewTab(
    metadata: List<MetadataEntry>,
    description: String?,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.md),
    ) {
        if (description != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(Spacing.md),
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.md))
            }
        }
        if (metadata.isNotEmpty()) {
            item {
                Text(
                    text = "DETAILS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.sm),
                )
            }
            items(metadata, key = { it.metadataId }) { entry ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    ) {
                        Text(
                            text = entry.fieldId
                                .replace("_", " ")
                                .replace(Regex("([A-Z])"), " $1")
                                .trim()
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.4f),
                        )
                        Text(
                            text = entry.value,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(0.6f),
                        )
                    }
                }
            }
        }
        if (description == null && metadata.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.Description,
                    title = "No details yet",
                    description = "Upload documents to extract metadata automatically.",
                )
            }
        }
    }
}

@Composable
private fun DocumentsTab(
    documents: List<Document>,
    onDocumentClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (documents.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.Description,
            title = "No documents",
            description = "Tap the + button to upload a document.",
            modifier = modifier.fillMaxSize(),
        )
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.md),
        ) {
            items(documents, key = { it.documentId }) { doc ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.xs),
                    onClick = { onDocumentClick(doc.documentId) },
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.weight(0.05f))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = doc.versions.firstOrNull()?.originalName ?: doc.documentType,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${doc.versions.size} version(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineTab(
    timeline: List<TimelineEntry>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.sm),
    ) {
        items(timeline.size) { index ->
            val entry = timeline[index]
            TimelineCard(
                title = entry.title,
                summary = entry.summary,
                dateLabel = entry.timestamp
                    .atZone(java.time.ZoneId.systemDefault())
                    .format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")),
                isLast = index == timeline.lastIndex,
                onClick = {},
            )
        }
    }
}

@Composable
private fun TasksTab(
    tasks: List<Task>,
    onCompleteTask: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tasks.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.Description,
            title = "No tasks",
            description = "Tasks are generated automatically when you add objects.",
            modifier = modifier.fillMaxSize(),
        )
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.md),
        ) {
            items(tasks, key = { it.taskId }) { task ->
                val isCompleted = task.status.name == "COMPLETED"
                val priorityColor = when (task.priority.name) {
                    "HIGH", "URGENT" -> MaterialTheme.colorScheme.error
                    "MEDIUM" -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.StartToEnd && !isCompleted) {
                            onCompleteTask(task.taskId)
                            true
                        } else false
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromEndToStart = false,
                    backgroundContent = {
                        val color = if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color, shape = MaterialTheme.shapes.medium)
                                .padding(horizontal = Spacing.md),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Complete",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    },
                    modifier = Modifier.padding(vertical = Spacing.xs),
                ) {
                    TaskCard(
                        title = task.title,
                        dueDateLabel = task.dueDate?.format(java.time.format.DateTimeFormatter.ofPattern("MMM d")),
                        priorityLabel = task.priority.name,
                        priorityColor = priorityColor,
                        isCompleted = isCompleted,
                        onComplete = { if (!isCompleted) onCompleteTask(task.taskId) },
                        onClick = {},
                    )
                }
            }
        }
    }
}

@Composable
private fun RelationshipsTab(
    relationships: List<Relationship>,
    relatedObjects: Map<String, LifeObject>,
    currentObjectId: String,
    onUnlink: (String) -> Unit,
    onLink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (relationships.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                EmptyState(
                    icon = Icons.Outlined.Link,
                    title = "No linked objects",
                    description = "Link this object to others to see connections.",
                )
                TextButton(onClick = onLink) { Text("Link an Object") }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.md),
        ) {
            items(relationships, key = { it.relationshipId }) { rel ->
                val otherId = if (rel.sourceObjectId == currentObjectId) rel.targetObjectId else rel.sourceObjectId
                val other = relatedObjects[otherId]
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.xs),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = other?.title ?: otherId,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = rel.relationshipType.replace("_", " ").lowercase()
                                    .replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onUnlink(rel.relationshipId) }) {
                            Icon(
                                imageVector = Icons.Outlined.LinkOff,
                                contentDescription = "Remove link",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}
