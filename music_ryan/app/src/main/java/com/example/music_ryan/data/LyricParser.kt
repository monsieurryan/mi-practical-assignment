package com.example.music_ryan.data

import com.example.music_ryan.data.model.LyricLine
import com.example.music_ryan.data.model.Lyrics

/**
 * 歌词解析器
 * 解析LRC格式的歌词文件
 */
object LyricParser {
    // LRC时间标签正则表达式，例如[00:12.34]
    private val timeTagRegex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})]")
    
    /**
     * 解析LRC格式的歌词内容
     * @param lrcContent LRC歌词文本内容
     * @return 解析后的歌词结构
     */
    fun parse(lrcContent: String): Lyrics {
        if (lrcContent.isBlank()) {
            return Lyrics(emptyList())
        }
        
        val lyricLines = mutableListOf<LyricLine>()
        
        // 按行分割歌词文本
        val lines = lrcContent.lines()
        
        println("LyricParser: 开始解析歌词，共 ${lines.size} 行")
        
        for (line in lines) {
            if (line.isBlank() || !line.startsWith("[")) {
                continue
            }
            
            // 处理有多个时间标签的行，如[00:23.45][01:23.45]歌词内容
            val timeMatches = timeTagRegex.findAll(line)
            val text = line.replaceFirst(Regex("(\\[\\d{2}:\\d{2}\\.\\d{2,3}])+"), "").trim()
            
            if (text.isBlank() || timeMatches.count() == 0) {
                continue
            }
            
            // 提取每个时间标签并创建对应的歌词行
            for (timeMatch in timeMatches) {
                val values = timeMatch.groupValues
                if (values.size >= 4) {
                    try {
                        val minutes = values[1].toInt()
                        val seconds = values[2].toInt()
                        val milliseconds = if (values[3].length == 2) values[3].toInt() * 10 else values[3].toInt()
                        
                        // 转换为总毫秒数
                        val timeMs = (minutes * 60 * 1000) + (seconds * 1000) + milliseconds
                        
                        lyricLines.add(LyricLine(timeMs.toLong(), text))
                        // println("LyricParser: 解析行 [${minutes}:${seconds}.${milliseconds}](${timeMs}ms) $text")
                    } catch (e: Exception) {
                        println("LyricParser: 解析时间标签失败: ${e.message}")
                    }
                }
            }
        }
        
        // 按时间排序
        val sortedLyrics = lyricLines.sortedBy { it.time }
        println("LyricParser: 歌词解析完成，共解析出 ${sortedLyrics.size} 行有效歌词")
        
        return Lyrics(sortedLyrics)
    }
} 