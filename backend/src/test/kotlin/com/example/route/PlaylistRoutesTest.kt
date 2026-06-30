package com.example.route

import com.example.config.DatabaseFactory
import com.example.config.DatabaseSettings
import com.example.config.JwtConfig
import com.example.config.configureSerialization
import com.example.config.configureStatusPages
import com.example.model.PlaylistTracks
import com.example.model.Playlists
import com.example.model.Tracks
import com.example.model.Users
import io.ktor.client.request.delete
import io.ktor.client.request.get
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

class PlaylistRoutesTest {
    private var ownerId: Int = 0
    private var intruderId: Int = 0
    private var trackId: Int = 0
    private var playlistId: Int = 0

    @BeforeTest
    fun setUpDatabase() {
        val databaseName = "playlist_${UUID.randomUUID().toString().replace("-", "")}"
        DatabaseFactory.init(
            DatabaseSettings(
                "jdbc:h2:mem:$databaseName;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa",
                ""
            )
        )
        transaction {
            ownerId = createUser("owner", "owner@example.com")
            intruderId = createUser("intruder", "intruder@example.com")
            trackId = Tracks.insert {
                it[userId] = ownerId
                it[title] = "Test track"
                it[fileUrl] = "uploads/test.wav"
                it[createdAt] = System.currentTimeMillis()
            } get Tracks.id
            playlistId = Playlists.insert {
                it[userId] = ownerId
                it[name] = "Private list"
                it[isPublic] = false
                it[createdAt] = System.currentTimeMillis()
            } get Playlists.id
            PlaylistTracks.insert {
                it[PlaylistTracks.playlistId] = this@PlaylistRoutesTest.playlistId
                it[PlaylistTracks.trackId] = this@PlaylistRoutesTest.trackId
            }
        }
    }

    @Test
    fun `non owner cannot remove a track from playlist`() = testApplication {
        installPlaylistApplication()

        val response = client.post("/api/v1/playlist/$playlistId/remove") {
            bearer(intruderId, "intruder")
            contentType(ContentType.Application.Json)
            setBody("""{"trackId":$trackId}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals(1L, transaction { PlaylistTracks.selectAll().count() })
    }

    @Test
    fun `private playlist is hidden from non owner`() = testApplication {
        installPlaylistApplication()

        val anonymous = client.get("/api/v1/playlist/$playlistId")
        val intruder = client.get("/api/v1/playlist/$playlistId/tracks") {
            bearer(intruderId, "intruder")
        }

        assertEquals(HttpStatusCode.Forbidden, anonymous.status)
        assertEquals(HttpStatusCode.Forbidden, intruder.status)
    }

    @Test
    fun `non owner cannot delete playlist`() = testApplication {
        installPlaylistApplication()

        val response = client.delete("/api/v1/playlist/$playlistId") {
            bearer(intruderId, "intruder")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals(1L, transaction { Playlists.selectAll().count() })
    }

    @Test
    fun `adding the same track twice returns conflict`() = testApplication {
        installPlaylistApplication()

        val response = client.post("/api/v1/playlist/$playlistId/add") {
            bearer(ownerId, "owner")
            contentType(ContentType.Application.Json)
            setBody("""{"trackId":$trackId}""")
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals(1L, transaction { PlaylistTracks.selectAll().count() })
    }

    @Test
    fun `deleting playlist also deletes playlist track relations`() = testApplication {
        installPlaylistApplication()

        val response = client.delete("/api/v1/playlist/$playlistId") {
            bearer(ownerId, "owner")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(0L, transaction { Playlists.selectAll().count() })
        assertEquals(0L, transaction { PlaylistTracks.selectAll().count() })
    }

    private fun io.ktor.server.testing.ApplicationTestBuilder.installPlaylistApplication() {
        application {
            configureSerialization()
            configureStatusPages()
            playlistRoutes()
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.bearer(userId: Int, username: String) {
        header(HttpHeaders.Authorization, "Bearer ${JwtConfig.generateToken(userId, username)}")
    }

    private fun createUser(username: String, email: String): Int = Users.insert {
        it[Users.username] = username
        it[Users.email] = email
        it[Users.password] = "not-used-in-route-test"
        it[Users.createdAt] = System.currentTimeMillis()
    } get Users.id
}
