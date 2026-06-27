package com.lifepilot.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifepilot.designsystem.theme.Spacing

@Composable
fun TimelineCard(
    title: String,
    summary: String?,
    dateLabel: String,
    isLast: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "$title, $dateLabel"
            }
            .clickable(onClick = onClick)
            .height(IntrinsicSize.Min),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .width(8.dp)
                    .height(8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.extraSmall,
                    ),
            )
            if (!isLast) {
                VerticalDivider(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = Spacing.md)
                .padding(end = Spacing.md),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (summary != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}
