package com.example.music_ryan.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.music_ryan.data.PlaylistManager
import com.example.music_ryan.data.model.Song
import com.example.music_ryan.player.MusicPlayer
import com.example.music_ryan.player.PlayerState
import com.example.music_ryan.MusicApplication
import com.example.music_ryan.data.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.graphics.Bitmap
import kotlinx.coroutines.cancel
import kotlin.time.Duration.Companion.milliseconds



class PlayerViewModel(application: Application) : AndroidViewModel(application), MusicPlayer.OnCompletionListener {
    // 使用 MusicApplication 获取全局 MusicPlayer 实例
    private val musicPlayer = MusicApplication.getMusicPlayer(application)
    
    // 用户偏好设置，用于保存收藏状态
    private val userPreferences = UserPreferences(application)
    
    // 使用LyricsManager管理歌词
    private val lyricsManager = LyricsManager(application)
    val lyrics = lyricsManager.lyrics
    val isLoadingLyrics = lyricsManager.isLoadingLyrics
    val lyricError = lyricsManager.lyricError
    
    // 使用ThemeManager管理颜色
    private val themeManager = ThemeManager()
    
    /**
     * 查询背景色缓存
     */
    fun getBackgroundColor(coverUrl: String): Int? {
        return themeManager.getBackgroundColor(coverUrl)
    }
    
    /**
     * 异步处理并提取图片主题色
     */
    fun processAndExtractColorAsync(coverUrl: String, bitmap: Bitmap?) {
        if (coverUrl.isBlank() || bitmap == null) return
        
        viewModelScope.launch(Dispatchers.Default) {
            try {
                // 使用ThemeManager处理
                themeManager.processAndExtractColor(coverUrl, bitmap)
                println("PlayerViewModel: 完成封面主题色处理")
            } catch (e: Exception) {
                println("PlayerViewModel: 处理封面主题色时出错: ${e.message}")
            }
        }
    }

    // 当前歌曲的收藏状态
    private val _isFavorite = MutableStateFlow(false)
    val isFavorite = _isFavorite.asStateFlow()

    // 组合 MusicPlayer 的状态和 PlaylistManager 的状态
    val playerState: StateFlow<PlayerState> = combine(
        musicPlayer.playerState,
        PlaylistManager.playlist,
        PlaylistManager.currentSong
    ) { playerState, playlist, currentSong ->
        // 合并播放列表状态：优先使用PlaylistManager的播放列表
        val mergedPlaylist = if (playlist.isNotEmpty()) playlist else playerState.playlist
        
        // 合并当前歌曲状态：优先使用PlaylistManager的当前歌曲
        val effectiveTrack = currentSong ?: playerState.currentTrack
        
        // 根据当前歌曲和播放列表，计算当前索引
        val effectiveIndex = if (effectiveTrack != null && mergedPlaylist.isNotEmpty()) {
            mergedPlaylist.indexOfFirst { it.id == effectiveTrack.id }.let { 
                if (it == -1) 0 else it 
            }
        } else {
            playerState.currentTrackIndex
        }
        
        // 组合最终状态
        playerState.copy(
            playlist = mergedPlaylist,
            currentTrack = effectiveTrack,
            currentTrackIndex = effectiveIndex,
            isPlaying = playerState.isPlaying,
            currentPosition = playerState.currentPosition,
            duration = playerState.duration
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PlayerState(
            playlist = PlaylistManager.getPlaylist(),
            currentTrack = PlaylistManager.currentSong.value
        )
    )

    init {
        // 设置歌曲播放完成监听器
        musicPlayer.setOnCompletionListener(this)
        
        // 在视图模型初始化时，确保MusicPlayer和PlaylistManager状态同步
        syncMusicPlayerWithPlaylistManager()
        
        // 监听PlaylistManager的currentSong变化，自动更新播放核心
        viewModelScope.launch {
            // 收集PlaylistManager的currentSong流
            PlaylistManager.currentSong.collect { newCurrentSong ->
                // 当currentSong变化时，更新播放核心的当前曲目并立即播放
                if (newCurrentSong != null) {
                    val playlist = PlaylistManager.getPlaylist()
                    val index = playlist.indexOfFirst { it.id == newCurrentSong.id }
                    
                    // 确保找到歌曲
                    if (index != -1) {
                        // 只有当新歌曲与当前播放的不同时才触发播放
                        if (musicPlayer.playerState.value.currentTrack?.id != newCurrentSong.id) {
                            println("PlayerViewModel: 监听到PlaylistManager当前歌曲变化，立即播放新歌曲: ${newCurrentSong.musicName}")
                            
                            // 注意：这里不需要处理歌词加载，因为addModuleSongsToPlaylist已经负责处理了
                            // 这个监听器主要是处理其他来源的currentSong变化
                            // 加载新歌曲的歌词（不等待加载完成）
                            loadLyrics(newCurrentSong)
                            
                            // 直接播放新的当前歌曲
                            musicPlayer.playTrackAt(index)
                        } else {
                            println("PlayerViewModel: 当前歌曲未变化，无需重新播放")
                        }
                    } else {
                        println("PlayerViewModel: 无法在播放列表中找到歌曲: ${newCurrentSong.musicName}")
                    }
                }
            }
        }
        
        // 监听播放器状态变化，确保歌词与播放的歌曲保持同步
        viewModelScope.launch {
            musicPlayer.playerState.collect { state ->
                val currentTrack = state.currentTrack
                if (currentTrack != null) {
                    // 如果当前显示的歌词为空，则加载歌词
                    if (lyrics.value.lines.isEmpty()) {
                        println("PlayerViewModel: 检测到播放器状态变化，确保加载正确的歌词 - ${currentTrack.musicName}")
                        loadLyrics(currentTrack)
                    }
                }
            }
        }
        
        // 设置当前歌曲观察者，用于更新收藏状态
        setupCurrentTrackObserver()
        
        // 读取初始收藏状态
        playerState.value.currentTrack?.id?.let { 
            checkFavoriteStatus(it)
        }
    }
    
    /**
     * 同步MusicPlayer和PlaylistManager的状态
     * 确保播放核心正在播放PlaylistManager中设置的当前歌曲
     * @param autoPlay 是否自动开始播放，默认为false
     */
    private fun syncMusicPlayerWithPlaylistManager(autoPlay: Boolean = false) {
        viewModelScope.launch {
            val playlist = PlaylistManager.getPlaylist()
            if (playlist.isNotEmpty()) {
                // 更新播放器的播放列表
                musicPlayer.setPlaylist(playlist)
                
                // 如果有当前歌曲，确保MusicPlayer也使用相同的当前歌曲
                PlaylistManager.currentSong.value?.let { currentSong ->
                    val index = playlist.indexOfFirst { it.id == currentSong.id }
                    if (index != -1) {
                        if (autoPlay) {
                            // 自动播放模式
                            musicPlayer.playTrackAt(index)
                        } else {
                            // 只同步当前曲目，不自动播放
                            musicPlayer.updateCurrentTrack(currentSong, index)
                        }
                    }
                }
            }
        }
    }

    /**
     * 添加歌曲到播放列表顶部
     */
    fun addToTopOfPlaylist(track: Song) {
        // 将歌曲添加到 PlaylistManager 顶部
        PlaylistManager.addToTopOfPlaylist(track)
        
        // 同步更新 MusicPlayer 的播放列表和当前曲目
        syncMusicPlayerWithPlaylistManager()
        
        println("PlayerViewModel: 已将歌曲添加到播放列表顶部: ${track.musicName}")
    }
    
    /**
     * 播放指定歌曲
     */
    fun playTrack(track: Song) {
        println("PlayerViewModel: 尝试播放歌曲: ${track.musicName}")
        
        // 获取当前播放列表
        val playlist = PlaylistManager.getPlaylist()
        println("PlayerViewModel: 当前播放列表大小: ${playlist.size}")
        
        // 先检查是否是当前正在播放的歌曲
        val currentTrack = playerState.value.currentTrack
        if (currentTrack != null && currentTrack.id == track.id) {
            // 如果是同一首歌，则切换播放暂停状态
            println("PlayerViewModel: 点击的是当前播放的歌曲，切换播放/暂停状态")
            togglePlayPause()
            return
        }
        
        viewModelScope.launch {
            try {
                // 获取当前播放列表
                val playlist = PlaylistManager.getPlaylist()
                
                // 查找歌曲在播放列表中的索引
                val index = playlist.indexOfFirst { it.id == track.id }
                println("PlayerViewModel: 歌曲在播放列表中的索引: $index")
                
                if (index != -1) {
                    // 歌曲已在播放列表中，设置为当前歌曲并播放
                    println("PlayerViewModel: 歌曲已在播放列表中，索引: $index, 直接播放")
                    PlaylistManager.setCurrentSong(track)
                    musicPlayer.playTrackAt(index)
                } else {
                    // 歌曲不在播放列表中，先添加到顶部
                    println("PlayerViewModel: 歌曲不在播放列表中，添加到顶部")
                    PlaylistManager.addToTopOfPlaylist(track)
                    PlaylistManager.setCurrentSong(track)
                    
                    // 重新获取更新后的播放列表并播放
                    val updatedPlaylist = PlaylistManager.getPlaylist()
                    musicPlayer.setPlaylist(updatedPlaylist)
                    musicPlayer.playTrackAt(0) // 添加到顶部，索引应为0
                }
            } catch (e: Exception) {
                println("PlayerViewModel: 播放失败: ${e.message}")
            }
        }
    }

    /**
     * 切换播放/暂停状态
     */
    fun togglePlayPause() {
        musicPlayer.togglePlayPause()
    }

    /**
     * 播放下一首
     */
    fun playNext() {
        if (playerState.value.playlist.isEmpty()) return
        
        // 计算下一首歌曲的索引
        val nextIndex = if (playerState.value.isShuffleEnabled) {
            // 随机模式：随机选择一首不同于当前歌曲的歌曲
            if (playerState.value.playlist.size > 1) {
                var randomIndex: Int
                do {
                    randomIndex = (0 until playerState.value.playlist.size).random()
                } while (randomIndex == playerState.value.currentTrackIndex)
                randomIndex
            } else {
                // 只有一首歌曲时，仍然播放该歌曲
                0
            }
        } else {
            // 顺序模式：播放列表中的下一首
            (playerState.value.currentTrackIndex + 1) % playerState.value.playlist.size
        }
        
        val nextSong = playerState.value.playlist[nextIndex]
        println("PlayerViewModel: 播放下一首歌曲: ${nextSong.musicName}, 索引: $nextIndex")
        
        // 更新PlaylistManager的当前歌曲
        PlaylistManager.setCurrentSong(nextSong)
        // 直接使用计算好的索引播放
        musicPlayer.playTrackAt(nextIndex)
    }

    /**
     * 播放上一首
     */
    fun playPrevious() {
        if (playerState.value.playlist.isEmpty()) return
        
        // 计算上一首歌曲的索引
        val previousIndex = if (playerState.value.isShuffleEnabled) {
            // 随机模式：随机选择一首不同于当前歌曲的歌曲
            if (playerState.value.playlist.size > 1) {
                var randomIndex: Int
                do {
                    randomIndex = (0 until playerState.value.playlist.size).random()
                } while (randomIndex == playerState.value.currentTrackIndex)
                randomIndex
            } else {
                // 只有一首歌曲时，仍然播放该歌曲
                0
            }
        } else {
            // 顺序模式：播放列表中的上一首
            if (playerState.value.currentTrackIndex > 0) {
                playerState.value.currentTrackIndex - 1
            } else {
                playerState.value.playlist.size - 1
            }
        }
        
        val previousSong = playerState.value.playlist[previousIndex]
        println("PlayerViewModel: 播放上一首歌曲: ${previousSong.musicName}, 索引: $previousIndex")
        
        // 更新PlaylistManager的当前歌曲
        PlaylistManager.setCurrentSong(previousSong)
        // 直接使用计算好的索引播放
        musicPlayer.playTrackAt(previousIndex)
    }

    /**
     * 切换随机播放状态
     */
    fun toggleShuffle() {
        musicPlayer.toggleShuffle()
        println("PlayerViewModel: 切换随机播放状态")
    }
    
    /**
     * 切换单曲循环状态
     */
    fun toggleRepeat() {
        musicPlayer.toggleRepeat()
        println("PlayerViewModel: 切换单曲循环状态")
    }

    /**
     * 跳转到指定播放位置
     */
    fun seekTo(position: Long) {
        // 为了防止UI不同步，先更新本地状态再调用播放器方法
        println("PlayerViewModel: 拖动进度条到位置: ${formatTime(position)}")
        musicPlayer.seekTo(position)
    }
    
    /**
     * 格式化时间为分:秒格式
     */
    private fun formatTime(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    /**
     * 加载歌词
     * @param song 要加载歌词的歌曲
     * @return 返回加载任务的Job
     */
    fun loadLyrics(song: Song): kotlinx.coroutines.Job {
        return viewModelScope.launch(Dispatchers.IO) {
            try {
                // 确保当前歌曲与要加载歌词的歌曲一致
                if (playerState.value.currentTrack?.id != song.id) {
                    println("PlayerViewModel: 当前播放的歌曲与要加载歌词的歌曲不一致，跳过加载")
                    return@launch
                }
                
                // 使用LyricsManager加载歌词
                lyricsManager.loadLyricsInternal(song, song.id)
                // 歌词加载完成后，预加载下一首歌曲的歌词
                preloadNextLyrics()
            } catch (e: Exception) {
                println("PlayerViewModel: 加载歌词失败: ${e.message}")
            }
        }
    }

    /**
     * 预加载下一首歌曲的歌词
     */
    fun preloadNextLyrics() {
        // 获取当前播放列表和索引
        val playlist = playerState.value.playlist
        val currentIndex = playerState.value.currentTrackIndex
        
        if (playlist.isEmpty() || currentIndex < 0 || currentIndex >= playlist.size) {
            return
        }
        
        // 计算下一首歌曲索引
        val nextIndex = if (playerState.value.isShuffleEnabled) {
            // 随机模式：随机选择一首不同于当前歌曲的歌曲
            if (playlist.size > 1) {
                var randomIndex: Int
                do {
                    randomIndex = (0 until playlist.size).random()
                } while (randomIndex == currentIndex)
                randomIndex
            } else {
                return // 只有一首歌曲时不需要预加载
            }
        } else {
            // 顺序模式：播放列表中的下一首
            (currentIndex + 1) % playlist.size
        }
        
        // 获取下一首歌曲
        val nextSong = playlist[nextIndex]
        
        // 使用LyricsManager预加载
        viewModelScope.launch(Dispatchers.IO) {
            try {
                lyricsManager.preloadLyrics(nextSong)
                println("PlayerViewModel: 成功预加载歌词: ${nextSong.musicName}")
            } catch (e: Exception) {
                println("PlayerViewModel: 预加载歌词失败: ${e.message}")
            }
        }
    }

    /**
     * 实现 MusicPlayer.OnCompletionListener 接口
     * 当歌曲播放完成时，自动播放下一首
     */
    override fun onCompletion(isRepeatEnabled: Boolean, isShuffleEnabled: Boolean) {
        // 注意：单曲循环模式已经在 MusicPlayer 中处理
        println("PlayerViewModel: 收到歌曲播放完成通知，播放下一首")
        if (!isRepeatEnabled) {
            // 单曲循环已在 MusicPlayer 中处理，这里只处理非循环情况
            playNext()
        }
    }

    override fun onCleared() {
        super.onCleared()
        // 注销监听器
        try {
            println("PlayerViewModel: 注销监听器")
            // 不能直接设为null，改为使用空的实现
            musicPlayer.setOnCompletionListener(object : MusicPlayer.OnCompletionListener {
                override fun onCompletion(isRepeatEnabled: Boolean, isShuffleEnabled: Boolean) {
                    // 空实现
                }
            })
        } catch (e: Exception) {
            println("PlayerViewModel: 注销监听器时出错: ${e.message}")
        }
        
        // 取消所有协程任务
        try {
            println("PlayerViewModel: 取消所有协程任务")
            viewModelScope.cancel() // 直接取消整个viewModelScope
        } catch (e: Exception) {
            println("PlayerViewModel: 取消协程任务时出错: ${e.message}")
        }
        
        // 清空缓存
        println("PlayerViewModel: 清空缓存")
        themeManager.clearCache()
        
        // 清空错误状态
        _isFavorite.value = false
        
        // 注意：MusicPlayer实例是全局共享的，不在这里完全释放
        // 但我们可以暂停播放和释放其他相关资源
        println("PlayerViewModel: onCleared() - ViewModel已销毁")
    }

    /**
     * 检查当前歌曲的收藏状态
     */
    fun checkFavoriteStatus(songId: Int) {
        viewModelScope.launch {
            try {
                println("PlayerViewModel: 检查歌曲ID=$songId 的收藏状态")
                val isFav = userPreferences.isSongFavorite(songId.toString())
                _isFavorite.value = isFav
                println("PlayerViewModel: 歌曲ID=$songId 的收藏状态: $isFav")
            } catch (e: Exception) {
                println("PlayerViewModel: 检查收藏状态出错: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    // 监听当前播放歌曲变化并处理收藏状态
    private fun setupCurrentTrackObserver() {
        var lastCheckedTrackId: Int? = null
        
        viewModelScope.launch {
            playerState.collect { state ->
                // 当前歌曲变化时检查收藏状态
                val currentTrackId = state.currentTrack?.id
                
                // 只有当歌曲ID真正变化时才检查收藏状态
                if (currentTrackId != null && currentTrackId != lastCheckedTrackId) {
                    println("PlayerViewModel: 当前歌曲变更为ID=$currentTrackId, 开始检查收藏状态")
                    checkFavoriteStatus(currentTrackId)
                    lastCheckedTrackId = currentTrackId
                } else if (currentTrackId == null && lastCheckedTrackId != null) {
                    // 当曲目从有到无变化时
                    println("PlayerViewModel: 当前没有播放歌曲")
                    _isFavorite.value = false
                    lastCheckedTrackId = null
                }
            }
        }
    }
    
    /**
     * 切换当前歌曲的收藏状态
     */
    fun toggleFavorite(songId: Int) {
        viewModelScope.launch {
            val newStatus = userPreferences.toggleFavoriteSong(songId.toString())
            _isFavorite.value = newStatus
        }
    }

    /**
     * 从播放列表中删除歌曲
     * @param song 要删除的歌曲
     * @return 如果删除后播放列表为空，返回true，否则返回false
     */
    fun removeSongFromPlaylist(song: Song): Boolean {
        println("PlayerViewModel: 开始删除歌曲: ${song.musicName}")
        
        // 从PlaylistManager中删除歌曲
        val (success, isCurrentSong, nextSong) = PlaylistManager.removeSongFromPlaylist(song)
        
        if (!success) {
            println("PlayerViewModel: 删除歌曲失败，可能歌曲不在播放列表中")
            return false
        }
        
        // 获取当前播放列表
        val updatedPlaylist = PlaylistManager.getPlaylist()
        
        // 如果删除后播放列表为空
        if (updatedPlaylist.isEmpty()) {
            println("PlayerViewModel: 删除后播放列表为空，停止播放")
            musicPlayer.stop()
            // 返回true表示列表为空，UI层可以据此关闭播放列表
            return true
        }
        
        // 如果删除的是当前播放的歌曲
        if (isCurrentSong) {
            println("PlayerViewModel: 删除的是当前播放的歌曲，需要处理下一首播放")
            
            // 如果nextSong为null，说明播放列表为空，已在上面处理
            if (nextSong != null) {
                val nextIndex = updatedPlaylist.indexOfFirst { it.id == nextSong.id }
                
                if (playerState.value.isShuffleEnabled) {
                    // 随机模式：随机选择一首不同的歌曲播放
                    if (updatedPlaylist.size > 1) {
                        // 随机选择一首不是当前歌曲的歌曲
                        var randomIndex: Int
                        do {
                            randomIndex = (0 until updatedPlaylist.size).random()
                        } while (randomIndex == nextIndex)
                        
                        val randomSong = updatedPlaylist[randomIndex]
                        PlaylistManager.setCurrentSong(randomSong)
                        musicPlayer.setPlaylist(updatedPlaylist)
                        musicPlayer.playTrackAt(randomIndex)
                        println("PlayerViewModel: 随机模式，播放新的随机歌曲: ${randomSong.musicName}")
                    } else {
                        // 只有一首歌曲时，播放该歌曲
                        PlaylistManager.setCurrentSong(nextSong)
                        musicPlayer.setPlaylist(updatedPlaylist)
                        musicPlayer.playTrackAt(nextIndex)
                        println("PlayerViewModel: 随机模式，但只有一首歌曲，播放: ${nextSong.musicName}")
                    }
                } else if (playerState.value.isRepeatEnabled) {
                    // 单曲循环模式：直接播放下一首
                    PlaylistManager.setCurrentSong(nextSong)
                    musicPlayer.setPlaylist(updatedPlaylist)
                    musicPlayer.playTrackAt(nextIndex)
                    println("PlayerViewModel: 单曲循环模式，播放下一首: ${nextSong.musicName}")
                } else {
                    // 顺序模式：直接播放下一首
                    PlaylistManager.setCurrentSong(nextSong)
                    musicPlayer.setPlaylist(updatedPlaylist)
                    musicPlayer.playTrackAt(nextIndex)
                    println("PlayerViewModel: 顺序模式，播放下一首: ${nextSong.musicName}")
                }
            }
        } else {
            // 如果删除的不是当前播放的歌曲，只需要更新播放列表
            musicPlayer.setPlaylist(updatedPlaylist)
            println("PlayerViewModel: 删除的不是当前播放的歌曲，只更新播放列表")
        }
        
        // 返回false表示列表不为空，可以继续播放
        return false
    }

    /**
     * 将模块中的所有歌曲添加到播放列表，并设置点击的歌曲为当前播放
     * @param clickedSong 用户点击的歌曲，将设置为当前播放
     * @param moduleList 该模块中的所有歌曲列表
     * @param waitForLyrics 是否等待歌词加载完成再播放歌曲（首次自动播放时应设为true）
     */
    fun addModuleSongsToPlaylist(
        clickedSong: Song, 
        moduleList: List<Song>,
        waitForLyrics: Boolean = false
    ) {
        println("PlayerViewModel: 将模块中的所有${moduleList.size}首歌曲添加到播放列表，当前点击的歌曲: ${clickedSong.musicName}")
        
        // 先获取当前播放列表
        val currentPlaylist = PlaylistManager.getPlaylist()
        println("PlayerViewModel: 当前播放列表大小: ${currentPlaylist.size}")
        
        // 首先检查点击的歌曲是否是当前正在播放的歌曲
        val currentTrack = playerState.value.currentTrack
        if (currentTrack != null && currentTrack.id == clickedSong.id) {
            // 如果是同一首歌，则切换播放/暂停状态
            println("PlayerViewModel: 点击的是当前播放的歌曲，切换播放/暂停状态")
            togglePlayPause()
            return
        }
        
        // 需要添加的歌曲列表
        val songsToAdd = mutableListOf<Song>()
        
        // 添加所有没有在当前播放列表中的歌曲
        moduleList.forEach { song ->
            // 确保歌曲有有效的 musicUrl
            val songWithValidUrl = if (song.musicUrl.isNotEmpty()) {
                song
            } else {
                // 如果没有有效的 musicUrl，使用默认的音频URL
                song.copy(musicUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3")
            }
            
            // 检查歌曲是否已在播放列表中
            val existsInPlaylist = currentPlaylist.any { it.id == songWithValidUrl.id }
            if (!existsInPlaylist) {
                songsToAdd.add(songWithValidUrl)
                println("PlayerViewModel: 添加歌曲到播放列表: ${songWithValidUrl.musicName}")
            } else {
                println("PlayerViewModel: 歌曲已存在于播放列表中，跳过: ${songWithValidUrl.musicName}")
            }
        }
        
        // 添加新的歌曲到播放列表
        if (songsToAdd.isNotEmpty()) {
            songsToAdd.forEach { PlaylistManager.addToEndOfPlaylist(it) }
            println("PlayerViewModel: 成功添加${songsToAdd.size}首歌曲到播放列表")
        }
        
        // 播放被点击的歌曲
        // 重新获取更新后的播放列表
        val updatedPlaylist = PlaylistManager.getPlaylist()
        val clickedSongIndex = updatedPlaylist.indexOfFirst { it.id == clickedSong.id }
        
        if (clickedSongIndex != -1) {
            println("PlayerViewModel: 开始播放点击的歌曲: ${clickedSong.musicName}, 索引: $clickedSongIndex")
            
            // 首次自动播放时，等待歌词加载完成再播放
            if (waitForLyrics) {
                viewModelScope.launch {
                    println("PlayerViewModel: 等待歌词加载完成后再播放歌曲...")
                    // 加载歌词并等待完成
                    val lyricJob = loadLyrics(clickedSong)
                    lyricJob.join()
                    
                    // 歌词加载完成后再设置当前歌曲并开始播放
                    println("PlayerViewModel: 歌词加载完成，开始播放歌曲: ${clickedSong.musicName}")
                    PlaylistManager.setCurrentSong(clickedSong)
                    musicPlayer.setPlaylist(updatedPlaylist)
                    musicPlayer.playTrackAt(clickedSongIndex)
                }
            } else {
                // 普通情况下并行加载歌词和播放歌曲
                loadLyrics(clickedSong)
                // 设置当前歌曲并开始播放
                PlaylistManager.setCurrentSong(clickedSong)
                musicPlayer.setPlaylist(updatedPlaylist)
                musicPlayer.playTrackAt(clickedSongIndex)
            }
        } else {
            println("PlayerViewModel: 错误! 无法在播放列表中找到点击的歌曲: ${clickedSong.musicName}")
        }
    }

    /**
     * 简单的重试播放机制
     * 当出现错误时可以调用此方法尝试重新播放当前歌曲
     */
    fun retryPlayback() {
        val currentTrack = playerState.value.currentTrack ?: return
        println("PlayerViewModel: 尝试重新播放当前歌曲: ${currentTrack.musicName}")
        
        viewModelScope.launch {
            try {
                // 添加短暂延迟后重试
                delay(500.milliseconds)
                
                // 检查URL是否有效
                val url = currentTrack.musicUrl
                if (url.isBlank()) {
                    println("PlayerViewModel: 歌曲URL为空，无法播放")
                    return@launch
                }
                
                // 重新播放当前歌曲
                musicPlayer.playTrack(currentTrack)
            } catch (e: Exception) {
                println("PlayerViewModel: 重试播放失败: ${e.message}")
            }
        }
    }
} 