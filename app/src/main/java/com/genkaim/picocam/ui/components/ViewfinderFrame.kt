package com.genkaim.picocam.ui.components

import android.os.Build
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.genkaim.picocam.camera.EffectiveFilter
import com.genkaim.picocam.camera.buildFilterColorMatrix

@Composable
fun ViewfinderFrame(
    controller: LifecycleCameraController,
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

    // 边框用「draw 层 border」而非「layout 层 padding」：
    // · padding 会把内层 AndroidView 缩小 —— 折叠态（小岛）时高度直接被吃成 0，
    //   PreviewView 的 Surface 为 0，cameraX 永远不会 STREAMING，焦段/手动参数失效。
    // · border 只在绘制层覆盖外缘，AndroidView 始终占满外框 → 任何 progress 下都能 STREAMING。
    // 视觉上：外框=圆角矩形，背景=纯黑；AndroidView=填满；border 在 AndroidView 之上盖掉边缘，形成"取景框"观感。
    Box(
        modifier
            .clip(RoundedCornerShape(cornerDp.dp))
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    this.controller = controller
                    controller.bindToLifecycle(lifecycleOwner)
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
            modifier = Modifier.fillMaxSize(),
            update = { previewView.value = it },
        )
        if (vignette > 0f) {
            Canvas(Modifier.fillMaxSize()) {
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
        if (flashAlpha > 0f) Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = flashAlpha)))
        // 取景框黑边：画在 AndroidView 之上，盖住外缘。
        // borderDp 可由调用方传 0（让 AndroidView 完全露出）或正值（露出取景框带边框）。
        // 折叠态下 borderDp 较大，会把整张小岛都覆盖住 → 呈现纯黑灵动岛外观。
        if (borderDp > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .border(borderDp.dp, Color.Black, RoundedCornerShape(cornerDp.dp)),
            )
        }
    }
}
