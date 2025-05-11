package com.example.music_ryan.data

import com.example.music_ryan.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 播放列表管理器
 * 单例类，用于在整个应用中保持播放列表状态
 */
object PlaylistManager {
    // 当前播放列表
    private val _playlist = MutableStateFlow<List<Song>>(emptyList())
    val playlist: StateFlow<List<Song>> = _playlist.asStateFlow()
    
    // 当前播放的歌曲
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()
    
    /**
     * 添加歌曲到播放列表顶部
     */
    fun addToTopOfPlaylist(song: Song) {
        val currentList = _playlist.value
        
        // 检查歌曲是否已在列表中
        val existingIndex = currentList.indexOfFirst { it.id == song.id }
        
        if (existingIndex != -1) {
            // 歌曲已存在，将其从原位置移除并添加到顶部
            val newList = currentList.toMutableList()
            newList.removeAt(existingIndex)
            newList.add(0, song)
            _playlist.value = newList
        } else {
            // 歌曲不存在，直接添加到顶部
            _playlist.value = listOf(song) + currentList
        }
        
        // 设置为当前歌曲
        _currentSong.value = song
        println("PlaylistManager: 添加歌曲到播放列表顶部: ${song.musicName}")
    }
    
    /**
     * 添加歌曲到播放列表末尾并设置为当前歌曲
     */
    fun addToEndOfPlaylist(song: Song) {
        val currentList = _playlist.value
        println("PlaylistManager: 当前播放列表大小: ${currentList.size}")
        
        // 检查歌曲是否已在列表中
        val existingIndex = currentList.indexOfFirst { it.id == song.id }
        println("PlaylistManager: 检查歌曲 ${song.musicName} 是否在列表中，索引: $existingIndex")
        
        if (existingIndex != -1) {
            // 如果歌曲已存在，不做添加操作，只设置为当前歌曲
            println("PlaylistManager: 歌曲已存在于列表中，将其设置为当前歌曲: ${song.musicName}")
            _currentSong.value = song
        } else {
            // 歌曲不存在，添加到末尾
            val newList = currentList + song
            _playlist.value = newList
            // 设置为当前歌曲
            _currentSong.value = song
            println("PlaylistManager: 添加歌曲到播放列表末尾 (新大小: ${newList.size})，并设置为当前歌曲: ${song.musicName}")
            
            // 验证添加成功
            val afterAddIndex = _playlist.value.indexOfFirst { it.id == song.id }
            println("PlaylistManager: 验证添加后索引: $afterAddIndex")
        }
    }
    
    /**
     * 设置当前播放的歌曲
     */
    fun setCurrentSong(song: Song) {
        // 检查是否当前歌曲已经是这首歌
        val currentSong = _currentSong.value
        if (currentSong?.id == song.id) {
            println("PlaylistManager: 当前歌曲未变化，仍为: ${song.musicName}")
            return
        }
        
        // 设置新的当前歌曲
        _currentSong.value = song
        println("PlaylistManager: 设置新的当前歌曲: ${song.musicName}")
    }
    
    /**
     * 获取整个播放列表
     */
    fun getPlaylist(): List<Song> {
        return _playlist.value
    }
    
    /**
     * 清空播放列表
     */
    fun clearPlaylist() {
        _playlist.value = emptyList()
        _currentSong.value = null
        println("PlaylistManager: 清空播放列表")
    }
    
    /**
     * 从播放列表中删除歌曲
     * @return 返回一个Triple，包含：
     * - 是否成功删除
     * - 是否删除的是当前歌曲
     * - 如果删除的是当前歌曲，返回删除后的下一首歌曲（可能为null）
     */
    fun removeSongFromPlaylist(song: Song): Triple<Boolean, Boolean, Song?> {
        val currentList = _playlist.value
        println("PlaylistManager: 尝试从播放列表中删除歌曲: ${song.musicName}")
        
        // 检查歌曲是否在列表中
        val index = currentList.indexOfFirst { it.id == song.id }
        
        if (index == -1) {
            // 歌曲不在列表中
            println("PlaylistManager: 歌曲不在播放列表中，无法删除")
            return Triple(false, false, null)
        }
        
        // 判断是否是当前歌曲
        val isCurrentSong = _currentSong.value?.id == song.id
        println("PlaylistManager: 删除的${if (isCurrentSong) "是" else "不是"}当前播放的歌曲")
        
        // 移除歌曲
        val newList = currentList.toMutableList()
        newList.removeAt(index)
        _playlist.value = newList
        
        // 如果删除的是当前歌曲，需要重新设置当前歌曲
        var nextSong: Song? = null
        
        if (isCurrentSong) {
            if (newList.isEmpty()) {
                // 删除后播放列表为空
                _currentSong.value = null
                println("PlaylistManager: 删除后播放列表为空，设置当前歌曲为null")
            } else {
                // 删除后播放列表不为空，选择下一首歌曲作为当前歌曲
                // 索引处理：如果删除的是最后一首，则播放第一首
                val nextIndex = if (index < newList.size) index else 0
                nextSong = newList[nextIndex]
                _currentSong.value = nextSong
                println("PlaylistManager: 设置新的当前歌曲: ${nextSong.musicName}")
            }
        }
        
        println("PlaylistManager: 成功从播放列表中删除歌曲，剩余歌曲数: ${newList.size}")
        return Triple(true, isCurrentSong, nextSong)
    }
} 