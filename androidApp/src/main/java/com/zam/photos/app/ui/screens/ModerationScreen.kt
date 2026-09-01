package com.zam.photos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zam.photos.app.ModerationAccess
import com.zam.photos.app.R
import com.zam.photos.app.data.Comment
import com.zam.photos.app.data.Post
import com.zam.photos.app.data.UserProfile
import com.zam.photos.app.ui.components.EmptyState
import com.zam.photos.app.ui.components.RefreshOnResume
import com.zam.photos.app.ui.theme.BorderLight
import com.zam.photos.app.ui.theme.Terracotta
import com.zam.photos.app.ui.theme.TextMuted
import com.zam.photos.app.viewmodel.ModerationTab
import com.zam.photos.app.viewmodel.ModerationViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModerationScreen(
    onBack: () -> Unit,
    onOpenPost: (String) -> Unit,
    viewModel: ModerationViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    RefreshOnResume { viewModel.refresh() }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        TopAppBar(
            title = { Text(stringResource(R.string.moderation_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            }
        )
        HorizontalDivider(color = BorderLight)

        ScrollableTabRow(
            selectedTabIndex = when (state.tab) {
                ModerationTab.Users -> 0
                ModerationTab.Posts -> 1
                ModerationTab.Comments -> 2
            },
            edgePadding = 16.dp,
            divider = {}
        ) {
            Tab(
                selected = state.tab == ModerationTab.Users,
                onClick = { viewModel.selectTab(ModerationTab.Users) },
                text = { Text(stringResource(R.string.moderation_users_tab, state.usersTotal)) }
            )
            Tab(
                selected = state.tab == ModerationTab.Posts,
                onClick = { viewModel.selectTab(ModerationTab.Posts) },
                text = { Text(stringResource(R.string.moderation_posts_tab, state.postsTotal)) }
            )
            Tab(
                selected = state.tab == ModerationTab.Comments,
                onClick = { viewModel.selectTab(ModerationTab.Comments) },
                text = { Text(stringResource(R.string.moderation_comments_tab, state.commentsTotal)) }
            )
        }
        HorizontalDivider(color = BorderLight)

        state.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(16.dp)
            )
        }

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.tab == ModerationTab.Users && state.users.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Outlined.Person,
                    title = stringResource(R.string.moderation_users_empty),
                    subtitle = stringResource(R.string.moderation_users_empty_subtitle)
                )
            }

            state.tab == ModerationTab.Posts && state.posts.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Outlined.Article,
                    title = stringResource(R.string.moderation_posts_empty),
                    subtitle = stringResource(R.string.moderation_posts_empty_subtitle)
                )
            }

            state.tab == ModerationTab.Comments && state.comments.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    title = stringResource(R.string.moderation_comments_empty),
                    subtitle = stringResource(R.string.moderation_comments_empty_subtitle)
                )
            }

            state.tab == ModerationTab.Users -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.users, key = { it.id }) { user ->
                    ModerationUserRow(
                        user = user,
                        isBusy = state.deletingId == user.id,
                        onApprove = { viewModel.approveUser(user.id) },
                        onReject = { viewModel.rejectUser(user.id) }
                    )
                }
            }

            state.tab == ModerationTab.Posts -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.posts, key = { it.id }) { post ->
                    ModerationPostRow(
                        post = post,
                        isDeleting = state.deletingId == post.id,
                        onOpen = { onOpenPost(post.id) },
                        onDelete = { viewModel.deletePost(post.id) }
                    )
                }
            }

            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.comments, key = { it.id }) { comment ->
                    ModerationCommentRow(
                        comment = comment,
                        isDeleting = state.deletingId == comment.id,
                        onOpenPost = { onOpenPost(comment.postId) },
                        onDelete = { viewModel.deleteComment(comment.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModerationUserRow(
    user: UserProfile,
    isBusy: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val isAdminAccount = ModerationAccess.isModerator(user.email)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, fontWeight = FontWeight.SemiBold)
                Text(user.email.ifBlank { user.username }, color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = when {
                    user.isPending -> stringResource(R.string.moderation_status_pending)
                    user.isRejected -> stringResource(R.string.moderation_status_rejected)
                    else -> stringResource(R.string.moderation_status_approved)
                },
                color = when {
                    user.isPending -> Terracotta
                    user.isRejected -> MaterialTheme.colorScheme.error
                    else -> TextMuted
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
        if (!isAdminAccount) {
            Spacer(modifier = Modifier.height(12.dp))
            when {
                user.isPending -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onApprove,
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(stringResource(R.string.moderation_approve))
                    }
                    OutlinedButton(
                        onClick = onReject,
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(stringResource(R.string.moderation_reject))
                    }
                }

                user.isApproved -> OutlinedButton(
                    onClick = onReject,
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(R.string.moderation_block))
                }

                user.isRejected -> Button(
                    onClick = onApprove,
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(R.string.moderation_approve))
                }
            }
        }
    }
}

@Composable
private fun ModerationPostRow(
    post: Post,
    isDeleting: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .clickable(onClick = onOpen)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(post.author.name, fontWeight = FontWeight.SemiBold)
                Text(
                    text = post.author.email.ifBlank { post.author.username },
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onDelete, enabled = !isDeleting) {
                if (isDeleting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.moderation_delete_post),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        if (post.content.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = post.content,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        post.imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
            Spacer(modifier = Modifier.height(8.dp))
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.moderation_meta, post.likes, post.comments, post.createdAt),
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ModerationCommentRow(
    comment: Comment,
    isDeleting: Boolean,
    onOpenPost: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(comment.author.name, fontWeight = FontWeight.SemiBold)
                Text(
                    text = comment.author.email.ifBlank { comment.author.username },
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onDelete, enabled = !isDeleting) {
                if (isDeleting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.moderation_delete_comment),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = comment.content,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(comment.createdAt, color = TextMuted, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onOpenPost) {
            Text(stringResource(R.string.moderation_open_post))
        }
    }
}
