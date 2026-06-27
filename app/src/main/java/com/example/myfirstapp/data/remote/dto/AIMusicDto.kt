package com.example.myfirstapp.data.remote.dto

data class AIGenerationRequest(
    val prompt: String,
    val genre: String? = null,
    val bpm: Int? = null,
    val durationSec: Int = 30
)

data class AIGenerationResponse(
    val id: Int,
    val status: String,
    val prompt: String,
    val genre: String? = null,
    val bpm: Int? = null,
    val durationSec: Int,
    val provider: String,
    val progress: Int = 0,
    val audioUrl: String? = null,
    val trackId: Int? = null,
    val error: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

data class AIPublishRequest(
    val title: String? = null,
    val description: String? = null,
    val tags: List<String>? = null
)

data class AIErrorResponse(
    val error: String,
    val code: Int,
    val message: String
)
