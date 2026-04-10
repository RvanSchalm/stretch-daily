package com.stretchdaily.app.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.stretchdaily.app.R
import com.stretchdaily.app.core.datastore.SettingsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Thin wrapper around [SoundPool] that plays short chimes during the
 * follow-along session. The two cues — start and end — are loaded eagerly
 * on construction so playback is instant. Every public play method checks
 * [SettingsDataStore.audioCuesEnabled] before firing so the toggle in
 * Settings takes effect immediately without restarting the session.
 */
@Singleton
class SessionAudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val startId: Int = soundPool.load(context, R.raw.chime_start, 1)
    private val endId: Int = soundPool.load(context, R.raw.chime_end, 1)

    suspend fun playStart() {
        if (settingsDataStore.audioCuesEnabled.first()) {
            soundPool.play(startId, 1f, 1f, 1, 0, 1f)
        }
    }

    suspend fun playEnd() {
        if (settingsDataStore.audioCuesEnabled.first()) {
            soundPool.play(endId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool.release()
    }
}
