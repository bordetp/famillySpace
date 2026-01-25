package com.zam.photos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.zam.photos.app.navigation.AppNavHost
import com.zam.photos.app.ui.theme.FamilySpaceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FamilySpaceTheme {
                Surface {
                    FamilySpaceApp()
                }
            }
        }
    }
}

@Composable
fun FamilySpaceApp() {
    AppNavHost()
}
