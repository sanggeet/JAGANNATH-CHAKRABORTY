package com.example.playback

import android.content.Context
import android.content.Intent
import com.example.model.PlaybackState
import com.example.model.RepeatMode
import com.example.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PlayerManager {

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    fun updateState(transform: (PlaybackState) -> PlaybackState) {
        _state.value = transform(_state.value)
    }

    fun playSong(context: Context, song: Song, queue: List<Song>) {
        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_PLAY_SONG
            putExtra(MusicPlaybackService.EXTRA_SONG_ID, song.id)
            putExtra(MusicPlaybackService.EXTRA_SONG_TITLE, song.title)
            putExtra(MusicPlaybackService.EXTRA_SONG_ARTIST, song.artist)
            putExtra(MusicPlaybackService.EXTRA_SONG_CATEGORY, song.category)
            putExtra(MusicPlaybackService.EXTRA_STREAM_URL, song.streamUrl)
        }
        _state.value = _state.value.copy(
            queue = queue,
            currentIndex = queue.indexOfFirst { it.id == song.id }.let { if (it >= 0) it else 0 }
        )
        context.startService(intent)
    }

    fun togglePlayPause(context: Context) {
        val action = if (_state.value.isPlaying) {
            MusicPlaybackService.ACTION_PAUSE
        } else {
            MusicPlaybackService.ACTION_RESUME
        }
        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            this.action = action
        }
        context.startService(intent)
    }

    fun skipNext(context: Context) {
        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_NEXT
        }
        context.startService(intent)
    }

    fun skipPrevious(context: Context) {
        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_PREVIOUS
        }
        context.startService(intent)
    }

    fun seekTo(context: Context, positionMs: Long) {
        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_SEEK
            putExtra(MusicPlaybackService.EXTRA_SEEK_POS, positionMs)
        }
        context.startService(intent)
    }

    fun toggleShuffle() {
        val newShuffle = !_state.value.isShuffleEnabled
        _state.value = _state.value.copy(isShuffleEnabled = newShuffle)
    }

    fun cycleRepeatMode() {
        val nextMode = when (_state.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _state.value = _state.value.copy(repeatMode = nextMode)
    }

    fun stopPlayback(context: Context) {
        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_STOP
        }
        context.startService(intent)
    }
}
