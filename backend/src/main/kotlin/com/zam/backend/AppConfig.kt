package com.zam.backend

import java.io.File

data class AppConfig(
    val host: String = System.getenv("HOST") ?: "0.0.0.0",
    val port: Int = System.getenv("PORT")?.toIntOrNull() ?: 8080,
    val jwtSecret: String = System.getenv("JWT_SECRET") ?: "dev-secret-change-in-production-min-32-chars!!",
    val jwtIssuer: String = System.getenv("JWT_ISSUER") ?: "familyspace",
    val jwtAudience: String = System.getenv("JWT_AUDIENCE") ?: "familyspace-users",
    val jwtExpiryHours: Long = System.getenv("JWT_EXPIRY_HOURS")?.toLongOrNull() ?: 168L,
    val databaseUrl: String = System.getenv("DATABASE_URL")
        ?: "jdbc:postgresql://localhost:5432/familyspace",
    val databaseUser: String = System.getenv("DATABASE_USER") ?: "familyspace",
    val databasePassword: String = System.getenv("DATABASE_PASSWORD") ?: "familyspace",
    val uploadDir: String = System.getenv("UPLOAD_DIR") ?: "uploads",
    val publicBaseUrl: String = System.getenv("PUBLIC_BASE_URL") ?: "http://localhost:8080",
    val maxUploadBytes: Long = System.getenv("MAX_UPLOAD_BYTES")?.toLongOrNull() ?: 50L * 1024 * 1024,
    val fcmServerKey: String = System.getenv("FCM_SERVER_KEY") ?: "",
    val firebaseAdminJson: String = System.getenv("FIREBASE_ADMIN_JSON") ?: "",
    val googleClientId: String = System.getenv("GOOGLE_CLIENT_ID") ?: "",
    val adminEmail: String = System.getenv("ADMIN_EMAIL") ?: "deceirem@gmail.com",
) {
    fun resolveFirebaseAdminJson(): String? {
        val candidates = listOfNotNull(
            firebaseAdminJson.takeIf { it.isNotBlank() },
            "deploy/firebase-admin.json",
            "/app/firebase-admin.json"
        )
        return candidates.firstOrNull { File(it).isFile }
    }
}
