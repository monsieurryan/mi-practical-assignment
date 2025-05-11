package com.example.music_ryan

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import okhttp3.OkHttpClient
import okhttp3.Cache
import java.util.concurrent.TimeUnit
import com.example.music_ryan.player.MusicPlayer
import com.example.music_ryan.util.ImageLoaderUtil

class MusicApplication : Application(), ImageLoaderFactory {
    // 全局单例 MusicPlayer 实例
    companion object {
        private var musicPlayer: MusicPlayer? = null
        private var okHttpClient: OkHttpClient? = null
        
        fun getMusicPlayer(context: Application): MusicPlayer {
            if (musicPlayer == null) {
                musicPlayer = MusicPlayer(context)
            }
            return musicPlayer!!
        }
        
        // 获取全局OkHttpClient实例的方法
        fun getOkHttpClient(context: Application): OkHttpClient {
            if (okHttpClient == null) {
                okHttpClient = createOkHttpClient(context)
            }
            return okHttpClient!!
        }
        
        // 创建OkHttpClient实例的私有方法
        private fun createOkHttpClient(context: Application): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .cache(Cache(context.cacheDir.resolve("okhttp_cache"), 10L * 1024L * 1024L)) // 添加10MB缓存
                .build()
        }
        
        // 创建带自定义缓存路径的OkHttpClient
        fun getOkHttpClientWithCache(context: Application, cacheName: String, cacheSizeMB: Int = 5): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .cache(Cache(context.cacheDir.resolve(cacheName), cacheSizeMB.toLong() * 1024L * 1024L))
                .build()
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        // 在应用启动时初始化OkHttpClient单例
        if (okHttpClient == null) {
            okHttpClient = createOkHttpClient(this)
        }
        
        // 初始化NetworkClient
        try {
            com.example.music_ryan.network.NetworkClient.initialize(this)
            println("MusicApplication: NetworkClient已初始化")
        } catch (e: Exception) {
            println("MusicApplication: NetworkClient初始化失败: ${e.message}")
        }
        
        // 初始化并测试工具类ImageLoaderUtil
        try {
            val imageLoader = ImageLoaderUtil.getImageLoader(this)
            println("MusicApplication: ImageLoaderUtil已初始化，成功获取应用级ImageLoader")
        } catch (e: Exception) {
            println("MusicApplication: ImageLoaderUtil初始化失败: ${e.message}")
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        println("MusicApplication: onTerminate() 被调用，应用即将退出")
        
        try {
            // 应用退出时释放资源
            musicPlayer?.releaseCompletely()
            println("MusicApplication: 已释放 MusicPlayer 资源")
        } catch (e: Exception) {
            println("MusicApplication: 释放 MusicPlayer 资源时发生异常: ${e.message}")
        }
        
        try {
            // 取消所有进行中的网络请求
            okHttpClient?.dispatcher?.cancelAll()
            // 同时取消NetworkClient中的请求
            com.example.music_ryan.network.NetworkClient.cancelAllRequests()
            println("MusicApplication: 已取消所有进行中的网络请求")
        } catch (e: Exception) {
            println("MusicApplication: 取消网络请求时发生异常: ${e.message}")
        }
        
        // 确保清空单例实例
        musicPlayer = null
        okHttpClient = null
        println("MusicApplication: 已清空所有单例引用")
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .okHttpClient {
                // 直接使用全局单例的OkHttpClient
                getOkHttpClient(this@MusicApplication)
            }
            .logger(DebugLogger())
            .crossfade(true)
            .build()
    }
} 