package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.AdItem
import com.example.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

class DriveFolderRepository(private val context: Context) {

    companion object {
        private const val TAG = "DriveFolderRepository"

        // STRICT INSTRUCTION: NEVER CONFUSE THE 2 LINKS
        const val SONGS_FOLDER_ID = "1qPOpSIBu-1JKl25WjhUccZDJZ6jELa0n"
        const val SONGS_FOLDER_URL = "https://drive.google.com/drive/folders/$SONGS_FOLDER_ID"

        const val ADS_FOLDER_ID = "1-x16HnYzOpzHIvefQj8txB21fC-YJkPf"
        const val ADS_FOLDER_URL = "https://drive.google.com/drive/folders/1-x16HnYzOpzHIvefQj8txB21fC-YJkPf?usp=drive_link"

        var adsFolderStatus: String = "Auto-fetching ads from Drive..."
            private set

        private const val PREFS_NAME = "sanggeet_drive_cache"
        private const val KEY_CACHED_SONGS = "cached_songs_json"
        private const val KEY_CACHED_ADS = "cached_ads_json"

        // Initial verified ads from user's live Google Drive Ads folder
        val SEED_ADS = listOf(
            AdItem(
                id = "1qwKlcIhCAdfHQ7KzKXnTjB7CjJ48K3LO",
                title = "Sanggeet Audio Ad - Nov 04 (1:56PM)",
                streamUrl = "https://drive.google.com/uc?export=download&id=1qwKlcIhCAdfHQ7KzKXnTjB7CjJ48K3LO",
                durationSeconds = 30
            ),
            AdItem(
                id = "1dYe2JzU-oJzrVeALIK26eYVDhyPGJOif",
                title = "Sanggeet Audio Ad - Nov 04 (3:16PM)",
                streamUrl = "https://drive.google.com/uc?export=download&id=1dYe2JzU-oJzrVeALIK26eYVDhyPGJOif",
                durationSeconds = 30
            )
        )

        // Initial verified songs from live Google Drive folder
        val SEED_SONGS = listOf(
            Song(
                id = "19Fpj3dE82MQrWYT2l8gmZa9flc9U3seR",
                title = "माँ तेरी शक्ति अपार",
                artist = "Sanggeet Devotional",
                category = "Bhakti Music",
                fileSizeFormatted = "4.2 MB",
                streamUrl = "https://drive.google.com/uc?export=download&id=19Fpj3dE82MQrWYT2l8gmZa9flc9U3seR"
            ),
            Song(
                id = "11eNEK3DrhaiENwByAun904qAUqpsSEYH",
                title = "Bas Tum Raho",
                artist = "Sanggeet Romance",
                category = "Romantic Songs",
                fileSizeFormatted = "3.8 MB",
                streamUrl = "https://drive.google.com/uc?export=download&id=11eNEK3DrhaiENwByAun904qAUqpsSEYH"
            ),
            Song(
                id = "1XxXOF3Z8aIFLII2WTTqAiygG6i_zLQtC",
                title = "Galti Meri Thi (Female)",
                artist = "Sanggeet Melodies",
                category = "Sad Songs",
                fileSizeFormatted = "4.1 MB",
                streamUrl = "https://drive.google.com/uc?export=download&id=1XxXOF3Z8aIFLII2WTTqAiygG6i_zLQtC"
            ),
            Song(
                id = "15Zqvdvqt0IWxIlYnV0PzBGEjuwBlDoZp",
                title = "Galti Meri Thi",
                artist = "Sanggeet Melodies",
                category = "Sad Songs",
                fileSizeFormatted = "3.9 MB",
                streamUrl = "https://drive.google.com/uc?export=download&id=15Zqvdvqt0IWxIlYnV0PzBGEjuwBlDoZp"
            ),
            Song(
                id = "1LjOWfSvEThVBMNiaNfVIv3oMGzHmJKHB",
                title = "Hanuman Chalisa",
                artist = "Sanggeet Bhakti",
                category = "Bhakti Music",
                fileSizeFormatted = "3.5 MB",
                streamUrl = "https://drive.google.com/uc?export=download&id=1LjOWfSvEThVBMNiaNfVIv3oMGzHmJKHB"
            ),
            Song(
                id = "1aEyLMwXzJb_HQiteSMwf2WMeXfu2ijdm",
                title = "Maa Ke Aanchal Yaad Aata Hai",
                artist = "Sanggeet Folk",
                category = "Sad Songs",
                fileSizeFormatted = "4.2 MB",
                streamUrl = "https://drive.google.com/uc?export=download&id=1aEyLMwXzJb_HQiteSMwf2WMeXfu2ijdm"
            ),
            Song(
                id = "1hn35Q0mbuVTDZaagnbdEhBbV1pcVdwlJ",
                title = "Tere Ishq Mein Kho Jaaun",
                artist = "Sanggeet Romance",
                category = "Romantic Songs",
                fileSizeFormatted = "4.0 MB",
                streamUrl = "https://drive.google.com/uc?export=download&id=1hn35Q0mbuVTDZaagnbdEhBbV1pcVdwlJ"
            ),
            Song(
                id = "12itB0khxlmDGLlw1UafDb_18_kDoCOLR",
                title = "Tumse Milne Ke Baad",
                artist = "Sanggeet Romance",
                category = "Romantic Songs",
                fileSizeFormatted = "4.1 MB",
                streamUrl = "https://drive.google.com/uc?export=download&id=12itB0khxlmDGLlw1UafDb_18_kDoCOLR"
            ),
            Song(
                id = "1aR0svFr943yWBts6AAA9zpSGE3Bm254C",
                title = "कृष्ण की चेतावनी – अधर्म का अंत",
                artist = "Sanggeet Katha",
                category = "Motivational Songs",
                fileSizeFormatted = "4.1 MB",
                streamUrl = "https://drive.google.com/uc?export=download&id=1aR0svFr943yWBts6AAA9zpSGE3Bm254C"
            ),
            Song(
                id = "1V7jG3J0vwKhnbdhbASAMwH4HI58reLbC",
                title = "महाकाल का तांडव Sanggeet",
                artist = "Sanggeet Shiva",
                category = "Bhakti Music",
                fileSizeFormatted = "6.4 MB",
                streamUrl = "https://drive.google.com/uc?export=download&id=1V7jG3J0vwKhnbdhbASAMwH4HI58reLbC"
            ),
            Song(
                id = "1JEo89rreMWsYt1knhJmQQM-sCJUb9XQ6",
                title = "मिट्टी के वीर",
                artist = "Sanggeet Patriotic",
                category = "Patriotic Songs",
                fileSizeFormatted = "4.0 MB",
                streamUrl = "https://drive.google.com/uc?export=download&id=1JEo89rreMWsYt1knhJmQQM-sCJUb9XQ6"
            )
        )
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun getSongs(forceRefresh: Boolean = true): List<Song> = withContext(Dispatchers.IO) {
        try {
            val fetched = fetchSongsFromGoogleDrive(SONGS_FOLDER_ID)
            if (fetched.isNotEmpty()) {
                saveCachedSongs(fetched)
                return@withContext fetched
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching live songs from Drive: ${e.message}", e)
        }

        val cached = getCachedSongs()
        if (cached.isNotEmpty()) cached else SEED_SONGS
    }

    suspend fun getAds(forceRefresh: Boolean = true): List<AdItem> = withContext(Dispatchers.IO) {
        try {
            val fetched = fetchAdsFromGoogleDrive(ADS_FOLDER_ID)
            if (fetched.isNotEmpty()) {
                saveCachedAds(fetched)
                adsFolderStatus = "Connected: ${fetched.size} live ad(s) in Drive folder"
                return@withContext fetched
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching live ads from Drive: ${e.message}", e)
            adsFolderStatus = "Drive status: ${e.message ?: "Restricted"}"
        }

        val cached = getCachedAds()
        if (cached.isNotEmpty()) cached else SEED_ADS
    }

    private fun fetchSongsFromGoogleDrive(folderId: String): List<Song> {
        val url = URL("https://drive.google.com/drive/folders/$folderId")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        conn.instanceFollowRedirects = true

        val html = conn.inputStream.bufferedReader().use(BufferedReader::readText)
        val songs = mutableListOf<Song>()

        // Extractor 1: data-id with data-tooltip (Most reliable in Drive DOM)
        val pattern1 = Pattern.compile(
            "data-id=[\"']([a-zA-Z0-9_-]{20,})[\"'][^>]*data-tooltip=[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE
        )
        val matcher1 = pattern1.matcher(html)
        while (matcher1.find()) {
            val id = matcher1.group(1) ?: continue
            val rawName = matcher1.group(2) ?: continue
            val clean = Song.cleanTitle(rawName.replace("Audio", "", ignoreCase = true).trim())
            val category = Song.inferCategory(clean)
            songs.add(
                Song(
                    id = id,
                    title = clean,
                    artist = "Sanggeet Studio",
                    category = category,
                    streamUrl = "https://drive.google.com/uc?export=download&id=$id"
                )
            )
        }

        // Extractor 2: aria-label with filename and data-id
        if (songs.isEmpty()) {
            val pattern2 = Pattern.compile(
                "aria-label=[\"']([^\"']+\\.(?:mp3|wav|m4a|aac|ogg|opus|flac))[^\"']*[\"'][\\s\\S]*?data-id=[\"']([a-zA-Z0-9_-]{20,})[\"']",
                Pattern.CASE_INSENSITIVE
            )
            val matcher2 = pattern2.matcher(html)
            while (matcher2.find()) {
                val rawName = matcher2.group(1) ?: continue
                val id = matcher2.group(2) ?: continue
                val clean = Song.cleanTitle(rawName)
                val category = Song.inferCategory(clean)
                songs.add(
                    Song(
                        id = id,
                        title = clean,
                        artist = "Sanggeet Studio",
                        category = category,
                        streamUrl = "https://drive.google.com/uc?export=download&id=$id"
                    )
                )
            }
        }

        return songs.distinctBy { it.id }
    }

    private fun fetchAdsFromGoogleDrive(folderId: String): List<AdItem> {
        val url = URL(ADS_FOLDER_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        conn.instanceFollowRedirects = true

        val finalUrl = conn.url.toString()
        if (finalUrl.contains("signin") || finalUrl.contains("accounts.google.com")) {
            Log.w(TAG, "Ads folder redirected to Google Account sign-in. Folder requires 'Anyone with the link can view' permission.")
            adsFolderStatus = "Ads folder requires public link permission in Google Drive"
        }

        val html = conn.inputStream.bufferedReader().use(BufferedReader::readText)
        val ads = mutableListOf<AdItem>()

        // Extractor 1: data-id with data-tooltip
        val pattern1 = Pattern.compile(
            "data-id=[\"']([a-zA-Z0-9_-]{20,})[\"'][^>]*data-tooltip=[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE
        )
        val matcher1 = pattern1.matcher(html)
        while (matcher1.find()) {
            val id = matcher1.group(1) ?: continue
            val rawName = matcher1.group(2) ?: continue
            val clean = Song.cleanTitle(rawName.replace("Audio", "", ignoreCase = true).trim())
            ads.add(
                AdItem(
                    id = id,
                    title = if (clean.isNotBlank()) clean else "Sanggeet Sponsor Ad",
                    streamUrl = "https://drive.google.com/uc?export=download&id=$id",
                    durationSeconds = 30
                )
            )
        }

        // Extractor 2: aria-label with filename (.wav, .mp3, etc.) and data-id
        if (ads.isEmpty()) {
            val pattern2 = Pattern.compile(
                "aria-label=[\"']([^\"']+\\.(?:wav|mp3|m4a|aac|ogg|opus|flac))[^\"']*[\"'][\\s\\S]*?data-id=[\"']([a-zA-Z0-9_-]{20,})[\"']",
                Pattern.CASE_INSENSITIVE
            )
            val matcher2 = pattern2.matcher(html)
            while (matcher2.find()) {
                val rawName = matcher2.group(1) ?: continue
                val id = matcher2.group(2) ?: continue
                ads.add(
                    AdItem(
                        id = id,
                        title = Song.cleanTitle(rawName),
                        streamUrl = "https://drive.google.com/uc?export=download&id=$id",
                        durationSeconds = 30
                    )
                )
            }
        }

        return ads.distinctBy { it.id }
    }

    private fun getCachedSongs(): List<Song> {
        val json = prefs.getString(KEY_CACHED_SONGS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<Song>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    Song(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        artist = obj.optString("artist", "Sanggeet Studio"),
                        category = obj.optString("category", "All"),
                        fileSizeFormatted = obj.optString("size", ""),
                        streamUrl = obj.optString("url", "https://drive.google.com/uc?export=download&id=${obj.getString("id")}")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveCachedSongs(songs: List<Song>) {
        val arr = JSONArray()
        songs.forEach { song ->
            val obj = JSONObject()
            obj.put("id", song.id)
            obj.put("title", song.title)
            obj.put("artist", song.artist)
            obj.put("category", song.category)
            obj.put("size", song.fileSizeFormatted)
            obj.put("url", song.streamUrl)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_CACHED_SONGS, arr.toString()).apply()
    }

    private fun getCachedAds(): List<AdItem> {
        val json = prefs.getString(KEY_CACHED_ADS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<AdItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    AdItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        streamUrl = obj.getString("url"),
                        durationSeconds = obj.optInt("duration", 30)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveCachedAds(ads: List<AdItem>) {
        val arr = JSONArray()
        ads.forEach { ad ->
            val obj = JSONObject()
            obj.put("id", ad.id)
            obj.put("title", ad.title)
            obj.put("url", ad.streamUrl)
            obj.put("duration", ad.durationSeconds)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_CACHED_ADS, arr.toString()).apply()
    }
}
