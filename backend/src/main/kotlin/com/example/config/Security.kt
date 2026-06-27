package com.example.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

object JwtConfig {
    @Volatile
    private var secret = run {
        val envSecret = System.getenv("JWT_SECRET")
        if (envSecret == null) {
            System.err.println("WARNING: JWT_SECRET environment variable is not set. Using fallback key. Set JWT_SECRET for production.")
            "ai-music-secret-key-change-in-production"
        } else {
            envSecret
        }
    }
    private const val ISSUER = "ai-music-app"
    private const val VALIDITY_MS = 7 * 24 * 60 * 60 * 1000L

    private val algorithm = Algorithm.HMAC256(secret)

    fun generateToken(userId: Int, username: String): String {
        return JWT.create()
            .withIssuer(ISSUER)
            .withClaim("userId", userId)
            .withClaim("username", username)
            .withExpiresAt(Date(System.currentTimeMillis() + VALIDITY_MS))
            .sign(algorithm)
    }

    fun verifyToken(token: String): Map<String, Any>? {
        return try {
            val verifier = JWT.require(algorithm).withIssuer(ISSUER).build()
            val decoded = verifier.verify(token)
            mapOf(
                "userId" to (decoded.getClaim("userId").asInt() ?: 0),
                "username" to (decoded.getClaim("username").asString() ?: "")
            )
        } catch (_: Exception) {
            null
        }
    }
}
