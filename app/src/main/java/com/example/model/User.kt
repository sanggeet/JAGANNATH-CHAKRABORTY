package com.example.model

import com.google.firebase.auth.FirebaseUser
import org.json.JSONObject
import java.util.UUID

data class User(
    val id: String,
    val name: String,
    val email: String,
    val photoUrl: String? = null,
    val avatarEmoji: String = "🎧",
    val joinedDate: String = "November 2026",
    val isGuest: Boolean = false
) {
    fun toJson(): String {
        val obj = JSONObject().apply {
            put("id", id)
            put("name", name)
            put("email", email)
            put("photoUrl", photoUrl ?: "")
            put("avatar", avatarEmoji)
            put("joined", joinedDate)
            put("is_guest", isGuest)
        }
        return obj.toString()
    }

    companion object {
        fun fromFirebaseUser(fbUser: FirebaseUser): User {
            val displayName = fbUser.displayName?.ifBlank { null }
                ?: fbUser.email?.substringBefore("@")?.replace(".", " ")?.capitalizeWords()
                ?: "Sanggeet Listener"
            return User(
                id = fbUser.uid,
                name = displayName,
                email = fbUser.email ?: "",
                photoUrl = fbUser.photoUrl?.toString(),
                avatarEmoji = "🎵",
                isGuest = false
            )
        }

        fun fromJson(jsonStr: String): User? {
            return try {
                val obj = JSONObject(jsonStr)
                User(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    email = obj.getString("email"),
                    photoUrl = obj.optString("photoUrl").ifBlank { null },
                    avatarEmoji = obj.optString("avatar", "🎧"),
                    joinedDate = obj.optString("joined", "November 2026"),
                    isGuest = obj.optBoolean("is_guest", false)
                )
            } catch (e: Exception) {
                null
            }
        }

        fun createGuest(name: String = "Guest Listener"): User {
            return User(
                id = "guest_${UUID.randomUUID().toString().take(8)}",
                name = name,
                email = "guest@sanggeet.app",
                avatarEmoji = "✨",
                isGuest = true
            )
        }

        private fun String.capitalizeWords(): String =
            split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
}
