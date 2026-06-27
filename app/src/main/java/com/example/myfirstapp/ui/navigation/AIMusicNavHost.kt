package com.example.myfirstapp.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myfirstapp.ui.components.MiniPlayerBar
import com.example.myfirstapp.ui.screen.*
import com.example.myfirstapp.ui.viewmodel.PlayerViewModel

@Composable
fun AIMusicNavHost(modifier: Modifier = Modifier, onPlaySong: (Long) -> Unit) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = BottomTab.Home.route,
        modifier = modifier
    ) {
        composable(BottomTab.Home.route) {
            MainScaffold(navController = navController, currentTab = BottomTab.Home) {
                HomeScreen(
                    onPlaySong = onPlaySong,
                    onNavigateToAI = { navController.navigate(Screen.AIGenerate.createRoute()) },
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                    onTrackDiscussion = { trackId -> navController.navigate(Screen.TrackDiscussion.createRoute(trackId)) }
                )
            }
        }

        composable(BottomTab.Library.route) {
            MainScaffold(navController = navController, currentTab = BottomTab.Library) {
                LibraryScreen(
                    onPlaySong = onPlaySong,
                    onNavigateToPlaylists = {
                        navController.navigate(Screen.Playlists.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToAI = {
                        navController.navigate(Screen.AIGenerate.createRoute())
                    },
                    onNavigateToWorks = { navController.navigate(Screen.WorkManagement.route) },
                    onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) }
                )
            }
        }

        composable(BottomTab.Create.route) {
            MainScaffold(navController = navController, currentTab = BottomTab.Create) {
                CreateScreen(
                    onNavigateToAI = {
                        navController.navigate(Screen.AIGenerate.createRoute())
                    },
                    onNavigateToAIWithPrompt = { prompt ->
                        navController.navigate(Screen.AIGenerate.createRoute(prompt))
                    },
                    onNavigateToWorks = { navController.navigate(Screen.WorkManagement.route) },
                    onUploadClick = { navController.navigate(Screen.UploadMusic.route) }
                )
            }
        }

        composable(BottomTab.Profile.route) {
            MainScaffold(navController = navController, currentTab = BottomTab.Profile) {
                ProfileScreen(
                    onNavigateToPlaylists = {
                        navController.navigate(Screen.Playlists.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route)
                    },
                    onNavigateToWorks = { navController.navigate(Screen.WorkManagement.route) },
                    onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                    onNavigateToLibrary = { navController.navigate(BottomTab.Library.route) }
                )
            }
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(navArgument("songId") { type = NavType.LongType })
        ) { backStackEntry ->
            PlayerScreen(
                songId = backStackEntry.arguments?.getLong("songId") ?: 0L,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AIGenerate.route,
            arguments = listOf(navArgument("initialPrompt") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            AIGenerateScreen(
                initialPrompt = backStackEntry.arguments?.getString("initialPrompt").orEmpty(),
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Playlists.route) {
            PlaylistScreen(
                onBack = { navController.popBackStack() },
                onPlaylistClick = { playlistId ->
                    navController.navigate(Screen.PlaylistDetail.createRoute(playlistId))
                }
            )
        }

        composable(
            route = Screen.PlaylistDetail.route,
            arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
            PlaylistDetailScreen(
                playlistId = playlistId,
                onBack = { navController.popBackStack() },
                onPlaySong = { songId ->
                    onPlaySong(songId)
                    navController.navigate(Screen.Player.createRoute(songId))
                },
                onAddSongs = { id ->
                    navController.navigate(Screen.AddSongsToPlaylist.createRoute(id))
                }
            )
        }

        composable(
            route = Screen.AddSongsToPlaylist.route,
            arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
        ) { backStackEntry ->
            AddSongsToPlaylistScreen(
                playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Login.route) {
            LoginScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onPlaySong = { songId ->
                    onPlaySong(songId)
                    navController.navigate(Screen.Player.createRoute(songId))
                }
            )
        }

        composable(Screen.WorkManagement.route) {
            WorkManagementScreen(
                onBack = { navController.popBackStack() },
                onUpload = { navController.navigate(Screen.UploadMusic.route) },
                onTrackClick = { trackId -> navController.navigate(Screen.TrackDiscussion.createRoute(trackId)) },
                onRestoreGeneration = { job ->
                    if (job.trackId != null) {
                        navController.navigate(Screen.TrackDiscussion.createRoute(job.trackId))
                    } else {
                        navController.navigate(Screen.AIGenerate.createRoute(job.prompt))
                    }
                }
            )
        }

        composable(Screen.UploadMusic.route) {
            UploadMusicScreen(
                onBack = { navController.popBackStack() },
                onUploaded = { trackId ->
                    navController.navigate(Screen.TrackDiscussion.createRoute(trackId)) {
                        popUpTo(Screen.WorkManagement.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onBack = { navController.popBackStack() },
                onTrackClick = { trackId -> navController.navigate(Screen.TrackDiscussion.createRoute(trackId)) }
            )
        }

        composable(
            route = Screen.TrackDiscussion.route,
            arguments = listOf(navArgument("trackId") { type = NavType.IntType })
        ) { backStackEntry ->
            TrackDiscussionScreen(
                trackId = backStackEntry.arguments?.getInt("trackId") ?: 0,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun MainScaffold(
    navController: androidx.navigation.NavController,
    currentTab: BottomTab,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val playerState by playerViewModel.playerState.collectAsState()
    val currentSong = playerState.currentSong

    Scaffold(
        bottomBar = {
            Column {
                if (currentSong != null) {
                    MiniPlayerBar(
                        currentSong = currentSong,
                        isPlaying = playerState.isPlaying,
                        onToggle = { playerViewModel.togglePlayPause() },
                        onOpenPlayer = {
                            navController.navigate(Screen.Player.createRoute(currentSong.id)) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                BottomNavBar(navController = navController)
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.padding(innerPadding)
        ) {
            content()
        }
    }
}
