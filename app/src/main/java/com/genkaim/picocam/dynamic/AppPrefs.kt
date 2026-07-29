package com.genkaim.picocam.dynamic

import android.content.Context
import com.genkaim.picocam.dynamic.AnimConfig
import com.genkaim.picocam.dynamic.AnimPrefs.animConfig
import com.genkaim.picocam.dynamic.DynamicIslandConfig
import com.genkaim.picocam.dynamic.DynamicIslandPrefs.dynamicIslandConfig
import com.genkaim.picocam.dynamic.LangConfig
import com.genkaim.picocam.dynamic.LangPrefs.langConfig
import com.genkaim.picocam.dynamic.ThemeConfig
import com.genkaim.picocam.dynamic.ThemePrefs.themeConfig
import com.genkaim.picocam.dynamic.ViewfinderConfig
import com.genkaim.picocam.dynamic.ViewfinderPrefs.viewfinderConfig
import com.genkaim.picocam.dynamic.FramePrefs.frameConfig
import com.genkaim.picocam.dynamic.SoundPrefs.soundConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/**
 * 进程内配置缓存：在 Application 启动时把 DataStore 的值预读进内存，
 * 让首屏 UI 直接用「真实配置」渲染，避免「先用默认再跳变」的露陷（露馅）。
 * 写盘后同步刷新缓存，保证相机页与各设置页读取一致。
 */
object AppPrefs {
    private val _viewfinder = MutableStateFlow(ViewfinderConfig())
    val viewfinder = _viewfinder.asStateFlow()

    private val _dynamicIsland = MutableStateFlow(DynamicIslandConfig())
    val dynamicIsland = _dynamicIsland.asStateFlow()

    private val _animSettings = MutableStateFlow(AnimConfig())
    val animSettings = _animSettings.asStateFlow()

    private val _theme = MutableStateFlow(ThemeConfig())
    val theme = _theme.asStateFlow()

    private val _lang = MutableStateFlow(LangConfig())
    val lang = _lang.asStateFlow()

    private val _frame = MutableStateFlow(FrameConfig())
    val frame = _frame.asStateFlow()

    private val _sound = MutableStateFlow(SoundConfig())
    val sound = _sound.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded = _loaded.asStateFlow()

    /** 应用启动时调用：把磁盘上的配置读入内存缓存（应在首个界面渲染前完成）。 */
    suspend fun preload(context: Context) {
        _viewfinder.value = context.viewfinderConfig.first()
        _dynamicIsland.value = context.dynamicIslandConfig.first()
        _animSettings.value = context.animConfig.first()
        _theme.value = context.themeConfig.first()
        _lang.value = context.langConfig.first()
        _frame.value = context.frameConfig.first()
        _sound.value = context.soundConfig.first()
        _loaded.value = true
    }

    /** 写盘成功后同步内存缓存。 */
    fun updateViewfinder(cfg: ViewfinderConfig) { _viewfinder.value = cfg }
    fun updateDynamicIsland(cfg: DynamicIslandConfig) { _dynamicIsland.value = cfg }
    fun updateAnimSettings(cfg: AnimConfig) { _animSettings.value = cfg }
    fun updateTheme(cfg: ThemeConfig) { _theme.value = cfg }
    fun updateLang(cfg: LangConfig) { _lang.value = cfg }
    fun updateFrame(cfg: FrameConfig) { _frame.value = cfg }
    fun updateSound(cfg: SoundConfig) { _sound.value = cfg }
}
