package com.genkaim.picocam.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.location.Location
import com.genkaim.picocam.TintState
import com.genkaim.picocam.camera.EffectiveFilter
import com.genkaim.picocam.camera.buildFilterColorMatrix
import androidx.exifinterface.media.ExifInterface
import androidx.palette.graphics.Palette
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.roundToInt

/** 照片存储工具：存到 app 私有目录，拍照后自动添加拍立得白色边框。 */
object PhotoStorage {

    /** 缩略图长边目标尺寸（px）。对三列网格(~120dp)与详情底部缩略图条(~56dp)均足够清晰，
     *  文件仅约 20~40KB，磁盘读取/解码开销相比 8MB 原图可忽略不计。 */
    const val THUMB_SIZE = 400

    /** 加框解码目标长边（px）。原图常 4000px+，全分辨率解码最慢且最吃设备性能（不同机型时长差异主要来源）。
     *  采样到该值已足够屏幕/详情页满屏展示，decode + 滤镜 + 编码耗时大幅下降，跨设备更一致。 */
    private const val TARGET_EDGE_PX = 1600

    private fun getDir(context: Context): File =
        context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
            ?: File(context.filesDir, "pictures").also { it.mkdirs() }

    fun generateFile(context: Context): File {
        val dir = getDir(context).also { it.mkdirs() }
        val name = "InstCam_${System.currentTimeMillis()}.jpg"
        return File(dir, name)
    }

    /** 给原图添加拍立得白边，同时根据 EXIF 旋转纠正方向，可选写入 GPS 位置和应用滤镜效果。 */
    suspend fun addPolaroidFrame(
        file: File,
        location: Location? = null,
        eff: EffectiveFilter = EffectiveFilter(),
    ): File = withContext(Dispatchers.IO) {
        try {
            // 读取 EXIF 方向
            val orientation = try {
                val exif = ExifInterface(file.path)
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } catch (_: Exception) { ExifInterface.ORIENTATION_NORMAL }

            val src = decodeSampled(file.path) ?: return@withContext file

            // 根据 EXIF 旋转原图
            val rotated = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> {
                    val m = Matrix().apply { postRotate(90f) }
                    Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
                }
                ExifInterface.ORIENTATION_ROTATE_180 -> {
                    val m = Matrix().apply { postRotate(180f) }
                    Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> {
                    val m = Matrix().apply { postRotate(270f) }
                    Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
                }
                else -> src
            }
            if (rotated !== src) src.recycle()

            // 1:1 中心裁切：与取景框 FILL_CENTER 预览一致——用户看到什么就拍什么
            val side = minOf(rotated.width, rotated.height)
            val cropX = (rotated.width - side) / 2
            val cropY = (rotated.height - side) / 2
            val squared = Bitmap.createBitmap(rotated, cropX, cropY, side, side)
            if (squared !== rotated) rotated.recycle()

            // 若应用了滤镜：额外保存"无框无滤镜原图正方形"侧车，供编辑时作为无滤镜基图
            // （编辑默认选中拍照滤镜，基图须为未滤镜版本，避免对已有滤镜照片再次叠加滤镜）
            if (eff != EffectiveFilter()) {
                try {
                    FileOutputStream(sourceSquareFileFor(file)).use { out -> squared.compress(Bitmap.CompressFormat.JPEG, 92, out) }
                } catch (_: Exception) {}
            }

            // 应用滤镜效果到裁切后的正方形图
            val filtered = applyFilterToBitmap(squared, eff)
            squared.recycle()

            val border = (filtered.width * 0.06f).toInt().coerceAtLeast(16)
            val bottom = border * 6
            val w = filtered.width + border * 2
            val h = filtered.height + border + bottom
            val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bm)
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(filtered, border.toFloat(), border.toFloat(), null)
            filtered.recycle()
            FileOutputStream(file).use { out ->
                bm.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            // 用内存中的 bm 直接生成缩略图（避免再次解码刚写好的文件），省一次磁盘解码、降低跨设备耗时差异
            saveThumbnailFromBitmap(bm, thumbnailFileFor(file), THUMB_SIZE)
            bm.recycle()

            // 写入 EXIF：清除方向 + 写入 GPS
            try {
                val exif = ExifInterface(file.path)
                exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                if (location != null) {
                    val lat = decimalToDms(location.latitude)
                    val lng = decimalToDms(location.longitude)
                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, "${lat[0]}/1,${lat[1]}/1,${lat[2]}/100")
                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, if (location.latitude >= 0) "N" else "S")
                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "${lng[0]}/1,${lng[1]}/1,${lng[2]}/100")
                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, if (location.longitude >= 0) "E" else "W")
                }
                exif.saveAttributes()
            } catch (_: Exception) {}
        } catch (_: Exception) {}
        file
    }

    /** 按目标长边采样解码：避免直接解码 4000px+ 原图（最慢且最吃设备性能的步骤）。 */
    private fun decodeSampled(path: String): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val longEdge = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
        var sample = 1
        while (longEdge / sample / 2 > TARGET_EDGE_PX) sample *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    /** 由内存 Bitmap 直接生成缩略图（长边 [size]），避免重复磁盘解码。 */
    private fun saveThumbnailFromBitmap(src: Bitmap, thumbFile: File, size: Int) {
        val longEdge = maxOf(src.width, src.height).coerceAtLeast(1)
        val sample = maxOf(1, longEdge / size)
        val w = (src.width / sample).coerceAtLeast(1)
        val h = (src.height / sample).coerceAtLeast(1)
        val t = Bitmap.createScaledBitmap(src, w, h, true)
        thumbFile.parentFile?.mkdirs()
        FileOutputStream(thumbFile).use { fos -> t.compress(Bitmap.CompressFormat.JPEG, 82, fos) }
        t.recycle()
    }

    /** 给定原图文件，返回其缩略图文件（即便尚不存在也返回预期路径）。
     *  命名规则：原图 InstCam_xxx.jpg → InstCam_xxx_thumb.jpg（与原图同目录）。 */
    fun thumbnailFileFor(file: File): File =
        File(file.parentFile, "${file.nameWithoutExtension}_thumb.jpg")

    /** 返回列表/缩略图条应加载的图片：若存在缩略图则用缩略图，否则回退原图（旧照片或缩略图尚未生成时）。 */
    fun thumbnailFor(file: File): File {
        val t = thumbnailFileFor(file)
        return if (t.exists()) t else file
    }

    /**
     * 拍照「原图无框正方形」侧车文件：同目录、扩展名前加 .src（仅当拍照时应用了滤镜才存在）。
     * 这是未叠加任何滤镜、未加拍立得白边的正方形原图，作为编辑基图——
     * 编辑时默认选中拍照滤镜并把它叠加在该基图上，与相册中已滤镜的照片一致，避免双重叠加滤镜。
     */
    fun sourceSquareFileFor(file: File): File =
        File(file.parent, "${file.nameWithoutExtension}.src.jpg")

    /** 拍照「滤镜元信息」侧车文件（JSON）：记录拍照时应用的滤镜状态，供编辑默认选中。 */
    fun filterMetaFileFor(file: File): File =
        File(file.parent, "${file.nameWithoutExtension}.filter.json")

    /** 拍照滤镜元信息快照（与 EditState 一一对应，用于编辑时重建默认滤镜状态）。 */
    data class CapturedFilterMeta(
        val tint: TintState,
        val tintSat: Float, val tintBri: Float, val tintStrength: Float,
        val vignette: Float, val exposure: Float, val warmth: Float,
        val saturation: Float, val brightness: Float, val contrast: Float,
    )

    /** 读取「拍照原图无框正方形」侧车；不存在（拍照未用滤镜或文件已删）返回 null。 */
    fun loadSourceSquare(file: File): Bitmap? = runCatching {
        val f = sourceSquareFileFor(file)
        if (!f.exists()) null else BitmapFactory.decodeFile(f.path)
    }.getOrNull()

    /** 写入拍照滤镜元信息：tint 状态 + 取色盘位置/强度 + 完整 EffectiveFilter 数值（完整重建编辑态）。 */
    fun saveFilterMeta(file: File, eff: EffectiveFilter, tint: TintState, sat: Float, bri: Float, strength: Float) {
        try {
            val o = JSONObject()
            o.put("tint", tint.name)
            o.put("sat", sat); o.put("bri", bri); o.put("strength", strength)
            o.put("vignette", eff.vignette); o.put("exposure", eff.exposure)
            o.put("warmth", eff.warmth); o.put("saturation", eff.saturation)
            o.put("brightness", eff.brightness); o.put("contrast", eff.contrast)
            filterMetaFileFor(file).writeText(o.toString())
        } catch (_: Exception) {}
    }

    /** 读取拍照滤镜元信息；文件不存在或解析失败返回 null。 */
    fun loadFilterMeta(file: File): CapturedFilterMeta? = runCatching {
        val o = JSONObject(filterMetaFileFor(file).readText())
        CapturedFilterMeta(
            tint = TintState.valueOf(o.getString("tint")),
            tintSat = o.optDouble("sat", 0.5).toFloat(),
            tintBri = o.optDouble("bri", 0.5).toFloat(),
            tintStrength = o.optDouble("strength", 0.5).toFloat(),
            vignette = o.optDouble("vignette", 0.0).toFloat(),
            exposure = o.optDouble("exposure", 0.0).toFloat(),
            warmth = o.optDouble("warmth", 0.0).toFloat(),
            saturation = o.optDouble("saturation", 0.0).toFloat(),
            brightness = o.optDouble("brightness", 0.0).toFloat(),
            contrast = o.optDouble("contrast", 0.0).toFloat(),
        )
    }.getOrNull()

    /** 清除滤镜元信息侧车 + 原图侧车（无滤镜时不再需要）。 */
    fun clearFilterMeta(file: File) {
        filterMetaFileFor(file).delete()
        sourceSquareFileFor(file).delete()
    }

    /** 删除照片及其缩略图 / 原图侧车 / 滤镜元信息侧车。 */
    fun deletePhotoWithSidecars(file: File): Boolean {
        thumbnailFileFor(file).delete()
        sourceSquareFileFor(file).delete()
        filterMetaFileFor(file).delete()
        return file.delete()
    }

    /**
     * 由原图生成缩略图（长边 [size]），写入 [thumbnailFileFor] 路径。
     * 用 inSampleSize 直接解码到目标尺寸，避免先解码全分辨率再缩放的内存浪费。
     * 失败或不支持时返回 null（调用方回退原图）。
     */
    fun generateThumbnail(file: File, size: Int = THUMB_SIZE): File? = runCatching {
        val out = thumbnailFileFor(file)
        if (out.exists() && out.length() > 0) return@runCatching out
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
        var sample = 1
        val longEdge = if (bounds.outWidth > bounds.outHeight) bounds.outWidth else bounds.outHeight
        while (longEdge / sample / 2 > size) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bm = BitmapFactory.decodeFile(file.path, opts) ?: return@runCatching null
        out.parentFile?.mkdirs()
        FileOutputStream(out).use { fos -> bm.compress(Bitmap.CompressFormat.JPEG, 82, fos) }
        bm.recycle()
        out
    }.getOrNull()

    /** 将十进制度数转为 EXIF DMS 格式的三个整数值（度、分、毫秒）。 */
    private fun decimalToDms(decimal: Double): IntArray {
        val a = kotlin.math.abs(decimal)
        val d = a.toInt()
        val m = ((a - d) * 60).toInt()
        val s100 = ((a - d - m / 60.0) * 3600 * 100).roundToInt()
        return intArrayOf(d, m, s100)
    }

    fun listPhotos(context: Context, limit: Int = Int.MAX_VALUE): List<File> {
        val all = getDir(context)
            .listFiles { f -> f.extension.equals("jpg", ignoreCase = true) }
            ?.filter { !it.nameWithoutExtension.endsWith("_thumb") }   // 排除缩略图，避免相册把缩略图当独立照片重复展示
            ?.filter { !it.nameWithoutExtension.contains(".src") }      // 排除"原图无框正方形"侧车
            ?.filter { !it.nameWithoutExtension.contains(".orig") }     // 排除编辑复原用的原图备份
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
        return if (limit == Int.MAX_VALUE) all else all.take(limit)
    }

    /**
     * 拷贝出一份「去掉 GPS 位置信息」的副本，用于分享（保护隐私）。
     * 副本写在 cacheDir（FileProvider 已授权 cache-path 访问），调用方负责后续分享。
     * 若 EXIF 处理失败，仍返回带原 EXIF 的副本（不抛异常，分享照常进行）。
     */
    fun copyWithoutLocation(context: Context, src: File): File {
        val out = File(context.cacheDir, "share_noloc_${src.name}")
        src.copyTo(out, overwrite = true)
        try {
            val exif = ExifInterface(out.path)
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, null)
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, null)
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, null)
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, null)
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, null)
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, null)
            exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, null)
            exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, null)
            exif.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, null)
            exif.saveAttributes()
        } catch (_: Exception) {}
        return out
    }

    /** 对 Bitmap 应用滤镜效果（ColorMatrix 方式），参数与预览 RenderEffect 完全一致。 */
    fun applyFilterToBitmap(src: Bitmap, eff: EffectiveFilter): Bitmap {
        val cm = buildFilterColorMatrix(eff)

        val out = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        canvas.drawBitmap(src, 0f, 0f, paint)
        if (eff.vignette > 0f) {
            val strength = (eff.vignette * 0xD9).toInt().coerceIn(0, 255)
            val radius = kotlin.math.hypot(out.width / 2f, out.height / 2f)
            val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    out.width / 2f, out.height / 2f, radius,
                    intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, android.graphics.Color.argb(strength, 0, 0, 0)),
                    floatArrayOf(0f, 0.4f, 1f), Shader.TileMode.CLAMP,
                )
            }
            canvas.drawRect(0f, 0f, out.width.toFloat(), out.height.toFloat(), vignettePaint)
        }
        return out
    }

    /**
     * 裁掉拍立得白边，返回内正方形 Bitmap（无边框），供编辑器在编辑态展示/处理。
     * 边框由 addPolaroidFrame 按 `filtered.width * 0.06` 计算，故满图宽 W 与边距关系为 边距 ≈ 0.0536·W；
     * 这里用略大的 0.054·W 反推并裁切，保证白边被完全去掉（多裁 <1px 照片内容，不可见）。
     */
    fun cropInnerSquare(file: File): Bitmap? = runCatching {
        val src = decodeSampled(file.path) ?: return@runCatching null
        val w = src.width
        val borderCrop = (w * 0.054f).roundToInt().coerceAtLeast(1)
        val side = (w - 2 * borderCrop).coerceAtLeast(1)
        val sq = Bitmap.createBitmap(src, borderCrop, borderCrop, side, side)
        src.recycle()
        sq
    }.getOrNull()

    /**
     * 编辑器保存：对无边框正方形 [square] 应用滤镜、加回拍立得白边并写回 [file]（覆盖原图），
     * 同时更新缩略图、保留原图 GPS（addPolaroidFrame 仅在显式传 location 时写 GPS，编辑态从原文件继承）。
     */
    suspend fun reencodeWithFrame(context: Context, file: File, square: Bitmap, eff: EffectiveFilter): File = withContext(Dispatchers.IO) {
        try {
            val gps = readGpsTags(file)   // 先读原图 GPS，待写回
            val filtered = applyFilterToBitmap(square, eff)
            val border = (filtered.width * 0.06f).toInt().coerceAtLeast(16)
            val bottom = border * 6
            val w = filtered.width + border * 2
            val h = filtered.height + border + bottom
            val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bm)
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(filtered, border.toFloat(), border.toFloat(), null)
            filtered.recycle()
            FileOutputStream(file).use { out -> bm.compress(Bitmap.CompressFormat.JPEG, 95, out) }
            // 用内存 bm 直接生成缩略图（避免再次解码刚写好的文件），与 addPolaroidFrame 一致
            saveThumbnailFromBitmap(bm, thumbnailFileFor(file), THUMB_SIZE)
            bm.recycle()
            try {
                val exif = ExifInterface(file.path)
                exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                gps.forEach { (k, v) -> exif.setAttribute(k, v) }
                exif.saveAttributes()
            } catch (_: Exception) {}
        } catch (_: Exception) {}
        file
    }

    /**
     * 编辑器 CROP 页"旋转"参数：先把源图绕中心旋转 [degrees] 度，
     * 再从旋转后的结果中心裁出最大正方形并缩放/裁切到合适尺寸返回。
     * 不回收传入的 [src]（调用方负责），仅回收内部生成的旋转中间图。
     */
    fun rotateAndCropSquare(src: Bitmap, degrees: Float): Bitmap {
        val rotated = if (degrees == 0f) {
            src
        } else {
            val m = Matrix().apply { postRotate(degrees) }
            Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
        }
        val side = if (rotated.width < rotated.height) rotated.width else rotated.height
        val x = (rotated.width - side) / 2
        val y = (rotated.height - side) / 2
        val out = Bitmap.createBitmap(rotated, x, y, side, side)
        // 仅当中间旋转图与源、与输出都不同时才回收（degrees==0 时 rotated===src，不可回收）
        if (rotated !== src && rotated !== out) rotated.recycle()
        return out
    }

    /** 读取原图 EXIF 中的 GPS 相关字段（若存在），用于编辑后写回，保护位置信息。 */
    private fun readGpsTags(file: File): Map<String, String> = runCatching {
        val exif = ExifInterface(file.path)
        listOf(
            ExifInterface.TAG_GPS_LATITUDE, ExifInterface.TAG_GPS_LATITUDE_REF,
            ExifInterface.TAG_GPS_LONGITUDE, ExifInterface.TAG_GPS_LONGITUDE_REF,
            ExifInterface.TAG_GPS_ALTITUDE, ExifInterface.TAG_GPS_ALTITUDE_REF,
            ExifInterface.TAG_GPS_TIMESTAMP, ExifInterface.TAG_GPS_DATESTAMP,
            ExifInterface.TAG_GPS_PROCESSING_METHOD,
        ).mapNotNull { k -> exif.getAttribute(k)?.let { v -> k to v } }.toMap()
    }.getOrDefault(emptyMap())

    /** 从照片文件提取主色（莫奈取色）。与 PhotoViewerActivity 逻辑完全一致：
     *  缩小到 ~256px → Palette → 低饱和 Muted 色板（getLightMutedColor 优先，退 Muted→LightVibrant→Dominant）。 */
    suspend fun extractDominantColor(file: File, fallback: Int): Int = withContext(Dispatchers.IO) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            val target = 256
            var sample = 1
            var w = bounds.outWidth; var h = bounds.outHeight
            while (maxOf(w, h) / sample > target * 2) { sample *= 2 }
            val opts = BitmapFactory.Options().apply { inSampleSize = sample; inJustDecodeBounds = false }
            val bm = BitmapFactory.decodeFile(file.path, opts) ?: return@withContext fallback
            val palette = Palette.from(bm).generate()
            bm.recycle()
            // 官方 Palette 指导：Muted 系列为低饱和色板，背景柔和淡彩，不与照片抢色。
            palette.getLightMutedColor(
                palette.getMutedColor(
                    palette.getLightVibrantColor(palette.getDominantColor(fallback))
                )
            )
        } catch (_: Exception) { fallback }
    }
}
