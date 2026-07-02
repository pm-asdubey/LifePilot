package com.lifepilot.features.library.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import com.lifepilot.designsystem.components.ObjectCardSkeleton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifepilot.designsystem.components.EmptyState
import com.lifepilot.designsystem.components.ObjectCard
import com.lifepilot.designsystem.icon.domainIcon
import com.lifepilot.designsystem.theme.Spacing
import com.lifepilot.designsystem.theme.StatusActive
import com.lifepilot.designsystem.theme.StatusArchived
import com.lifepilot.designsystem.theme.StatusExpired
import com.lifepilot.designsystem.theme.StatusRenewalDue
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.lifepilot.domain.model.DomainLifeState
import com.lifepilot.domain.model.ObjectStatus
import com.lifepilot.features.library.state.LibrarySortOrder
import com.lifepilot.features.library.viewmodel.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToObject: (String) -> Unit,
    onAddObject: () -> Unit,
    onNavigateToSearch: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    // All domain groups start collapsed; user taps the header to expand.
    var expandedDomains by remember { mutableStateOf(emptySet<String>()) }
    // Tracks which domain understanding cards have been dismissed for this session.
    var dismissedUnderstandingCards by remember { mutableStateOf(emptySet<String>()) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Remove ${uiState.selectedObjectIds.size} items?") },
            text = { Text("This will permanently remove the selected items and all their documents. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteSelected()
                    },
                ) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            if (uiState.isSelecting) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = viewModel::exitSelectionMode) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel selection")
                        }
                    },
                    title = {
                        Text(
                            text = "${uiState.selectedObjectIds.size} selected",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = "My Records",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSearch) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.Sort,
                                    contentDescription = "Sort",
                                    tint = if (uiState.sortOrder != LibrarySortOrder.UPDATED_RECENT)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                            ) {
                                LibrarySortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = order.label,
                                                color = if (uiState.sortOrder == order)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.onSurface,
                                            )
                                        },
                                        onClick = {
                                            viewModel.setSortOrder(order)
                                            showSortMenu = false
                                        },
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            }
        },
        floatingActionButton = {
            if (!uiState.isSelecting) {
                FloatingActionButton(
                    onClick = onAddObject,
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Track something new",
                    )
                }
            }
        },
        bottomBar = {
            if (uiState.isSelecting && uiState.selectedObjectIds.isNotEmpty()) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        Button(
                            onClick = viewModel::archiveSelected,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                Icons.Filled.Archive,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text("Archive")
                        }
                        Button(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text("Remove")
                        }
                    }
                }
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        if (uiState.isLoading) {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(vertical = com.lifepilot.designsystem.theme.Spacing.sm),
            ) {
                items(6) { ObjectCardSkeleton() }
            }
            return@Scaffold
        }

        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (uiState.domains.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = Spacing.md),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        item {
                            FilterChip(
                                selected = uiState.selectedDomain == null,
                                onClick = { viewModel.selectDomain(null) },
                                label = { Text("All") },
                            )
                        }
                        items(uiState.domains) { domain ->
                            FilterChip(
                                selected = uiState.selectedDomain == domain.domain,
                                onClick = { viewModel.selectDomain(domain.domain) },
                                label = { Text("${domain.displayName} (${domain.objectCount})") },
                            )
                        }
                    }
                }

                if (uiState.objects.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.FolderOpen,
                        title = "Your library is empty",
                        description = "Add your first record — a passport, job, property or insurance policy — and LifePilot will keep everything in order.",
                        actionLabel = "Add a record",
                        onAction = onAddObject,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    // Group by domain when no domain filter is active (Object Tree view)
                    val selectedDomainFilter = uiState.selectedDomain
                    val domainLifeStateMap = uiState.domains.associate { it.domain to it.lifeState }
                    val domainGroups: List<Pair<String, List<com.lifepilot.domain.model.LifeObject>>> =
                        if (selectedDomainFilter == null) {
                            uiState.objects.groupBy { it.domain }.entries
                                .sortedBy { it.key }
                                .map { it.key to it.value }
                        } else {
                            listOf(selectedDomainFilter to uiState.objects)
                        }

                    LazyColumn(
                        contentPadding = PaddingValues(
                            horizontal = Spacing.md,
                            vertical = Spacing.sm,
                        ),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier.weight(1f),
                    ) {
                        // When a specific domain is selected, show its understanding card at top.
                        // This fixes ISSUE-011 where the card was hidden in filtered view.
                        if (selectedDomainFilter != null) {
                            val filteredLifeState = domainLifeStateMap[selectedDomainFilter]
                            if (filteredLifeState != null && filteredLifeState.currentSituation.isNotBlank() &&
                                selectedDomainFilter !in dismissedUnderstandingCards) {
                                item(key = "understanding_filtered") {
                                    DomainUnderstandingCard(
                                        lifeState = filteredLifeState,
                                        onDismiss = {
                                            dismissedUnderstandingCards =
                                                dismissedUnderstandingCards + selectedDomainFilter
                                        },
                                    )
                                }
                            }
                        }

                        domainGroups.forEach { (domain, objects) ->
                            if (selectedDomainFilter == null) {
                                val isExpanded = domain in expandedDomains
                                stickyHeader(key = "header_$domain") {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = androidx.compose.ui.Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.background)
                                            .clickable {
                                                expandedDomains = if (isExpanded) {
                                                    expandedDomains - domain
                                                } else {
                                                    expandedDomains + domain
                                                }
                                            }
                                            .padding(top = Spacing.sm, bottom = Spacing.xs),
                                    ) {
                                        Icon(
                                            imageVector = domainIcon(domain),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = androidx.compose.ui.Modifier.size(20.dp),
                                        )
                                        Spacer(modifier = androidx.compose.ui.Modifier.width(Spacing.xs))
                                        Text(
                                            text = domain,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = androidx.compose.ui.Modifier.weight(1f),
                                        )
                                        Text(
                                            text = "${objects.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        )
                                        Spacer(modifier = androidx.compose.ui.Modifier.width(Spacing.xs))
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = androidx.compose.ui.Modifier.size(18.dp),
                                        )
                                    }
                                }
                                // Domain understanding card — only shown when domain is expanded.
                                val lifeState = domainLifeStateMap[domain]
                                if (isExpanded && lifeState != null && lifeState.currentSituation.isNotBlank() &&
                                    domain !in dismissedUnderstandingCards) {
                                    item(key = "understanding_$domain") {
                                        DomainUnderstandingCard(
                                            lifeState = lifeState,
                                            onDismiss = {
                                                dismissedUnderstandingCards =
                                                    dismissedUnderstandingCards + domain
                                            },
                                        )
                                    }
                                }
                            }
                            // Objects only show when domain is expanded (or when a domain filter is active).
                            if (selectedDomainFilter != null || domain in expandedDomains) {
                                items(objects, key = { it.objectId }) { obj ->
                                    val statusColor = when (obj.status) {
                                        ObjectStatus.ACTIVE -> StatusActive
                                        ObjectStatus.RENEWAL_DUE -> StatusRenewalDue
                                        ObjectStatus.EXPIRED -> StatusExpired
                                        ObjectStatus.ARCHIVED -> StatusArchived
                                        ObjectStatus.INACTIVE -> MaterialTheme.colorScheme.onSurfaceVariant
                                        ObjectStatus.DRAFT -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    val statusLabel = when (obj.status) {
                                        ObjectStatus.ACTIVE -> "Active"
                                        ObjectStatus.RENEWAL_DUE -> "Renewal due"
                                        ObjectStatus.EXPIRED -> "Expired"
                                        ObjectStatus.ARCHIVED -> "Archived"
                                        ObjectStatus.INACTIVE -> "Inactive"
                                        ObjectStatus.DRAFT -> "Draft"
                                    }
                                    val isSelected = obj.objectId in uiState.selectedObjectIds
                                    ObjectCard(
                                        title = obj.title,
                                        subtitle = obj.description,
                                        objectType = obj.objectType,
                                        domain = obj.domain,
                                        statusLabel = statusLabel,
                                        statusColor = statusColor,
                                        icon = domainIcon(obj.domain),
                                        isSelected = isSelected,
                                        onClick = {
                                            if (uiState.isSelecting) {
                                                viewModel.toggleObjectSelection(obj.objectId)
                                            } else {
                                                onNavigateToObject(obj.objectId)
                                            }
                                        },
                                        onLongClick = {
                                            if (!uiState.isSelecting) {
                                                viewModel.enterSelectionMode(obj.objectId)
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DomainUnderstandingCard(
    lifeState: DomainLifeState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Current Understanding",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(onClick = onDismiss),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = lifeState.currentSituation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (lifeState.recommendations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "→ ${lifeState.recommendations.first()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
