package com.zam.backend.repository

import com.zam.shared.UserDto
import java.time.OffsetDateTime
import java.util.UUID

data class UserEntity(
    val id: UUID,
    val email: String,
    val passwordHash: String?,
    val googleId: String?,
    val username: String,
    val displayName: String,
    val bio: String,
    val avatarUrl: String?,
    val pushEnabled: Boolean = true,
    val approvalStatus: String = "pending",
    val createdAt: OffsetDateTime? = null
)

interface UserRepository {
    fun createFromGoogle(
        googleId: String,
        email: String,
        username: String,
        displayName: String,
        avatarUrl: String?,
        approvalStatus: String = "pending"
    ): UserEntity

    fun findByGoogleId(googleId: String): UserEntity?
    fun findByEmail(email: String): UserEntity?
    fun findById(id: UUID): UserEntity?
    fun linkGoogleAccount(userId: UUID, googleId: String, displayName: String, avatarUrl: String?): UserEntity
    fun syncGoogleProfile(userId: UUID, displayName: String, avatarUrl: String?, email: String): UserEntity
    fun usernameExists(username: String): Boolean
    fun search(query: String, excludeId: UUID, limit: Int = 20): List<UserEntity>
    fun setPushEnabled(userId: UUID, enabled: Boolean)
    fun listByApprovalStatus(status: String?, limit: Int, offset: Int): List<UserEntity>
    fun countByApprovalStatus(status: String?): Int
    fun setApprovalStatus(userId: UUID, status: String): UserEntity?
}

interface PostRepository {
    fun create(authorId: UUID, content: String, imageUrl: String?): PostWithAuthor
    fun list(viewerId: UUID, limit: Int, offset: Int, authorIds: List<UUID>? = null): List<PostWithAuthor>
    fun count(authorIds: List<UUID>? = null): Int
    fun findById(id: UUID, viewerId: UUID? = null): PostWithAuthor?
    fun listByAuthor(authorId: UUID, viewerId: UUID): List<PostWithAuthor>
    fun delete(id: UUID, authorId: UUID): Boolean
    fun deleteById(id: UUID): Boolean
}

interface CommentRepository {
    fun create(postId: UUID, authorId: UUID, content: String, parentId: UUID? = null): CommentWithAuthor
    fun listByPost(postId: UUID): List<CommentWithAuthor>
    fun listAll(limit: Int, offset: Int): List<CommentWithAuthor>
    fun countAll(): Int
    fun countByPost(postId: UUID): Int
    fun findById(id: UUID): CommentWithAuthor?
    fun update(id: UUID, authorId: UUID, content: String): CommentWithAuthor?
    fun delete(id: UUID, authorId: UUID): Boolean
    fun deleteById(id: UUID): Boolean
}

interface LikeRepository {
    fun toggle(userId: UUID, postId: UUID): Boolean
    fun count(postId: UUID): Int
    fun isLiked(userId: UUID, postId: UUID): Boolean
}

data class NotificationRecord(
    val id: UUID,
    val recipientId: UUID,
    val actorId: UUID?,
    val type: String,
    val message: String,
    val postId: UUID?,
    val commentId: UUID?,
    val conversationId: UUID?,
    val createdAt: OffsetDateTime,
    val isRead: Boolean
)

interface NotificationRepository {
    fun create(
        recipientId: UUID,
        actorId: UUID?,
        type: String,
        message: String,
        postId: UUID? = null,
        commentId: UUID? = null,
        conversationId: UUID? = null
    ): NotificationRecord

    fun listForUser(userId: UUID, limit: Int = 50): List<NotificationRecord>
    fun unreadCount(userId: UUID): Int
    fun markAllRead(userId: UUID)
    fun markRead(userId: UUID, id: UUID)
}

data class ConversationRecord(
    val id: UUID,
    val type: String,
    val title: String?,
    val createdBy: UUID?,
    val familyId: UUID?,
    val createdAt: OffsetDateTime
)

data class MessageRecord(
    val id: UUID,
    val conversationId: UUID,
    val authorId: UUID,
    val body: String,
    val imageUrl: String?,
    val createdAt: OffsetDateTime
)

interface ConversationRepository {
    fun create(type: String, title: String?, createdBy: UUID, memberIds: List<UUID>, familyId: UUID? = null): ConversationRecord
    fun findById(id: UUID): ConversationRecord?
    fun findDm(userA: UUID, userB: UUID): ConversationRecord?
    fun listForUser(userId: UUID): List<ConversationRecord>
    fun memberIds(conversationId: UUID): List<UUID>
    fun isMember(conversationId: UUID, userId: UUID): Boolean
    fun addMember(conversationId: UUID, userId: UUID)
    fun lastMessage(conversationId: UUID): MessageRecord?
    fun unreadCount(conversationId: UUID, userId: UUID): Int
    fun markRead(conversationId: UUID, userId: UUID)
    fun insertMessage(conversationId: UUID, authorId: UUID, body: String, imageUrl: String?): MessageRecord
    fun listMessages(conversationId: UUID, before: OffsetDateTime?, limit: Int): List<MessageRecord>
}

data class FamilyRecord(
    val id: UUID,
    val name: String,
    val inviteCode: String,
    val createdBy: UUID,
    val conversationId: UUID?,
    val createdAt: OffsetDateTime
)

interface FamilyRepository {
    fun create(name: String, createdBy: UUID, inviteCode: String, conversationId: UUID?): FamilyRecord
    fun findById(id: UUID): FamilyRecord?
    /** The single family for this deployment, if any. */
    fun findPrimary(): FamilyRecord?
    fun findByMember(userId: UUID): FamilyRecord?
    fun findByInviteCode(code: String): FamilyRecord?
    fun addMember(familyId: UUID, userId: UUID, role: String = "member")
    fun removeMember(familyId: UUID, userId: UUID)
    fun memberIds(familyId: UUID): List<UUID>
    fun memberCount(familyId: UUID): Int
    fun setConversationId(familyId: UUID, conversationId: UUID)
}

interface FcmTokenRepository {
    fun save(userId: UUID, token: String)
    fun tokensFor(userId: UUID): List<String>
    fun delete(userId: UUID, token: String)
}

data class PostWithAuthor(
    val id: UUID,
    val authorId: UUID,
    val author: UserDto,
    val content: String,
    val imageUrl: String?,
    val createdAt: String,
    val commentCount: Int,
    val likeCount: Int = 0,
    val isLiked: Boolean = false
)

data class CommentWithAuthor(
    val id: UUID,
    val postId: UUID,
    val authorId: UUID,
    val author: UserDto,
    val content: String,
    val createdAt: String,
    val parentId: UUID? = null
)

fun UserEntity.toDto() = UserDto(
    id = id.toString(),
    email = email,
    username = username,
    displayName = displayName,
    bio = bio,
    avatarUrl = avatarUrl,
    approvalStatus = approvalStatus
)
