package com.example.route

import com.example.config.DatabaseFactory
import com.example.config.DatabaseSettings
import com.example.config.JwtConfig
import com.example.config.configureSerialization
import com.example.config.configureStatusPages
import com.example.model.Follows
import com.example.model.Users
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SocialRoutesTest {
    private var userId = 0
    private var targetId = 0

    @BeforeTest
    fun setUpDatabase() {
        val name = "social_${UUID.randomUUID().toString().replace("-", "")}"
        DatabaseFactory.init(DatabaseSettings("jdbc:h2:mem:$name;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", ""))
        transaction {
            userId = createUser("follower", "follower@example.com")
            targetId = createUser("creator", "creator@example.com")
        }
    }

    @Test
    fun `following and unfollowing keeps a single relation`() = testApplication {
        installRoutes()

        val followed = client.post("/api/v1/user/$targetId/follow") { bearer() }
        assertEquals(HttpStatusCode.OK, followed.status)
        assertEquals(1L, transaction { Follows.selectAll().count() })

        val unfollowed = client.post("/api/v1/user/$targetId/follow") { bearer() }
        assertEquals(HttpStatusCode.OK, unfollowed.status)
        assertEquals(0L, transaction { Follows.selectAll().count() })
    }

    @Test
    fun `following unknown user returns not found without creating relation`() = testApplication {
        installRoutes()

        val response = client.post("/api/v1/user/999999/follow") { bearer() }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals(0L, transaction { Follows.selectAll().count() })
    }

    private fun io.ktor.server.testing.ApplicationTestBuilder.installRoutes() {
        application {
            configureSerialization()
            configureStatusPages()
            socialRoutes()
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.bearer() {
        header(HttpHeaders.Authorization, "Bearer ${JwtConfig.generateToken(userId, "follower")}")
    }

    private fun createUser(username: String, email: String): Int = Users.insert {
        it[Users.username] = username
        it[Users.email] = email
        it[password] = "unused"
        it[createdAt] = System.currentTimeMillis()
    } get Users.id
}
