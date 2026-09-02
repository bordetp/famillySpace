package com.zam.photos.app.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.zam.photos.app.MainActivity
import com.zam.photos.app.R
import com.zam.photos.app.data.repository.DeviceRepository
import kotlinx.coroutines.tasks.await

class PushTokenManager(
    private val context: Context,
    private val deviceRepository: DeviceRepository
) {
    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Family Space",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
    }

    fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    suspend fun registerToken() {
        ensureChannel()
        if (!canPostNotifications()) return
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            if (token.isNotBlank()) {
                deviceRepository.saveFcmToken(token)
            }
        }
    }

    fun showNotification(title: String, body: String, data: Map<String, String>) {
        if (!canPostNotifications()) return
        ensureChannel()

        val postId = data["postId"]
        val conversationId = data["conversationId"]
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            when {
                postId != null -> setData(android.net.Uri.parse("familyspace://post/$postId"))
                conversationId != null ->
                    setData(android.net.Uri.parse("familyspace://conversation/$conversationId"))
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (postId ?: conversationId ?: title).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        const val CHANNEL_ID = "familyspace_social"
    }
}
