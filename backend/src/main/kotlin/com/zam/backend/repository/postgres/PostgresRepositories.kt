package com.zam.backend.repository.postgres

import com.zam.backend.db.CommentsTable
import com.zam.backend.db.ConversationMembersTable
import com.zam.backend.db.ConversationsTable
import com.zam.backend.db.FamiliesTable
import com.zam.backend.db.FamilyMembersTable
import com.zam.backend.db.FcmTokensTable
import com.zam.backend.db.LikesTable
import com.zam.backend.db.MessagesTable
import com.zam.backend.db.NotificationsTable
import com.zam.backend.db.PostsTable
import com.zam.backend.db.UsersTable
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
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

private val dateFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

class PostgresUserRepository : UserRepository {
    override fun createFromGoogle(
        googleId: String,
        email: String,
        username: String,
        displayName: String,
        avatarUrl: String?,
        approvalStatus: String
    ): UserEntity = transaction {
        val id = UUID.randomUUID()
        val now = OffsetDateTime.now()
        UsersTable.insert {
            it[UsersTable.id] = id
            it[UsersTable.email] = email
            it[UsersTable.passwordHash] = null
            it[UsersTable.googleId] = googleId
            it[UsersTable.username] = username
            it[UsersTable.displayName] = displayName
            it[UsersTable.bio] = ""
            it[UsersTable.avatarUrl] = avatarUrl
            it[UsersTable.createdAt] = now
            it[UsersTable.pushEnabled] = true
            it[UsersTable.approvalStatus] = approvalStatus
        }
        UserEntity(id, email, null, googleId, username, displayName, "", avatarUrl, true, approvalStatus, now)
    }

    override fun findByGoogleId(googleId: String): UserEntity? = transaction {
        UsersTable.select { UsersTable.googleId eq googleId }.singleOrNull()?.toEntity()
    }

    override fun findByEmail(email: String): UserEntity? = transaction {
        UsersTable.select { UsersTable.email eq email }.singleOrNull()?.toEntity()
    }

    override fun findById(id: UUID): UserEntity? = transaction {
        UsersTable.select { UsersTable.id eq id }.singleOrNull()?.toEntity()
    }

    override fun linkGoogleAccount(
        userId: UUID,
        googleId: String,
        displayName: String,
        avatarUrl: String?
    ): UserEntity = transaction {
        UsersTable.update({ UsersTable.id eq userId }) {
            it[UsersTable.googleId] = googleId
            it[UsersTable.displayName] = displayName
            if (avatarUrl != null) {
                it[UsersTable.avatarUrl] = avatarUrl
            }
        }
        findById(userId)!!
    }

    override fun syncGoogleProfile(
        userId: UUID,
        displayName: String,
        avatarUrl: String?,
        email: String
    ): UserEntity = transaction {
        UsersTable.update({ UsersTable.id eq userId }) {
            it[UsersTable.displayName] = displayName
            it[UsersTable.email] = email
            if (avatarUrl != null) {
                it[UsersTable.avatarUrl] = avatarUrl
            }
        }
        findById(userId)!!
    }

    override fun usernameExists(username: String): Boolean = transaction {
        UsersTable.select { UsersTable.username eq username }.count() > 0
    }

    override fun search(query: String, excludeId: UUID, limit: Int): List<UserEntity> = transaction {
        val q = "%${query.trim().lowercase()}%"
        UsersTable
            .select {
                (UsersTable.id neq excludeId) and (
                    (UsersTable.username.lowerCase() like q) or
                        (UsersTable.displayName.lowerCase() like q) or
                        (UsersTable.email.lowerCase() like q)
                    )
            }
            .limit(limit)
            .map { it.toEntity() }
    }

    override fun setPushEnabled(userId: UUID, enabled: Boolean) {
        transaction {
            UsersTable.update({ UsersTable.id eq userId }) {
                it[pushEnabled] = enabled
            }
        }
    }

    override fun listByApprovalStatus(status: String?, limit: Int, offset: Int): List<UserEntity> = transaction {
        val query = if (status.isNullOrBlank()) {
            UsersTable.selectAll()
        } else {
            UsersTable.select { UsersTable.approvalStatus eq status }
        }
        query
            .orderBy(UsersTable.createdAt to SortOrder.DESC)
            .limit(limit, offset.toLong())
            .map { it.toEntity() }
    }

    override fun countByApprovalStatus(status: String?): Int = transaction {
        if (status.isNullOrBlank()) {
            UsersTable.selectAll().count().toInt()
        } else {
            UsersTable.select { UsersTable.approvalStatus eq status }.count().toInt()
        }
    }

    override fun setApprovalStatus(userId: UUID, status: String): UserEntity? = transaction {
        val updated = UsersTable.update({ UsersTable.id eq userId }) {
            it[UsersTable.approvalStatus] = status
        }
        if (updated == 0) null else findById(userId)
    }

    private fun ResultRow.toEntity() = UserEntity(
        id = this[UsersTable.id],
        email = this[UsersTable.email],
        passwordHash = this[UsersTable.passwordHash],
        googleId = this[UsersTable.googleId],
        username = this[UsersTable.username],
        displayName = this[UsersTable.displayName],
        bio = this[UsersTable.bio],
        avatarUrl = this[UsersTable.avatarUrl],
        pushEnabled = this[UsersTable.pushEnabled],
        approvalStatus = this[UsersTable.approvalStatus],
        createdAt = this[UsersTable.createdAt]
    )
}

class PostgresPostRepository(
    private val userRepository: UserRepository,
    private val commentRepository: CommentRepository
) : PostRepository {

    override fun create(authorId: UUID, content: String, imageUrl: String?): PostWithAuthor = transaction {
        val id = UUID.randomUUID()
        val now = OffsetDateTime.now()
        PostsTable.insert {
            it[PostsTable.id] = id
            it[PostsTable.authorId] = authorId
            it[PostsTable.content] = content
            it[PostsTable.imageUrl] = imageUrl
            it[PostsTable.createdAt] = now
        }
        val author = userRepository.findById(authorId)!!.toDto()
        PostWithAuthor(id, authorId, author, content, imageUrl, now.format(dateFormatter), 0, 0, false)
    }

    override fun list(viewerId: UUID, limit: Int, offset: Int, authorIds: List<UUID>?): List<PostWithAuthor> = transaction {
        val query = if (authorIds != null) {
            PostsTable.select { PostsTable.authorId inList authorIds }
        } else {
            PostsTable.selectAll()
        }
        query
            .orderBy(PostsTable.createdAt to SortOrder.DESC)
            .limit(limit, offset.toLong())
            .map { rowToPost(it, viewerId) }
    }

    override fun count(authorIds: List<UUID>?): Int = transaction {
        if (authorIds != null) {
            PostsTable.select { PostsTable.authorId inList authorIds }.count().toInt()
        } else {
            PostsTable.selectAll().count().toInt()
        }
    }

    override fun findById(id: UUID, viewerId: UUID?): PostWithAuthor? = transaction {
        PostsTable.select { PostsTable.id eq id }.singleOrNull()?.let { rowToPost(it, viewerId) }
    }

    override fun listByAuthor(authorId: UUID, viewerId: UUID): List<PostWithAuthor> = transaction {
        PostsTable
            .select { PostsTable.authorId eq authorId }
            .orderBy(PostsTable.createdAt to SortOrder.DESC)
            .map { rowToPost(it, viewerId) }
    }

    override fun delete(id: UUID, authorId: UUID): Boolean = transaction {
        PostsTable.deleteWhere { (PostsTable.id eq id) and (PostsTable.authorId eq authorId) } > 0
    }

    override fun deleteById(id: UUID): Boolean = transaction {
        PostsTable.deleteWhere { PostsTable.id eq id } > 0
    }

    private fun rowToPost(row: ResultRow, viewerId: UUID?): PostWithAuthor {
        val postId = row[PostsTable.id]
        val authorId = row[PostsTable.authorId]
        val author = userRepository.findById(authorId)!!.toDto()
        val likeCount = LikesTable.select { LikesTable.postId eq postId }.count().toInt()
        val isLiked = viewerId != null && LikesTable.select {
            (LikesTable.postId eq postId) and (LikesTable.userId eq viewerId)
        }.count() > 0
        return PostWithAuthor(
            id = postId,
            authorId = authorId,
            author = author,
            content = row[PostsTable.content],
            imageUrl = row[PostsTable.imageUrl],
            createdAt = row[PostsTable.createdAt].format(dateFormatter),
            commentCount = commentRepository.countByPost(postId),
            likeCount = likeCount,
            isLiked = isLiked
        )
    }
}

class PostgresCommentRepository(
    private val userRepository: UserRepository
) : CommentRepository {

    override fun create(postId: UUID, authorId: UUID, content: String, parentId: UUID?): CommentWithAuthor = transaction {
        val id = UUID.randomUUID()
        val now = OffsetDateTime.now()
        CommentsTable.insert {
            it[CommentsTable.id] = id
            it[CommentsTable.postId] = postId
            it[CommentsTable.authorId] = authorId
            it[CommentsTable.content] = content
            it[CommentsTable.createdAt] = now
            it[CommentsTable.parentId] = parentId
        }
        val author = userRepository.findById(authorId)!!.toDto()
        CommentWithAuthor(id, postId, authorId, author, content, now.format(dateFormatter), parentId)
    }

    override fun listByPost(postId: UUID): List<CommentWithAuthor> = transaction {
        CommentsTable
            .select { CommentsTable.postId eq postId }
            .orderBy(CommentsTable.createdAt to SortOrder.ASC)
            .map { rowToComment(it) }
    }

    override fun listAll(limit: Int, offset: Int): List<CommentWithAuthor> = transaction {
        CommentsTable
            .selectAll()
            .orderBy(CommentsTable.createdAt to SortOrder.DESC)
            .limit(limit, offset.toLong())
            .map { rowToComment(it) }
    }

    override fun countAll(): Int = transaction {
        CommentsTable.selectAll().count().toInt()
    }

    override fun countByPost(postId: UUID): Int = transaction {
        CommentsTable.select { CommentsTable.postId eq postId }.count().toInt()
    }

    override fun findById(id: UUID): CommentWithAuthor? = transaction {
        CommentsTable.select { CommentsTable.id eq id }.singleOrNull()?.let { rowToComment(it) }
    }

    override fun update(id: UUID, authorId: UUID, content: String): CommentWithAuthor? = transaction {
        val updated = CommentsTable.update({ (CommentsTable.id eq id) and (CommentsTable.authorId eq authorId) }) {
            it[CommentsTable.content] = content
        }
        if (updated == 0) null else findById(id)
    }

    override fun delete(id: UUID, authorId: UUID): Boolean = transaction {
        CommentsTable.deleteWhere { (CommentsTable.id eq id) and (CommentsTable.authorId eq authorId) } > 0
    }

    override fun deleteById(id: UUID): Boolean = transaction {
        CommentsTable.deleteWhere { CommentsTable.id eq id } > 0
    }

    private fun rowToComment(row: ResultRow): CommentWithAuthor {
        val authorId = row[CommentsTable.authorId]
        val author = userRepository.findById(authorId)!!.toDto()
        return CommentWithAuthor(
            id = row[CommentsTable.id],
            postId = row[CommentsTable.postId],
            authorId = authorId,
            author = author,
            content = row[CommentsTable.content],
            createdAt = row[CommentsTable.createdAt].format(dateFormatter),
            parentId = row[CommentsTable.parentId]
        )
    }
}

class PostgresLikeRepository : LikeRepository {
    override fun toggle(userId: UUID, postId: UUID): Boolean = transaction {
        val existing = LikesTable.select {
            (LikesTable.userId eq userId) and (LikesTable.postId eq postId)
        }.count() > 0
        if (existing) {
            LikesTable.deleteWhere { (LikesTable.userId eq userId) and (LikesTable.postId eq postId) }
            false
        } else {
            LikesTable.insert {
                it[LikesTable.userId] = userId
                it[LikesTable.postId] = postId
                it[createdAt] = OffsetDateTime.now()
            }
            true
        }
    }

    override fun count(postId: UUID): Int = transaction {
        LikesTable.select { LikesTable.postId eq postId }.count().toInt()
    }

    override fun isLiked(userId: UUID, postId: UUID): Boolean = transaction {
        LikesTable.select { (LikesTable.userId eq userId) and (LikesTable.postId eq postId) }.count() > 0
    }
}

class PostgresNotificationRepository : NotificationRepository {
    override fun create(
        recipientId: UUID,
        actorId: UUID?,
        type: String,
        message: String,
        postId: UUID?,
        commentId: UUID?,
        conversationId: UUID?
    ): NotificationRecord = transaction {
        val id = UUID.randomUUID()
        val now = OffsetDateTime.now()
        NotificationsTable.insert {
            it[NotificationsTable.id] = id
            it[NotificationsTable.recipientId] = recipientId
            it[NotificationsTable.actorId] = actorId
            it[NotificationsTable.type] = type
            it[NotificationsTable.message] = message
            it[NotificationsTable.postId] = postId
            it[NotificationsTable.commentId] = commentId
            it[NotificationsTable.conversationId] = conversationId
            it[readAt] = null
            it[createdAt] = now
        }
        NotificationRecord(id, recipientId, actorId, type, message, postId, commentId, conversationId, now, false)
    }

    override fun listForUser(userId: UUID, limit: Int): List<NotificationRecord> = transaction {
        NotificationsTable
            .select { NotificationsTable.recipientId eq userId }
            .orderBy(NotificationsTable.createdAt to SortOrder.DESC)
            .limit(limit)
            .map { it.toNotification() }
    }

    override fun unreadCount(userId: UUID): Int = transaction {
        NotificationsTable.select {
            (NotificationsTable.recipientId eq userId) and NotificationsTable.readAt.isNull()
        }.count().toInt()
    }

    override fun markAllRead(userId: UUID) {
        transaction {
            NotificationsTable.update({
                (NotificationsTable.recipientId eq userId) and NotificationsTable.readAt.isNull()
            }) {
                it[readAt] = OffsetDateTime.now()
            }
        }
    }

    override fun markRead(userId: UUID, id: UUID) {
        transaction {
            NotificationsTable.update({
                (NotificationsTable.id eq id) and (NotificationsTable.recipientId eq userId)
            }) {
                it[readAt] = OffsetDateTime.now()
            }
        }
    }

    private fun ResultRow.toNotification() = NotificationRecord(
        id = this[NotificationsTable.id],
        recipientId = this[NotificationsTable.recipientId],
        actorId = this[NotificationsTable.actorId],
        type = this[NotificationsTable.type],
        message = this[NotificationsTable.message],
        postId = this[NotificationsTable.postId],
        commentId = this[NotificationsTable.commentId],
        conversationId = this[NotificationsTable.conversationId],
        createdAt = this[NotificationsTable.createdAt],
        isRead = this[NotificationsTable.readAt] != null
    )
}

class PostgresConversationRepository(
    private val userRepository: UserRepository
) : ConversationRepository {
    override fun create(
        type: String,
        title: String?,
        createdBy: UUID,
        memberIds: List<UUID>,
        familyId: UUID?
    ): ConversationRecord = transaction {
        val id = UUID.randomUUID()
        val now = OffsetDateTime.now()
        ConversationsTable.insert {
            it[ConversationsTable.id] = id
            it[ConversationsTable.type] = type
            it[ConversationsTable.title] = title
            it[ConversationsTable.createdBy] = createdBy
            it[ConversationsTable.familyId] = familyId
            it[createdAt] = now
        }
        val uniqueMembers = (memberIds + createdBy).distinct()
        uniqueMembers.forEach { memberId ->
            ConversationMembersTable.insert {
                it[conversationId] = id
                it[userId] = memberId
                it[joinedAt] = now
                it[lastReadAt] = now
            }
        }
        ConversationRecord(id, type, title, createdBy, familyId, now)
    }

    override fun findById(id: UUID): ConversationRecord? = transaction {
        ConversationsTable.select { ConversationsTable.id eq id }.singleOrNull()?.toConversation()
    }

    override fun findDm(userA: UUID, userB: UUID): ConversationRecord? = transaction {
        val mine = ConversationMembersTable
            .select { ConversationMembersTable.userId eq userA }
            .map { it[ConversationMembersTable.conversationId] }
        if (mine.isEmpty()) return@transaction null
        ConversationsTable
            .select { (ConversationsTable.id inList mine) and (ConversationsTable.type eq "dm") }
            .map { it.toConversation() }
            .firstOrNull { conv ->
                val members = ConversationMembersTable
                    .select { ConversationMembersTable.conversationId eq conv.id }
                    .map { row -> row[ConversationMembersTable.userId] }
                members.size == 2 && members.containsAll(listOf(userA, userB))
            }
    }

    override fun listForUser(userId: UUID): List<ConversationRecord> = transaction {
        val ids = ConversationMembersTable
            .select { ConversationMembersTable.userId eq userId }
            .map { it[ConversationMembersTable.conversationId] }
        if (ids.isEmpty()) return@transaction emptyList()
        ConversationsTable
            .select { ConversationsTable.id inList ids }
            .map { it.toConversation() }
            .sortedByDescending { lastMessage(it.id)?.createdAt ?: it.createdAt }
    }

    override fun memberIds(conversationId: UUID): List<UUID> = transaction {
        ConversationMembersTable
            .select { ConversationMembersTable.conversationId eq conversationId }
            .map { it[ConversationMembersTable.userId] }
    }

    override fun isMember(conversationId: UUID, userId: UUID): Boolean = transaction {
        ConversationMembersTable.select {
            (ConversationMembersTable.conversationId eq conversationId) and (ConversationMembersTable.userId eq userId)
        }.count() > 0
    }

    override fun addMember(conversationId: UUID, userId: UUID) {
        transaction {
            val exists = ConversationMembersTable.select {
                (ConversationMembersTable.conversationId eq conversationId) and (ConversationMembersTable.userId eq userId)
            }.count() > 0
            if (!exists) {
                ConversationMembersTable.insert {
                    it[ConversationMembersTable.conversationId] = conversationId
                    it[ConversationMembersTable.userId] = userId
                    it[joinedAt] = OffsetDateTime.now()
                    it[lastReadAt] = OffsetDateTime.now()
                }
            }
        }
    }

    override fun lastMessage(conversationId: UUID): MessageRecord? = transaction {
        MessagesTable
            .select { MessagesTable.conversationId eq conversationId }
            .orderBy(MessagesTable.createdAt to SortOrder.DESC)
            .limit(1)
            .firstOrNull()
            ?.toMessage()
    }

    override fun unreadCount(conversationId: UUID, userId: UUID): Int = transaction {
        val lastRead = ConversationMembersTable
            .select {
                (ConversationMembersTable.conversationId eq conversationId) and (ConversationMembersTable.userId eq userId)
            }
            .singleOrNull()
            ?.get(ConversationMembersTable.lastReadAt)
        val query = if (lastRead == null) {
            MessagesTable.select {
                (MessagesTable.conversationId eq conversationId) and (MessagesTable.authorId neq userId)
            }
        } else {
            MessagesTable.select {
                (MessagesTable.conversationId eq conversationId) and
                    (MessagesTable.authorId neq userId) and
                    (MessagesTable.createdAt greater lastRead)
            }
        }
        query.count().toInt()
    }

    override fun markRead(conversationId: UUID, userId: UUID) {
        transaction {
            ConversationMembersTable.update({
                (ConversationMembersTable.conversationId eq conversationId) and (ConversationMembersTable.userId eq userId)
            }) {
                it[lastReadAt] = OffsetDateTime.now()
            }
        }
    }

    override fun insertMessage(conversationId: UUID, authorId: UUID, body: String, imageUrl: String?): MessageRecord =
        transaction {
            val id = UUID.randomUUID()
            val now = OffsetDateTime.now()
            MessagesTable.insert {
                it[MessagesTable.id] = id
                it[MessagesTable.conversationId] = conversationId
                it[MessagesTable.authorId] = authorId
                it[MessagesTable.body] = body
                it[MessagesTable.imageUrl] = imageUrl
                it[createdAt] = now
            }
            MessageRecord(id, conversationId, authorId, body, imageUrl, now)
        }

    override fun listMessages(conversationId: UUID, before: OffsetDateTime?, limit: Int): List<MessageRecord> = transaction {
        val base = if (before != null) {
            MessagesTable.select {
                (MessagesTable.conversationId eq conversationId) and (MessagesTable.createdAt less before)
            }
        } else {
            MessagesTable.select { MessagesTable.conversationId eq conversationId }
        }
        base.orderBy(MessagesTable.createdAt to SortOrder.DESC)
            .limit(limit)
            .map { it.toMessage() }
            .reversed()
    }

    private fun ResultRow.toConversation() = ConversationRecord(
        id = this[ConversationsTable.id],
        type = this[ConversationsTable.type],
        title = this[ConversationsTable.title],
        createdBy = this[ConversationsTable.createdBy],
        familyId = this[ConversationsTable.familyId],
        createdAt = this[ConversationsTable.createdAt]
    )

    private fun ResultRow.toMessage() = MessageRecord(
        id = this[MessagesTable.id],
        conversationId = this[MessagesTable.conversationId],
        authorId = this[MessagesTable.authorId],
        body = this[MessagesTable.body],
        imageUrl = this[MessagesTable.imageUrl],
        createdAt = this[MessagesTable.createdAt]
    )
}

class PostgresFamilyRepository : FamilyRepository {
    override fun create(name: String, createdBy: UUID, inviteCode: String, conversationId: UUID?): FamilyRecord =
        transaction {
            val id = UUID.randomUUID()
            val now = OffsetDateTime.now()
            FamiliesTable.insert {
                it[FamiliesTable.id] = id
                it[FamiliesTable.name] = name
                it[FamiliesTable.inviteCode] = inviteCode
                it[FamiliesTable.createdBy] = createdBy
                it[FamiliesTable.conversationId] = conversationId
                it[createdAt] = now
            }
            FamilyMembersTable.insert {
                it[familyId] = id
                it[userId] = createdBy
                it[role] = "admin"
                it[joinedAt] = now
            }
            FamilyRecord(id, name, inviteCode, createdBy, conversationId, now)
        }

    override fun findById(id: UUID): FamilyRecord? = transaction {
        FamiliesTable.select { FamiliesTable.id eq id }.singleOrNull()?.toFamily()
    }

    override fun findByMember(userId: UUID): FamilyRecord? = transaction {
        val familyId = FamilyMembersTable
            .select { FamilyMembersTable.userId eq userId }
            .singleOrNull()
            ?.get(FamilyMembersTable.familyId)
            ?: return@transaction null
        findById(familyId)
    }

    override fun findByInviteCode(code: String): FamilyRecord? = transaction {
        FamiliesTable.select { FamiliesTable.inviteCode eq code.uppercase() }.singleOrNull()?.toFamily()
    }

    override fun addMember(familyId: UUID, userId: UUID, role: String) {
        transaction {
            val exists = FamilyMembersTable.select {
                (FamilyMembersTable.familyId eq familyId) and (FamilyMembersTable.userId eq userId)
            }.count() > 0
            if (!exists) {
                FamilyMembersTable.insert {
                    it[FamilyMembersTable.familyId] = familyId
                    it[FamilyMembersTable.userId] = userId
                    it[FamilyMembersTable.role] = role
                    it[joinedAt] = OffsetDateTime.now()
                }
            }
        }
    }

    override fun removeMember(familyId: UUID, userId: UUID) {
        transaction {
            FamilyMembersTable.deleteWhere {
                (FamilyMembersTable.familyId eq familyId) and (FamilyMembersTable.userId eq userId)
            }
        }
    }

    override fun memberIds(familyId: UUID): List<UUID> = transaction {
        FamilyMembersTable.select { FamilyMembersTable.familyId eq familyId }
            .map { it[FamilyMembersTable.userId] }
    }

    override fun memberCount(familyId: UUID): Int = transaction {
        FamilyMembersTable.select { FamilyMembersTable.familyId eq familyId }.count().toInt()
    }

    override fun setConversationId(familyId: UUID, conversationId: UUID) {
        transaction {
            FamiliesTable.update({ FamiliesTable.id eq familyId }) {
                it[FamiliesTable.conversationId] = conversationId
            }
            ConversationsTable.update({ ConversationsTable.id eq conversationId }) {
                it[ConversationsTable.familyId] = familyId
            }
        }
    }

    private fun ResultRow.toFamily() = FamilyRecord(
        id = this[FamiliesTable.id],
        name = this[FamiliesTable.name],
        inviteCode = this[FamiliesTable.inviteCode],
        createdBy = this[FamiliesTable.createdBy],
        conversationId = this[FamiliesTable.conversationId],
        createdAt = this[FamiliesTable.createdAt]
    )
}

class PostgresFcmTokenRepository : FcmTokenRepository {
    override fun save(userId: UUID, token: String) {
        transaction {
            val exists = FcmTokensTable.select {
                (FcmTokensTable.userId eq userId) and (FcmTokensTable.token eq token)
            }.count() > 0
            if (exists) {
                FcmTokensTable.update({
                    (FcmTokensTable.userId eq userId) and (FcmTokensTable.token eq token)
                }) {
                    it[updatedAt] = OffsetDateTime.now()
                }
            } else {
                FcmTokensTable.insert {
                    it[FcmTokensTable.userId] = userId
                    it[FcmTokensTable.token] = token
                    it[updatedAt] = OffsetDateTime.now()
                }
            }
        }
    }

    override fun tokensFor(userId: UUID): List<String> = transaction {
        FcmTokensTable.select { FcmTokensTable.userId eq userId }.map { it[FcmTokensTable.token] }
    }

    override fun delete(userId: UUID, token: String) {
        transaction {
            FcmTokensTable.deleteWhere { (FcmTokensTable.userId eq userId) and (FcmTokensTable.token eq token) }
        }
    }
}
