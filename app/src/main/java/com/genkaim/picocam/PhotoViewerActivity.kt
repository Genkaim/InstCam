package com.genkaim.picocam

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.content.res.Configuration
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.palette.graphics.Palette
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.genkaim.picocam.ui.theme.RetroBrown
import com.genkaim.picocam.ui.theme.RetroCream
import com.genkaim.picocam.ui.theme.RetroDarkBg
import com.genkaim.picocam.ui.theme.RetroDarkSurface
import com.genkaim.picocam.ui.theme.RetroPaper
import com.genkaim.picocam.ui.theme.RetroRust
import com.genkaim.picocam.TintState
import com.genkaim.picocam.camera.EffectiveFilter
import com.genkaim.picocam.camera.PhotoStorage
import com.genkaim.picocam.camera.buildFilterColorMatrix
import com.genkaim.picocam.dynamic.AppPrefs
import com.genkaim.picocam.dynamic.isDarkMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatPhotoTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private suspend fun readLocationFromExif(context: Context, file: File): String? = withContext(Dispatchers.IO) {
    try {
        val exif = androidx.exifinterface.media.ExifInterface(file.path)
        val latLats = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE)
        val latRef = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE_REF)
        val lngLats = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE)
        val lngRef = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE_REF)
        if (latLats == null || lngLats == null) return@withContext null
        val lat = parseDms(latLats) * if (latRef == "S") -1 else 1
        val lng = parseDms(lngLats) * if (lngRef == "W") -1 else 1
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses: List<Address> = geocoder.getFromLocation(lat, lng, 1) ?: emptyList()
        if (addresses.isNotEmpty()) {
            val a = addresses[0]
            a.getAddressLine(0) ?: buildString {
                a.locality?.let { append(it) }
                a.subAdminArea?.let { if (isNotEmpty()) append(" · "); append(it) }
                a.adminArea?.let { if (isNotEmpty()) append(" · "); append(it) }
            }
        } else null
    } catch (_: Exception) { null }
}

private fun parseDms(dms: String): Double {
    return try {
        val parts = dms.split(",").map { it.trim() }
        if (parts.size < 3) return 0.0
        fun parseFraction(s: String): Double {
            val slash = s.indexOf('/')
            return if (slash > 0) s.substring(0, slash).toDouble() / s.substring(slash + 1).toDouble()
            else s.toDouble()
        }
        val d = parseFraction(parts[0])
        val m = parseFraction(parts[1])
        val s = parseFraction(parts[2])
        d + m / 60.0 + s / 3600.0
    } catch (_: Exception) { 0.0 }
}

/**
 * 从图片中提取主色用于背景。
 * 关键优化：
 * ① 先用 inJustDecodeBounds=true 仅读出尺寸、按目标尺寸（~256px）计算 inSampleSize，
 *    再用 inSampleSize 解码出小图 → Palette 取色。32MP 原图原大小解码要 ~100ms+、Palette 更慢；
 *    缩小到 256px 后整段耗时一般 < 50ms，"闪一下原色"时间显著缩短。
 * ② 返回的 Int 会被外层 animateColorAsState 用 tween(400) 平滑过渡到新色，
 *    即使解码仍有一瞬的等待，背景色也是平滑渐变而不是"瞬切回原色"→ 真正看不到原色闪烁。
 */
private suspend fun extractDominantColor(file: File, fallback: Int): Int = withContext(Dispatchers.IO) {
    try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        // 计算 inSampleSize：将图片缩小到短边 ~256px，Palette 取色完全够用
        val target = 256
        var sample = 1
        var w = bounds.outWidth; var h = bounds.outHeight
        while (maxOf(w, h) / sample > target * 2) {  // 留 2x 余量给 Palette 边缘色提取
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inJustDecodeBounds = false
        }
        val bm = BitmapFactory.decodeFile(file.path, opts) ?: return@withContext fallback
        val palette = Palette.from(bm).generate()
        bm.recycle()
        // 取色策略（官方 Palette 指导）：以低饱和的 Muted 色板为主，getLightMutedColor 优先取柔和亮色主调，
        // 退到 Muted → LightVibrant → Dominant。原 getLightVibrantColor 饱和度过高（背景刺眼），
        // 改 Muted 系列后背景为低饱和柔色，更符合"莫奈"淡彩且不与照片抢色。
        palette.getLightMutedColor(
            palette.getMutedColor(
                palette.getLightVibrantColor(palette.getDominantColor(fallback))
            )
        )
    } catch (_: Exception) { fallback }
}

private fun textColorForBg(argb: Int): Color {
    val luminance = (0.299 * AndroidColor.red(argb) + 0.587 * AndroidColor.green(argb) + 0.114 * AndroidColor.blue(argb)) / 255.0
    return if (luminance > 0.5) RetroBrown else RetroCream
}

/** 给定 Compose Color，返回是否"偏亮"（用于决定文字/胶囊是浅底深字还是深底浅字）。 */
private fun isBgLight(c: Color): Boolean {
    val a = c.toArgb()
    val l = (0.299 * AndroidColor.red(a) + 0.587 * AndroidColor.green(a) + 0.114 * AndroidColor.blue(a)) / 255.0
    return l > 0.5
}

class PhotoViewerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val filePath = intent?.getStringExtra("file_path")
        if (filePath == null) { finish(); return }
        setContent {
            PhotoViewerContent(startFile = File(filePath), onDismiss = { finish() }, onDelete = { finish() })
        }
        // 仅"拍照过渡后进入"时渐入，避免影响相册点击等其他进入方式（保持各自默认动画）
        if (intent?.getBooleanExtra("fade_in", false) == true) {
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}

@Composable
private fun PhotoViewerContent(startFile: File, onDismiss: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // 深色模式：背景改为深灰（莫奈色淡显后叠半透明灰遮罩）
    val theme by AppPrefs.theme.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val isSystemDark = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val isDark = isDarkMode(theme.mode, isSystemDark)
    val darkBase = RetroDarkBg
    // 深色模式下的灰色控件底色（按钮/胶囊），以及白色前景（图标/文字）
    val graySurface = RetroDarkSurface
    val whiteOnDark = Color.White

    // 相册列表（用于左右滑动切换 / 底部缩略图条），按拍照时间倒序
    var photos by remember { mutableStateOf<List<File>>(emptyList()) }
    val pagerState = rememberPagerState(initialPage = 0) { photos.size.coerceAtLeast(1) }
    val thumbListState = rememberLazyListState()
    var isZoomed by remember { mutableStateOf(false) }

    LaunchedEffect(photos) {
        if (photos.isEmpty()) {
            val list = withContext(Dispatchers.IO) { PhotoStorage.listPhotos(context) }
            photos = list
            val idx = list.indexOfFirst { it.absolutePath == startFile.absolutePath }.coerceAtLeast(0)
            pagerState.scrollToPage(idx)
            // 当前缩略图通过 contentPadding.start=48dp 形成"距左一定距离"的效果（而非 scrollOffset 截断，
            // scrollOffset>0 会把照片左侧裁掉导致看起来出屏幕），前一张在左侧微露（胶片条感）
            thumbListState.scrollToItem(idx)
        }
    }
    // 切页时复位缩放，避免上一页的放大状态残留并锁死翻页
    LaunchedEffect(pagerState.currentPage) { isZoomed = false }

    val currentIndex = pagerState.currentPage
    val currentFile = photos.getOrNull(currentIndex) ?: startFile

    // 当前照片：目标背景色 + 位置信息（随翻页更新）
    var bgColorTarget by remember { mutableIntStateOf(RetroPaper.toArgb()) }
    LaunchedEffect(currentFile) { bgColorTarget = extractDominantColor(currentFile, RetroPaper.toArgb()) }
    val bgColor by animateColorAsState(
        targetValue = Color(bgColorTarget),
        animationSpec = tween(durationMillis = 400, easing = LinearEasing),
        label = "bgColor",
    )
    val fgColor = remember(bgColor) { textColorForBg(bgColor.toArgb()) }
    val bgLight = remember(bgColor) { isBgLight(bgColor) }

    // —— 编辑器状态 ——
    var editing by remember { mutableStateOf(false) }
    var workBitmap by remember { mutableStateOf<Bitmap?>(null) }
    // 编辑控件（进入编辑时复位为中性，让用户从零调整）
    var editTint by remember { mutableStateOf(TintState.NONE) }
    var editTintStrength by remember { mutableFloatStateOf(0.5f) }
    var editBw by remember { mutableStateOf(false) }
    var editBwInt by remember { mutableFloatStateOf(0.5f) }
    var editVig by remember { mutableStateOf(false) }
    var editVigInt by remember { mutableFloatStateOf(0.5f) }
    // 编辑器底部菜单当前页：裁切 / 调节 / 滤镜（默认裁切）
    var editTab by remember { mutableStateOf(EditorTab.CROP) }
    // 调节滑块（0~1，0.5 = 中性）：曝光度 / 亮度 / 饱和度
    var editExp by remember { mutableFloatStateOf(0.5f) }
    var editBri by remember { mutableFloatStateOf(0.5f) }
    var editSat by remember { mutableFloatStateOf(0.5f) }
    var imageVersion by remember { mutableIntStateOf(0) }

    // 底部按钮栏：进入时渐入、离场（返回/删除）时渐隐（仅 alpha，淡入即可，不上滑）
    val bottomBarAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        bottomBarAlpha.animateTo(1f, tween(durationMillis = 320))
    }
    /** 离场：底部按钮先渐隐，再执行 finish / 删除等退出动作。 */
    fun requestLeave(action: () -> Unit) {
        scope.launch {
            bottomBarAlpha.animateTo(0f, tween(durationMillis = 200))
            action()
        }
    }

    /** 进入编辑：裁掉拍立得白边得到无边框正方形，存在内存中供预览/处理（原文件不变）。 */
    fun enterEdit() {
        scope.launch {
            val sq = PhotoStorage.cropInnerSquare(currentFile)
            if (sq != null) {
                workBitmap = sq
                editTab = EditorTab.CROP
                editTint = TintState.NONE; editTintStrength = 0.5f
                editBw = false; editBwInt = 0.5f
                editVig = false; editVigInt = 0.5f
                editExp = 0.5f; editBri = 0.5f; editSat = 0.5f
                editing = true
            } else {
                Toast.makeText(context, "无法编辑此照片", Toast.LENGTH_SHORT).show()
            }
        }
    }
    /** 取消编辑：丢弃内存中的无边框图，原文件（带框）保持不变 → 退出时边框自动恢复。 */
    fun exitEdit() {
        editing = false
        workBitmap?.recycle()
        workBitmap = null
    }
    // 系统级返回（手势/物理键）：编辑中拦截并退出编辑，而非关闭整个详情页
    BackHandler(enabled = editing) { exitEdit() }
    /** 保存编辑：对无边框图应用滤镜并重新加回拍立得白边写回原文件，刷新预览缓存与主色。 */
    fun saveEdit() {
        val sq = workBitmap ?: return
        scope.launch {
            PhotoStorage.reencodeWithFrame(context, currentFile, sq, EffectiveFilter(
                grayscale = if (editBw) editBwInt else 0f,
                vignette = if (editVig) editVigInt else 0f,
                exposure = (editExp - 0.5f) * 2f,
                warmth = when (editTint) { TintState.WARM -> editTintStrength; TintState.COOL -> -editTintStrength; else -> 0f },
                saturation = (editSat - 0.5f) * 2f,
                brightness = (editBri - 0.5f) * 2f,
            ))
            sq.recycle()
            workBitmap = null
            editing = false
            imageVersion++
            bgColorTarget = extractDominantColor(currentFile, RetroPaper.toArgb())
        }
    }

    var locationText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(currentFile) { locationText = readLocationFromExif(context, currentFile) }

    val capsuleBg = if (isDark) graySurface else (if (bgLight) RetroCream.copy(alpha = 0.85f) else Color(0x80000000))

    Box(
        modifier = Modifier.fillMaxSize().background(if (isDark) darkBase else bgColor),
    ) {
        if (isDark) {
            // 莫奈取色淡显，再叠半透明灰遮罩 → 整体呈深灰而非刺眼原色
            Box(Modifier.fillMaxSize().background(bgColor.copy(alpha = 0.4f)))
            Box(Modifier.fillMaxSize().background(Color(0xFF1A1A1A).copy(alpha = 0.6f)))
        }

        // 左右滑动切换照片（放大时禁用翻页，改为平移图片）
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isZoomed,
        ) { page ->
            val f = photos.getOrNull(page) ?: startFile
            ZoomablePhoto(
                file = f,
                // 上下非对称 padding（上小下大）让照片视觉中心略偏上，视觉平衡
                modifier = Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 36.dp),
                cacheKey = "${f.path}#$imageVersion",
                onZoomedChange = { isZoomed = it },
            )
        }

        // 顶部返回：深色模式 = 灰底白图标
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 8.dp, top = 8.dp)
                .size(44.dp)
                .clip(CircleShape)
            .background(if (isDark) graySurface else (if (bgLight) RetroCream.copy(alpha = 0.85f) else Color(0x80000000)))
            .clickable(onClick = { requestLeave(onDismiss) }),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = if (isDark) whiteOnDark else fgColor)
        }

        // 顶部时间 + 位置：深色模式统一白色文字
        Column(
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(formatPhotoTime(currentFile.lastModified()), style = MaterialTheme.typography.labelSmall, color = if (isDark) whiteOnDark else fgColor, textAlign = TextAlign.Center)
            if (locationText != null) {
                Text(locationText!!, style = MaterialTheme.typography.labelSmall, color = (if (isDark) whiteOnDark else fgColor).copy(alpha = 0.8f), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 2.dp))
            }
        }

        // 底部：缩略图条 + 三个按钮（进入渐入 / 离场渐隐）
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
            .fillMaxWidth()
            .graphicsLayer {
                alpha = bottomBarAlpha.value
            },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 相邻照片横向缩略图（当前高亮）；点击跳转
            if (photos.size > 1) {
                LazyRow(
                    state = thumbListState,
                    contentPadding = PaddingValues(start = 48.dp, end = 24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items(photos, key = { it.absolutePath }) { f ->
                        val selected = f.absolutePath == currentFile.absolutePath
                        ThumbItem(
                            file = f,
                            selected = selected,
                            isDark = isDark,
                            version = imageVersion,
                            onClick = {
                                scope.launch {
                                    isZoomed = false
                                    pagerState.scrollToPage(photos.indexOfFirst { it.absolutePath == f.absolutePath }.coerceAtLeast(0))
                                }
                            },
                        )
                    }
                }
                // 缩略图随当前页自动把"当前照片"滚到距左侧 48dp 的位置（contentPadding 提供距离，不截断）
                LaunchedEffect(currentIndex) {
                    if (photos.isNotEmpty()) thumbListState.animateScrollToItem(currentIndex)
                }
            }

            Row(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 保存（胶囊组左端，仅图标）
                Row(
                    modifier = Modifier
                        .height(56.dp)
                        .width(80.dp)
                        .clip(RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp, topEnd = 4.dp, bottomEnd = 4.dp))
                        .background(capsuleBg)
                        .clickable { scope.launch { saveToGallery(context, currentFile); Toast.makeText(context, "已保存到系统相册", Toast.LENGTH_SHORT).show() } },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Filled.Download, contentDescription = "保存", modifier = Modifier.size(22.dp), tint = if (isDark) whiteOnDark else fgColor)
                }

                Spacer(Modifier.width(2.dp))

                // 分享（胶囊组中段，仅图标）
                Row(
                    modifier = Modifier
                        .height(56.dp)
                        .width(80.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(capsuleBg)
                        .clickable { scope.launch { shareImage(context, currentFile) } },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Filled.Share, contentDescription = "分享", modifier = Modifier.size(22.dp), tint = if (isDark) whiteOnDark else fgColor)
                }

                Spacer(Modifier.width(2.dp))

                // 编辑（并入胶囊组最右端，仅图标，与保存/分享等宽、同底色）
                Row(
                    modifier = Modifier
                        .height(56.dp)
                        .width(80.dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 28.dp, bottomEnd = 28.dp))
                        .background(capsuleBg)
                        .clickable(onClick = { enterEdit() }),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = "编辑", modifier = Modifier.size(22.dp), tint = if (isDark) whiteOnDark else fgColor)
                }

                Spacer(Modifier.width(16.dp))

                // 删除圆形：深色模式 = 灰底白图标（保留，rust 强调色）
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isDark) graySurface else RetroRust.copy(alpha = 0.15f))
                        .clickable { showDeleteConfirm = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除", tint = if (isDark) whiteOnDark else RetroRust)
                }
            }
        }

        // 编辑态覆盖层（全屏不透明，盖住普通浏览界面）
        if (editing && workBitmap != null) {
            PhotoEditorOverlay(
                frameFile = currentFile,
                bitmap = workBitmap!!,
                isDark = isDark,
                darkBase = darkBase,
                capsuleBg = capsuleBg,
                fgColor = fgColor,
                whiteOnDark = whiteOnDark,
                tintState = editTint,
                onTintChange = { editTint = it },
                tintStrength = editTintStrength,
                onTintStrengthChange = { editTintStrength = it },
                bwOn = editBw, onBwChange = { editBw = it },
                bwInt = editBwInt, onBwIntChange = { editBwInt = it },
                vigOn = editVig, onVigChange = { editVig = it },
                vigInt = editVigInt, onVigIntChange = { editVigInt = it },
                exp = editExp, onExpChange = { editExp = it },
                bri = editBri, onBriChange = { editBri = it },
                sat = editSat, onSatChange = { editSat = it },
                tab = editTab, onTabChange = { editTab = it },
                onCancel = { exitEdit() },
                onSave = { saveEdit() },
            )
        }

    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除照片") },
            text = { Text("确定要删除这张照片吗？") },
            confirmButton = { TextButton(onClick = { requestLeave { currentFile.delete(); onDelete() } }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") } },
        )
    }
}

/** 编辑器底部菜单三页。 */
private enum class EditorTab { CROP, ADJUST, FILTER }

/**
 * 照片编辑器覆盖层（重构版）：
 *  - 顶部：左上返回（与设置返回键一致）、右上保存；
 *  - 进入时：带白框的原图【渐出】，无边框正方形【平滑放大并居中】（ContentScale.Fit，不左右填满）；
 *  - 底部两行：第一行菜单（裁切/调节/滤镜，默认裁切），第二行对应当前页内容（左右居中）。
 * 取消 = 丢弃内存无边框图、原文件（带框）不变 → 退出后边框自动恢复；
 * 保存 = 对无边框图应用滤镜并重新加回白边写回原文件。
 */
@Composable
private fun PhotoEditorOverlay(
    frameFile: File,
    bitmap: Bitmap,
    isDark: Boolean,
    darkBase: Color,
    capsuleBg: Color,
    fgColor: Color,
    whiteOnDark: Color,
    tintState: TintState,
    onTintChange: (TintState) -> Unit,
    tintStrength: Float,
    onTintStrengthChange: (Float) -> Unit,
    bwOn: Boolean, onBwChange: (Boolean) -> Unit, bwInt: Float, onBwIntChange: (Float) -> Unit,
    vigOn: Boolean, onVigChange: (Boolean) -> Unit, vigInt: Float, onVigIntChange: (Float) -> Unit,
    exp: Float, onExpChange: (Float) -> Unit,
    bri: Float, onBriChange: (Float) -> Unit,
    sat: Float, onSatChange: (Float) -> Unit,
    tab: EditorTab, onTabChange: (EditorTab) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    // 由控件合成 EffectiveFilter（与拍照滤镜管线共用 buildFilterColorMatrix / applyFilterToBitmap）
    val eff = EffectiveFilter(
        grayscale = if (bwOn) bwInt else 0f,
        vignette = if (vigOn) vigInt else 0f,
        exposure = (exp - 0.5f) * 2f,
        warmth = when (tintState) { TintState.WARM -> tintStrength; TintState.COOL -> -tintStrength; else -> 0f },
        saturation = (sat - 0.5f) * 2f,
        brightness = (bri - 0.5f) * 2f,
    )
    val composeCm = ColorMatrix(buildFilterColorMatrix(eff).getArray())
    val editorBg = if (isDark) darkBase else Color(0xFF2B2722)
    val fg = if (isDark) whiteOnDark else fgColor
    val sliderColors = SliderDefaults.colors(
        thumbColor = if (isDark) Color.White else RetroRust,
        activeTrackColor = if (isDark) Color(0xFFCFCFCF) else RetroRust,
        inactiveTrackColor = if (isDark) Color(0xFF3A3A3A) else Color(0xFF5A5147),
    )

    // 进入过渡：带框原图渐出(framedAlpha 1→0)、无边框图渐入(photoAlpha 0→1)并平滑放大居中(enterScale 0.82→1)
    val enterScale = remember { Animatable(0.82f) }
    val framedAlpha = remember { Animatable(1f) }
    val photoAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { enterScale.animateTo(1f, tween(durationMillis = 360, easing = FastOutSlowInEasing)) }
        launch { framedAlpha.animateTo(0f, tween(durationMillis = 240)) }
        launch { photoAlpha.animateTo(1f, tween(durationMillis = 300)) }
    }

    Box(Modifier.fillMaxSize().background(editorBg)) {
        // 顶部：左上返回 / 右上保存
        Row(
            Modifier.align(Alignment.TopCenter).statusBarsPadding().fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 返回：与设置页左上返回键一致（圆形 + ArrowBack）
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(capsuleBg)
                    .clickable(onClick = onCancel), contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = fg) }
            // 保存：铁锈红圆形 + 勾
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(RetroRust)
                    .clickable(onClick = onSave), contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Check, contentDescription = "保存", tint = Color.White) }
        }

        // 中部：照片（不左右填满，ContentScale.Fit 居中）
        Box(Modifier.fillMaxSize().padding(top = 72.dp, bottom = 232.dp), contentAlignment = Alignment.Center) {
            // 带白框原图：进入时渐出
            AsyncImage(
                model = frameFile, contentDescription = null, contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = framedAlpha.value },
            )
            // 无边框正方形：渐入 + 平滑放大居中
            Image(
                bitmap = bitmap.asImageBitmap(), contentDescription = "编辑预览", contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    alpha = photoAlpha.value
                    scaleX = enterScale.value; scaleY = enterScale.value
                    colorFilter = ColorFilter.colorMatrix(composeCm)
                },
            )
            // 暗角预览（近似，保存时才烘焙）
            if (vigOn) {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = (vigInt * 0.69f).coerceIn(0f, 1f))),
                        ),
                    ),
                )
            }
            // 裁切页：1:1 固定比例取景框（源已是正方形，仅展示、不可改比例）
            if (tab == EditorTab.CROP) {
                Box(
                    Modifier.aspectRatio(1f).fillMaxSize()
                        .border(2.dp, fg.copy(alpha = 0.55f), RoundedCornerShape(10.dp)),
                )
                Box(
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) { Text("1:1", color = Color.White, fontSize = 13.sp) }
            }
        }

        // 底部：两行（菜单 + 内容）
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 第一行：菜单（裁切 / 调节 / 滤镜），默认裁切
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                EditorTab.values().forEach { t ->
                    val label = when (t) { EditorTab.CROP -> "裁切"; EditorTab.ADJUST -> "调节"; EditorTab.FILTER -> "滤镜" }
                    val selected = tab == t
                    Box(
                        Modifier.padding(horizontal = 6.dp).clip(RoundedCornerShape(18.dp))
                            .background(if (selected) RetroRust else Color.Transparent)
                            .border(1.dp, if (selected) RetroRust else fg.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                            .clickable { onTabChange(t) }.padding(horizontal = 22.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(label, color = if (selected) Color.White else fg, fontSize = 14.sp) }
                }
            }
            // 第二行：对应当前页的内容（左右居中）
            Box(
                Modifier.fillMaxWidth().background(editorBg)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                when (tab) {
                    EditorTab.CROP -> {
                        // 仅 1:1，无可选比例；这里给一句说明（取景框已在中部展示）
                        Text("仅支持 1:1 比例", color = fg.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.align(Alignment.Center))
                    }
                    EditorTab.ADJUST -> {
                        Column(Modifier.fillMaxWidth()) {
                            EditorSlider("曝光度", exp, onExpChange, sliderColors, fg)
                            Spacer(Modifier.height(10.dp))
                            EditorSlider("亮度", bri, onBriChange, sliderColors, fg)
                            Spacer(Modifier.height(10.dp))
                            EditorSlider("饱和度", sat, onSatChange, sliderColors, fg)
                        }
                    }
                    EditorTab.FILTER -> {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            // 调色盘：无 / 暖色 / 冷色（与效果面板一致）
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                EditPill("无", selected = tintState == TintState.NONE, isDark = isDark, onClick = { onTintChange(TintState.NONE) })
                                EditPill("暖色", selected = tintState == TintState.WARM, isDark = isDark, onClick = { onTintChange(TintState.WARM) })
                                EditPill("冷色", selected = tintState == TintState.COOL, isDark = isDark, onClick = { onTintChange(TintState.COOL) })
                            }
                            if (tintState != TintState.NONE) {
                                Slider(value = tintStrength, onValueChange = onTintStrengthChange, valueRange = 0f..1f,
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = sliderColors)
                            }
                            Spacer(Modifier.height(10.dp))
                            // 滤镜开关：黑白 / 暗角 / 亮度（与效果面板一致；亮度与"调节"共用同一亮度值）
                            val briOn = bri != 0.5f
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                EditPill("黑白", selected = bwOn, isDark = isDark, onClick = { onBwChange(!bwOn) })
                                EditPill("暗角", selected = vigOn, isDark = isDark, onClick = { onVigChange(!vigOn) })
                                EditPill("亮度", selected = briOn, isDark = isDark, onClick = { onBriChange(if (briOn) 0.5f else 0.68f) })
                            }
                            if (bwOn) Slider(value = bwInt, onValueChange = onBwIntChange, valueRange = 0f..1f, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = sliderColors)
                            if (vigOn) Slider(value = vigInt, onValueChange = onVigIntChange, valueRange = 0f..1f, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), colors = sliderColors)
                            if (briOn) Slider(value = bri, onValueChange = onBriChange, valueRange = 0f..1f, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), colors = sliderColors)
                        }
                    }
                }
            }
        }
    }
}

/** 调节页的单行滑块：标签 + 滑条。 */
@Composable
private fun EditorSlider(label: String, value: Float, onValueChange: (Float) -> Unit, colors: SliderColors, labelColor: Color) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = labelColor, fontSize = 14.sp, modifier = Modifier.width(64.dp))
        Slider(value = value, onValueChange = onValueChange, valueRange = 0f..1f, modifier = Modifier.weight(1f), colors = colors)
    }
}

/** 编辑控件用的小药丸按钮（选中=铁锈红填充，未选=半透明底）。 */
@Composable
private fun EditPill(text: String, selected: Boolean, isDark: Boolean, onClick: () -> Unit) {
    val bg = if (selected) RetroRust.copy(alpha = 0.85f) else (if (isDark) RetroDarkSurface else Color(0x33000000))
    val fg = if (selected) Color.White else (if (isDark) Color.White else RetroBrown)
    Box(
        Modifier.clip(RoundedCornerShape(16.dp)).background(bg).clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = MaterialTheme.typography.labelMedium, color = fg) }
}

/** 底部缩略图：按照片真实比例（非正方形）显示；选中态描边随深浅色（深色=灰、浅色=深棕）。 */
@Composable
private fun ThumbItem(file: File, selected: Boolean, isDark: Boolean, version: Int = 0, onClick: () -> Unit) {
    // 提前读取文件的真实宽高比（仅读头部，毫秒级），使占位格在图片渲染前就是正确的比例
    val fileRatio = remember(file) {
        try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, opts)
            if (opts.outWidth > 0 && opts.outHeight > 0) opts.outWidth.toFloat() / opts.outHeight.toFloat() else 1f
        } catch (_: Exception) { 1f }
    }
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(PhotoStorage.thumbnailFor(file))
            .size(PhotoStorage.THUMB_SIZE)
            .crossfade(300)
            .memoryCacheKey("${file.path}#$version")
            .build(),
        contentDescription = null,
    ) {
        val imageRatio = painter?.intrinsicSize?.let { if (it.width > 0f && it.height > 0f) it.width / it.height else null }
        // 优先用 painter 加载后的精确比例，否则用文件头读到的比例
        val ratio = imageRatio ?: fileRatio
        val imagePainter = painter
        val borderColor = if (isDark) Color.Gray else RetroBrown
        Box(
            Modifier
                .padding(horizontal = 4.dp)
                .height(56.dp)
                .aspectRatio(ratio)
                .background(if (isDark) RetroDarkSurface else Color(0x33000000))
                .border(if (selected) 2.dp else 0.dp, if (selected) borderColor else Color.Transparent, RoundedCornerShape(0.dp))
                .clickable(onClick = onClick),
        ) {
            if (imagePainter != null) {
                Image(painter = imagePainter, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            if (isDark) {
                Box(Modifier.fillMaxSize().background(Color(0x1AFFFFFF)))
            }
        }
    }
}

/**
 * 可双指捏合缩放 / 单指拖拽平移 / 双指旋转的照片。scale 限制在 1x~4x，归一时复位平移。
 * 通过 onZoomedChange 把"是否处于放大态"上抛给父级，用于放大时禁用 ViewPager 翻页。
 */
@Composable
private fun ZoomablePhoto(file: File, modifier: Modifier = Modifier, cacheKey: String? = null, onZoomedChange: (Boolean) -> Unit = {}) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    val state = rememberTransformableState { zoomChange, panChange, rotationChange ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        if (scale > 1f) {
            offsetX += panChange.x
            offsetY += panChange.y
        } else {
            offsetX = 0f; offsetY = 0f
        }
        rotation += rotationChange
        onZoomedChange(scale > 1.001f)
    }
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(file).crossfade(true)
            .memoryCacheKey(cacheKey ?: file.absolutePath).build(),
        contentDescription = "查看照片",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY, rotationZ = rotation)
            // 未放大时禁用 transformable（不消费手势），横滑全部交予 HorizontalPager 切换照片；
            // 放大后才启用 transformable，同时 Pager 由 userScrollEnabled=false 禁用翻页
            .transformable(state = state, enabled = scale > 1.001f)
            // 双击切换 1x ↔ 2.5x（未放大时也可双击进入放大态，绕开 transformable 禁用的 pinch）
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.001f) {
                            scale = 1f; offsetX = 0f; offsetY = 0f
                        } else {
                            scale = 2.5f
                        }
                        onZoomedChange(scale > 1.001f)
                    },
                )
            },
    )
}

private suspend fun saveToGalleryWithoutLocation(context: Context, file: File) {
    withContext(Dispatchers.IO) {
        try {
            val tempFile = File(context.cacheDir, "stripped_${file.name}")
            file.copyTo(tempFile, overwrite = true)
            val exif = androidx.exifinterface.media.ExifInterface(tempFile.path)
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE, null)
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE_REF, null)
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE, null)
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE_REF, null)
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE, null)
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE_REF, null)
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_TIMESTAMP, null)
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_DATESTAMP, null)
            exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_GPS_PROCESSING_METHOD, null)
            exif.saveAttributes()

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext
            context.contentResolver.openOutputStream(uri)?.use { out ->
                tempFile.inputStream().use { inp -> inp.copyTo(out) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            }
            tempFile.delete()
        } catch (_: Exception) {}
    }
}

private suspend fun saveToGallery(context: Context, file: File) {
    withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                // 直接拷贝字节，保留原图 EXIF（含拍照时写入的 GPS 位置），不再重新编码
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

/**
 * 系统级分享：把照片 file 通过 FileProvider 暴露为 content:// URI，再用 Intent.ACTION_SEND 唤起系统分享面板。
 * 用户可选择 微信/QQ/邮件/蓝牙 等任意目标 App，原图直传、不需要先拷贝到相册。
 *
 * 注意：
 * ① authorities 须与 AndroidManifest 里 FileProvider 的 ${applicationId}.fileprovider 一致；
 * ② Intent 必须 addFlags(FLAG_GRANT_READ_URI_PERMISSION) 才能让目标 App 读取 content URI；
 * ③ 用 createChooser 强制显示选择器，用户体验更可控；
 * ④ 不在主线程做 IO；FileProvider.getUriForFile 本身很快（只查 manifest），故放主线程也安全。
 */
/**
 * 系统级分享：先制作「去掉位置信息」的副本，再通过 FileProvider 分享该副本，保护隐私。
 * 见文件顶部 shareImage 注释（authorities / FLAG_GRANT_READ_URI_PERMISSION / createChooser）。
 */
private suspend fun shareImage(context: Context, file: File) {
    val shareFile = withContext(Dispatchers.IO) { PhotoStorage.copyWithoutLocation(context, file) }
    try {
        val authority = "${context.packageName}.fileprovider"
        val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, shareFile)
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
