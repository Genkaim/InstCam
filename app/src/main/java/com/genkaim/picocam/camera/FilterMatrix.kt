package com.genkaim.picocam.camera

import android.graphics.ColorMatrix
import kotlin.math.pow

/**
 * 把 [EffectiveFilter] 构建为单个 ColorMatrix，预览（RenderEffect）与保存（Canvas）共用，
 * 保证两路滤镜效果一致。操作顺序必须与 GLSL 着色器（filter_fragment.glsl）严格一致：
 * 曝光 → 色温 → 饱和度 → 灰度。
 *
 * 关键（修复"冷色下往右饱和度反而变低"）：色温必须在饱和度【之前】叠加。
 * 若像旧实现那样把色温放到饱和度之后，色温的蓝移只是最后一层"平坦加色"、不会被饱和度放大：
 * 冷色时往右加饱和度只是放大画面原有（偏暖/中性）色、再糊上一层平蓝 → 显得浑浊；
 * 而往左（去饱和→灰度）再加平蓝反而像更干净、更"饱和"的蓝，于是观感上左右颠倒。
 * 把色温放到饱和度之前，蓝移会随饱和度一起被放大，往右才是更浓的蓝（与暖色对称）。
 */
fun buildFilterColorMatrix(eff: EffectiveFilter): ColorMatrix {
    // ColorMatrix.postConcat(M) 语义为 cm = M * cm，即 M 在【已有变换之后】作用。
    // 依次 postConcat(曝光, 色温, 饱和度, 灰度) → 像素实际经历顺序即 曝光→色温→饱和度→灰度，与 shader 对齐。
    val cm = ColorMatrix()
    // 曝光：shader 为 color *= pow(2, uExposure)；逐通道乘 2^exposure
    if (eff.exposure != 0f) {
        val f = 2.0.pow(eff.exposure.toDouble()).toFloat()
        cm.postConcat(scaleMatrix(f))
    }
    // 色温：shader 为 color.r += uWarmth*0.15 / color.b -= uWarmth*0.15（加性）——须在饱和度之前
    if (eff.warmth != 0f) cm.postConcat(warmthMatrix(eff.warmth))
    // 饱和度：shader 为 mix(luma, color, 1+uSaturation)；ColorMatrix.setSaturation(1+s) 等价
    if (eff.saturation != 0f) cm.postConcat(ColorMatrix().apply { setSaturation(1f + eff.saturation) })
    // 灰度（黑白）：最后叠加，覆盖彩色；shader 为 mix(color, luma, uGrayscale)
    if (eff.grayscale > 0f) cm.postConcat(grayscaleMatrix(eff.grayscale))
    return cm
}

private fun scaleMatrix(f: Float) = ColorMatrix(floatArrayOf(
    f, 0f, 0f, 0f, 0f,
    0f, f, 0f, 0f, 0f,
    0f, 0f, f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f,
))

/** 色温加性矩阵：R += w*0.15*255，B -= w*0.15*255（匹配 GLSL）。 */
private fun warmthMatrix(w: Float) = ColorMatrix(floatArrayOf(
    1f, 0f, 0f, 0f, w * 0.15f * 255f,
    0f, 1f, 0f, 0f, 0f,
    0f, 0f, 1f, 0f, -w * 0.15f * 255f,
    0f, 0f, 0f, 1f, 0f,
))

/** 灰度：setSaturation(1-g)，g=1 时完全去色。 */
private fun grayscaleMatrix(g: Float) = ColorMatrix().apply { setSaturation(1f - g) }
