/**
 * Auth Routes
 * POST /api/v1/auth/register - Register a new user
 * POST /api/v1/auth/login - Login with credentials
 * POST /api/v1/auth/verify - Verify JWT token validity
 */
package com.example.route

import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.config.JwtConfig
import com.example.config.RateLimiter
import com.example.model.AuthRequest
import com.example.model.AuthResponse
import com.example.model.ErrorResponse
import com.example.model.Users
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("AuthRoutes")
private val usernameRegex = Regex("^[\\p{L}\\p{N}_-]+$")

fun Application.authRoutes() {
    routing {
        post("/api/v1/auth/register") {
            if (!RateLimiter.rateLimit(call)) return@post

            val req = call.receive<AuthRequest>()
            val username = req.username.trim()

            if (username.length !in 2..20 || !usernameRegex.matches(username)) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("用户名需为2-20位中文、字母、数字、下划线或短横线")
                )
                return@post
            }

            if (req.password.length < 6) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("密码至少需要6个字符"))
                return@post
            }

            val existing = transaction {
                Users.select { Users.username eq username }.singleOrNull()
            }
            if (existing != null) {
                call.respond(HttpStatusCode.Conflict, ErrorResponse("用户名已存在"))
                return@post
            }

            val hash = BCrypt.withDefaults().hashToString(12, req.password.toCharArray())
            val email = req.email?.trim()?.takeIf { it.isNotBlank() }
                ?: "user_${System.currentTimeMillis()}@local.invalid"

            val uid = transaction {
                Users.insert {
                    it[Users.username] = username
                    it[Users.email] = email
                    it[Users.password] = hash
                    it[Users.createdAt] = System.currentTimeMillis()
                } get Users.id
            }

            val token = JwtConfig.generateToken(uid, username)
            logger.info("Registration success: username={}, userId={}", username, uid)
            call.respond(HttpStatusCode.Created, AuthResponse(token, uid, username))
        }

        post("/api/v1/auth/login") {
            if (!RateLimiter.rateLimit(call)) return@post

            val req = call.receive<AuthRequest>()
            val username = req.username.trim()
            val user = transaction {
                Users.select { Users.username eq username }.singleOrNull()
            } ?: run {
                logger.warn("Login failed: username={}, reason=user_not_found", username)
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("用户名或密码错误"))
                return@post
            }

            val valid = BCrypt.verifyer().verify(req.password.toCharArray(), user[Users.password]).verified
            if (!valid) {
                logger.warn("Login failed: username={}, reason=invalid_password", username)
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("用户名或密码错误"))
                return@post
            }

            val token = JwtConfig.generateToken(user[Users.id], user[Users.username])
            logger.info("Login success: username={}, userId={}", user[Users.username], user[Users.id])
            call.respond(AuthResponse(token, user[Users.id], user[Users.username]))
        }

        post("/api/v1/auth/verify") {
            val authHeader = call.request.headers["Authorization"] ?: ""
            val token = authHeader.removePrefix("Bearer ")
            val claims = JwtConfig.verifyToken(token)
            if (claims == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid token"))
            } else {
                call.respond(mapOf("valid" to true, "userId" to claims["userId"], "username" to claims["username"]))
            }
        }
    }
}
