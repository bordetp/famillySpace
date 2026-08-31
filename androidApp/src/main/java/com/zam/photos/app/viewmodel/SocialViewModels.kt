package com.zam.photos.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zam.photos.app.data.api.chatEvents
import com.zam.photos.app.data.local.TokenStore
import com.zam.photos.app.data.mapper.toUiMessage
import com.zam.photos.app.data.repository.ApiResult
import com.zam.photos.app.data.repository.ChatRepository
import com.zam.photos.app.data.repository.DeviceRepository
import com.zam.photos.app.data.repository.FamilyRepository
import com.zam.photos.app.data.repository.PostRepository
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InboxViewModel(private val chatRepository: ChatRepository) : ViewModel() {
    private val _state = MutableStateFlow(InboxUiState())
    val state: StateFlow<InboxUiState> = _state.asStateFlow()

    val unreadTotal: Int
        get() = _state.value.conversations.sumOf { it.unreadCount }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = _state.value.conversations.isEmpty())
            when (val result = chatRepository.inbox()) {
                is ApiResult.Success -> _state.value = InboxUiState(conversations = result.data, isLoading = false)
                is ApiResult.Error -> _state.value = _state.value.copy(isLoading = false, error = result.message)
            }
        }
    }
}

class ChatThreadViewModel(
    private val chatRepository: ChatRepository,
    private val client: HttpClient,
    private val tokenStore: TokenStore,
    private val conversationId: String
) : ViewModel() {
    private val _state = MutableStateFlow(ChatThreadUiState())
    val state: StateFlow<ChatThreadUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            load()
            chatRepository.markRead(conversationId)
            chatEvents(client, tokenStore).collect { event ->
                val message = event.message ?: return@collect
                if (event.conversationId == conversationId) {
                    val mapped = message.toUiMessage()
                    if (_state.value.messages.none { it.id == mapped.id }) {
                        _state.value = _state.value.copy(messages = _state.value.messages + mapped)
                    }
                    chatRepository.markRead(conversationId)
                }
            }
        }
    }

    fun reload() {
        viewModelScope.launch {
            load()
            chatRepository.markRead(conversationId)
        }
    }

    private suspend fun load() {
        when (val inbox = chatRepository.inbox()) {
            is ApiResult.Success -> {
                val conv = inbox.data.find { it.id == conversationId }
                _state.value = _state.value.copy(title = conv?.title.orEmpty())
            }
            is ApiResult.Error -> Unit
        }
        when (val result = chatRepository.messages(conversationId)) {
            is ApiResult.Success -> _state.value = _state.value.copy(isLoading = false, messages = result.data)
            is ApiResult.Error -> _state.value = _state.value.copy(isLoading = false, error = result.message)
        }
    }

    fun send(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isSending = true)
            when (val result = chatRepository.send(conversationId, text.trim())) {
                is ApiResult.Success -> {
                    val exists = _state.value.messages.any { it.id == result.data.id }
                    _state.value = _state.value.copy(
                        isSending = false,
                        messages = if (exists) _state.value.messages else _state.value.messages + result.data
                    )
                }
                is ApiResult.Error -> _state.value = _state.value.copy(isSending = false, error = result.message)
            }
        }
    }
}

class NewConversationViewModel(private val chatRepository: ChatRepository) : ViewModel() {
    private val _state = MutableStateFlow(NewConversationUiState())
    val state: StateFlow<NewConversationUiState> = _state.asStateFlow()

    fun updateQuery(query: String) {
        _state.value = _state.value.copy(query = query)
        if (query.length < 2) {
            _state.value = _state.value.copy(results = emptyList())
            return
        }
        viewModelScope.launch {
            when (val result = chatRepository.searchUsers(query)) {
                is ApiResult.Success -> _state.value = _state.value.copy(results = result.data)
                is ApiResult.Error -> _state.value = _state.value.copy(error = result.message)
            }
        }
    }

    fun toggleGroup(enabled: Boolean) {
        _state.value = _state.value.copy(isGroup = enabled)
    }

    fun updateGroupTitle(title: String) {
        _state.value = _state.value.copy(groupTitle = title)
    }

    fun toggleUser(user: com.zam.photos.app.data.UserProfile) {
        val selected = _state.value.selected.toMutableList()
        if (selected.any { it.id == user.id }) selected.removeAll { it.id == user.id } else selected.add(user)
        _state.value = _state.value.copy(selected = selected)
    }

    fun start(onOpened: (String) -> Unit) {
        viewModelScope.launch {
            val selected = _state.value.selected
            if (selected.isEmpty()) return@launch
            val result = if (_state.value.isGroup) {
                chatRepository.createGroup(_state.value.groupTitle.ifBlank { "Groupe" }, selected.map { it.id })
            } else {
                chatRepository.openDm(selected.first().id)
            }
            when (result) {
                is ApiResult.Success -> onOpened(result.data.id)
                is ApiResult.Error -> _state.value = _state.value.copy(error = result.message)
            }
        }
    }
}

class SettingsViewModel(
    private val familyRepository: FamilyRepository,
    private val deviceRepository: DeviceRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val family = when (val result = familyRepository.me()) {
                is ApiResult.Success -> result.data
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(error = result.message, isLoading = false)
                    return@launch
                }
            }
            val push = when (val result = deviceRepository.notificationSettings()) {
                is ApiResult.Success -> result.data
                is ApiResult.Error -> true
            }
            _state.value = SettingsUiState(pushEnabled = push, family = family, isLoading = false)
        }
    }

    fun setFamilyName(value: String) {
        _state.value = _state.value.copy(familyName = value)
    }

    fun setInviteCode(value: String) {
        _state.value = _state.value.copy(inviteCode = value)
    }

    fun setPushEnabled(enabled: Boolean) {
        viewModelScope.launch {
            when (val result = deviceRepository.setPushEnabled(enabled)) {
                is ApiResult.Success -> _state.value = _state.value.copy(pushEnabled = result.data)
                is ApiResult.Error -> _state.value = _state.value.copy(error = result.message)
            }
        }
    }

    fun createFamily() {
        viewModelScope.launch {
            when (val result = familyRepository.create(_state.value.familyName)) {
                is ApiResult.Success -> _state.value = _state.value.copy(family = result.data, error = null)
                is ApiResult.Error -> _state.value = _state.value.copy(error = result.message)
            }
        }
    }

    fun joinFamily() {
        viewModelScope.launch {
            when (val result = familyRepository.join(_state.value.inviteCode)) {
                is ApiResult.Success -> _state.value = _state.value.copy(family = result.data, error = null)
                is ApiResult.Error -> _state.value = _state.value.copy(error = result.message)
            }
        }
    }

    fun leaveFamily() {
        viewModelScope.launch {
            when (familyRepository.leave()) {
                is ApiResult.Success -> _state.value = _state.value.copy(family = null)
                is ApiResult.Error -> _state.value = _state.value.copy(error = "Impossible de quitter la famille")
            }
        }
    }
}

class PostDetailViewModel(
    private val postRepository: PostRepository,
    private val postId: String
) : ViewModel() {
    private val _state = MutableStateFlow(PostDetailUiState())
    val state: StateFlow<PostDetailUiState> = _state.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            when (val result = postRepository.getPost(postId)) {
                is ApiResult.Success -> _state.value = PostDetailUiState(post = result.data, isLoading = false)
                is ApiResult.Error -> {
                    if (_state.value.post == null) {
                        _state.value = PostDetailUiState(isLoading = false, error = result.message)
                    }
                }
            }
        }
    }

    fun toggleLike() {
        val post = _state.value.post ?: return
        viewModelScope.launch {
            when (val result = postRepository.toggleLike(post.id)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        post = post.copy(isLiked = result.data.liked, likes = result.data.likeCount)
                    )
                }
                is ApiResult.Error -> Unit
            }
        }
    }
}
