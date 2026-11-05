package com.example.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState as SystemPlaybackState
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.DriveFolderRepository
import com.example.model.AdItem
import com.example.model.RepeatMode
import com.example.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random

class MusicPlaybackService : Service(), MediaPlayer.OnPreparedListener,
    MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    companion object {
        private const val TAG = "MusicPlaybackService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "sanggeet_music_channel"

        const val ACTION_PLAY_SONG = "com.example.action.PLAY_SONG"
        const val ACTION_RESUME = "com.example.action.RESUME"
        const val ACTION_PAUSE = "com.example.action.PAUSE"
        const val ACTION_NEXT = "com.example.action.NEXT"
        const val ACTION_PREVIOUS = "com.example.action.PREVIOUS"
        const val ACTION_SEEK = "com.example.action.SEEK"
        const val ACTION_STOP = "com.example.action.STOP"

        const val EXTRA_SONG_ID = "extra_song_id"
        const val EXTRA_SONG_TITLE = "extra_song_title"
        const val EXTRA_SONG_ARTIST = "extra_song_artist"
        const val EXTRA_SONG_CATEGORY = "extra_song_category"
        const val EXTRA_STREAM_URL = "extra_stream_url"
        const val EXTRA_SEEK_POS = "extra_seek_pos"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var adPlayer: MediaPlayer? = null
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null
    private var adCountdownJob: Job? = null

    private lateinit var driveRepo: DriveFolderRepository
    private var cachedAds: List<AdItem> = emptyList()
    private val random = Random()

    override fun onCreate() {
        super.onCreate()
        driveRepo = DriveFolderRepository(applicationContext)
        createNotificationChannel()
        initMediaSession()

        serviceScope.launch(Dispatchers.IO) {
            try {
                cachedAds = driveRepo.getAds()
            } catch (e: Exception) {
                Log.e(TAG, "Failed loading ads: ${e.message}")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sanggeet Playback Controls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground playback notification with audio controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun initMediaSession() {
        mediaSession = MediaSession(this, "SanggeetMediaSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    resumeMusic()
                }

                override fun onPause() {
                    pauseMusic()
                }

                override fun onSkipToNext() {
                    playNextSong()
                }

                override fun onSkipToPrevious() {
                    playPreviousSong()
                }

                override fun onStop() {
                    stopMusic()
                }

                override fun onSeekTo(pos: Long) {
                    seekMusic(pos)
                }
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_SONG -> {
                val songId = intent.getStringExtra(EXTRA_SONG_ID) ?: return START_NOT_STICKY
                val title = intent.getStringExtra(EXTRA_SONG_TITLE) ?: "Unknown Track"
                val artist = intent.getStringExtra(EXTRA_SONG_ARTIST) ?: "Sanggeet Studio"
                val category = intent.getStringExtra(EXTRA_SONG_CATEGORY) ?: "Music"
                val url = intent.getStringExtra(EXTRA_STREAM_URL)
                    ?: "https://drive.google.com/uc?export=download&id=$songId"

                val song = Song(
                    id = songId,
                    title = title,
                    artist = artist,
                    category = category,
                    streamUrl = url
                )
                startPlaySong(song)
            }
            ACTION_RESUME -> resumeMusic()
            ACTION_PAUSE -> pauseMusic()
            ACTION_NEXT -> playNextSong()
            ACTION_PREVIOUS -> playPreviousSong()
            ACTION_SEEK -> {
                val pos = intent.getLongExtra(EXTRA_SEEK_POS, 0L)
                seekMusic(pos)
            }
            ACTION_STOP -> stopMusic()
        }
        return START_NOT_STICKY
    }

    private fun startPlaySong(song: Song) {
        // Stop any running ad
        stopAdPlayback()

        PlayerManager.updateState {
            it.copy(
                currentSong = song,
                isBuffering = true,
                isPlaying = false,
                currentPositionMs = 0L,
                playbackError = null
            )
        }

        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer().apply {
                    setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setOnPreparedListener(this@MusicPlaybackService)
                    setOnCompletionListener(this@MusicPlaybackService)
                    setOnErrorListener(this@MusicPlaybackService)
                }
            } else {
                mediaPlayer?.reset()
            }

            mediaPlayer?.setDataSource(song.streamUrl)
            mediaPlayer?.prepareAsync()

            startForeground(NOTIFICATION_ID, buildNotification(song, isPlaying = true, isBuffering = true))
        } catch (e: Exception) {
            Log.e(TAG, "Error starting playback for song ${song.title}: ${e.message}", e)
            PlayerManager.updateState {
                it.copy(isBuffering = false, isPlaying = false, playbackError = "Unable to stream track")
            }
        }
    }

    override fun onPrepared(mp: MediaPlayer?) {
        mp?.start()
        val duration = mp?.duration?.toLong() ?: 0L
        PlayerManager.updateState {
            it.copy(
                isPlaying = true,
                isBuffering = false,
                durationMs = duration
            )
        }
        updateSystemPlaybackState(SystemPlaybackState.STATE_PLAYING, mp?.currentPosition?.toLong() ?: 0L)
        startProgressTracker()

        PlayerManager.state.value.currentSong?.let { song ->
            startForeground(NOTIFICATION_ID, buildNotification(song, isPlaying = true, isBuffering = false))
        }
    }

    private fun resumeMusic() {
        if (PlayerManager.state.value.isAdPlaying) return
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                PlayerManager.updateState { s -> s.copy(isPlaying = true) }
                updateSystemPlaybackState(SystemPlaybackState.STATE_PLAYING, it.currentPosition.toLong())
                startProgressTracker()
                PlayerManager.state.value.currentSong?.let { song ->
                    startForeground(NOTIFICATION_ID, buildNotification(song, isPlaying = true, isBuffering = false))
                }
            }
        }
    }

    private fun pauseMusic() {
        if (PlayerManager.state.value.isAdPlaying) return
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                PlayerManager.updateState { s -> s.copy(isPlaying = false) }
                updateSystemPlaybackState(SystemPlaybackState.STATE_PAUSED, it.currentPosition.toLong())
                progressJob?.cancel()
                PlayerManager.state.value.currentSong?.let { song ->
                    startForeground(NOTIFICATION_ID, buildNotification(song, isPlaying = false, isBuffering = false))
                }
            }
        }
    }

    private fun seekMusic(pos: Long) {
        if (PlayerManager.state.value.isAdPlaying) return
        try {
            mediaPlayer?.seekTo(pos.toInt())
            PlayerManager.updateState { it.copy(currentPositionMs = pos) }
            updateSystemPlaybackState(
                if (mediaPlayer?.isPlaying == true) SystemPlaybackState.STATE_PLAYING else SystemPlaybackState.STATE_PAUSED,
                pos
            )
        } catch (e: Exception) {
            Log.e(TAG, "Seek error: ${e.message}")
        }
    }

    private fun playNextSong() {
        val state = PlayerManager.state.value
        val queue = state.queue
        if (queue.isEmpty()) return

        val nextIndex = if (state.isShuffleEnabled && queue.size > 1) {
            var candidate: Int
            do {
                candidate = random.nextInt(queue.size)
            } while (candidate == state.currentIndex)
            candidate
        } else {
            (state.currentIndex + 1) % queue.size
        }

        val nextSong = queue[nextIndex]
        PlayerManager.updateState {
            it.copy(currentIndex = nextIndex)
        }
        startPlaySong(nextSong)
    }

    private fun playPreviousSong() {
        val state = PlayerManager.state.value
        val queue = state.queue
        if (queue.isEmpty()) return

        // If played more than 3 seconds, replay current song
        if ((mediaPlayer?.currentPosition ?: 0) > 3000) {
            seekMusic(0L)
            return
        }

        val prevIndex = if (state.currentIndex - 1 < 0) queue.size - 1 else state.currentIndex - 1
        val prevSong = queue[prevIndex]
        PlayerManager.updateState {
            it.copy(currentIndex = prevIndex)
        }
        startPlaySong(prevSong)
    }

    override fun onCompletion(mp: MediaPlayer?) {
        val state = PlayerManager.state.value
        if (state.repeatMode == RepeatMode.ONE) {
            seekMusic(0L)
            mediaPlayer?.start()
            return
        }

        // Increment played songs counter
        val updatedCount = state.songsPlayedSinceLastAd + 1

        // Check if 4 songs played -> Trigger 30-sec ad
        if (updatedCount >= 4) {
            PlayerManager.updateState {
                it.copy(songsPlayedSinceLastAd = 0)
            }
            triggerAudioAd()
        } else {
            PlayerManager.updateState {
                it.copy(songsPlayedSinceLastAd = updatedCount)
            }
            playNextSong()
        }
    }

    private fun triggerAudioAd() {
        stopProgressTracker()
        mediaPlayer?.pause()

        serviceScope.launch(Dispatchers.IO) {
            try {
                val fresh = driveRepo.getAds(forceRefresh = true)
                if (fresh.isNotEmpty()) cachedAds = fresh
            } catch (e: Exception) {
                // ignore
            }
        }

        val availableAds = if (cachedAds.isNotEmpty()) cachedAds else DriveFolderRepository.SEED_ADS
        val adToPlay = availableAds[random.nextInt(availableAds.size)]

        PlayerManager.updateState {
            it.copy(
                isAdPlaying = true,
                adCountdownSeconds = 30,
                currentAdTitle = adToPlay.title,
                isPlaying = true
            )
        }

        // Show ad notification
        val adNotification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_sanggeet_logo)
            .setContentTitle("📢 Sponsored Audio Ad (30s)")
            .setContentText(adToPlay.title)
            .setOngoing(true)
            .setContentIntent(createActivityPendingIntent())
            .build()
        startForeground(NOTIFICATION_ID, adNotification)

        try {
            adPlayer?.release()
            adPlayer = MediaPlayer().apply {
                setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(adToPlay.streamUrl)
                prepareAsync()
                setOnPreparedListener { it.start() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ad playback error: ${e.message}")
        }

        // Start 30-second countdown
        adCountdownJob?.cancel()
        adCountdownJob = serviceScope.launch {
            for (sec in 30 downTo 1) {
                PlayerManager.updateState { it.copy(adCountdownSeconds = sec) }
                delay(1000)
            }
            // Ad finished! Resume music
            stopAdPlayback()
            PlayerManager.updateState {
                it.copy(
                    isAdPlaying = false,
                    adCountdownSeconds = 0
                )
            }
            playNextSong()
        }
    }

    private fun stopAdPlayback() {
        adCountdownJob?.cancel()
        adCountdownJob = null
        try {
            adPlayer?.stop()
            adPlayer?.release()
        } catch (e: Exception) {
            // ignore
        }
        adPlayer = null
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
        PlayerManager.updateState {
            it.copy(
                isPlaying = false,
                isBuffering = false,
                playbackError = "Error loading song. Auto-skipping to next..."
            )
        }
        serviceScope.launch {
            delay(1500)
            playNextSong()
        }
        return true
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            while (isActive) {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        val pos = it.currentPosition.toLong()
                        val dur = it.duration.toLong()
                        PlayerManager.updateState { s ->
                            s.copy(currentPositionMs = pos, durationMs = dur)
                        }
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun stopMusic() {
        stopAdPlayback()
        stopProgressTracker()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // ignore
        }
        mediaPlayer = null
        PlayerManager.updateState {
            it.copy(
                isPlaying = false,
                isBuffering = false,
                currentPositionMs = 0L
            )
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // MANDATORY REQUIREMENT: If someone clears the app, song will be stopped
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "App task removed / cleared from recent tasks. Stopping playback.")
        stopMusic()
    }

    private fun buildNotification(song: Song, isPlaying: Boolean, isBuffering: Boolean): Notification {
        val playPauseIcon = if (isPlaying) R.drawable.ic_notif_pause else R.drawable.ic_notif_play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        val prevPending = createActionPendingIntent(ACTION_PREVIOUS, 1)
        val playPausePending = createActionPendingIntent(if (isPlaying) ACTION_PAUSE else ACTION_RESUME, 2)
        val nextPending = createActionPendingIntent(ACTION_NEXT, 3)
        val stopPending = createActionPendingIntent(ACTION_STOP, 4)

        val statusText = if (isBuffering) "Buffering..." else "${song.artist} • ${song.category}"

        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_sanggeet_logo)
            .setContentTitle(song.title)
            .setContentText(statusText)
            .setSubText("Sanggeet")
            .setContentIntent(createActivityPendingIntent())
            .setOngoing(isPlaying)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_notif_prev),
                    "Previous",
                    prevPending
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(this, playPauseIcon),
                    playPauseTitle,
                    playPausePending
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_notif_next),
                    "Next",
                    nextPending
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_notif_stop),
                    "Stop",
                    stopPending
                ).build()
            )

        val mediaStyle = Notification.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)
        mediaSession?.sessionToken?.let { token ->
            mediaStyle.setMediaSession(token)
        }
        builder.style = mediaStyle

        return builder.build()
    }

    private fun createActionPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, MusicPlaybackService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createActivityPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun updateSystemPlaybackState(state: Int, position: Long) {
        val actions = SystemPlaybackState.ACTION_PLAY or
                SystemPlaybackState.ACTION_PAUSE or
                SystemPlaybackState.ACTION_SKIP_TO_NEXT or
                SystemPlaybackState.ACTION_SKIP_TO_PREVIOUS or
                SystemPlaybackState.ACTION_SEEK_TO or
                SystemPlaybackState.ACTION_STOP

        mediaSession?.setPlaybackState(
            SystemPlaybackState.Builder()
                .setActions(actions)
                .setState(state, position, 1.0f)
                .build()
        )
    }

    override fun onDestroy() {
        stopAdPlayback()
        stopProgressTracker()
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            // ignore
        }
        mediaPlayer = null
        try {
            mediaSession?.release()
        } catch (e: Exception) {
            // ignore
        }
        mediaSession = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
