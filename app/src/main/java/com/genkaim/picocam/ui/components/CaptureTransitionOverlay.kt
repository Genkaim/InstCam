package com.genkaim.picocam.ui.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.genkaim.picocam.camera.PhotoStorage
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.genkaim.picocam.dynamic.DynamicIslandConfig
import com.genkaim.picocam.dynamic.ViewfinderConfig
import com.genkaim.picocam.ui.theme.RetroBrown
import com.genkaim.picocam.ui.theme.RetroCream
import com.genkaim.picocam.ui.theme.RetroDarkBg
import com.genkaim.picocam.ui.theme.RetroDarkSurface
import com.genkaim.picocam.ui.theme.RetroPaper
import com.genkaim.picocam.ui.theme.RetroRust
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** 灵动岛外壳颜色（与取景框外框一致）。 */
private val IslandShellColor = Color.Black

/** 深色模式下灵动岛外壳描边色（比 Color.Gray 稍深，使纯黑外壳上的描边更清晰可见）。 */
private val IslandBorderDark = Color(0xFF5A5A5A)

/** 全局背景色（与相机页 BgColor 一致）。 */
private val AppBgColor = Color(0xFFF0EDE6)

/**
 * 模拟真实打印机的位移缓动（梯形速度曲线）：先加速 → 匀速 → 减速至 0。
 * - 速度从 0 平滑升起（无起步突变），中段保持匀速，末端平滑降到 0（无骤停）。
 * - 三段占比见常量 accel/const/decel；曲线 C1 连续，总位移恒为 1。
 */
private val PrinterEasing = Easing { fraction ->
    val accel = 0.20f         // 加速段占时比（略增）
    val const = 0.70f         // 匀速段占时比
    val decel = 1f - accel - const   // = 0.18，减速段同样略增
    val v = 2f / (1f + const) // 恒定速度，使总位移 = 1
    when {
        fraction <= accel -> 0.5f * v * fraction * fraction / accel
        fraction <= accel + const -> 0.5f * v * accel + v * (fraction - accel)
        else -> {
            val u = fraction - (accel + const)
            0.5f * v * accel + v * const + v * u - 0.5f * (v / decel) * u * u
        }
    }
}

/**
 * 拍照后的过渡动画：
 * ① 取景框（方形）morph 到【设置里的灵动岛】——宽度/高度/圆角/位置均按灵动岛设置；
 * ② 随后横向变长：以设置岛中心为锚点左右对称加长到屏幕 78% 宽（高度/圆角/位置不变）→ 横向出纸插槽；
 *    动画过程中不铺全屏遮罩，界面其余元素（缩放条/控制栏/相册）保持可见、不动。
 * ③ "打印"阶段（慢、梯形速度曲线：先加速→匀速→减速至0，无速度突变）：灵动岛外壳为单一圆角矩形（四角均用预设圆角 shellCorner，始终呈现预设外观，不再拆两半）；
 *    照片为小尺寸（略小于灵动岛宽度），用 clipRect 在"出纸口"(占岛高 seamFrac 处)裁切——出纸口以上被黑色外壳遮住、以下才露出，最初看不见，随后向下"打印"出来，全部出现。出纸口位置由 seamFrac 控制（现 0.9，可改）。
 * ③ "放大"阶段：照片等比放大到详情页满屏尺寸并移到正中；期间灵动岛外壳淡出。
 * ④ 放大完成后：背景变为【全局背景色半透明 + 模糊】（由 CameraContent 对相机层做模糊 + 本组件叠加半透明背景）；
 *    照片正下方出现三个按钮（保存/分享/删除，样式遵循照片详情页：保存+分享为连通胶囊、删除为圆形）。
 *    关闭方式：点击背景处，或点击删除（删除后退出）。左上角箭头已去掉。
 */
@Composable
fun CaptureTransitionOverlay(
    file: File,
    vfConfig: ViewfinderConfig,
    diConfig: DynamicIslandConfig,
    albumSlot: Rect?,
    isDark: Boolean = false,
    photoVersion: Long = 0L,
    extractedColor: Int = 0xFFEDE0C8.toInt(),  // 莫奈取色结果（由 ViewModel 在 addPolaroidFrame 完成后提取）
    onDetailState: (Boolean) -> Unit,
    onExit: suspend (Boolean) -> Unit,
    onAddToAlbum: suspend () -> Unit,
    onViewfinderAutoOpen: () -> Unit = {},
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val screenW = configuration.screenWidthDp.toFloat()
    val screenH = configuration.screenHeightDp.toFloat()
    val densityPx = density.density

    var showDetail by remember { mutableStateOf(false) }
    var showPhoto by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }      // 仅关闭淡出阶段为 true（打印/grow/详情展示期均为 false → 照片保持可见）
    var flying by remember { mutableStateOf(false) }        // 照片飞入相册阶段为 true（此时不淡出照片，而是飞入）
    var deleted by remember { mutableStateOf(false) }      // 删除时跳过飞入动画（照片已删，不飞入相册）
    // 标记 onAddToAlbum 是否已被调用（正常动画结束 or close() 中触发）
    var addedToAlbum by remember { mutableStateOf(false) }
    // rememberUpdatedState：close() 协程中通过 .value 读取最新 albumSlot（协程启动时捕获的是旧值）
    val albumSlotRef = rememberUpdatedState(albumSlot)
    val photoVersionRef = rememberUpdatedState(photoVersion)
    // 飞入目标：在 close() 触发的瞬间快照当前 albumSlot，避免在 450ms 飞入过程中因网格重布局导致目标漂移
    var flySlot by remember { mutableStateOf<Rect?>(null) }
    var photoBottomPx by remember { mutableStateOf(0f) }   // 照片底部在屏幕中的 y（px），用于把按钮放到照片正下方
    var photoTopPx by remember { mutableStateOf(0f) }      // 照片顶部 y（px），飞入动画用
    var photoLeftPx by remember { mutableStateOf(0f) }     // 照片左边 x（px），飞入动画用
    var photoWidthPx by remember { mutableStateOf(0f) }   // 照片布局宽（px），飞入缩放用
    var photoHeightPx by remember { mutableStateOf(0f) }  // 照片布局高（px），飞入用
    // 过渡层根 Box 的【实测】像素尺寸：避免用 configuration.screenHeightDp*density 推导中心/高度，
    // 因不同机型（导航栏/挖孔/显示缩放）下后者常≠真实窗口像素高，会导致照片打印起止位置算错（超出灵动岛/多走一段）。
    var overlayWpx by remember { mutableStateOf(0f) }
    var overlayHpx by remember { mutableStateOf(0f) }

    // 详情态时照片（及按钮）整体上移量（px）；grow 完成后照片中心不再居中而是略偏上
    val detailUpPx = 60f * densityPx
    // 仅关闭时照片淡出（与背景遮罩/模糊同步淡出）；飞入相册时保持不透明
    val photoAlpha by animateFloatAsState(if (closing && !flying) 0f else 1f, animationSpec = tween(250))
    // 飞入相册动画（声明在前以供 close() 引用；LaunchedEffect 中 snapTo 重置）
    val fly = remember { Animatable(0f) }

    /** 关闭：有相册占位且非删除时照片飞入相册（缩放+位移+回正），否则简单淡出。动画结束后再真正卸载。 */
    fun close() {
        if (!showDetail || closing) return
        closing = true
        showDetail = false  // scrim 淡出，露出相册照片
        onDetailState(false)  // 模糊淡出
        // 按钮通过 closing 标记隐藏（见 AnimatedVisibility 条件）
        scope.launch {
            // 预览结束后才把照片加入相册（同步刷新 + 设占位），等网格布局完成再飞入
            if (!addedToAlbum) {
                onAddToAlbum()
                addedToAlbum = true
            }
            // 等一帧让 albumSlot=null（onAddToAlbum 中重置）传播到 albumSlotRef
            // 否则轮询第一次就读到旧的非 null 值，跳过等待
            delay(16)
            // 轮询等待网格完成布局（占位 cell 的 onGloballyPositioned 测量新 albumSlot）
            var slot: Rect? = null
            var attempts = 0
            while (attempts < 30) {            // 最多等 30×16ms ≈ 480ms
                slot = albumSlotRef.value
                if (slot != null) break
                delay(16)
                attempts++
            }
            flySlot = slot                     // 快照飞入目标
            if (!deleted && slot != null && photoWidthPx > 0f) {
                flying = true
                fly.animateTo(1f, tween(450))     // 照片飞入相册占位
            } else {
                delay(300)                       // 删除或无占位：略长于淡出时长
            }
            // 平滑过渡完成后再自动展开取景框（progressState 全程不变，albumSlot 位置一致）
            if (!deleted) onViewfinderAutoOpen()
            onExit(deleted)
        }
    }

    // —— 起点：取景框当前矩形（与 CameraContent 同一套计算）——
    val fromW = vfConfig.widthDp.toFloat()
    val fromH = vfConfig.widthDp.toFloat()
    val fromLeft = (screenW - fromW) / 2f
    val halfH = screenH / 2f
    val maxOffset = (halfH - vfConfig.widthDp).coerceAtLeast(0f)
    val fromTop = (halfH / 2f + (vfConfig.posY - 0.5f) * maxOffset - fromH / 2f).coerceAtLeast(0f)
    val fromCorner = vfConfig.cornerDp.toFloat()

    // —— 灵动岛（设置态/正常折叠态）：宽度/高度/圆角/位置均按设置（diConfig）——
    val isW = diConfig.widthDp.toFloat().coerceAtLeast(20f)
    val isH = diConfig.heightDp.toFloat().coerceAtLeast(12f)
    val isCorner = diConfig.cornerDp.toFloat()
    val isCenterX = diConfig.posX * screenW
    val isCenterY = diConfig.posY * screenH
    val isLeft = isCenterX - isW / 2f
    val isRight = isLeft + isW
    val isTop = (isCenterY - isH / 2f).coerceAtLeast(4f)

    // —— 打印态（加长版）：仅【横向】变长（更宽），高度/圆角/竖向位置完全沿用设置 → 横向出纸插槽外观 ——
    // 加长后【强制居中于屏幕】：出纸插槽始终落在屏幕中线，避免非对称加长导致整体偏左/偏右；
    // 打印完成后由 shrink 把宽度过渡回设置指定的正常长度(isW)。
    // 加长后的"距左右屏幕边距离"取 min(a, b)：
    //   a = 默认加长态(占屏 defaultPillFrac)距左右边的距离 = (1 - defaultPillFrac)/2 · 屏宽；
    //   b = 正常岛(不加长)距左右边的最小值 = min(岛左距, 岛右距)；
    //   实际加长后距左右边 = min(a, b) → 岛越贴边(b 越小)加长越宽、但永不窄于默认宽度(≥ defaultPillFrac 占比)，
    //   岛足够居中(b ≥ a)时保持默认加长宽度。
    val defaultPillFrac = 0.78f
    val a = screenW * (1f - defaultPillFrac) / 2f
    val b = min(isLeft, screenW - isRight).coerceAtLeast(0f)
    val edgeDist = min(a, b)
    val pillW = (screenW - 2f * edgeDist).coerceIn(isW, screenW)
    val pillLeft = (screenW - pillW) / 2f      // 加长版强制居中：extend 期间从设置岛位置滑向屏幕中线
    val pillTop = isTop                        // 高度相同 → 顶边相同
    val pillCenterY = isCenterY

    // —— 打印态照片：略小于灵动岛宽度（岛放得下）；放大后 = 详情页满屏尺寸 ——
    val photoPrintW = pillW * 0.85f
    val photoFullW = screenW - 48f
    val scalePrint = (photoPrintW / photoFullW).coerceIn(0.2f, 1f)

    val morph = remember { Animatable(0f) }
    val extend = remember { Animatable(0f) }   // morph 完成后：横向加长（设置岛 → 打印态加长版）
    val emit = remember { Animatable(0f) }
    val grow = remember { Animatable(0f) }
    val shrink = remember { Animatable(0f) }   // 打印/grow 完成后：加长版岛 → 设置正常长度(仅宽度)的过渡

    LaunchedEffect(file) {
        morph.snapTo(0f); extend.snapTo(0f); emit.snapTo(0f); grow.snapTo(0f); shrink.snapTo(0f); fly.snapTo(0f)
        flying = false; deleted = false; showPhoto = false
        val initialVersion = photoVersionRef.value
        morph.animateTo(1f, tween(durationMillis = 380))                          // ① 取景框 → 设置里的灵动岛大小
        delay(200)                                                                // ② HOLD：正常灵动岛清晰可见
        extend.animateTo(1f, tween(durationMillis = 300))                         // ③ 横向变长（设置岛 → 加长版打印态）
        // 等 addPolaroidFrame 完成（photoVersion 变化），确保读到带白框版本；
        // 并 flooring 到固定最小间隔，使不同设备"加长→照片出现"的观感一致（不再随机型处理快慢而差）
        val gapStartNs = System.nanoTime()
        var waitAttempts = 0
        while (photoVersionRef.value == initialVersion && waitAttempts < 100) {    // 最多等 5 秒
            delay(50)
            waitAttempts++
        }
        val waitedMs = (System.nanoTime() - gapStartNs) / 1_000_000
        val MIN_APPEAR_GAP_MS = 300L   // 加长完成后到照片出现的最小间隔(ms)：两设备都至少等这么久 → 一致
        if (waitedMs < MIN_APPEAR_GAP_MS) delay(MIN_APPEAR_GAP_MS - waitedMs)
        showPhoto = true                                                          // ④ addPolaroidFrame 已完成，读到带白框版本
        emit.animateTo(1f, tween(durationMillis = 1700, easing = PrinterEasing))  // ⑤ 打印
        showDetail = true                                                         // ⑥ 背景进入模糊/预览模式
        onDetailState(true)
        grow.animateTo(1f, tween(durationMillis = 450))                         // ⑦ 照片放大（开始过渡到正中央）
        // ★ 占位时机：grow 完成（照片完全过渡到正中央）后调用 onAddToAlbum
        // 时机原因：打印动画（emit）期间相册冻结；照片刚开始放大时相册也冻结；
        // 只有照片完全到达中央后才让相册看到新文件 + 占位 cell（用户此时已被 overlay 遮挡，看不到相册变化）。
        // 这样相册从"冻结→突然看到占位"的视觉冲击最小（发生在用户视觉焦点从相册转移到照片之后）。
        onAddToAlbum()
        addedToAlbum = true
        shrink.animateTo(1f, tween(durationMillis = 320))                        // ⑧ 加长版岛过渡回正常长度
        // ⑨ 自动展开取景框在 close() 中飞入动画完成后触发（progressState 全程不变）
    }

    val lerp = { a: Float, b: Float, t: Float -> a + (b - a) * t }
    val m = morph.value        // 取景框 → 设置岛
    val x = extend.value       // 设置岛 → 打印态（横向加长）
    val e = emit.value
    val g = grow.value
    val s = shrink.value       // 打印态 → 设置岛（仅宽度）

    // 宽度：morph 把取景框收至设置宽度 isW；extend 横向加长到 pillW；shrink 把 pillW 收回到 isW
    val baseW = lerp(fromW, isW, m)
    val shellW = when {
        s > 0f -> lerp(pillW, isW, s)
        x > 0f -> lerp(isW, pillW, x)
        else -> baseW
    }
    // 高度：始终跟随 morph 到设置高度 isH（打印态不加高）
    val shellH = lerp(fromH, isH, m)
    // 顶边：morph 到设置顶边；extend/shrink 不改变（高度相同）
    val baseLeft = lerp(fromLeft, isLeft, m)
    val shellLeft = when {
        s > 0f -> lerp(pillLeft, isLeft, s)
        x > 0f -> lerp(isLeft, pillLeft, x)
        else -> baseLeft
    }
    val shellTop = lerp(fromTop, isTop, m)
    val shellCorner = lerp(fromCorner, isCorner, m)
    val shellAlpha = if (closing) 1f else if (e < 1f) 1f else (1f - g)
    // 出纸口（纸张露出位置）占岛高比例：仅决定 clip 裁切线位置；灵动岛外壳始终是完整圆角矩形，不随出纸口拆分
    val seamFrac = 0.9f
    val seamYdp = shellTop + shellH * seamFrac

    val revealLinePx = seamYdp * densityPx
    // 用实测根 Box 尺寸推导中心/高度（首帧未测得时回退 configuration 值，避免除零/跳变）
    val screenHpx = if (overlayHpx > 0f) overlayHpx else screenH * densityPx
    val screenCenterYpx = screenHpx / 2f
    // 照片布局高度（onGloballyPositioned 测得，pre-transform）；打印态按 scalePrint 缩放后的可见高度
    val photoLaidHpx = max(photoBottomPx - screenCenterYpx, 0f) * 2f
    val photoScaledHpx = photoLaidHpx * scalePrint
    // 打印起点：照片底边刚落在出纸口（整张在上方、被裁切）→ 一进入打印就立刻冒头（无延迟）；
    // 打印终点：照片顶边到达出纸口（整张在下方、完全露出）→ 打印完成，不再继续下移，直接 grow。
    val tyStart = revealLinePx - screenCenterYpx - photoScaledHpx / 2f
    val tyEnd = revealLinePx - screenCenterYpx + photoScaledHpx / 2f

    val scale = if (e < 1f) scalePrint else lerp(scalePrint, 0.92f, g)   // 详情态照片比满屏略小一点
    val ty = if (e < 1f) lerp(tyStart, tyEnd, e) else lerp(tyEnd, -detailUpPx, g)
    val photoTilt = lerp(0f, 3.5f, g)   // 详情态照片略微倾斜（度）

    // 飞入相册动画参数（fly=0 时无效；fly>0 时覆盖 scale/translation/rotation 将照片送往相册占位）
    val flyVal = fly.value
    val photoLayoutCx = photoLeftPx + photoWidthPx / 2f
    val photoLayoutCy = photoTopPx + photoHeightPx / 2f
    // 使用 close() 快照的 flySlot 作为落点，保证飞入全程目标固定（不受网格重布局影响）
    val flyTargetScale = if (photoWidthPx > 0f) (flySlot?.let { (it.width / photoWidthPx).coerceIn(0.05f, 1f) } ?: 0.2f) else 0.2f
    val flyTargetCx = flySlot?.center?.x ?: photoLayoutCx
    val flyTargetCy = flySlot?.center?.y ?: photoLayoutCy

    val toPx = { v: Float -> (v * densityPx).roundToInt() }
    // 底部按钮底色：深色模式用灰色，浅色模式沿用奶油白
    val capsuleBg = if (isDark) RetroDarkSurface else RetroCream.copy(alpha = 0.85f)
    val btnIconColor = if (isDark) Color.White else RetroBrown
    val btnDelBg = if (isDark) RetroDarkSurface else RetroRust.copy(alpha = 0.15f)
    val btnDelIcon = if (isDark) Color.White else RetroRust
    // 莫奈取色背景：直接使用 ViewModel 在 addPolaroidFrame 完成后提取的颜色（同一 IO 协程，无时序问题）
    val monetColor by animateColorAsState(
        targetValue = Color(extractedColor),
        animationSpec = tween(durationMillis = 400, easing = LinearEasing),
        label = "monetBg",
    )
    // 详情背景：浅色模式 = 莫奈色（90% 不透明）；深色模式 = 深灰底，莫奈色淡显后再叠半透明灰遮罩 → 整体呈深灰
    val scrimColor = if (isDark) Color(0xFF242424).copy(alpha = 0.92f) else monetColor.copy(alpha = 0.9f)
    // 按钮行高度（px）：56 按钮 + 16 下边距
    val btnRowHpx = (56 + 16) * densityPx

    Box(Modifier.fillMaxSize()
        .onSizeChanged { overlayWpx = it.width.toFloat(); overlayHpx = it.height.toFloat() }
        .background(Color.Transparent)  // 保持透明：过渡期间相册透出可见（showDetail=true 时 scrim 内部会铺 bgColor 遮挡相册）
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }) {
        // 模糊背景层已完全移除（用户要求：完全去除预览照片时的背景）

        // 半透明遮罩（scrim）；点击背景处关闭
        // 取色逻辑与 PhotoViewerActivity 完全一致（同函数 + 同 RetroPaper fallback + getLightMutedColor）。
        // 浅色模式：背景 = monetColor.copy(alpha = 0.9f)，与详情页相同取色但 90% 不透明（用户要求）
        // 深色模式：底色为深灰（RetroDarkBg），叠莫奈色淡显 + 灰遮罩 → 与详情页 dark 模式结构一致
        AnimatedVisibility(
            visible = showDetail,
            enter = fadeIn(tween(400)),
            exit = fadeOut(tween(400)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(if (isDark) Color(0xFF242424) else monetColor)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { close() },
            ) {
                if (isDark) {
                    // 深色：与 PhotoViewerActivity 完全一致——底色 RetroDarkBg + 莫奈色淡显 + 灰遮罩（移除多余的 scrimColor 层）
                    Box(Modifier.fillMaxSize().background(monetColor.copy(alpha = 0.4f)))
                    Box(Modifier.fillMaxSize().background(Color(0xFF1A1A1A).copy(alpha = 0.6f)))
                }
                // 浅色：底色已经是莫奈 0.9，不再叠任何层（避免被稀释）
            }
        }

        // 灵动岛外壳：单一圆角矩形（四角均用预设圆角 shellCorner），作为"打印机机身"始终呈现预设外观；
        // 纸张由下方 clipRect 裁切只在出纸口(seam)以下露出，出纸口以上被本黑色外壳遮住——即"用黑色元素遮住纸张"
        if (shellAlpha > 0.01f) {
            Box(
                Modifier
                    .offset { IntOffset(toPx(shellLeft), toPx(shellTop)) }
                    .size(shellW.dp, shellH.dp)
                    .clip(RoundedCornerShape(shellCorner.dp))
                    .background(IslandShellColor)
                    .then(if (isDark) Modifier.border(1.dp, IslandBorderDark, RoundedCornerShape(shellCorner.dp)) else Modifier)
                    .graphicsLayer { alpha = shellAlpha },
            )
        }

        // 照片图层：仅在 showPhoto = true（Extend 完成后）才渲染，确保读到带白框版本
        Box(
            Modifier
                .fillMaxSize()
                .then(if (e < 1f) Modifier.drawWithContent { clipRect(top = revealLinePx) { this@drawWithContent.drawContent() } } else Modifier),
        ) {
            if (showPhoto) {
                // 读文件实际尺寸算 aspectRatio，避免 AsyncImage 无高度约束时比例错误
                val photoRatio = remember(file, photoVersion) {
                    try {
                        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(file.path, o)
                        if (o.outWidth > 0 && o.outHeight > 0) o.outWidth.toFloat() / o.outHeight.toFloat() else 0.8f
                    } catch (_: Exception) { 0.8f }
                }
                AsyncImage(
                    model = remember(file, photoVersion) {
                        ImageRequest.Builder(context)
                            .data(file)
                            .memoryCacheKey("overlay_${file.path}_$photoVersion")
                            .diskCacheKey("overlay_${file.path}_$photoVersion")
                            .build()
                    },
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(24.dp)
                        .aspectRatio(photoRatio)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                        .onGloballyPositioned { coords ->
                            val topLeft = coords.localToRoot(Offset.Zero)
                            photoLeftPx = topLeft.x
                            photoTopPx = topLeft.y
                            photoWidthPx = coords.size.width.toFloat()
                            photoHeightPx = coords.size.height.toFloat()
                            photoBottomPx = topLeft.y + coords.size.height
                        }
                        .graphicsLayer {
                            if (flyVal > 0f) {
                                scaleX = lerp(0.92f, flyTargetScale, flyVal)
                                scaleY = lerp(0.92f, flyTargetScale, flyVal)
                                translationX = lerp(0f, flyTargetCx - photoLayoutCx, flyVal)
                                translationY = lerp(-detailUpPx, flyTargetCy - photoLayoutCy, flyVal)
                                rotationZ = lerp(3.5f, 0f, flyVal)
                                alpha = 1f
                            } else {
                                scaleX = scale
                                scaleY = scale
                                translationY = ty
                                rotationZ = photoTilt
                                alpha = (if (e > 0.001f || g > 0.001f) 1f else 0f) * photoAlpha
                            }
                        },
                )
            }
        }

        // 顶部常驻灵动岛（详情态）：仅在 grow 进行中/完成后渲染（g>0），杜绝 morph/extend/emit 期间可见。
        // grow 期间随 g 在"加长版"打印位置淡入（与打印机机身岛无缝衔接），随后 shrink(0→1) 把宽度过渡回设置正常长度(isW)。
        // closing 时也立即隐藏（否则 close() 期间屏幕顶部仍显示黑色小岛直到 overlay 卸载）。
        if (g > 0.001f && !closing) {
            val piW = lerp(pillW, isW, s)
            val piH = isH
            val piLeft = lerp(pillLeft, isLeft, s)
            val piTop = isTop
            val piCorner = isCorner
            Box(
                Modifier
                    .offset { IntOffset(toPx(piLeft), toPx(piTop)) }
                    .size(piW.dp, piH.dp)
                    .clip(RoundedCornerShape(piCorner.dp))
                    .background(IslandShellColor)
                    .then(if (isDark) Modifier.border(1.dp, IslandBorderDark, RoundedCornerShape(piCorner.dp)) else Modifier)
                    .graphicsLayer { alpha = g },
            )
        }

        // 放大完成后：照片正下方的三个按钮（保存/分享/删除）；closing 时立即隐藏，背景保持
        AnimatedVisibility(
            visible = showDetail && !closing,
            enter = fadeIn(tween(250)) + slideInVertically(animationSpec = tween(250), initialOffsetY = { it / 2 }),
            exit = fadeOut(tween(250)) + slideOutVertically(animationSpec = tween(250), targetOffsetY = { it / 2 }),
            modifier = Modifier.fillMaxWidth(),
        ) {
            val desiredY = photoBottomPx + 12f * densityPx - detailUpPx
            val clampedY = min(desiredY, screenHpx - btnRowHpx - 8f * densityPx)
            Row(
                Modifier
                    .offset { IntOffset(0, clampedY.toInt()) }
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },   // 按钮行整个区域 consume，避免点到背景误关
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 保存（左胶囊：仅左圆角大）
                Row(
                    Modifier
                        .height(56.dp).width(128.dp)
                        .clip(RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp, topEnd = 6.dp, bottomEnd = 6.dp))
                        .background(capsuleBg)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            scope.launch {
                                saveToGallery(context, file)
                                Toast.makeText(context, "已保存到系统相册", Toast.LENGTH_SHORT).show()
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(22.dp), tint = btnIconColor)
                    Text("保存", color = btnIconColor, fontSize = 13.sp, modifier = Modifier.padding(start = 6.dp))
                }

                Spacer(Modifier.width(2.dp))

                // 分享（右胶囊：仅右圆角大）
                Row(
                    Modifier
                        .height(56.dp).width(128.dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 28.dp, bottomEnd = 28.dp))
                        .background(capsuleBg)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { scope.launch { shareImage(context, file) } },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(22.dp), tint = btnIconColor)
                    Text("分享", color = btnIconColor, fontSize = 13.sp, modifier = Modifier.padding(start = 6.dp))
                }

                Spacer(Modifier.width(16.dp))

                // 删除（圆形）
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                    .background(btnDelBg)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { if (file.delete()) { deleted = true; close() } },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除", tint = btnDelIcon)
                }
            }
        }
    }
}

private suspend fun saveToGallery(context: Context, file: File) {
    withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext
            context.contentResolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { inp -> inp.copyTo(out) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            }
        } catch (_: Exception) {}
    }
}

/** 分享「去掉位置信息」的副本，保护隐私（见 PhotoViewerActivity.shareImage）。 */
private suspend fun shareImage(context: Context, file: File) {
    val shareFile = withContext(Dispatchers.IO) { PhotoStorage.copyWithoutLocation(context, file) }
    try {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, shareFile)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, "分享照片").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "分享失败：${e.localizedMessage ?: "未知错误"}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * 从照片解码小图并用 Palette 取主色（逻辑同 PhotoViewerActivity.extractDominantColor）。
 * 先 inJustDecodeBounds 读尺寸、按目标(~256px)算 inSampleSize，再解码小图 → Palette，控制耗时；
 * 返回 LightMuted 主色（柔和不刺眼），失败回退 fallback。
 */
private suspend fun extractDominantColor(file: File, fallback: Int): Int = withContext(Dispatchers.IO) {
    try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        val target = 256
        var sample = 1
        var w = bounds.outWidth; var h = bounds.outHeight
        while (maxOf(w, h) / sample > target * 2) { sample *= 2 }   // 留 2x 余量给 Palette 边缘色
        val opts = BitmapFactory.Options().apply { inSampleSize = sample; inJustDecodeBounds = false }
        val bm = BitmapFactory.decodeFile(file.path, opts) ?: return@withContext fallback
        val palette = Palette.from(bm).generate()
        bm.recycle()
        // 取色策略：getLightVibrantColor 为主（取亮色主调），getDominantColor 为 fallback（屏幕照片等无 light vibrant swatch 时退到最显著色）
        // 之前用 getDominantColor 对夕阳照片会取到暗色建筑轮廓（dominant = 最频繁色 = 建筑剪影）→ 背景变深棕
        // getLightVibrantColor 优先取亮色（夕阳天空、屏幕白底等），对各种照片都能取到反映主题的亮色
        palette.getLightVibrantColor(palette.getDominantColor(fallback))
    } catch (_: Exception) { fallback }
}

/** 去色：将颜色向等亮度灰度混合 factor 比例，得到柔和的"莫奈"低饱和背景色。 */
private fun desaturate(argb: Int, factor: Float): Int {
    val r = AndroidColor.red(argb); val g = AndroidColor.green(argb); val b = AndroidColor.blue(argb)
    val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    val nr = (r + (gray - r) * factor).toInt().coerceIn(0, 255)
    val ng = (g + (gray - g) * factor).toInt().coerceIn(0, 255)
    val nb = (b + (gray - b) * factor).toInt().coerceIn(0, 255)
    return AndroidColor.argb(255, nr, ng, nb)
}
