package com.example.myfirstapp.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.MediaStore
import com.example.myfirstapp.common.Constants
import com.example.myfirstapp.data.local.MusicDatabase
import com.example.myfirstapp.data.local.PlaylistEntity
import com.example.myfirstapp.data.local.SongEntity
import com.example.myfirstapp.data.remote.MusicApi
import com.example.myfirstapp.data.remote.dto.AIPublishRequest
import com.example.myfirstapp.data.remote.dto.AIGenerationRequest
import com.example.myfirstapp.data.remote.dto.AIGenerationResponse
import com.example.myfirstapp.data.remote.dto.TrackDto
import com.example.myfirstapp.data.security.SecurePreferences
import com.example.myfirstapp.util.backendUrl
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicDatabase: MusicDatabase,
    private val musicApi: MusicApi,
    private val securePreferences: SecurePreferences
) {
    val songsFlow: Flow<List<SongEntity>> = musicDatabase.songDao().getAllSongs()
    val aiGeneratedSongsFlow: Flow<List<SongEntity>> = musicDatabase.songDao().getAIGeneratedSongs()
    val playlistsFlow: Flow<List<PlaylistEntity>> = musicDatabase.playlistDao().getAllPlaylists()

    @Suppress("DEPRECATION") // ALBUM column still functional on API 29+; needed for album name string
    suspend fun scanLocalMusic(): List<SongEntity> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<SongEntity>()
        val contentResolver: ContentResolver = context.contentResolver
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )
        val cursor = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val albumIdColumn = it.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val title = it.getString(titleColumn)
                val artist = it.getString(artistColumn)
                val album = it.getString(albumColumn)
                val duration = it.getLong(durationColumn)
                if (duration < Constants.MIN_AUDIO_DURATION_MS) continue
                val data = it.getString(dataColumn)
                val songUri = Uri.withAppendedPath(uri, id.toString()).toString()
                val aId = if (albumIdColumn >= 0) it.getLong(albumIdColumn) else null
                songs.add(
                    SongEntity(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        duration = duration,
                        uri = songUri,
                        filePath = data,
                        albumId = if (aId != null && aId > 0) aId else null
                    )
                )
            }
        }
        musicDatabase.songDao().clearAllSongs()
        musicDatabase.songDao().insertSongs(songs)
        songs
    }

    private fun getAuthHeader(): String {
        val token = securePreferences.getAuthToken() ?: return ""
        return "Bearer $token"
    }

    suspend fun generateMusic(request: AIGenerationRequest): Result<AIGenerationResponse> =
        withContext(Dispatchers.IO) {
            try {
                val auth = getAuthHeader()
                if (auth.isEmpty()) return@withContext Result.failure(Exception("请先登录"))
                val response = musicApi.createGeneration(auth, request)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.withAbsoluteAudioUrl())
                } else {
                    Result.failure(Exception("AI生成提交失败: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getGenerationStatus(jobId: Int): Result<AIGenerationResponse> =
        withContext(Dispatchers.IO) {
            try {
                val auth = getAuthHeader()
                if (auth.isEmpty()) return@withContext Result.failure(Exception("请先登录"))
                val response = musicApi.getGeneration(auth, jobId)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.withAbsoluteAudioUrl())
                } else {
                    Result.failure(Exception("查询生成状态失败: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getMyGenerations(): Result<List<AIGenerationResponse>> =
        withContext(Dispatchers.IO) {
            try {
                val auth = getAuthHeader()
                if (auth.isEmpty()) return@withContext Result.failure(Exception("请先登录"))
                val response = musicApi.getMyGenerations(auth)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.map { it.withAbsoluteAudioUrl() })
                } else {
                    Result.failure(Exception("加载生成历史失败: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun publishGeneration(jobId: Int, title: String?, description: String?): Result<TrackDto> =
        withContext(Dispatchers.IO) {
            try {
                val auth = getAuthHeader()
                if (auth.isEmpty()) return@withContext Result.failure(Exception("请先登录"))
                val response = musicApi.publishGeneration(
                    auth,
                    jobId,
                    AIPublishRequest(
                        title = title?.takeIf { it.isNotBlank() },
                        description = description?.takeIf { it.isNotBlank() },
                        tags = listOf("AI生成")
                    )
                )
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("发布失败: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getMyPublishedTracks(userId: Int): Result<List<TrackDto>> =
        withContext(Dispatchers.IO) {
            try {
                val response = musicApi.getUserTracks(userId)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("加载我的作品失败: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun uploadMusicFile(
        uri: Uri,
        title: String,
        artist: String?,
        genre: String?,
        bpm: Int?,
        description: String?
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val auth = getAuthHeader()
            if (auth.isEmpty()) return@withContext Result.failure(Exception("请先登录"))

            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext Result.failure(Exception("无法读取音频文件"))
            val fileName = resolveFileName(uri)
            val mediaType = "audio/*".toMediaTypeOrNull()
            val fileBody = bytes.toRequestBody(mediaType)
            val filePart = MultipartBody.Part.createFormData("file", fileName, fileBody)
            val textType = "text/plain".toMediaTypeOrNull()
            fun text(value: String?) = value?.takeIf { it.isNotBlank() }?.toRequestBody(textType)

            val response = musicApi.uploadFile(
                auth = auth,
                file = filePart,
                title = title.toRequestBody(textType),
                artist = text(artist),
                genre = text(genre),
                bpm = bpm?.toString()?.toRequestBody(textType),
                description = text(description)
            )
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!["id"] ?: 0)
            } else {
                Result.failure(Exception("上传失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun resolveFileName(uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) return cursor.getString(index) ?: "upload_audio"
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "upload_audio"
    }

    private fun AIGenerationResponse.withAbsoluteAudioUrl(): AIGenerationResponse {
        val url = audioUrl ?: return this
        if (url.startsWith("http://") || url.startsWith("https://")) return this
        return copy(audioUrl = backendUrl(url))
    }

    suspend fun getSongById(id: Long): SongEntity? = musicDatabase.songDao().getSongById(id)

    suspend fun insertSong(song: SongEntity): Long = musicDatabase.songDao().insertSong(song)

    suspend fun createPlaylist(name: String): Long =
        musicDatabase.playlistDao().insertPlaylist(PlaylistEntity(name = name))

    fun getSongsInPlaylist(playlistId: Long): Flow<List<SongEntity>> =
        musicDatabase.playlistDao().getSongsInPlaylist(playlistId)
}
