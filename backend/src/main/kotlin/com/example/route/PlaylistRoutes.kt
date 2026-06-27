/**
 * Playlist Routes
 * POST   /api/v1/playlist             — Create a new playlist (auth required)
 * GET    /api/v1/playlist/mine        — Get current user's playlists (auth required)
 * GET    /api/v1/playlist/public      — Get all public playlists
 * GET    /api/v1/playlist/{id}        — Get playlist details
 * GET    /api/v1/playlist/{id}/tracks — Get tracks in a playlist
 * POST   /api/v1/playlist/{id}/add    — Add track to playlist (auth required)
 * POST   /api/v1/playlist/{id}/remove — Remove track from playlist (auth required)
 * DELETE /api/v1/playlist/{id}        — Delete playlist (auth required)
 */
package com.example.route

import com.example.middleware.requireAuth
import com.example.model.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.playlistRoutes() {
    routing {
        post("/api/v1/playlist") {
            val (uid, _) = call.requireAuth() ?: return@post
            val req = call.receive<PlaylistRequest>()
            val pid = transaction {
                Playlists.insert {
                    it[Playlists.userId] = uid
                    it[Playlists.name] = req.name
                    it[Playlists.coverUrl] = req.coverUrl
                    it[Playlists.isPublic] = req.isPublic
                    it[Playlists.createdAt] = System.currentTimeMillis()
                } get Playlists.id
            }
            call.respond(HttpStatusCode.Created, mapOf("id" to pid))
        }

        get("/api/v1/playlist/mine") {
            val (uid, _) = call.requireAuth() ?: return@get
            val playlists = transaction {
                val rows = (Playlists innerJoin Users).select { Playlists.userId eq uid }
                    .orderBy(Playlists.createdAt, SortOrder.DESC).toList()
                val pids = rows.map { it[Playlists.id] }
                val countMap = mutableMapOf<Int, Int>()
                if (pids.isNotEmpty()) {
                    PlaylistTracks.select { PlaylistTracks.playlistId inList pids }
                        .forEach { row ->
                            val pid = row[PlaylistTracks.playlistId]
                            countMap[pid] = (countMap[pid] ?: 0) + 1
                        }
                }
                rows.map { row ->
                    PlaylistResponse(
                        id = row[Playlists.id], userId = row[Playlists.userId],
                        username = row[Users.username], name = row[Playlists.name],
                        coverUrl = row[Playlists.coverUrl], isPublic = row[Playlists.isPublic],
                        trackCount = countMap[row[Playlists.id]] ?: 0,
                        createdAt = row[Playlists.createdAt]
                    )
                }
            }
            call.respond(playlists)
        }

        get("/api/v1/playlist/public") {
            val playlists = transaction {
                val rows = (Playlists innerJoin Users).select { Playlists.isPublic eq true }
                    .orderBy(Playlists.createdAt, SortOrder.DESC).toList()
                val pids = rows.map { it[Playlists.id] }
                val countMap = mutableMapOf<Int, Int>()
                if (pids.isNotEmpty()) {
                    PlaylistTracks.select { PlaylistTracks.playlistId inList pids }
                        .forEach { row ->
                            val pid = row[PlaylistTracks.playlistId]
                            countMap[pid] = (countMap[pid] ?: 0) + 1
                        }
                }
                rows.map { row ->
                    PlaylistResponse(
                        id = row[Playlists.id], userId = row[Playlists.userId],
                        username = row[Users.username], name = row[Playlists.name],
                        coverUrl = row[Playlists.coverUrl], isPublic = row[Playlists.isPublic],
                        trackCount = countMap[row[Playlists.id]] ?: 0,
                        createdAt = row[Playlists.createdAt]
                    )
                }
            }
            call.respond(playlists)
        }

        get("/api/v1/playlist/{id}") {
            val pid = call.parameters["id"]?.toIntOrNull() ?: return@get
            val playlist = transaction {
                (Playlists innerJoin Users).select { Playlists.id eq pid }.singleOrNull()
            } ?: run { call.respond(HttpStatusCode.NotFound, ErrorResponse("Not found")); return@get }
            val cnt = transaction { PlaylistTracks.select { PlaylistTracks.playlistId eq pid }.count().toInt() }
            call.respond(PlaylistResponse(
                id = playlist[Playlists.id], userId = playlist[Playlists.userId],
                username = playlist[Users.username], name = playlist[Playlists.name],
                coverUrl = playlist[Playlists.coverUrl], isPublic = playlist[Playlists.isPublic],
                trackCount = cnt, createdAt = playlist[Playlists.createdAt]
            ))
        }

        get("/api/v1/playlist/{id}/tracks") {
            val pid = call.parameters["id"]?.toIntOrNull() ?: return@get
            val tracks = transaction {
                (PlaylistTracks innerJoin Tracks innerJoin Users).select { PlaylistTracks.playlistId eq pid }
                    .orderBy(PlaylistTracks.position)
                    .map { it.toTrackResponse(it[Users.username]) }
            }
            call.respond(tracks)
        }

        post("/api/v1/playlist/{id}/add") {
            val (uid, _) = call.requireAuth() ?: return@post
            val pid = call.parameters["id"]?.toIntOrNull() ?: return@post
            val playlist = transaction {
                Playlists.select { Playlists.id eq pid }.singleOrNull()
            } ?: run {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Playlist not found"))
                return@post
            }
            if (playlist[Playlists.userId] != uid) {
                call.respond(HttpStatusCode.Forbidden, ErrorResponse("You do not own this playlist"))
                return@post
            }
            val req = call.receive<Map<String, Int>>()
            val tid = req["trackId"] ?: return@post
            transaction {
                PlaylistTracks.insert {
                    it[PlaylistTracks.playlistId] = pid
                    it[PlaylistTracks.trackId] = tid
                }
            }
            call.respond(SuccessResponse("Track added"))
        }

        post("/api/v1/playlist/{id}/remove") {
            call.requireAuth() ?: return@post
            val pid = call.parameters["id"]?.toIntOrNull() ?: return@post
            val req = call.receive<Map<String, Int>>()
            val tid = req["trackId"] ?: return@post
            transaction {
                PlaylistTracks.deleteWhere { with(SqlExpressionBuilder) { val a: Op<Boolean> = PlaylistTracks.playlistId eq pid; val b: Op<Boolean> = PlaylistTracks.trackId eq tid; a and b } }
            }
            call.respond(SuccessResponse("Track removed"))
        }

        delete("/api/v1/playlist/{id}") {
            val (uid, _) = call.requireAuth() ?: return@delete
            val pid = call.parameters["id"]?.toIntOrNull() ?: return@delete
            transaction {
                Playlists.deleteWhere { with(SqlExpressionBuilder) { val a: Op<Boolean> = Playlists.id eq pid; val b: Op<Boolean> = Playlists.userId eq uid; a and b } }
            }
            call.respond(SuccessResponse("Deleted"))
        }
    }
}
