package com.example.model

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object Users : Table("users") {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 50).uniqueIndex()
    val email = varchar("email", 255).uniqueIndex()
    val password = varchar("password", 255)
    val avatarUrl = varchar("avatar_url", 500).nullable()
    val bio = text("bio").nullable()
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object Tracks : Table("tracks") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id).index()
    val title = varchar("title", 200)
    val artist = varchar("artist", 200).nullable()
    val genre = varchar("genre", 50).nullable()
    val bpm = integer("bpm").nullable()
    val durationSec = integer("duration_sec").nullable()
    val fileUrl = varchar("file_url", 500)
    val coverUrl = varchar("cover_url", 500).nullable()
    val description = text("description").nullable()
    val tags = varchar("tags", 500).nullable()
    val playCount = integer("play_count").default(0).index()
    val likeCount = integer("like_count").default(0)
    val commentCount = integer("comment_count").default(0)
    val isAiGenerated = bool("is_ai_generated").default(false)
    val aiPrompt = text("ai_prompt").nullable()
    val createdAt = long("created_at").index()

    override val primaryKey = PrimaryKey(id)
}

object AiGenerationJobs : Table("ai_generation_jobs") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id).index()
    val prompt = text("prompt")
    val genre = varchar("genre", 50).nullable()
    val bpm = integer("bpm").nullable()
    val durationSec = integer("duration_sec")
    val provider = varchar("provider", 50)
    val providerTaskId = varchar("provider_task_id", 255).nullable().index()
    val status = varchar("status", 20).index()
    val progress = integer("progress").default(0)
    val outputUrl = varchar("output_url", 1000).nullable()
    val audioFilePath = varchar("audio_file_path", 500).nullable()
    val error = text("error").nullable()
    val trackId = integer("track_id").nullable()
    val createdAt = long("created_at").index()
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object Playlists : Table("playlists") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val name = varchar("name", 100)
    val coverUrl = varchar("cover_url", 500).nullable()
    val isPublic = bool("is_public").default(true)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object PlaylistTracks : Table("playlist_tracks") {
    val playlistId = integer("playlist_id").references(Playlists.id)
    val trackId = integer("track_id").references(Tracks.id)
    val position = integer("position").default(0)

    override val primaryKey = PrimaryKey(playlistId, trackId)
}

object Likes : Table("likes") {
    val userId = integer("user_id").references(Users.id).index()
    val trackId = integer("track_id").references(Tracks.id).index()
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(userId, trackId)
}

object Comments : Table("comments") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val trackId = integer("track_id").references(Tracks.id).index()
    val content = text("content")
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object PlayHistory : Table("play_history") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id).index()
    val trackId = integer("track_id").references(Tracks.id).index()
    val playedAt = long("played_at")

    override val primaryKey = PrimaryKey(id)
}

object Follows : Table("follows") {
    val followerId = integer("follower_id").references(Users.id).index()
    val followedId = integer("followed_id").references(Users.id).index()
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(followerId, followedId)
}
