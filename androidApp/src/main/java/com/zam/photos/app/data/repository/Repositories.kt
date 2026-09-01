package com.zam.photos.app.data.repository

import com.zam.photos.app.data.local.TokenStore
import com.zam.photos.app.debug.AuthDebugLog
import com.zam.photos.app.data.mapper.toProfile
import com.zam.photos.app.data.mapper.toUiComment
import com.zam.photos.app.data.mapper.toUiConversation
import com.zam.photos.app.data.mapper.toUiFamily
import com.zam.photos.app.data.mapper.toUiMessage
import com.zam.photos.app.data.mapper.toUiNotification
import com.zam.photos.app.data.mapper.toUiPost
import com.zam.photos.app.data.Comment
import com.zam.photos.app.data.Post
import com.zam.photos.app.data.UserProfile
import com.zam.shared.DevAuthRequest
import com.zam.shared.GoogleAuthRequest
import com.zam.shared.AuthResponse
import com.zam.shared.CommentRequest
import com.zam.shared.CreatePostRequest
import com.zam.shared.ErrorResponse
import com.zam.shared.PostsPageResponse
import com.zam.shared.UploadResponse
import com.zam.shared.UserDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.first

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
}

class AuthRepository(
    private val client: HttpClient,
    private val tokenStore: TokenStore,
    private val googleSignInHelper: com.zam.photos.app.auth.GoogleSignInHelper
) {
    suspend fun signInWithGoogle(idToken: String): ApiResult<Unit> {
        return try {
            val response = client.post("/api/auth/google") {
                setBody(GoogleAuthRequest(idToken = idToken))
            }
            AuthDebugLog.log("API: POST /api/auth/google → HTTP ${response.status.value}")
            if (response.status.isSuccess()) {
                val auth: AuthResponse = response.body()
                tokenStore.saveSession(auth.accessToken, auth.user)
                AuthDebugLog.log("API: token JWT sauvegardé (user=${auth.user.email})")
                ApiResult.Success(Unit)
            } else {
                val body = response.bodyAsText()
                AuthDebugLog.log("API: réponse erreur — ${body.take(120)}")
                ApiResult.Error(parseError(body))
            }
        } catch (e: Exception) {
            AuthDebugLog.log("API: exception réseau — ${e.message}")
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun signInDevBypass(secret: String): ApiResult<Unit> {
        return try {
            val response = client.post("/api/auth/dev") {
                setBody(DevAuthRequest(secret = secret))
            }
            AuthDebugLog.log("API: POST /api/auth/dev → HTTP ${response.status.value}")
            if (response.status.isSuccess()) {
                val auth: AuthResponse = response.body()
                tokenStore.saveSession(auth.accessToken, auth.user)
                AuthDebugLog.log("API: connexion test OK (user=${auth.user.email})")
                ApiResult.Success(Unit)
            } else {
                val body = response.bodyAsText()
                AuthDebugLog.log("API: dev bypass erreur — ${body.take(120)}")
                ApiResult.Error(parseError(body))
            }
        } catch (e: Exception) {
            AuthDebugLog.log("API: exception réseau — ${e.message}")
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun logout(context: android.content.Context) {
        googleSignInHelper.signOut(context)
        tokenStore.clear()
    }

    suspend fun isLoggedIn(): Boolean = tokenStore.isLoggedIn()

    suspend fun cachedUser(): UserProfile? = tokenStore.getUser()?.toProfile()

    suspend fun currentUserId(): String? = tokenStore.getUser()?.id

    suspend fun currentUser(): UserProfile? {
        return try {
            val response = client.get("/api/auth/me")
            if (response.status.isSuccess()) {
                val user: UserDto = response.body()
                tokenStore.updateUser(user)
                user.toProfile()
            } else {
                cachedUser()
            }
        } catch (_: Exception) {
            cachedUser()
        }
    }
}

class PostRepository(private val client: HttpClient) {
    data class FeedPage(val posts: List<Post>, val total: Int)

    suspend fun getFeed(limit: Int = 20, offset: Int = 0): ApiResult<FeedPage> {
        return try {
            val response = client.get("/api/posts?limit=$limit&offset=$offset")
            if (response.status.isSuccess()) {
                val page: PostsPageResponse = response.body()
                ApiResult.Success(FeedPage(page.posts.map { it.toUiPost() }, page.total))
            } else {
                ApiResult.Error(parseError(response.bodyAsText()))
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun getPost(postId: String): ApiResult<Post> {
        return try {
            val response = client.get("/api/posts/$postId")
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<com.zam.shared.PostDto>().toUiPost())
            } else {
                ApiResult.Error(parseError(response.bodyAsText()))
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun createPost(content: String, imageUrl: String?): ApiResult<Post> {
        return try {
            val response = client.post("/api/posts") {
                setBody(CreatePostRequest(content = content, imageUrl = imageUrl))
            }
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<com.zam.shared.PostDto>().toUiPost())
            } else {
                ApiResult.Error(parseError(response.bodyAsText()))
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun deletePost(postId: String): ApiResult<Unit> {
        return try {
            val response = client.delete("/api/posts/$postId")
            if (response.status.isSuccess() || response.status.value == 204) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error(parseError(response.bodyAsText()))
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun toggleLike(postId: String): ApiResult<com.zam.shared.LikeResponse> {
        return try {
            val response = client.post("/api/posts/$postId/like")
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body())
            } else {
                ApiResult.Error(parseError(response.bodyAsText()))
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun getComments(postId: String): ApiResult<List<Comment>> {
        return try {
            val response = client.get("/api/posts/$postId/comments")
            if (response.status.isSuccess()) {
                val comments: List<com.zam.shared.CommentDto> = response.body()
                ApiResult.Success(comments.map { it.toUiComment() })
            } else {
                ApiResult.Error(parseError(response.bodyAsText()))
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun addComment(postId: String, content: String, parentId: String? = null): ApiResult<Comment> {
        return try {
            val response = client.post("/api/posts/$postId/comments") {
                setBody(CommentRequest(content = content, parentId = parentId))
            }
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<com.zam.shared.CommentDto>().toUiComment())
            } else {
                ApiResult.Error(parseError(response.bodyAsText()))
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun updateComment(commentId: String, content: String): ApiResult<Comment> {
        return try {
            val response = client.put("/api/comments/$commentId") {
                setBody(com.zam.shared.UpdateCommentRequest(content))
            }
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<com.zam.shared.CommentDto>().toUiComment())
            } else {
                ApiResult.Error(parseError(response.bodyAsText()))
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun deleteComment(commentId: String): ApiResult<Unit> {
        return try {
            val response = client.delete("/api/comments/$commentId")
            if (response.status.isSuccess() || response.status.value == 204) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error(parseError(response.bodyAsText()))
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun getUserPosts(): ApiResult<List<Post>> {
        return try {
            val response = client.get("/api/users/me/posts")
            if (response.status.isSuccess()) {
                val posts: List<com.zam.shared.PostDto> = response.body()
                ApiResult.Success(posts.map { it.toUiPost() })
            } else {
                ApiResult.Error(parseError(response.bodyAsText()))
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun uploadImage(bytes: ByteArray, filename: String): ApiResult<String> {
        return try {
            val mime = when {
                filename.endsWith(".png", true) -> "image/png"
                filename.endsWith(".webp", true) -> "image/webp"
                filename.endsWith(".mp4", true) -> "video/mp4"
                filename.endsWith(".mov", true) -> "video/quicktime"
                else -> "image/jpeg"
            }
            val response = client.submitFormWithBinaryData(
                url = "/api/uploads",
                formData = formData {
                    append("file", bytes, Headers.build {
                        append(HttpHeaders.ContentType, mime)
                        append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                    })
                }
            )
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<UploadResponse>().url)
            } else {
                ApiResult.Error(parseError(response.bodyAsText()))
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Upload failed")
        }
    }
}

class NotificationRepository(private val client: HttpClient) {
    suspend fun list(): ApiResult<Pair<List<com.zam.photos.app.data.AppNotification>, Int>> {
        return try {
            val response = client.get("/api/notifications")
            if (response.status.isSuccess()) {
                val page: com.zam.shared.NotificationsPageResponse = response.body()
                ApiResult.Success(page.notifications.map { it.toUiNotification() } to page.unreadCount)
            } else {
                ApiResult.Error(parseError(response.bodyAsText()))
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun markAllRead(): ApiResult<Unit> = emptyResult(client.post("/api/notifications/read"))

    suspend fun markRead(id: String): ApiResult<Unit> = emptyResult(client.post("/api/notifications/$id/read"))
}

class ChatRepository(private val client: HttpClient) {
    suspend fun inbox(): ApiResult<List<com.zam.photos.app.data.Conversation>> {
        return try {
            val response = client.get("/api/conversations")
            if (response.status.isSuccess()) {
                val items: List<com.zam.shared.ConversationDto> = response.body()
                ApiResult.Success(items.map { it.toUiConversation() })
            } else ApiResult.Error(parseError(response.bodyAsText()))
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun openDm(userId: String): ApiResult<com.zam.photos.app.data.Conversation> {
        return try {
            val response = client.post("/api/conversations/dm") {
                setBody(com.zam.shared.CreateDmRequest(userId))
            }
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<com.zam.shared.ConversationDto>().toUiConversation())
            } else ApiResult.Error(parseError(response.bodyAsText()))
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun createGroup(title: String, memberIds: List<String>): ApiResult<com.zam.photos.app.data.Conversation> {
        return try {
            val response = client.post("/api/conversations/groups") {
                setBody(com.zam.shared.CreateGroupRequest(title, memberIds))
            }
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<com.zam.shared.ConversationDto>().toUiConversation())
            } else ApiResult.Error(parseError(response.bodyAsText()))
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun messages(conversationId: String): ApiResult<List<com.zam.photos.app.data.ChatMessage>> {
        return try {
            val response = client.get("/api/conversations/$conversationId/messages?limit=80")
            if (response.status.isSuccess()) {
                val items: List<com.zam.shared.MessageDto> = response.body()
                ApiResult.Success(items.map { it.toUiMessage() })
            } else ApiResult.Error(parseError(response.bodyAsText()))
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun send(conversationId: String, body: String): ApiResult<com.zam.photos.app.data.ChatMessage> {
        return try {
            val response = client.post("/api/conversations/$conversationId/messages") {
                setBody(com.zam.shared.SendMessageRequest(body))
            }
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<com.zam.shared.MessageDto>().toUiMessage())
            } else ApiResult.Error(parseError(response.bodyAsText()))
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun markRead(conversationId: String): ApiResult<Unit> =
        emptyResult(client.post("/api/conversations/$conversationId/read"))

    suspend fun searchUsers(query: String): ApiResult<List<UserProfile>> {
        return try {
            val response = client.get("/api/users/search") {
                parameter("q", query)
            }
            if (response.status.isSuccess()) {
                val users: List<UserDto> = response.body()
                ApiResult.Success(users.map { it.toProfile() })
            } else ApiResult.Error(parseError(response.bodyAsText()))
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }
}

class FamilyRepository(private val client: HttpClient) {
    suspend fun me(): ApiResult<com.zam.photos.app.data.FamilyCircle?> {
        return try {
            val response = client.get("/api/family/me")
            if (response.status.isSuccess()) {
                val body: com.zam.shared.FamilyMeResponse = response.body()
                ApiResult.Success(body.family?.toUiFamily())
            } else ApiResult.Error(parseError(response.bodyAsText()))
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun create(name: String): ApiResult<com.zam.photos.app.data.FamilyCircle> {
        return try {
            val response = client.post("/api/family") {
                setBody(com.zam.shared.CreateFamilyRequest(name))
            }
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<com.zam.shared.FamilyDto>().toUiFamily())
            } else ApiResult.Error(parseError(response.bodyAsText()))
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun join(code: String): ApiResult<com.zam.photos.app.data.FamilyCircle> {
        return try {
            val response = client.post("/api/family/join") {
                setBody(com.zam.shared.JoinFamilyRequest(code))
            }
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<com.zam.shared.FamilyDto>().toUiFamily())
            } else ApiResult.Error(parseError(response.bodyAsText()))
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun leave(): ApiResult<Unit> = emptyResult(client.post("/api/family/leave"))
}

class DeviceRepository(private val client: HttpClient) {
    suspend fun saveFcmToken(token: String): ApiResult<Unit> {
        return try {
            val response = client.post("/api/devices/fcm") {
                setBody(com.zam.shared.FcmTokenRequest(token))
            }
            emptyResult(response)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun notificationSettings(): ApiResult<Boolean> {
        return try {
            val response = client.get("/api/settings/notifications")
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<com.zam.shared.NotificationSettingsDto>().pushEnabled)
            } else ApiResult.Error(parseError(response.bodyAsText()))
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun setPushEnabled(enabled: Boolean): ApiResult<Boolean> {
        return try {
            val response = client.put("/api/settings/notifications") {
                setBody(com.zam.shared.NotificationSettingsRequest(enabled))
            }
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<com.zam.shared.NotificationSettingsDto>().pushEnabled)
            } else ApiResult.Error(parseError(response.bodyAsText()))
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }
}

class AdminRepository(private val client: HttpClient) {
    suspend fun listPosts(limit: Int = 30, offset: Int = 0): ApiResult<PostRepository.FeedPage> {
        return try {
            val response = client.get("/api/admin/posts") {
                parameter("limit", limit)
                parameter("offset", offset)
            }
            if (response.status.isSuccess()) {
                val page = response.body<PostsPageResponse>()
                ApiResult.Success(PostRepository.FeedPage(page.posts.map { it.toUiPost() }, page.total))
            } else {
                ApiResult.Error(parseHttpError(response))
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun deletePost(postId: String): ApiResult<Unit> {
        return try {
            emptyResult(client.delete("/api/admin/posts/$postId"))
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun listComments(limit: Int = 30, offset: Int = 0): ApiResult<Pair<List<Comment>, Int>> {
        return try {
            val response = client.get("/api/admin/comments") {
                parameter("limit", limit)
                parameter("offset", offset)
            }
            if (response.status.isSuccess()) {
                val page = response.body<com.zam.shared.CommentsPageResponse>()
                ApiResult.Success(page.comments.map { it.toUiComment() } to page.total)
            } else {
                ApiResult.Error(parseHttpError(response))
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun deleteComment(commentId: String): ApiResult<Unit> {
        return try {
            emptyResult(client.delete("/api/admin/comments/$commentId"))
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun listUsers(status: String? = null, limit: Int = 50, offset: Int = 0): ApiResult<Pair<List<UserProfile>, Int>> {
        return try {
            val response = client.get("/api/admin/users") {
                if (!status.isNullOrBlank()) parameter("status", status)
                parameter("limit", limit)
                parameter("offset", offset)
            }
            if (response.status.isSuccess()) {
                val page = response.body<com.zam.shared.UsersPageResponse>()
                ApiResult.Success(page.users.map { it.toProfile() } to page.total)
            } else {
                ApiResult.Error(parseHttpError(response))
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun setUserApproval(userId: String, status: String): ApiResult<UserProfile> {
        return try {
            val response = client.put("/api/admin/users/$userId/approval") {
                setBody(com.zam.shared.UpdateApprovalRequest(status))
            }
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<com.zam.shared.UserDto>().toProfile())
            } else {
                ApiResult.Error(parseHttpError(response))
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }
}

private suspend fun emptyResult(response: io.ktor.client.statement.HttpResponse): ApiResult<Unit> {
    return if (response.status.isSuccess() || response.status.value == 204) {
        ApiResult.Success(Unit)
    } else {
        ApiResult.Error(parseHttpError(response))
    }
}

private suspend fun parseHttpError(response: io.ktor.client.statement.HttpResponse): String {
    val body = response.bodyAsText()
    if (body.isNotBlank()) {
        return try {
            kotlinx.serialization.json.Json.decodeFromString<ErrorResponse>(body).error
        } catch (_: Exception) {
            "Request failed"
        }
    }
    return when (response.status.value) {
        401 -> "Session expirée"
        403 -> "Accès non autorisé"
        404 -> "Service modération indisponible (API non déployée)"
        else -> "Erreur serveur (${response.status.value})"
    }
}

private fun parseError(body: String): String {
    if (body.isBlank()) return "Session expirée ou non autorisée"
    return try {
        kotlinx.serialization.json.Json.decodeFromString<ErrorResponse>(body).error
    } catch (_: Exception) {
        "Request failed"
    }
}
