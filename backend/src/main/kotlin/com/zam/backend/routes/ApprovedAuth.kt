package com.zam.backend.routes

import com.zam.backend.services.AuthService
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.intercept

fun Route.authenticateApproved(authService: AuthService, build: Route.() -> Unit) {
    authenticate("auth-jwt") {
        intercept(ApplicationCallPipeline.Call) {
            val principal = call.principal<JWTPrincipal>() ?: return@intercept
            authService.requireApproved(principal.userId())
        }
        build()
    }
}
