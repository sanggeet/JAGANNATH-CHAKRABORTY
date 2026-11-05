package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.PlaybackState
import com.example.model.Song
import com.example.model.User
import com.example.ui.theme.BhaktiColor
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.FolkColor
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MotivationalColor
import com.example.ui.theme.PatrioticColor
import com.example.ui.theme.RomanticColor
import com.example.ui.theme.RubyHeart
import com.example.ui.theme.SadSongColor
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun HomeScreen(
    songs: List<Song>,
    selectedCategory: String,
    searchQuery: String,
    isLoading: Boolean,
    playbackState: PlaybackState,
    onCategorySelect: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onSongClick: (Song) -> Unit,
    onPlayAll: (shuffle: Boolean) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    currentUser: User? = null,
    onOpenLogin: () -> Unit = {},
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val categories = listOf(
        "All" to "🎵",
        "Bhakti Music" to "🪔",
        "Romantic Songs" to "💖",
        "Sad Songs" to "💧",
        "Patriotic Songs" to "🇮🇳",
        "Motivational Songs" to "🏆",
        "Folk Music" to "🥁",
        "Favorites" to "⭐"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_songs_list"),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 80.dp,
            start = 16.dp,
            end = 16.dp
        )
    ) {
        // Hero Header matching the Sanggeet Logo
        item {
            HeroHeader(
                playbackState = playbackState,
                isLoading = isLoading,
                currentUser = currentUser,
                onRefresh = onRefresh,
                onOpenLogin = onOpenLogin
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search songs, artists, devotionals...", color = TextMuted) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = GoldLight)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedContainerColor = DarkSurfaceElevated,
                    unfocusedContainerColor = DarkSurfaceElevated
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_text_field")
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Category Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { (category, icon) ->
                    val isSelected = category.equals(selectedCategory, ignoreCase = true)
                    val catColor = getCategoryColor(category)
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .border(
                                1.dp,
                                if (isSelected) catColor else DarkBorder,
                                RoundedCornerShape(50)
                            )
                            .clickable { onCategorySelect(category) }
                            .testTag("category_chip_$category"),
                        color = if (isSelected) catColor.copy(alpha = 0.2f) else DarkSurfaceElevated
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = icon, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = category,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) catColor else TextWhite
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Quick Actions Row: Play All, Shuffle All, Song Count
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${songs.size} Songs Available",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextMuted
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onPlayAll(false) },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("play_all_btn")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play All", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { onPlayAll(true) },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldLight),
                        modifier = Modifier.testTag("shuffle_all_btn")
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null, tint = GoldLight, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Shuffle", color = TextWhite, fontSize = 13.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Songs List
        if (songs.isEmpty() && !isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🔍", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No songs found",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try switching category or clear search query",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(songs, key = { it.id }) { song ->
                val isCurrentlyPlaying = playbackState.currentSong?.id == song.id
                SongItemCard(
                    song = song,
                    isPlaying = isCurrentlyPlaying && playbackState.isPlaying,
                    isCurrentSong = isCurrentlyPlaying,
                    onClick = { onSongClick(song) },
                    onToggleFavorite = { onToggleFavorite(song) },
                    onAddToPlaylist = { onAddToPlaylist(song) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun HeroHeader(
    playbackState: PlaybackState,
    isLoading: Boolean,
    currentUser: User? = null,
    onRefresh: () -> Unit,
    onOpenLogin: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(GoldPrimary.copy(alpha = 0.5f), Color.Transparent)),
                RoundedCornerShape(20.dp)
            ),
        color = DarkSurfaceElevated
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_sanggeet_logo),
                contentDescription = "Sanggeet App Icon",
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, GoldPrimary, CircleShape)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SANGGEET",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = GoldPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "HD",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(GoldLight)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }

                Text(
                    text = "Har Dil Ki Apni Dhun",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    ),
                    color = TextWhite
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "30s Ad: In ${playbackState.songsUntilNextAd} ${if (playbackState.songsUntilNextAd == 1) "song" else "songs"} (Drive Ads Folder)",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldLight
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.testTag("refresh_drive_songs_btn")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = GoldPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync from Drive",
                            tint = GoldPrimary
                        )
                    }
                }

                IconButton(
                    onClick = onOpenLogin,
                    modifier = Modifier.testTag("home_user_profile_btn")
                ) {
                    if (currentUser != null) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceHighlight)
                                .border(1.dp, GoldPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = currentUser.avatarEmoji, fontSize = 16.sp)
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Sign In",
                            tint = GoldPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SongItemCard(
    song: Song,
    isPlaying: Boolean,
    isCurrentSong: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    val catColor = getCategoryColor(song.category)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                1.dp,
                if (isCurrentSong) GoldPrimary else DarkBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .testTag("song_item_${song.id}"),
        color = if (isCurrentSong) GoldContainer.copy(alpha = 0.4f) else DarkSurfaceElevated
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon / Visualizer indicator
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(catColor.copy(alpha = 0.2f))
                    .border(1.dp, catColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    SmallPlayingVisualizer()
                } else {
                    Text(
                        text = getCategoryEmoji(song.category),
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Artist
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isCurrentSong) GoldPrimary else TextWhite
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.category,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = catColor
                    )
                    Text(
                        text = " • ${song.artist}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = TextMuted
                    )
                }
            }

            // Favorite button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.testTag("fav_song_${song.id}")
            ) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (song.isFavorite) RubyHeart else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Add to playlist button
            IconButton(
                onClick = onAddToPlaylist,
                modifier = Modifier.testTag("add_playlist_song_${song.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SmallPlayingVisualizer() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { i ->
            val infiniteTransition = rememberInfiniteTransition(label = "viz_$i")
            val heightScale by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(350 + (i * 120), easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "h"
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(18.dp * heightScale)
                    .clip(RoundedCornerShape(1.dp))
                    .background(GoldPrimary)
            )
        }
    }
}

private fun getCategoryColor(category: String): Color {
    val lower = category.lowercase()
    return when {
        lower.contains("bhakti") -> BhaktiColor
        lower.contains("romantic") -> RomanticColor
        lower.contains("sad") -> SadSongColor
        lower.contains("patriotic") -> PatrioticColor
        lower.contains("motivational") -> MotivationalColor
        lower.contains("favorite") -> RubyHeart
        else -> FolkColor
    }
}

private fun getCategoryEmoji(category: String): String {
    val lower = category.lowercase()
    return when {
        lower.contains("bhakti") -> "🪔"
        lower.contains("romantic") -> "💖"
        lower.contains("sad") -> "💧"
        lower.contains("patriotic") -> "🇮🇳"
        lower.contains("motivational") -> "🏆"
        lower.contains("favorite") -> "⭐"
        else -> "🥁"
    }
}
