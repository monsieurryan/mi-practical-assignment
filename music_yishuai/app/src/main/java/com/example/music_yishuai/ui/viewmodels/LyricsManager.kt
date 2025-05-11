package com.example.music_yishuai.ui.viewmodels

import android.app.Application
import com.example.music_yishuai.MusicApplication
import com.example.music_yishuai.data.LyricParser
import com.example.music_yishuai.data.model.Lyrics
import com.example.music_yishuai.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Request
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

/**
 * 歌词管理类，负责歌词的加载、缓存和状态管理
 */
class LyricsManager(application: Application) {
    
    // 歌词相关状态
    private val _lyrics = MutableStateFlow<Lyrics>(Lyrics(emptyList()))
    val lyrics: StateFlow<Lyrics> = _lyrics.asStateFlow()
    
    private val _isLoadingLyrics = MutableStateFlow(false)
    val isLoadingLyrics: StateFlow<Boolean> = _isLoadingLyrics.asStateFlow()
    
    private val _lyricError = MutableStateFlow<String?>(null)
    val lyricError: StateFlow<String?> = _lyricError.asStateFlow()
    
    // 使用全局OkHttpClient
    private val okHttpClient by lazy {
        MusicApplication.getOkHttpClientWithCache(application, "lyrics_cache", 5)
    }
    
    // 歌词缓存
    private val lyricsCache = mutableMapOf<String, Lyrics>()
    
    /**
     * 获取当前歌词行索引
     */
    fun getCurrentLyricIndex(currentPosition: Long): Int {
        return lyrics.value.getCurrentLineIndex(currentPosition)
    }
    
    /**
     * 预加载指定歌曲的歌词
     */
    suspend fun preloadLyrics(song: Song) {
        // 检查是否已经有缓存
        val cacheKey = song.lyricUrl.ifBlank { "song_${song.id}_default_lyrics" }
        if (lyricsCache.containsKey(cacheKey)) {
            println("LyricsManager: 歌曲的歌词已在缓存中")
            return
        }
        
        // 在后台线程中预加载歌词
        println("LyricsManager: 预加载歌曲的歌词: ${song.musicName}")
        try {
            loadLyricsInternal(song, isPreloading = true)
            println("LyricsManager: 成功预加载歌词: ${song.musicName}")
        } catch (e: Exception) {
            println("LyricsManager: 预加载歌词失败: ${e.message}")
        }
    }
    
    /**
     * 内部歌词加载实现
     * @param song 要加载歌词的歌曲
     * @param isPreloading 是否是预加载模式（预加载模式不更新UI状态）
     * @return 加载的歌词对象
     */
    suspend fun loadLyricsInternal(song: Song, currentSongId: Int? = null, isPreloading: Boolean = false): Lyrics? {
        // 确保当前歌曲与要加载歌词的歌曲一致
        if (!isPreloading && currentSongId != null && currentSongId != song.id) {
            println("LyricsManager: 当前播放的歌曲与要加载歌词的歌曲不一致，跳过加载")
            return null
        }
        
        // 如果不是预加载，更新UI状态
        if (!isPreloading) {
            withContext(Dispatchers.Main) {
                _isLoadingLyrics.value = true
                _lyricError.value = null
            }
        }
        
        // 检查是否有歌词URL
        if (song.lyricUrl.isBlank()) {
            println("LyricsManager: 歌曲没有提供歌词URL，使用本地测试歌词")
            return loadTestLyricsInternal(song.id.toString(), isPreloading)
        }
        
        // 检查URL是否有效
        try {
            val url = java.net.URL(song.lyricUrl)
            if (url.protocol != "http" && url.protocol != "https") {
                println("LyricsManager: 不支持的歌词URL协议: ${url.protocol}")
                if (!isPreloading) {
                    withContext(Dispatchers.Main) {
                        _lyricError.value = "不支持的歌词URL协议: ${url.protocol}"
                    }
                }
                return loadTestLyricsInternal(song.id.toString(), isPreloading)
            }
        } catch (e: Exception) {
            println("LyricsManager: 歌词URL格式无效: ${e.message}")
            if (!isPreloading) {
                withContext(Dispatchers.Main) {
                    _lyricError.value = "歌词URL格式无效"
                }
            }
            return loadTestLyricsInternal(song.id.toString(), isPreloading)
        }
        
        // 使用通用缓存方法获取或加载歌词
        return getLyricsWithCache(song, isPreloading) {
            // 尝试加载网络歌词，包含重试逻辑
            var retryCount = 0
            val maxRetries = 2
            var lastError: Exception? = null
            
            while (retryCount <= maxRetries) {
                try {
                    // 创建请求
                    val request = Request.Builder()
                        .url(song.lyricUrl)
                        .build()
                    
                    // 添加重试日志
                    if (retryCount > 0) {
                        println("LyricsManager: 歌词加载重试 #$retryCount: ${song.musicName}")
                        delay((500 * retryCount).milliseconds) // 延迟增加
                    } else {
                        println("LyricsManager: 开始执行网络请求获取歌词")
                    }
                    
                    // 带有超时的网络请求
                    val response = withTimeoutOrNull(5000) { // 5秒超时
                        okHttpClient.newCall(request).execute()
                    } ?: throw IOException("歌词请求超时")
                    
                    println("LyricsManager: 歌词请求完成，状态码: ${response.code}")
                    
                    if (!response.isSuccessful) {
                        throw IOException("获取歌词失败: 状态码 ${response.code}")
                    }
                    
                    // 检查响应体是否为空
                    val responseBody = response.body
                        ?: throw IOException("获取歌词失败: 响应体为空")
                    
                    // 获取歌词内容
                    val lyricsContent = responseBody.string()
                    
                    // 检查内容是否为空
                    if (lyricsContent.isBlank()) {
                        throw IOException("歌词内容为空")
                    }
                    
                    println("LyricsManager: 成功读取歌词内容，长度: ${lyricsContent.length}")
                    
                    // 在IO线程中解析歌词
                    val parsedLyrics = LyricParser.parse(lyricsContent)
                    
                    // 正常清理资源
                    response.close()
                    return@getLyricsWithCache parsedLyrics
                } catch (e: Exception) {
                    println("LyricsManager: 歌词加载出错: ${e.message}")
                    lastError = e
                    retryCount++
                    
                    // 如果已经达到最大重试次数，或者是预加载模式，则不再重试
                    if (retryCount > maxRetries || isPreloading) {
                        break
                    }
                }
            }
            
            // 所有重试都失败
            println("LyricsManager: 歌词加载失败，使用测试歌词: ${lastError?.message}")
            if (!isPreloading) {
                withContext(Dispatchers.Main) {
                    _lyricError.value = "加载歌词失败: ${lastError?.message}"
                }
            }
            throw lastError ?: IOException("未知错误")
        } ?: loadTestLyricsInternal(song.id.toString(), isPreloading) // 如果发生异常，使用测试歌词
    }
    
    /**
     * 内部测试歌词加载实现
     * @param cacheKey 缓存键
     * @param isPreloading 是否是预加载模式
     * @return 加载的歌词对象
     */
    private suspend fun loadTestLyricsInternal(cacheKey: String, isPreloading: Boolean): Lyrics {
        // 创建一个模拟的Song对象以使用通用缓存方法
        val dummySong = Song(
            id = -1,
            musicName = "测试歌曲",
            author = "测试作者",
            coverUrl = "",
            musicUrl = "",
            lyricUrl = ""
        )
        
        // 使用通用缓存方法获取或加载测试歌词
        return getLyricsWithCache(dummySong.copy(id = cacheKey.toIntOrNull() ?: -1), isPreloading) {
            val testLyrics = """
                [00:00.00] 测试歌词
                [00:02.00] 这是一个测试歌词文件
                [00:05.00] 用于在无法加载网络歌词时显示
                [00:08.00] 你可以看到这些歌词会跟随音乐播放
                [00:12.00] 自动滚动并高亮显示当前行
                [00:16.00] 你也可以拖动进度条
                [00:20.00] 歌词会自动跳转到对应位置
                [00:24.00] 这个功能非常实用
                [00:28.00] 希望你喜欢这个音乐播放器
                [00:32.00] 谢谢使用
            """.trimIndent()
            
            // 生成随机偏移时间，使不同歌曲显示不同的测试歌词
            val randomOffset = (cacheKey.hashCode() % 10) * 1000L // 0到9秒的随机偏移
            
            // 解析测试歌词
            val parsedLyrics = LyricParser.parse(testLyrics)
            
            // 应用随机偏移，使不同歌曲的测试歌词显示不同
            val adjustedLines = parsedLyrics.lines.map { line ->
                line.copy(time = line.time + randomOffset)
            }
            
            Lyrics(adjustedLines)
        } ?: Lyrics(emptyList()) // 如果发生异常，返回空歌词
    }
    
    /**
     * 通用的歌词缓存逻辑
     * @param song 要加载歌词的歌曲
     * @param isPreloading 是否是预加载模式
     * @param loadLyrics 加载歌词的函数
     * @return 加载的歌词对象
     */
    private suspend fun getLyricsWithCache(
        song: Song,
        isPreloading: Boolean = false,
        loadLyrics: suspend () -> Lyrics
    ): Lyrics? {
        val cacheKey = song.lyricUrl.ifBlank { "song_${song.id}_default_lyrics" }
        
        return withCache(
            cacheKey = cacheKey,
            cache = lyricsCache,
            loadData = loadLyrics,
            isPreloading = isPreloading,
            onCacheHit = { lyrics ->
                withContext(Dispatchers.Main) {
                    _lyrics.value = lyrics
                    _isLoadingLyrics.value = false
                    _lyricError.value = null
                }
            },
            onDataLoaded = { lyrics ->
                withContext(Dispatchers.Main) {
                    _lyrics.value = lyrics
                    _isLoadingLyrics.value = false
                    _lyricError.value = null
                }
                println("LyricsManager: 歌词加载成功，共 ${lyrics.lines.size} 行")
            }
        )
    }
    
    /**
     * 通用缓存逻辑，用于获取和更新缓存
     */
    private suspend fun <T> withCache(
        cacheKey: String,
        cache: MutableMap<String, T>,
        loadData: suspend () -> T,
        isPreloading: Boolean = false,
        onCacheHit: suspend (T) -> Unit = {},
        onDataLoaded: suspend (T) -> Unit = {}
    ): T? {
        // 从缓存检查
        val cachedData = cache[cacheKey]
        if (cachedData != null) {
            if (!isPreloading) {
                onCacheHit(cachedData)
                println("LyricsManager: 使用缓存数据 - $cacheKey")
            }
            return cachedData
        }
        
        try {
            // 加载数据
            val data = loadData()
            
            // 保存到缓存
            cache[cacheKey] = data
            
            // 如果不是预加载，调用数据加载完成回调
            if (!isPreloading) {
                onDataLoaded(data)
            }
            
            return data
        } catch (e: Exception) {
            println("LyricsManager: 加载数据失败: ${e.message}")
            throw e
        }
    }
    
    /**
     * 清空歌词缓存
     */
    fun clearCache() {
        lyricsCache.clear()
        _lyricError.value = null
        _isLoadingLyrics.value = false
    }
} 