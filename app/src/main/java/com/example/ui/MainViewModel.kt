package com.example.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DriveFolderRepository
import com.example.data.PlaylistRepository
import com.example.model.PlaybackState
import com.example.model.Playlist
import com.example.model.Song
import com.example.playback.PlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import com.example.data.AuthRepository
import com.example.model.User

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val driveRepo = DriveFolderRepository(application)
    val playlistRepo = PlaylistRepository(application)
    val authRepo = AuthRepository(application)

    val currentUser: StateFlow<User?> = authRepo.currentUser

    val playbackState: StateFlow<PlaybackState> = PlayerManager.state

    private val _songs = MutableStateFlow<List<Song>>(DriveFolderRepository.SEED_SONGS)
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Dialog & Sheet states
    var songForAddToPlaylist: Song? = null
    var playlistToShare: Playlist? = null

    val playlists: StateFlow<List<Playlist>> = playlistRepo.playlistsFlow
    val favoriteSongIds: StateFlow<Set<String>> = playlistRepo.favoriteSongIds

    val filteredSongs: StateFlow<List<Song>> = combine(
        _songs,
        _selectedCategory,
        _searchQuery,
        favoriteSongIds
    ) { allSongs, category, query, favIds ->
        allSongs.map { song ->
            song.copy(isFavorite = favIds.contains(song.id))
        }.filter { song ->
            val matchesCategory = if (category == "All") {
                true
            } else if (category == "Favorites") {
                song.isFavorite
            } else {
                song.category.equals(category, ignoreCase = true)
            }

            val matchesQuery = query.isBlank() ||
                    song.title.contains(query, ignoreCase = true) ||
                    song.artist.contains(query, ignoreCase = true) ||
                    song.category.contains(query, ignoreCase = true)

            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DriveFolderRepository.SEED_SONGS)

    init {
        refreshSongs(force = true)

        // Automatic continuous live sync: re-fetches songs and ads every 12 seconds
        // so any newly uploaded audios in Google Drive appear automatically without manual interaction
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(12_000)
                try {
                    val liveSongs = driveRepo.getSongs(forceRefresh = true)
                    if (liveSongs.isNotEmpty() && liveSongs != _songs.value) {
                        _songs.value = liveSongs
                    }
                    driveRepo.getAds(forceRefresh = true)
                } catch (e: Exception) {
                    // ignore temporary connection blips
                }
            }
        }
    }

    fun refreshSongs(force: Boolean = true) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val fetched = driveRepo.getSongs(forceRefresh = force)
                if (fetched.isNotEmpty()) {
                    _songs.value = fetched
                }
                // Automatically fetch latest ads from Google Drive folder 1-x16HnYzOpzHIvefQj8txB21fC-YJkPf
                driveRepo.getAds(forceRefresh = force)
            } catch (e: Exception) {
                // keep current
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun playSong(song: Song, customQueue: List<Song>? = null) {
        val queue = customQueue ?: _songs.value
        PlayerManager.playSong(getApplication(), song, queue)
    }

    fun playAll(shuffle: Boolean = false) {
        val list = filteredSongs.value
        if (list.isNotEmpty()) {
            if (shuffle) {
                if (!playbackState.value.isShuffleEnabled) {
                    PlayerManager.toggleShuffle()
                }
                val randomStart = list.random()
                PlayerManager.playSong(getApplication(), randomStart, list)
            } else {
                PlayerManager.playSong(getApplication(), list.first(), list)
            }
        }
    }

    fun togglePlayPause() {
        PlayerManager.togglePlayPause(getApplication())
    }

    fun skipNext() {
        PlayerManager.skipNext(getApplication())
    }

    fun skipPrevious() {
        PlayerManager.skipPrevious(getApplication())
    }

    fun seekTo(posMs: Long) {
        PlayerManager.seekTo(getApplication(), posMs)
    }

    fun toggleShuffle() {
        PlayerManager.toggleShuffle()
    }

    fun cycleRepeatMode() {
        PlayerManager.cycleRepeatMode()
    }

    fun toggleFavorite(song: Song) {
        playlistRepo.toggleFavorite(song.id)
    }

    fun createPlaylist(name: String, description: String, emoji: String) {
        playlistRepo.createPlaylist(name, description, emoji)
    }

    fun deletePlaylist(playlistId: String) {
        playlistRepo.deletePlaylist(playlistId)
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        playlistRepo.addSongToPlaylist(playlistId, songId)
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        playlistRepo.removeSongFromPlaylist(playlistId, songId)
    }

    fun sharePlaylist(context: Context, playlist: Playlist) {
        val songNames = playlist.songIds.mapNotNull { id ->
            _songs.value.find { it.id == id }?.title
        }.joinToString("\n• ")

        val shareText = """
🎶 Check out my playlist "${playlist.name}" on Sanggeet! 
${if (playlist.description.isNotBlank()) "\"${playlist.description}\"\n" else ""}
Tracks (${playlist.songIds.size}):
• $songNames

📲 Import into Sanggeet App using Code:
[SANGGEET_PLAYLIST_START]
${playlist.toJson()}
[SANGGEET_PLAYLIST_END]

Listen to high-quality Indian music on Sanggeet - Har Dil Ki Apni Dhun!
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Sanggeet Playlist: ${playlist.name}")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val chooser = Intent.createChooser(intent, "Share Playlist with Friends")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun importPlaylist(codeOrJson: String): Boolean {
        var clean = codeOrJson.trim()
        if (clean.contains("[SANGGEET_PLAYLIST_START]") && clean.contains("[SANGGEET_PLAYLIST_END]")) {
            val start = clean.indexOf("[SANGGEET_PLAYLIST_START]") + "[SANGGEET_PLAYLIST_START]".length
            val end = clean.indexOf("[SANGGEET_PLAYLIST_END]")
            clean = clean.substring(start, end).trim()
        }
        val imported = playlistRepo.importSharedPlaylist(clean)
        return imported != null
    }

    fun signInWithGoogle(
        activity: Activity,
        onSuccess: (User) -> Unit,
        onError: (String) -> Unit,
        onCancelled: () -> Unit
    ) {
        authRepo.signInWithGoogle(activity, onSuccess, onError, onCancelled)
    }

    fun attemptSilentSignIn(onSuccess: (User) -> Unit = {}) {
        authRepo.attemptSilentSignIn(
            onSuccess = onSuccess,
            onUnauthenticated = { /* keep existing state */ }
        )
    }

    fun guestLogin(onSuccess: () -> Unit) {
        authRepo.quickGuestLogin()
        onSuccess()
    }

    fun logout(onComplete: () -> Unit = {}) {
        authRepo.signOut(onComplete)
    }
}
