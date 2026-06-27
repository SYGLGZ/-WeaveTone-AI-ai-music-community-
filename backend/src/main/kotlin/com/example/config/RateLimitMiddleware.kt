package com.example.config

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

private val logger = LoggerFactory.getLogger("RateLimiter")

object RateLimiter {
    private val requestLog = ConcurrentHashMap<String, MutableList<Long>>()

    suspend fun rateLimit(call: ApplicationCall, maxRequests: Int = 10, windowMs: Long = 60_000): Boolean {
        val ip = call.request.local.remoteHost
        val now = System.currentTimeMillis()
        val timestamps = requestLog.getOrPut(ip) { mutableListOf() }

        val blocked: Boolean
        synchronized(timestamps) {
            timestamps.removeAll { now - it > windowMs }
            if (timestamps.size >= maxRequests) {
                blocked = true
            } else {
                timestamps.add(now)
                blocked = false
            }
        }

        if (blocked) {
            logger.warn("Rate limit triggered: ip={}, count={}, windowMs={}", ip, timestamps.size, windowMs)
            call.respond(HttpStatusCode(429, "Too Many Requests"), mapOf("error" to "Rate limit exceeded. Try again later."))
            return false
        }
        return true
    }
}
