package com.zam.photos.app

import android.app.Application
import com.zam.photos.app.di.appModule
import com.zam.photos.app.push.PushTokenManager
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class FamilySpaceApplication : Application() {
    private val pushTokenManager: PushTokenManager by inject()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@FamilySpaceApplication)
            modules(appModule)
        }
        pushTokenManager.ensureChannel()
    }
}
