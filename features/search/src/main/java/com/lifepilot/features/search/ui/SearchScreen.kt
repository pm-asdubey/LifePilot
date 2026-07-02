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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifepilot.designsystem.components.EmptyState
import com.lifepilot.designsystem.components.SectionHeader
import com.lifepilot.designsystem.icon.domainIcon
import com.lifepilot.designsystem.theme.Spacing
import com.lifepilot.domain.model.SearchEntityType
import com.lifepilot.features.search.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    onNavigateToObject: (String) -> Unit,
    onNavigateToTask: (String) -> Unit,
    onNavigateToConversation: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.md),
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
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
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        viewModel.onSearch(uiState.query)
                    },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.sm),
            )

            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isSearching -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    }

                    uiState.query.isBlank() && uiState.recentSearches.isNotEmpty() -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            SectionHeader(
                                title = "RECENT",
                                modifier = Modifier.padding(top = Spacing.md),
                            )
                            LazyColumn(
                                contentPadding = PaddingValues(top = Spacing.sm),
                                modifier = Modifier.fillMaxSize(),
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
                        }
                    }

                    uiState.query.isBlank() -> {
                        EmptyState(
                            icon = Icons.Outlined.Search,
                            title = "Search your life",
                            description = "Search across all your documents, records and tasks.",
                        )
                    }

                    uiState.results.isEmpty() -> {
                        EmptyState(
                            icon = Icons.Outlined.Search,
                            title = "Nothing found",
                            description = "No matches for \"${uiState.query}\".",
                        )
                    }

                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(top = Spacing.sm),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(uiState.results, key = { it.entityId }) { result ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.onSearch(uiState.query)
                                            when (result.entityType) {
                                                SearchEntityType.OBJECT -> onNavigateToObject(result.entityId)
                                                SearchEntityType.TASK -> onNavigateToTask(result.entityId)
                                                SearchEntityType.CONVERSATION -> onNavigateToConversation(result.entityId)
                                                else -> onNavigateToObject(result.entityId)
                                            }
                                        }
                                        .padding(
                                            horizontal = Spacing.md,
                                            vertical = Spacing.sm,
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    val domain = result.domain
                                    if (domain != null) {
                                        Icon(
                                            imageVector = domainIcon(domain),
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
                                        val subtitle = result.subtitle
                                        if (subtitle != null) {
                                            Text(
                                                text = subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    val typeLabel = when (result.entityType) {
                                        SearchEntityType.GOAL -> "Goal"
                                        SearchEntityType.TASK -> "Task"
                                        SearchEntityType.CONVERSATION -> "Chat"
                                        SearchEntityType.OBJECT -> result.objectType?.replaceFirstChar { it.uppercase() }
                                        SearchEntityType.DOCUMENT -> "Doc"
                                        SearchEntityType.EVENT -> "Event"
                                    }
                                    if (typeLabel != null) {
                                        Text(
                                            text = typeLabel,
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
    }
}
