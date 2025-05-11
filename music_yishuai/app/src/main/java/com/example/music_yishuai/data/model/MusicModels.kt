package com.example.music_yishuai.data.model

/**
 * 歌曲数据类
 * @property id 歌曲ID
 * @property musicName 歌曲名称
 * @property author 作者
 * @property coverUrl 封面图片URL
 * @property musicUrl 音乐文件URL
 * @property lyricUrl 歌词文件URL
 */
data class Song(
    val id: Int,
    val musicName: String,
    val author: String,
    val coverUrl: String,
    val musicUrl: String,
    val lyricUrl: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Song) return false

        return id == other.id
    }

    override fun hashCode(): Int {
        return id
    }
}

