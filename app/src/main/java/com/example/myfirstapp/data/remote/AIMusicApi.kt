package com.example.myfirstapp.data.remote

import com.example.myfirstapp.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface MusicApi {
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: AuthRequest): Response<AuthResponse>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    @GET("api/v1/music/discover")
    suspend fun discover(@Query("page") page: Int = 1): Response<List<TrackDto>>

    @GET("api/v1/music/hot")
    suspend fun hot(): Response<List<TrackDto>>

    @GET("api/v1/music/liked/mine")
    suspend fun getLikedTracks(
        @Header("Authorization") auth: String
    ): Response<List<TrackDto>>

    @GET("api/v1/music/search")
    suspend fun search(@Query("q") query: String): Response<SearchResultDto>

    @POST("api/v1/music/upload")
    suspend fun upload(
        @Header("Authorization") auth: String,
        @Body request: TrackUploadDto
    ): Response<Map<String, Int>>

    @Multipart
    @POST("api/v1/music/upload/file")
    suspend fun uploadFile(
        @Header("Authorization") auth: String,
        @Part file: MultipartBody.Part,
        @Part("title") title: RequestBody,
        @Part("artist") artist: RequestBody?,
        @Part("genre") genre: RequestBody?,
        @Part("bpm") bpm: RequestBody?,
        @Part("description") description: RequestBody?
    ): Response<Map<String, Int>>

    @GET("api/v1/music/{id}")
    suspend fun getTrack(@Path("id") id: Int): Response<TrackDto>

    @POST("api/v1/music/{id}/like")
    suspend fun toggleLike(
        @Header("Authorization") auth: String,
        @Path("id") id: Int
    ): Response<Map<String, String>>

    @GET("api/v1/music/{id}/comments")
    suspend fun getComments(@Path("id") id: Int): Response<List<CommentDto>>

    @POST("api/v1/music/{id}/comment")
    suspend fun postComment(
        @Header("Authorization") auth: String,
        @Path("id") id: Int,
        @Body content: Map<String, String>
    ): Response<Map<String, String>>

    @GET("api/v1/playlist/public")
    suspend fun getPublicPlaylists(): Response<List<PlaylistDto>>

    @GET("api/v1/playlist/mine")
    suspend fun getMyPlaylists(@Header("Authorization") auth: String): Response<List<PlaylistDto>>

    @POST("api/v1/playlist")
    suspend fun createPlaylist(
        @Header("Authorization") auth: String,
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): Response<Map<String, Int>>

    @GET("api/v1/playlist/{id}")
    suspend fun getPlaylist(@Path("id") id: Int): Response<PlaylistDto>

    @GET("api/v1/playlist/{id}/tracks")
    suspend fun getPlaylistTracks(@Path("id") id: Int): Response<List<TrackDto>>

    @POST("api/v1/playlist/{id}/add")
    suspend fun addToPlaylist(
        @Header("Authorization") auth: String,
        @Path("id") id: Int,
        @Body body: Map<String, Int>
    ): Response<Map<String, String>>

    @POST("api/v1/playlist/{id}/remove")
    suspend fun removeFromPlaylist(
        @Header("Authorization") auth: String,
        @Path("id") id: Int,
        @Body body: Map<String, Int>
    ): Response<Map<String, String>>

    @DELETE("api/v1/playlist/{id}")
    suspend fun deletePlaylist(
        @Header("Authorization") auth: String,
        @Path("id") id: Int
    ): Response<Map<String, String>>

    @POST("api/v1/ai/generations")
    suspend fun createGeneration(
        @Header("Authorization") auth: String,
        @Body request: AIGenerationRequest
    ): Response<AIGenerationResponse>

    @GET("api/v1/ai/generations/{id}")
    suspend fun getGeneration(
        @Header("Authorization") auth: String,
        @Path("id") id: Int
    ): Response<AIGenerationResponse>

    @GET("api/v1/ai/generations/mine")
    suspend fun getMyGenerations(
        @Header("Authorization") auth: String
    ): Response<List<AIGenerationResponse>>

    @POST("api/v1/ai/generations/{id}/publish")
    suspend fun publishGeneration(
        @Header("Authorization") auth: String,
        @Path("id") id: Int,
        @Body request: AIPublishRequest
    ): Response<TrackDto>

    @GET("api/v1/user/{id}/profile")
    suspend fun getUserProfile(@Path("id") id: Int): Response<UserProfileDto>

    @POST("api/v1/user/{id}/follow")
    suspend fun toggleFollow(
        @Header("Authorization") auth: String,
        @Path("id") id: Int
    ): Response<Map<String, String>>

    @GET("api/v1/user/{id}/tracks")
    suspend fun getUserTracks(@Path("id") id: Int): Response<List<TrackDto>>
}
