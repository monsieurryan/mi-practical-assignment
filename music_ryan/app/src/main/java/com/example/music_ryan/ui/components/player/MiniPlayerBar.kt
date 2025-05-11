package com.example.music_ryan.ui.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.music_ryan.R
import com.example.music_ryan.ui.theme.MiSans
import com.example.music_ryan.ui.viewmodels.PlayerViewModel
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars

@Composable
fun MiniPlayerBar(
    modifier: Modifier = Modifier,
    onBarClick: () -> Unit = {},
    onPlaylistClick: () -> Unit = {}
) {
    val viewModel: PlayerViewModel = viewModel()
    val playerState by viewModel.playerState.collectAsState()
    
    // 获取系统底部安全区域高度
    val navigationBarsHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    
    // 获取当前播放歌曲信息
    val currentTrack = playerState.currentTrack
    
    // 控制底部播放列表的显示状态
    var isPlaylistVisible by remember { mutableStateOf(false) }
    
    // 如果没有当前播放歌曲，不显示播放栏
    if (currentTrack == null) {
        return
    }
    
    Box(modifier = modifier.fillMaxWidth()) {
        // 主要播放条
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clickable(onClick = onBarClick),
                color = Color(0xFF2A2A2A),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 歌曲封面 (小圆形)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Gray.copy(alpha = 0.2f))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(currentTrack.coverUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Album Art",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    // 歌曲信息
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = currentTrack.musicName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = MiSans,
                            color = Color.White
                        )
                        
                        Text(
                            text = currentTrack.author,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = MiSans,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    
                    // 播放/暂停按钮
                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (playerState.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                            ),
                            contentDescription = if (playerState.isPlaying) "暂停" else "播放",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // 播放列表按钮
                    IconButton(
                        onClick = { 
                            // 切换播放列表的显示状态
                            isPlaylistVisible = !isPlaylistVisible
                            // 同时调用外部传入的点击事件处理
                            onPlaylistClick()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_playlist),
                            contentDescription = "播放列表",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            // 底部安全区域填充
            if (navigationBarsHeight > 0.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(navigationBarsHeight)
                        .background(Color(0xFF2A2A2A))
                )
            }
        }
    }
    
    // 将播放列表移到Box外部，作为独立组件
    BottomSheetPlaylist(
        isVisible = isPlaylistVisible,
        onDismiss = { isPlaylistVisible = false },
        modifier = Modifier.fillMaxSize()
    )
} 