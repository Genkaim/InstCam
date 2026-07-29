package com.genkaim.picocam.dynamic

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.genkaim.picocam.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** 主题模式。 */
enum class ThemeMode(val label: String, val labelRes: Int) {
    LIGHT("浅色", R.string.theme_light),
    DARK("深色", R.string.theme_dark),
    SYSTEM("跟随系统", R.string.theme_system);

    companion object {
        fun fromOrdinal(o: Int) = entries.getOrElse(o) { SYSTEM }
    }
}

data class ThemeConfig(val mode: ThemeMode = ThemeMode.SYSTEM, val onboarded: Boolean = false)

/** 根据主题模式与系统暗色状态，判定当前是否处于深色模式。 */
fun isDarkMode(mode: ThemeMode, isSystemDark: Boolean): Boolean = when (mode) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> isSystemDark
}

private val Context.themeStore: DataStore<Preferences> by preferencesDataStore(name = "theme_settings")
private val K_THEME = intPreferencesKey("theme_mode")
private val K_ONBOARDED = booleanPreferencesKey("onboarded")

object ThemePrefs {
    private fun Preferences.toConfig() = ThemeConfig(
        mode = ThemeMode.fromOrdinal(this[K_THEME] ?: ThemeMode.SYSTEM.ordinal),
        onboarded = this[K_ONBOARDED] ?: false,
    )

    val Context.themeConfig: Flow<ThemeConfig>
        get() = themeStore.data.map { it.toConfig() }

    suspend fun Context.saveThemeSettings(block: ThemeConfig.() -> ThemeConfig) {
        val next = themeStore.data.first().toConfig().block()
        themeStore.edit { p ->
            p[K_THEME] = next.mode.ordinal
            p[K_ONBOARDED] = next.onboarded
        }
        AppPrefs.updateTheme(next)
    }
}
