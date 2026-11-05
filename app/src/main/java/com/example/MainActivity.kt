package com.example

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Playlist
import com.example.model.Song
import com.example.model.User
import com.example.ui.MainViewModel
import com.example.ui.components.AdBannerOverlay
import com.example.ui.components.AddToPlaylistSheet
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.ImportPlaylistDialog
import com.example.ui.components.MiniPlayer
import com.example.ui.components.NowPlayingSheet
import com.example.ui.components.SharePlaylistDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SanggeetTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SanggeetTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

enum class NavigationTab {
    HOME, PLAYLISTS
}

@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Request notification permission for Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Notification permission is needed for playback controls", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        // Attempt silent Google sign-in for existing authorized accounts
        viewModel.attemptSilentSignIn()
    }

    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val songs by viewModel.filteredSongs.collectAsStateWithLifecycle()
    val allSongs by viewModel.songs.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(NavigationTab.HOME) }
    var selectedPlaylistForDetail by remember { mutableStateOf<Playlist?>(null) }
    var isNowPlayingExpanded by remember { mutableStateOf(false) }

    // Dialog & Sheet States
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showImportPlaylistDialog by remember { mutableStateOf(false) }
    var showLoginScreen by remember { mutableStateOf(false) }
    var songForAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var playlistToShare by remember { mutableStateOf<Playlist?>(null) }
    var isSigningInWithGoogle by remember { mutableStateOf(false) }
    var googleSignInError by remember { mutableStateOf<String?>(null) }
    val activity = context as? Activity

    // Show error snackbar if any
    LaunchedEffect(playbackState.playbackError) {
        playbackState.playbackError?.let { err ->
            snackbarHostState.showSnackbar(err)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Mini Player docked right above NavigationBar
                AnimatedVisibility(
                    visible = playbackState.currentSong != null && !isNowPlayingExpanded,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    MiniPlayer(
                        state = playbackState,
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onSkipNext = { viewModel.skipNext() },
                        onClick = { isNowPlayingExpanded = true }
                    )
                }

                // Bottom Navigation Bar
                NavigationBar(
                    containerColor = DarkSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkBorder)
                ) {
                    NavigationBarItem(
                        selected = currentTab == NavigationTab.HOME && selectedPlaylistForDetail == null,
                        onClick = {
                            selectedPlaylistForDetail = null
                            currentTab = NavigationTab.HOME
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = GoldPrimary,
                            indicatorColor = GoldPrimary,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("nav_tab_home")
                    )

                    NavigationBarItem(
                        selected = currentTab == NavigationTab.PLAYLISTS || selectedPlaylistForDetail != null,
                        onClick = {
                            selectedPlaylistForDetail = null
                            currentTab = NavigationTab.PLAYLISTS
                        },
                        icon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Playlists") },
                        label = { Text("Playlists") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = GoldPrimary,
                            indicatorColor = GoldPrimary,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("nav_tab_playlists")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (selectedPlaylistForDetail != null) {
                val currentPlaylist = playlists.find { it.id == selectedPlaylistForDetail?.id } ?: selectedPlaylistForDetail!!
                PlaylistDetailScreen(
                    playlist = currentPlaylist,
                    allSongs = allSongs,
                    playbackState = playbackState,
                    onBack = { selectedPlaylistForDetail = null },
                    onPlaySong = { song, queue -> viewModel.playSong(song, queue) },
                    onPlayAll = { queue, shuffle ->
                        if (queue.isNotEmpty()) {
                            if (shuffle) {
                                if (!playbackState.isShuffleEnabled) viewModel.toggleShuffle()
                                viewModel.playSong(queue.random(), queue)
                            } else {
                                viewModel.playSong(queue.first(), queue)
                            }
                        }
                    },
                    onSharePlaylist = { pl ->
                        viewModel.sharePlaylist(context, pl)
                    },
                    onRemoveSong = { songId ->
                        viewModel.removeSongFromPlaylist(currentPlaylist.id, songId)
                    },
                    onDeletePlaylist = { plId ->
                        viewModel.deletePlaylist(plId)
                        selectedPlaylistForDetail = null
                    },
                    contentPadding = innerPadding
                )
            } else {
                when (currentTab) {
                    NavigationTab.HOME -> {
                        HomeScreen(
                            songs = songs,
                            selectedCategory = selectedCategory,
                            searchQuery = searchQuery,
                            isLoading = isLoading,
                            playbackState = playbackState,
                            onCategorySelect = { viewModel.selectCategory(it) },
                            onSearchChange = { viewModel.setSearchQuery(it) },
                            onRefresh = { viewModel.refreshSongs(force = true) },
                            onSongClick = { viewModel.playSong(it) },
                            onPlayAll = { shuffle -> viewModel.playAll(shuffle) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onAddToPlaylist = { song ->
                                if (currentUser != null) {
                                    songForAddToPlaylist = song
                                } else {
                                    Toast.makeText(context, "Please sign in to add songs to playlists", Toast.LENGTH_SHORT).show()
                                    showLoginScreen = true
                                }
                            },
                            currentUser = currentUser,
                            onOpenLogin = { showLoginScreen = true },
                            contentPadding = innerPadding
                        )
                    }

                    NavigationTab.PLAYLISTS -> {
                        PlaylistsScreen(
                            playlists = playlists,
                            allSongs = allSongs,
                            currentUser = currentUser,
                            onSelectPlaylist = { selectedPlaylistForDetail = it },
                            onPlayPlaylist = { pl ->
                                val tracks = pl.songIds.mapNotNull { id -> allSongs.find { it.id == id } }
                                if (tracks.isNotEmpty()) {
                                    viewModel.playSong(tracks.first(), tracks)
                                } else {
                                    Toast.makeText(context, "Playlist is empty", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onSharePlaylist = { pl ->
                                viewModel.sharePlaylist(context, pl)
                            },
                            onDeletePlaylist = { viewModel.deletePlaylist(it) },
                            onCreatePlaylistClick = {
                                if (currentUser != null) {
                                    showCreatePlaylistDialog = true
                                } else {
                                    showLoginScreen = true
                                }
                            },
                            onImportPlaylistClick = {
                                if (currentUser != null) {
                                    showImportPlaylistDialog = true
                                } else {
                                    showLoginScreen = true
                                }
                            },
                            onOpenLogin = { showLoginScreen = true },
                            onLogout = {
                                viewModel.logout {
                                    Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
                                }
                            },
                            contentPadding = innerPadding
                        )
                    }
                }
            }

            // Expandable Now Playing Full Screen Modal
            AnimatedVisibility(
                visible = isNowPlayingExpanded && playbackState.currentSong != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                playbackState.currentSong?.let { song ->
                    NowPlayingSheet(
                        state = playbackState,
                        isFavorite = favoriteIds.contains(song.id),
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onSkipNext = { viewModel.skipNext() },
                        onSkipPrevious = { viewModel.skipPrevious() },
                        onSeekTo = { viewModel.seekTo(it) },
                        onToggleShuffle = { viewModel.toggleShuffle() },
                        onCycleRepeat = { viewModel.cycleRepeatMode() },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onAddToPlaylist = { song ->
                            if (currentUser != null) {
                                songForAddToPlaylist = song
                            } else {
                                Toast.makeText(context, "Please sign in to add songs to playlists", Toast.LENGTH_SHORT).show()
                                showLoginScreen = true
                            }
                        },
                        onDismiss = { isNowPlayingExpanded = false }
                    )
                }
            }

            // 30-Second Audio Ad Overlay (Triggers after every 4 songs)
            AdBannerOverlay(
                isAdPlaying = playbackState.isAdPlaying,
                adCountdownSeconds = playbackState.adCountdownSeconds,
                adTitle = playbackState.currentAdTitle
            )
        }
    }

    // Dialogs
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name, desc, emoji ->
                val newPl = viewModel.playlistRepo.createPlaylist(name, desc, emoji)
                showCreatePlaylistDialog = false
                selectedPlaylistForDetail = newPl
                Toast.makeText(context, "Playlist created!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showImportPlaylistDialog) {
        ImportPlaylistDialog(
            onDismiss = { showImportPlaylistDialog = false },
            onImport = { code ->
                val success = viewModel.importPlaylist(code)
                showImportPlaylistDialog = false
                if (success) {
                    Toast.makeText(context, "Playlist imported successfully! 🎉", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Invalid playlist code. Please verify and try again.", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    songForAddToPlaylist?.let { song ->
        AddToPlaylistSheet(
            song = song,
            playlists = playlists,
            onAddToPlaylist = { plId ->
                viewModel.addSongToPlaylist(plId, song.id)
                songForAddToPlaylist = null
                Toast.makeText(context, "Added to playlist!", Toast.LENGTH_SHORT).show()
            },
            onCreateNewPlaylist = {
                songForAddToPlaylist = null
                if (currentUser != null) {
                    showCreatePlaylistDialog = true
                } else {
                    showLoginScreen = true
                }
            },
            onDismiss = { songForAddToPlaylist = null }
        )
    }

    playlistToShare?.let { playlist ->
        SharePlaylistDialog(
            playlist = playlist,
            onShareExternal = {
                viewModel.sharePlaylist(context, playlist)
                playlistToShare = null
            },
            onDismiss = { playlistToShare = null }
        )
    }

    if (showLoginScreen) {
        BackHandler {
            if (!isSigningInWithGoogle) {
                showLoginScreen = false
            }
        }
        LoginScreen(
            onSignInWithGoogle = {
                if (activity != null) {
                    isSigningInWithGoogle = true
                    googleSignInError = null
                    viewModel.signInWithGoogle(
                        activity = activity,
                        onSuccess = { user ->
                            isSigningInWithGoogle = false
                            showLoginScreen = false
                            Toast.makeText(context, "Welcome, ${user.name}! 🎵", Toast.LENGTH_SHORT).show()
                        },
                        onError = { err ->
                            isSigningInWithGoogle = false
                            googleSignInError = err
                        },
                        onCancelled = {
                            isSigningInWithGoogle = false
                        }
                    )
                } else {
                    googleSignInError = "Activity context is required for Google Sign-In"
                }
            },
            onGuestLogin = {
                viewModel.guestLogin {
                    showLoginScreen = false
                    Toast.makeText(context, "Browsing as Guest", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = {
                if (!isSigningInWithGoogle) {
                    showLoginScreen = false
                }
            },
            isSigningIn = isSigningInWithGoogle,
            errorMessage = googleSignInError,
            onClearError = { googleSignInError = null }
        )
    }
}
