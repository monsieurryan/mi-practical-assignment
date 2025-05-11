package com.example.music_yishuai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import com.google.accompanist.pager.ExperimentalPagerApi
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.music_yishuai.ui.viewmodels.HomeViewModel
import com.example.music_yishuai.data.model.Song
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import com.example.music_yishuai.ui.components.SongCard
import com.example.music_yishuai.ui.components.SongCardStyle
import com.example.music_yishuai.ui.theme.MiSans
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import kotlinx.coroutines.launch
import androidx.compose.material.ExperimentalMaterialApi
import com.example.music_yishuai.ui.viewmodels.PlayerViewModel
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.absoluteValue

@OptIn(ExperimentalPagerApi::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onSearchClick: (String) -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
    onNavigateToPlayer: (String, String, String) -> Unit = { _, _, _ -> },
    bottomPadding: Dp = 0.dp
) {
    var searchText by remember { mutableStateOf("") }
    val homePageData = viewModel.homePageData
    val isRefreshing = viewModel.isLoading
    val scope = rememberCoroutineScope()
    val playerViewModel: PlayerViewModel = viewModel()
    
    // 首次打开随机播放逻辑
    val hasAutoPlayed = remember { mutableStateOf(viewModel.getAndUpdateAutoPlayState()) }
    
    // 当首页数据加载完成后，随机选择一个模块的歌曲进行播放
    LaunchedEffect(homePageData) {
        if (homePageData != null && homePageData.isNotEmpty() && !hasAutoPlayed.value) {
            // 标记已经自动播放过，避免重复播放
            hasAutoPlayed.value = true
            
            // 过滤掉空模块
            val nonEmptyModules = homePageData.filter { it.musicInfoList.isNotEmpty() }
            
            if (nonEmptyModules.isNotEmpty()) {
                // 随机选择一个模块
                val randomModuleIndex = (0 until nonEmptyModules.size).random()
                val selectedModule = nonEmptyModules[randomModuleIndex]
                
                // 随机选择该模块中的一首歌曲
                val randomSongIndex = (0 until selectedModule.musicInfoList.size).random()
                val selectedSong = selectedModule.musicInfoList[randomSongIndex]
                
                println("HomeScreen: 首次打开自动播放随机歌曲 - 模块索引: $randomModuleIndex, 歌曲: ${selectedSong.musicName}")
                
                // 将该模块的所有歌曲添加到播放列表，并设置选中的歌曲为当前播放
                // 首次自动播放时，等待歌词加载完成再播放歌曲
                playerViewModel.addModuleSongsToPlaylist(selectedSong, selectedModule.musicInfoList, waitForLyrics = true)
            }
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 固定在顶部的搜索栏
        SearchBar(
            searchText = searchText,
            onSearchTextChange = { searchText = it },
            onSearch = onSearchClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        // 可下拉刷新的内容区域
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            val pullRefreshState = rememberPullRefreshState(
                refreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        viewModel.loadHomePageData()
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 60.dp + bottomPadding)
                ) {
                    if (homePageData != null) {
                        homePageData.forEachIndexed { index, moduleConfig ->
                            when (moduleConfig.style) {
                                1 -> {
                                    // Banner
                                    item {
                                        BannerSection(
                                            bannerList = moduleConfig.musicInfoList,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(160.dp)
                                                .padding(horizontal = 16.dp),
                                            onNavigateToPlayer = onNavigateToPlayer
                                        )
                                    }
                                }
                                2 -> {
                                    // 大卡片，一次显示一张，居中显示
                                    item {
                                        SectionTitle(
                                            title = getLocalModuleName(index),
                                            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp)
                                        )
                                        SingleCardPager(
                                            musicList = moduleConfig.musicInfoList,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(220.dp),
                                            onNavigateToPlayer = onNavigateToPlayer
                                        )
                                    }
                                }
                                3 -> {
                                    // 大卡片，一行一个，填充屏幕
                                    item {
                                        SectionTitle(
                                            title = getLocalModuleName(index),
                                            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp)
                                        )
                                        HorizontalMusicList(
                                            musicList = moduleConfig.musicInfoList,
                                            style = SongCardStyle.LARGE,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp),
                                            onNavigateToPlayer = onNavigateToPlayer
                                        )
                                    }
                                }
                                4 -> {
                                    // 小卡片，一行多个
                                    item {
                                        SectionTitle(
                                            title = getLocalModuleName(index),
                                            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp)
                                        )
                                        HorizontalMusicList(
                                            musicList = moduleConfig.musicInfoList,
                                            style = SongCardStyle.SMALL,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp),
                                            onNavigateToPlayer = onNavigateToPlayer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 下拉刷新指示器
                PullRefreshIndicator(
                    refreshing = isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    backgroundColor = Color.White,
                    contentColor = Color(0xFF3325CD)
                )
            }

            // 错误提示
            viewModel.error?.let { error ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = error)
                }
            }
        }
    }
}

private fun getLocalModuleName(index: Int): String {
    return when (index) {
        1 -> "专属好歌"
        2 -> "每日推荐"
        3 -> "热门金曲"
        else -> "推荐歌曲"
    }
}

@Composable
fun SearchBar(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = searchText,
        onValueChange = onSearchTextChange,
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(Color(0xFFF6F7F8)),
        placeholder = {
            Text(
                text = "一直很安静",
                color = Color.Gray,
                fontFamily = MiSans
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color.Gray
            )
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFF6F7F8),
            unfocusedContainerColor = Color(0xFFF6F7F8),
            disabledContainerColor = Color(0xFFF6F7F8),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch(searchText) }),
        singleLine = true
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BannerSection(
    bannerList: List<Song>,
    modifier: Modifier = Modifier,
    onNavigateToPlayer: (String, String, String) -> Unit = { _, _, _ -> }
) {
    val pagerState = rememberPagerState(pageCount = { bannerList.size })
    val viewModel: PlayerViewModel = viewModel()
    val playerState by viewModel.playerState.collectAsState()
    
    // 自动轮播
    LaunchedEffect(Unit) {
        while(true) {
            delay(3000) // 3秒切换一次
            val nextPage = (pagerState.currentPage + 1) % bannerList.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Box(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable {
                    // 获取当前页面显示的歌曲
                    val currentSong = bannerList[pagerState.currentPage]
                    // 确保歌曲有有效的 musicUrl
                    val songWithValidUrl = if (currentSong.musicUrl.isNotEmpty()) {
                        currentSong
                    } else {
                        // 如果没有有效的 musicUrl，使用默认的音频URL
                        currentSong.copy(musicUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3")
                    }
                    
                    // 检查是否是当前正在播放的歌曲
                    val isCurrentlyPlaying = playerState.currentTrack != null && 
                                            playerState.currentTrack?.id == songWithValidUrl.id
                    
                    // 如果点击的是当前正在播放的歌曲，则切换播放/暂停状态
                    if (isCurrentlyPlaying) {
                        println("HomeScreen: 点击当前播放中的Banner歌曲 ${songWithValidUrl.musicName}，切换播放/暂停状态")
                        viewModel.togglePlayPause()
                    } else {
                        // 使用新方法：将整个模块的歌曲添加到播放列表
                        println("HomeScreen: 点击Banner歌曲 ${songWithValidUrl.musicName}，将整个模块的歌曲添加到播放列表")
                        viewModel.addModuleSongsToPlaylist(songWithValidUrl, bannerList)
                    }
                    
                    // 导航到播放器页面
                    onNavigateToPlayer(
                        songWithValidUrl.musicName,
                        songWithValidUrl.author,
                        songWithValidUrl.coverUrl
                    )
                }
        ) { page ->
            AsyncImage(
                model = bannerList[page].coverUrl,
                contentDescription = bannerList[page].musicName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // 添加指示器
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(bannerList.size) { index ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == index) Color.White
                            else Color.White.copy(alpha = 0.5f)
                        )
                )
            }
        }

        // 添加标题
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.3f))
                .padding(16.dp)
        ) {
            Text(
                text = bannerList[pagerState.currentPage].musicName,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = MiSans
            )
        }
    }
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = MiSans,
        modifier = modifier
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SingleCardPager(
    musicList: List<Song>,
    modifier: Modifier = Modifier,
    onNavigateToPlayer: (String, String, String) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { musicList.size })
    val viewModel: PlayerViewModel = viewModel()
    val playerState by viewModel.playerState.collectAsState()
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(vertical = 8.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 0.dp,
            contentPadding = PaddingValues(start = 70.dp, end = 50.dp)
        ) { page ->
            val song = musicList[page]
            // 确保歌曲有有效的 musicUrl
            var songWithValidUrl = if (song.musicUrl.isNotEmpty()) {
                song
            } else {
                // 如果没有有效的 musicUrl，使用默认的音频URL
                song.copy(musicUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3")
            }
            
            // 检查是否是当前正在播放的歌曲
            val isCurrentlyPlaying = playerState.currentTrack != null && 
                                    playerState.currentTrack?.id == songWithValidUrl.id
            
            // 计算缩放比例
            val pageOffset = (
                (page - pagerState.currentPage) + pagerState
                    .currentPageOffsetFraction
            ).coerceIn(-1f, 1f)
            
            // 根据页面偏移计算缩放值，稍微增加非焦点卡片的缩放值
            val scaleValue = lerpFloat(
                start = 0.85f,
                stop = 1f,
                fraction = 1f - pageOffset.absoluteValue
            ).coerceIn(0.85f, 1f)
            
            // 根据页面偏移计算透明度，增加非焦点卡片的透明度
            val alphaValue = lerpFloat(
                start = 0.7f,
                stop = 1f,
                fraction = 1f - pageOffset.absoluteValue
            ).coerceIn(0.7f, 1f)
            
            SongCard(
                song = songWithValidUrl,
                style = SongCardStyle.LARGE,
                isPlaying = isCurrentlyPlaying,
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.95f)
                    .graphicsLayer {
                        scaleX = scaleValue
                        scaleY = scaleValue
                        alpha = alphaValue
                    },
                onCardClick = {
                    // 使用新方法：将整个模块的歌曲添加到播放列表
                    println("HomeScreen: 点击歌曲卡片 ${songWithValidUrl.musicName}，将整个模块的歌曲添加到播放列表")
                    viewModel.addModuleSongsToPlaylist(songWithValidUrl, musicList)
                    
                    // 无论如何都导航到播放器页面
                    onNavigateToPlayer(
                        songWithValidUrl.musicName,
                        songWithValidUrl.author,
                        songWithValidUrl.coverUrl
                    )
                }
            )
        }
        
        // 添加指示器
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(musicList.size) { index ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == index) Color(0xFF3325CD)
                            else Color(0xFF3325CD).copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}

// 更改函数名，避免与变量名冲突
private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + fraction * (stop - start)
}

@Composable
private fun HorizontalMusicList(
    musicList: List<Song>,
    style: SongCardStyle = SongCardStyle.SMALL,
    modifier: Modifier = Modifier,
    onNavigateToPlayer: (String, String, String) -> Unit
) {
    val viewModel: PlayerViewModel = viewModel()
    val playerState by viewModel.playerState.collectAsState()
    
    // 根据样式设置不同的布局参数
    val (contentPadding, itemSpacing) = when (style) {
        SongCardStyle.SMALL -> Pair(
            PaddingValues(horizontal = 16.dp),
            Arrangement.spacedBy(12.dp)
        )
        SongCardStyle.MEDIUM -> Pair(
            PaddingValues(horizontal = 16.dp),
            Arrangement.spacedBy(24.dp)
        )
        SongCardStyle.LARGE -> Pair(
            PaddingValues(horizontal = 16.dp),
            Arrangement.spacedBy(16.dp)
        )
    }
    
    LazyRow(
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = itemSpacing
    ) {
        items(musicList.size) { index ->
            val song = musicList[index]
            // 确保歌曲有有效的 musicUrl
            val songWithValidUrl = if (song.musicUrl.isNotEmpty()) {
                song
            } else {
                // 如果没有有效的 musicUrl，使用默认的音频URL
                song.copy(musicUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3")
            }
            
            // 检查是否是当前正在播放的歌曲
            val isCurrentlyPlaying = playerState.currentTrack != null && 
                                     playerState.currentTrack?.id == songWithValidUrl.id
            
            // 为大卡片添加宽度修饰符
            val cardModifier = if (style == SongCardStyle.LARGE) {
                Modifier
                    .width(300.dp)
                    .fillMaxHeight()
            } else {
                Modifier.fillMaxHeight()
            }
            
            SongCard(
                song = songWithValidUrl,
                style = style,
                isPlaying = isCurrentlyPlaying,
                modifier = cardModifier,
                onCardClick = {
                    // 使用新方法：将整个模块的歌曲添加到播放列表
                    println("HomeScreen: 点击歌曲卡片 ${songWithValidUrl.musicName}，将整个模块的歌曲添加到播放列表")
                    viewModel.addModuleSongsToPlaylist(songWithValidUrl, musicList)
                    
                    // 无论如何都导航到播放器页面
                    onNavigateToPlayer(
                        songWithValidUrl.musicName,
                        songWithValidUrl.author,
                        songWithValidUrl.coverUrl
                    )
                }
            )
        }
    }
} 