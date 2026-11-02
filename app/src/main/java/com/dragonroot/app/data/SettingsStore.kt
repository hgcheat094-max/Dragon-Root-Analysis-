package com.dragonroot.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "dragonroot_settings")

data class SettingsState(
    val backgroundSync: Boolean = true,
    val unmeteredOnly: Boolean = true,
    val notifications: Boolean = false,
    val lastSyncAt: Long = 0L,
    val lastSyncMessage: String = "Never synced"
)

class SettingsStore(private val context: Context) {
    private object Keys {
        val sync = booleanPreferencesKey("background_sync")
        val unmetered = booleanPreferencesKey("unmetered_only")
        val notify = booleanPreferencesKey("notifications")
        val lastSync = longPreferencesKey("last_sync_at")
        val lastMessage = stringPreferencesKey("last_sync_message")
    }

    val flow: Flow<SettingsState> = context.settingsDataStore.data.map { p ->
        SettingsState(
            backgroundSync = p[Keys.sync] ?: true,
            unmeteredOnly = p[Keys.unmetered] ?: true,
            notifications = p[Keys.notify] ?: false,
            lastSyncAt = p[Keys.lastSync] ?: 0L,
            lastSyncMessage = p[Keys.lastMessage] ?: "Never synced"
        )
    }

    suspend fun setBackgroundSync(v: Boolean) { context.settingsDataStore.edit { it[Keys.sync] = v } }
    suspend fun setUnmeteredOnly(v: Boolean) { context.settingsDataStore.edit { it[Keys.unmetered] = v } }
    suspend fun setNotifications(v: Boolean) { context.settingsDataStore.edit { it[Keys.notify] = v } }

    suspend fun recordSync(at: Long, message: String) {
        context.settingsDataStore.edit { it[Keys.lastSync] = at; it[Keys.lastMessage] = message }
    }

    private fun latestKey(model: String) = stringPreferencesKey("latest_build_" + model.lowercase().filter { it.isLetterOrDigit() })

    suspend fun lastSeenLatestBuild(model: String): String? = context.settingsDataStore.data.first()[latestKey(model)]
    suspend fun setLastSeenLatestBuild(model: String, build: String) {
        context.settingsDataStore.edit { it[latestKey(model)] = build }
    }

    suspend fun clearAll() { context.settingsDataStore.edit { it.clear() } }
}
