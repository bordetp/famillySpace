package com.zam.photos.app.data

data class UserProfile(
    val id: String,
    val name: String,
    val username: String,
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
    val author: UserProfile,
    val content: String,
    val createdAt: String,
    val likes: Int = 0,
    val isLiked: Boolean = false
)
