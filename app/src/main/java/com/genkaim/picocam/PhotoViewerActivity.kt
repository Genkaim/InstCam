package com.genkaim.picocam

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.ColorMatrixColorFilter
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.viewinterop.AndroidView
import com.canhub.cropper.CropImageView
import com.canhub.cropper.CropImageOptions
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
import com.genkaim.picocam.ui.components.ColorSquare
import com.genkaim.picocam.ui.components.TintArrow
import com.genkaim.picocam.dynamic.AppPrefs
import com.genkaim.picocam.dynamic.isDarkMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

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

/** 原图备份路径：同目录、扩展名前加 .orig（用于非破坏式编辑复原，需求②）。 */
private fun origBackupOf(file: File): File =
    File(file.parent, "${file.nameWithoutExtension}.orig.${file.extension}")

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

/** 颜色叠加：在 base 之上以 alpha 覆盖 over，返回合成色（用于计算深色模式实际背景合成色）。 */
private fun blendOver(base: Color, over: Color, alpha: Float): Color {
    val a = alpha.coerceIn(0f, 1f)
    return Color(
        red = base.red * (1f - a) + over.red * a,
        green = base.green * (1f - a) + over.green * a,
        blue = base.blue * (1f - a) + over.blue * a,
        alpha = 1f,
    )
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

    // —— 编辑器状态（统一用 EditState 快照，便于撤销/重做）——
    var editing by remember { mutableStateOf(false) }
    // 编辑用工作位图（MutableState，便于编辑层直接回写裁切结果，使其它栏/保存同步）
    var workBitmap = remember { mutableStateOf<Bitmap?>(null) }
    // CanHub 裁切视图句柄（CROP 页内嵌的 CropImageView），供保存时取裁切结果
    var cropViewState = remember { mutableStateOf<CropImageView?>(null) }
    // 编辑控件（进入编辑时复位为中性，让用户从零调整）；所有可调项都收进 EditState
    var editState by remember { mutableStateOf(EditState()) }
    var imageVersion by remember { mutableIntStateOf(0) }
    // 历史栈：history[0] = 进入编辑时的中性状态；每次操作 checkpoint 推入新快照
    var history by remember { mutableStateOf(listOf(EditState())) }
    var historyIndex by remember { mutableIntStateOf(0) }

    /** 推入一次历史检查点：与当前栈顶相同则跳过（避免重复）。 */
    fun checkpoint() {
        val cur = editState
        if (historyIndex in history.indices && history[historyIndex] == cur) return
        val truncated = if (historyIndex < history.size - 1) history.subList(0, historyIndex + 1) else history
        history = truncated + cur
        historyIndex = history.size - 1
    }
    fun undo() { if (historyIndex > 0) { historyIndex--; editState = history[historyIndex] } }
    fun redo() { if (historyIndex < history.size - 1) { historyIndex++; editState = history[historyIndex] } }

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

    /** 进入编辑：取无边框正方形；若拍照时应用了滤镜，则优先用"无滤镜原图侧车"作基图，
     *  并默认选中拍照滤镜（基图未滤镜 + 叠加同一滤镜 = 与相册照片一致，避免双重叠加滤镜）。 */
    fun enterEdit() {
        scope.launch {
            val base = PhotoStorage.loadSourceSquare(currentFile) ?: PhotoStorage.cropInnerSquare(currentFile)
            if (base != null) {
                workBitmap.value = base
                // 编辑默认选中拍照时应用的滤镜：基图用无滤镜原图，叠加同一滤镜 → 与相册照片一致
                val meta = PhotoStorage.loadFilterMeta(currentFile)
                editState = if (meta != null) {
                    val tintStrengthEff = when (meta.tint) {
                        TintState.WARM -> meta.tintStrength
                        TintState.COOL -> -meta.tintStrength
                        else -> 0f
                    }
                    EditState(
                        tint = meta.tint,
                        tintSat = meta.tintSat,
                        tintBri = 0.5f,   // 色盘亮度(Y)已并入相机 exposure，归零避免与 exp 双计（见 toEffectiveFilter）
                        tintStrength = meta.tintStrength,
                        vig = meta.vignette > 0.001f,
                        vigInt = meta.vignette.coerceIn(0f, 1f),
                        exp = (meta.exposure / 2f + 0.5f).coerceIn(0f, 1f),   // 承载相机合并后的完整曝光（含亮度滤镜+色盘Y）
                        sat = 0.5f,
                        con = (meta.contrast / 2f + 0.5f).coerceIn(0f, 1f),
                    )
                } else EditState()
                history = listOf(editState); historyIndex = 0
                editing = true
            } else {
                Toast.makeText(context, "无法编辑此照片", Toast.LENGTH_SHORT).show()
            }
        }
    }
    /** 取消编辑：丢弃内存中的无边框图，原文件（带框）保持不变 → 退出时边框自动恢复。 */
    fun exitEdit() {
        editing = false
        workBitmap.value?.recycle()
        workBitmap.value = null
    }
    /** 保存编辑：对无边框图应用滤镜（含旋转裁方）并重新加回拍立得白边写回原文件，刷新预览缓存与主色。 */
    fun saveEdit() {
        val view = cropViewState.value ?: run {
            // 没有裁切视图（理论上不会发生）→ 直接退出编辑
            editing = false
            return
        }
        val sq = workBitmap.value ?: return
        // 非破坏式：首次保存前备份原图（带框）到同目录 .orig 副本，便于复原（需求②）
        val backup = origBackupOf(currentFile)
        if (!backup.exists()) { try { currentFile.copyTo(backup, overwrite = false) } catch (_: Exception) {} }
        scope.launch {
            val s = editState
            // 裁切/旋转由 CanHub CropImageView 内部完成，getCroppedImage 直接得到 1:1 结果
            val cropped = view.getCroppedImage()
            val base = cropped ?: sq
            // 滤镜状态合成：与相机拍照直出一致（见 EditState.toEffectiveFilter）
            val eff = s.toEffectiveFilter()
            PhotoStorage.reencodeWithFrame(context, currentFile, base, eff)
            if (cropped != null && cropped !== sq) cropped.recycle()
            sq.recycle()
            workBitmap.value = null
            editing = false
            imageVersion++
            // 同步滤镜元信息：使下次编辑默认选中当前滤镜（与相册照片一致）
            if (s.tint != TintState.NONE) {
                PhotoStorage.saveFilterMeta(currentFile, eff, s.tint, s.tintSat, s.tintBri, s.tintStrength)
            } else {
                PhotoStorage.clearFilterMeta(currentFile)
            }
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
        if (editing && workBitmap.value != null) {
            PhotoEditorOverlay(
                state = editState,
                onStateChange = { editState = it },
                onCommit = { checkpoint() },
                onUndo = { undo() },
                onRedo = { redo() },
                canUndo = historyIndex > 0,
                canRedo = historyIndex < history.size - 1,
                onCancel = { exitEdit() },
                onSave = { saveEdit() },
                onRestoreOriginal = {
                    // 用首次保存前备份的原图覆盖当前文件，刷新预览后退出编辑（需求②）
                    val b = origBackupOf(currentFile)
                    if (b.exists()) { b.copyTo(currentFile, overwrite = true) }
                    imageVersion++
                    exitEdit()
                },
                cropViewState = cropViewState,
                frameFile = currentFile,
                bitmapState = workBitmap,
                isDark = isDark,
                darkBase = darkBase,
                bgColor = bgColor,
                capsuleBg = capsuleBg,
                fgColor = fgColor,
                whiteOnDark = whiteOnDark,
            )
        }

    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除照片") },
            text = { Text("确定要删除这张照片吗？") },
            confirmButton = { TextButton(onClick = {
                showDeleteConfirm = false
                val idx = pagerState.currentPage
                val oldSize = photos.size
                val deleted = currentFile
                val newList = photos.filter { it.absolutePath != deleted.absolutePath }
                scope.launch {
                    // 仅当相册删空才退出主界面；否则切换到相邻照片（不退出）
                    if (newList.isEmpty()) {
                        PhotoStorage.deletePhotoWithSidecars(deleted)
                        photos = emptyList()
                        onDelete()
                    } else {
                        // 删除后目标位置（新列表索引）：删的是末张→落回前一张；否则→下一张滑入中央
                        val wasLast = idx >= oldSize - 1
                        val targetInNew = if (wasLast) (idx - 1).coerceAtLeast(0) else idx.coerceAtMost(newList.size - 1)
                        // 旧列表中对应位置（先滑入，旧列表仍显示，避免跳变）
                        val slideToOld = if (wasLast) (idx - 1).coerceAtLeast(0) else (idx + 1).coerceAtMost(oldSize - 1)
                        pagerState.animateScrollToPage(
                            slideToOld,
                            animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
                        )
                        PhotoStorage.deletePhotoWithSidecars(deleted)
                        photos = newList
                        // 旧列表目标位 == 新列表目标位（同一文件），瞬间定位无跳变
                        pagerState.scrollToPage(targetInNew)
                    }
                }
            }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") } },
        )
    }
}

/** 编辑器底部菜单三页。 */
private enum class EditorTab { CROP, ADJUST, FILTER }

/**
 * 编辑器完整状态快照：撤销/重做只需整体替换此对象。
 * 包含所有可调项：当前页 / 冷暖色调 / 取色盘位置(tintSat,tintBri) / 各滤镜开关与强度 /
 * 调节滑块 / 旋转角度。裁切手势由 CanHub CropImageView 内部接管（不进快照）。
 */
private data class EditState(
    val tab: EditorTab = EditorTab.CROP,
    val tint: TintState = TintState.NONE,
    val tintStrength: Float = 0.5f,
    val tintSat: Float = 0.5f,
    val tintBri: Float = 0.5f,
    val vig: Boolean = false,
    val vigInt: Float = 0.5f,
    val exp: Float = 0.5f,
    val sat: Float = 0.5f,
    val con: Float = 0.5f,     // 对比度：0.5=中性
    val rotation: Float = 0f,
)

/**
 * 把编辑态合成为与相机拍照直出【完全一致】的 EffectiveFilter。
 *
 * 关键对齐（修复"相册改滤镜与拍照拍出来不一致"）：相机侧 [CameraViewModel.effective] 中
 * 暖/冷色盘的两个轴贡献为：
 *   - X(饱和度) → saturation 通道，系数 SAT_SCALE=1；
 *   - Y(明暗)   → exposure 通道（与"亮度"滤镜合并），系数 BRIGHT_SCALE=0.5；
 *   - 相机【从不】写 brightness 通道（亮度走 exposure）。
 * 故这里把色盘 Y 并入 exposure、系数 0.5，brightness 恒为 0，保证同一滤镜在相册编辑与拍照直出像素一致。
 * 相册独立的"亮度"滑块(bri)也映射到 exposure（对应相机"亮度"滤镜），保持通道统一。
 */
private fun EditState.toEffectiveFilter(): EffectiveFilter {
    val tintSatC = if (tint != TintState.NONE) (tintSat - 0.5f) * 2f else 0f            // SAT_SCALE=1
    val tintBriC = if (tint != TintState.NONE) (0.5f - tintBri) * 2f * 0.5f else 0f    // BRIGHT_SCALE=0.5，并入 exposure
        return EffectiveFilter(
        vignette = if (vig) vigInt else 0f,
        exposure = (exp - 0.5f) * 2f + tintBriC,
        warmth = (when (tint) { TintState.WARM -> tintStrength; TintState.COOL -> -tintStrength; else -> 0f }),
        saturation = (sat - 0.5f) * 2f + tintSatC,
        brightness = 0f,   // 与相机一致：相机从不设置 brightness 通道
        contrast = (con - 0.5f) * 2f,
    )
}

/**
 * 照片编辑器覆盖层：
 *  - 顶部：左上返回 / 右侧 撤销·重做·保存；进入与退出均带淡入淡出（顶栏淡、底栏淡入+上滑）。
 *  - 进入时：带白框原图先【渐出】；白框消失后，无边框正方形再【淡入并平滑放大居中】（沿用原动画，保持不变）。
 *  - 中部照片：占据顶/底面板之间剩余空间；CROP 页可拖拽平移、叠加自定义正方形取景框。
 *  - 底部面板：上半 = 当前页参数、下半 = 菜单栏（左对齐、实底背景、选中=RetroRust）。
 * 退出（返回/保存）先走离场动画（leaving 标记），动画结束后才回调 onCancel/onSave。
 */
@Composable
private fun PhotoEditorOverlay(
    state: EditState,
    onStateChange: (EditState) -> Unit,
    onCommit: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onRestoreOriginal: () -> Unit,
    cropViewState: MutableState<CropImageView?>,
    frameFile: File,
    bitmapState: MutableState<Bitmap?>,
    isDark: Boolean,
    darkBase: Color,
    bgColor: Color,
    capsuleBg: Color,
    fgColor: Color,
    whiteOnDark: Color,
) {
    // 由控件合成 EffectiveFilter（与相机拍照直出一致，见 EditState.toEffectiveFilter）
    val eff = state.toEffectiveFilter()
    val androidMatrix = buildFilterColorMatrix(eff)
    val composeCm = ColorMatrix(androidMatrix.getArray())
    val fg = if (isDark) whiteOnDark else fgColor
    // 裁切页滤镜预览：CanHub CropImageView 内部含一个真正的 ImageView(R.id.ImageView_image, 见 4.7.0 源码)，
    // 直接给这个 ImageView 设置 colorFilter 即可在裁切页显示滤镜——只作用于图片、不影响裁切遮罩/白框，
    // 且是 API1+ 稳定 API（此前用的 RenderEffect 需 API31+ 且作用于整个 ViewGroup，实测在部分机型不生效）。
    // colorFilter 仅改显示、不改底层 Bitmap，故 getCroppedImage() 仍返回未滤镜图，saveEdit 不会二次叠加。
    // 用 remember 缓存 matrix 数组的内容哈希，避免每次重组都新建 ColorMatrix 导致 LaunchedEffect 空转。
    val matrixKey = remember(androidMatrix) { androidMatrix.getArray().toList() }
    LaunchedEffect(matrixKey, cropViewState.value) {
        val view = cropViewState.value ?: return@LaunchedEffect
        val cf = if (eff.isIdentity()) null else ColorMatrixColorFilter(androidMatrix)
        // 递归找到内部 ImageView 应用滤镜（imageView 为 private，故按类型遍历子 view）
        fun applyCF(v: View) {
            when (v) {
                is ImageView -> v.colorFilter = cf
                is ViewGroup -> for (i in 0 until v.childCount) applyCF(v.getChildAt(i))
            }
        }
        applyCF(view)
    }
    // 滤镜页左右遮罩应使用的背景色：深色模式须叠加两层遮罩后的实际合成色，否则遮罩与页面背景有色差
    val maskColor = if (isDark) {
        val c1 = blendOver(darkBase, bgColor, 0.4f)
        blendOver(c1, Color(0xFF1A1A1A), 0.6f)
    } else bgColor
    val sliderColors = SliderDefaults.colors(
        thumbColor = if (isDark) Color.White else RetroRust,
        activeTrackColor = if (isDark) Color(0xFFCFCFCF) else RetroRust,
        inactiveTrackColor = if (isDark) Color(0xFF3A3A3A) else Color(0xFF5A5147),
    )

    // 进入过渡（分三段）：
    //  1) 画面先呈现"带白框原图"(framedAlpha=1)，随后白框淡出(framedAlpha 1→0)，无边框方图交叉淡入(photoAlpha 0→1)
    //  2) 白框消失后，中心正方形照片"微微放大"(enterScale 0.9→1)
    //  3) 放大过程中，裁切白框(cropAlpha)、顶栏(topAlpha)、底部菜单(bottomAlpha/bottomOffset) 同步浮现
    // 进入过渡（分三段）：
    //  1) 带白框原图先稳定显示（待加载完成，避免"闪一下"），随后淡出；无边框方图交叉淡入
    //  2) 白框消失后，中心正方形照片"微微放大"(enterScale 0.9→1)
    //  3) 放大过程中，裁切白框(cropAlpha)、顶栏、底部菜单同步浮现
    val enterScale = remember { Animatable(0.9f) }
    val framedAlpha = remember { Animatable(0f) }
    val photoAlpha = remember { Animatable(0f) }
    val cropAlpha = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(1f) }       // 整个编辑层退出时整体渐出（需求⑦）
    // 切页平滑过渡：CROP / FILTER 两页照片始终挂载并交叉淡入淡出（避免重建 CropImageView 的闪动，需求⑧）
    val cropPageAlpha by animateFloatAsState(if (state.tab == EditorTab.CROP) 1f else 0f, tween(350), label = "cropPage")
    val photoPageAlpha by animateFloatAsState(if (state.tab == EditorTab.CROP) 0f else 1f, tween(350), label = "photoPage")
    // 滤镜页整体预览元素略微缩小（宽度更窄），与切页动画同步过渡
    val previewScale by animateFloatAsState(if (state.tab == EditorTab.FILTER) 0.88f else 1f, tween(350), label = "previewScale")
    // 滤镜切换的模糊过渡改为按离中心距离逐页应用（见 EditorFilterPanel），此处不再做整页模糊
    // 顶栏 / 底栏 进入、离场动画（Animatable）
    val topAlpha = remember { Animatable(0f) }
    val bottomAlpha = remember { Animatable(0f) }
    val bottomOffset = remember { Animatable(48f) }
    var leaving by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showDiscard by remember { mutableStateOf(false) }
    var framedReady by remember { mutableStateOf(false) }   // 带框原图是否加载完成
    // 旋转滑块当前角度（CROP 页）：直接驱动 CanHub 原生 setRotatedDegrees，库自动外切覆盖裁切框
    var cropRotation by remember { mutableFloatStateOf(0f) }
    // 当前 CropImageView 实际显示的位图（可能已裁切/旋转）；bitmapState 为"提交给其它栏/保存"的基图
    val liveBitmap = remember { mutableStateOf(bitmapState.value!!) }
    // 旋转烘焙相关状态：rotSource = 旋转基准图（可能是原基图或"裁切后"的裁切结果）；
    // rotOwned = rotSource 是否为本次新生成的裁切图（需回收）；lastRotBmp = 上一帧旋转烘焙图；lastDeg = 上次已烘焙的整数角。
    var rotSource by remember { mutableStateOf<Bitmap?>(null) }
    var rotOwned by remember { mutableStateOf(false) }
    var lastRotBmp by remember { mutableStateOf<Bitmap?>(null) }
    var lastDeg by remember { mutableStateOf(0) }
    // 裁切框在 CropImageView 视图坐标里的矩形（供白框置顶重绘用），随拖动/旋转/复位/首次布局实时更新。
    // 用 post() 在布局完成后取值，避免首帧视图尺寸=0 导致坐标错位（此前"白框跑到屏幕底部"的根因）。
    var cropWin by remember { mutableStateOf(RectF()) }
    /** 提交旋转（离开 CROP 页或保存前调用）：把"裁切+旋转"结果烘焙为新的基图喂给 CanHub，白框复位为满框。 */
    fun commitRotationIfNeeded() {
        val view = cropViewState.value ?: return
        lastRotBmp?.let { if (it !== rotSource && it !== liveBitmap.value) it.recycle() }
        lastRotBmp = null
        if (rotSource != null) {
            if (cropRotation != 0f) {
                val final = rotateCover(rotSource!!, cropRotation)
                if (rotOwned) rotSource?.recycle()
                liveBitmap.value = final
                view.setImageBitmap(final)
            } else {
                // 角为 0 但已捕获裁切基准：把裁切结果作为基图，供其它编辑页显示裁切后的图片
                liveBitmap.value = rotSource!!
                view.setImageBitmap(rotSource!!)
            }
            rotSource = null; rotOwned = false
        } else {
            // 无旋转：若裁切框非满，提交裁切结果作为基图（保持原行为）
            val r = view.cropRect
            val base = liveBitmap.value
            val nonFull = r != null && (r.left != 0 || r.top != 0 || r.right != base.width || r.bottom != base.height)
            if (nonFull) {
                val c = view.getCroppedImage()
                if (c != null && c !== bitmapState.value) {
                    liveBitmap.value = c; view.setImageBitmap(c)
                }
            }
        }
        cropRotation = 0f; lastDeg = 0
    }
    // 离开"裁切"页：把当前裁切(含旋转)结果烘焙进 liveBitmap，使其它编辑页(ADJUST/FILTER)显示裁切后的图片。
    // 保存仍由 getCroppedImage() 执行真正裁切。
    var prevTab by remember { mutableStateOf(state.tab) }
    LaunchedEffect(state.tab) {
        if (prevTab == EditorTab.CROP && state.tab != EditorTab.CROP) {
            commitRotationIfNeeded()
        }
        prevTab = state.tab
    }

    // 进入动画：待带框原图加载完成后才启动（避免异步加载导致的"闪一下"，需求①）
    LaunchedEffect(framedReady) {
        if (!framedReady) return@LaunchedEffect
        launch { framedAlpha.animateTo(0f, tween(durationMillis = 260)) }
        launch { delay(140); photoAlpha.animateTo(1f, tween(durationMillis = 240)) }
        launch {
            delay(240)
            launch { enterScale.animateTo(1f, tween(durationMillis = 420, easing = FastOutSlowInEasing)) }
            launch { cropAlpha.animateTo(1f, tween(durationMillis = 360)) }
            launch { topAlpha.animateTo(1f, tween(durationMillis = 360)) }
            launch { bottomAlpha.animateTo(1f, tween(durationMillis = 380)) }
            launch { bottomOffset.animateTo(0f, tween(durationMillis = 380, easing = FastOutSlowInEasing)) }
        }
    }
    // 兜底：若带框原图迟迟未加载（异常），也启动进入动画
    LaunchedEffect(Unit) { delay(600); if (!framedReady) framedReady = true }
    LaunchedEffect(leaving) {
        if (leaving) {
            launch { contentAlpha.animateTo(0f, tween(durationMillis = 220)) }
            launch { topAlpha.animateTo(0f, tween(durationMillis = 200)) }
            launch { bottomAlpha.animateTo(0f, tween(durationMillis = 200)) }
            launch { bottomOffset.animateTo(48f, tween(durationMillis = 200)) }
            delay(220)
            pendingAction?.invoke()
            leaving = false
            pendingAction = null
        }
    }

    fun requestExit(action: () -> Unit) { pendingAction = action; leaving = true }
    fun handleBack() {
        if (canUndo) showDiscard = true else requestExit(onCancel)
    }

    // 编辑中拦截系统返回：已修改则弹确认框，否则淡出退出
    BackHandler(enabled = !leaving) { handleBack() }

    Box(Modifier.fillMaxSize().background(if (isDark) darkBase else bgColor).graphicsLayer { alpha = contentAlpha.value }
        .pointerInput(Unit) { }) {
        // 与照片详情页背景保持一致：深色模式先叠取色淡显 + 半透明灰遮罩
        if (isDark) {
            Box(Modifier.fillMaxSize().background(bgColor.copy(alpha = 0.4f)))
            Box(Modifier.fillMaxSize().background(Color(0xFF1A1A1A).copy(alpha = 0.6f)))
        }

        // 进入时"带白框原图"：与相册详情页 ZoomablePhoto 完全一致的尺寸/定位（同一文件、Fit、相同内边距），
        // 作为背景层在顶/底控件之下，与无边框预览做交叉淡入（带框先渐出 → 无边框方图渐入）。
        AsyncImage(
            model = frameFile, contentDescription = null, contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
                .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 36.dp)
                .graphicsLayer { alpha = framedAlpha.value },
            onSuccess = { framedReady = true },
        )

        Column(Modifier.fillMaxSize()) {
            // 顶栏：左上返回 / 右侧 撤销、重做、保存
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 8.dp)
                    .graphicsLayer { alpha = topAlpha.value },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 返回：与相册详情页返回键完全一致（圆形 + ArrowBack，背景 capsuleBg、图标色 whiteOnDark/fgColor）
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(capsuleBg)
                        .clickable(onClick = { handleBack() }), contentAlignment = Alignment.Center,
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = if (isDark) whiteOnDark else fgColor) }
                // 右侧：撤销 / 重做 / 保存
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp)
                            .clickable(enabled = canUndo) { onUndo() }, contentAlignment = Alignment.Center,
                    ) { Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "撤销", tint = if (canUndo) fg else fg.copy(alpha = 0.35f)) }
                    Box(
                        Modifier.size(40.dp)
                            .clickable(enabled = canRedo) { onRedo() }, contentAlignment = Alignment.Center,
                    ) { Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "重做", tint = if (canRedo) fg else fg.copy(alpha = 0.35f)) }
                    // 保存：铁锈红圆形 + 勾
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(RetroRust)
                            .clickable(onClick = { requestExit { commitRotationIfNeeded(); onSave() } }), contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Check, contentDescription = "保存", tint = Color.White) }
                }
            }

            // 中部：照片占据顶/底面板之间的剩余空间；不左右沾满（左右内边距），大小随底栏高度自适应
            Box(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                // 照片层 + 裁切指示层：CROP 页用 CanHub CropImageView（照片 + 灰遮罩 + 白框一体，手势内部接管）。
                // 通过 cropPageAlpha 与 FILTER 页的照片交叉淡入淡出（需求⑧），切页平滑无闪。
                // 分层（由下到上，需求④）：根背景色 → 照片层 → 裁切指示层(CanHub 灰遮罩+白框) → 交互层(顶/底栏)。
                // 关键：内外层 Box 用 aspectRatio(1f) 限制为 1:1 方框 → CanHub 暗色遮罩只覆盖照片区域，不影响背景；
                // clip 使旋转放大后的内容不溢出到其它 UI
                Box(Modifier.fillMaxSize().aspectRatio(1f)
                    .graphicsLayer { scaleX = previewScale; scaleY = previewScale }
                    .clipToBounds()) {
                    AndroidView(
                        factory = { ctx ->
                        CropImageView(ctx).apply {
                            setImageCropOptions(buildCropOptions(ctx))
                            // 裁切框随拖动/旋转实时变化 → 同步到 cropWin，触发白框置顶重绘
                            setOnCropWindowChangedListener { cropWin = cropWindowRect ?: RectF() }
                            cropViewState.value = this
                        }
                        },
                        modifier = Modifier.fillMaxSize()
                            .graphicsLayer {
                                alpha = cropPageAlpha * cropAlpha.value
                            },
                    )
                    // 进入编辑/切换照片/提交裁切时：复位旋转并把当前基图喂给 CropImageView（仅基图变化时设置一次）
                    LaunchedEffect(bitmapState.value) {
                        val bmp = bitmapState.value ?: return@LaunchedEffect
                        cropRotation = 0f
                        rotSource = null; rotOwned = false; lastRotBmp = null; lastDeg = 0
                        liveBitmap.value = bmp
                        // setImageBitmap 复位为满框（白框满框、不旋转）；旋转改由手动 rotateCover 烘焙
                        cropViewState.value?.setImageBitmap(liveBitmap.value)
                        // 布局完成后读取一次裁切框坐标（post 确保视图已测量，避免首帧错位），同步白框置顶
                        val cv = cropViewState.value
                        cv?.post { cropWin = cv.cropWindowRect ?: RectF() }
                    }
                    // 非 CROP 页照片层：无边框方图（白框消失后淡入 + 微微放大 + 滤镜预览）；显示"已提交裁切"的基图
                    Image(
                        // 其它编辑页(ADJUST/FILTER)显示【裁切后】的图片：离开 CROP 页时 commitRotationIfNeeded()
                        // 已把裁切/旋转结果烘焙进 liveBitmap；保存仍由 getCroppedImage() 真正裁切。用 liveBitmap 而非原图。
                        bitmap = liveBitmap.value.asImageBitmap(), contentDescription = "编辑预览", contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            alpha = photoPageAlpha * photoAlpha.value
                            scaleX = enterScale.value; scaleY = enterScale.value
                            colorFilter = ColorFilter.colorMatrix(composeCm)
                        },
                    )
                    // 暗角预览：所有页（含 CROP）统一叠加同一径向暗角层，半径=半对角线、stops 与落盘 applyFilterToBitmap
                    // 完全一致，确保裁切/调节/滤镜三页所见一致、且与成片一致（修复"裁切时暗角不生效"）。
                    // 白框置顶：白框在暗角层【之上】重绘（见下方 CROP 专属 Box），故暗角不会压暗白框。
                    if (state.vig) {
                        val vigAlpha = (state.vigInt * 0xD9 / 0xFF).coerceIn(0f, 1f)   // = vigInt * 217/255 ≈ vigInt*0.85，与保存端一致
                        Box(
                            Modifier.fillMaxSize()
                                .graphicsLayer { alpha = maxOf(cropPageAlpha * cropAlpha.value, photoPageAlpha * photoAlpha.value) }
                                .drawBehind {
                                    drawRect(
                                        Brush.radialGradient(
                                            colorStops = arrayOf(
                                                0f to Color.Transparent,
                                                0.4f to Color.Transparent,
                                                1f to Color.Black.copy(alpha = vigAlpha),
                                            ),
                                            center = center,
                                            radius = (size.minDimension / 2f) * 1.41421356f,   // 半对角线（正方形），使 100% 落在角上
                                        ),
                                    )
                                },
                            )
                        }
                    // 白色指示框置顶（仅 CROP 页）：在暗角层【之上】重绘白框 + 3×3 网格 + 四角加粗，
                    // 使裁切指示始终清晰（层级：白框 > 暗角 > 照片）。
                    // 关键：用 cropWin 状态（由 setOnCropWindowChangedListener 拖动时实时更新 + 首次布局 post() 种子）驱动重绘，
                    // 既保证白框随拖动/旋转实时跟随，又避免此前"缓存 stale 坐标 → 白框错位到屏幕底部"的问题。
                    if (state.tab == EditorTab.CROP) {
                        val rect = cropWin
                        if (rect.width() > 1f && rect.height() > 1f) {
                            val lw = rect.left; val tw = rect.top
                            val rw = rect.right; val bw = rect.bottom
                            val w = rw - lw; val h = bw - tw
                            Box(
                                Modifier.fillMaxSize()
                                    .graphicsLayer { alpha = cropPageAlpha * cropAlpha.value }
                                    .drawBehind {
                                        val lineW = 1.dp.toPx()
                                        val gA = 0.5f
                                        // 3×3 网格
                                        for (i in 1..2) {
                                            val gx = lw + w * i / 3f
                                            drawLine(Color.White.copy(alpha = gA), Offset(gx, tw), Offset(gx, bw), strokeWidth = lineW)
                                            val gy = tw + h * i / 3f
                                            drawLine(Color.White.copy(alpha = gA), Offset(lw, gy), Offset(rw, gy), strokeWidth = lineW)
                                        }
                                        // 白框描边
                                        drawRect(
                                            color = Color.White,
                                            topLeft = Offset(lw, tw),
                                            size = Size(w, h),
                                            style = Stroke(2.dp.toPx()),
                                        )
                                        // 四角加粗
                                        val cl = 18.dp.toPx(); val cw = 3.dp.toPx()
                                        val segs = listOf(
                                            Offset(lw, tw) to Offset(lw + cl, tw), Offset(lw, tw) to Offset(lw, tw + cl),
                                            Offset(rw, tw) to Offset(rw - cl, tw), Offset(rw, tw) to Offset(rw, tw + cl),
                                            Offset(lw, bw) to Offset(lw + cl, bw), Offset(lw, bw) to Offset(lw, bw - cl),
                                            Offset(rw, bw) to Offset(rw - cl, bw), Offset(rw, bw) to Offset(rw, bw - cl),
                                        )
                                        segs.forEach { (a, b) -> drawLine(Color.White, a, b, strokeWidth = cw) }
                                    },
                            )
                        }
                    }
                }

            }

            // 底部面板：上半参数 + 下半菜单（上半参数高度随页切换经 animateContentSize 平滑变化，使上方照片随之平滑移动）
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp)
                    .graphicsLayer { alpha = bottomAlpha.value; translationY = bottomOffset.value },
            ) {
                // 上半部分：对应当前页的参数。用 AnimatedContent + SizeTransform 让面板高度平滑过渡，
                // 上方照片（weight(1f)）随之平滑移动，避免切页时照片位置硬跳（需求③）
                AnimatedContent(
                    targetState = state.tab,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                    transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) using SizeTransform { _, _ -> tween(300) } },
                    contentAlignment = Alignment.Center,
                ) { tab ->
                    when (tab) {
                        EditorTab.CROP -> {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Slider(
                                        value = cropRotation,
                        onValueChange = { new ->
                            val view = cropViewState.value ?: return@Slider
                            cropRotation = new
                            val deg = new.roundToInt()
                            if (deg == 0) {
                                // 回到 0°：复位显示到基准图（白框满框），保留 rotSource 供再次旋转/提交
                                lastRotBmp?.let { if (it !== rotSource && it !== liveBitmap.value) it.recycle() }
                                lastRotBmp = null
                                view.setImageBitmap(rotSource ?: liveBitmap.value)
                                lastDeg = 0
                                return@Slider
                            }
                            // 首次离开 0°：捕获旋转基准（若已裁切则取裁切结果，"旋转作用于裁切部分"）
                            if (rotSource == null) {
                                val r = view.cropRect
                                val base = liveBitmap.value
                                val full = r == null || (r.left == 0 && r.top == 0 && r.right == base.width && r.bottom == base.height)
                                if (!full) {
                                    val c = view.getCroppedImage()
                                    if (c != null && c !== base) { rotSource = c; rotOwned = true }
                                }
                                if (rotSource == null) { rotSource = base; rotOwned = false }
                            }
                            // 仅在整数角变化时烘焙（避免每帧重建 Bitmap），始终从 rotSource 旋转，杜绝累积
                            if (deg != lastDeg) {
                                val nb = rotateCover(rotSource!!, deg.toFloat())
                                lastRotBmp?.let { if (it !== rotSource && it !== liveBitmap.value) it.recycle() }
                                lastRotBmp = if (nb !== rotSource) nb else null
                                view.setImageBitmap(nb)
                                lastDeg = deg
                            }
                        },
                                        valueRange = -45f..45f,
                                        modifier = Modifier.weight(1f),
                                        colors = sliderColors,
                                        onValueChangeFinished = { onCommit() },
                                    )
                                    Text("${cropRotation.toInt()}°", color = fg, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp).width(40.dp))
                                    // 右侧复位按钮：跳回"不旋转 + 满框裁切"（需求⑤）
                                    Box(
                                        Modifier.size(34.dp).clip(CircleShape).background(capsuleBg)
                                            .clickable {
                                            cropViewState.value?.let { v ->
                                                // 复位：回收旋转中间图，喂回原始基图，白框满框、不旋转
                                                lastRotBmp?.let { if (it !== rotSource && it !== liveBitmap.value) it.recycle() }
                                                lastRotBmp = null
                                                if (rotOwned) rotSource?.recycle()
                                                rotSource = null; rotOwned = false
                                                v.setImageBitmap(liveBitmap.value)
                                            }
                                            cropRotation = 0f; lastDeg = 0
                                            onCommit()
                                        },
                                        contentAlignment = Alignment.Center,
                                    ) { Icon(Icons.Filled.Refresh, contentDescription = "复位", tint = fg) }
                                }
                            }
                        }
                        EditorTab.ADJUST -> {
                            Column(Modifier.fillMaxWidth()) {
                                EditorSlider("曝光度", state.exp, { onStateChange(state.copy(exp = it)) }, sliderColors, fg, onCommit,
                                    onReset = { onStateChange(state.copy(exp = 0.5f)); onCommit() }, resetBg = capsuleBg)
                                Spacer(Modifier.height(10.dp))
                                EditorSlider("对比度", state.con, { onStateChange(state.copy(con = it)) }, sliderColors, fg, onCommit,
                                    onReset = { onStateChange(state.copy(con = 0.5f)); onCommit() }, resetBg = capsuleBg)
                                Spacer(Modifier.height(10.dp))
                                EditorSlider("饱和度", state.sat, { onStateChange(state.copy(sat = it)) }, sliderColors, fg, onCommit,
                                    onReset = { onStateChange(state.copy(sat = 0.5f)); onCommit() }, resetBg = capsuleBg)
                                Spacer(Modifier.height(10.dp))
                                // 暗角：滑块 >0 自动开启 vig，=0 关闭；预览与落盘共用 vigInt（0~1 强度）
                                EditorSlider("暗角", state.vigInt, { onStateChange(state.copy(vigInt = it, vig = it > 0.001f)) }, sliderColors, fg, onCommit,
                                    onReset = { onStateChange(state.copy(vigInt = 0f, vig = false)); onCommit() }, resetBg = capsuleBg)
                            }
                        }
                        EditorTab.FILTER -> {
                            EditorFilterPanel(
                                state = state,
                                onStateChange = onStateChange,
                                onCommit = onCommit,
                                isDark = isDark,
                                fg = fg,
                                maskColor = maskColor,
                            )
                        }
                    }
                }
                // 下半部分：菜单栏（裁切 / 滤镜），左对齐；背景色落在"选中按钮"上（非整条栏），故栏本身透明无底色
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically,
                ) {
                    EditorTab.values().forEach { t ->
                        val label = when (t) { EditorTab.CROP -> "裁切"; EditorTab.ADJUST -> "调节"; EditorTab.FILTER -> "滤镜" }
                        val selected = state.tab == t
                        Box(
                            Modifier.padding(horizontal = 6.dp).clip(RoundedCornerShape(18.dp))
                                .background(if (selected) RetroRust else Color.Transparent)
                                .clickable { onStateChange(state.copy(tab = t)); onCommit() }
                                .padding(horizontal = 22.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(label, color = if (selected) Color.White else fg, fontSize = 14.sp) }
                    }
                }
            }
        }
    }

    // 返回确认丢弃修改（可一并还原为裁切前的原图，需求②）
    if (showDiscard) {
        val canRestore = origBackupOf(frameFile).exists()
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text("放弃修改") },
            text = { Text(if (canRestore) "当前编辑尚未保存。可放弃修改，或一并还原为裁切前的原图。" else "当前编辑尚未保存，确定放弃这些修改吗？") },
            confirmButton = { TextButton(onClick = { showDiscard = false; requestExit(onCancel) }) { Text("放弃") } },
            dismissButton = {
                Row {
                    if (canRestore) {
                        TextButton(onClick = { showDiscard = false; requestExit(onRestoreOriginal) }) { Text("还原原图") }
                    }
                    TextButton(onClick = { showDiscard = false }) { Text("继续编辑") }
                }
            },
        )
    }
}

/** 滤镜轮播预设：每个预设对应一组编辑状态（暖/冷/黑白/原图），底部滑块控制其强度。 */
private enum class FilterId { ORIGIN, WARM, COOL }
private data class FilterPreset(val id: FilterId, val name: String, val warm: Boolean, val cool: Boolean, val hasSlider: Boolean)
private val FILTER_PRESETS = listOf(
    FilterPreset(FilterId.ORIGIN, "原图", false, false, false),
    FilterPreset(FilterId.WARM, "暖色", true, false, true),
    FilterPreset(FilterId.COOL, "冷色", false, true, true),
)
private fun currentFilterIndex(state: EditState): Int = when {
    state.tint == TintState.WARM -> FILTER_PRESETS.indexOfFirst { it.id == FilterId.WARM }
    state.tint == TintState.COOL -> FILTER_PRESETS.indexOfFirst { it.id == FilterId.COOL }
    else -> FILTER_PRESETS.indexOfFirst { it.id == FilterId.ORIGIN }
}
private fun applyFilterPreset(state: EditState, p: FilterPreset): EditState = when (p.id) {
    FilterId.ORIGIN -> state.copy(tint = TintState.NONE)
    FilterId.WARM -> state.copy(tint = TintState.WARM)
    FilterId.COOL -> state.copy(tint = TintState.COOL)
}

/**
 * 滤镜页：左右滑动（或左右箭头）在预设间切换的横滑轮播。
 *  - 每页 = 正方形调色盘(ColorSquare) + 与正方形等宽的强度滑块（原图无滑块）。
 *  - 滑动时当前项左移、缩小、变淡，右侧新项进入（HorizontalPager + 基于离中心偏移的 scale/alpha）。
 *  - 最底部滤镜名固定，不随轮播横向动画移动。
 */
@Composable
private fun EditorFilterPanel(
    state: EditState,
    onStateChange: (EditState) -> Unit,
    onCommit: () -> Unit,
    isDark: Boolean,
    fg: Color,
    maskColor: Color,
) {
    val arrowColor = if (isDark) Color(0xFFCFCFCF) else Color(0xFF6B5744)
    val squareSize = 150.dp
    val pagerState = rememberPagerState(initialPage = currentFilterIndex(state), pageCount = { FILTER_PRESETS.size })
    val scope = rememberCoroutineScope()
    // 落定到某页时把该滤镜应用到 state（一致性判断避免重复触发/循环）
    LaunchedEffect(pagerState.currentPage) {
        val p = FILTER_PRESETS[pagerState.currentPage]
        val next = applyFilterPreset(state, p)
        if (next != state) { onStateChange(next); onCommit() }
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        // 整个面板（方形调色盘 + 滑块 + 滤镜名）都包进同一个可横滑的 Box：
        //  - 横滑切换滤镜的手势区域覆盖整个面板（含滤镜名，需求②）
        //  - 左右渐变遮罩覆盖整高（含滑块与滤镜名，需求①）
        Box(Modifier.fillMaxWidth().height(squareSize + 120.dp)) {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 44.dp),
                pageSpacing = 0.dp,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val p = FILTER_PRESETS[page]
                val pageOffset = (page - pagerState.currentPage - pagerState.currentPageOffsetFraction)
                // 模糊行程缩短：偏移达半页(0.5)即达最大模糊，而非整页
                val dist = (minOf(kotlin.math.abs(pageOffset), 0.5f) / 0.5f).coerceIn(0f, 1f)
                val scale = (1f - dist * 0.28f).coerceAtLeast(0.5f)
                val pageAlpha = (1f - dist * 0.6f).coerceAtLeast(0.2f)
                Column(
                    Modifier.fillMaxWidth().graphicsLayer {
                        scaleX = scale; scaleY = scale; alpha = pageAlpha
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // 原图：不显示调色盘，居中画一个禁止 logo（与调色盘同高）；其余滤镜：启用可调色盘（需求⑨）
                    if (p.id == FilterId.ORIGIN) {
                        NoFilterIcon(Modifier.size(squareSize), color = fg)
                    } else {
                        // 调色盘取值：暖/冷 = 饱和度(X) + 亮度(Y)
                        val dotX = when (p.id) {
                            FilterId.WARM, FilterId.COOL -> state.tintSat
                            else -> 0.5f
                        }
                        val dotY = when (p.id) {
                            FilterId.WARM, FilterId.COOL -> state.tintBri
                            else -> 0.5f
                        }
                        ColorSquare(
                            dotX = dotX, dotY = dotY,
                            onDotChange = { x, y ->
                                onStateChange(
                                    when (p.id) {
                                        FilterId.WARM, FilterId.COOL -> state.copy(tintSat = x, tintBri = y)
                                        else -> state
                                    },
                                )
                            },
                            onDotChangeFinished = { onCommit() },
                            warm = p.warm, cool = p.cool,
                            enabled = true,
                            isDark = isDark,
                            modifier = Modifier.size(squareSize),
                        )
                    }
                    if (p.hasSlider) {
                        Spacer(Modifier.height(8.dp))
                        val value = when (p.id) {
                            FilterId.WARM, FilterId.COOL -> state.tintStrength
                            else -> 0.5f
                        }
                        Slider(
                            value = value,
                            onValueChange = {
                                onStateChange(
                                    when (p.id) {
                                        FilterId.WARM, FilterId.COOL -> state.copy(tintStrength = it)
                                        else -> state
                                    },
                                )
                            },
                            valueRange = 0f..1f,
                            modifier = Modifier.width(squareSize),
                            colors = SliderDefaults.colors(
                                thumbColor = if (isDark) Color.White else RetroRust,
                                activeTrackColor = if (isDark) Color(0xFFCFCFCF) else RetroRust,
                                inactiveTrackColor = if (isDark) Color(0xFF3A3A3A) else Color(0xFF5A5147),
                            ),
                            onValueChangeFinished = { onCommit() },
                        )
                    }
                }
            }
            // 左遮罩：背景色 → 透明（覆盖整高）
            Box(Modifier.align(Alignment.CenterStart).width(44.dp).fillMaxHeight()
                .background(Brush.horizontalGradient(listOf(maskColor, Color.Transparent))))
            // 右遮罩：透明 → 背景色
            Box(Modifier.align(Alignment.CenterEnd).width(44.dp).fillMaxHeight()
                .background(Brush.horizontalGradient(listOf(Color.Transparent, maskColor))))
            // 左右箭头（叠加层，不占用滑动区，点击仍可翻页）
            Box(Modifier.align(Alignment.CenterStart).padding(start = 4.dp)) {
                TintArrow(isLeft = true, color = arrowColor, onClick = {
                    scope.launch { pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0)) }
                })
            }
            Box(Modifier.align(Alignment.CenterEnd).padding(end = 4.dp)) {
                TintArrow(isLeft = false, color = arrowColor, onClick = {
                    scope.launch { pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(FILTER_PRESETS.size - 1)) }
                })
            }
            // 滤镜名（底部居中，不随横滑移动；整块含此区域都可滑动切换）
            Text(
                FILTER_PRESETS[pagerState.currentPage].name,
                color = fg, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
            )
        }
    }
}

/** 无滤镜时的"禁止"logo：居中显示 🚫 emoji，整体与调色盘同高。 */
@Composable
private fun NoFilterIcon(modifier: Modifier, color: Color) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text("🚫", fontSize = 48.sp, textAlign = TextAlign.Center)
    }
}

/** 暖/冷/无 三态循环：dir<0 上一态，dir>0 下一态。 */
private fun cycleTint(current: TintState, dir: Int): TintState {
    return when (current) {
        TintState.NONE -> if (dir < 0) TintState.COOL else TintState.WARM
        TintState.WARM -> if (dir < 0) TintState.NONE else TintState.COOL
        TintState.COOL -> if (dir < 0) TintState.WARM else TintState.NONE
    }
}

/**
 * 构造 CanHub CropImageView 的 CropImageOptions：
 *  - 1:1 固定比例、默认裁切框占满整张照片（initialCropWindowPaddingRatio=0）；
 *  - 旋转由本文件 rotateCover 手动烘焙（非库原生），白框(裁切框)始终保持满框不动；
 *    关闭 autoZoom 以避免拖动裁切框时库自动放大导致白框异常溢出屏幕。
 *  - 细白边框 + 四角加粗相连（borderCornerOffset=0），3×3 网格常显，不自动缩放。
 */
/**
 * 手动"外切旋转"：把源图绕中心旋转 [deg] 度，并放大到恰好覆盖原图（cover），
 * 输出与原图同尺寸 Bitmap。放大后的内容会溢出原图边界（裁切到同尺寸），即"白框不动、图片放大旋转"。
 * 用于编辑器 CROP 页旋转（替代 CanHub 原生旋转，避免 autoZoom 导致拖动白框异常溢出）。
 * 不回收传入的 [src]（调用方负责）。
 */
private fun rotateCover(src: Bitmap, deg: Float): Bitmap {
    if (deg == 0f) return src
    val w = src.width; val h = src.height
    val rad = Math.toRadians(deg.toDouble())
    // cover 缩放：正方形源在正方形视口内旋转后铺满所需的最小均匀放大系数
    val k = 1f / maxOf(Math.abs(Math.cos(rad)).toFloat(), Math.abs(Math.sin(rad)).toFloat())
    val out = Bitmap.createBitmap(w, h, src.config ?: Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val m = Matrix().apply {
        setRotate(deg, w / 2f, h / 2f)
        postScale(k, k, w / 2f, h / 2f)
    }
    canvas.drawBitmap(src, m, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    return out
}

private fun buildCropOptions(context: Context): CropImageOptions {
    val d = context.resources.displayMetrics.density
    val cLen = 24f * d               // 四角加粗长度（px），比之前更小
    // 最小裁切窗口基于"原 40dp 角长"计算，保持与之前一致（不随四角缩小而变小）
    val minWin = (2 * 40f * d + 5f).toInt()
    return CropImageOptions(
        cropShape = CropImageView.CropShape.RECTANGLE,
        guidelines = CropImageView.Guidelines.ON,
        fixAspectRatio = true,
        aspectRatioX = 1,
        aspectRatioY = 1,
        initialCropWindowPaddingRatio = 0f,
        // 关闭 autoZoom：旋转改由 rotateCover 手动烘焙（白框固定、图片放大覆盖），
        // 开启反而会在拖动裁切框时自动放大导致白框异常溢出屏幕。
        autoZoomEnabled = false,
        // 白框/四角/网格保持白色且【可见】：由 CanHub 原生 overlay 直接绘制并接管拖动手势，
        // 保证只有一层白框、位置正确、可拖动（之前改为 Compose 置顶重绘会出现坐标错位/位置错乱）。
        // 层级：白框(CanHub 原生) 在 照片 之上；CROP 页不叠加 Compose 暗角，改用 CanHub 自带
        // "裁切区外变暗"遮罩，使白框始终在遮罩之上、清晰可见。
        showCropOverlay = true,
        showProgressBar = false,
        borderLineThickness = 2f * d,
        borderCornerThickness = 3f * d,
        borderCornerLength = cLen,
        borderCornerOffset = 0f,
        borderLineColor = AndroidColor.WHITE,
        borderCornerColor = AndroidColor.WHITE,
        guidelinesColor = AndroidColor.WHITE,
        minCropWindowWidth = minWin,
        minCropWindowHeight = minWin,
    )
}

/** 调节页的单行滑块：标签 + 滑条；松手时回调查 checkpoint（避免拖动过程产生大量历史点）。 */
@Composable
private fun EditorSlider(label: String, value: Float, onValueChange: (Float) -> Unit, colors: SliderColors, labelColor: Color, onChangeFinished: () -> Unit = {}, onReset: (() -> Unit)? = null, resetBg: Color = Color.Transparent) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = labelColor, fontSize = 14.sp, modifier = Modifier.width(64.dp))
        Slider(value = value, onValueChange = onValueChange, valueRange = 0f..1f, modifier = Modifier.weight(1f), colors = colors, onValueChangeFinished = onChangeFinished)
        if (onReset != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(resetBg)
                    .clickable { onReset() },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Refresh, contentDescription = "复原", tint = labelColor) }
        }
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
                .clip(RoundedCornerShape(4.dp))
                .background(if (isDark) RetroDarkSurface else Color(0x33000000))
                .border(if (selected) 2.dp else 0.dp, if (selected) borderColor else Color.Transparent, RoundedCornerShape(4.dp))
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
