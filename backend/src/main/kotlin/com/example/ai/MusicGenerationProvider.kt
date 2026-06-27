package com.example.ai

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.math.min
import kotlin.math.PI
import kotlin.math.sin

enum class AiGenerationStatus {
    PENDING, RUNNING, SUCCEEDED, FAILED, PUBLISHED
}

data class ProviderGenerationRequest(
    val prompt: String,
    val genre: String?,
    val bpm: Int?,
    val durationSec: Int
)

data class ProviderSubmission(
    val providerTaskId: String,
    val status: AiGenerationStatus = AiGenerationStatus.RUNNING,
    val progress: Int = 5
)

data class ProviderStatus(
    val status: AiGenerationStatus,
    val progress: Int,
    val outputUrl: String? = null,
    val error: String? = null
)

interface MusicGenerationProvider {
    val name: String
    suspend fun submit(request: ProviderGenerationRequest): ProviderSubmission
    suspend fun getStatus(providerTaskId: String): ProviderStatus
    suspend fun downloadAudio(outputUrl: String?, target: File)
}

object MusicGenerationProviderFactory {
    fun create(): MusicGenerationProvider {
        val provider = System.getenv("AI_PROVIDER")?.lowercase() ?: "replicate"
        return when (provider) {
            "fake", "demo" -> FakeMusicGenerationProvider()
            "minimax", "minimax-music", "minimax_music" -> MiniMaxMusicProvider()
            else -> ReplicateMusicGenProvider()
        }
    }
}

class MiniMaxMusicProvider(
    private val apiKey: String? = System.getenv("MINIMAX_API_KEY"),
    private val model: String = System.getenv("MINIMAX_MODEL") ?: "music-2.6-free",
    private val endpoint: String = System.getenv("MINIMAX_MUSIC_ENDPOINT")
        ?: "https://api.minimaxi.com/v1/music_generation",
    private val outputFormat: String = System.getenv("MINIMAX_OUTPUT_FORMAT") ?: "url",
    private val instrumental: Boolean = (System.getenv("MINIMAX_INSTRUMENTAL") ?: "true").toBooleanStrictOrNull() ?: true,
    private val requestTimeoutMs: Long = System.getenv("MINIMAX_REQUEST_TIMEOUT_MS")?.toLongOrNull() ?: 300_000L
) : MusicGenerationProvider {
    override val name: String = "minimax-music"
    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = requestTimeoutMs
            connectTimeoutMillis = 30_000L
            socketTimeoutMillis = requestTimeoutMs
        }
    }
    private val json = Json { ignoreUnknownKeys = true }
    private val statusCache = mutableMapOf<String, ProviderStatus>()

    override suspend fun submit(request: ProviderGenerationRequest): ProviderSubmission {
        val token = apiKey?.takeIf { it.isNotBlank() }
            ?: error("MINIMAX_API_KEY is not configured. Set AI_PROVIDER=fake for offline demo mode.")

        val payload = buildJsonObject {
            put("model", JsonPrimitive(model))
            put("prompt", JsonPrimitive(buildPrompt(request)))
            put("stream", JsonPrimitive(false))
            put("output_format", JsonPrimitive(outputFormat))
            put("is_instrumental", JsonPrimitive(instrumental))
            put("audio_setting", buildJsonObject {
                put("sample_rate", JsonPrimitive(44_100))
                put("bitrate", JsonPrimitive(256_000))
                put("format", JsonPrimitive("mp3"))
            })
        }

        val response = client.post(endpoint) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(payload.toString())
        }
        val responseText = response.bodyAsText()
        if (response.status.value !in 200..299) {
            error("MiniMax HTTP ${response.status.value}: ${responseText.take(800)}")
        }

        val root = json.parseToJsonElement(responseText).jsonObject
        val taskId = readTraceId(root) ?: "minimax_${System.currentTimeMillis()}"
        val error = readError(root)
        if (error != null) {
            val failed = ProviderStatus(AiGenerationStatus.FAILED, 100, error = error)
            statusCache[taskId] = failed
            return ProviderSubmission(taskId, AiGenerationStatus.FAILED, 100)
        }

        val audio = readOutput(root)
            ?: error("MiniMax response did not contain generated audio output: ${responseText.take(800)}")
        val status = ProviderStatus(AiGenerationStatus.SUCCEEDED, 100, outputUrl = "minimax://$taskId")
        statusCache[taskId] = if (audio.startsWith("http://") || audio.startsWith("https://")) {
            status.copy(outputUrl = audio)
        } else {
            status.copy(outputUrl = "minimax-hex://$taskId.mp3")
        }
        if (!audio.startsWith("http://") && !audio.startsWith("https://")) {
            hexOutputCache[taskId] = audio
        }
        return ProviderSubmission(taskId, AiGenerationStatus.RUNNING, 95)
    }

    override suspend fun getStatus(providerTaskId: String): ProviderStatus {
        return statusCache[providerTaskId]
            ?: ProviderStatus(AiGenerationStatus.SUCCEEDED, 100, outputUrl = "minimax-hex://$providerTaskId.mp3")
    }

    override suspend fun downloadAudio(outputUrl: String?, target: File) {
        val url = outputUrl ?: error("MiniMax succeeded but did not return audio output")
        target.parentFile?.mkdirs()
        withContext(Dispatchers.IO) {
            when {
                url.startsWith("minimax-hex://") -> {
                    val taskId = url.removePrefix("minimax-hex://").substringBeforeLast(".")
                    val hex = hexOutputCache[taskId] ?: error("MiniMax hex audio cache expired")
                    target.writeBytes(hexToBytes(hex))
                }
                url.startsWith("http://") || url.startsWith("https://") -> {
                    @Suppress("DEPRECATION")
                    java.net.URL(url).openStream().use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                else -> error("Unsupported MiniMax audio output: $url")
            }
        }
    }

    private fun buildPrompt(request: ProviderGenerationRequest): String {
        val parts = mutableListOf(request.prompt.trim())
        request.genre?.takeIf { it.isNotBlank() }?.let { parts += "Genre: $it." }
        request.bpm?.let { parts += "Target BPM: $it." }
        parts += "Duration: ${request.durationSec} seconds."
        if (instrumental) parts += "Instrumental music only."
        return parts.joinToString(" ")
    }

    private fun readOutput(root: JsonObject): String? {
        val data = root["data"]?.jsonObject
        return data?.get("audio")?.jsonPrimitive?.contentOrNull
            ?: data?.get("audio_url")?.jsonPrimitive?.contentOrNull
            ?: data?.get("url")?.jsonPrimitive?.contentOrNull
            ?: root["audio"]?.jsonPrimitive?.contentOrNull
            ?: root["audio_url"]?.jsonPrimitive?.contentOrNull
            ?: root["output"]?.jsonPrimitive?.contentOrNull
    }

    private fun readTraceId(root: JsonObject): String? {
        return root["trace_id"]?.jsonPrimitive?.contentOrNull
            ?: root["id"]?.jsonPrimitive?.contentOrNull
            ?: root["task_id"]?.jsonPrimitive?.contentOrNull
    }

    private fun readError(root: JsonObject): String? {
        val baseResp = root["base_resp"]?.jsonObject
        val statusCode = baseResp?.get("status_code")?.jsonPrimitive?.contentOrNull
        val statusMsg = baseResp?.get("status_msg")?.jsonPrimitive?.contentOrNull
        val isSuccess = statusCode == null || statusCode == "0"
        return if (isSuccess) null else (statusMsg ?: "MiniMax request failed: $statusCode")
    }

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.trim().removePrefix("0x")
        require(clean.length % 2 == 0) { "Invalid MiniMax hex audio length" }
        return ByteArray(clean.length / 2) { index ->
            clean.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }

    companion object {
        private val hexOutputCache = mutableMapOf<String, String>()
    }
}

class ReplicateMusicGenProvider(
    private val token: String? = System.getenv("REPLICATE_API_TOKEN"),
    private val modelOwner: String = System.getenv("REPLICATE_MODEL_OWNER") ?: "meta",
    private val modelName: String = System.getenv("REPLICATE_MODEL_NAME") ?: "musicgen",
    private val modelVersion: String? = System.getenv("REPLICATE_MODEL_VERSION")?.takeIf { it.isNotBlank() },
    private val predictionsUrlOverride: String? = System.getenv("REPLICATE_PREDICTIONS_URL")?.takeIf { it.isNotBlank() }
) : MusicGenerationProvider {
    override val name: String = "replicate-musicgen"
    private val client = HttpClient(CIO)
    private val json = Json { ignoreUnknownKeys = true }
    private val predictionGetUrls = mutableMapOf<String, String>()

    override suspend fun submit(request: ProviderGenerationRequest): ProviderSubmission {
        val apiToken = token?.takeIf { it.isNotBlank() }
            ?: error("REPLICATE_API_TOKEN is not configured. Set AI_PROVIDER=fake for offline demo mode.")

        val input = buildJsonObject {
            put("prompt", JsonPrimitive(buildPrompt(request)))
            put("duration", JsonPrimitive(request.durationSec))
            request.bpm?.let { put("bpm", JsonPrimitive(it)) }
        }

        val payload = if (modelVersion != null) {
            buildJsonObject {
                put("version", JsonPrimitive(modelVersion))
                put("input", input)
            }
        } else {
            buildJsonObject { put("input", input) }
        }

        val predictionsUrl = predictionsUrlOverride
            ?: if (modelVersion != null) {
                "https://api.replicate.com/v1/predictions"
            } else {
                "https://api.replicate.com/v1/models/$modelOwner/$modelName/predictions"
            }

        val responseText = client.post(predictionsUrl) {
            header(HttpHeaders.Authorization, "Bearer $apiToken")
            contentType(ContentType.Application.Json)
            setBody(payload.toString())
        }.bodyAsText()

        val root = json.parseToJsonElement(responseText).jsonObject
        val id = root["id"]?.jsonPrimitive?.contentOrNull
            ?: error("Replicate response did not contain prediction id")
        root["urls"]?.jsonObject?.get("get")?.jsonPrimitive?.contentOrNull?.let {
            predictionGetUrls[id] = it
        }
        val mapped = mapStatus(root["status"]?.jsonPrimitive?.contentOrNull)
        return ProviderSubmission(
            providerTaskId = id,
            status = mapped,
            progress = if (mapped == AiGenerationStatus.SUCCEEDED) 100 else 5
        )
    }

    override suspend fun getStatus(providerTaskId: String): ProviderStatus {
        val apiToken = token?.takeIf { it.isNotBlank() }
            ?: error("REPLICATE_API_TOKEN is not configured")
        val statusUrl = predictionGetUrls[providerTaskId]
            ?: "https://api.replicate.com/v1/predictions/$providerTaskId"
        val responseText = client.get(statusUrl) {
            header(HttpHeaders.Authorization, "Bearer $apiToken")
        }.bodyAsText()
        val root = json.parseToJsonElement(responseText).jsonObject
        val status = mapStatus(root["status"]?.jsonPrimitive?.contentOrNull)
        val outputUrl = readOutputUrl(root["output"])
        val error = root["error"]?.jsonPrimitive?.contentOrNull
        return ProviderStatus(
            status = status,
            progress = when (status) {
                AiGenerationStatus.SUCCEEDED -> 100
                AiGenerationStatus.FAILED -> 100
                else -> 40
            },
            outputUrl = outputUrl,
            error = error
        )
    }

    override suspend fun downloadAudio(outputUrl: String?, target: File) {
        val url = outputUrl ?: error("Replicate succeeded but did not return an audio URL")
        target.parentFile?.mkdirs()
        withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            java.net.URL(url).openStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }

    private fun buildPrompt(request: ProviderGenerationRequest): String {
        val parts = mutableListOf(request.prompt.trim())
        request.genre?.takeIf { it.isNotBlank() }?.let { parts += "Genre: $it." }
        request.bpm?.let { parts += "Target BPM: $it." }
        return parts.joinToString(" ")
    }

    private fun mapStatus(raw: String?): AiGenerationStatus = when (raw?.lowercase()) {
        "succeeded", "success", "completed" -> AiGenerationStatus.SUCCEEDED
        "failed", "canceled", "cancelled" -> AiGenerationStatus.FAILED
        "starting", "processing", "running" -> AiGenerationStatus.RUNNING
        else -> AiGenerationStatus.PENDING
    }

    private fun readOutputUrl(output: kotlinx.serialization.json.JsonElement?): String? = when (output) {
        is JsonPrimitive -> output.contentOrNull
        is JsonArray -> output.firstOrNull()?.jsonPrimitive?.contentOrNull
        is JsonObject -> output["audio"]?.jsonPrimitive?.contentOrNull
            ?: output["url"]?.jsonPrimitive?.contentOrNull
        else -> null
    }
}

class FakeMusicGenerationProvider : MusicGenerationProvider {
    override val name: String = "fake-musicgen"

    override suspend fun submit(request: ProviderGenerationRequest): ProviderSubmission {
        val taskId = "fake_${System.currentTimeMillis()}"
        return ProviderSubmission(providerTaskId = taskId, status = AiGenerationStatus.RUNNING, progress = 10)
    }

    override suspend fun getStatus(providerTaskId: String): ProviderStatus {
        val startedAt = providerTaskId.substringAfter("fake_", "0").toLongOrNull() ?: 0L
        val elapsed = System.currentTimeMillis() - startedAt
        val progress = min(100, 10 + (elapsed / 60).toInt())
        return if (elapsed >= 3_000L) {
            ProviderStatus(AiGenerationStatus.SUCCEEDED, 100, outputUrl = "fake://generated-audio")
        } else {
            ProviderStatus(AiGenerationStatus.RUNNING, progress)
        }
    }

    override suspend fun downloadAudio(outputUrl: String?, target: File) {
        target.parentFile?.mkdirs()
        withContext(Dispatchers.IO) {
            target.outputStream().use { output ->
                val sampleRate = 44_100
                val notes = listOf(
                    523.25, 659.25, 783.99, 1046.50,
                    783.99, 659.25, 587.33, 659.25,
                    523.25, 659.25, 783.99, 987.77,
                    880.00, 783.99, 659.25, 523.25
                )
                val noteDurationMs = 220
                val gapDurationMs = 25
                val samplesPerNote = sampleRate * noteDurationMs / 1000
                val samplesPerGap = sampleRate * gapDurationMs / 1000
                val samples = notes.size * (samplesPerNote + samplesPerGap)
                val dataSize = samples * 2
                output.write("RIFF".toByteArray())
                writeIntLe(output, 36 + dataSize)
                output.write("WAVEfmt ".toByteArray())
                writeIntLe(output, 16)
                writeShortLe(output, 1)
                writeShortLe(output, 1)
                writeIntLe(output, sampleRate)
                writeIntLe(output, sampleRate * 2)
                writeShortLe(output, 2)
                writeShortLe(output, 16)
                output.write("data".toByteArray())
                writeIntLe(output, dataSize)
                notes.forEach { frequency ->
                    repeat(samplesPerNote) { i ->
                        val t = i.toDouble() / sampleRate.toDouble()
                        val square = if (sin(2.0 * PI * frequency * t) >= 0.0) 1.0 else -1.0
                        val envelope = when {
                            i < sampleRate * 0.01 -> i / (sampleRate * 0.01)
                            i > samplesPerNote - sampleRate * 0.02 -> (samplesPerNote - i) / (sampleRate * 0.02)
                            else -> 1.0
                        }.coerceIn(0.0, 1.0)
                        val sample = (square * envelope * 9_000).toInt()
                        writeShortLe(output, sample)
                    }
                    repeat(samplesPerGap) {
                        writeShortLe(output, 0)
                    }
                }
            }
        }
    }

    private fun writeIntLe(output: java.io.OutputStream, value: Int) {
        output.write(value and 0xff)
        output.write((value shr 8) and 0xff)
        output.write((value shr 16) and 0xff)
        output.write((value shr 24) and 0xff)
    }

    private fun writeShortLe(output: java.io.OutputStream, value: Int) {
        output.write(value and 0xff)
        output.write((value shr 8) and 0xff)
    }
}
