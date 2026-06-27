package com.example.route

import com.example.config.configureSerialization
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class SystemRoutesTest {
    @Test
    fun `health endpoint reports service is up`() = testApplication {
        application {
            configureSerialization()
            systemRoutes()
        }

        val response = client.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
        val payload = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("UP", payload["status"]?.jsonPrimitive?.content)
    }
}
