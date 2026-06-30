package com.example.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JwtConfigTest {
    @Test
    fun `generated token contains expected identity`() {
        val token = JwtConfig.generateToken(42, "测试用户")

        val claims = JwtConfig.verifyToken(token)

        assertEquals(42, claims?.get("userId"))
        assertEquals("测试用户", claims?.get("username"))
    }

    @Test
    fun `tampered token is rejected`() {
        val token = JwtConfig.generateToken(7, "owner")
        val parts = token.split('.').toMutableList()
        val replacement = if (parts[1].first() == 'a') 'b' else 'a'
        parts[1] = replacement + parts[1].drop(1)

        assertNull(JwtConfig.verifyToken(parts.joinToString(".")))
    }
}
