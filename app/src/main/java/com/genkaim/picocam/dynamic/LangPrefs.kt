package com.genkaim.picocam.dynamic

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.genkaim.picocam.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** 应用语言模式。默认跟随系统。 */
enum class LangMode(
    val code: String,
    /** 设置对话框/当前状态展示用字符串资源。 */
    val labelRes: Int,
) {
    SYSTEM("system", R.string.lang_system),
    ZH_HANS("zh-Hans", R.string.lang_zh_hans),
    ZH_HANT("zh-Hant", R.string.lang_zh_hant),
    EN("en", R.string.lang_en);

    companion object {
        fun fromOrdinal(o: Int) = entries.getOrElse(o) { SYSTEM }
        fun fromCode(c: String) = entries.firstOrNull { it.code == c } ?: SYSTEM
    }
}

data class LangConfig(val mode: LangMode = LangMode.SYSTEM)

private val Context.langStore: DataStore<Preferences> by preferencesDataStore(name = "lang_settings")
private val K_LANG = intPreferencesKey("lang_mode")

/** 按 [LangMode] 把 context 包裹成对应 Locale（SYSTEM 原样返回，交由系统决定）。 */
object AppLocale {
    fun wrap(context: Context, mode: LangMode): Context {
        if (mode == LangMode.SYSTEM) return context
        val locale = when (mode) {
            LangMode.ZH_HANS -> java.util.Locale.forLanguageTag("zh-Hans")
            LangMode.ZH_HANT -> java.util.Locale.forLanguageTag("zh-Hant")
            LangMode.EN -> java.util.Locale.ENGLISH
        }
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}

object LangPrefs {
    private fun Preferences.toConfig() = LangConfig(
        mode = LangMode.fromOrdinal(this[K_LANG] ?: LangMode.SYSTEM.ordinal),
    )

    val Context.langConfig: Flow<LangConfig>
        get() = langStore.data.catch { emit(emptyPreferences()) }.map { it.toConfig() }

    suspend fun Context.saveLangSettings(block: LangConfig.() -> LangConfig) {
        val next = langStore.data.first().toConfig().block()
        langStore.edit { p -> p[K_LANG] = next.mode.ordinal }
        AppPrefs.updateLang(next)
    }
}
