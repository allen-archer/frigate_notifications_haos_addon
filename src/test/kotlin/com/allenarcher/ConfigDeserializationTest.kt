package com.allenarcher

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ConfigDeserializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val fullJson = """
        {
          "ntfy_enabled": true,
          "ntfy_url": "http://192.168.1.105:3050",
          "ntfy_user": "alice",
          "ntfy_password": "secret",
          "ntfy_token": "tk_abc123",
          "ntfy_topic": "alerts",
          "ntfy_tags": [
            {"object": "car", "tags": "car"},
            {"object": "person", "tags": "walking"}
          ],
          "grouping_enabled": true,
          "grouping_minutes": 10,
          "ntfy_normal_priority": 3,
          "ntfy_lower_priority": 2,
          "ha_enabled": false,
          "ha_entity_ids": ["notify.mobile_app"],
          "frigate_url": "https://frigate.example.com",
          "disabled_cameras": [
            {"camera_name": "front_door", "disabled_objects": ["car"]}
          ],
          "disabled_objects": ["bird"],
          "snapshot_options": {
            "bbox": 1,
            "timestamp": 1,
            "crop": 1,
            "h": 480,
            "quality": 90
          },
          "mqtt_address": "tcp://192.168.1.105",
          "mqtt_port": 1883,
          "mqtt_topic": "frigate/events",
          "mqtt_username": "user",
          "mqtt_password": "pass",
          "log_level": "debug"
        }
    """.trimIndent()

    @Test
    fun `all fields deserialize correctly`() {
        val config = json.decodeFromString<Config>(fullJson)

        assertEquals(true, config.ntfyEnabled)
        assertEquals("http://192.168.1.105:3050", config.ntfyUrl)
        assertEquals("alice", config.ntfyUser)
        assertEquals("secret", config.ntfyPassword)
        assertEquals("tk_abc123", config.ntfyToken)
        assertEquals("alerts", config.ntfyTopic)
        assertEquals(true, config.groupingEnabled)
        assertEquals(10, config.groupingMinutes)
        assertEquals(3, config.ntfyNormalPriority)
        assertEquals(2, config.ntfyLowerPriority)
        assertEquals(false, config.haEnabled)
        assertEquals("https://frigate.example.com", config.frigateUrl)
        assertEquals("tcp://192.168.1.105", config.mqttAddress)
        assertEquals(1883, config.mqttPort)
        assertEquals("frigate/events", config.mqttTopic)
        assertEquals("user", config.mqttUsername)
        assertEquals("pass", config.mqttPassword)
        assertEquals("debug", config.logLevel)
    }

    @Test
    fun `ntfy tags deserialize correctly`() {
        val config = json.decodeFromString<Config>(fullJson)

        val tags = assertNotNull(config.ntfyTags)
        assertEquals(2, tags.size)
        assertEquals("car", tags[0].objectName)
        assertEquals("car", tags[0].tags)
        assertEquals("person", tags[1].objectName)
        assertEquals("walking", tags[1].tags)
    }

    @Test
    fun `ha entity ids deserialize correctly`() {
        val config = json.decodeFromString<Config>(fullJson)

        val ids = assertNotNull(config.haEntityIds)
        assertEquals(listOf("notify.mobile_app"), ids)
    }

    @Test
    fun `disabled cameras deserialize correctly`() {
        val config = json.decodeFromString<Config>(fullJson)

        val cameras = assertNotNull(config.disabledCameras)
        assertEquals(1, cameras.size)
        assertEquals("front_door", cameras[0].cameraName)
        assertEquals(listOf("car"), cameras[0].disabledObjects)
    }

    @Test
    fun `disabled objects deserialize correctly`() {
        val config = json.decodeFromString<Config>(fullJson)

        assertEquals(listOf("bird"), config.disabledObjects)
    }

    @Test
    fun `snapshot options deserialize correctly`() {
        val config = json.decodeFromString<Config>(fullJson)

        val opts = assertNotNull(config.snapshotOptions)
        assertEquals(1, opts.bbox)
        assertEquals(1, opts.timestamp)
        assertEquals(1, opts.crop)
        assertEquals(480, opts.h)
        assertEquals(90, opts.quality)
    }

    @Test
    fun `optional fields default to null`() {
        val minimalJson = """
            {
              "ntfy_enabled": true,
              "ntfy_url": "http://localhost",
              "ntfy_topic": "test",
              "grouping_enabled": false,
              "ntfy_normal_priority": 3,
              "ha_enabled": false,
              "frigate_url": "http://localhost",
              "mqtt_address": "tcp://localhost",
              "mqtt_topic": "frigate/events"
            }
        """.trimIndent()

        val config = json.decodeFromString<Config>(minimalJson)

        assertNull(config.ntfyUser)
        assertNull(config.ntfyPassword)
        assertNull(config.ntfyToken)
        assertNull(config.ntfyTags)
        assertNull(config.groupingMinutes)
        assertNull(config.ntfyLowerPriority)
        assertNull(config.haEntityIds)
        assertNull(config.disabledCameras)
        assertNull(config.disabledObjects)
        assertNull(config.snapshotOptions)
        assertNull(config.mqttPort)
        assertNull(config.mqttUsername)
        assertNull(config.mqttPassword)
        assertEquals("error", config.logLevel)
    }

    @Test
    fun `snapshot options toQueryString formats correctly`() {
        val full = SnapshotOptions(bbox = 1, timestamp = 1, crop = 1, h = 480, quality = 90)
        assertEquals("?bbox=1&timestamp=1&crop=1&h=480&quality=90", full.toQueryString())

        val partial = SnapshotOptions(h = 320, quality = 75)
        assertEquals("?h=320&quality=75", partial.toQueryString())

        val empty = SnapshotOptions()
        assertEquals("", empty.toQueryString())
    }
}