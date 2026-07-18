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
import com.genkaim.picocam.camera.EffectiveFilter
import com.genkaim.picocam.camera.buildFilterColorMatrix
import androidx.exifinterface.media.ExifInterface
import androidx.palette.graphics.Palette
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
            val strength = (eff.vignette * 0xB0).toInt().coerceIn(0, 255)
            val radius = kotlin.math.hypot(out.width / 2f, out.height / 2f)
            val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    out.width / 2f, out.height / 2f, radius,
                    intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, android.graphics.Color.argb(strength, 0, 0, 0)),
                    floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP,
                )
            }
            canvas.drawRect(0f, 0f, out.width.toFloat(), out.height.toFloat(), vignettePaint)
        }
        return out
    }

    /** 从照片文件提取主色（莫奈取色）。与 PhotoViewerActivity 逻辑完全一致：
     *  缩小到 ~256px → Palette → getLightVibrantColor(getDominantColor(fallback))。 */
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
            palette.getLightVibrantColor(palette.getDominantColor(fallback))
        } catch (_: Exception) { fallback }
    }
}
