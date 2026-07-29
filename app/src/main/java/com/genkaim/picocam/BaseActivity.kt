package com.genkaim.picocam

import android.content.Context
import androidx.activity.ComponentActivity
import com.genkaim.picocam.dynamic.AppLocale
import com.genkaim.picocam.dynamic.AppPrefs

/** 所有 Activity 的基类：在 attachBaseContext 阶段按当前语言模式包裹 Locale，
 *  使 Compose 的 stringResource 与 Context 资源解析都使用用户所选语言（系统默认则原样）。 */
abstract class BaseActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase, AppPrefs.lang.value.mode))
    }
}
