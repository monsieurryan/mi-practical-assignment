package com.example.music_ryan.ui.components.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.music_ryan.ui.theme.MiSans
import com.example.music_ryan.ui.theme.PlayerStyles
import androidx.compose.foundation.layout.offset

@Composable
fun SongInfo(
    musicName: String,
    author: String,
    coverUrl: String,
    isLoading: Boolean = false,
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier,
    coverScale: Float = 1f,
    coverCornerRadius: Float = 0f,
    coverPositionY: Float = 0f // 0表示中央位置，1表示底部
) {
    // 计算封面垂直位置偏移量
    // 当coverPositionY为0时，封面在垂直中心位置
    // 当coverPositionY为1时，封面向下偏移，位于底部位置
    val verticalOffset = if (coverPositionY > 0f) {
        // 计算垂直偏移，最大偏移220dp（大致是屏幕高度的1/3）
        220.dp * coverPositionY
    } else {
        0.dp
    }
    
    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = PlayerStyles.horizontalPadding)
                .offset(y = verticalOffset), // 应用垂直偏移
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 封面旋转动画
            val infiniteTransition = rememberInfiniteTransition(label = "albumRotation")
            val rotationAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 20000, // 20秒旋转一圈
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotationAnimation"
            )
            
            // 当前旋转状态
            var currentRotation by remember { mutableStateOf(0f) }
            
            // 根据播放状态控制旋转角度
            val rotation = if (isPlaying) {
                // 播放时使用动画角度
                rotationAngle
            } else {
                // 暂停时保持当前角度
                currentRotation
            }
            
            // 更新当前旋转角度
            LaunchedEffect(rotationAngle, isPlaying) {
                if (isPlaying) {
                    currentRotation = rotationAngle
                }
            }
            
            // 封面
            Box(
                modifier = Modifier
                    .size(PlayerStyles.albumArtSize * coverScale)
                    // 边框装饰效果
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.2f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        ),
                        shape = if (coverCornerRadius > 0) 
                                RoundedCornerShape(coverCornerRadius.dp) 
                                else CircleShape
                    )
                    .padding(8.dp)
                    .clip(if (coverCornerRadius > 0) 
                            RoundedCornerShape(coverCornerRadius.dp) 
                            else CircleShape)
                    .background(if (isLoading) Color(0xFF2D2D2D) else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Album Art",
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(rotation), // 应用旋转角度
                    contentScale = ContentScale.Crop
                )
                
                // 中心唱片孔装饰，仅在非圆角模式下显示
                if (coverCornerRadius <= 0) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center)
                            .background(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = CircleShape
                            )
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = Color.White.copy(alpha = 0.8f),
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }

            // 歌曲信息，仅在非动画过渡状态下显示（封面位于中心位置且接近原始大小）
            if (coverScale >= 0.95f && coverPositionY <= 0.1f) {
                Text(
                    text = musicName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    fontFamily = MiSans,
                    color = Color.White
                )
                
                Text(
                    text = author,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    fontFamily = MiSans,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
} 