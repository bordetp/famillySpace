package com.zam.backend.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object UsersTable : Table("users") {
    val id = uuid("id")
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255).nullable()
    val googleId = varchar("google_id", 255).nullable().uniqueIndex()
    val username = varchar("username", 64)
    val displayName = varchar("display_name", 128)
    val bio = text("bio")
    val avatarUrl = text("avatar_url").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val pushEnabled = bool("push_enabled").default(true)
    val approvalStatus = varchar("approval_status", 16).default("pending")

    override val primaryKey = PrimaryKey(id)
}

object PostsTable : Table("posts") {
    val id = uuid("id")
    val authorId = uuid("author_id").references(UsersTable.id)
    val content = text("content")
    val imageUrl = text("image_url").nullable()
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}

object CommentsTable : Table("comments") {
    val id = uuid("id")
    val postId = uuid("post_id").references(PostsTable.id)
    val authorId = uuid("author_id").references(UsersTable.id)
    val content = text("content")
    val createdAt = timestampWithTimeZone("created_at")
    val parentId = uuid("parent_id").references(id).nullable()

    override val primaryKey = PrimaryKey(id)
}

object LikesTable : Table("likes") {
    val userId = uuid("user_id").references(UsersTable.id)
    val postId = uuid("post_id").references(PostsTable.id)
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(userId, postId)
}

object NotificationsTable : Table("notifications") {
    val id = uuid("id")
    val recipientId = uuid("recipient_id").references(UsersTable.id)
    val actorId = uuid("actor_id").references(UsersTable.id).nullable()
    val type = varchar("type", 32)
    val message = text("message")
    val postId = uuid("post_id").references(PostsTable.id).nullable()
    val commentId = uuid("comment_id").references(CommentsTable.id).nullable()
    val conversationId = uuid("conversation_id").nullable()
    val readAt = timestampWithTimeZone("read_at").nullable()
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}

object ConversationsTable : Table("conversations") {
    val id = uuid("id")
    val type = varchar("type", 16)
    val title = varchar("title", 128).nullable()
    val createdBy = uuid("created_by").references(UsersTable.id).nullable()
    val familyId = uuid("family_id").nullable()
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}

object ConversationMembersTable : Table("conversation_members") {
    val conversationId = uuid("conversation_id").references(ConversationsTable.id)
    val userId = uuid("user_id").references(UsersTable.id)
    val joinedAt = timestampWithTimeZone("joined_at")
    val lastReadAt = timestampWithTimeZone("last_read_at").nullable()

    override val primaryKey = PrimaryKey(conversationId, userId)
}

object MessagesTable : Table("messages") {
    val id = uuid("id")
    val conversationId = uuid("conversation_id").references(ConversationsTable.id)
    val authorId = uuid("author_id").references(UsersTable.id)
    val body = text("body")
    val imageUrl = text("image_url").nullable()
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}

object FamiliesTable : Table("families") {
    val id = uuid("id")
    val name = varchar("name", 128)
    val inviteCode = varchar("invite_code", 16).uniqueIndex()
    val createdBy = uuid("created_by").references(UsersTable.id)
    val conversationId = uuid("conversation_id").references(ConversationsTable.id).nullable()
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}

object FamilyMembersTable : Table("family_members") {
    val familyId = uuid("family_id").references(FamiliesTable.id)
    val userId = uuid("user_id").references(UsersTable.id)
    val role = varchar("role", 16)
    val joinedAt = timestampWithTimeZone("joined_at")

    override val primaryKey = PrimaryKey(familyId, userId)
}

object FcmTokensTable : Table("fcm_tokens") {
    val userId = uuid("user_id").references(UsersTable.id)
    val token = text("token")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(userId, token)
}
