package com.example.data

import android.content.Context
import com.example.model.Playlist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.util.UUID

class PlaylistRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("sanggeet_playlists", Context.MODE_PRIVATE)
    private val favPrefs = context.getSharedPreferences("sanggeet_favorites", Context.MODE_PRIVATE)

    private val _playlistsFlow = MutableStateFlow<List<Playlist>>(emptyList())
    val playlistsFlow: StateFlow<List<Playlist>> = _playlistsFlow.asStateFlow()

    private val _favoriteSongIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteSongIds: StateFlow<Set<String>> = _favoriteSongIds.asStateFlow()

    init {
        loadPlaylists()
        loadFavorites()
    }

    private fun loadPlaylists() {
        val raw = prefs.getString("playlists_data", null)
        val list = mutableListOf<Playlist>()
        if (raw != null) {
            try {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    Playlist.fromJson(arr.getString(i))?.let { list.add(it) }
                }
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }

        if (list.isEmpty()) {
            // Seed initial personalized playlists
            val defaultPlaylists = listOf(
                Playlist(
                    id = "preset_bhakti",
                    name = "Bhakti Dhun 🙏",
                    description = "Sacred devotional chants and peaceful morning prayers",
                    emoji = "🪔",
                    songIds = listOf(
                        "19Fpj3dE82MQrWYT2l8gmZa9flc9U3seR",
                        "1LjOWfSvEThVBMNiaNfVIv3oMGzHmJKHB",
                        "1aR0svFr943yWBts6AAA9zpSGE3Bm254C",
                        "1V7jG3J0vwKhnbdhbASAMwH4HI58reLbC"
                    ),
                    isSystemPreset = true
                ),
                Playlist(
                    id = "preset_romantic",
                    name = "Romantic Beats 💖",
                    description = "Soulful romantic tunes for calm evenings",
                    emoji = "💖",
                    songIds = listOf(
                        "11eNEK3DrhaiENwByAun904qAUqpsSEYH",
                        "12itB0khxlmDGLlw1UafDb_18_kDoCOLR"
                    ),
                    isSystemPreset = true
                ),
                Playlist(
                    id = "preset_patriotic",
                    name = "Desh Bhakti 🇮🇳",
                    description = "Songs honoring our brave soldiers and nation",
                    emoji = "🇮🇳",
                    songIds = listOf(
                        "1JEo89rreMWsYt1knhJmQQM-sCJUb9XQ6"
                    ),
                    isSystemPreset = true
                )
            )
            list.addAll(defaultPlaylists)
            savePlaylists(list)
        }
        _playlistsFlow.value = list
    }

    private fun savePlaylists(list: List<Playlist>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("playlists_data", arr.toString()).apply()
        _playlistsFlow.value = list
    }

    fun createPlaylist(name: String, description: String = "", emoji: String = "🎵"): Playlist {
        val newPlaylist = Playlist(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            description = description.trim(),
            emoji = emoji,
            songIds = emptyList()
        )
        val current = _playlistsFlow.value.toMutableList()
        current.add(0, newPlaylist)
        savePlaylists(current)
        return newPlaylist
    }

    fun deletePlaylist(playlistId: String) {
        val current = _playlistsFlow.value.filterNot { it.id == playlistId }
        savePlaylists(current)
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        val current = _playlistsFlow.value.map { pl ->
            if (pl.id == playlistId && !pl.songIds.contains(songId)) {
                pl.copy(songIds = pl.songIds + songId)
            } else {
                pl
            }
        }
        savePlaylists(current)
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        val current = _playlistsFlow.value.map { pl ->
            if (pl.id == playlistId) {
                pl.copy(songIds = pl.songIds.filterNot { it == songId })
            } else {
                pl
            }
        }
        savePlaylists(current)
    }

    fun importSharedPlaylist(rawJsonOrCode: String): Playlist? {
        val cleaned = rawJsonOrCode.trim()
        val parsed = Playlist.fromJson(cleaned) ?: return null
        val newId = UUID.randomUUID().toString()
        val imported = parsed.copy(
            id = newId,
            name = "${parsed.name} (Shared)",
            isSystemPreset = false
        )
        val current = _playlistsFlow.value.toMutableList()
        current.add(0, imported)
        savePlaylists(current)
        return imported
    }

    private fun loadFavorites() {
        val set = favPrefs.getStringSet("fav_songs", emptySet()) ?: emptySet()
        _favoriteSongIds.value = set
    }

    fun toggleFavorite(songId: String) {
        val current = _favoriteSongIds.value.toMutableSet()
        if (current.contains(songId)) {
            current.remove(songId)
        } else {
            current.add(songId)
        }
        favPrefs.edit().putStringSet("fav_songs", current).apply()
        _favoriteSongIds.value = current
    }

    fun isFavorite(songId: String): Boolean {
        return _favoriteSongIds.value.contains(songId)
    }
}
