package com.zam.photos.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zam.photos.app.ui.theme.Clay
import com.zam.photos.app.ui.theme.Sage
import com.zam.photos.app.ui.theme.Sand
import com.zam.photos.app.ui.theme.Terracotta
import kotlin.math.abs

private val AvatarPalette = listOf(Terracotta, Clay, Sage, Sand)

fun avatarColorFor(name: String): Color =
    AvatarPalette[abs(name.hashCode()) % AvatarPalette.size]

@Composable
fun Avatar(
    name: String,
    imageUrl: String?,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val initials = name.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifBlank { "?" }

    var imageFailed by remember(imageUrl) { mutableStateOf(false) }

    if (!imageUrl.isNullOrBlank() && !imageFailed) {
        AsyncImage(
            model = imageUrl,
            contentDescription = name,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
            onError = { imageFailed = true }
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(avatarColorFor(name)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = (size.value * 0.38f).sp
            )
        }
    }
}
