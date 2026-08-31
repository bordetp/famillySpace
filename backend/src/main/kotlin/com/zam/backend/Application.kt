package com.zam.backend

import com.zam.backend.db.DatabaseFactory
import com.zam.backend.plugins.configureAuthentication
import com.zam.backend.plugins.configureCors
import com.zam.backend.plugins.configureSerialization
import com.zam.backend.plugins.configureStatusPages
import com.zam.backend.realtime.ChatHub
import com.zam.backend.repository.postgres.PostgresCommentRepository
import com.zam.backend.repository.postgres.PostgresConversationRepository
import com.zam.backend.repository.postgres.PostgresFamilyRepository
import com.zam.backend.repository.postgres.PostgresFcmTokenRepository
import com.zam.backend.repository.postgres.PostgresLikeRepository
import com.zam.backend.repository.postgres.PostgresNotificationRepository
import com.zam.backend.repository.postgres.PostgresPostRepository
import com.zam.backend.repository.postgres.PostgresUserRepository
import com.zam.backend.routes.adminRoutes
import com.zam.backend.routes.authRoutes
import com.zam.backend.routes.chatRoutes
import com.zam.backend.routes.chatSocket
import com.zam.backend.routes.deviceRoutes
import com.zam.backend.routes.familyRoutes
import com.zam.backend.routes.healthRoutes
import com.zam.backend.routes.notificationRoutes
import com.zam.backend.routes.postRoutes
import com.zam.backend.routes.staticUploadRoutes
import com.zam.backend.routes.uploadRoutes
import com.zam.backend.services.AdminService
import com.zam.backend.services.AuthService
import com.zam.backend.services.ChatService
import com.zam.backend.services.DeviceService
import com.zam.backend.services.FamilyService
import com.zam.backend.services.FcmSender
import com.zam.backend.services.GoogleIdTokenService
import com.zam.backend.services.ImageService
import com.zam.backend.services.NotificationService
import com.zam.backend.services.PostService
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import java.io.File
import java.time.Duration

fun main() {
    val config = AppConfig()
    embeddedServer(Netty, port = config.port, host = config.host) {
        module(config)
    }.start(wait = true)
}

fun Application.module(config: AppConfig = AppConfig()) {
    DatabaseFactory.init(config)

    File(config.uploadDir).mkdirs()

    val userRepository = PostgresUserRepository()
    val commentRepository = PostgresCommentRepository(userRepository)
    val postRepository = PostgresPostRepository(userRepository, commentRepository)
    val likeRepository = PostgresLikeRepository()
    val notificationRepository = PostgresNotificationRepository()
    val conversationRepository = PostgresConversationRepository(userRepository)
    val familyRepository = PostgresFamilyRepository()
    val fcmTokenRepository = PostgresFcmTokenRepository()
    val chatHub = ChatHub()
    val fcmSender = FcmSender(config)

    val authService = AuthService(config, userRepository, GoogleIdTokenService(config))
    val notificationService = NotificationService(notificationRepository, userRepository, fcmTokenRepository, fcmSender)
    val postService = PostService(postRepository, commentRepository, likeRepository, familyRepository, notificationService)
    val adminService = AdminService(config, userRepository, postRepository, commentRepository)
    val chatService = ChatService(conversationRepository, userRepository, notificationService, chatHub)
    val familyService = FamilyService(familyRepository, conversationRepository, userRepository, notificationService)
    val deviceService = DeviceService(userRepository, fcmTokenRepository)
    val imageService = ImageService(config)

    install(CallLogging)
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(20)
        timeout = Duration.ofSeconds(30)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    configureSerialization()
    configureCors()
    configureStatusPages()
    configureAuthentication(config)

    routing {
        healthRoutes()
        staticUploadRoutes(config.uploadDir)
        authRoutes(authService)
        postRoutes(postService, authService)
        adminRoutes(adminService)
        notificationRoutes(notificationService, authService)
        chatRoutes(chatService, authService)
        familyRoutes(familyService, authService)
        deviceRoutes(deviceService, authService)
        uploadRoutes(imageService, authService)
        chatSocket(chatHub, authService)
    }
}
