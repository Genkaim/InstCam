package com.genkaim.picocam.camera

/**
 * 单个滤镜的参数（每个滤镜独立存储）。
 * - enabled：开关是否启用（与“选中编辑”解耦）
 * - intensity：滑块强度 0~1（所有滤镜都有）
 * - saturation：正方形 x（仅暖/冷用），0.5 = 基准，向右更鲜艳
 * - brightness：正方形 y（仅暖/冷用），0.5 = 基准，向下更暗
 */
data class FilterParams(
    val enabled: Boolean = false,
    val intensity: Float = 1f,
    val saturation: Float = 0.5f,
    val brightness: Float = 0.5f,
)

/**
 * 预览（GLSL）与保存（ColorMatrix）共用的“有效参数”，
 * 由 [CameraViewModel] 把各滤镜的开关/强度/正方形位置合并得到。
 */
data class EffectiveFilter(
    val grayscale: Float = 0f,  // 0~1  （黑白）
    val vignette: Float = 0f,   // 0~1  （暗角）
    val exposure: Float = 0f,   // -1~1 （曝光，2^exposure 乘法）
    val warmth: Float = 0f,     // -1~1 （暖色正 / 冷色负）
    val saturation: Float = 0f, // -1~1 （饱和度，setSaturation(1+s)）
    val brightness: Float = 0f, // -1~1 （亮度，线性乘法 1+brightness）
) {
    fun isIdentity(): Boolean =
        grayscale == 0f && vignette == 0f && exposure == 0f && warmth == 0f && saturation == 0f && brightness == 0f
}

/** 滤镜的固定顺序（与 UI 列表一致）。 */
val FILTER_KEYS = listOf("黑白", "暗角", "亮度", "暖色", "冷色")

/** 亮度滤镜的默认强度：0.5 = 正常，>0.5 变亮，<0.5 变暗。 */
const val BRIGHTNESS_KEY = "亮度"
const val BRIGHTNESS_DEFAULT = 0.5f
