package com.example.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Playlist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val songIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val emoji: String = "🎵",
    val isSystemPreset: Boolean = false
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("name", name)
        obj.put("desc", description)
        obj.put("emoji", emoji)
        val arr = JSONArray()
        songIds.forEach { arr.put(it) }
        obj.put("songs", arr)
        return obj.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): Playlist? {
            return try {
                val obj = JSONObject(jsonStr)
                val songList = mutableListOf<String>()
                val arr = obj.optJSONArray("songs")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        songList.add(arr.getString(i))
                    }
                }
                Playlist(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    name = obj.optString("name", "Shared Playlist"),
                    description = obj.optString("desc", ""),
                    emoji = obj.optString("emoji", "🎵"),
                    songIds = songList,
                    isSystemPreset = false
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
