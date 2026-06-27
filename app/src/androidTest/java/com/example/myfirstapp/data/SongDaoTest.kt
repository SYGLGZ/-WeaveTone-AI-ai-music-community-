package com.example.myfirstapp.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myfirstapp.data.local.MusicDatabase
import com.example.myfirstapp.data.local.SongEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SongDaoTest {

    private lateinit var database: MusicDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MusicDatabase::class.java)
            .build()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testInsertAndGetSong() = runBlocking {
        val song = SongEntity(
            id = 1L,
            title = "Test Song",
            artist = "Test Artist",
            album = "Test Album",
            duration = 30000L,
            uri = "content://test",
            isAIGenerated = false
        )
        database.songDao().insertSong(song)
        val retrieved = database.songDao().getSongById(1L)
        assertNotNull(retrieved)
        assertEquals("Test Song", retrieved?.title)
        assertEquals("Test Artist", retrieved?.artist)
    }

    @Test
    fun testDeleteSong() = runBlocking {
        val song = SongEntity(
            id = 2L,
            title = "Delete Me",
            artist = "Artist",
            duration = 30000L,
            uri = "content://test"
        )
        database.songDao().insertSong(song)
        database.songDao().deleteSongById(2L)
        val retrieved = database.songDao().getSongById(2L)
        assertNull(retrieved)
    }

    @Test
    fun testGetAllSongsFlow() = runBlocking {
        val song1 = SongEntity(id = 3L, title = "Song 1", artist = "A", duration = 30000L, uri = "uri1")
        val song2 = SongEntity(id = 4L, title = "Song 2", artist = "B", duration = 45000L, uri = "uri2")
        database.songDao().insertSong(song1)
        database.songDao().insertSong(song2)
        val songs = database.songDao().getAllSongs().first()
        assertEquals(2, songs.size)
    }

    @Test
    fun testGetAIGeneratedSongs() = runBlocking {
        val normalSong = SongEntity(id = 5L, title = "Normal", artist = "A", duration = 30000L, uri = "uri", isAIGenerated = false)
        val aiSong = SongEntity(id = 6L, title = "AI Song", artist = "AI", duration = 30000L, uri = "uri", isAIGenerated = true)
        database.songDao().insertSong(normalSong)
        database.songDao().insertSong(aiSong)
        val aiSongs = database.songDao().getAIGeneratedSongs().first()
        assertEquals(1, aiSongs.size)
        assertEquals("AI Song", aiSongs[0].title)
    }
}
