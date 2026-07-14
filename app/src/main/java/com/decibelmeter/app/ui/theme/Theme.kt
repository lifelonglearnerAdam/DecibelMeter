package com.decibelmeter.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── 参考 XMSLEEP Material3 和 Glyph-Decibel-Meter 的深色主题风格 ──

// Primary: 深邃蓝黑
val Primary = Color(0xFF1A1A2E)
val PrimaryVariant = Color(0xFF16213E)
val Secondary = Color(0xFF0F3460)

// Accent colors
val Accent = Color(0xFFE94560)
val AccentGreen = Color(0xFF4CAF50)
val AccentYellow = Color(0xFFFFC107)
val AccentOrange = Color(0xFFFF9800)
val AccentRed = Color(0xFFF44336)

// Light theme
val LightBackground = Color(0xFFF8F9FA)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF1A1A2E)
val LightOnSurfaceVariant = Color(0xFF6C757D)

// Dark theme
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E2E)
val DarkOnSurface = Color(0xFFE8E8E8)
val DarkOnSurfaceVariant = Color(0xFFA0A0A0)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryVariant,
    secondary = Secondary,
    onSecondary = Color.White,
    tertiary = Accent,
    background = LightBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = AccentRed,
    surfaceVariant = Color(0xFFE9ECEF),
    outline = Color(0xFFDEE2E6),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF3A6BD5),
    onPrimary = Color.White,
    primaryContainer = PrimaryVariant,
    secondary = Color(0xFF5B8DEF),
    onSecondary = Color.White,
    tertiary = Accent,
    background = DarkBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = Color(0xFFFF6B6B),
    surfaceVariant = Color(0xFF2A2A3A),
    outline = Color(0xFF3A3A4A),
)

@Composable
fun DecibelMeterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
