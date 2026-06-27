package com.example.myfirstapp.repository

import android.content.Context
import com.example.myfirstapp.data.local.MusicDatabase
import com.example.myfirstapp.data.local.PlaylistDao
import com.example.myfirstapp.data.local.SongDao
import com.example.myfirstapp.data.remote.MusicApi
import com.example.myfirstapp.data.remote.dto.AIGenerationRequest
import com.example.myfirstapp.data.remote.dto.AIGenerationResponse
import com.example.myfirstapp.data.repository.MusicRepository
import com.example.myfirstapp.data.security.SecurePreferences
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import retrofit2.Response

@RunWith(MockitoJUnitRunner::class)
class MusicRepositoryTest {
    @Mock private lateinit var context: Context
    @Mock private lateinit var musicDatabase: MusicDatabase
    @Mock private lateinit var songDao: SongDao
    @Mock private lateinit var playlistDao: PlaylistDao
    @Mock private lateinit var musicApi: MusicApi
    @Mock private lateinit var securePreferences: SecurePreferences

    private lateinit var repository: MusicRepository

    @Before
    fun setUp() {
        whenever(musicDatabase.songDao()).thenReturn(songDao)
        whenever(musicDatabase.playlistDao()).thenReturn(playlistDao)
        whenever(songDao.getAllSongs()).thenReturn(flowOf(emptyList()))
        whenever(songDao.getAIGeneratedSongs()).thenReturn(flowOf(emptyList()))
        whenever(playlistDao.getAllPlaylists()).thenReturn(flowOf(emptyList()))
        repository = MusicRepository(context, musicDatabase, musicApi, securePreferences)
    }

    @Test
    fun `generateMusic returns persisted job on successful backend response`() = runTest {
        val request = AIGenerationRequest(prompt = "8bit boss battle", genre = "electronic")
        val response = AIGenerationResponse(
            id = 7,
            status = "PENDING",
            prompt = request.prompt,
            genre = request.genre,
            durationSec = 30,
            provider = "fake-musicgen",
            createdAt = 1L,
            updatedAt = 1L
        )
        whenever(securePreferences.getAuthToken()).thenReturn("jwt-token")
        whenever(musicApi.createGeneration(any(), any())).thenReturn(Response.success(response))

        val result = repository.generateMusic(request)

        assertTrue(result.isSuccess)
        assertEquals(7, result.getOrThrow().id)
    }

    @Test
    fun `generateMusic rejects unauthenticated request before calling backend`() = runTest {
        whenever(securePreferences.getAuthToken()).thenReturn(null)

        val result = repository.generateMusic(AIGenerationRequest(prompt = "test"))

        assertTrue(result.isFailure)
        assertEquals("请先登录", result.exceptionOrNull()?.message)
    }

    @Test
    fun `generateMusic exposes backend HTTP failure`() = runTest {
        whenever(securePreferences.getAuthToken()).thenReturn("jwt-token")
        whenever(musicApi.createGeneration(any(), any())).thenReturn(
            Response.error(
                503,
                "{\"error\":\"provider unavailable\"}"
                    .toResponseBody("application/json".toMediaType())
            )
        )

        val result = repository.generateMusic(AIGenerationRequest(prompt = "test"))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("503"))
    }
}
