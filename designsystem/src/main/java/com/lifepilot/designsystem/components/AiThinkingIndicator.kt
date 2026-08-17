package com.lifepilot.designsystem.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.lifepilot.designsystem.icon.LifePilotLogo
import com.lifepilot.designsystem.theme.Spacing
import kotlinx.coroutines.delay

/**
 * The rotating "thinking" copy. Extracted so the cycling behaviour is unit-testable and cannot
 * silently regress.
 */
object ThinkingMessages {
    val all: List<String> = listOf(
        "Reviewing your records",
        "Checking your goals",
        "Searching your history",
        "Analysing life state",
        "Consulting documents",
        "Preparing a response",
        "Checking Planner",
        "Reviewing your profile",
    )

    /** Next index in the rotation, wrapping at the end. */
    fun next(index: Int): Int = (index + 1) % all.size

    /** The message at [index], safe for any integer (wraps). */
    fun messageAt(index: Int): String = all[((index % all.size) + all.size) % all.size]
}

/**
 * Animated thinking indicator shown while the AI is processing.
 *
 * Layout: the LifePilot **compass mark** (the AI's avatar) + a WhatsApp-style **animated three-dots**
 * + cycling copy. When [statusMessage] is non-null (e.g. "Looking up your records…") it is shown
 * directly; otherwise the indicator rotates through [ThinkingMessages] so the UI never looks frozen.
 */
@Composable
fun AiThinkingIndicator(
    statusMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    var index by remember { mutableIntStateOf(0) }

    // Always rotate the copy so the indicator never looks frozen. A specific [statusMessage] (e.g.
    // "Looking up your records…") is shown as the first line, then the rotation continues.
    LaunchedEffect(Unit) {
        while (true) {
            delay(2200)
            index = ThinkingMessages.next(index)
        }
    }

    val displayText = when {
        statusMessage != null && index == 0 -> statusMessage
        else -> "${ThinkingMessages.messageAt(index)}…"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(vertical = Spacing.xs),
    ) {
        Image(
            painter = rememberVectorPainter(LifePilotLogo.imageVector),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        TypingDots(color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(Spacing.sm))
        AnimatedContent(
            targetState = displayText,
            transitionSpec = {
                (slideInVertically { it / 2 } + fadeIn()) togetherWith
                    (slideOutVertically { -it / 2 } + fadeOut())
            },
            label = "thinking_message",
        ) { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** WhatsApp-style three dots that fade in a staggered wave. */
@Composable
private fun TypingDots(color: Color) {
    val transition = rememberInfiniteTransition(label = "typing_dots")
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(3) { i ->
            val dotAlpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600, delayMillis = i * 160, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot_$i",
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .alpha(dotAlpha)
                    .background(color, CircleShape),
            )
        }
    }
}
