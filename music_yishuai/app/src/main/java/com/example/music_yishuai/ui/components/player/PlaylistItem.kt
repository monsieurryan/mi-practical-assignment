package com.example.music_yishuai.ui.components.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.music_yishuai.R
import com.example.music_yishuai.data.model.Song
import com.example.music_yishuai.ui.theme.MiSans

@Composable
fun PlaylistItem(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 歌曲信息
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(
                text = song.musicName,
                color = if (isPlaying) Color.White else Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                fontFamily = MiSans,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.author,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
                fontFamily = MiSans,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 播放状态指示器
        if (isPlaying) {
            Text(
                text = "正在播放",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontFamily = MiSans,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        
        // 删除按钮
        IconButton(
            onClick = onDeleteClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_delete),
                contentDescription = "删除",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
} 