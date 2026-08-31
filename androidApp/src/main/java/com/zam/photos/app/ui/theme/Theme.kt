package com.zam.photos.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Terracotta,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF8EAE0),
    onPrimaryContainer = TerracottaDark,
    secondary = Sage,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EDE4),
    onSecondaryContainer = Color(0xFF3A4534),
    tertiary = Clay,
    onTertiary = Color.White,
    background = SurfaceWarm,
    onBackground = Ink,
    surface = SurfaceCard,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF0EBE3),
    onSurfaceVariant = TextMuted,
    outline = BorderLight,
    outlineVariant = BorderStripe,
    error = Color(0xFFB3261E),
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = TerracottaLight,
    onPrimary = Ink,
    primaryContainer = TerracottaDark,
    onPrimaryContainer = Color(0xFFF8EAE0),
    secondary = Sage,
    onSecondary = Color.White,
    background = SurfaceDark,
    onBackground = Color(0xFFF4F1EC),
    surface = CardDark,
    onSurface = Color(0xFFF4F1EC),
    surfaceVariant = Color(0xFF3A352E),
    onSurfaceVariant = TextMuted,
    outline = Color(0xFF5A534A),
    outlineVariant = Color(0xFF3A352E)
)

@Composable
fun FamilySpaceTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colorScheme = if (dark) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
