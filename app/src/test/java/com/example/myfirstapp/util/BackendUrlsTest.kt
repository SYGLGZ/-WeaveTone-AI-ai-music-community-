package com.example.myfirstapp.util

import org.junit.Assert.assertTrue
import org.junit.Test

class BackendUrlsTest {
    @Test
    fun `track stream URL uses configured backend and canonical API path`() {
        val url = trackStreamUrl(42)

        assertTrue(url.startsWith("http://") || url.startsWith("https://"))
        assertTrue(url.endsWith("/api/v1/music/42/stream"))
    }
}
