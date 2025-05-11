package com.example.music_ryan.player

import com.example.music_ryan.data.model.Song

data class PlayerState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val currentTrackIndex: Int = 0,
    val currentTrack: Song? = null,
    val playlist: List<Song> = emptyList(),
    val isShuffleEnabled: Boolean = false,
    val isRepeatEnabled: Boolean = false,
    val wasPlayingBeforeNetworkLoss: Boolean = false,
    val playbackError: String? = null,
    val isBuffering: Boolean = false
) 