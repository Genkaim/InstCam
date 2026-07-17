package com.genkaim.picocam.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// 固定一套复古配色，不随系统深浅切换，保证胶片质感统一
private val RetroColorScheme = lightColorScheme(
    primary = RetroBrown,
    onPrimary = RetroCream,
    secondary = RetroAmber,
    onSecondary = RetroInk,
    tertiary = RetroRust,
    background = RetroPaper,
    onBackground = RetroInk,
    surface = RetroCream,
    onSurface = RetroInk,
    error = RetroRust,
    onError = RetroCream,
)

@Composable
fun PicocamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RetroColorScheme,
        typography = RetroTypography,
        content = content,
    )
}
