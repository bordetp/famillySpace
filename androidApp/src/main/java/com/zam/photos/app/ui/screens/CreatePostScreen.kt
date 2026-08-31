package com.zam.photos.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zam.photos.app.R
import com.zam.photos.app.ui.components.AdaptivePostImage
import com.zam.photos.app.ui.theme.BorderLight
import com.zam.photos.app.ui.theme.BorderStripe
import com.zam.photos.app.ui.theme.SurfaceWarm
import com.zam.photos.app.ui.theme.Terracotta
import com.zam.photos.app.ui.theme.TextMuted
import com.zam.photos.app.ui.theme.TextPlaceholder
import com.zam.photos.app.viewmodel.CreatePostViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun CreatePostScreen(
    onPostCreated: () -> Unit,
    onDismiss: () -> Unit = onPostCreated,
    viewModel: CreatePostViewModel = koinViewModel()
) {
    var content by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var uploadedImageUrl by remember { mutableStateOf<String?>(null) }
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val previewMaxHeight = (LocalConfiguration.current.screenHeightDp * 0.45f).dp

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        imageUri = uri
        uploadedImageUrl = null
        uri?.let {
            val bytes = context.contentResolver.openInputStream(it)?.readBytes() ?: return@let
            val mime = context.contentResolver.getType(it).orEmpty()
            val filename = if (mime.startsWith("video")) {
                "video_${System.currentTimeMillis()}.mp4"
            } else {
                "photo_${System.currentTimeMillis()}.jpg"
            }
            viewModel.uploadImage(bytes, filename) { url ->
                uploadedImageUrl = url
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.new_post), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(R.string.share),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                color = Terracotta,
                modifier = Modifier.clickable(enabled = !state.isLoading && (content.isNotBlank() || !uploadedImageUrl.isNullOrBlank())) {
                    viewModel.createPost(content, uploadedImageUrl) {
                        viewModel.reset()
                        onPostCreated()
                    }
                }
            )
        }
        HorizontalDivider(color = BorderLight)

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            if (imageUri != null || uploadedImageUrl != null) {
                val previewUrl = uploadedImageUrl
                if (previewUrl != null) {
                    AdaptivePostImage(url = previewUrl)
                } else {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().heightIn(max = previewMaxHeight),
                        contentScale = ContentScale.Fit
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(previewMaxHeight)
                        .background(SurfaceWarm)
                        .clickable {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(30.dp), tint = TextPlaceholder)
                        Text(stringResource(R.string.add_photo), color = TextPlaceholder, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = uploadedImageUrl ?: imageUri,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.5.dp, BorderStripe, RoundedCornerShape(8.dp))
                        .clickable {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = TextPlaceholder, modifier = Modifier.size(18.dp))
                }
            }

            androidx.compose.foundation.text.BasicTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                decorationBox = { inner ->
                    if (content.isEmpty()) {
                        Text(stringResource(R.string.post_hint), color = TextMuted)
                    }
                    inner()
                }
            )

            HorizontalDivider(color = BorderLight, modifier = Modifier.padding(horizontal = 22.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Groups, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.size(12.dp))
                Text(stringResource(R.string.audience_all), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Place, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.size(12.dp))
                Text(stringResource(R.string.add_location), style = MaterialTheme.typography.bodyLarge, color = TextMuted, modifier = Modifier.weight(1f))
            }

            if (state.isUploading || state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp),
                    color = Terracotta
                )
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 22.dp))
            }
        }
    }
}