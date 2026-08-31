package com.zam.photos.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zam.shared.UserDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "familyspace_auth")

class TokenStore(private val context: Context) {
    private val tokenKey = stringPreferencesKey("access_token")
    private val userKey = stringPreferencesKey("user_json")

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[tokenKey] }
    val userFlow: Flow<UserDto?> = context.dataStore.data.map { prefs ->
        prefs[userKey]?.let { Json.decodeFromString<UserDto>(it) }
    }

    suspend fun getToken(): String? = context.dataStore.data.first()[tokenKey]

    suspend fun getUser(): UserDto? = context.dataStore.data.first()[userKey]?.let {
        Json.decodeFromString<UserDto>(it)
    }

    suspend fun saveSession(token: String, user: UserDto) {
        context.dataStore.edit { prefs ->
            prefs[tokenKey] = token
            prefs[userKey] = Json.encodeToString(user)
        }
    }

    suspend fun updateUser(user: UserDto) {
        context.dataStore.edit { prefs ->
            prefs[userKey] = Json.encodeToString(user)
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun isLoggedIn(): Boolean = !getToken().isNullOrBlank()
}
