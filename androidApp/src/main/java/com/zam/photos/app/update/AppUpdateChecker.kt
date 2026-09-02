package com.zam.photos.app.update

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.tasks.await

class AppUpdateChecker(private val activity: Activity) {
    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)

    suspend fun check(): AppUpdateStatus {
        val info = awaitAppUpdateInfo() ?: return AppUpdateStatus.UpToDate
        return when (info.updateAvailability()) {
            UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ->
                AppUpdateStatus.InProgress(info)
            UpdateAvailability.UPDATE_AVAILABLE -> {
                val immediate = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                val flexible = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                // Play priority 4–5 = force; internal testing often allows both types.
                val forceUpdate = info.updatePriority() >= 4 || (immediate && !flexible)
                AppUpdateStatus.Available(
                    info = info,
                    forceUpdate = forceUpdate,
                    allowsInAppUpdate = immediate || flexible
                )
            }
            else -> AppUpdateStatus.UpToDate
        }
    }

    fun startInAppUpdate(info: AppUpdateInfo, forceUpdate: Boolean): Boolean {
        val type = when {
            forceUpdate && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
            info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> AppUpdateType.FLEXIBLE
            info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
            else -> return false
        }
        return try {
            appUpdateManager.startUpdateFlow(
                info,
                activity,
                AppUpdateOptions.newBuilder(type).build()
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openPlayStore() {
        val packageName = activity.packageName
        val marketIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$packageName")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            activity.startActivity(marketIntent)
        } catch (_: Exception) {
            activity.startActivity(webIntent)
        }
    }

    private suspend fun awaitAppUpdateInfo(): AppUpdateInfo? =
        try {
            appUpdateManager.appUpdateInfo.await()
        } catch (_: Exception) {
            null
        }
}

sealed class AppUpdateStatus {
    data object UpToDate : AppUpdateStatus()
    data class Available(
        val info: AppUpdateInfo,
        val forceUpdate: Boolean,
        val allowsInAppUpdate: Boolean
    ) : AppUpdateStatus()
    data class InProgress(val info: AppUpdateInfo) : AppUpdateStatus()
}
