package com.allenarcher

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FrigateEventDeserializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val newEventJson = """
        {
          "before": {
            "id": "1728764816.726714-rnq349",
            "camera": "front",
            "frame_time": 1728764816.726714,
            "snapshot": null,
            "label": "car",
            "sub_label": null,
            "top_score": 0,
            "false_positive": true,
            "start_time": 1728764816.726714,
            "end_time": null,
            "score": 0.82421875,
            "box": [1075, 105, 1234, 195],
            "area": 14310,
            "ratio": 1.7666666666666666,
            "region": [972, 0, 1292, 320],
            "stationary": false,
            "motionless_count": 0,
            "position_changes": 0,
            "current_zones": [],
            "entered_zones": [],
            "has_clip": false,
            "has_snapshot": false,
            "attributes": {},
            "current_attributes": []
          },
          "after": {
            "id": "1728764816.726714-rnq349",
            "camera": "front",
            "frame_time": 1728764816.805299,
            "snapshot": {
              "frame_time": 1728764816.805299,
              "box": [1077, 107, 1234, 198],
              "area": 14287,
              "region": [971, 0, 1291, 320],
              "score": 0.8203125,
              "attributes": []
            },
            "label": "car",
            "sub_label": null,
            "top_score": 0.8203125,
            "false_positive": false,
            "start_time": 1728764816.726714,
            "end_time": null,
            "score": 0.8203125,
            "box": [1077, 107, 1234, 198],
            "area": 14287,
            "ratio": 1.7252747252747254,
            "region": [971, 0, 1291, 320],
            "stationary": false,
            "motionless_count": 1,
            "position_changes": 0,
            "current_zones": [],
            "entered_zones": [],
            "has_clip": false,
            "has_snapshot": false,
            "attributes": {},
            "current_attributes": []
          },
          "type": "new"
        }
    """.trimIndent()

    private val updateEventJson = """
        {
          "before": {
            "id": "1728764816.726714-rnq349",
            "camera": "front",
            "frame_time": 1728764817.130143,
            "snapshot": {
              "frame_time": 1728764817.130143,
              "box": [1085, 107, 1234, 198],
              "area": 13559,
              "region": [971, 0, 1291, 320],
              "score": 0.78125,
              "attributes": []
            },
            "label": "car",
            "sub_label": null,
            "top_score": 0.8203125,
            "false_positive": false,
            "start_time": 1728764816.726714,
            "end_time": null,
            "score": 0.78125,
            "box": [1085, 107, 1234, 198],
            "area": 13559,
            "ratio": 1.6373626373626373,
            "region": [971, 0, 1291, 320],
            "stationary": false,
            "motionless_count": 0,
            "position_changes": 1,
            "current_zones": ["driveway"],
            "entered_zones": ["driveway"],
            "has_clip": true,
            "has_snapshot": true,
            "attributes": {},
            "current_attributes": []
          },
          "after": {
            "id": "1728764816.726714-rnq349",
            "camera": "front",
            "frame_time": 1728764822.216698,
            "snapshot": {
              "frame_time": 1728764821.821785,
              "box": [810, 186, 1081, 480],
              "area": 79530,
              "region": [648, 0, 1288, 640],
              "score": 0.87890625,
              "attributes": []
            },
            "label": "car",
            "sub_label": "Tesla",
            "top_score": 0.87890625,
            "false_positive": false,
            "start_time": 1728764816.726714,
            "end_time": null,
            "score": 0.87890625,
            "box": [810, 186, 1081, 480],
            "area": 79530,
            "ratio": 0.9217687074829932,
            "region": [648, 0, 1288, 640],
            "stationary": true,
            "motionless_count": 10,
            "position_changes": 3,
            "current_zones": ["driveway"],
            "entered_zones": ["driveway"],
            "has_clip": true,
            "has_snapshot": true,
            "attributes": {},
            "current_attributes": []
          },
          "type": "update"
        }
    """.trimIndent()

    @Test
    fun `type and top-level fields deserialize correctly`() {
        val newEvent = json.decodeFromString<FrigateEvent>(newEventJson)
        assertEquals("new", newEvent.type)

        val updateEvent = json.decodeFromString<FrigateEvent>(updateEventJson)
        assertEquals("update", updateEvent.type)
    }

    @Test
    fun `before detection deserializes correctly`() {
        val event = json.decodeFromString<FrigateEvent>(newEventJson)
        val before = event.before

        assertEquals("1728764816.726714-rnq349", before.id)
        assertEquals("front", before.camera)
        assertEquals(1728764816.726714, before.frameTime)
        assertEquals("car", before.label)
        assertNull(before.subLabel)
        assertEquals(0.0, before.topScore)
        assertEquals(true, before.falsePositive)
        assertEquals(1728764816.726714, before.startTime)
        assertNull(before.endTime)
        assertEquals(0.82421875, before.score)
        assertEquals(listOf(1075, 105, 1234, 195), before.box)
        assertEquals(14310, before.area)
        assertEquals(listOf(972, 0, 1292, 320), before.region)
        assertEquals(false, before.stationary)
        assertEquals(0, before.motionlessCount)
        assertEquals(0, before.positionChanges)
        assertEquals(emptyList(), before.currentZones)
        assertEquals(emptyList(), before.enteredZones)
        assertEquals(false, before.hasClip)
        assertEquals(false, before.hasSnapshot)
        assertNull(before.snapshot)
    }

    @Test
    fun `after detection deserializes correctly`() {
        val event = json.decodeFromString<FrigateEvent>(newEventJson)
        val after = event.after

        assertEquals("1728764816.726714-rnq349", after.id)
        assertEquals("front", after.camera)
        assertEquals("car", after.label)
        assertNull(after.subLabel)
        assertEquals(0.8203125, after.topScore)
        assertEquals(false, after.falsePositive)
        assertNull(after.endTime)
        assertEquals(false, after.hasSnapshot)
    }

    @Test
    fun `snapshot deserializes correctly`() {
        val event = json.decodeFromString<FrigateEvent>(newEventJson)
        val snapshot = assertNotNull(event.after.snapshot)

        assertEquals(1728764816.805299, snapshot.frameTime)
        assertEquals(listOf(1077, 107, 1234, 198), snapshot.box)
        assertEquals(14287, snapshot.area)
        assertEquals(listOf(971, 0, 1291, 320), snapshot.region)
        assertEquals(0.8203125, snapshot.score)
    }

    @Test
    fun `zones deserialize correctly`() {
        val event = json.decodeFromString<FrigateEvent>(updateEventJson)

        assertEquals(listOf("driveway"), event.after.currentZones)
        assertEquals(listOf("driveway"), event.after.enteredZones)
    }

    @Test
    fun `sub_label deserializes correctly`() {
        val event = json.decodeFromString<FrigateEvent>(updateEventJson)
        assertEquals("Tesla", event.after.subLabel)
    }

    @Test
    fun `stationary and counts deserialize correctly`() {
        val event = json.decodeFromString<FrigateEvent>(updateEventJson)
        val after = event.after

        assertEquals(true, after.stationary)
        assertEquals(10, after.motionlessCount)
        assertEquals(3, after.positionChanges)
    }

    @Test
    fun `has_clip and has_snapshot deserialize correctly`() {
        val event = json.decodeFromString<FrigateEvent>(updateEventJson)

        assertEquals(true, event.before.hasClip)
        assertEquals(true, event.before.hasSnapshot)
        assertEquals(true, event.after.hasClip)
        assertEquals(true, event.after.hasSnapshot)
    }
}