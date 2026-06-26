package com.lifepilot.features.object.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifepilot.designsystem.components.EmptyState
import com.lifepilot.designsystem.components.StatusChip
import com.lifepilot.designsystem.components.TimelineCard
import com.lifepilot.designsystem.theme.Spacing
import com.lifepilot.designsystem.theme.StatusActive
import com.lifepilot.designsystem.theme.StatusArchived
import com.lifepilot.designsystem.theme.StatusExpired
import com.lifepilot.designsystem.theme.StatusRenewalDue
import com.lifepilot.domain.model.Document
import com.lifepilot.domain.model.MetadataEntry
import com.lifepilot.domain.model.ObjectStatus
import com.lifepilot.domain.model.TimelineEntry
import com.lifepilot.features.object.state.ObjectDetailTab
import com.lifepilot.features.object.viewmodel.ObjectDetailViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDocument: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ObjectDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
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
            // Status header
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

            // Tabs
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
                            text = entry.fieldId.replace(Regex("([A-Z])"), " $1").trim(),
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
            description = "Upload documents to this object.",
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
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")),
                isLast = index == timeline.lastIndex,
                onClick = {},
            )
        }
    }
}

@Composable
private fun TasksTab(
    tasks: List<com.lifepilot.domain.model.Task>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.md),
    ) {
        items(tasks, key = { it.taskId }) { task ->
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = Spacing.sm),
            )
        }
    }
}

