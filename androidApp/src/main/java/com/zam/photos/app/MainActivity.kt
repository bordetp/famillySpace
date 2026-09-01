package com.zam.photos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.zam.photos.app.data.local.ThemeStore
import com.zam.photos.app.navigation.AppNavHost
import com.zam.photos.app.ui.theme.FamilySpaceTheme
import com.zam.photos.app.ui.theme.ThemeMode
import com.zam.photos.app.update.AppUpdateHost
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val themeStore: ThemeStore by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by themeStore.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
            FamilySpaceTheme(themeMode = themeMode) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    FamilySpaceApp()
                }
            }
        }
    }
}

@Composable
fun FamilySpaceApp() {
    AppUpdateHost {
        AppNavHost()
    }
}
