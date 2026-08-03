package com.sportynix.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import com.sportynix.app.presentation.theme.ThemeMode

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sportynix_session")

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_LANGUAGE = stringPreferencesKey("app_language")
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { prefs -> prefs[KEY_ACCESS_TOKEN] }
    val refreshToken: Flow<String?> = context.dataStore.data.map { prefs -> prefs[KEY_REFRESH_TOKEN] }
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[KEY_IS_LOGGED_IN] ?: false }
    val userId: Flow<String?> = context.dataStore.data.map { prefs -> prefs[KEY_USER_ID] }
    val userName: Flow<String?> = context.dataStore.data.map { prefs -> prefs[KEY_USER_NAME] }
    val userEmail: Flow<String?> = context.dataStore.data.map { prefs -> prefs[KEY_USER_EMAIL] }
    // Swift defaults to dark mode on first launch; persist every explicit change.
    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[KEY_DARK_MODE] ?: true }
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE]?.let { stored ->
            ThemeMode.entries.firstOrNull { it.name == stored }
        } ?: prefs[KEY_DARK_MODE]?.let { legacyDark ->
            if (legacyDark) ThemeMode.DARK else ThemeMode.LIGHT
        } ?: ThemeMode.SYSTEM
    }

    suspend fun getAccessTokenSync(): String? {
        return context.dataStore.data.map { prefs -> prefs[KEY_ACCESS_TOKEN] }.firstOrNull()
    }

    suspend fun getRefreshTokenSync(): String? {
        return context.dataStore.data.map { prefs -> prefs[KEY_REFRESH_TOKEN] }.firstOrNull()
    }

    suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
        userId: String,
        email: String,
        name: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_REFRESH_TOKEN] = refreshToken
            prefs[KEY_USER_ID] = userId
            prefs[KEY_USER_EMAIL] = email
            prefs[KEY_USER_NAME] = name
            prefs[KEY_IS_LOGGED_IN] = true
        }
    }

    suspend fun updateTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        setThemeMode(if (enabled) ThemeMode.DARK else ThemeMode.LIGHT)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
            if (mode == ThemeMode.SYSTEM) prefs.remove(KEY_DARK_MODE)
            else prefs[KEY_DARK_MODE] = mode == ThemeMode.DARK
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_ACCESS_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_USER_EMAIL)
            prefs.remove(KEY_USER_NAME)
            prefs[KEY_IS_LOGGED_IN] = false
        }
    }
}
