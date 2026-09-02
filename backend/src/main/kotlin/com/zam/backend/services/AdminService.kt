package com.zam.backend.services

import com.zam.backend.AppConfig
import com.zam.backend.repository.CommentRepository
import com.zam.backend.repository.ConversationRepository
import com.zam.backend.repository.FamilyRepository
import com.zam.backend.repository.PostRepository
import com.zam.backend.repository.UserRepository
import com.zam.backend.repository.toDto
import com.zam.shared.CommentDto
import com.zam.shared.PostDto
import com.zam.shared.UserDto
import java.util.UUID

class AdminService(
    private val config: AppConfig,
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val familyRepository: FamilyRepository,
    private val conversationRepository: ConversationRepository
) {
    fun requireAdmin(userId: UUID) {
        val user = userRepository.findById(userId)
            ?: throw ValidationException("User not found", "NOT_FOUND")
        val adminEmail = config.adminEmail.trim().lowercase()
        if (user.email.trim().lowercase() != adminEmail) {
            throw ValidationException("Admin access required", "FORBIDDEN")
        }
    }

    fun listAllPosts(adminId: UUID, limit: Int, offset: Int): Pair<List<PostDto>, Int> {
        requireAdmin(adminId)
        val safeLimit = limit.coerceIn(1, 50)
        val safeOffset = offset.coerceAtLeast(0)
        val posts = postRepository.list(adminId, safeLimit, safeOffset, authorIds = null).map { it.toDto() }
        val total = postRepository.count(authorIds = null)
        return posts to total
    }

    fun deletePost(adminId: UUID, postId: UUID) {
        requireAdmin(adminId)
        if (!postRepository.deleteById(postId)) {
            throw ValidationException("Post not found", "NOT_FOUND")
        }
    }

    fun listAllComments(adminId: UUID, limit: Int, offset: Int): Pair<List<CommentDto>, Int> {
        requireAdmin(adminId)
        val safeLimit = limit.coerceIn(1, 50)
        val safeOffset = offset.coerceAtLeast(0)
        val comments = commentRepository.listAll(safeLimit, safeOffset).map { it.toDto() }
        val total = commentRepository.countAll()
        return comments to total
    }

    fun deleteComment(adminId: UUID, commentId: UUID) {
        requireAdmin(adminId)
        if (!commentRepository.deleteById(commentId)) {
            throw ValidationException("Comment not found", "NOT_FOUND")
        }
    }

    fun listUsers(adminId: UUID, status: String?, limit: Int, offset: Int): Pair<List<UserDto>, Int> {
        requireAdmin(adminId)
        val normalized = status?.trim()?.lowercase()?.takeIf { it in setOf("pending", "approved", "rejected") }
        val safeLimit = limit.coerceIn(1, 50)
        val safeOffset = offset.coerceAtLeast(0)
        val users = userRepository.listByApprovalStatus(normalized, safeLimit, safeOffset).map { it.toDto() }
        val total = userRepository.countByApprovalStatus(normalized)
        return users to total
    }

    fun setUserApproval(adminId: UUID, targetUserId: UUID, status: String): UserDto {
        requireAdmin(adminId)
        val normalized = status.trim().lowercase()
        if (normalized !in setOf("pending", "approved", "rejected")) {
            throw ValidationException("Invalid approval status", "INVALID_STATUS")
        }
        val target = userRepository.findById(targetUserId)
            ?: throw ValidationException("User not found", "NOT_FOUND")
        if (target.email.trim().lowercase() == config.adminEmail.trim().lowercase() && normalized != "approved") {
            throw ValidationException("Cannot change admin approval status", "FORBIDDEN")
        }
        val updated = userRepository.setApprovalStatus(targetUserId, normalized)?.toDto()
            ?: throw ValidationException("User not found", "NOT_FOUND")
        if (normalized == "approved") {
            addToPrimaryFamily(targetUserId)
        }
        return updated
    }

    private fun addToPrimaryFamily(userId: UUID) {
        val family = familyRepository.findPrimary() ?: return
        if (familyRepository.findByMember(userId) != null) return
        familyRepository.addMember(family.id, userId)
        family.conversationId?.let { conversationRepository.addMember(it, userId) }
    }
}
