package com.genkaim.picocam

import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.genkaim.picocam.R
import com.genkaim.picocam.dynamic.AnimConfig
import com.genkaim.picocam.dynamic.AnimPrefs.saveAnimSettings
import com.genkaim.picocam.dynamic.AppPrefs
import com.genkaim.picocam.dynamic.DynamicIslandSettingsActivity
import com.genkaim.picocam.dynamic.ThemeConfig
import com.genkaim.picocam.dynamic.ThemeMode
import com.genkaim.picocam.dynamic.ThemePrefs.saveThemeSettings
import com.genkaim.picocam.dynamic.ViewfinderSettingsActivity
import com.genkaim.picocam.dynamic.isDarkMode
import com.genkaim.picocam.ui.theme.RetroBrown
import com.genkaim.picocam.ui.theme.RetroBrownLight
import com.genkaim.picocam.ui.theme.RetroCream
import com.genkaim.picocam.ui.theme.RetroDarkBg
import com.genkaim.picocam.ui.theme.RetroPaper
import com.genkaim.picocam.ui.theme.onSurface
import com.genkaim.picocam.ui.theme.onSurfaceSoft
import com.genkaim.picocam.ui.theme.surfaceCard
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
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
            Text("设置", style = MaterialTheme.typography.headlineMedium, color = onSurface(isDark), modifier = Modifier.alpha(1f - titleCollapse))
            Spacer(Modifier.height(16.dp))

            // —— 第一组：灵动岛与取景框设置 ——
            GroupTitle("灵动岛与取景框设置", isDark)
            Spacer(Modifier.height(10.dp))
            SettingsGroupColumn {
                // 灵动岛入口（上大圆角、下小圆角）
                SettingRow("灵动岛", "记录灵动岛位置与尺寸", topCorners = true, isDark = isDark) {
                    context.startActivity(Intent(context, DynamicIslandSettingsActivity::class.java))
                }
                // 取景框入口（在灵动岛下侧，上小圆角、下大圆角）
                SettingRow("取景框", "调整取景框位置 / 尺寸 / 圆角", bottomCorners = true, isDark = isDark) {
                    context.startActivity(Intent(context, ViewfinderSettingsActivity::class.java))
                }
            }

            // 组间间距
            Spacer(Modifier.height(16.dp))

            // —— 第二组：动画设置 ——
            GroupTitle("动画设置", isDark)
            Spacer(Modifier.height(10.dp))
            SettingsGroupColumn {
                // 动画总开关（上大圆角）；关闭后本组其余元素变灰且不可操作
                SettingToggleRow(
                    title = "动画总开关",
                    desc = "关闭后仅正常拍照，不播放拍立得动画",
                    checked = anim.animEnabled,
                    onCheckedChange = { saveAnim { copy(animEnabled = it) } },
                    topCorners = true,
                    isDark = isDark,
                )
                // 拍照后默认打开取景框（下大圆角）；动画关闭时变灰不可操作
                SettingToggleRow(
                    title = "拍照后默认打开取景框",
                    desc = "拍完自动展开到设置里指定的大小",
                    checked = anim.openViewfinderAfterCapture,
                    enabled = anim.animEnabled,
                    onCheckedChange = { saveAnim { copy(openViewfinderAfterCapture = it) } },
                    bottomCorners = true,
                    isDark = isDark,
                )
            }

            Spacer(Modifier.height(16.dp))

            // —— 第三组：个性化 ——
            GroupTitle("个性化", isDark)
            Spacer(Modifier.height(10.dp))
            ThemeColorRow(
                current = theme.mode,
                isDark = isDark,
                onSelect = { saveTheme { copy(mode = it) } },
            )

            // 组间间距
            Spacer(Modifier.height(16.dp))

            // —— 第四组：杂项 ——
            GroupTitle("杂项", isDark)
            Spacer(Modifier.height(10.dp))
            SettingsGroupColumn {
                // 反馈与Bug提交：移到本组，点击跳转反馈链接（预留）
                SettingRow("反馈与Bug提交", "帮助我们改进产品", topCorners = true, isDark = isDark) {
                    openLink(context, FEEDBACK_URL)
                }
                // 查看最新版本：点击打开 GitHub releases 页面
                SettingRow("查看最新版本", "当前版本 V$versionName", middleCorners = true, isDark = isDark) {
                    openLink(context, RELEASES_URL)
                }
                // 打赏作者：底部大圆角
                SettingRow("打赏作者（B站发电）", "如果喜欢 InstCam，可以请作者喝杯咖啡", bottomCorners = true, isDark = isDark) {
                    openLink(context, REWARD_URL)
                }
            }

            // 组间间距
            Spacer(Modifier.height(16.dp))

            // —— 第五组：Developer ——
            GroupTitle("Developer", isDark)
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
            GroupTitle("特别鸣谢", isDark)
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
            "设置",
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
    Text(text, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Light), color = onSurfaceSoft(isDark))
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
            Column(modifier = Modifier.padding(end = 12.dp)) {
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
) {
    var showDialog by remember { mutableStateOf(false) }
    // 整行可点按，点击弹出系统风格单选对话框
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
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
                Text("主题颜色", style = MaterialTheme.typography.bodyLarge, color = onSurface(isDark))
                Text("当前：${current.label}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal), color = onSurfaceSoft(isDark))
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = onSurface(isDark))
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            // 容器色随深色模式切换：深色=深灰卡片，浅色=奶油白，保证和系统配色一致
            containerColor = surfaceCard(isDark),
            title = { Text("主题颜色", color = onSurface(isDark)) },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
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
                            Text(mode.label, color = onSurface(isDark), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("取消", color = onSurfaceSoft(isDark)) }
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
