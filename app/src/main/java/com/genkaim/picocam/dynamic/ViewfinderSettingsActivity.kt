package com.genkaim.picocam.dynamic

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.genkaim.picocam.dynamic.AppPrefs
import com.genkaim.picocam.dynamic.ViewfinderPrefs.saveViewfinder
import com.genkaim.picocam.dynamic.isDarkMode
import com.genkaim.picocam.ui.theme.RetroBrown
import com.genkaim.picocam.ui.theme.RetroBrownLight
import com.genkaim.picocam.ui.theme.RetroCream
import com.genkaim.picocam.ui.theme.RetroDarkBg
import com.genkaim.picocam.ui.theme.RetroDarkSurface
import com.genkaim.picocam.ui.theme.RetroInk
import com.genkaim.picocam.ui.theme.RetroPaper
import com.genkaim.picocam.ui.theme.RetroRust
import com.genkaim.picocam.ui.theme.onSurface
import com.genkaim.picocam.ui.theme.onSurfaceSoft
import com.genkaim.picocam.ui.theme.surfaceCard
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 取景框设置页：上半部分是一个取景框示意（方形，可上下拖动定位），下半部分是调整参数。
 * 记录垂直位置、宽度、圆角三个参数，供主界面取景框使用。
 */
class ViewfinderSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideStatusBar()
        val onboarding = intent?.getBooleanExtra("onboarding", false) ?: false
        setContent {
            ViewfinderSettingsContent(
                onboarding = onboarding,
                onDone = { setResult(RESULT_OK); finish() },
                onBack = { finish() },
            )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideStatusBar()
    }

    private fun hideStatusBar() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.statusBars())
    }
}

@Composable
private fun ViewfinderSettingsContent(onboarding: Boolean, onDone: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val cfg by AppPrefs.viewfinder.collectAsStateWithLifecycle()
    val diCfg by AppPrefs.dynamicIsland.collectAsStateWithLifecycle()
    // 深色模式判定
    val theme by AppPrefs.theme.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isSystemDark = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val isDark = isDarkMode(theme.mode, isSystemDark)

    var posY by remember { mutableFloatStateOf(cfg.posY) }
    var width by remember { mutableFloatStateOf(cfg.widthDp.toFloat()) }
    var corner by remember { mutableFloatStateOf(cfg.cornerDp.toFloat()) }
    var showReset by remember { mutableStateOf(false) }

    // 每次配置流有新值（含启动首次读盘）都同步到本地状态；
    // 拖动时只改本地 state 不写盘，cfg 不变不会打断拖动，松手 commit 后 cfg 回写相同值也不跳变。
    LaunchedEffect(cfg) {
        posY = cfg.posY
        width = cfg.widthDp.toFloat()
        corner = cfg.cornerDp.toFloat()
    }

    fun commit() {
        scope.launch {
            context.saveViewfinder {
                copy(
                    posY = posY,
                    widthDp = width.roundToInt(),
                    cornerDp = corner.roundToInt(),
                )
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) RetroDarkBg else RetroPaper),
    ) {
        val screenWpx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
        val screenHpx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
        val halfHpx = screenHpx / 2f
        val framePx = with(density) { width.dp.toPx() }
        // 边框宽度取灵动岛高度（与主界面一致），并约束不超过取景框一半
        val border = diCfg.heightDp.coerceIn(0, width.roundToInt() / 2)
        val innerCorner = (corner - border).coerceAtLeast(0f)
        // 示意区（上半部分）内的竖向定位：基于可用高度百分比，posY=0.5 居中，与主线一致
        val maxOffsetPx = (halfHpx - framePx).coerceAtLeast(0f)
        val offsetY = (posY - 0.5f) * maxOffsetPx
        val cy = halfHpx / 2f + offsetY
        val cx = screenWpx / 2f

        // 上半部分：取景框示意（方形，可上下拖动）
        Box(
            modifier = Modifier
                .offset { IntOffset((cx - framePx / 2f).roundToInt(), (cy - framePx / 2f).roundToInt()) }
                .size(width.dp)
                .clip(RoundedCornerShape(corner.dp))
                .background(Color(0xFF1A1A1A))
                .padding(border.dp)
                .clip(RoundedCornerShape(innerCorner.dp))
                .background(Color(0xFF0D0D0D))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, drag ->
                            change.consume()
                            // 仅竖向移动
                            val newCy = (cy + drag.y).coerceIn(framePx / 2f, halfHpx - framePx / 2f)
                            posY = if (maxOffsetPx > 0f) {
                                ((newCy - halfHpx / 2f) / maxOffsetPx + 0.5f).coerceIn(0f, 1f)
                            } else 0.5f
                        },
                        onDragEnd = { commit() },
                    )
                },
        )

        // 下半部分：参数面板：头部（返回+标题）固定，内容可滚动
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(if (isDark) RetroDarkSurface else RetroCream),
        ) {
            // 固定头部：返回（左）+ 标题 + 右侧「完成」药丸按钮
            // 引导流程没有返回按钮，标题 start padding 加大到 24dp 避免贴边；正常设置入口用 8dp 容纳返回按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (onboarding) 24.dp else 8.dp,
                        end = 24.dp,
                        top = 12.dp,
                        bottom = 12.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 设置入口：左上返回键（与设置页一致的 44dp 圆药丸）；引导流程无返回
                if (!onboarding) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(surfaceCard(isDark))
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = onSurface(isDark))
                    }
                    Spacer(Modifier.width(12.dp))
                }
                Text("调整取景框", style = MaterialTheme.typography.titleLarge, color = onSurface(isDark))
                Spacer(Modifier.weight(1f))
                // 引导流程：右上「完成」药丸按钮
                if (onboarding) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(if (isDark) Color(0xFF3A3A3A) else RetroBrown)
                            .clickable(onClick = onDone)
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "完成",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isDark) Color.White else RetroCream,
                        )
                    }
                }
            }

            // 可滚动内容区
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "上方是取景框示意，可直接上下拖动定位，或用下面的滑块调整位置、宽度与圆角。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = onSurface(isDark),
                )

                // 还原默认（二次确认）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) RetroRust else RetroBrown)
                        .clickable(onClick = { showReset = true })
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("还原默认", style = MaterialTheme.typography.titleMedium, color = RetroCream)
                }

                SettingSlider("上下位置", posY, { posY = it }, { commit() }, 0f..1f, "${(posY * 100).roundToInt()}%", isDark)
                SettingSlider("宽度", width, { width = it }, { commit() }, 120f..400f, "${width.roundToInt()} dp", isDark)
                SettingSlider("圆角", corner, { corner = it }, { commit() }, 0f..120f, "${corner.roundToInt()} dp", isDark)
            }
        }

        if (showReset) {
            AlertDialog(
                onDismissRequest = { showReset = false },
                // 容器色随深色模式：深色=深灰卡片，浅色=奶油白
                containerColor = surfaceCard(isDark),
                title = { Text("还原默认", color = onSurface(isDark)) },
                text = { Text("确定要将取景框参数还原为默认值吗？此操作不可撤销。", color = onSurface(isDark)) },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch { context.saveViewfinder { ViewfinderConfig() } }
                        showReset = false
                    }) { Text("还原", color = RetroRust) }
                },
                dismissButton = {
                    TextButton(onClick = { showReset = false }) { Text("取消", color = onSurfaceSoft(isDark)) }
                },
            )
        }
    }
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    display: String,
    isDark: Boolean,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = onSurface(isDark))
            Text(display, style = MaterialTheme.typography.bodyMedium, color = onSurface(isDark))
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = if (isDark) Color.White else RetroBrown,
                activeTrackColor = if (isDark) Color.White else RetroBrown,
                inactiveTrackColor = onSurfaceSoft(isDark).copy(alpha = 0.4f),
            ),
        )
    }
}
