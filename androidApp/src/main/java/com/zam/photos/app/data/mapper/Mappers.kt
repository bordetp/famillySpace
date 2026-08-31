package com.zam.photos.app.data.mapper

import com.zam.photos.app.data.AppNotification
import com.zam.photos.app.data.ChatMessage
import com.zam.photos.app.data.Comment
import com.zam.photos.app.data.Conversation
import com.zam.photos.app.data.FamilyCircle
import com.zam.photos.app.data.NotificationType
import com.zam.photos.app.data.Post
import com.zam.photos.app.data.UserProfile
import com.zam.shared.CommentDto
import com.zam.shared.ConversationDto
import com.zam.shared.FamilyDto
import com.zam.shared.MessageDto
import com.zam.shared.NotificationDto
import com.zam.shared.PostDto
import com.zam.shared.UserDto
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val zone = ZoneId.systemDefault()

fun UserDto.toProfile() = UserProfile(
    id = id,
    name = displayName.ifBlank { username },
    username = "@$username",
    email = email,
    bio = bio,
    profileImageUrl = normalizeGoogleAvatarUrl(avatarUrl),
    approvalStatus = approvalStatus
)

fun normalizeGoogleAvatarUrl(avatarUrl: String?): String {
    val url = avatarUrl?.trim().orEmpty()
    if (url.isBlank()) return ""
    return url
        .replace(Regex("=s\\d+(-c)?$"), "=s256-c")
        .replace(Regex("/s\\d+-c/"), "/s256-c/")
}

fun PostDto.toUiPost() = Post(
    id = id,
    authorId = authorId,
    author = author.toProfile(),
    content = content,
    imageUrl = imageUrl,
    createdAt = formatRelativeTime(createdAt),
    likes = likeCount,
    comments = commentCount,
    isLiked = isLiked
)

fun CommentDto.toUiComment() = Comment(
    id = id,
    postId = postId,
    authorId = authorId,
    author = author.toProfile(),
    content = content,
    createdAt = formatRelativeTime(createdAt),
    parentId = parentId
)

fun NotificationDto.toUiNotification(): AppNotification {
    val actorUser = actor
    return AppNotification(
        id = id,
        type = runCatching { NotificationType.valueOf(type) }.getOrDefault(NotificationType.COMMENT),
        actorName = actorUser?.displayName?.ifBlank { actorUser.username } ?: "Family Space",
        actorImageUrl = actorUser?.avatarUrl?.let { normalizeGoogleAvatarUrl(it) },
        message = message,
        timeAgo = formatRelativeTime(createdAt),
        postId = postId,
        conversationId = conversationId,
        isRead = isRead
    )
}

fun ConversationDto.toUiConversation() = Conversation(
    id = id,
    type = type,
    title = title ?: members.firstOrNull()?.displayName.orEmpty(),
    members = members.map { it.toProfile() },
    lastMessage = lastMessage?.toUiMessage(),
    unreadCount = unreadCount,
    updatedAt = formatRelativeTime(updatedAt)
)

fun MessageDto.toUiMessage() = ChatMessage(
    id = id,
    conversationId = conversationId,
    authorId = authorId,
    author = author.toProfile(),
    body = body,
    imageUrl = imageUrl,
    createdAt = formatRelativeTime(createdAt)
)

fun FamilyDto.toUiFamily() = FamilyCircle(
    id = id,
    name = name,
    inviteCode = inviteCode,
    createdBy = createdBy,
    memberCount = memberCount,
    conversationId = conversationId,
    members = members.map { it.toProfile() }
)

fun formatRelativeTime(isoTimestamp: String): String {
    return try {
        val instant = OffsetDateTime.parse(isoTimestamp).toInstant()
        val now = OffsetDateTime.now().toInstant()
        val minutes = ChronoUnit.MINUTES.between(instant, now)
        when {
            minutes < 1 -> "À l'instant"
            minutes < 60 -> "Il y a ${minutes} min"
            minutes < 1440 -> "Il y a ${minutes / 60} h"
            minutes < 10080 -> "Il y a ${minutes / 1440} j"
            else -> DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(zone).format(instant)
        }
    } catch (_: Exception) {
        isoTimestamp
    }
}
