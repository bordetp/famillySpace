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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AppUpdateChecker(private val activity: Activity) {
    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)

    suspend fun check(): AppUpdateStatus {
        val info = awaitAppUpdateInfo() ?: return AppUpdateStatus.UpToDate
        val available = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
        if (!available) return AppUpdateStatus.UpToDate

        val immediate = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
        val flexible = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
        return AppUpdateStatus.Available(
            info = info,
            forceUpdate = immediate && !flexible,
            allowsInAppUpdate = immediate || flexible
        )
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
        suspendCancellableCoroutine { cont ->
            appUpdateManager.appUpdateInfo
                .addOnSuccessListener { info ->
                    if (cont.isActive) cont.resume(info)
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(null)
                }
        }
}

sealed class AppUpdateStatus {
    data object UpToDate : AppUpdateStatus()
    data class Available(
        val info: AppUpdateInfo,
        val forceUpdate: Boolean,
        val allowsInAppUpdate: Boolean
    ) : AppUpdateStatus()
}
