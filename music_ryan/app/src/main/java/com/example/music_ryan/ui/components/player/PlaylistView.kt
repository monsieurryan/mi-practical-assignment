package com.example.music_ryan.ui.components.player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.music_ryan.data.model.Song
import com.example.music_ryan.ui.theme.MiSans
import com.example.music_ryan.ui.theme.PlayerStyles

@Composable
fun PlaylistView(
    playlist: List<Song>,
    currentSong: Song?,
    onSongClick: (Song) -> Unit,
    onSongDelete: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = PlayerStyles.horizontalPadding)
    ) {
        // 标题和歌曲数量
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "播放列表",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MiSans
            )
            
            Text(
                text = "${playlist.size} 首歌曲",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontFamily = MiSans
            )
        }

        // 播放列表内容
        if (playlist.isEmpty()) {
            // 显示空列表提示
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "播放列表为空\n点击歌曲添加到播放列表",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 16.sp,
                    fontFamily = MiSans,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }
        } else {
            // 显示播放列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(playlist) { index, song ->
                    // 判断是否为当前播放歌曲的方法更加可靠：使用id比较而不是对象相等
                    val isCurrentlyPlaying = currentSong != null && song.id == currentSong.id
                    
                    PlaylistItem(
                        song = song,
                        isPlaying = isCurrentlyPlaying,
                        onClick = { onSongClick(song) },
                        onDeleteClick = { onSongDelete(song) }
                    )
                    
                    // 最后一项不显示分隔线
                    if (index < playlist.size - 1) {
                        Divider(
                            color = Color.White.copy(alpha = 0.1f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
} 