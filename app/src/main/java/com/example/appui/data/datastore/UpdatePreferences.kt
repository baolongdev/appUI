package com.example.appui.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.updateDataStore: DataStore<Preferences> by preferencesDataStore(name = "update_preferences")

@Singleton
class UpdatePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.updateDataStore

    companion object {
        private val KEY_SNOOZE_UNTIL = longPreferencesKey("snooze_until")
        private val KEY_DOWNLOADING_URL = stringPreferencesKey("downloading_url")
        private val KEY_IS_DOWNLOADING = booleanPreferencesKey("is_downloading")
        private val KEY_DOWNLOAD_PROGRESS = floatPreferencesKey("download_progress")
        private val KEY_UPDATE_SCREEN_VISITED = booleanPreferencesKey("update_screen_visited")
        private val KEY_SNOOZED_VERSION = stringPreferencesKey("snoozed_version") // ✅ NEW
    }

    // ✅ NEW: Snooze update (7 days)
    suspend fun snoozeUpdate(version: String, days: Int = 7) {
        val snoozeUntil = System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000L)
        dataStore.edit { prefs ->
            prefs[KEY_SNOOZE_UNTIL] = snoozeUntil
            prefs[KEY_SNOOZED_VERSION] = version
        }
    }

    suspend fun isUpdateSnoozed(version: String): Boolean {
        return dataStore.data.map { prefs ->
            val snoozeUntil = prefs[KEY_SNOOZE_UNTIL] ?: 0L
            val snoozedVersion = prefs[KEY_SNOOZED_VERSION]

            // Only snoozed if same version AND time not expired
            snoozedVersion == version && System.currentTimeMillis() < snoozeUntil
        }.first()
    }

    suspend fun clearSnooze() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_SNOOZE_UNTIL)
            prefs.remove(KEY_SNOOZED_VERSION)
        }
    }

    // Mark Update screen visited
    suspend fun markUpdateScreenVisited() {
        dataStore.edit { prefs ->
            prefs[KEY_UPDATE_SCREEN_VISITED] = true
        }
    }

    suspend fun isUpdateScreenVisited(): Boolean {
        return dataStore.data.map { prefs ->
            prefs[KEY_UPDATE_SCREEN_VISITED] ?: false
        }.first()
    }

    suspend fun clearUpdateScreenVisited() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_UPDATE_SCREEN_VISITED)
        }
    }

    // Download state
    suspend fun saveDownloadState(url: String, progress: Float) {
        dataStore.edit { prefs ->
            prefs[KEY_DOWNLOADING_URL] = url
            prefs[KEY_IS_DOWNLOADING] = true
            prefs[KEY_DOWNLOAD_PROGRESS] = progress
        }
    }

    suspend fun clearDownloadState() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_DOWNLOADING_URL)
            prefs.remove(KEY_IS_DOWNLOADING)
            prefs.remove(KEY_DOWNLOAD_PROGRESS)
        }
    }

    suspend fun getDownloadState(): DownloadState? {
        return dataStore.data.map { prefs ->
            val url = prefs[KEY_DOWNLOADING_URL]
            val isDownloading = prefs[KEY_IS_DOWNLOADING] ?: false
            val progress = prefs[KEY_DOWNLOAD_PROGRESS] ?: 0f

            if (url != null && isDownloading) {
                DownloadState(url, progress)
            } else null
        }.first()
    }
}

data class DownloadState(
    val url: String,
    val progress: Float
)
