package com.example.music_ryan.util

import android.content.Context
import coil.Coil
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Size

/**
 * 图片加载工具类
 * 提供统一的图片加载接口，使用应用级ImageLoader
 */
object ImageLoaderUtil {
    /**
     * 获取应用级ImageLoader实例
     * @param context 上下文
     * @return 应用级ImageLoader实例
     */
    fun getImageLoader(context: Context): ImageLoader {
        return Coil.imageLoader(context)
    }
    
    /**
     * 构建标准的ImageRequest请求
     * @param context 上下文
     * @param data 图片数据源（URL、资源ID等）
     * @param size 图片加载尺寸，默认为原始尺寸
     * @param allowHardware 是否允许硬件加速，默认为true
     * @param crossfade 是否使用淡入淡出效果，默认为true
     * @return 构建好的ImageRequest
     */
    fun buildImageRequest(
        context: Context,
        data: Any,
        size: Size = Size.ORIGINAL,
        allowHardware: Boolean = true,
        crossfade: Boolean = true
    ): ImageRequest {
        return ImageRequest.Builder(context)
            .data(data)
            .size(size)
            .allowHardware(allowHardware)
            .crossfade(crossfade)
            .build()
    }
    
    /**
     * 预加载图片到缓存
     * @param context 上下文
     * @param data 图片数据源（URL等）
     */
    fun preloadImage(context: Context, data: Any) {
        val request = buildImageRequest(context, data)
        getImageLoader(context).enqueue(request)
    }
    
    /**
     * 清除图片缓存
     * @param context 上下文
     */
    fun clearImageCache(context: Context) {
        try {
            getImageLoader(context).memoryCache?.clear()
            println("ImageLoaderUtil: 已清除内存缓存")
        } catch (e: Exception) {
            println("ImageLoaderUtil: 清除内存缓存失败: ${e.message}")
        }
    }
} 