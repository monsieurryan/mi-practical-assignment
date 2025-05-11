package com.example.music_yishuai.data.model

/**
 * 歌词数据模型
 * @property time 歌词时间点（毫秒）
 * @property text 歌词文本
 */
data class LyricLine(
    val time: Long,
    val text: String
)

/**
 * 完整歌词数据
 * @property lines 所有歌词行
 */
data class Lyrics(
    val lines: List<LyricLine> = emptyList()
) {
    /**
     * 根据当前播放时间获取当前应显示的歌词行索引
     */
    fun getCurrentLineIndex(currentTime: Long): Int {
        if (lines.isEmpty()) return -1
        
        // 如果当前时间小于第一行时间，返回第一行
        if (currentTime < lines.first().time) return 0
        
        // 二分查找找到最接近但不超过当前时间的行
        var low = 0
        var high = lines.size - 1
        
        while (low <= high) {
            val mid = (low + high) / 2
            val midTime = lines[mid].time
            
            if (mid < lines.size - 1 && currentTime >= midTime && currentTime < lines[mid + 1].time) {
                return mid
            } else if (currentTime < midTime) {
                high = mid - 1
            } else {
                low = mid + 1
            }
        }
        
        // 如果当前时间大于最后一行时间，返回最后一行
        return if (currentTime >= lines.last().time) lines.size - 1 else -1
    }
} 