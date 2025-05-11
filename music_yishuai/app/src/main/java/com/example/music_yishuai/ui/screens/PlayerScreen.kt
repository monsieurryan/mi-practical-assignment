package com.example.music_yishuai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.music_yishuai.R
import com.example.music_yishuai.ui.theme.PlayerStyles as Styles
import com.example.music_yishuai.ui.theme.MiSans
import coil.request.SuccessResult
import kotlinx.coroutines.launch
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.example.music_yishuai.ui.components.player.*
import com.example.music_yishuai.data.model.Song
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.music_yishuai.ui.viewmodels.PlayerViewModel
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.music_yishuai.util.ImageLoaderUtil
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.palette.graphics.Palette

@Composable
fun PlayerScreen(
    musicName: String = "",
    author: String = "",
    coverUrl: String = "",
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier,
    comeFromMiniPlayer: Boolean = true,
    initialCoverScale: Float = 0.4f,
    initialCoverCornerRadius: Float = 22f,
    initialPositionY: Float = 1f
) {
    val context = LocalContext.current
    val viewModel: PlayerViewModel = viewModel()
    val playerState by viewModel.playerState.collectAsState()
    
    // 收集歌词状态
    val lyrics by viewModel.lyrics.collectAsState()
    val isLoadingLyrics by viewModel.isLoadingLyrics.collectAsState()
    val lyricError by viewModel.lyricError.collectAsState()
    
    var targetColor by remember { mutableStateOf(Color(0xFF1E1E1E)) }
    var isColorLoading by remember { mutableStateOf(true) }
    var isDataReady by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { 3 }) // 3个页面：封面、歌词、播放列表

    // 页面进入/退出动画状态
    var isEntering by remember { mutableStateOf(true) }
    var isExiting by remember { mutableStateOf(false) }
    
    // 页面进入动画的起始位置
    val initialOffsetY = remember { Animatable(if (comeFromMiniPlayer) 1000f else 0f) }
    
    // 封面图片的动画参数 - 使用传入的初始值
    val coverScale = remember { Animatable(initialCoverScale) }
    val coverCornerRadius = remember { Animatable(initialCoverCornerRadius) }
    val coverPositionY = remember { Animatable(initialPositionY) } // 0表示中央位置，1表示底部
    
    // 设置退出动画
    val animatedOffsetY by animateFloatAsState(
        targetValue = if (isExiting) 1000f else 0f,
        animationSpec = tween(
            durationMillis = 200,
            easing = LinearEasing
        ),
        label = "offsetYAnimation",
        finishedListener = { 
            if (isExiting) {
                onClose()
            }
        }
    )
    
    // 背景透明度动画，下拉时更透明，可以看到主页内容
    val animatedBgAlpha by animateFloatAsState(
        targetValue = if (isExiting) 0f else 1f,
        animationSpec = tween(
            durationMillis = 200,
            easing = LinearEasing
        ),
        label = "backgroundAlphaAnimation"
    )
    
    // 内容透明度动画
    val animatedContentAlpha by animateFloatAsState(
        targetValue = if (isExiting) 0f else 1f,
        animationSpec = tween(
            durationMillis = 200,
            easing = LinearEasing
        ),
        label = "contentAlphaAnimation"
    )

    // 获取当前播放歌曲信息，优先使用playerState中的信息
    val currentTrack = playerState.currentTrack
    val currentMusic = currentTrack?.musicName ?: musicName
    val currentAuthor = currentTrack?.author ?: author
    val currentCoverUrl = currentTrack?.coverUrl ?: coverUrl

    // 首先，在后台线程添加歌曲到播放列表并加载数据
    LaunchedEffect(musicName, author, coverUrl) {
        withContext(Dispatchers.Default) {
            if (musicName.isNotEmpty() && author.isNotEmpty() && coverUrl.isNotEmpty() && 
                playerState.playlist.isEmpty()) {
                println("PlayerScreen: 创建初始歌曲并添加到播放列表: $musicName")
                // 创建一个临时的Song对象
                val song = Song(
                    id = System.currentTimeMillis().toInt(), // 临时ID
                    musicName = musicName,
                    author = author,
                    coverUrl = coverUrl,
                    // 使用一个真实可用的样例音频URL
                    musicUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3", 
                    // 提供一个测试歌词URL或留空以使用测试歌词
                    lyricUrl = ""
                )
                
                // 将歌曲添加到播放列表顶部
                withContext(Dispatchers.Main) {
                    viewModel.addToTopOfPlaylist(song)
                }
            }
            
            // 数据加载完成
            withContext(Dispatchers.Main) {
                isDataReady = true
            }
        }
    }

    // 然后，在后台线程中提取颜色主题
    LaunchedEffect(currentCoverUrl) {
        if (currentCoverUrl.isEmpty()) return@LaunchedEffect
        
        // 首先尝试从缓存获取颜色
        val cachedColor = viewModel.getBackgroundColor(currentCoverUrl)
        if (cachedColor != null) {
            // 使用缓存的颜色
            targetColor = Color(cachedColor)
            isColorLoading = false
            println("PlayerScreen: 使用缓存的背景色")
            return@LaunchedEffect
        }
        
        // 缓存中没有，需要重新获取
        withContext(Dispatchers.IO) {
            isColorLoading = true
            try {
                println("PlayerScreen: 提取封面主题色: $currentCoverUrl")
                // 使用ImageLoaderUtil构建请求并获取应用级ImageLoader
                val request = ImageLoaderUtil.buildImageRequest(
                    context = context,
                    data = currentCoverUrl,
                    allowHardware = false
                )
                
                // 使用应用级ImageLoader执行请求
                val result = ImageLoaderUtil.getImageLoader(context).execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as BitmapDrawable).bitmap
                    
                    // 使用Palette库提取颜色
                    val palette = Palette.from(bitmap).generate()
                    
                    // 优先使用暗色饱和颜色，提供更好的视觉效果
                    val extractedColor = palette.getDarkVibrantColor(
                        palette.getDarkMutedColor(
                            palette.getVibrantColor(
                                palette.getMutedColor(Color(0xFF1E1E1E).hashCode())
                            )
                        )
                    )
                    
                    // 同时启动异步颜色处理，用于缓存和进一步处理
                    viewModel.processAndExtractColorAsync(currentCoverUrl, bitmap)
                    
                    withContext(Dispatchers.Main) {
                        targetColor = Color(extractedColor)
                        println("PlayerScreen: 封面主题色提取成功并已缓存")
                    }
                }
            } catch (e: Exception) {
                println("PlayerScreen: 提取封面主题色失败: ${e.message}")
                withContext(Dispatchers.Main) {
                    targetColor = Color(0xFF1E1E1E)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isColorLoading = false
                }
            }
        }
    }
    
    // 在数据加载完成后执行入场动画
    LaunchedEffect(isDataReady) {
        if (isDataReady && isEntering) {
            // 并行执行多个动画
            // 1. 页面从底部向上滑动
            launch {
                initialOffsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = FastOutSlowInEasing
                    )
                )
            }
            
            // 封面和相关动画已经在HomeWithPlayerScreen中处理，不需要在这里重复
            
            isEntering = false
        }
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "backgroundColor"
    )

    // 处理播放列表中歌曲的点击事件
    val handleSongClick: (Song) -> Unit = { song ->
        println("PlayerScreen: 点击播放列表中的歌曲: ${song.musicName}")
        viewModel.playTrack(song)
    }

    // 当歌曲变化时，重新加载歌词
    LaunchedEffect(playerState.currentTrack?.id) {
        playerState.currentTrack?.let { currentTrack ->
            println("PlayerScreen: 当前歌曲变化，重新加载歌词")
            viewModel.loadLyrics(currentTrack)
        }
    }

    // 将整个屏幕内容包装在一个Box中，应用动画效果
    Box(
        modifier = modifier
            .graphicsLayer { 
                // 页面动画：进入时上滑，退出时下滑
                translationY = if (isEntering) initialOffsetY.value else animatedOffsetY
            }
    ) {
        // 背景层 - 使用半透明效果，允许下方的主页内容透出
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(animatedColor.copy(alpha = animatedBgAlpha))
        )
        
        // 内容层
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = Styles.maxWidth)
                .graphicsLayer {
                    alpha = animatedContentAlpha
                }
        ) {
            TopBar(
                onClose = { 
                    // 点击关闭按钮时，立即触发退出动画
                    isExiting = true
                },
                viewModel = viewModel,
                currentTrack = playerState.currentTrack
            )
            
            // 当播放列表为空时显示加载指示器
            if (playerState.playlist.isEmpty() && playerState.currentTrack == null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                // 可滑动的内容区域
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> SongInfo(
                                musicName = currentMusic,
                                author = currentAuthor,
                                coverUrl = currentCoverUrl,
                                isLoading = isColorLoading,
                                isPlaying = playerState.isPlaying,
                                coverScale = coverScale.value,
                                coverCornerRadius = coverCornerRadius.value,
                                coverPositionY = coverPositionY.value
                            )
                            1 -> LyricsView(
                                lyrics = lyrics,
                                isLoading = isLoadingLyrics,
                                errorMessage = lyricError,
                                currentPosition = playerState.currentPosition,
                                modifier = Modifier.fillMaxSize()
                            )
                            2 -> PlaylistView(
                                playlist = playerState.playlist,
                                currentSong = playerState.currentTrack,
                                onSongClick = handleSongClick,
                                onSongDelete = { song ->
                                    val isPlaylistEmptyAfterDelete = viewModel.removeSongFromPlaylist(song)
                                    if (isPlaylistEmptyAfterDelete) {
                                        // 如果删除后播放列表为空，触发动画关闭
                                        isExiting = true
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 底部控制区域
            PlayerControls(
                currentPosition = playerState.currentPosition,
                duration = playerState.duration,
                isPlaying = playerState.isPlaying,
                isShuffleEnabled = playerState.isShuffleEnabled,
                isRepeatEnabled = playerState.isRepeatEnabled,
                currentTrackId = playerState.currentTrack?.id,
                onSeek = viewModel::seekTo,
                onPlayPause = viewModel::togglePlayPause,
                onNext = viewModel::playNext,
                onPrevious = viewModel::playPrevious,
                onShuffle = viewModel::toggleShuffle,
                onRepeat = viewModel::toggleRepeat,
                onRetry = { playerState.currentTrack?.let { viewModel.retryPlayback() } }
            )
            
            // 显示播放错误信息
            playerState.playbackError?.let { error ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(Color(0x33FF0000), RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "错误",
                            tint = Color.White
                        )
                        Text(
                            text = error,
                            color = Color.White,
                            fontFamily = MiSans,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = { viewModel.retryPlayback() },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color.White
                            )
                        ) {
                            Text("重试", fontFamily = MiSans)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    onClose: () -> Unit,
    viewModel: PlayerViewModel,
    currentTrack: Song?
) {
    // 收集收藏状态
    val isFavorite by viewModel.isFavorite.collectAsState()
    
    // 动画状态
    val scale = remember { Animatable(1f) }
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    
    // 处理收藏/取消收藏动画和状态
    val toggleFavorite = {
        // 获取当前歌曲ID
        currentTrack?.id?.let { songId ->
            // 触发收藏状态切换
            viewModel.toggleFavorite(songId)
            
            // 执行动画
            scope.launch {
                if (!isFavorite) { // 当前未收藏，将变为收藏状态
                    // 收藏动画：放大至1.2倍并旋转360度
                    launch {
                        scale.animateTo(
                            targetValue = 1.2f,
                            animationSpec = tween(500)
                        )
                        scale.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(500)
                        )
                    }
                    launch {
                        rotation.animateTo(
                            targetValue = 360f,
                            animationSpec = tween(1000)
                        )
                        rotation.snapTo(0f) // 重置旋转
                    }
                } else { // 当前已收藏，将变为未收藏状态
                    // 取消收藏动画：缩小至0.8倍
                    scale.animateTo(
                        targetValue = 0.8f,
                        animationSpec = tween(500)
                    )
                    scale.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(500)
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = Styles.topPadding)
    ) {
        // 左上角下拉关闭按钮
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(48.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_down),
                contentDescription = "关闭",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        
        // 右上角收藏按钮
        IconButton(
            onClick = { toggleFavorite() },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(48.dp)
        ) {
            Icon(
                painter = painterResource(
                    id = if (isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline
                ),
                contentDescription = if (isFavorite) "取消收藏" else "收藏",
                tint = if (isFavorite) Color(0xFFFFD700) else Color.White, // 收藏时显示金色
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        rotationY = rotation.value
                    }
            )
        }
    }
}

