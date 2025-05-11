package com.example.music_yishuai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.music_yishuai.R
import com.example.music_yishuai.data.model.Song
import com.example.music_yishuai.ui.theme.MiSans

enum class SongCardStyle {
    SMALL,      // 小卡片，一行多个
    MEDIUM,     // 中等卡片，一行一个，两边可见
    LARGE       // 大卡片，一行一个，填充屏幕
}

@Composable
fun SongCard(
    song: Song,
    style: SongCardStyle = SongCardStyle.SMALL,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    onCardClick: () -> Unit = {}
) {
    // 根据样式设置不同的尺寸和圆角
    val (width, height, cornerRadius) = when (style) {
        SongCardStyle.SMALL -> Triple(160.dp, 180.dp, 12.dp)
        SongCardStyle.MEDIUM -> Triple(280.dp, 180.dp, 16.dp)
        SongCardStyle.LARGE -> Triple(0.dp, 180.dp, 20.dp) // 宽度为0表示填充父容器
    }

    Surface(
        modifier = modifier
            .then(
                if (style == SongCardStyle.LARGE) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.width(width)
                }
            )
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .clickable { onCardClick() },
        color = Color(0xFF3325CD).copy(alpha = 0.05f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 背景图片
            AsyncImage(
                model = song.coverUrl,
                contentDescription = song.musicName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // 底部文字区域的高斯模糊背景
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f),
                                Color.Black.copy(alpha = 0.8f)
                            ),
                            startY = 0f,
                            endY = 150f
                        )
                    )
            ) {
                // 文字内容
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = song.musicName,
                        fontSize = when (style) {
                            SongCardStyle.SMALL -> 14.sp
                            SongCardStyle.MEDIUM -> 16.sp
                            SongCardStyle.LARGE -> 18.sp
                        },
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = MiSans,
                        color = Color.White
                    )
                    Text(
                        text = song.author,
                        fontSize = when (style) {
                            SongCardStyle.SMALL -> 12.sp
                            SongCardStyle.MEDIUM -> 14.sp
                            SongCardStyle.LARGE -> 16.sp
                        },
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = MiSans,
                        modifier = Modifier.fillMaxWidth()
                            .padding(end = 32.dp) // 为播放按钮预留空间
                    )
                }
            }
            
            // 播放图标
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.0f))
                    .clickable { onCardClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isPlaying) R.drawable.ic_pause else R.drawable.union
                    ),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
            }
        }
    }
} 