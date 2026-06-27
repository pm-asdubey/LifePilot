package com.lifepilot.features.objectdetail.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifepilot.designsystem.theme.Spacing
import com.lifepilot.domain.model.LifeObject

private val RELATIONSHIP_TYPES = listOf(
    "RELATED_TO" to "Related To",
    "DEPENDS_ON" to "Depends On",
    "PART_OF" to "Part Of",
    "REFERENCED_BY" to "Referenced By",
    "SUPERSEDES" to "Supersedes",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkObjectSheet(
    availableObjects: List<LifeObject>,
    onLink: (targetObjectId: String, relationshipType: String) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    var query by remember { mutableStateOf("") }
    var selectedObjectId by remember { mutableStateOf<String?>(null) }
    var selectedType by remember { mutableStateOf(RELATIONSHIP_TYPES.first().first) }

    val filtered = remember(query, availableObjects) {
        if (query.isBlank()) availableObjects
        else availableObjects.filter { it.title.contains(query, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.padding(horizontal = Spacing.md)) {
            Text(
                text = "Link Object",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = Spacing.md),
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search objects") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            LazyColumn(
                contentPadding = PaddingValues(bottom = Spacing.md),
                modifier = Modifier.height(200.dp),
            ) {
                items(filtered, key = { it.objectId }) { obj ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedObjectId = obj.objectId }
                            .padding(vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedObjectId == obj.objectId,
                            onClick = { selectedObjectId = obj.objectId },
                        )
                        Column(modifier = Modifier.padding(start = Spacing.sm)) {
                            Text(
                                text = obj.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = obj.objectType,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            HorizontalDivider()
            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = "Relationship type",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            RELATIONSHIP_TYPES.forEach { (type, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedType = type }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = Spacing.sm),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = {
                        val targetId = selectedObjectId ?: return@TextButton
                        onLink(targetId, selectedType)
                    },
                    enabled = selectedObjectId != null,
                ) { Text("Link") }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}
