package com.example.model

enum class RepeatMode {
    OFF, ALL, ONE
}

data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.ALL,
    val songsPlayedSinceLastAd: Int = 0,
    val isAdPlaying: Boolean = false,
    val adCountdownSeconds: Int = 0,
    val currentAdTitle: String = "Sanggeet Sponsor Announcement",
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val playbackError: String? = null
) {
    val progress: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val songsUntilNextAd: Int
        get() = (4 - (songsPlayedSinceLastAd % 4)).coerceIn(1, 4)
}
