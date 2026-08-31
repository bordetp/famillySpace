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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zam.photos.app.R
import com.zam.photos.app.data.Conversation
import com.zam.photos.app.ui.components.Avatar
import com.zam.photos.app.ui.components.EmptyState
import com.zam.photos.app.ui.components.RefreshOnResume
import com.zam.photos.app.di.activityKoinViewModel
import com.zam.photos.app.ui.theme.BorderLight
import com.zam.photos.app.ui.theme.SurfaceWarm
import com.zam.photos.app.ui.theme.Terracotta
import com.zam.photos.app.ui.theme.TextMuted
import com.zam.photos.app.viewmodel.ChatThreadViewModel
import com.zam.photos.app.viewmodel.InboxViewModel
import com.zam.photos.app.viewmodel.NewConversationViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MessagesInboxScreen(
    onOpenConversation: (String) -> Unit,
    onNewConversation: () -> Unit,
    viewModel: InboxViewModel = activityKoinViewModel()
) {
    val state by viewModel.state.collectAsState()
    RefreshOnResume { viewModel.refresh() }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                stringResource(R.string.messages),
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                style = MaterialTheme.typography.titleLarge
            )
            HorizontalDivider(color = BorderLight)
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(color = Terracotta)
                }
                state.conversations.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        Icons.Outlined.ChatBubbleOutline,
                        stringResource(R.string.messages_empty),
                        stringResource(R.string.messages_empty_now)
                    )
                }
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.conversations, key = { it.id }) { conversation ->
                        ConversationRow(conversation) { onOpenConversation(conversation.id) }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onNewConversation,
            containerColor = Terracotta,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(22.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.new_conversation))
        }
    }
}

@Composable
private fun ConversationRow(conversation: Conversation, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val name = conversation.title.ifBlank { conversation.members.firstOrNull()?.name ?: "?" }
        Avatar(name = name, imageUrl = conversation.members.firstOrNull()?.profileImageUrl, size = 48.dp)
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(name, fontWeight = FontWeight.SemiBold)
            Text(
                conversation.lastMessage?.body ?: stringResource(R.string.no_messages_yet),
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(conversation.updatedAt, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            if (conversation.unreadCount > 0) {
                Box(
                    modifier = Modifier.padding(top = 6.dp).clip(RoundedCornerShape(10.dp)).background(Terracotta).padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("${conversation.unreadCount}", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
    HorizontalDivider(color = BorderLight, modifier = Modifier.padding(start = 82.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadScreen(
    conversationId: String,
    onBack: () -> Unit,
    viewModel: ChatThreadViewModel = koinViewModel { parametersOf(conversationId) }
) {
    val state by viewModel.state.collectAsState()
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    RefreshOnResume(conversationId) { viewModel.reload() }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).imePadding()) {
        TopAppBar(
            title = { Text(state.title.ifBlank { stringResource(R.string.messages) }) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            }
        )
        HorizontalDivider(color = BorderLight)
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.messages, key = { it.id }) { message ->
                val mine = false
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (message.body.isNotEmpty()) Arrangement.Start else Arrangement.Start) {
                    Avatar(name = message.author.name, imageUrl = message.author.profileImageUrl.takeIf { it.isNotBlank() }, size = 28.dp)
                    Column(
                        modifier = Modifier.padding(start = 8.dp).clip(RoundedCornerShape(14.dp)).background(SurfaceWarm).padding(10.dp)
                    ) {
                        Text(message.author.name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        Text(message.body, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        HorizontalDivider(color = BorderLight)
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(SurfaceWarm).padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    decorationBox = { inner ->
                        if (text.isEmpty()) Text(stringResource(R.string.message_hint), color = TextMuted)
                        inner()
                    }
                )
            }
            IconButton(
                onClick = {
                    viewModel.send(text)
                    text = ""
                },
                enabled = text.isNotBlank() && !state.isSending
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.send), tint = Terracotta)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewConversationScreen(
    onBack: () -> Unit,
    onOpened: (String) -> Unit,
    viewModel: NewConversationViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        TopAppBar(
            title = { Text(stringResource(R.string.new_conversation)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            },
            actions = {
                Text(
                    stringResource(R.string.start_chat),
                    color = Terracotta,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(end = 16.dp).clickable { viewModel.start(onOpened) }
                )
            }
        )
        HorizontalDivider(color = BorderLight)
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !state.isGroup,
                onClick = { viewModel.toggleGroup(false) },
                label = { Text(stringResource(R.string.direct_message)) }
            )
            FilterChip(
                selected = state.isGroup,
                onClick = { viewModel.toggleGroup(true) },
                label = { Text(stringResource(R.string.group_chat)) }
            )
        }
        if (state.isGroup) {
            OutlinedTextField(
                value = state.groupTitle,
                onValueChange = viewModel::updateGroupTitle,
                label = { Text(stringResource(R.string.group_name)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::updateQuery,
            label = { Text(stringResource(R.string.search_people)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true
        )
        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 12.dp)) {
            items(state.results, key = { it.id }) { user ->
                val selected = state.selected.any { it.id == user.id }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleUser(user) }.padding(horizontal = 22.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(name = user.name, imageUrl = user.profileImageUrl.takeIf { it.isNotBlank() }, size = 40.dp)
                    Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(user.name, fontWeight = FontWeight.SemiBold)
                        Text(user.username, color = TextMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    if (selected) Text("✓", color = Terracotta, fontWeight = FontWeight.Bold)
                }
            }
        }
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
        }
    }
}
