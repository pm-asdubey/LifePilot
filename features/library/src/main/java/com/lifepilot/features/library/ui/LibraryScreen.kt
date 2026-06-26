package com.lifepilot.features.library.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.DriveEta
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifepilot.designsystem.components.EmptyState
import com.lifepilot.designsystem.components.ObjectCard
import com.lifepilot.designsystem.theme.Spacing
import com.lifepilot.designsystem.theme.StatusActive
import com.lifepilot.designsystem.theme.StatusArchived
import com.lifepilot.designsystem.theme.StatusExpired
import com.lifepilot.designsystem.theme.StatusRenewalDue
import com.lifepilot.domain.model.ObjectStatus
import com.lifepilot.features.library.viewmodel.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToObject: (String) -> Unit,
    onAddObject: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Library",
                        style = MaterialTheme.typography.titleLarge,
                    )
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
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
                    title = "No objects yet",
                    description = "Add your first document or object to get started.",
                    actionLabel = "Add Object",
                    onAction = onAddObject,
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        horizontal = Spacing.md,
                        vertical = Spacing.sm,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(uiState.objects, key = { it.objectId }) { obj ->
                        val statusColor = when (obj.status) {
                            ObjectStatus.ACTIVE -> StatusActive
                            ObjectStatus.RENEWAL_DUE -> StatusRenewalDue
                            ObjectStatus.EXPIRED -> StatusExpired
                            ObjectStatus.ARCHIVED -> StatusArchived
                            ObjectStatus.DRAFT -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        ObjectCard(
                            title = obj.title,
                            subtitle = obj.description,
                            objectType = obj.objectType,
                            domain = obj.domain,
                            statusLabel = obj.status.name.replace("_", " "),
                            statusColor = statusColor,
                            icon = domainIcon(obj.domain),
                            onClick = { onNavigateToObject(obj.objectId) },
                        )
                    }
                }
            }
        }
    }
}

private fun domainIcon(domain: String): ImageVector = when (domain.lowercase()) {
    "identity" -> Icons.Outlined.Badge
    "career" -> Icons.Outlined.Work
    "property" -> Icons.Outlined.Home
    "vehicle" -> Icons.Outlined.DriveEta
    "finance" -> Icons.Outlined.AccountBalance
    else -> Icons.Outlined.FolderOpen
}
