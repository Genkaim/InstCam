package com.genkaim.picocam.dynamic

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.genkaim.picocam.dynamic.AppPrefs
import com.genkaim.picocam.dynamic.DynamicIslandPrefs.saveDynamicIsland
import com.genkaim.picocam.dynamic.ViewfinderSettingsActivity
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
 * 灵动岛位置记录页：只让用户"画出"自己机型灵动岛/挖孔在屏幕上的位置与尺寸，
 * 把参数（水平/垂直位置、宽、高、圆角）存进 DataStore 供后续使用。
 * 全屏沉浸，顶部留白区与真实屏幕顶部 1:1 对齐，方便对准物理挖孔。
 */
class DynamicIslandSettingsActivity : ComponentActivity() {
    private val vfLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // 取景框设置完成（点「完成」）后，连同本页一起关闭，回到主界面
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideStatusBar()
        val onboarding = intent?.getBooleanExtra("onboarding", false) ?: false
        setContent {
            DynamicIslandSettingsContent(
                onboarding = onboarding,
                onNext = {
                    vfLauncher.launch(
                        Intent(this@DynamicIslandSettingsActivity, ViewfinderSettingsActivity::class.java)
                            .putExtra("onboarding", true),
                    )
                },
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
private fun DynamicIslandSettingsContent(onboarding: Boolean, onNext: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val cfg by AppPrefs.dynamicIsland.collectAsStateWithLifecycle()
    // 深色模式判定
    val theme by AppPrefs.theme.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isSystemDark = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val isDark = isDarkMode(theme.mode, isSystemDark)

    var posX by remember { mutableFloatStateOf(cfg.posX) }
    var posY by remember { mutableFloatStateOf(cfg.posY) }
    var width by remember { mutableFloatStateOf(cfg.widthDp.toFloat()) }
    var height by remember { mutableFloatStateOf(cfg.heightDp.toFloat()) }
    var corner by remember { mutableFloatStateOf(cfg.cornerDp.toFloat()) }
    var showReset by remember { mutableStateOf(false) }

    // 每次配置流有新值（含启动首次读盘）都同步到本地状态；
    // 拖动时只改本地 state 不写盘，cfg 不变不会打断拖动，松手 commit 后 cfg 回写相同值也不跳变。
    LaunchedEffect(cfg) {
        posX = cfg.posX
        posY = cfg.posY
        width = cfg.widthDp.toFloat()
        height = cfg.heightDp.toFloat()
        corner = cfg.cornerDp.toFloat()
    }

    fun commit() {
        scope.launch {
            context.saveDynamicIsland {
                copy(
                    enabled = true,
                    posX = posX,
                    posY = posY,
                    widthDp = width.roundToInt(),
                    heightDp = height.roundToInt(),
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
        val screenWpx = with(density) { maxWidth.toPx() }
        val screenHpx = with(density) { maxHeight.toPx() }
        val pillWpx = with(density) { width.dp.toPx() }
        val pillHpx = with(density) { height.dp.toPx() }
        val offX = (posX * screenWpx - pillWpx / 2f)
        val offY = (posY * screenHpx - pillHpx / 2f)
        val cornerDp = (if (corner < height / 2f) corner else height / 2f).coerceAtLeast(0f)

        // 顶部留白区里的灵动岛示意块（可直接拖动）：半透明 + 红色描边
        Box(
            modifier = Modifier
                .offset { IntOffset(offX.roundToInt(), offY.roundToInt()) }
                .size(width.dp, height.dp)
                .clip(RoundedCornerShape(cornerDp.dp))
                .background(Color.Black.copy(alpha = 0.35f))
                .border(2.dp, Color.Red, RoundedCornerShape(cornerDp.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, drag ->
                            change.consume()
                            posX = ((posX * screenWpx + drag.x) / screenWpx).coerceIn(0f, 1f)
                            posY = ((posY * screenHpx + drag.y) / screenHpx).coerceIn(0f, 0.3f)
                        },
                        onDragEnd = { commit() },
                    )
                },
        )

        // 底部参数面板：头部（返回+标题）固定，内容可滚动
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                .background(if (isDark) RetroDarkSurface else RetroCream),
        ) {
            // 固定头部：返回（左）+ 标题 + 右侧箭头
            // 引导流程没有返回按钮，标题 start padding 加大到 24dp 避免贴边；正常设置入口用 8dp 容纳返回按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (onboarding) 24.dp else 8.dp,
                        end = 24.dp,
                        top = 20.dp,
                        bottom = 20.dp,
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
                Text("标记灵动岛位置", style = MaterialTheme.typography.titleLarge, color = onSurface(isDark))
                Spacer(Modifier.weight(1f))
                // 引导流程：右上箭头进入取景框设置
                if (onboarding) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isDark) RetroDarkSurface else RetroBrown)
                            .clickable(onClick = onNext),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "下一步",
                            tint = if (isDark) Color.White else RetroCream,
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

                SettingSlider("水平位置", posX, { posX = it }, { commit() }, 0f..1f, "${(posX * 100).roundToInt()}%", isDark)
                SettingSlider("垂直位置", posY, { posY = it }, { commit() }, 0f..0.3f, "${(posY * 100).roundToInt()}%", isDark)
                SettingSlider("宽度", width, { width = it }, { commit() }, 16f..400f, "${width.roundToInt()} dp", isDark)
                SettingSlider("高度", height, { height = it }, { commit() }, 16f..160f, "${height.roundToInt()} dp", isDark)
                SettingSlider("圆角", corner, { corner = it }, { commit() }, 0f..80f, "${corner.roundToInt()} dp", isDark)
            }
        }

        if (showReset) {
            AlertDialog(
                onDismissRequest = { showReset = false },
                // 容器色随深色模式：深色=深灰卡片，浅色=奶油白
                containerColor = surfaceCard(isDark),
                title = { Text("还原默认", color = onSurface(isDark)) },
                text = { Text("确定要将灵动岛参数还原为默认值吗？此操作不可撤销。", color = onSurface(isDark)) },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch { context.saveDynamicIsland { DynamicIslandConfig() } }
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
