package com.zam.shared

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val username: String,
    val displayName: String,
    val bio: String = "",
    val avatarUrl: String? = null,
    val approvalStatus: String = "approved"
)

@Serializable
data class PostDto(
    val id: String,
    val authorId: String,
    val author: UserDto,
    val content: String,
    val imageUrl: String? = null,
    val createdAt: String,
    val commentCount: Int = 0,
    val likeCount: Int = 0,
    val isLiked: Boolean = false
)

@Serializable
data class CommentDto(
    val id: String,
    val postId: String,
    val authorId: String,
    val author: UserDto,
    val content: String,
    val createdAt: String,
    val parentId: String? = null
)

@Serializable
data class GoogleAuthRequest(
    val idToken: String
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val userId: String,
    val user: UserDto
)

@Serializable
data class CreatePostRequest(
    val content: String,
    val imageUrl: String? = null
)

@Serializable
data class CommentRequest(
    val content: String,
    val parentId: String? = null
)

@Serializable
data class UpdateCommentRequest(
    val content: String
)

@Serializable
data class UploadResponse(
    val url: String
)

@Serializable
data class ErrorResponse(
    val error: String,
    val code: String? = null
)

@Serializable
data class PostsPageResponse(
    val posts: List<PostDto>,
    val total: Int
)

@Serializable
data class CommentsPageResponse(
    val comments: List<CommentDto>,
    val total: Int
)

@Serializable
data class UsersPageResponse(
    val users: List<UserDto>,
    val total: Int
)

@Serializable
data class UpdateApprovalRequest(
    val status: String
)

@Serializable
data class HealthResponse(
    val status: String = "ok"
)

@Serializable
data class LikeResponse(
    val liked: Boolean,
    val likeCount: Int
)

@Serializable
data class NotificationDto(
    val id: String,
    val type: String,
    val actor: UserDto?,
    val message: String,
    val postId: String? = null,
    val commentId: String? = null,
    val conversationId: String? = null,
    val createdAt: String,
    val isRead: Boolean
)

@Serializable
data class NotificationsPageResponse(
    val notifications: List<NotificationDto>,
    val unreadCount: Int
)

@Serializable
data class ConversationDto(
    val id: String,
    val type: String,
    val title: String?,
    val members: List<UserDto>,
    val lastMessage: MessageDto? = null,
    val unreadCount: Int = 0,
    val updatedAt: String
)

@Serializable
data class MessageDto(
    val id: String,
    val conversationId: String,
    val authorId: String,
    val author: UserDto,
    val body: String,
    val imageUrl: String? = null,
    val createdAt: String
)

@Serializable
data class CreateDmRequest(
    val userId: String
)

@Serializable
data class CreateGroupRequest(
    val title: String,
    val memberIds: List<String> = emptyList()
)

@Serializable
data class SendMessageRequest(
    val body: String,
    val imageUrl: String? = null
)

@Serializable
data class FamilyDto(
    val id: String,
    val name: String,
    val inviteCode: String,
    val createdBy: String,
    val memberCount: Int,
    val conversationId: String? = null,
    val members: List<UserDto> = emptyList()
)

@Serializable
data class CreateFamilyRequest(
    val name: String
)

@Serializable
data class JoinFamilyRequest(
    val code: String
)

@Serializable
data class FcmTokenRequest(
    val token: String
)

@Serializable
data class NotificationSettingsRequest(
    val pushEnabled: Boolean
)

@Serializable
data class NotificationSettingsDto(
    val pushEnabled: Boolean
)

@Serializable
data class ChatEventDto(
    val type: String,
    val conversationId: String? = null,
    val message: MessageDto? = null
)

@Serializable
data class FamilyMeResponse(
    val family: FamilyDto? = null
)
