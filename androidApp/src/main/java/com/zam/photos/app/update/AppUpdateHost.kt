package com.zam.photos.app.update

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.zam.photos.app.auth.findActivity
import com.zam.photos.app.ui.components.RefreshOnResume
import com.zam.photos.app.ui.components.UpdateAvailableOverlay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Play's listing often knows about an update before the In-App Update API (esp. internal testing). */
private val RETRY_DELAYS_MS = listOf(15_000L, 45_000L, 120_000L)

@Composable
fun AppUpdateHost(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<AppUpdateStatus>(AppUpdateStatus.UpToDate) }
    var dismissed by remember { mutableStateOf(false) }

    RefreshOnResume {
        val currentActivity = activity ?: return@RefreshOnResume
        // Re-prompt when returning to the app (e.g. after checking Play Store).
        dismissed = false
        val checker = AppUpdateChecker(currentActivity)

        suspend fun apply(result: AppUpdateStatus) {
            when (result) {
                is AppUpdateStatus.InProgress -> {
                    checker.startInAppUpdate(result.info, forceUpdate = true)
                }
                is AppUpdateStatus.Available -> {
                    if (result.forceUpdate || !dismissed) {
                        status = result
                    }
                }
                AppUpdateStatus.UpToDate -> {
                    status = AppUpdateStatus.UpToDate
                }
            }
        }

        var result = checker.check()
        apply(result)

        // Retry while foregrounded: listing often beats In-App Update API on internal track.
        if (result is AppUpdateStatus.UpToDate) {
            for (waitMs in RETRY_DELAYS_MS) {
                delay(waitMs)
                result = checker.check()
                apply(result)
                if (result !is AppUpdateStatus.UpToDate) break
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()
        val available = status as? AppUpdateStatus.Available
        if (available != null && (available.forceUpdate || !dismissed)) {
            UpdateAvailableOverlay(
                forceUpdate = available.forceUpdate,
                onUpdateClick = {
                    val currentActivity = activity ?: return@UpdateAvailableOverlay
                    scope.launch {
                        val checker = AppUpdateChecker(currentActivity)
                        val started = if (available.allowsInAppUpdate) {
                            checker.startInAppUpdate(available.info, available.forceUpdate)
                        } else {
                            false
                        }
                        if (!started) {
                            checker.openPlayStore()
                        }
                    }
                },
                onDismiss = { dismissed = true }
            )
        }
    }
}
