package com.zam.backend.plugins

import com.zam.shared.ErrorResponse
import com.zam.backend.services.AuthException
import com.zam.backend.services.ValidationException
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.json.Json

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }
}

fun Application.configureCors() {
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
    }
}

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<AuthException> { call, cause ->
            val status = when (cause.code) {
                "NOT_FOUND" -> HttpStatusCode.NotFound
                "EMAIL_EXISTS" -> HttpStatusCode.Conflict
                "GOOGLE_NOT_CONFIGURED" -> HttpStatusCode.ServiceUnavailable
                "INVALID_GOOGLE_TOKEN", "NO_EMAIL" -> HttpStatusCode.Unauthorized
                "DEV_AUTH_DISABLED" -> HttpStatusCode.NotFound
                "DEV_AUTH_FORBIDDEN" -> HttpStatusCode.Forbidden
                else -> HttpStatusCode.Unauthorized
            }
            call.respond(status, ErrorResponse(cause.message ?: "Auth error", cause.code))
        }
        exception<ValidationException> { call, cause ->
            val status = when (cause.code) {
                "NOT_FOUND" -> HttpStatusCode.NotFound
                "FORBIDDEN" -> HttpStatusCode.Forbidden
                "ACCOUNT_PENDING", "ACCOUNT_REJECTED" -> HttpStatusCode.Forbidden
                else -> HttpStatusCode.BadRequest
            }
            call.respond(status, ErrorResponse(cause.message ?: "Validation error", cause.code))
        }
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled error", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Internal server error"))
        }
    }
}
