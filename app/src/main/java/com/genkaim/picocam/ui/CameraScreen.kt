package com.genkaim.picocam.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.genkaim.picocam.CameraViewModel
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import android.graphics.BitmapFactory
import com.genkaim.picocam.PhotoViewerActivity
import com.genkaim.picocam.SettingsActivity
import com.genkaim.picocam.ui.components.CameraParamItem
import com.genkaim.picocam.ui.components.CaptureTransitionOverlay
import com.genkaim.picocam.ui.components.ControlBar
import com.genkaim.picocam.ui.components.EffectsPanel
import com.genkaim.picocam.ui.components.ViewfinderFrame
import com.genkaim.picocam.ui.components.ZoomBar
import com.genkaim.picocam.ui.components.SlideHintIcon
import com.genkaim.picocam.dynamic.AppPrefs
import com.genkaim.picocam.dynamic.DynamicIslandConfig
import com.genkaim.picocam.dynamic.DynamicIslandSettingsActivity
import com.genkaim.picocam.dynamic.ThemeMode
import com.genkaim.picocam.dynamic.ViewfinderConfig
import com.genkaim.picocam.dynamic.isDarkMode
import com.genkaim.picocam.dynamic.ThemePrefs.saveThemeSettings
import com.genkaim.picocam.ui.theme.RetroBrown
import com.genkaim.picocam.ui.theme.RetroCream
import com.genkaim.picocam.ui.theme.RetroRust
import com.genkaim.picocam.ui.theme.onSurface
import com.genkaim.picocam.ui.theme.onSurfaceSoft
import com.genkaim.picocam.ui.theme.surfaceCard

@Composable
fun CameraScreen(vm: CameraViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasCamPerm by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    ) }
    var hasLocPerm by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    ) }
    val camLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCamPerm = it }
    val locLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        hasLocPerm = perms.values.any { it }
    }
    val viewerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        vm.refreshPhotos()
        vm.restoreZoom()
    }

    val captureWithLocation: () -> Unit = {
        if (hasLocPerm) {
            try {
                val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
                val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                vm.setLastLocation(loc)
            } catch (_: Exception) {}
        }
        vm.takePhoto()
    }

    // 深色模式下主背景改为深灰
    val configuration = LocalConfiguration.current
    val theme by AppPrefs.theme.collectAsStateWithLifecycle()
    val isSystemDark = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val isDark = isDarkMode(theme.mode, isSystemDark)
    val bgColor = if (isDark) Color(0xFF242424) else Color(0xFFF0EDE6)

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        // 等配置预读完成再渲染，避免首帧用默认参数再跳变（露陷）
        val loaded by AppPrefs.loaded.collectAsStateWithLifecycle()
        if (loaded) {
            if (hasCamPerm && hasLocPerm && theme.onboarded) {
                CameraContent(vm, onSelect = { file, fadeIn ->
                    viewerLauncher.launch(
                        Intent(context, PhotoViewerActivity::class.java)
                            .putExtra("file_path", file.path)
                            .putExtra("fade_in", fadeIn),
                    )
                }, onShutter = captureWithLocation)
            } else {
                PermissionRequest(
                    hasCamera = hasCamPerm,
                    hasLocation = hasLocPerm,
                    isDark = isDark,
                    onRequestCamera = { camLauncher.launch(Manifest.permission.CAMERA) },
                    onRequestLocation = { locLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
                    onNext = {
                        // 引导流程：仅在此处手动触发灵动岛/取景框设置，并立即持久化「已引导」，
                        // 相机页本身不再自动触发设置（符合「仅引导界面才触发」）。
                        scope.launch { context.saveThemeSettings { copy(onboarded = true) } }
                        context.startActivity(
                            Intent(context, DynamicIslandSettingsActivity::class.java)
                                .putExtra("onboarding", true),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun CameraContent(vm: CameraViewModel, onSelect: (File, Boolean) -> Unit, onShutter: () -> Unit) {
    val context = LocalContext.current
    val photos by vm.photos.collectAsStateWithLifecycle()
    val flashMode by vm.flashMode.collectAsStateWithLifecycle()
    val isBackCamera by vm.isBackCamera.collectAsStateWithLifecycle()
    val zoomLevel by vm.zoomLevel.collectAsStateWithLifecycle()
    val vfConfig by AppPrefs.viewfinder.collectAsStateWithLifecycle()
    val diConfig by AppPrefs.dynamicIsland.collectAsStateWithLifecycle()
    val anim by AppPrefs.animSettings.collectAsStateWithLifecycle()
    val placeholderPhoto by vm.placeholderPhoto.collectAsStateWithLifecycle()
    val photoVersion by vm.photoVersion.collectAsStateWithLifecycle()
    val theme by AppPrefs.theme.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val densityPx = density.density
    // 深色模式判定：用户选深色→true，选浅色→false，跟随系统→按系统暗色模式
    val isSystemDark = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val isDark = when (theme.mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemDark
    }
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    var effectsExpanded by remember { mutableStateOf(false) }
    // 相册首位占位位置（退出预览时照片飞入此处）；由占位 cell 的 onGloballyPositioned 测量
    var albumSlot by remember { mutableStateOf<Rect?>(null) }

    // 拍照过渡动画状态：transitionFile 非空时显示 CaptureTransitionOverlay；animating 用于防止动画期间重复触发
    var transitionFile by remember { mutableStateOf<File?>(null) }
    var animating by remember { mutableStateOf(false) }
    // ============ 多选状态：长按照片进入多选模式 ============
    var selectionMode by remember { mutableStateOf(false) }
    val selectedPhotos = remember { mutableStateListOf<File>() }
    // ============ 展开/折叠手势状态 ============
    // progress：1 = 取景框完全展开（相机态），0 = 折叠为灵动岛（隐藏取景框、只留岛）。全程跟手，松手吸附到 0/1。
    val progressState = remember { mutableFloatStateOf(0f) }
    // 多选删除确认弹窗
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<File?>(null) }
    // 进入多选模式：自动折叠到灵动岛（长按时触发）
    LaunchedEffect(selectionMode) {
        if (selectionMode) {
            animate(progressState.floatValue, 0f, animationSpec = tween(280)) { v: Float, _: Float ->
                progressState.floatValue = v
            }
        }
    }

    LaunchedEffect(Unit) {
        vm.photoCaptured.collect { file ->
            if (!animating) {
                animating = true
                effectsExpanded = false   // 拍照播放动画时隐藏效果区（若仍展开）
            if (!anim.animEnabled) {
                // 无动画：CameraViewModel 拍照后把 file 加入 _photos（带占位 cell，视觉与 GridPhotoCard 一致），
                // 避免相册从"空状态"突然变到有照片的视觉跳变。addPolaroidFrame 完成后通过 _photoProcessing = false
                // 通知 UI 端：清占位（GridPhotoCard 走 else 分支，新组合项 fileRatio 第一次读到完整文件 → 正确比例）、
                // 释放 animating。clearPlaceholder 必须在 photoProcessing 变 false **之后**（保证 addPolaroidFrame 完成、
                // 文件已带白框），否则 GridPhotoCard 第一次渲染会读到残缺文件。
                // 关键：takePhoto.onImageSaved 不再更新 _photos（相册在 showDetail=true 之前冻结），
                // 这里需要主动调 refreshPhotosSync 更新 _photos + setPlaceholder 设置占位
                vm.setPlaceholder(file)
                vm.refreshPhotosSync()
                scope.launch {
                    while (vm.photoProcessing.value) {
                        delay(30)
                    }
                    vm.clearPlaceholder()
                    animating = false
                }
                return@collect
            }
                // 先把取景框快照到展开态，使过渡 overlay 的 morph 从展开态→灵动岛 视觉正确
                progressState.floatValue = 1f
                transitionFile = file
                // 同时后台逐步把 progressState 收回到 0，过渡结束时取景框已是折叠态
                scope.launch {
                    animate(progressState.floatValue, 0f, animationSpec = tween(durationMillis = 380)) { v, _ ->
                        progressState.floatValue = v
                    }
                }
            }
        }
    }

    val flashAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        vm.shutterFlash.collect { flashAlpha.snapTo(0.9f); flashAlpha.animateTo(0f, tween(150)) }
    }

    val p = progressState.floatValue
    // 操控区自然高度（px，unbounded 测得，与是否被裁切无关）；给个初值≈200dp 避免首帧塌陷
    var ctrlNaturalPx by remember { mutableStateOf((200 * densityPx).roundToInt()) }
    // 跟手行程：手指位移 dragRangePx 对应 progress 走完 0↔1（可调节手感）
    val dragRangePx = 260f * densityPx

    // 跟手拖拽：按位移改变 progress，返回实际消费的 px（用于 nestedScroll 协作）
    fun applyDrag(dy: Float): Float {
        val old = progressState.floatValue
        val neo = (old + dy / dragRangePx).coerceIn(0f, 1f)
        progressState.floatValue = neo
        return (neo - old) * dragRangePx
    }
    // 松手吸附：快速滑动按方向定，否则按是否过半。tween 动画仍跟手感（承接手指速度）
    suspend fun settleTo(velocity: Float) {
        val cur = progressState.floatValue
        val target = when {
            velocity > 900f -> 1f
            velocity < -900f -> 0f
            else -> if (cur > 0.5f) 1f else 0f
        }
        animate(cur, target, animationSpec = tween(durationMillis = 260)) { v, _ -> progressState.floatValue = v }
    }

    // 相册嵌套滚动协作：
    // · 上滑(dy<0) 且已展开(progress>0) 且相册在顶部 → 优先折叠取景框（preScroll 消费）；折叠完剩余量交给相册滚动。
    // · 下滑(dy>0) 且未完全展开 → 相册先滚回顶部(preScroll 不拦)，到顶后剩余量(postScroll)用于展开取景框。
    // 这样即满足："展开时相册不在顶部则相册滚动；在顶部才折叠"，以及"折叠态下滑到顶后展开"。
    val nested = remember(dragRangePx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y
                if (dy < 0f && progressState.floatValue > 0f && !gridState.canScrollBackward) {
                    return Offset(0f, applyDrag(dy))
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y
                if (dy > 0f && progressState.floatValue < 1f) {
                    return Offset(0f, applyDrag(dy))
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val midway = progressState.floatValue in 0.0001f..0.9999f
                if (midway) settleTo(available.y)
                return if (midway) available else Velocity.Zero
            }
        }
    }

    // 取景框拖拽（作用于取景框/灵动岛本体）：竖向跟手改 progress，松手吸附
    val headerDrag = Modifier.pointerInput(selectionMode, effectsExpanded) {
        if (!selectionMode && !effectsExpanded) {
            detectVerticalDragGestures(
                onVerticalDrag = { change, dragAmount -> applyDrag(dragAmount); change.consume() },
                onDragEnd = { scope.launch { settleTo(0f) } },
                onDragCancel = { scope.launch { settleTo(0f) } },
            )
        }
    }

    // ============ 几何插值：灵动岛(p=0) ↔ 取景框(p=1) ============
    val screenH = configuration.screenHeightDp.toFloat()
    val screenW = configuration.screenWidthDp.toFloat()
    val halfH = screenH / 2f
    val vfW = vfConfig.widthDp.toFloat()
    val maxOffset = (halfH - vfConfig.widthDp).coerceAtLeast(0f)
    val vfTop = (halfH / 2f + (vfConfig.posY - 0.5f) * maxOffset - vfConfig.widthDp.toFloat() / 2f).coerceAtLeast(0f)
    val vfCenterX = screenW / 2f
    // 灵动岛折叠态：位置/大小按设置
    val isW = diConfig.widthDp.toFloat()
    val isH = diConfig.heightDp.toFloat().coerceAtLeast(12f)
    val isCenterX = diConfig.posX * screenW
    val isTop = (diConfig.posY * screenH - isH / 2f).coerceAtLeast(2f)
    val lerp = { a: Float, b: Float, t: Float -> a + (b - a) * t }
    // 插值出当前帧的取景框矩形与圆角、横向偏移
    val fW = lerp(isW, vfW, p)
    val fH = lerp(isH, vfW, p)     // 展开态为正方形(高=vfW)
    val fTop = lerp(isTop, vfTop, p)
    val fCorner = lerp(diConfig.cornerDp.toFloat(), vfConfig.cornerDp.toFloat(), p)
    val fCenterX = lerp(isCenterX, vfCenterX, p)
    val xOffsetPx = ((fCenterX - vfCenterX) * densityPx).roundToInt()
    // 边框宽度取灵动岛高度，并约束不超过取景框一半；折叠态该边框会填满小尺寸取景框 → 呈现纯黑灵动岛外观
    val borderDp = diConfig.heightDp.coerceIn(0, vfConfig.widthDp / 2)

    // 操控区两段动画：先高度(0→0.5)由 0 到自然值，再不透明度(0.5→1)由 0 到 1（折叠时自动反向：先淡出再收高度）
    val heightFactor = (p / 0.5f).coerceIn(0f, 1f)
    val ctrlAlpha = ((p - 0.5f) / 0.5f).coerceIn(0f, 1f)
    val ctrlNaturalDp = with(density) { ctrlNaturalPx.toDp() }

    Box(modifier = Modifier.fillMaxSize()) {
        // 模糊已移至 CaptureTransitionOverlay 内部（用刚拍的照片做模糊背景层，scrim 之前），
        // 相机内容层（取景框/操控区/相册/效果面板）保持清晰，用户能看到相册里的其他照片。
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部留白：随 progress 在灵动岛顶 ↔ 取景框顶 之间插值
            Spacer(Modifier.height(fTop.dp))

            // 取景框：随 progress morph（灵动岛尺寸/位置/圆角 ↔ 取景框）。
            // 重要：ViewfinderFrame 内部的"黑边框"已改为 draw 层 border（不再用 padding 缩小内层 AndroidView），
            // 折叠态时 AndroidView 仍占满外框（isW × isH），PreviewView 的 Surface 非零，cameraX 持续 STREAMING，
            // 焦段/手动参数可正常应用。视觉上：折叠态边框 = 厚度 = 岛高，恰好覆盖整张岛 → 呈现纯黑灵动岛外观，
            // 所以 p ≈ 0 时不需要再 alpha=0（否则连岛本身也看不到了）。
            ViewfinderFrame(
                controller = vm.controller,
                flashAlpha = if (transitionFile == null) flashAlpha.value else 0f,
                eff = vm.effective,
                cornerDp = fCorner,
                borderDp = borderDp.toFloat(),
                onPreviewStreaming = vm::onPreviewStreaming,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .offset { IntOffset(xOffsetPx, 0) }
                    .size(fW.dp, fH.dp)
                    .then(if (isDark) Modifier.border(1.dp, Color(0xFF5A5A5A), RoundedCornerShape(fCorner.dp)) else Modifier)
                    .alpha(if (transitionFile == null) 1f else 0f)
                    .then(headerDrag),
            )

            // 操控区（缩放条 + 参数行 + 控制栏）：整体作两段动画。外层 Box 高度=自然高度*heightFactor 并裁切；
            // 内层 Column 用 wrapContentHeight(unbounded) 始终按自然高度测量（onSizeChanged 稳定），alpha=ctrlAlpha。
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(ctrlNaturalDp * heightFactor)
                    .clipToBounds(),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(align = Alignment.Top, unbounded = true)
                        .onSizeChanged { ctrlNaturalPx = it.height }
                        .graphicsLayer { alpha = ctrlAlpha },
                ) {
                    // 缩放条与取景框之间的间距
                    Spacer(Modifier.height(10.dp))
                    // 缩放条
                    ZoomBar(zoomLevel = zoomLevel, isDark = isDark, onZoomChange = vm::setZoom,
                        modifier = Modifier.padding(top = 2.dp, bottom = 0.dp))
                    // 手动相机参数行：横滑调节
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CameraParamItem(
                            name = "对焦距离",
                            valueText = vm.focusDistanceText(),
                            fraction = vm.focusFraction,
                            onFractionChange = vm::updateFocusFraction,
                            isDark = isDark,
                            modifier = Modifier.weight(1f),
                        )
                        CameraParamItem(
                            name = "快门速度",
                            valueText = vm.shutterSpeedText(),
                            fraction = vm.shutterFraction,
                            onFractionChange = vm::updateShutterFraction,
                            isDark = isDark,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // 控制栏
                    Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
                        ControlBar(flashMode, isBackCamera, effectsExpanded, vm::toggleFlash, vm::switchCamera,
                            isDark = isDark,
                            onShutter = onShutter, onSettings = {
                                context.startActivity(Intent(context, SettingsActivity::class.java))
                            }, onToggleEffects = { effectsExpanded = !effectsExpanded })
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // 相册顶部与上方（灵动岛/操控区）之间留点空隙：折叠态即灵动岛与相册的间距，展开态即操控区与相册的间距
            Spacer(Modifier.height(8.dp))

            // 剩余区域：相册（底层）+ 效果面板（悬浮层），weight(1f) 吸收全部高度变化（折叠时相册占满）
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (photos.isEmpty()) {
                    // 空相册占位：整块可竖向拖拽（无列表可滚动，故直接用拖拽切换展开/折叠）
                    Box(
                        Modifier
                            .fillMaxSize()
                            .then(headerDrag),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("向下滑动打开相机", style = MaterialTheme.typography.bodyLarge, color = onSurfaceSoft(isDark))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("标有 ", style = MaterialTheme.typography.bodyMedium, color = onSurfaceSoft(isDark))
                                SlideHintIcon(color = onSurfaceSoft(isDark))
                                Text(" 图标的区域左右滑动可以调节参数", style = MaterialTheme.typography.bodyMedium, color = onSurfaceSoft(isDark))
                            }
                            // 长按提示：拍下首张照片后，长按照片即可进入批量编辑模式（多选删除/分享）
                            Text("长按照片可以批量编辑", style = MaterialTheme.typography.bodyMedium, color = onSurfaceSoft(isDark))
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(3), contentPadding = PaddingValues(top = 44.dp, bottom = 12.dp, start = 12.dp, end = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize().nestedScroll(nested)) {
                        items(photos, key = { it.path }) { file ->
                            if (placeholderPhoto == file) {
                                // 完全透明占位：只占空间（避免相册从空状态突变到有格子）+ 测量飞入落点
                                // 不显示任何视觉元素。placeRatio key 含 photoVersion → addPolaroidFrame 完成后
                                // 比例重算为正确值（虽然透明看不到，但 GridPhotoCard 切换时 fileRatio 也对应正确）。
                                val placeRatio = remember(file, photoVersion) {
                                    try {
                                        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                        BitmapFactory.decodeFile(file.path, o)
                                        if (o.outWidth > 0 && o.outHeight > 0) o.outWidth.toFloat() / o.outHeight.toFloat() else 1f
                                    } catch (_: Exception) { 1f }
                                }
                                Box(
                                    Modifier
                                        .aspectRatio(placeRatio)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Transparent)
                                        .onGloballyPositioned { coords ->
                                            val pos = coords.localToRoot(Offset.Zero)
                                            val w = coords.size.width.toFloat()
                                            val h = coords.size.height.toFloat()
                                            albumSlot = Rect(pos.x, pos.y, pos.x + w, pos.y + h)
                                        },
                                )
                            } else {
                                val isSelected = selectedPhotos.contains(file)
                                GridPhotoCard(
                                    file = file,
                                    isDark = isDark,
                                    selectionMode = selectionMode,
                                    isSelected = isSelected,
                                    photoVersion = photoVersion,
                                    onClick = {
                                        if (selectionMode) {
                                            if (isSelected) selectedPhotos.remove(file) else selectedPhotos.add(file)
                                        } else {
                                            onSelect(file, false)
                                        }
                                    },
                                    onLongClick = {
                                        if (!selectionMode) {
                                            selectionMode = true
                                            selectedPhotos.clear()
                                        }
                                        if (isSelected) selectedPhotos.remove(file) else selectedPhotos.add(file)
                                    },
                                )
                            }
                        }   // 关闭 items
                    }   // 关闭 LazyVerticalGrid

                // 相册顶部渐变（背景色→透明）：仅相册展开(折叠/岛态, p=0)时可见，随展开淡出；让灵动岛下方的相册内容柔和渐显
                val albumTopFade = (1f - p).coerceIn(0f, 1f)
                val bgColor = if (isDark) Color(0xFF242424) else Color(0xFFF0EDE6)
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(32.dp)
                        .graphicsLayer { alpha = albumTopFade }
                        .background(Brush.verticalGradient(listOf(bgColor, Color.Transparent))),
                )

                // EffectsPanel 已移到 if/else 外部（确保空相册时也能显示）

                // 多选模式：悬浮胶囊底部菜单 + 渐入/上滑/下滑动效 + 点击空白退出选择
                Column(Modifier.fillMaxSize().background(Color.Transparent)) {
                    AnimatedVisibility(
                        visible = selectionMode,
                        enter = fadeIn(tween(200)),
                        exit = fadeOut(tween(200)),
                ) {
                    Box(Modifier.fillMaxSize()) {
                        // 悬浮底部胶囊（抬高到底部 40dp），点击"取消"退出多选
                        // 浅色模式用 desaturated RetroPaper（0xFFEAE3D5）：保留暖色调但降低饱和度，
                        // 与相册背景 Color(0xFFF0EDE6) 形成柔和但清晰的对比，不抢视觉焦点；深色模式保持 surfaceCard
                        // 所有元素固定宽高 + maxLines=1：文字内容变化（"已选 1"↔"已选 999"、"全选"↔"取消全选"）
                        // 不影响布局，dp 单位随屏幕密度自动缩放（多屏适配）
                        Row(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 40.dp)
                                .padding(horizontal = 16.dp)  // 左右间距
                                .clip(RoundedCornerShape(28.dp))
                                .background(if (isDark) surfaceCard(isDark) else Color(0xFFEAE3D5))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // 取消按钮：固定宽度，无背景
                            Box(
                                modifier = Modifier.width(40.dp).height(36.dp).clickable {
                                    selectionMode = false; selectedPhotos.clear()
                                },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("取消", color = onSurface(isDark), maxLines = 1)
                            }
                            Spacer(Modifier.width(8.dp))
                            // 已选计数：固定宽度，容纳"已选 99"
                            Box(
                                modifier = Modifier.width(52.dp).height(36.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "已选 ${selectedPhotos.size}",
                                    color = onSurfaceSoft(isDark),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            // 全选按钮：固定宽度 Box 居中文字，避免"全选"↔"取消全选"切换时按钮宽度变化
                            val allSelected = photos.isNotEmpty() && selectedPhotos.size == photos.size
                            Box(
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable {
                                        if (allSelected) selectedPhotos.clear()
                                        else { selectedPhotos.clear(); selectedPhotos.addAll(photos) }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    if (allSelected) "取消全选" else "全选",
                                    color = onSurfaceSoft(isDark),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            // 分享胶囊：固定宽度
                            Box(
                                Modifier.width(60.dp).height(44.dp).clip(RoundedCornerShape(22.dp))
                                    .background(if (selectedPhotos.size > 0) surfaceCard(isDark) else onSurfaceSoft(isDark).copy(alpha = 0.3f))
                                    .clickable(enabled = selectedPhotos.size > 0) {
                                        val uris = ArrayList<Uri>()
                                        selectedPhotos.forEach { f ->
                                            try {
                                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                                    context, "${context.packageName}.fileprovider", f
                                                )
                                                uris.add(uri)
                                            } catch (_: Exception) {}
                                        }
                                        if (uris.isNotEmpty()) {
                                            val sendIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                                type = "image/jpeg"
                                                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "分享 ${selectedPhotos.size} 张照片"))
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "分享",
                                    color = if (selectedPhotos.size > 0) onSurface(isDark) else onSurfaceSoft(isDark),
                                    maxLines = 1,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            // 删除圆形图标（参考相册详情页右下角删除键）：44dp 圆形 + Delete 图标
                            Box(
                                Modifier.size(44.dp).clip(CircleShape)
                                    .background(if (selectedPhotos.size > 0) RetroRust.copy(alpha = 0.15f) else onSurfaceSoft(isDark).copy(alpha = 0.3f))
                                    .clickable(enabled = selectedPhotos.size > 0) { showDeleteConfirm = true },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "删除",
                                    tint = if (selectedPhotos.size > 0) RetroRust else onSurfaceSoft(isDark),
                                )
                            }
                        }   // Row 结束
                    }   // Box(fillMaxSize) 结束
                }   // AnimatedVisibility 结束
            }   // Column 结束（多选菜单列）
                }   // 关闭 else (空相册 / 有相册)

                // 效果面板：悬浮叠加层，浮在相册上方（控制栏正下方），展开/收起不影响相册布局
                // 关键：放在 if/else 外部，确保空相册时也能显示（之前在 else 内导致无照片时打不开）
                EffectsPanel(
                    visible = effectsExpanded,
                    vm = vm,
                    isDark = isDark,
                    modifier = Modifier.fillMaxSize(),
                )

        }   // 关闭 weight(1f) Box
        }   // 关闭 Column（相机内容）

        // 拍照后过渡动画：取景框 → 灵动岛 → 打印 → 放大 → 本页详情。
        // 关键：放在 Column 外面（Box outermost 内），不受 Column 布局/weight 分配影响。
        // 这样过渡 overlay 自己 fillMaxSize 覆盖整个屏幕，不被 Column 内的 Box(weight) 测量/约束干扰。
        if (transitionFile != null) {
            CaptureTransitionOverlay(
                file = transitionFile!!,
                vfConfig = vfConfig,
                diConfig = diConfig,
                albumSlot = albumSlot,
                isDark = isDark,
                photoVersion = photoVersion,
                photoProcessing = vm.photoProcessing.collectAsStateWithLifecycle().value,
                onDetailState = { },
                onAddToAlbum = {
                    // 关键：不要重置 albumSlot = null
                    // 原因：拍照后 _photos 立即包含新文件，albumSlot 已经在 grow 期间被占位 cell onGloballyPositioned 测量。
                    // 如果这里重置 albumSlot，refreshPhotosSync 后 _photos 内容未变（StateFlow equals 不通知），
                    // LazyVerticalGrid 不重组 → 占位 cell 不重新布局 → onGloballyPositioned 不触发 → albumSlot 永远为 null
                    // → close() 飞入动画的 flySlot 始终为 null → 飞入动画不播放。
                    vm.setPlaceholder(transitionFile!!)
                    vm.refreshPhotosSync()
                    // 预加载缩略图到 Coil 内存缓存，使 clearPlaceholder 后 GridPhotoCard 直接命中缓存无闪烁
                    scope.launch {
                        val f = transitionFile!!
                        val req = ImageRequest.Builder(context)
                            .data(f)
                            .size(150)
                            .memoryCacheKey("thumb_${f.path}_${f.lastModified()}")
                            .build()
                        context.imageLoader.execute(req)
                    }
                },
                onViewfinderAutoOpen = {
                    // 进入预览模式时（背景模糊后）自动打开取景框
                    if (anim.openViewfinderAfterCapture) {
                        scope.launch {
                            animate(progressState.floatValue, 1f, animationSpec = tween(durationMillis = 380)) { v, _ ->
                                progressState.floatValue = v
                            }
                        }
                    }
                },
                onExit = { deleted ->
                    vm.clearPlaceholder()   // 占位变真实照片
                    animating = false
                    transitionFile = null
                    vm.restoreZoom()
                    // 仅删除时才刷新列表（需移除已删文件）；正常关闭不刷新，避免动画后缩略图闪动
                    if (deleted) vm.refreshPhotosSync()
                },
            )
        }

        // 多选删除二次确认（适配深色模式）。同样放在 Column 外面，避免被 Column 内容遮挡。
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                containerColor = surfaceCard(isDark),
                title = { Text("删除照片", fontFamily = FontFamily.Default, color = onSurface(isDark)) },
                text = { Text("确定要删除选中的 ${selectedPhotos.size} 张照片吗？此操作不可撤销。", fontFamily = FontFamily.Default, color = onSurface(isDark)) },
                confirmButton = {
                    TextButton(onClick = {
                        selectedPhotos.toList().forEach { vm.deletePhoto(it) }
                        selectionMode = false
                        selectedPhotos.clear()
                        showDeleteConfirm = false
                    }) { Text("删除", fontFamily = FontFamily.Default, color = RetroRust) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("取消", fontFamily = FontFamily.Default, color = onSurfaceSoft(isDark)) }
                },
            )
        }
    }   // 关闭 Box outermost
}   // 关闭 CameraContent

@Composable
private fun GridPhotoCard(
    file: File,
    isDark: Boolean,
    selectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    photoVersion: Long,
) {
    val context = LocalContext.current
    // 预读照片真实比例（仅读文件头）。photoVersion 变化（新照 / Polaroid 完成）时不必重读所有格子——比例只跟文件本身有关。
    val fileRatio = remember(file) {
        try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, opts)
            if (opts.outWidth > 0 && opts.outHeight > 0) opts.outWidth.toFloat() / opts.outHeight.toFloat() else 1f
        } catch (_: Exception) { 1f }
    }
    // ImageRequest：size(150) 缩略图尺寸（三列网格每格 ~120dp，150px 足够清晰且解码更快）；
    // memoryCacheKey 含 photoVersion → 缓存失效自动重载 Polaroid 版；
    // crossfade(300) → 拍照后 addPolaroidFrame 完成时，GridPhotoCard 从占位 cell 切换过来，
    // 图片从无到有 300ms 淡入，避免"格子突然有图"的视觉跳变
    val request = remember(file, photoVersion) {
        ImageRequest.Builder(context)
            .data(file)
            .size(150)
            .memoryCacheKey("thumb_${file.path}_${file.lastModified()}")
            .crossfade(300)
            .build()
    }
    Box(
        Modifier
            .aspectRatio(fileRatio)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) RetroRust.copy(alpha = 0.35f) else surfaceCard(isDark))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) RetroCream else onSurfaceSoft(isDark).copy(alpha = 0.3f),
                shape = RoundedCornerShape(4.dp),
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        // 用 AsyncImage 替代 SubcomposeAsyncImage：避免子组合（subcomposition）开销，
        // 每张缩略图节省一次独立的 Compose 作用域创建/回收，大量照片时帧率明显提升。
        AsyncImage(
            model = request,
            contentDescription = "照片",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // 深色模式：照片上叠一层浅灰色遮罩
        if (isDark) {
            Box(Modifier.fillMaxSize().background(Color(0x1AFFFFFF)))
        }
        // 多选模式：右上角选中圆（填充色代表已选，无色=未选，无勾）
        if (selectionMode) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) RetroRust else Color(0x66000000))
                    .border(1.5.dp, if (isSelected) RetroCream else Color(0x88FFFFFF), CircleShape),
            )
        }
    }
}

@Composable
private fun PermissionRequest(hasCamera: Boolean, hasLocation: Boolean, isDark: Boolean, onRequestCamera: () -> Unit, onRequestLocation: () -> Unit, onNext: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("InstCam", style = MaterialTheme.typography.titleLarge, color = onSurface(isDark))
        Spacer(Modifier.height(12.dp))
        Text("需要授权后才能使用相机", style = MaterialTheme.typography.bodyMedium, color = onSurfaceSoft(isDark), textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))

        Box(
            modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(percent = 50))
            .background(if (hasCamera) surfaceCard(isDark) else RetroBrown)
            .clickable(enabled = !hasCamera, onClick = onRequestCamera)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (hasCamera) "✓ 相机已授权" else "授予相机权限",
                style = MaterialTheme.typography.titleMedium,
                color = if (hasCamera) onSurface(isDark) else RetroCream,
            )
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(percent = 50))
                .background(if (hasLocation) surfaceCard(isDark) else RetroBrown)
                .clickable(enabled = !hasLocation, onClick = onRequestLocation)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (hasLocation) "✓ 定位已授权" else "授予定位权限（照片记录位置）",
                style = MaterialTheme.typography.titleMedium,
                color = if (hasLocation) onSurface(isDark) else RetroCream,
            )
        }

        // 两个权限都授予后，稍等一下自动进入引导设置（不再需要手动点击"下一步"）
        if (hasCamera && hasLocation) {
            LaunchedEffect(Unit) {
                delay(400L)
                onNext()
            }
        }
    }
}