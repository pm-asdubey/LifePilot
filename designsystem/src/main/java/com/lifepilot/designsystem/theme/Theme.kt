package com.lifepilot.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// LifePilot "calm & premium" light theme — teal → indigo accents on soft light surfaces.
// Palette mirrors the onboarding design mockup so the whole app shares one visual language.
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0EA5A4),            // teal
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCFF1EF),
    onPrimaryContainer = Color(0xFF08514F),
    secondary = Color(0xFF6366F1),          // indigo
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE3E4FD),
    onSecondaryContainer = Color(0xFF31329A),
    tertiary = Color(0xFFB06A12),           // warm amber for attention
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFDF1DE),
    onTertiaryContainer = Color(0xFF6E4208),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFDE4E4),
    onErrorContainer = Color(0xFF7A1414),
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF16182B),       // ink
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF16182B),
    surfaceVariant = Color(0xFFEEF0F6),
    onSurfaceVariant = Color(0xFF6B6F85),   // muted
    outline = Color(0xFFD7DAE6),
    outlineVariant = Color(0xFFE7E9F1),
    inverseSurface = Color(0xFF16182B),
    inverseOnSurface = Color(0xFFF6F7FB),
    inversePrimary = Color(0xFF7FE0DD),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBFCFE),
    surfaceContainer = Color(0xFFF6F7FB),
    surfaceContainerHigh = Color(0xFFEEF0F6),
    surfaceContainerHighest = Color(0xFFE7E9F1),
    scrim = Color(0xFF16182B),
)

// Original dark palette — selectable via Settings (Light / Dark / System).
private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant,
    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,
    inversePrimary = InversePrimary,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
)

@Composable
fun LifePilotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // System bar icons: dark icons on the light theme, light icons on the dark theme.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LifePilotTypography,
        shapes = LifePilotShapes,
        content = content,
    )
}
