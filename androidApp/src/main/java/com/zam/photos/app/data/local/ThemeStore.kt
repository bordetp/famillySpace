package com.zam.photos.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zam.photos.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "familyspace_settings")

class ThemeStore(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")

    val themeModeFlow: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        ThemeMode.fromStorage(prefs[themeKey])
    }

    suspend fun getThemeMode(): ThemeMode =
        ThemeMode.fromStorage(context.themeDataStore.data.first()[themeKey])

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[themeKey] = mode.storageValue
        }
    }
}
