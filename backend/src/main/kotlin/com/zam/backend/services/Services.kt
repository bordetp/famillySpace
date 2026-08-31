package com.zam.backend.services

import com.zam.backend.AppConfig
import com.zam.backend.repository.UserEntity
import com.zam.backend.repository.UserRepository
import com.zam.backend.repository.toDto
import com.zam.shared.AuthResponse
import com.zam.shared.GoogleAuthRequest
import com.zam.shared.UserDto
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date
import java.util.UUID

class AuthService(
    private val config: AppConfig,
    private val userRepository: UserRepository,
    private val googleIdTokenService: GoogleIdTokenService
) {
    fun googleSignIn(request: GoogleAuthRequest): AuthResponse {
        val profile = googleIdTokenService.verify(request.idToken)

        val existingByGoogle = userRepository.findByGoogleId(profile.googleId)
        var user = when {
            existingByGoogle != null -> userRepository.syncGoogleProfile(
                userId = existingByGoogle.id,
                displayName = profile.displayName,
                avatarUrl = profile.avatarUrl,
                email = profile.email
            )
            else -> {
                val existingByEmail = userRepository.findByEmail(profile.email)
                when {
                    existingByEmail != null -> {
                        if (existingByEmail.googleId != null && existingByEmail.googleId != profile.googleId) {
                            throw AuthException("Email already linked to another account", "EMAIL_EXISTS")
                        }
                        userRepository.linkGoogleAccount(
                            userId = existingByEmail.id,
                            googleId = profile.googleId,
                            displayName = profile.displayName,
                            avatarUrl = profile.avatarUrl
                        )
                    }
                    else -> userRepository.createFromGoogle(
                        googleId = profile.googleId,
                        email = profile.email,
                        username = uniqueUsername(profile),
                        displayName = profile.displayName,
                        avatarUrl = profile.avatarUrl,
                        approvalStatus = initialApprovalStatus(profile.email)
                    )
                }
            }
        }

        user = ensureAdminApproved(user)
        return buildAuthResponse(user)
    }

    fun devSignIn(secret: String): AuthResponse {
        if (!config.devAuthBypass) {
            throw AuthException("Not found", "DEV_AUTH_DISABLED")
        }
        if (config.devAuthSecret.isBlank() || secret != config.devAuthSecret) {
            throw AuthException("Forbidden", "DEV_AUTH_FORBIDDEN")
        }

        val email = config.devAuthEmail.trim().lowercase()
        val displayName = email.substringBefore('@')
            .replace('.', ' ')
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        var user = userRepository.findByEmail(email) ?: userRepository.createFromGoogle(
            googleId = "dev-bypass:$email",
            email = email,
            username = uniqueUsernameFromEmail(email),
            displayName = displayName,
            avatarUrl = null,
            approvalStatus = initialApprovalStatus(email)
        )
        user = ensureAdminApproved(user)
        return buildAuthResponse(user)
    }

    fun me(userId: UUID): UserDto {
        return userRepository.findById(userId)?.toDto()
            ?: throw AuthException("User not found", "NOT_FOUND")
    }

    fun requireApproved(userId: UUID) {
        val user = userRepository.findById(userId)
            ?: throw AuthException("User not found", "NOT_FOUND")
        when (user.approvalStatus.lowercase()) {
            "approved" -> Unit
            "rejected" -> throw ValidationException("Account rejected", "ACCOUNT_REJECTED")
            else -> throw ValidationException("Account pending approval", "ACCOUNT_PENDING")
        }
    }

    private fun initialApprovalStatus(email: String): String =
        if (isAdminEmail(email)) "approved" else "pending"

    private fun ensureAdminApproved(user: UserEntity): UserEntity {
        if (!isAdminEmail(user.email)) return user
        if (user.approvalStatus.equals("approved", ignoreCase = true)) return user
        return userRepository.setApprovalStatus(user.id, "approved") ?: user
    }

    private fun isAdminEmail(email: String): Boolean =
        email.trim().lowercase() == config.adminEmail.trim().lowercase()

    fun verifyUserId(token: String): UUID {
        val decoded = JWT.require(Algorithm.HMAC256(config.jwtSecret))
            .withAudience(config.jwtAudience)
            .withIssuer(config.jwtIssuer)
            .build()
            .verify(token)
        return UUID.fromString(decoded.getClaim("userId").asString())
    }

    fun createToken(userId: UUID): String {
        val expiry = Date(System.currentTimeMillis() + config.jwtExpiryHours * 3600 * 1000)
        return JWT.create()
            .withIssuer(config.jwtIssuer)
            .withAudience(config.jwtAudience)
            .withClaim("userId", userId.toString())
            .withExpiresAt(expiry)
            .sign(Algorithm.HMAC256(config.jwtSecret))
    }

    private fun buildAuthResponse(user: UserEntity): AuthResponse {
        val token = createToken(user.id)
        return AuthResponse(
            accessToken = token,
            userId = user.id.toString(),
            user = user.toDto()
        )
    }

    private fun uniqueUsernameFromEmail(email: String): String {
        val base = sanitizeUsername(email.substringBefore('@')).ifBlank { "user" }
        if (!userRepository.usernameExists(base)) {
            return base
        }
        val suffix = email.hashCode().toUInt().toString(16).take(8)
        return "${base}_$suffix".take(64)
    }

    private fun uniqueUsername(profile: GoogleProfile): String {
        val base = sanitizeUsername(profile.displayName)
            .ifBlank { sanitizeUsername(profile.email.substringBefore('@')) }
            .ifBlank { "user" }
        if (!userRepository.usernameExists(base)) {
            return base
        }
        val suffix = profile.googleId.take(8).lowercase()
        val candidate = "${base}_$suffix".take(64)
        return if (!userRepository.usernameExists(candidate)) {
            candidate
        } else {
            "${base}_${suffix.take(4)}_${System.currentTimeMillis() % 10000}".take(64)
        }
    }

    private fun sanitizeUsername(value: String): String {
        val cleaned = value.lowercase()
            .replace(Regex("[^a-z0-9_]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
        return cleaned.take(64)
    }
}

class ImageService(private val config: AppConfig) {
    private val allowedExtensions = setOf("jpg", "jpeg", "png", "webp", "mp4", "mov", "webm")

    fun saveImage(bytes: ByteArray, originalFilename: String?): String {
        if (bytes.size > config.maxUploadBytes) {
            throw ValidationException("File too large", "FILE_TOO_LARGE")
        }
        val ext = originalFilename?.substringAfterLast('.', "")?.lowercase()?.takeIf { it in allowedExtensions }
            ?: detectExtension(bytes)
            ?: throw ValidationException("Unsupported media format", "INVALID_FORMAT")

        val uploadDir = java.io.File(config.uploadDir)
        uploadDir.mkdirs()

        val filename = "${UUID.randomUUID()}.$ext"
        val file = java.io.File(uploadDir, filename)
        file.writeBytes(bytes)

        return "${config.publicBaseUrl.trimEnd('/')}/uploads/$filename"
    }

    private fun detectExtension(bytes: ByteArray): String? {
        if (bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) return "jpg"
        if (bytes.size >= 8 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte()) return "png"
        if (bytes.size >= 12 && String(bytes.sliceArray(0..3)) == "RIFF") return "webp"
        if (bytes.size >= 12 && String(bytes.sliceArray(4..7)) == "ftyp") return "mp4"
        return null
    }
}

class AuthException(message: String, val code: String) : Exception(message)
class ValidationException(message: String, val code: String) : Exception(message)
