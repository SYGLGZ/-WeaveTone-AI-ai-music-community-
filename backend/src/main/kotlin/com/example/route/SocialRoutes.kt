/**
 * Social Routes
 * GET  /api/v1/user/{id}/profile   — Get user profile
 * POST /api/v1/user/{id}/follow    — Toggle follow/unfollow (auth required)
 * GET  /api/v1/user/{id}/followers — Get user's followers
 */
package com.example.route

import com.example.middleware.requireAuth
import com.example.model.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.socialRoutes() {
    routing {
        get("/api/v1/user/{id}/profile") {
            val pid = call.parameters["id"]?.toIntOrNull() ?: return@get
            val profile = transaction {
                val user = Users.select { Users.id eq pid }.singleOrNull() ?: return@transaction null
                val tc = Tracks.select { Tracks.userId eq pid }.count().toInt()
                val fc = Follows.select { Follows.followedId eq pid }.count().toInt()
                val fgc = Follows.select { Follows.followerId eq pid }.count().toInt()
                UserProfileResponse(
                    id = user[Users.id], username = user[Users.username],
                    avatarUrl = user[Users.avatarUrl], bio = user[Users.bio],
                    trackCount = tc, followerCount = fc, followingCount = fgc
                )
            }
            if (profile == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))
                return@get
            }
            call.respond(profile)
        }

        post("/api/v1/user/{id}/follow") {
            val (uid, _) = call.requireAuth() ?: return@post
            val targetId = call.parameters["id"]?.toIntOrNull() ?: return@post
            if (uid == targetId) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Cannot follow yourself"))
                return@post
            }
            try {
                transaction {
                    Follows.insert {
                        it[Follows.followerId] = uid
                        it[Follows.followedId] = targetId
                        it[Follows.createdAt] = System.currentTimeMillis()
                    }
                }
                call.respond(SuccessResponse("Followed"))
            } catch (e: Exception) {
                transaction {
                    Follows.deleteWhere { with(SqlExpressionBuilder) { val a: Op<Boolean> = Follows.followerId eq uid; val b: Op<Boolean> = Follows.followedId eq targetId; a and b } }
                }
                call.respond(SuccessResponse("Unfollowed"))
            }
        }

        get("/api/v1/user/{id}/followers") {
            val targetId = call.parameters["id"]?.toIntOrNull() ?: return@get
            val followers = transaction {
                (Follows innerJoin Users).select { Follows.followedId eq targetId }
                    .map { UserProfileResponse(
                        id = it[Users.id], username = it[Users.username],
                        avatarUrl = it[Users.avatarUrl], bio = it[Users.bio],
                        trackCount = 0, followerCount = 0, followingCount = 0
                    ) }
            }
            call.respond(followers)
        }
    }
}
