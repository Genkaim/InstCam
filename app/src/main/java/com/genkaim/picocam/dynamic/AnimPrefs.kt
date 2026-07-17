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

/** 动画相关设置。 */
data class AnimConfig(
    val animEnabled: Boolean = true,                  // 动画总开关：关闭后仅正常拍照、无拍立得动画
    val openViewfinderAfterCapture: Boolean = false,   // 拍照后默认打开取景框到正常设置大小（grow 结束后）
)

private val Context.animStore: DataStore<Preferences> by preferencesDataStore(name = "anim_settings")

object AnimPrefs {
    private val K_ANIM_ENABLED = booleanPreferencesKey("anim_enabled")
    private val K_OPEN_VF = booleanPreferencesKey("open_viewfinder_after_capture")

    private fun Preferences.toConfig() = AnimConfig(
        animEnabled = this[K_ANIM_ENABLED] ?: true,
        openViewfinderAfterCapture = this[K_OPEN_VF] ?: false,
    )

    /** 当前配置流，UI 订阅它。 */
    val Context.animConfig: Flow<AnimConfig>
        get() = animStore.data.map { it.toConfig() }

    /** 基于当前值做一次整体更新并写盘。 */
    suspend fun Context.saveAnimSettings(block: AnimConfig.() -> AnimConfig) {
        val next = animStore.data.first().toConfig().block()
        animStore.edit { p ->
            p[K_ANIM_ENABLED] = next.animEnabled
            p[K_OPEN_VF] = next.openViewfinderAfterCapture
        }
        AppPrefs.updateAnimSettings(next)
    }
}
