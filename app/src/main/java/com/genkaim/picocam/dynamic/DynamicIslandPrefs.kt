package com.genkaim.picocam.dynamic

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** 灵动岛配置：位置用屏幕占比（0~1，药丸中心位置），尺寸/圆角用 dp，保证跨分辨率/旋转都能准确定位。 */
data class DynamicIslandConfig(
    val enabled: Boolean = false,
    val posX: Float = 0.5f,      // 水平中心占比：0=最左，0.5=居中，1=最右
    val posY: Float = 0.06f,     // 垂直中心占比：0=最顶，1=最底
    val widthDp: Int = 120,
    val heightDp: Int = 36,
    val cornerDp: Int = 18,
    val hideStatusBar: Boolean = true,
)

private val Context.diStore: DataStore<Preferences> by preferencesDataStore(name = "dynamic_island")

object DynamicIslandPrefs {
    private val K_ENABLED = booleanPreferencesKey("di_enabled")
    private val K_POS_X = floatPreferencesKey("di_pos_x")
    private val K_POS_Y = floatPreferencesKey("di_pos_y")
    private val K_WIDTH = intPreferencesKey("di_width")
    private val K_HEIGHT = intPreferencesKey("di_height")
    private val K_CORNER = intPreferencesKey("di_corner")
    private val K_HIDE = booleanPreferencesKey("di_hide_status_bar")

    private fun Preferences.toConfig() = DynamicIslandConfig(
        enabled = this[K_ENABLED] ?: false,
        posX = this[K_POS_X] ?: 0.5f,
        posY = this[K_POS_Y] ?: 0.06f,
        widthDp = this[K_WIDTH] ?: 120,
        heightDp = this[K_HEIGHT] ?: 36,
        cornerDp = this[K_CORNER] ?: 18,
        hideStatusBar = this[K_HIDE] ?: true,
    )

    /** 当前配置流，UI 与服务均订阅它。 */
    val Context.dynamicIslandConfig: Flow<DynamicIslandConfig>
        get() = diStore.data.map { it.toConfig() }

    /** 基于当前值做一次整体更新并写盘。 */
    suspend fun Context.saveDynamicIsland(block: DynamicIslandConfig.() -> DynamicIslandConfig) {
        val next = diStore.data.first().toConfig().block()
        diStore.edit { p ->
            p[K_ENABLED] = next.enabled
            p[K_POS_X] = next.posX
            p[K_POS_Y] = next.posY
            p[K_WIDTH] = next.widthDp
            p[K_HEIGHT] = next.heightDp
            p[K_CORNER] = next.cornerDp
            p[K_HIDE] = next.hideStatusBar
        }
        AppPrefs.updateDynamicIsland(next)
    }
}
