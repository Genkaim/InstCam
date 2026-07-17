package com.genkaim.picocam.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genkaim.picocam.FlashMode
import com.genkaim.picocam.ui.theme.RetroBrown
import com.genkaim.picocam.ui.theme.RetroBrownLight
import com.genkaim.picocam.ui.theme.RetroCream
import com.genkaim.picocam.ui.theme.RetroRust
import com.genkaim.picocam.ui.theme.onSurface
import com.genkaim.picocam.ui.theme.onSurfaceSoft

@Composable
fun ControlBar(
    flashMode: FlashMode,
    isBackCamera: Boolean,
    effectsExpanded: Boolean,
    onToggleFlash: () -> Unit,
    onSwitchCamera: () -> Unit,
    onShutter: () -> Unit,
    onSettings: () -> Unit,
    onToggleEffects: () -> Unit,
    isDark: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsButton(isDark = isDark, onClick = onSettings)
        FlashButton(flashMode = flashMode, isDark = isDark, onClick = onToggleFlash)
        ShutterButton(onClick = onShutter, isDark = isDark)
        SwitchCameraButton(isBackCamera = isBackCamera, isDark = isDark, onClick = onSwitchCamera)
        EffectsButton(effectsExpanded = effectsExpanded, isDark = isDark, onClick = onToggleEffects)
    }
}

/**
 * 手动相机参数项（对焦距离 / 快门速度）：与滤镜滑块同交互——左右横滑调节，名字后显示 < > 图标，
 * 右侧显示当前参数值。fraction 0~1 由调用方映射到对应相机参数（0 = 自动）。
 */
@Composable
fun CameraParamItem(
    name: String,
    valueText: String,
    fraction: Float,
    onFractionChange: (Float) -> Unit,
    isDark: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var btnWidth by remember { mutableStateOf(1) }
    var dragStart by remember { mutableFloatStateOf(0f) }
    var dragAccum by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    // 用 rememberUpdatedState 取最新 fraction，避免 pointerInput(Unit) 只捕获首次组合的值（导致每次拖拽都从 0 起算）
    val fractionState by rememberUpdatedState(fraction)
    val onFractionChangeState by rememberUpdatedState(onFractionChange)
    val density = LocalDensity.current
    Box(modifier) {
        // 实际项：去掉背景，直接与取景框/预览融合
        Box(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .onSizeChanged { btnWidth = it.width }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragging = true; dragStart = fractionState; dragAccum = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragAccum += dragAmount
                            // 整钮宽 0.7 对应满量程(0~1)，与滤镜滑块一致
                            val newVal = (dragStart + dragAccum / (btnWidth.toFloat() * 0.7f).coerceAtLeast(1f))
                                .coerceIn(0f, 1f)
                            onFractionChangeState(newVal)
                        },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                    )
                },
            contentAlignment = Alignment.CenterStart,
        ) {
                Row(
                Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(name, color = if (isDark) Color.White else Color(0xFF6B5744), fontSize = 13.sp)
                Spacer(Modifier.width(4.dp))
                // 与滤镜区域一致的"横滑调节"提示图标 < >
                SlideHintIcon(color = if (isDark) Color(0xFFCFCFCF) else Color(0xFFB6A796))
                Spacer(Modifier.weight(1f))
                Text(valueText, color = if (isDark) Color.White else RetroBrown, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        // 滑动示意气泡：浮于项正上方，深色填充比例 + 实时参数值（与滤镜滑块弹窗风格一致）
        AnimatedVisibility(
            visible = dragging,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, with(density) { (-48).dp.toPx() }.toInt()) },
            enter = fadeIn(tween(150)) + slideInVertically(
                animationSpec = tween(150),
                initialOffsetY = { (it * 0.5f).toInt() }, // 从略低处上浮到位
            ),
            exit = fadeOut(tween(120)) + slideOutVertically(
                animationSpec = tween(120),
                targetOffsetY = { -(it * 0.3f).toInt() }, // 淡出时略微上浮
            ),
        ) {
            ParamHintBubble(fraction = fraction, label = valueText)
        }
    }
}

/** 手动参数项滑动时浮于项正上方的"调节示意"：固定长度轨道，深色填充代表参数百分比（指示数据），
 *  最左端固定标注"默认"（最左=默认），中间显示实时参数值（与滤镜滑块弹窗风格一致）。 */
@Composable
private fun ParamHintBubble(fraction: Float, label: String, modifier: Modifier = Modifier) {
    val frac = fraction.coerceIn(0f, 1f)
    Box(
        modifier
            .width(200.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFE0DCD4)),
    ) {
        // 深色填充：从左侧按百分比增长，指示当前数据位置
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(frac)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF3A2E22)),
        )
        // 最左端固定标注"默认"——填充盖住时改为浅色保证可读
        Text(
            "默认",
            color = if (frac > 0.12f) Color.White else Color(0xFF3A2E22),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 14.dp),
        )
        // 中间实时参数值
        Text(
            label,
            color = if (frac >= 0.5f) Color.White else Color(0xFF3A2E22),
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
fun ZoomBar(zoomLevel: Float, onZoomChange: (Float) -> Unit, isDark: Boolean = false, modifier: Modifier = Modifier) {
    val levels = listOf(1f, 2f, 3f, 5f)
    // 倍数切换区域不再有背景（无论深浅色都透明），仅保留激活态的圆形高亮
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            levels.forEach { level ->
                val active = zoomLevel == level
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (active) RetroRust else Color.Transparent)
                        .clickable { onZoomChange(level) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (level == 1f) "1x" else "${level.toInt()}x",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (active) RetroCream else onSurface(isDark),
                    )
                }
            }
        }
    }
}

@Composable
private fun FlashButton(flashMode: FlashMode, isDark: Boolean, onClick: () -> Unit) {
    RetroCircleButton(onClick = onClick) {
        AnimatedContent(targetState = flashMode, label = "flashIcon") { mode ->
            val icon: ImageVector = when (mode) {
                FlashMode.OFF -> Icons.Filled.FlashOff
                FlashMode.AUTO -> Icons.Filled.FlashAuto
                FlashMode.ON -> Icons.Filled.FlashOn
            }
            // 关闭态：深肤色用棕、深灰底用白；开启/自动态铁锈红高亮
            val offTint = if (isDark) Color.White else RetroBrownLight
            Icon(icon, contentDescription = "闪光灯",
                tint = if (mode == FlashMode.OFF) offTint else RetroRust)
        }
    }
}

@Composable
private fun SwitchCameraButton(isBackCamera: Boolean, isDark: Boolean, onClick: () -> Unit) {
    var rotation by remember { mutableFloatStateOf(0f) }
    val rot by animateFloatAsState(targetValue = rotation, animationSpec = tween(420), label = "camRot")
    RetroCircleButton(onClick = {
        rotation += 180f
        onClick()
    }) {
        Icon(Icons.Filled.Cameraswitch, contentDescription = "切换摄像头",
            tint = if (isDark) Color.White else RetroBrown, modifier = Modifier.rotate(rot))
    }
}

@Composable
private fun SettingsButton(isDark: Boolean, onClick: () -> Unit) {
    RetroCircleButton(onClick = onClick) {
        Icon(Icons.Filled.Settings, contentDescription = "设置", tint = if (isDark) Color.White else RetroBrown)
    }
}

@Composable
private fun EffectsButton(effectsExpanded: Boolean, isDark: Boolean, onClick: () -> Unit) {
    RetroCircleButton(onClick = onClick) {
        Icon(
            imageVector = if (effectsExpanded) Icons.Filled.Close else Icons.Filled.AutoAwesome,
            contentDescription = "特效",
            tint = if (effectsExpanded) RetroRust else if (isDark) Color.White else RetroBrown,
        )
    }
}

@Composable
private fun RetroCircleButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}
