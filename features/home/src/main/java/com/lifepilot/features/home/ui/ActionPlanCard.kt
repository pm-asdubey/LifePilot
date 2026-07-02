package com.lifepilot.features.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.lifepilot.designsystem.theme.Spacing
import com.lifepilot.domain.model.ActionItem
import com.lifepilot.domain.model.ActionPlan

/**
 * Renders a pending [ActionPlan] for user review and approval.
 *
 * The user can check/uncheck individual items, view clarifying questions, and
 * either approve all checked items or step through them one by one.
 */
@Composable
fun ActionPlanCard(
    plan: ActionPlan,
    onApprove: (plan: ActionPlan) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    var checkedIds by remember(plan.planId) {
        mutableStateOf(plan.items.filter { it.isChecked }.map { it.itemId }.toSet())
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .padding(Spacing.lg)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.EventNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "Action plan",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = plan.summary,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )

            Spacer(modifier = Modifier.height(Spacing.md))
            val userVisibleItems = plan.items.filter { it !is ActionItem.UpdateDomainUnderstanding }
            Text(
                text = "${checkedIds.size} of ${userVisibleItems.size} selected",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            ActionPlanSection(
                title = "Records to update",
                items = plan.items.filter { it.isRecordUpdate() && it !is ActionItem.UpdateDomainUnderstanding },
                checkedIds = checkedIds,
                onCheckedChange = { itemId, checked ->
                    checkedIds = if (checked) checkedIds + itemId else checkedIds - itemId
                },
            )

            ActionPlanSection(
                title = "Objects to create",
                items = plan.items.filter { it.isObjectCreation() },
                checkedIds = checkedIds,
                onCheckedChange = { itemId, checked ->
                    checkedIds = if (checked) checkedIds + itemId else checkedIds - itemId
                },
            )

            ActionPlanSection(
                title = "Tasks to create",
                items = plan.items.filter { it.isTaskCreation() },
                checkedIds = checkedIds,
                onCheckedChange = { itemId, checked ->
                    checkedIds = if (checked) checkedIds + itemId else checkedIds - itemId
                },
            )

            if (plan.clarifyingQuestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.md))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Open questions",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                plan.clarifyingQuestions.forEach { question ->
                    Text(
                        text = "• ${question.text}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.End),
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        "Not now",
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                    )
                }
                FilledTonalButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onApprove(planWithSelection(plan, checkedIds))
                }) {
                    Text("Approve")
                }
            }
        }
    }
}

@Composable
private fun ActionPlanItemRow(
    item: ActionItem,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    if (!item.isEnabled) return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = item.typeLabel(),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    modifier = Modifier.height(24.dp),
                )
            }
            if (item.dependsOn.isNotEmpty()) {
                Text(
                    text = "After: ${item.dependsOn.joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
                )
            }
        }
    }
}

private fun ActionItem.typeLabel(): String = when (this) {
    is ActionItem.UpdateRecord -> "Record update"
    is ActionItem.CreateRecord -> "New record"
    is ActionItem.UpdateStatus -> "Status update"
    is ActionItem.CreateTask -> "Task"
    is ActionItem.UpdateDomainUnderstanding -> "Domain update"
}

@Composable
private fun ActionPlanSection(
    title: String,
    items: List<ActionItem>,
    checkedIds: Set<String>,
    onCheckedChange: (itemId: String, checked: Boolean) -> Unit,
) {
    if (items.isEmpty()) return

    Spacer(modifier = Modifier.height(Spacing.sm))
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
    )
    Spacer(modifier = Modifier.height(Spacing.xs))
    items.forEach { item ->
        ActionPlanItemRow(
            item = item,
            checked = item.itemId in checkedIds,
            onCheckedChange = { checked -> onCheckedChange(item.itemId, checked) },
        )
        if (item != items.last()) {
            Spacer(modifier = Modifier.height(Spacing.xs))
        }
    }
    HorizontalDivider(
        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.08f),
        modifier = Modifier.padding(top = Spacing.sm),
    )
}

private fun ActionItem.isRecordUpdate(): Boolean = when (this) {
    is ActionItem.UpdateRecord,
    is ActionItem.UpdateStatus,
    is ActionItem.UpdateDomainUnderstanding -> true
    is ActionItem.CreateRecord,
    is ActionItem.CreateTask -> false
}

private fun ActionItem.isObjectCreation(): Boolean = this is ActionItem.CreateRecord

private fun ActionItem.isTaskCreation(): Boolean = this is ActionItem.CreateTask

private fun planWithSelection(plan: ActionPlan, checkedIds: Set<String>): ActionPlan {
    return plan.copy(
        items = plan.items.map { item ->
            when (item) {
                is ActionItem.UpdateRecord -> item.copy(isChecked = item.itemId in checkedIds)
                is ActionItem.CreateRecord -> item.copy(isChecked = item.itemId in checkedIds)
                is ActionItem.UpdateStatus -> item.copy(isChecked = item.itemId in checkedIds)
                is ActionItem.CreateTask -> item.copy(isChecked = item.itemId in checkedIds)
                is ActionItem.UpdateDomainUnderstanding -> item.copy(isChecked = item.itemId in checkedIds)
            }
        },
    )
}
