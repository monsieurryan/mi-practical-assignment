package com.example.music_yishuai.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object PlayerStyles {
    // Colors
    val backgroundColor = Color(0xFF8B2323) // 深红色背景
    val textColor = Color.White
    val progressBarActiveColor = Color.White
    val progressBarInactiveColor = Color.White.copy(alpha = 0.3f)

    // Dimensions
    val maxWidth = 430.dp
    val topPadding = 16.dp
    val horizontalPadding = 24.dp
    val albumArtSize = 280.dp
    val controlIconSize = 24.dp
    val smallIconSize = 20.dp
    val progressBarHeight = 3.dp

    // Typography
    val titleFontSize = 20.sp
    val subtitleFontSize = 14.sp
    val timeFontSize = 12.sp
} 