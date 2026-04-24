package com.stretchdaily.app.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.preferencesStore by preferencesDataStore(name = "stretch_daily_settings")

/**
 * Thin wrapper over the [Preferences] DataStore for app-wide settings. Each
 * setting gets a typed key + a `Flow` for observation and a `suspend fun` for
 * mutation. The file lives in `core/datastore/` so future settings can drop
 * in next to the existing ones.
 */
@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    val audioCuesEnabled: Flow<Boolean> = context.preferencesStore.data.map { prefs ->
        prefs[KEY_AUDIO_CUES] ?: DEFAULT_AUDIO_CUES
    }

    suspend fun setAudioCuesEnabled(enabled: Boolean) {
        context.preferencesStore.edit { prefs ->
            prefs[KEY_AUDIO_CUES] = enabled
        }
    }

    /**
     * Controls the "next benchmark day" banner on the Dashboard. Default is
     * `false` — the user opted out of monthly benchmark nagging during
     * brainstorming (see design spec §8.1). Surfaced in Settings (R6) as a
     * toggle so the user can opt back in.
     */
    val benchmarkBannerEnabled: Flow<Boolean> = context.preferencesStore.data.map { prefs ->
        prefs[KEY_BENCHMARK_BANNER] ?: DEFAULT_BENCHMARK_BANNER
    }

    suspend fun setBenchmarkBannerEnabled(enabled: Boolean) {
        context.preferencesStore.edit { prefs ->
            prefs[KEY_BENCHMARK_BANNER] = enabled
        }
    }

    companion object {
        private val KEY_AUDIO_CUES = booleanPreferencesKey("audio_cues_enabled")
        private const val DEFAULT_AUDIO_CUES = true

        private val KEY_BENCHMARK_BANNER = booleanPreferencesKey("benchmark_banner_enabled")
        private const val DEFAULT_BENCHMARK_BANNER = false
    }
}
