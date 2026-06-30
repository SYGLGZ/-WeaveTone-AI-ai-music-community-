package com.example.route

import com.example.config.DatabaseFactory
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

@Serializable
data class ServiceInfo(
    val service: String,
    val version: String,
    val status: String
)

@Serializable
data class HealthResponse(
    val status: String,
    val timestamp: Long
)

fun Application.systemRoutes() {
    routing {
        get("/") {
            call.respond(ServiceInfo("ai-music-backend", "1.0.0", "running"))
        }
        get("/health") {
            call.respond(HealthResponse("UP", System.currentTimeMillis()))
        }
        get("/ready") {
            val ready = DatabaseFactory.isReady()
            call.respond(
                if (ready) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable,
                HealthResponse(if (ready) "UP" else "DOWN", System.currentTimeMillis())
            )
        }
    }
}
