package com.example

import com.example.config.configureDatabase
import com.example.config.configureSerialization
import com.example.config.configureStatusPages
import com.example.config.configureCORS
import com.example.route.authRoutes
import com.example.route.trackRoutes
import com.example.route.playlistRoutes
import com.example.route.socialRoutes
import com.example.route.searchRoutes
import com.example.route.aiGenerationRoutes
import com.example.route.systemRoutes
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

private fun Application.module() {
    configureDatabase()
    configureSerialization()
    configureStatusPages()
    configureCORS()
    systemRoutes()
    authRoutes()
    trackRoutes()
    playlistRoutes()
    socialRoutes()
    searchRoutes()
    aiGenerationRoutes()
}
