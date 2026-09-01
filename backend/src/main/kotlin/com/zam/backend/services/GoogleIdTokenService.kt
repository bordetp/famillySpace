package com.zam.backend.services

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.zam.backend.AppConfig

data class GoogleProfile(
    val googleId: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String?
)

class GoogleIdTokenService(private val config: AppConfig) {
    private val verifier: GoogleIdTokenVerifier? by lazy {
        val clientId = config.googleClientId.trim()
        if (clientId.isBlank()) return@lazy null
        GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(listOf(clientId))
            .build()
    }

    fun verify(idToken: String): GoogleProfile {
        val googleVerifier = verifier
            ?: throw AuthException("Google sign-in not configured", "GOOGLE_NOT_CONFIGURED")
        val token = googleVerifier.verify(idToken)
            ?: throw AuthException("Invalid Google token", "INVALID_GOOGLE_TOKEN")
        val payload = token.payload
        val email = payload.email?.trim()?.lowercase()
            ?: throw AuthException("Google account has no email", "NO_EMAIL")
        return GoogleProfile(
            googleId = payload.subject,
            email = email,
            displayName = payload["name"]?.toString()?.trim().orEmpty().ifBlank {
                email.substringBefore('@')
            },
            avatarUrl = payload["picture"]?.toString()
        )
    }
}
