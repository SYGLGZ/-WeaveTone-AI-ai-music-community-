package com.example.config

import com.example.model.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    private val dataSource by lazy {
        val url = System.getenv("DB_URL") ?: "jdbc:postgresql://127.0.0.1:5432/aimusic"
        val user = System.getenv("DB_USER") ?: "aimusic"
        val password = System.getenv("DB_PASSWORD") ?: "aimusic123"
        val driver = if (url.startsWith("jdbc:postgresql")) "org.postgresql.Driver" else "org.h2.Driver"

        val cfg = HikariConfig()
        cfg.setDriverClassName(driver)
        cfg.setJdbcUrl(url)
        cfg.setUsername(user)
        cfg.setPassword(password)
        cfg.setMaximumPoolSize(10)
        cfg.setAutoCommit(false)
        HikariDataSource(cfg)
    }

    fun init() {
        Database.connect(dataSource)
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Users, Tracks, AiGenerationJobs, Playlists, PlaylistTracks, Likes, Comments, PlayHistory, Follows
            )
        }
    }
}

fun Application.configureDatabase() {
    DatabaseFactory.init()
}
