package com.zam.backend.routes

import com.zam.backend.realtime.ChatHub
import com.zam.backend.services.AuthService
import com.zam.backend.services.ChatService
import com.zam.backend.services.DeviceService
import com.zam.backend.services.FamilyService
import com.zam.backend.services.ImageService
import com.zam.backend.services.NotificationService
import com.zam.backend.services.PostService
import com.zam.backend.services.ValidationException
import com.zam.shared.CommentRequest
import com.zam.shared.CreateDmRequest
import com.zam.shared.CreateFamilyRequest
import com.zam.shared.CreateGroupRequest
import com.zam.shared.CreatePostRequest
import com.zam.shared.FamilyMeResponse
import com.zam.shared.DevAuthRequest
import com.zam.shared.FcmTokenRequest
import com.zam.shared.GoogleAuthRequest
import com.zam.shared.HealthResponse
import com.zam.shared.JoinFamilyRequest
import com.zam.shared.NotificationSettingsRequest
import com.zam.shared.NotificationsPageResponse
import com.zam.shared.PostsPageResponse
import com.zam.shared.SendMessageRequest
import com.zam.shared.UpdateCommentRequest
import com.zam.shared.UploadResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import java.io.File
import java.time.OffsetDateTime
import java.util.UUID

fun Route.healthRoutes() {
    get("/health") {
        call.respond(HealthResponse())
    }
}

fun Route.staticUploadRoutes(uploadDir: String) {
    get("/uploads/{filename}") {
        val filename = call.parameters["filename"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        if (filename.contains("..") || filename.contains('/')) {
            return@get call.respond(HttpStatusCode.BadRequest)
        }
        val file = File(uploadDir, filename)
        if (!file.exists()) return@get call.respond(HttpStatusCode.NotFound)
        call.respondFile(file)
    }
}

fun Route.authRoutes(authService: AuthService) {
    route("/api/auth") {
        post("/google") {
            val request = call.receive<GoogleAuthRequest>()
            call.respond(authService.googleSignIn(request))
        }
        post("/dev") {
            val request = call.receive<DevAuthRequest>()
            call.respond(authService.devSignIn(request.secret))
        }
        authenticate("auth-jwt") {
            get("/me") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                call.respond(authService.me(userId))
            }
        }
    }
}

fun Route.postRoutes(postService: PostService, authService: AuthService) {
    authenticateApproved(authService) {
        route("/api/posts") {
            get {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val (posts, total) = postService.listPosts(userId, limit, offset)
                call.respond(PostsPageResponse(posts, total))
            }
            post {
                val request = call.receive<CreatePostRequest>()
                val userId = call.principal<JWTPrincipal>()!!.userId()
                call.respond(postService.createPost(userId, request.content, request.imageUrl))
            }
            get("/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val postId = call.parameters["id"]!!.toUuid()
                call.respond(postService.getPost(postId, userId))
            }
            delete("/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val postId = call.parameters["id"]!!.toUuid()
                postService.deletePost(postId, userId)
                call.respond(HttpStatusCode.NoContent)
            }
            post("/{id}/like") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val postId = call.parameters["id"]!!.toUuid()
                call.respond(postService.toggleLike(postId, userId))
            }
            get("/{id}/comments") {
                val postId = call.parameters["id"]!!.toUuid()
                call.respond(postService.getComments(postId))
            }
            post("/{id}/comments") {
                val postId = call.parameters["id"]!!.toUuid()
                val request = call.receive<CommentRequest>()
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val parentId = request.parentId?.toUuid()
                call.respond(postService.addComment(postId, userId, request.content, parentId))
            }
        }
        route("/api/comments") {
            put("/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val commentId = call.parameters["id"]!!.toUuid()
                val request = call.receive<UpdateCommentRequest>()
                call.respond(postService.updateComment(commentId, userId, request.content))
            }
            delete("/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val commentId = call.parameters["id"]!!.toUuid()
                postService.deleteComment(commentId, userId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
        get("/api/users/me/posts") {
            val userId = call.principal<JWTPrincipal>()!!.userId()
            call.respond(postService.userPosts(userId))
        }
    }
}

fun Route.notificationRoutes(notificationService: NotificationService, authService: AuthService) {
    authenticateApproved(authService) {
        route("/api/notifications") {
            get {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val (items, unread) = notificationService.list(userId)
                call.respond(NotificationsPageResponse(items, unread))
            }
            post("/read") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                notificationService.markAllRead(userId)
                call.respond(HttpStatusCode.NoContent)
            }
            post("/{id}/read") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                notificationService.markRead(userId, call.parameters["id"]!!.toUuid())
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

fun Route.chatRoutes(chatService: ChatService, authService: AuthService) {
    authenticateApproved(authService) {
        get("/api/users/search") {
            val userId = call.principal<JWTPrincipal>()!!.userId()
            val q = call.request.queryParameters["q"].orEmpty()
            call.respond(chatService.searchUsers(userId, q))
        }
        route("/api/conversations") {
            get {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                call.respond(chatService.inbox(userId))
            }
            post("/dm") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val request = call.receive<CreateDmRequest>()
                call.respond(chatService.getOrCreateDm(userId, request.userId.toUuid()))
            }
            post("/groups") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val request = call.receive<CreateGroupRequest>()
                call.respond(chatService.createGroup(userId, request.title, request.memberIds.map { it.toUuid() }))
            }
            get("/{id}/messages") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val conversationId = call.parameters["id"]!!.toUuid()
                val before = call.request.queryParameters["before"]?.let { OffsetDateTime.parse(it) }
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                call.respond(chatService.messages(userId, conversationId, before, limit))
            }
            post("/{id}/messages") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val conversationId = call.parameters["id"]!!.toUuid()
                val request = call.receive<SendMessageRequest>()
                call.respond(chatService.sendMessage(userId, conversationId, request.body, request.imageUrl))
            }
            post("/{id}/read") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                chatService.markRead(userId, call.parameters["id"]!!.toUuid())
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

fun Route.familyRoutes(familyService: FamilyService, authService: AuthService) {
    authenticateApproved(authService) {
        route("/api/family") {
            get("/me") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                call.respond(FamilyMeResponse(familyService.me(userId)))
            }
            post {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val request = call.receive<CreateFamilyRequest>()
                call.respond(familyService.create(userId, request.name))
            }
            post("/join") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val request = call.receive<JoinFamilyRequest>()
                call.respond(familyService.join(userId, request.code))
            }
            post("/leave") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                familyService.leave(userId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

fun Route.deviceRoutes(deviceService: DeviceService, authService: AuthService) {
    authenticateApproved(authService) {
        post("/api/devices/fcm") {
            val userId = call.principal<JWTPrincipal>()!!.userId()
            val request = call.receive<FcmTokenRequest>()
            deviceService.saveToken(userId, request.token)
            call.respond(HttpStatusCode.NoContent)
        }
        get("/api/settings/notifications") {
            val userId = call.principal<JWTPrincipal>()!!.userId()
            call.respond(deviceService.settings(userId))
        }
        put("/api/settings/notifications") {
            val userId = call.principal<JWTPrincipal>()!!.userId()
            val request = call.receive<NotificationSettingsRequest>()
            call.respond(deviceService.updateSettings(userId, request.pushEnabled))
        }
    }
}

fun Route.uploadRoutes(imageService: ImageService, authService: AuthService) {
    authenticateApproved(authService) {
        post("/api/uploads") {
            val multipart = call.receiveMultipart()
            var bytes: ByteArray? = null
            var filename: String? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        filename = part.originalFileName
                        bytes = part.streamProvider().readBytes()
                    }
                    else -> Unit
                }
                part.dispose()
            }

            val data = bytes ?: return@post call.respond(HttpStatusCode.BadRequest, "No file uploaded")
            val url = imageService.saveImage(data, filename)
            call.respond(UploadResponse(url))
        }
    }
}

fun Route.chatSocket(chatHub: ChatHub, authService: AuthService) {
    webSocket("/ws/chat") {
        val token = call.request.queryParameters["token"]
            ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing token"))
        val userId = try {
            authService.verifyUserId(token)
        } catch (_: Exception) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
            return@webSocket
        }
        chatHub.add(userId, this)
        try {
            for (frame in incoming) {
                if (frame is Frame.Text && frame.readText() == "ping") {
                    send(Frame.Text("pong"))
                }
            }
        } finally {
            chatHub.remove(userId, this)
        }
    }
}

fun JWTPrincipal.userId(): UUID =
    UUID.fromString(payload.getClaim("userId").asString())

fun String.toUuid(): UUID = try {
    UUID.fromString(this)
} catch (_: Exception) {
    throw ValidationException("Invalid id", "INVALID_ID")
}
