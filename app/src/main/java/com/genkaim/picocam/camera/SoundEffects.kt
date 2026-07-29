package com.genkaim.picocam.camera

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.genkaim.picocam.R
import com.genkaim.picocam.dynamic.AppPrefs

/**
 * 音效管理：使用 SoundPool 低延迟播放快门/打印音效，受 [AppPrefs.sound] 设置控制。
 * 单例，由 Application 或 Activity 初始化。
 */
object SoundEffects {

    private var pool: SoundPool? = null
    private var shutterId: Int = 0
    private var printId: Int = 0
    private var loaded = false

    /** 加载音效资源。应在 Activity 生命周期中调用（如 onCreate）。 */
    fun init(context: Context) {
        if (loaded) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        pool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(attrs)
            .build().also { sp ->
                sp.setOnLoadCompleteListener { _, _, _ -> loaded = true }
                shutterId = sp.load(context, R.raw.shutter, 1)
                printId = sp.load(context, R.raw.print, 1)
            }
    }

    /** 释放音效资源。应在 Activity 生命周期中调用（如 onDestroy）。 */
    fun release() {
        pool?.release()
        pool = null
        loaded = false
    }

    /** 播放快门音效（受设置控制）。 */
    fun playShutter() {
        val s = AppPrefs.sound.value
        if (!s.enabled || !s.shutterSound) return
        pool?.let { if (loaded) it.play(shutterId, 0.8f, 0.8f, 1, 0, 1f) }
    }

    /** 播放打印音效（受设置控制）。 */
    fun playPrint() {
        val s = AppPrefs.sound.value
        if (!s.enabled || !s.printSound) return
        pool?.let { if (loaded) it.play(printId, 0.8f, 0.8f, 1, 0, 1f) }
    }
}
