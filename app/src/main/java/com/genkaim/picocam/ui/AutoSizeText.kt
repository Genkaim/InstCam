package com.genkaim.picocam.ui

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.genkaim.picocam.dynamic.AppPrefs
import com.genkaim.picocam.dynamic.LangMode
import java.util.Locale

/**
 * 当前界面语言是否为中文（简体/繁体，或系统默认且系统语言为 zh）。
 * 中文文案不启用动态缩放，保持设计字号（避免中文被压得过小、破坏排版）。
 */
fun isCJKLanguage(): Boolean {
    val mode = AppPrefs.lang.value.mode
    return when (mode) {
        LangMode.SYSTEM -> Locale.getDefault().language.lowercase().startsWith("zh")
        LangMode.ZH_HANS, LangMode.ZH_HANT -> true
        else -> false
    }
}

/**
 * 中英分流的动态字号文本：
 * - 中文（简/繁）：用普通 Text 固定 maxFontSize，不缩放。
 * - 其它语言（如 English）：用 AutoSizeText 动态缩放，单行不换行（softWrap=false + maxLines=1）。
 * 用于效果面板、相机参数「自动」等位置——英文长词可自适应缩小，中文保持原字号。
 */
@Composable
fun LangAutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    maxFontSize: TextUnit = 14.sp,
    minFontSize: TextUnit = 9.sp,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    group: ColumnMinFontSize? = null,
) {
    if (isCJKLanguage()) {
        Text(
            text = text,
            modifier = modifier,
            color = color,
            fontSize = maxFontSize,
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            textAlign = textAlign,
            letterSpacing = letterSpacing,
            maxLines = 1,
            style = style,
        )
    } else {
        AutoSizeText(
            text = text,
            modifier = modifier,
            maxFontSize = maxFontSize,
            minFontSize = minFontSize,
            color = color,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            textAlign = textAlign,
            letterSpacing = letterSpacing,
            style = style,
            group = group,
        )
    }
}

/**
 * 一组共享最小字号的文本状态：组内所有 AutoSizeText 最终都收敛到「整组最窄所需的最小字号」，
 * 保证同一栏/同一列的标签字号完全一致（不会有的大、有的小）。
 * 在栏/列的根可组合处用 rememberColumnMinFontSize() 创建，再传给该栏内每个 AutoSizeText 的 group 参数。
 */
typealias ColumnMinFontSize = MutableState<Float?>

@Composable
fun rememberColumnMinFontSize(): ColumnMinFontSize {
    return remember { mutableStateOf<Float?>(null) }
}

/**
 * 动态文字大小：在可用宽度内自动缩放字号，保证单行显示、绝不换行（softWrap=false + maxLines=1）。
 * 仅当文字实际宽度超过容器约束时才缩小字号（从 maxFontSize 向下收敛到 minFontSize），正常短文字保持 maxFontSize。
 *
 * 若传入 group（同一栏/列共享的最小字号状态），则本文本最终字号 = 整组统一的最小值（group.value），
 * 每个成员都会在 onTextLayout 按「实际测量」重算自身所需字号（含溢出检查）并回报给组取最小——
 * 因此即使组值被提前设得偏大，仍会触发进一步收敛，不会出现整栏溢出。
 * 居中场景给 modifier 传 Modifier.fillMaxWidth() 并设置 textAlign。
 */
@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    maxFontSize: TextUnit,
    minFontSize: TextUnit = 9.sp,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    group: ColumnMinFontSize? = null,
) {
    // 分组模式：最终字号 = 整组已算出的统一最小值；未算出前先用 maxFontSize 渲染以便测量
    var fontSize by remember(text) { mutableStateOf(maxFontSize) }
    val targetSize: TextUnit = if (group != null) (group.value?.sp ?: maxFontSize) else fontSize
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = targetSize,
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        textAlign = textAlign,
        letterSpacing = letterSpacing,
        maxLines = 1,
        softWrap = false,
        style = style,
        onTextLayout = { result: TextLayoutResult ->
            if (group == null) {
                // 独立模式：仅在超宽时向下收敛
                if (result.didOverflowWidth && fontSize.value > minFontSize.value && result.multiParagraph.width > 0f) {
                    val ratio = (result.size.width.toFloat() / result.multiParagraph.width).coerceIn(0f, 1f)
                    val next = (fontSize.value * ratio).coerceAtLeast(minFontSize.value)
                    if (next < fontSize.value) fontSize = next.sp
                }
            } else {
                // 分组模式：始终按实际测量重算本文本所需字号（含溢出检查），回报给组取最小值
                val natural = if (result.didOverflowWidth && result.multiParagraph.width > 0f) {
                    val ratio = (result.size.width.toFloat() / result.multiParagraph.width).coerceIn(0f, 1f)
                    (targetSize.value * ratio).coerceAtLeast(minFontSize.value)
                } else {
                    targetSize.value
                }
                group.value = minOf(group.value ?: natural, natural)
            }
        },
    )
}
