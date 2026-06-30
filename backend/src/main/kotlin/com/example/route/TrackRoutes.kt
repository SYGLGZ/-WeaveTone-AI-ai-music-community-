/**
 * Track Routes
 * POST   /api/v1/music/upload        — Upload a new track (JSON, auth required)
 * POST   /api/v1/music/upload/file   — Upload with file (multipart, auth required)
 * GET    /api/v1/music/discover      — Discover tracks (paginated)
 * GET    /api/v1/music/hot           — Get hot/popular tracks
 * GET    /api/v1/music/{id}          — Get track details
 * DELETE /api/v1/music/{id}          — Delete track (auth required)
 * GET    /api/v1/music/{id}/stream   — Stream audio file
 * POST   /api/v1/music/{id}/like     — Toggle like (auth required)
 * POST   /api/v1/music/{id}/view     — Increment play count
 * GET    /api/v1/music/{id}/comments — Get comments
 * POST   /api/v1/music/{id}/comment  — Post comment (auth required)
 * GET    /api/v1/user/{id}/history   — Get play history
 * GET    /api/v1/user/{id}/tracks    — Get user's tracks
 */
package com.example.route

import com.example.middleware.requireAuth
import com.example.middleware.optionalAuth
import com.example.model.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.io.File
import java.util.UUID

private val logger = LoggerFactory.getLogger("TrackRoutes")

fun Application.trackRoutes() {
    routing {
        post("/api/v1/music/upload") {
            val (uid, uname) = call.requireAuth() ?: return@post
            val req = call.receive<TrackUploadRequest>()
            if (req.title.length !in 1..200) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Title must be 1-200 characters"))
                return@post
            }
            if (req.bpm != null && req.bpm !in 1..300) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("BPM must be between 1 and 300"))
                return@post
            }
            if (req.durationSec != null && req.durationSec !in 1..7200) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Duration must be between 1 and 7200 seconds"))
                return@post
            }
            val trackId = transaction {
                Tracks.insert {
                    it[Tracks.userId] = uid
                    it[Tracks.title] = req.title
                    it[Tracks.artist] = req.artist ?: uname
                    it[Tracks.genre] = req.genre
                    it[Tracks.bpm] = req.bpm
                    it[Tracks.durationSec] = req.durationSec
                    it[Tracks.fileUrl] = "uploads/${req.title}"
                    it[Tracks.description] = req.description
                    it[Tracks.tags] = req.tags?.joinToString(",")
                    it[Tracks.isAiGenerated] = req.isAiGenerated
                    it[Tracks.aiPrompt] = req.aiPrompt
                    it[Tracks.createdAt] = System.currentTimeMillis()
                } get Tracks.id
            }
            logger.info("Track uploaded: id={}, title={}, userId={}", trackId, req.title, uid)
            call.respond(HttpStatusCode.Created, mapOf("id" to trackId))
        }

        post("/api/v1/music/upload/file") {
            val (uid, uname) = call.requireAuth() ?: return@post
            val multipart = call.receiveMultipart()
            var title = ""
            var artist: String? = null
            var genre: String? = null
            var bpm: Int? = null
            var durationSec: Int? = null
            var description: String? = null
            var tags: String? = null
            var isAiGenerated = false
            var aiPrompt: String? = null
            var filePath = ""
            var uploadError: UploadValidationException? = null
            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "title" -> title = part.value
                            "artist" -> artist = part.value
                            "genre" -> genre = part.value
                            "bpm" -> bpm = part.value.toIntOrNull()
                            "durationSec" -> durationSec = part.value.toIntOrNull()
                            "description" -> description = part.value
                            "tags" -> tags = part.value
                            "isAiGenerated" -> isAiGenerated = part.value.toBooleanStrictOrNull() ?: false
                            "aiPrompt" -> aiPrompt = part.value
                        }
                    }
                    is PartData.FileItem -> {
                        if (part.name == "file" && filePath.isBlank() && uploadError == null) {
                            try {
                                filePath = saveUploadedAudio(part)
                            } catch (e: UploadValidationException) {
                                uploadError = e
                            }
                        }
                    }
                    else -> {}
                }
                part.dispose()
            }
            uploadError?.let {
                call.respond(it.status, ErrorResponse(it.message ?: "Invalid audio upload"))
                return@post
            }
            title = title.trim()
            if (title.length !in 1..200) {
                File(filePath).delete()
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Title must be 1-200 characters"))
                return@post
            }
            if (filePath.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("File is required"))
                return@post
            }
            if (bpm != null && bpm !in 1..300) {
                File(filePath).delete()
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("BPM must be between 1 and 300"))
                return@post
            }
            if (description != null && description!!.length > 2_000) {
                File(filePath).delete()
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Description must be at most 2000 characters"))
                return@post
            }
            val finalArtist = artist ?: uname
            val trackId = transaction {
                Tracks.insert {
                    it[Tracks.userId] = uid
                    it[Tracks.title] = title
                    it[Tracks.artist] = finalArtist
                    it[Tracks.genre] = genre
                    it[Tracks.bpm] = bpm
                    it[Tracks.durationSec] = durationSec
                    it[Tracks.fileUrl] = filePath
                    it[Tracks.description] = description
                    it[Tracks.tags] = tags
                    it[Tracks.isAiGenerated] = isAiGenerated
                    it[Tracks.aiPrompt] = aiPrompt
                    it[Tracks.createdAt] = System.currentTimeMillis()
                } get Tracks.id
            }
            logger.info("Track file uploaded: id={}, title={}, userId={}, file={}", trackId, title, uid, filePath)
            call.respond(HttpStatusCode.Created, mapOf("id" to trackId))
        }

        get("/api/v1/music/discover") {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val limit = 20
            val offset = (page - 1) * limit
            val tracks = transaction {
                (Tracks innerJoin Users).selectAll().orderBy(Tracks.createdAt, SortOrder.DESC).limit(limit, offset.toLong())
                    .map { it.toTrackResponse(it[Users.username]) }
            }
            call.respond(tracks)
        }

        get("/api/v1/music/hot") {
            val tracks = transaction {
                (Tracks innerJoin Users).selectAll().orderBy(Tracks.playCount, SortOrder.DESC).limit(20)
                    .map { it.toTrackResponse(it[Users.username]) }
            }
            call.respond(tracks)
        }

        get("/api/v1/music/liked/mine") {
            val (uid, _) = call.requireAuth() ?: return@get
            val tracks = transaction {
                (Likes innerJoin Tracks innerJoin Users)
                    .select { Likes.userId eq uid }
                    .orderBy(Likes.createdAt, SortOrder.DESC)
                    .map { row ->
                        row.toTrackResponse(row[Users.username]).copy(isLiked = true)
                    }
            }
            call.respond(tracks)
        }

        get("/api/v1/music/{id}") {
            val tid = call.parameters["id"]?.toIntOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid id"))
                return@get
            }
            val track = transaction {
                (Tracks innerJoin Users).select { Tracks.id eq tid }.singleOrNull()?.let { it.toTrackResponse(it[Users.username]) }
            } ?: run {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Track not found"))
                return@get
            }
            call.respond(track)
        }

        delete("/api/v1/music/{id}") {
            val (uid, _) = call.requireAuth() ?: return@delete
            val tid = call.parameters["id"]?.toIntOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid id"))
                return@delete
            }
            val track = transaction { Tracks.select { Tracks.id eq tid }.singleOrNull() } ?: run {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Track not found"))
                return@delete
            }
            if (track[Tracks.userId] != uid) {
                call.respond(HttpStatusCode.Forbidden, ErrorResponse("You do not own this track"))
                return@delete
            }
            transaction {
                PlaylistTracks.deleteWhere { with(SqlExpressionBuilder) { PlaylistTracks.trackId eq tid } }
                Likes.deleteWhere { with(SqlExpressionBuilder) { Likes.trackId eq tid } }
                Comments.deleteWhere { with(SqlExpressionBuilder) { Comments.trackId eq tid } }
                PlayHistory.deleteWhere { with(SqlExpressionBuilder) { PlayHistory.trackId eq tid } }
                AiGenerationJobs.update({ AiGenerationJobs.trackId eq tid }) {
                    it[AiGenerationJobs.trackId] = null
                    it[AiGenerationJobs.status] = "SUCCEEDED"
                    it[AiGenerationJobs.updatedAt] = System.currentTimeMillis()
                }
                Tracks.deleteWhere { with(SqlExpressionBuilder) { Tracks.id eq tid } }
            }
            call.respond(SuccessResponse("Deleted"))
        }

        get("/api/v1/music/{id}/stream") {
            val tid = call.parameters["id"]?.toIntOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid id"))
                return@get
            }
            val fileUrl = transaction {
                Tracks.select { Tracks.id eq tid }.singleOrNull()?.get(Tracks.fileUrl)
            } ?: run {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Track not found"))
                return@get
            }
            val file = File(fileUrl)
            if (!file.exists()) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("File not found on disk"))
                return@get
            }
            val ext = file.extension.lowercase()
            val contentType = when (ext) {
                "mp3" -> ContentType.Audio.MPEG
                "wav" -> ContentType.parse("audio/wav")
                "ogg" -> ContentType.parse("audio/ogg")
                "flac" -> ContentType.parse("audio/flac")
                "aac" -> ContentType.parse("audio/aac")
                else -> ContentType.Application.OctetStream
            }
            call.response.header(HttpHeaders.ContentType, contentType.toString())
            call.respondFile(file)
        }

        post("/api/v1/music/{id}/like") {
            val (uid, _) = call.requireAuth() ?: return@post
            val tid = call.parameters["id"]?.toIntOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid id"))
                return@post
            }
            val liked = transaction {
                Tracks.select { Tracks.id eq tid }.forUpdate().singleOrNull()
                    ?: return@transaction null
                val existing = Likes.select {
                    (Likes.userId eq uid) and (Likes.trackId eq tid)
                }.count() > 0
                if (existing) {
                    Likes.deleteWhere {
                        with(SqlExpressionBuilder) {
                            (Likes.userId eq uid) and (Likes.trackId eq tid)
                        }
                    }
                } else {
                    Likes.insert {
                        it[Likes.userId] = uid
                        it[Likes.trackId] = tid
                        it[Likes.createdAt] = System.currentTimeMillis()
                    }
                }
                val actualCount = Likes.select { Likes.trackId eq tid }.count().toInt()
                Tracks.update({ Tracks.id eq tid }) { it[Tracks.likeCount] = actualCount }
                !existing
            }
            if (liked == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Track not found"))
                return@post
            } else if (liked) {
                logger.info("Like added: userId={}, trackId={}", uid, tid)
                call.respond(SuccessResponse("Liked"))
            } else {
                logger.info("Like removed: userId={}, trackId={}", uid, tid)
                call.respond(SuccessResponse("Unliked"))
            }
        }

        post("/api/v1/music/{id}/view") {
            val tid = call.parameters["id"]?.toIntOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid id"))
                return@post
            }
            val updated = transaction {
                Tracks.update({ Tracks.id eq tid }) {
                    with(SqlExpressionBuilder) {
                        it[Tracks.playCount] = Tracks.playCount + 1
                    }
                }
            }
            if (updated == 0) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Track not found"))
                return@post
            }
            val (userId, _) = call.optionalAuth()
            if (userId != null) {
                transaction {
                    PlayHistory.insert {
                        it[PlayHistory.userId] = userId
                        it[PlayHistory.trackId] = tid
                        it[PlayHistory.playedAt] = System.currentTimeMillis()
                    }
                }
            }
            call.respond(SuccessResponse("OK"))
        }

        get("/api/v1/music/{id}/comments") {
            val tid = call.parameters["id"]?.toIntOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid id"))
                return@get
            }
            val comments = transaction {
                (Comments innerJoin Users).select { Comments.trackId eq tid }
                    .orderBy(Comments.createdAt, SortOrder.DESC)
                    .map { row ->
                        CommentResponse(
                            id = row[Comments.id], userId = row[Comments.userId],
                            username = row[Users.username], content = row[Comments.content],
                            createdAt = row[Comments.createdAt]
                        )
                    }
            }
            call.respond(comments)
        }

        post("/api/v1/music/{id}/comment") {
            val (uid, _) = call.requireAuth() ?: return@post
            val tid = call.parameters["id"]?.toIntOrNull() ?: return@post
            val exists = transaction { Tracks.select { Tracks.id eq tid }.count() > 0 }
            if (!exists) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Track not found"))
                return@post
            }
            val req = call.receive<CommentRequest>()
            val content = req.content.trim()
            if (content.length !in 1..500) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Comment content must be 1-500 characters"))
                return@post
            }
            transaction {
                Comments.insert {
                    it[Comments.userId] = uid
                    it[Comments.trackId] = tid
                    it[Comments.content] = content
                    it[Comments.createdAt] = System.currentTimeMillis()
                }
                Tracks.update({ Tracks.id eq tid }) {
                    with(SqlExpressionBuilder) {
                        it[Tracks.commentCount] = Tracks.commentCount + 1
                    }
                }
            }
            logger.info("Comment added: userId={}, trackId={}", uid, tid)
            call.respond(HttpStatusCode.Created, SuccessResponse("Comment added"))
        }

        get("/api/v1/user/{id}/history") {
            val (authenticatedUserId, _) = call.requireAuth() ?: return@get
            val uid = call.parameters["id"]?.toIntOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid user id"))
                return@get
            }
            if (uid != authenticatedUserId) {
                call.respond(HttpStatusCode.Forbidden, ErrorResponse("Play history is private"))
                return@get
            }
            val history = transaction {
                (PlayHistory innerJoin Tracks innerJoin Users)
                    .select { PlayHistory.userId eq uid }
                    .orderBy(PlayHistory.playedAt, SortOrder.DESC)
                    .limit(30)
                    .map { row ->
                        PlayHistoryResponse(
                            id = row[PlayHistory.id],
                            userId = row[PlayHistory.userId],
                            username = row[Users.username],
                            trackId = row[PlayHistory.trackId],
                            title = row[Tracks.title],
                            artist = row[Tracks.artist],
                            playedAt = row[PlayHistory.playedAt]
                        )
                    }
            }
            call.respond(history)
        }

        get("/api/v1/user/{id}/tracks") {
            val uid = call.parameters["id"]?.toIntOrNull() ?: return@get
            val tracks = transaction {
                (Tracks innerJoin Users).select { Tracks.userId eq uid }
                    .orderBy(Tracks.createdAt, SortOrder.DESC)
                    .map { it.toTrackResponse(it[Users.username]) }
            }
            call.respond(tracks)
        }
    }
}

private const val MAX_AUDIO_UPLOAD_BYTES = 50L * 1024L * 1024L
private val allowedAudioExtensions = setOf("mp3", "wav", "ogg", "flac", "aac", "m4a")

private class UploadValidationException(
    override val message: String,
    val status: HttpStatusCode = HttpStatusCode.BadRequest
) : Exception(message)

@Suppress("DEPRECATION")
private fun saveUploadedAudio(part: PartData.FileItem): String {
    val originalName = part.originalFileName?.trim().orEmpty()
    val extension = originalName.substringAfterLast('.', "").lowercase()
    if (extension !in allowedAudioExtensions) {
        throw UploadValidationException("Supported audio formats: ${allowedAudioExtensions.joinToString()}")
    }
    val baseName = originalName.substringBeforeLast('.')
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
        .trim('_', '.')
        .take(80)
        .ifBlank { "audio" }
    val savedName = "${UUID.randomUUID()}_${baseName}.$extension"
    val uploadDir = File("uploads").apply { mkdirs() }
    val target = File(uploadDir, savedName)
    var copied = 0L
    try {
        part.streamProvider().use { input ->
            target.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    copied += count
                    if (copied > MAX_AUDIO_UPLOAD_BYTES) {
                        throw UploadValidationException(
                            "Audio file must be at most 50 MB",
                            HttpStatusCode.PayloadTooLarge
                        )
                    }
                    output.write(buffer, 0, count)
                }
            }
        }
    } catch (e: Exception) {
        target.delete()
        throw e
    }
    if (copied == 0L) {
        target.delete()
        throw UploadValidationException("Audio file is empty")
    }
    return target.invariantSeparatorsPath
}

fun ResultRow.toTrackResponse(username: String = ""): TrackResponse {
    val uname = if (username.isNotEmpty()) username else {
        val uid = this[Tracks.userId]
        Users.select { Users.id eq uid }.single()[Users.username]
    }
    return TrackResponse(
        id = this[Tracks.id], userId = this[Tracks.userId],
        username = uname, title = this[Tracks.title],
        artist = this[Tracks.artist] ?: "", genre = this[Tracks.genre],
        bpm = this[Tracks.bpm], durationSec = this[Tracks.durationSec],
        fileUrl = this[Tracks.fileUrl] ?: "", coverUrl = this[Tracks.coverUrl],
        description = this[Tracks.description],
        tags = this[Tracks.tags]?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        playCount = this[Tracks.playCount], likeCount = this[Tracks.likeCount],
        commentCount = this[Tracks.commentCount], isAiGenerated = this[Tracks.isAiGenerated],
        aiPrompt = this[Tracks.aiPrompt], createdAt = this[Tracks.createdAt]
    )
}
