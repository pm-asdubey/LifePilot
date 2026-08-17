package com.lifepilot.features.objectdetail.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.lifepilot.domain.model.MetadataSource
import com.lifepilot.domain.model.ObjectStatus

import com.lifepilot.domain.model.Task
import com.lifepilot.domain.model.TimelineEntry
import com.lifepilot.domain.model.VerificationStatus
import com.lifepilot.features.objectdetail.state.ObjectDetailTab
import com.lifepilot.features.objectdetail.viewmodel.ObjectDetailViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDocument: (String) -> Unit,
    onUploadDocument: (String) -> Unit,
    onEditMetadata: (String) -> Unit = {},
    onVerifyDocument: (objectId: String, versionId: String) -> Unit = { _, _ -> },
    onArchived: () -> Unit = {},
    onAskAiAboutObject: (objectId: String, objectType: String, title: String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
    viewModel: ObjectDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Remove this record?") },
            text = {
                Text("\"${uiState.lifeObject?.title}\" and all its documents will be permanently removed.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteObject { onNavigateBack() }
                    }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
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
                                contentDescription = "Edit details",
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
                                            "Remove",
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
                        contentDescription = "Add document",
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
                Text("Record not found")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // AI found details banner
            val pendingVersionId = uiState.pendingVerificationVersionId
            if (pendingVersionId != null) {
                AiFoundDetailsBanner(
                    onReview = {
                        onVerifyDocument(obj.objectId, pendingVersionId)
                    },
                    onDismiss = viewModel::dismissPendingVerification,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

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
                val statusLabel = when (obj.status) {
                    ObjectStatus.ACTIVE -> "Active"
                    ObjectStatus.RENEWAL_DUE -> "Renewal due"
                    ObjectStatus.EXPIRED -> "Expired"
                    ObjectStatus.ARCHIVED -> "Archived"
                    ObjectStatus.INACTIVE -> "Inactive"
                    ObjectStatus.DRAFT -> "Draft"
                }
                StatusChip(
                    label = statusLabel,
                    color = statusColor,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = obj.objectType.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val temporalLabel = temporalContextLabel(obj.objectType, obj.metadata)
            if (temporalLabel != null) {
                Text(
                    text = temporalLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = 2.dp),
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
            }
        }
    }
}

@Composable
private fun AiFoundDetailsBanner(
    onReview: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        shape = MaterialTheme.shapes.small,
        modifier = modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI found details in your document",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = "Tap to review and save what looks correct.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                )
            }
            TextButton(onClick = onReview) {
                Text("Review", color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(16.dp),
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
        // AI Context card — surfaced prominently when present
        val aiContext = metadata.firstOrNull { it.fieldId == "ai_context" }
        if (aiContext != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text(
                                text = "AI Context",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            )
                        }
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = aiContext.value,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.sm))
            }
        }

        val regularMetadata = metadata.filter { it.fieldId != "ai_context" }
        if (regularMetadata.isNotEmpty()) {
            item {
                Text(
                    text = "Details",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.sm),
                )
            }
            items(regularMetadata, key = { it.metadataId }) { entry ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
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
                        ProvenanceBadge(entry)
                    }
                }
            }
        }
        if (description == null && metadata.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.Description,
                    title = "No details yet",
                    description = "Upload documents to fill this in automatically.",
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
            title = "No documents yet",
            description = "Tap the + button to add a document.",
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
                            if (doc.versions.size > 1) {
                                Text(
                                    text = "${doc.versions.size} versions",
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
            title = "Nothing to do",
            description = "Tasks will appear here as you add information.",
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
                val haptic = LocalHapticFeedback.current
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.StartToEnd && !isCompleted) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                                contentDescription = "Mark done",
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
                        priorityLabel = task.priority.name.lowercase().replaceFirstChar { it.uppercase() },
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
private fun ProvenanceBadge(entry: MetadataEntry) {
    val (label, color) = when (entry.verificationStatus) {
        VerificationStatus.VERIFIED -> "Confirmed" to MaterialTheme.colorScheme.tertiary
        VerificationStatus.REJECTED -> "Dismissed" to MaterialTheme.colorScheme.error
        VerificationStatus.UNVERIFIED -> when (entry.source) {
            MetadataSource.USER -> return // user-entered values need no badge
            MetadataSource.OCR -> "Scanned · unconfirmed" to MaterialTheme.colorScheme.onSurfaceVariant
            MetadataSource.AI_EXTRACTED -> "AI · unconfirmed" to MaterialTheme.colorScheme.onSurfaceVariant
            MetadataSource.SYSTEM -> return
        }
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color.copy(alpha = 0.7f),
        modifier = Modifier.padding(top = 2.dp),
    )
}

// Maps an object type + its metadata to a one-line contextual temporal phrase.
// Returns null when the object type has no meaningful temporal framing.
private fun temporalContextLabel(objectType: String, metadata: List<MetadataEntry>): String? {
    val today = LocalDate.now()

    fun metaDate(vararg fieldIds: String): LocalDate? {
        val parsers = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d MMM yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        )
        for (fieldId in fieldIds) {
            val raw = metadata.firstOrNull {
                it.fieldId.equals(fieldId, ignoreCase = true) ||
                    it.fieldId.replace("_", "").equals(fieldId.replace("_", ""), ignoreCase = true)
            }?.value?.trim() ?: continue
            for (fmt in parsers) {
                try { return LocalDate.parse(raw, fmt) } catch (_: DateTimeParseException) {}
            }
        }
        return null
    }

    fun daysAgo(date: LocalDate): Long = ChronoUnit.DAYS.between(date, today)
    fun daysUntil(date: LocalDate): Long = ChronoUnit.DAYS.between(today, date)

    fun pastPhrase(date: LocalDate, verb: String): String {
        val days = daysAgo(date)
        val suffix = when {
            days == 0L -> "today"
            days == 1L -> "yesterday"
            days < 30 -> "$days days ago"
            days < 365 -> "${days / 30} months ago"
            else -> "${days / 365} year${if (days / 365 > 1) "s" else ""} ago"
        }
        return "$verb ${date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))} · $suffix"
    }

    fun futurePhrase(date: LocalDate, verb: String): String {
        val days = daysUntil(date)
        val suffix = when {
            days == 0L -> "today"
            days == 1L -> "tomorrow"
            days < 30 -> "in $days days"
            days < 365 -> "in ${days / 30} months"
            else -> "in ${days / 365} year${if (days / 365 > 1) "s" else ""}"
        }
        return "$verb ${date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))} · $suffix"
    }

    return when (objectType.lowercase().replace(" ", "").replace("_", "")) {
        "job", "employment" -> {
            val start = metaDate("start_date", "startDate", "joining_date", "joiningDate", "date_of_joining")
            val end = metaDate("end_date", "endDate", "leaving_date", "leavingDate")
            when {
                end != null && end <= today -> pastPhrase(end, "Left")
                start != null && start > today -> futurePhrase(start, "Joining")
                start != null -> pastPhrase(start, "Joined")
                else -> null
            }
        }
        "travel" -> {
            val departure = metaDate("departure_date", "departureDate", "start_date", "startDate")
            val returnDate = metaDate("return_date", "returnDate", "end_date", "endDate")
            when {
                returnDate != null && returnDate < today -> pastPhrase(departure ?: returnDate, "Travelled")
                departure != null && departure > today -> futurePhrase(departure, "Departing")
                departure != null -> "Currently travelling · departed ${departure.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}"
                else -> null
            }
        }
        "marriage" -> {
            val date = metaDate("marriage_date", "marriageDate", "wedding_date", "weddingDate")
            when {
                date == null -> null
                date > today -> futurePhrase(date, "Getting married on")
                else -> pastPhrase(date, "Married on")
            }
        }
        "childbirth" -> {
            val date = metaDate("birth_date", "birthDate", "date_of_birth", "dateOfBirth", "due_date", "dueDate")
            when {
                date == null -> null
                date > today -> futurePhrase(date, "Due")
                else -> pastPhrase(date, "Born")
            }
        }
        "deathofarelative", "death_of_relative" -> {
            val date = metaDate("date_of_death", "dateOfDeath", "death_date", "deathDate")
            if (date != null) pastPhrase(date, "Passed away on") else null
        }
        "interview" -> {
            val date = metaDate("interview_date", "interviewDate", "scheduled_date", "scheduledDate")
            when {
                date == null -> null
                date > today -> futurePhrase(date, "Interview on")
                else -> pastPhrase(date, "Interviewed on")
            }
        }
        "education" -> {
            val start = metaDate("start_date", "startDate", "enrollment_date", "enrollmentDate")
            val end = metaDate("end_date", "endDate", "graduation_date", "graduationDate")
            when {
                end != null && end <= today -> pastPhrase(end, "Graduated")
                start != null && start > today -> futurePhrase(start, "Starting")
                start != null -> pastPhrase(start, "Enrolled")
                else -> null
            }
        }
        "visa" -> {
            val expiry = metaDate("expiry_date", "expiryDate")
            val issue = metaDate("issue_date", "issueDate")
            val fmt = DateTimeFormatter.ofPattern("d MMM yyyy")
            when (val status = com.lifepilot.domain.model.ExpiryStatus.of(expiry, today)) {
                com.lifepilot.domain.model.ExpiryStatus.Expired -> "Expired ${expiry!!.format(fmt)}"
                is com.lifepilot.domain.model.ExpiryStatus.ExpiringSoon ->
                    "Expiring ${expiry!!.format(fmt)} · ${status.daysLeft} days left"
                else -> issue?.let { "Valid since ${it.format(fmt)}" }
            }
        }
        "property", "rentagreement" -> {
            val start = metaDate("possession_date", "possessionDate", "lease_start", "leaseStart", "start_date", "startDate")
            when {
                start == null -> null
                start > today -> futurePhrase(start, "Possession")
                else -> pastPhrase(start, "Owned since")
            }
        }
        else -> null
    }
}
