package com.example.music_yishuai.ui.viewmodels

import android.graphics.Bitmap
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/**
 * 主题管理类，负责颜色提取和背景色管理
 */
class ThemeManager {
    
    // 背景色缓存
    private val _backgroundColors = MutableStateFlow<Map<String, Int>>(emptyMap())
    
    /**
     * 查询背景色缓存
     */
    fun getBackgroundColor(coverUrl: String): Int? {
        return _backgroundColors.value[coverUrl]
    }
    
    /**
     * 保存背景色到缓存
     */
    fun saveBackgroundColor(coverUrl: String, color: Int) {
        if (coverUrl.isBlank()) return
        
        _backgroundColors.update { current ->
            val map = current.toMutableMap()
            map[coverUrl] = color
            map
        }
    }
    
    /**
     * 轻量级封面颜色提取
     * 使用5x5采样网格获取主色调，减少处理时间
     */
    fun extractDominantColor(bitmap: Bitmap?): Int {
        if (bitmap == null) return 0xFF3325CD.toInt() // 默认主题色
        
        try {
            // 缩小图片尺寸进行采样
            val sampledBitmap = Bitmap.createScaledBitmap(bitmap, 5, 5, true)
            
            // 计算平均色值
            var redSum = 0
            var greenSum = 0
            var blueSum = 0
            var pixelCount = 0
            
            for (y in 0 until sampledBitmap.height) {
                for (x in 0 until sampledBitmap.width) {
                    val pixel = sampledBitmap.getPixel(x, y)
                    
                    // 忽略过于明亮或过于暗淡的像素
                    val red = android.graphics.Color.red(pixel)
                    val green = android.graphics.Color.green(pixel)
                    val blue = android.graphics.Color.blue(pixel)
                    val brightness = (red + green + blue) / 3
                    
                    if (brightness > 30 && brightness < 230) {
                        redSum += red
                        greenSum += green
                        blueSum += blue
                        pixelCount++
                    }
                }
            }
            
            // 清理临时位图
            if (sampledBitmap != bitmap) {
                sampledBitmap.recycle()
            }
            
            // 计算平均值并创建颜色
            return if (pixelCount > 0) {
                val avgRed = redSum / pixelCount
                val avgGreen = greenSum / pixelCount
                val avgBlue = blueSum / pixelCount
                
                // 调整饱和度和亮度以获得更好的视觉效果
                val hsv = FloatArray(3)
                android.graphics.Color.RGBToHSV(avgRed, avgGreen, avgBlue, hsv)
                hsv[1] = kotlin.math.min(hsv[1] * 1.2f, 1.0f) // 增加饱和度
                hsv[2] = kotlin.math.min(hsv[2] * 0.9f, 1.0f) // 稍微降低亮度
                
                android.graphics.Color.HSVToColor(hsv)
            } else {
                0xFF3325CD.toInt() // 默认主题色
            }
        } catch (e: Exception) {
            println("ThemeManager: 提取颜色出错: ${e.message}")
            return 0xFF3325CD.toInt() // 发生错误时返回默认色
        }
    }
    
    /**
     * 提取并处理图片的主题色
     * @param coverUrl 封面URL，用作缓存键
     * @param bitmap 图片位图
     * @return 提取的颜色，如果失败则返回默认颜色
     */
    suspend fun processAndExtractColor(coverUrl: String, bitmap: Bitmap?): Int {
        if (coverUrl.isBlank() || bitmap == null) return 0xFF3325CD.toInt()
        
        // 检查缓存
        val cachedColor = getBackgroundColor(coverUrl)
        if (cachedColor != null) {
            return cachedColor
        }
        
        return withContext(Dispatchers.Default) {
            try {
                println("ThemeManager: 开始处理封面图片并提取主题色")
                
                // 首先尝试使用Palette库提取颜色
                val extractedColor = try {
                    val palette = Palette.from(bitmap).generate()
                    
                    // 优先使用暗色饱和颜色，提供更好的视觉效果
                    palette.getDarkVibrantColor(
                        palette.getDarkMutedColor(
                            palette.getVibrantColor(
                                palette.getMutedColor(0xFF3325CD.toInt())
                            )
                        )
                    )
                } catch (e: Exception) {
                    println("ThemeManager: Palette提取失败，使用备用算法: ${e.message}")
                    // 备用：使用简单算法提取主色调
                    extractDominantColor(bitmap)
                }
                
                // 调整颜色以适合背景使用
                val adjustedColor = adjustColorForBackground(extractedColor)
                
                // 更新背景色缓存
                saveBackgroundColor(coverUrl, adjustedColor)
                
                println("ThemeManager: 完成封面主题色处理并保存到缓存")
                
                adjustedColor
            } catch (e: Exception) {
                println("ThemeManager: 处理封面主题色时出错: ${e.message}")
                val defaultColor = 0xFF3325CD.toInt()
                saveBackgroundColor(coverUrl, defaultColor)
                defaultColor
            }
        }
    }
    
    /**
     * 调整颜色使其更适合作为背景
     * 降低亮度并增加饱和度，使颜色更加美观
     */
    fun adjustColorForBackground(color: Int): Int {
        try {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(color, hsv)
            
            // 增加饱和度
            hsv[1] = kotlin.math.min(hsv[1] * 1.2f, 1.0f)
            
            // 降低亮度（减少明度值）
            hsv[2] = kotlin.math.max(hsv[2] * 0.85f, 0.1f)
            
            return android.graphics.Color.HSVToColor(hsv)
        } catch (e: Exception) {
            println("ThemeManager: 调整颜色失败: ${e.message}")
            return color
        }
    }
    
    /**
     * 清空背景色缓存
     */
    fun clearCache() {
        _backgroundColors.value = emptyMap()
    }
} 