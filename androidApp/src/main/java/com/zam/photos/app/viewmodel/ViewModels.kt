package com.zam.photos.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zam.photos.app.BuildConfig
import com.zam.photos.app.auth.GoogleSignInHelper
import com.zam.photos.app.auth.GoogleSignInResult
import com.zam.photos.app.data.repository.AuthRepository
import com.zam.photos.app.debug.AuthDebugLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SessionViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _isLoggedIn = MutableStateFlow<Boolean?>(null)
    val isLoggedIn: StateFlow<Boolean?> = _isLoggedIn.asStateFlow()

    init {
        viewModelScope.launch {
            _isLoggedIn.value = authRepository.isLoggedIn()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoggedIn.value = authRepository.isLoggedIn()
        }
    }

    fun markLoggedIn() {
        _isLoggedIn.value = true
    }
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val googleSignInHelper: GoogleSignInHelper
) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun beginGoogleSignIn(
        activity: android.app.Activity,
        launchSignIn: (androidx.activity.result.IntentSenderRequest) -> Unit,
        onSuccess: () -> Unit,
    ) {
        AuthDebugLog.log("Auth: début connexion (API=${BuildConfig.API_BASE_URL})")
        _state.value = AuthUiState(isLoading = true, error = null)
        googleSignInHelper.beginSignIn(
            activity = activity,
            onReady = launchSignIn,
            onError = { result -> handleGoogleSignInResult(result, onSuccess) },
        )
    }

    fun completeGoogleSignIn(
        activity: android.app.Activity,
        data: android.content.Intent?,
        onSuccess: () -> Unit,
    ) {
        handleGoogleSignInResult(googleSignInHelper.parseSignInResult(activity, data), onSuccess)
    }

    fun cancelGoogleSignIn(resultCode: Int? = null) {
        AuthDebugLog.log(
            if (resultCode != null) {
                "Auth: annulé (resultCode=$resultCode, attendu ${android.app.Activity.RESULT_OK})"
            } else {
                "Auth: annulé"
            }
        )
        _state.value = AuthUiState()
    }

    private fun handleGoogleSignInResult(googleResult: GoogleSignInResult, onSuccess: () -> Unit) {
        viewModelScope.launch {
            when (googleResult) {
                is GoogleSignInResult.Success -> {
                    AuthDebugLog.log("Auth: envoi idToken au backend…")
                    when (val result = authRepository.signInWithGoogle(googleResult.idToken)) {
                        is com.zam.photos.app.data.repository.ApiResult.Success -> {
                            AuthDebugLog.log("Auth: backend OK — session enregistrée")
                            _state.value = AuthUiState()
                            AuthDebugLog.log("Auth: navigation vers le feed")
                            onSuccess()
                        }
                        is com.zam.photos.app.data.repository.ApiResult.Error -> {
                            AuthDebugLog.log("Auth: backend ERREUR — ${result.message}")
                            _state.value = AuthUiState(error = result.message)
                        }
                    }
                }
                is GoogleSignInResult.Cancelled -> {
                    AuthDebugLog.log("Auth: Google annulé (pas de token)")
                    _state.value = AuthUiState()
                }
                is GoogleSignInResult.Error -> {
                    AuthDebugLog.log("Auth: Google ERREUR — ${googleResult.message}")
                    _state.value = AuthUiState(error = googleResult.message)
                }
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun devBypassLogin(onSuccess: () -> Unit) {
        if (!BuildConfig.DEV_AUTH_BYPASS) return
        viewModelScope.launch {
            AuthDebugLog.log("Auth: connexion test (bypass dev)…")
            _state.value = AuthUiState(isLoading = true, error = null)
            when (val result = authRepository.signInDevBypass(BuildConfig.DEV_AUTH_SECRET)) {
                is com.zam.photos.app.data.repository.ApiResult.Success -> {
                    AuthDebugLog.log("Auth: bypass OK — navigation feed")
                    _state.value = AuthUiState()
                    onSuccess()
                }
                is com.zam.photos.app.data.repository.ApiResult.Error -> {
                    AuthDebugLog.log("Auth: bypass ERREUR — ${result.message}")
                    _state.value = AuthUiState(error = result.message)
                }
            }
        }
    }
}

class FeedViewModel(
    private val postRepository: com.zam.photos.app.data.repository.PostRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(FeedUiState())
    val state: StateFlow<FeedUiState> = _state.asStateFlow()
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    init {
        viewModelScope.launch { _currentUserId.value = authRepository.currentUserId() }
        loadFeed()
    }

    fun loadFeed(silent: Boolean = false) {
        viewModelScope.launch {
            _currentUserId.value = authRepository.currentUserId()
            val hasPosts = _state.value.posts.isNotEmpty()
            if (!silent) {
                _state.value = _state.value.copy(
                    isLoading = !hasPosts,
                    isRefreshing = hasPosts,
                    error = null
                )
            }
            when (val result = postRepository.getFeed(limit = PAGE_SIZE, offset = 0)) {
                is com.zam.photos.app.data.repository.ApiResult.Success -> {
                    _state.value = FeedUiState(
                        isLoading = false,
                        isRefreshing = false,
                        posts = result.data.posts,
                        hasMore = result.data.posts.size < result.data.total
                    )
                }
                is com.zam.photos.app.data.repository.ApiResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = if (silent && hasPosts) _state.value.error else result.message
                    )
                }
            }
        }
    }

    fun onCommentCountChanged(postId: String, delta: Int) {
        if (delta == 0) return
        _state.value = _state.value.copy(
            posts = _state.value.posts.map { post ->
                if (post.id == postId) post.copy(comments = (post.comments + delta).coerceAtLeast(0))
                else post
            }
        )
    }

    fun loadMore() {
        val current = _state.value
        if (current.isLoadingMore || !current.hasMore || current.isLoading) return
        viewModelScope.launch {
            _state.value = current.copy(isLoadingMore = true)
            when (val result = postRepository.getFeed(limit = PAGE_SIZE, offset = current.posts.size)) {
                is com.zam.photos.app.data.repository.ApiResult.Success -> {
                    val merged = current.posts + result.data.posts
                    _state.value = _state.value.copy(
                        isLoadingMore = false,
                        posts = merged,
                        hasMore = merged.size < result.data.total
                    )
                }
                is com.zam.photos.app.data.repository.ApiResult.Error -> {
                    _state.value = _state.value.copy(isLoadingMore = false)
                }
            }
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            when (val result = postRepository.toggleLike(postId)) {
                is com.zam.photos.app.data.repository.ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        posts = _state.value.posts.map { post ->
                            if (post.id == postId) post.copy(isLiked = result.data.liked, likes = result.data.likeCount)
                            else post
                        }
                    )
                }
                is com.zam.photos.app.data.repository.ApiResult.Error -> Unit
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            when (postRepository.deletePost(postId)) {
                is com.zam.photos.app.data.repository.ApiResult.Success -> {
                    _state.value = _state.value.copy(posts = _state.value.posts.filter { it.id != postId })
                }
                is com.zam.photos.app.data.repository.ApiResult.Error -> Unit
            }
        }
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}

class CreatePostViewModel(private val postRepository: com.zam.photos.app.data.repository.PostRepository) : ViewModel() {
    private val _state = MutableStateFlow(CreatePostUiState())
    val state: StateFlow<CreatePostUiState> = _state.asStateFlow()

    fun createPost(content: String, imageUrl: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.value = CreatePostUiState(isLoading = true)
            when (val result = postRepository.createPost(content, imageUrl)) {
                is com.zam.photos.app.data.repository.ApiResult.Success -> {
                    _state.value = CreatePostUiState(success = true)
                    onSuccess()
                }
                is com.zam.photos.app.data.repository.ApiResult.Error -> {
                    _state.value = CreatePostUiState(error = result.message)
                }
            }
        }
    }

    fun uploadImage(bytes: ByteArray, filename: String, onUploaded: (String) -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUploading = true, error = null)
            when (val result = postRepository.uploadImage(bytes, filename)) {
                is com.zam.photos.app.data.repository.ApiResult.Success -> {
                    _state.value = _state.value.copy(isUploading = false)
                    onUploaded(result.data)
                }
                is com.zam.photos.app.data.repository.ApiResult.Error -> {
                    _state.value = _state.value.copy(isUploading = false, error = result.message)
                }
            }
        }
    }

    fun reset() {
        _state.value = CreatePostUiState()
    }
}

class CommentsViewModel(
    private val postRepository: com.zam.photos.app.data.repository.PostRepository,
    private val postId: String
) : ViewModel() {
    private val _state = MutableStateFlow(CommentsUiState())
    val state: StateFlow<CommentsUiState> = _state.asStateFlow()

    init { loadComments() }

    fun loadComments() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = postRepository.getComments(postId)) {
                is com.zam.photos.app.data.repository.ApiResult.Success -> {
                    _state.value = CommentsUiState(isLoading = false, comments = result.data)
                }
                is com.zam.photos.app.data.repository.ApiResult.Error -> {
                    _state.value = CommentsUiState(isLoading = false, error = result.message)
                }
            }
        }
    }

    fun addComment(content: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSending = true)
            val parentId = _state.value.replyTo?.id
            when (val result = postRepository.addComment(postId, content, parentId)) {
                is com.zam.photos.app.data.repository.ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        isSending = false,
                        comments = _state.value.comments + result.data,
                        replyTo = null
                    )
                    onSuccess()
                }
                is com.zam.photos.app.data.repository.ApiResult.Error -> {
                    _state.value = _state.value.copy(isSending = false, error = result.message)
                }
            }
        }
    }

    fun setReplyTo(comment: com.zam.photos.app.data.Comment?) {
        _state.value = _state.value.copy(replyTo = comment)
    }

    fun updateComment(commentId: String, content: String) {
        viewModelScope.launch {
            when (val result = postRepository.updateComment(commentId, content)) {
                is com.zam.photos.app.data.repository.ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        comments = _state.value.comments.map { if (it.id == commentId) result.data else it }
                    )
                }
                is com.zam.photos.app.data.repository.ApiResult.Error -> {
                    _state.value = _state.value.copy(error = result.message)
                }
            }
        }
    }

    fun deleteComment(commentId: String, onRemoved: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val removedCount = _state.value.comments.count { it.id == commentId || it.parentId == commentId }
            when (val result = postRepository.deleteComment(commentId)) {
                is com.zam.photos.app.data.repository.ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        comments = _state.value.comments.filter { it.id != commentId && it.parentId != commentId }
                    )
                    onRemoved(removedCount)
                }
                is com.zam.photos.app.data.repository.ApiResult.Error -> {
                    _state.value = _state.value.copy(error = result.message)
                }
            }
        }
    }
}

class ProfileViewModel(private val authRepository: AuthRepository, private val postRepository: com.zam.photos.app.data.repository.PostRepository) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (authRepository.isLoggedIn()) loadProfile()
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            val cached = authRepository.cachedUser()
            if (cached != null && _state.value.user == null) {
                _state.value = _state.value.copy(user = cached, isLoading = true, error = null)
            } else {
                _state.value = _state.value.copy(isLoading = true, error = null)
            }

            val user = authRepository.currentUser()
            if (user == null) {
                _state.value = ProfileUiState(isLoading = false, error = "Session expirée")
                return@launch
            }
            if (!user.isApproved) {
                _state.value = ProfileUiState(isLoading = false, user = user)
                return@launch
            }
            when (val postsResult = postRepository.getUserPosts()) {
                is com.zam.photos.app.data.repository.ApiResult.Success -> {
                    _state.value = ProfileUiState(isLoading = false, user = user, posts = postsResult.data)
                }
                is com.zam.photos.app.data.repository.ApiResult.Error -> {
                    _state.value = ProfileUiState(isLoading = false, user = user, error = postsResult.message)
                }
            }
        }
    }

    fun logout(context: android.content.Context, onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout(context)
            _state.value = ProfileUiState()
            onLoggedOut()
        }
    }
}

class NotificationsViewModel(
    private val notificationRepository: com.zam.photos.app.data.repository.NotificationRepository
) : ViewModel() {
    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = _state.value.notifications.isEmpty())
            when (val result = notificationRepository.list()) {
                is com.zam.photos.app.data.repository.ApiResult.Success -> {
                    _state.value = NotificationsUiState(
                        notifications = result.data.first,
                        unreadCount = result.data.second,
                        isLoading = false
                    )
                }
                is com.zam.photos.app.data.repository.ApiResult.Error -> {
                    _state.value = _state.value.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    fun onScreenOpened() {
        viewModelScope.launch {
            notificationRepository.markAllRead()
            refresh()
        }
    }

    fun onNotificationClick(id: String) {
        viewModelScope.launch {
            notificationRepository.markRead(id)
            _state.value = _state.value.copy(
                notifications = _state.value.notifications.map {
                    if (it.id == id) it.copy(isRead = true) else it
                },
                unreadCount = _state.value.notifications.count { !it.isRead && it.id != id }
            )
        }
    }
}

class ModerationViewModel(
    private val adminRepository: com.zam.photos.app.data.repository.AdminRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ModerationUiState())
    val state: StateFlow<ModerationUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun selectTab(tab: ModerationTab) {
        _state.value = _state.value.copy(tab = tab, error = null)
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (_state.value.tab) {
                ModerationTab.Users -> loadUsers()
                ModerationTab.Posts -> loadPosts()
                ModerationTab.Comments -> loadComments()
            }
        }
    }

    private suspend fun loadUsers() {
        when (val result = adminRepository.listUsers(status = null)) {
            is com.zam.photos.app.data.repository.ApiResult.Success -> {
                _state.value = _state.value.copy(
                    users = sortUsersForModeration(result.data.first),
                    usersTotal = result.data.second,
                    isLoading = false
                )
            }
            is com.zam.photos.app.data.repository.ApiResult.Error -> {
                _state.value = _state.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    private fun sortUsersForModeration(users: List<com.zam.photos.app.data.UserProfile>): List<com.zam.photos.app.data.UserProfile> {
        val priority = mapOf("pending" to 0, "approved" to 1, "rejected" to 2)
        return users.sortedWith(
            compareBy(
                { priority[it.approvalStatus.lowercase()] ?: 3 },
                { it.name.lowercase() }
            )
        )
    }

    private suspend fun loadPosts() {
        when (val result = adminRepository.listPosts()) {
            is com.zam.photos.app.data.repository.ApiResult.Success -> {
                _state.value = _state.value.copy(
                    posts = result.data.posts,
                    postsTotal = result.data.total,
                    isLoading = false
                )
            }
            is com.zam.photos.app.data.repository.ApiResult.Error -> {
                _state.value = _state.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    private suspend fun loadComments() {
        when (val result = adminRepository.listComments()) {
            is com.zam.photos.app.data.repository.ApiResult.Success -> {
                _state.value = _state.value.copy(
                    comments = result.data.first,
                    commentsTotal = result.data.second,
                    isLoading = false
                )
            }
            is com.zam.photos.app.data.repository.ApiResult.Error -> {
                _state.value = _state.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(deletingId = postId, error = null)
            when (val result = adminRepository.deletePost(postId)) {
                is com.zam.photos.app.data.repository.ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        posts = _state.value.posts.filterNot { it.id == postId },
                        postsTotal = (_state.value.postsTotal - 1).coerceAtLeast(0),
                        deletingId = null
                    )
                }
                is com.zam.photos.app.data.repository.ApiResult.Error -> {
                    _state.value = _state.value.copy(deletingId = null, error = result.message)
                }
            }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(deletingId = commentId, error = null)
            when (val result = adminRepository.deleteComment(commentId)) {
                is com.zam.photos.app.data.repository.ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        comments = _state.value.comments.filterNot { it.id == commentId },
                        commentsTotal = (_state.value.commentsTotal - 1).coerceAtLeast(0),
                        deletingId = null
                    )
                }
                is com.zam.photos.app.data.repository.ApiResult.Error -> {
                    _state.value = _state.value.copy(deletingId = null, error = result.message)
                }
            }
        }
    }

    fun approveUser(userId: String) = setUserApproval(userId, "approved")

    fun rejectUser(userId: String) = setUserApproval(userId, "rejected")

    private fun setUserApproval(userId: String, status: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(deletingId = userId, error = null)
            when (val result = adminRepository.setUserApproval(userId, status)) {
                is com.zam.photos.app.data.repository.ApiResult.Success -> {
                    val updatedUsers = sortUsersForModeration(
                        _state.value.users.map { if (it.id == userId) result.data else it }
                    )
                    _state.value = _state.value.copy(
                        users = updatedUsers,
                        deletingId = null
                    )
                }
                is com.zam.photos.app.data.repository.ApiResult.Error -> {
                    _state.value = _state.value.copy(deletingId = null, error = result.message)
                }
            }
        }
    }
}
