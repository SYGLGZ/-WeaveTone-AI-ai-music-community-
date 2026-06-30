package com.example.route

import com.example.ai.AiGenerationStatus
import com.example.config.DatabaseFactory
import com.example.config.DatabaseSettings
import com.example.config.JwtConfig
import com.example.config.configureSerialization
import com.example.config.configureStatusPages
import com.example.model.AiGenerationJobs
import com.example.model.Tracks
import com.example.model.Users
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AiGenerationRoutesTest {
    private var userId = 0
    private var jobId = 0

    @BeforeTest
    fun setUpDatabase() {
        val name = "ai_${UUID.randomUUID().toString().replace("-", "")}"
        DatabaseFactory.init(DatabaseSettings("jdbc:h2:mem:$name;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", ""))
        transaction {
            userId = Users.insert {
                it[username] = "creator"
                it[email] = "creator@example.com"
                it[password] = "unused"
                it[createdAt] = System.currentTimeMillis()
            } get Users.id
            val now = System.currentTimeMillis()
            jobId = AiGenerationJobs.insert {
                it[AiGenerationJobs.userId] = this@AiGenerationRoutesTest.userId
                it[prompt] = "test prompt"
                it[durationSec] = 30
                it[provider] = "fake"
                it[status] = AiGenerationStatus.SUCCEEDED.name
                it[progress] = 100
                it[audioFilePath] = "uploads/ai/test.wav"
                it[createdAt] = now
                it[updatedAt] = now
            } get AiGenerationJobs.id
        }
    }

    @Test
    fun `publishing the same generation is idempotent`() = testApplication {
        application {
            configureSerialization()
            configureStatusPages()
            aiGenerationRoutes()
        }

        val first = publish()
        val second = publish()

        assertEquals(HttpStatusCode.Created, first.status)
        assertEquals(HttpStatusCode.OK, second.status)
        assertEquals(1L, transaction { Tracks.selectAll().count() })
    }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.publish() =
        client.post("/api/v1/ai/generations/$jobId/publish") {
            header(HttpHeaders.Authorization, "Bearer ${JwtConfig.generateToken(userId, "creator")}")
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
}
