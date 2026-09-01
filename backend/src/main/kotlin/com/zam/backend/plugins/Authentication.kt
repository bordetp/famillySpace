package com.zam.backend.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.zam.backend.AppConfig
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt

fun Application.configureAuthentication(config: AppConfig) {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = config.jwtIssuer
            verifier(
                JWT
                    .require(Algorithm.HMAC256(config.jwtSecret))
                    .withAudience(config.jwtAudience)
                    .withIssuer(config.jwtIssuer)
                    .build()
            )
            validate { credential ->
                val userId = credential.payload.getClaim("userId").asString()
                if (!userId.isNullOrBlank()) JWTPrincipal(credential.payload) else null
            }
        }
    }
}
