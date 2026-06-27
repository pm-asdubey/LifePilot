package com.lifepilot.features.search.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifepilot.designsystem.components.EmptyState
import com.lifepilot.designsystem.components.SectionHeader
import com.lifepilot.designsystem.icon.domainIcon
import com.lifepilot.designsystem.theme.Spacing
import com.lifepilot.domain.model.SearchEntityType
import com.lifepilot.features.search.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateToObject: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.md),
        ) {
            SearchBar(
                query = uiState.query,
                onQueryChange = viewModel::onQueryChange,
                onSearch = viewModel::onSearch,
                active = false,
                onActiveChange = {},
                placeholder = { Text("Search objects, documents, tasks...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    if (uiState.query.isNotBlank()) {
                        IconButton(onClick = viewModel::clearSearch) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear",
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                content = {},
            )

            if (uiState.isSearching) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
                return@Scaffold
            }

            if (uiState.query.isBlank()) {
                if (uiState.recentSearches.isNotEmpty()) {
                    SectionHeader(
                        title = "RECENT SEARCHES",
                        modifier = Modifier.padding(top = Spacing.md),
                    )
                    LazyColumn(
                        contentPadding = PaddingValues(top = Spacing.sm),
                        modifier = Modifier.weight(1f),
                    ) {
                        items(uiState.recentSearches) { query ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.onQueryChange(query) }
                                    .padding(
                                        horizontal = Spacing.md,
                                        vertical = Spacing.sm,
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(Spacing.md))
                                Text(
                                    text = query,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                } else {
                    EmptyState(
                        icon = Icons.Outlined.Search,
                        title = "Search your life",
                        description = "Find any document, object, task or event.",
                    )
                }
            } else if (uiState.results.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Search,
                    title = "No results",
                    description = "No matches found for \"${uiState.query}\".",
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(top = Spacing.sm),
                    modifier = Modifier.weight(1f),
                ) {
                    items(uiState.results, key = { it.entityId }) { result ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (result.entityType == SearchEntityType.OBJECT) {
                                        onNavigateToObject(result.entityId)
                                        viewModel.onSearch(uiState.query)
                                    }
                                }
                                .padding(
                                    horizontal = Spacing.md,
                                    vertical = Spacing.sm,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (result.domain != null) {
                                Icon(
                                    imageVector = domainIcon(result.domain),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(Spacing.sm))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = result.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (result.subtitle != null) {
                                    Text(
                                        text = result.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (result.objectType != null) {
                                Text(
                                    text = result.objectType,
                                    style = MaterialTheme.typography.labelSmall,
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
