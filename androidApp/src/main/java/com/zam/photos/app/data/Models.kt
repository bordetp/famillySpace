package com.zam.photos.app.data

data class UserProfile(
    val id: String,
    val name: String,
    val username: String,
    val email: String = "",
    val bio: String,
    val profileImageUrl: String,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val approvalStatus: String = "approved"
) {
    val isApproved: Boolean get() = approvalStatus.equals("approved", ignoreCase = true)
    val isPending: Boolean get() = approvalStatus.equals("pending", ignoreCase = true)
    val isRejected: Boolean get() = approvalStatus.equals("rejected", ignoreCase = true)
}

data class Post(
    val id: String,
    val authorId: String,
    val author: UserProfile,
    val content: String,
    val imageUrl: String? = null,
    val createdAt: String,
    val likes: Int = 0,
    val comments: Int = 0,
    val shares: Int = 0,
    val isLiked: Boolean = false
)

data class Comment(
    val id: String,
    val postId: String,
    val authorId: String,
    val author: UserProfile,
    val content: String,
    val createdAt: String,
    val parentId: String? = null,
    val likes: Int = 0,
    val isLiked: Boolean = false
)

enum class NotificationType {
    LIKE,
    COMMENT,
    NEW_POST,
    MENTION,
    JOIN,
    MESSAGE
}

data class AppNotification(
    val id: String,
    val type: NotificationType,
    val actorName: String,
    val actorImageUrl: String? = null,
    val message: String,
    val timeAgo: String,
    val postId: String? = null,
    val conversationId: String? = null,
    val isRead: Boolean = false
)

data class Conversation(
    val id: String,
    val type: String,
    val title: String,
    val members: List<UserProfile>,
    val lastMessage: ChatMessage?,
    val unreadCount: Int,
    val updatedAt: String
)

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val authorId: String,
    val author: UserProfile,
    val body: String,
    val imageUrl: String?,
    val createdAt: String
)

data class FamilyCircle(
    val id: String,
    val name: String,
    val inviteCode: String,
    val createdBy: String,
    val memberCount: Int,
    val conversationId: String?,
    val members: List<UserProfile>
)
