package com.example.model

data class Song(
    val id: String,
    val title: String,
    val artist: String = "Sanggeet Originals",
    val category: String = "All",
    val durationMs: Long = 0L,
    val streamUrl: String = "https://drive.google.com/uc?export=download&id=$id",
    val fileSizeFormatted: String = "",
    val isFavorite: Boolean = false
) {
    companion object {
        fun cleanTitle(rawName: String): String {
            return rawName
                .replace(".mp3", "", ignoreCase = true)
                .replace(".wav", "", ignoreCase = true)
                .replace(".m4a", "", ignoreCase = true)
                .replace("&quot;", "")
                .replace("usesuno.com", "")
                .replace("-", " ")
                .replace("_", " ")
                .trim()
        }

        fun inferCategory(title: String): String {
            val lower = title.lowercase()
            return when {
                lower.contains("माँ") || lower.contains("hanuman") || lower.contains("chalisa") ||
                        lower.contains("कृष्ण") || lower.contains("महाकाल") || lower.contains("bhakti") ||
                        lower.contains("shakti") || lower.contains("shiv") -> "Bhakti Music"

                lower.contains("tum") || lower.contains("raho") || lower.contains("milne") ||
                        lower.contains("pyar") || lower.contains("love") || lower.contains("dil") ||
                        lower.contains("ishq") -> "Romantic Songs"

                lower.contains("galti") || lower.contains("aanchal") || lower.contains("yaad") ||
                        lower.contains("sad") || lower.contains("dard") -> "Sad Songs"

                lower.contains("वीर") || lower.contains("mitti") || lower.contains("desh") ||
                        lower.contains("bharat") || lower.contains("vande") -> "Patriotic Songs"

                lower.contains("तांडव") || lower.contains("चेतावनी") || lower.contains("power") ||
                        lower.contains("josh") -> "Motivational Songs"

                else -> "Folk Music"
            }
        }
    }
}

data class AdItem(
    val id: String,
    val title: String,
    val streamUrl: String,
    val durationSeconds: Int = 30
)
