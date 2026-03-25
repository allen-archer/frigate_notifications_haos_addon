package com.allenarcher

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class HaNotificationSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val notification = HaNotification(
        title = "Car",
        message = "Front",
        data = HaNotificationData(
            image = "https://frigate.example.com/api/events/abc123/snapshot.jpg?h=480&quality=90",
            clickAction = "https://frigate.example.com/api/events/abc123/clip.mp4"
        )
    )

    @Test
    fun `serializes to correct JSON structure`() {
        val result = json.decodeFromString<HaNotification>(json.encodeToString(notification))

        assertEquals("Car", result.title)
        assertEquals("Front", result.message)
        assertEquals("https://frigate.example.com/api/events/abc123/snapshot.jpg?h=480&quality=90", result.data.image)
        assertEquals("https://frigate.example.com/api/events/abc123/clip.mp4", result.data.clickAction)
    }

    @Test
    fun `clickAction field name is preserved`() {
        val serialized = json.encodeToString(notification)
        assert(serialized.contains("\"clickAction\""))
    }

    @Test
    fun `deserializes from JSON correctly`() {
        val raw = """
            {
              "title": "Person",
              "message": "Back",
              "data": {
                "image": "https://frigate.example.com/api/events/xyz/snapshot.jpg",
                "clickAction": "https://frigate.example.com/api/events/xyz/clip.mp4"
              }
            }
        """.trimIndent()

        val result = json.decodeFromString<HaNotification>(raw)

        assertEquals("Person", result.title)
        assertEquals("Back", result.message)
        assertEquals("https://frigate.example.com/api/events/xyz/snapshot.jpg", result.data.image)
        assertEquals("https://frigate.example.com/api/events/xyz/clip.mp4", result.data.clickAction)
    }
}