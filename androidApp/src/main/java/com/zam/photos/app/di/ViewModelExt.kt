package com.zam.photos.app.di

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.ParametersDefinition

/** Shared across destinations so the feed count can update while comments are open. */
@Composable
inline fun <reified T : ViewModel> activityKoinViewModel(
    noinline parameters: ParametersDefinition? = null
): T {
    val activity = LocalContext.current as ComponentActivity
    return koinViewModel(viewModelStoreOwner = activity, parameters = parameters)
}
