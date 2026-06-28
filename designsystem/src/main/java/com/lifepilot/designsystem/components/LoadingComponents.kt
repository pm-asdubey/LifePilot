package com.lifepilot.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lifepilot.designsystem.theme.Spacing

fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue = -300f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translate",
    )
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest

    background(
        brush = Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = Offset(translateX, 0f),
            end = Offset(translateX + 600f, 0f),
        ),
        shape = RoundedCornerShape(6.dp),
    )
}

@Composable
fun ObjectCardSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .shimmer(),
        )
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(15.dp)
                    .shimmer(),
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(12.dp)
                    .shimmer(),
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.28f)
                    .height(18.dp)
                    .shimmer(),
            )
        }
    }
}

@Composable
fun TaskCardSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .shimmer(),
        )
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(14.dp)
                    .shimmer(),
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(11.dp)
                    .shimmer(),
            )
        }
    }
}

@Composable
fun HomeBriefSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = Spacing.md)) {
        Spacer(modifier = Modifier.height(Spacing.lg))
        Box(modifier = Modifier.fillMaxWidth(0.4f).height(14.dp).shimmer())
        Spacer(modifier = Modifier.height(Spacing.xs))
        Box(modifier = Modifier.fillMaxWidth(0.7f).height(32.dp).shimmer())
        Spacer(modifier = Modifier.height(Spacing.xl))
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .shimmer(),
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
        }
        Spacer(modifier = Modifier.height(Spacing.lg))
        Box(modifier = Modifier.fillMaxWidth(0.3f).height(13.dp).shimmer())
        Spacer(modifier = Modifier.height(Spacing.sm))
        repeat(2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shimmer(),
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
        }
    }
}

@Composable
fun PlannerSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = Spacing.md)) {
        Spacer(modifier = Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth(0.45f).height(13.dp).shimmer())
        Spacer(modifier = Modifier.height(Spacing.sm))
        Box(modifier = Modifier.fillMaxWidth().height(88.dp).shimmer())
        Spacer(modifier = Modifier.height(Spacing.lg))
        Box(modifier = Modifier.fillMaxWidth(0.35f).height(13.dp).shimmer())
        Spacer(modifier = Modifier.height(Spacing.sm))
        repeat(4) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Box(modifier = Modifier.size(20.dp).shimmer())
                Spacer(modifier = Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp).shimmer())
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth(0.4f).height(11.dp).shimmer())
                }
            }
            Spacer(modifier = Modifier.height(Spacing.xs))
        }
    }
}
