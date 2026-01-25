package com.zam.backend

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.util.UUID

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(CallLogging)
        install(ContentNegotiation) { json() }
        install(Authentication) {
            jwt("auth-jwt") {
                // Placeholder verifier; wire real JWT provider later
                verifier { token -> JWTPrincipal(token.payload) }
                validate { credential ->
                    if (credential.payload.getClaim("userId").asString() != null) JWTPrincipal(credential.payload) else null
                }
            }
        }

        val repository = InMemoryRepository()

        routing {
            route("/api") {
                post("/auth/register") {
                    val request = call.receive<AuthRequest>()
                    val user = repository.register(request.email, request.password)
                    call.respond(AuthResponse(userId = user.id))
                }
                post("/auth/login") {
                    val request = call.receive<AuthRequest>()
                    val user = repository.login(request.email, request.password)
                    call.respond(AuthResponse(userId = user.id))
                }

                authenticate("auth-jwt") {
                    route("/posts") {
                        get {
                            call.respond(repository.posts())
                        }
                        post {
                            val postRequest = call.receive<CreatePostRequest>()
                            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString() ?: ""
                            val post = repository.addPost(userId, postRequest.content)
                            call.respond(post)
                        }
                        get("/{id}/comments") {
                            val postId = call.parameters["id"] ?: return@get call.respondText("Missing id", status = io.ktor.http.HttpStatusCode.BadRequest)
                            call.respond(repository.comments(postId))
                        }
                        post("/{id}/comments") {
                            val postId = call.parameters["id"] ?: return@post call.respondText("Missing id", status = io.ktor.http.HttpStatusCode.BadRequest)
                            val request = call.receive<CommentRequest>()
                            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString() ?: ""
                            val comment = repository.addComment(postId, userId, request.content)
                            call.respond(comment)
                        }
                    }

                    route("/users") {
                        get("/{id}") {
                            val id = call.parameters["id"] ?: return@get call.respondText("Missing id", status = io.ktor.http.HttpStatusCode.BadRequest)
                            call.respond(repository.user(id))
                        }
                    }
                }
            }
        }
    }.start(wait = true)
}

class InMemoryRepository {
    private val users = mutableMapOf<String, User>()
    private val posts = mutableListOf<Post>()
    private val comments = mutableListOf<Comment>()

    fun register(email: String, password: String): User {
        val id = UUID.randomUUID().toString()
        val user = User(id = id, email = email, password = password)
        users[id] = user
        return user
    }

    fun login(email: String, password: String): User {
        return users.values.firstOrNull { it.email == email && it.password == password }
            ?: register(email, password)
    }

    fun addPost(userId: String, content: String): Post {
        val post = Post(id = UUID.randomUUID().toString(), authorId = userId, content = content)
        posts.add(post)
        return post
    }

    fun posts(): List<Post> = posts

    fun addComment(postId: String, userId: String, content: String): Comment {
        val comment = Comment(id = UUID.randomUUID().toString(), postId = postId, authorId = userId, content = content)
        comments.add(comment)
        return comment
    }

    fun comments(postId: String): List<Comment> = comments.filter { it.postId == postId }

    fun user(id: String): User? = users[id]
}

@Serializable
data class User(val id: String, val email: String, val password: String)

@Serializable
data class Post(val id: String, val authorId: String, val content: String)

@Serializable
data class Comment(val id: String, val postId: String, val authorId: String, val content: String)

@Serializable
data class AuthRequest(val email: String, val password: String)

@Serializable
data class AuthResponse(val userId: String)

@Serializable
data class CreatePostRequest(val content: String)

@Serializable
data class CommentRequest(val content: String)
