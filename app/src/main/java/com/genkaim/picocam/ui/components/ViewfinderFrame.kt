package com.genkaim.picocam.ui.components

import android.os.Build
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.genkaim.picocam.camera.EffectiveFilter
import com.genkaim.picocam.camera.buildFilterColorMatrix

/** 内圆角下限（dp）：当 外圆角−边框厚度 < 此值时夹到此处，避免内圆角转负渲染成直角。 */
private const val MIN_INNER_CORNER_DP = 8f

@Composable
fun ViewfinderFrame(
    preview: Preview,
    bindCameraUseCases: (androidx.lifecycle.LifecycleOwner) -> Unit,
    flashAlpha: Float,
    eff: EffectiveFilter = EffectiveFilter(),
    cornerDp: Float = 48f,
    borderDp: Float = 16f,
    onPreviewStreaming: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewView = remember { mutableStateOf<PreviewView?>(null) }
    var vignette by remember { mutableStateOf(0f) }

    LaunchedEffect(eff, previewView.value) {
        vignette = eff.vignette
        val view = previewView.value ?: return@LaunchedEffect
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                view.setRenderEffect(
                    if (eff.isIdentity()) null
                    else android.graphics.RenderEffect.createColorFilterEffect(
                        android.graphics.ColorMatrixColorFilter(buildFilterColorMatrix(eff)),
                    )
                )
            } catch (_: Exception) {}
        }
    }

    // 切回前台（如从设置页返回主界面）时，MainActivity 会经历 ON_RESUME：LifecycleCameraController 在此时重新绑定相机，
    // 但首帧仍以 1x（默认焦段）渲染；而原 previewStreamState=STREAMING 回调发生在首帧【之后】，故会闪一下 1x。
    // 这里在 ON_RESUME 抢先按已保存焦段(_zoomLevel)套用，保证重新绑定后的首帧即正确倍率；STREAMING 回调保留作兜底。
    DisposableEffect(lifecycleOwner, onPreviewStreaming) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onPreviewStreaming()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 黑边用「draw 层 Canvas」而非「layout 层 padding」：
    // · padding 会把内层 AndroidView 缩小 —— 折叠态（小岛）时高度直接被吃成 0，
    //   PreviewView 的 Surface 为 0，cameraX 永远不会 STREAMING，焦段/手动参数失效。
    // · Canvas 黑边只在绘制层覆盖外缘，AndroidView 始终占满外框 → 任何 progress 下都能 STREAMING。
    // 圆角模型（按用户要求）：外圆角 = cornerDp（滑块直接调外圆角）；内圆角 = max(cornerDp - borderDp, MIN_INNER_CORNER_DP)。
    // · 内圆角 = 外圆角 − 边框厚度 的嵌套圆角矩形；仅当 cornerDp < borderDp（内圆角 < 下限）时夹到 MIN_INNER_CORNER_DP，避免变直角。
    // · 早期用 Modifier.border（描边居中）内半径 = cornerDp-borderDp/2，cornerDp<borderDp/2 时转负→自相交→渲染直角（旧 bug）。
    // 裁切分层：父 Box 与黑框均裁到 cornerDp（外圆角）；预览/暗角各自再裁到 cornerDp，外观不变、深色模式光晕不动。
    val innerCornerDp = (cornerDp - borderDp).coerceAtLeast(MIN_INNER_CORNER_DP)
    Box(
        modifier
            .clip(RoundedCornerShape(cornerDp.dp))
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    // FILL_CENTER：把 4:3 预览中心裁切成正方形，与 addPolaroidFrame 的 1:1 中心裁切一致（WYSIWYG）。
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    // 把 PreviewView 的 surfaceProvider 接到 Preview use case，
                    // 然后由 VM 把 preview + imageCapture 绑到 lifecycle（强制 4:3 硬约束）。
                    preview.setSurfaceProvider(this.surfaceProvider)
                    bindCameraUseCases(lifecycleOwner)
                    // 限制取景框帧率：相机出流(STREAMING)后回调 VM，由 VM 统一把 FPS + 对焦 + 快门
                    // 合并应用到 Camera2CameraControl（避免多次 setCaptureRequestOptions 互相覆盖）。
                    // 切摄像头/重新绑定时流会重新 STREAMING，观察者会再次触发，确保新相机同样限帧并套用当前手动参数。
                    previewStreamState.observe(lifecycleOwner) { state ->
                        if (state == PreviewView.StreamState.STREAMING) {
                            onPreviewStreaming()
                        }
                    }
                }
            },
            // PreviewView 必须始终 fillMaxSize：其尺寸一旦变化（如按比例内缩），
            // FILL_CENTER 会按新尺寸重新计算缩放 → 二次裁切 → 预览异常放大（尤其在展开动画中每帧重算）。
            // 黑边由上方 draw 层 Canvas 覆盖绘制，不占布局空间，保证 PreviewView 尺寸恒定 = WYSIWYG。
            // 仅按「内圆角」cornerDp 裁切预览（尺寸不变，外观与历史一致）。
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(cornerDp.dp)),
            update = { previewView.value = it },
        )
        // 暗角（深色模式/滤镜）：按 cornerDp 独立裁切，外观完全不变（"光晕"不动）。
        if (vignette > 0f) {
            Canvas(Modifier.fillMaxSize().clip(RoundedCornerShape(cornerDp.dp))) {
                val r = size.maxDimension * 0.75f
                val cx = size.width / 2f
                val cy = size.height / 2.2f
                val a = (vignette * 0xB0).toInt().coerceIn(0, 255)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Transparent, Color(android.graphics.Color.argb(a, 0, 0, 0))),
                        center = Offset(cx, cy),
                        radius = r,
                    ),
                    radius = r, center = Offset(cx, cy),
                )
            }
        }
        if (flashAlpha > 0f) Box(Modifier.fillMaxSize().clip(RoundedCornerShape(cornerDp.dp)).background(Color.White.copy(alpha = flashAlpha)))
        // 黑边框：外圆角 = cornerDp（=用户设定，滑块调外圆角），内圆角 = max(cornerDp-borderDp, MIN_INNER_CORNER_DP)。
        // 正常情况（cornerDp>=borderDp）内圆角 = cornerDp-borderDp，与外层同心、径向厚度 = borderDp（四角均匀）；
        // cornerDp<borderDp 时内圆角夹到 MIN_INNER_CORNER_DP（>=0，绝不自相交），仅角落略非均匀，但不再渲染成直角。
        // 本层用「外层圆角矩形 − 内层圆角矩形」even-odd 路径直接填充；父/本 Box 均裁到 cornerDp（外圆角）。
        // borderDp 可传 0（无边框，AndroidView 全露）或正值；折叠态 borderDp≈岛高 → 内层矩形尺寸≤0 → 整张纯黑（灵动岛外观）。
        if (borderDp > 0f) {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(cornerDp.dp))) {
                Canvas(Modifier.fillMaxSize()) {
                    val bPx = borderDp.dp.toPx()
                    val outerR = cornerDp.dp.toPx()
                    val innerR = innerCornerDp.dp.toPx()
                    val innerLeft = bPx
                    val innerTop = bPx
                    val innerRight = size.width - bPx
                    val innerBottom = size.height - bPx
                    if (innerRight > innerLeft && innerBottom > innerTop) {
                        val frame = Path().apply {
                            addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(outerR, outerR)))
                            addRoundRect(RoundRect(innerLeft, innerTop, innerRight, innerBottom, CornerRadius(innerR, innerR)))
                            fillType = PathFillType.EvenOdd
                        }
                        drawPath(frame, Color.Black)
                    } else {
                        // 边框厚度 >= 半尺寸：整张填满黑（折叠态纯黑灵动岛外观）
                        drawRoundRect(Color.Black, cornerRadius = CornerRadius(outerR, outerR))
                    }
                }
            }
        }
    }
}
