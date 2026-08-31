package com.zam.backend.services

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import com.zam.backend.AppConfig
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class FcmSender(private val config: AppConfig) {
    private val messaging: FirebaseMessaging? by lazy { initFirebaseMessaging() }

    fun send(
        tokens: List<String>,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ) {
        if (tokens.isEmpty()) return
        val firebase = messaging
        if (firebase != null) {
            tokens.distinct().forEach { token ->
                runCatching { sendViaAdmin(firebase, token, title, body, data) }
            }
            return
        }
        if (config.fcmServerKey.isBlank()) return
        tokens.distinct().forEach { token ->
            runCatching { sendViaLegacy(token, title, body) }
        }
    }

    private fun initFirebaseMessaging(): FirebaseMessaging? {
        val jsonPath = config.resolveFirebaseAdminJson() ?: return null
        val credentials = GoogleCredentials.fromStream(File(jsonPath).inputStream())
            .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .build()
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
        }
        return FirebaseMessaging.getInstance()
    }

    private fun sendViaAdmin(
        firebase: FirebaseMessaging,
        token: String,
        title: String,
        body: String,
        data: Map<String, String>
    ) {
        val builder = Message.builder()
            .setToken(token)
            .setNotification(
                Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build()
            )
            .putData("channel_id", "familyspace_social")
        data.forEach { (key, value) -> builder.putData(key, value) }
        firebase.send(builder.build())
    }

    private fun sendViaLegacy(token: String, title: String, body: String) {
        val connection = (URL("https://fcm.googleapis.com/fcm/send").openConnection() as HttpURLConnection)
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Authorization", "key=${config.fcmServerKey}")
        val payload =
            """{"to":"$token","notification":{"title":${jsonString(title)},"body":${jsonString(body)}},"priority":"high"}"""
        connection.outputStream.use { it.write(payload.toByteArray()) }
        connection.inputStream.use { it.readBytes() }
        connection.disconnect()
    }

    private fun jsonString(value: String): String =
        "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
}
