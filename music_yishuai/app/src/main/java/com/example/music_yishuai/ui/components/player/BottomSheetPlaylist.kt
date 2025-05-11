package com.example.music_yishuai.ui.components.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.music_yishuai.ui.viewmodels.PlayerViewModel
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars

@Composable
fun BottomSheetPlaylist(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: PlayerViewModel = viewModel()
    val playerState by viewModel.playerState.collectAsState()
    val density = LocalDensity.current
    
    // 获取系统底部安全区域高度
    val navigationBarsHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    
    // 播放列表sheet高度（屏幕高度的60%）
    val fullSheetHeight = with(density) { 60.dp * 10 } // 约为屏幕高度的60%
    
    // 当前显示高度的状态
    var currentHeight by remember { mutableStateOf(0.dp) }
    
    // 手势拖动的状态
    var dragOffset by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    
    // 控制动画关闭的状态
    var isClosing by remember { mutableStateOf(false) }
    
    // 根据isVisible状态动画显示/隐藏底部弹出框
    val targetHeight = if (isVisible && !isClosing) fullSheetHeight else 0.dp
    
    // 应用动画效果
    val animatedHeight by animateDpAsState(
        targetValue = if (isDragging) {
            (currentHeight.value - dragOffset).coerceAtLeast(0f).dp
        } else {
            targetHeight
        },
        label = "sheetHeightAnimation",
        finishedListener = {
            // 动画完成后检查是否需要关闭
            if (isClosing && it == 0.dp) {
                // 真正调用关闭回调
                onDismiss()
                // 重置状态
                isClosing = false
            }
        }
    )
    
    // 动画关闭函数，不直接调用onDismiss
    val animatedClose = {
        isClosing = true
    }
    
    // 监听isVisible变化
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            currentHeight = 0.dp
            dragOffset = 0f
            isClosing = false
        } else {
            currentHeight = fullSheetHeight
            isClosing = false
        }
    }
    
    // 监听动画高度变化
    LaunchedEffect(animatedHeight) {
        // 当高度接近0时自动关闭
        if (!isDragging && !isClosing && animatedHeight < 50.dp && isVisible) {
            onDismiss()
        }
    }
    
    // 仅在isVisible为true时显示
    if (isVisible) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .zIndex(2f), // 确保在MiniPlayerBar之上
            contentAlignment = Alignment.BottomCenter
        ) {
            // 半透明背景覆盖整个屏幕
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .pointerInput(Unit) {
                        detectTapGestures {
                            // 点击背景关闭播放列表，使用动画关闭
                            animatedClose()
                        }
                    }
            )

            // 播放列表内容
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(animatedHeight + navigationBarsHeight)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { 
                                isDragging = true 
                            },
                            onDragEnd = {
                                isDragging = false
                                // 如果拖动超过一定阈值，关闭弹窗
                                if (dragOffset > fullSheetHeight.value / 3) {
                                    // 使用动画关闭，不直接调用onDismiss
                                    animatedClose()
                                } else {
                                    // 否则恢复到完全展开状态
                                    dragOffset = 0f
                                    currentHeight = fullSheetHeight
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                                dragOffset = 0f
                            },
                            onDrag = { change, dragAmount ->
                                // 只响应向下的拖动（正值）
                                if (dragAmount.y > 0) {
                                    dragOffset += dragAmount.y
                                }
                                change.consume()
                            }
                        )
                    },
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = Color(0xFF2A2A2A)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 拖动条指示器
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.3f))
                        )
                    }
                    
                    // 播放列表内容
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        PlaylistView(
                            playlist = playerState.playlist,
                            currentSong = playerState.currentTrack,
                            onSongClick = { song ->
                                viewModel.playTrack(song)
                                // 点击后不要立即关闭，让用户可以继续操作播放列表
                            },
                            onSongDelete = { song ->
                                // 处理删除操作
                                val isListEmpty = viewModel.removeSongFromPlaylist(song)
                                
                                // 如果删除后播放列表为空，关闭播放列表界面
                                if (isListEmpty) {
                                    // 使用动画关闭，然后触发onDismiss回调
                                    animatedClose()
                                }
                            }
                        )
                    }
                    
                    // 底部安全区域填充
                    if (navigationBarsHeight > 0.dp) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(navigationBarsHeight)
                        )
                    }
                }
            }
        }
    }
} 