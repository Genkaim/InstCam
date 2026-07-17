package com.genkaim.picocam.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.genkaim.picocam.ui.theme.RetroBrown
import com.genkaim.picocam.ui.theme.RetroCream

@Composable
fun ShutterButton(onClick: () -> Unit, isDark: Boolean = false, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale = if (pressed) 0.88f else 1f

    // 深色模式：黑底白边；浅色模式：奶油底棕边
    val bg = if (isDark) Color(0xFF1A1A1A) else RetroCream
    val outerBorder = if (isDark) Color.White else RetroBrown
    val innerBorder = if (isDark) Color(0xFF555555) else Color(0xFFE6DCC9)

    Box(
        modifier = modifier
            .size(78.dp)
            .scale(scale)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .clip(CircleShape)
            .background(bg)
            .border(3.dp, outerBorder, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .border(1.dp, innerBorder, CircleShape),
        )
    }
}
