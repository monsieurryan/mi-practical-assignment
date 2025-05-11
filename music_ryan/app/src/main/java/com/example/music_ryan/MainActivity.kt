package com.example.music_ryan

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.music_ryan.ui.screens.HomeScreen
import com.example.music_ryan.ui.screens.PlayerScreen
import com.example.music_ryan.ui.screens.LaunchScreen
import com.example.music_ryan.ui.theme.music_ryanTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import java.net.URLEncoder
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import com.example.music_ryan.MusicApplication
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.zIndex
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.music_ryan.ui.components.player.MiniPlayerBar
import com.example.music_ryan.ui.viewmodels.PlayerViewModel
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelProvider
import com.example.music_ryan.ui.viewmodels.HomeViewModel
import coil.Coil
import coil.ImageLoader


@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 确保应用级ImageLoader已正确初始化并记录日志
        try {
            val imageLoader = Coil.imageLoader(this)
            println("MainActivity: 已成功获取Coil应用级ImageLoader实例")
        } catch (e: Exception) {
            println("MainActivity: 获取Coil应用级ImageLoader失败: ${e.message}")
        }
        
        setContent {
            music_ryanTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "launch") {
                    composable("launch") {
                        LaunchScreen(
                            onLaunchComplete = {
                                navController.navigate("home") {
                                    popUpTo("launch") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("home") {
                        HomeWithPlayerScreen()
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // 获取 MusicPlayer 实例但不停止播放
        // 音乐应该在后台继续播放，我们只需暂停一些可能的UI更新
        try {
            val musicPlayer = MusicApplication.getMusicPlayer(application as Application)
            // 使用更清晰的方法名暂停UI更新
            musicPlayer.pauseProgressUpdate()
            println("MainActivity: 应用进入后台，暂停UI更新但继续播放音乐")
        } catch (e: Exception) {
            println("MainActivity: onPause 处理异常: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        // 应用恢复到前台，恢复UI更新
        try {
            val musicPlayer = MusicApplication.getMusicPlayer(application as Application)
            // 恢复Handler回调以更新UI，如果之前在播放则继续播放
            musicPlayer.resumePlayback()
            println("MainActivity: 应用恢复前台，重新启动UI更新")
        } catch (e: Exception) {
            println("MainActivity: onResume 处理异常: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 如果是应用真正退出（不是配置更改导致的销毁），则释放所有资源
        if (isFinishing) {
            // 先通知HomeViewModel应用正在退出
            try {
                val homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
                // 重置自动播放状态同时会设置退出标志
                homeViewModel.resetAutoPlayState()
                println("成功重置自动播放状态并标记应用退出")
            } catch (e: Exception) {
                println("重置自动播放状态失败: ${e.message}")
            }
            
            // 确保取消所有网络请求
            try {
                // 直接调用NetworkClient的取消方法
                com.example.music_ryan.network.NetworkClient.cancelAllRequests()
                println("MainActivity: 已取消所有进行中的网络请求")
            } catch (e: Exception) {
                println("MainActivity: 取消网络请求时发生异常: ${e.message}")
            }
            
            // 这里调用 MusicApplication 中的 onTerminate 方法
            // 虽然 onTerminate() 也会调用，但在某些情况下 onTerminate 可能不被调用
            // 所以在 Activity 结束时也尝试释放资源
            (application as MusicApplication).onTerminate()
            
            println("MainActivity 被销毁，应用退出，释放 MusicPlayer 资源")
        }
    }
}

@Composable
fun HomeWithPlayerScreen() {
    // 添加状态控制播放器显示
    val viewModel: PlayerViewModel = viewModel()
    val playerState by viewModel.playerState.collectAsState()
    
    // 控制全屏播放器显示的状态
    var isFullScreenPlayerVisible by remember { mutableStateOf(false) }
    
    // 当前选中的歌曲信息
    var selectedTrackInfo by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    
    // 封面过渡动画的共享状态
    val coverScale = remember { Animatable(1f) }
    val coverCornerRadius = remember { Animatable(0f) }
    val coverPositionY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    
    // 当playerState.currentTrack变为null时，自动关闭全屏播放器
    LaunchedEffect(playerState.currentTrack) {
        if (playerState.currentTrack == null && isFullScreenPlayerVisible) {
            // 不要立即关闭，给PlayerScreen的退出动画留出时间
            // 延迟300毫秒后再关闭全屏播放器，确保播放器的退出动画完成
            kotlinx.coroutines.delay(300)
            isFullScreenPlayerVisible = false
            println("MainActivity: 检测到播放列表为空，自动关闭全屏播放器")
        }
    }
    
    // 显示迷你播放栏的条件：有正在播放的歌曲，且全屏播放器未显示
    val showMiniPlayer = playerState.currentTrack != null && !isFullScreenPlayerVisible
    
    // 监听播放器显示状态变化，控制动画
    LaunchedEffect(isFullScreenPlayerVisible) {
        if (isFullScreenPlayerVisible) {
            // 从迷你播放栏过渡到全屏播放页面
            scope.launch {
                coverScale.snapTo(0.4f) // 起始缩放比例
                coverCornerRadius.snapTo(22f) // 起始圆角
                coverPositionY.snapTo(1f) // 起始位置
                
                // 启动动画到全屏播放页面状态
                launch {
                    coverScale.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
                launch {
                    coverCornerRadius.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
                launch {
                    coverPositionY.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
            }
        }
    }
    
    // 获取系统底部安全区域高度
    val navigationBarsHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    
    // 计算总的底部填充：迷你播放器高度 + 底部安全区域
    val miniPlayerTotalHeight = if (showMiniPlayer) 64.dp + navigationBarsHeight else 0.dp
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 主页作为底层
        HomeScreen(
            modifier = Modifier.fillMaxSize(),
            onNavigateToPlayer = { musicName, author, coverUrl ->
                // 设置选中的歌曲信息并显示全屏播放器
                selectedTrackInfo = Triple(musicName, author, coverUrl)
                isFullScreenPlayerVisible = true
            },
            bottomPadding = miniPlayerTotalHeight
        )
        
        // 播放器作为上层覆盖，当显示状态为true时显示
        if (isFullScreenPlayerVisible && selectedTrackInfo != null) {
            val (musicName, author, coverUrl) = selectedTrackInfo!!
            PlayerScreen(
                musicName = musicName,
                author = author,
                coverUrl = coverUrl,
                onClose = {
                    // 关闭时仅隐藏全屏播放器，不清除选中的歌曲信息
                    isFullScreenPlayerVisible = false
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f), // 确保播放器在HomeScreen上层
                comeFromMiniPlayer = true, // 从迷你播放栏过渡而来
                initialCoverScale = coverScale.value,
                initialCoverCornerRadius = coverCornerRadius.value,
                initialPositionY = coverPositionY.value
            )
        }
        
        // 底部的迷你播放栏，仅当有歌曲播放且全屏播放器未显示时展示
        if (showMiniPlayer) {
            MiniPlayerBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(0.9f), // 确保在HomeScreen上方但在全屏播放器下方
                onBarClick = {
                    // 当点击播放栏时，显示全屏播放器并使用当前播放的歌曲信息
                    playerState.currentTrack?.let { track ->
                        selectedTrackInfo = Triple(
                            track.musicName,
                            track.author,
                            track.coverUrl
                        )
                        isFullScreenPlayerVisible = true
                    }
                },
                onPlaylistClick = {
                    // 不再导航到播放页面，而是直接在MiniPlayerBar中处理播放列表的显示
                    // 此处可以留空，因为播放列表显示逻辑已经在MiniPlayerBar内部处理
                }
            )
        }
    }
}