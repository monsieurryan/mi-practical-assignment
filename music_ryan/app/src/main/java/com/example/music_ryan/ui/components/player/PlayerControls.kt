package com.example.music_ryan.ui.components.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.music_ryan.R
import com.example.music_ryan.ui.theme.MiSans
import com.example.music_ryan.ui.theme.PlayerStyles
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.animation.core.tween

@Composable
fun PlayerControls(
    currentPosition: Long,
    duration: Long,
    isPlaying: Boolean,
    isShuffleEnabled: Boolean = false,
    isRepeatEnabled: Boolean = false,
    currentTrackId: Int? = null,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onRetry: () -> Unit = {}
) {
    // 确保 duration 非负
    val safeDuration = if (duration <= 0L) 1L else duration
    // 确保 currentPosition 在有效范围内
    val safePosition = currentPosition.coerceIn(0L, safeDuration)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 32.dp, vertical = 32.dp)
    ) {
        // 使用自定义进度条替代Slider
        ModernProgressBar(
            progress = if (safeDuration > 0) safePosition.toFloat() / safeDuration.toFloat() else 0f,
            currentTrackId = currentTrackId,
            duration = safeDuration,
            onSeek = { progress -> 
                val newPosition = (progress * safeDuration).toLong()
                onSeek(newPosition) 
            },
            modifier = Modifier.fillMaxWidth()
        )

        // 时间显示
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(safePosition),
                color = PlayerStyles.textColor.copy(alpha = 0.7f),
                fontSize = PlayerStyles.timeFontSize,
                fontFamily = MiSans
            )
            Text(
                text = formatTime(safeDuration),
                color = PlayerStyles.textColor.copy(alpha = 0.7f),
                fontSize = PlayerStyles.timeFontSize,
                fontFamily = MiSans
            )
        }

        Spacer(modifier = Modifier.height(36.dp))
        
        // 播放控制按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 随机播放按钮
            Image(
                painter = painterResource(id = R.drawable.ic_shuffle),
                contentDescription = "Shuffle",
                modifier = Modifier
                    .size(PlayerStyles.controlIconSize)
                    .clickable { onShuffle() },
                // 根据随机播放状态设置不同的透明度
                alpha = if (isShuffleEnabled) 1f else 0.5f
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(42.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_skip_previous),
                    contentDescription = "Previous",
                    modifier = Modifier
                        .size(26.dp)
                        .clickable { onPrevious() }
                )
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.2f),
                                    Color.White.copy(alpha = 0.1f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .clickable { onPlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(
                            id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                        ),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(28.dp)
                    )
                }
                Image(
                    painter = painterResource(id = R.drawable.ic_skip_next),
                    contentDescription = "Next",
                    modifier = Modifier
                        .size(26.dp)
                        .clickable { onNext() }
                )
            }

            // 单曲循环按钮
            Image(
                painter = painterResource(id = R.drawable.ic_repeat),
                contentDescription = "Repeat",
                modifier = Modifier
                    .size(PlayerStyles.controlIconSize)
                    .clickable { onRepeat() },
                // 根据单曲循环状态设置不同的透明度
                alpha = if (isRepeatEnabled) 1f else 0.5f
            )
        }
    }
}

@Composable
fun ModernProgressBar(
    progress: Float,
    currentTrackId: Int? = null,
    duration: Long = 0L,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf(progress) }
    
    // 使用key来重置组件状态
    // 每次歌曲ID或时长变化时重置整个组件
    key(currentTrackId, duration) {
        // 在歌曲ID改变或歌曲时长改变时重置拖动状态和缓存的进度
        LaunchedEffect(Unit) {
            isDragging = false
            dragProgress = progress
            println("ModernProgressBar: 重置进度条状态 - trackId: $currentTrackId, duration: $duration")
        }
        
        // 更新 dragProgress 以跟踪外部进度变化
        LaunchedEffect(progress) {
            if (!isDragging) {
                dragProgress = progress
            }
        }
        
        val effectiveProgress = if (isDragging) dragProgress else progress
        
        // 进度条动画 - 使用更平滑的动画，但减少延迟以提高响应性
        val animatedProgress by animateFloatAsState(
            targetValue = effectiveProgress,
            animationSpec = tween(durationMillis = 100), // 减少动画时间提高响应性
            label = "ProgressAnimation"
        )
        
        // 缓冲效果 - 总是稍微超过实际进度，但最多提前10%
        val bufferProgress = (progress + 0.1f).coerceAtMost(1f)
        
        Box(
            modifier = modifier
                .height(40.dp) // 增加高度以增大可点击区域
                .onGloballyPositioned { size = it.size }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (size.width > 0) {
                            val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                            dragProgress = newProgress
                            // 直接调用 onSeek 通知宿主组件
                            onSeek(newProgress)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            // 设置拖动状态并立即更新进度值
                            isDragging = true
                            if (size.width > 0) {
                                dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                            }
                        },
                        onDragEnd = {
                            // 先通知宿主组件，然后再设置拖动状态，避免UI跳变
                            onSeek(dragProgress)
                            // 使用协程延迟一点点时间再重置状态，避免跳变
                            isDragging = false
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            // 消耗触摸事件，确保其他组件不会接收到这次事件
                            change.consume()
                            
                            // 更新进度值
                            if (size.width > 0) {
                                val delta = dragAmount / size.width
                                // 使用较低的移动阈值，提高灵敏度
                                dragProgress = (dragProgress + delta).coerceIn(0f, 1f)
                            }
                        }
                    )
                }
        ) {
            // 背景轨道
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PlayerStyles.progressBarInactiveColor.copy(alpha = 0.3f))
            )
            
            // 缓冲条
            Box(
                modifier = Modifier
                    .fillMaxWidth(bufferProgress)
                    .height(4.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PlayerStyles.progressBarActiveColor.copy(alpha = 0.4f))
            )
            
            // 进度条
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(4.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                PlayerStyles.progressBarActiveColor,
                                PlayerStyles.progressBarActiveColor.copy(alpha = 0.8f)
                            )
                        )
                    )
            )
            
            // 拖动手柄 - 修复位置计算逻辑
            val thumbRadius = 8.dp // 手柄半径
            Box(
                modifier = Modifier
                    .size(thumbRadius * 2) // 确保宽高一致
                    .offset(
                        x = with(LocalDensity.current) {
                            // 计算手柄位置，考虑手柄大小确保位置准确
                            val widthPx = size.width.toFloat()
                            val thumbRadiusPx = thumbRadius.toPx()
                            val positionPx = if (widthPx > 0) animatedProgress * widthPx else 0f
                            
                            // 从中心点减去半径以保证中心在正确位置，并确保不为负
                            (positionPx - thumbRadiusPx).coerceAtLeast(0f).toDp()
                        }
                    )
                    .align(Alignment.CenterStart)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White,
                                Color.White.copy(alpha = 0.9f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .padding(4.dp)
            ) {
                // 拖动手柄内部小圆点
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(PlayerStyles.progressBarActiveColor)
                )
            }
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
} 