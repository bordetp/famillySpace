package com.zam.photos.app.viewmodel

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

data class FeedUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val posts: List<com.zam.photos.app.data.Post> = emptyList(),
    val hasMore: Boolean = true,
    val error: String? = null
)

data class CreatePostUiState(
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

data class CommentsUiState(
    val isLoading: Boolean = true,
    val comments: List<com.zam.photos.app.data.Comment> = emptyList(),
    val error: String? = null,
    val isSending: Boolean = false,
    val replyTo: com.zam.photos.app.data.Comment? = null
)

data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: com.zam.photos.app.data.UserProfile? = null,
    val posts: List<com.zam.photos.app.data.Post> = emptyList(),
    val error: String? = null
)

data class NotificationsUiState(
    val notifications: List<com.zam.photos.app.data.AppNotification> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class InboxUiState(
    val conversations: List<com.zam.photos.app.data.Conversation> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

data class ChatThreadUiState(
    val messages: List<com.zam.photos.app.data.ChatMessage> = emptyList(),
    val title: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val isSending: Boolean = false
)

data class NewConversationUiState(
    val query: String = "",
    val results: List<com.zam.photos.app.data.UserProfile> = emptyList(),
    val selected: List<com.zam.photos.app.data.UserProfile> = emptyList(),
    val groupTitle: String = "",
    val isGroup: Boolean = false,
    val error: String? = null
)

data class SettingsUiState(
    val pushEnabled: Boolean = true,
    val family: com.zam.photos.app.data.FamilyCircle? = null,
    val familyName: String = "",
    val inviteCode: String = "",
    val error: String? = null,
    val isLoading: Boolean = true
)

data class PostDetailUiState(
    val post: com.zam.photos.app.data.Post? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

enum class ModerationTab { Users, Posts, Comments }

data class ModerationUiState(
    val tab: ModerationTab = ModerationTab.Users,
    val users: List<com.zam.photos.app.data.UserProfile> = emptyList(),
    val posts: List<com.zam.photos.app.data.Post> = emptyList(),
    val comments: List<com.zam.photos.app.data.Comment> = emptyList(),
    val usersTotal: Int = 0,
    val postsTotal: Int = 0,
    val commentsTotal: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val deletingId: String? = null
)
