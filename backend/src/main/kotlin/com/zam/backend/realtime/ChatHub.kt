package com.zam.backend.realtime

import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ChatHub {
    private val sessions = ConcurrentHashMap<UUID, MutableSet<WebSocketSession>>()

    fun add(userId: UUID, session: WebSocketSession) {
        sessions.computeIfAbsent(userId) { ConcurrentHashMap.newKeySet() }.add(session)
    }

    fun remove(userId: UUID, session: WebSocketSession) {
        sessions[userId]?.remove(session)
        if (sessions[userId]?.isEmpty() == true) {
            sessions.remove(userId)
        }
    }

    suspend fun sendTo(userId: UUID, payload: String) {
        sessions[userId]?.forEach { session ->
            runCatching { session.send(Frame.Text(payload)) }
        }
    }
}
