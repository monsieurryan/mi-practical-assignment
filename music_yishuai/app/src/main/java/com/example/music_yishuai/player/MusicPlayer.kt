package com.example.music_yishuai.player

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import com.example.music_yishuai.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import android.media.AudioManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.os.Build

class MusicPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 200L // 200毫秒更新一次进度，使播放更平滑

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    // 持久化的播放列表
    private var _playlist: List<Song> = emptyList()
    
    // 音频焦点管理
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var playOnAudioFocusGain = false
    
    // 音频焦点变化监听器
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // 长时间失去音频焦点，完全停止播放
                println("MusicPlayer: 音频焦点完全丢失，暂停播放")
                if (_playerState.value.isPlaying) {
                    playOnAudioFocusGain = true
                    pausePlaybackInternal()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // 暂时失去音频焦点，暂停播放但准备稍后恢复
                println("MusicPlayer: 暂时失去音频焦点，暂停播放")
                if (_playerState.value.isPlaying) {
                    playOnAudioFocusGain = true
                    pausePlaybackInternal()
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // 获得音频焦点
                println("MusicPlayer: 获得音频焦点")
                if (playOnAudioFocusGain) {
                    playOnAudioFocusGain = false
                    resumePlaybackInternal()
                }
            }
        }
    }
    
    // 定义歌曲播放完成监听器接口
    interface OnCompletionListener {
        fun onCompletion(isRepeatEnabled: Boolean, isShuffleEnabled: Boolean)
    }
    
    // 歌曲播放完成回调
    private var onCompletionListener: OnCompletionListener? = null
    
    // 设置歌曲播放完成监听器
    fun setOnCompletionListener(listener: OnCompletionListener) {
        onCompletionListener = listener
    }
    
    // 切换随机播放状态
    fun toggleShuffle() {
        val isShuffleEnabled = !_playerState.value.isShuffleEnabled
        _playerState.update { it.copy(isShuffleEnabled = isShuffleEnabled) }
        println("MusicPlayer: 随机播放已${if (isShuffleEnabled) "开启" else "关闭"}")
    }
    
    // 切换单曲循环状态
    fun toggleRepeat() {
        val isRepeatEnabled = !_playerState.value.isRepeatEnabled
        _playerState.update { it.copy(isRepeatEnabled = isRepeatEnabled) }
        println("MusicPlayer: 单曲循环已${if (isRepeatEnabled) "开启" else "关闭"}")
    }
    
    private fun updatePlaylist(value: List<Song>) {
        _playlist = value
        // 每次更新播放列表时同步更新 playerState 中的播放列表
        _playerState.update { it.copy(playlist = value) }
    }

    private val progressUpdater = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    updateProgress(player.currentPosition.toLong())
                }
            }
            handler.postDelayed(this, updateInterval)
        }
    }

    init {
        handler.post(progressUpdater)
    }
    
    /**
     * 请求音频焦点
     * @return 是否成功获取焦点
     */
    private fun requestAudioFocus(): Boolean {
        println("MusicPlayer: 请求音频焦点")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0及以上使用新的音频焦点API
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
                
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            
            val result = audioManager.requestAudioFocus(audioFocusRequest!!)
            result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            // 旧版API
            val result = audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }
    
    /**
     * 放弃音频焦点
     */
    private fun abandonAudioFocus() {
        println("MusicPlayer: 放弃音频焦点")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
        } else {
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
        playOnAudioFocusGain = false
    }
    
    /**
     * 内部使用的暂停播放方法，不改变playOnAudioFocusGain标志
     */
    private fun pausePlaybackInternal() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _playerState.update { it.copy(isPlaying = false) }
                println("MusicPlayer: 暂停播放")
            }
        }
    }
    
    /**
     * 内部使用的恢复播放方法，不检查音频焦点
     */
    private fun resumePlaybackInternal() {
        mediaPlayer?.let { player ->
            if (!player.isPlaying) {
                player.start()
                _playerState.update { it.copy(isPlaying = true) }
                println("MusicPlayer: 恢复播放")
            }
        }
    }

    fun setPlaylist(tracks: List<Song>) {
        if (tracks.isEmpty()) {
            return // 防止设置空播放列表
        }
        
        updatePlaylist(tracks)
        
        // 如果当前没有正在播放的歌曲，则更新当前歌曲为列表第一首
        if (_playerState.value.currentTrack == null) {
            _playerState.update { it.copy(
                currentTrack = _playlist[0],
                currentTrackIndex = 0
            )}
        } else {
            // 确保当前歌曲在播放列表中的索引是正确的
            val currentTrack = _playerState.value.currentTrack
            val index = _playlist.indexOfFirst { it.id == currentTrack?.id }
            if (index != -1) {
                _playerState.update { it.copy(currentTrackIndex = index) }
            }
        }
    }

    fun addToTopOfPlaylist(track: Song) {
        // 如果播放列表为空，则直接设置播放列表
        if (_playlist.isEmpty()) {
            updatePlaylist(listOf(track))
            _playerState.update { it.copy(
                currentTrackIndex = 0,
                currentTrack = track
            )}
            return
        }
        
        // 1. 检查歌曲是否已在列表中
        val existingIndex = _playlist.indexOfFirst { it.id == track.id }
        
        // 2. 根据情况处理列表
        if (existingIndex != -1) {
            // 歌曲已存在，将其从原位置移除并添加到顶部
            val newList = _playlist.toMutableList()
            newList.removeAt(existingIndex)
            newList.add(0, track)
            updatePlaylist(newList)
        } else {
            // 歌曲不存在，直接添加到顶部
            updatePlaylist(listOf(track) + _playlist)
        }
        
        // 更新当前曲目
        _playerState.update { it.copy(
            currentTrackIndex = 0,
            currentTrack = track
        )}
        
        println("MusicPlayer: 歌曲已添加到播放列表顶部: ${track.musicName}，但未自动播放")
    }

    fun playTrack(track: Song) {
        val index = _playlist.indexOfFirst { it.id == track.id }
        if (index != -1) {
            playTrackAt(index)
        } else {
            // 如果歌曲不在播放列表中，将其添加到顶部并播放
            addToTopOfPlaylist(track)
            playTrackAt(0)
        }
    }

    fun playTrackAt(index: Int) {
        println("MusicPlayer: 尝试播放索引 $index 的歌曲")
        
        if (_playlist.isEmpty()) {
            println("MusicPlayer: 播放列表为空，无法播放")
            return
        }
        
        println("MusicPlayer: 当前播放列表大小: ${_playlist.size}")
        
        if (index in _playlist.indices) {
            val track = _playlist[index]
            println("MusicPlayer: 找到索引 $index 的歌曲: ${track.musicName}")
            
            // 请求音频焦点
            if (!requestAudioFocus()) {
                println("MusicPlayer: 无法获取音频焦点，播放失败")
                return
            }
            
            // 完全重置播放状态
            // 使用单独更新以确保状态变化能被各UI组件检测到
            _playerState.update { it.copy(currentPosition = 0) }
            
            // 然后更新其他状态信息
            _playerState.update { it.copy(
                currentTrackIndex = index,
                currentTrack = track,
                isPlaying = true, // 强制设置为播放中状态
                duration = 0 // 先重置时长，等媒体加载完成后会更新
            )}
            
            println("MusicPlayer: 已更新播放状态，currentTrackIndex=$index, 歌曲=${track.musicName}, isPlaying=true")
            
            try {
                // 使用安全的方式释放之前的 MediaPlayer
                releaseMediaPlayer()
                
                // 创建新的 MediaPlayer
                println("MusicPlayer: 创建新的 MediaPlayer 实例，播放歌曲: ${track.musicName}")
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(track.musicUrl)
                    println("MusicPlayer: 设置数据源: ${track.musicUrl}")
                    
                    // 设置音频属性 (Android 6.0及以上)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                        )
                    } else {
                        setAudioStreamType(AudioManager.STREAM_MUSIC)
                    }
                    
                    // 设置监听器
                    setOnPreparedListener { mp ->
                        // 确保这是当前的MediaPlayer实例
                        if (mp == mediaPlayer) {
                            println("MusicPlayer: MediaPlayer 准备完成，开始播放: ${track.musicName}")
                            // 强制播放，不检查状态
                            mp.start()
                            // 更新持续时间
                            _playerState.update { it.copy(
                                duration = mp.duration.toLong(),
                                isPlaying = true // 确保状态为播放中
                            )}
                            println("MusicPlayer: 播放已开始，持续时间: ${mp.duration}ms")
                        } else {
                            println("MusicPlayer: 忽略旧的 MediaPlayer 准备完成回调")
                        }
                    }
                    
                    setOnErrorListener { mp, what, extra ->
                        // 添加更详细的错误处理
                        val errorMessage = when(what) {
                            MediaPlayer.MEDIA_ERROR_UNKNOWN -> "未知错误"
                            MediaPlayer.MEDIA_ERROR_SERVER_DIED -> "媒体服务器故障"
                            else -> "错误码: $what, $extra"
                        }
                        println("MusicPlayer: 播放错误 - $errorMessage")
                        
                        // 更新UI状态
                        _playerState.update { it.copy(isPlaying = false) }
                        
                        // 如果是服务器故障，尝试重新创建MediaPlayer
                        if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
                            // 在实际应用中，这里可以添加延迟重试的逻辑
                            println("MusicPlayer: 媒体服务器故障，需要重新创建播放器")
                        }
                        
                        true // 表示错误已处理
                    }
                    
                    setOnCompletionListener {
                        println("MusicPlayer: 歌曲播放完成，通知监听器")
                        val isRepeatEnabled = _playerState.value.isRepeatEnabled
                        val isShuffleEnabled = _playerState.value.isShuffleEnabled
                        
                        if (isRepeatEnabled) {
                            // 单曲循环模式，重新播放当前歌曲
                            println("MusicPlayer: 单曲循环模式，重新播放当前歌曲")
                            seekTo(0)
                            start()
                        } else if (_playlist.size > 1) {
                            // 通知外部监听器处理，并传递随机播放和单曲循环状态
                            onCompletionListener?.onCompletion(isRepeatEnabled, isShuffleEnabled)
                        } else {
                            // 播放列表只有一首歌，循环播放当前歌曲
                            seekTo(0)
                            start()
                        }
                    }
                    
                    // 处理播放器信息和缓冲状态
                    setOnInfoListener { mp, what, extra ->
                        when (what) {
                            MediaPlayer.MEDIA_INFO_BUFFERING_START -> {
                                println("MusicPlayer: 开始缓冲")
                            }
                            MediaPlayer.MEDIA_INFO_BUFFERING_END -> {
                                println("MusicPlayer: 缓冲结束")
                            }
                        }
                        false // 让系统继续处理这个事件
                    }
                    
                    // 开始异步准备
                    println("MusicPlayer: 开始异步准备播放")
                    prepareAsync()
                }
            } catch (e: Exception) {
                // 处理异常
                println("MusicPlayer: 播放异常: ${e.message}")
                e.printStackTrace()
                _playerState.update { it.copy(isPlaying = false) }
                
                // 确保MediaPlayer被正确释放
                releaseMediaPlayer()
                
                // 放弃音频焦点
                abandonAudioFocus()
            }
        } else {
            println("MusicPlayer: 播放索引超出范围: $index, 播放列表大小: ${_playlist.size}")
        }
    }

    /**
     * 安全释放MediaPlayer资源
     */
    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.apply {
                println("MusicPlayer: 停止和释放旧的 MediaPlayer 实例")
                try {
                    if (isPlaying) {
                        stop()
                    }
                    reset()
                    release()
                } catch (e: Exception) {
                    println("MusicPlayer: 释放MediaPlayer时发生异常: ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("MusicPlayer: 访问MediaPlayer时发生异常: ${e.message}")
        } finally {
            mediaPlayer = null
        }
    }

    fun togglePlayPause() {
        // 如果播放列表为空，不执行任何操作
        if (_playlist.isEmpty() || _playerState.value.currentTrack == null) {
            println("MusicPlayer: 播放列表为空或无当前曲目，无法播放/暂停")
            return
        }
        
        // 首先获取当前播放状态
        val currentlyPlaying = _playerState.value.isPlaying
        
        if (currentlyPlaying) {
            // 当前正在播放，需要暂停
            println("MusicPlayer: 暂停播放")
            pausePlaybackInternal()
            playOnAudioFocusGain = false // 用户主动暂停，不需要在获得焦点时自动恢复
            
            // 不需要放弃音频焦点，保持音频焦点以便稍后快速恢复
        } else {
            // 当前已暂停，需要开始播放
            // 尝试请求音频焦点
            if (requestAudioFocus()) {
                println("MusicPlayer: 获得音频焦点，开始播放")
                
                if (mediaPlayer != null) {
                    // MediaPlayer存在，直接开始播放
                    resumePlaybackInternal()
                } else if (_playerState.value.currentTrack != null) {
                    // MediaPlayer不存在，需要重新创建
                    println("MusicPlayer: MediaPlayer不存在，重新创建并播放当前歌曲")
                    try {
                        playTrack(_playerState.value.currentTrack!!)
                    } catch (e: Exception) {
                        println("MusicPlayer: 重新播放失败: ${e.message}")
                        _playerState.update { it.copy(isPlaying = false) }
                        abandonAudioFocus()
                    }
                }
            } else {
                println("MusicPlayer: 无法获取音频焦点，播放失败")
                _playerState.update { it.copy(isPlaying = false) }
            }
        }
    }

//    fun playNext() {
//        if (_playlist.isEmpty()) {
//            println("MusicPlayer: 播放列表为空，无法播放下一首")
//            return
//        }
//
//        val nextIndex = (_playerState.value.currentTrackIndex + 1) % _playlist.size
//        playTrackAt(nextIndex)
//    }
//
//    fun playPrevious() {
//        if (_playlist.isEmpty()) {
//            println("MusicPlayer: 播放列表为空，无法播放上一首")
//            return
//        }
//
//        val previousIndex = if (_playerState.value.currentTrackIndex > 0) {
//            _playerState.value.currentTrackIndex - 1
//        } else {
//            _playlist.size - 1
//        }
//        playTrackAt(previousIndex)
//    }

    fun seekTo(position: Long) {
        // 边界检查：确保位置在有效范围内
        val safePosition = position.coerceIn(0L, _playerState.value.duration.coerceAtLeast(1L))
        
        mediaPlayer?.let { player ->
            try {
                player.seekTo(safePosition.toInt())
                updateProgress(safePosition)
                println("MusicPlayer: 跳转播放位置到 ${formatTime(safePosition)}")
            } catch (e: Exception) {
                println("MusicPlayer: 跳转播放位置异常: ${e.message}")
                // 即使发生异常，也更新UI状态，以保持UI同步
                updateProgress(safePosition)
            }
        } ?: run {
            // MediaPlayer 不存在时，仍然更新状态，保持UI一致性
            println("MusicPlayer: MediaPlayer不存在，只更新进度状态")
            updateProgress(safePosition)
        }
    }

    private fun updateProgress(position: Long) {
        // 立即更新进度状态，不管变化大小
        val currentPosition = _playerState.value.currentPosition
        if (position != currentPosition) {
            _playerState.update { it.copy(currentPosition = position) }
        }
    }

    private fun formatTime(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    /**
     * 暂停进度更新，但不停止播放
     * 用于应用进入后台时调用，减少不必要的UI更新
     */
    fun pauseProgressUpdate() {
        // 仅移除进度更新回调，不影响实际播放
        handler.removeCallbacks(progressUpdater)
        println("MusicPlayer: pauseProgressUpdate() 被调用，暂停UI更新但继续播放")
    }

    /**
     * 向后兼容的方法，功能与 pauseProgressUpdate 相同
     * 保留此方法是为了维持与现有代码的兼容性
     */
//    fun release() {
//        // 调用新方法实现相同功能
//        pauseProgressUpdate()
//    }

    // 在应用完全退出时调用此方法
    fun releaseCompletely() {
        handler.removeCallbacks(progressUpdater)
        try {
            // 释放MediaPlayer资源
            releaseMediaPlayer()
            
            // 放弃音频焦点
            abandonAudioFocus()
            
            // 重置状态
            _playerState.update { PlayerState() }
            _playlist = emptyList()
            playOnAudioFocusGain = false
            
            println("MusicPlayer: 完全释放所有资源")
        } catch (e: Exception) {
            // 忽略释放时的异常
            println("MusicPlayer: 释放资源时发生异常: ${e.message}")
        }
    }
    
    /**
     * 恢复UI更新和播放状态
     * 用于应用从后台返回前台时调用
     * 重新启动进度更新器并确保播放状态与 playerState 一致
     */
    fun resumePlayback() {
        // 恢复进度更新
        handler.post(progressUpdater)
        
        // 如果之前正在播放，则尝试继续播放
        if (_playerState.value.isPlaying && mediaPlayer != null) {
            try {
                // 请求音频焦点
                if (requestAudioFocus()) {
                    if (!mediaPlayer!!.isPlaying) {
                        mediaPlayer!!.start()
                        println("MusicPlayer: 恢复播放")
                    }
                } else {
                    // 无法获取音频焦点，更新状态为暂停
                    _playerState.update { it.copy(isPlaying = false) }
                    println("MusicPlayer: 无法获取音频焦点，保持暂停状态")
                }
            } catch (e: Exception) {
                println("MusicPlayer: 恢复播放异常: ${e.message}")
                // 异常情况下更新状态为暂停
                _playerState.update { it.copy(isPlaying = false) }
            }
        }
    }

    /**
     * 更新当前曲目，但不自动播放
     * 用于同步PlaylistManager的状态到MusicPlayer
     */
    fun updateCurrentTrack(track: Song, index: Int) {
        if (index in _playlist.indices) {
            // 如果当前有歌曲正在播放，则保留播放状态
            val currentlyPlaying = _playerState.value.isPlaying
            
            // 只更新当前曲目的索引和信息，不触发播放
            _playerState.update { it.copy(
                currentTrackIndex = index,
                currentTrack = track
            )}
            
            // 如果MediaPlayer不存在且之前正在播放，需要创建新的MediaPlayer但不自动播放
            if (mediaPlayer == null && currentlyPlaying) {
                try {
                    // 创建新的MediaPlayer但尚不播放
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(track.musicUrl)
                        
                        setOnPreparedListener { mp ->
                            // 当准备完成时，如果仍在播放状态才开始播放
                            if (_playerState.value.isPlaying) {
                                mp.start()
                            }
                            // 更新持续时间
                            _playerState.update { it.copy(
                                duration = mp.duration.toLong()
                            )}
                        }
                        
                        setOnErrorListener { mp, what, extra ->
                            println("MusicPlayer: 播放错误 what=$what, extra=$extra")
                            _playerState.update { it.copy(isPlaying = false) }
                            true
                        }
                        
                        setOnCompletionListener {
                            println("MusicPlayer: 歌曲播放完成，通知监听器")
                            val isRepeatEnabled = _playerState.value.isRepeatEnabled
                            val isShuffleEnabled = _playerState.value.isShuffleEnabled
                            
                            if (isRepeatEnabled) {
                                // 单曲循环模式，重新播放当前歌曲
                                println("MusicPlayer: 单曲循环模式，重新播放当前歌曲")
                                seekTo(0)
                                start()
                            } else if (_playlist.size > 1) {
                                // 通知外部监听器处理，并传递随机播放和单曲循环状态
                                onCompletionListener?.onCompletion(isRepeatEnabled, isShuffleEnabled)
                            } else {
                                // 播放列表只有一首歌，循环播放当前歌曲
                                seekTo(0)
                                start()
                            }
                        }
                        
                        // 开始异步准备
                        prepareAsync()
                    }
                } catch (e: Exception) {
                    println("MusicPlayer: 创建MediaPlayer异常: ${e.message}")
                }
            }
            
            println("MusicPlayer: 更新当前曲目为 ${track.musicName}，索引 $index，但不自动播放")
        } else {
            println("MusicPlayer: 更新曲目失败，索引超出范围: $index, 播放列表大小: ${_playlist.size}")
        }
    }

    /**
     * 停止播放
     * 释放MediaPlayer资源并放弃音频焦点
     */
    fun stop() {
        // 释放MediaPlayer资源
        releaseMediaPlayer()
        
        // 放弃音频焦点
        abandonAudioFocus()
        
        // 更新播放状态
        _playerState.update { 
            it.copy(
                isPlaying = false,
                currentPosition = 0L,
                currentTrack = null
            )
        }
        
        playOnAudioFocusGain = false
        println("MusicPlayer: 已停止播放并重置播放器")
    }
} 