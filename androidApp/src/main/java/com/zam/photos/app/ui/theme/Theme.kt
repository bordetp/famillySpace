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
    onSurfaceVariant = TextMutedLight,
    outline = BorderLight,
    outlineVariant = BorderStripe,
    error = Color(0xFFB3261E),
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = TerracottaLight,
    onPrimary = Color(0xFF1A120C),
    primaryContainer = Color(0xFF5A3824),
    onPrimaryContainer = Color(0xFFF8EAE0),
    secondary = Color(0xFF9AAB8C),
    onSecondary = Color(0xFF141A12),
    secondaryContainer = Color(0xFF2C3327),
    onSecondaryContainer = Color(0xFFD5DDCB),
    tertiary = Color(0xFFE08B6E),
    onTertiary = Color(0xFF1A120C),
    background = SurfaceDark,
    onBackground = Color(0xFFF2EDE6),
    surface = CardDark,
    onSurface = Color(0xFFF2EDE6),
    surfaceVariant = Color(0xFF2A2620),
    onSurfaceVariant = TextMutedDark,
    outline = Color(0xFF4A443C),
    outlineVariant = Color(0xFF332E28),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    inverseSurface = Color(0xFFF2EDE6),
    inverseOnSurface = Color(0xFF2A2620)
)

@Composable
fun FamilySpaceTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
