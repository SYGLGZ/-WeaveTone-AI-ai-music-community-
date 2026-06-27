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
                        if (part.name == "file") {
                            val originalName = part.originalFileName ?: "upload"
                            val savedName = "${System.currentTimeMillis()}_${originalName}"
                            val uploadDir = File("uploads")
                            if (!uploadDir.exists()) uploadDir.mkdirs()
                            val file = File(uploadDir, savedName)
                            part.streamProvider().use { input ->
                                file.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            filePath = "uploads/$savedName"
                        }
                    }
                    else -> {}
                }
                part.dispose()
            }
            if (title.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Title is required"))
                return@post
            }
            if (filePath.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("File is required"))
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
            val tid = call.parameters["id"]?.toIntOrNull() ?: return@delete
            transaction {
                Tracks.deleteWhere { with(SqlExpressionBuilder) { val a: Op<Boolean> = Tracks.id eq tid; val b: Op<Boolean> = Tracks.userId eq uid; a and b } }
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
            val tid = call.parameters["id"]?.toIntOrNull() ?: return@post
            val exists = transaction { Tracks.select { Tracks.id eq tid }.count() > 0 }
            if (!exists) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Track not found"))
                return@post
            }
            try {
                transaction {
                    Likes.insert {
                        it[Likes.userId] = uid
                        it[Likes.trackId] = tid
                        it[Likes.createdAt] = System.currentTimeMillis()
                    }
                    Tracks.update({ Tracks.id eq tid }) {
                        with(SqlExpressionBuilder) {
                            it[Tracks.likeCount] = Tracks.likeCount + 1
                        }
                    }
                }
                logger.info("Like added: userId={}, trackId={}", uid, tid)
                call.respond(SuccessResponse("Liked"))
            } catch (e: Exception) {
                transaction {
                    Likes.deleteWhere { with(SqlExpressionBuilder) { val a: Op<Boolean> = Likes.userId eq uid; val b: Op<Boolean> = Likes.trackId eq tid; a and b } }
                    Tracks.update({ Tracks.id eq tid }) {
                        with(SqlExpressionBuilder) {
                            it[Tracks.likeCount] = Tracks.likeCount - 1
                        }
                    }
                }
                logger.info("Like removed: userId={}, trackId={}", uid, tid)
                call.respond(SuccessResponse("Unliked"))
            }
        }

        post("/api/v1/music/{id}/view") {
            val tid = call.parameters["id"]?.toIntOrNull() ?: return@post
            transaction {
                Tracks.update({ Tracks.id eq tid }) {
                    with(SqlExpressionBuilder) {
                        it[Tracks.playCount] = Tracks.playCount + 1
                    }
                }
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
            val tid = call.parameters["id"]?.toIntOrNull() ?: return@get
            val comments = transaction {
                Comments.select { Comments.trackId eq tid }
                    .orderBy(Comments.createdAt, SortOrder.DESC)
                    .map { row ->
                        val cu = Users.select { Users.id eq row[Comments.userId] }.single()
                        CommentResponse(
                            id = row[Comments.id], userId = row[Comments.userId],
                            username = cu[Users.username], content = row[Comments.content],
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
            if (req.content.length !in 1..500) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Comment content must be 1-500 characters"))
                return@post
            }
            transaction {
                Comments.insert {
                    it[Comments.userId] = uid
                    it[Comments.trackId] = tid
                    it[Comments.content] = req.content
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
            val uid = call.parameters["id"]?.toIntOrNull() ?: return@get
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
