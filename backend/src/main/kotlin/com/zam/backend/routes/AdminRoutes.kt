package com.zam.backend.routes

import com.zam.backend.services.AdminService
import com.zam.shared.CommentsPageResponse
import com.zam.shared.PostsPageResponse
import com.zam.shared.UpdateApprovalRequest
import com.zam.shared.UsersPageResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.adminRoutes(adminService: AdminService) {
    authenticate("auth-jwt") {
        route("/api/admin") {
            get("/posts") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 30
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val (posts, total) = adminService.listAllPosts(userId, limit, offset)
                call.respond(PostsPageResponse(posts, total))
            }
            delete("/posts/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val postId = call.parameters["id"]!!.toUuid()
                adminService.deletePost(userId, postId)
                call.respond(HttpStatusCode.NoContent)
            }
            get("/comments") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 30
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val (comments, total) = adminService.listAllComments(userId, limit, offset)
                call.respond(CommentsPageResponse(comments, total))
            }
            delete("/comments/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val commentId = call.parameters["id"]!!.toUuid()
                adminService.deleteComment(userId, commentId)
                call.respond(HttpStatusCode.NoContent)
            }
            get("/users") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val status = call.request.queryParameters["status"]
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val (users, total) = adminService.listUsers(userId, status, limit, offset)
                call.respond(UsersPageResponse(users, total))
            }
            put("/users/{id}/approval") {
                val userId = call.principal<JWTPrincipal>()!!.userId()
                val targetId = call.parameters["id"]!!.toUuid()
                val request = call.receive<UpdateApprovalRequest>()
                call.respond(adminService.setUserApproval(userId, targetId, request.status))
            }
        }
    }
}
