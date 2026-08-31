package com.zam.photos.app.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Temporary on-screen auth tracing — remove before production release. */
object AuthDebugLog {
    private const val MAX_LINES = 50
    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    fun log(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _lines.value = (_lines.value + "[$time] $message").takeLast(MAX_LINES)
    }

    fun clear() {
        _lines.value = emptyList()
    }
}
