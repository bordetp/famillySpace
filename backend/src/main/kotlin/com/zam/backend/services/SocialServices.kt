package com.zam.backend.services

import com.zam.backend.AppConfig
import com.zam.backend.realtime.ChatHub
import com.zam.backend.repository.CommentRepository
import com.zam.backend.repository.CommentWithAuthor
import com.zam.backend.repository.ConversationRecord
import com.zam.backend.repository.ConversationRepository
import com.zam.backend.repository.FamilyRecord
import com.zam.backend.repository.FamilyRepository
import com.zam.backend.repository.FcmTokenRepository
import com.zam.backend.repository.LikeRepository
import com.zam.backend.repository.MessageRecord
import com.zam.backend.repository.NotificationRecord
import com.zam.backend.repository.NotificationRepository
import com.zam.backend.repository.PostRepository
import com.zam.backend.repository.PostWithAuthor
import com.zam.backend.repository.UserEntity
import com.zam.backend.repository.UserRepository
import com.zam.backend.repository.toDto
import com.zam.shared.ChatEventDto
import com.zam.shared.CommentDto
import com.zam.shared.ConversationDto
import com.zam.shared.FamilyDto
import com.zam.shared.LikeResponse
import com.zam.shared.MessageDto
import com.zam.shared.NotificationDto
import com.zam.shared.NotificationSettingsDto
import com.zam.shared.PostDto
import com.zam.shared.UserDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

private val dateFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

class PostService(
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val likeRepository: LikeRepository,
    private val familyRepository: FamilyRepository,
    private val notificationService: NotificationService
) {
    fun listPosts(viewerId: UUID, limit: Int, offset: Int): Pair<List<PostDto>, Int> {
        val safeLimit = limit.coerceIn(1, 50)
        val safeOffset = offset.coerceAtLeast(0)
        val family = familyRepository.findByMember(viewerId)
        val authorIds = family?.let { familyRepository.memberIds(it.id) }
        val posts = postRepository.list(viewerId, safeLimit, safeOffset, authorIds).map { it.toDto() }
        val total = postRepository.count(authorIds)
        return posts to total
    }

    fun getPost(id: UUID, viewerId: UUID): PostDto {
        return postRepository.findById(id, viewerId)?.toDto()
            ?: throw ValidationException("Post not found", "NOT_FOUND")
    }

    fun createPost(authorId: UUID, content: String, imageUrl: String?): PostDto {
        val trimmed = content.trim()
        if (trimmed.isEmpty() && imageUrl.isNullOrBlank()) {
            throw ValidationException("Post must have content or an image", "EMPTY_POST")
        }
        if (trimmed.length > 2000) {
            throw ValidationException("Post content too long", "CONTENT_TOO_LONG")
        }
        val post = postRepository.create(authorId, trimmed, imageUrl)
        val family = familyRepository.findByMember(authorId)
        if (family != null) {
            familyRepository.memberIds(family.id)
                .filter { it != authorId }
                .forEach { memberId ->
                    notificationService.notify(
                        recipientId = memberId,
                        actorId = authorId,
                        type = "NEW_POST",
                        message = "a partagé une nouvelle publication",
                        postId = post.id
                    )
                }
        }
        return post.toDto()
    }

    fun deletePost(postId: UUID, userId: UUID) {
        val post = postRepository.findById(postId, userId)
            ?: throw ValidationException("Post not found", "NOT_FOUND")
        if (post.authorId != userId) {
            throw ValidationException("You can only delete your own posts", "FORBIDDEN")
        }
        postRepository.delete(postId, userId)
    }

    fun toggleLike(postId: UUID, userId: UUID): LikeResponse {
        val post = postRepository.findById(postId, userId)
            ?: throw ValidationException("Post not found", "NOT_FOUND")
        val liked = likeRepository.toggle(userId, postId)
        if (liked && post.authorId != userId) {
            notificationService.notify(
                recipientId = post.authorId,
                actorId = userId,
                type = "LIKE",
                message = "a aimé votre publication",
                postId = postId
            )
        }
        return LikeResponse(liked = liked, likeCount = likeRepository.count(postId))
    }

    fun getComments(postId: UUID): List<CommentDto> {
        if (postRepository.findById(postId) == null) {
            throw ValidationException("Post not found", "NOT_FOUND")
        }
        return commentRepository.listByPost(postId).map { it.toDto() }
    }

    fun addComment(postId: UUID, authorId: UUID, content: String, parentId: UUID?): CommentDto {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) {
            throw ValidationException("Comment cannot be empty", "EMPTY_COMMENT")
        }
        val post = postRepository.findById(postId)
            ?: throw ValidationException("Post not found", "NOT_FOUND")
        if (parentId != null && commentRepository.findById(parentId) == null) {
            throw ValidationException("Parent comment not found", "NOT_FOUND")
        }
        val comment = commentRepository.create(postId, authorId, trimmed, parentId)
        if (post.authorId != authorId) {
            notificationService.notify(
                recipientId = post.authorId,
                actorId = authorId,
                type = "COMMENT",
                message = "a commenté : « ${trimmed.take(80)} »",
                postId = postId,
                commentId = comment.id
            )
        }
        return comment.toDto()
    }

    fun updateComment(commentId: UUID, userId: UUID, content: String): CommentDto {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) {
            throw ValidationException("Comment cannot be empty", "EMPTY_COMMENT")
        }
        return commentRepository.update(commentId, userId, trimmed)?.toDto()
            ?: throw ValidationException("Comment not found", "NOT_FOUND")
    }

    fun deleteComment(commentId: UUID, userId: UUID) {
        if (!commentRepository.delete(commentId, userId)) {
            throw ValidationException("Comment not found", "NOT_FOUND")
        }
    }

    fun userPosts(userId: UUID): List<PostDto> {
        return postRepository.listByAuthor(userId, userId).map { it.toDto() }
    }
}

class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    private val fcmTokenRepository: FcmTokenRepository,
    private val fcmSender: FcmSender
) {
    fun notify(
        recipientId: UUID,
        actorId: UUID?,
        type: String,
        message: String,
        postId: UUID? = null,
        commentId: UUID? = null,
        conversationId: UUID? = null
    ) {
        notificationRepository.create(recipientId, actorId, type, message, postId, commentId, conversationId)
        val recipient = userRepository.findById(recipientId) ?: return
        if (!recipient.pushEnabled) return
        val actorName = actorId?.let { userRepository.findById(it)?.displayName } ?: "Family Space"
        val data = buildMap {
            postId?.let { put("postId", it.toString()) }
            commentId?.let { put("commentId", it.toString()) }
            conversationId?.let { put("conversationId", it.toString()) }
        }
        fcmSender.send(fcmTokenRepository.tokensFor(recipientId), actorName, message, data)
    }

    fun list(userId: UUID): Pair<List<NotificationDto>, Int> {
        val records = notificationRepository.listForUser(userId)
        val unread = notificationRepository.unreadCount(userId)
        return records.map { it.toDto() } to unread
    }

    fun markAllRead(userId: UUID) = notificationRepository.markAllRead(userId)

    fun markRead(userId: UUID, id: UUID) = notificationRepository.markRead(userId, id)

    private fun NotificationRecord.toDto(): NotificationDto {
        val actor = actorId?.let { userRepository.findById(it)?.toDto() }
        return NotificationDto(
            id = id.toString(),
            type = type,
            actor = actor,
            message = message,
            postId = postId?.toString(),
            commentId = commentId?.toString(),
            conversationId = conversationId?.toString(),
            createdAt = createdAt.format(dateFormatter),
            isRead = isRead
        )
    }
}

class ChatService(
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService,
    private val chatHub: ChatHub
) {
    fun inbox(userId: UUID): List<ConversationDto> {
        return conversationRepository.listForUser(userId).map { it.toDto(userId) }
    }

    fun getOrCreateDm(userId: UUID, otherId: UUID): ConversationDto {
        if (userId == otherId) {
            throw ValidationException("Cannot message yourself", "INVALID_DM")
        }
        userRepository.findById(otherId) ?: throw ValidationException("User not found", "NOT_FOUND")
        val existing = conversationRepository.findDm(userId, otherId)
        val conv = existing ?: conversationRepository.create("dm", null, userId, listOf(otherId))
        return conv.toDto(userId)
    }

    fun createGroup(userId: UUID, title: String, memberIds: List<UUID>): ConversationDto {
        val name = title.trim()
        if (name.isEmpty()) throw ValidationException("Group title required", "EMPTY_TITLE")
        val members = memberIds.filter { it != userId }.distinct()
        members.forEach {
            userRepository.findById(it) ?: throw ValidationException("User not found", "NOT_FOUND")
        }
        val conv = conversationRepository.create("group", name, userId, members)
        return conv.toDto(userId)
    }

    fun messages(userId: UUID, conversationId: UUID, before: OffsetDateTime?, limit: Int): List<MessageDto> {
        ensureMember(conversationId, userId)
        return conversationRepository.listMessages(conversationId, before, limit.coerceIn(1, 100))
            .map { it.toDto() }
    }

    suspend fun sendMessage(userId: UUID, conversationId: UUID, body: String, imageUrl: String?): MessageDto {
        ensureMember(conversationId, userId)
        val trimmed = body.trim()
        if (trimmed.isEmpty() && imageUrl.isNullOrBlank()) {
            throw ValidationException("Message cannot be empty", "EMPTY_MESSAGE")
        }
        val record = conversationRepository.insertMessage(conversationId, userId, trimmed, imageUrl)
        conversationRepository.markRead(conversationId, userId)
        val dto = record.toDto()
        val payload = json.encodeToString(ChatEventDto(type = "message", conversationId = conversationId.toString(), message = dto))
        conversationRepository.memberIds(conversationId).filter { it != userId }.forEach { memberId ->
            chatHub.sendTo(memberId, payload)
            notificationService.notify(
                recipientId = memberId,
                actorId = userId,
                type = "MESSAGE",
                message = if (trimmed.isNotBlank()) trimmed.take(80) else "a envoyé une photo",
                conversationId = conversationId
            )
        }
        return dto
    }

    fun markRead(userId: UUID, conversationId: UUID) {
        ensureMember(conversationId, userId)
        conversationRepository.markRead(conversationId, userId)
    }

    fun searchUsers(userId: UUID, query: String): List<UserDto> {
        if (query.trim().length < 2) return emptyList()
        return userRepository.search(query, userId).map { it.toDto() }
    }

    private fun ensureMember(conversationId: UUID, userId: UUID) {
        if (!conversationRepository.isMember(conversationId, userId)) {
            throw ValidationException("Conversation not found", "NOT_FOUND")
        }
    }

    private fun ConversationRecord.toDto(viewerId: UUID): ConversationDto {
        val members = conversationRepository.memberIds(id).mapNotNull { userRepository.findById(it)?.toDto() }
        val last = conversationRepository.lastMessage(id)
        return ConversationDto(
            id = id.toString(),
            type = type,
            title = title ?: members.firstOrNull { it.id != viewerId.toString() }?.displayName,
            members = members,
            lastMessage = last?.toDto(),
            unreadCount = conversationRepository.unreadCount(id, viewerId),
            updatedAt = (last?.createdAt ?: createdAt).format(dateFormatter)
        )
    }

    private fun MessageRecord.toDto(): MessageDto {
        val author = userRepository.findById(authorId)?.toDto()
            ?: UserDto(authorId.toString(), "", "user", "Utilisateur")
        return MessageDto(
            id = id.toString(),
            conversationId = conversationId.toString(),
            authorId = authorId.toString(),
            author = author,
            body = body,
            imageUrl = imageUrl,
            createdAt = createdAt.format(dateFormatter)
        )
    }
}

class FamilyService(
    private val familyRepository: FamilyRepository,
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService
) {
    fun me(userId: UUID): FamilyDto? {
        val family = familyRepository.findByMember(userId) ?: return null
        return family.toDto()
    }

    fun create(userId: UUID, name: String): FamilyDto {
        if (familyRepository.findByMember(userId) != null) {
            throw ValidationException("You already belong to a family", "ALREADY_IN_FAMILY")
        }
        val trimmed = name.trim()
        if (trimmed.isEmpty()) throw ValidationException("Family name required", "EMPTY_NAME")
        val code = generateCode()
        val family = familyRepository.create(trimmed, userId, code, null)
        val conversation = conversationRepository.create("group", trimmed, userId, emptyList(), family.id)
        familyRepository.setConversationId(family.id, conversation.id)
        return familyRepository.findById(family.id)!!.toDto()
    }

    fun join(userId: UUID, code: String): FamilyDto {
        if (familyRepository.findByMember(userId) != null) {
            throw ValidationException("You already belong to a family", "ALREADY_IN_FAMILY")
        }
        val family = familyRepository.findByInviteCode(code.trim())
            ?: throw ValidationException("Invalid invite code", "INVALID_CODE")
        familyRepository.addMember(family.id, userId)
        family.conversationId?.let { conversationRepository.addMember(it, userId) }
        familyRepository.memberIds(family.id).filter { it != userId }.forEach { memberId ->
            notificationService.notify(
                recipientId = memberId,
                actorId = userId,
                type = "JOIN",
                message = "a rejoint ${family.name}"
            )
        }
        return familyRepository.findById(family.id)!!.toDto()
    }

    fun leave(userId: UUID) {
        val family = familyRepository.findByMember(userId)
            ?: throw ValidationException("You are not in a family", "NOT_IN_FAMILY")
        familyRepository.removeMember(family.id, userId)
    }

    private fun FamilyRecord.toDto(): FamilyDto {
        val members = familyRepository.memberIds(id).mapNotNull { userRepository.findById(it)?.toDto() }
        return FamilyDto(
            id = id.toString(),
            name = name,
            inviteCode = inviteCode,
            createdBy = createdBy.toString(),
            memberCount = members.size,
            conversationId = conversationId?.toString(),
            members = members
        )
    }

    private fun generateCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..8).map { alphabet.random() }.joinToString("")
    }
}

class DeviceService(
    private val userRepository: UserRepository,
    private val fcmTokenRepository: FcmTokenRepository
) {
    fun saveToken(userId: UUID, token: String) {
        if (token.isBlank()) throw ValidationException("Token required", "EMPTY_TOKEN")
        fcmTokenRepository.save(userId, token.trim())
    }

    fun settings(userId: UUID): NotificationSettingsDto {
        val user = userRepository.findById(userId) ?: throw ValidationException("User not found", "NOT_FOUND")
        return NotificationSettingsDto(user.pushEnabled)
    }

    fun updateSettings(userId: UUID, pushEnabled: Boolean): NotificationSettingsDto {
        userRepository.setPushEnabled(userId, pushEnabled)
        return NotificationSettingsDto(pushEnabled)
    }
}

fun PostWithAuthor.toDto() = PostDto(
    id = id.toString(),
    authorId = authorId.toString(),
    author = author,
    content = content,
    imageUrl = imageUrl,
    createdAt = createdAt,
    commentCount = commentCount,
    likeCount = likeCount,
    isLiked = isLiked
)

fun CommentWithAuthor.toDto() = CommentDto(
    id = id.toString(),
    postId = postId.toString(),
    authorId = authorId.toString(),
    author = author,
    content = content,
    createdAt = createdAt,
    parentId = parentId?.toString()
)
