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
            val pid = call.parameters["id"]?.toIntOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid user id"))
                return@get
            }
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
            val targetId = call.parameters["id"]?.toIntOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid user id"))
                return@post
            }
            if (uid == targetId) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Cannot follow yourself"))
                return@post
            }
            val result = transaction {
                Users.select { Users.id eq targetId }.forUpdate().singleOrNull()
                    ?: return@transaction FollowResult.USER_NOT_FOUND
                val exists = Follows.select {
                    (Follows.followerId eq uid) and (Follows.followedId eq targetId)
                }.count() > 0
                if (exists) {
                    Follows.deleteWhere {
                        with(SqlExpressionBuilder) {
                            (Follows.followerId eq uid) and (Follows.followedId eq targetId)
                        }
                    }
                    FollowResult.UNFOLLOWED
                } else {
                    Follows.insert {
                        it[Follows.followerId] = uid
                        it[Follows.followedId] = targetId
                        it[Follows.createdAt] = System.currentTimeMillis()
                    }
                    FollowResult.FOLLOWED
                }
            }
            when (result) {
                FollowResult.USER_NOT_FOUND -> {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))
                    return@post
                }
                FollowResult.FOLLOWED -> call.respond(SuccessResponse("Followed"))
                FollowResult.UNFOLLOWED -> call.respond(SuccessResponse("Unfollowed"))
            }
        }

        get("/api/v1/user/{id}/followers") {
            val targetId = call.parameters["id"]?.toIntOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid user id"))
                return@get
            }
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

private enum class FollowResult { FOLLOWED, UNFOLLOWED, USER_NOT_FOUND }
