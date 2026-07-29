package com.genkaim.picocam.dynamic

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** 音效相关设置。 */
data class SoundConfig(
    val enabled: Boolean = false,              // 音效总开关（默认关闭）
    val shutterSound: Boolean = true,          // 快门音效
    val printSound: Boolean = true,            // 打印音效
)

private val Context.soundStore: DataStore<Preferences> by preferencesDataStore(name = "sound_settings")

object SoundPrefs {
    private val K_ENABLED = booleanPreferencesKey("enabled")
    private val K_SHUTTER = booleanPreferencesKey("shutter_sound")
    private val K_PRINT = booleanPreferencesKey("print_sound")

    private fun Preferences.toConfig() = SoundConfig(
        enabled = this[K_ENABLED] ?: false,
        shutterSound = this[K_SHUTTER] ?: true,
        printSound = this[K_PRINT] ?: true,
    )

    val Context.soundConfig: Flow<SoundConfig>
        get() = soundStore.data.map { it.toConfig() }

    suspend fun Context.saveSoundSettings(block: SoundConfig.() -> SoundConfig) {
        val next = soundStore.data.first().toConfig().block()
        soundStore.edit { p ->
            p[K_ENABLED] = next.enabled
            p[K_SHUTTER] = next.shutterSound
            p[K_PRINT] = next.printSound
        }
        AppPrefs.updateSound(next)
    }
}
