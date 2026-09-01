package com.zam.photos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.zam.photos.app.R
import com.zam.photos.app.data.AppNotification
import com.zam.photos.app.data.NotificationType
import com.zam.photos.app.ui.components.Avatar
import com.zam.photos.app.ui.components.EmptyState
import com.zam.photos.app.ui.components.RefreshOnResume
import com.zam.photos.app.di.activityKoinViewModel
import com.zam.photos.app.ui.theme.Terracotta
import com.zam.photos.app.ui.theme.appBorder
import com.zam.photos.app.ui.theme.appMuted
import com.zam.photos.app.ui.theme.appUnread
import com.zam.photos.app.viewmodel.NotificationsViewModel

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenPost: (String) -> Unit = {},
    onOpenConversation: (String) -> Unit = {},
    viewModel: NotificationsViewModel = activityKoinViewModel()
) {
    val state by viewModel.state.collectAsState()

    RefreshOnResume { viewModel.onScreenOpened() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(
                stringResource(R.string.notifications),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.appBorder)

        if (state.notifications.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    Icons.Outlined.Notifications,
                    stringResource(R.string.notifications_empty),
                    stringResource(R.string.notifications_empty_subtitle)
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.notifications, key = { it.id }) { notification ->
                    NotificationRow(
                        notification = notification,
                        onClick = {
                            viewModel.onNotificationClick(notification.id)
                            when {
                                !notification.conversationId.isNullOrBlank() -> onOpenConversation(notification.conversationId)
                                !notification.postId.isNullOrBlank() -> onOpenPost(notification.postId)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notification: AppNotification,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (notification.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.appUnread)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Avatar(
            name = notification.actorName,
            imageUrl = notification.actorImageUrl,
            size = 44.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append(notification.actorName)
                    }
                    append(" ")
                    append(notification.message)
                },
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                notification.timeAgo,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.appMuted,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        NotificationTypeIcon(type = notification.type)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.appBorder, modifier = Modifier.padding(start = 78.dp))
}

@Composable
private fun NotificationTypeIcon(type: NotificationType) {
    val (icon, tint) = when (type) {
        NotificationType.LIKE -> Icons.Filled.Favorite to Terracotta
        NotificationType.COMMENT -> Icons.Outlined.ChatBubbleOutline to MaterialTheme.colorScheme.onSurfaceVariant
        NotificationType.NEW_POST -> Icons.Outlined.PhotoCamera to MaterialTheme.colorScheme.onSurfaceVariant
        NotificationType.MENTION -> Icons.Outlined.AlternateEmail to Terracotta
        NotificationType.JOIN -> Icons.Filled.PersonAdd to MaterialTheme.colorScheme.onSurfaceVariant
        NotificationType.MESSAGE -> Icons.Outlined.ChatBubbleOutline to Terracotta
    }
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = tint)
    }
}
