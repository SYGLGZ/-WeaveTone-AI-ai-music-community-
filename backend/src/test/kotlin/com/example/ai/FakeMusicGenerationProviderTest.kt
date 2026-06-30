package com.example.ai

import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeMusicGenerationProviderTest {
    private val provider = FakeMusicGenerationProvider()

    @Test
    fun `fake provider completes deterministically and writes playable wav`() = runBlocking {
        val completedTaskId = "fake_${System.currentTimeMillis() - 4_000}"

        val status = provider.getStatus(completedTaskId)
        val output = Files.createTempFile("fake-music-", ".wav").toFile()
        try {
            provider.downloadAudio(status.outputUrl, output)

            assertEquals(AiGenerationStatus.SUCCEEDED, status.status)
            assertEquals(100, status.progress)
            assertTrue(output.length() > 44)
            assertEquals("RIFF", output.inputStream().use { String(it.readNBytes(4)) })
        } finally {
            output.delete()
        }
    }
}
