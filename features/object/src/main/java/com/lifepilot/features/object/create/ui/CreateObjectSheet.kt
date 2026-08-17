package com.lifepilot.features.objectdetail.create.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import com.lifepilot.designsystem.icon.objectTypeIcon
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifepilot.designsystem.theme.Spacing
import com.lifepilot.features.objectdetail.create.state.CreateObjectStep
import com.lifepilot.features.objectdetail.create.viewmodel.CreateObjectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateObjectSheet(
    onDismiss: () -> Unit,
    onObjectCreated: (String) -> Unit,
    initialDomain: String? = null,
    viewModel: CreateObjectViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(initialDomain) {
        viewModel.setDomainFilter(initialDomain)
    }

    LaunchedEffect(state.created) {
        if (state.created && state.createdObjectId != null) {
            onObjectCreated(state.createdObjectId!!)
        }
    }

    // Scope the offered types to the chosen domain when one was requested. If that domain has no
    // registered record types yet, fall back to the full list so the domain can still receive a record.
    val typesForDomain = state.domainFilter
        ?.let { d -> state.availableTypes.filter { it.domain.equals(d, ignoreCase = true) } }
        ?.takeIf { it.isNotEmpty() }
        ?: state.availableTypes

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md),
        ) {
            when (state.step) {
                CreateObjectStep.SELECT_TYPE -> {
                    Text(
                        text = state.domainFilter?.let { "Add to $it" }
                            ?: "What would you like to track?",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = Spacing.md),
                    )
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(typesForDomain, key = { it.objectType }) { typeItem ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                ),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectType(typeItem.objectType) },
                            ) {
                                Row(
                                    modifier = Modifier.padding(Spacing.md),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = objectTypeIcon(typeItem.icon),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.md))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = typeItem.displayName,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = typeItem.domain,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Filled.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(Spacing.xxl)) }
                    }
                }

                CreateObjectStep.FILL_DETAILS -> {
                    val selectedType = state.availableTypes.find { it.objectType == state.selectedType }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = Spacing.md),
                    ) {
                        IconButton(onClick = viewModel::goBack) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                        Text(
                            text = selectedType?.displayName ?: (state.selectedType ?: ""),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }

                    OutlinedTextField(
                        value = state.title,
                        onValueChange = viewModel::onTitleChange,
                        label = { Text("Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.error != null,
                    )

                    Spacer(modifier = Modifier.height(Spacing.md))

                    OutlinedTextField(
                        value = state.description,
                        onValueChange = viewModel::onDescriptionChange,
                        label = { Text("Notes (optional)") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (state.error != null) {
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(
                            text = state.error!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.lg))

                    Button(
                        onClick = viewModel::createObject,
                        enabled = !state.isLoading && state.title.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (state.isLoading) "Saving..." else "Start tracking")
                    }
                    Spacer(modifier = Modifier.height(Spacing.xxl))
                }
            }
        }
    }
}
