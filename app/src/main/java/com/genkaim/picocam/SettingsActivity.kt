package com.genkaim.picocam

import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import com.genkaim.picocam.BaseActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.genkaim.picocam.R
import com.genkaim.picocam.dynamic.AnimConfig
import com.genkaim.picocam.dynamic.AnimPrefs.saveAnimSettings
import com.genkaim.picocam.dynamic.AppPrefs
import com.genkaim.picocam.dynamic.DynamicIslandSettingsActivity
import com.genkaim.picocam.dynamic.ThemeConfig
import com.genkaim.picocam.dynamic.ThemeMode
import com.genkaim.picocam.dynamic.ThemePrefs.saveThemeSettings
import com.genkaim.picocam.dynamic.LangMode
import com.genkaim.picocam.dynamic.AppLocale
import com.genkaim.picocam.dynamic.LangPrefs.saveLangSettings
import com.genkaim.picocam.dynamic.ViewfinderSettingsActivity
import com.genkaim.picocam.dynamic.isDarkMode
import com.genkaim.picocam.ui.theme.RetroBrown
import com.genkaim.picocam.ui.theme.RetroBrownLight
import com.genkaim.picocam.ui.theme.RetroCream
import com.genkaim.picocam.ui.theme.RetroDarkBg
import com.genkaim.picocam.ui.theme.RetroPaper
import com.genkaim.picocam.ui.theme.RetroRust
import com.genkaim.picocam.ui.theme.onSurface
import com.genkaim.picocam.ui.theme.onSurfaceSoft
import com.genkaim.picocam.ui.theme.surfaceCard
import com.genkaim.picocam.camera.PhotoStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import com.genkaim.picocam.dynamic.FrameConfig
import com.genkaim.picocam.dynamic.SoundConfig
import com.genkaim.picocam.dynamic.DEFAULT_FRAME_COLOR
import com.genkaim.picocam.dynamic.FramePrefs.saveFrameSettings
import com.genkaim.picocam.dynamic.SoundPrefs.saveSoundSettings
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults

class SettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SettingsContent(onBack = { finish() })
        }
    }
}

@Composable
private fun SettingsContent(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val anim by AppPrefs.animSettings.collectAsStateWithLifecycle()
    val theme by AppPrefs.theme.collectAsStateWithLifecycle()
    val lang by AppPrefs.lang.collectAsStateWithLifecycle()
    val frame by AppPrefs.frame.collectAsStateWithLifecycle()
    val sound by AppPrefs.sound.collectAsStateWithLifecycle()

    // 导入照片：选择后直接导入并添加拍立得白框
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val result = PhotoStorage.importPhoto(context, uri, addFrame = true, frame.color, frame.isFrosted)
                val msg = if (result != null) context.getString(R.string.import_success) else context.getString(R.string.import_failed)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 应用版本号（用于「查看最新版本」）
    val versionName = remember {
        try {
            val pi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pi.versionName ?: "1.0"
        } catch (_: Exception) { "1.0" }
    }

    // 深色模式：背景改为深灰
    val configuration = LocalConfiguration.current
    val isSystemDark = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val isDark = isDarkMode(theme.mode, isSystemDark)
    val pageBg = if (isDark) RetroDarkBg else RetroPaper

    fun saveAnim(block: AnimConfig.() -> AnimConfig) {
        scope.launch { context.saveAnimSettings(block) }
    }
    fun saveTheme(block: ThemeConfig.() -> ThemeConfig) {
        scope.launch { context.saveThemeSettings(block) }
    }
    fun saveLang(block: com.genkaim.picocam.dynamic.LangConfig.() -> com.genkaim.picocam.dynamic.LangConfig) {
        // 先同步更新内存缓存，确保紧接着的 recreate() 在 attachBaseContext 阶段即拿到新语言；
        val next = AppPrefs.lang.value.block()
        com.genkaim.picocam.dynamic.AppPrefs.updateLang(next)
        // 持久化必须【同步】完成：onApplied 会调用 recreate()，而本组合作用域的 rememberCoroutineScope
        // 协程会被 recreate 销毁而取消，若用 scope.launch 写 DataStore 会被中断 → 重启后语言丢回系统默认。
        // 故用 runBlocking(Dispatchers.IO) 确保落盘后再重建界面。
        runBlocking(Dispatchers.IO) { context.saveLangSettings { next } }
    }
    fun saveFrame(block: FrameConfig.() -> FrameConfig) {
        scope.launch { context.saveFrameSettings(block) }
    }
    fun saveSound(block: SoundConfig.() -> SoundConfig) {
        scope.launch { context.saveSoundSettings(block) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
            .statusBarsPadding(),
    ) {
        val scrollState = rememberScrollState()
        // 滚动上移时标题收缩到返回按钮右侧的进度（0=原始大标题，1=收缩到顶栏）
        val titleCollapse by remember { derivedStateOf { (scrollState.value / 120f).coerceIn(0f, 1f) } }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 72.dp),
        ) {
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium, color = onSurface(isDark), modifier = Modifier.alpha(1f - titleCollapse).padding(start = 16.dp))
            Spacer(Modifier.height(16.dp))

            // —— 第一组：灵动岛与取景框设置 ——
            GroupTitle(stringResource(R.string.settings_group_island), isDark)
            Spacer(Modifier.height(10.dp))
            SettingsGroupColumn {
                // 灵动岛入口（上大圆角、下小圆角）
                SettingRow(stringResource(R.string.settings_island), stringResource(R.string.settings_island_desc), topCorners = true, isDark = isDark) {
                    context.startActivity(Intent(context, DynamicIslandSettingsActivity::class.java))
                }
                // 取景框入口（在灵动岛下侧，上小圆角、下大圆角）
                SettingRow(stringResource(R.string.settings_viewfinder), stringResource(R.string.settings_viewfinder_desc), bottomCorners = true, isDark = isDark) {
                    context.startActivity(Intent(context, ViewfinderSettingsActivity::class.java))
                }
            }

            // 组间间距
            Spacer(Modifier.height(16.dp))

            // —— 第二组：动画设置 ——
            GroupTitle(stringResource(R.string.settings_group_anim), isDark)
            Spacer(Modifier.height(10.dp))
            SettingsGroupColumn {
                // 动画总开关（上大圆角）；关闭后本组其余元素变灰且不可操作
                SettingToggleRow(
                    title = stringResource(R.string.settings_anim_toggle),
                    desc = stringResource(R.string.settings_anim_toggle_desc),
                    checked = anim.animEnabled,
                    onCheckedChange = { saveAnim { copy(animEnabled = it) } },
                    topCorners = true,
                    isDark = isDark,
                )
                // 拍照后默认打开取景框（下大圆角）；动画关闭时变灰不可操作
                SettingToggleRow(
                    title = stringResource(R.string.settings_open_vf),
                    desc = stringResource(R.string.settings_open_vf_desc),
                    checked = anim.openViewfinderAfterCapture,
                    enabled = anim.animEnabled,
                    onCheckedChange = { saveAnim { copy(openViewfinderAfterCapture = it) } },
                    bottomCorners = true,
                    isDark = isDark,
                )
            }

            Spacer(Modifier.height(16.dp))

            // —— 第三组：音效 ——
            GroupTitle(stringResource(R.string.settings_group_sound), isDark)
            Spacer(Modifier.height(10.dp))
            SettingsGroupColumn {
                val soundEnabled = sound.enabled
                SettingToggleRow(
                    stringResource(R.string.settings_sound_toggle),
                    stringResource(R.string.settings_sound_toggle_desc),
                    checked = soundEnabled,
                    onCheckedChange = { saveSound { copy(enabled = it) } },
                    topCorners = true,
                    isDark = isDark,
                )
                SettingToggleRow(
                    stringResource(R.string.settings_shutter_sound),
                    stringResource(R.string.settings_shutter_sound_desc),
                    checked = sound.shutterSound,
                    onCheckedChange = { saveSound { copy(shutterSound = it) } },
                    middleCorners = true,
                    isDark = isDark,
                    enabled = soundEnabled,
                )
                SettingToggleRow(
                    stringResource(R.string.settings_print_sound),
                    stringResource(R.string.settings_print_sound_desc),
                    checked = sound.printSound,
                    onCheckedChange = { saveSound { copy(printSound = it) } },
                    bottomCorners = true,
                    isDark = isDark,
                    enabled = soundEnabled,
                )
            }

            // 组间间距
            Spacer(Modifier.height(16.dp))

            // —— 第四组：个性化 ——
            GroupTitle(stringResource(R.string.settings_group_personal), isDark)
            Spacer(Modifier.height(10.dp))
            SettingsGroupColumn {
                ThemeColorRow(
                    current = theme.mode,
                    isDark = isDark,
                    onSelect = { saveTheme { copy(mode = it) } },
                    topCorners = true,
                )
                FrameColorRow(
                    current = frame,
                    isDark = isDark,
                    onSelect = { saveFrame { copy(color = it.color, isFrosted = it.isFrosted) } },
                    middleCorners = true,
                )
                LanguageRow(
                    current = lang.mode,
                    isDark = isDark,
                    onSelect = { saveLang { copy(mode = it) } },
                    onApplied = { (context as? androidx.activity.ComponentActivity)?.recreate() },
                    bottomCorners = true,
                )
            }

            // 组间间距
            Spacer(Modifier.height(16.dp))

            // —— 第五组：杂项 ——
            GroupTitle(stringResource(R.string.settings_group_misc), isDark)
            Spacer(Modifier.height(10.dp))
            SettingsGroupColumn {
                // 导入照片（第一项，顶部大圆角）
                SettingRow(stringResource(R.string.settings_import), stringResource(R.string.settings_import_desc), topCorners = true, isDark = isDark) {
                    importLauncher.launch("image/*")
                }
                // 反馈与Bug提交
                SettingRow(stringResource(R.string.settings_feedback), stringResource(R.string.settings_feedback_desc), middleCorners = true, isDark = isDark) {
                    openLink(context, FEEDBACK_URL)
                }
                // 查看最新版本
                SettingRow(stringResource(R.string.settings_version), stringResource(R.string.settings_version_desc, versionName), middleCorners = true, isDark = isDark) {
                    openLink(context, RELEASES_URL)
                }
                // 打赏作者：底部大圆角
                SettingRow(stringResource(R.string.settings_reward), stringResource(R.string.settings_reward_desc), bottomCorners = true, isDark = isDark) {
                    openLink(context, REWARD_URL)
                }
            }

            // 组间间距
            Spacer(Modifier.height(16.dp))

            // —— 第六组：Developer ——
            GroupTitle(stringResource(R.string.settings_group_developer), isDark)
            Spacer(Modifier.height(10.dp))
            // 每个元素去掉背景颜色；头像使用真实图片
            // Genkaim + GitHub 单独一行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.avatar_genkaim),
                    contentDescription = "Genkaim",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
                Spacer(Modifier.width(16.dp))
                Text("Genkaim", style = MaterialTheme.typography.bodyLarge, color = onSurface(isDark), modifier = Modifier.align(Alignment.CenterVertically).weight(1f))
                    // GitHub logo（右侧），点击预留跳转；容器高与头像一致(56dp)确保垂直居中统一
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .padding(end = 12.dp)
                            .align(Alignment.CenterVertically)
                            .clickable { openLink(context, GITHUB_URL) },
                        contentAlignment = Alignment.Center,
                ) {
                    GithubLogo(tint = onSurface(isDark))
                }
            }
            // 特别鸣谢：与 Developer 标题→内容的距离保持一致（10dp）
            Spacer(Modifier.height(16.dp))
            GroupTitle(stringResource(R.string.settings_group_thanks), isDark)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.avatar_zczzzzz),
                    contentDescription = "zczzzzz",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
                Spacer(Modifier.width(16.dp))
                Text("zczzzzz", style = MaterialTheme.typography.bodyLarge, color = onSurface(isDark), modifier = Modifier.align(Alignment.CenterVertically).weight(1f))
            }
        }

        // 顶部渐变遮罩：背景色→透明，滚动内容在顶部淡出
        Box(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().height(72.dp)
                .background(Brush.verticalGradient(listOf(pageBg, pageBg.copy(alpha = 0f)))),
        )

        // 返回按钮放在 Column 之后（上层），确保不被滚动内容遮盖
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 8.dp)
                .size(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(surfaceCard(isDark))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = onSurface(isDark))
        }

        // 滚动上移后收缩到返回按钮右侧的小标题
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleLarge,
            color = onSurface(isDark),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 64.dp, top = 14.dp)
                .alpha(titleCollapse),
        )
    }

    // 主题选择改为 Material 风格单选对话框（ThemeColorRow 内弹出 AlertDialog），适配深色模式
}

/** 分组标题：靠左，小字、细字重、淡色。 */
@Composable
private fun GroupTitle(text: String, isDark: Boolean) {
    Text(text, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Light), color = onSurfaceSoft(isDark), modifier = Modifier.padding(start = 16.dp))
}

/** 一组相连卡片：组内元素上下间距较小。 */
@Composable
private fun SettingsGroupColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

/** 卡片行的圆角：与 SettingsGroupColumn 的 4.dp 间距一致——
 *  组内首尾为大圆角(28dp)、中间为小圆角(4dp)。 */
private fun rowShape(
    topCorners: Boolean = false,
    bottomCorners: Boolean = false,
    middleCorners: Boolean = false,
): RoundedCornerShape = when {
    middleCorners -> RoundedCornerShape(4.dp)
    topCorners -> RoundedCornerShape(
        topStart = 28.dp, topEnd = 28.dp, bottomStart = 4.dp, bottomEnd = 4.dp,
    )
    bottomCorners -> RoundedCornerShape(
        topStart = 4.dp, topEnd = 4.dp, bottomStart = 28.dp, bottomEnd = 28.dp,
    )
    else -> RoundedCornerShape(28.dp)
}

@Composable
private fun SettingRow(
    title: String,
    desc: String,
    modifier: Modifier = Modifier,
    topCorners: Boolean = false,
    bottomCorners: Boolean = false,
    allCorners: Boolean = false,
    middleCorners: Boolean = false,
    isDark: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = when {
        allCorners -> RoundedCornerShape(28.dp)
        middleCorners -> RoundedCornerShape(4.dp)
        topCorners -> RoundedCornerShape(
            topStart = 28.dp, topEnd = 28.dp, bottomStart = 4.dp, bottomEnd = 4.dp,
        )
        bottomCorners -> RoundedCornerShape(
            topStart = 4.dp, topEnd = 4.dp, bottomStart = 28.dp, bottomEnd = 28.dp,
        )
        else -> RoundedCornerShape(12.dp)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(surfaceCard(isDark))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = onSurface(isDark))
                Text(desc, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal), color = onSurfaceSoft(isDark))
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = onSurface(isDark))
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    topCorners: Boolean = false,
    bottomCorners: Boolean = false,
    middleCorners: Boolean = false,
    isDark: Boolean = false,
    enabled: Boolean = true,
) {
    val shape = when {
        topCorners -> RoundedCornerShape(
            topStart = 28.dp, topEnd = 28.dp, bottomStart = 4.dp, bottomEnd = 4.dp,
        )
        bottomCorners -> RoundedCornerShape(
            topStart = 4.dp, topEnd = 4.dp, bottomStart = 28.dp, bottomEnd = 28.dp,
        )
        middleCorners -> RoundedCornerShape(4.dp)
        else -> RoundedCornerShape(12.dp)
    }
    val titleColor = if (enabled) onSurface(isDark) else onSurface(isDark).copy(alpha = 0.35f)
    val descColor = if (enabled) onSurfaceSoft(isDark) else onSurfaceSoft(isDark).copy(alpha = 0.35f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(surfaceCard(isDark))
            .then(if (enabled) Modifier.clickable { onCheckedChange(!checked) } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
                Text(desc, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal), color = descColor)
            }
            Switch(
                checked = checked,
                onCheckedChange = { if (enabled) onCheckedChange(it) },
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = RetroCream,
                    checkedTrackColor = RetroBrown,
                    uncheckedThumbColor = RetroCream,
                    uncheckedTrackColor = RetroBrownLight,
                ),
            )
        }
    }
}

/**
 * 个性化 - 主题颜色：行内展示当前模式，点按整行弹出 Material 风格单选对话框
 * （浅色 / 深色 / 跟随系统），选中即生效。对话框容器与文字、单选圆点均适配深色模式。
 */
@Composable
private fun ThemeColorRow(
    current: ThemeMode,
    isDark: Boolean,
    onSelect: (ThemeMode) -> Unit,
    topCorners: Boolean = false,
    bottomCorners: Boolean = false,
    middleCorners: Boolean = false,
) {
    var showDialog by remember { mutableStateOf(false) }
    val shape = rowShape(topCorners, bottomCorners, middleCorners)
    // 整行可点按，点击弹出系统风格单选对话框
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(surfaceCard(isDark))
            .clickable(onClick = { showDialog = true })
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.bodyLarge, color = onSurface(isDark))
                Text(stringResource(R.string.settings_theme_current, stringResource(current.labelRes)), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal), color = onSurfaceSoft(isDark))
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = onSurface(isDark))
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            // 容器色随深色模式切换：深色=深灰卡片，浅色=奶油白，保证和系统配色一致
            containerColor = surfaceCard(isDark),
            title = { Text(stringResource(R.string.dialog_theme_title), color = onSurface(isDark)) },
            text = {
                Column {
                    // 顺序：系统默认 → 浅色 → 深色（系统默认置首）
                    listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK).forEach { mode ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(mode); showDialog = false },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = current == mode,
                                onClick = { onSelect(mode); showDialog = false },
                                colors = RadioButtonDefaults.colors(
                                    // 深色模式选中态改用奶油白，避免在深灰卡片上对比不足
                                    selectedColor = if (isDark) RetroCream else RetroBrown,
                                    unselectedColor = onSurfaceSoft(isDark),
                                ),
                            )
                            Text(stringResource(mode.labelRes), color = onSurface(isDark), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.dialog_cancel), color = onSurfaceSoft(isDark)) }
            },
        )
    }
}

/** 拍立得相框纯色预设（复古胶片风，不透明）。默认两行共 10 色，最后两色（墨绿/酒红）合并为下方"毛玻璃"宽色块，故此处保留 8 个。 */
private val FRAME_SOLID_COLORS = listOf(
    0xFFFFFFFF.toInt(), // 白
    0xFFEDE0C8.toInt(), // 奶油
    0xFFF2E2B8.toInt(), // 米黄
    0xFFD8C3A5.toInt(), // 沙色
    0xFF8A5A3B.toInt(), // 复古棕
    0xFF4B3621.toInt(), // 莫卡
    0xFF1A1A1A.toInt(), // 墨黑
    0xFF2C3E50.toInt(), // 深蓝
)

/** 相框选择：纯色 或 毛玻璃（照片模糊压暗当背景）。 */
private sealed class FrameChoice {
    data class Solid(val color: Int) : FrameChoice()
    object Frosted : FrameChoice()
}

/** 毛玻璃选择指示的圆点笔刷：中心白色光斑径向渐变至透明，配合 blur 模拟毛玻璃质感（与编辑器一致）。 */
private val frostedDotBrush = Brush.radialGradient(
    listOf(Color(0xE6FFFFFF), Color(0x00FFFFFF)),
)

@Composable
private fun FrameColorRow(
    current: FrameConfig,
    isDark: Boolean,
    onSelect: (FrameConfig) -> Unit,
    topCorners: Boolean = false,
    bottomCorners: Boolean = false,
    middleCorners: Boolean = false,
) {
    var showDialog by remember { mutableStateOf(false) }
    val shape = rowShape(topCorners, bottomCorners, middleCorners)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(surfaceCard(isDark))
            .clickable(onClick = { showDialog = true })
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(stringResource(R.string.settings_frame_color), style = MaterialTheme.typography.bodyLarge, color = onSurface(isDark))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Box(
                        Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (current.isFrosted) frostedDotBrush else SolidColor(Color(current.color)))
                            .border(1.dp, onSurfaceSoft(isDark).copy(alpha = 0.4f), RoundedCornerShape(4.dp)),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(if (current.isFrosted) R.string.frame_frosted else R.string.settings_frame_color_current),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                        color = onSurfaceSoft(isDark),
                    )
                }
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = onSurface(isDark))
        }
    }

    if (showDialog) {
        FrameColorPickerDialog(
            initial = current,
            isDark = isDark,
            onDismiss = { showDialog = false },
            onConfirm = { cfg -> onSelect(cfg); showDialog = false },
        )
    }
}

@Composable
private fun FrameColorPickerDialog(
    initial: FrameConfig,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (FrameConfig) -> Unit,
) {
    var sel by remember { mutableStateOf<FrameChoice>(if (initial.isFrosted) FrameChoice.Frosted else FrameChoice.Solid(initial.color)) }
    val hsv = remember { FloatArray(3) }
    remember(initial.color) { android.graphics.Color.colorToHSV(initial.color, hsv) }
    var hue by remember { mutableStateOf(hsv[0]) }
    var sat by remember { mutableStateOf(hsv[1]) }
    var value by remember { mutableStateOf(hsv[2]) }

    fun solidFromHsv() = FrameChoice.Solid(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value)))

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surfaceCard(isDark),
        title = { Text(stringResource(R.string.dialog_frame_title), color = onSurface(isDark)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                // 预览：毛玻璃（柔和实色底 + 中心模糊白点）或当前纯色
                val frostedPreviewBg = if (isDark) Color(0xFF3A3A3A) else Color(0xFFE7DFD1)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (sel is FrameChoice.Frosted) frostedPreviewBg else Color((sel as FrameChoice.Solid).color))
                        .border(1.dp, onSurfaceSoft(isDark).copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                ) {
                    if (sel is FrameChoice.Frosted) {
                        Box(Modifier.align(Alignment.Center).size(72.dp).blur(16.dp).background(frostedDotBrush, CircleShape))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.frame_presets), color = onSurfaceSoft(isDark), style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.height(104.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(FRAME_SOLID_COLORS.size) { i ->
                        val c = FRAME_SOLID_COLORS[i]
                        val selected = sel is FrameChoice.Solid && (sel as FrameChoice.Solid).color == c
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .border(
                                    if (selected) 3.dp else 1.dp,
                                    if (selected) (if (isDark) RetroCream else RetroBrown) else onSurfaceSoft(isDark).copy(alpha = 0.4f),
                                    CircleShape,
                                )
                                .clickable {
                                    sel = FrameChoice.Solid(c)
                                    android.graphics.Color.colorToHSV(c, hsv)
                                    hue = hsv[0]; sat = hsv[1]; value = hsv[2]
                                },
                        )
                    }
                    // 毛玻璃：50% 圆角胶囊，背景 = 柔和实色底 + 中心"模糊白点"（径向渐变 + blur），与编辑器一致
                    item(span = { GridItemSpan(2) }) {
                        val selected = sel is FrameChoice.Frosted
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (isDark) Color(0xFF3A3A3A) else Color(0xFFE7DFD1))
                                .border(
                                    if (selected) 3.dp else 1.dp,
                                    if (selected) (if (isDark) RetroCream else RetroBrown) else onSurfaceSoft(isDark).copy(alpha = 0.4f),
                                    RoundedCornerShape(50),
                                )
                                .clickable { sel = FrameChoice.Frosted },
                            contentAlignment = Alignment.Center,
                        ) {
                            // 模糊圆点（API<31 时 blur 为 no-op，径向渐变本身已是柔化光斑，观感仍成立）
                            Box(Modifier.size(72.dp).blur(16.dp).background(frostedDotBrush, CircleShape))
                            Text(stringResource(R.string.frame_frosted), color = if (isDark) Color.White else Color(0xFF1A1A1A), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                // HSV 色线：毛玻璃选中时灰化禁用（毛玻璃背景与自定义颜色互斥，避免拖动把选择切回纯色）
                val hsvEnabled = sel !is FrameChoice.Frosted
                val hsvLabelColor = if (hsvEnabled) onSurface(isDark) else onSurface(isDark).copy(alpha = 0.35f)
                Text(stringResource(R.string.frame_custom), color = if (hsvEnabled) onSurfaceSoft(isDark) else onSurfaceSoft(isDark).copy(alpha = 0.35f), style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.frame_hue), color = hsvLabelColor, style = MaterialTheme.typography.labelSmall)
                Slider(value = hue / 360f, onValueChange = { hue = it * 360f; sel = solidFromHsv() }, colors = frameSliderColors(isDark), enabled = hsvEnabled)
                Text(stringResource(R.string.frame_saturation), color = hsvLabelColor, style = MaterialTheme.typography.labelSmall)
                Slider(value = sat, onValueChange = { sat = it; sel = solidFromHsv() }, colors = frameSliderColors(isDark), enabled = hsvEnabled)
                Text(stringResource(R.string.frame_value), color = hsvLabelColor, style = MaterialTheme.typography.labelSmall)
                Slider(value = value, onValueChange = { value = it; sel = solidFromHsv() }, colors = frameSliderColors(isDark), enabled = hsvEnabled)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    FrameConfig(
                        color = if (sel is FrameChoice.Solid) (sel as FrameChoice.Solid).color else DEFAULT_FRAME_COLOR,
                        isFrosted = sel is FrameChoice.Frosted,
                    ),
                )
            }) { Text(stringResource(R.string.dialog_done), color = onSurface(isDark)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel), color = onSurfaceSoft(isDark)) }
        },
    )
}

@Composable
private fun frameSliderColors(isDark: Boolean) = SliderDefaults.colors(
    thumbColor = if (isDark) RetroCream else RetroBrown,
    activeTrackColor = if (isDark) RetroCream else RetroBrown,
    inactiveTrackColor = RetroBrownLight,
    // 毛玻璃选中时的禁用灰化态
    disabledThumbColor = if (isDark) Color(0xFF6E6E6E) else Color(0xFFB0A79A),
    disabledActiveTrackColor = if (isDark) Color(0xFF555555) else Color(0xFFC4BBAD),
    disabledInactiveTrackColor = if (isDark) Color(0xFF2E2E2E) else Color(0xFFD8D0C4),
)

/**
 * 个性化 - 语言：行内展示当前语言，点按整行弹出 Material 风格单选对话框
 * （系统默认 / 简体中文 / 繁体中文 / 英文），选中即生效并重建当前界面以套用。
 */
@Composable
private fun LanguageRow(
    current: LangMode,
    isDark: Boolean,
    onSelect: (LangMode) -> Unit,
    onApplied: () -> Unit,
    topCorners: Boolean = false,
    bottomCorners: Boolean = false,
    middleCorners: Boolean = false,
) {
    var showDialog by remember { mutableStateOf(false) }
    val shape = rowShape(topCorners, bottomCorners, middleCorners)
    // 每个选项以「自身母语」展示（不受当前 app 语言影响）：用对应 Locale 的 context 取字符串
    val ctx = LocalContext.current
    val nativeLabel: (LangMode) -> String = { mode ->
        if (mode == LangMode.SYSTEM) ctx.getString(mode.labelRes) else AppLocale.wrap(ctx, mode).getString(mode.labelRes)
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(surfaceCard(isDark))
            .clickable(onClick = { showDialog = true })
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.bodyLarge, color = onSurface(isDark))
                Text(stringResource(R.string.settings_language_current, nativeLabel(current)), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal), color = onSurfaceSoft(isDark))
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = onSurface(isDark))
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = surfaceCard(isDark),
            title = { Text(stringResource(R.string.dialog_language_title), color = onSurface(isDark)) },
            text = {
                Column {
                    LangMode.entries.forEach { mode ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(mode)
                                    showDialog = false
                                    onApplied()
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = current == mode,
                                onClick = {
                                    onSelect(mode)
                                    showDialog = false
                                    onApplied()
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = if (isDark) RetroCream else RetroBrown,
                                    unselectedColor = onSurfaceSoft(isDark),
                                ),
                            )
                            Text(nativeLabel(mode), color = onSurface(isDark), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.dialog_cancel), color = onSurfaceSoft(isDark)) }
            },
        )
    }
}

// 预留链接（待用户提供）
private const val GITHUB_URL = "https://github.com/Genkaim"
private const val FEEDBACK_URL = "https://github.com/Genkaim/InstCam/issues"
private const val RELEASES_URL = "https://github.com/Genkaim/InstCam/releases"
private const val REWARD_URL = "https://space.bilibili.com/479907494"

// GitHub mark 路径（viewBox 24x24），用于绘制 logo 占位
private const val GITHUB_PATH = "M12 1.5C6.2 1.5 1.5 6.2 1.5 12c0 4.65 3.01 8.59 7.2 9.99.53.1.72-.23.72-.5v-1.75c-2.93.64-3.55-1.41-3.55-1.41-.48-1.22-1.17-1.55-1.17-1.55-.96-.65.07-.64.07-.64 1.06.08 1.62 1.09 1.62 1.09.94 1.61 2.47 1.15 3.07.88.1-.68.37-1.15.67-1.41-2.34-.27-4.8-1.17-4.8-5.2 0-1.15.41-2.09 1.08-2.83-.11-.27-.47-1.34.1-2.79 0 0 .88-.28 2.88 1.08a9.9 9.9 0 0 1 5.24 0c2-1.36 2.88-1.08 2.88-1.08.57 1.45.21 2.52.1 2.79.67.74 1.08 1.68 1.08 2.83 0 4.04-2.46 4.93-4.81 5.19.38.33.71.97.71 1.96v2.9c0 .28.19.61.73.5A10.51 10.51 0 0 0 22.5 12C22.5 6.2 17.8 1.5 12 1.5z"

private fun openLink(context: Context, url: String) {
    if (url.isBlank()) return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) {
        // 链接给出后处理异常
    }
}

@Composable
private fun GithubLogo(tint: Color) {
    val path = remember { PathParser().parsePathString(GITHUB_PATH).toPath() }
    // 注意：drawPath 以路径自然坐标(0~24, viewBox)绘制，不会自动缩放。
    // 必须用 scale 变换将 24×24 等比缩放到 Canvas 尺寸，否则路径在各密度下"大小没变"。
    Canvas(Modifier.size(28.dp)) {
        // 将 24×24 viewBox 路径等比缩放到 Canvas 实际尺寸后绘制
        val s = size.width / 24f
        val mx = Matrix(floatArrayOf(
            s, 0f, 0f, 0f,
            0f, s, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f,
        ))
        val scaledPath = Path().apply {
            addPath(path, Offset.Zero)
            transform(mx)
        }
        drawPath(path = scaledPath, color = tint)
    }
}
