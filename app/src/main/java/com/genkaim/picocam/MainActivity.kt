package com.genkaim.picocam

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import com.genkaim.picocam.BaseActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.genkaim.picocam.dynamic.AppPrefs
import com.genkaim.picocam.ui.CameraScreen
import com.genkaim.picocam.ui.theme.PicocamTheme

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideStatusBar()
        setContent {
            PicocamTheme {
                // 运行时切换语言后，从设置页返回本页时重新创建以套用新语言（设置页自身已 recreate）
                val langMode = AppPrefs.lang.collectAsStateWithLifecycle()
                val initialLang = remember { langMode.value.mode }
                LaunchedEffect(langMode.value.mode) {
                    if (langMode.value.mode != initialLang) recreate()
                }
                CameraScreen()
            }
        }
    }

    // 每次窗口重新获得焦点（如从下拉通知栏/后台返回）都重新隐藏，
    // 修复状态栏偶尔重现、菜单栏又冒出来的问题。
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideStatusBar()
    }

    private fun hideStatusBar() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        // 手势滑出的状态栏为"临时显示"，会自动再次隐藏
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.statusBars())
    }
}
