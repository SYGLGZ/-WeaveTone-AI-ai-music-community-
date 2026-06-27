package com.example.myfirstapp.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Library : Screen("library")
    object Create : Screen("create")
    object Profile : Screen("profile")
    object Player : Screen("player/{songId}") {
        fun createRoute(songId: Long) = "player/$songId"
    }
    object AIGenerate : Screen("ai_generate?initialPrompt={initialPrompt}") {
        fun createRoute(initialPrompt: String? = null): String {
            return if (initialPrompt.isNullOrBlank()) "ai_generate"
            else "ai_generate?initialPrompt=${Uri.encode(initialPrompt)}"
        }
    }
    object Playlists : Screen("playlists")
    object PlaylistDetail : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }
    object AddSongsToPlaylist : Screen("playlist/{playlistId}/add_songs") {
        fun createRoute(playlistId: Long) = "playlist/$playlistId/add_songs"
    }
    object Settings : Screen("settings")
    object Login : Screen("login")
    object Search : Screen("search")
    object WorkManagement : Screen("work_management")
    object UploadMusic : Screen("upload_music")
    object Favorites : Screen("favorites")
    object TrackDiscussion : Screen("track_discussion/{trackId}") {
        fun createRoute(trackId: Int) = "track_discussion/$trackId"
    }
}
