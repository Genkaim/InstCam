package com.genkaim.picocam

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.genkaim.picocam.dynamic.AppPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class PicocamApp : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // 配置 Coil ImageLoader：
        // · 内存缓存从默认 25% heap 提升到 35%——冷启动后更多缩略图可驻留内存，滚动更丝滑
        // · 磁盘缓存 256MB——减少重复下载/解码（虽然本地文件仍会读盘，但磁盘 cache 通过 lastModified 判断是否过期）
        // · bitmapPool 默认即启用，此处显式同内存缓存大小
        val imageLoader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.35)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_cache"))
                    .maxSizeBytes(256 * 1024 * 1024) // 256MB
                    .build()
            }
            .build()
        Coil.setImageLoader(imageLoader)

        // 同步预读配置进内存缓存（block 直到读完），确保首个 Activity 的 attachBaseContext 即拿到正确语言/主题，
        // 避免「先用默认再跳变」。Coil 预热的异步任务仍走 scope.launch，不阻塞启动。
        runBlocking { AppPrefs.preload(applicationContext) }
        scope.launch { /* 预留：后续可在此做耗时的资源预热 */ }
    }
}
