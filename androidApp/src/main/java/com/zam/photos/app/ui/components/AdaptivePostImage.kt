package com.zam.photos.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import coil.compose.AsyncImage
import com.zam.photos.app.ui.theme.SurfaceWarm

/** Caps feed/detail width so photos are not stretched into a thin band on tablets. */
val MaxFeedWidth = 640.dp

fun Modifier.feedContentWidth(): Modifier = fillMaxWidth().widthIn(max = MaxFeedWidth)

private const val MinPhotoAspect = 0.75f
private const val MaxPhotoAspect = 1.91f
private const val DefaultPhotoAspect = 1f

@Composable
fun AdaptivePostImage(
    url: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.68f).dp
    var aspect by remember(url) { mutableFloatStateOf(DefaultPhotoAspect) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceWarm),
        contentAlignment = Alignment.Center
    ) {
        val width = if (maxWidth == Dp.Infinity) MaxFeedWidth else maxWidth
        val height = min(width / aspect, maxHeight)
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
            onSuccess = { state ->
                val size = state.painter.intrinsicSize
                if (size.isSpecified && size.width > 0f && size.height > 0f) {
                    aspect = (size.width / size.height).coerceIn(MinPhotoAspect, MaxPhotoAspect)
                }
            }
        )
    }
}
