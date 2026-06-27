package com.example.middleware

import com.example.config.JwtConfig
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("AuthMiddleware")

suspend fun ApplicationCall.requireAuth(): Pair<Int, String>? {
    val authHeader = request.headers["Authorization"]
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        logger.warn("Auth failed: missing token, ip={}", request.local.remoteHost)
        respond(HttpStatusCode.Unauthorized, mapOf("error" to "Missing auth token"))
        return null
    }
    val token = authHeader.removePrefix("Bearer ")
    val claims = JwtConfig.verifyToken(token)
    if (claims == null) {
        logger.warn("Auth failed: invalid token, ip={}", request.local.remoteHost)
        respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid or expired token"))
        return null
    }
    return claims["userId"] as Int to claims["username"] as String
}

suspend fun ApplicationCall.optionalAuth(): Pair<Int?, String?> {
    val authHeader = request.headers["Authorization"] ?: return null to null
    if (!authHeader.startsWith("Bearer ")) return null to null
    val token = authHeader.removePrefix("Bearer ")
    val claims = JwtConfig.verifyToken(token) ?: return null to null
    return claims["userId"] as Int to claims["username"] as String
}
