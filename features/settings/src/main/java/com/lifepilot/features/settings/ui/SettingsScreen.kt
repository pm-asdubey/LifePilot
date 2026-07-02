package com.lifepilot.features.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifepilot.designsystem.components.SectionHeader
import com.lifepilot.designsystem.theme.Spacing
import com.lifepilot.domain.model.UpdateStatus
import com.lifepilot.features.settings.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importData(it) }
    }

    if (uiState.showCreateProfile) {
        AlertDialog(
            onDismissRequest = viewModel::hideCreateProfile,
            title = { Text("Create Profile") },
            text = {
                OutlinedTextField(
                    value = uiState.newProfileName,
                    onValueChange = viewModel::onNewProfileNameChange,
                    label = { Text("Name") },
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

    uiState.profileToEdit?.let { profile ->
        AlertDialog(
            onDismissRequest = viewModel::hideEditProfile,
            title = { Text("Rename Profile") },
            text = {
                OutlinedTextField(
                    value = uiState.editProfileName,
                    onValueChange = viewModel::onEditProfileNameChange,
                    label = { Text("Name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::saveProfileEdit) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideEditProfile) { Text("Cancel") }
            },
        )
    }

    uiState.profileToDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = viewModel::hideDeleteProfile,
            title = { Text("Delete Profile") },
            text = {
                Text("Delete \"${profile.displayName}\"? This cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = viewModel::deleteProfile) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideDeleteProfile) { Text("Cancel") }
            },
        )
    }

    if (uiState.showAiConfig) {
        AlertDialog(
            onDismissRequest = viewModel::hideAiConfig,
            title = { Text("Connect to AI") },
            text = {
                Column {
                    OutlinedTextField(
                        value = uiState.aiProvider,
                        onValueChange = viewModel::onAiProviderChange,
                        label = { Text("Provider") },
                        placeholder = { Text("anthropic / nvidia") },
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
                        label = { Text("Model") },
                        placeholder = { Text("claude-sonnet-4-6 / meta/llama-3.1-70b-instruct") },
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
                        } else {
                            Row {
                                IconButton(onClick = { viewModel.showEditProfile(profile) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = "Rename",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { viewModel.showDeleteProfile(profile) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
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
                SectionHeader(title = "INTELLIGENCE")
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
                                text = "Connect to AI",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (uiState.aiProvider.isNotBlank())
                                    "${uiState.aiProvider} • ${uiState.aiModel.ifBlank { "default model" }}"
                                else
                                    "Not connected — tap to set up",
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

            item {
                Spacer(modifier = Modifier.height(Spacing.lg))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(Spacing.md))
                SectionHeader(title = "BACKUP & RESTORE")
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
                        .padding(horizontal = Spacing.md),
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Upload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Back up data",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Save a copy of all your records",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (uiState.isExporting) {
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        if (uiState.exportResult != null) {
                            val result = uiState.exportResult!!
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Text(
                                text = "Done: ${result.objectCount} records, ${result.documentCount} documents, ${result.taskCount} tasks",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            if (uiState.exportShareUri != null) {
                                Spacer(modifier = Modifier.height(Spacing.xs))
                                TextButton(onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_STREAM, uiState.exportShareUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(intent, "Share backup file")
                                    )
                                }) {
                                    Text("Share backup file")
                                }
                            }
                        }
                        if (uiState.exportError != null) {
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Text(
                                text = "Backup failed: ${uiState.exportError}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        TextButton(
                            onClick = viewModel::exportData,
                            enabled = !uiState.isExporting && uiState.activeProfile != null,
                        ) {
                            Text("Back up now")
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(Spacing.md))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md),
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Restore data",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Restore records from a backup file",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (uiState.isImporting) {
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        if (uiState.importResult != null) {
                            val result = uiState.importResult!!
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Text(
                                text = "Done: ${result.objectsImported} records restored" +
                                    if (result.objectsSkipped > 0) ", ${result.objectsSkipped} skipped" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (uiState.importError != null) {
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Text(
                                text = "Restore failed: ${uiState.importError}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        TextButton(
                            onClick = { importFileLauncher.launch("application/json") },
                            enabled = !uiState.isImporting && uiState.activeProfile != null,
                        ) {
                            Text("Choose backup file")
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(Spacing.lg))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(Spacing.md))
                SectionHeader(title = "SECURITY")
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
                        .padding(horizontal = Spacing.md),
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Fingerprint,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "App lock",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Require fingerprint or PIN/pattern to open the app",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = uiState.biometricLockEnabled,
                            onCheckedChange = { viewModel.toggleBiometricLock(it) },
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(Spacing.lg))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(Spacing.md))
                SectionHeader(title = "APP UPDATE")
                Spacer(modifier = Modifier.height(Spacing.sm))
            }

            item {
                UpdateSection(
                    status = uiState.updateStatus,
                    isChecking = uiState.isCheckingUpdate,
                    onCheckNow = viewModel::checkForUpdate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md),
                )
            }

            item { Spacer(modifier = Modifier.height(Spacing.xxl)) }
        }
    }
}

@Composable
private fun UpdateSection(
    status: UpdateStatus,
    isChecking: Boolean,
    onCheckNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Software update",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    val subtitle = when (status) {
                        is UpdateStatus.Unknown -> "Tap to check for updates"
                        is UpdateStatus.Checking -> "Checking…"
                        is UpdateStatus.UpToDate -> "You are on the latest version"
                        is UpdateStatus.UpdateAvailable -> "Version ${status.info.latestVersion} is available"
                        is UpdateStatus.UnableToCheck -> "Could not check — try again later"
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (status is UpdateStatus.UpdateAvailable)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }

            if (status is UpdateStatus.UpdateAvailable) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = "Released ${status.info.publishedAt.take(10)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (status.info.releaseNotes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = status.info.releaseNotes.lines().take(3).joinToString("\n"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.sm))
                FilledTonalButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(status.info.releaseUrl))
                        context.startActivity(intent)
                    },
                ) {
                    Text("Download Update")
                }
            } else {
                Spacer(modifier = Modifier.height(Spacing.sm))
                TextButton(
                    onClick = onCheckNow,
                    enabled = !isChecking,
                ) {
                    Text("Check now")
                }
            }
        }
    }
}
