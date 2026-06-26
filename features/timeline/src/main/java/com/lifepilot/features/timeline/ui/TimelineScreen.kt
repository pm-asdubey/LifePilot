package com.lifepilot.features.timeline.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifepilot.designsystem.components.EmptyState
import com.lifepilot.designsystem.components.TimelineCard
import com.lifepilot.designsystem.theme.Spacing
import com.lifepilot.features.timeline.viewmodel.TimelineViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    onNavigateToObject: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TimelineViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Timeline",
                        style = MaterialTheme.typography.titleLarge,
                    )
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

        if (uiState.entries.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.Timeline,
                title = "No activity yet",
                description = "Your timeline will show all changes to your objects.",
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
            if (uiState.availableSourceTypes.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedSourceType == null,
                            onClick = { viewModel.selectFilter(null) },
                            label = { Text("All (${uiState.entries.size})") },
                        )
                    }
                    items(uiState.availableSourceTypes) { sourceType ->
                        val count = uiState.entries.count { it.sourceType.name == sourceType }
                        FilterChip(
                            selected = uiState.selectedSourceType == sourceType,
                            onClick = { viewModel.selectFilter(sourceType) },
                            label = {
                                Text(
                                    sourceType
                                        .replace("_", " ")
                                        .lowercase()
                                        .replaceFirstChar { it.uppercase() } + " ($count)"
                                )
                            },
                        )
                    }
                }
            }

            val displayEntries = uiState.filteredEntries.ifEmpty { uiState.entries }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = Spacing.sm,
                    vertical = Spacing.md,
                ),
            ) {
                itemsIndexed(
                    items = displayEntries,
                    key = { _, entry -> entry.timelineId },
                ) { index, entry ->
                    TimelineCard(
                        title = entry.title,
                        summary = entry.summary,
                        dateLabel = entry.timestamp
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")),
                        isLast = index == displayEntries.lastIndex,
                        onClick = {
                            entry.objectId?.let { onNavigateToObject(it) }
                        },
                    )
                }
            }
        }
    }
}
