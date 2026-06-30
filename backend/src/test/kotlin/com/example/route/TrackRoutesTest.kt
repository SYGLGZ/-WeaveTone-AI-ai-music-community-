package com.example.route

import com.example.config.DatabaseFactory
import com.example.config.DatabaseSettings
import com.example.config.JwtConfig
import com.example.config.configureSerialization
import com.example.config.configureStatusPages
import com.example.model.Likes
import com.example.model.Tracks
import com.example.model.Users
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TrackRoutesTest {
    private var ownerId = 0
    private var intruderId = 0
    private var trackId = 0

    @BeforeTest
    fun setUpDatabase() {
        val name = "track_${UUID.randomUUID().toString().replace("-", "")}"
        DatabaseFactory.init(DatabaseSettings("jdbc:h2:mem:$name;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", ""))
        transaction {
            ownerId = createUser("owner", "owner@example.com")
            intruderId = createUser("intruder", "intruder@example.com")
            trackId = Tracks.insert {
                it[userId] = ownerId
                it[title] = "Owned track"
                it[fileUrl] = "uploads/test.wav"
                it[createdAt] = System.currentTimeMillis()
            } get Tracks.id
        }
    }

    @Test
    fun `non owner cannot delete track`() = testApplication {
        installRoutes()

        val response = client.delete("/api/v1/music/$trackId") { bearer(intruderId, "intruder") }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals(1L, transaction { Tracks.selectAll().count() })
    }

    @Test
    fun `like toggle keeps relation and denormalized count consistent`() = testApplication {
        installRoutes()

        val liked = client.post("/api/v1/music/$trackId/like") { bearer(intruderId, "intruder") }
        assertEquals(HttpStatusCode.OK, liked.status)
        assertEquals(1L, transaction { Likes.selectAll().count() })
        assertEquals(1, transaction { Tracks.select { Tracks.id eq trackId }.single()[Tracks.likeCount] })

        val unliked = client.post("/api/v1/music/$trackId/like") { bearer(intruderId, "intruder") }
        assertEquals(HttpStatusCode.OK, unliked.status)
        assertEquals(0L, transaction { Likes.selectAll().count() })
        assertEquals(0, transaction { Tracks.select { Tracks.id eq trackId }.single()[Tracks.likeCount] })
    }

    private fun io.ktor.server.testing.ApplicationTestBuilder.installRoutes() {
        application {
            configureSerialization()
            configureStatusPages()
            trackRoutes()
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.bearer(userId: Int, username: String) {
        header(HttpHeaders.Authorization, "Bearer ${JwtConfig.generateToken(userId, username)}")
    }

    private fun createUser(username: String, email: String): Int = Users.insert {
        it[Users.username] = username
        it[Users.email] = email
        it[password] = "unused"
        it[createdAt] = System.currentTimeMillis()
    } get Users.id
}
