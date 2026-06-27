package com.example.myfirstapp.data.remote.dto

data class AuthRequest(
    val username: String,
    val password: String,
    val email: String? = null
)

data class AuthResponse(
    val token: String,
    val userId: Int,
    val username: String
)

data class TrackDto(
    val id: Int,
    val userId: Int,
    val username: String = "",
    val title: String,
    val artist: String?,
    val genre: String?,
    val bpm: Int?,
    val durationSec: Int?,
    val fileUrl: String,
    val coverUrl: String?,
    val description: String?,
    val tags: List<String> = emptyList(),
    val playCount: Int = 0,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isAiGenerated: Boolean = false,
    val aiPrompt: String?,
    val createdAt: Long,
    val isLiked: Boolean = false
)

data class TrackUploadDto(
    val title: String,
    val artist: String? = null,
    val genre: String? = null,
    val bpm: Int? = null,
    val durationSec: Int? = null,
    val description: String? = null,
    val tags: List<String>? = null,
    val isAiGenerated: Boolean = false,
    val aiPrompt: String? = null
)

data class PlaylistDto(
    val id: Int,
    val userId: Int,
    val username: String = "",
    val name: String,
    val coverUrl: String?,
    val isPublic: Boolean = true,
    val trackCount: Int = 0,
    val createdAt: Long
)

data class CommentDto(
    val id: Int,
    val userId: Int,
    val username: String,
    val content: String,
    val createdAt: Long
)

data class SearchResultDto(
    val tracks: List<TrackDto> = emptyList(),
    val users: List<UserProfileDto> = emptyList()
)

data class UserProfileDto(
    val id: Int,
    val username: String,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val trackCount: Int = 0,
    val followerCount: Int = 0,
    val followingCount: Int = 0
)

data class ErrorResponse(
    val error: String
)
