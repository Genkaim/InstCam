package com.genkaim.picocam.dynamic

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** 拍立得相框默认颜色：不透明白。 */
const val DEFAULT_FRAME_COLOR: Int = android.graphics.Color.WHITE

/** 拍立得相框配置：自定义颜色（ARGB 整型，不透明） + 是否启用"毛玻璃"模糊背景。 */
data class FrameConfig(
    val color: Int = DEFAULT_FRAME_COLOR,
    val isFrosted: Boolean = false,
)

private val Context.frameStore: DataStore<Preferences> by preferencesDataStore(name = "frame_settings")
private val K_FRAME_COLOR = intPreferencesKey("frame_color")
private val K_FRAME_FROSTED = booleanPreferencesKey("frame_frosted")

object FramePrefs {
    private fun Preferences.toConfig() = FrameConfig(
        color = this[K_FRAME_COLOR] ?: DEFAULT_FRAME_COLOR,
        isFrosted = this[K_FRAME_FROSTED] ?: false,
    )

    val Context.frameConfig: Flow<FrameConfig>
        get() = frameStore.data.map { it.toConfig() }

    suspend fun Context.saveFrameSettings(block: FrameConfig.() -> FrameConfig) {
        val next = frameStore.data.first().toConfig().block()
        frameStore.edit { p ->
            p[K_FRAME_COLOR] = next.color
            p[K_FRAME_FROSTED] = next.isFrosted
        }
        AppPrefs.updateFrame(next)
    }
}
