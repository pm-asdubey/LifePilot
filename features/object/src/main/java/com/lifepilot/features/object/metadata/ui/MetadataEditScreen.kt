package com.lifepilot.features.object.metadata.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifepilot.designsystem.theme.Spacing
import com.lifepilot.domain.model.schema.MetadataFieldDefinition
import com.lifepilot.features.object.metadata.viewmodel.MetadataEditViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataEditScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MetadataEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.saved) {
        if (state.saved) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::save,
                        enabled = !state.isSaving,
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.padding(Spacing.xs))
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Save",
                            )
                        }
                    }
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
            contentPadding = PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            items(state.fields, key = { it.fieldId }) { field ->
                MetadataFieldInput(
                    field = field,
                    value = state.values[field.fieldId] ?: "",
                    error = state.errors[field.fieldId],
                    onValueChange = { viewModel.onFieldValueChange(field.fieldId, it) },
                )
            }
            item {
                Spacer(modifier = Modifier.height(Spacing.md))
                Button(
                    onClick = viewModel::save,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (state.isSaving) "Saving..." else "Save Details")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetadataFieldInput(
    field: MetadataFieldDefinition,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (field.enumValues.isNotEmpty()) {
            EnumField(
                field = field,
                value = value,
                error = error,
                onValueChange = onValueChange,
            )
        } else {
            val keyboardType = when (field.fieldType) {
                "NUMBER", "CURRENCY" -> KeyboardType.Decimal
                "DATE" -> KeyboardType.Number
                else -> KeyboardType.Text
            }
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = {
                    Text(if (field.required) "${field.displayName} *" else field.displayName)
                },
                placeholder = (field.placeholder ?: field.hint)?.let { { Text(it) } },
                singleLine = field.fieldType != "MULTILINE_TEXT",
                minLines = if (field.fieldType == "MULTILINE_TEXT") 3 else 1,
                maxLines = if (field.fieldType == "MULTILINE_TEXT") 5 else 1,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                isError = error != null,
                supportingText = if (error != null) {
                    { Text(error, color = MaterialTheme.colorScheme.error) }
                } else null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnumField(
    field: MetadataFieldDefinition,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = value.ifBlank { "Select ${field.displayName}" },
            onValueChange = {},
            readOnly = true,
            label = { Text(if (field.required) "${field.displayName} *" else field.displayName) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            isError = error != null,
            supportingText = if (error != null) {
                { Text(error, color = MaterialTheme.colorScheme.error) }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            field.enumValues.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.replace("_", " ")) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
