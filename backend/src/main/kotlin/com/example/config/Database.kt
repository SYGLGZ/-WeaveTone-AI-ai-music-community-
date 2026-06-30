package com.example.config

import com.example.model.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    @Volatile
    private var database: Database? = null
    private var dataSource: HikariDataSource? = null

    @Synchronized
    fun init(settings: DatabaseSettings = DatabaseSettings.fromEnvironment()) {
        val driver = if (settings.url.startsWith("jdbc:postgresql")) "org.postgresql.Driver" else "org.h2.Driver"
        val cfg = HikariConfig()
        cfg.setDriverClassName(driver)
        cfg.setJdbcUrl(settings.url)
        cfg.setUsername(settings.user)
        cfg.setPassword(settings.password)
        cfg.setMaximumPoolSize(10)
        cfg.setAutoCommit(false)
        val newDataSource = HikariDataSource(cfg)
        val newDatabase = Database.connect(newDataSource)
        transaction(newDatabase) {
            SchemaUtils.createMissingTablesAndColumns(
                Users, Tracks, AiGenerationJobs, Playlists, PlaylistTracks, Likes, Comments, PlayHistory, Follows
            )
        }
        dataSource?.close()
        dataSource = newDataSource
        database = newDatabase
    }

    fun isReady(): Boolean {
        val current = database ?: return false
        return runCatching {
            transaction(current) {
                exec("SELECT 1") { result -> result.next() }
            } == true
        }.getOrDefault(false)
    }
}

data class DatabaseSettings(
    val url: String,
    val user: String,
    val password: String
) {
    companion object {
        fun fromEnvironment() = DatabaseSettings(
            url = System.getenv("DB_URL") ?: "jdbc:postgresql://127.0.0.1:5432/aimusic",
            user = System.getenv("DB_USER") ?: "aimusic",
            password = System.getenv("DB_PASSWORD") ?: "aimusic123"
        )
    }
}

fun Application.configureDatabase() {
    DatabaseFactory.init()
}
