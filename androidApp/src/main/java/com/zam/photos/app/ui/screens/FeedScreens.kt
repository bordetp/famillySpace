package com.zam.photos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zam.photos.app.R
import com.zam.photos.app.data.Comment
import com.zam.photos.app.data.Post
import com.zam.photos.app.ui.components.AdaptivePostImage
import com.zam.photos.app.ui.components.Avatar
import com.zam.photos.app.ui.components.EmptyState
import com.zam.photos.app.ui.components.FamilySpaceFeedHeader
import com.zam.photos.app.ui.components.MaxFeedWidth
import com.zam.photos.app.ui.components.PostActionsRow
import com.zam.photos.app.ui.components.PostCard
import com.zam.photos.app.ui.components.RefreshOnResume
import com.zam.photos.app.ui.theme.BorderStripe
import com.zam.photos.app.ui.theme.InkSecondary
import com.zam.photos.app.ui.theme.Terracotta
import com.zam.photos.app.ui.theme.appBorder
import com.zam.photos.app.ui.theme.appMuted
import com.zam.photos.app.ui.theme.appPlaceholder
import com.zam.photos.app.ui.theme.appSurfaceWarm
import com.zam.photos.app.di.activityKoinViewModel
import com.zam.photos.app.viewmodel.CommentsViewModel
import com.zam.photos.app.viewmodel.FeedViewModel
import com.zam.photos.app.viewmodel.InboxViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private fun sharePost(context: android.content.Context, post: Post) {
    sharePostLink(context, post.id, post.author.name, post.content)
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FeedScreen(
    onOpenComments: (String) -> Unit,
    onOpenPost: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
    viewModel: FeedViewModel = activityKoinViewModel(),
    profileViewModel: com.zam.photos.app.viewmodel.ProfileViewModel = activityKoinViewModel(),
    notificationsViewModel: com.zam.photos.app.viewmodel.NotificationsViewModel = activityKoinViewModel(),
    inboxViewModel: InboxViewModel = activityKoinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val profileState by profileViewModel.state.collectAsState()
    val notificationsState by notificationsViewModel.state.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val context = LocalContext.current
    val user = profileState.user
    val pullState = rememberPullRefreshState(state.isRefreshing, { viewModel.loadFeed() })
    val listState = rememberLazyListState()

    RefreshOnResume {
        viewModel.loadFeed(silent = true)
        notificationsViewModel.refresh()
        inboxViewModel.refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        FamilySpaceFeedHeader(
            onNotificationsClick = onOpenNotifications,
            onProfileClick = onOpenProfile,
            userName = user?.name,
            userImageUrl = user?.profileImageUrl?.takeIf { it.isNotBlank() },
            unreadNotificationCount = notificationsState.unreadCount
        )

        Box(modifier = Modifier.fillMaxSize().pullRefresh(pullState)) {
            when {
                state.isLoading -> LoadingBox()
                state.error != null && state.posts.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(Icons.Outlined.RssFeed, stringResource(R.string.error_feed), state.error!!)
                    Button(onClick = { viewModel.loadFeed() }, modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)) {
                        Text(stringResource(R.string.retry))
                    }
                }
                state.posts.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(Icons.Outlined.RssFeed, stringResource(R.string.empty_feed), stringResource(R.string.empty_feed_subtitle))
                }
                else -> {
                    val shouldLoadMore = state.hasMore && !state.isLoadingMore
                    LaunchedEffect(listState.firstVisibleItemIndex, listState.layoutInfo.totalItemsCount, shouldLoadMore) {
                        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        if (shouldLoadMore && lastVisible >= state.posts.size - 3) {
                            viewModel.loadMore()
                        }
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(state.posts, key = { it.id }) { post ->
                            PostCard(
                                post = post,
                                onCommentClick = { onOpenComments(post.id) },
                                onLikeClick = { viewModel.toggleLike(post.id) },
                                onShareClick = { sharePost(context, post) },
                                onPostClick = { onOpenPost(post.id) },
                                canDelete = currentUserId == post.authorId,
                                onDeleteClick = { viewModel.deletePost(post.id) }
                            )
                        }
                    }
                }
            }
            PullRefreshIndicator(state.isRefreshing, pullState, Modifier.align(Alignment.TopCenter), contentColor = Terracotta)
        }
    }
}

@Composable
fun MessagesScreen() {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.messages),
            modifier = Modifier.padding(top = 18.dp, bottom = 12.dp),
            style = MaterialTheme.typography.titleLarge
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.appBorder)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                Icons.Outlined.ChatBubbleOutline,
                stringResource(R.string.messages_empty),
                stringResource(R.string.messages_subtitle)
            )
        }
    }
}

@Composable
fun ExploreScreen(
    onOpenPost: (String) -> Unit,
    viewModel: FeedViewModel = activityKoinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val imagePosts = state.posts.filter { !it.imageUrl.isNullOrBlank() }
    val columns = exploreColumnCount(LocalConfiguration.current.screenWidthDp)

    RefreshOnResume { viewModel.loadFeed(silent = true) }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Text(
            stringResource(R.string.explore),
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
            style = MaterialTheme.typography.titleLarge
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.appBorder)

        when {
            state.isLoading -> LoadingBox()
            imagePosts.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(Icons.Outlined.PhotoLibrary, stringResource(R.string.explore_empty), stringResource(R.string.explore_subtitle))
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(3.dp)
            ) {
                items(imagePosts, key = { it.id }) { post ->
                    AsyncImage(
                        model = post.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable { onOpenPost(post.id) },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun PostDetailScreen(
    postId: String,
    onBack: () -> Unit,
    onOpenComments: (String) -> Unit,
    detailViewModel: com.zam.photos.app.viewmodel.PostDetailViewModel = koinViewModel { parametersOf(postId) },
    commentsViewModel: CommentsViewModel = koinViewModel { parametersOf(postId) },
    feedViewModel: FeedViewModel = activityKoinViewModel()
) {
    val detailState by detailViewModel.state.collectAsState()
    val commentsState by commentsViewModel.state.collectAsState()
    val post = detailState.post
    val context = LocalContext.current

    RefreshOnResume(postId) {
        detailViewModel.reload()
        commentsViewModel.loadComments()
        feedViewModel.loadFeed(silent = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.post_detail), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 4.dp))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.appBorder)

        if (post == null) {
            LoadingBox()
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Row(
                    modifier = Modifier
                        .widthIn(max = MaxFeedWidth)
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(name = post.author.name, imageUrl = post.author.profileImageUrl.takeIf { it.isNotBlank() }, size = 40.dp)
                    Column(modifier = Modifier.padding(start = 10.dp)) {
                        Text(post.author.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        Text(post.createdAt, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.appMuted)
                    }
                }
            }
            item {
                if (!post.imageUrl.isNullOrBlank()) {
                    AdaptivePostImage(
                        url = post.imageUrl,
                        modifier = Modifier.widthIn(max = MaxFeedWidth)
                    )
                }
            }
            item {
                PostActionsRow(
                    post = post,
                    onCommentClick = { onOpenComments(postId) },
                    onLikeClick = { detailViewModel.toggleLike() },
                    onShareClick = { sharePost(context, post) },
                    modifier = Modifier
                        .widthIn(max = MaxFeedWidth)
                        .padding(horizontal = 22.dp, vertical = 14.dp)
                )
                if (post.content.isNotBlank()) {
                    Text(
                        post.content,
                        modifier = Modifier
                            .widthIn(max = MaxFeedWidth)
                            .padding(horizontal = 22.dp)
                            .padding(bottom = 18.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = InkSecondary
                    )
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.appBorder,
                    modifier = Modifier.widthIn(max = MaxFeedWidth).padding(horizontal = 22.dp)
                )
            }
            item {
                Column(
                    modifier = Modifier
                        .widthIn(max = MaxFeedWidth)
                        .padding(horizontal = 22.dp, vertical = 16.dp)
                ) {
                    Text(
                        stringResource(R.string.comments_section),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.appMuted,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    commentsState.comments.take(2).forEach { comment ->
                        InlineCommentRow(comment)
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                    if (post.comments > 0) {
                        Text(
                            stringResource(R.string.view_all_comments, post.comments),
                            color = Terracotta,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.clickable { onOpenComments(postId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommentsScreen(
    postId: String,
    onBack: () -> Unit,
    viewModel: CommentsViewModel = koinViewModel { parametersOf(postId) },
    feedViewModel: FeedViewModel = activityKoinViewModel(),
    profileViewModel: com.zam.photos.app.viewmodel.ProfileViewModel = activityKoinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val profileState by profileViewModel.state.collectAsState()
    var commentText by remember { mutableStateOf("") }
    val user = profileState.user

    RefreshOnResume(postId) { viewModel.loadComments() }

    Box(modifier = Modifier.fillMaxSize().imePadding()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x59000000))
                .clickable(onClick = onBack)
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(BorderStripe)
                )
            }
            Text(
                text = stringResource(R.string.comments_count_title, state.comments.size),
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.appBorder)

            when {
                state.isLoading -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Terracotta)
                }
                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .padding(horizontal = 22.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.comments, key = { it.id }) { comment ->
                        CommentItem(
                            comment = comment,
                            isOwn = user?.id == comment.authorId,
                            onReply = { viewModel.setReplyTo(comment) },
                            onDelete = {
                                viewModel.deleteComment(comment.id) { removed ->
                                    feedViewModel.onCommentCountChanged(postId, -removed)
                                }
                            },
                            onEdit = { viewModel.updateComment(comment.id, it) }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.appBorder)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (user != null) {
                    Avatar(name = user.name, imageUrl = user.profileImageUrl.takeIf { it.isNotBlank() }, size = 32.dp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.appSurfaceWarm)
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        decorationBox = { inner ->
                            if (commentText.isEmpty()) {
                                val hint = state.replyTo?.let { stringResource(R.string.reply_to, it.author.name) }
                                    ?: stringResource(R.string.add_comment)
                                Text(hint, color = MaterialTheme.colorScheme.appMuted, style = MaterialTheme.typography.bodyMedium)
                            }
                            inner()
                        }
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            viewModel.addComment(commentText) {
                                feedViewModel.onCommentCountChanged(postId, 1)
                            }
                            commentText = ""
                        }
                    },
                    enabled = !state.isSending && commentText.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Terracotta)
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    isOwn: Boolean = false,
    onReply: () -> Unit = {},
    onDelete: () -> Unit = {},
    onEdit: (String) -> Unit = {}
) {
    var menuOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(comment.content) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = if (comment.parentId != null) 28.dp else 0.dp),
        verticalAlignment = Alignment.Top
    ) {
        Avatar(
            name = comment.author.name,
            imageUrl = comment.author.profileImageUrl.takeIf { it.isNotBlank() },
            size = 32.dp
        )
        Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
            if (editing) {
                androidx.compose.foundation.text.BasicTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth()
                )
                Row {
                    TextButton(onClick = {
                        onEdit(editText)
                        editing = false
                    }) { Text(stringResource(R.string.save)) }
                    TextButton(onClick = { editing = false }) { Text(stringResource(R.string.back)) }
                }
            } else {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(comment.author.name) }
                        append(" ")
                        withStyle(SpanStyle(color = InkSecondary)) { append(comment.content) }
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row(modifier = Modifier.padding(top = 4.dp)) {
                Text(comment.createdAt, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.appMuted)
                Text(
                    " · ${stringResource(R.string.reply)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.appMuted,
                    modifier = Modifier.clickable(onClick = onReply)
                )
            }
        }
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(0.dp))
            }
            if (isOwn) {
                Text("···", modifier = Modifier.clickable { menuOpen = true }.padding(4.dp), color = MaterialTheme.colorScheme.appMuted)
                androidx.compose.material3.DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit_comment)) },
                        onClick = {
                            menuOpen = false
                            editing = true
                        }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete_comment)) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InlineCommentRow(comment: Comment) {
    Row(verticalAlignment = Alignment.Top) {
        Avatar(name = comment.author.name, imageUrl = comment.author.profileImageUrl.takeIf { it.isNotBlank() }, size = 30.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(comment.author.name) }
                append(" ")
                withStyle(SpanStyle(color = InkSecondary)) { append(comment.content) }
            },
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: com.zam.photos.app.viewmodel.ProfileViewModel = activityKoinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val user = state.user
    val context = LocalContext.current
    val totalHearts = state.posts.sumOf { it.likes }
    val gridColumns = exploreColumnCount(LocalConfiguration.current.screenWidthDp)

    RefreshOnResume { viewModel.loadProfile() }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.profile), style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (state.isLoading && user == null) {
            LoadingBox()
        } else if (user != null) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Avatar(
                            name = user.name,
                            imageUrl = user.profileImageUrl.takeIf { it.isNotBlank() },
                            size = 96.dp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(user.name, style = MaterialTheme.typography.headlineSmall)
                        if (user.email.isNotBlank()) {
                            Text(
                                user.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.appMuted,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                        Text(
                            user.username,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.appMuted,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_google),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.Unspecified
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.connected_with_google),
                                style = MaterialTheme.typography.labelMedium,
                                color = InkSecondary
                            )
                        }
                        if (user.bio.isNotBlank()) {
                            Text(
                                user.bio,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.appMuted,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileStat(value = "${state.posts.size}", label = stringResource(R.string.stat_posts))
                        ProfileStat(value = "$totalHearts", label = stringResource(R.string.stat_hearts))
                        ProfileStat(value = "${user.followerCount.coerceAtLeast(1)}", label = stringResource(R.string.stat_family))
                    }
                }
                if (state.error != null) {
                    item {
                        Text(
                            state.error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp)
                        )
                    }
                }
                val imagePosts = state.posts.filter { !it.imageUrl.isNullOrBlank() }
                val gridRows = imagePosts.chunked(gridColumns)
                items(gridRows.size) { index ->
                    val rowPosts = gridRows[index]
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        rowPosts.forEach { post ->
                            AsyncImage(
                                model = post.imageUrl,
                                contentDescription = null,
                                modifier = Modifier.weight(1f).aspectRatio(1f),
                                contentScale = ContentScale.Crop
                            )
                        }
                        repeat(gridColumns - rowPosts.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                item {
                    OutlinedButton(
                        onClick = { viewModel.logout(context, onLogout) },
                        modifier = Modifier.fillMaxWidth().padding(22.dp)
                    ) {
                        Text(stringResource(R.string.logout))
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    Icons.Outlined.Person,
                    stringResource(R.string.profile),
                    state.error ?: stringResource(R.string.error_feed)
                )
            }
        }
    }
}

private fun exploreColumnCount(screenWidthDp: Int): Int = when {
    screenWidthDp >= 1100 -> 5
    screenWidthDp >= 700 -> 4
    else -> 3
}

@Composable
private fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.appMuted, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun LoadingBox() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Terracotta)
    }
}
