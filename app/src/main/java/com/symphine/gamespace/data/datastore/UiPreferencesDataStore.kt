package com.symphine.gamespace.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.symphine.gamespace.core.model.GameProfileType
import com.symphine.gamespace.core.model.UiSettingsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.symphineDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "symphine_preferences"
)

class UiPreferencesDataStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private object Keys {
        val UI_SETTINGS_JSON = stringPreferencesKey("ui_settings_json")
        val ACTIVE_GLOBAL_PROFILE = stringPreferencesKey("active_global_profile")
        val USER_ADDED_GAMES = stringSetPreferencesKey("user_added_games")
        val USER_EXCLUDED_GAMES = stringSetPreferencesKey("user_excluded_games")
        val CUSTOM_GROUPS = stringSetPreferencesKey("custom_groups")
        val WHITELISTED_PACKAGES = stringSetPreferencesKey("whitelisted_packages")
        val DIAGNOSTICS_PROFILE = stringPreferencesKey("diagnostics_profile")
    }

    val uiSettingsFlow: Flow<UiSettingsState> = context.symphineDataStore.data.map { prefs ->
        prefs[Keys.UI_SETTINGS_JSON]?.let { raw ->
            runCatching { json.decodeFromString<UiSettingsState>(raw) }.getOrNull()
        } ?: UiSettingsState()
    }

    val activeGlobalProfileFlow: Flow<String> = context.symphineDataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_GLOBAL_PROFILE] ?: GameProfileType.BALANCED.id
    }

    val userAddedGamesFlow: Flow<Set<String>> = context.symphineDataStore.data.map { prefs ->
        prefs[Keys.USER_ADDED_GAMES] ?: emptySet()
    }

    val userExcludedGamesFlow: Flow<Set<String>> = context.symphineDataStore.data.map { prefs ->
        prefs[Keys.USER_EXCLUDED_GAMES] ?: emptySet()
    }

    val customGroupsFlow: Flow<Set<String>> = context.symphineDataStore.data.map { prefs ->
        prefs[Keys.CUSTOM_GROUPS] ?: setOf("Default", "Competitive", "FPS", "RPG", "Casual")
    }

    val whitelistedPackagesFlow: Flow<Set<String>> = context.symphineDataStore.data.map { prefs ->
        prefs[Keys.WHITELISTED_PACKAGES] ?: emptySet()
    }

    val diagnosticsProfileFlow: Flow<String> = context.symphineDataStore.data.map { prefs ->
        prefs[Keys.DIAGNOSTICS_PROFILE] ?: "Default"
    }

    suspend fun updateUiSettings(transform: (UiSettingsState) -> UiSettingsState) {
        context.symphineDataStore.edit { prefs ->
            val current = prefs[Keys.UI_SETTINGS_JSON]?.let {
                runCatching { json.decodeFromString<UiSettingsState>(it) }.getOrNull()
            } ?: UiSettingsState()
            val updated = transform(current)
            prefs[Keys.UI_SETTINGS_JSON] = json.encodeToString(updated)
        }
    }

    suspend fun replaceUiSettings(newSettings: UiSettingsState) {
        context.symphineDataStore.edit { prefs ->
            prefs[Keys.UI_SETTINGS_JSON] = json.encodeToString(newSettings)
        }
    }

    suspend fun resetUiSettings() {
        replaceUiSettings(UiSettingsState())
    }

    suspend fun setActiveGlobalProfile(profileId: String) {
        context.symphineDataStore.edit { prefs ->
            prefs[Keys.ACTIVE_GLOBAL_PROFILE] = profileId
        }
    }

    suspend fun addUserGame(packageName: String) {
        context.symphineDataStore.edit { prefs ->
            val added = (prefs[Keys.USER_ADDED_GAMES] ?: emptySet()) + packageName
            val excluded = (prefs[Keys.USER_EXCLUDED_GAMES] ?: emptySet()) - packageName
            prefs[Keys.USER_ADDED_GAMES] = added
            prefs[Keys.USER_EXCLUDED_GAMES] = excluded
        }
    }

    suspend fun excludeGame(packageName: String) {
        context.symphineDataStore.edit { prefs ->
            val added = (prefs[Keys.USER_ADDED_GAMES] ?: emptySet()) - packageName
            val excluded = (prefs[Keys.USER_EXCLUDED_GAMES] ?: emptySet()) + packageName
            prefs[Keys.USER_ADDED_GAMES] = added
            prefs[Keys.USER_EXCLUDED_GAMES] = excluded
        }
    }

    suspend fun addCustomGroup(groupName: String) {
        val clean = groupName.trim()
        if (clean.isEmpty()) return
        context.symphineDataStore.edit { prefs ->
            val groups = (prefs[Keys.CUSTOM_GROUPS] ?: setOf("Default", "Competitive", "FPS", "RPG", "Casual")) + clean
            prefs[Keys.CUSTOM_GROUPS] = groups
        }
    }

    suspend fun toggleWhitelist(packageName: String) {
        context.symphineDataStore.edit { prefs ->
            val current = prefs[Keys.WHITELISTED_PACKAGES] ?: emptySet()
            prefs[Keys.WHITELISTED_PACKAGES] = if (packageName in current) current - packageName else current + packageName
        }
    }

    suspend fun setDiagnosticsProfile(profile: String) {
        context.symphineDataStore.edit { prefs ->
            prefs[Keys.DIAGNOSTICS_PROFILE] = profile
        }
    }

    suspend fun clearAllPreferences() {
        context.symphineDataStore.edit { it.clear() }
    }
}
