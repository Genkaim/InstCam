package com.genkaim.picocam.dynamic

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** 取景框配置：垂直位置用占比（0~1，0=最上，0.5=居中，1=最下），宽/圆角用 dp。 */
data class ViewfinderConfig(
    val posY: Float = 0.5f,      // 垂直中心占比
    val widthDp: Int = 340,     // 取景框边长（方形）
    val cornerDp: Int = 48,     // 外圆角
)

private val Context.vfStore: DataStore<Preferences> by preferencesDataStore(name = "viewfinder")

object ViewfinderPrefs {
    private val K_POS_Y = floatPreferencesKey("vf_pos_y")
    private val K_WIDTH = intPreferencesKey("vf_width")
    private val K_CORNER = intPreferencesKey("vf_corner")

    private fun Preferences.toConfig() = ViewfinderConfig(
        posY = this[K_POS_Y] ?: 0.5f,
        widthDp = this[K_WIDTH] ?: 340,
        cornerDp = this[K_CORNER] ?: 48,
    )

    /** 当前配置流，UI 订阅它。 */
    val Context.viewfinderConfig: Flow<ViewfinderConfig>
        get() = vfStore.data.map { it.toConfig() }

    /** 基于当前值做一次整体更新并写盘。 */
    suspend fun Context.saveViewfinder(block: ViewfinderConfig.() -> ViewfinderConfig) {
        val next = vfStore.data.first().toConfig().block()
        vfStore.edit { p ->
            p[K_POS_Y] = next.posY
            p[K_WIDTH] = next.widthDp
            p[K_CORNER] = next.cornerDp
        }
        AppPrefs.updateViewfinder(next)
    }
}
