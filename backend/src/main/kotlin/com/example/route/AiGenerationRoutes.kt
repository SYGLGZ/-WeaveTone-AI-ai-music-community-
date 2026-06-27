package com.example.route

import com.example.ai.AiGenerationStatus
import com.example.ai.MusicGenerationProviderFactory
import com.example.ai.ProviderGenerationRequest
import com.example.middleware.requireAuth
import com.example.model.AiGenerationJobs
import com.example.model.AiGenerationRequest
import com.example.model.AiGenerationResponse
import com.example.model.AiPublishRequest
import com.example.model.ErrorResponse
import com.example.model.TrackResponse
import com.example.model.Tracks
import com.example.model.Users
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.io.File

private val aiLogger = LoggerFactory.getLogger("AiGenerationRoutes")
private val aiProvider by lazy { MusicGenerationProviderFactory.create() }
private val aiGenerationWorkerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

fun Application.aiGenerationRoutes() {
    routing {
        post("/api/v1/ai/generations") {
            val (uid, _) = call.requireAuth() ?: return@post
            val req = call.receive<AiGenerationRequest>()
            val prompt = req.prompt.trim()
            if (prompt.length !in 1..500) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Prompt must be 1-500 characters"))
                return@post
            }
            if (req.durationSec !in 5..120) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Duration must be between 5 and 120 seconds"))
                return@post
            }
            if (req.bpm != null && req.bpm !in 40..220) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("BPM must be between 40 and 220"))
                return@post
            }

            val hasRunningJob = transaction {
                AiGenerationJobs
                    .select {
                        (AiGenerationJobs.userId eq uid) and (
                            (AiGenerationJobs.status eq AiGenerationStatus.PENDING.name) or
                                (AiGenerationJobs.status eq AiGenerationStatus.RUNNING.name)
                        )
                    }
                    .count() > 0
            }
            if (hasRunningJob) {
                call.respond(HttpStatusCode.Conflict, ErrorResponse("You already have a generation task running"))
                return@post
            }

            val now = System.currentTimeMillis()
            val jobId = transaction {
                AiGenerationJobs.insert {
                    it[AiGenerationJobs.userId] = uid
                    it[AiGenerationJobs.prompt] = prompt
                    it[AiGenerationJobs.genre] = req.genre?.take(50)
                    it[AiGenerationJobs.bpm] = req.bpm
                    it[AiGenerationJobs.durationSec] = req.durationSec
                    it[AiGenerationJobs.provider] = aiProvider.name
                    it[AiGenerationJobs.status] = AiGenerationStatus.PENDING.name
                    it[AiGenerationJobs.progress] = 0
                    it[AiGenerationJobs.createdAt] = now
                    it[AiGenerationJobs.updatedAt] = now
                } get AiGenerationJobs.id
            }

            submitGenerationInBackground(
                jobId = jobId,
                userId = uid,
                prompt = prompt,
                genre = req.genre,
                bpm = req.bpm,
                durationSec = req.durationSec
            )

            val row = findOwnedJob(jobId, uid) ?: run {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Generation job not found"))
                return@post
            }
            call.respond(HttpStatusCode.Created, row.toAiGenerationResponse())
        }

        get("/api/v1/ai/generations/mine") {
            val (uid, _) = call.requireAuth() ?: return@get
            val jobs = transaction {
                AiGenerationJobs.select { AiGenerationJobs.userId eq uid }
                    .orderBy(AiGenerationJobs.createdAt, SortOrder.DESC)
                    .limit(20)
                    .map { it.toAiGenerationResponse() }
            }
            call.respond(jobs)
        }

        get("/api/v1/ai/generations/{id}") {
            val (uid, _) = call.requireAuth() ?: return@get
            val jobId = call.parameters["id"]?.toIntOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid generation id"))
                return@get
            }
            val beforeSync = findOwnedJob(jobId, uid) ?: run {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Generation job not found"))
                return@get
            }
            syncProviderStatusIfNeeded(beforeSync)
            val row = findOwnedJob(jobId, uid) ?: beforeSync
            call.respond(row.toAiGenerationResponse())
        }

        get("/api/v1/ai/generations/{id}/audio") {
            val (uid, _) = call.requireAuth() ?: return@get
            val jobId = call.parameters["id"]?.toIntOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid generation id"))
                return@get
            }
            val row = findOwnedJob(jobId, uid) ?: run {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Generation job not found"))
                return@get
            }
            val status = row[AiGenerationJobs.status]
            if (status != AiGenerationStatus.SUCCEEDED.name && status != AiGenerationStatus.PUBLISHED.name) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Audio is not ready"))
                return@get
            }
            val path = row[AiGenerationJobs.audioFilePath] ?: run {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Audio file is missing"))
                return@get
            }
            val file = File(path)
            if (!file.exists()) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Audio file is missing on disk"))
                return@get
            }
            call.response.headers.append(HttpHeaders.ContentType, audioContentType(file).toString())
            call.respondFile(file)
        }

        post("/api/v1/ai/generations/{id}/publish") {
            val (uid, uname) = call.requireAuth() ?: return@post
            val jobId = call.parameters["id"]?.toIntOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid generation id"))
                return@post
            }
            val req = call.receive<AiPublishRequest>()
            val row = findOwnedJob(jobId, uid) ?: run {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Generation job not found"))
                return@post
            }
            val status = row[AiGenerationJobs.status]
            if (status != AiGenerationStatus.SUCCEEDED.name && status != AiGenerationStatus.PUBLISHED.name) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Only succeeded generations can be published"))
                return@post
            }
            val audioPath = row[AiGenerationJobs.audioFilePath] ?: run {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Generated audio is not available"))
                return@post
            }
            row[AiGenerationJobs.trackId]?.let { existingTrackId ->
                val existing = findTrack(existingTrackId)
                if (existing != null) {
                    call.respond(existing)
                    return@post
                }
            }

            val title = req.title?.trim()?.takeIf { it.isNotBlank() }?.take(200)
                ?: "AI生成 - ${row[AiGenerationJobs.prompt].take(24)}"
            val now = System.currentTimeMillis()
            val trackId = transaction {
                Tracks.insert {
                    it[Tracks.userId] = uid
                    it[Tracks.title] = title
                    it[Tracks.artist] = uname
                    it[Tracks.genre] = row[AiGenerationJobs.genre]
                    it[Tracks.bpm] = row[AiGenerationJobs.bpm]
                    it[Tracks.durationSec] = row[AiGenerationJobs.durationSec]
                    it[Tracks.fileUrl] = audioPath
                    it[Tracks.description] = req.description
                    it[Tracks.tags] = req.tags?.joinToString(",")
                    it[Tracks.isAiGenerated] = true
                    it[Tracks.aiPrompt] = row[AiGenerationJobs.prompt]
                    it[Tracks.createdAt] = now
                } get Tracks.id
            }
            transaction {
                AiGenerationJobs.update({ AiGenerationJobs.id eq jobId }) {
                    it[AiGenerationJobs.status] = AiGenerationStatus.PUBLISHED.name
                    it[AiGenerationJobs.trackId] = trackId
                    it[AiGenerationJobs.updatedAt] = System.currentTimeMillis()
                }
            }
            val published = findTrack(trackId) ?: run {
                call.respond(HttpStatusCode.Created, mapOf("id" to trackId))
                return@post
            }
            call.respond(HttpStatusCode.Created, published)
        }
    }
}

private fun findOwnedJob(jobId: Int, userId: Int): ResultRow? = transaction {
    AiGenerationJobs
        .select { (AiGenerationJobs.id eq jobId) and (AiGenerationJobs.userId eq userId) }
        .singleOrNull()
}

private suspend fun syncProviderStatusIfNeeded(row: ResultRow) {
    val status = row[AiGenerationJobs.status]
    if (status != AiGenerationStatus.PENDING.name && status != AiGenerationStatus.RUNNING.name) return
    val providerTaskId = row[AiGenerationJobs.providerTaskId] ?: return
    val jobId = row[AiGenerationJobs.id]
    try {
        val providerStatus = aiProvider.getStatus(providerTaskId)
        var localPath: String? = row[AiGenerationJobs.audioFilePath]
        if (providerStatus.status == AiGenerationStatus.SUCCEEDED && localPath == null) {
            localPath = withContext(Dispatchers.IO) {
                val ext = extensionFromUrl(providerStatus.outputUrl)
                val file = File("uploads/ai", "generation_${jobId}.$ext")
                aiProvider.downloadAudio(providerStatus.outputUrl, file)
                file.path
            }
        }
        transaction {
            AiGenerationJobs.update({ AiGenerationJobs.id eq jobId }) {
                it[AiGenerationJobs.status] = providerStatus.status.name
                it[AiGenerationJobs.progress] = providerStatus.progress.coerceIn(0, 100)
                it[AiGenerationJobs.outputUrl] = providerStatus.outputUrl
                it[AiGenerationJobs.audioFilePath] = localPath
                it[AiGenerationJobs.error] = providerStatus.error
                it[AiGenerationJobs.updatedAt] = System.currentTimeMillis()
            }
        }
    } catch (e: Exception) {
        aiLogger.warn("AI generation status sync failed: jobId={}", jobId, e)
        transaction {
            AiGenerationJobs.update({ AiGenerationJobs.id eq jobId }) {
                it[AiGenerationJobs.status] = AiGenerationStatus.FAILED.name
                it[AiGenerationJobs.progress] = 100
                it[AiGenerationJobs.error] = e.message ?: "Provider status sync failed"
                it[AiGenerationJobs.updatedAt] = System.currentTimeMillis()
            }
        }
    }
}

private fun submitGenerationInBackground(
    jobId: Int,
    userId: Int,
    prompt: String,
    genre: String?,
    bpm: Int?,
    durationSec: Int
) {
    aiGenerationWorkerScope.launch {
        try {
            val submission = aiProvider.submit(
                ProviderGenerationRequest(
                    prompt = prompt,
                    genre = genre,
                    bpm = bpm,
                    durationSec = durationSec
                )
            )
            val submissionError = if (submission.status == AiGenerationStatus.FAILED) {
                runCatching { aiProvider.getStatus(submission.providerTaskId).error }.getOrNull()
            } else {
                null
            }
            transaction {
                AiGenerationJobs.update({ AiGenerationJobs.id eq jobId }) {
                    it[AiGenerationJobs.providerTaskId] = submission.providerTaskId
                    it[AiGenerationJobs.status] = submission.status.name
                    it[AiGenerationJobs.progress] = submission.progress
                    it[AiGenerationJobs.error] = submissionError
                    it[AiGenerationJobs.updatedAt] = System.currentTimeMillis()
                }
            }
            if (submission.status == AiGenerationStatus.FAILED) {
                aiLogger.warn(
                    "AI generation provider rejected task: jobId={}, userId={}, provider={}, providerTaskId={}, error={}",
                    jobId,
                    userId,
                    aiProvider.name,
                    submission.providerTaskId,
                    submissionError ?: "Provider returned FAILED without error detail"
                )
            } else {
                aiLogger.info(
                    "AI generation submitted: jobId={}, userId={}, provider={}, providerTaskId={}, status={}, progress={}",
                    jobId,
                    userId,
                    aiProvider.name,
                    submission.providerTaskId,
                    submission.status,
                    submission.progress
                )
            }
        } catch (e: Exception) {
            aiLogger.warn("AI generation submit failed: jobId={}, userId={}", jobId, userId, e)
            transaction {
                AiGenerationJobs.update({ AiGenerationJobs.id eq jobId }) {
                    it[AiGenerationJobs.status] = AiGenerationStatus.FAILED.name
                    it[AiGenerationJobs.progress] = 100
                    it[AiGenerationJobs.error] = e.message ?: "Provider submit failed"
                    it[AiGenerationJobs.updatedAt] = System.currentTimeMillis()
                }
            }
        }
    }
}

private fun ResultRow.toAiGenerationResponse(): AiGenerationResponse {
    val status = this[AiGenerationJobs.status]
    val audioUrl = if (
        (status == AiGenerationStatus.SUCCEEDED.name || status == AiGenerationStatus.PUBLISHED.name) &&
        this[AiGenerationJobs.audioFilePath] != null
    ) {
        "/api/v1/ai/generations/${this[AiGenerationJobs.id]}/audio"
    } else {
        null
    }
    return AiGenerationResponse(
        id = this[AiGenerationJobs.id],
        status = status,
        prompt = this[AiGenerationJobs.prompt],
        genre = this[AiGenerationJobs.genre],
        bpm = this[AiGenerationJobs.bpm],
        durationSec = this[AiGenerationJobs.durationSec],
        provider = this[AiGenerationJobs.provider],
        progress = this[AiGenerationJobs.progress],
        audioUrl = audioUrl,
        trackId = this[AiGenerationJobs.trackId],
        error = this[AiGenerationJobs.error],
        createdAt = this[AiGenerationJobs.createdAt],
        updatedAt = this[AiGenerationJobs.updatedAt]
    )
}

private fun findTrack(trackId: Int): TrackResponse? = transaction {
    (Tracks innerJoin Users)
        .select { Tracks.id eq trackId }
        .singleOrNull()
        ?.let { row -> row.toTrackResponse(row[Users.username]) }
}

private fun extensionFromUrl(url: String?): String {
    val clean = url?.substringBefore("?")?.substringAfterLast("/") ?: ""
    val ext = clean.substringAfterLast(".", missingDelimiterValue = "").lowercase()
    return when (ext) {
        "mp3", "wav", "ogg", "flac", "aac", "m4a" -> ext
        else -> "wav"
    }
}

private fun audioContentType(file: File): ContentType = when (file.extension.lowercase()) {
    "mp3" -> ContentType.Audio.MPEG
    "wav" -> ContentType.parse("audio/wav")
    "ogg" -> ContentType.parse("audio/ogg")
    "flac" -> ContentType.parse("audio/flac")
    "aac", "m4a" -> ContentType.parse("audio/aac")
    else -> ContentType.Application.OctetStream
}
