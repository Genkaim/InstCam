package com.genkaim.picocam.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt
import kotlin.math.roundToInt
import com.genkaim.picocam.CameraViewModel
import com.genkaim.picocam.TintState
import com.genkaim.picocam.ui.theme.RetroBrown
import com.genkaim.picocam.ui.theme.RetroDarkBg
import com.genkaim.picocam.ui.theme.RetroDarkSurface
import com.genkaim.picocam.ui.theme.RetroRust
import com.genkaim.picocam.ui.theme.RetroSoftRed

/** 横滑调节映射：整钮宽的该比例对应的位移即达到满量程(0~1)，值越小手指行程越短。 */
private const val DRAG_FULL_FRACTION = 0.7f

/** 调色盘网格：点间距(GRID_SPACING_DP)与边缘留白(GRID_MARGIN_DP)恒定为设计基准，
 *  调色盘随多屏适配缩放时按尺寸重算点数 → 点密度(间距)不变。
 *  原点 164dp 配 11×11 点：绘制区 = 164-2*22 = 120dp，间距 = 120/(11-1) = 12dp。 */
private val GRID_MARGIN_DP = 22.dp
private val GRID_SPACING_DP = 12.dp
private const val MIN_GRID_DOTS = 3
private const val MAX_GRID_DOTS = 25

@Composable
fun EffectsPanel(
    visible: Boolean,
    vm: CameraViewModel,
    isDark: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val tintState = vm.tintState
    // 右侧仅保留滑块型滤镜（黑白 / 暗角 / 亮度）；暖色/冷色改为左侧调色盘下方的切换控件
    val rightFilterKeys = listOf("黑白", "暗角", "亮度")

    // 深色模式色板：背景深灰、控件底色、滑块轨道、进度填充等
    val panelBg = if (isDark) RetroDarkBg else Color(0xFFF0EDE6)
    val ctrlBg = if (isDark) RetroDarkSurface else Color(0xFFE0DCD4)
    val trackInactive = if (isDark) Color(0xFF3A3A3A) else Color(0xFFE0DCD4)
    val trackActive = if (isDark) Color(0xFFCFCFCF) else RetroRust

    // 性能修复：不再用 BoxWithConstraints。展开动画（expandVertically）在部分 Compose 版本中
    // 会逐帧改变约束，而 BoxWithConstraints 会在约束变化时重新执行整个内容 lambda，
    // 导致每帧重新组合整棵子树（ColorSquare / LazyColumn / 多个带动画 State 的按钮），引发掉帧。
    // 改用稳定尺寸来源：屏宽取自 LocalConfiguration（与展开动画无关），squareSide 取自固定面板高度常量，
    // 使面板内容只组合一次，展开/收起仅由 AnimatedVisibility 的 graphics layer 处理。
    val screenW = LocalConfiguration.current.screenWidthDp
    val controlBtnDiameter = 44
    val controlInset = ((screenW - controlBtnDiameter * 5) / 6 - 12).coerceAtLeast(0).dp
    // 控制栏按钮外缘到屏幕边的距离（= “设置”钮左缘 / “X”钮右缘，= (屏宽-220)/6）。
    // 背景层铺满整屏，但调色盘左缘与滤镜列表右缘按此对齐，保持与上一版一致的按钮对齐
    val edgeInset = controlInset + 12.dp
    // 调色盘靠右对齐后，右缘距屏幕中线留出的边距（与下方左半区 Box 的 padding(end) 共用，
    // 保证满宽时调色盘左缘仍对齐"设置"钮、右缘距中线恰好此值）
    val paletteRightPad = 12.dp
    // 面板可用高度（上下约束基准）：EffectsPanel 为 fillMaxSize 铺在相册 weight 区，容器高即预算。
    // 首帧尚未测得时用屏幕高兜底（仅首帧；且横向约束通常更紧，不会闪）。
    val screenH = LocalConfiguration.current.screenHeightDp
    val density = LocalDensity.current
    var panelHPx by remember { mutableStateOf(0) }
    val panelHDp = if (panelHPx > 0) (panelHPx / density.density).dp else screenH.dp
    val sliderHeight = 30.dp   // 强度滑块高度（改矮，为上方调色盘让出纵向空间；leftFixedH 预算随之减小→squareSide 在高度受限机型上增大）
    // ===== 调色盘边长 squareSide（同时作为滑块长、切换滤镜区宽，三者同宽）=====
    // 由两个约束共同决定，取较小者：
    // ① 左右约束：左侧对齐控制区左缘(edgeInset)，左元素不得越过屏幕中线，即 squareSide ≤ screenW/2 - edgeInset
    // ② 上下约束：左侧总高(调色盘+滑块+切换区) ≤ 面板可用高(panelHDp) 减上下留白，即 squareSide ≤ panelHDp - topPad - bottomPad - leftFixedH
    //    再统一减去 bottomLift(底部抬高)，使菜单整体上移一点（在任何屏宽/屏高约束下都生效）
    val paletteSliderGap = 6.dp        // 调色盘与强度滑块间距
    val sliderFilterGap = 8.dp         // 强度滑块与切换滤镜区间距
    val tintSelH = 48.dp               // 切换滤镜区(TintSelector)高度（含其上下 padding），用于上下约束预算
    val restoreBtnH = 44.dp            // 右侧"还原"按钮高度（FilterBtn 固定 44dp）
    val listRestoreGap = 6.dp          // 右侧参数列表与还原按钮间距
    val topPad = 12.dp                 // 面板内容距顶留白（与 Row 的 padding(top=12.dp) 一致）
    val bottomPad = 8.dp               // 面板内容距底留白
    val bottomLift = 23.dp             // 效果菜单整体从底部抬高一点（内容上移、底留白增大）
    // 左侧固定高度（不含调色盘本身）：滑块 + 两段间距 + 切换滤镜区
    val leftFixedH = paletteSliderGap + sliderHeight + sliderFilterGap + tintSelH
    // 左右 / 上下 两条约束各自的允许最大值，取较小者；再夹到 [120,260] 保证可用性
    // 左右约束：左侧对齐控制区左缘(edgeInset)，调色盘靠右对齐且右缘距中线留 paletteRightPad，
    // 即 squareSide ≤ 屏宽/2 - edgeInset - paletteRightPad
    val maxByWidth = (screenW.dp / 2f) - edgeInset - paletteRightPad
    val maxByHeight = panelHDp - topPad - bottomPad - bottomLift - leftFixedH
    val squareSide = minOf(maxByWidth, maxByHeight).coerceIn(120.dp, 260.dp)
    // 左侧总高（调色盘+滑块+切换区）；右侧参数列表高度据此反推，使左右总高一致
    val leftTotalH = squareSide + leftFixedH
    val listBoxH = (leftTotalH - listRestoreGap - restoreBtnH).coerceAtLeast(0.dp)
    // 调色盘与滤镜列表之间的实心间隔（连通块内部，保持整块感又有呼吸空间）。
    // 调色盘出现/不出现时列表位置与宽度均以此为准，保证两态排版一致
    val paletteListGap = 16.dp
    // 底部羽化高度（透明→半透背景色→不透明背景色），单一全宽元素，只负责底部这一区域；
    // 末端为不透明背景色，与左渐变叠在左下角时不会乘法发白
    val bottomFadeHeight = 56.dp
    // 右侧参数区固定宽度 = 屏宽 50% - edgeInset(控制区 x 钮右缘到屏右缘距离) - 间隔(paletteListGap)。
    // 配合主 Row 的 padding(end = edgeInset)，使参数列表 & 还原按钮右缘恰好对齐控制区右侧 x 钮。
    // 列表左缘与左侧调色盘右缘之间天然留出 paletteListGap 间距。
    val rightWidth = (screenW.dp / 2f - edgeInset - paletteListGap).coerceAtLeast(60.dp)

    // 横滑调节示意（弹窗）：记录各滤镜按钮位置/宽度，拖拽时在面板层叠加"同形按钮"示意（避免被列表裁剪）
    // 这些 map 改用普通可变容器（不再 mutableStateOf/StateMap），原因：
    // ① 它们只在 onGloballyPositioned 时被写入，整张表的 State 化会让 EffectsPanel 在每次子布局变化时都重订阅、成为滑动掉帧主要嫌疑；
    // ② 读取它们的只有 Modifier.offset { } lambda（draw 阶段、每帧跑、不需要响应式）。
    // dragHintName 仍保留为 State，仅用它驱动 AnimatedVisibility 显隐。
    val panelCoordRef = remember { object { var v: LayoutCoordinates? = null } }
    val btnPositions = remember { mutableMapOf<String, Offset>() }
    val btnWidths = remember { mutableMapOf<String, Dp>() }
    var dragHintName by remember { mutableStateOf<String?>(null) }
    // 退出动画期间沿用最后一次定位，避免 dragHintName 置空后偏移算成 (0,0) 导致弹窗瞬移、退出动画看不见
    var lastBtnPos by remember { mutableStateOf(Offset.Zero) }
    var lastPanelPos by remember { mutableStateOf(Offset.Zero) }
    // 滤镜切换(TintSelector)横滑示意：拖拽时在切换钮上方弹出示意气泡
    var tintDragging by remember { mutableStateOf(false) }
    var tintBtnPos by remember { mutableStateOf(Offset.Zero) }
    var tintBtnWidth by remember { mutableStateOf(0.dp) }

    Box(modifier.onGloballyPositioned { panelCoordRef.v = it }) {
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.fillMaxWidth(),
            enter = fadeIn(animationSpec = tween(120, easing = EaseOut)),
            exit = fadeOut(animationSpec = tween(200)),
        ) {
            Box(
                Modifier.fillMaxSize().onSizeChanged { panelHPx = it.height },
            ) {
                // ===== 背景：单一图层（Layer1），永远只有一层 —— 杜绝左右背景接缝、右背景盖住左渐变 =====
                // 有调色盘：整块实色。
                // 无调色盘：左透明→右实色横向渐变（iOS 相册风）；渐变在 leftBgWidth 处到达实色、之后保持实色到最右，左右无缝。
                // 关键：不要把“整块实色”与“左渐变”做成两层叠加——实色底上叠 Transparent→bg 渐变会看不见（底已全是 bg），
                // 正确做法是让“背景本身”就是这层渐变，一层到底。
                val leftBgWidth = screenW.dp / 2f
                val leftFrac = (leftBgWidth / screenW.dp).coerceIn(0f, 1f)
                val aTransEnd = (leftFrac * 0.65f).coerceAtLeast(0.26f)
                val aSolid = leftFrac.coerceAtLeast(aTransEnd + 0.001f)
                // 背景分两层（互不干扰）：
                // ① 基础层（全宽、常驻）：左透明→右实色横向渐变（无调色盘时的 iOS 相册风背景）。
                //    右侧滤镜区永远由这层提供实色背景，调色盘出入时它纹丝不动 → 右侧滤镜背景不受影响。
                // ② 调色盘背景层（仅覆盖左侧 leftBgWidth 区域）：实色 0xFFF0EDE6，alpha=paletteBgProgress，
                //    由 animateFloatAsState 驱动（与调色盘淡入淡出同为 90ms）。调色盘出现/消失时只在左侧淡入淡出，
                //    右侧滤镜区背景恒定不变。
                // 调色盘常显，左背景层恒为实色（不再随选中滤镜淡入淡出）
                val paletteBgProgress = 1f
                // ① 基础层：全宽横向渐变（左透明→右实色），常驻不变
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.25f to Color.Transparent,
                                aTransEnd to panelBg.copy(alpha = 0.45f),
                                aSolid to panelBg,
                                1f to panelBg,
                            ),
                        ),
                    ),
                )
                // ② 调色盘背景层：仅左侧 leftBgWidth 区域，随调色盘同步淡入淡出（不影响右侧）
                Box(
                    Modifier.align(Alignment.TopStart).width(leftBgWidth).fillMaxHeight()
                        .background(panelBg.copy(alpha = paletteBgProgress)),
                )
                // ── 底部渐变（Layer3）：透明 → 半透背景色 → 不透明背景色，只负责底部 bottomFadeHeight 一段。
                //    末端为不透明背景色（非透明），与左渐变叠在左下角时不会发白，过渡自然
                Box(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(bottomFadeHeight)
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to Color.Transparent,
                                    0.5f to panelBg.copy(alpha = 0.55f),
                                    1f to panelBg,
                                ),
                            )
                        ),
                )
                // 右缘留出 edgeInset：右侧参数列表/还原按钮右缘对齐控制区右侧 x 钮；
                // 底部留出 bottomLift：实现"菜单抬升"（替代从 squareSide 扣减，避免靠右对齐时左缘偏出"设置"钮）
                Row(Modifier.fillMaxSize().padding(top = 12.dp, end = edgeInset, bottom = bottomLift)) {
                    // ═══ 左侧：调色盘 + 强度滑块 + 滤镜切换（占左半区，靠右对齐）═══
                    val tintActive = tintState != TintState.NONE
                    val tintParams = when (tintState) {
                        TintState.WARM -> vm.filters["暖色"]
                        TintState.COOL -> vm.filters["冷色"]
                        TintState.NONE -> null
                    }
                    // 左半区固定宽 = 屏宽 50%；调色盘靠右对齐（TopEnd → 右缘贴中线），宽受限时 squareSide=屏宽/2-edgeInset → 左缘恰对齐"设置"钮左缘
                    Box(Modifier.width(screenW.dp / 2f).fillMaxHeight().padding(end = paletteRightPad)) {
                        Column(
                            Modifier.align(Alignment.TopEnd).width(squareSide),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top,
                    ) {
                        // 横滑拖动 TintSelector 时，调色盘+滑块整体透明度降低（tintDragging→0.35）
                        val paletteAlpha by animateFloatAsState(
                            targetValue = if (tintDragging) 0.35f else 1f,
                            animationSpec = tween(200),
                            label = "paletteAlpha",
                        )
                        Box(Modifier.alpha(paletteAlpha)) {
                            // 调色盘 + 强度滑块：用 AnimatedContent 实现滤镜切换时的半透明交叉过渡（fadeIn/fadeOut）
                            AnimatedContent(
                                targetState = tintState,
                                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
                                label = "paletteFade",
                            ) { state ->
                                val active = state != TintState.NONE
                                val p = when (state) {
                                    TintState.WARM -> vm.filters["暖色"]
                                    TintState.COOL -> vm.filters["冷色"]
                                    TintState.NONE -> null
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    ColorSquare(
                                        dotX = p?.saturation ?: 0.5f,
                                        dotY = p?.brightness ?: 0.5f,
                                        onDotChange = { x, y -> vm.adjustTint { copy(saturation = x, brightness = y) } },
                                        warm = state == TintState.WARM,
                                        cool = state == TintState.COOL,
                                        enabled = active,
                                        isDark = isDark,
                                        modifier = Modifier.size(squareSide),
                                    )
                                    Spacer(Modifier.height(paletteSliderGap))
                                    Slider(
                                        value = p?.intensity ?: 0.5f,
                                        onValueChange = { v -> vm.adjustTint { copy(intensity = v) } },
                                        enabled = active,
                                        modifier = Modifier.width(squareSide).height(sliderHeight),
                                        colors = SliderDefaults.colors(
                                            thumbColor = if (isDark) Color.White else RetroRust,
                                            activeTrackColor = if (isDark) trackActive else RetroRust,
                                            inactiveTrackColor = trackInactive,
                                            disabledThumbColor = if (isDark) Color(0xFF555555) else Color(0xFFB6A796),
                                            disabledActiveTrackColor = if (isDark) Color(0xFF444444) else Color(0xFFC9C2B5),
                                            disabledInactiveTrackColor = if (isDark) Color(0xFF333333) else Color(0xFFE0DCD4),
                                        ),
                                    )
                                    Spacer(Modifier.height(sliderFilterGap))
                                }
                            }
                        }
                        // 滤镜切换：位于调色盘下方；左右箭头 + 整体左右滑动切换三态
                        TintSelector(
                            tintState = tintState,
                            isDark = isDark,
                            squareSide = squareSide,
                            onCycle = { vm.cycleTint(it) },
                            modifier = Modifier.width(squareSide),
                            onDragHintStart = { tintDragging = true },
                            onDragHintEnd = { tintDragging = false },
                            onBtnLayout = { _, pos, w -> tintBtnPos = pos; tintBtnWidth = with(density) { w.toDp() } },
                        )
                    }

                    }

                    // 间隔：左侧调色盘与右侧参数区之间的留白
                    Spacer(Modifier.width(paletteListGap))

                    // ═══ 右侧：参数列表（右缘对齐控制区 x 钮）+ 还原，靠上排版（顶部与左侧调色盘齐平）═══
                    Box(Modifier.width(rightWidth).fillMaxHeight()) {
                        Column(
                            Modifier.fillMaxHeight().width(rightWidth).clip(RoundedCornerShape(20.dp)),
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.Top,
                            ) {
                                // 列表区高度 = 左侧总高 - (列表与还原间距 + 还原按钮高)，使右侧总高 = 左侧总高
                                Box(
                                    Modifier.fillMaxWidth().height(listBoxH)
                                        .clip(RoundedCornerShape(20.dp)),
                                ) {
                                    LazyColumn(
                                        Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(top = 0.dp, bottom = 8.dp),
                                    ) {
                                        items(rightFilterKeys.size, key = { i -> rightFilterKeys[i] }) { i ->
                                            val name = rightFilterKeys[i]
                                            val fp = vm.filters[name]!!
                                            FilterBtn(
                                                name = name,
                                                active = false,
                                                applied = fp.enabled,
                                                onSelect = {},
                                                onToggle = { vm.setEnabled(name, !fp.enabled) },
                                                sliderOnly = true,
                                                hasPalette = false,
                                                intensity = fp.intensity,
                                                onIntensityChange = { vm.updateFilterParams(name) { copy(intensity = it) } },
                                                onEnable = { vm.setEnabled(name, true) },
                                                onBtnLayout = { n, p, w -> btnPositions[n] = p; btnWidths[n] = Dp(w / density.density) },
                                                onDragHintStart = { n -> dragHintName = n },
                                                onDragHintEnd = { dragHintName = null },
                                                isDark = isDark,
                                            )
                                            Spacer(Modifier.height(8.dp))
                                        }
                                    }
                                }
                                Spacer(Modifier.height(listRestoreGap))
                                // 还原：与左侧切换按钮同高度，靠上
                                FilterBtn("还原", active = false, applied = false, showSwitch = false, onSelect = { vm.resetAll() }, onToggle = {}, isDark = isDark, textColor = RetroSoftRed)
                            }
                    }
                }
            }
        }

        // 横滑调节示意弹窗：与滤镜按钮同形同大，悬浮于对应按钮正上方，深色填满代表参数百分比
        AnimatedVisibility(
            visible = dragHintName != null,
            modifier = Modifier.offset {
                val panelPos = panelCoordRef.v?.localToRoot(Offset.Zero) ?: Offset.Zero
                val currentBtnPos = dragHintName?.let { btnPositions[it] } ?: lastBtnPos
                if (dragHintName != null) {
                    lastBtnPos = currentBtnPos
                    lastPanelPos = panelPos
                }
                val bubH = with(density) { 44.dp.toPx() }
                val gap = with(density) { 8.dp.toPx() }
                IntOffset(
                    (currentBtnPos.x - lastPanelPos.x).roundToInt(),
                    (currentBtnPos.y - lastPanelPos.y - bubH - gap).roundToInt(),
                )
            },
            enter = fadeIn(tween(150)) + slideInVertically(
                animationSpec = tween(150),
                initialOffsetY = { (it * 0.5f).toInt() }, // 从略低处上浮到位
            ),
            exit = fadeOut(tween(120)) + slideOutVertically(
                animationSpec = tween(120),
                targetOffsetY = { -(it * 0.3f).toInt() }, // 淡出时略微上浮
            ),
        ) {
            val name = dragHintName
            if (name != null) {
                SlideHintBubble(
                    intensity = vm.filters[name]?.intensity ?: 0f,
                    isDark = isDark,
                    modifier = Modifier.width(btnWidths[name] ?: 0.dp),
                )
            }
        }

        // 滤镜切换(TintSelector)横滑示意弹窗：悬浮于切换钮正上方，显示当前滤镜名与方向示意
        AnimatedVisibility(
            visible = tintDragging,
            modifier = Modifier.offset {
                val panelPos = panelCoordRef.v?.localToRoot(Offset.Zero) ?: Offset.Zero
                val currentBtnPos = tintBtnPos
                val bubH = with(density) { 44.dp.toPx() }
                val gap = with(density) { 8.dp.toPx() }
                IntOffset(
                    (currentBtnPos.x - panelPos.x).roundToInt(),
                    (currentBtnPos.y - panelPos.y - bubH - gap).roundToInt(),
                )
            },
            enter = fadeIn(tween(150)) + slideInVertically(
                animationSpec = tween(150),
                initialOffsetY = { (it * 0.5f).toInt() },
            ),
            exit = fadeOut(tween(120)) + slideOutVertically(
                animationSpec = tween(120),
                targetOffsetY = { -(it * 0.3f).toInt() },
            ),
        ) {
            TintHintBubble(
                tintState = tintState,
                isDark = isDark,
                modifier = Modifier.width(tintBtnWidth),
            )
        }
    }
}

@Composable
internal fun TintSelector(
    modifier: Modifier = Modifier,
    tintState: TintState,
    isDark: Boolean = false,
    squareSide: Dp = 164.dp,
    onCycle: (Int) -> Unit,
    onDragHintStart: () -> Unit = {},
    onDragHintEnd: () -> Unit = {},
    onBtnLayout: (String, Offset, Int) -> Unit = { _, _, _ -> },
) {
    val name = when (tintState) {
        TintState.NONE -> "无滤镜"
        TintState.WARM -> "暖色"
        TintState.COOL -> "冷色"
    }
    var dragAccum by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val stepPx = with(density) { 40.dp.toPx() } // 每累积拖动约 40dp 切换一个状态，半屏(~160dp)可遍历全部三态
    val arrowColor = if (isDark) Color(0xFFCFCFCF) else Color(0xFF6B5744)
    // <> 图标用与箭头相近的柔和色（略淡半透明），不过分突出
    val hintColor = if (isDark) Color(0xFFB0B0B0) else Color(0xFF8C7A6B)
    // 名称与 <> 图标字号随调色盘尺寸缩放（以 164dp 为基准 1.0），并夹在合理区间：
    // 保证两端箭头始终为完整圆形、名称不溢出，中间区随 squareSide 自然伸缩
    val scale = (squareSide / 164.dp).coerceIn(0.72f, 1.5f)
    // 名称与 <> 图标字号随调色盘尺寸缩放（以 164dp 为基准 1.0），并夹在合理区间；
    // 上限钉到 13.sp——与右侧参数调整文字字号一致，避免大屏上滤镜名比参数文字明显更大
    val nameSize = (16f * scale).coerceAtMost(13f).sp
    Row(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .onGloballyPositioned { coords -> onBtnLayout("__TINT__", coords.localToRoot(Offset.Zero), coords.size.width) }
            // 切换控件本身无背景色（< > 箭头亦无底色，仅保留圆形点击热区），避免与面板背景叠出多余色块
            // 横滑映射：累积距离 / stepPx = 切换次数，半屏滑动可遍历全部三态，见 onHorizontalDrag
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccum = 0f; onDragHintStart() },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val old = dragAccum
                        dragAccum += dragAmount
                        // 按 stepPx 槽位变化量触发状态循环，左滑 dragAccum 负→diff 负
                        val oldSlot = (old / stepPx).toInt()
                        val newSlot = (dragAccum / stepPx).toInt()
                        val diff = newSlot - oldSlot
                        if (diff > 0) repeat(diff) { onCycle(-1) }
                        else if (diff < 0) repeat(-diff) { onCycle(1) }
                    },
                    onDragEnd = { onDragHintEnd() },
                    onDragCancel = { onDragHintEnd() },
                )
            }
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 左箭头：始终贴左、完整圆形（无底色）
        TintArrow(isLeft = true, color = arrowColor, onClick = { onCycle(-1) })
        // 中间区域占满剩余宽度（weight(1f)），名称与 <> 图标居中；字号随调色盘尺寸动态调整
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(name, color = if (isDark) Color.White else RetroBrown, fontSize = nameSize, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 1)
            Spacer(Modifier.width((2 * scale).dp))
            SlideHintIcon(color = hintColor, scale = scale)
        }
        // 右箭头：始终贴右、完整圆形（无底色）
        TintArrow(isLeft = false, color = arrowColor, onClick = { onCycle(1) })
    }
}

@Composable
internal fun TintArrow(isLeft: Boolean, color: Color, onClick: () -> Unit) {
    // 加大点击热区与图标尺寸（需求⑨）
    Box(
        Modifier
            .width(30.dp)
            .height(48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(24.dp, 24.dp)) {
            val s = 3.dp.toPx()
            val cy = size.height / 2f
            if (isLeft) {
                drawLine(color, Offset(size.width * 0.62f, cy - size.height * 0.32f), Offset(size.width * 0.38f, cy), strokeWidth = s, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.38f, cy), Offset(size.width * 0.62f, cy + size.height * 0.32f), strokeWidth = s, cap = StrokeCap.Round)
            } else {
                drawLine(color, Offset(size.width * 0.38f, cy - size.height * 0.32f), Offset(size.width * 0.62f, cy), strokeWidth = s, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.62f, cy), Offset(size.width * 0.38f, cy + size.height * 0.32f), strokeWidth = s, cap = StrokeCap.Round)
            }
        }
    }
}

/** 滤镜切换(TintSelector)横滑示意弹窗：显示当前滤镜名与 < > 方向提示。 */
@Composable
internal fun TintHintBubble(
    tintState: TintState,
    isDark: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val label = when (tintState) {
        TintState.NONE -> "无滤镜"
        TintState.WARM -> "暖色"
        TintState.COOL -> "冷色"
    }
    Box(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (isDark) RetroDarkSurface else Color(0xFFE0DCD4)),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text("< ", color = if (isDark) Color(0xFFCFCFCF) else Color(0xFF6B5744), fontSize = 13.sp)
            Text(label, color = if (isDark) Color.White else RetroBrown, fontSize = 13.sp)
            Text(" >", color = if (isDark) Color(0xFFCFCFCF) else Color(0xFF6B5744), fontSize = 13.sp)
        }
    }
}

@Composable
internal fun ColorSquare(
    dotX: Float, dotY: Float,
    onDotChange: (Float, Float) -> Unit,
    onDotChangeFinished: () -> Unit = {},
    warm: Boolean = false,
    cool: Boolean = false,
    enabled: Boolean = true,
    isDark: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var squareSize by remember { mutableFloatStateOf(0f) }
    var dotPressed by remember { mutableStateOf(false) }       // 按压大白点时放大
    val density = LocalDensity.current
    // 按当前调色盘实测尺寸(px)计算网格点数：绘制区 = size - 2*留白，点数 = 绘制区/间距 + 1。
    // 间距(GRID_SPACING_DP)恒定 ⇒ 调色盘缩放时点密度不变。
    fun gridCount(sizePx: Float): Int {
        val mp = with(density) { GRID_MARGIN_DP.toPx() }
        val sp = with(density) { GRID_SPACING_DP.toPx() }
        val draw = (sizePx - mp * 2f).coerceAtLeast(sp)
        // 取奇数个点，使大点(选中点)初始能落在正中心网格（需求⑨）
        val n = ((draw / sp).roundToInt() + 1).coerceIn(MIN_GRID_DOTS, MAX_GRID_DOTS)
        return if (n % 2 == 0) n + 1 else n
    }
    // 大点外圈半径：拖拽/显示范围仅保留这一圈边距，使大点可触达网格(留白22dp)的全部位置，且始终完整可见
    val dotEdgeInsetPx = with(density) { 11.dp.toPx() }
    // 松手后吸附位置（仅视觉，参数不变）：按压/拖动时跟随手指自由移动（无动画滞后）；松手后平滑吸附到最近网格点
    val snapX by animateFloatAsState(
        targetValue = if (dotPressed) dotX else if (squareSize <= 0f) {
            // 首帧尚未测到尺寸时直接用真实参数，避免吸附算法用 squareSize=0 算出离谱初值（如 -22）后滑入
            dotX
        } else {
            val m = with(density) { GRID_MARGIN_DP.toPx() }
            val c = gridCount(squareSize).coerceAtLeast(2)
            val st = (squareSize - m * 2) / (c - 1).toFloat().coerceAtLeast(1f)
            val dpx = dotEdgeInsetPx + dotX * (squareSize - dotEdgeInsetPx * 2f)
            val nc = ((dpx - m) / st).roundToInt().coerceIn(0, c - 1)
            ((m + nc * st) - dotEdgeInsetPx) / (squareSize - dotEdgeInsetPx * 2f).coerceAtLeast(1f)
        },
        animationSpec = tween(160), label = "snapX",
    )
    val snapY by animateFloatAsState(
        targetValue = if (dotPressed) dotY else if (squareSize <= 0f) {
            dotY
        } else {
            val m = with(density) { GRID_MARGIN_DP.toPx() }
            val c = gridCount(squareSize).coerceAtLeast(2)
            val st = (squareSize - m * 2) / (c - 1).toFloat().coerceAtLeast(1f)
            val dpy = dotEdgeInsetPx + dotY * (squareSize - dotEdgeInsetPx * 2f)
            val nr = ((dpy - m) / st).roundToInt().coerceIn(0, c - 1)
            ((m + nr * st) - dotEdgeInsetPx) / (squareSize - dotEdgeInsetPx * 2f).coerceAtLeast(1f)
        },
        animationSpec = tween(160), label = "snapY",
    )
    // 当前显示坐标：按压/拖动 = 原始参数（跟手，无动画滞后）；松手 = 吸附位置（带动画）
    val curX = if (dotPressed) dotX else snapX
    val curY = if (dotPressed) dotY else snapY
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(34.dp))
            .then(
                // 用 Canvas 绘制二维渐变背景 + 网格点（模仿参考图效果）
                Modifier.drawBehind {
                    val w = size.width; val h = size.height
                    // ---- 二维渐变：四角插值（分层近似）----
                    // 暖色模式：左上浅白 → 左下深青绿 → 右上浅珊瑚红 → 右下暗红橙
                    // 冷色模式：左上浅白 → 左下深青蓝 → 右上浅蓝 → 右下暗靛蓝
                    // 无滤镜：纯浅灰（不可操控、无渐变），暖/冷：四角插值渐变
                    val noneGray = Color(0xFFD0D0D0)
                    val tl = if (warm) Color(0xFFD8DDD4) else if (cool) Color(0xFFD8DEE4) else noneGray
                    val bl = if (warm) Color(0xFF1A3A38) else if (cool) Color(0xFF1A2A3A) else noneGray
                    val tr = if (warm) Color(0xFFFF6A45) else if (cool) Color(0xFF5A78FF) else noneGray
                    val br = if (warm) Color(0xFFC04020) else if (cool) Color(0xFF2030A0) else noneGray

                    // 第一层：水平色调梯度（左列中值→右列中值）
                    val leftMid = Color(
                        red = (tl.red + bl.red) * 0.5f,
                        green = (tl.green + bl.green) * 0.5f,
                        blue = (tl.blue + bl.blue) * 0.5f,
                        alpha = (tl.alpha + bl.alpha) * 0.5f,
                    )
                    val rightMid = Color(
                        red = (tr.red + br.red) * 0.5f,
                        green = (tr.green + br.green) * 0.5f,
                        blue = (tr.blue + br.blue) * 0.5f,
                        alpha = (tr.alpha + br.alpha) * 0.5f,
                    )
                    drawRect(
                        brush = Brush.horizontalGradient(listOf(leftMid, rightMid)),
                        size = Size(w, h),
                    )
                    // 第二层：垂直明暗叠加（上亮→下暗），增强上下过渡；底部为干净的深蓝灰。仅暖/冷色叠加，无滤镜保持纯浅灰
                    if (warm || cool) {
                        drawRect(
                            brush = Brush.verticalGradient(listOf(
                                Color.White.copy(alpha = 0.25f),
                                Color.Transparent,
                                Color(0xFF0D1620).copy(alpha = 0.85f), // 更深、偏蓝的干净蓝灰（更沉）
                            )),
                            size = Size(w, h),
                        )
                    }

                    // ---- 网格点：按压/拖动时仅“近点变大变白”(径向)；松手时仅“整行整列变亮”(十字，无渐隐)----
                    // 点数随调色盘实测尺寸变化，间距(GRID_SPACING_DP)恒定 ⇒ 密度不变
                    val cols = gridCount(w)
                    val rows = cols
                    val dotR = 3.5f       // 网格点最小（初始）半径
                    val extraR = 7f       // 紧邻大点时额外增大的半径（大幅增强）
                    val margin = with(density) { GRID_MARGIN_DP.toPx() } // 边缘留白：网格点不靠近正方形边缘
                    val stepX = (w - margin * 2) / (cols - 1)
                    val stepY = (h - margin * 2) / (rows - 1)
                    // 大点中心像素坐标：按压/拖动 = 跟手自由位置(curX/Y)；松手 = 吸附位置
                    val dotCx = dotEdgeInsetPx + curX * (w - dotEdgeInsetPx * 2f)
                    val dotCy = dotEdgeInsetPx + curY * (h - dotEdgeInsetPx * 2f)
                    val infl = w * 0.25f      // 径向影响半径（25% 边长）：仅按压/拖动时生效
                    // 按压/拖动：仅“近点变大变白”（径向）；松手：仅“整行整列变亮”（十字，无渐隐）
                    val radialActive = if (dotPressed) 1f else 0f
                    val crossActive = !dotPressed
                    for (row in 0 until rows) {
                        for (col in 0 until cols) {
                            val cx = margin + col * stepX
                            val cy = margin + row * stepY
                            val dx = cx - dotCx
                            val dy = cy - dotCy
                            val d = sqrt(dx * dx + dy * dy)
                            val radial = (1f - d / infl).coerceIn(0f, 1f) * radialActive // 0(远/松手) ~ 1(紧贴大点)
                            // 整行整列：与白点同行或同列即整条变亮（仅松手时生效，不再渐隐）
                            val adx = if (dx < 0f) -dx else dx
                            val ady = if (dy < 0f) -dy else dy
                            val onCross = crossActive && (adx < stepX * 0.5f || ady < stepY * 0.5f)
                            val r = dotR + radial * extraR                  // 近大远小，最小 = dotR
                            val alpha = (0.4f + radial * 0.5f + if (onCross) 0.5f else 0f).coerceIn(0f, 1f)
                            drawCircle(Color(1f, 1f, 1f, alpha), radius = r, center = Offset(cx, cy))
                        }
                    }

                    // ---- 边框：轻微加深，左上透明、向右下渐变变暗（带圆角）----
                    val borderW = with(density) { 3.5.dp.toPx() }
                    val corner = with(density) { 32.25.dp.toPx() } // 外圆角34dp，内缩半个边宽1.75dp
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color(0x33000000)),
                            start = Offset(0f, 0f),
                            end = Offset(w, h),
                        ),
                        topLeft = Offset(borderW / 2, borderW / 2),
                        size = Size(w - borderW, h - borderW),
                        cornerRadius = CornerRadius(corner),
                        style = Stroke(width = borderW),
                    )
                }
            )
            .clipToBounds()
            .onSizeChanged { squareSize = it.width.toFloat() }
            .then(
                if (enabled) Modifier.pointerInput(Unit) {
                    // 迁移自已废弃的 forEachGesture { awaitPointerEventScope { ... } }：
                    // awaitEachGesture 自带 awaitPointerEventScope 作用域（去掉中间层），且官方推荐替代 forEachGesture。
                    awaitEachGesture {
                        // 手指按下立即放大（即使未移动也生效）
                        val down = awaitFirstDown(requireUnconsumed = false)
                        dotPressed = true
                        try {
                            // 拖动期间大点自由跟随手指，松手才吸附
                            drag(down.id) { change ->
                                change.consume()
                                val insetX = dotEdgeInsetPx
                                val insetY = dotEdgeInsetPx
                                val rangeX = (size.width - insetX * 2f).coerceAtLeast(1f)
                                val rangeY = (size.height - insetY * 2f).coerceAtLeast(1f)
                                val x = ((change.position.x - insetX) / rangeX).coerceIn(0f, 1f)
                                val y = ((change.position.y - insetY) / rangeY).coerceIn(0f, 1f)
                                onDotChange(x, y)
                            }
                        } finally {
                            // 松手：恢复大小、关闭径向、整行整列变亮、吸附到最近小点（视觉）
                            dotPressed = false
                            onDotChangeFinished()
                        }
                    }
                } else Modifier
            ),
    ) {
        // 取色点：纯白实心圆，按压/拖动时放大；松手吸附到最近小点（仅视觉）
        if (squareSize > 0f) {
            val baseDotSize = 12.dp
            val pressedDotSize = 20.dp
            // 按压/拖动时白点平滑变大，松开恢复
            val animDotSize by animateDpAsState(
                targetValue = if (dotPressed) pressedDotSize else baseDotSize,
                animationSpec = tween(120), label = "dotSize",
            )
            val dotRadiusPx = with(density) { (animDotSize / 2).toPx() }
            // 大点位置：用 curX/curY（按压/拖动跟随手指，松手吸附最近网格点）
            val inset = dotEdgeInsetPx
            val range = (squareSize - inset * 2f).coerceAtLeast(1f)
            val px = (inset + curX * range).toInt()
            val py = (inset + curY * range).toInt()
            Box(
                Modifier.offset { IntOffset((px - dotRadiusPx).toInt(), (py - dotRadiusPx).toInt()) }
                    .size(animDotSize).clip(CircleShape).background(Color.White)
            )
        }
    }
}

@Composable
internal fun FilterBtn(
    name: String,
    active: Boolean,
    applied: Boolean,
    onSelect: () -> Unit,
    onToggle: () -> Unit,
    showSwitch: Boolean = true,
    sliderOnly: Boolean = false,
    hasPalette: Boolean = false,
    intensity: Float = 1f,
    onIntensityChange: (Float) -> Unit = {},
    onEnable: () -> Unit = {},
    onBtnLayout: (String, Offset, Int) -> Unit = { _, _, _ -> },
    onDragHintStart: (String) -> Unit = {},
    onDragHintEnd: () -> Unit = {},
    isDark: Boolean = false,
    textColor: Color = (if (isDark) Color(0xFFCFCFCF) else Color(0xFF6B5744)),
    modifier: Modifier = Modifier,
) {
    // 滑块-only 滤镜：记录按钮宽度与拖拽起点，把横向位移映射成强度(0~1)
    var btnWidth by remember { mutableStateOf(1) }
    var dragStartIntensity by remember { mutableFloatStateOf(0f) }
    var dragAccum by remember { mutableFloatStateOf(0f) }
    // 刚结束一次拖拽的短时间内屏蔽 clickable 的点击，避免“横滑后误触发选中/取消选中”
    var justDragged by remember { mutableStateOf(false) }
    LaunchedEffect(justDragged) {
        if (justDragged) {
            delay(250)
            justDragged = false
        }
    }
    // pointerInput 闭包中捕获最新回调 / 状态，避免 stale lambda
    val onIntensityChangeState by rememberUpdatedState(onIntensityChange)
    val onEnableState by rememberUpdatedState(onEnable)
    val intensityState by rememberUpdatedState(intensity)
    val appliedState by rememberUpdatedState(applied)

    // 外层 Box：容纳按钮本体；并上报按钮在屏幕中的位置，供“横滑调节示意”弹窗定位
    Box(
        modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords -> onBtnLayout(name, coords.localToRoot(Offset.Zero), coords.size.width) },
    ) {
        // 按钮本体
        Box(
            Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(22.dp))
                .background(if (active) RetroRust else (if (isDark) RetroDarkSurface else Color(0xFFE0DCD4)))
                .onSizeChanged { btnWidth = it.width }
                .then(
                    if (sliderOnly) Modifier.pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                // 触发：记录起点强度、重置累加、弹出“横滑调节示意”
                                dragStartIntensity = intensityState
                                dragAccum = 0f
                                onDragHintStart(name)
                                // 若滤镜未启用，拖拽时自动启用，使强度变化立即在预览可见
                                if (!appliedState) onEnableState()
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                dragAccum += dragAmount
                                // 映射长度略短：整钮宽的 0.7 即对应满量程(0~1)
                                val newVal = (dragStartIntensity + dragAccum / (btnWidth.toFloat() * DRAG_FULL_FRACTION).coerceAtLeast(1f))
                                    .coerceIn(0f, 1f)
                                onIntensityChangeState(newVal)
                            },
                            onDragEnd = { onDragHintEnd(); justDragged = true },
                            onDragCancel = { onDragHintEnd(); justDragged = true },
                        )
                    } else Modifier
                )
                .clickable {
                    if (justDragged) { justDragged = false; return@clickable }
                    onSelect()
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 14.dp, end = 56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    name,
                    color = if (active) Color.White else textColor,
                    fontSize = 13.sp,
                )
                // 图标（<> 与方块）比文字淡一档：用更浅的不透明色，避免半透明圆头在箭头顶点处叠加变深
                val iconColor = if (active) Color(0xFFD9D0C4) else (if (isDark) Color(0xFFCFCFCF) else Color(0xFFB6A796))
                if (sliderOnly) {
                    Spacer(Modifier.width(4.dp))
                    SlideHintIcon(color = iconColor)
                }
                if (hasPalette) {
                    Spacer(Modifier.width(6.dp))
                    PaletteLogo(color = iconColor)
                }
            }
                if (showSwitch) {
                FilterSwitch(
                    checked = applied,
                    isDark = isDark,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp),
                )
            }
        }
    }
}

/** 滑块-only 滤镜的"滑动调整"提示图标：< > 两个箭头，圆头端帽在顶点叠出圆角；整体偏小、偏左。
 *  scale：随调色盘尺寸缩放（默认 1f，对滤镜按钮内用法无影响）。 */
@Composable
internal fun SlideHintIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF6B5744), scale: Float = 1f) {
    Canvas(modifier.size(16.dp * scale, 11.dp * scale)) {
        val w = size.width
        val h = size.height
        val cy = h / 2f
        val s = (1.4.dp * scale).toPx()
        // 左箭头 '<'（指向左）：顶点在左，两臂在右
        drawLine(color, Offset(w * 0.42f, cy - h * 0.38f), Offset(w * 0.16f, cy), strokeWidth = s, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.16f, cy), Offset(w * 0.42f, cy + h * 0.38f), strokeWidth = s, cap = StrokeCap.Round)
        // 右箭头 '>'（指向右）：顶点在右，两臂在左
        drawLine(color, Offset(w * 0.58f, cy - h * 0.38f), Offset(w * 0.84f, cy), strokeWidth = s, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.84f, cy), Offset(w * 0.58f, cy + h * 0.38f), strokeWidth = s, cap = StrokeCap.Round)
    }
}

/** 滤镜按钮右侧的小开关：带轨道色 + 旋钮色 + 位置过渡动效。 */
@Composable
internal fun FilterSwitch(
    checked: Boolean,
    isDark: Boolean = false,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) (if (isDark) Color(0xFFCFCFCF) else Color.White) else (if (isDark) Color(0xFF555555) else Color(0xFFBDB6A8)),
        animationSpec = tween(200), label = "switchTrack",
    )
    val knobColor by animateColorAsState(
        targetValue = if (checked) RetroRust else (if (isDark) Color(0xFFCCCCCC) else Color.White),
        animationSpec = tween(200), label = "switchKnobColor",
    )
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = tween(200), label = "switchKnob",
    )
    val density = LocalDensity.current
    val knobDy = with(density) { 2.dp.toPx() }.toInt()
    Box(
        modifier = modifier
            .size(44.dp, 24.dp).clip(RoundedCornerShape(12.dp))
            .background(trackColor)
            .clickable { onCheckedChange() },
    ) {
        Box(
            Modifier.offset { IntOffset(with(density) { knobOffset.toPx() }.toInt(), knobDy) }
                .size(20.dp).clip(CircleShape)
                .background(knobColor),
        )
    }
}

/** 有调色盘（暖色/冷色）滤镜的名字后方 logo：圆角小矩形，中空（仅描边、不填充）。 */
@Composable
internal fun PaletteLogo(modifier: Modifier = Modifier, color: Color = Color(0xFF6B5744)) {
    Box(
        modifier
            .size(11.dp)
            .border(1.dp, color, RoundedCornerShape(3.dp)),
    )
}

/** 横滑触发后浮于对应滤镜按钮正上方的“调节示意”：与按钮同形同大，深色填满代表参数百分比。 */
@Composable
internal fun SlideHintBubble(intensity: Float, isDark: Boolean = false, modifier: Modifier = Modifier) {
    val pct = (intensity * 100f).roundToInt()
    val frac = intensity.coerceIn(0f, 1f)
    Box(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (isDark) RetroDarkSurface else Color(0xFFE0DCD4)),
    ) {
        // 深色自左向右填满，宽度 = 参数百分比
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(frac)
                .clip(RoundedCornerShape(22.dp))
                .background(if (isDark) Color(0xFFCFCFCF) else Color(0xFF3A2E22)),
        )
        Text(
            "$pct%",
            color = if (frac >= 0.5f) (if (isDark) Color(0xFF242424) else Color.White) else (if (isDark) Color(0xFFCFCFCF) else Color(0xFF3A2E22)),
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}
