package com.lifepilot.features.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifepilot.designsystem.components.SectionHeader
import com.lifepilot.designsystem.theme.Spacing
import com.lifepilot.features.settings.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.showCreateProfile) {
        AlertDialog(
            onDismissRequest = viewModel::hideCreateProfile,
            title = { Text("Create Profile") },
            text = {
                OutlinedTextField(
                    value = uiState.newProfileName,
                    onValueChange = viewModel::onNewProfileNameChange,
                    label = { Text("Profile Name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::createProfile) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideCreateProfile) { Text("Cancel") }
            },
        )
    }

    if (uiState.showAiConfig) {
        AlertDialog(
            onDismissRequest = viewModel::hideAiConfig,
            title = { Text("AI Provider") },
            text = {
                Column {
                    OutlinedTextField(
                        value = uiState.aiProvider,
                        onValueChange = viewModel::onAiProviderChange,
                        label = { Text("Provider (anthropic / openai / gemini)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    OutlinedTextField(
                        value = uiState.aiApiKey,
                        onValueChange = viewModel::onAiApiKeyChange,
                        label = { Text("API Key") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    OutlinedTextField(
                        value = uiState.aiModel,
                        onValueChange = viewModel::onAiModelChange,
                        label = { Text("Model (e.g. claude-sonnet-4-6)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::saveAiConfig) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::clearAiConfig) { Text("Clear") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            item {
                Spacer(modifier = Modifier.height(Spacing.md))
                SectionHeader(
                    title = "PROFILES",
                    actionLabel = "Add",
                    onAction = viewModel::showCreateProfile,
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
            }

            items(uiState.profiles, key = { it.profileId }) { profile ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (profile.isPrimary)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = 4.dp)
                        .clickable { viewModel.switchProfile(profile.profileId) },
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = null,
                            tint = if (profile.isPrimary)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(40.dp),
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = Spacing.md),
                        ) {
                            Text(
                                text = profile.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (profile.isPrimary)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        if (profile.isPrimary) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Active",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }

            if (uiState.profiles.isEmpty() && !uiState.isLoading) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.xxl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))
                        Text(
                            text = "No profiles yet",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        TextButton(onClick = viewModel::showCreateProfile) {
                            Text("Create your first profile")
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(Spacing.lg))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(Spacing.md))
                SectionHeader(title = "AI CONFIGURATION")
                Spacer(modifier = Modifier.height(Spacing.sm))
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md)
                        .clickable { viewModel.showAiConfig() },
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = Spacing.md),
                        ) {
                            Text(
                                text = "AI Provider",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (uiState.aiProvider.isNotBlank())
                                    "${uiState.aiProvider} • ${uiState.aiModel.ifBlank { "default model" }}"
                                else
                                    "Not configured — tap to set up",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(Spacing.xxl)) }
        }
    }
}
