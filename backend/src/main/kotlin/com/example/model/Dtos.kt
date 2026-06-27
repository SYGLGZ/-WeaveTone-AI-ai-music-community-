package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(val username: String, val password: String, val email: String? = null)

@Serializable
data class AuthResponse(val token: String, val userId: Int, val username: String)

@Serializable
data class TrackResponse(
    val id: Int,
    val userId: Int,
    val username: String,
    val title: String,
    val artist: String?,
    val genre: String?,
    val bpm: Int?,
    val durationSec: Int?,
    val fileUrl: String,
    val coverUrl: String?,
    val description: String?,
    val tags: List<String> = emptyList(),
    val playCount: Int,
    val likeCount: Int,
    val commentCount: Int,
    val isAiGenerated: Boolean,
    val aiPrompt: String?,
    val createdAt: Long,
    val isLiked: Boolean = false
)

@Serializable
data class TrackUploadRequest(
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

@Serializable
data class AiGenerationRequest(
    val prompt: String,
    val genre: String? = null,
    val bpm: Int? = null,
    val durationSec: Int = 30
)

@Serializable
data class AiGenerationResponse(
    val id: Int,
    val status: String,
    val prompt: String,
    val genre: String?,
    val bpm: Int?,
    val durationSec: Int,
    val provider: String,
    val progress: Int,
    val audioUrl: String? = null,
    val trackId: Int? = null,
    val error: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class AiPublishRequest(
    val title: String? = null,
    val description: String? = null,
    val tags: List<String>? = null
)

@Serializable
data class PlaylistRequest(val name: String, val isPublic: Boolean = true, val coverUrl: String? = null)

@Serializable
data class PlaylistResponse(
    val id: Int,
    val userId: Int,
    val username: String,
    val name: String,
    val coverUrl: String?,
    val isPublic: Boolean,
    val trackCount: Int,
    val createdAt: Long
)

@Serializable
data class CommentRequest(val content: String)

@Serializable
data class CommentResponse(val id: Int, val userId: Int, val username: String, val content: String, val createdAt: Long)

@Serializable
data class UserProfileResponse(
    val id: Int,
    val username: String,
    val avatarUrl: String?,
    val bio: String?,
    val trackCount: Int,
    val followerCount: Int,
    val followingCount: Int
)

@Serializable
data class PlayHistoryResponse(
    val id: Int,
    val userId: Int,
    val username: String,
    val trackId: Int,
    val title: String,
    val artist: String?,
    val playedAt: Long
)

@Serializable
data class ErrorResponse(val error: String)

@Serializable
data class SuccessResponse(val message: String)
