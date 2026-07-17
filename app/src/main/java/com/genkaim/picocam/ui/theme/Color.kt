package com.genkaim.picocam.ui.theme

import androidx.compose.ui.graphics.Color

// ── 复古胶片暖色调 ──
val RetroPaper = Color(0xFFEDE0C8)       // 牛皮纸米黄（主背景）
val RetroCream = Color(0xFFF5EDE2)       // 奶油白（控件底色）
val RetroBrown = Color(0xFF5C4033)       // 深棕（主文字/边框）
val RetroBrownLight = Color(0xFF8C7A6B)  // 浅棕（副文字）
val RetroBrownDark = Color(0xFF2E1F16)   // 深棕近黑（取景框底色）
val RetroAmber = Color(0xFFC18A3D)       // 暗金（点缀线）
val RetroAmberDark = Color(0xFF8B6914)   // 深金（副铭牌）
val RetroRust = Color(0xFF8B4513)        // 铁锈红（快门中心/激活态）
val RetroInk = Color(0xFF2B241F)         // 墨黑（正文）

// 强调/警示色（淡红）：用于"还原"按钮文字，提示此操作会清空所有滤镜
val RetroSoftRed = Color(0xFFB85450)     // 偏砖红的淡色（复古调，与 RetroRust 相近但更柔）

// 深色模式背景（中性深灰）
val RetroDarkBg = Color(0xFF242424)       // 主背景深灰
val RetroDarkSurface = Color(0xFF2E2E2E)  // 卡片/控件深灰（略亮于背景）

// ── 深色模式前景（文字/图标）：深灰背景上统一用白色，浅色模式沿用复古棕系 ──
fun onSurface(isDark: Boolean): Color = if (isDark) Color.White else RetroBrown
fun onInk(isDark: Boolean): Color = if (isDark) Color.White else RetroInk
fun onSurfaceSoft(isDark: Boolean): Color = if (isDark) Color(0xFFCFCFCF) else RetroBrownLight
fun surfaceCard(isDark: Boolean): Color = if (isDark) RetroDarkSurface else RetroCream
