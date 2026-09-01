package com.zam.photos.app.data.api

import com.zam.photos.app.BuildConfig
import com.zam.photos.app.data.local.TokenStore
import com.zam.shared.ChatEventDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

fun createHttpClient(tokenStore: TokenStore): HttpClient = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json(json)
    }
    install(Logging) {
        level = LogLevel.INFO
    }
    install(WebSockets)
    install(freshBearerPlugin(tokenStore))
    defaultRequest {
        url(BuildConfig.API_BASE_URL)
        contentType(ContentType.Application.Json)
    }
}

/**
 * Always reads the current JWT from DataStore. Ktor's Auth bearer plugin caches the first
 * loadTokens() result (often null on the login screen), so the feed would stay unauthorized
 * until process restart.
 */
private fun freshBearerPlugin(tokenStore: TokenStore) = createClientPlugin("FreshBearer") {
    onRequest { request, _ ->
        val path = request.url.toString()
        if (path.contains("/api/auth/google")) return@onRequest
        val token = tokenStore.getToken()
        if (!token.isNullOrBlank()) {
            request.headers.remove(HttpHeaders.Authorization)
            request.headers.append(HttpHeaders.Authorization, "Bearer $token")
        }
    }
}

fun chatEvents(client: HttpClient, tokenStore: TokenStore): Flow<ChatEventDto> = flow {
    val token = tokenStore.getToken() ?: return@flow
    val wsUrl = BuildConfig.API_BASE_URL
        .replace("https://", "wss://")
        .replace("http://", "ws://")
        .trimEnd('/') + "/ws/chat?token=$token"
    while (true) {
        try {
            client.webSocket(urlString = wsUrl) {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        if (text == "pong") continue
                        runCatching { json.decodeFromString<ChatEventDto>(text) }
                            .getOrNull()
                            ?.let { emit(it) }
                    }
                }
            }
        } catch (_: Exception) {
            kotlinx.coroutines.delay(3_000)
        }
    }
}
