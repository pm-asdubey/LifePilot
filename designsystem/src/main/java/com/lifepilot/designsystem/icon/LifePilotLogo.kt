package com.lifepilot.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * LifePilot brand mark: a compass — outer ring, N/E/S/W cardinal ticks and an elongated two-tone
 * needle that clearly points up. Matches the adaptive launcher icon so the in-app mark is consistent.
 *
 * A reusable [ImageVector] for Home headers, empty states, the AI avatar and splash surfaces.
 * Drawn on a 48x48 viewport, centered at (24, 24).
 */
object LifePilotLogo {

    private val Indigo = Color(0xFF4B54A6)
    private val MutedIndigo = Color(0xFF9AA2D4)
    private val Teal = Color(0xFF2FA398)
    private val Ring = Color(0x664B54A6)

    val imageVector: ImageVector by lazy {
        ImageVector.Builder(
            name = "LifePilotLogo",
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f,
        ).apply {
            // Outer guide ring
            path(
                stroke = SolidColor(Ring),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4f, 24f)
                arcToRelative(20f, 20f, 0f, true, false, 40f, 0f)
                arcToRelative(20f, 20f, 0f, true, false, -40f, 0f)
                close()
            }
            // Cardinal ticks (N, E, S, W) just inside the ring
            path(
                stroke = SolidColor(Ring),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(24f, 6f); lineTo(24f, 9f)
                moveTo(42f, 24f); lineTo(39f, 24f)
                moveTo(24f, 42f); lineTo(24f, 39f)
                moveTo(6f, 24f); lineTo(9f, 24f)
            }
            // Needle — north half: tall, bright, clearly points UP
            path(fill = SolidColor(Indigo)) {
                moveTo(24f, 9f)
                lineTo(20.5f, 24f)
                lineTo(27.5f, 24f)
                close()
            }
            // Needle — south half: shorter, muted counterweight
            path(fill = SolidColor(MutedIndigo)) {
                moveTo(24f, 34f)
                lineTo(21.5f, 24f)
                lineTo(26.5f, 24f)
                close()
            }
            // Center hub (teal)
            path(fill = SolidColor(Teal)) {
                moveTo(21.6f, 24f)
                arcToRelative(2.4f, 2.4f, 0f, true, false, 4.8f, 0f)
                arcToRelative(2.4f, 2.4f, 0f, true, false, -4.8f, 0f)
                close()
            }
        }.build()
    }
}
