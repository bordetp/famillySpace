package com.zam.photos.app.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.zam.photos.app.data.repository.DeviceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent

class FamilySpaceMessagingService : FirebaseMessagingService(), KoinComponent {
    private val deviceRepository: DeviceRepository by inject()
    private val pushTokenManager: PushTokenManager by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch {
            deviceRepository.saveFcmToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: getString(com.zam.photos.app.R.string.app_name)
        val body = message.notification?.body ?: return
        pushTokenManager.showNotification(title, body, message.data)
    }
}
