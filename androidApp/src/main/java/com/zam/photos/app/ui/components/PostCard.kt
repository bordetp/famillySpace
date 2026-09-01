package com.zam.photos.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.zam.photos.app.R
import com.zam.photos.app.data.Post
import com.zam.photos.app.ui.theme.InkSecondary
import com.zam.photos.app.ui.theme.Terracotta
import com.zam.photos.app.ui.theme.appBorder
import com.zam.photos.app.ui.theme.appMuted
import com.zam.photos.app.ui.theme.appPlaceholder
import com.zam.photos.app.ui.theme.appSurfaceWarm

@Composable
fun PostCard(
    post: Post,
    onCommentClick: () -> Unit,
    onLikeClick: () -> Unit,
    onShareClick: () -> Unit,
    onPostClick: () -> Unit = {},
    canDelete: Boolean = false,
    onDeleteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = MaxFeedWidth)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onPostClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(name = post.author.name, imageUrl = post.author.profileImageUrl.takeIf { it.isNotBlank() }, size = 40.dp)
            Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                Text(post.author.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Text(post.createdAt, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.appMuted)
            }
            PostOverflowMenu(canDelete = canDelete, onDeleteClick = onDeleteClick)
        }

        if (!post.imageUrl.isNullOrBlank()) {
            AdaptivePostImage(url = post.imageUrl)
        } else if (post.content.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(MaterialTheme.colorScheme.appSurfaceWarm),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    post.content,
                    modifier = Modifier.padding(22.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = InkSecondary
                )
            }
        }

        PostActionsRow(
            post = post,
            onCommentClick = onCommentClick,
            onLikeClick = onLikeClick,
            onShareClick = onShareClick,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp)
        )

        if (post.content.isNotBlank() && !post.imageUrl.isNullOrBlank()) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(post.author.name) }
                    append(" ")
                    withStyle(SpanStyle(color = InkSecondary)) { append(post.content) }
                },
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 20.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(MaterialTheme.colorScheme.appSurfaceWarm)
        )
    }
}

@Composable
fun PostOverflowMenu(canDelete: Boolean, onDeleteClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.post_menu), tint = MaterialTheme.colorScheme.appMuted, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (canDelete) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete_post)) },
                    onClick = {
                        expanded = false
                        onDeleteClick()
                    }
                )
            } else {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.share_post_link)) },
                    onClick = { expanded = false }
                )
            }
        }
    }
}

@Composable
fun PostActionsRow(
    post: Post,
    onCommentClick: () -> Unit,
    onLikeClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onLikeClick)
        ) {
            Icon(
                if (post.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = stringResource(R.string.action_like),
                modifier = Modifier.size(20.dp),
                tint = if (post.isLiked) Terracotta else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("${post.likes}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onCommentClick)) {
            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = stringResource(R.string.action_comment), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("${post.comments}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            Icons.Outlined.Share,
            contentDescription = stringResource(R.string.action_share),
            modifier = Modifier.size(19.dp).clickable(onClick = onShareClick)
        )
    }
}
