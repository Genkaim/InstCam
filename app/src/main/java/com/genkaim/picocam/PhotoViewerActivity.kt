package com.genkaim.picocam

import android.content.Context
import android.net.Uri
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
import java.io.FileOutputStream
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.content.res.Configuration
import android.provider.MediaStore
import android.widget.Toast
import com.genkaim.picocam.BaseActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.PhotoFilter
import androidx.compose.material.icons.filled.CropPortrait
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
import androidx.compose.ui.res.stringResource
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
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
import com.genkaim.picocam.ui.AutoSizeText
import com.genkaim.picocam.ui.rememberColumnMinFontSize
import com.genkaim.picocam.dynamic.AppPrefs
import com.genkaim.picocam.dynamic.LangMode
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
        // 地址跟随 App 语言（而非系统语言）：中文/英文取对应 Locale，系统默认则回退系统语言
        val geocoderLocale = when (AppPrefs.lang.value.mode) {
            LangMode.ZH_HANS -> Locale.forLanguageTag("zh-Hans")
            LangMode.ZH_HANT -> Locale.forLanguageTag("zh-Hant")
            LangMode.EN -> Locale.ENGLISH
            else -> Locale.getDefault()
        }
        val geocoder = Geocoder(context, geocoderLocale)
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

/** 开关：true=详情页"编辑"按钮调起【系统图片编辑器】(ACTION_EDIT)，传入去掉拍立得白边的无边框图编辑，
 *  返回后补白底成 1:1 并重新加回白框写回原图（系统编辑器只对系统相册里的图才会原地写回，故无框图先插入系统相册）；
 *  false=使用内置编辑器。无可用系统编辑器时自动回退到内置编辑器。 */
private const val EXTRA_OPEN_EDIT = "open_edit"
private const val USE_SYSTEM_EDITOR = true

/**
 * 系统编辑：先把无边框图插入系统相册(MediaStore)作为临时可写回条目发起 ACTION_EDIT，
 * 各品牌系统编辑器只对"系统相册里自己的图"才会原地写回(覆盖)；返回后由 systemEditLauncher
 * 把结果重新加框(1:1 白底补方 + 拍立得白边)写回原文件并覆盖系统相册里对应的带框条目。
 */

/** 删除一条 MediaStore 记录（清理系统相册里我们插入/另存的临时图）。 */
private fun deleteMediaStore(context: Context, uri: Uri?) {
    if (uri == null) return
    runCatching { context.contentResolver.delete(uri, null, null) }
}

/** 从 content URI 读取 Bitmap（用于读回编辑器结果）。 */
private fun readBitmapFromUri(context: Context, uri: Uri): Bitmap? =
    runCatching { context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } }.getOrNull()

/** 查询系统相册中"启动编辑后新增/修改"的最新一张图（兜底捕获编辑器另存到系统相册的结果），排除我们自己的条目；返回其 Uri（用于读回并删除重复）。 */
private fun queryNewestEditedSince(context: Context, sinceMs: Long, excludeUri: Uri?): Uri? {
    val cr = context.contentResolver
    val sinceSec = sinceMs / 1000
    val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val proj = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_MODIFIED)
    val sel = "${MediaStore.Images.Media.DATE_MODIFIED} > ?"
    cr.query(uri, proj, sel, arrayOf(sinceSec.toString()), "${MediaStore.Images.Media.DATE_MODIFIED} DESC")
        ?.use { c ->
            if (c.moveToFirst()) {
                val id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                val found = android.content.ContentUris.withAppendedId(uri, id)
                if (found != excludeUri) return found
            }
        }
    return null
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

class PhotoViewerActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val filePath = intent?.getStringExtra("file_path")
        if (filePath == null) { finish(); return }
        val startInEdit = intent?.getBooleanExtra(EXTRA_OPEN_EDIT, false) ?: false
        setContent {
            PhotoViewerContent(startFile = File(filePath), startInEdit = startInEdit, onDismiss = { finish() }, onDelete = { finish() })
        }
    }
}

@Composable
private fun PhotoViewerContent(startFile: File, startInEdit: Boolean = false, onDismiss: () -> Unit, onDelete: () -> Unit) {
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
    // 是否走"深色表面"：系统深色模式，或照片莫奈取色很黑（此时页面文字已适配为白字）→ 背景/操控区都切深灰
    val useDarkSurface = isDark || !bgLight

    // —— 编辑器状态（统一用 EditState 快照，便于撤销/重做）——
    var editing by remember { mutableStateOf(false) }
    // 编辑用工作位图（MutableState，便于编辑层直接回写裁切结果，使其它栏/保存同步）
    var workBitmap = remember { mutableStateOf<Bitmap?>(null) }
    // 编辑控件（进入编辑时复位为中性，让用户从零调整）；所有可调项都收进 EditState
    var editState by remember { mutableStateOf(EditState()) }
    var imageVersion by remember { mutableIntStateOf(0) }
    // 系统编辑器（ACTION_EDIT）流程状态：无边框临时文件、写回目标（带框原图）、修改前时间戳（用于判断是否真被编辑）
    var sysEditTempFile by remember { mutableStateOf<File?>(null) }
    var sysEditTargetFile by remember { mutableStateOf<File?>(null) }
    var sysEditBeforeModified by remember { mutableStateOf(0L) }
    var sysEditStartMs by remember { mutableStateOf(0L) }
    var sysEditMediaUri by remember { mutableStateOf<Uri?>(null) }
    // 原始带框图在系统相册里的条目（用于回写加框结果）；临时无框条目是 sysEditMediaUri
    var sysEditFramedUri by remember { mutableStateOf<Uri?>(null) }
    val systemEditLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val target = sysEditTargetFile
        val startMs = sysEditStartMs
        val mediaUri = sysEditMediaUri       // 临时无框条目（编辑器编辑对象）
        val framedUri = sysEditFramedUri     // 原始带框条目（回写目标）
        sysEditTempFile = null
        sysEditTargetFile = null
        sysEditMediaUri = null
        sysEditFramedUri = null
        if (target == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            var raw: Bitmap? = null
            var rawUri: Uri? = null   // 编辑结果来源 Uri（用于删除"另存"产生的重复系统相册条目）
            // ① 另存型编辑器常把新文件 URI 通过 result Intent 的 data 回传
            val resultUri = res.data?.data
            if (resultUri != null) {
                raw = readBitmapFromUri(context, resultUri)
                rawUri = resultUri
            }
            // ② 兜底：扫描系统相册，找启动后新增/修改的最新一张图（捕获编辑器另存到系统相册的结果），排除我们自己的条目
            if (raw == null) {
                val found = queryNewestEditedSince(context, startMs, mediaUri)
                if (found != null) {
                    raw = readBitmapFromUri(context, found)
                    rawUri = found
                }
            }
            // ③ 写回型：系统编辑器把结果原地写回了我们的临时无框 URI（更新了同一记录）
            if (raw == null && mediaUri != null) {
                raw = readBitmapFromUri(context, mediaUri)
                rawUri = mediaUri
            }
            if (raw != null) {
                // 传给系统编辑器的是无滤镜原图；返回后把拍照时应用的滤镜效果重新叠加回来，
                // 使成品 = 系统编辑结果 + 原滤镜 + 拍立得白边（用户诉求：回来要把效果全部加回来）。
                val meta = PhotoStorage.loadFilterMeta(target)
                val baseEff = if (meta != null) EffectiveFilter(
                    grayscale = meta.grayscale,
                    vignette = meta.vignette,
                    exposure = meta.exposure,
                    warmth = meta.warmth,
                    saturation = meta.saturation,
                    brightness = meta.brightness,
                    contrast = meta.contrast,
                ) else EffectiveFilter()
                // 编辑成品：颜色矩阵 + 暗角只作用于照片本身（processEditSquare 内部完成），
                // 非 1:1 才底部补白成正方形，白底不被滤镜/暗角污染，与编辑器预览完全一致。
                val processed = PhotoStorage.processEditSquare(raw, baseEff, AppPrefs.frame.value.color, isFrosted = AppPrefs.frame.value.isFrosted)
                PhotoStorage.reencodeWithFrame(context, target, processed, AppPrefs.frame.value.color, isFrosted = AppPrefs.frame.value.isFrosted)
                // 同步相框选择侧车，使下次编辑默认与本次成品一致
                PhotoStorage.saveFrameMeta(target, AppPrefs.frame.value.isFrosted, AppPrefs.frame.value.color)
                if (processed !== raw) processed.recycle()
                // 更新"下次编辑基图"侧车：保存系统编辑返回的【原始图 raw】——不含补白/暗角/滤镜，
                // 保留其真实比例。关键：补白只写进成品(target)，绝不写进 sourceSquare，
                // 否则下次内建/系统编辑会以带白底的图为基底、残留白边（用户诉求）。
                // 无条件替换：无论原照片是否有滤镜元信息，经外部相册裁切后，内建相册的原始图片都要换成裁切后的结果。
                PhotoStorage.saveSourceSquare(target, raw)
                raw.recycle()
                // 同步系统相册：覆盖原始带框条目；若没有映射（旧照片/落盘失败）则插入新条目并记录映射
                if (framedUri != null) {
                    val framedBmp = BitmapFactory.decodeFile(target.path)
                    if (framedBmp != null) {
                        PhotoStorage.overwriteUriWithBitmap(context, framedUri, framedBmp, target)
                        framedBmp.recycle()
                    }
                } else {
                    val newUri = PhotoStorage.saveToGalleryReturnUri(context, target)
                    if (newUri != null) PhotoStorage.writeGalleryId(target, newUri)
                }
                // 保留滤镜元信息与原图侧车（效果已重新叠加回成品，供后续编辑复用）；不再 clearFilterMeta。
                // 若编辑器"另存"产生了独立于我们条目之外的新记录，删掉以免系统相册出现重复
                if (rawUri != null && rawUri != mediaUri && rawUri != framedUri) deleteMediaStore(context, rawUri)
            }
            // 无论是否编辑成功，都清理临时无框条目（编辑期间短暂出现在系统相册，避免残留）
            deleteMediaStore(context, mediaUri)
            withContext(Dispatchers.Main) {
                imageVersion++
                bgColorTarget = extractDominantColor(target, RetroPaper.toArgb())
            }
        }
    }
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

    // 底部按钮栏：进入浏览/退出编辑时渐入、进入编辑时渐隐（仅 alpha）
    val bottomBarAlpha = remember { Animatable(0f) }
    LaunchedEffect(editing) {
        bottomBarAlpha.animateTo(if (editing) 0f else 1f, tween(durationMillis = 300))
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
                // 默认框 = 照片实际拍照时用的框：优先读侧车；无侧车(旧照片)从成品图底栏反推实际框，
                // 避免回退到全局默认的毛玻璃而与照片实际框不符
                val fm = PhotoStorage.loadFrameMeta(currentFile) ?: PhotoStorage.detectFrameMeta(currentFile)
                val defaultFrosted = fm?.isFrosted ?: AppPrefs.frame.value.isFrosted
                val defaultColor = fm?.frameColor ?: AppPrefs.frame.value.color
                editState = if (meta != null) {
                    val tintStrengthEff = when (meta.tint) {
                        TintState.WARM -> meta.tintStrength
                        TintState.COOL -> -meta.tintStrength
                        else -> 0f
                    }
                    EditState(
                        tint = meta.tint,
                        tintSat = meta.tintSat,
                        // 恢复色盘亮度(Y)到滤镜栏的取色盘位置，不再并入亮度栏（否则滤镜栏上下数据丢失）
                        tintBri = meta.tintBri,
                        tintStrength = meta.tintStrength,
                        vig = meta.vignette > 0.001f,
                        vigInt = meta.vignette.coerceIn(0f, 1f),
                        grayscale = meta.grayscale.coerceIn(0f, 1f),
                        // 曝光只承载相机「亮度」滤镜：meta.exposure = 亮度滤镜 + 色盘Y(明暗)，需把色盘Y扣除
                        // （色盘Y = (0.5 - tintBri)，仅调色盘激活时有效，与相机 bright 系数一致）
                        exp = run {
                            val paletteY = if (meta.tint != TintState.NONE) (0.5f - meta.tintBri) else 0f
                            ((meta.exposure - paletteY).coerceIn(-1f, 1f) / 2f + 0.5f).coerceIn(0f, 1f)
                        },
                        sat = 0.5f,
                        con = (meta.contrast / 2f + 0.5f).coerceIn(0f, 1f),
                        frame = true,
                        frameColor = defaultColor,
                        isFrosted = defaultFrosted,
                    )
                } else EditState(frame = true, frameColor = defaultColor, isFrosted = defaultFrosted)
                history = listOf(editState); historyIndex = 0
                editing = true
            } else {
                Toast.makeText(context, context.getString(R.string.editor_cannot_edit), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 通过 open_edit 进入时直接进编辑态（自写编辑器）：currentFile 在未加载列表时回退为 startFile（即目标照片），
    // enterEdit 操作的就是该文件，故无需等待列表加载；照片列表随后加载并定位 pager，返回浏览时已在正确位置。
    LaunchedEffect(startInEdit) {
        if (startInEdit) enterEdit()
    }

    /** 调起【系统图片编辑器】(ACTION_EDIT) 编辑当前照片：
     *  先去掉拍立得白边得到无边框图（优先拍照原图侧车，否则裁掉白边），把无边框图插入系统相册作为临时可写回条目——
     *  各品牌系统编辑器只对"系统相册里自己的图"才会原地写回，故以此条目作为编辑对象，返回后重新加框写回。
     *  原始带框图在系统相册里的条目(framedUri)用于回写加框结果（没有映射则回写时新建）。
     *  无可用系统编辑器则回退内置编辑器。 */
    fun launchSystemEdit() {
        scope.launch(Dispatchers.IO) {
            // 取无边框图（优先拍照原图侧车，否则裁掉白边），作为系统编辑器的编辑对象
            val inner = PhotoStorage.loadSourceSquare(currentFile) ?: PhotoStorage.cropInnerSquare(currentFile)
            if (inner == null) {
                withContext(Dispatchers.Main) { Toast.makeText(context, context.getString(R.string.editor_cannot_edit), Toast.LENGTH_SHORT).show() }
                return@launch
            }
            // 把无边框图插入系统相册作为临时可写回条目（编辑期间会短暂出现在系统相册，编辑结束即删除）
            val tempUri = PhotoStorage.saveBitmapToGallery(context, inner)
            inner.recycle()
            if (tempUri == null) {
                withContext(Dispatchers.Main) { Toast.makeText(context, context.getString(R.string.editor_cannot_edit), Toast.LENGTH_SHORT).show() }
                return@launch
            }
            // 原始带框图在系统相册里的条目（用于回写加框结果）；可能没有映射（旧照片/落盘失败）
            val framedUri = PhotoStorage.readGalleryUri(context, currentFile)
            val startMs = System.currentTimeMillis()
            withContext(Dispatchers.Main) {
                sysEditTempFile = null
                sysEditTargetFile = currentFile
                sysEditBeforeModified = 0L
                sysEditStartMs = startMs
                sysEditMediaUri = tempUri
                sysEditFramedUri = framedUri
                val intent = runCatching {
                    Intent(Intent.ACTION_EDIT).apply {
                        setDataAndType(tempUri, "image/jpeg")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    }
                }.getOrNull()
                if (intent != null && intent.resolveActivity(context.packageManager) != null) {
                    systemEditLauncher.launch(intent)
                } else {
                    deleteMediaStore(context, tempUri)   // 无系统编辑器 → 清理临时无框条目
                    enterEdit()   // 回退内置编辑器
                }
            }
        }
    }
    /** 取消编辑：丢弃内存中的无边框图，原文件（带框）保持不变 → 退出时边框自动恢复。 */
    fun exitEdit() {
        editing = false
        workBitmap.value?.recycle()
        workBitmap.value = null
    }
    /** 保存编辑：对编辑基图应用滤镜并重新加回拍立得白边写回原文件，刷新预览缓存与主色。
     *  裁切功能已移除，故直接以当前基图（外部编辑结果或上一次编辑结果）为保存对象。 */
    fun saveEdit() {
        val sq = workBitmap.value ?: return
        // 非破坏式：首次保存前备份原图（带框）到同目录 .orig 副本，便于复原（需求②）
        val backup = origBackupOf(currentFile)
        if (!backup.exists()) { try { currentFile.copyTo(backup, overwrite = false) } catch (_: Exception) {} }
        scope.launch {
            val s = editState
            // 滤镜状态合成：与相机拍照直出一致（见 EditState.toEffectiveFilter）
            val eff = s.toEffectiveFilter()
            // 非 1:1 时先烤暗角再底部留白成正方形（与系统编辑返回路径一致，构图不被破坏）；
            // 1:1 直接叠加滤镜后加拍立得白边。颜色矩阵 + 暗角只作用于照片本身，
            // 随后才底部补白，白底不被滤镜/暗角污染（与预览一致）。
            val frameColor = editState.frameColor
            val bgColorForSquare = when {
                !editState.frame -> AndroidColor.WHITE
                editState.isFrosted -> 0
                else -> frameColor
            }
            val processed = PhotoStorage.processEditSquare(sq, eff, bgColorForSquare, isFrosted = editState.isFrosted)
            if (editState.frame) {
                PhotoStorage.reencodeWithFrame(context, currentFile, processed, frameColor, isFrosted = editState.isFrosted)
            } else {
                PhotoStorage.reencodePlain(context, currentFile, processed)
            }
            // 同步相框选择侧车，使下次编辑默认与本次成品一致（修：编辑改了框后必须更新 meta，否则下次进入仍默认旧框）
            PhotoStorage.saveFrameMeta(currentFile, editState.isFrosted, frameColor)
            // 更新下次编辑基图侧车：保存"无白边"的编辑结果（保留真实比例，白底只写进成品）
            PhotoStorage.saveSourceSquare(currentFile, sq)
            if (processed !== sq) processed.recycle()
            sq.recycle()
            workBitmap.value = null
            editing = false
            imageVersion++
            // 同步滤镜元信息：使下次编辑默认选中当前滤镜（与相册照片一致）。
            // 保存范围覆盖全部调整通道（黑白/暗角/曝光/暖冷/饱和/对比度），
            // 只要有任意效果（非恒等）就保留，确保系统编辑返回后能原样重建（不再只看 tint）。
            if (!eff.isIdentity()) {
                PhotoStorage.saveFilterMeta(currentFile, eff, s.tint, s.tintSat, s.tintBri, s.tintStrength)
            } else {
                PhotoStorage.clearFilterMeta(currentFile)
            }
            // 同步系统相册镜像：覆盖带框条目（无映射则插入新条目并记录），否则系统相册里仍是编辑前的旧图
            withContext(Dispatchers.IO) {
                val framedUri = PhotoStorage.readGalleryUri(context, currentFile)
                if (framedUri != null) {
                    val framedBmp = BitmapFactory.decodeFile(currentFile.path)
                    if (framedBmp != null) {
                        PhotoStorage.overwriteUriWithBitmap(context, framedUri, framedBmp, currentFile)
                        framedBmp.recycle()
                    }
                } else {
                    val newUri = PhotoStorage.saveToGalleryReturnUri(context, currentFile)
                    if (newUri != null) PhotoStorage.writeGalleryId(currentFile, newUri)
                }
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
        // 进入编辑时随顶/底栏一起淡出（bottomBarAlpha→0），退出编辑淡入
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = bottomBarAlpha.value },
            userScrollEnabled = !isZoomed,
        ) { page ->
            val f = photos.getOrNull(page) ?: startFile
            ZoomablePhoto(
                file = f,
                // 上下非对称 padding（上小下大）让照片视觉中心略偏上，视觉平衡
                modifier = Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 36.dp),
                // [修复] 原 cacheKey 仅用 imageVersion，activity 重建时它归零，会命中旧的「path#0」缓存位图。
                // 改用文件 lastModified()（内容指纹）为主、imageVersion 为辅，确保重建/编辑后都加载新图。
                cacheKey = "${f.path}#${f.lastModified()}#$imageVersion",
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
                .graphicsLayer { alpha = bottomBarAlpha.value }
            .background(if (isDark) graySurface else (if (bgLight) RetroCream.copy(alpha = 0.85f) else Color(0x80000000)))
            .clickable(onClick = { requestLeave(onDismiss) }),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = if (isDark) whiteOnDark else fgColor)
        }

        // 顶部时间 + 位置：深色模式统一白色文字；地址可换行且左右留出必要间距
        Column(
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 12.dp).fillMaxWidth().graphicsLayer { alpha = bottomBarAlpha.value },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(formatPhotoTime(currentFile.lastModified()), style = MaterialTheme.typography.labelSmall, color = if (isDark) whiteOnDark else fgColor, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(start = 56.dp, end = 24.dp))
            if (locationText != null) {
                Text(locationText!!, style = MaterialTheme.typography.labelSmall, color = (if (isDark) whiteOnDark else fgColor).copy(alpha = 0.8f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(start = 56.dp, end = 24.dp, top = 2.dp, bottom = 2.dp))
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
                            version = f.lastModified(),
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
                // 分享（胶囊组左端，仅图标）
                Row(
                    modifier = Modifier
                        .height(56.dp)
                        .width(80.dp)
                        .clip(RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp, topEnd = 4.dp, bottomEnd = 4.dp))
                        .background(capsuleBg)
                        .clickable { scope.launch { shareImage(context, currentFile) } },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.action_share), modifier = Modifier.size(22.dp), tint = if (isDark) whiteOnDark else fgColor)
                }

                Spacer(Modifier.width(2.dp))

                // 编辑（系统图片编辑器，ACTION_EDIT），胶囊中段，与分享/自写编辑等宽、同底色
                Row(
                    modifier = Modifier
                        .height(56.dp)
                        .width(80.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(capsuleBg)
                        .clickable(onClick = { if (USE_SYSTEM_EDITOR) launchSystemEdit() else enterEdit() }),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.cd_system_edit), modifier = Modifier.size(22.dp), tint = if (isDark) whiteOnDark else fgColor)
                }

                Spacer(Modifier.width(2.dp))

                // 编辑（自写内置编辑器，不同图标），胶囊组最右端
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
                    Icon(Icons.Filled.Tune, contentDescription = stringResource(R.string.cd_custom_edit), modifier = Modifier.size(22.dp), tint = if (isDark) whiteOnDark else fgColor)
                }

                Spacer(Modifier.width(16.dp))

                // 删除（圆形，深色模式 = 灰底白图标；浅色模式 = 铁锈红淡底红图标）
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isDark) graySurface else RetroRust.copy(alpha = 0.15f))
                        .clickable { showDeleteConfirm = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_delete), tint = if (isDark) whiteOnDark else RetroRust)
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
                frameFile = currentFile,
                bitmapState = workBitmap,
                isDark = isDark,
                darkSurface = useDarkSurface,
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
            title = { Text(stringResource(R.string.viewer_delete_photo)) },
            text = { Text(stringResource(R.string.viewer_delete_confirm)) },
            confirmButton = { TextButton(onClick = {
                showDeleteConfirm = false
                val idx = pagerState.currentPage
                val oldSize = photos.size
                val deleted = currentFile
                val newList = photos.filter { it.absolutePath != deleted.absolutePath }
                scope.launch {
                    // 仅当相册删空才退出主界面；否则切换到相邻照片（不退出）
                if (newList.isEmpty()) {
                    PhotoStorage.deletePhotoWithSidecars(context, deleted)
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
                        PhotoStorage.deletePhotoWithSidecars(context, deleted)
                        photos = newList
                        // 旧列表目标位 == 新列表目标位（同一文件），瞬间定位无跳变
                        pagerState.scrollToPage(targetInNew)
                    }
                }
            }) { Text(stringResource(R.string.camera_delete)) } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.dialog_cancel)) } },
        )
    }
}

/** 编辑器底部菜单两页。 */
private enum class EditorTab { ADJUST, FILTER, FRAME }

/**
 * 编辑器完整状态快照：撤销/重做只需整体替换此对象。
 * 包含所有可调项：当前页 / 冷暖色调 / 取色盘位置(tintSat,tintBri) / 各滤镜开关与强度 /
 * 调节滑块。裁切功能已移除，内建编辑器只做调节与滤镜。
 */
private data class EditState(
    val tab: EditorTab = EditorTab.ADJUST,
    val tint: TintState = TintState.NONE,
    val tintStrength: Float = 0.5f,
    val tintSat: Float = 0.5f,
    val tintBri: Float = 0.5f,
    val vig: Boolean = false,
    val vigInt: Float = 0.5f,
    val grayscale: Float = 0f,   // 黑白：0~1（与相机「黑白」滤镜一致）
    val exp: Float = 0.5f,
    val sat: Float = 0.5f,
    val con: Float = 0.5f,     // 对比度：0.5=中性
    // 拍立得相框：强制开启（成品必须有拍立得边框，编辑器不提供关闭开关），仅可改色/毛玻璃
    val frame: Boolean = true,
    val frameColor: Int = 0xFFFFFFFF.toInt(),
    val isFrosted: Boolean = false,
)

/**
 * 把编辑态合成为与相机拍照直出【完全一致】的 EffectiveFilter。
 *
 * 关键对齐（修复"相册改滤镜与拍照拍出来不一致"）：相机侧 [CameraViewModel.effective] 中
 * 暖/冷色盘的两个轴贡献为：
 *   - X(饱和度) → saturation 通道；
 *   - 暖/冷选择  → warmth 通道；
 *   - Y(明暗)    → exposure 通道（仅调色盘激活时），系数与相机 BRIGHT_SCALE=0.5 一致：
 *                 相机 bright = (0.5 - tintBri) * 2 * 0.5 = (0.5 - tintBri)；
 *                 但【不】并入 ADJUST 的"亮度"滑块——它仅停留在滤镜栏的调色盘内
 *                 （需求：调色盘暗色不要映射到亮度栏，滤镜栏上下数据不丢失）。
 *   - 相机【从不】写 brightness 通道（亮度走 exposure）。
 * 故：亮度栏(exp) 与 调色盘Y 各自独立地经 exposure 通道叠加，二者互不侵占，
 *     编辑预览/成品与拍照直出完全对应、且滤镜栏取色盘保留原始上下位置。
 */
private fun EditState.toEffectiveFilter(): EffectiveFilter {
    val tintSatC = if (tint != TintState.NONE) (tintSat - 0.5f) * 2f else 0f            // X(饱和度) → saturation 通道
    // Y(明暗) → exposure，仅调色盘激活时；与相机 bright=(0.5-tintBri) 一致。保留在调色盘(滤镜栏)内，不并入「亮度」滑块。
    val tintBriC = if (tint != TintState.NONE) (0.5f - tintBri) else 0f
    return EffectiveFilter(
        grayscale = grayscale,
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
    frameFile: File,
    bitmapState: MutableState<Bitmap?>,
    isDark: Boolean,
    darkSurface: Boolean,
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
    // 操控区（底部圆角卡片）底色：深色表面恒为深灰 RetroDarkSurface，浅色为奶油白 RetroCream。
    // 触发深色表面的两种情形：①系统深色模式(isDark)；②照片莫奈取色很黑(darkSurface，此时页面文字已变白)。
    // 不跟随照片主色 bgColor（否则与页面同色而"消失"）；统一走 this 深灰，不与照片动态色混。
    val editorCardBg = if (darkSurface) RetroDarkSurface else RetroCream
    // 滤镜预览统一由下方照片 Image 层通过 colorMatrix 处理（裁切页已移除，不再需要给 CropImageView 单独设滤镜）
    // 滤镜页左右遮罩颜色：滤镜面板位于底部圆角卡片内，须与卡片底色一致（RetroDarkSurface/RetroCream）
    // ——底部面板背景与 maskColor 必须联动，改卡片底色时要同步改这里
    val maskColor = editorCardBg
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
    val contentAlpha = remember { Animatable(0f) }       // 整个编辑层：进入时渐入（与详情页照片淡出交叉）、退出时整体渐出（需求⑦）
    // 删去 CROP 页后仅 ADJUST / FILTER 两页，照片层始终挂载；不再有裁切页交叉淡出
    val cropPageAlpha = 0f
    val photoPageAlpha = 1f
    // 滤镜页整体预览元素略微缩小（宽度更窄），与切页动画同步过渡
    val previewScale by animateFloatAsState(if (state.tab == EditorTab.FILTER) 0.88f else 1f, tween(350), label = "previewScale")
    // 拍立得相框：仅在「相框」栏显示预览（调节/滤镜栏不显示相框），切到相框栏时边框渐入
    val showFrame = state.frame && state.tab == EditorTab.FRAME
    val frameAlpha by animateFloatAsState(if (showFrame) 1f else 0f, tween(220), label = "frameAlpha")
    // 滤镜切换的模糊过渡改为按离中心距离逐页应用（见 EditorFilterPanel），此处不再做整页模糊
    // 顶栏 / 底栏 进入、离场动画（Animatable）
    val topAlpha = remember { Animatable(0f) }
    val bottomAlpha = remember { Animatable(0f) }
    val bottomOffset = remember { Animatable(48f) }
    var leaving by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showDiscard by remember { mutableStateOf(false) }
    var framedReady by remember { mutableStateOf(false) }   // 带框原图是否加载完成
    // 当前显示的位图（编辑基图，即外部编辑结果或上一次编辑结果）；预览与保存均基于此，不再有裁切/旋转。
    val liveBitmap = remember { mutableStateOf(bitmapState.value!!) }

    // 进入动画：待带框原图加载完成后才启动（避免异步加载导致的"闪一下"，需求①）
    LaunchedEffect(framedReady) {
        if (!framedReady) return@LaunchedEffect
        launch { contentAlpha.animateTo(1f, tween(durationMillis = 300)) }   // 编辑层整体渐入（与详情页照片淡出交叉）
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
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = if (isDark) whiteOnDark else fgColor) }
                // 右侧：撤销 / 重做 / 保存
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp)
                            .clickable(enabled = canUndo) { onUndo() }, contentAlignment = Alignment.Center,
                    ) { Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(R.string.cd_undo), tint = if (canUndo) fg else fg.copy(alpha = 0.35f)) }
                    Box(
                        Modifier.size(40.dp)
                            .clickable(enabled = canRedo) { onRedo() }, contentAlignment = Alignment.Center,
                    ) { Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = stringResource(R.string.cd_redo), tint = if (canRedo) fg else fg.copy(alpha = 0.35f)) }
                    // 保存：铁锈红圆形 + 勾
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(RetroRust)
                            .clickable(onClick = { requestExit { onSave() } }), contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.cd_save), tint = Color.White) }
                }
            }

            // 中部：照片占据顶/底面板之间的剩余空间；不左右沾满（左右内边距），大小随底栏高度自适应
            Box(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                // 照片层：1:1 方框，编辑基图直接全屏显示（不再有裁切视图）。
                // clip 使内容不溢出到其它 UI；previewScale 在滤镜页略微缩小（见下方）。
                Box(Modifier.fillMaxSize().aspectRatio(1f)
                    .graphicsLayer { scaleX = previewScale * enterScale.value; scaleY = previewScale * enterScale.value }
                    .clipToBounds()) {
                    // 进入编辑/切换照片时：把当前基图作为预览位图（外部编辑结果或上一次编辑结果）
                    LaunchedEffect(bitmapState.value) {
                        val bmp = bitmapState.value ?: return@LaunchedEffect
                        liveBitmap.value = bmp
                    }

                    if (showFrame) {
                        // 拍立得相框：竖版卡片居中（frameColor 底），照片按真实比例嵌入、滤镜仅作用于照片。
                        // 卡片比例：边框 b=0.06*s、底部额外留白 6b → 宽=s+2b、高=s+7b → 比例 (1+0.12)/(1+0.42)。
                        // 与落盘 reencodeWithFrame 几何一致，做到预览即成品（边框跟随照片）。
                        BoxWithConstraints(
                            Modifier.fillMaxSize().graphicsLayer { alpha = frameAlpha },
                        ) {
                            val side = maxOf(maxWidth, maxHeight)   // 本层为正方形区域，maxWidth==maxHeight
                            val cardAspect = (1f + 0.12f) / (1f + 0.42f)
                            val cardW = side * cardAspect
                            val cardH = side
                            // 照片内嵌内边距：顶/左/右 = b/卡片高，底部 = 6b/卡片高（占卡片高比例）
                            val inset = side * (0.06f / 1.42f)
                            val bottomInset = side * (0.36f / 1.42f)
                            val photoBmp = liveBitmap.value
                            // 毛玻璃背景：直接调用与落盘完全相同的算法（高清缩放：输出的约 1/5 再放大，保留照片内容的磨砂感）预生成位图。
                            // 不再用 graphicsLayer renderEffect（API 31 以下是 no-op → "预览模糊没生效"bug），
                            // 且观感与拍照、保存成品完全一致（同一函数、同一缩放比例不变量）。
                            val frostedBg = remember(photoBmp, state.isFrosted) {
                                if (state.isFrosted) PhotoStorage.frostedBackground(photoBmp, 512, (512f * 1.42f / 1.12f).roundToInt())
                                else null
                            }
                            Box(
                                Modifier.align(Alignment.Center).width(cardW).height(cardH)
                                    .clipToBounds()
                                    .background(if (state.isFrosted) Color.Transparent else Color(state.frameColor)),
                            ) {
                                // 毛玻璃背景（仅选中毛玻璃时；无压暗遮罩）；滤镜同样作用于背景（落盘用的是已滤镜照片生成背景）
                                if (state.isFrosted && frostedBg != null) {
                                    Image(
                                        bitmap = frostedBg.asImageBitmap(), contentDescription = null, contentScale = ContentScale.FillBounds,
                                        modifier = Modifier.matchParentSize().graphicsLayer {
                                            colorFilter = ColorFilter.colorMatrix(composeCm)
                                        },
                                    )
                                }
                                // 照片层：在卡片内按内边距嵌入（该区域恰好为正方形，Fit 满铺），滤镜只作用于照片、不染框
                                Image(
                                    bitmap = photoBmp.asImageBitmap(), contentDescription = stringResource(R.string.cd_edit_preview), contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                        .padding(top = inset, bottom = bottomInset, start = inset, end = inset)
                                        .graphicsLayer {
                                            alpha = photoAlpha.value
                                            colorFilter = ColorFilter.colorMatrix(composeCm)
                                        },
                                )
                                // 暗角预览：裁剪到照片内嵌区域（与落盘一致），不污染相框
                                if (state.vig) {
                                    val vigAlpha = (state.vigInt * 0xD9 / 0xFF).coerceIn(0f, 1f)
                                    Box(
                                        Modifier.fillMaxSize()
                                            .padding(top = inset, bottom = bottomInset, start = inset, end = inset)
                                            .graphicsLayer { alpha = photoAlpha.value }
                                            .drawBehind {
                                                val dispW = size.width
                                                val dispH = size.height
                                                val vigRadius = kotlin.math.hypot(dispW / 2f, dispH / 2f)
                                                clipRect(left = 0f, top = 0f, right = dispW, bottom = dispH) {
                                                    drawRect(
                                                        Brush.radialGradient(
                                                            colorStops = arrayOf(
                                                                0f to Color.Transparent,
                                                                0.4f to Color.Transparent,
                                                                1f to Color.Black.copy(alpha = vigAlpha),
                                                            ),
                                                            center = center,
                                                            radius = vigRadius,
                                                        ),
                                                    )
                                                }
                                            },
                                    )
                                }
                            }
                        }
                    } else {
                        // 无框：照片直接满铺（与原行为一致）
                        Image(
                            bitmap = liveBitmap.value.asImageBitmap(), contentDescription = stringResource(R.string.cd_edit_preview), contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().graphicsLayer {
                                alpha = photoAlpha.value
                                colorFilter = ColorFilter.colorMatrix(composeCm)
                            },
                        )
                        if (state.vig) {
                            val vigAlpha = (state.vigInt * 0xD9 / 0xFF).coerceIn(0f, 1f)   // = vigInt * 217/255 ≈ vigInt*0.85，与保存端一致
                            val bmp = liveBitmap.value
                            Box(
                                Modifier.fillMaxSize()
                                    .graphicsLayer { alpha = photoAlpha.value }
                                    .drawBehind {
                                        // 暗角必须按照片「实际比例」计算并裁剪到照片实际显示矩形，而非 1:1 方框：
                                        val pw = bmp.width.toFloat()
                                        val ph = bmp.height.toFloat()
                                        val scale = kotlin.math.min(size.width / pw, size.height / ph)
                                        val dispW = pw * scale
                                        val dispH = ph * scale
                                        val dispX = (size.width - dispW) / 2f
                                        val dispY = (size.height - dispH) / 2f
                                        val vigRadius = kotlin.math.hypot(dispW / 2f, dispH / 2f)
                                        clipRect(
                                            left = dispX, top = dispY,
                                            right = dispX + dispW, bottom = dispY + dispH,
                                        ) {
                                            drawRect(
                                                Brush.radialGradient(
                                                    colorStops = arrayOf(
                                                        0f to Color.Transparent,
                                                        0.4f to Color.Transparent,
                                                        1f to Color.Black.copy(alpha = vigAlpha),
                                                    ),
                                                    center = center,
                                                    radius = vigRadius,
                                                ),
                                            )
                                        }
                                    },
                            )
                        }
                    }
                }

            }

            // 底部面板：上半参数 + 下半菜单。悬浮圆角矩形卡片（四角圆角，与屏幕左右/底部留距离），
            // 高度随栏目（ADJUST/FILTER/FRAME）切换经 AnimatedContent 自动变化
            Column(
                Modifier.fillMaxWidth()
                    .graphicsLayer { alpha = bottomAlpha.value; translationY = bottomOffset.value }
                    .navigationBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                    // 操控区背景：深色表面(系统深色 或 莫奈取色很黑)→深灰、浅色→奶油白，不跟随照片主色。
                    // 深色表面额外加 elevation 阴影：页面深色底(0xFF242424)与深灰表面(0xFF2E2E2E)仅差 4%，
                    // 不加点阴影会被"吸"进页面、看起来像没背景；阴影让深灰卡片清晰独立。
                    .then(if (darkSurface) Modifier.shadow(12.dp, RoundedCornerShape(28.dp), ambientColor = Color.Black, spotColor = Color.Black) else Modifier)
                    .background(editorCardBg, RoundedCornerShape(28.dp))
                    .padding(bottom = 6.dp),
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
                        EditorTab.ADJUST -> {
                            Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                                // 本栏（第一栏）所有滑块标签共享一个最小字号，保证整列标签大小一致
                                val grp = rememberColumnMinFontSize()
                                // 黑白：与拍照前「黑白」滤镜一一对应（0~1，0=关闭）；预览与落盘共用 grayscale
                                EditorSlider(stringResource(R.string.editor_slider_grayscale), state.grayscale, { onStateChange(state.copy(grayscale = it)) }, sliderColors, fg, onCommit,
                                    onReset = { onStateChange(state.copy(grayscale = 0f)); onCommit() }, resetBg = capsuleBg, group = grp)
                                Spacer(Modifier.height(10.dp))
                                // 亮度：与拍照前「亮度」滤镜一一对应（曝光通道，0.5=中性）
                                EditorSlider(stringResource(R.string.editor_slider_brightness), state.exp, { onStateChange(state.copy(exp = it)) }, sliderColors, fg, onCommit,
                                    onReset = { onStateChange(state.copy(exp = 0.5f)); onCommit() }, resetBg = capsuleBg, group = grp)
                                Spacer(Modifier.height(10.dp))
                                EditorSlider(stringResource(R.string.editor_slider_contrast), state.con, { onStateChange(state.copy(con = it)) }, sliderColors, fg, onCommit,
                                    onReset = { onStateChange(state.copy(con = 0.5f)); onCommit() }, resetBg = capsuleBg, group = grp)
                                Spacer(Modifier.height(10.dp))
                                EditorSlider(stringResource(R.string.editor_slider_saturation), state.sat, { onStateChange(state.copy(sat = it)) }, sliderColors, fg, onCommit,
                                    onReset = { onStateChange(state.copy(sat = 0.5f)); onCommit() }, resetBg = capsuleBg, group = grp)
                                Spacer(Modifier.height(10.dp))
                                // 暗角：滑块 >0 自动开启 vig，=0 关闭；预览与落盘共用 vigInt（0~1 强度）
                                EditorSlider(stringResource(R.string.editor_slider_vignette), state.vigInt, { onStateChange(state.copy(vigInt = it, vig = it > 0.001f)) }, sliderColors, fg, onCommit,
                                    onReset = { onStateChange(state.copy(vigInt = 0f, vig = false)); onCommit() }, resetBg = capsuleBg, group = grp)
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
                        EditorTab.FRAME -> {
                            EditorFramePanel(
                                state = state,
                                onStateChange = onStateChange,
                                onCommit = onCommit,
                                isDark = isDark,
                                fg = fg,
                            )
                        }
                    }
                }
                // 下半部分：菜单栏（调节 / 滤镜 / 相框），图标按钮、左对齐；
                // 卡片自带底色，按钮背景只落在"选中项"（铁锈红），未选中透明
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically,
                ) {
                    EditorTab.values().forEach { t ->
                        val label = when (t) { EditorTab.ADJUST -> stringResource(R.string.editor_tab_adjust); EditorTab.FILTER -> stringResource(R.string.editor_tab_filter); EditorTab.FRAME -> stringResource(R.string.editor_tab_frame) }
                        val icon = when (t) { EditorTab.ADJUST -> Icons.Filled.Tune; EditorTab.FILTER -> Icons.Filled.PhotoFilter; EditorTab.FRAME -> Icons.Filled.CropPortrait }
                        val selected = state.tab == t
                        Box(
                            Modifier.padding(horizontal = 6.dp).clip(RoundedCornerShape(18.dp))
                                .background(if (selected) RetroRust else Color.Transparent)
                                .clickable { onStateChange(state.copy(tab = t)) }
                                .padding(horizontal = 22.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) { Icon(icon, contentDescription = label, tint = if (selected) Color.White else fg, modifier = Modifier.size(20.dp)) }
                    }
                }
            }
        }
    }

    // 返回确认丢弃修改（可一并还原为裁切前的原图，需求②）
    if (showDiscard) {
        val canRestore = origBackupOf(frameFile).exists()
        // 弹窗底部按钮共享一个最小字号：整行按钮文字大小一致
        val discardBtnMin = rememberColumnMinFontSize()
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text(stringResource(R.string.editor_discard_title)) },
            text = { Text(if (canRestore) stringResource(R.string.editor_discard_text_can_restore) else stringResource(R.string.editor_discard_text)) },
            confirmButton = { TextButton(onClick = { showDiscard = false; requestExit(onCancel) }) { AutoSizeText(stringResource(R.string.editor_discard), maxFontSize = 14.sp, group = discardBtnMin) } },
            dismissButton = {
                Row {
                    if (canRestore) {
                        TextButton(onClick = { showDiscard = false; requestExit(onRestoreOriginal) }) { AutoSizeText(stringResource(R.string.editor_restore_original), maxFontSize = 14.sp, group = discardBtnMin) }
                    }
                    TextButton(onClick = { showDiscard = false }) { AutoSizeText(stringResource(R.string.editor_continue_edit), maxFontSize = 14.sp, group = discardBtnMin) }
                }
            },
        )
    }
}

/** 拍立得相框颜色预设（复古胶片风，不透明）。与设置页一致。 */
private val FRAME_SOLID_COLORS = listOf(
    0xFFFFFFFF.toInt(), // 白
    0xFFEDE0C8.toInt(), // 奶油
    0xFFF2E2B8.toInt(), // 米黄
    0xFFD8C3A5.toInt(), // 沙色
    0xFF8A5A3B.toInt(), // 复古棕
    0xFF4B3621.toInt(), // 莫卡
    0xFF1A1A1A.toInt(), // 墨黑
    0xFF2C3E50.toInt(), // 深蓝
)

/** 毛玻璃选择指示的圆点笔刷：中心白色光斑径向渐变至透明，配合 blur 模拟毛玻璃质感。 */
private val frostedDotBrush = Brush.radialGradient(
    listOf(Color(0xE6FFFFFF), Color(0x00FFFFFF)),
)

/** 滤镜轮播预设：每个预设对应一组编辑状态（暖/冷/原图），底部滑块控制其强度。黑白已移至「调节」栏。 */
private enum class FilterId { ORIGIN, WARM, COOL }
private data class FilterPreset(val id: FilterId, val nameRes: Int, val warm: Boolean, val cool: Boolean, val hasSlider: Boolean)
private val FILTER_PRESETS = listOf(
    FilterPreset(FilterId.ORIGIN, R.string.filter_origin, false, false, false),
    FilterPreset(FilterId.WARM, R.string.filter_warm, true, false, true),
    FilterPreset(FilterId.COOL, R.string.filter_cool, false, true, true),
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
                    // 原图：画禁止 logo；暖/冷：启用可调色盘（黑白已移至「调节」栏，需求⑨）
                    when (p.id) {
                        FilterId.ORIGIN -> NoFilterIcon(Modifier.size(squareSize), color = fg)
                        else -> {
                            // 调色盘取值：暖/冷 = 饱和度(X)；Y(dotY) 仅作取色指示，不映射亮度
                            val dotX = when (p.id) {
                                FilterId.WARM, FilterId.COOL -> state.tintSat
                            }
                            val dotY = when (p.id) {
                                FilterId.WARM, FilterId.COOL -> state.tintBri
                            }
                            ColorSquare(
                                dotX = dotX, dotY = dotY,
                                onDotChange = { x, y ->
                                    onStateChange(
                                        when (p.id) {
                                            FilterId.WARM, FilterId.COOL -> state.copy(tintSat = x, tintBri = y)
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
                            modifier = Modifier.width(squareSize).height(36.dp),
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
            AutoSizeText(
                stringResource(FILTER_PRESETS[pagerState.currentPage].nameRes),
                color = fg, maxFontSize = 13.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 4.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * 相框页：拍立得边框【强制开启】（成品必须有相框，不提供关闭开关），
 * 提供预设色板 + HSV 自定义取色。颜色变更会写回 EditState.frameColor，与落盘一致。
 * 毛玻璃选中时：HSV 色线（色相/饱和度/明度）灰化且不可拖动（毛玻璃背景与自定义颜色互斥）。
 */
@Composable
private fun EditorFramePanel(
    state: EditState,
    onStateChange: (EditState) -> Unit,
    onCommit: () -> Unit,
    isDark: Boolean,
    fg: Color,
) {
    // 由当前 frameColor 推导 HSV（每次重组重算，保持与预设点击同步）
    val hsv = remember { FloatArray(3) }
    remember(state.frameColor) { AndroidColor.colorToHSV(state.frameColor, hsv) }
    // 毛玻璃模式下 HSV 色线禁用：轨道/滑钮/文字统一灰化
    val hsvEnabled = !state.isFrosted
    val frameSlider = SliderDefaults.colors(
        thumbColor = if (isDark) Color.White else RetroRust,
        activeTrackColor = if (isDark) Color(0xFFCFCFCF) else RetroRust,
        inactiveTrackColor = if (isDark) Color(0xFF3A3A3A) else Color(0xFF5A5147),
        disabledThumbColor = if (isDark) Color(0xFF6E6E6E) else Color(0xFFB0A79A),
        disabledActiveTrackColor = if (isDark) Color(0xFF555555) else Color(0xFFC4BBAD),
        disabledInactiveTrackColor = if (isDark) Color(0xFF2E2E2E) else Color(0xFFD8D0C4),
    )
    val hsvLabelColor = if (hsvEnabled) fg else fg.copy(alpha = 0.35f)
    // 本栏（最后一栏）所有标签共享一个最小字号，保证整列标签大小一致
    val grp = rememberColumnMinFontSize()
    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        // 颜色（相框恒开启）
        AutoSizeText(stringResource(R.string.editor_frame_color), maxFontSize = 14.sp, color = fg, group = grp)
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier.height(104.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            userScrollEnabled = false,
        ) {
            items(FRAME_SOLID_COLORS.size) { i ->
                val c = FRAME_SOLID_COLORS[i]
                val selected = !state.isFrosted && state.frameColor == c
                Box(
                    Modifier.size(40.dp).clip(CircleShape)
                        .background(Color(c))
                        .border(
                            if (selected) 3.dp else 1.dp,
                            if (selected) (if (isDark) RetroCream else RetroRust) else fg.copy(alpha = 0.4f),
                            CircleShape,
                        )
                        .clickable {
                            onStateChange(state.copy(frameColor = c, isFrosted = false)); onCommit()
                        },
                )
            }
            // 毛玻璃：50% 圆角胶囊，背景 = 素色底 + 中心"模糊圆点"（径向渐变白色光斑 + blur），模拟毛玻璃质感
            item(span = { GridItemSpan(2) }) {
                val selected = state.isFrosted
                Box(
                    Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(50))
                        .background(if (isDark) Color(0xFF3A3A3A) else Color(0xFFE7DFD1))
                        .border(
                            if (selected) 3.dp else 1.dp,
                            if (selected) (if (isDark) RetroCream else RetroRust) else fg.copy(alpha = 0.4f),
                            RoundedCornerShape(50),
                        )
                        .clickable { onStateChange(state.copy(frame = true, isFrosted = true)); onCommit() },
                    contentAlignment = Alignment.Center,
                ) {
                    // 模糊圆点（API<31 时 blur 为 no-op，径向渐变本身已是柔化光斑，观感仍成立）
                    Box(Modifier.size(72.dp).blur(16.dp).background(frostedDotBrush, CircleShape))
                    AutoSizeText(stringResource(R.string.frame_frosted), modifier = Modifier.fillMaxWidth(), maxFontSize = 13.sp, color = fg, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, group = grp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        // HSV 色线：与「调节」栏同款布局（左文字 + 右滑块，EditorSlider）；
        // 毛玻璃选中时灰化禁用（enabled=false），避免拖动把选择切回纯色
        EditorSlider(stringResource(R.string.frame_hue), hsv[0] / 360f, {
            hsv[0] = it * 360f
            onStateChange(state.copy(frameColor = AndroidColor.HSVToColor(hsv), isFrosted = false))
        }, frameSlider, hsvLabelColor, onCommit, enabled = hsvEnabled, group = grp)
        Spacer(Modifier.height(10.dp))
        EditorSlider(stringResource(R.string.frame_saturation), hsv[1], {
            hsv[1] = it
            onStateChange(state.copy(frameColor = AndroidColor.HSVToColor(hsv), isFrosted = false))
        }, frameSlider, hsvLabelColor, onCommit, enabled = hsvEnabled, group = grp)
        Spacer(Modifier.height(10.dp))
        EditorSlider(stringResource(R.string.frame_value), hsv[2], {
            hsv[2] = it
            onStateChange(state.copy(frameColor = AndroidColor.HSVToColor(hsv), isFrosted = false))
        }, frameSlider, hsvLabelColor, onCommit, enabled = hsvEnabled, group = grp)
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
 * 手动"外切旋转"：把源图绕中心旋转 [deg] 度，并放大到恰好覆盖原图（cover），
 * 输出与原图同尺寸 Bitmap。放大后的内容会溢出原图边界（裁切到同尺寸），即"白框不动、图片放大旋转"。
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

/** 调节页的单行滑块：标签 + 滑条（高度压缩至 36dp，比默认 48dp 更紧凑）；松手时回调查 checkpoint（避免拖动过程产生大量历史点）。 */
@Composable
private fun EditorSlider(label: String, value: Float, onValueChange: (Float) -> Unit, colors: SliderColors, labelColor: Color, onChangeFinished: () -> Unit = {}, onReset: (() -> Unit)? = null, resetBg: Color = Color.Transparent, enabled: Boolean = true, group: com.genkaim.picocam.ui.ColumnMinFontSize? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        // 标签列固定 72dp，右侧留 8dp 间距，避免分组统一字号后文字紧贴滑块
        AutoSizeText(label, modifier = Modifier.width(72.dp).padding(end = 8.dp), maxFontSize = 14.sp, color = labelColor, group = group)
        Slider(value = value, onValueChange = onValueChange, valueRange = 0f..1f, modifier = Modifier.weight(1f).height(36.dp), colors = colors, onValueChangeFinished = onChangeFinished, enabled = enabled)
        if (onReset != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(resetBg)
                    .clickable { onReset() },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.cd_restore), tint = labelColor) }
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
    ) { AutoSizeText(text, modifier = Modifier.fillMaxWidth(), maxFontSize = 13.sp, color = fg, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center) }
}

/** 底部缩略图：按照片真实比例（非正方形）显示；选中态描边随深浅色（深色=灰、浅色=深棕）。 */
@Composable
private fun ThumbItem(file: File, selected: Boolean, isDark: Boolean, version: Long = 0L, onClick: () -> Unit) {
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
        contentDescription = stringResource(R.string.cd_view_photo),
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
        val chooser = Intent.createChooser(sendIntent, context.getString(R.string.camera_share_single)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.toast_share_failed, e.localizedMessage ?: context.getString(R.string.unknown_error)), Toast.LENGTH_SHORT).show()
    }
}
