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
import kotlinx.coroutines.launch

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
        val checker = AppUpdateChecker(currentActivity)
        when (val result = checker.check()) {
            is AppUpdateStatus.Available -> {
                if (result.forceUpdate || !dismissed) {
                    status = result
                }
            }
            AppUpdateStatus.UpToDate -> {
                status = AppUpdateStatus.UpToDate
                dismissed = false
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
