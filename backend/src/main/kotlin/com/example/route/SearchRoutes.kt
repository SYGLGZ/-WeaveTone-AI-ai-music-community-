/**
 * Search Routes
 * GET /api/v1/music/search — Search tracks and users by query
 * GET /api/v1/music/genres — List all available genres
 */
package com.example.route

import com.example.model.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.searchRoutes() {
    routing {
        get("/api/v1/music/search") {
            val q = call.request.queryParameters["q"] ?: ""
            val type = call.request.queryParameters["type"] ?: "all"
            val ql = q.lowercase()
            val tracks = if (type == "all" || type == "track") {
                transaction {
                    (Tracks innerJoin Users).select {
                        Tracks.title.lowerCase() like "%$ql%" or (Tracks.artist.lowerCase() like "%$ql%")
                    }.orderBy(Tracks.createdAt, SortOrder.DESC).limit(20).map { it.toTrackResponse(it[Users.username]) }
                }
            } else emptyList()
            val users = if (type == "all" || type == "user") {
                transaction {
                    Users.select { Users.username.lowerCase() like "%$ql%" }.limit(10)
                        .map { UserProfileResponse(
                            id = it[Users.id], username = it[Users.username],
                            avatarUrl = it[Users.avatarUrl], bio = it[Users.bio],
                            trackCount = 0, followerCount = 0, followingCount = 0
                        ) }
                }
            } else emptyList()
            call.respond(mapOf("tracks" to tracks, "users" to users))
        }

        get("/api/v1/music/genres") {
            val genres = transaction {
                Tracks.select { Tracks.genre.isNotNull() and Tracks.genre.neq("") }
                    .map { it[Tracks.genre]!! }.distinct().sorted()
            }
            call.respond(genres)
        }
    }
}
