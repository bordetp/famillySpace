package com.zam.photos.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

val ColorScheme.appMuted: Color
    get() = onSurfaceVariant

val ColorScheme.appBorder: Color
    get() = outline

val ColorScheme.appSurfaceWarm: Color
    get() = surfaceVariant

val ColorScheme.appPlaceholder: Color
    get() = onSurfaceVariant.copy(alpha = 0.72f)

val ColorScheme.appUnread: Color
    get() = primaryContainer.copy(alpha = if (background.luminance() < 0.5f) 0.45f else 1f)

private fun Color.luminance(): Float {
    val r = red
    val g = green
    val b = blue
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}
