package com.example.music_ryan.ui.components.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.music_ryan.data.model.LyricLine
import com.example.music_ryan.data.model.Lyrics
import com.example.music_ryan.ui.theme.MiSans
import kotlinx.coroutines.launch

/**
 * 歌词显示组件
 */
@Composable
fun LyricsView(
    lyrics: Lyrics,
    isLoading: Boolean,
    errorMessage: String?,
    currentPosition: Long,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White.copy(alpha = 0.7f)
            )
        } else if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
                fontFamily = MiSans,
                textAlign = TextAlign.Center
            )
        } else if (lyrics.lines.isEmpty()) {
            Text(
                text = "暂无歌词",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
                fontFamily = MiSans,
                textAlign = TextAlign.Center
            )
        } else {
            LyricsContent(
                lyrics = lyrics,
                currentPosition = currentPosition
            )
        }
    }
}

/**
 * 歌词内容显示
 */
@Composable
private fun LyricsContent(
    lyrics: Lyrics,
    currentPosition: Long
) {
    // 获取当前歌词行索引
    val currentLineIndex = lyrics.getCurrentLineIndex(currentPosition)

    // 列表状态
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // 当前行变化时，滚动到该行，使用更平滑的滚动效果
    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 0 && lyrics.lines.isNotEmpty()) {
            coroutineScope.launch {
                // 使用smoothScrollToItem替代animateScrollToItem以获得更平滑的滚动
                listState.animateScrollToItem(
                    index = currentLineIndex.coerceAtMost(lyrics.lines.size - 1),
                    scrollOffset = 0  // 明确指定偏移量为0，确保歌词行完全居中
                )
            }
        }
    }
    
    // 添加空间，让歌词列表在屏幕上有足够的显示空间
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 主歌词列表
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            // 增加垂直内边距，确保在滚动时上下有足够的空白区域
            contentPadding = PaddingValues(vertical = 200.dp, horizontal = 24.dp)
        ) {
            // 添加顶部空白项，减少滚动时顶部的闪烁
            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
            
            // 歌词行，使用更小的垂直内边距
            itemsIndexed(lyrics.lines) { index, line ->
                LyricLine(
                    lyric = line,
                    isActive = index == currentLineIndex,
                    // 减小垂直内边距以减少滚动距离
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
            
            // 添加底部空白项，减少滚动时底部的闪烁
            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

/**
 * 单行歌词显示
 */
@Composable
private fun LyricLine(
    lyric: LyricLine,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    // 使用动画来平滑过渡，但减小值的变化范围减少闪烁
    val alpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.6f,  // 将非活动歌词的透明度提高
        label = "alphaAnimation"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.05f else 1.0f,  // 减小放大比例，降低视觉冲击
        label = "scaleAnimation"
    )
    
    val fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
    val fontSize = if (isActive) 17.sp else 16.sp  // 减小字体大小差异
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = lyric.text,
            color = Color.White.copy(alpha = alpha),
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = MiSans,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .alpha(alpha)
                .scale(scale)
        )
    }
} 